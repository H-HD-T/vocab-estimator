import request from './request'

export function generatePaper(userId, count = 40) {
  return request.get('/test/paper', { params: { userId, count } })
}

export function submitTest(userId, answers) {
  return request.post('/test/submit?userId=' + userId, answers)
}

export function getTestHistory(userId) {
  return request.get('/test/history/' + userId)
}

export function getTestRecords(params) {
  return request.get('/test/records', { params })
}
