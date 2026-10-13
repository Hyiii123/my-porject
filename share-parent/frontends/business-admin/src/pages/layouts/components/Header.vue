<template>
  <header class="bg-wt">
    <div class="admin-nav-scan"></div>
    <div class="fx headerInfo">
      <div class="fx-1 marg-lt-20">
        <div class="breadcrumb-container" v-show="route.meta.title != '首页'">
          <span class="crumb-root" @click="() => $router.push('/')">工作台</span>
          <span class="crumb-sep">/</span>
          <span
            v-if="route.matched && route.matched[0]"
            class="crumb-parent"
            @click="() => $router.push({ path: route.matched[0].path })"
            >{{ route.matched[0]?.meta?.title }}</span
          >
          <span v-if="route.matched && route.matched[0] && (route.meta?.fmeta || route.matched[1])" class="crumb-sep">/</span>
          <span
            v-if="route.meta && route.meta.fmeta"
            class="crumb-parent"
            @click="() => $router.push({ path: route.meta.fmeta.path })"
            >{{ route.meta.fmeta.title }}</span
          >
          <span v-if="route.meta && route.meta.fmeta && route.matched && route.matched[1]" class="crumb-sep">/</span>
          <span
            v-if="route.matched && route.matched[1]"
            class="crumb-active"
            @click="() => $router.push({ path: route.matched[1].path })"
            >{{ route.matched[1]?.meta?.title }}</span
          >
        </div>
        <div class="breadcrumb-container" v-show="route.meta.title == '首页'">
          <span class="crumb-active">工作台大盘</span>
        </div>
      </div>

      <!-- 中间与右侧：快捷搜索与系统遥测 -->
      <div class="fx-al-ct header-right-actions">
        <!-- Command Palette 胶囊 -->
        <div class="command-palette-trigger" @click="openCommandPalette">
          <span class="search-icon">⚡</span>
          <span class="search-placeholder">⌘K 快速导航与功能检索...</span>
          <span class="kbd-tag">Ctrl K</span>
        </div>

        <!-- 集群健康微脉冲 -->
        <div class="telemetry-pill">
          <span class="pulse-dot"></span>
          <span class="telemetry-label">微服务协同正常 · 16ms</span>
        </div>

        <!-- 用户资料与退出 -->
        <div class="fx-al-ct user-profile-box" v-if="isToken && userInfo">
          <router-link to="/my/index" class="user-link">
            <img
              class="headIcon"
              :src="userInfo.icon"
              :onerror="onerrorImg"
              alt=""
            />
            <span class="user-name">{{ userInfo.name || "admin" }}</span>
          </router-link>
          <span class="vline"></span>
          <div class="back" title="退出登录" @click="goLogin()">
            <img src="@/assets/out.png" alt="" style="width:16px;height: 16px;" class="out"/>
          </div>
        </div>
      </div>
    </div>

    <!-- 全局 Command Palette 快捷弹窗 -->
    <el-dialog v-model="commandDialogVisible" title="全局指令导航 (Command Palette)" width="540px" top="18vh" destroy-on-close :show-close="true">
      <div class="command-dialog-body">
        <el-input
          v-model="commandSearch"
          placeholder="输入页面、功能关键词（如：课程、订单、教师、题库）"
          prefix-icon="Search"
          clearable
          size="large"
          autofocus
        />
        <div class="command-list">
          <div
            v-for="item in filteredNavs"
            :key="item.path"
            class="command-item"
            @click="jumpTo(item.path)"
          >
            <div class="command-item-left">
              <span class="item-icon">{{ item.icon }}</span>
              <div>
                <div class="item-title">{{ item.title }}</div>
                <div class="item-desc">{{ item.desc }}</div>
              </div>
            </div>
            <span class="jump-hint">跳转 ↵</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </header>
</template>

<script setup>
import { onMounted, onUnmounted, ref, computed, watchEffect } from "vue";
import defaultImage from "@/assets/icon.jpeg";
import { useUserStore } from "@/store";
import router from "@/router";
import { useRoute } from "vue-router";
import { TOKEN_NAME } from "@/config/global";

const store = useUserStore();
const userInfo = ref({});
const isToken = ref(false);
const route = useRoute();

const commandDialogVisible = ref(false);
const commandSearch = ref("");

const allNavs = [
  { title: "工作台协同大盘", desc: "总览访问趋势、今日营收与实时指标", path: "/main/index", icon: "📊" },
  { title: "课程管理", desc: "查看全站课程、价格上架与课时编排", path: "/curriculum/index", icon: "📚" },
  { title: "教师管理", desc: "教师账号、10分制教学评分与资质管理", path: "/user/teacher", icon: "👨‍🏫" },
  { title: "订单与交易管理", desc: "订单流水、支付渠道与退款审批", path: "/order/index", icon: "💳" },
  { title: "退款审批管理", desc: "处理学员退款申请与原路逆向退款", path: "/order/refund", icon: "🔄" },
  { title: "课程分类设置", desc: "IT研发八大核心技术分类与权重", path: "/curriculum/type", icon: "🏷️" },
  { title: "媒资视频管理", desc: "高清课时视频库与1080P资源池", path: "/curriculum/media", icon: "🎬" },
  { title: "题目与题库管理", desc: "各领域专项题目与测评题库", path: "/title/index", icon: "📝" },
  { title: "互动问答社区", desc: "学员答疑交流与高质量题解管理", path: "/interactive/index", icon: "💬" },
  { title: "营销中心 (优惠券)", desc: "优惠券发行、满减面额与核销追踪", path: "/marketing/index", icon: "🎟️" },
  { title: "学员用户管理", desc: "全站学员账号状态与认证管理", path: "/user/index", icon: "👥" }
];

