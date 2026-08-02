import http from './http'

export default {
  buscar: (params) => http.get('/dashboard', { params })
}