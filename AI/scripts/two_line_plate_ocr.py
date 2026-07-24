from pathlib import Path

import cv2
import numpy as np
from ultralytics import YOLO

from config import MODEL_PATHS
from crnn_core import CRNNRecognizer
from plate_grammar import (
    REGION_NAMES,
    clean_text,
    is_valid_region,
    is_valid_truck_body,
    is_valid_truck_full,
)


class TwoLineTruckOCR:
    """
    2줄 번호판 운영 추론.

    021·024의 처리 규칙:
        1. up_reg / up_num 검출
        2. class별 최고 confidence bbox 선택
        3. bbox padding
        4. 두 bbox의 아래쪽 끝 아래를 하단 행으로 crop
        5. up_num + 하단 행을 가로 결합
        6. 023 전용 CRNN으로 본문 인식

    지역명:
        models/two_line_region_classifier.pt로
        검출된 up_reg crop의 지역명을 분류합니다.

        지역명과 본문을 결합하여
        예: 경북 + 80바4630 -> 경북80바4630
        형태의 전체 번호판을 반환합니다.

        운영 프로젝트에서는 지역명 모델을 필수로 사용합니다.
    """

    IMGSZ = 640
    YOLO_CONF = 0.25
    YOLO_IOU = 0.50

    BOX_PAD_X = 0.06
    BOX_PAD_Y = 0.10
    BOTTOM_GAP_RATIO = 0.015
    JOIN_GAP = 8

    LOW_LAYOUT_CONFIDENCE = 0.50
    REGION_IMGSZ = 224
    LOW_REGION_CONFIDENCE = 0.50

    def __init__(
        self,
        layout_model_path=None,
        body_model_path=None,
        region_model_path=None,
    ):
        self.layout_model_path = Path(
            layout_model_path
            or MODEL_PATHS["two_line_layout_detector"]
        )
        self.body_model_path = Path(
            body_model_path
            or MODEL_PATHS["two_line_body_crnn"]
        )

        self.region_model_path = Path(
            region_model_path
            or MODEL_PATHS["two_line_region_classifier"]
        )

        if not self.layout_model_path.exists():
            raise FileNotFoundError(
                f"2줄 내부 영역 YOLO 모델이 없습니다: "
                f"{self.layout_model_path}"
            )

        if not self.body_model_path.exists():
            raise FileNotFoundError(
                f"2줄 본문 CRNN 모델이 없습니다: "
                f"{self.body_model_path}"
            )

        self.layout_model = YOLO(
            str(self.layout_model_path)
        )
        self.body_recognizer = CRNNRecognizer(
            self.body_model_path
        )

        if not self.region_model_path.exists():
            raise FileNotFoundError(
                f"2줄 지역명 분류 모델이 없습니다: "
                f"{self.region_model_path}"
            )

        self.region_model = YOLO(
            str(self.region_model_path)
        )

        self.class_ids = self._resolve_layout_class_ids()

    def _resolve_layout_class_ids(self):
        names = self.layout_model.names

        if isinstance(names, list):
            names = {
                index: name
                for index, name in enumerate(names)
            }

        normalized = {
            int(class_id): str(name).strip().lower()
            for class_id, name in names.items()
        }

        up_reg_id = next(
            (
                class_id
                for class_id, name in normalized.items()
                if name in {
                    "up_reg", "up-region", "upregion",
                    "region", "지역명",
                }
            ),
            None,
        )

        up_num_id = next(
            (
                class_id
                for class_id, name in normalized.items()
                if name in {
                    "up_num", "up-number", "upnumber",
                    "number", "상단숫자",
                }
            ),
            None,
        )

        # 013~025 데이터셋의 확정 class 순서
        if up_reg_id is None:
            up_reg_id = 0
        if up_num_id is None:
            up_num_id = 1

        return {
            "up_reg": int(up_reg_id),
            "up_num": int(up_num_id),
        }

    @staticmethod
    def _padded_box(
        box,
        image_width,
        image_height,
        pad_x,
        pad_y,
    ):
        x1, y1, x2, y2 = map(float, box)

        box_width = x2 - x1
        box_height = y2 - y1

        x1 -= box_width * pad_x
        x2 += box_width * pad_x
        y1 -= box_height * pad_y
        y2 += box_height * pad_y

        return (
            max(0, int(round(x1))),
            max(0, int(round(y1))),
            min(image_width, int(round(x2))),
            min(image_height, int(round(y2))),
        )

    @staticmethod
    def _crop_box(image, box):
        x1, y1, x2, y2 = box

        if x2 <= x1 or y2 <= y1:
            return None

        crop = image[y1:y2, x1:x2]

        if crop.size == 0:
            return None

        return crop.copy()

    @staticmethod
    def _normalize_height(image, target_height):
        height, width = image.shape[:2]

        if height <= 0 or width <= 0:
            return None

        new_width = max(
            1,
            int(round(
                width * target_height / height
            )),
        )

        return cv2.resize(
            image,
            (new_width, target_height),
            interpolation=cv2.INTER_CUBIC,
        )

    @classmethod
    def _join_horizontal(
        cls,
        left,
        right,
        gap,
    ):
        target_height = max(
            left.shape[0],
            right.shape[0],
        )

        left_resized = cls._normalize_height(
            left,
            target_height,
        )
        right_resized = cls._normalize_height(
            right,
            target_height,
        )

        if (
            left_resized is None
            or right_resized is None
        ):
            return None

        spacer = np.full(
            (target_height, gap, 3),
            255,
            dtype=np.uint8,
        )

        return np.concatenate(
            [
                left_resized,
                spacer,
                right_resized,
            ],
            axis=1,
        )

    def _detect_layout(self, crop):
        result = self.layout_model.predict(
            source=crop,
            imgsz=self.IMGSZ,
            conf=self.YOLO_CONF,
            iou=self.YOLO_IOU,
            verbose=False,
        )[0]

        candidates = {
            "up_reg": [],
            "up_num": [],
        }

        id_to_key = {
            self.class_ids["up_reg"]: "up_reg",
            self.class_ids["up_num"]: "up_num",
        }

        if (
            result.boxes is not None
            and len(result.boxes) > 0
        ):
            boxes = (
                result.boxes.xyxy
                .detach()
                .cpu()
                .numpy()
            )
            classes = (
                result.boxes.cls
                .detach()
                .cpu()
                .numpy()
                .astype(int)
            )
            confidences = (
                result.boxes.conf
                .detach()
                .cpu()
                .numpy()
            )

            for bbox, class_id, confidence in zip(
                boxes,
                classes,
                confidences,
            ):
                key = id_to_key.get(int(class_id))
                if key is None:
                    continue

                candidates[key].append({
                    "bbox": [
                        float(value)
                        for value in bbox.tolist()
                    ],
                    "confidence": float(confidence),
                })

        selected = {
            key: (
                max(
                    values,
                    key=lambda item: item["confidence"],
                )
                if values
                else None
            )
            for key, values in candidates.items()
        }

        if selected["up_reg"] and selected["up_num"]:
            status = "success"
        elif (
            selected["up_reg"] is None
            and selected["up_num"] is None
        ):
            status = "missing_both"
        elif selected["up_reg"] is None:
            status = "missing_up_reg"
        else:
            status = "missing_up_num"

        return {
            "status": status,
            "up_reg": selected["up_reg"],
            "up_num": selected["up_num"],
            "candidate_counts": {
                key: len(value)
                for key, value in candidates.items()
            },
        }

    def _reconstruct(self, crop, layout):
        if layout["status"] != "success":
            return {
                "status": layout["status"],
            }

        image_height, image_width = crop.shape[:2]

        up_reg_box = self._padded_box(
            layout["up_reg"]["bbox"],
            image_width,
            image_height,
            self.BOX_PAD_X,
            self.BOX_PAD_Y,
        )

        up_num_box = self._padded_box(
            layout["up_num"]["bbox"],
            image_width,
            image_height,
            self.BOX_PAD_X,
            self.BOX_PAD_Y,
        )

        up_reg_crop = self._crop_box(
            crop,
            up_reg_box,
        )
        up_num_crop = self._crop_box(
            crop,
            up_num_box,
        )

        bottom_start = int(round(
            max(
                up_reg_box[3],
                up_num_box[3],
            )
            + image_height * self.BOTTOM_GAP_RATIO
        ))

        bottom_start = max(
            0,
            min(image_height - 1, bottom_start),
        )

        bottom_box = (
            0,
            bottom_start,
            image_width,
            image_height,
        )

        bottom_crop = self._crop_box(
            crop,
            bottom_box,
        )

        if (
            up_reg_crop is None
            or up_num_crop is None
            or bottom_crop is None
        ):
            return {
                "status": "crop_failed",
                "up_reg_box": list(up_reg_box),
                "up_num_box": list(up_num_box),
                "bottom_box": list(bottom_box),
            }

        reconstructed_body = self._join_horizontal(
            up_num_crop,
            bottom_crop,
            gap=self.JOIN_GAP,
        )

        if reconstructed_body is None:
            return {
                "status": "reconstruction_failed",
                "up_reg_box": list(up_reg_box),
                "up_num_box": list(up_num_box),
                "bottom_box": list(bottom_box),
            }

        return {
            "status": "success",
            "up_reg_box": list(up_reg_box),
            "up_num_box": list(up_num_box),
            "bottom_box": list(bottom_box),
            "up_reg_crop": up_reg_crop,
            "reconstructed_body": reconstructed_body,
        }

    def _predict_region(self, up_reg_crop):
        result = self.region_model.predict(
            source=up_reg_crop,
            imgsz=self.REGION_IMGSZ,
            verbose=False,
        )[0]

        if result.probs is None:
            return {
                "status": "invalid_model_task",
                "region": "",
                "confidence": 0.0,
                "second_confidence": 0.0,
                "margin": 0.0,
            }

        probabilities = (
            result.probs.data
            .detach()
            .cpu()
            .numpy()
        )

        ranking = sorted(
            [
                (
                    str(result.names[index]),
                    float(probability),
                )
                for index, probability
                in enumerate(probabilities)
            ],
            key=lambda item: item[1],
            reverse=True,
        )

        region = clean_text(
            ranking[0][0]
        )
        confidence = ranking[0][1]
        second_confidence = (
            ranking[1][1]
            if len(ranking) > 1
            else 0.0
        )

        valid = (
            region in REGION_NAMES
            and is_valid_region(region)
        )

        return {
            "status": (
                "success"
                if valid
                else "unknown_region_class"
            ),
            "region": region if valid else "",
            "confidence": float(confidence),
            "second_confidence": float(
                second_confidence
            ),
            "margin": float(
                confidence - second_confidence
            ),
            "low_confidence": bool(
                confidence
                < self.LOW_REGION_CONFIDENCE
            ),
        }

    def predict(
        self,
        crop,
        region_text="",
        region_confidence=0.0,
    ):
        if crop is None or crop.size == 0:
            return {
                "plate_number": "",
                "ocr_raw": "",
                "ocr_confidence": 0.0,
                "grammar_valid": False,
                "recognition_level": "none",
                "recognition_level_score": 0,
                "details": {
                    "status": "EMPTY_TWO_LINE_CROP",
                },
            }

        layout = self._detect_layout(crop)
        reconstruction = self._reconstruct(
            crop,
            layout,
        )

        if reconstruction["status"] != "success":
            return {
                "plate_number": "",
                "ocr_raw": "",
                "ocr_confidence": 0.0,
                "grammar_valid": False,
                "recognition_level": "none",
                "recognition_level_score": 0,
                "details": {
                    "status": reconstruction["status"],
                    "layout": layout,
                    "boxes": {
                        "up_reg": reconstruction.get(
                            "up_reg_box"
                        ),
                        "up_num": reconstruction.get(
                            "up_num_box"
                        ),
                        "bottom": reconstruction.get(
                            "bottom_box"
                        ),
                    },
                },
            }

        # OpenCV BGR 배열을 PIL 기반 CRNN에 넘기기 전에 RGB로 변환.
        reconstructed_rgb = cv2.cvtColor(
            reconstruction["reconstructed_body"],
            cv2.COLOR_BGR2RGB,
        )

        body_result = self.body_recognizer.predict(
            reconstructed_rgb
        )

        body_text = clean_text(
            body_result["text"]
        )
        body_valid = is_valid_truck_body(
            body_text
        )

        supplied_region = clean_text(
            region_text
        )

        if supplied_region and is_valid_region(
            supplied_region
        ):
            region_result = {
                "status": "external",
                "region": supplied_region,
                "confidence": float(
                    region_confidence
                ),
                "second_confidence": 0.0,
                "margin": float(
                    region_confidence
                ),
                "low_confidence": False,
            }
        else:
            region_result = self._predict_region(
                reconstruction["up_reg_crop"]
            )

        predicted_region = region_result.get(
            "region",
            "",
        )

        if predicted_region:
            plate_number = clean_text(
                predicted_region + body_text
            )
            full_valid = is_valid_truck_full(
                plate_number
            )
        else:
            plate_number = body_text
            full_valid = False

        if full_valid:
            recognition_level = "full"
            recognition_level_score = 2
            grammar_valid = True
        elif body_valid:
            recognition_level = "body_only"
            recognition_level_score = 1
            grammar_valid = True
        else:
            recognition_level = "none"
            recognition_level_score = 0
            grammar_valid = False

        up_reg_confidence = float(
            layout["up_reg"]["confidence"]
        )
        up_num_confidence = float(
            layout["up_num"]["confidence"]
        )

        layout_confidence = (
            up_reg_confidence
            + up_num_confidence
        ) / 2.0

        confidence_values = [
            layout_confidence,
            float(body_result["confidence"]),
        ]

        if predicted_region:
            confidence_values.append(
                float(region_result["confidence"])
            )

        ocr_confidence = (
            sum(confidence_values)
            / len(confidence_values)
        )

        low_layout_confidence = (
            up_reg_confidence
            < self.LOW_LAYOUT_CONFIDENCE
            or up_num_confidence
            < self.LOW_LAYOUT_CONFIDENCE
        )

        return {
            "plate_number": plate_number,
            "ocr_raw": body_result["raw"],
            "ocr_confidence": float(
                ocr_confidence
            ),
            "grammar_valid": bool(
                grammar_valid
            ),
            "recognition_level": recognition_level,
            "recognition_level_score": (
                recognition_level_score
            ),
            "details": {
                "status": (
                    "success_full"
                    if full_valid
                    else (
                        "success_body_only"
                        if body_valid
                        else "body_format_error"
                    )
                ),
                "region": region_result,
                "body_text": body_text,
                "body_grammar_valid": bool(
                    body_valid
                ),
                "full_grammar_valid": bool(
                    full_valid
                ),
                "low_layout_confidence": bool(
                    low_layout_confidence
                ),
                "up_reg_confidence": (
                    up_reg_confidence
                ),
                "up_num_confidence": (
                    up_num_confidence
                ),
                "layout": layout,
                "boxes": {
                    "up_reg": reconstruction[
                        "up_reg_box"
                    ],
                    "up_num": reconstruction[
                        "up_num_box"
                    ],
                    "bottom": reconstruction[
                        "bottom_box"
                    ],
                },
                # numpy 이미지 배열은 JSON 응답에 넣지 않습니다.
                "debug_image_shapes": {
                    "up_reg": list(
                        reconstruction[
                            "up_reg_crop"
                        ].shape
                    ),
                    "reconstructed_body": list(
                        reconstruction[
                            "reconstructed_body"
                        ].shape
                    ),
                },
            },
        }
