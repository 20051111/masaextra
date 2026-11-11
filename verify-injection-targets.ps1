# Verifies that every @Inject/@ModifyVariable/@WrapOperation target used by
# masaextra mixins is a method the TARGET CLASS ITSELF declares.
#
# Mixin cannot inject into a method a class only inherits. That mistake shipped
# once (LeverBlock#setPlacedBy is inherited from Block in 1.20.4) and crashed
# startup, so this check is run before every release build.

$root = Split-Path $PSScriptRoot -Parent
$javap = Join-Path $root '_build\jdk25\jdk-25.0.5.0.2+1\bin\javap.exe'
$named = Join-Path $root '_build\gradle-home\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged\1.20.4-loom.mappings.1_20_4.layered+hash.2198-v2\minecraft-merged-1.20.4-loom.mappings.1_20_4.layered+hash.2198-v2.jar'
$proj = Join-Path $root '_build\src\masaextra-1.20.4'
$srcDir = Join-Path $proj 'src\main\java\com\example\masaextra\mixin'

# mixin class -> (target FQN, list of injected method names)
$checks = @(
    @{ F='MixinLeverBlockVanillaProtocol'; T='net.minecraft.world.level.block.LeverBlock'; M=@() }
)

$problems = 0
Get-ChildItem $srcDir -Recurse -Filter '*.java' | ForEach-Object {
    $text = Get-Content $_.FullName -Raw

    # target class from @Mixin(...)
    $target = $null
    if ($text -match '@Mixin\(\s*([\w.]+)\.class\s*\)') { $target = $Matches[1] }
    elseif ($text -match '@Mixin\(\s*targets\s*=\s*"([^"]+)"\s*\)') { $target = ($Matches[1] -replace '/', '.') }
    if (-not $target) { return }

    # every method = "name" inside @Inject / @ModifyVariable / @WrapOperation
    $methods = [regex]::Matches($text, 'method\s*=\s*"([^"]+)"') |
        ForEach-Object { $_.Groups[1].Value } |
        ForEach-Object {
            if ($_ -match '^L[^;]+;([\w<>]+)\(') { $Matches[1] }   # full descriptor form
            elseif ($_ -match '^([\w]+)[\(;]') { $Matches[1] }     # plain name or name(...)
            else { $_ }
        } | Sort-Object -Unique
    if (-not $methods) { return }

    $out = & $javap -p -classpath $named $target 2>&1 | Out-String
    if ($out -match 'Error|错误') { Write-Host ("MISSING TARGET CLASS {0} ({1})" -f $target, $_.Name) -ForegroundColor Red; $script:problems++; return }

    # methods actually declared on the class (javap prints declared members only)
    $declared = [regex]::Matches($out, '^\s+(?:public|protected|private|static|final|abstract|\s)*[\w.<>\[\]]+\s+([\w<>]+)\s*\(', 'Multiline') |
        ForEach-Object { $_.Groups[1].Value }

    foreach ($m in $methods) {
        if ($declared -notcontains $m) {
            Write-Host ("NOT DECLARED  {0}: '{1}' is not declared by {2}" -f $_.Name, $m, $target) -ForegroundColor Red
            $script:problems++
        }
    }
}

if ($problems -eq 0) { Write-Host 'All injection targets are declared on their target classes.' -ForegroundColor Green }
else { Write-Host ("{0} problem(s) found." -f $problems) -ForegroundColor Red; exit 1 }
