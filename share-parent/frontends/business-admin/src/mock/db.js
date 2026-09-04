// Mock 数据库 - 支持增删改查和本地持久化
import { ref, reactive } from 'vue'

// 本地存储工具
const storage = {
  get(key) {
    try {
      const data = localStorage.getItem(`mock_${key}`)
      return data ? JSON.parse(data) : null
    } catch {
      return null
    }
  },
  set(key, value) {
    localStorage.setItem(`mock_${key}`, JSON.stringify(value))
  },
  remove(key) {
    localStorage.removeItem(`mock_${key}`)
  }
}

// 生成唯一 ID
function generateId() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

// 初始化数据
function initData(key, defaultData) {
  const stored = storage.get(key)
  if (stored) return stored
  storage.set(key, defaultData)
  return defaultData
}

// ============ 课程分类数据 ============
const defaultCategories = [
  { id: 1, name: '前端开发', icon: 'Monitor', sort: 1, status: 1, createTime: '2026-05-10 00:00:00' },
  { id: 2, name: '后端开发', icon: 'Server', sort: 2, status: 1, createTime: '2026-05-10 00:00:00' },
  { id: 3, name: '移动开发', icon: 'Mobile', sort: 3, status: 1, createTime: '2026-05-10 00:00:00' },
  { id: 4, name: '数据库', icon: 'Coin', sort: 4, status: 1, createTime: '2026-05-10 00:00:00' },
  { id: 5, name: '云计算', icon: 'Connection', sort: 5, status: 1, createTime: '2026-05-10 00:00:00' },
  { id: 6, name: '人工智能', icon: 'Cpu', sort: 6, status: 1, createTime: '2026-05-10 00:00:00' },
  { id: 7, name: '数据科学', icon: 'DataAnalysis', sort: 7, status: 1, createTime: '2026-05-10 00:00:00' },
  { id: 8, name: '网络安全', icon: 'Lock', sort: 8, status: 1, createTime: '2026-05-10 00:00:00' },
  { id: 9, name: '游戏开发', icon: 'GameBoy', sort: 9, status: 1, createTime: '2026-05-10 00:00:00' },
  { id: 10, name: '区块链', icon: 'Link', sort: 10, status: 1, createTime: '2026-05-10 00:00:00' }
]

// ============ 用户数据 ============
const defaultUsers = [
  { id: 1, username: 'admin', nickname: '张三', phone: '13800138000', email: 'admin@zhiwenxueban.com', type: 'admin', status: 1, avatar: '', createTime: '2026-05-10 00:00:00' },
  { id: 2, username: 'zhangsan', nickname: '张三', phone: '13800138001', email: 'zhangsan@qq.com', type: 'student', status: 1, avatar: '', createTime: '2026-05-10 10:00:00' },
  { id: 3, username: 'lisi', nickname: '李四', phone: '13800138002', email: 'lisi@qq.com', type: 'student', status: 1, avatar: '', createTime: '2026-05-10 14:00:00' },
  { id: 4, username: 'wangwu', nickname: '王五', phone: '13800138003', email: 'wangwu@qq.com', type: 'student', status: 1, avatar: '', createTime: '2026-05-10 09:00:00' },
  { id: 5, username: 'zhaoliu', nickname: '赵六', phone: '13800138004', email: 'zhaoliu@qq.com', type: 'student', status: 0, avatar: '', createTime: '2026-05-10 11:00:00' },
  { id: 6, username: 'teacher_zhang', nickname: '张老师', phone: '13900139001', email: 'zhang@zhiwenxueban.com', type: 'teacher', status: 1, avatar: '', createTime: '2026-05-10 00:00:00' },
  { id: 7, username: 'teacher_li', nickname: '李老师', phone: '13900139002', email: 'li@zhiwenxueban.com', type: 'teacher', status: 1, avatar: '', createTime: '2026-05-10 00:00:00' },
  { id: 8, username: 'teacher_wang', nickname: '王老师', phone: '13900139003', email: 'wang@zhiwenxueban.com', type: 'teacher', status: 1, avatar: '', createTime: '2026-05-10 00:00:00' },
  { id: 9, username: 'employee01', nickname: '员工小明', phone: '13700137001', email: 'xiaoming@zhiwenxueban.com', type: 'employee', status: 1, avatar: '', createTime: '2026-05-10 09:00:00' },
  { id: 10, username: 'employee02', nickname: '员工小红', phone: '13700137002', email: 'xiaohong@zhiwenxueban.com', type: 'employee', status: 1, avatar: '', createTime: '2026-05-10 10:00:00' }
]

