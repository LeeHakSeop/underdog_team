import argparse
import csv
from pathlib import Path

import cv2
import numpy as np
import torch
from torch.utils.data import DataLoader, Dataset

from plate_postprocess import postprocess_plate
from plate_preprocess import (
    AVAILABLE_PREPROCESS_MODES,
    preprocess_mode_name,
    preprocess_plate_image,
    safe_preprocess_label,
)
from train_crnn_ocr import CRNN, edit_distance, greedy_decode, load_rows, normalize_plate, resolve


def read_image_unicode(path: Path):
    data = np.frombuffer(path.read_bytes(), dtype=np.uint8)
    return cv2.imdecode(data, cv2.IMREAD_COLOR)


class PreprocessPlateDataset(Dataset):
    def __init__(self, rows, root: Path, char_to_idx, width: int, height: int, preprocess: str):
        self.rows = rows
        self.root = root
        self.char_to_idx = char_to_idx
        self.width = width
        self.height = height
        self.preprocess = preprocess

    def __len__(self):
        return len(self.rows)

    def __getitem__(self, index):
        row = self.rows[index]
        image_path = Path(row["image"])
        if not image_path.is_absolute():
            image_path = self.root / image_path

        image = read_image_unicode(image_path)
        if image is None:
            raise FileNotFoundError(f"Could not read image: {image_path}")

        image = preprocess_plate_image(image, self.preprocess)
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        resized = cv2.resize(gray, (self.width, self.height), interpolation=cv2.INTER_CUBIC)

        tensor = torch.tensor(resized, dtype=torch.float32).view(1, self.height, self.width) / 255.0
        tensor = (tensor - 0.5) / 0.5
        target = torch.tensor([self.char_to_idx[c] for c in row["label"] if c in self.char_to_idx], dtype=torch.long)

        return {
            "image": tensor,
            "target": target,
            "label": row["label"],
            "path": str(image_path),
        }


def collate_batch(batch):
    images = torch.stack([item["image"] for item in batch])
    targets = torch.cat([item["target"] for item in batch])
    target_lengths = torch.tensor([len(item["target"]) for item in batch], dtype=torch.long)
    labels = [item["label"] for item in batch]
    paths = [item["path"] for item in batch]
    return images, targets, target_lengths, labels, paths


@torch.no_grad()
def evaluate_mode(model, loader, device, idx_to_char, mode: str, report_path: Path):
    model.eval()
    total = 0
    raw_correct = 0
    post_correct = 0
    raw_edit_distance = 0
    post_edit_distance = 0
    total_chars = 0
    invalid_format = 0
    changed_by_rule = 0
    fixed_by_rule = 0
    broken_by_rule = 0
    rows = []

    for images, _, _, labels, paths in loader:
        images = images.to(device)
        logits = model(images)
        predictions = greedy_decode(logits, idx_to_char)

        for path, label, prediction in zip(paths, labels, predictions):
            post = postprocess_plate(prediction)
            normalized_label = normalize_plate(label)
            normalized_prediction = normalize_plate(prediction)
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
            invalid_format += int(not post.is_valid)
            changed_by_rule += int(post.changed)
            fixed_by_rule += int((not raw_is_correct) and post_is_correct)
            broken_by_rule += int(raw_is_correct and (not post_is_correct))

            rows.append(
                {
                    "preprocess": mode,
                    "image": path,
                    "label": label,
                    "raw_pred": prediction,
                    "post_pred": post.corrected,
                    "normalized_label": normalized_label,
                    "normalized_raw_pred": normalized_prediction,
                    "normalized_post_pred": normalized_post_prediction,
                    "raw_edit_distance": raw_distance,
                    "post_edit_distance": post_distance,
                    "raw_correct": str(raw_is_correct),
                    "post_correct": str(post_is_correct),
                    "is_valid_plate": str(post.is_valid),
                    "changed_by_rule": str(post.changed),
                    "post_rules": "|".join(post.rules),
                    "needs_review": str(post.needs_review),
                }
            )

    report_path.parent.mkdir(parents=True, exist_ok=True)
    with report_path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(
            file,
            fieldnames=[
                "preprocess",
                "image",
                "label",
                "raw_pred",
                "post_pred",
                "normalized_label",
                "normalized_raw_pred",
                "normalized_post_pred",
                "raw_edit_distance",
                "post_edit_distance",
                "raw_correct",
                "post_correct",
                "is_valid_plate",
                "changed_by_rule",
                "post_rules",
                "needs_review",
            ],
        )
        writer.writeheader()
        writer.writerows(rows)

    raw_accuracy = (raw_correct / total * 100.0) if total else 0.0
    post_accuracy = (post_correct / total * 100.0) if total else 0.0
    raw_char_accuracy = max(0.0, (1.0 - raw_edit_distance / total_chars) * 100.0) if total_chars else 0.0
    post_char_accuracy = max(0.0, (1.0 - post_edit_distance / total_chars) * 100.0) if total_chars else 0.0

    return {
        "preprocess": mode,
        "total": total,
        "raw_correct": raw_correct,
        "raw_accuracy": raw_accuracy,
        "raw_char_accuracy": raw_char_accuracy,
        "raw_avg_edit_distance": (raw_edit_distance / total) if total else 0.0,
        "post_correct": post_correct,
        "post_accuracy": post_accuracy,
        "post_char_accuracy": post_char_accuracy,
        "post_avg_edit_distance": (post_edit_distance / total) if total else 0.0,
        "invalid_format": invalid_format,
        "changed_by_rule": changed_by_rule,
        "fixed_by_rule": fixed_by_rule,
        "broken_by_rule": broken_by_rule,
        "report": str(report_path),
    }


