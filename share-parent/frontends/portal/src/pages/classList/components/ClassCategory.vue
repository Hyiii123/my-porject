<!-- 课程分类导航面板 - IAIC 风格 -->
<template>
  <div class="classCategory-panel" :class="{ 'is-float': type === 'float' }">
    <!-- 面板顶部标题 -->
    <div class="category-panel-header">
      <div class="header-title">
        <span class="icon">📚</span>
        <span class="txt">热门学科赛道</span>
      </div>
      <span class="tag">全景图谱</span>
    </div>

    <!-- 分类列表 -->
    <div class="category-list" @mouseleave="mouseoutHandle">
      <div
        v-for="item in displayCategories"
        :key="item.id"
        class="category-row"
        @mouseenter="mouseoverHandle(item)"
        @click="handleCategoryClick(item)"
      >
        <div class="row-main">
          <div class="row-icon-title">
            <span class="cat-icon">{{ getCategoryIcon(item.name) }}</span>
            <span class="cat-name">{{ item.name || item.categoryName }}</span>
          </div>
          <span class="cat-arrow">›</span>
        </div>

        <!-- 领域热门技术微标签 -->
        <div class="row-sub">
          <span
            v-for="(sub, sIdx) in getSubTags(item)"
            :key="sIdx"
            class="sub-tag"
            @click.stop="handleSubClick(item, sub)"
          >
            {{ sub.name }}
          </span>
        </div>
      </div>

      <!-- 悬停飞出多维技能大面板 (Flyout Detail Panel) -->
      <transition name="fade-flyout">
        <div
          v-if="isDetails && currentItem"
          class="category-flyout"
          @mouseenter="isDetails = true"
          @mouseleave="mouseoutHandle"
        >
          <div class="flyout-header">
            <div class="flyout-title">
              <span class="cat-icon-lg">{{ getCategoryIcon(currentItem.name) }}</span>
              <span class="title-text">{{ currentItem.name || currentItem.categoryName }}</span>
            </div>
            <div class="flyout-explore" @click="handleCategoryClick(currentItem)">
              <span>探索全部课程</span>
              <span class="arrow">→</span>
            </div>
          </div>

          <div class="flyout-desc" v-if="currentItem.description">
            {{ currentItem.description }}
          </div>

          <!-- 子分类与大厂技术矩阵 -->
          <div class="flyout-groups">
            <div
              v-for="(group, gIdx) in getFlyoutGroups(currentItem)"
              :key="gIdx"
              class="flyout-group"
            >
              <div class="group-label">{{ group.title }}</div>
              <div class="group-tags">
                <span
                  v-for="(tag, tIdx) in group.tags"
                  :key="tIdx"
                  class="group-tag"
                  @click="handleTagClick(tag)"
                >
                  {{ tag }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { useRouter } from "vue-router";

const props = defineProps({
  data: {
    type: Array,
    default: () => [],
  },
  type: {
    type: String,
    default: "",
  },
});

const router = useRouter();
const isDetails = ref(false);
const currentItem = ref(null);

// 默认兜底分类数据，彻底避免后端加载延迟或空值时出现大白块
const defaultCategories = [
  { id: 1, name: "前端开发", description: "从 Vue3/React 现代工程化到微前端与 WebAssembly 深度攻坚" },
  { id: 2, name: "后端开发", description: "涵盖 Spring Boot 3、Spring Cloud Alibaba 与亿级并发微服务中台" },
  { id: 6, name: "人工智能", description: "前沿大语言模型应用、Prompt 工程、RAG 向量检索与多智能体 Agent 架构" },
  { id: 3, name: "移动开发", description: "Flutter 3.x 跨端实战、Android 核心架构与 HarmonyOS 原生开发" },
  { id: 5, name: "云计算与DevOps", description: "Docker 容器化、Kubernetes 集群编排、Istio 与全链路可观测性运维" },
];

const displayCategories = computed(() => {
  if (Array.isArray(props.data) && props.data.length > 0) {
    return props.data.slice(0, 5);
  }
  return defaultCategories;
});

const getCategoryIcon = (name) => {
  const n = String(name || "");
  if (n.includes("前端")) return "💻";
  if (n.includes("后端") || n.includes("架构")) return "⚙️";
  if (n.includes("人工智能") || n.includes("AI") || n.includes("模型")) return "🤖";
  if (n.includes("移动") || n.includes("Flutter")) return "📱";
  if (n.includes("云") || n.includes("DevOps") || n.includes("容器")) return "☁️";
  if (n.includes("数据") || n.includes("数据库")) return "🗄️";
  if (n.includes("安全")) return "🔒";
  if (n.includes("游戏")) return "🎮";
  if (n.includes("区块链")) return "⛓️";
  return "⚡";
};

const getSubTags = (item) => {
  if (Array.isArray(item.children) && item.children.length > 0) {
    return item.children.slice(0, 2);
  }
  const n = String(item.name || item.categoryName || "");
  if (n.includes("前端")) return [{ name: "Vue3 实战" }, { name: "React18" }];
  if (n.includes("后端")) return [{ name: "SpringBoot" }, { name: "Go微服务" }];
  if (n.includes("人工智能") || n.includes("AI")) return [{ name: "大模型RAG" }, { name: "Agent推演" }];
  if (n.includes("移动")) return [{ name: "Flutter 3" }, { name: "Android" }];
  if (n.includes("云") || n.includes("DevOps")) return [{ name: "Docker" }, { name: "K8s编排" }];
  if (n.includes("数据")) return [{ name: "MySQL" }, { name: "Redis集群" }];
  return [{ name: "进阶必修" }, { name: "实战项目" }];
};

const getFlyoutGroups = (item) => {
  const n = String(item.name || item.categoryName || "");
  if (n.includes("前端")) {
    return [
      { title: "核心框架与语言", tags: ["Vue 3.x", "React 18", "TypeScript", "ES6+", "Pinia", "Next.js"] },
      { title: "生产级企业实战", tags: ["工程化与Vite", "微前端无界", "SSR服务端渲染", "可视化ECharts", "组件库封装"] },
      { title: "大厂能力模型", tags: ["性能首屏优化", "前端安全XSS/CSRF", "B端低代码平台", "跨端Taro/UniApp"] }
    ];
  }
  if (n.includes("后端")) {
    return [
      { title: "主流企业框架", tags: ["Spring Boot 3", "Spring Cloud", "MyBatis-Plus", "Netty", "Go Gin"] },
      { title: "高并发中间件", tags: ["RocketMQ 5", "Redis分布式锁", "MySQL内核调优", "Sentinel限流", "Seata事务"] },
      { title: "架构与设计", tags: ["DDD领域驱动设计", "分库分表Sharding", "JVM内存调优", "分布式链路追踪"] }
    ];
  }
  if (n.includes("人工智能") || n.includes("AI")) {
    return [
      { title: "大模型前沿体系", tags: ["LangChain", "Spring AI", "Qdrant向量库", "Prompt工程", "RAG架构实战"] },
      { title: "多智能体协同", tags: ["Actor-Critic博弈", "Kahn DAG规划", "状态机图编排", "自动反思回路", "Eval评测"] },
      { title: "模型算法与微调", tags: ["PyTorch", "Transformer", "LoRA微调", "vLLM推理加速", "知识库问答"] }
    ];
  }
  return [
    { title: "核心专业知识", tags: ["核心基础与规范", "主流开发框架", "企业级中间件", "分布式生产治理"] },
    { title: "大厂实战进阶", tags: ["性能诊断调优", "故障排查攻坚", "全链路高可用", "生产压测实践"] }
  ];
};

const mouseoverHandle = (item) => {
  currentItem.value = item;
  isDetails.value = true;
};

const mouseoutHandle = () => {
  isDetails.value = false;
};

const handleCategoryClick = (item) => {
  isDetails.value = false;
  router.push({ path: "/classList/index", query: { categoryId: item.id } });
};

const handleSubClick = (item, sub) => {
  isDetails.value = false;
  router.push({ path: "/classList/index", query: { categoryId: item.id, keyword: sub.name } });
};

const handleTagClick = (tag) => {
  isDetails.value = false;
  router.push({ path: "/classList/index", query: { keyword: tag } });
};
</script>

<style lang="scss" scoped>
.classCategory-panel {
  position: relative;
  width: 100%;
  height: 420px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
}

.category-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: var(--sky);
  border-bottom: 1px solid var(--line);

  .header-title {
    display: flex;
    align-items: center;
    gap: 8px;
    .icon {
      font-size: 16px;
    }
    .txt {
      font-size: 13.5px;
      font-weight: 700;
      color: var(--navy);
      letter-spacing: 0.03em;
    }
  }

  .tag {
    font-size: 11px;
    font-weight: 600;
    color: var(--azure);
    background: rgba(30, 137, 241, 0.1);
    padding: 2px 8px;
    border-radius: 10px;
  }
}

