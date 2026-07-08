# AI v1 Package

## 구성

```text
AI_v1_package/
├─ models/
│  └─ team_yolo_best.pt
├─ scripts/
│  ├─ predict_plate.py
│  └─ ocr_postprocess.py
├─ samples/
│  ├─ 01도4663.jpeg
│  ├─ 01도4684.jpeg
│  └─ 01도5115.jpeg
├─ result/
│  └─ api_crop/
└─ dependencies.txt
```

## 설치

```powershell
conda activate paddle310
pip install -r dependencies.txt
```

## 실행

패키지 루트 기준으로 실행한다.

```powershell
cd AI_v1_package
python scripts/predict_plate.py samples/01도4663.jpeg
```

## FastAPI 실행

```powershell
cd AI_v1_package
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## FastAPI 엔드포인트

```text
GET  /api/health
POST /api/plate-recognition
```

`POST /api/plate-recognition`은 `multipart/form-data` 형식으로 `file` 값을 전달한다.

## FastAPI 처리 흐름

```text
controller/plate_controller.py
업로드 파일 받기

service/plate_service.py
파일 저장 후 predict_plate.py 호출

scripts/predict_plate.py
YOLO + OCR 번호판 인식

결과 JSON 반환
```

## 결과 예시

```json
{
  "detected": true,
  "plateNumber": "01도4663",
  "ocrRaw": "01도4663",
  "confidence": 0.82,
  "detectionConfidence": 0.83,
  "ocrConfidence": 0.98,
  "needReview": false,
  "reviewReasons": [],
  "cropPath": "result/api_crop/01도4663_plate_0.jpg",
  "candidates": []
}
```

## 참고

- `models/team_yolo_best.pt`: YOLO 번호판 탐지 모델
- `scripts/predict_plate.py`: YOLO 탐지 후 PaddleOCR 수행
- `scripts/ocr_postprocess.py`: OCR 결과 후처리 함수
- `result/api_crop/`: 실행 중 crop 이미지 저장 위치