def parse_modes(value: str):
    modes = [mode.strip().lower() for mode in value.split(",") if mode.strip()]
    unknown = [mode for mode in modes if preprocess_mode_name(mode) not in AVAILABLE_PREPROCESS_MODES]
    if unknown:
        raise SystemExit(f"Unknown preprocess mode(s): {', '.join(unknown)}")
    return modes


def infer_image_root(rows, labels_csv: Path, requested_root: str):
    if requested_root:
        return resolve(requested_root)
    if not rows:
        return Path.cwd()

    first_image = Path(rows[0]["image"])
    if first_image.is_absolute():
        return Path.cwd()

    candidates = [
        Path.cwd(),
        labels_csv.parent,
        labels_csv.parent.parent,
    ]
    for candidate in candidates:
        if (candidate / first_image).exists():
            return candidate
    return Path.cwd()


def main():
    parser = argparse.ArgumentParser(description="Run CRNN OCR preprocessing ablation and write CSV reports.")
    parser.add_argument("--model", default="model/best.pt", help="Path to CRNN best.pt checkpoint.")
    parser.add_argument("--labels-csv", required=True, help="labels.csv with image,label,split columns.")
    parser.add_argument("--image-root", default="", help="Root directory for relative image paths in labels.csv.")
    parser.add_argument("--output", default="runs/ocr/preprocess_ablation", help="Output directory.")
    parser.add_argument("--split", default="val", choices=["all", "train", "val"])
    parser.add_argument("--modes", default="none,gray,clahe,sharpen,threshold")
    parser.add_argument("--batch-size", type=int, default=64)
    parser.add_argument("--device", default="auto", choices=["auto", "cpu", "cuda"])
    args = parser.parse_args()

    model_path = resolve(args.model)
    labels_csv = resolve(args.labels_csv)
    output = resolve(args.output)
    output.mkdir(parents=True, exist_ok=True)
    modes = parse_modes(args.modes)

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
    image_root = infer_image_root(rows, labels_csv, args.image_root)

    model = CRNN(num_classes=len(charset) + 1).to(device)
    model.load_state_dict(checkpoint["model"])

    summaries = []
    for mode in modes:
        mode_label = safe_preprocess_label(mode)
        dataset = PreprocessPlateDataset(rows, image_root, char_to_idx, width, height, preprocess=mode)
        loader = DataLoader(
            dataset,
            batch_size=args.batch_size,
            shuffle=False,
            num_workers=0,
            collate_fn=collate_batch,
        )
        report_path = output / f"evaluation_{args.split}_{mode_label}.csv"
        metrics = evaluate_mode(model, loader, device, idx_to_char, mode, report_path)
        summaries.append(metrics)
        print(
            f"{mode}: raw_acc={metrics['raw_accuracy']:.2f}% "
            f"post_acc={metrics['post_accuracy']:.2f}% "
            f"raw_char={metrics['raw_char_accuracy']:.2f}% "
            f"post_char={metrics['post_char_accuracy']:.2f}% "
            f"invalid={metrics['invalid_format']}"
        )

    summary_path = output / f"summary_{args.split}.csv"
    with summary_path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(
            file,
            fieldnames=[
                "preprocess",
                "total",
                "raw_correct",
                "raw_accuracy",
                "raw_char_accuracy",
                "raw_avg_edit_distance",
                "post_correct",
                "post_accuracy",
                "post_char_accuracy",
                "post_avg_edit_distance",
                "invalid_format",
                "changed_by_rule",
                "fixed_by_rule",
                "broken_by_rule",
                "report",
            ],
        )
        writer.writeheader()
        for metrics in summaries:
            writer.writerow(
                {
                    **metrics,
                    "raw_accuracy": f"{metrics['raw_accuracy']:.2f}",
                    "raw_char_accuracy": f"{metrics['raw_char_accuracy']:.2f}",
                    "raw_avg_edit_distance": f"{metrics['raw_avg_edit_distance']:.3f}",
                    "post_accuracy": f"{metrics['post_accuracy']:.2f}",
                    "post_char_accuracy": f"{metrics['post_char_accuracy']:.2f}",
                    "post_avg_edit_distance": f"{metrics['post_avg_edit_distance']:.3f}",
                }
            )

    print(f"Summary: {summary_path}")


if __name__ == "__main__":
    main()
