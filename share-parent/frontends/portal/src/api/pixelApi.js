import request from '@/utils/request.js'

const API_KEY_STORAGE_KEY = 'customer_service_pixel_api_key'
const MODEL_STORAGE_KEY = 'customer_service_pixel_api_model'
const DEFAULT_MODEL = 'gpt-5.5'

function normalizeApiKey(apiKey) {
  return String(apiKey || '')
    .trim()
    .replace(/^Bearer\s+/i, '')
    .replace(/^['"]|['"]$/g, '')
    .trim()
}

export function getPixelApiKey() {
  return localStorage.getItem(API_KEY_STORAGE_KEY) || ''
}

export function setPixelApiKey(apiKey) {
  const value = normalizeApiKey(apiKey)
  if (value) localStorage.setItem(API_KEY_STORAGE_KEY, value)
  else localStorage.removeItem(API_KEY_STORAGE_KEY)
}

export function clearPixelApiKey() {
  localStorage.removeItem(API_KEY_STORAGE_KEY)
}

export function getPixelModel() {
  return localStorage.getItem(MODEL_STORAGE_KEY) || DEFAULT_MODEL
}

export function setPixelModel(model) {
  const value = String(model || '').trim() || DEFAULT_MODEL
  localStorage.setItem(MODEL_STORAGE_KEY, value)
}

/**
 * 保留旧方法名以兼容历史调用方，但不再从浏览器直连第三方 AI。
 * 所有请求统一进入客服服务，由后端负责调用 Pixel 第三方接口、降级知识库并记录会话消息。
 */
export async function askPixelAi({ sessionId, apiKey, messages = [], model = DEFAULT_MODEL, userName = '访客用户' } = {}) {
  const key = normalizeApiKey(apiKey)
  if (!key) throw new Error('请先配置第三方 AI API Key')

  const latestQuestion = [...messages]
    .reverse()
    .find((message) => message?.role === 'user' && String(message.content || '').trim())
  if (!latestQuestion) throw new Error('请输入要咨询的问题')

  let currentSessionId = sessionId
  if (!currentSessionId) {
    const created = await request({
      url: '/customer/session',
      method: 'post',
      data: { userName },
    })
    if (created.code !== 200 || !created.data?.id) {
      throw new Error(created.msg || '客服会话创建失败')
    }
    currentSessionId = created.data.id
  }

  const response = await request({
    url: `/customer/session/${currentSessionId}/messages`,
    method: 'post',
    timeout: 60000,
    data: {
      content: String(latestQuestion.content).trim(),
      apiKey: key,
      model: String(model || DEFAULT_MODEL).trim() || DEFAULT_MODEL,
    },
  })
  if (response.code !== 200 || !response.data?.message) {
    throw new Error(response.msg || response.message || '客服暂时不可用')
  }
  return response.data.message.content || ''
}
