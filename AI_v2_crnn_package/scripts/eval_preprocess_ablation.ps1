param(
    [string]$Model = "model\best.pt",
    [Parameter(Mandatory=$true)]
    [string]$LabelsCsv,
    [string]$ImageRoot = "",
    [string]$Output = "runs\ocr\preprocess_ablation",
    [ValidateSet("all", "train", "val")]
    [string]$Split = "val",
    [string]$Modes = "none,gray,clahe,sharpen,threshold",
    [int]$BatchSize = 64,
    [string]$Device = "auto",
    [string]$ExtraPythonPath = ""
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$WorkspaceRoot = Split-Path -Parent $ProjectRoot
$BundledPython = Join-Path $env:USERPROFILE ".cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"
$PythonCandidates = @(
    (Join-Path $ProjectRoot ".venv\Scripts\python.exe"),
    $BundledPython,
    "python"
)
$Python = $PythonCandidates | Where-Object { $_ -eq "python" -or (Test-Path $_) } | Select-Object -First 1

Push-Location $ProjectRoot
try {
    $OriginalPythonPath = $env:PYTHONPATH
    if (-not $ExtraPythonPath) {
        $SitePackageCandidates = @(
            (Join-Path $WorkspaceRoot "team_ai\PARSeq\.venv\Lib\site-packages"),
            (Join-Path $WorkspaceRoot "team_ai\SVTRv2\.venv\Lib\site-packages"),
            (Join-Path $WorkspaceRoot "team_ai\yolo11\.venv\Lib\site-packages")
        )
        $ExtraPythonPath = $SitePackageCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
    }
    if ($ExtraPythonPath) {
        if ($env:PYTHONPATH) {
            $env:PYTHONPATH = "$ExtraPythonPath;$env:PYTHONPATH"
        }
        else {
            $env:PYTHONPATH = $ExtraPythonPath
        }
    }

    & $Python scripts\eval_preprocess_ablation.py `
        --model $Model `
        --labels-csv $LabelsCsv `
        --image-root $ImageRoot `
        --output $Output `
        --split $Split `
        --modes $Modes `
        --batch-size $BatchSize `
        --device $Device
}
finally {
    $env:PYTHONPATH = $OriginalPythonPath
    Pop-Location
}
