from plate_grammar import is_valid_by_type


def candidate_rank_key(candidate):
    """
    공통 후보 선택 우선순위:

    1. 인식 완성도
       2 = 지역명까지 포함한 전체 번호판 문법 정상
       1 = 2줄 번호판 본문까지만 문법 정상
       0 = 문법 불일치

    2. 해당 OCR 모듈이 판단한 문법 정상 여부
    3. 화면 중앙성
    4. OCR confidence
    5. 번호판 검출 confidence
    6. 형태 분류 confidence
    """
    plate_type = candidate.get(
        "plate_type",
        "",
    )
    plate_number = candidate.get(
        "plate_number",
        "",
    )

    full_valid = is_valid_by_type(
        plate_type,
        plate_number,
    )

    level_score = candidate.get(
        "recognition_level_score"
    )

    if level_score is None:
        level_score = 2 if full_valid else 0

    grammar_valid = candidate.get(
        "grammar_valid"
    )

    if grammar_valid is None:
        grammar_valid = full_valid

    return (
        int(level_score),
        int(bool(grammar_valid)),
        float(candidate.get("center_score", 0.0)),
        float(candidate.get("ocr_confidence", 0.0)),
        float(
            candidate.get(
                "detection_confidence",
                0.0,
            )
        ),
        float(
            candidate.get(
                "type_confidence",
                0.0,
            )
        ),
    )


def choose_best_candidate(candidates):
    if not candidates:
        return None

    return max(
        candidates,
        key=candidate_rank_key,
    )
