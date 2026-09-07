<template>
  <div class="home-container">
    <!-- 轮播图/英雄区 -->
    <div class="banner-section">
      <el-carousel height="380px" :interval="6000" arrow="hover">
        <el-carousel-item v-for="banner in banners" :key="banner.id">
          <div class="banner-item" :style="{ backgroundColor: banner.bgColor }">
            <div class="container banner-inner">
              <div class="banner-content">
                <span class="banner-badge">{{ banner.tag || '官方精选' }}</span>
                <h2 class="banner-title">{{ banner.title }}</h2>
                <p class="banner-subtitle">{{ banner.subtitle }}</p>
                <div class="banner-actions">
                  <el-button type="primary" size="large" @click="goBanner(banner)" class="action-btn">
                    立即开启学习
                  </el-button>
                  <el-button size="large" @click="$router.push('/search/index')" class="browse-btn">
                    浏览全部课程
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- 继续学习快捷横幅 (登录用户专享) -->
    <div v-if="recentLearning" class="container continue-learning-wrapper">
      <div class="continue-card">
        <div class="card-left">
          <div class="icon-box">
            <el-icon :size="24"><Reading /></el-icon>
          </div>
          <div class="learning-info">
            <div class="learning-label">您上次正在学习</div>
            <h4 class="learning-title">{{ recentLearning.courseName || recentLearning.name }}</h4>
            <div class="learning-sub">
              已学习 {{ recentLearning.learnedSections || 0 }} / {{ recentLearning.sections || recentLearning.totalSections || 10 }} 节
            </div>
          </div>
        </div>
        <div class="card-right">
          <div class="progress-box">
            <el-progress
              :percentage="calcProgress(recentLearning)"
              :stroke-width="8"
              color="#2563EB"
            />
          </div>
          <el-button type="primary" class="continue-btn" @click="goLearning(recentLearning)">
            继续学习
          </el-button>
        </div>
      </div>
    </div>

    <!-- 课程分类导航 -->
    <div class="section container">
      <div class="section-header">
        <div class="header-left">
          <h3 class="section-title">课程专业领域</h3>
          <span class="section-sub">覆盖全栈开发、系统架构、算法数据与工程化实践</span>
        </div>
        <el-button type="primary" link @click="$router.push('/search/index')">全部领域 <el-icon><ArrowRight /></el-icon></el-button>
      </div>
      <div class="category-grid">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="category-item"
          @click="$router.push(`/search/index?categoryId=${cat.id}`)"
        >
          <div class="category-icon">
            <span class="icon-text">{{ cat.iconText }}</span>
          </div>
          <div class="category-info">
            <span class="category-name">{{ cat.name }}</span>
            <span class="category-count">{{ cat.count }} 门专业课</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 推荐好课 -->
    <div class="section container">
      <div class="section-header">
        <div class="header-left">
          <h3 class="section-title">重磅推荐课程</h3>
          <span class="section-sub">专家评审团队精选高品质知识内容</span>
        </div>
        <el-button type="primary" link @click="$router.push('/search/index')">查看更多 <el-icon><ArrowRight /></el-icon></el-button>
      </div>
      <div class="course-grid">
        <div
          v-for="course in recommendCourses"
          :key="course.id"
          class="course-card"
          @click="$router.push(`/details?id=${course.id}`)"
        >
          <div class="course-cover">
            <img :src="course.cover || '/src/assets/images/classDetails/default-cover.png'" :alt="course.title" loading="lazy" />
            <div class="course-badge free" v-if="course.price === 0">免费学习</div>
          </div>
          <div class="course-info">
            <h4 class="course-title" :title="course.title">{{ course.title }}</h4>
            <div class="course-meta">
              <span class="teacher">{{ course.teacherName }}</span>
              <span class="learners">{{ course.learners }} 人在学</span>
            </div>
            <div class="course-price">
              <span v-if="course.price > 0" class="price">¥{{ (course.price / 100).toFixed(2) }}</span>
              <span v-else class="free">免费</span>
              <span v-if="course.originalPrice > course.price" class="original-price">¥{{ (course.originalPrice / 100).toFixed(2) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 热门排行与新课速递 -->
    <div class="section container rank-and-new-row">
      <!-- 热门好课排行 -->
      <div class="hot-section">
        <div class="section-header">
          <div class="header-left">
            <h3 class="section-title">热门学习排行</h3>
            <span class="section-sub">学员最受欢迎课程 TOP 榜</span>
          </div>
        </div>
        <div class="hot-course-list">
          <div
            v-for="(course, index) in hotCourses.slice(0, 5)"
            :key="course.id"
            class="hot-course-item"
            @click="$router.push(`/details?id=${course.id}`)"
          >
            <div class="rank-badge" :class="`rank-${index + 1}`">{{ index + 1 }}</div>
            <div class="course-cover">
              <img :src="course.cover || '/src/assets/images/classDetails/default-cover.png'" :alt="course.title" />
            </div>
            <div class="course-info">
              <h4 class="title" :title="course.title">{{ course.title }}</h4>
              <div class="meta">
                <span>{{ course.teacherName }}</span>
                <span>{{ course.learners }} 学员</span>
              </div>
            </div>
            <div class="price">
              <span v-if="course.price > 0">¥{{ (course.price / 100).toFixed(0) }}</span>
              <span v-else class="free-text">免费</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 新课精选 -->
      <div class="new-section">
        <div class="section-header">
          <div class="header-left">
            <h3 class="section-title">最新上线课程</h3>
            <span class="section-sub">与技术潮流同步的前沿实训</span>
          </div>
        </div>
        <div class="new-course-grid">
          <div
            v-for="course in newCourses.slice(0, 4)"
            :key="course.id"
            class="new-course-card"
            @click="$router.push(`/details?id=${course.id}`)"
          >
            <div class="cover-box">
              <img :src="course.cover || '/src/assets/images/classDetails/default-cover.png'" :alt="course.title" />
              <span class="new-tag">NEW</span>
            </div>
            <div class="content-box">
              <h4 class="title" :title="course.title">{{ course.title }}</h4>
              <div class="meta">讲师：{{ course.teacherName }}</div>
              <div class="price-row">
                <span class="price" v-if="course.price > 0">¥{{ (course.price / 100).toFixed(0) }}</span>
                <span class="free" v-else>免费</span>
                <span class="learners">{{ course.learners }} 人在学</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 平台数据指标统计 -->
    <div class="stats-section">
      <div class="container">
        <div class="stats-grid">
          <div v-for="stat in stats" :key="stat.label" class="stat-item">
            <div class="stat-number">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Reading, ArrowRight } from '@element-plus/icons-vue'
import { getClassCategorys, getRecommendClassList, classSeach, getMylessons } from '@/api/class.js'
import { getBanners } from '@/api/home.js'

const router = useRouter()

const banners = ref([])
const categories = ref([])
const recommendCourses = ref([])
const hotCourses = ref([])
const newCourses = ref([])
const allCourses = ref([])
const recentLearning = ref(null)

const normalizeCourse = (course = {}) => ({
  ...course,
  id: course.id,
  title: course.title || course.courseName || '未命名课程',
  cover: course.cover || course.coverUrl || '',
  teacherName: course.teacherName || '讲师团队',
  price: Number(course.price || 0),
  originalPrice: Number(course.originalPrice ?? course.price ?? 0),
  learners: Number(course.learners ?? course.learnerCount ?? 0)
})

const normalizeRows = (response) => {
  const data = response?.data
  const rows = Array.isArray(data) ? data : (data?.list || data?.rows || [])
  return rows.map(normalizeCourse)
}

const stats = computed(() => [
  { value: allCourses.value.reduce((sum, course) => sum + course.learners, 0).toLocaleString(), label: '累计学习人次' },
  { value: allCourses.value.length || '10+', label: '精品专业课程' },
  { value: new Set(allCourses.value.map(course => course.teacherName)).size || '12+', label: '资深行业名师' },
  { value: '99.2%', label: '学员好评率' }
])

const goBanner = (banner) => {
  router.push(banner.link || '/search/index')
}

const calcProgress = (item) => {
  if (!item) return 0
  const learned = Number(item.learnedSections || 0)
  const total = Number(item.sections || item.totalSections || 10)
  if (total === 0) return 0
  return Math.min(100, Math.round((learned / total) * 100))
}

const goLearning = (item) => {
  if (item && item.courseId) {
    router.push({ path: '/learning/index', query: { id: item.courseId } })
  } else if (item && item.id) {
    router.push({ path: '/details', query: { id: item.id } })
  }
}

onMounted(async () => {
  const bannerPalettes = [
    { bg: '#1E293B', tag: '实战体系课' },
    { bg: '#0F766E', tag: '前沿技术研习' },
    { bg: '#1E40AF', tag: '系统化进阶' },
    { bg: '#334155', tag: '技能拓展专题' }
  ]

  const [bannerResponse, categoryResponse, courseResponse, recommendResponse, hotResponse, newResponse] = await Promise.allSettled([
    getBanners(),
    getClassCategorys({ includeDisabled: false }),
    classSeach({ pageNo: 1, pageSize: 200 }),
    getRecommendClassList('home'),
    getRecommendClassList('hot'),
    getRecommendClassList('new')
  ])

  if (bannerResponse.status === 'fulfilled' && bannerResponse.value?.code === 200) {
    const rows = Array.isArray(bannerResponse.value.data) ? bannerResponse.value.data : []
    banners.value = rows.map((banner, index) => {
      const palette = bannerPalettes[index % bannerPalettes.length]
      return {
        ...banner,
        subtitle: banner.subtitle || '面向未来职业发展的专业在线教育知识库',
        bgColor: palette.bg,
        tag: palette.tag
      }
    })
  }

  // 兜底 banner 保证即使接口为空也具备专业视觉
  if (!banners.value.length) {
    banners.value = [
      {
        id: 'default-1',
        title: '专业在线教育平台 · 智问学伴',
        subtitle: '融汇体系化课程与 AI 智能助教，随时随地开启沉浸式学习',
        bgColor: '#1E293B',
        tag: '新一代在线学习',
        link: '/search/index'
      }
    ]
  }

  if (categoryResponse.status === 'fulfilled' && categoryResponse.value?.code === 200) {
    const data = categoryResponse.value.data
    const rows = Array.isArray(data) ? data : (data?.list || [])
    categories.value = rows.filter(item => Number(item.status ?? 1) === 1).map((item) => ({
      ...item,
      name: item.name || item.categoryName,
      iconText: (item.name || item.categoryName || '课').slice(0, 2),
      count: Number(item.courseCount ?? 0)
    }))
  }

  if (courseResponse.status === 'fulfilled' && courseResponse.value?.code === 200) allCourses.value = normalizeRows(courseResponse.value)
  if (recommendResponse.status === 'fulfilled' && recommendResponse.value?.code === 200) recommendCourses.value = normalizeRows(recommendResponse.value)
  if (hotResponse.status === 'fulfilled' && hotResponse.value?.code === 200) hotCourses.value = normalizeRows(hotResponse.value)
  if (newResponse.status === 'fulfilled' && newResponse.value?.code === 200) newCourses.value = normalizeRows(newResponse.value)

  if (allCourses.value.length) {
    categories.value = categories.value.map(category => ({
      ...category,
      count: allCourses.value.filter(course => String(course.categoryId) === String(category.id)).length
    }))
  }

  // 若用户已登录，获取最近学习记录
  const token = sessionStorage.getItem('token')
  if (token) {
    try {
      const lessonRes = await getMylessons()
      const lessons = lessonRes?.data?.list || lessonRes?.data?.rows || (Array.isArray(lessonRes?.data) ? lessonRes.data : [])
      if (lessons.length > 0) {
        recentLearning.value = lessons[0]
      }
    } catch (e) {
      // 忽略未登录或未参加课程的静默错误
    }
  }
})
</script>

<style scoped lang="scss">
.home-container {
  background-color: #F8FAFC;
  min-height: 100vh;
  padding-bottom: 60px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  box-sizing: border-box;
}

/* 轮播图/英雄区 */
.banner-section {
  width: 100%;
  margin-bottom: 32px;
}

.banner-item {
  height: 100%;
  display: flex;
  align-items: center;
  position: relative;
  transition: background-color 0.4s ease;
}

.banner-inner {
  display: flex;
  align-items: center;
  height: 100%;
}

.banner-content {
  max-width: 600px;
  color: #FFFFFF;

  .banner-badge {
    display: inline-block;
    background: rgba(255, 255, 255, 0.15);
    border: 1px solid rgba(255, 255, 255, 0.25);
    backdrop-filter: blur(4px);
    color: #FFFFFF;
    font-size: 12px;
    font-weight: 500;
    padding: 3px 12px;
    border-radius: 4px;
    margin-bottom: 16px;
    letter-spacing: 0.5px;
  }

  .banner-title {
    font-size: 32px;
    font-weight: 700;
    line-height: 1.25;
    margin: 0 0 12px 0;
    letter-spacing: -0.5px;
  }

  .banner-subtitle {
    font-size: 15px;
    color: #CBD5E1;
    line-height: 1.5;
    margin: 0 0 24px 0;
  }

  .banner-actions {
    display: flex;
    gap: 12px;

    .action-btn {
      background: #2563EB;
      border-color: #2563EB;
      font-weight: 600;
      padding: 0 24px;
      &:hover {
        background: #1D4ED8;
        border-color: #1D4ED8;
      }
    }

    .browse-btn {
      background: rgba(255, 255, 255, 0.12);
      border-color: rgba(255, 255, 255, 0.3);
      color: #FFFFFF;
      font-weight: 500;
      &:hover {
        background: rgba(255, 255, 255, 0.2);
        color: #FFFFFF;
      }
    }
  }
}

/* 继续学习快捷条 */
.continue-learning-wrapper {
  margin-top: -16px;
  margin-bottom: 32px;
  position: relative;
  z-index: 10;
}

.continue-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 12px -2px rgba(15, 23, 42, 0.06);

  .card-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .icon-box {
      width: 44px;
      height: 44px;
      background: #EFF6FF;
      color: #2563EB;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .learning-label {
      font-size: 12px;
      color: #64748B;
      margin-bottom: 2px;
    }

    .learning-title {
      font-size: 16px;
      font-weight: 600;
      color: #0F172A;
      margin: 0;
    }

    .learning-sub {
      font-size: 12px;
      color: #94A3B8;
      margin-top: 2px;
    }
  }

  .card-right {
    display: flex;
    align-items: center;
    gap: 20px;

    .progress-box {
      width: 140px;
    }

    .continue-btn {
      padding: 0 20px;
      font-weight: 500;
    }
  }
}

