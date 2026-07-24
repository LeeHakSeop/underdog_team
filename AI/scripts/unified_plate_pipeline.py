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
    """
    번호판 후보별 처리 순서

    1. 일반 번호판 CRNN을 먼저 실행
    2. 일반 번호판 문법에 맞으면 normal로 확정
    3. 일반 문법에 맞지 않을 때만
       one_line / two_line 분류 모델을 실행
    4. 분류된 트럭 전용 OCR 실행

    plate_type_classifier.pt는
    one_line / two_line 두 클래스만 학습된 모델이므로
    일반 번호판 여부를 판단하는 데 사용하지 않는다.
    """

    def __init__(self):
        self.detector = PlateCandidateDetector()

        # 이 모델은 일반/1줄/2줄 3분류가 아니라
        # 1줄 트럭/2줄 트럭 2분류 모델이다.
        self.truck_type_classifier = PlateTypeClassifier()

        self.normal_ocr = NormalPlateOCR()
        self.truck_ocr_handlers = {
            "one_line": OneLineTruckOCR(),
            "two_line": TwoLineTruckOCR(),
        }

    @staticmethod
    def _recognition_level(ocr_result):
        return ocr_result.get(
            "recognition_level",
            "full" if ocr_result["grammar_valid"] else "none",
        )

    @staticmethod
    def _recognition_level_score(ocr_result):
        return ocr_result.get(
            "recognition_level_score",
            2 if ocr_result["grammar_valid"] else 0,
        )

    def _make_candidate_result(
        self,
        candidate,
        plate_type,
        raw_type_class,
        type_confidence,
        ocr_result,
        route,
    ):
        final_confidence = (
            candidate["detection_confidence"]
            * max(float(type_confidence), 0.0)
            * max(float(ocr_result["ocr_confidence"]), 0.0)
        )

        details = dict(ocr_result.get("details", {}))
        details["routing"] = route

        return {
            "candidate_index": candidate["candidate_index"],
            "plate_type": plate_type,
            "raw_type_class": raw_type_class,
            "plate_number": ocr_result["plate_number"],
            "ocr_raw": ocr_result["ocr_raw"],
            "grammar_valid": ocr_result["grammar_valid"],
            "recognition_level": self._recognition_level(ocr_result),
            "recognition_level_score": self._recognition_level_score(
                ocr_result
            ),
            "detection_confidence": candidate[
                "detection_confidence"
            ],
            "type_confidence": float(type_confidence),
            "ocr_confidence": ocr_result["ocr_confidence"],
            "final_confidence": final_confidence,
            "center_score": candidate["center_score"],
            "box": candidate["box"],
            "crop_path": candidate["crop_path"],
            "details": details,
        }

    def _recognize_candidate(self, candidate):
        crop = candidate["crop"]

        # 1) 일반 번호판을 먼저 시도한다.
        normal_result = self.normal_ocr.predict(crop)

        # print(
        #     "[NORMAL FIRST]",
        #     f"candidate={candidate['candidate_index']}",
        #     f"text={normal_result['plate_number']}",
        #     f"valid={normal_result['grammar_valid']}",
        #     f"confidence={normal_result['ocr_confidence']:.4f}",
        # )

        if normal_result["grammar_valid"]:
            return self._make_candidate_result(
                candidate=candidate,
                plate_type="normal",
                raw_type_class="normal_by_grammar",
                type_confidence=1.0,
                ocr_result=normal_result,
                route="normal_grammar_first",
            )

        # 2) 일반 번호판 문법 실패 시에만
        #    1줄/2줄 트럭 형태를 분류한다.
        type_result = self.truck_type_classifier.predict(crop)
        plate_type = type_result["plate_type"]

        if plate_type not in self.truck_ocr_handlers:
            raise ValueError(
                "트럭 형태 분류 결과가 one_line/two_line이 아닙니다: "
                f"{plate_type}"
            )

        truck_result = self.truck_ocr_handlers[
            plate_type
        ].predict(crop)

        return self._make_candidate_result(
            candidate=candidate,
            plate_type=plate_type,
            raw_type_class=type_result["raw_class_name"],
            type_confidence=type_result["type_confidence"],
            ocr_result=truck_result,
            route="truck_classifier_after_normal_failure",
        )

    def predict(self, image_path):
        image_path = Path(image_path)
        detected_candidates = self.detector.detect(image_path)

        recognized_candidates = []

        for candidate in detected_candidates:
            try:
                recognized = self._recognize_candidate(candidate)
                recognized_candidates.append(recognized)
            except Exception as error:
                print(
                    "[CANDIDATE ERROR]",
                    f"candidate={candidate.get('candidate_index')}",
                    repr(error),
                )

                recognized_candidates.append({
                    "candidate_index": candidate["candidate_index"],
                    "plate_type": "",
                    "raw_type_class": "",
                    "plate_number": "",
                    "ocr_raw": "",
                    "grammar_valid": False,
                    "recognition_level": "none",
                    "recognition_level_score": 0,
                    "detection_confidence": candidate[
                        "detection_confidence"
                    ],
                    "type_confidence": 0.0,
                    "ocr_confidence": 0.0,
                    "final_confidence": 0.0,
                    "center_score": candidate["center_score"],
                    "box": candidate["box"],
                    "crop_path": candidate["crop_path"],
                    "details": {
                        "status": "candidate_processing_error",
                        "error": str(error),
                    },
                })

        best = choose_best_candidate(recognized_candidates)

        if best is None:
            return {
                "detected": False,
                "plateNumber": "",
                "plateType": "",
                "needReview": True,
                "reviewReasons": ["PLATE_NOT_DETECTED"],
                "candidates": [],
            }

        review_reasons = []
        recognition_level = best.get("recognition_level", "none")

        if recognition_level == "body_only":
            review_reasons.append(
                "TWO_LINE_REGION_MODEL_MISSING_OR_FAILED"
            )
        elif not best["grammar_valid"]:
            review_reasons.append("FORMAT_ERROR")

        detail_status = best.get("details", {}).get("status", "")

        if (
            detail_status
            and not str(detail_status).startswith("success")
        ):
            review_reasons.append(str(detail_status))

        details = best.get("details", {})

        if details.get("low_layout_confidence", False):
            review_reasons.append(
                "LOW_TWO_LINE_LAYOUT_CONFIDENCE"
            )

        region_details = details.get("region", {})

        if region_details.get("low_confidence", False):
            review_reasons.append(
                "LOW_TWO_LINE_REGION_CONFIDENCE"
            )

        review_reasons = list(dict.fromkeys(review_reasons))

        return {
            "detected": True,
            "plateNumber": best["plate_number"],
            "plateType": best["plate_type"],
            "recognitionLevel": recognition_level,
            "confidence": best["final_confidence"],
            "detectionConfidence": best[
                "detection_confidence"
            ],
            "typeConfidence": best["type_confidence"],
            "ocrConfidence": best["ocr_confidence"],
            "centerScore": best["center_score"],
            "box": best["box"],
            "cropPath": best["crop_path"],
            "needReview": bool(review_reasons),
            "reviewReasons": review_reasons,
            "candidates": recognized_candidates,
        }


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps(
            {"error": "IMAGE_PATH_REQUIRED"},
            ensure_ascii=False,
        ))
        raise SystemExit(1)

    pipeline = UnifiedPlatePipeline()
    result = pipeline.predict(sys.argv[1])

    print(json.dumps(
        result,
        ensure_ascii=False,
        indent=2,
    ))
