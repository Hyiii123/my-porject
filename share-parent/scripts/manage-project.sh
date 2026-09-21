#!/bin/bash
# ==============================================================================
# 智问学伴 (Zhiwen-Share) 全栈项目运维与一键启停脚本 (升级版)
# 适用环境: 阿里云 ECS Linux (Ubuntu/Debian/CentOS)
# 目录: /opt/tianji/share-parent
# ==============================================================================

export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH
set -o pipefail

BASE_DIR="/opt/tianji/share-parent"
cd "$BASE_DIR" || { echo "[ERROR] 无法进入项目目录 $BASE_DIR"; exit 1; }

# 颜色定义
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
BOLD='\033[1m'

log_info() {
    echo -e "${CYAN}[INFO] $(date '+%Y-%m-%d %H:%M:%S')${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS] $(date '+%Y-%m-%d %H:%M:%S')${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN] $(date '+%Y-%m-%d %H:%M:%S')${NC} $1"
}

log_err() {
    echo -e "${RED}[ERROR] $(date '+%Y-%m-%d %H:%M:%S')${NC} $1"
}

# 获取公网 IP (每日公网 IP 校验与动态获取 Rule 7)
get_public_ip() {
    local ip
    ip=$(curl -s --connect-timeout 2 http://100.100.100.200/latest/meta-data/eipv4 2>/dev/null)
    if [ -z "$ip" ]; then
        ip=$(curl -s --connect-timeout 2 https://api.ipify.org 2>/dev/null)
    fi
    if [ -z "$ip" ]; then
        ip="47.121.26.136"
    fi
    echo "$ip"
}

# 标准化服务名称 (移除 zhiwen- 前缀，映射到 docker-compose 服务名)
normalize_service() {
    local name="$1"
    name="${name#zhiwen-}"
    echo "$name"
}

# 将 compose 服务名转换为容器名
service_to_container() {
    local svc="$1"
    svc="${svc#zhiwen-}"
    echo "zhiwen-${svc}"
}

# 等待容器健康就绪
wait_container_healthy() {
    local container_name=$1
    local max_wait=${2:-60}
    local interval=2
    local elapsed=0

    log_info "等待容器 ${container_name} 就绪 (最长等待 ${max_wait}s)..."
    while [ $elapsed -lt $max_wait ]; do
        local status
        status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_name" 2>/dev/null)
        if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then
            log_success "容器 ${container_name} 状态: [${status}]"
            return 0
        fi
        sleep $interval
        elapsed=$((elapsed + interval))
    done

    log_warn "容器 ${container_name} 未在 ${max_wait}s 内就绪 (当前状态: ${status:-unknown})"
    return 1
}

# 等待指定微服务在 Nacos 注册中心上线
wait_nacos_service() {
    local service_name=$1
    local max_wait=${2:-60}
    local interval=3
    local elapsed=0

    log_info "等待核心服务 ${service_name} 注册至 Nacos (最长 ${max_wait}s)..."
    while [ $elapsed -lt $max_wait ]; do
        local nacos_res
        nacos_res=$(curl -s --connect-timeout 2 'http://127.0.0.1:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=50' 2>/dev/null)
        if echo "$nacos_res" | grep -q "\"${service_name}\""; then
            log_success "核心服务 ${service_name} 已成功注册至 Nacos"
            return 0
        fi
        sleep $interval
        elapsed=$((elapsed + interval))
    done
    log_warn "核心服务 ${service_name} 暂未在 Nacos 现身 (超时 ${max_wait}s，将在后台继续初始化)"
    return 1
}

# 启动全套项目服务或单个微服务
start_project() {
    local target_svc="$1"
    if [ -n "$target_svc" ]; then
        local svc
        svc=$(normalize_service "$target_svc")
        local container
        container=$(service_to_container "$svc")
        log_info "正在启动单个服务: ${svc} (容器: ${container})..."
        docker compose -p zhiwen-share up -d "$svc"
        wait_container_healthy "$container" 45
        log_success "单个服务 ${svc} 启动完成。"
        return
    fi

    echo -e "\n${BOLD}==============================================================================${NC}"
    echo -e "${GREEN}${BOLD}         🚀 智问学伴 (Zhiwen-Share) 全套微服务集群平滑阶梯式启动中...         ${NC}"
    echo -e "${BOLD}==============================================================================${NC}\n"

    # Tier 1: 基础设施 (MySQL, Redis, Qdrant)
    log_info "[Tier 1/5] 正在启动基础数据中间件 (MySQL, Redis, Qdrant)..."
    docker compose -p zhiwen-share up -d mysql redis qdrant
    wait_container_healthy "zhiwen-mysql" 60
    wait_container_healthy "zhiwen-redis" 25
    wait_container_healthy "zhiwen-qdrant" 20

    # Tier 2: 注册配置中心与 AI 推理引擎 (Nacos, Embedding, Recommend)
    log_info "[Tier 2/5] 正在启动配置注册中心与 AI 推理引擎 (Nacos, Embedding, Recommend)..."
    docker compose -p zhiwen-share up -d nacos embedding recommend
    wait_container_healthy "zhiwen-nacos" 60
    wait_container_healthy "zhiwen-embedding" 30
    wait_container_healthy "zhiwen-recommend" 20

    # Tier 3: 认证鉴权与系统权限核心 (Auth, System)
    # 率先完成核心鉴权组件类加载，规避 7 个 JVM 同时争抢 2 vCPU
    log_info "[Tier 3/5] 正在启动认证鉴权与系统核心微服务 (Auth, System)..."
    docker compose -p zhiwen-share up -d auth system
    wait_nacos_service "share-auth" 50
    wait_nacos_service "share-system" 50

    # Tier 4: API 网关与三大 Web 前端 (Gateway, Portal UI, Business UI, RuoYi UI)
    log_info "[Tier 4/5] 正在启动统一 API 网关与前端界面 (Gateway, Portal UI, Business UI, RuoYi UI)..."
    docker compose -p zhiwen-share up -d gateway portal-ui business-admin-ui ruoyi-ui
    wait_nacos_service "share-gateway" 45

    # Tier 5: 业务微服务错峰分批启动 (避免 CPU Starvation 与类加载风暴)
    log_info "[Tier 5/5] 正在分批启动业务微服务 (第 1 批: File, Customer)..."
    docker compose -p zhiwen-share up -d file customer
    sleep 8
    log_info "[Tier 5/5] 正在分批启动业务微服务 (第 2 批: Trade, Education)..."
    docker compose -p zhiwen-share up -d trade education

    # 动态智能轮询检测 Nacos 微服务注册状态
    echo -e "\n${CYAN}[CHECK] 正在智能轮询 Nacos 核心微服务注册状态 (目标: 7 个核心微服务)...${NC}"
    local max_poll=360
    local poll_interval=4
    local elapsed=0
    local target_count=7
    local current_count=0
    local target_services=("share-gateway" "share-system" "share-auth" "share-education" "share-trade" "share-customer" "share-file")
    local missing_svcs=()

    while [ $elapsed -lt $max_poll ]; do
        local nacos_res
        nacos_res=$(curl -s --connect-timeout 2 'http://127.0.0.1:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=50' 2>/dev/null)
        missing_svcs=()
        for s in "${target_services[@]}"; do
            if ! echo "$nacos_res" | grep -q "\"$s\""; then
                missing_svcs+=("$s")
            fi
        done

        current_count=$((target_count - ${#missing_svcs[@]}))
        if [ ${#missing_svcs[@]} -eq 0 ]; then
            echo ""
            log_success "全部 7 个核心微服务已顺利完成类加载并在 Nacos 注册上线！"
            break
        fi

        local missing_str="${missing_svcs[*]}"
        echo -ne "  ⏳ 注册进度: [${current_count}/${target_count}] | 正在装配: [${missing_str}] (已耗时: ${elapsed}s / 上限: ${max_poll}s)   \r"
        sleep $poll_interval
        elapsed=$((elapsed + poll_interval))
    done
    echo ""

    if [ ${#missing_svcs[@]} -gt 0 ]; then
        log_warn "以下微服务在 ${max_poll}s 轮询结束时仍在后台装配中: ${missing_svcs[*]}"
        echo -e "  ${YELLOW}💡 提示: 2 vCPU 环境下 Spring AI/重型模块加载较慢。若后续未就绪，可执行: ./start-project.ps1 restart <服务名>${NC}"
    fi

    # 汇报最终运行状态与健康检查
    health_check
    status_project
}

# 停止全套或单个服务容器
stop_project() {
    local target_svc="$1"
    if [ -n "$target_svc" ]; then
        local svc
        svc=$(normalize_service "$target_svc")
        log_info "正在停止单个服务: ${svc}..."
        docker compose -p zhiwen-share stop "$svc"
        log_success "服务 ${svc} 已停止。"
        return
    fi

    echo -e "\n${YELLOW}${BOLD}正在安全停止智问学伴微服务集群 (严格保留 MySQL/Redis/Nacos 数据持久卷)...${NC}"
    docker compose -p zhiwen-share stop
    log_success "全部 16 个项目容器已安全停止。"
}

# 重启全套或单个服务
restart_project() {
    local target_svc="$1"
    if [ -n "$target_svc" ]; then
        local svc
        svc=$(normalize_service "$target_svc")
        local container
        container=$(service_to_container "$svc")
        log_info "正在重启单个服务: ${svc} (容器: ${container})..."
        docker compose -p zhiwen-share restart "$svc"
        wait_container_healthy "$container" 45
        log_success "服务 ${svc} 重启成功。"
        return
    fi

    log_info "正在执行全套集群平滑重启流程..."
    stop_project
    sleep 3
    start_project
}

# 自动化健康体检
health_check() {
    local pub_ip
    pub_ip=$(get_public_ip)

    echo -e "\n${BOLD}==============================================================================${NC}"
    echo -e "${GREEN}${BOLD}                    🏥 智问学伴 (Zhiwen-Share) 端到端健康体检                  ${NC}"
    echo -e "${BOLD}==============================================================================${NC}"

    check_http() {
        local name="$1"
        local url="$2"
        local expected="${3:-200}"
        local code
        code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "$url" 2>/dev/null)
        if [ "$code" = "$expected" ]; then
            printf "  %-32s %-38s [ ${GREEN}PASS (%s)${NC} ]\n" "$name" "$url" "$code"
        elif [ "$code" != "000" ] && [ -n "$code" ]; then
            printf "  %-32s %-38s [ ${YELLOW}WARN (%s)${NC} ]\n" "$name" "$url" "$code"
        else
            printf "  %-32s %-38s [ ${RED}FAIL (DOWN)${NC} ]\n" "$name" "$url"
        fi
    }

    echo -e "${BOLD}【核心入口与前端 UI】${NC}"
    check_http "网关统一入口 (Gateway)" "http://127.0.0.1:8080/actuator/health" "200"
    check_http "学生端门户 (Portal UI)" "http://127.0.0.1:18081/" "200"
    check_http "运营管理端 (Business UI)" "http://127.0.0.1:18082/" "200"
    check_http "若依管理端 (RuoYi UI)" "http://127.0.0.1:18080/" "200"

    echo -e "\n${BOLD}【中枢配置与 AI 算法】${NC}"
    check_http "配置注册中心 (Nacos)" "http://127.0.0.1:8848/nacos/" "200"
    check_http "AI 向量检索 (Embedding)" "http://127.0.0.1:18000/health" "200"
    check_http "Qdrant 向量库 (Qdrant)" "http://127.0.0.1:16333/collections" "200"
    check_http "AI 推荐算法 (Recommend)" "http://127.0.0.1:15000/api/recommend/health" "200"

    echo -e "\n${BOLD}【Java 核心微服务直连探针】${NC}"
    check_http "认证授权中心 (Auth)" "http://127.0.0.1:19200/actuator/health" "200"
    check_http "系统管理中枢 (System)" "http://127.0.0.1:19201/actuator/health" "200"
    check_http "核心教育平台 (Education)" "http://127.0.0.1:19210/actuator/health" "200"
    check_http "学员用户中心 (Customer)" "http://127.0.0.1:19206/actuator/health" "200"
    check_http "交易结算中心 (Trade)" "http://127.0.0.1:19211/actuator/health" "200"
    check_http "文件对象存储 (File)" "http://127.0.0.1:19300/actuator/health" "200"

    echo -e "\n${BOLD}【网关业务路由连通性】${NC}"
    check_http "网关路由 -> 教育智能体" "http://127.0.0.1:8080/cs/courses/recommendations/evals/metrics" "200"
    echo -e "==============================================================================\n"
}

# 状态巡检与地址汇总
status_project() {
    local pub_ip
    pub_ip=$(get_public_ip)

    echo -e "\n${BOLD}==============================================================================${NC}"
    echo -e "${GREEN}${BOLD}                    📊 智问学伴 (Zhiwen-Share) 运行状态概览                  ${NC}"
    echo -e "${BOLD}==============================================================================${NC}"
    echo -e "服务器公网 IP : ${CYAN}${pub_ip}${NC}"
    echo -e "系统当前负载   : $(uptime | awk -F'load average:' '{ print $2 }')"
    echo -e "内存使用统计   : $(free -h | awk '/Mem:/ {print "已用: " $3 " / 总共: " $2 " (可用: " $7 ")"}')"
    echo ""

    echo -e "${BOLD}【Docker 容器运行列表】${NC}"
    docker ps --filter "name=zhiwen-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    echo ""

    echo -e "${BOLD}【Nacos 微服务注册列表】${NC}"
    local nacos_res
    nacos_res=$(curl -s --connect-timeout 2 'http://127.0.0.1:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=50' 2>/dev/null)
    if [ -n "$nacos_res" ]; then
        echo -e "  注册清单: ${CYAN}${nacos_res}${NC}"
    else
        echo -e "  ${RED}[WARN] 无法连接 Nacos 8848 端口${NC}"
    fi
    echo ""

    echo -e "${BOLD}【各端公网访问入口】${NC}"
    echo -e "  🎯 学生端门户 (Portal UI)         : ${GREEN}http://${pub_ip}:18081${NC}"
    echo -e "  🏢 机构/运营管理端 (Business UI)  : ${GREEN}http://${pub_ip}:18082${NC}"
    echo -e "  ⚙️ 若依系统管理端 (RuoYi UI)      : ${GREEN}http://${pub_ip}:18080${NC}"
    echo -e "  🌐 API 统一网关 Gateway           : ${CYAN}http://${pub_ip}:8080${NC}"
    echo -e "  🧭 Nacos 控制台 (nacos/nacos)     : ${CYAN}http://${pub_ip}:8848/nacos${NC}"
    echo -e "  🤖 AI 推荐算法微服务 (DRAG-KP4SR) : ${YELLOW}http://${pub_ip}:15000/api/recommend/predict${NC}"
    echo -e "  🧠 AI 向量检索服务 (FastAPI)      : ${YELLOW}http://${pub_ip}:18000/health${NC}"
    echo -e "  📦 Qdrant 向量数据库             : ${YELLOW}http://${pub_ip}:16333/collections${NC}"
    echo -e "==============================================================================\n"
}

# 查看日志快捷方式
logs_service() {
    local svc=$1
    if [ -z "$svc" ]; then
        echo "请指定要查看日志的服务或容器名称，如: ./manage-project.sh logs gateway"
        echo "可选容器: $(docker ps --filter 'name=zhiwen-' --format '{{.Names}}' | tr '\n' ' ')"
        return
    fi
    local container
    if [[ "$svc" == zhiwen-* ]]; then
        container="$svc"
    else
        container="zhiwen-${svc}"
    fi
    docker logs --tail 100 "$container"
}

# 命令行入口调度
case "$1" in
    start)
        start_project "$2"
        ;;
    stop)
        stop_project "$2"
        ;;
    restart)
        restart_project "$2"
        ;;
    status)
        status_project
        ;;
    health|check)
        health_check
        ;;
    logs)
        logs_service "$2"
        ;;
    *)
        echo -e "${BOLD}用法:${NC} $0 {start|stop|restart|status|health|logs [service_name]}"
        echo -e "  ${GREEN}start [svc]${NC}   : 平滑阶梯式启动全套集群，或单独启动指定微服务"
        echo -e "  ${YELLOW}stop [svc]${NC}    : 安全停止全套容器，或单独停止指定微服务"
        echo -e "  ${CYAN}restart [svc]${NC} : 重启全套服务，或单独重启指定微服务"
        echo -e "  ${GREEN}status${NC}        : 查看当前运行状态、系统负载及访问地址"
        echo -e "  ${GREEN}health${NC}        : 执行端到端各微服务及 UI 端口健康体检"
        echo -e "  ${CYAN}logs <svc>${NC}    : 查看指定容器最近 100 行日志 (例如: $0 logs gateway)"
        exit 1
        ;;
esac
