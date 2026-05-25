$ErrorActionPreference = "Stop"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Tools = Join-Path $Root ".tools"
$Downloads = Join-Path $Tools "downloads"
$JdkDir = Join-Path $Tools "jdk17"
$SdkDir = Join-Path $Tools "android-sdk"
$CmdlineLatest = Join-Path $SdkDir "cmdline-tools\latest"

New-Item -ItemType Directory -Force $Tools, $Downloads, $SdkDir | Out-Null

function Download-File($Url, $OutFile) {
    if (Test-Path $OutFile) {
        Write-Host "Using cached $OutFile"
        return
    }
    Write-Host "Downloading $Url"
    Invoke-WebRequest -Uri $Url -OutFile $OutFile -UseBasicParsing
}

if (-not (Test-Path (Join-Path $JdkDir "bin\java.exe"))) {
    $JdkZip = Join-Path $Downloads "temurin-jdk17-windows-x64.zip"
    Download-File `
        "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse" `
        $JdkZip

    $JdkExtract = Join-Path $Downloads "jdk17-extract"
    if (Test-Path $JdkExtract) {
        Remove-Item -LiteralPath $JdkExtract -Recurse -Force
    }
    Expand-Archive -LiteralPath $JdkZip -DestinationPath $JdkExtract -Force
    $ExtractedJdk = Get-ChildItem -LiteralPath $JdkExtract -Directory | Where-Object {
        Test-Path (Join-Path $_.FullName "bin\java.exe")
    } | Select-Object -First 1
    if (-not $ExtractedJdk) {
        throw "Downloaded JDK archive did not contain bin\java.exe"
    }
    if (Test-Path $JdkDir) {
        Remove-Item -LiteralPath $JdkDir -Recurse -Force
    }
    Move-Item -LiteralPath $ExtractedJdk.FullName -Destination $JdkDir
}

if (-not (Test-Path (Join-Path $CmdlineLatest "bin\sdkmanager.bat"))) {
    $CmdZip = Join-Path $Downloads "commandlinetools-win-latest.zip"
    Download-File `
        "https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip" `
        $CmdZip

    $CmdExtract = Join-Path $Downloads "cmdline-tools-extract"
    if (Test-Path $CmdExtract) {
        Remove-Item -LiteralPath $CmdExtract -Recurse -Force
    }
    Expand-Archive -LiteralPath $CmdZip -DestinationPath $CmdExtract -Force
    $RawCmd = Join-Path $CmdExtract "cmdline-tools"
    if (-not (Test-Path (Join-Path $RawCmd "bin\sdkmanager.bat"))) {
        throw "Downloaded Android command line tools archive did not contain sdkmanager.bat"
    }
    New-Item -ItemType Directory -Force (Split-Path $CmdlineLatest -Parent) | Out-Null
    if (Test-Path $CmdlineLatest) {
        Remove-Item -LiteralPath $CmdlineLatest -Recurse -Force
    }
    Move-Item -LiteralPath $RawCmd -Destination $CmdlineLatest
}

$env:JAVA_HOME = $JdkDir
$env:ANDROID_HOME = $SdkDir
$env:ANDROID_SDK_ROOT = $SdkDir
$env:GRADLE_USER_HOME = Join-Path $Tools "gradle-home"
$env:Path = "$JdkDir\bin;$SdkDir\platform-tools;$env:Path"

$SdkManager = Join-Path $CmdlineLatest "bin\sdkmanager.bat"
& $SdkManager --sdk_root=$SdkDir "platform-tools" "platforms;android-35" "build-tools;35.0.0"
1..20 | ForEach-Object { "y" } | & $SdkManager --sdk_root=$SdkDir --licenses

Write-Host "JAVA_HOME=$JdkDir"
Write-Host "ANDROID_SDK_ROOT=$SdkDir"
Write-Host "GRADLE_USER_HOME=$env:GRADLE_USER_HOME"
