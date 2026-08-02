import http from './http'

export default {
  buscar: () => http.get('/dashboard')
}