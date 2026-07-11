from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from app.service.plate_service import predict_upload_file

router = APIRouter()


@router.post("/api/plate-recognition")
async def recognize_plate(
    file: UploadFile = File(...),
    ocrType: str = Form("paddle"),
):
    if not file:
        raise HTTPException(
            status_code=400,
            detail={
                "code": "FILE_REQUIRED",
                "message": "번호판 인식에 사용할 이미지 파일이 필요합니다.",
            },
        )

    result = await predict_upload_file(file, ocrType)
    return result
