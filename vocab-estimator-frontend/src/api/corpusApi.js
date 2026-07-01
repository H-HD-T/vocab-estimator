import request from './request'

export function importCorpusFile(corpusType, formData) {
  return request.post('/corpus/import?corpusType=' + corpusType, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function importCorpusText(corpusType, text) {
  return request.post('/corpus/import-text?corpusType=' + corpusType, text, {
    headers: { 'Content-Type': 'text/plain' }
  })
}

export function analyzeCorpus(corpusId) {
  return request.get('/corpus/analyze/' + corpusId)
}

export function analyzeAllCorpuses() {
  return request.get('/corpus/analyze-all')
}
