import axios from 'axios'
import { ElMessage } from 'element-plus'

// Create axios instance
const request = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Response interceptor
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || 'Request failed')
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    return res
  },
  error => {
    ElMessage.error('Network error: ' + (error.message || 'Unknown error'))
    return Promise.reject(error)
  }
)

export default request
