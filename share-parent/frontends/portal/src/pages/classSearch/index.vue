<template>
  <div class="search-container">
    <!-- 搜索头部 -->
    <div class="search-header">
      <div class="container">
        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索课程"
            size="large"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            clearable
          >
            <template #append>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
        <div class="search-stats" v-if="keyword">
          共找到 <span class="count">{{ total }}</span> 门 "{{ keyword }}" 相关课程
        </div>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="container filter-section">
      <div class="filter-group">
        <span class="filter-label">课程分类：</span>
        <div class="filter-options">
          <span
            v-for="cat in categories"
            :key="cat.id"
            :class="['filter-item', { active: selectedCategory === cat.id }]"
            @click="selectCategory(cat.id)"
          >
            {{ cat.name }}
          </span>
        </div>
      </div>
      <div class="filter-group">
        <span class="filter-label">价格：</span>
        <div class="filter-options">
          <span
            v-for="price in priceOptions"
            :key="price.value"
            :class="['filter-item', { active: selectedPrice === price.value }]"
            @click="selectPrice(price.value)"
          >
            {{ price.label }}
          </span>
        </div>
      </div>
    </div>

    <!-- 排序和结果 -->
    <div class="container result-section">
      <div class="result-header">
        <div class="sort-options">
          <span
            v-for="sort in sortOptions"
            :key="sort.value"
            :class="['sort-item', { active: currentSort === sort.value }]"
            @click="handleSort(sort.value)"
          >
            {{ sort.label }}
          </span>
        </div>
        <div class="result-count">
          共 {{ total }} 门课程
        </div>
      </div>

      <!-- 课程列表 -->
      <div class="course-grid" v-if="courseList.length > 0">
        <div
          v-for="course in courseList"
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

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <el-empty description="暂无搜索结果" />
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next, jumper"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { classSeach, getClassCategorys } from '@/api/class.js'

const route = useRoute()
const router = useRouter()

// 搜索关键词
const keyword = ref('')

// 分类数据由教育服务提供；“全部”是筛选器的固定入口，不是业务数据。
const categories = ref([{ id: '', name: '全部' }])

// 价格选项
const priceOptions = [
  { value: '', label: '全部' },
  { value: 'free', label: '免费' },
  { value: 'paid', label: '付费' }
]

// 排序选项
const sortOptions = [
  { value: 'recommend', label: '推荐' },
  { value: 'newest', label: '最新' },
  { value: 'hottest', label: '最热' },
  { value: 'price_asc', label: '价格↑' },
  { value: 'price_desc', label: '价格↓' }
]

// 筛选状态
const selectedCategory = ref('')
const selectedPrice = ref('')
const currentSort = ref('recommend')

// 分页
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 课程列表
const courseList = ref([])
const loading = ref(false)

const normalizeCourse = (course = {}) => ({
  ...course,
  id: course.id,
  title: course.title || course.courseName || '未命名课程',
  cover: course.cover || course.coverUrl || '',
  teacherName: course.teacherName || '讲师团队',
  categoryId: String(course.categoryId ?? ''),
  price: Number(course.price || 0),
  originalPrice: Number(course.originalPrice ?? course.price ?? 0),
  learners: Number(course.learners ?? course.learnerCount ?? 0)
})

// 搜索课程
const searchCourses = async () => {
  loading.value = true
  const sortBy = {
    newest: 'newest',
    hottest: 'hot',
    price_asc: 'priceAsc',
    price_desc: 'priceDesc'
  }[currentSort.value]
  try {
    const response = await classSeach({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      categoryId: selectedCategory.value || undefined,
      priceType: selectedPrice.value || undefined,
      sortBy
    })
    if (response?.code !== 200) throw new Error(response?.msg || '课程加载失败')
    const data = response.data || {}
    const rows = Array.isArray(data) ? data : (data.list || data.rows || [])
    courseList.value = rows.map(normalizeCourse)
    total.value = Number(Array.isArray(data) ? rows.length : (data.total ?? rows.length))
  } catch (error) {
    courseList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const response = await getClassCategorys({ includeDisabled: false })
    if (response?.code !== 200) throw new Error(response?.msg || '分类加载失败')
    const rows = Array.isArray(response.data) ? response.data : (response.data?.list || [])
    categories.value = [
      { id: '', name: '全部' },
      ...rows.filter(item => Number(item.status ?? 1) === 1).map(item => ({
        id: String(item.id),
        name: item.name || item.categoryName
      }))
    ]
  } catch (error) {
    categories.value = [{ id: '', name: '全部' }]
  }
}

// 选择分类
const selectCategory = (categoryId) => {
  selectedCategory.value = categoryId
  currentPage.value = 1
  searchCourses()
}

// 选择价格
const selectPrice = (price) => {
  selectedPrice.value = price
  currentPage.value = 1
  searchCourses()
}

// 排序
const handleSort = (sort) => {
  currentSort.value = sort
  currentPage.value = 1
  searchCourses()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  searchCourses()
}

// 分页
const handlePageChange = () => {
  searchCourses()
  window.scrollTo(0, 0)
}

// 监听路由参数
watch(() => [route.query.keyword, route.query.categoryId], ([routeKeyword, routeCategory]) => {
  keyword.value = routeKeyword ? String(routeKeyword) : ''
  selectedCategory.value = routeCategory ? String(routeCategory) : ''
  currentPage.value = 1
  searchCourses()
}, { immediate: true })

onMounted(loadCategories)
</script>

<style scoped>
.search-container {
  background: #f5f7fa;
  min-height: 100vh;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* 搜索头部 */
.search-header {
  background: #fff;
  padding: 30px 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.search-box {
  max-width: 600px;
  margin: 0 auto;
}

.search-stats {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #606266;
}

.search-stats .count {
  color: #409eff;
  font-weight: 600;
}

/* 筛选条件 */
.filter-section {
  background: #fff;
  padding: 20px;
  margin-top: 20px;
  border-radius: 12px;
}

.filter-group {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
}

.filter-group:last-child {
  margin-bottom: 0;
}

.filter-label {
  width: 80px;
  font-size: 14px;
  color: #606266;
  line-height: 32px;
  flex-shrink: 0;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-item {
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-item:hover {
  color: #409eff;
  background: #ecf5ff;
}

.filter-item.active {
  color: #fff;
  background: #409eff;
}

/* 结果区域 */
.result-section {
  margin-top: 20px;
  padding-bottom: 40px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.sort-options {
  display: flex;
  gap: 16px;
}

.sort-item {
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  padding: 8px 0;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.sort-item:hover {
  color: #409eff;
}

.sort-item.active {
  color: #409eff;
  border-bottom-color: #409eff;
}

.result-count {
  font-size: 14px;
  color: #909399;
}

/* 课程网格 */
.course-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
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
  height: 140px;
  overflow: hidden;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 12px;
  border-radius: 4px;
  background: #67c23a;
  color: #fff;
  font-size: 12px;
}

.course-info {
  padding: 16px;
}

.course-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  height: 42px;
}

.course-meta {
  display: flex;
  gap: 12px;
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

/* 空状态 */
.empty-state {
  padding: 80px 0;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .course-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .course-grid {
    grid-template-columns: 1fr;
  }

  .filter-group {
    flex-direction: column;
  }

  .filter-label {
    margin-bottom: 8px;
  }
}
</style>
