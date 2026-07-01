import request from './request'

export function registerUser(data) {
  return request.post('/users/register', data)
}

export function getUserById(id) {
  return request.get('/users/' + id)
}

export function getUserByCode(code) {
  return request.get('/users/code/' + code)
}

export function getUsersPage(params) {
  return request.get('/users/page', { params })
}
