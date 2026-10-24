param([string]$Gradle = '')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $Gradle) { $Gradle = Join-Path $projectRoot 'gradlew.bat' }
$previousDirectory = Get-Location
try {
    Set-Location -LiteralPath $projectRoot
    & $Gradle assembleDebug lintDebug --console plain
    if ($LASTEXITCODE -ne 0) { throw 'APK build or Lint failed; do not push this build.' }
    $config = Get-Content -LiteralPath 'app/build.gradle' -Raw -Encoding UTF8
    $match = [regex]::Match($config, "versionName\s+'([^']+)'")
    if (-not $match.Success) { throw 'versionName was not found.' }
    $sdk = $env:ANDROID_HOME
    if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
    if (-not $sdk -and (Test-Path -LiteralPath 'local.properties')) {
        $sdkLine = Get-Content -LiteralPath 'local.properties' -Encoding UTF8 | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($sdkLine) { $sdk = ($sdkLine -replace '^sdk\.dir=', '').Replace('\:', ':').Replace('\\', '\') }
    }
    $signer = if ($sdk) { Join-Path $sdk 'build-tools/36.0.0/apksigner.bat' } else { '' }
    if (-not $signer -or -not (Test-Path -LiteralPath $signer)) { throw 'Configure ANDROID_HOME or local.properties with Build Tools 36.0.0 to verify the APK.' }
    & $signer verify --verbose 'app/build/outputs/apk/debug/app-debug.apk'
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    $releaseDirectory = Join-Path $projectRoot 'releases'
    New-Item -ItemType Directory -Force -Path $releaseDirectory | Out-Null
    $apk = Join-Path $releaseDirectory ('LightStart-' + $match.Groups[1].Value + '-debug.apk')
    Copy-Item -LiteralPath 'app/build/outputs/apk/debug/app-debug.apk' -Destination $apk
    $hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText($apk + '.sha256', $hash + '  ' + [IO.Path]::GetFileName($apk) + "`n", (New-Object Text.UTF8Encoding($false)))
    Write-Output "APK ready: $apk"
} finally { Set-Location -LiteralPath $previousDirectory.Path }
