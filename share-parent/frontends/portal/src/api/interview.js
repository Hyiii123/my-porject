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

// 个人中心简历与 AI 深度诊断 API
export const getMyResume = () =>
  request({ url: '/customer/interview/resume/my', method: 'get' })

export const saveResume = (data) =>
  request({ url: '/customer/interview/resume/save', method: 'post', data })

export const analyzeResume = (data) =>
  request({ url: '/customer/interview/resume/analyze', method: 'post', data, timeout: 60000 })

export const uploadResumeFile = (formData) =>
  request({
    url: '/customer/interview/resume/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000
  })

export const clearResume = () =>
  request({ url: '/customer/interview/resume/clear', method: 'delete' })
