import request from '@/utils/request.js'

export const getKnowledgePage = (params) =>
  request({ url: '/customer/admin/knowledge/list', method: 'get', params })
export const saveKnowledge = (data) =>
  request({ url: '/customer/admin/knowledge', method: 'post', data })
export const updateKnowledge = (data) =>
  request({ url: '/customer/admin/knowledge', method: 'put', data })
export const deleteKnowledge = (id) =>
  request({ url: `/customer/admin/knowledge/${id}`, method: 'delete' })

export const getFaqPage = (params) =>
  request({ url: '/customer/admin/faq/list', method: 'get', params })
export const saveFaq = (data) =>
  request({ url: '/customer/admin/faq', method: 'post', data })
export const updateFaq = (data) =>
  request({ url: '/customer/admin/faq', method: 'put', data })
export const deleteFaq = (id) =>
  request({ url: `/customer/admin/faq/${id}`, method: 'delete' })

export const getSessionPage = (params) =>
  request({ url: '/customer/admin/sessions/list', method: 'get', params })
export const getSessionDetails = (id) =>
  request({ url: `/customer/admin/sessions/${id}`, method: 'get' })
export const closeSession = (id) =>
  request({ url: `/customer/admin/sessions/${id}/close`, method: 'post' })
export const getServiceStats = (params) =>
  request({ url: '/customer/admin/statistics/overview', method: 'get', params })

export const getAiConfig = () =>
  request({ url: '/customer/admin/ai/config', method: 'get' })
export const saveAiConfig = (data) =>
  request({ url: '/customer/admin/ai/config', method: 'put', data })
export const testAi = (data) =>
  request({ url: '/customer/admin/ai/test', method: 'post', data })
