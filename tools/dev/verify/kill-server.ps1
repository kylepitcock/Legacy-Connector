# Kills the headless test server of ONE checkout: the java process whose command line mentions serverSubTestRunProgramArgs
# (the program-args file ModDevGradle writes under the checkout's build directory) and that checkout's path. Other java processes are untouched.
param([Parameter(Mandatory = $true)][string]$Root)
$root = $Root.Replace('/', '\').TrimEnd('\')
# worktrees live below the main checkout (.claude\worktrees\<name>): their servers mention the main path too and must be left alone
$nested = $root + '\.claude\worktrees\'
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {
    $c = $_.CommandLine
    if (-not $c -or $c -notlike '*serverSubTestRunProgramArgs*') { return $false }
    $c = $c.Replace('/', '\')
    ($c -like ('*' + $root + '\*')) -and ($c -notlike ('*' + $nested + '*'))
} | ForEach-Object {
    Write-Host "killing server pid $($_.ProcessId)"
    Stop-Process -Id $_.ProcessId -Force
}
