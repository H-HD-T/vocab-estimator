import request from './request'

export function importValidationData(jsonData) {
  return request.post('/validation/import', jsonData, {
    headers: { 'Content-Type': 'application/json' }
  })
}

export function uploadValidationFile(formData) {
  return request.post('/validation/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
