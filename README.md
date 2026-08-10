# AI

항만 게이트 번호판 인식 FastAPI 통합 패키지입니다.

이 폴더 하나 안에서 YOLO 번호판 탐지, PaddleOCR 추론, CRNN 추론을 모두 관리합니다.

## 1. 폴더 구조

```text
AI/
  app/
    main.py
    config/
    controller/
    model/
    service/

  scripts/
    predict_plate.py
    paddle_ocr_predict.py
    crnn_ocr_predict.py
    ocr_postprocess.py
    train_crnn_ocr.py

  models/
    team_yolo_best.pt
    crnn_best.pt
    crnn_charset.json
    crnn_summary.txt

  samples/
  dependencies.txt
  README.md
```

## 2. 실행 흐름

```text
Spring Boot
→ FastAPI /api/plate-recognition
→ predict_plate.py
→ YOLO 번호판 crop
→ PaddleOCR 또는 CRNN 선택 실행
→ 번호판 결과 JSON 반환
```

## 3. 모델 선택 방식

API 주소는 하나만 사용합니다.

```text
POST /api/plate-recognition
```

form-data 값:

```text
file: 이미지 파일
ocrType: paddle 또는 crnn
```

```text
ocrType=paddle
→ scripts/paddle_ocr_predict.py 실행

ocrType=crnn
→ scripts/crnn_ocr_predict.py 실행
```

## 4. 서버 실행

```bash
cd C:\hakseop\under_dog_team\AI
conda activate paddle310
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

서버 확인:

```text
http://localhost:8000/api/health
```

## 5. 필요한 패키지 설치

```bash
pip install -r dependencies.txt
```

## 6. 반환 JSON 기준

Spring Boot는 아래 형태의 응답을 기준으로 처리합니다.

```json
{
  "detected": true,
  "plateNumber": "01도4663",
  "ocrRaw": "01도4663",
  "confidence": 0.83,
  "detectionConfidence": 0.95,
  "ocrConfidence": 0.87,
  "needReview": false,
  "reviewReasons": [],
  "cropPath": "result/api_crop/01도4663_plate_0.jpg",
  "candidates": [],
  "ocrType": "paddle"
}
```

## 7. 모델 파일 관리 기준

새 모델이 생기면 `models/` 안에 추가합니다.

```text
YOLO 모델
→ models/team_yolo_best.pt

CRNN 모델
→ models/crnn_best.pt
```

모델 파일명을 바꾸는 경우에는 아래 파일의 경로도 같이 수정해야 합니다.

```text
scripts/predict_plate.py
scripts/crnn_ocr_predict.py
```

## 8. 주의사항

기존 API 주소는 바꾸지 않습니다.

```text
Spring: /api/plate-recognition/recognize
FastAPI: /api/plate-recognition
```

모델만 `ocrType` 값으로 전환합니다.
