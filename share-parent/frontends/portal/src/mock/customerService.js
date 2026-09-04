const KEY = {
  knowledge: 'mock_customer_knowledge',
  faqs: 'mock_customer_faqs',
  sessions: 'mock_customer_sessions',
}

const time = () => {
  const d = new Date()
  const p = (v) => String(v).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
const copy = (data) => JSON.parse(JSON.stringify(data))
const id = (prefix) => `${prefix}-${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}`

const knowledgeSeed = [
  { id: 'kb-1', question: '如何注册账号？', answer: '点击页面右上角“注册”，填写手机号和验证码后即可完成注册。', keywords: '注册,账号,手机号', category: '账号与登录', status: 1, updateTime: time() },
  { id: 'kb-2', question: '课程支持退款吗？', answer: '课程购买后 7 天内，且学习进度不超过 30% 时可以申请退款。', keywords: '退款,退课,售后', category: '订单售后', status: 1, updateTime: time() },
  { id: 'kb-3', question: '有哪些支付方式？', answer: '目前支持微信支付、支付宝和银行卡支付。', keywords: '支付,付款,支付宝,微信', category: '订单支付', status: 1, updateTime: time() },
  { id: 'kb-4', question: '如何查看我的学习进度？', answer: '登录后进入“我的课程”，课程卡片会展示最近学习进度，学习页面也会自动保存播放记录。', keywords: '学习进度,课程,学习记录', category: '课程学习', status: 1, updateTime: time() },
  { id: 'kb-5', question: '视频播放不了怎么办？', answer: '请先检查网络并刷新页面，建议使用最新版 Chrome、Edge 或 Safari 浏览器；仍无法播放时请记录浏览器版本和页面提示，再重新发起咨询。', keywords: '视频,播放,卡顿,黑屏', category: '课程学习', status: 1, updateTime: time() },
  { id: 'kb-6', question: '课程可以开发票吗？', answer: '可以。完成支付后进入订单详情，点击“申请发票”填写抬头和邮箱，电子发票会在审核后发送。', keywords: '发票,抬头,报销,订单', category: '订单售后', status: 1, updateTime: time() },
  { id: 'kb-7', question: '学完课程可以获得证书吗？', answer: '完成课程要求并通过结业考试后，可以在个人中心申请电子结业证书。', keywords: '证书,结业,考试', category: '课程学习', status: 1, updateTime: time() },
  { id: 'kb-8', question: '忘记密码怎么处理？', answer: '可以在登录页使用短信登录，登录后到个人设置中重新设置密码。', keywords: '密码,忘记密码,登录', category: '账号与登录', status: 1, updateTime: time() },
  { id: 'kb-9', question: '优惠券在哪里使用？', answer: '在课程结算页选择可用优惠券即可抵扣；每笔订单通常只能使用一张优惠券。', keywords: '优惠券,抵扣,优惠', category: '优惠活动', status: 1, updateTime: time() },
  { id: 'kb-10', question: '客服中心可以咨询哪些内容？', answer: '客服中心支持解答账号登录、课程学习、订单支付、发票和优惠活动等常见问题。', keywords: '客服,咨询,帮助,账号,课程,订单', category: '客服服务', status: 1, updateTime: time() },
]
const faqSeed = [
  { id: 'faq-1', question: '如何领取新用户优惠券？', answer: '登录后打开首页优惠券入口即可领取新用户专享券。', category: '优惠活动', sort: 1, enabled: 1, updateTime: time() },
  { id: 'faq-2', question: '购买课程后在哪里学习？', answer: '进入“我的课程”即可查看已购买课程和学习计划。', category: '课程学习', sort: 2, enabled: 1, updateTime: time() },
  { id: 'faq-3', question: '客服中心可以咨询哪些内容？', answer: '支持咨询账号登录、课程学习、订单支付、发票和优惠活动等常见问题。', category: '客服服务', sort: 3, enabled: 1, updateTime: time() },
  { id: 'faq-4', question: '忘记密码怎么办？', answer: '在登录页选择短信登录，登录后到个人设置中重新设置密码。', category: '账号与登录', sort: 4, enabled: 1, updateTime: time() },
  { id: 'faq-5', question: '视频播放卡顿怎么解决？', answer: '建议切换网络并刷新页面，使用 Chrome 或 Edge 浏览器体验更佳。', category: '课程学习', sort: 5, enabled: 1, updateTime: time() },
  { id: 'faq-6', question: '课程是否支持开发票？', answer: '支持电子发票，进入订单详情即可申请。', category: '订单售后', sort: 6, enabled: 1, updateTime: time() },
  { id: 'faq-7', question: '优惠券可以叠加使用吗？', answer: '每笔订单默认只能使用一张优惠券，具体以结算页展示为准。', category: '优惠活动', sort: 7, enabled: 1, updateTime: time() },
  { id: 'faq-8', question: '课程购买后有效期多久？', answer: '已购买课程通常支持长期学习，具体有效期以课程详情页说明为准。', category: '课程学习', sort: 8, enabled: 1, updateTime: time() },
  { id: 'faq-9', question: '如何修改个人资料？', answer: '登录后进入个人中心-设置，即可修改头像、昵称和联系方式。', category: '账号与登录', sort: 9, enabled: 1, updateTime: time() },
  { id: 'faq-10', question: '订单支付失败怎么办？', answer: '请检查支付账户状态、网络和支付方式后重新发起支付，并保留页面提示方便继续咨询。', category: '订单支付', sort: 10, enabled: 1, updateTime: time() },
]
const sessionSeed = [
  {
    id: 'CS20260818001', userId: '2', userName: '张三', source: 'AI客服', status: 'closed', satisfaction: 5,
    evaluation: { score: 5, tags: ['回答准确'], comment: '很快解决了问题' }, startedAt: '2026-08-18 09:23:00', updatedAt: '2026-08-18 09:25:18',
    messages: [
      { id: 'm-1', type: 'user', senderName: '张三', content: '课程支持退款吗？', time: '2026-08-18 09:23:00' },
      { id: 'm-2', type: 'ai', senderName: 'AI客服', content: '课程购买后 7 天内，且学习进度不超过 30% 时可以申请退款。', time: '2026-08-18 09:23:03' },
      { id: 'm-3', type: 'user', senderName: '张三', content: '明白了，谢谢。', time: '2026-08-18 09:25:10' },
      { id: 'm-4', type: 'ai', senderName: 'AI客服', content: '感谢您的反馈，祝您学习愉快！', time: '2026-08-18 09:25:18' },
    ],
  },
  {
    id: 'CS20260818002', userId: '3', userName: '李四', source: 'AI客服', status: 'closed', satisfaction: 4,
    evaluation: { score: 4, tags: ['响应及时'], comment: '支付问题已解决' },
    startedAt: '2026-08-18 10:05:00', updatedAt: '2026-08-18 10:08:20',
    messages: [
      { id: 'm-5', type: 'user', senderName: '李四', content: '我的订单一直没有支付成功。', time: '2026-08-18 10:05:00' },
      { id: 'm-6', type: 'ai', senderName: 'AI客服', content: '请检查支付账户余额、网络和支付方式后重新发起支付；如果仍然失败，请记录页面提示继续咨询。', time: '2026-08-18 10:05:05' },
      { id: 'm-7', type: 'user', senderName: '李四', content: '已经可以支付了，谢谢。', time: '2026-08-18 10:08:20' },
    ],
  },
  { id: 'CS20260818003', userId: '4', userName: '王五', source: 'AI客服', status: 'closed', satisfaction: 4, evaluation: { score: 4, tags: ['响应及时'], comment: '问题解决了' }, startedAt: '2026-08-17 15:12:00', updatedAt: '2026-08-17 15:15:40', lastMessage: '祝您学习愉快！', messages: [{ id: 'm-8', type: 'user', senderName: '王五', content: '视频播放不了怎么办？', time: '2026-08-17 15:12:00' }, { id: 'm-9', type: 'ai', senderName: 'AI客服', content: '请先检查网络并刷新页面，建议使用最新版浏览器。', time: '2026-08-17 15:12:04' }] },
  { id: 'CS20260817004', userId: '5', userName: '赵六', source: 'AI客服', status: 'closed', satisfaction: 5, evaluation: { score: 5, tags: ['回答准确', '表达清晰'], comment: '' }, startedAt: '2026-08-17 11:30:00', updatedAt: '2026-08-17 11:32:10', lastMessage: '可以在订单详情申请电子发票。', messages: [{ id: 'm-10', type: 'user', senderName: '赵六', content: '课程可以开发票吗？', time: '2026-08-17 11:30:00' }, { id: 'm-11', type: 'ai', senderName: 'AI客服', content: '可以在订单详情申请电子发票。', time: '2026-08-17 11:30:04' }] },
  { id: 'CS20260816005', userId: '6', userName: '小陈', source: 'AI客服', status: 'closed', satisfaction: 3, evaluation: { score: 3, tags: ['问题未解决'], comment: '希望增加更多支付方式' }, startedAt: '2026-08-16 16:20:00', updatedAt: '2026-08-16 16:28:00', lastMessage: '目前支持微信支付、支付宝和银行卡支付。', messages: [{ id: 'm-12', type: 'user', senderName: '小陈', content: '有哪些支付方式？', time: '2026-08-16 16:20:00' }, { id: 'm-13', type: 'ai', senderName: 'AI客服', content: '目前支持微信支付、支付宝和银行卡支付。', time: '2026-08-16 16:28:00' }] },
  { id: 'CS20260816006', userId: '7', userName: '小林', source: 'AI客服', status: 'ai', satisfaction: null, evaluation: null, startedAt: '2026-08-16 14:45:00', updatedAt: '2026-08-16 14:46:12', lastMessage: '如何查看我的学习进度？', messages: [{ id: 'm-14', type: 'user', senderName: '小林', content: '如何查看我的学习进度？', time: '2026-08-16 14:46:12' }] },
  { id: 'CS20260815007', userId: '8', userName: '周同学', source: 'AI客服', status: 'closed', satisfaction: 4, evaluation: { score: 4, tags: ['表达清晰'], comment: '' }, startedAt: '2026-08-15 09:10:00', updatedAt: '2026-08-15 09:12:20', lastMessage: '登录后进入我的课程即可查看。', messages: [{ id: 'm-15', type: 'user', senderName: '周同学', content: '购买课程后在哪里学习？', time: '2026-08-15 09:10:00' }, { id: 'm-16', type: 'ai', senderName: 'AI客服', content: '登录后进入我的课程即可查看。', time: '2026-08-15 09:10:04' }] },
  { id: 'CS20260814008', userId: '9', userName: '小吴', source: 'AI客服', status: 'ai', satisfaction: null, evaluation: null, startedAt: '2026-08-14 20:05:00', updatedAt: '2026-08-14 20:08:00', lastMessage: '客服中心支持咨询课程学习和订单等常见问题。', messages: [{ id: 'm-17', type: 'user', senderName: '小吴', content: '我想咨询课程套餐。', time: '2026-08-14 20:05:00' }, { id: 'm-18', type: 'ai', senderName: 'AI客服', content: '客服中心支持咨询课程学习、订单支付和优惠活动等常见问题。', time: '2026-08-14 20:08:00' }] },
]

function read(name, seed) {
  try {
    const stored = JSON.parse(localStorage.getItem(KEY[name]))
    if (!Array.isArray(stored)) return copy(seed)
    const existing = new Set(stored.map((item) => String(item.id)))
    const merged = [...stored, ...seed.filter((item) => !existing.has(String(item.id))).map(copy)]
    return name === 'sessions' ? merged.map(normalizeSession) : merged
  } catch (e) { return copy(seed) }
}
function write(name, value) { localStorage.setItem(KEY[name], JSON.stringify(value)) }
function normalizeSession(item) {
  const session = copy(item)
  if (session.source === '人工客服' || session.status === 'human') {
    session.source = 'AI客服'
    session.status = session.status === 'closed' ? 'closed' : 'ai'
  }
  session.messages = (session.messages || []).map((message) => {
    const normalized = { ...message }
    if (normalized.type === 'agent') {
      normalized.type = 'ai'
      normalized.senderName = 'AI客服'
    }
    normalized.content = String(normalized.content || '').replaceAll('人工客服', '客服').replaceAll('转接', '继续处理')
    return normalized
  })
  return session
}
function page(list, params = {}) {
  const current = Number(params.page) || 1
  const size = Number(params.pageSize) || 10
  return { list: list.slice((current - 1) * size, current * size), total: list.length, page: current, pageSize: size }
}
function append(session, type, content, senderName) {
  const message = { id: id('m'), type, content, senderName, time: time() }
  session.messages.push(message); session.updatedAt = message.time; session.lastMessage = content
  return message
}
function sessionOf(sessionId, data = {}) {
  const sessions = read('sessions', sessionSeed)
  let session = sessions.find((item) => String(item.id) === String(sessionId))
  if (!session) {
    session = { id: sessionId || `CS${Date.now()}`, userId: data.userId || 'guest', userName: data.userName || '访客用户', source: 'AI客服', status: 'ai', satisfaction: null, evaluation: null, startedAt: time(), updatedAt: time(), messages: [] }
    sessions.unshift(session)
  }
  return { sessions, session }
}
function answerFor(question) {
  const text = String(question || '').toLowerCase()
  const list = read('knowledge', knowledgeSeed).filter((item) => item.status === 1)
  return list.find((item) => `${item.question},${item.keywords}`.split(/[，,、\s]+/).some((term) => term && text.includes(term.toLowerCase())))
}

export const customerService = {
  faqs(params = {}) {
    let list = read('faqs', faqSeed).filter((item) => item.enabled === 1)
    if (params.category) list = list.filter((item) => item.category === params.category)
    return list.sort((a, b) => a.sort - b.sort)
  },
  knowledgePage(params = {}) {
    let list = read('knowledge', knowledgeSeed)
    if (params.keyword) list = list.filter((item) => `${item.question}${item.answer}${item.keywords}`.includes(params.keyword))
    if (params.status !== undefined && params.status !== '') list = list.filter((item) => Number(item.status) === Number(params.status))
    return page(list, params)
  },
  saveKnowledge(data = {}) { const list = read('knowledge', knowledgeSeed); const item = { ...data, id: data.id || id('kb'), status: Number(data.status) === 0 ? 0 : 1, updateTime: time() }; list.unshift(item); write('knowledge', list); return item },
  updateKnowledge(itemId, data = {}) { const list = read('knowledge', knowledgeSeed); const index = list.findIndex((item) => String(item.id) === String(itemId)); if (index < 0) return null; list[index] = { ...list[index], ...data, id: list[index].id, updateTime: time() }; write('knowledge', list); return list[index] },
  deleteKnowledge(itemId) { write('knowledge', read('knowledge', knowledgeSeed).filter((item) => String(item.id) !== String(itemId))); return true },
  faqPage(params = {}) { let list = read('faqs', faqSeed); if (params.keyword) list = list.filter((item) => `${item.question}${item.answer}${item.category}`.includes(params.keyword)); if (params.enabled !== undefined && params.enabled !== '') list = list.filter((item) => Number(item.enabled) === Number(params.enabled)); return page(list.sort((a, b) => a.sort - b.sort), params) },
  saveFaq(data = {}) { const list = read('faqs', faqSeed); const item = { ...data, id: data.id || id('faq'), sort: Number(data.sort) || list.length + 1, enabled: Number(data.enabled) === 0 ? 0 : 1, updateTime: time() }; list.push(item); write('faqs', list); return item },
  updateFaq(itemId, data = {}) { const list = read('faqs', faqSeed); const index = list.findIndex((item) => String(item.id) === String(itemId)); if (index < 0) return null; list[index] = { ...list[index], ...data, id: list[index].id, updateTime: time() }; write('faqs', list); return list[index] },
  deleteFaq(itemId) { write('faqs', read('faqs', faqSeed).filter((item) => String(item.id) !== String(itemId))); return true },
  ask(data = {}) {
    const { sessions, session } = sessionOf(data.sessionId, data)
    append(session, 'user', String(data.question || '').trim(), session.userName)
    const matched = answerFor(data.question)
    const content = matched ? matched.answer : '我暂时没有找到完全匹配的答案。您可以换个说法再问一次，或者参考左侧常见问题。'
    const message = append(session, 'ai', content, 'AI客服'); session.status = 'ai'; write('sessions', sessions)
    return { sessionId: session.id, message, matched: Boolean(matched), suggestions: this.faqs().slice(0, 4) }
  },
  recordAiReply(data = {}) {
    const { sessions, session } = sessionOf(data.sessionId, data)
    if (data.question) append(session, 'user', String(data.question).trim(), session.userName)
    const message = append(session, 'ai', String(data.answer || '').trim(), '第三方 AI')
    session.status = 'ai'; session.source = 'AI客服'; write('sessions', sessions)
    return { sessionId: session.id, message }
  },
  getSession(itemId) { return read('sessions', sessionSeed).find((item) => String(item.id) === String(itemId)) || null },
  sessionPage(params = {}) { let list = read('sessions', sessionSeed); if (params.keyword) list = list.filter((item) => `${item.id}${item.userName}${item.lastMessage}`.includes(params.keyword)); if (params.status) list = list.filter((item) => item.status === params.status); return page(list.sort((a, b) => String(b.updatedAt).localeCompare(String(a.updatedAt))), params) },
  evaluate(itemId, data = {}) { const { sessions, session } = sessionOf(itemId, data); session.satisfaction = Number(data.score) || 0; session.evaluation = { score: session.satisfaction, tags: data.tags || [], comment: data.comment || '' }; session.status = 'closed'; session.updatedAt = time(); write('sessions', sessions); return session },
  stats() {
    const list = read('sessions', sessionSeed); const rated = list.filter((item) => item.satisfaction); const questions = {}
    list.forEach((session) => session.messages.filter((message) => message.type === 'user').forEach((message) => { questions[message.content] = (questions[message.content] || 0) + 1 }))
    return { totalSessions: list.length, aiResolved: list.filter((item) => item.status === 'ai' || item.status === 'closed').length, satisfactionRate: rated.length ? Math.round(rated.reduce((sum, item) => sum + item.satisfaction, 0) / rated.length * 20) : 0, averageMessages: list.length ? Math.round(list.reduce((sum, item) => sum + item.messages.length, 0) / list.length) : 0, topQuestions: Object.entries(questions).map(([question, count]) => ({ question, count })).sort((a, b) => b.count - a.count).slice(0, 5), trend: [{ date: '08-14', sessions: 8, resolved: 6 }, { date: '08-15', sessions: 12, resolved: 10 }, { date: '08-16', sessions: 9, resolved: 7 }, { date: '08-17', sessions: 15, resolved: 12 }, { date: '08-18', sessions: list.length + 8, resolved: list.filter((item) => item.source === 'AI客服').length + 6 }] }
  },
}
