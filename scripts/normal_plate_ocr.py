from config import MODEL_PATHS
from crnn_core import CRNNRecognizer
from plate_grammar import clean_text, is_valid_normal


class NormalPlateOCR:
    def __init__(self, model_path=None):
        self.recognizer = CRNNRecognizer(
            model_path or MODEL_PATHS["normal_crnn"]
        )

    def predict(self, crop):
        result = self.recognizer.predict(crop)
        text = clean_text(result["text"])

        return {
            "plate_number": text,
            "ocr_raw": result["raw"],
            "ocr_confidence": result["confidence"],
            "grammar_valid": is_valid_normal(text),
            "details": {},
        }
