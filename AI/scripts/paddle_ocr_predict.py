import os

os.environ.setdefault("PADDLE_PDX_ENABLE_MKLDNN_BYDEFAULT", "False")
os.environ.setdefault("FLAGS_use_mkldnn", "0")

from paddleocr import PaddleOCR

from ocr_postprocess import (
    clean_plate_text,
    normalize_plate_pattern,
    correct_plate_korean,
)


ocr = PaddleOCR(
    lang="korean",
    use_doc_orientation_classify=False,
    use_doc_unwarping=False,
    use_textline_orientation=False,
)


def collect_texts_and_scores(result):
    raw_texts = []
    confidence_list = []

    if not result:
        return raw_texts, confidence_list

    for item in result:
        if isinstance(item, dict):
            texts = item.get("rec_texts") or []
            scores = item.get("rec_scores") or []
            raw_texts.extend(str(text) for text in texts)
            confidence_list.extend(float(score) for score in scores)
            continue

        if isinstance(item, (list, tuple)):
            for line in item:
                if len(line) >= 2 and isinstance(line[1], (list, tuple)):
                    raw_texts.append(str(line[1][0]))
                    confidence_list.append(float(line[1][1]))

    return raw_texts, confidence_list


def predict_paddle_ocr(crop_path):
    result = ocr.predict(
        str(crop_path),
        use_doc_orientation_classify=False,
        use_doc_unwarping=False,
        use_textline_orientation=False,
    )
    raw_texts = []
    confidence_list = []

    raw_texts, confidence_list = collect_texts_and_scores(result)

    ocr_raw = "".join(raw_texts)

    if confidence_list:
        ocr_confidence = sum(confidence_list) / len(confidence_list)
    else:
        ocr_confidence = 0.0

    plate_number = clean_plate_text(ocr_raw)
    plate_number = normalize_plate_pattern(plate_number)
    plate_number = correct_plate_korean(plate_number)

    return ocr_raw, plate_number, ocr_confidence
