<#
.SYNOPSIS
    智问学伴 (Zhiwen-Share) 全栈项目一键启停与状态运维脚本 (Windows 本地端)

.DESCRIPTION
    本脚本在本地执行，通过阿里云 Workbench CLI 远程调度 ECS 云端微服务集群。
    遵循项目智能体守则：
      - 严禁在本地拉起 Java/Docker 实例，全部运算闭环在云端 ECS (Rule 5)。
      - 每日自动校验并感知云端 ECS 动态公网 IP (Rule 7)。
      - 采用分层阶梯式平滑启动，保护 ECS 云盘 IOPS，规避 CPU 尖峰 (Rule 2)。
      - 支持全局或针对单微服务进行启停、重启与日志追踪，杜绝无谓的全局重启 (Rule 3, 8)。

.EXAMPLE
    .\start-project.ps1                   # 默认一键启动全套微服务集群及前端
    .\start-project.ps1 -Status           # 查看运行状态、系统负载及各端公网访问地址
    .\start-project.ps1 -Health           # 执行端到端各微服务及 UI 端口健康体检
    .\start-project.ps1 -Stop             # 安全停止云端全部容器 (保留持久化数据卷)
    .\start-project.ps1 -Restart          # 重启全套微服务集群
    .\start-project.ps1 restart education # 仅重启教育核心微服务 (精准单服务运维)
    .\start-project.ps1 logs gateway      # 查看 API 网关最新日志
#>

param(
    [Parameter(Position = 0)]
    [ValidateSet("start", "stop", "restart", "status", "health", "check", "logs")]
    [string]$Action = "start",

    [Parameter(Position = 1)]
    [string]$Service = "",

    [string]$InstanceId = "i-f8z1loc07p8p5ve8c7jf",

    [switch]$Stop,
    [switch]$Restart,
    [switch]$Status,
    [switch]$Health,
    [switch]$Logs
)

# 处理 switch 参数映射
if ($Stop) { $Action = "stop" }
elseif ($Restart) { $Action = "restart" }
elseif ($Status) { $Action = "status" }
elseif ($Health) { $Action = "health" }
elseif ($Logs) { $Action = "logs" }

# 检查本地环境是否已就绪 workbench 命令
$workbenchCmd = Get-Command "workbench" -ErrorAction SilentlyContinue
if (-not $workbenchCmd) {
    Write-Host "[ERROR] 未检测到 workbench CLI 工具，请检查环境变量 PATH 或 D:\nodejs_global\workbench.exe" -ForegroundColor Red
    exit 1
}

Write-Host "==============================================================================" -ForegroundColor DarkCyan
Write-Host "   🌟 智问学伴 (Zhiwen-Share) 远程服务器控制台 (ECS: $InstanceId)" -ForegroundColor Cyan
Write-Host "==============================================================================" -ForegroundColor DarkCyan

# 每日公网 IP 校验 (Rule 7)
Write-Host "[1/2] 正在校验阿里云 ECS 实时公网 IP..." -ForegroundColor Gray
$pubIp = ""
try {
    $ipOutput = & workbench exec --instance-id $InstanceId --command "curl -s --connect-timeout 3 http://100.100.100.200/latest/meta-data/eipv4"
    if ($LASTEXITCODE -eq 0 -and $ipOutput) {
        $pubIp = ($ipOutput | Out-String).Trim()
        if ($pubIp -match "([0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3})") {
            $pubIp = $Matches[1]
        }
    }
} catch {
    Write-Host "[WARN] 无法获取元数据公网 IP，回退至默认 IP" -ForegroundColor Yellow
}

if (-not $pubIp) {
    $pubIp = "47.120.67.187"
}
Write-Host "      ECS 公网 IP: $pubIp" -ForegroundColor Green

# 远程调度服务端管理脚本
$targetDesc = if ($Service) { "[$Action $Service]" } else { "[$Action]" }
Write-Host "[2/2] 正在远程执行指令: $targetDesc..." -ForegroundColor Gray
Write-Host ""

$remoteCmd = "/opt/tianji/share-parent/scripts/manage-project.sh $Action"
if ($Service) {
    $remoteCmd += " $Service"
}

# 全量启动需要阶梯式等待+健康检查+Nacos注册，设置400s超时；单服务或巡检命令使用120s
$timeoutSeconds = 120
if ($Action -eq "start" -or $Action -eq "restart") {
    if (-not $Service) {
        $timeoutSeconds = 400
    }
}

& workbench exec --instance-id $InstanceId --timeout $timeoutSeconds --command "$remoteCmd"

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] 远程执行遇到错误，退出码: $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}

if ($Action -eq "start" -or $Action -eq "status" -or $Action -eq "health" -or $Action -eq "check") {
    Write-Host ""
    Write-Host "==============================================================================" -ForegroundColor DarkCyan
    Write-Host "   🎉 访问链接汇总 (可在终端按住 Ctrl 点击打开):" -ForegroundColor Green
    Write-Host "   • 学生端门户 (Portal)            : http://$($pubIp):18081" -ForegroundColor Yellow
    Write-Host "   • 机构/运营管理端 (Business)     : http://$($pubIp):18082" -ForegroundColor Yellow
    Write-Host "   • 若依基础管理端 (RuoYi)         : http://$($pubIp):18080" -ForegroundColor Yellow
    Write-Host "   • API 网关统一入口               : http://$($pubIp):8080" -ForegroundColor Cyan
    Write-Host "   • Nacos 配置与注册中心           : http://$($pubIp):8848/nacos  (nacos / nacos)" -ForegroundColor Cyan
    Write-Host "   • 推荐算法服务 (Recommend)       : http://$($pubIp):15000/api/recommend/predict" -ForegroundColor DarkGray
    Write-Host "   • 向量检索知识库 (Embedding)     : http://$($pubIp):18000/health" -ForegroundColor DarkGray
    Write-Host "==============================================================================" -ForegroundColor DarkCyan
}
