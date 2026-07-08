import argparse
import csv
import json
from pathlib import Path

import torch
from torch.utils.data import DataLoader

from train_crnn_ocr import CRNN, PlateDataset, collate_batch, evaluate, load_rows, resolve
from train_crnn_ocr import edit_distance, greedy_decode, normalize_plate
from plate_postprocess import postprocess_plate


@torch.no_grad()
def evaluate_with_postprocess(model, loader, device, idx_to_char, report_path):
    model.eval()
    total = 0
    raw_correct = 0
    post_correct = 0
    raw_edit_distance = 0
    post_edit_distance = 0
    total_chars = 0
    rows = []

    for images, _, _, labels, paths in loader:
        images = images.to(device)
        logits = model(images)
        predictions = greedy_decode(logits, idx_to_char)
        for path, label, prediction in zip(paths, labels, predictions):
            normalized_label = normalize_plate(label)
            normalized_prediction = normalize_plate(prediction)
            post = postprocess_plate(prediction)
            normalized_post_prediction = normalize_plate(post.corrected)

            raw_distance = edit_distance(normalized_label, normalized_prediction)
            post_distance = edit_distance(normalized_label, normalized_post_prediction)
            raw_is_correct = normalized_label == normalized_prediction
            post_is_correct = normalized_label == normalized_post_prediction

            total += 1
            raw_correct += int(raw_is_correct)
            post_correct += int(post_is_correct)
            total_chars += len(normalized_label)
            raw_edit_distance += raw_distance
            post_edit_distance += post_distance
            rows.append(
                {
                    "image": path,
                    "label": label,
                    "prediction": prediction,
                    "post_prediction": post.corrected,
                    "normalized_label": normalized_label,
                    "normalized_prediction": normalized_prediction,
                    "normalized_post_prediction": normalized_post_prediction,
                    "raw_edit_distance": raw_distance,
                    "post_edit_distance": post_distance,
                    "raw_is_correct": str(raw_is_correct),
                    "post_is_correct": str(post_is_correct),
                    "post_rules": "|".join(post.rules),
                    "post_is_valid": str(post.is_valid),
                    "post_needs_review": str(post.needs_review),
                }
            )

    report_path.parent.mkdir(parents=True, exist_ok=True)
    with report_path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(
            file,
            fieldnames=[
                "image",
                "label",
                "prediction",
                "post_prediction",
                "normalized_label",
                "normalized_prediction",
                "normalized_post_prediction",
                "raw_edit_distance",
                "post_edit_distance",
                "raw_is_correct",
                "post_is_correct",
                "post_rules",
                "post_is_valid",
                "post_needs_review",
            ],
        )
        writer.writeheader()
        writer.writerows(rows)

    raw_accuracy = (raw_correct / total * 100.0) if total else 0.0
    post_accuracy = (post_correct / total * 100.0) if total else 0.0
    raw_char_accuracy = (
        max(0.0, (1.0 - (raw_edit_distance / total_chars)) * 100.0)
        if total_chars
        else 0.0
    )
    post_char_accuracy = (
        max(0.0, (1.0 - (post_edit_distance / total_chars)) * 100.0)
        if total_chars
        else 0.0
    )
    return {
        "total": total,
        "raw_correct": raw_correct,
        "post_correct": post_correct,
        "raw_accuracy": raw_accuracy,
        "post_accuracy": post_accuracy,
        "raw_char_accuracy": raw_char_accuracy,
        "post_char_accuracy": post_char_accuracy,
        "raw_avg_edit_distance": (raw_edit_distance / total) if total else 0.0,
        "post_avg_edit_distance": (post_edit_distance / total) if total else 0.0,
    }


