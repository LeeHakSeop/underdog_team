from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
MODEL_DIR = BASE_DIR / "models"
RESULT_DIR = BASE_DIR / "result"
CANDIDATE_DIR = RESULT_DIR / "plate_candidates"
FINAL_DIR = RESULT_DIR / "final"

for folder in (CANDIDATE_DIR, FINAL_DIR):
    folder.mkdir(parents=True, exist_ok=True)

MODEL_PATHS = {
    "plate_detector": MODEL_DIR / "team_yolo_best.pt",
    "plate_type_classifier": MODEL_DIR / "line_rec_best.pt",
    "normal_crnn": MODEL_DIR / "crnn_best.pt",
    "one_line_region_classifier": MODEL_DIR / "one_line_region_classifier.pt",
    "one_line_body_crnn": MODEL_DIR / "one_line_body_crnn.pt",
    "two_line_layout_detector": MODEL_DIR / "two_line_layout_detector.pt",
    "two_line_body_crnn": MODEL_DIR / "two_line_body_crnn.pt",
    "two_line_region_classifier": MODEL_DIR / "two_line_region_classifier.pt",
    # 기존 코드 호환용 별칭
    "team_yolo_best": MODEL_DIR / "team_yolo_best.pt",
    "line_rec_best": MODEL_DIR / "line_rec_best.pt",
    "crnn_best": MODEL_DIR / "crnn_best.pt",
}

DETECTION_CONFIDENCE = 0.25
TYPE_IMAGE_SIZE = 224

CENTER_TARGET_X_RATIO = 0.50
CENTER_TARGET_Y_RATIO = 0.55
CENTER_Y_WEIGHT = 0.40

REGION_RATIOS = [0.16, 0.18, 0.20, 0.22, 0.24]
BODY_START_RATIOS = [
    0.12, 0.13, 0.14, 0.15, 0.16,
    0.17, 0.18, 0.19, 0.20,
]

TYPE_CLASS_ALIASES = {
    "normal": {
        "normal", "general", "일반", "0",
    },
    "one_line": {
        "one_line", "one-line", "oneline", "1line",
        "truck_one_line", "truck_vertical",
        "1줄", "1줄트럭", "1",
    },
    "two_line": {
        "two_line", "two-line", "twoline", "2line",
        "truck_two_line",
        "2줄", "2줄트럭", "2",
    },
}
