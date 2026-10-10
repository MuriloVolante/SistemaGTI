import http from './http'

export default {
  preview: (recurso) => http.get(`/integracao/preview/${recurso}`),
  listar: () => http.get('/integracao/chaves'),
  criar: (payload) => http.post('/integracao/chaves', payload),
  detalhar: (id) => http.get(`/integracao/chaves/${id}`),
  desligar: (id) => http.delete(`/integracao/chaves/${id}`),
  girar: (id, diasCarencia) => http.post(`/integracao/chaves/${id}/girar`, { diasCarencia }),
  tentativasNegadas: (pagina = 0, tamanho = 50) =>
    http.get('/integracao/chaves/tentativas-negadas', { params: { pagina, tamanho } })
}