// ============ 课程数据 ============
const defaultCourses = [
  {
    id: 1,
    title: 'Vue3 从入门到精通',
    categoryId: 1,
    categoryName: '前端开发',
    teacherId: 6,
    teacherName: '张老师',
    cover: '/src/assets/images/courses/vue3.svg',
    price: 19900,
    originalPrice: 39900,
    lessons: 48,
    learners: 12580,
    status: 1,
    description: '全面掌握Vue3核心语法、组合式API、Pinia状态管理、Vue Router等',
    chapters: [
      { id: 1, title: '第一章 Vue3基础', sections: [
        { id: 1, title: '1.1 Vue3简介', duration: '15:00', type: 'video' },
        { id: 2, title: '1.2 搭建开发环境', duration: '20:00', type: 'video' },
        { id: 3, title: '1.3 模板语法', duration: '25:00', type: 'video' }
      ]},
      { id: 2, title: '第二章 组合式API', sections: [
        { id: 4, title: '2.1 setup函数', duration: '30:00', type: 'video' },
        { id: 5, title: '2.2 ref与reactive', duration: '35:00', type: 'video' },
        { id: 6, title: '2.3 生命周期', duration: '25:00', type: 'video' }
      ]}
    ],
    createTime: '2026-05-10 10:00:00'
  },
  {
    id: 2,
    title: 'React18 实战教程',
    categoryId: 1,
    categoryName: '前端开发',
    teacherId: 7,
    teacherName: '李老师',
    cover: '/src/assets/images/courses/react.svg',
    price: 24900,
    originalPrice: 49900,
    lessons: 60,
    learners: 9850,
    status: 1,
    description: '深入学习React18新特性、Hooks、Redux、Next.js等',
    chapters: [
      { id: 1, title: '第一章 React基础', sections: [
        { id: 1, title: '1.1 React简介', duration: '15:00', type: 'video' },
        { id: 2, title: '1.2 JSX语法', duration: '20:00', type: 'video' }
      ]},
      { id: 2, title: '第二章 Hooks', sections: [
        { id: 3, title: '2.1 useState', duration: '30:00', type: 'video' },
        { id: 4, title: '2.2 useEffect', duration: '35:00', type: 'video' }
      ]}
    ],
    createTime: '2026-05-10 10:00:00'
  },
  {
    id: 3,
    title: 'Java SpringBoot 实战',
    categoryId: 2,
    categoryName: '后端开发',
    teacherId: 8,
    teacherName: '王老师',
    cover: '/src/assets/images/courses/springboot.svg',
    price: 29900,
    originalPrice: 59900,
    lessons: 72,
    learners: 15680,
    status: 1,
    description: '从零开始学习SpringBoot，掌握微服务架构设计',
    chapters: [
      { id: 1, title: '第一章 SpringBoot入门', sections: [
        { id: 1, title: '1.1 SpringBoot简介', duration: '20:00', type: 'video' },
        { id: 2, title: '1.2 项目搭建', duration: '25:00', type: 'video' }
      ]}
    ],
    createTime: '2026-05-10 10:00:00'
  },
  {
    id: 4,
    title: 'Python 全栈开发',
    categoryId: 2,
    categoryName: '后端开发',
    teacherId: 6,
    teacherName: '张老师',
    cover: '/src/assets/images/courses/python.svg',
    price: 22900,
    originalPrice: 45900,
    lessons: 56,
    learners: 18920,
    status: 1,
    description: 'Python基础、Django/Flask框架、数据库、API开发',
    chapters: [],
    createTime: '2026-05-10 10:00:00'
  },
  {
    id: 5,
    title: 'TypeScript 高级编程',
    categoryId: 1,
    categoryName: '前端开发',
    teacherId: 7,
    teacherName: '李老师',
    cover: '/src/assets/images/courses/typescript.svg',
    price: 14900,
    originalPrice: 29900,
    lessons: 36,
    learners: 7560,
    status: 2,
    description: '掌握TypeScript高级类型、泛型、装饰器等高级特性',
    chapters: [],
    createTime: '2026-05-10 10:00:00'
  },
  {
    id: 6,
    title: 'Docker 容器化实战',
    categoryId: 5,
    categoryName: '云计算',
    teacherId: 8,
    teacherName: '王老师',
    cover: '/src/assets/images/courses/docker.svg',
    price: 15900,
    originalPrice: 31900,
    lessons: 32,
    learners: 9870,
    status: 1,
    description: 'Docker基础、Docker Compose、K8s入门',
    chapters: [],
    createTime: '2026-05-10 10:00:00'
  },
  {
    id: 7,
    title: 'MySQL 数据库优化',
    categoryId: 4,
    categoryName: '数据库',
    teacherId: 6,
    teacherName: '张老师',
    cover: '/src/assets/images/courses/mysql.svg',
    price: 16900,
    originalPrice: 33900,
    lessons: 38,
    learners: 11230,
    status: 1,
    description: '索引优化、查询优化、分库分表、主从复制',
    chapters: [],
    createTime: '2026-05-10 10:00:00'
  },
  {
    id: 8,
    title: '机器学习入门',
    categoryId: 6,
    categoryName: '人工智能',
    teacherId: 7,
    teacherName: '李老师',
    cover: '/src/assets/images/courses/ml.svg',
    price: 34900,
    originalPrice: 69900,
    lessons: 68,
    learners: 14560,
    status: 3,
    description: 'Python机器学习、Scikit-learn、TensorFlow基础',
    chapters: [],
    createTime: '2026-05-10 10:00:00'
  },
  {
    id: 9,
    title: 'Flutter 跨平台开发',
    categoryId: 3,
    categoryName: '移动开发',
    teacherId: 8,
    teacherName: '王老师',
    cover: '/src/assets/images/courses/flutter.svg',
    price: 26900,
    originalPrice: 53900,
    lessons: 52,
    learners: 6780,
    status: 0,
    description: '使用Flutter开发iOS和Android应用',
    chapters: [],
    createTime: '2026-05-10 10:00:00'
  },
  {
    id: 10,
    title: 'Web前端性能优化',
    categoryId: 1,
    categoryName: '前端开发',
    teacherId: 6,
    teacherName: '张老师',
    cover: '/src/assets/images/courses/web.svg',
    price: 12900,
    originalPrice: 25900,
    lessons: 28,
    learners: 5680,
    status: 1,
    description: '加载优化、渲染优化、缓存策略、性能监控',
    chapters: [],
    createTime: '2026-05-10 10:00:00'
  }
]

