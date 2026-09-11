import request from '@/utils/request.js'

// AI 全真模拟面试与职涯评测 API
export const startInterview = (data) =>
  request({ url: '/customer/interview/start', method: 'post', data, timeout: 60000 })

export const submitInterviewAnswer = (data) =>
  request({ url: '/customer/interview/answer', method: 'post', data, timeout: 60000 })

export const submitInterviewCode = (data) =>
  request({ url: '/customer/interview/code', method: 'post', data, timeout: 60000 })

export const finishInterview = (sessionId) =>
  request({ url: `/customer/interview/finish/${sessionId}`, method: 'post', timeout: 60000 })

export const getInterviewDetail = (sessionId) =>
  request({ url: `/customer/interview/${sessionId}`, method: 'get' })

export const getMyInterviews = (params) =>
  request({ url: '/customer/interview/my', method: 'get', params })

export const terminateInterview = (sessionId) =>
  request({ url: `/customer/interview/terminate/${sessionId}`, method: 'post' })
