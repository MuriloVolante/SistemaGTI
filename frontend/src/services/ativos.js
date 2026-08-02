import http from './http'

export default {
  listar: () => http.get('/ativos'),
  buscar: (id) => http.get(`/ativos/${id}`),
  valores: (id) => http.get(`/ativos/${id}/valores`),
  criar: (dados) => http.post('/ativos', dados),
  atualizar: (id, dados) => http.put(`/ativos/${id}`, dados),
  anexos: {
    listar: (id) => http.get(`/ativos/${id}/anexos`),
    enviar: (id, formData) => http.post(`/ativos/${id}/anexos`, formData),
    buscar: (anexoId) => http.get(`/ativos/anexos/${anexoId}`, { responseType: 'blob' }),
    excluir: (anexoId) => http.delete(`/ativos/anexos/${anexoId}`)
  },
  impactoExclusao: (id) => http.get(`/ativos/${id}/impacto-exclusao`),
  excluir: (id) => http.delete(`/ativos/${id}`),
  historico: (id) => http.get(`/ativos/${id}/historico`),
  manutencoes: {
    listar: (id) => http.get(`/ativos/${id}/manutencoes`),
    criar: (id, dados) => http.post(`/ativos/${id}/manutencoes`, dados)
  }
}