# Kills the headless test server of ONE checkout: the java process whose command line mentions serverSubTestRunProgramArgs
# (the program-args file ModDevGradle writes under the checkout's build directory) and that checkout's path. Other java processes are untouched.
param([Parameter(Mandatory = $true)][string]$Root)
$root = $Root.Replace('/', '\')
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {
    $c = $_.CommandLine
    $c -and $c -like '*serverSubTestRunProgramArgs*' -and ($c.Replace('/', '\') -like ('*' + $root + '*'))
} | ForEach-Object {
    Write-Host "killing server pid $($_.ProcessId)"
    Stop-Process -Id $_.ProcessId -Force
}