// ============ 题目数据 ============
const defaultQuestions = [
  { id: 1, type: 'single', title: 'Vue3中，以下哪个是组合式API的入口函数？', options: ['created', 'mounted', 'setup', 'init'], answer: 'C', score: 10, categoryId: 1, createTime: '2026-05-10 10:00:00' },
  { id: 2, type: 'single', title: 'React中，以下哪个Hook用于管理副作用？', options: ['useState', 'useEffect', 'useContext', 'useReducer'], answer: 'B', score: 10, categoryId: 1, createTime: '2026-05-10 10:00:00' },
  { id: 3, type: 'multiple', title: '以下哪些是JavaScript的数据类型？', options: ['string', 'number', 'boolean', 'float'], answer: 'ABC', score: 10, categoryId: 1, createTime: '2026-05-10 10:00:00' },
  { id: 4, type: 'judge', title: 'Vue3完全兼容Vue2的语法。', options: ['正确', '错误'], answer: 'B', score: 10, categoryId: 1, createTime: '2026-05-10 10:00:00' },
  { id: 5, type: 'single', title: 'SpringBoot中，@Autowired注解的作用是？', options: ['创建对象', '依赖注入', '声明Bean', '配置路由'], answer: 'B', score: 10, categoryId: 2, createTime: '2026-05-10 10:00:00' },
  { id: 6, type: 'single', title: 'Python中，以下哪个不是内置数据类型？', options: ['list', 'dict', 'array', 'tuple'], answer: 'C', score: 10, categoryId: 2, createTime: '2026-05-10 10:00:00' },
  { id: 7, type: 'multiple', title: '以下哪些是关系型数据库？', options: ['MySQL', 'MongoDB', 'PostgreSQL', 'Redis'], answer: 'AC', score: 10, categoryId: 4, createTime: '2026-05-10 10:00:00' },
  { id: 8, type: 'single', title: 'Docker中，用于构建镜像的文件是？', options: ['docker-compose.yml', 'Dockerfile', '.dockerignore', 'docker.cfg'], answer: 'B', score: 10, categoryId: 5, createTime: '2026-05-10 10:00:00' },
  { id: 9, type: 'judge', title: '机器学习分为监督学习和无监督学习两大类。', options: ['正确', '错误'], answer: 'A', score: 10, categoryId: 6, createTime: '2026-05-10 10:00:00' },
  { id: 10, type: 'single', title: 'TypeScript中，以下哪个关键字用于定义接口？', options: ['class', 'interface', 'type', 'enum'], answer: 'B', score: 10, categoryId: 1, createTime: '2026-05-10 10:00:00' }
]

