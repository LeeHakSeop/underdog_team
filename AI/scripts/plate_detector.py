from pathlib import Path

from ultralytics import YOLO

from config import (
    MODEL_PATHS,
    DETECTION_CONFIDENCE,
    CANDIDATE_DIR,
)
from image_utils import imread_unicode, imwrite_unicode


class PlateCandidateDetector:
    def __init__(self, model_path=None):
        self.model_path = Path(
            model_path or MODEL_PATHS["plate_detector"]
        )
        if not self.model_path.exists():
            raise FileNotFoundError(self.model_path)
        self.model = YOLO(str(self.model_path))

    @staticmethod
    def _center_score(box, image_width, image_height):
        from config import (
            CENTER_TARGET_X_RATIO,
            CENTER_TARGET_Y_RATIO,
            CENTER_Y_WEIGHT,
        )

        x1, y1, x2, y2 = box
        box_cx = (x1 + x2) / 2.0
        box_cy = (y1 + y2) / 2.0

        target_x = image_width * CENTER_TARGET_X_RATIO
        target_y = image_height * CENTER_TARGET_Y_RATIO

        dx = abs(box_cx - target_x) / max(image_width / 2.0, 1.0)
        dy = abs(box_cy - target_y) / max(image_height / 2.0, 1.0)

        distance = dx + dy * CENTER_Y_WEIGHT
        return float(max(0.0, 1.0 - min(1.0, distance)))

    def detect(self, image_path):
        image_path = Path(image_path)
        image = imread_unicode(image_path)
        if image is None:
            raise RuntimeError(f"이미지 읽기 실패: {image_path}")

        image_height, image_width = image.shape[:2]
        results = self.model.predict(
            source=str(image_path),
            conf=DETECTION_CONFIDENCE,
            verbose=False,
        )

        candidates = []
        global_index = 0

        for result in results:
            for box in result.boxes:
                x1, y1, x2, y2 = (
                    box.xyxy[0].cpu().numpy().astype(int).tolist()
                )
                confidence = float(box.conf[0].cpu().numpy())

                x1 = max(0, min(x1, image_width - 1))
                x2 = max(1, min(x2, image_width))
                y1 = max(0, min(y1, image_height - 1))
                y2 = max(1, min(y2, image_height))

                crop = image[y1:y2, x1:x2]
                if crop.size == 0:
                    continue

                crop_path = (
                    CANDIDATE_DIR
                    / f"{image_path.stem}__candidate_{global_index:02d}.jpg"
                )
                imwrite_unicode(crop_path, crop)

                candidates.append({
                    "candidate_index": global_index,
                    "crop": crop,
                    "crop_path": str(crop_path),
                    "box": [x1, y1, x2, y2],
                    "detection_confidence": confidence,
                    "center_score": self._center_score(
                        [x1, y1, x2, y2],
                        image_width,
                        image_height,
                    ),
                })
                global_index += 1

        return candidates
