import http from './http'

export default {
  listar: () => http.get('/chamados'),
  buscar: (id) => http.get(`/chamados/${id}`),
  porAtivo: (ativoId) => http.get(`/ativos/${ativoId}/chamados`),
  criar: (dados) => http.post('/chamados', dados),
  atualizar: (id, dados) => http.put(`/chamados/${id}`, dados),
  assumir: (id) => http.patch(`/chamados/${id}/assumir`),
  mudarStatus: (id, status) => http.patch(`/chamados/${id}/status`, { status }),
  reabrir: (id) => http.patch(`/chamados/${id}/reabrir`),
  vincularAtivo: (id, ativoId) => http.patch(`/chamados/${id}/ativoId`, { ativoId }),
  excluir: (id) => http.delete(`/chamados/${id}`),
  mensagens: {
    listar: (id) => http.get(`/chamados/${id}/mensagens`),
    enviar: (id, texto) => http.post(`/chamados/${id}/mensagens`, { texto })
  },
  impactoExclusao: (id) => http.get(`/chamados/${id}/impacto-exclusao`)
}