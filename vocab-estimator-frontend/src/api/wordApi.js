import request from './request'

export function getWordPage(params) {
  return request.get('/words/page', { params })
}

export function getWordById(id) {
  return request.get('/words/' + id)
}

export function addWord(data) {
  return request.post('/words', data)
}

export function updateWord(data) {
  return request.put('/words', data)
}

export function deleteWord(id) {
  return request.delete('/words/' + id)
}

export function searchWords(keyword) {
  return request.get('/words/search', { params: { keyword } })
}

export function getWordCount() {
  return request.get('/words/count')
}
