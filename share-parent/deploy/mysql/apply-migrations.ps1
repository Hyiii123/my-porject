[CmdletBinding()]
param(
    [string]$ComposeProject = 'tianji-share',
    [string]$MysqlService = 'mysql'
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$migrationDirectory = Join-Path $projectRoot 'sql\migrations'

if (-not (Test-Path -LiteralPath $migrationDirectory)) {
    throw "迁移目录不存在：$migrationDirectory"
}

docker compose -p $ComposeProject ps --status running $MysqlService | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "MySQL 服务未运行，请先执行：docker compose -p $ComposeProject up -d mysql"
}

$migrationFiles = Get-ChildItem -LiteralPath $migrationDirectory -Filter 'V*.sql' -File |
    Sort-Object @{ Expression = {
            if ($_.BaseName -match '^V(\d+)') { [int]$Matches[1] } else { [int]::MaxValue }
        } }, Name
if ($migrationFiles.Count -eq 0) {
    Write-Host '没有找到待执行的 SQL 迁移文件。'
    exit 0
}

# 使用容器内的 MYSQL_ROOT_PASSWORD，避免把密码写进命令行或提交到仓库。
# 不能直接使用“Get-Content | docker ...”：Windows PowerShell 会按当前代码页
# 把管道字符串重新编码，中文 SQL 在容器中会变成 ???。这里通过进程标准输入
# 写入原始 UTF-8 字节，确保迁移脚本中的中文不丢失。
$mysqlCommand = 'mysql -uroot -p$MYSQL_ROOT_PASSWORD --default-character-set=utf8mb4'
foreach ($migrationFile in $migrationFiles) {
    Write-Host ("执行 {0}" -f $migrationFile.Name)
    $process = [System.Diagnostics.Process]::new()
    # 这里的参数均来自脚本固定值或简单服务名，使用单独的参数字符串
    # 兼容 Windows PowerShell / .NET Framework，不再依赖 ArgumentList。
    $dockerArguments = 'compose -p "{0}" exec -T "{1}" sh -c "{2}"' -f `
        $ComposeProject, $MysqlService, $mysqlCommand
    $process.StartInfo = [System.Diagnostics.ProcessStartInfo]::new('docker', $dockerArguments)
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.RedirectStandardInput = $true
    $process.StartInfo.RedirectStandardOutput = $true
    $process.StartInfo.RedirectStandardError = $true

    [void]$process.Start()
    $bytes = [System.IO.File]::ReadAllBytes($migrationFile.FullName)
    $process.StandardInput.BaseStream.Write($bytes, 0, $bytes.Length)
    $process.StandardInput.Close()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    $exitCode = $process.ExitCode
    $process.Dispose()

    if ($stdout) { Write-Host $stdout.TrimEnd() }
    if ($stderr) { Write-Warning $stderr.TrimEnd() }
    if ($exitCode -ne 0) {
        throw "迁移失败：$($migrationFile.Name)"
    }
}

Write-Host ("已执行 {0} 个幂等迁移文件。" -f $migrationFiles.Count)
