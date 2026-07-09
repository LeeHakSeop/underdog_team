param(
    [Parameter(Mandatory=$true)]
    [string]$Model,
    [string]$LabelsCsv = "ocr_test_10000\labels.csv",
    [string]$Output = "runs\ocr\crnn_plate_all_eval",
    [ValidateSet("all", "train", "val")]
    [string]$Split = "all",
    [int]$BatchSize = 64,
    [string]$Device = "auto"
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$PythonCandidates = @(
    (Join-Path $ProjectRoot ".venv\Scripts\python.exe"),
    (Join-Path $ProjectRoot "..\yolo11\.venv\Scripts\python.exe"),
    "python"
)
$Python = $PythonCandidates | Where-Object { $_ -eq "python" -or (Test-Path $_) } | Select-Object -First 1

Push-Location $ProjectRoot
try {
    & $Python scripts\evaluate_crnn_ocr.py `
        --model $Model `
        --labels-csv $LabelsCsv `
        --output $Output `
        --split $Split `
        --batch-size $BatchSize `
        --device $Device
}
finally {
    Pop-Location
}
