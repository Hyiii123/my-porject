<template>
  <div class="app-container home">
    <!-- 顶部概览 Banner -->
    <div class="overview-banner">
      <div class="banner-content">
        <div class="banner-badge">
          <span class="status-dot"></span>
          <span>微服务集群状态：正常运行</span>
        </div>
        <h1 class="banner-title">智问学伴 · 在线教育管理平台</h1>
        <p class="banner-desc">
          统一基础服务治理控制台，提供用户鉴权、角色权限、微服务路由、系统监控与基础数据支撑能力。
        </p>
        <div class="banner-actions">
          <el-button type="primary" @click="goBusinessAdmin">
            <el-icon><Monitor /></el-icon>
            进入业务管理端
          </el-button>
          <el-button @click="goPortal">
            <el-icon><Reading /></el-icon>
            访问学员门户端
          </el-button>
          <el-button @click="goRoute('/monitor/server')">
            <el-icon><Cpu /></el-icon>
            服务监控
          </el-button>
        </div>
      </div>
      <div class="banner-meta">
        <div class="meta-item">
          <span class="meta-label">系统版本</span>
          <span class="meta-value">v{{ version }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">服务架构</span>
          <span class="meta-value">Spring Cloud</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">前端栈</span>
          <span class="meta-value">Vue 3 + Vite</span>
        </div>
      </div>
    </div>

    <!-- 关键状态卡片 -->
    <div class="metric-grid">
      <div class="metric-card">
        <div class="metric-icon blue">
          <el-icon><Connection /></el-icon>
        </div>
        <div class="metric-info">
          <div class="metric-value">4 个核心服务</div>
          <div class="metric-label">网关、认证、课程业务、AI 客服</div>
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-icon green">
          <el-icon><Select /></el-icon>
        </div>
        <div class="metric-info">
          <div class="metric-value">3 端一体协同</div>
          <div class="metric-label">管理端、业务后台、学员门户</div>
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-icon orange">
          <el-icon><Lock /></el-icon>
        </div>
        <div class="metric-info">
          <div class="metric-value">JWT + RBAC</div>
          <div class="metric-label">多级角色与端侧安全鉴权</div>
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-icon purple">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="metric-info">
          <div class="metric-value">Redis + MySQL</div>
          <div class="metric-label">分布式缓存与高可靠持久化</div>
        </div>
      </div>
    </div>

    <!-- 主体两列布局 -->
    <el-row :gutter="20" class="main-row">
      <!-- 左列：微服务集群拓扑与状态 -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="content-card">
          <template #header>
            <div class="card-header">
              <span class="header-title">微服务集群服务治理</span>
              <el-tag type="success" size="small" effect="plain">已接入 Nacos 注册中心</el-tag>
            </div>
          </template>
          <div class="service-table">
            <div v-for="srv in serviceList" :key="srv.name" class="service-item">
              <div class="service-main">
                <div class="service-indicator online"></div>
                <div>
                  <div class="service-name">{{ srv.name }}</div>
                  <div class="service-desc">{{ srv.desc }}</div>
                </div>
              </div>
              <div class="service-meta">
                <span class="service-port">{{ srv.port }}</span>
                <el-tag :type="srv.statusType" size="small">{{ srv.status }}</el-tag>
              </div>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="content-card" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span class="header-title">平台技术架构与规范</span>
            </div>
          </template>
          <div class="tech-grid">
            <div class="tech-group">
              <h4>后端技术体系</h4>
              <div class="tag-wrap">
                <el-tag size="small">Spring Boot 2.7</el-tag>
                <el-tag size="small">Spring Cloud 2021</el-tag>
                <el-tag size="small">Spring Cloud Alibaba</el-tag>
                <el-tag size="small">Nacos 注册与配置</el-tag>
                <el-tag size="small">Sentinel 限流熔断</el-tag>
                <el-tag size="small">MinIO 媒资对象存储</el-tag>
                <el-tag size="small">MySQL 8.0</el-tag>
                <el-tag size="small">Redis 缓存集群</el-tag>
              </div>
            </div>
            <div class="tech-group">
              <h4>前端技术体系</h4>
              <div class="tag-wrap">
                <el-tag size="small" type="success">Vue 3 组合式 API</el-tag>
                <el-tag size="small" type="success">Vite 高速构建</el-tag>
                <el-tag size="small" type="success">Element Plus</el-tag>
                <el-tag size="small" type="success">Pinia / Vuex 状态</el-tag>
                <el-tag size="small" type="success">Sass 样式工程</el-tag>
                <el-tag size="small" type="success">Axios 请求封装</el-tag>
                <el-tag size="small" type="success">Responsive 响应式</el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右列：快捷操作与常用监控入口 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="content-card">
          <template #header>
            <div class="card-header">
              <span class="header-title">系统运维与权限管理</span>
            </div>
          </template>
          <div class="quick-nav-grid">
            <div class="quick-nav-item" @click="goRoute('/system/user')">
              <el-icon class="nav-icon"><User /></el-icon>
              <div class="nav-text">
                <span class="nav-title">用户管理</span>
                <span class="nav-sub">系统账号与部门配置</span>
              </div>
            </div>
            <div class="quick-nav-item" @click="goRoute('/system/role')">
              <el-icon class="nav-icon"><Key /></el-icon>
              <div class="nav-text">
                <span class="nav-title">角色权限</span>
                <span class="nav-sub">岗位权限与菜单授权</span>
              </div>
            </div>
            <div class="quick-nav-item" @click="goRoute('/system/menu')">
              <el-icon class="nav-icon"><Menu /></el-icon>
              <div class="nav-text">
                <span class="nav-title">菜单管理</span>
                <span class="nav-sub">路由树与按钮级权限</span>
              </div>
            </div>
            <div class="quick-nav-item" @click="goRoute('/monitor/online')">
              <el-icon class="nav-icon"><Avatar /></el-icon>
              <div class="nav-text">
                <span class="nav-title">在线用户</span>
                <span class="nav-sub">会话监控与强退管理</span>
              </div>
            </div>
            <div class="quick-nav-item" @click="goRoute('/monitor/job')">
              <el-icon class="nav-icon"><Timer /></el-icon>
              <div class="nav-text">
                <span class="nav-title">定时任务</span>
                <span class="nav-sub">Cron 任务调度与日志</span>
              </div>
            </div>
            <div class="quick-nav-item" @click="goRoute('/monitor/operlog')">
              <el-icon class="nav-icon"><Document /></el-icon>
              <div class="nav-text">
                <span class="nav-title">操作日志</span>
                <span class="nav-sub">系统业务审计追踪</span>
              </div>
            </div>
            <div class="quick-nav-item" @click="goRoute('/monitor/server')">
              <el-icon class="nav-icon"><Cpu /></el-icon>
              <div class="nav-text">
                <span class="nav-title">服务监控</span>
                <span class="nav-sub">JVM、CPU、磁盘状态</span>
              </div>
            </div>
            <div class="quick-nav-item" @click="goRoute('/tool/swagger')">
              <el-icon class="nav-icon"><Link /></el-icon>
              <div class="nav-text">
                <span class="nav-title">接口文档</span>
                <span class="nav-sub">Swagger 接口契约</span>
              </div>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="content-card" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span class="header-title">产品端入口导航</span>
            </div>
          </template>
          <div class="portal-nav-list">
            <div class="portal-item" @click="goBusinessAdmin">
              <div class="portal-info">
                <div class="portal-title">在线教育业务管理端 (Business Admin)</div>
                <div class="portal-desc">课程上架、学员管理、教师录入、订单退款、智能问答与客服</div>
              </div>
              <el-button type="primary" link>打开 &rarr;</el-button>
            </div>
            <div class="portal-item" @click="goPortal">
              <div class="portal-info">
                <div class="portal-title">在线教育学员端门户 (Student Portal)</div>
                <div class="portal-desc">课程浏览、视频播放、章节学习、问答社区、模拟做题与个人中心</div>
              </div>
              <el-button type="primary" link>打开 &rarr;</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Monitor,
  Reading,
  Cpu,
  Connection,
  Select,
  Lock,
  DataLine,
  User,
  Key,
  Menu,
  Avatar,
  Timer,
  Document,
  Link
} from '@element-plus/icons-vue'

