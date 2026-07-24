import sys

from fastapi import UploadFile

from app.config.path_config import BASE_DIR, UPLOAD_DIR

SCRIPTS_DIR = BASE_DIR / "scripts"

if str(SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPTS_DIR))

from unified_plate_pipeline import UnifiedPlatePipeline


pipeline = UnifiedPlatePipeline()


def normalize_candidate(candidate):
    return {
        "plateNumber": candidate.get("plate_number", ""),
        "ocrRaw": candidate.get("ocr_raw", ""),
        "confidence": float(candidate.get("final_confidence", 0.0) or 0.0),
        "detectionConfidence": float(
            candidate.get("detection_confidence", 0.0) or 0.0
        ),
        "ocrConfidence": float(candidate.get("ocr_confidence", 0.0) or 0.0),
        "needReview": not candidate.get("grammar_valid", False),
        "reviewReasons": [],
        "cropPath": candidate.get("crop_path", ""),
        "ocrType": "unified",
    }


def normalize_result(result):
    """단일 모델 출력을 Spring FastApiPlateResponseDTO 형식으로 고정한다."""
    plate_number = result.get("plateNumber", "")

    return {
        "detected": bool(result.get("detected", False)),
        "plateNumber": plate_number,
        "ocrRaw": result.get("ocrRaw", plate_number),
        "confidence": float(result.get("confidence", 0.0) or 0.0),
        "detectionConfidence": float(
            result.get("detectionConfidence", 0.0) or 0.0
        ),
        "ocrConfidence": float(result.get("ocrConfidence", 0.0) or 0.0),
        "needReview": bool(result.get("needReview", False)),
        "reviewReasons": result.get("reviewReasons", []) or [],
        "cropPath": result.get("cropPath", ""),
        "candidates": [
            normalize_candidate(candidate)
            for candidate in result.get("candidates", [])
        ],
        "ocrType": "unified",
    }


async def predict_upload_file(file: UploadFile):
    file_name = file.filename or "upload.jpg"
    save_path = UPLOAD_DIR / file_name

    contents = await file.read()

    if not contents:
        return normalize_result({
            "detected": False,
            "needReview": True,
            "reviewReasons": ["EMPTY_FILE"],
        })

    with open(save_path, "wb") as buffer:
        buffer.write(contents)

    try:
        result = pipeline.predict(save_path)
    except Exception:
        return normalize_result({
            "detected": False,
<<<<<<< HEAD
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
        if (ocr_type or "").lower() != "crnn":
            try:
                return predict_plate(save_path, "crnn")
            except Exception:
                pass

        return {
            "detected": False,
            "plateNumber": "",
            "ocrRaw": "",
            "confidence": 0,
            "detectionConfidence": 0,
            "ocrConfidence": 0,
=======
>>>>>>> 7fbd6506b96f09e1a4feffc970b50aafa75abb64
            "needReview": True,
            "reviewReasons": ["AI_PROCESS_FAILED"],
        })

    return normalize_result(result)
