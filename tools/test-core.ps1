$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$testOutput = Join-Path $projectRoot 'app/build/core-tests'
New-Item -ItemType Directory -Force -Path $testOutput | Out-Null
& javac -encoding UTF-8 -d $testOutput (Join-Path $projectRoot 'app/src/main/java/com/qingping/app/SkipPolicy.java') (Join-Path $PSScriptRoot 'PolicyChecks.java')
if ($LASTEXITCODE -ne 0) { throw 'Core test compilation failed' }
& java -cp $testOutput com.qingping.app.PolicyChecks
if ($LASTEXITCODE -ne 0) { throw 'Core policy checks failed' }
