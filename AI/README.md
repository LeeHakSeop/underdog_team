# Port Gate 단일 번호판 인식 모델

`AI (1)`은 프로젝트에서 사용하는 유일한 번호판 인식 서버입니다.

## 처리 흐름

```text
Spring Boot
→ POST http://127.0.0.1:8000/api/plate-recognition
→ UnifiedPlatePipeline
→ 번호판 검출
→ 일반/화물차 번호판 유형 판별
→ 유형별 OCR
→ Spring DTO 형식의 결과 반환
```

모델 선택값은 사용하지 않습니다. 모든 요청은 `UnifiedPlatePipeline` 하나로 처리합니다.

## 실행

```powershell
cd "C:\hakseop\under_dog_team\AI (1)"
C:\Users\503-12\miniconda3\envs\paddle310\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

또는 프로젝트 루트의 `프로젝트_실행.bat`을 실행합니다.

상태 확인:

```text
http://127.0.0.1:8000/api/health
```

인식 요청:

```text
POST /api/plate-recognition
Content-Type: multipart/form-data
file: 이미지 파일
```

응답 예시:

```json
{
  "detected": true,
  "plateNumber": "04주5228",
  "ocrRaw": "04주5228",
  "confidence": 0.8,
  "detectionConfidence": 0.91,
  "ocrConfidence": 0.88,
  "needReview": false,
  "reviewReasons": [],
  "cropPath": "result/api_crop/sample.jpg",
  "candidates": [],
  "ocrType": "unified"
}
```