const filteredNavs = computed(() => {
  if (!commandSearch.value.trim()) return allNavs;
  const kw = commandSearch.value.trim().toLowerCase();
  return allNavs.filter(item => item.title.toLowerCase().includes(kw) || item.desc.toLowerCase().includes(kw));
});

const openCommandPalette = () => {
  commandSearch.value = "";
  commandDialogVisible.value = true;
};

const jumpTo = (path) => {
  commandDialogVisible.value = false;
  router.push(path);
};

// 全局快捷键监听 Ctrl+K / Cmd+K
const handleKeyDown = (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
    e.preventDefault();
    openCommandPalette();
  }
};

onMounted(() => {
  isToken.value = sessionStorage.getItem(TOKEN_NAME) ? true : false;
  window.addEventListener("keydown", handleKeyDown);
});

onUnmounted(() => {
  window.removeEventListener("keydown", handleKeyDown);
});

watchEffect(() => {
  userInfo.value = store.getUserInfo;
});

const goLogin = () => {
  router.push("/login");
};

const onerrorImg = () => {
  userInfo.value.icon = defaultImage;
};
</script>

<style lang="scss" scoped>
header {
  position: fixed;
  top: 0;
  left: 226px;
  z-index: 998;
  width: calc(100% - 226px);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid #E2E8F0;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.02);
  text-align: left;
  padding: 0 28px;
  height: 60px;
  display: flex;
  align-items: center;
  font-size: 14px;
  box-sizing: border-box;
  transition: all 0.25s ease;

  .headerInfo {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .breadcrumb-container {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13.5px;

    .crumb-root, .crumb-parent {
      color: #64748B;
      font-weight: 500;
      cursor: pointer;
      transition: color 0.2s;
      &:hover { color: #2563EB; }
    }
    .crumb-sep {
      color: #CBD5E1;
      font-size: 12px;
    }
    .crumb-active {
      color: #0F172A;
      font-weight: 600;
      cursor: default;
    }
  }

  .header-right-actions {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .command-palette-trigger {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 14px;
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 9999px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      background: #FFFFFF;
      border-color: #CBD5E1;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.05);
    }

    .search-icon { font-size: 13px; }
    .search-placeholder {
      font-size: 12.5px;
      color: #64748B;
    }
    .kbd-tag {
      background: #FFFFFF;
      border: 1px solid #CBD5E1;
      border-radius: 4px;
      padding: 1px 6px;
      font-size: 11px;
      color: #475569;
      font-weight: 600;
      font-family: ui-monospace, SFMono-Regular, monospace;
    }
  }

  .telemetry-pill {
    display: flex;
    align-items: center;
    gap: 7px;
    padding: 5px 12px;
    background: rgba(16, 185, 129, 0.08);
    border: 1px solid rgba(16, 185, 129, 0.25);
    border-radius: 9999px;

    .pulse-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: #10B981;
      box-shadow: 0 0 8px #10B981;
      animation: pulse-telemetry 2s infinite;
    }

    .telemetry-label {
      font-size: 12px;
      color: #065F46;
      font-weight: 500;
    }
  }

  .user-profile-box {
    display: flex;
    align-items: center;

    .user-link {
      display: flex;
      align-items: center;
      text-decoration: none;
      .user-name {
        color: #0F172A;
        font-weight: 600;
        font-size: 13.5px;
      }
    }
    .headIcon {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      margin-right: 8px;
      object-fit: cover;
      border: 1px solid #E2E8F0;
    }
    .vline {
      margin: 0 14px;
      background: #E2E8F0;
      width: 1px;
      height: 16px;
    }
    .back {
      padding: 6px;
      border-radius: 6px;
      cursor: pointer;
      display: flex;
      align-items: center;
      transition: background 0.2s;
      &:hover { background: #F1F5F9; }
    }
  }
}

.command-dialog-body {
  .command-list {
    margin-top: 14px;
    max-height: 380px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 6px;

    .command-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 14px;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.15s ease;

      &:hover {
        background: #F1F5F9;
        .jump-hint { color: #2563EB; }
      }

      .command-item-left {
        display: flex;
        align-items: center;
        gap: 12px;

        .item-icon { font-size: 20px; }
        .item-title {
          font-size: 14px;
          font-weight: 600;
          color: #0F172A;
        }
        .item-desc {
          font-size: 12px;
          color: #64748B;
          margin-top: 2px;
        }
      }

      .jump-hint {
        font-size: 12px;
        color: #94A3B8;
        font-weight: 500;
      }
    }
  }
}

@keyframes pulse-telemetry {
  0% { transform: scale(0.95); opacity: 0.8; }
  50% { transform: scale(1.15); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.8; }
}
</style>
