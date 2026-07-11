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

    if not contents:
        return {
            "detected": False,
            "plateNumber": "",
            "ocrRaw": "",
            "confidence": 0,
            "detectionConfidence": 0,
            "ocrConfidence": 0,
            "needReview": True,
            "reviewReasons": ["EMPTY_FILE"],
            "error": {
                "code": "EMPTY_FILE",
                "message": "업로드된 이미지 파일이 비어 있습니다.",
            },
            "candidates": [],
        }

    with open(save_path, "wb") as buffer:
        buffer.write(contents)

    try:
        result = predict_plate(save_path, ocr_type)
    except FileNotFoundError:
        return {
            "detected": False,
            "plateNumber": "",
            "ocrRaw": "",
            "confidence": 0,
            "detectionConfidence": 0,
            "ocrConfidence": 0,
            "needReview": True,
            "reviewReasons": ["IMAGE_FILE_NOT_FOUND"],
            "error": {
                "code": "IMAGE_FILE_NOT_FOUND",
                "message": "저장된 이미지 파일을 찾을 수 없습니다.",
            },
            "candidates": [],
        }
    except Exception as error:
        return {
            "detected": False,
            "plateNumber": "",
            "ocrRaw": "",
            "confidence": 0,
            "detectionConfidence": 0,
            "ocrConfidence": 0,
            "needReview": True,
            "reviewReasons": ["AI_PROCESS_FAILED"],
            "error": {
                "code": "AI_PROCESS_FAILED",
                "message": str(error) or "번호판 인식 처리 중 오류가 발생했습니다.",
            },
            "candidates": [],
        }

    return result
