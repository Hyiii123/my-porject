<template>
  <div class="home-container">
    <!-- 顶部核心区：AI 多智能体协同决策看板 (替换原 Banner 轮播图) -->
    <div class="hero-agent-section container">
      <!-- 多智能体协同推理实时动态看板 (Live Agent Reasoning HUD) -->
      <AgentReasoningHUD @recalculate="handleRecalculateRecommendations" />
    </div>

    <!-- 智能个性化专属推荐 (基于多智能体协同系统与 IT 知识图谱) -->
    <div class="section container personalized-section" v-if="personalizedCourses.length">
      <div class="section-header">
        <div class="header-left">
          <h3 class="section-title">🎯 为您专属推荐</h3>
          <span class="section-sub">基于您的学情画像与目标规划，由多智能体协同实时规划</span>
        </div>
        <div class="header-right" style="display: flex; align-items: center; gap: 12px;">
          <el-button type="success" plain round size="small" @click="openLearningPathModal">
            🗺️ 查看 AI 学习成长路径
          </el-button>
          <el-button type="primary" link @click="$router.push('/search/index')">全部课程 <el-icon><ArrowRight /></el-icon></el-button>
        </div>
      </div>
      <div class="course-grid">
        <div
          v-for="course in personalizedCourses"
          :key="course.id"
          class="course-card personalized-card"
          @click="$router.push(`/details?id=${course.id}`)"
        >
          <div class="course-cover">
            <img :src="course.cover || defaultCover" :alt="course.title" fetchpriority="high" @error="handleImgError" />
            <div class="match-score-badge" v-if="course.matchScore">{{ course.matchScore }}% 契合度</div>
          </div>
          <div class="course-info">
            <h4 class="course-title" :title="course.title">{{ course.title }}</h4>

            <!-- AI 推荐理由 (Explainable AI 极简导学理由) -->
            <div class="recommend-reason-row" v-if="course.recommendReason">
              <span class="reason-icon">💡</span>
              <span class="reason-text">{{ course.recommendReason }}</span>
            </div>

            <div class="course-meta">
              <span class="teacher">{{ course.teacherName }}</span>
              <span class="difficulty-tag" v-if="course.difficulty">
                {{ course.difficulty === 1 ? '初级' : course.difficulty === 3 ? '高级' : '进阶' }}
              </span>
              <span class="learners">{{ course.learners }}人在学</span>
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

    <!-- 继续学习快捷横幅 (仅当登录且存在真实有效学习记录时展示) -->
    <div v-if="recentLearning && (recentLearning.courseName || recentLearning.title)" class="container continue-learning-wrapper">
      <div class="continue-card">
        <div class="card-left">
          <div class="icon-box">
            <el-icon :size="24"><Reading /></el-icon>
          </div>
          <div class="learning-info">
            <div class="learning-label">您上次正在学习</div>
            <h4 class="learning-title">{{ recentLearning.courseName || recentLearning.title }}</h4>
            <div class="learning-sub">
              已学习 {{ recentLearning.completedLessons ?? recentLearning.learnedSections ?? 0 }} / {{ recentLearning.totalLessons ?? recentLearning.sections ?? recentLearning.totalSections ?? 10 }} 节
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
            <img :src="course.cover || defaultCover" :alt="course.title" fetchpriority="high" @error="handleImgError" />
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
            <h3 class="section-title">🔥 课程高频点赞榜</h3>
            <span class="section-sub">Redis ZSet 实时热度排行榜 · 学员高频点赞认证</span>
          </div>
        </div>
        <div class="hot-course-list">
          <div
            v-for="(course, index) in (rankingCourses.length ? rankingCourses : hotCourses).slice(0, 5)"
            :key="course.id"
            class="hot-course-item"
            @click="$router.push(`/details?id=${course.id}`)"
          >
            <div class="rank-badge" :class="`rank-${index + 1}`">{{ index + 1 }}</div>
            <div class="course-cover">
              <img :src="course.cover || defaultCover" :alt="course.title" @error="handleImgError" />
            </div>
            <div class="course-info">
              <h4 class="title" :title="course.title">{{ course.title }}</h4>
              <div class="meta">
                <span>{{ course.teacherName }}</span>
                <span><el-icon><Pointer /></el-icon> {{ course.likeCount || course.likes || course.learners || 0 }} 赞</span>
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
              <img :src="course.cover || defaultCover" :alt="course.title" @error="handleImgError" />
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



    <!-- AI 职业成长进阶拓扑全景大屏组件 -->
    <CareerPathDrawer ref="careerPathDrawerRef" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Reading, ArrowRight, Pointer, Loading } from '@element-plus/icons-vue'
