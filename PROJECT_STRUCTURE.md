# Project Structure

This repository contains a port logistics team project with a Spring backend, a Vue frontend, database seed scripts, and local planning materials.

## Main Folders

- `backend/portprj/` - Spring Boot backend project.
- `frontend/` - Vue/Vite frontend project.
- `DB/` - PostgreSQL SQL scripts for carrier, driver, and vehicle data.
- Planning materials folder - Local documents, PDFs, and screenshots. These files are ignored by Git.

## Frontend Data Rule

Frontend table views are aligned to the DB diagram tables:

- `carrier`
- `driver`
- `vehicle`
- `work_order`
- `container`
- `yard_sector`
- `gate_log`
- `plate_recognition`
- `work_status_history`
- `exception_log`

The old `frontend/src/data/mockData.js` file was removed. Temporary frontend read data now lives in `frontend/src/data/dbData.js` and uses DB column names only, so it can be replaced by real API responses later.

## Generated Or Local-Only Folders

The following folders are local dependencies, build outputs, IDE metadata, or caches:

- `.venv/`
- `.tools/`
- `.pytest_cache/`
- `.idea/`
- `backend/portprj/.gradle/`
- `backend/portprj/build/`
- `frontend/node_modules/`
- `frontend/dist/`

These are intentionally ignored and can be regenerated.

## Useful Commands

Backend:

```powershell
cd backend/portprj
./gradlew bootRun
```

Frontend:

```powershell
cd frontend
npm run dev
```

Frontend production build:

```powershell
cd frontend
npm run build
```
