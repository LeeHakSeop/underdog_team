from paddleocr import PaddleOCR

from ocr_postprocess import (
    clean_plate_text,
    normalize_plate_pattern,
    correct_plate_korean,
)


ocr = PaddleOCR(lang="korean", use_angle_cls=True)


def predict_paddle_ocr(crop_path):
    result = ocr.ocr(str(crop_path), cls=True)

    raw_texts = []
    confidence_list = []

    if result and result[0]:
        for line in result[0]:
            raw_texts.append(line[1][0])
            confidence_list.append(float(line[1][1]))

    ocr_raw = "".join(raw_texts)

    if confidence_list:
        ocr_confidence = sum(confidence_list) / len(confidence_list)
    else:
        ocr_confidence = 0.0

    plate_number = clean_plate_text(ocr_raw)
    plate_number = normalize_plate_pattern(plate_number)
    plate_number = correct_plate_korean(plate_number)

    return ocr_raw, plate_number, ocr_confidence
