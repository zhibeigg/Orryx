param([Parameter(ValueFromRemainingArguments = $true)][string[]]$InstallerArgs)
$ErrorActionPreference = "Stop"
$SkillDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$SuiteInstaller = Join-Path (Resolve-Path (Join-Path $SkillDir "..\..")) "install.ps1"
if (-not (Test-Path -LiteralPath $SuiteInstaller)) {
    Write-Error "未找到 orryx-creation-suite 根安装器。此组件不能单独提取安装；请恢复完整套件目录，或安装同级 orryx-creation-suite-runtime。"
    exit 1
}
& $SuiteInstaller @InstallerArgs
exit $LASTEXITCODE
