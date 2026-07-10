import sys
from pathlib import Path

from PIL import Image
import torch

from ocr_postprocess import (
    clean_plate_text,
    normalize_plate_pattern,
    correct_plate_korean,
)


BASE_DIR = Path(__file__).resolve().parents[1]
PROJECT_DIR = BASE_DIR.parent
CRNN_PACKAGE_DIR = PROJECT_DIR / "AI_v2_crnn_package"
CRNN_SCRIPT_DIR = CRNN_PACKAGE_DIR / "scripts"
CRNN_MODEL_PATH = CRNN_PACKAGE_DIR / "model" / "best.pt"

sys.path.append(str(CRNN_SCRIPT_DIR))

from train_crnn_ocr import CRNN, greedy_decode


device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
checkpoint = torch.load(CRNN_MODEL_PATH, map_location=device)
charset = checkpoint["charset"]
width = checkpoint.get("width", 224)
height = checkpoint.get("height", 32)

idx_to_char = {}
for index, char in enumerate(charset):
    idx_to_char[str(index + 1)] = char
idx_to_char["0"] = ""

model = CRNN(num_classes=len(charset) + 1).to(device)
model.load_state_dict(checkpoint["model"])
model.eval()


def image_to_tensor(crop_path):
    image = Image.open(crop_path).convert("L")
    image = image.resize((width, height), Image.Resampling.BICUBIC)

    tensor = torch.tensor(list(image.getdata()), dtype=torch.float32)
    tensor = tensor.view(1, height, width) / 255.0
    tensor = (tensor - 0.5) / 0.5
    tensor = tensor.unsqueeze(0)

    return tensor.to(device)


def predict_crnn_ocr(crop_path):
    image_tensor = image_to_tensor(crop_path)

    with torch.no_grad():
        logits = model(image_tensor)
        predictions = greedy_decode(logits, idx_to_char)

    ocr_raw = predictions[0] if predictions else ""

    plate_number = clean_plate_text(ocr_raw)
    plate_number = normalize_plate_pattern(plate_number)
    plate_number = correct_plate_korean(plate_number)

    return ocr_raw, plate_number, 1.0