import { getClassCategorys, getRecommendClassList, classSeach, getMylessons, getCourseLikeRanking, getPersonalizedRecommendations, getPersonalizedLearningPath } from '@/api/class.js'
import defaultCover from '@/assets/images/courses/default-cover.svg'
import vue3Svg from '@/assets/images/courses/vue3.svg'
import springbootSvg from '@/assets/images/courses/springboot.svg'
import mlSvg from '@/assets/images/courses/ml.svg'
import mysqlSvg from '@/assets/images/courses/mysql.svg'
import AgentReasoningHUD from '@/components/AgentReasoningHUD.vue'
import CareerPathDrawer from '@/components/CareerPathDrawer.vue'

const router = useRouter()

const HOME_CACHE_KEY = 'tianji_portal_home_cache_v2'

const DEFAULT_PREHEAT_CATEGORIES = [
  { id: '1', name: '前端开发', iconText: '前端', count: 48 },
  { id: '2', name: '后端开发', iconText: '后端', count: 72 },
  { id: '3', name: '移动开发', iconText: '移动', count: 28 },
  { id: '4', name: '数据库', iconText: '数据', count: 36 },
  { id: '5', name: '云计算与DevOps', iconText: '云计', count: 32 },
  { id: '6', name: '人工智能', iconText: '人工', count: 42 },
  { id: '7', name: '数据科学', iconText: '数据', count: 26 },
  { id: '8', name: '网络安全', iconText: '网络', count: 20 },
]

const DEFAULT_PREHEAT_COURSES = [
  {
    id: '1',
    title: 'Vue3 从入门到精通',
    cover: vue3Svg,
    teacherName: '张老师',
    price: 19900,
    originalPrice: 39900,
    learners: 12580,
    matchScore: 98,
    matchTag: '🔥 目标岗位强契合',
    recommendReason: '全面掌握Vue3核心语法、组合式API与状态管理',
    difficulty: 1
  },
  {
    id: '4',
    title: 'Java SpringBoot 实战',
    cover: springbootSvg,
    teacherName: '王老师',
    price: 29900,
    originalPrice: 59900,
    learners: 15680,
    matchScore: 95,
    matchTag: '📌 先修必修基石',
    recommendReason: '从零开始学习SpringBoot，掌握微服务架构设计',
    difficulty: 2
  },
  {
    id: '10',
    title: '机器学习入门',
    cover: mlSvg,
    teacherName: '黄老师',
    price: 34900,
    originalPrice: 69900,
    learners: 14560,
    matchScore: 92,
    matchTag: '💡 关键技能补齐',
    recommendReason: 'Python机器学习、Scikit-learn、TensorFlow实战',
    difficulty: 1
  },
  {
    id: '8',
    title: 'MySQL 数据库优化',
    cover: mysqlSvg,
    teacherName: '刘老师',
    price: 16900,
    originalPrice: 33900,
    learners: 11230,
    matchScore: 90,
    matchTag: '🚀 架构突破攻坚',
    recommendReason: '索引优化、查询优化、分库分表、主从复制',
    difficulty: 3
  }
]

const readHomeCache = () => {
  try {
    const raw = localStorage.getItem(HOME_CACHE_KEY)
    if (raw) {
      const data = JSON.parse(raw)
      // 彻底清理历史缓存在公共本地存储中的旧 recentLearning，杜绝跨用户污染
      if (data && data.recentLearning) {
        delete data.recentLearning
        localStorage.setItem(HOME_CACHE_KEY, JSON.stringify(data))
      }
      return data
    }
  } catch (e) {}
  return null
}

const initialHomeData = readHomeCache()

const categories = ref(initialHomeData?.categories?.length ? initialHomeData.categories : DEFAULT_PREHEAT_CATEGORIES)
const recommendCourses = ref(initialHomeData?.recommendCourses?.length ? initialHomeData.recommendCourses : DEFAULT_PREHEAT_COURSES)
const personalizedCourses = ref(initialHomeData?.personalizedCourses?.length ? initialHomeData.personalizedCourses : DEFAULT_PREHEAT_COURSES)
const hotCourses = ref(initialHomeData?.hotCourses?.length ? initialHomeData.hotCourses : DEFAULT_PREHEAT_COURSES)
const rankingCourses = ref(initialHomeData?.rankingCourses?.length ? initialHomeData.rankingCourses : DEFAULT_PREHEAT_COURSES)
const newCourses = ref(initialHomeData?.newCourses?.length ? initialHomeData.newCourses : DEFAULT_PREHEAT_COURSES)
// 用户学习记录属于私有登录态数据，严禁从公共缓存读取，初始严格置为 null
const recentLearning = ref(null)

let saveTimer = null
const persistCache = () => {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    try {
      localStorage.setItem(HOME_CACHE_KEY, JSON.stringify({
        categories: categories.value,
        recommendCourses: recommendCourses.value,
        personalizedCourses: personalizedCourses.value,
        hotCourses: hotCourses.value,
        rankingCourses: rankingCourses.value,
        newCourses: newCourses.value,
        timestamp: Date.now()
      }))
    } catch (e) {}
  }, 300)
}