const router = useRouter()
const version = ref('3.6.3')

const serviceList = ref([
  {
    name: 'share-gateway (统一微服务网关)',
    desc: '全局鉴权路由、流量控制、白名单过滤、跨域处理',
    port: 'Port: 8080',
    status: '运行中',
    statusType: 'success'
  },
  {
    name: 'share-auth (统一认证中心)',
    desc: '用户身份签权、Token 签发与自动续期、密码加密',
    port: 'Port: 9200',
    status: '运行中',
    statusType: 'success'
  },
  {
    name: 'share-modules-system (系统基础服务)',
    desc: '用户管理、角色权限、菜单字典、系统参数与日志审计',
    port: 'Port: 9201',
    status: '运行中',
    statusType: 'success'
  },
  {
    name: 'share-modules-course (在线教育业务服务)',
    desc: '课程、章节目录、媒资视频、教师学员、题库做题、订单退款',
    port: 'Port: 9202',
    status: '运行中',
    statusType: 'success'
  },
  {
    name: 'share-modules-qa (智能问答与客服中心)',
    desc: '学员问答、第三方 Pixel AI 连通接入、客服知识库与会话',
    port: 'Port: 9203',
    status: '运行中',
    statusType: 'success'
  }
])

function goRoute(path) {
  router.push(path)
}

function goBusinessAdmin() {
  const host = window.location.hostname || 'localhost'
  window.open(`http://${host}:5174/`, '_blank')
}

