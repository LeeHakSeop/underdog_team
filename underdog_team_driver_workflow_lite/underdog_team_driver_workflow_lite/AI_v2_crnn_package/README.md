# AI_v2_crnn_package

YOLO로 crop된 번호판 이미지를 CRNN OCR 모델로 평가하기 위한 공유 패키지입니다.

현재 패키지는 모델 추론/평가에 필요한 파일만 포함합니다.
학습 원본 데이터, 평가 결과 CSV, 오답 엑셀, 문서 파일은 제외했습니다.

## 1. 폴더 구조

```text
AI_v2_crnn_package/
  model/
    best.pt
    charset.json
    summary.txt

  scripts/
    evaluate_crnn_ocr.py
    evaluate_crnn_ocr.ps1
    plate_postprocess.py
    train_crnn_ocr.py

  dependencies.txt
  README.md
```

## 2. 파일 설명

```text
model/best.pt
```

최종 학습된 CRNN OCR 모델 파일입니다.

```text
model/charset.json
```

모델이 인식하는 문자 목록과 입력 이미지 크기 정보입니다.

```text
model/summary.txt
```

학습 조건과 검증 성능 요약입니다.

```text
scripts/evaluate_crnn_ocr.py
```

labels.csv 기준으로 OCR 평가를 실행하는 파일입니다.

```text
scripts/evaluate_crnn_ocr.ps1
```

Windows PowerShell에서 평가 명령어를 짧게 실행하기 위한 파일입니다.

```text
scripts/plate_postprocess.py
```

OCR 결과 후처리 파일입니다.

```text
scripts/train_crnn_ocr.py
```

CRNN 모델 구조와 데이터셋 처리 코드가 들어 있습니다.
파일명은 train이지만, evaluate_crnn_ocr.py가 내부의 CRNN 클래스를 사용하므로 평가 실행에도 필요합니다.

## 3. 설치 패키지

```bash
pip install -r dependencies.txt
```

필요 패키지:

```text
torch
torchvision
pillow
openpyxl
```

## 4. labels.csv 형식

평가용 labels.csv는 아래 컬럼이 필요합니다.

```csv
image,label,split
images/01가2345.jpg,01가2345,val
images/12나3456.jpg,12나3456,val
```

컬럼 설명:

```text
image: crop된 번호판 이미지 경로
label: 정답 번호판
split: train, val, all 중 평가 구분값
```

## 5. 실행 방법

패키지 폴더로 이동합니다.

```bash
cd C:\hakseop\under_dog_team\AI_v2_crnn_package
```

PowerShell에서 실행합니다.

```powershell
.\scripts\evaluate_crnn_ocr.ps1 `
  -LabelsCsv <labels.csv 경로> `
  -Model model\best.pt `
  -Output runs\ocr\eval_crnn `
  -Split all `
  -Device auto
```

예시:

```powershell
.\scripts\evaluate_crnn_ocr.ps1 `
  -LabelsCsv C:\hakseop\data\labels.csv `
  -Model model\best.pt `
  -Output runs\ocr\eval_crnn `
  -Split val `
  -Device auto
```

## 6. Device 옵션

```text
auto: CUDA 사용 가능 시 GPU, 아니면 CPU
cuda: GPU 강제 사용
cpu: CPU 사용
```

## 7. 현재 모델 성능

```text
Validation total: 948
Validation correct: 929
Validation accuracy: 98.00%
Validation char accuracy: 99.35%
Validation avg edit distance: 0.049
```

## 8. 제외한 파일

아래 파일은 깃 공유용 패키지에서 제외했습니다.

```text
중복 best.pt
evaluation.csv
FALSE목록.xlsx
정리.docx
기존 summary.txt 중복본
실행 결과 runs/
```

## 9. 전달 기준

이 패키지는 CRNN OCR 평가용입니다.
YOLO 객체 탐지 모델과 결합할 경우, YOLO가 번호판 이미지를 crop한 뒤 해당 crop 이미지를 labels.csv 기준으로 평가하거나 별도 추론 흐름에 연결해야 합니다.
