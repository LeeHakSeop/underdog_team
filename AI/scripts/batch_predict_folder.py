import argparse
import csv
import json
from pathlib import Path

from predict_plate import predict_plate


IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


def iter_images(input_dir):
    for path in sorted(input_dir.iterdir(), key=lambda p: p.name):
        if path.is_file() and path.suffix.lower() in IMAGE_SUFFIXES:
            yield path


def main():
    parser = argparse.ArgumentParser(description="Run plate recognition for every image in a folder.")
    parser.add_argument("input_dir", help="Folder containing plate images.")
    parser.add_argument("--ocr-type", choices=["paddle", "crnn"], default="paddle")
    parser.add_argument("--output-dir", default=str(Path("result") / "batch_predict"))
    parser.add_argument("--name", default="truck_plates")
    args = parser.parse_args()

    input_dir = Path(args.input_dir)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    images = list(iter_images(input_dir))
    results = []

    for index, image_path in enumerate(images, start=1):
        result = predict_plate(image_path, args.ocr_type)
        row = {
            "index": index,
            "fileName": image_path.name,
            "imagePath": str(image_path),
            **result,
        }
        results.append(row)
        print(
            f"[{index}/{len(images)}] {image_path.name} -> "
            f"{row.get('plateNumber', '')} detected={row.get('detected')} review={row.get('needReview')}"
        )

    json_path = output_dir / f"{args.name}.json"
    csv_path = output_dir / f"{args.name}.csv"

    json_path.write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")

    csv_fields = [
        "index",
        "fileName",
        "imagePath",
        "detected",
        "plateNumber",
        "ocrRaw",
        "confidence",
        "detectionConfidence",
        "ocrConfidence",
        "centerScore",
        "needReview",
        "reviewReasons",
        "cropPath",
        "ocrType",
    ]
    with csv_path.open("w", newline="", encoding="utf-8-sig") as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=csv_fields, extrasaction="ignore")
        writer.writeheader()
        for row in results:
            csv_row = row.copy()
            csv_row["reviewReasons"] = "|".join(csv_row.get("reviewReasons", []))
            writer.writerow(csv_row)

    detected_count = sum(1 for row in results if row.get("detected"))
    review_count = sum(1 for row in results if row.get("needReview"))

    print(json.dumps({
        "inputCount": len(images),
        "detectedCount": detected_count,
        "needReviewCount": review_count,
        "jsonPath": str(json_path),
        "csvPath": str(csv_path),
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
