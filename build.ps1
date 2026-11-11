# Self-check, then build. Run this instead of `gradle build`.
#
# The check catches the failure classes that cost real startup crashes here:
#   - @Mixin target that does not exist
#   - an injected method the target only inherits (Mixin cannot inject into those)
#   - handlers that capture locals by name (released jars ship no LocalVariableTable)
#   - mixin sources that are not registered in masaextra.mixins.json
#   - UTF-8 BOMs (javac rejects them) and broken JSON resources
#
# Usage:  .\build.ps1 [-SkipCheck]
param(
    [switch]$SkipCheck
)

$ErrorActionPreference = 'Stop'
$proj = $PSScriptRoot
$root = Split-Path (Split-Path $proj -Parent) -Parent
$tools = Join-Path $root '_build\tools'
$java = Join-Path $root '_build\jdk25\jdk-25.0.5.0.2+1\bin\java.exe'
$javap = Join-Path $root '_build\jdk25\jdk-25.0.5.0.2+1\bin\javap.exe'
$gradle = Join-Path $root '_build\gradle\gradle-9.7.1\bin\gradle.bat'
$gradleHome = Join-Path $root '_build\gradle-home'

$mcNamed = Join-Path $gradleHome 'caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged\1.20.4-loom.mappings.1_20_4.layered+hash.2198-v2\minecraft-merged-1.20.4-loom.mappings.1_20_4.layered+hash.2198-v2.jar'
$litematica = Join-Path $root 'mods\litematica-fabric-1.20.4-0.17.4.jar'
$malilib = (Get-ChildItem (Join-Path $root 'mods') -Filter 'malilib*.jar' | Select-Object -First 1).FullName
$carpetExtra = Join-Path $root 'mods\carpet-extra-1.20.3-1.4.128.jar'

if (-not $SkipCheck) {
    Write-Host '=== pre-build self check ===' -ForegroundColor Cyan
    & $java (Join-Path $tools 'PreBuildCheck.java') $proj $javap $mcNamed $litematica $malilib $carpetExtra
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'Build aborted: fix the problems above first.' -ForegroundColor Red
        exit 1
    }
}

Write-Host '=== build ===' -ForegroundColor Cyan
$env:GRADLE_USER_HOME = $gradleHome
$env:JAVA_HOME = Join-Path $root '_build\jdk25\jdk-25.0.5.0.2+1'
$tmp = Join-Path $root '_dsh_tmp\gradletmp'
New-Item -ItemType Directory $tmp -Force | Out-Null
$env:TEMP = $tmp
$env:TMP = $tmp

Push-Location $proj
try {
    & $gradle build --offline --console=plain --no-daemon
    $gradleExit = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($gradleExit -ne 0) {
    Write-Host 'BUILD FAILED' -ForegroundColor Red
    exit 1
}

$version = (Select-String -Path (Join-Path $proj 'gradle.properties') -Pattern '^version=(.*)$').Matches[0].Groups[1].Value.Trim()
$jar = Join-Path $proj "build\libs\masaextra-$version.jar"
if (Test-Path $jar) {
    Write-Host "BUILD OK -> $jar" -ForegroundColor Green
} else {
    Write-Host "Build reported success but $jar is missing." -ForegroundColor Red
    exit 1
}
