import json
import sys

from plate_detector import PlateCandidateDetector
from plate_type_classifier import PlateTypeClassifier


if len(sys.argv) < 2:
    raise SystemExit(
        "사용법: python scripts/test_candidate_and_type.py 이미지경로"
    )

detector = PlateCandidateDetector()
classifier = PlateTypeClassifier()

rows = []

for candidate in detector.detect(sys.argv[1]):
    type_result = classifier.predict(candidate["crop"])
    rows.append({
        "candidate_index": candidate["candidate_index"],
        "crop_path": candidate["crop_path"],
        "box": candidate["box"],
        "detection_confidence": candidate["detection_confidence"],
        "center_score": candidate["center_score"],
        **type_result,
    })

print(json.dumps(rows, ensure_ascii=False, indent=2))
