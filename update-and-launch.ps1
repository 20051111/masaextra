# 一键更新 masaextra 并重启游戏。
#
# 流程：停游戏 → 构建（先自检）→ 清理 mods 旧版本 → 装新 jar → 启动游戏
#
# 用法：
#   .\update-and-launch.ps1            # 默认
#   .\update-and-launch.ps1 -NoLaunch  # 只更新不启动
#   .\update-and-launch.ps1 -SkipBuild # 用已有 jar，只替换并重启
param(
    [switch]$NoLaunch,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$proj = $PSScriptRoot
$root = Split-Path (Split-Path $proj -Parent) -Parent
$launcher = 'C:\Users\Danil\Desktop\Plain Craft Launcher 2.exe'

function Stop-Game {
    $procs = Get-Process | Where-Object { $_.ProcessName -match '^javaw?$' }
    if (-not $procs) {
        Write-Host '[1/5] 游戏未运行' -ForegroundColor DarkGray
        return
    }
    foreach ($p in $procs) {
        Write-Host "[1/5] 停止游戏进程 PID $($p.Id)" -ForegroundColor Yellow
        Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
    }
    # 等文件句柄释放，否则 mods 里的 jar 删不掉
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Milliseconds 500
        if (-not (Get-Process | Where-Object { $_.ProcessName -match '^javaw?$' })) { break }
    }
}

function Build-Jar {
    if ($SkipBuild) {
        Write-Host '[2/5] 跳过构建' -ForegroundColor DarkGray
        return
    }
    $java = Join-Path $root '_build\jdk25\jdk-25.0.5.0.2+1\bin\java.exe'
    $javap = Join-Path $root '_build\jdk25\jdk-25.0.5.0.2+1\bin\javap.exe'
    $gradleHome = Join-Path $root '_build\gradle-home'
    $named = Join-Path $gradleHome 'caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged\1.20.4-loom.mappings.1_20_4.layered+hash.2198-v2\minecraft-merged-1.20.4-loom.mappings.1_20_4.layered+hash.2198-v2.jar'
    $litematica = Join-Path $root 'mods\litematica-fabric-1.20.4-0.17.4.jar'
    $malilib = (Get-ChildItem (Join-Path $root 'mods') -Filter 'malilib*.jar' | Select-Object -First 1).FullName
    $carpetExtra = Join-Path $root 'mods\carpet-extra-1.20.3-1.4.128.jar'

    Write-Host '[2/5] 自检' -ForegroundColor Cyan
    & $java (Join-Path $root '_build\tools\PreBuildCheck.java') $proj $javap $named $litematica $malilib $carpetExtra
    if ($LASTEXITCODE -ne 0) { throw '自检未通过，已中止' }

    Write-Host '[2/5] 构建' -ForegroundColor Cyan
    $env:GRADLE_USER_HOME = $gradleHome
    $env:JAVA_HOME = Join-Path $root '_build\jdk25\jdk-25.0.5.0.2+1'
    $tmp = Join-Path $root '_dsh_tmp\gradletmp'
    New-Item -ItemType Directory $tmp -Force | Out-Null
    $env:TEMP = $tmp
    $env:TMP = $tmp
    Push-Location $proj
    try {
        & (Join-Path $root '_build\gradle\gradle-9.7.1\bin\gradle.bat') build --offline --console=plain --no-daemon
        if ($LASTEXITCODE -ne 0) { throw '构建失败' }
    } finally { Pop-Location }
}

function Install-Jar {
    $version = (Select-String -Path (Join-Path $proj 'gradle.properties') -Pattern '^version=(.*)$').Matches[0].Groups[1].Value.Trim()
    $built = Join-Path $proj "build\libs\masaextra-$version.jar"
    if (-not (Test-Path $built)) { throw "找不到构建产物 $built" }

    Write-Host "[3/5] 清理 mods 中的其它 masaextra 版本" -ForegroundColor Cyan
    $mods = Join-Path $root 'mods'
    $target = "masaextra-$version.jar"
    Get-ChildItem $mods -Filter 'masaextra*.jar' | ForEach-Object {
        if ($_.Name -ne $target) {
            Move-Item $_.FullName (Join-Path $root "_build\disabled-$($_.Name)") -Force
            Write-Host "  移出 $($_.Name)" -ForegroundColor DarkGray
        }
    }

    Write-Host "[4/5] 安装 $target" -ForegroundColor Cyan
    Copy-Item $built (Join-Path $mods $target) -Force
    Get-ChildItem $mods -Filter 'masaextra*.jar' | ForEach-Object { "  $($_.Name)  $($_.Length) bytes" }
}

function Start-Game {
    if ($NoLaunch) {
        Write-Host '[5/5] 跳过启动' -ForegroundColor DarkGray
        return
    }
    Write-Host '[5/5] 启动 PCL' -ForegroundColor Cyan
    if (-not (Test-Path $launcher)) { throw "找不到启动器 $launcher" }
    if (-not (Get-Process -Name 'Plain Craft Launcher 2' -ErrorAction SilentlyContinue)) {
        Start-Process -FilePath $launcher -WorkingDirectory 'C:\Users\Danil\Desktop'
        Write-Host '  PCL 已启动，请在启动器中点“启动游戏”' -ForegroundColor Yellow
    } else {
        Write-Host '  PCL 已在运行，请在启动器中点“启动游戏”' -ForegroundColor Yellow
    }
}

Stop-Game
Build-Jar
Install-Jar
Start-Game
Write-Host '完成' -ForegroundColor Green
