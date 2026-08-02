export const STATUS_BADGE = { ABERTO: 'bloq', EM_ANDAMENTO: 'date', CONCLUIDO: 'ativo' }
export const STATUS_LABEL = { ABERTO: 'Aberto', EM_ANDAMENTO: 'Em andamento', CONCLUIDO: 'Concluído' }

export const PRIORIDADE_BADGE = { MUITO_ALTA: 'bloq', ALTA: 'date', MEDIA: 'varchar', BAIXA: 'ativo' }
export const PRIORIDADE_LABEL = { MUITO_ALTA: 'Muito alta', ALTA: 'Alta', MEDIA: 'Média', BAIXA: 'Baixa' }

export function formatDataHora(d) {
  if (!d) return '-'
  const dt = new Date(d)
  return dt.toLocaleDateString('pt-BR') + ' ' + dt.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}