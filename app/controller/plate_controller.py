from fastapi import APIRouter, File, Form, UploadFile

from app.service.plate_service import predict_upload_file

router = APIRouter()


@router.post("/api/plate-recognition")
async def recognize_plate(
    file: UploadFile = File(...),
    ocrType: str = Form("paddle"),
):
    result = await predict_upload_file(file, ocrType)
    return result
