param(
    [string]$Platform = "claude-code",
    [switch]$Project,
    [string]$Path,
    [string]$Components = "all",
    [switch]$All,
    [switch]$Force,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$Installer = Join-Path $PSScriptRoot "scripts/install_suite.py"
$Arguments = @($Installer, "--platform", $Platform, "--components", $Components)
if ($Project) { $Arguments += "--project" }
if ($Path) { $Arguments += @("--path", $Path) }
if ($All) { $Arguments += "--all" }
if ($Force) { $Arguments += "--force" }
if ($DryRun) { $Arguments += "--dry-run" }

if (Get-Command py -ErrorAction SilentlyContinue) {
    & py -3 @Arguments
} elseif (Get-Command python3 -ErrorAction SilentlyContinue) {
    & python3 @Arguments
} elseif (Get-Command python -ErrorAction SilentlyContinue) {
    & python @Arguments
} else {
    throw "Python 3.10 or newer is required."
}
exit $LASTEXITCODE
