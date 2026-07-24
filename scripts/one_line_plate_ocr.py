from collections import Counter
from pathlib import Path

from ultralytics import YOLO

from config import (
    MODEL_PATHS,
    REGION_RATIOS,
    BODY_START_RATIOS,
    TYPE_IMAGE_SIZE,
)
from crnn_core import CRNNRecognizer
from plate_grammar import (
    clean_text,
    is_valid_truck_body,
    is_valid_truck_full,
)


class OneLineTruckOCR:
    def __init__(
        self,
        region_model_path=None,
        body_model_path=None,
    ):
        region_model_path = Path(
            region_model_path
            or MODEL_PATHS["one_line_region_classifier"]
        )
        if not region_model_path.exists():
            raise FileNotFoundError(region_model_path)

        self.region_model = YOLO(str(region_model_path))
        self.body_recognizer = CRNNRecognizer(
            body_model_path
            or MODEL_PATHS["one_line_body_crnn"]
        )

    def _predict_region(self, crop):
        height, width = crop.shape[:2]
        rows = []

        for ratio in REGION_RATIOS:
            end_x = max(1, int(round(width * ratio)))
            region_crop = crop[:, :end_x]

            result = self.region_model.predict(
                source=region_crop,
                imgsz=TYPE_IMAGE_SIZE,
                verbose=False,
            )[0]

            class_id = int(result.probs.top1)
            rows.append({
                "ratio": ratio,
                "region": str(result.names[class_id]),
                "confidence": float(result.probs.top1conf),
            })

        votes = Counter(row["region"] for row in rows)
        max_vote = max(votes.values())
        tied = [
            name for name, count in votes.items()
            if count == max_vote
        ]

        confidence_sum = {
            name: sum(
                row["confidence"]
                for row in rows
                if row["region"] == name
            )
            for name in tied
        }

        selected = max(
            tied,
            key=lambda name: confidence_sum[name],
        )
        selected_rows = [
            row for row in rows
            if row["region"] == selected
        ]

        return {
            "region": selected,
            "confidence": sum(
                row["confidence"]
                for row in selected_rows
            ) / len(selected_rows),
            "votes": max_vote,
            "candidates": rows,
        }

    def _predict_body(self, crop):
        height, width = crop.shape[:2]
        candidates = []

        for ratio in BODY_START_RATIOS:
            start_x = min(
                width - 1,
                max(0, int(round(width * ratio))),
            )
            body_crop = crop[:, start_x:]
            result = self.body_recognizer.predict(body_crop)
            text = clean_text(result["text"])

            candidates.append({
                "start_ratio": ratio,
                "body": text,
                "raw": result["raw"],
                "confidence": result["confidence"],
                "grammar_valid": is_valid_truck_body(text),
            })

        valid = [
            candidate
            for candidate in candidates
            if candidate["grammar_valid"]
        ]
        pool = valid if valid else candidates

        selected = sorted(
            pool,
            key=lambda item: (
                item["grammar_valid"],
                item["confidence"],
            ),
            reverse=True,
        )[0]

        return {
            **selected,
            "candidates": candidates,
        }

    def predict(self, crop):
        region = self._predict_region(crop)
        body = self._predict_body(crop)

        plate_number = clean_text(
            region["region"] + body["body"]
        )

        return {
            "plate_number": plate_number,
            "ocr_raw": (
                f"{region['region']}|{body['raw']}"
            ),
            "ocr_confidence": float(
                (region["confidence"] + body["confidence"]) / 2.0
            ),
            "grammar_valid": is_valid_truck_full(plate_number),
            "details": {
                "region": region,
                "body": body,
            },
        }