.category-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.category-row {
  flex: 1;
  padding: 10px 18px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-bottom: 1px solid rgba(228, 237, 248, 0.6);
  cursor: pointer;
  transition: all 0.2s ease;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--sky-2);
    padding-left: 22px;
    .cat-arrow {
      transform: translateX(3px);
      color: var(--azure);
    }
    .cat-name {
      color: var(--azure);
    }
  }

  .row-main {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 4px;

    .row-icon-title {
      display: flex;
      align-items: center;
      gap: 9px;

      .cat-icon {
        font-size: 15px;
      }
      .cat-name {
        font-size: 14px;
        font-weight: 600;
        color: var(--ink);
        transition: color 0.2s;
      }
    }

    .cat-arrow {
      font-size: 18px;
      color: #94a3b8;
      font-weight: 300;
      transition: all 0.2s;
    }
  }

  .row-sub {
    display: flex;
    align-items: center;
    gap: 8px;
    padding-left: 24px;

    .sub-tag {
      font-size: 11.5px;
      color: var(--slate);
      background: rgba(86, 104, 138, 0.07);
      padding: 1px 6px;
      border-radius: 4px;
      transition: all 0.15s;

      &:hover {
        color: var(--azure);
        background: rgba(30, 137, 241, 0.12);
      }
    }
  }
}

