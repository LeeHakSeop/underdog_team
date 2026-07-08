import sys
import json
import re
from pathlib import Path

import cv2
import numpy as np
from ultralytics import YOLO

from ocr_postprocess import (
    PLATE_KOREAN,
)
from paddle_ocr_predict import predict_paddle_ocr
from crnn_ocr_predict import predict_crnn_ocr

BASE_DIR = Path(__file__).resolve().parents[1]
MODEL_PATH = BASE_DIR / "models" / "team_yolo_best.pt"
SAVE_DIR = BASE_DIR / "result" / "api_crop"
SAVE_DIR.mkdir(parents=True, exist_ok=True)

MIN_DETECTION_CONFIDENCE = 0.50
MIN_OCR_CONFIDENCE = 0.70
MIN_FINAL_CONFIDENCE = 0.60

model = YOLO(MODEL_PATH)


def imread_korean(path):
    if not Path(path).exists():
        return None

    img_array = np.fromfile(str(path), dtype=np.uint8)
    return cv2.imdecode(img_array, cv2.IMREAD_COLOR)


def imwrite_korean(path, img):
    success, encoded = cv2.imencode(Path(path).suffix, img)
    if success:
        encoded.tofile(str(path))
    return success


def is_plate_format(text):
    return re.match(r"^\d{2,3}[가-힣]\d{4}$", str(text)) is not None


def get_korean_char(text):
    m = re.search(r"[가-힣]", str(text))
    return m.group() if m else None


def need_review_check(text):
    reasons = []

    if not text:
        reasons.append("EMPTY")

    if not is_plate_format(text):
        reasons.append("FORMAT_ERROR")

    kr = get_korean_char(text)

    if kr is None:
        reasons.append("KOREAN_MISSING")
    elif kr not in PLATE_KOREAN:
        reasons.append("KOREAN_NOT_ALLOWED")

    return len(reasons) > 0, reasons


def add_confidence_review_reasons(reasons, detection_confidence, ocr_confidence, final_confidence):
    if detection_confidence < MIN_DETECTION_CONFIDENCE:
        reasons.append("LOW_DETECTION_CONFIDENCE")

    if ocr_confidence < MIN_OCR_CONFIDENCE:
        reasons.append("LOW_OCR_CONFIDENCE")

    if final_confidence < MIN_FINAL_CONFIDENCE:
        reasons.append("LOW_FINAL_CONFIDENCE")

    return reasons


def run_ocr(crop_path, ocr_type):
    if ocr_type == "crnn":
        return predict_crnn_ocr(crop_path)

    return predict_paddle_ocr(crop_path)


def make_empty_result(ocr_type, reasons):
    return {
        "detected": False,
        "plateNumber": "",
        "ocrRaw": "",
        "confidence": 0.0,
        "detectionConfidence": 0.0,
        "ocrConfidence": 0.0,
        "needReview": True,
        "reviewReasons": reasons,
        "cropPath": "",
        "candidates": [],
        "ocrType": ocr_type,
    }


def predict_plate(image_path, ocr_type="paddle"):
    ocr_type = (ocr_type or "paddle").lower()

    if ocr_type not in ["paddle", "crnn"]:
        ocr_type = "paddle"

    img = imread_korean(image_path)

    if img is None:
        return make_empty_result(ocr_type, ["IMAGE_READ_FAIL"])

    results = model.predict(source=str(image_path), conf=0.25, verbose=False)

    candidates = []

    for r in results:
        for i, box in enumerate(r.boxes):
            x1, y1, x2, y2 = box.xyxy[0].cpu().numpy().astype(int)
            detection_confidence = float(box.conf[0].cpu().numpy())

            crop = img[y1:y2, x1:x2]

            if crop.size == 0:
                continue

            crop_path = SAVE_DIR / f"{Path(image_path).stem}_plate_{i}.jpg"
            imwrite_korean(crop_path, crop)

            ocr_raw, ocr_clean, ocr_confidence = run_ocr(crop_path, ocr_type)
            final_confidence = detection_confidence * ocr_confidence
            need_review, reasons = need_review_check(ocr_clean)
            reasons = add_confidence_review_reasons(
                reasons,
                detection_confidence,
                ocr_confidence,
                final_confidence,
            )
            need_review = len(reasons) > 0

            candidates.append({
                "plateNumber": ocr_clean,
                "ocrRaw": ocr_raw,
                "confidence": final_confidence,
                "detectionConfidence": detection_confidence,
                "ocrConfidence": ocr_confidence,
                "needReview": need_review,
                "reviewReasons": reasons,
                "cropPath": crop_path.as_posix(),
                "ocrType": ocr_type,
            })

    if not candidates:
        return make_empty_result(ocr_type, ["PLATE_NOT_DETECTED"])

    # 우선순위: 정상 번호판 형식 > confidence 높은 것
    valid = [c for c in candidates if c["needReview"] is False]

    if valid:
        best = sorted(valid, key=lambda x: x["confidence"], reverse=True)[0]
    else:
        best = sorted(candidates, key=lambda x: x["confidence"], reverse=True)[0]

    return {
        "detected": True,
        "plateNumber": best["plateNumber"],
        "ocrRaw": best["ocrRaw"],
        "confidence": best["confidence"],
        "detectionConfidence": best["detectionConfidence"],
        "ocrConfidence": best["ocrConfidence"],
        "needReview": best["needReview"],
        "reviewReasons": best["reviewReasons"],
        "cropPath": best["cropPath"],
        "candidates": candidates,
        "ocrType": ocr_type,
    }


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({
            "error": "IMAGE_PATH_REQUIRED"
        }, ensure_ascii=False))
        sys.exit(1)

    image_path = sys.argv[1]
    result = predict_plate(image_path)

    print(json.dumps(result, ensure_ascii=False))
