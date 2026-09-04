// Mock 数据拦截器
import categoryData from './category.json'
import coursesData from './courses.json'
import teacherData from './teacher.json'
import userData from './user.json'
import couponData from './coupon.json'
import orderData from './order.json'
import learningData from './learning.json'
import examData from './exam.json'
import homeData from './home.json'

// Mock 用户数据（支持登录）
const mockUsers = {
  'admin': {
    id: 1,
    username: 'admin',
    nickname: '张三',
    avatar: '/src/assets/images/users/default-avatar.svg',
    phone: '13800138000',
    email: 'admin@zhiwenxueban.com',
    token: 'mock-token-admin-' + Date.now(),
    roles: ['admin']
  },
  'user': {
    id: 2,
    username: 'user',
    nickname: '普通用户',
    avatar: '/src/assets/images/users/default-avatar.svg',
    phone: '13800138001',
    email: 'user@zhiwenxueban.com',
    token: 'mock-token-user-' + Date.now(),
    roles: ['user']
  }
}

// Mock 数据映射
const mockDataMap = {
  // 课程分类
  '/cs/categorys/list': categoryData,
  '/cs/categorys/all': categoryData,
  // 课程列表
  '/cs/courses/page': coursesData,
  // 教师列表
  '/cs/courses/teachers': teacherData,
  // 用户信息
  '/as/accounts/admin/login': {
    code: 200,
    message: 'success',
    data: {
      token: 'mock-token-' + Date.now(),
      userId: 1
    }
  },
  '/as/accounts/login': {
    code: 200,
    message: 'success',
    data: {
      token: 'mock-token-' + Date.now(),
      userId: 1
    }
  },
  '/as/accounts/user/info': userData,
  // 优惠券
  '/prs/coupons/list': couponData,
  '/prs/user-coupons/page': couponData,
  // 订单
  '/ts/orders/page': orderData,
  // 学习记录
  '/ls/lessons/page': learningData,
  '/ls/lessons/now': learningData,
  // 考试记录
  '/es/exams/page': examData,
  // 首页数据
  '/api/home': homeData,
  '/ss/recommend/1': homeData.data.recommendCourses,
  '/ss/recommend/2': homeData.data.hotCourses,
  '/ss/recommend/3': homeData.data.newCourses,
  // 学习计划
  '/ls/plans': {
    code: 200,
    message: 'success',
    data: [
      { id: 1, courseId: 1, courseName: 'Vue3 从入门到精通', planDate: '2024-03-25', status: 1 },
      { id: 2, courseId: 4, courseName: 'Java SpringBoot 实战', planDate: '2024-03-26', status: 0 }
    ]
  },
  // 签到记录
  '/ls/sign-records': {
    code: 200,
    message: 'success',
    data: {
      totalDays: 15,
      continuousDays: 5,
      todaySigned: false,
      records: [
        { date: '2026-05-10', points: 10 },
        { date: '2026-05-10', points: 10 },
        { date: '2026-05-10', points: 10 }
      ]
    }
  },
  // 积分信息
  '/ls/points/today': {
    code: 200,
    message: 'success',
    data: {
      todayPoints: 0,
      totalPoints: 5860,
      rank: 128
    }
  }
}

// 解析请求路径
function getUrlPath(url) {
  try {
    // 移除查询参数
    let path = url.split('?')[0]

    // 移除协议和域名
    if (path.includes('://')) {
      const urlObj = new URL(path)
      path = urlObj.pathname
    }

    // 移除 baseURL 前缀（如 /api）
    path = path.replace(/^\/api/, '')

    // 确保路径以 / 开头
    if (!path.startsWith('/')) {
      path = '/' + path
    }

    console.log(`[Mock] Parsed URL path: ${path}`)
    return path
  } catch (e) {
    console.error('[Mock] URL parse error:', e)
    return url
  }
}

// 获取 Mock 数据
export function getMockData(url, method, params) {
  const path = getUrlPath(url)
  console.log(`[Mock] Looking for mock data: ${method} ${path}`)

  // 处理登录请求
  if (path.includes('/as/accounts/login') || path.includes('/as/accounts/admin/login')) {
    console.log(`[Mock] Login request:`, params)
    const username = params?.username || params?.phone || 'admin'
    const user = mockUsers[username] || mockUsers['admin']
    return {
      code: 200,
      message: 'success',
      data: {
        token: user.token,
        userId: user.id
      }
    }
  }

  // 处理用户信息请求
  if (path.includes('/as/accounts/user/info') || path.includes('/as/accounts/info')) {
    const token = params?.token || ''
    // 根据 token 查找用户
    for (const [key, user] of Object.entries(mockUsers)) {
      if (token.includes(key)) {
        return {
          code: 200,
          message: 'success',
          data: user
        }
      }
    }
    // 默认返回张三信息
    return {
      code: 200,
      message: 'success',
      data: mockUsers['admin']
    }
  }

  // 遍历 mockDataMap 查找匹配
  for (const [key, value] of Object.entries(mockDataMap)) {
    if (path === key || path.startsWith(key + '/') || path.includes(key)) {
      console.log(`[Mock] Found mock data for: ${key}`)
      return JSON.parse(JSON.stringify(value))
    }
  }

  // 处理带参数的请求
  if (path.includes('/cs/courses/baseInfo/')) {
    const id = parseInt(path.split('/').pop())
    const course = coursesData.data.list.find(c => c.id === id)
    if (course) {
      return {
        code: 200,
        message: 'success',
        data: course
      }
    }
  }

  if (path.includes('/cs/categorys/')) {
    const id = parseInt(path.split('/').pop())
    const category = categoryData.data.find(c => c.id === id)
    if (category) {
      return {
        code: 200,
        message: 'success',
        data: category
      }
    }
  }

  // 处理课程详情
  if (path.includes('/cs/courses/')) {
    const id = parseInt(path.split('/').pop())
    const course = coursesData.data.list.find(c => c.id === id)
    if (course) {
      return {
        code: 200,
        message: 'success',
        data: course
      }
    }
  }

  // 默认返回空数据
  console.warn(`[Mock] No mock data found for: ${method} ${path}`)
  return null
}

// 检查是否启用 Mock
export function isMockEnabled() {
  // Mock 仅作为历史兼容工具保留，必须显式开启，避免开发环境误拦截真实接口。
  return import.meta.env.VITE_USE_MOCK === 'true'
}
