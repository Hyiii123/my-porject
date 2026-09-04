[CmdletBinding()]
param(
    [string]$ComposeProject = 'tianji-share',
    [string]$MysqlService = 'mysql',
    [switch]$LegacyManualMode
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$migrationComposeFile = Join-Path $projectRoot 'docker-compose.migrate.yml'
if (-not (Test-Path -LiteralPath $migrationComposeFile)) {
    throw "Flyway 编排文件不存在：$migrationComposeFile"
}

docker compose -p $ComposeProject ps --status running $MysqlService | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "MySQL 服务未运行，请先执行：docker compose -p $ComposeProject up -d mysql"
}

if ($LegacyManualMode) {
    Write-Warning 'LegacyManualMode 已废弃；当前迁移统一由 Flyway 记录版本。'
}

Write-Host '开始执行 Flyway 版本迁移（一次性任务，不重启业务服务）。'
docker compose -p $ComposeProject -f (Join-Path $projectRoot 'docker-compose.yml') -f $migrationComposeFile run --rm db-migrate
if ($LASTEXITCODE -ne 0) {
    throw 'Flyway 数据库迁移失败。'
}
Write-Host 'Flyway 数据库迁移完成。'
