import http from './http'

export default {
  listar: () => http.get('/tipos-ativo'),
  buscar: (id) => http.get(`/tipos-ativo/${id}`),
  criar: (dados) => http.post('/tipos-ativo', dados),
  atualizar: (id, dados) => http.put(`/tipos-ativo/${id}`, dados),
  impactoExclusao: (id) => http.get(`/tipos-ativo/${id}/impacto-exclusao`),
  excluir: (id) => http.delete(`/tipos-ativo/${id}`),
  campos: {
    listar: (tipoId) => http.get(`/tipos-ativo/${tipoId}/campos`),
    criar: (tipoId, dados) => http.post(`/tipos-ativo/${tipoId}/campos`, dados),
    excluir: (campoId) => http.delete(`/tipos-ativo/campos/${campoId}`)
  }
}