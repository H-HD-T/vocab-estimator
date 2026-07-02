import request from './request'

export function uploadBatchText(data) {
  return request.post('/batch/upload-text', data)
}

export function uploadBatchFile(formData) {
  return request.post('/batch/upload-file', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function runSamplingTest(params) {
  return request.post('/batch/sampling', null, { params })
}

export function getBatchHistory() {
  return request.get('/batch/history')
}

export function runStabilityTest() {
  return request.post('/batch/stability-test')
}