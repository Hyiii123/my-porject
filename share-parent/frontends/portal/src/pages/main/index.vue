<template>
  <div class="home-container">
    <!-- 轮播图 -->
    <div class="banner-section">
      <el-carousel height="400px" :interval="5000" arrow="hover">
        <el-carousel-item v-for="banner in banners" :key="banner.id">
          <div class="banner-item" :style="{ background: banner.bgColor }">
            <div class="banner-content">
              <h2>{{ banner.title }}</h2>
              <p>{{ banner.subtitle }}</p>
              <el-button type="primary" size="large" @click="goBanner(banner)">立即学习</el-button>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- 课程分类 -->
    <div class="section container">
      <div class="section-header">
        <h3>课程分类</h3>
        <el-button type="primary" link @click="$router.push('/search/index')">查看全部 ></el-button>
      </div>
      <div class="category-grid">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="category-item"
          @click="$router.push(`/search/index?categoryId=${cat.id}`)"
        >
          <div class="category-icon" :style="{ background: cat.bgColor }">
            <span class="icon-text">{{ cat.iconText }}</span>
          </div>
          <span class="category-name">{{ cat.name }}</span>
          <span class="category-count">{{ cat.count }}门课程</span>
        </div>
      </div>
    </div>

    <!-- 推荐课程 -->
    <div class="section container">
      <div class="section-header">
        <h3>推荐课程</h3>
        <el-button type="primary" link @click="$router.push('/search/index')">查看全部 ></el-button>
      </div>
      <div class="course-grid">
        <div
          v-for="course in recommendCourses"
          :key="course.id"
          class="course-card"
          @click="$router.push(`/details/index?id=${course.id}`)"
        >
          <div class="course-cover">
            <img :src="course.cover" :alt="course.title" />
            <div class="course-badge" v-if="course.price === 0">免费</div>
          </div>
          <div class="course-info">
            <h4 class="course-title">{{ course.title }}</h4>
            <div class="course-meta">
              <span class="teacher">{{ course.teacherName }}</span>
              <span class="learners">{{ course.learners }}人在学</span>
            </div>
            <div class="course-price">
              <span v-if="course.price > 0" class="price">¥{{ (course.price / 100).toFixed(0) }}</span>
              <span v-else class="free">免费</span>
              <span v-if="course.originalPrice > course.price" class="original-price">¥{{ (course.originalPrice / 100).toFixed(0) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 热门课程 -->
    <div class="section container">
      <div class="section-header">
        <h3>热门课程</h3>
        <el-button type="primary" link @click="$router.push('/search/index')">查看全部 ></el-button>
      </div>
      <div class="hot-course-list">
        <div
          v-for="(course, index) in hotCourses"
          :key="course.id"
          class="hot-course-item"
          @click="$router.push(`/details/index?id=${course.id}`)"
        >
          <div class="rank" :class="{ 'top-3': index < 3 }">{{ index + 1 }}</div>
          <div class="course-cover">
            <img :src="course.cover" :alt="course.title" />
          </div>
          <div class="course-info">
            <h4>{{ course.title }}</h4>
            <div class="meta">
              <span>{{ course.teacherName }}</span>
              <span>{{ course.learners }}人在学</span>
            </div>
          </div>
          <div class="price">¥{{ (course.price / 100).toFixed(0) }}</div>
        </div>
      </div>
    </div>

    <!-- 新课推荐 -->
    <div class="section container">
      <div class="section-header">
        <h3>新课推荐</h3>
        <el-button type="primary" link @click="$router.push('/search/index')">查看全部 ></el-button>
      </div>
      <div class="course-grid">
        <div
          v-for="course in newCourses"
          :key="course.id"
          class="course-card"
          @click="$router.push(`/details/index?id=${course.id}`)"
        >
          <div class="course-cover">
            <img :src="course.cover" :alt="course.title" />
            <div class="new-badge">新课</div>
          </div>
          <div class="course-info">
            <h4 class="course-title">{{ course.title }}</h4>
            <div class="course-meta">
              <span class="teacher">{{ course.teacherName }}</span>
              <span class="learners">{{ course.learners }}人在学</span>
            </div>
            <div class="course-price">
              <span class="price">¥{{ (course.price / 100).toFixed(0) }}</span>
              <span v-if="course.originalPrice > course.price" class="original-price">¥{{ (course.originalPrice / 100).toFixed(0) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据统计 -->
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
import { getClassCategorys, getRecommendClassList, classSeach } from '@/api/class.js'
import { getBanners } from '@/api/home.js'

const router = useRouter()

const banners = ref([])
const categories = ref([])
const recommendCourses = ref([])
const hotCourses = ref([])
const newCourses = ref([])
const allCourses = ref([])

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
  { value: allCourses.value.length, label: '精品课程' },
  { value: new Set(allCourses.value.map(course => course.teacherName)).size, label: '专业讲师' },
  { value: allCourses.value.length ? `${(allCourses.value.reduce((sum, course) => sum + Number(course.rating || 0), 0) / allCourses.value.length).toFixed(1)}分` : '--', label: '课程平均评分' }
])

const goBanner = (banner) => {
  router.push(banner.link || '/search/index')
}

onMounted(async () => {
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
    const gradients = [
      'linear-gradient(135deg, #42b883 0%, #35495e 100%)',
      'linear-gradient(135deg, #6db33f 0%, #333333 100%)',
      'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      'linear-gradient(135deg, #ff6f00 0%, #ff9100 100%)'
    ]
    banners.value = rows.map((banner, index) => ({ ...banner, subtitle: banner.subtitle || '精选课程，随时开始学习', bgColor: gradients[index % gradients.length] }))
  }
  if (categoryResponse.status === 'fulfilled' && categoryResponse.value?.code === 200) {
    const data = categoryResponse.value.data
    const rows = Array.isArray(data) ? data : (data?.list || [])
    const colors = ['#667eea,#764ba2', '#f093fb,#f5576c', '#4facfe,#00f2fe', '#43e97b,#38f9d7', '#fa709a,#fee140', '#a18cd1,#fbc2eb']
    categories.value = rows.filter(item => Number(item.status ?? 1) === 1).map((item, index) => ({
      ...item,
      name: item.name || item.categoryName,
      iconText: (item.name || item.categoryName || '课').slice(0, 2),
      count: Number(item.courseCount ?? 0),
      bgColor: `linear-gradient(135deg, ${colors[index % colors.length]})`
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
})
</script>

<style scoped>
.home-container {
  background: #f5f7fa;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* 轮播图 */
.banner-section {
  margin-bottom: 40px;
}

.banner-item {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-content {
  text-align: center;
  color: #fff;
}

.banner-content h2 {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 16px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.banner-content p {
  font-size: 18px;
  margin-bottom: 24px;
  opacity: 0.9;
}

/* 分类 */
.section {
  margin-bottom: 40px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-header h3 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
}

.category-item {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.category-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.category-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
}

.icon-text {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.category-name {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.category-count {
  font-size: 12px;
  color: #909399;
}

/* 课程卡片 */
.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.course-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.course-cover {
  position: relative;
  height: 160px;
  overflow: hidden;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-badge,
.new-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
}

.course-badge {
  background: #67c23a;
}

.new-badge {
  background: #f56c6c;
}

.course-info {
  padding: 16px;
}

.course-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.course-price {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}

.free {
  font-size: 18px;
  font-weight: 700;
  color: #67c23a;
}

.original-price {
  font-size: 13px;
  color: #c0c4cc;
  text-decoration: line-through;
}

/* 热门课程 */
.hot-course-list {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.hot-course-item {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.hot-course-item:last-child {
  border-bottom: none;
}

.hot-course-item:hover {
  background: #f8f9fa;
}

.rank {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #909399;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  margin-right: 16px;
}

.rank.top-3 {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.hot-course-item .course-cover {
  width: 80px;
  height: 45px;
  border-radius: 6px;
  overflow: hidden;
  margin-right: 16px;
}

.hot-course-item .course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hot-course-item .course-info {
  flex: 1;
  padding: 0;
}

.hot-course-item .course-info h4 {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px;
}

.hot-course-item .meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #909399;
}

.hot-course-item .price {
  font-size: 16px;
  font-weight: 700;
  color: #f56c6c;
  margin-left: 16px;
}

/* 数据统计 */
.stats-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60px 0;
  margin-top: 40px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 40px;
  text-align: center;
  color: #fff;
}

.stat-number {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 16px;
  opacity: 0.9;
}

/* 响应式 */
@media (max-width: 1200px) {
  .category-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .course-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
