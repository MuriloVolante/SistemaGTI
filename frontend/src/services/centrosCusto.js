import http from './http'

export default {
  listar: () => http.get('/centros-custo'),
  criar: (nome) => http.post('/centros-custo', { nome })
}