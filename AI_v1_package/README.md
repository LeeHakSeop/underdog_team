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

## 실행 방법

패키지 폴더 기준으로 실행한다.

```powershell
cd AI_v1_package
python scripts/predict_plate.py samples/01도4663.jpeg
```

## 결과 예시

```json
{
  "detected": true,
  "plateNumber": "01도4663",
  "ocrRaw": "01도4663",
  "confidence": 0.76,
  "detectionConfidence": 0.83,
  "ocrConfidence": 0.91,
  "needReview": false,
  "reviewReasons": [],
  "cropPath": "result/api_crop/01도4663_plate_0.jpg",
  "candidates": []
}
```

## 참고

- `models/team_yolo_best.pt`는 YOLO 번호판 탐지 모델이다.
- `scripts/predict_plate.py`는 YOLO 탐지 후 PaddleOCR을 실행한다.
- `scripts/ocr_postprocess.py`는 OCR 결과 후처리 함수가 들어 있다.
- `result/api_crop/`에는 실행 중 crop 이미지가 저장된다.
- 패키지 위치가 바뀌어도 `predict_plate.py`는 현재 패키지 루트를 기준으로 모델과 결과 폴더를 찾도록 수정되어 있다.
