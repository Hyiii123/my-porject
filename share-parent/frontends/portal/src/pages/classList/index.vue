<!-- 课程中心 - IAIC 风格 -->
<template>
  <div class="mainWrapper">
    <!-- 1. 顶部 IAIC Hero Banner -->
    <section class="classList-hero">
      <div class="hero-glow"></div>
      <div class="container hero-content">
        <div class="home-eyebrow">
          <span class="idx">01</span>
          <span class="bar"></span>
          <span>COMPREHENSIVE CURRICULUM ARCHITECTURE</span>
        </div>
        <h1 class="hero-title">全景前沿技术课程中心 · <span class="accent">产学研认证体系</span></h1>
        <p class="hero-desc">
          紧密对标一线大厂技术标准与国家胜任力模型，涵盖 Java 微服务架构、大模型微调与 RAG、Go 高并发云原生、大数据流批计算等核心技术链路。
        </p>
      </div>
    </section>

    <!-- 2. 分类与轮播区 -->
    <div class="container banner-section">
      <div class="banner-card fx">
        <div class="categorys">
          <ClassCategory :data="classCategorys"></ClassCategory>
        </div>
        <div class="swiper-box fx-1">
          <Swiper :data="imags"></Swiper>
        </div>
      </div>
    </div>

    <!-- 3. 技术方向筛选胶囊条 (IAIC Category Filter Bar) -->
    <div class="container filter-container">
      <div class="filter-pills-bar">
        <button
          class="pill-btn"
          :class="{ active: !currentCatId && !searchKeyword }"
          @click="handleSelectCategory(null)"
        >
          全部赛道
        </button>
        <button
          v-for="cat in classCategorys"
          :key="cat.id"
          class="pill-btn"
          :class="{ active: String(currentCatId) === String(cat.id) }"
          @click="handleSelectCategory(cat.id)"
        >
          <span class="cat-icon">{{ getCategoryIcon(cat.name) }}</span>
          <span>{{ cat.name }}</span>
        </button>
      </div>

      <!-- 激活状态标签与快捷清除 -->
      <div v-if="currentCatId || searchKeyword" class="filter-active-tags">
        <span class="label">当前筛选：</span>
        <el-tag
          v-if="currentCatId"
          closable
          effect="dark"
          class="filter-tag"
          @close="handleSelectCategory(null)"
        >
          {{ currentCategoryName }}
        </el-tag>
        <el-tag
          v-if="searchKeyword"
          closable
          effect="plain"
          type="success"
          class="filter-tag"
          @close="clearSearchKeyword"
        >
          关键词: {{ searchKeyword }}
        </el-tag>
        <button class="reset-filter-btn" @click="resetFilters">重置全部</button>
      </div>
    </div>

    <!-- 4. 定向分类/搜索结果专区 -->
    <div v-if="currentCatId || searchKeyword" class="class-section-wrap">
      <div class="container">
        <OpenClass
          :title="`🎯 检索与分类结果 · ${currentCategoryName} ${searchKeyword} (共 ${filteredCourses.length} 门课程)`"
          :data="filteredCourses"
        ></OpenClass>
      </div>
    </div>

    <!-- 5. 未筛选时的默认三专区展示 -->
    <template v-else>
      <div class="class-section-wrap">
        <div class="container">
          <OpenClass
            title="🔥 直播公开课与名师实战"
            :data="liveClassData"
          ></OpenClass>
        </div>
      </div>

      <div class="class-section-wrap alt-bg">
        <div class="container">
          <OpenClass
            title="✨ 2026 前沿新课推荐"
            :data="newClassData"
          ></OpenClass>
        </div>
      </div>

      <div class="class-section-wrap">
        <div class="container">
          <OpenClass
            title="🏆 产学研精品攻坚好课"
            :data="featuredClassData"
          ></OpenClass>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getClassCategorys, getFreeClassList } from "@/api/class.js";
import ClassCategory from "@/components/ClassCategory.vue";
import OpenClass from "./components/OpenClass.vue";
import Swiper from "./components/Swiper.vue";
import banner1 from "@/assets/banner1.jpg";
import banner2 from "@/assets/banner2.jpg";
import banner3 from "@/assets/banner3.jpg";

const route = useRoute();
const router = useRouter();

const classCategorys = ref([]);
const imags = [banner1, banner2, banner3];
const freeClassData = ref([]);
const filteredCourses = ref([]);

const currentCatId = ref(null);
const searchKeyword = ref("");

const currentCategoryName = computed(() => {
  if (!currentCatId.value) return "";
  const cat = classCategorys.value.find((c) => String(c.id) === String(currentCatId.value));
  return cat ? cat.name : "已选方向";
});

const getCategoryIcon = (name) => {
  const n = String(name || "");
  if (n.includes("前端")) return "💻";
  if (n.includes("后端") || n.includes("架构")) return "⚙️";
  if (n.includes("人工智能") || n.includes("AI")) return "🤖";
  if (n.includes("移动")) return "📱";
  if (n.includes("云") || n.includes("DevOps")) return "☁️";
  if (n.includes("数据库")) return "🗄️";
  if (n.includes("安全")) return "🔒";
  if (n.includes("游戏")) return "🎮";
  if (n.includes("区块链")) return "⛓️";
  return "⚡";
};

const liveClassData = computed(() => {
  return freeClassData.value.slice(0, 4);
});

const newClassData = computed(() => {
  return freeClassData.value.length > 4 ? freeClassData.value.slice(4, 8) : freeClassData.value;
});

