import argparse
import csv
import json
import random
import re
from pathlib import Path

import cv2
import numpy as np
from ultralytics import YOLO


IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
LABEL_FILTER = re.compile(r"[^0-9A-Za-z\uAC00-\uD7A3]")


def imread_korean(path):
    image_array = np.fromfile(str(path), dtype=np.uint8)
    return cv2.imdecode(image_array, cv2.IMREAD_COLOR)


def imwrite_korean(path, image):
    success, encoded = cv2.imencode(Path(path).suffix, image)
    if success:
        encoded.tofile(str(path))
    return success


def normalize_label(value):
    return LABEL_FILTER.sub("", value or "").upper()


def iter_images(input_dir):
    for path in sorted(input_dir.iterdir(), key=lambda p: p.name):
        if path.is_file() and path.suffix.lower() in IMAGE_SUFFIXES:
            yield path


def center_score(x1, y1, x2, y2, image_width, image_height):
    box_cx = (x1 + x2) / 2.0
    box_cy = (y1 + y2) / 2.0
    dx = abs(box_cx - (image_width * 0.50)) / (image_width / 2.0)
    dy = abs(box_cy - (image_height * 0.55)) / (image_height / 2.0)
    return max(0.0, 1.0 - min(1.0, dx + (dy * 0.40)))


def expand_box(x1, y1, x2, y2, image_width, image_height, pad_ratio):
    width = x2 - x1
    height = y2 - y1
    pad_x = int(width * pad_ratio)
    pad_y = int(height * pad_ratio)
    return (
        max(0, x1 - pad_x),
        max(0, y1 - pad_y),
        min(image_width, x2 + pad_x),
        min(image_height, y2 + pad_y),
    )


def choose_box(boxes, image_width, image_height):
    candidates = []
    for box in boxes:
        x1, y1, x2, y2 = box.xyxy[0].cpu().numpy().astype(int)
        confidence = float(box.conf[0].cpu().numpy())
        score = center_score(x1, y1, x2, y2, image_width, image_height)
        candidates.append((score, confidence, x1, y1, x2, y2))
    if not candidates:
        return None
    return sorted(candidates, reverse=True)[0]


def main():
    parser = argparse.ArgumentParser(description="Prepare CRNN OCR crops and labels from truck plate images.")
    parser.add_argument("input_dir")
    parser.add_argument("--model", default=str(Path("models") / "team_yolo_best.pt"))
    parser.add_argument("--output-dir", default=str(Path("datasets") / "truck_crnn"))
    parser.add_argument("--val-ratio", type=float, default=0.10)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--conf", type=float, default=0.25)
    parser.add_argument("--pad-ratio", type=float, default=0.08)
    args = parser.parse_args()

    input_dir = Path(args.input_dir)
    output_dir = Path(args.output_dir)
    crop_dir = output_dir / "crops"
    crop_dir.mkdir(parents=True, exist_ok=True)

    model = YOLO(args.model)
    rows = []
    failures = []

    images = list(iter_images(input_dir))
    for index, image_path in enumerate(images, start=1):
        image = imread_korean(image_path)
        label = normalize_label(image_path.stem)
        if image is None:
            failures.append({"fileName": image_path.name, "reason": "IMAGE_READ_FAIL"})
            print(f"[{index}/{len(images)}] {image_path.name} -> read fail")
            continue

        image_height, image_width = image.shape[:2]
        results = model.predict(source=str(image_path), conf=args.conf, verbose=False)
        boxes = [box for result in results for box in result.boxes]
        selected = choose_box(boxes, image_width, image_height)
        if selected is None:
            failures.append({"fileName": image_path.name, "reason": "PLATE_NOT_DETECTED"})
            print(f"[{index}/{len(images)}] {image_path.name} -> no detection")
            continue

        score, confidence, x1, y1, x2, y2 = selected
        x1, y1, x2, y2 = expand_box(x1, y1, x2, y2, image_width, image_height, args.pad_ratio)
        crop = image[y1:y2, x1:x2]
        if crop.size == 0:
            failures.append({"fileName": image_path.name, "reason": "EMPTY_CROP"})
            print(f"[{index}/{len(images)}] {image_path.name} -> empty crop")
            continue

        crop_path = crop_dir / f"{image_path.stem}.jpg"
        imwrite_korean(crop_path, crop)
        rows.append(
            {
                "image": crop_path.as_posix(),
                "label": label,
                "split": "",
                "source": str(image_path),
                "detection_confidence": f"{confidence:.6f}",
                "center_score": f"{score:.6f}",
                "box": json.dumps([int(x1), int(y1), int(x2), int(y2)]),
            }
        )
        print(f"[{index}/{len(images)}] {image_path.name} -> {crop_path.name} label={label}")

    random.seed(args.seed)
    shuffled = rows[:]
    random.shuffle(shuffled)
    val_count = max(1, int(len(shuffled) * args.val_ratio)) if shuffled else 0
    val_images = {row["image"] for row in shuffled[:val_count]}
    for row in rows:
        row["split"] = "val" if row["image"] in val_images else "train"

    labels_path = output_dir / "labels.csv"
    metadata_path = output_dir / "metadata.json"
    failures_path = output_dir / "failures.csv"

    fieldnames = [
        "image",
        "label",
        "split",
        "source",
        "detection_confidence",
        "center_score",
        "box",
    ]
    with labels_path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    with failures_path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=["fileName", "reason"])
        writer.writeheader()
        writer.writerows(failures)

    summary = {
        "inputCount": len(images),
        "datasetCount": len(rows),
        "failureCount": len(failures),
        "trainCount": sum(1 for row in rows if row["split"] == "train"),
        "valCount": sum(1 for row in rows if row["split"] == "val"),
        "labelsPath": str(labels_path),
        "cropDir": str(crop_dir),
        "failuresPath": str(failures_path),
    }
    metadata_path.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
