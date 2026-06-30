# Port Gate Python Backend

FastAPI backend for the port gate vehicle entry and container sector guidance system.

## Local Setup

From the project root:

```powershell
.\.venv\Scripts\Activate.ps1
pip install -r backend-python\requirements.txt
Copy-Item backend-python\.env.example backend-python\.env
uvicorn app.main:app --reload --app-dir backend-python
```

API health check:

```text
http://localhost:8000/health
```
