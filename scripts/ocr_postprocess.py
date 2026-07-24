import re

PLATE_KOREAN = [
    "가", "나", "다", "라", "마",
    "거", "너", "더", "러", "머",
    "버", "서", "어", "저",
    "고", "노", "도", "로", "모",
    "보", "소", "오", "조",
    "구", "누", "두", "루", "무",
    "부", "수", "우", "주",
    "아", "바", "사", "자",
    "하", "허",
    "육", "합", "해", "호",
]

def extract_plate_korean(text):
    """
    번호판 문자열에서 한글 1글자 추출

    예)
    100더9774 -> 더
    384가4927 -> 가
    10349453 -> None
    """

    for ch in text:
        if "가" <= ch <= "힣":
            return ch

    return None

KOREAN_CORRECTION_MAP = {
    "덕": "더",
    "히": "하",
    "미": "마",
}


def correct_plate_korean(text):
    """
    번호판에서 사용하지 않는 한글을 보정
    예)
    100덕9774 -> 100더9774
    101히6496 -> 101하6496
    """

    kr = extract_plate_korean(text)

    if kr is None:
        return text

    if kr in PLATE_KOREAN:
        return text

    if kr in KOREAN_CORRECTION_MAP:
        return text.replace(kr, KOREAN_CORRECTION_MAP[kr], 1)

    return text

def clean_plate_text(text):
    """
    OCR 결과를 번호판 형태로 정리
    """

    # 1. 공백 제거
    text = text.replace(" ", "")

    # 2. 숫자, 한글만 남기기
    text = re.sub(r"[^0-9가-힣]", "", text)

    return text


def normalize_plate_pattern(text):
    """
    번호판 뒤 숫자가 5자리로 인식된 경우,
    마지막 숫자를 번호판 외곽선 오인식으로 보고 제거한다.

    예:
    01가74315 -> 01가7431
    충북92아17031 -> 충북92아1703
    """

    text = clean_plate_text(text)

    # 지역명 없는 번호판
    # 숫자 2~3자리 + 한글 1글자 + 숫자 5자리
    match_without_region = re.fullmatch(
        r"\d{2,3}[가-힣]\d{5}",
        text,
    )

    # 지역명 있는 번호판
    # 지역명 2글자 + 숫자 2~3자리 + 한글 1글자 + 숫자 5자리
    match_with_region = re.fullmatch(
        r"[가-힣]{2}\d{2,3}[가-힣]\d{5}",
        text,
    )

    if match_without_region or match_with_region:
        text = text[:-1]

    return text

if __name__ == "__main__":
    samples = [
        "(01구 8649",
        "384로9196]",
        "'3832 7844",
    ]
    

    for s in samples:
        print("원본 :", s)
        print("정리 :", clean_plate_text(s))
        print()
    print("번호판 허용 한글 개수:", len(PLATE_KOREAN))
    print("더 포함 여부:", "더" in PLATE_KOREAN)
    print("덕 포함 여부:", "덕" in PLATE_KOREAN)
    print()

    print(extract_plate_korean("100더9774"))
    print(extract_plate_korean("384가4927"))
    print(extract_plate_korean("10349453"))
    
    print(correct_plate_korean("100덕9774"))
    print(correct_plate_korean("101히6496"))
    print(correct_plate_korean("101미2263"))
    print(correct_plate_korean("100더9774"))