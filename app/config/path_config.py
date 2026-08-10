from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[2]
UPLOAD_DIR = BASE_DIR / "result" / "api_upload"

UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
