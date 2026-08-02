import http from './http'

export default {
  login: (nomeUsuario, senha) => http.post('/auth/login', { nomeUsuario, senha }),
  trocarSenha: (dados) => http.post('/auth/trocar-senha', dados),
  listar: () => http.get('/usuarios'),
  listarAtivos: () => http.get('/usuarios/ativos'),
  buscar: (id) => http.get(`/usuarios/${id}`),
  criar: (dados) => http.post('/usuarios', dados),
  atualizar: (id, dados) => http.put(`/usuarios/${id}`, dados),
  bloqueio: (id, bloqueado) => http.patch(`/usuarios/${id}/bloqueio`, { bloqueado }),
  impactoExclusao: (id) => http.get(`/usuarios/${id}/impacto-exclusao`),
  excluir: (id) => http.delete(`/usuarios/${id}`)
}