// ============ 优惠券数据 ============
const defaultCoupons = [
  { id: 1, name: '新用户专享券', type: 'fixed', value: 5000, minAmount: 0, totalCount: 1000, usedCount: 456, startTime: '2026-05-10', endTime: '2027-12-31', status: 1, createTime: '2026-05-10 00:00:00' },
  { id: 2, name: '满200减20券', type: 'fixed', value: 2000, minAmount: 20000, totalCount: 500, usedCount: 234, startTime: '2026-05-10', endTime: '2027-12-31', status: 1, createTime: '2026-05-10 00:00:00' },
  { id: 3, name: '8折优惠券', type: 'percent', value: 80, minAmount: 10000, totalCount: 300, usedCount: 128, startTime: '2026-05-10', endTime: '2027-12-31', status: 1, createTime: '2026-05-10 00:00:00' },
  { id: 4, name: '春季特惠券', type: 'fixed', value: 10000, minAmount: 50000, totalCount: 200, usedCount: 89, startTime: '2026-05-10', endTime: '2026-05-31', status: 1, createTime: '2026-05-10 00:00:00' },
  { id: 5, name: '课程体验券', type: 'fixed', value: 3000, minAmount: 0, totalCount: 2000, usedCount: 1256, startTime: '2026-05-10', endTime: '2027-12-31', status: 1, createTime: '2026-05-10 00:00:00' }
]

// ============ 订单数据 ============
const defaultOrders = [
  { id: 'ORD20240320001', userId: 2, userName: '张三', courseId: 1, courseName: 'Vue3 从入门到精通', amount: 19900, payAmount: 14900, status: 1, payType: 'alipay', createTime: '2026-05-10 10:30:00', payTime: '2026-05-10 10:35:00' },
  { id: 'ORD20240319002', userId: 3, userName: '李四', courseId: 2, courseName: 'React18 实战教程', amount: 24900, payAmount: 24900, status: 1, payType: 'wechat', createTime: '2026-05-10 14:20:00', payTime: '2026-05-10 14:25:00' },
  { id: 'ORD20240318003', userId: 4, userName: '王五', courseId: 3, courseName: 'Java SpringBoot 实战', amount: 29900, payAmount: 24900, status: 1, payType: 'alipay', createTime: '2026-05-10 09:15:00', payTime: '2026-05-10 09:20:00' },
  { id: 'ORD20240317004', userId: 2, userName: '张三', courseId: 4, courseName: 'Python 全栈开发', amount: 22900, payAmount: 22900, status: 2, payType: null, createTime: '2026-05-10 16:45:00', payTime: null },
  { id: 'ORD20240316005', userId: 5, userName: '赵六', courseId: 5, courseName: 'TypeScript 高级编程', amount: 14900, payAmount: 14900, status: 3, payType: 'wechat', createTime: '2026-05-10 11:30:00', payTime: '2026-05-10 11:35:00', refundTime: '2026-05-10 10:00:00' }
]