def main():
    parser = argparse.ArgumentParser(description="Evaluate a trained CRNN OCR model.")
    parser.add_argument("--model", required=True, help="Path to CRNN best.pt checkpoint.")
    parser.add_argument("--labels-csv", default="ocr_test_10000/labels.csv")
    parser.add_argument("--output", default="runs/ocr/crnn_plate_all_eval")
    parser.add_argument("--split", default="all", choices=["all", "train", "val"])
    parser.add_argument("--batch-size", type=int, default=64)
    parser.add_argument("--device", default="auto", choices=["auto", "cpu", "cuda"])
    parser.add_argument("--postprocess", action="store_true", help="Also score conservative plate postprocessing.")
    args = parser.parse_args()

    model_path = resolve(args.model)
    labels_csv = resolve(args.labels_csv)
    output = resolve(args.output)
    output.mkdir(parents=True, exist_ok=True)

    device = args.device
    if device == "auto":
        device = "cuda" if torch.cuda.is_available() else "cpu"
    device = torch.device(device)

    checkpoint = torch.load(model_path, map_location=device)
    charset = checkpoint["charset"]
    width = checkpoint.get("width", 160)
    height = checkpoint.get("height", 32)
    char_to_idx = {char: index + 1 for index, char in enumerate(charset)}
    idx_to_char = {str(index + 1): char for index, char in enumerate(charset)}
    idx_to_char["0"] = ""

    rows = load_rows(labels_csv)
    if args.split != "all":
        rows = [row for row in rows if row["split"] == args.split]
    if not rows:
        raise SystemExit(f"No rows found for split={args.split} in {labels_csv}")

    dataset = PlateDataset(rows, Path.cwd(), char_to_idx, width, height, allow_unknown_target=True)
    loader = DataLoader(
        dataset,
        batch_size=args.batch_size,
        shuffle=False,
        num_workers=0,
        collate_fn=collate_batch,
    )

    model = CRNN(num_classes=len(charset) + 1).to(device)
    model.load_state_dict(checkpoint["model"])

    report_path = output / f"evaluation_{args.split}.csv"
    if args.postprocess:
        metrics = evaluate_with_postprocess(model, loader, device, idx_to_char, report_path=report_path)
        total = metrics["total"]
        correct = metrics["raw_correct"]
        accuracy = metrics["raw_accuracy"]
        char_accuracy = metrics["raw_char_accuracy"]
        avg_edit_distance = metrics["raw_avg_edit_distance"]
    else:
        accuracy, correct, total, char_accuracy, avg_edit_distance = evaluate(
            model, loader, device, idx_to_char, report_path=report_path
        )

    summary = (
        f"Model: {model_path}\n"
        f"Labels: {labels_csv}\n"
        f"Split: {args.split}\n"
        f"Total: {total}\n"
        f"Correct: {correct}\n"
        f"Accuracy: {accuracy:.2f}%\n"
        f"Char accuracy: {char_accuracy:.2f}%\n"
        f"Avg edit distance: {avg_edit_distance:.3f}\n"
        f"Report: {report_path}\n"
    )
    if args.postprocess:
        summary += (
            f"Postprocess correct: {metrics['post_correct']}\n"
            f"Postprocess accuracy: {metrics['post_accuracy']:.2f}%\n"
            f"Postprocess char accuracy: {metrics['post_char_accuracy']:.2f}%\n"
            f"Postprocess avg edit distance: {metrics['post_avg_edit_distance']:.3f}\n"
        )
    (output / f"summary_{args.split}.txt").write_text(summary, encoding="utf-8")

    with (output / f"metrics_{args.split}.csv").open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(
            file,
            fieldnames=[
                "model",
                "labels",
                "split",
                "total",
                "correct",
                "accuracy",
                "char_accuracy",
                "avg_edit_distance",
                "post_correct",
                "post_accuracy",
                "post_char_accuracy",
                "post_avg_edit_distance",
            ],
        )
        writer.writeheader()
        writer.writerow(
            {
                "model": str(model_path),
                "labels": str(labels_csv),
                "split": args.split,
                "total": total,
                "correct": correct,
                "accuracy": f"{accuracy:.2f}",
                "char_accuracy": f"{char_accuracy:.2f}",
                "avg_edit_distance": f"{avg_edit_distance:.3f}",
                "post_correct": metrics["post_correct"] if args.postprocess else "",
                "post_accuracy": f"{metrics['post_accuracy']:.2f}" if args.postprocess else "",
                "post_char_accuracy": f"{metrics['post_char_accuracy']:.2f}" if args.postprocess else "",
                "post_avg_edit_distance": f"{metrics['post_avg_edit_distance']:.3f}" if args.postprocess else "",
            }
        )

    print(summary)


if __name__ == "__main__":
    main()
