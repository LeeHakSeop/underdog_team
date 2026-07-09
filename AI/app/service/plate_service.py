import sys
from pathlib import Path

from fastapi import UploadFile

from app.config.path_config import BASE_DIR, UPLOAD_DIR

SCRIPTS_DIR = BASE_DIR / "scripts"
sys.path.append(str(SCRIPTS_DIR))

from predict_plate import predict_plate


async def predict_upload_file(file: UploadFile, ocr_type: str = "paddle"):
    file_name = file.filename or "upload.jpg"
    save_path = UPLOAD_DIR / file_name

    contents = await file.read()

    with open(save_path, "wb") as buffer:
        buffer.write(contents)

    result = predict_plate(save_path, ocr_type)

    return result