/* 通用板块头部 */
.section {
  margin-bottom: 44px;
}

.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;

  .header-left {
    display: flex;
    align-items: baseline;
    gap: 12px;

    .section-title {
      font-size: 20px;
      font-weight: 700;
      color: #0F172A;
      margin: 0;
      letter-spacing: -0.3px;
    }

    .section-sub {
      font-size: 13px;
      color: #64748B;
    }
  }
}

/* 分类网格 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.category-item {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #2563EB;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
    transform: translateY(-2px);

    .category-icon {
      background: #2563EB;
      color: #FFFFFF;
    }
  }

  .category-icon {
    width: 38px;
    height: 38px;
    border-radius: 6px;
    background: #EFF6FF;
    color: #2563EB;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    font-weight: 700;
    flex-shrink: 0;
    transition: all 0.2s ease;
  }

  .category-info {
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .category-name {
      font-size: 14px;
      font-weight: 600;
      color: #0F172A;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .category-count {
      font-size: 12px;
      color: #94A3B8;
      margin-top: 2px;
    }
  }
}

/* 课程网格 */
.course-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.course-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 1px 3px 0 rgba(15, 23, 42, 0.04);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;

  &:hover {
    border-color: #CBD5E1;
    box-shadow: 0 8px 18px -4px rgba(15, 23, 42, 0.08);
    transform: translateY(-2px);
    .course-title {
      color: #2563EB;
    }
  }

  .course-cover {
    height: 150px;
    position: relative;
    overflow: hidden;
    background: #F1F5F9;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }

    .course-badge {
      position: absolute;
      top: 10px;
      left: 10px;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
      background: #059669;
      color: #FFFFFF;
    }
  }

  .course-info {
    padding: 14px;
    display: flex;
    flex-direction: column;
    flex: 1;

    .course-title {
      font-size: 14px;
      font-weight: 600;
      color: #0F172A;
      line-height: 1.45;
      height: 40px;
      margin-bottom: 8px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      transition: color 0.2s ease;
    }

    .course-meta {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: #64748B;
      margin-bottom: 12px;
    }

    .course-price {
      margin-top: auto;
      padding-top: 10px;
      border-top: 1px solid #F1F5F9;
      display: flex;
      align-items: baseline;
      gap: 8px;

      .price {
        font-size: 16px;
        font-weight: 700;
        color: #DC2626;
      }
      .free {
        font-size: 13px;
        font-weight: 600;
        color: #059669;
      }
      .original-price {
        font-size: 12px;
        color: #94A3B8;
        text-decoration: line-through;
      }
    }
  }
}

