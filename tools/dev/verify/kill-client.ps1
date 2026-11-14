# Kills the test client of ONE checkout and game directory: the java process whose command line mentions screenshotTestRunProgramArgs (written
# by ModDevGradle under the checkout's build directory) or the game directory, and this checkout's path. Other java processes are untouched.
param([Parameter(Mandatory = $true)][string]$Root, [Parameter(Mandatory = $true)][string]$GameDir)
$root = $Root.Replace('/', '\')
$gd = $GameDir.Replace('/', '\')
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {
    $c = $_.CommandLine
    if (-not $c) { return $false }
    $n = $c.Replace('/', '\')
    ($n -like ('*' + $root + '*')) -and ($n -like '*screenshotTestRunProgramArgs*' -or $n -like ('*' + $gd + '*'))
} | ForEach-Object {
    Write-Host "killing client pid $($_.ProcessId)"
    Stop-Process -Id $_.ProcessId -Force
}
