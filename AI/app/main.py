from fastapi import FastAPI

from app.controller.plate_controller import router as plate_router

app = FastAPI(
    title="Port Gate AI Plate Recognition API",
    version="1.0.0",
)

app.include_router(plate_router)


@app.get("/api/health")
def health_check():
    return {
        "status": "OK",
        "service": "plate-recognition",
    }
