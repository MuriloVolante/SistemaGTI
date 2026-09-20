import http from './http'

export default {
  preview: (recurso) => http.get(`/integracao/preview/${recurso}`),
  chave: () => http.get('/integracao/preview/chave')
}