/* 排行与新课双栏 */
.rank-and-new-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 28px;
}

.hot-section, .new-section {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px 0 rgba(15, 23, 42, 0.04);
}

.hot-course-list {
  display: flex;
  flex-direction: column;
}

.hot-course-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #F1F5F9;
  cursor: pointer;
  transition: all 0.2s ease;

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
  &:first-child {
    padding-top: 0;
  }

  &:hover {
    .title {
      color: #2563EB;
    }
  }

  .rank-badge {
    width: 26px;
    height: 26px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    font-weight: 700;
    margin-right: 14px;
    background: #F1F5F9;
    color: #64748B;

    &.rank-1 { background: #FEF3C7; color: #D97706; }
    &.rank-2 { background: #F1F5F9; color: #475569; }
    &.rank-3 { background: #FFEDD5; color: #C2410C; }
  }

  .course-cover {
    width: 64px;
    height: 40px;
    border-radius: 4px;
    overflow: hidden;
    margin-right: 12px;
    background: #F1F5F9;
    flex-shrink: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .course-info {
    flex: 1;
    overflow: hidden;

    .title {
      font-size: 13px;
      font-weight: 600;
      color: #0F172A;
      margin: 0 0 4px 0;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      transition: color 0.2s;
    }

    .meta {
      font-size: 11px;
      color: #94A3B8;
      display: flex;
      gap: 10px;
    }
  }

  .price {
    font-size: 14px;
    font-weight: 700;
    color: #DC2626;
    margin-left: 12px;

    .free-text {
      color: #059669;
      font-size: 12px;
    }
  }
}

/* 新课速递网格 */
.new-course-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.new-course-card {
  border: 1px solid #F1F5F9;
  border-radius: 6px;
  padding: 10px;
  cursor: pointer;
  display: flex;
  gap: 12px;
  background: #F8FAFC;
  transition: all 0.2s ease;

  &:hover {
    background: #EFF6FF;
    border-color: #DBEAFE;
    .title { color: #2563EB; }
  }

  .cover-box {
    width: 80px;
    height: 54px;
    border-radius: 4px;
    overflow: hidden;
    position: relative;
    flex-shrink: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .new-tag {
      position: absolute;
      top: 2px;
      left: 2px;
      background: #2563EB;
      color: #FFFFFF;
      font-size: 9px;
      font-weight: 700;
      padding: 1px 4px;
      border-radius: 2px;
    }
  }

  .content-box {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .title {
      font-size: 12px;
      font-weight: 600;
      color: #0F172A;
      margin: 0 0 4px 0;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      transition: color 0.2s;
    }

    .meta {
      font-size: 11px;
      color: #64748B;
      margin-bottom: auto;
    }

    .price-row {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .price {
        font-size: 13px;
        font-weight: 700;
        color: #DC2626;
      }
      .free {
        font-size: 12px;
        font-weight: 600;
        color: #059669;
      }
      .learners {
        font-size: 11px;
        color: #94A3B8;
      }
    }
  }
}

/* 数据统计区 */
.stats-section {
  background: #0F172A;
  padding: 48px 0;
  margin-top: 48px;
  border-radius: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px;
  text-align: center;
}

.stat-item {
  .stat-number {
    font-size: 32px;
    font-weight: 700;
    color: #FFFFFF;
    margin-bottom: 6px;
    letter-spacing: -0.5px;
  }

  .stat-label {
    font-size: 13px;
    color: #94A3B8;
  }
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .category-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .rank-and-new-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .banner-section {
    margin-bottom: 20px;
  }
  .banner-title {
    font-size: 24px !important;
  }
  .continue-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    .card-right {
      width: 100%;
      justify-content: space-between;
    }
  }
  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .course-grid {
    grid-template-columns: 1fr;
  }
  .new-course-grid {
    grid-template-columns: 1fr;
  }
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
  }
}
</style>
