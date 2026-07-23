from fastapi import APIRouter, File, UploadFile

from app.service.plate_service import predict_upload_file

router = APIRouter()


@router.post("/api/plate-recognition")
async def recognize_plate(
    file: UploadFile = File(...),
):
    result = await predict_upload_file(file)
    return result
