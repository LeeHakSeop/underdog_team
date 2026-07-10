import re
from dataclasses import dataclass


PLATE_FILTER = re.compile("[^0-9A-Za-z\uAC00-\uD7A3]")
PLATE_PATTERN = re.compile(r"^(?:[가-힣]{2})?\d{1,3}[가-힣]\d{4}$")
HANGUL_PATTERN = re.compile("[가-힣]")


@dataclass
class PostprocessResult:
    raw: str
    normalized: str
    corrected: str
    is_valid: bool
    changed: bool
    rules: list[str]
    needs_review: bool


def normalize_plate_text(value: str) -> str:
    return PLATE_FILTER.sub("", value or "").upper()


def collapse_repeated_hangul(value: str) -> tuple[str, bool]:
    chars = []
    changed = False
    previous = ""
    for char in value:
        if char == previous and HANGUL_PATTERN.fullmatch(char):
            changed = True
            continue
        chars.append(char)
        previous = char
    return "".join(chars), changed


def postprocess_plate(value: str) -> PostprocessResult:
    rules = []
    normalized = normalize_plate_text(value)
    if normalized != (value or ""):
        rules.append("normalize")

    corrected, changed_repeat = collapse_repeated_hangul(normalized)
    if changed_repeat:
        rules.append("collapse_repeated_hangul")

    is_valid = bool(PLATE_PATTERN.fullmatch(corrected))
    needs_review = not is_valid
    return PostprocessResult(
        raw=value or "",
        normalized=normalized,
        corrected=corrected,
        is_valid=is_valid,
        changed=corrected != (value or ""),
        rules=rules,
        needs_review=needs_review,
    )


if __name__ == "__main__":
    samples = ["822머머4929", "22두8930", "177허2445", "82-2머4929"]
    for sample in samples:
        print(postprocess_plate(sample))
