import json
import sys
from pathlib import Path

from candidate_selector import choose_best_candidate
from normal_plate_ocr import NormalPlateOCR
from one_line_plate_ocr import OneLineTruckOCR
from plate_detector import PlateCandidateDetector
from plate_type_classifier import PlateTypeClassifier
from two_line_plate_ocr import TwoLineTruckOCR


class UnifiedPlatePipeline:
    def __init__(self):
        self.detector = PlateCandidateDetector()
        self.type_classifier = PlateTypeClassifier()

        self.ocr_handlers = {
            "normal": NormalPlateOCR(),
            "one_line": OneLineTruckOCR(),
            "two_line": TwoLineTruckOCR(),
        }

    def predict(self, image_path):
        image_path = Path(image_path)
        detected_candidates = self.detector.detect(
            image_path
        )

        recognized_candidates = []

        for candidate in detected_candidates:
            type_result = self.type_classifier.predict(
                candidate["crop"]
            )
            plate_type = type_result[
                "plate_type"
            ]

            ocr_result = self.ocr_handlers[
                plate_type
            ].predict(candidate["crop"])

            final_confidence = (
                candidate["detection_confidence"]
                * type_result["type_confidence"]
                * max(
                    ocr_result["ocr_confidence"],
                    0.0,
                )
            )

            recognized_candidates.append({
                "candidate_index": candidate[
                    "candidate_index"
                ],
                "plate_type": plate_type,
                "raw_type_class": type_result[
                    "raw_class_name"
                ],
                "plate_number": ocr_result[
                    "plate_number"
                ],
                "ocr_raw": ocr_result[
                    "ocr_raw"
                ],
                "grammar_valid": ocr_result[
                    "grammar_valid"
                ],
                "recognition_level": (
                    ocr_result.get(
                        "recognition_level",
                        "full"
                        if ocr_result[
                            "grammar_valid"
                        ]
                        else "none",
                    )
                ),
                "recognition_level_score": (
                    ocr_result.get(
                        "recognition_level_score",
                        2
                        if ocr_result[
                            "grammar_valid"
                        ]
                        else 0,
                    )
                ),
                "detection_confidence": candidate[
                    "detection_confidence"
                ],
                "type_confidence": type_result[
                    "type_confidence"
                ],
                "ocr_confidence": ocr_result[
                    "ocr_confidence"
                ],
                "final_confidence": (
                    final_confidence
                ),
                "center_score": candidate[
                    "center_score"
                ],
                "box": candidate["box"],
                "crop_path": candidate[
                    "crop_path"
                ],
                "details": ocr_result[
                    "details"
                ],
            })

        best = choose_best_candidate(
            recognized_candidates
        )

        if best is None:
            return {
                "detected": False,
                "plateNumber": "",
                "plateType": "",
                "needReview": True,
                "reviewReasons": [
                    "PLATE_NOT_DETECTED"
                ],
                "candidates": [],
            }

        review_reasons = []

        recognition_level = best.get(
            "recognition_level",
            "none",
        )

        if recognition_level == "body_only":
            review_reasons.append(
                "TWO_LINE_REGION_MODEL_MISSING_OR_FAILED"
            )
        elif not best["grammar_valid"]:
            review_reasons.append(
                "FORMAT_ERROR"
            )

        detail_status = (
            best.get("details", {})
            .get("status", "")
        )

        # success_full, success_body_only는 오류 상태가 아님.
        if (
            detail_status
            and not str(detail_status).startswith(
                "success"
            )
        ):
            review_reasons.append(
                str(detail_status)
            )

        details = best.get(
            "details",
            {},
        )

        if details.get(
            "low_layout_confidence",
            False,
        ):
            review_reasons.append(
                "LOW_TWO_LINE_LAYOUT_CONFIDENCE"
            )

        region_details = details.get(
            "region",
            {},
        )

        if region_details.get(
            "low_confidence",
            False,
        ):
            review_reasons.append(
                "LOW_TWO_LINE_REGION_CONFIDENCE"
            )

        # 순서를 유지하며 중복 제거
        review_reasons = list(dict.fromkeys(
            review_reasons
        ))

        return {
            "detected": True,
            "plateNumber": best[
                "plate_number"
            ],
            "plateType": best[
                "plate_type"
            ],
            "recognitionLevel": (
                recognition_level
            ),
            "confidence": best[
                "final_confidence"
            ],
            "detectionConfidence": best[
                "detection_confidence"
            ],
            "typeConfidence": best[
                "type_confidence"
            ],
            "ocrConfidence": best[
                "ocr_confidence"
            ],
            "centerScore": best[
                "center_score"
            ],
            "box": best["box"],
            "cropPath": best[
                "crop_path"
            ],
            "needReview": bool(
                review_reasons
            ),
            "reviewReasons": review_reasons,
            "candidates": recognized_candidates,
        }


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({
            "error": "IMAGE_PATH_REQUIRED"
        }, ensure_ascii=False))
        raise SystemExit(1)

    pipeline = UnifiedPlatePipeline()
    result = pipeline.predict(sys.argv[1])

    print(json.dumps(
        result,
        ensure_ascii=False,
        indent=2,
    ))
