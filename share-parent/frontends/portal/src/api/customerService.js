import request from '@/utils/request.js'

// 客服服务统一走 share-customer，不再保留旧的人工客服协议。
export const createServiceSession = (data = {}) =>
  request({ url: '/customer/session', method: 'post', data })

export const getMyServiceSessions = (params) =>
  request({ url: '/customer/session/my', method: 'get', params })

export const getServiceSession = (sessionId) =>
  request({ url: `/customer/session/${sessionId}`, method: 'get' })

export const getServiceMessages = (sessionId) =>
  request({ url: `/customer/session/${sessionId}/messages`, method: 'get' })

export const sendServiceMessage = (sessionId, data) =>
  request({ url: `/customer/session/${sessionId}/messages`, method: 'post', data, timeout: 60000 })

export const recordAiReply = (sessionId, data) =>
  request({ url: `/customer/session/${sessionId}/messages/record`, method: 'post', data })

export const evaluateService = (sessionId, data) =>
  request({ url: `/customer/session/${sessionId}/evaluation`, method: 'post', data })

export const getServiceFaqs = (params) =>
  request({ url: '/customer/faq/public', method: 'get', params })