.category-flyout {
  position: absolute;
  top: 0;
  left: 260px;
  width: 540px;
  height: 420px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 0 16px 16px 0;
  box-shadow: 12px 0 36px -8px rgba(19, 41, 79, 0.15);
  z-index: 100;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;

  .flyout-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 14px;
    border-bottom: 1px solid var(--line);

    .flyout-title {
      display: flex;
      align-items: center;
      gap: 10px;
      .cat-icon-lg {
        font-size: 22px;
      }
      .title-text {
        font-size: 17px;
        font-weight: 700;
        color: var(--navy);
      }
    }

    .flyout-explore {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 13px;
      font-weight: 600;
      color: var(--azure);
      cursor: pointer;
      padding: 4px 10px;
      border-radius: 12px;
      background: var(--sky);
      transition: all 0.2s;

      &:hover {
        background: var(--azure);
        color: #fff;
      }
    }
  }

  .flyout-desc {
    font-size: 12.5px;
    color: var(--slate);
    line-height: 1.6;
    margin: 14px 0 16px;
  }

  .flyout-groups {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 16px;
    overflow-y: auto;

    .flyout-group {
      .group-label {
        font-size: 12.5px;
        font-weight: 700;
        color: var(--navy);
        margin-bottom: 8px;
      }

      .group-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .group-tag {
          font-size: 12px;
          color: var(--ink);
          background: var(--sky);
          border: 1px solid var(--line);
          padding: 4px 10px;
          border-radius: 6px;
          cursor: pointer;
          transition: all 0.15s;

          &:hover {
            color: #fff;
            background: var(--azure);
            border-color: var(--azure);
          }
        }
      }
    }
  }
}

.fade-flyout-enter-active,
.fade-flyout-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-flyout-enter-from,
.fade-flyout-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}
</style>
