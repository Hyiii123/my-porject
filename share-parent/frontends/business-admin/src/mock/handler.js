// Mock 请求处理器 - 使用本地数据库
import { db, crud, getStatistics, saveData, resetData, resetAllData } from './db'
import { customerService } from './customerService'

// 模拟延迟
function delay(ms = 100) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// 创建响应
function createResponse(data, code = 200, message = 'success') {
  return { code, message, data }
}

// 路由处理器映射
const handlers = {
  // ============ 登录相关 ============
  'POST /as/accounts/login': async (params, data) => {
    const { username, password } = data || {}
    const user = db.users.find(u => u.username === username || u.phone === username)
    if (user) {
      return createResponse({
        token: `mock-token-${user.id}-${Date.now()}`,
        userId: user.id,
        nickname: user.nickname
      })
    }
    return createResponse(null, 400, '用户名或密码错误')
  },

  'POST /as/accounts/admin/login': async (params, data) => {
    return handlers['POST /as/accounts/login'](params, data)
  },

  'GET /as/accounts/user/info': async (params) => {
    const userId = params.userId || 1
    const user = db.users.find(u => u.id === userId) || db.users[0]
    return createResponse(user)
  },

  'GET /as/accounts/info': async (params) => {
    return handlers['GET /as/accounts/user/info'](params)
  },

  // ============ 课程分类 ============
  'GET /cs/categorys/list': async (params) => {
    return createResponse(db.categories)
  },

  'GET /cs/categorys/all': async (params) => {
    return createResponse(db.categories)
  },

  'POST /cs/categorys/add': async (params, data) => {
    return crud.add('categories', data)
  },

  'PUT /cs/categorys/update': async (params, data) => {
    return crud.update('categories', data.id, data)
  },

  'DELETE /cs/categorys/:id': async (params) => {
    return crud.delete('categories', params.id)
  },

  // ============ 课程管理 ============
  'GET /cs/courses/page': async (params) => {
    return crud.list('courses', params)
  },

  'GET /cs/courses/baseInfo/:id': async (params) => {
    const result = crud.get('courses', params.id)
    return result
  },

  'POST /cs/courses/baseInfo/save': async (params, data) => {
    if (data.id) {
      return crud.update('courses', data.id, data)
    }
    return crud.add('courses', data)
  },

  'DELETE /cs/courses/delete/:id': async (params) => {
    return crud.delete('courses', params.id)
  },

  'POST /cs/courses/upShelf': async (params, data) => {
    return crud.update('courses', data.id, { status: 1 })
  },

  'POST /cs/courses/downShelf': async (params, data) => {
    return crud.update('courses', data.id, { status: 0 })
  },

  // ============ 题目管理 ============
  'GET /es/questions/page': async (params) => {
    return crud.list('questions', params)
  },

  'GET /es/questions/list': async (params) => {
    return crud.list('questions', params)
  },

  'POST /es/questions/add': async (params, data) => {
    return crud.add('questions', data)
  },

  'PUT /es/questions/update': async (params, data) => {
    return crud.update('questions', data.id, data)
  },

  'DELETE /es/questions/:id': async (params) => {
    return crud.delete('questions', params.id)
  },

  // ============ 用户管理 ============
  'GET /as/accounts/page': async (params) => {
    return crud.list('users', params)
  },

  'GET /as/accounts/list': async (params) => {
    return crud.list('users', params)
  },

  'POST /as/accounts/add': async (params, data) => {
    return crud.add('users', data)
  },

  'PUT /as/accounts/update': async (params, data) => {
    return crud.update('users', data.id, data)
  },

  'DELETE /as/accounts/:id': async (params) => {
    return crud.delete('users', params.id)
  },

  // ============ 优惠券管理 ============
  'GET /prs/coupons/list': async (params) => {
    return crud.list('coupons', params)
  },

  'GET /prs/coupons/page': async (params) => {
    return crud.list('coupons', params)
  },

  'POST /prs/coupons/add': async (params, data) => {
    return crud.add('coupons', data)
  },

  'PUT /prs/coupons/update': async (params, data) => {
    return crud.update('coupons', data.id, data)
  },

  'DELETE /prs/coupons/:id': async (params) => {
    return crud.delete('coupons', params.id)
  },

  // ============ 订单管理 ============
  'GET /ts/orders/page': async (params) => {
    return crud.list('orders', params)
  },

  'GET /ts/orders/list': async (params) => {
    return crud.list('orders', params)
  },

  // ============ 学习记录 ============
  'GET /ls/lessons/page': async (params) => {
    return crud.list('learningRecords', params)
  },

  'GET /ls/lessons/now': async (params) => {
    const records = db.learningRecords.filter(r => r.progress < 100)
    return createResponse(records)
  },

  // ============ 问答 ============
  'GET /ls/qa/list': async (params) => {
    return crud.list('qaList', params)
  },

  'POST /ls/qa/add': async (params, data) => {
    return crud.add('qaList', data)
  },

  // ============ 笔记 ============
  'GET /ls/notes/list': async (params) => {
    return crud.list('notes', params)
  },

  'POST /ls/notes/add': async (params, data) => {
    return crud.add('notes', data)
  },

  'PUT /ls/notes/update': async (params, data) => {
    return crud.update('notes', data.id, data)
  },

  'DELETE /ls/notes/:id': async (params) => {
    return crud.delete('notes', params.id)
  },

  // ============ 积分 ============
  'GET /ls/points/rank': async (params) => {
    return createResponse(db.points)
  },

  'GET /ls/sign-records': async (params) => {
    return createResponse({
      totalDays: 15,
      continuousDays: 5,
      todaySigned: false
    })
  },

  'POST /ls/sign-records': async (params, data) => {
    return createResponse({ points: 10, totalPoints: 5870 })
  },

  // ============ 首页数据 ============
  'GET /api/home': async (params) => {
    return createResponse({
      banners: [
        { id: 1, title: 'Vue3 从入门到精通', image: '/src/assets/images/banners/banner1.svg', link: '/details/index?id=1' },
        { id: 2, title: 'SpringBoot 微服务实战', image: '/src/assets/images/banners/banner2.svg', link: '/details/index?id=3' },
        { id: 3, title: '人工智能与机器学习', image: '/src/assets/images/banners/banner3.svg', link: '/details/index?id=8' }
      ],
      recommendCourses: db.courses.filter(c => c.status === 1).slice(0, 6),
      hotCourses: db.courses.filter(c => c.status === 1).sort((a, b) => b.learners - a.learners).slice(0, 6),
      newCourses: db.courses.filter(c => c.status === 1).sort((a, b) => new Date(b.createTime) - new Date(a.createTime)).slice(0, 6),
      categories: db.categories,
      statistics: {
        totalCourses: db.courses.length,
        totalStudents: 50000,
        totalTeachers: 13,
        totalLessons: 1000
      }
    })
  },

  'GET /ss/recommend/:type': async (params) => {
    const type = params.type
    if (type === '1') return createResponse(db.courses.filter(c => c.status === 1).slice(0, 6))
    if (type === '2') return createResponse(db.courses.filter(c => c.status === 1).sort((a, b) => b.learners - a.learners).slice(0, 6))
    if (type === '3') return createResponse(db.courses.filter(c => c.status === 1).sort((a, b) => new Date(b.createTime) - new Date(a.createTime)).slice(0, 6))
    return createResponse([])
  },

  // ============ 统计数据 ============
  'GET /api/statistics': async (params) => {
    return createResponse(getStatistics())
  },

  // ============ 搜索 ============
  'GET /ss/courses/portal': async (params) => {
    const { keyword, categoryId } = params
    let courses = db.courses.filter(c => c.status === 1)
    if (keyword) {
      courses = courses.filter(c => c.title.includes(keyword) || c.description.includes(keyword))
    }
    if (categoryId) {
      courses = courses.filter(c => c.categoryId === Number(categoryId))
    }
    return createResponse(courses)
  },

  // ============ 教师列表 ============
  'GET /cs/courses/teachers/:id': async (params) => {
    const teachers = db.users.filter(u => u.type === 'teacher')
    return createResponse(teachers)
  },

  // ============ 客服中心 ============
  'GET /cs/customer-service/faqs': async (params) => createResponse(customerService.faqs(params)),
  'POST /cs/customer-service/ask': async (params, data) => createResponse(customerService.ask(data || params)),
  'GET /cs/customer-service/sessions/:id': async (params) => createResponse(customerService.getSession(params.id)),
  'POST /cs/customer-service/sessions/:id/evaluate': async (params, data) => createResponse(customerService.evaluate(params.id, data || params)),

  'GET /cs/customer-service/knowledge/page': async (params) => createResponse(customerService.knowledgePage(params)),
  'POST /cs/customer-service/knowledge': async (params, data) => createResponse(customerService.saveKnowledge(data || params)),
  'PUT /cs/customer-service/knowledge/:id': async (params, data) => createResponse(customerService.updateKnowledge(params.id, data || params)),
  'DELETE /cs/customer-service/knowledge/:id': async (params) => createResponse(customerService.deleteKnowledge(params.id)),
  'GET /cs/customer-service/faqs/page': async (params) => createResponse(customerService.faqPage(params)),
  'POST /cs/customer-service/faqs': async (params, data) => createResponse(customerService.saveFaq(data || params)),
  'PUT /cs/customer-service/faqs/:id': async (params, data) => createResponse(customerService.updateFaq(params.id, data || params)),
  'DELETE /cs/customer-service/faqs/:id': async (params) => createResponse(customerService.deleteFaq(params.id)),
  'GET /cs/customer-service/sessions/page': async (params) => createResponse(customerService.sessionPage(params)),
  'GET /cs/customer-service/stats': async () => createResponse(customerService.stats()),
}

