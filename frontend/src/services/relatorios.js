import http from './http'

export default {
  ativos: (params) => http.get('/relatorios/ativos', { params }),
  exportar: (params) => http.get('/relatorios/ativos/export', { params, responseType: 'blob' })
}