// ============ 学习记录数据 ============
const defaultLearningRecords = [
  { id: 1, userId: 2, courseId: 1, courseName: 'Vue3 从入门到精通', progress: 65, lastLearnTime: '2026-05-10 10:30:00', completedLessons: 31, totalLessons: 48 },
  { id: 2, userId: 2, courseId: 3, courseName: 'Java SpringBoot 实战', progress: 100, lastLearnTime: '2026-05-10 16:45:00', completedLessons: 72, totalLessons: 72 },
  { id: 3, userId: 3, courseId: 2, courseName: 'React18 实战教程', progress: 45, lastLearnTime: '2026-05-10 14:20:00', completedLessons: 27, totalLessons: 60 },
  { id: 4, userId: 4, courseId: 4, courseName: 'Python 全栈开发', progress: 18, lastLearnTime: '2026-05-10 16:45:00', completedLessons: 10, totalLessons: 56 },
  { id: 5, userId: 2, courseId: 6, courseName: 'Docker 容器化实战', progress: 32, lastLearnTime: '2026-05-10 15:20:00', completedLessons: 10, totalLessons: 32 }
]

// ============ 问答数据 ============
const defaultQuestions_QA = [
  { id: 1, courseId: 1, userId: 2, userName: '张三', title: 'Vue3的setup函数和created哪个先执行？', content: '在Vue3中，setup函数和created生命周期钩子的执行顺序是怎样的？', answers: [
    { id: 1, userId: 6, userName: '张老师', content: 'setup函数在created之前执行，它是组件初始化时最先调用的函数。', isAccepted: true, createTime: '2026-05-10 11:00:00' }
  ], createTime: '2026-05-10 10:00:00' },
  { id: 2, courseId: 1, userId: 3, userName: '李四', title: 'ref和reactive有什么区别？', content: '请问ref和reactive在使用场景上有什么区别？', answers: [
    { id: 2, userId: 7, userName: '李老师', content: 'ref用于基本类型，reactive用于对象类型。ref需要.value访问，reactive不需要。', isAccepted: false, createTime: '2026-05-10 15:00:00' }
  ], createTime: '2026-05-10 14:00:00' },
  { id: 3, courseId: 2, userId: 4, userName: '王五', title: 'useEffect的依赖数组怎么用？', content: 'useEffect的依赖数组什么时候该传，什么时候不该传？', answers: [], createTime: '2026-05-10 09:00:00' }
]

// ============ 笔记数据 ============
const defaultNotes = [
  { id: 1, userId: 2, courseId: 1, courseName: 'Vue3 从入门到精通', sectionId: 1, sectionName: '1.1 Vue3简介', content: 'Vue3的主要新特性：组合式API、更好的TypeScript支持、性能提升。', isPublic: true, createTime: '2026-05-10 10:30:00' },
  { id: 2, userId: 2, courseId: 1, courseName: 'Vue3 从入门到精通', sectionId: 4, sectionName: '2.1 setup函数', content: 'setup函数是组合式API的入口，在组件创建之前执行。', isPublic: false, createTime: '2026-05-10 11:00:00' },
  { id: 3, userId: 3, courseId: 2, courseName: 'React18 实战教程', sectionId: 1, sectionName: '1.1 React简介', content: 'React是一个用于构建用户界面的JavaScript库。', isPublic: true, createTime: '2026-05-10 14:30:00' }
]

// ============ 积分数据 ============
const defaultPoints = [
  { userId: 2, nickname: '张三', totalPoints: 5860, rank: 1 },
  { userId: 3, nickname: '李四', totalPoints: 4520, rank: 2 },
  { userId: 4, nickname: '王五', totalPoints: 3680, rank: 3 },
  { userId: 5, nickname: '赵六', totalPoints: 2950, rank: 4 },
  { userId: 6, nickname: '张老师', totalPoints: 8920, rank: 5 },
  { userId: 7, nickname: '李老师', totalPoints: 7650, rank: 6 },
  { userId: 8, nickname: '王老师', totalPoints: 6780, rank: 7 }
]