function goPortal() {
  const host = window.location.hostname || 'localhost'
  window.open(`http://${host}:5173/`, '_blank')
}
</script>

<style scoped lang="scss">
.home {
  padding: 20px;
  background-color: #f8fafc;
  min-height: calc(100vh - 84px);
}

.overview-banner {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 28px 32px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);

  .banner-content {
    max-width: 680px;
  }

  .banner-badge {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: #f0fdf4;
    border: 1px solid #bbf7d0;
    color: #166534;
    font-size: 12px;
    font-weight: 500;
    padding: 4px 12px;
    border-radius: 20px;
    margin-bottom: 12px;

    .status-dot {
      width: 7px;
      height: 7px;
      background: #16a34a;
      border-radius: 50%;
    }
  }

  .banner-title {
    font-size: 24px;
    font-weight: 700;
    color: #0f172a;
    margin: 0 0 10px 0;
    letter-spacing: -0.01em;
  }

  .banner-desc {
    font-size: 14px;
    color: #64748b;
    line-height: 1.6;
    margin: 0 0 20px 0;
  }

  .banner-actions {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;

    .el-button {
      border-radius: 6px;
      font-weight: 500;
    }
  }

  .banner-meta {
    display: flex;
    gap: 24px;
    border-left: 1px solid #e2e8f0;
    padding-left: 32px;

    .meta-item {
      display: flex;
      flex-direction: column;
      gap: 6px;

      .meta-label {
        font-size: 12px;
        color: #94a3b8;
      }
      .meta-value {
        font-size: 15px;
        font-weight: 600;
        color: #0f172a;
      }
    }
  }
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;

  .metric-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 18px 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);

    .metric-icon {
      width: 44px;
      height: 44px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      flex-shrink: 0;

      &.blue { background: #eff6ff; color: #2563eb; }
      &.green { background: #f0fdf4; color: #16a34a; }
      &.orange { background: #fff7ed; color: #ea580c; }
      &.purple { background: #f5f3ff; color: #7c3aed; }
    }

    .metric-value {
      font-size: 15px;
      font-weight: 700;
      color: #0f172a;
      margin-bottom: 3px;
    }

    .metric-label {
      font-size: 12px;
      color: #64748b;
    }
  }
}

.content-card {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-title {
      font-size: 15px;
      font-weight: 600;
      color: #0f172a;
    }
  }
}

.service-table {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .service-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 14px;
    background: #f8fafc;
    border: 1px solid #f1f5f9;
    border-radius: 6px;

    .service-main {
      display: flex;
      align-items: center;
      gap: 12px;

      .service-indicator {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        flex-shrink: 0;

        &.online {
          background: #16a34a;
          box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
        }
      }

      .service-name {
        font-size: 13px;
        font-weight: 600;
        color: #1e293b;
        margin-bottom: 2px;
      }

      .service-desc {
        font-size: 12px;
        color: #64748b;
      }
    }

    .service-meta {
      display: flex;
      align-items: center;
      gap: 12px;

      .service-port {
        font-size: 12px;
        color: #94a3b8;
        font-family: monospace;
      }
    }
  }
}

.tech-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;

  .tech-group {
    h4 {
      font-size: 13px;
      font-weight: 600;
      color: #334155;
      margin: 0 0 12px 0;
    }
    .tag-wrap {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
  }
}

.quick-nav-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;

  .quick-nav-item {
    padding: 14px 16px;
    border-radius: 6px;
    background: #f8fafc;
    border: 1px solid #f1f5f9;
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #ffffff;
      border-color: #93c5fd;
      transform: translateY(-1px);
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
    }

    .nav-icon {
      font-size: 20px;
      color: #2563eb;
      flex-shrink: 0;
    }

    .nav-text {
      display: flex;
      flex-direction: column;

      .nav-title {
        font-size: 13px;
        font-weight: 600;
        color: #1e293b;
      }

      .nav-sub {
        font-size: 11px;
        color: #94a3b8;
        margin-top: 2px;
      }
    }
  }
}

.portal-nav-list {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .portal-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 16px;
    border-radius: 6px;
    background: #f8fafc;
    border: 1px solid #f1f5f9;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #ffffff;
      border-color: #93c5fd;
      transform: translateY(-1px);
    }

    .portal-info {
      .portal-title {
        font-size: 13px;
        font-weight: 600;
        color: #1e293b;
        margin-bottom: 3px;
      }

      .portal-desc {
        font-size: 12px;
        color: #64748b;
      }
    }
  }
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 992px) {
  .overview-banner {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;

    .banner-meta {
      border-left: none;
      padding-left: 0;
      border-top: 1px solid #e2e8f0;
      padding-top: 16px;
      width: 100%;
    }
  }
}

@media (max-width: 640px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
  .quick-nav-grid {
    grid-template-columns: 1fr;
  }
  .tech-grid {
    grid-template-columns: 1fr;
  }
}
</style>
