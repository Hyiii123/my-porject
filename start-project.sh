#!/usr/bin/env bash
# ==============================================================================
# 智问学伴 (Zhiwen-Share) 项目启动与状态管理脚本 (Bash / WSL / Git Bash)
# ==============================================================================

set -euo pipefail

INSTANCE_ID="i-f8z1loc07p8p5ve8c7jf"
ACTION="${1:-start}"
SERVICE="${2:-}"

echo "=============================================================================="
echo "   🌟 智问学伴 (Zhiwen-Share) 远程服务器控制台 (ECS: ${INSTANCE_ID})"
echo "=============================================================================="

# 检查 workbench
if ! command -v workbench &>/dev/null; then
    if [ -f "/d/nodejs_global/workbench.exe" ]; then
        WORKBENCH_BIN="/d/nodejs_global/workbench.exe"
    elif [ -f "D:/nodejs_global/workbench.exe" ]; then
        WORKBENCH_BIN="D:/nodejs_global/workbench.exe"
    else
        echo "[ERROR] 未检测到 workbench CLI 工具，请检查 PATH 环境变量" >&2
        exit 1
    fi
else
    WORKBENCH_BIN="workbench"
fi

# 获取公网 IP
echo "[1/2] 正在校验阿里云 ECS 实时公网 IP..."
PUB_IP="$("$WORKBENCH_BIN" exec --instance-id "${INSTANCE_ID}" --command "curl -s --connect-timeout 3 http://100.100.100.200/latest/meta-data/eipv4" 2>/dev/null | tr -d '\r\n' || true)"
if [ -z "$PUB_IP" ] || [[ ! "$PUB_IP" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    PUB_IP="47.120.67.187"
fi
echo "      ECS 公网 IP: ${PUB_IP}"

# 执行远程指令
CMD="/opt/tianji/share-parent/scripts/manage-project.sh ${ACTION}"
if [ -n "$SERVICE" ]; then
    CMD="${CMD} ${SERVICE}"
    echo "[2/2] 正在远程执行指令: [${ACTION} ${SERVICE}]..."
else
    echo "[2/2] 正在远程执行指令: [${ACTION}]..."
fi
echo ""

TIMEOUT=120
if [ "$ACTION" = "start" ] || [ "$ACTION" = "restart" ]; then
    if [ -z "$SERVICE" ]; then
        TIMEOUT=400
    fi
fi

"$WORKBENCH_BIN" exec --instance-id "${INSTANCE_ID}" --timeout "${TIMEOUT}" --command "${CMD}"

if [ "$ACTION" = "start" ] || [ "$ACTION" = "status" ] || [ "$ACTION" = "health" ] || [ "$ACTION" = "check" ]; then
    echo ""
    echo "=============================================================================="
    echo "   🎉 访问链接汇总 (可在终端按住 Ctrl 点击打开):"
    echo "   • 学生端门户 (Portal)            : http://${PUB_IP}:18081"
    echo "   • 机构/运营管理端 (Business)     : http://${PUB_IP}:18082"
    echo "   • 若依基础管理端 (RuoYi)         : http://${PUB_IP}:18080"
    echo "   • API 网关统一入口               : http://${PUB_IP}:8080"
    echo "   • Nacos 配置与注册中心           : http://${PUB_IP}:8848/nacos  (nacos / nacos)"
    echo "   • 推荐算法服务 (Recommend)       : http://${PUB_IP}:15000/api/recommend/predict"
    echo "   • 向量检索知识库 (Embedding)     : http://${PUB_IP}:18000/health"
    echo "=============================================================================="
fi
