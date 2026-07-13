$ErrorActionPreference = 'Stop'
$SkillDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Installer = Join-Path $SkillDir '..\..\install.ps1'
if (-not (Test-Path -LiteralPath $Installer -PathType Leaf)) {
    [Console]::Error.WriteLine('ERROR: 此组件不能单独安装；请保留完整 orryx-creation-suite 并运行套件根 install.ps1。')
    exit 2
}
& $Installer @args
exit $LASTEXITCODE
