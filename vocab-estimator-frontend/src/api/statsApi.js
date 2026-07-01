import request from './request'

export function getCorrelationStats() {
  return request.get('/stats/correlation')
}

export function getOverviewStats() {
  return request.get('/stats/overview')
}