// 初始化数据库
export const db = {
  categories: initData('categories', defaultCategories),
  users: initData('users', defaultUsers),
  courses: initData('courses', defaultCourses),
  questions: initData('questions', defaultQuestions),
  coupons: initData('coupons', defaultCoupons),
  orders: initData('orders', defaultOrders),
  learningRecords: initData('learningRecords', defaultLearningRecords),
  qaList: initData('qaList', defaultQuestions_QA),
  notes: initData('notes', defaultNotes),
  points: initData('points', defaultPoints)
}

// 保存数据到本地存储
export function saveData(key) {
  storage.set(key, db[key])
}

// 重置数据到默认值
export function resetData(key) {
  const defaultMap = {
    categories: defaultCategories,
    users: defaultUsers,
    courses: defaultCourses,
    questions: defaultQuestions,
    coupons: defaultCoupons,
    orders: defaultOrders,
    learningRecords: defaultLearningRecords,
    qaList: defaultQuestions_QA,
    notes: defaultNotes,
    points: defaultPoints
  }
  if (defaultMap[key]) {
    db[key] = JSON.parse(JSON.stringify(defaultMap[key]))
    saveData(key)
  }
}

// 重置所有数据
export function resetAllData() {
  Object.keys(db).forEach(key => resetData(key))
}

// CRUD 操作
export const crud = {
  // 获取列表
  list(key, params = {}) {
    let data = [...db[key]]

    // 筛选
    if (params) {
      Object.keys(params).forEach(paramKey => {
        if (params[paramKey] !== undefined && params[paramKey] !== '' && paramKey !== 'page' && paramKey !== 'pageSize') {
          data = data.filter(item => {
            if (typeof params[paramKey] === 'string') {
              return String(item[paramKey]).includes(params[paramKey])
            }
            return item[paramKey] === params[paramKey]
          })
        }
      })
    }

    // 分页
    const page = params.page || 1
    const pageSize = params.pageSize || 10
    const total = data.length
    const start = (page - 1) * pageSize
    const end = start + pageSize
    const list = data.slice(start, end)

    return { code: 200, message: 'success', data: { list, total, page, pageSize } }
  },

  // 获取详情
  get(key, id) {
    const item = db[key].find(item => item.id === id || item.id === String(id))
    return item ? { code: 200, message: 'success', data: item } : { code: 404, message: '未找到数据' }
  },

  // 新增
  add(key, data) {
    const newItem = {
      ...data,
      id: data.id || generateId(),
      createTime: data.createTime || new Date().toLocaleString()
    }
    db[key].push(newItem)
    saveData(key)
    return { code: 200, message: 'success', data: newItem }
  },

  // 更新
  update(key, id, data) {
    const index = db[key].findIndex(item => item.id === id || item.id === String(id))
    if (index === -1) return { code: 404, message: '未找到数据' }
    db[key][index] = { ...db[key][index], ...data }
    saveData(key)
    return { code: 200, message: 'success', data: db[key][index] }
  },

  // 删除
  delete(key, id) {
    const index = db[key].findIndex(item => item.id === id || item.id === String(id))
    if (index === -1) return { code: 404, message: '未找到数据' }
    const deleted = db[key].splice(index, 1)[0]
    saveData(key)
    return { code: 200, message: 'success', data: deleted }
  }
}

// 获取统计信息
export function getStatistics() {
  return {
    totalUsers: db.users.length,
    totalCourses: db.courses.length,
    totalOrders: db.orders.length,
    totalRevenue: db.orders.filter(o => o.status === 1).reduce((sum, o) => sum + o.payAmount, 0),
    activeCourses: db.courses.filter(c => c.status === 1).length,
    totalStudents: db.users.filter(u => u.type === 'student').length,
    totalTeachers: db.users.filter(u => u.type === 'teacher').length
  }
}