// 匹配路由
function matchRoute(method, url) {
  // 移除查询参数
  const path = url.split('?')[0]

  // 精确匹配
  const exactKey = `${method} ${path}`
  if (handlers[exactKey]) return { handler: handlers[exactKey], params: {} }

  // 模式匹配
  for (const [key, handler] of Object.entries(handlers)) {
    const [m, pattern] = key.split(' ')
    if (m !== method) continue

    // 将模式转为正则
    const regexStr = pattern.replace(/:(\w+)/g, '(?<$1>[^/]+)')
    const regex = new RegExp(`^${regexStr}$`)
    const match = path.match(regex)

    if (match) {
      return { handler, params: match.groups || {} }
    }
  }

  return null
}

// 处理请求
export async function handleRequest(url, method, data) {
  // 移除 baseURL 前缀
  let path = url
  if (path.includes('://')) {
    const urlObj = new URL(path)
    path = urlObj.pathname
  }

  // 解析查询参数
  const queryString = url.split('?')[1] || ''
  const params = {}
  if (queryString) {
    queryString.split('&').forEach(pair => {
      const [key, value] = pair.split('=')
      params[decodeURIComponent(key)] = decodeURIComponent(value || '')
    })
  }

  // 合并参数
  if (data && typeof data === 'object') {
    Object.assign(params, data)
  }

  console.log(`[Mock] ${method} ${path}`, params)

  // 匹配路由
  const route = matchRoute(method.toUpperCase(), path)

  if (route) {
    await delay(Math.random() * 200 + 50) // 模拟网络延迟
    const result = await route.handler({ ...params, ...route.params }, data)
    console.log(`[Mock] Response:`, result)
    return result
  }

  // 未匹配的路由返回空数据
  console.warn(`[Mock] No handler for: ${method} ${path}`)
  return createResponse(null, 200, 'success')
}

// 检查是否启用 Mock
export function isMockEnabled() {
  // Mock 仅作为历史兼容工具保留，必须显式开启，避免开发环境误拦截真实接口。
  return import.meta.env.VITE_USE_MOCK === 'true'
}