const careerPathDrawerRef = ref(null)

const openLearningPathModal = () => {
  careerPathDrawerRef.value?.openDrawer()
}

const handleRecalculateRecommendations = async (targetRole) => {
  try {
    const res = await getPersonalizedRecommendations({ limit: 4, targetRole })
    if (res && res.code === 200) {
      const pRows = normalizeRows(res)
      if (pRows.length) {
        personalizedCourses.value = pRows
      }
    }
  } catch (e) {
    console.error('重新计算个性化推荐失败:', e)
  }
}

const goCourseDetail = (courseId) => {
  if (!courseId) return
  router.push(`/details?id=${courseId}`)
}

const handleImgError = (e) => {
  if (e?.target && e.target.src !== defaultCover) {
    e.target.src = defaultCover
  }
}

const normalizeCourse = (course = {}) => {
  let price = Number(course.price || 0)
  let originalPrice = Number(course.originalPrice ?? course.price ?? 0)
  // 金额单位归一化保护：全站统一以分（cents）为标准单位。
  // 若接口返回大于 0 且小于 1000 的元单位数值（如 199 元），防御性自动换算为分（19900 分）
  if (price > 0 && price < 1000) {
    price = Math.round(price * 100)
  }
  if (originalPrice > 0 && originalPrice < 1000) {
    originalPrice = Math.round(originalPrice * 100)
  }
  return {
    ...course,
    id: course.id,
    title: course.title || course.courseName || '未命名课程',
    cover: course.cover || course.coverUrl || defaultCover,
    teacherName: course.teacherName || '讲师团队',
    price,
    originalPrice,
    learners: Number(course.learners ?? course.learnerCount ?? 0)
  }
}

const normalizeRows = (response) => {
  const data = response?.data
  const rows = Array.isArray(data) ? data : (data?.list || data?.rows || [])
  return rows.map(normalizeCourse)
}



const calcProgress = (item) => {
  if (!item) return 0
  if (item.progress != null || item.progressPercent != null) {
    return Math.min(100, Math.max(0, Math.round(Number(item.progress ?? item.progressPercent ?? 0))))
  }
  const learned = Number(item.completedLessons ?? item.learnedSections ?? 0)
  const total = Number(item.totalLessons ?? item.sections ?? item.totalSections ?? 0)
  if (total <= 0) return 0
  return Math.min(100, Math.max(0, Math.round((learned / total) * 100)))
}

const goLearning = (item) => {
  const cId = item?.courseId || item?.id
  if (cId) {
    router.push({ path: '/learning/index', query: { courseId: cId, id: cId } })
  }
}

