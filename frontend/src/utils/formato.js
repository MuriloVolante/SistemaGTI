export function formatData(d) {
  if (!d) return '-'
  return d.substring(0, 10).split('-').reverse().join('/')
}

export function formatValor(v) {
  if (v == null || v === '') return '-'
  return 'R$ ' + Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 2 })
}