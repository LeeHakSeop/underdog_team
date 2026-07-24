from pathlib import Path

from ultralytics import YOLO

from config import MODEL_PATHS, TYPE_CLASS_ALIASES, TYPE_IMAGE_SIZE


class PlateTypeClassifier:
    def __init__(self, model_path=None):
        self.model_path = Path(
            model_path or MODEL_PATHS["plate_type_classifier"]
        )
        if not self.model_path.exists():
            raise FileNotFoundError(self.model_path)
        self.model = YOLO(str(self.model_path))

    @staticmethod
    def _normalize_class_name(raw_name):
        value = str(raw_name).strip().lower()

        for canonical, aliases in TYPE_CLASS_ALIASES.items():
            normalized_aliases = {
                str(alias).strip().lower()
                for alias in aliases
            }
            if value in normalized_aliases:
                return canonical

        raise ValueError(
            f"알 수 없는 번호판 형태 class: {raw_name}. "
            f"scripts/config.py의 TYPE_CLASS_ALIASES를 수정하세요."
        )

    def predict(self, crop):
        result = self.model.predict(
            source=crop,
            imgsz=TYPE_IMAGE_SIZE,
            verbose=False,
        )[0]

        top1 = int(result.probs.top1)
        raw_name = result.names[top1]
        confidence = float(result.probs.top1conf)

        return {
            "plate_type": self._normalize_class_name(raw_name),
            "raw_class_name": str(raw_name),
            "type_confidence": confidence,
        }