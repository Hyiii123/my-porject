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

    <!-- 3. 直播公开课 -->
    <div class="class-section-wrap">
      <div class="container">
        <OpenClass
          title="🔥 直播公开课与名师实战"
          :data="freeClassData"
        ></OpenClass>
      </div>
    </div>

    <!-- 4. 新课推荐 -->
    <div class="class-section-wrap alt-bg">
      <div class="container">
        <OpenClass
          title="✨ 2026 前沿新课推荐"
          :data="freeClassData"
        ></OpenClass>
      </div>
    </div>

    <!-- 5. 产学研精品好课 -->
    <div class="class-section-wrap">
      <div class="container">
        <OpenClass
          title="🏆 产学研精品攻坚好课"
          :data="freeClassData"
        ></OpenClass>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { getClassCategorys, getFreeClassList } from "@/api/class.js";
import ClassCategory from "@/components/ClassCategory.vue";
import OpenClass from "./components/OpenClass.vue";
import Swiper from "./components/Swiper.vue";
import banner1 from "@/assets/banner1.jpg";
import banner2 from "@/assets/banner2.jpg";
import banner3 from "@/assets/banner3.jpg";

const classCategorys = ref([]);
const imags = [banner1, banner2, banner3];
const freeClassData = ref([]);

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
    const res = await getFreeClassList({ pageNo: 1, pageSize: 8 });
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
</style>
