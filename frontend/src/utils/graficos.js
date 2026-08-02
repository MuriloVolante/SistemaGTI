const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

export const STATUS_LABEL = { ATIVO: 'Ativo', MANUTENCAO: 'Manutenção', ESTOQUE: 'Estoque', DESCARTADO: 'Descartado' }
export const STATUS_COR = { ATIVO: '#16a34a', MANUTENCAO: '#d97706', ESTOQUE: '#0a6ed1', DESCARTADO: '#8993a4' }

export function fmt(v) {
  if (v == null) return '-'
  return Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function mesLabel(ym) {
  const m = parseInt(String(ym).split('-')[1], 10)
  return MESES[m - 1] || ym
}

export function barrasValor(dados) {
  const entries = Object.entries(dados || {}).sort((a, b) => b[1] - a[1])
  return {
    labels: entries.map((e) => e[0]),
    datasets: [{ data: entries.map((e) => Number(e[1])), backgroundColor: '#0a6ed1', borderRadius: 4, barThickness: 22 }]
  }
}

export const opcoesBarrasValor = {
  indexAxis: 'y',
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false }, tooltip: { callbacks: { label: (c) => 'R$ ' + fmt(c.raw) } } },
  scales: {
    x: { grid: { color: '#dfe1e6' }, ticks: { color: '#5e6c84', callback: (v) => 'R$ ' + v / 1000 + 'k' } },
    y: { grid: { display: false }, ticks: { color: '#5e6c84' } }
  }
}

export function linhaMensal(dados) {
  const entries = Object.entries(dados || {})
  return {
    labels: entries.map((e) => mesLabel(e[0])),
    datasets: [{
      data: entries.map((e) => Number(e[1])),
      borderColor: '#0a6ed1', borderWidth: 2, pointRadius: 3,
      pointBackgroundColor: '#0a6ed1', tension: 0.25
    }]
  }
}

export const opcoesLinhaMensal = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false }, tooltip: { callbacks: { label: (c) => 'R$ ' + fmt(c.raw) } } },
  scales: {
    x: { grid: { display: false }, ticks: { color: '#5e6c84' } },
    y: { grid: { color: '#dfe1e6' }, ticks: { color: '#5e6c84', callback: (v) => 'R$ ' + fmt(v) } }
  }
}

export function barrasContagem(labels, valores, cores) {
  return { labels, datasets: [{ data: valores, backgroundColor: cores, borderRadius: 4, barThickness: 22 }] }
}

export const opcoesBarrasContagem = {
  indexAxis: 'y',
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    x: { grid: { color: '#dfe1e6' }, ticks: { color: '#5e6c84', precision: 0, stepSize: 1 } },
    y: { grid: { display: false }, ticks: { color: '#5e6c84' } }
  }
}