const featuredClassData = computed(() => {
  return freeClassData.value.length > 8 ? freeClassData.value.slice(8, 12) : freeClassData.value.slice(0, 4);
});

const handleSelectCategory = (catId) => {
  currentCatId.value = catId;
  router.push({
    path: "/classList/index",
    query: {
      ...(catId ? { categoryId: catId } : {}),
      ...(searchKeyword.value ? { keyword: searchKeyword.value } : {})
    }
  });
};

const clearSearchKeyword = () => {
  searchKeyword.value = "";
  handleSelectCategory(currentCatId.value);
};

const resetFilters = () => {
  currentCatId.value = null;
  searchKeyword.value = "";
  router.push({ path: "/classList/index" });
};

const fetchFilteredData = async () => {
  if (!currentCatId.value && !searchKeyword.value) {
    return;
  }
  try {
    const params = { pageNo: 1, pageSize: 12 };
    if (currentCatId.value) params.categoryId = currentCatId.value;
    if (searchKeyword.value) params.keyword = searchKeyword.value;
    const res = await getFreeClassList(params);
    if (res.code == 200 && res.data) {
      filteredCourses.value = res.data.list || res.data.records || [];
    }
  } catch (e) {
    console.debug("过滤课程加载失败:", e);
  }
};

watch(
  () => route.query,
  (newQ) => {
    currentCatId.value = newQ.categoryId || null;
    searchKeyword.value = newQ.keyword || "";
    if (currentCatId.value || searchKeyword.value) {
      fetchFilteredData();
    }
  },
  { immediate: true }
);

const getClassCategoryData = async () => {
  try {
    const res = await getClassCategorys();
    if (res.code == 200 && Array.isArray(res.data)) {
      classCategorys.value = res.data;
    }
  } catch (e) {
    console.debug("分类加载失败:", e);
  }
};

const getFreeClassListData = async () => {
  try {
    const res = await getFreeClassList({ pageNo: 1, pageSize: 12 });
    if (res.code == 200 && res.data) {
      const list = res.data.list || res.data.records || [];
      freeClassData.value = list;
    }
  } catch (e) {
    console.debug("公开课加载失败:", e);
  }
};

onMounted(() => {
  getClassCategoryData();
  getFreeClassListData();
});
</script>

<style lang="scss" scoped>
.mainWrapper {
  background: var(--sky);
  min-height: 100vh;
  font-family: var(--cn);
  padding-bottom: 60px;
}

.classList-hero {
  position: relative;
  overflow: hidden;
  padding: 48px 0 36px;
  background: radial-gradient(60% 80% at 50% 0%, rgba(33, 198, 232, 0.14), transparent 60%),
              radial-gradient(50% 70% at 5% 100%, rgba(43, 134, 240, 0.12), transparent 60%),
              linear-gradient(135deg, #eaf4ff, #d6eaff 55%, #c8e0ff);
  border-bottom: 1px solid var(--line);

  .hero-glow {
    position: absolute;
    width: 600px;
    height: 300px;
    background: radial-gradient(ellipse, rgba(56, 182, 255, 0.3), transparent 65%);
    top: 50%; left: 50%;
    transform: translate(-50%, -50%);
    pointer-events: none;
  }
}

.hero-content {
  position: relative;
  z-index: 2;

  .hero-title {
    font-size: clamp(28px, 3.6vw, 40px);
    font-weight: 800;
    color: var(--ink);
    margin: 10px 0 8px;

    .accent {
      background: var(--grad);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }
  }

  .hero-desc {
    font-size: 15px;
    color: var(--slate);
    max-width: 720px;
    margin: 0;
    line-height: 1.7;
  }
}

.banner-section {
  margin-top: 24px;
  margin-bottom: 36px;
}

.banner-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: var(--shadow);

  .categorys {
    width: 260px;
    flex-shrink: 0;
    background: #fff;
    border-right: 1px solid var(--line);
    z-index: 10;
  }

  .swiper-box {
    overflow: hidden;
  }
}

.class-section-wrap {
  padding: 32px 0 16px;

  &.alt-bg {
    background: rgba(255, 255, 255, 0.6);
    border-top: 1px solid var(--line);
    border-bottom: 1px solid var(--line);
  }
}

.filter-container {
  margin-top: 28px;
  margin-bottom: 8px;
}

.filter-pills-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  background: var(--card);
  padding: 12px 16px;
  border-radius: 14px;
  border: 1px solid var(--line);
  box-shadow: 0 1px 3px rgba(19, 41, 79, 0.04);

  .pill-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 7px 16px;
    border-radius: 20px;
    border: 1px solid transparent;
    background: var(--sky);
    color: var(--ink);
    font-size: 13.5px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    .cat-icon {
      font-size: 14px;
    }

    &:hover {
      background: var(--sky-2);
      color: var(--azure);
    }

    &.active {
      background: var(--grad);
      color: #fff;
      font-weight: 700;
      box-shadow: 0 4px 12px rgba(30, 137, 241, 0.35);
    }
  }
}

.filter-active-tags {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
  padding: 0 4px;

  .label {
    font-size: 13px;
    color: var(--slate);
    font-weight: 600;
  }

  .filter-tag {
    font-weight: 600;
    border-radius: 6px;
  }

  .reset-filter-btn {
    background: transparent;
    border: none;
    color: var(--slate-2);
    font-size: 12px;
    cursor: pointer;
    text-decoration: underline;
    transition: color 0.15s;

    &:hover {
      color: var(--azure);
    }
  }
}

</style>
