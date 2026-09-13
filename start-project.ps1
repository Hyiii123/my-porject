<#
.SYNOPSIS
    智问学伴 (Tianji-Share) 全栈项目一键启停与状态运维脚本 (Windows 本地端)

.DESCRIPTION
    本脚本在本地执行，通过阿里云 Workbench CLI 远程调度 ECS 云端微服务集群。
    遵循项目智能体守则：
      - 严禁在本地拉起 Java/Docker 实例，全部运算闭环在云端 ECS。
      - 每日自动校验并感知云端 ECS 动态公网 IP。
      - 采用分层阶梯式启动，保护 ECS 云盘 IOPS，杜绝 CPU 争抢。

.EXAMPLE
    .\start-project.ps1             # 默认一键启动全套微服务及前端
    .\start-project.ps1 -Status     # 查看运行状态与各端公网访问地址
    .\start-project.ps1 -Stop       # 安全停止云端全部容器 (保留持久化数据卷)
    .\start-project.ps1 -Restart    # 重启全套服务
#>

param(
    [Parameter(Position = 0)]
    [ValidateSet("start", "stop", "restart", "status")]
    [string]$Action = "start",

    [string]$InstanceId = "i-f8z1loc07p8p5ve8c7jf",

    [switch]$Stop,
    [switch]$Restart,
    [switch]$Status
)

# 处理 switch 参数映射
if ($Stop) { $Action = "stop" }
elseif ($Restart) { $Action = "restart" }
elseif ($Status) { $Action = "status" }

# 检查本地环境是否已就绪 workbench 命令
$workbenchCmd = Get-Command "workbench" -ErrorAction SilentlyContinue
if (-not $workbenchCmd) {
    Write-Host "[ERROR] 未检测到 workbench CLI 工具，请检查环境变量 PATH 或 D:\nodejs_global\workbench.exe" -ForegroundColor Red
    exit 1
}

Write-Host "==============================================================================" -ForegroundColor DarkCyan
Write-Host "   🌟 智问学伴 (Tianji-Share) 远程服务器控制台 (ECS: $InstanceId)" -ForegroundColor Cyan
Write-Host "==============================================================================" -ForegroundColor DarkCyan

# 每日公网 IP 校验 (Rule 7)
Write-Host "[1/2] 正在校验阿里云 ECS 实时公网 IP..." -ForegroundColor Gray
$pubIp = ""
try {
    $ipOutput = & workbench exec --instance-id $InstanceId --command "curl -s --connect-timeout 3 http://100.100.100.200/latest/meta-data/eipv4"
    if ($LASTEXITCODE -eq 0 -and $ipOutput) {
        $pubIp = ($ipOutput | Out-String).Trim()
        # 过滤多余文本
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
Write-Host "[2/2] 正在远程执行指令: [$Action]..." -ForegroundColor Gray
Write-Host ""

$remoteCmd = "/opt/tianji/share-parent/scripts/manage-project.sh $Action"
# 启动流程含分层等待+健康检查+Nacos注册轮询，需要约2~4分钟，设置5分钟超时
& workbench exec --instance-id $InstanceId --timeout 300 --command "$remoteCmd"

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] 远程执行遇到错误，退出码: $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}

if ($Action -eq "start" -or $Action -eq "status") {
    Write-Host ""
    Write-Host "==============================================================================" -ForegroundColor DarkCyan
    Write-Host "   🎉 访问链接汇总 (可在终端按住 Ctrl 点击打开):" -ForegroundColor Green
    Write-Host "   • 学生端门户 (Portal)         : http://$($pubIp):18081" -ForegroundColor Yellow
    Write-Host "   • 机构/运营管理端 (Business)  : http://$($pubIp):18082" -ForegroundColor Yellow
    Write-Host "   • 若依基础管理端 (RuoYi)      : http://$($pubIp):18080" -ForegroundColor Yellow
    Write-Host "   • API 网关统一入口            : http://$($pubIp):8080" -ForegroundColor Cyan
    Write-Host "   • Nacos 配置与注册中心        : http://$($pubIp):8848/nacos  (nacos / nacos)" -ForegroundColor Cyan
    Write-Host "==============================================================================" -ForegroundColor DarkCyan
}