onMounted(() => {
  // 1. 课程分类与统计 (轻量快速，~20ms)
  getClassCategorys({ includeDisabled: false }).then(res => {
    if (res?.code === 200) {
      const data = res.data
      const rows = Array.isArray(data) ? data : (data?.list || [])
      categories.value = rows.filter(item => Number(item.status ?? 1) === 1).map((item) => ({
        ...item,
        name: item.name || item.categoryName,
        iconText: (item.name || item.categoryName || '课').slice(0, 2),
        count: Number(item.courseCount ?? 0)
      }))
      persistCache()
    }
  }).catch(e => console.warn('分类加载异常:', e))

  // 2. 首页核心重磅推荐 (优先保证主屏，~15ms)
  getRecommendClassList('home').then(res => {
    if (res?.code === 200) {
      const rows = normalizeRows(res)
      if (rows.length) {
        recommendCourses.value = rows
        // 若当前未缓存个性化推荐，立即秒级平滑兜底呈现
        if (!personalizedCourses.value.length) {
          personalizedCourses.value = rows.slice(0, 4).map((c, idx) => ({
            ...c,
            matchTag: idx === 0 ? '综合推荐' : '热门匹配',
            matchScore: 95 - idx * 5,
            recommendReason: '根据全站高频热度与实战技能匹配推荐',
            difficulty: c.difficulty || 2
          }))
        }
        persistCache()
      }
    }
  }).catch(e => console.warn('推荐好课加载异常:', e))

  // 3. 热门好课榜 (~15ms)
  getRecommendClassList('hot').then(res => {
    if (res?.code === 200) {
      const rows = normalizeRows(res)
      if (rows.length) {
        hotCourses.value = rows
        persistCache()
      }
    }
  }).catch(e => console.warn('热门好课加载异常:', e))

  // 4. 最新上线好课 (~15ms)
  getRecommendClassList('new').then(res => {
    if (res?.code === 200) {
      const rows = normalizeRows(res)
      if (rows.length) {
        newCourses.value = rows
        persistCache()
      }
    }
  }).catch(e => console.warn('最新课程加载异常:', e))

  // 5. 课程高频点赞榜 (Redis ZSet, ~10ms)
  getCourseLikeRanking({ limit: 10 }).then(res => {
    if (res?.code === 200) {
      const rows = normalizeRows(res)
      if (rows.length) {
        rankingCourses.value = rows
        persistCache()
      }
    }
  }).catch(e => console.warn('点赞排行榜加载异常:', e))

  // 6. 个性化多智能体推荐 (独立异步解耦，静默刷新)
  getPersonalizedRecommendations({ limit: 4 }).then(res => {
    if (res?.code === 200) {
      const pRows = normalizeRows(res)
      if (pRows.length) {
        personalizedCourses.value = pRows
        persistCache()
      }
    }
  }).catch(e => console.warn('个性化推荐加载异常:', e))

  // 7. 用户最近学习历史 (登录专享，严格基于真实接口数据)
  const loadRecentLearning = () => {
    const token = sessionStorage.getItem('token')
    if (!token) {
      recentLearning.value = null
      return
    }
    getMylessons({ pageNo: 1, pageSize: 5 }).then(lessonRes => {
      if (lessonRes?.code !== 200) {
        recentLearning.value = null
        return
      }
      const data = lessonRes.data
      const lessons = Array.isArray(data) ? data : (data?.list || data?.rows || [])
      if (lessons && lessons.length > 0) {
        const first = lessons[0]
        recentLearning.value = {
          ...first,
          courseId: first.courseId || first.id,
          courseName: first.courseName || first.title || '未命名课程',
          completedLessons: Number(first.completedLessons ?? first.learnedSections ?? 0),
          totalLessons: Number(first.totalLessons ?? first.sections ?? first.totalSections ?? 10),
          progress: Math.min(100, Math.max(0, Math.round(Number(first.progress ?? first.progressPercent ?? 0))))
        }
      } else {
        // 新用户或无真实学习记录，严格置空，绝不展示虚假数据
        recentLearning.value = null
      }
    }).catch(() => {
      recentLearning.value = null
    })
  }

  loadRecentLearning()
  window.addEventListener('user-profile-updated', loadRecentLearning)
  window.addEventListener('cart-updated', loadRecentLearning)

  onUnmounted(() => {
    window.removeEventListener('user-profile-updated', loadRecentLearning)
    window.removeEventListener('cart-updated', loadRecentLearning)
  })
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

/* 顶部核心区：AI 多智能体协同决策看板 */
.hero-agent-section {
  padding-top: 24px;
  margin-bottom: 24px;
}

.personalized-section {
  margin-bottom: 36px;
}

/* 继续学习快捷条 */
.continue-learning-wrapper {
  margin-top: 0;
  margin-bottom: 36px;
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
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 1px 3px 0 rgba(15, 23, 42, 0.04);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;

  &:hover {
    border-color: #93C5FD;
    box-shadow: 0 10px 25px -4px rgba(37, 99, 235, 0.08);
    transform: translateY(-2px);
    .course-title {
      color: #2563EB;
    }
  }

  &.personalized-card {
    border-color: #E2E8F0;
    box-shadow: 0 2px 8px -2px rgba(15, 23, 42, 0.05);

    &:hover {
      border-color: #60A5FA;
      box-shadow: 0 12px 24px -4px rgba(37, 99, 235, 0.12);
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

      &.match {
        background: #0284C7;
      }
    }

    .match-score-badge {
      position: absolute;
      top: 10px;
      right: 10px;
      padding: 3px 10px;
      border-radius: 20px;
      font-size: 11px;
      font-weight: 700;
      background: rgba(255, 255, 255, 0.92);
      color: #059669;
      backdrop-filter: blur(6px);
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
    }
  }

  .course-info {
    padding: 16px;
    display: flex;
    flex-direction: column;
    flex: 1;

    .course-title {
      font-size: 14px;
      font-weight: 600;
      color: #0F172A;
      line-height: 1.45;
      height: 40px;
      margin-bottom: 10px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      transition: color 0.2s ease;
    }

    .recommend-reason-row {
      display: flex;
      align-items: center;
      gap: 6px;
      background: #F0FDF4;
      border: 1px solid #DCFCE7;
      border-radius: 6px;
      padding: 5px 8px;
      margin-bottom: 12px;
      font-size: 11px;
      color: #166534;
      line-height: 1.4;

      .reason-icon {
        font-size: 12px;
        flex-shrink: 0;
      }
      .reason-text {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .course-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
      color: #64748B;
      margin-bottom: 12px;

      .difficulty-tag {
        display: inline-block;
        padding: 1px 6px;
        background: #F1F5F9;
        border-radius: 3px;
        font-size: 11px;
        color: #475569;
      }
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

}
</style>
