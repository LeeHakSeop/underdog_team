import re

REGION_NAMES = [
    "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
    "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주",
]

# 일반차량·민간 사업용·택배·렌터카에 사용하는 가운데 한글.
# 군용 접두 문자인 육/합/해는 공통 번호판 한글에서 제외합니다.
PLATE_KOREAN = [
    "가", "나", "다", "라", "마",
    "거", "너", "더", "러", "머",
    "버", "서", "어", "저",
    "고", "노", "도", "로", "모",
    "보", "소", "오", "조",
    "구", "누", "두", "루", "무",
    "부", "수", "우", "주",
    "바", "사", "아", "자",
    "배",
    "하", "허", "호",
]

REGION_PATTERN = "|".join(
    re.escape(value) for value in REGION_NAMES
)
KOREAN_PATTERN = "".join(
    re.escape(value) for value in PLATE_KOREAN
)

NORMAL_PATTERN = re.compile(
    rf"^(?:(?:{REGION_PATTERN}))?"
    rf"\d{{2,3}}[{KOREAN_PATTERN}]\d{{4}}$"
)

# 023·024 노트북의 실제 평가 문법:
# 앞 숫자 2~3자리 + 가운데 한글 + 뒤 숫자 4자리
TRUCK_BODY_PATTERN = re.compile(
    rf"^\d{{2,3}}[{KOREAN_PATTERN}]\d{{4}}$"
)

TRUCK_FULL_PATTERN = re.compile(
    rf"^(?:{REGION_PATTERN})"
    rf"\d{{2,3}}[{KOREAN_PATTERN}]\d{{4}}$"
)


def clean_text(text):
    return re.sub(
        r"[^0-9가-힣]",
        "",
        str(text or ""),
    )


def is_valid_region(text):
    return clean_text(text) in REGION_NAMES


def is_valid_normal(text):
    return bool(
        NORMAL_PATTERN.fullmatch(clean_text(text))
    )


def is_valid_truck_body(text):
    return bool(
        TRUCK_BODY_PATTERN.fullmatch(clean_text(text))
    )


def is_valid_truck_full(text):
    return bool(
        TRUCK_FULL_PATTERN.fullmatch(clean_text(text))
    )


def is_valid_by_type(plate_type, text):
    if plate_type == "normal":
        return is_valid_normal(text)

    if plate_type in {"one_line", "two_line"}:
        return is_valid_truck_full(text)

    return False
