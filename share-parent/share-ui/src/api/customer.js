import request from '@/utils/request'

// 用户端客服
export function createCustomerSession(data = {}) {
  return request({ url: '/customer/session', method: 'post', data })
}

export function listMyCustomerSessions(params) {
  return request({ url: '/customer/session/my', method: 'get', params })
}

export function getCustomerSession(sessionId) {
  return request({ url: '/customer/session/' + sessionId, method: 'get' })
}

export function listCustomerMessages(sessionId) {
  return request({ url: '/customer/session/' + sessionId + '/messages', method: 'get' })
}

export function sendCustomerMessage(sessionId, data) {
  return request({ url: '/customer/session/' + sessionId + '/messages', method: 'post', data })
}

export function evaluateCustomerSession(sessionId, data) {
  return request({ url: '/customer/session/' + sessionId + '/evaluation', method: 'post', data })
}

export function listPublicCustomerFaq(params = {}) {
  return request({ url: '/customer/faq/public', method: 'get', params })
}

// 管理端客服
export function listCustomerKnowledge(params) {
  return request({ url: '/customer/admin/knowledge/list', method: 'get', params })
}

export function getCustomerKnowledge(id) {
  return request({ url: '/customer/admin/knowledge/' + id, method: 'get' })
}

export function addCustomerKnowledge(data) {
  return request({ url: '/customer/admin/knowledge', method: 'post', data })
}

export function updateCustomerKnowledge(data) {
  return request({ url: '/customer/admin/knowledge', method: 'put', data })
}

export function delCustomerKnowledge(ids) {
  return request({ url: '/customer/admin/knowledge/' + ids, method: 'delete' })
}

export function listCustomerFaq(params) {
  return request({ url: '/customer/admin/faq/list', method: 'get', params })
}

export function getCustomerFaq(id) {
  return request({ url: '/customer/admin/faq/' + id, method: 'get' })
}

export function addCustomerFaq(data) {
  return request({ url: '/customer/admin/faq', method: 'post', data })
}

export function updateCustomerFaq(data) {
  return request({ url: '/customer/admin/faq', method: 'put', data })
}

export function delCustomerFaq(ids) {
  return request({ url: '/customer/admin/faq/' + ids, method: 'delete' })
}

export function listCustomerSessions(params) {
  return request({ url: '/customer/admin/sessions/list', method: 'get', params })
}

export function getCustomerAdminSession(id) {
  return request({ url: '/customer/admin/sessions/' + id, method: 'get' })
}

export function listCustomerAdminMessages(id) {
  return request({ url: '/customer/admin/sessions/' + id + '/messages', method: 'get' })
}

export function closeCustomerSession(id) {
  return request({ url: '/customer/admin/sessions/' + id + '/close', method: 'post' })
}

export function getCustomerStatistics() {
  return request({ url: '/customer/admin/statistics/overview', method: 'get' })
}

export function getCustomerAiConfig() {
  return request({ url: '/customer/admin/ai/config', method: 'get' })
}

export function updateCustomerAiConfig(data) {
  return request({ url: '/customer/admin/ai/config', method: 'put', data })
}

export function testCustomerAi(data) {
  return request({ url: '/customer/admin/ai/test', method: 'post', data })
}
