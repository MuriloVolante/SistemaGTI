const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

export const STATUS_LABEL = { ATIVO: 'Ativo', MANUTENCAO: 'Manutenção', ESTOQUE: 'Estoque', DESCARTADO: 'Descartado' }
export const STATUS_COR = { ATIVO: '#16a34a', MANUTENCAO: '#d97706', ESTOQUE: '#0a6ed1', DESCARTADO: '#8993a4' }
export const ORDEM_STATUS = ['ATIVO', 'MANUTENCAO', 'ESTOQUE', 'DESCARTADO']
export const RANK_CORES = ['#d4a017', '#94a3b8', '#a0522d']
export const DONUT_AZUIS = ['#0a6ed1', '#3b8fdd', '#6bafe8', '#95c8f0', '#064a8f', '#bcdcf6']

const FONTE = 'system-ui, -apple-system, Segoe UI, sans-serif'

export function fmt(v) {
  if (v == null) return '-'
  return Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function fmtCompacto(v) {
  const n = Number(v || 0)
  if (n >= 1000000) return 'R$ ' + (n / 1000000).toLocaleString('pt-BR', { maximumFractionDigits: 1 }) + 'M'
  if (n >= 1000) return 'R$ ' + (n / 1000).toLocaleString('pt-BR', { maximumFractionDigits: 1 }) + 'k'
  return 'R$ ' + n.toLocaleString('pt-BR', { maximumFractionDigits: 0 })
}

export function pctFmt(v, total) {
  if (!total) return '0%'
  return ((v / total) * 100).toLocaleString('pt-BR', { maximumFractionDigits: 1 }) + '%'
}

export function mesLabel(ym) {
  const m = parseInt(String(ym).split('-')[1], 10)
  return MESES[m - 1] || ym
}

export function ordenarEntries(obj) {
  return Object.entries(obj || {}).sort((a, b) => b[1] - a[1])
}

export function somar(entries) {
  return entries.reduce((a, e) => a + Number(e[1] || 0), 0)
}

export function alturaBarras(n) {
  return Math.max(120, (n || 1) * 34 + 46)
}

const AWARD_SVG = (cor) =>
  `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="${cor}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="6"/><path d="M15.477 12.89 17 22l-5-3-5 3 1.523-9.11"/></svg>`

export const rankIcons = []

export function carregarIconesRank(aoCarregar) {
  if (rankIcons.length) return
  RANK_CORES.forEach((cor, i) => {
    const img = new Image()
    img.onload = () => { if (typeof aoCarregar === 'function') aoCarregar() }
    img.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(AWARD_SVG(cor))
    rankIcons[i] = img
  })
}

export const donutCenterText = {
  id: 'donutCenterText',
  afterDraw(chart, args, opts) {
    const { ctx, chartArea } = chart
    if (!chartArea) return
    const cx = (chartArea.left + chartArea.right) / 2
    const cy = (chartArea.top + chartArea.bottom) / 2
    ctx.save()
    ctx.textAlign = 'center'
    ;((opts && opts.linhas) || []).forEach((l) => {
      ctx.fillStyle = l.cor
      ctx.font = l.font
      ctx.fillText(l.txt, cx, cy + l.dy)
    })
    ctx.restore()
  }
}

export const rankLabels = {
  id: 'rankLabels',
  afterDatasetsDraw(chart, args, opts) {
    if (!opts || !opts.ativo) return
    const ctx = chart.ctx
    const meta = chart.getDatasetMeta(0)
    const dados = chart.data.datasets[0].data
    const total = opts.total || 0
    const vFmt = opts.valorFmt || ((v) => String(v))
    ctx.save()
    ctx.textBaseline = 'middle'
    meta.data.forEach((bar, i) => {
      ctx.textAlign = 'left'
      ctx.font = `600 12.5px ${FONTE}`
      ctx.fillStyle = i < 3 ? RANK_CORES[i] : '#172b4d'
      const vTxt = vFmt(dados[i])
      ctx.fillText(vTxt, bar.x + 10, bar.y)
      const w = ctx.measureText(vTxt).width
      ctx.font = `500 11.5px ${FONTE}`
      ctx.fillStyle = '#5e6c84'
      ctx.fillText(pctFmt(dados[i], total), bar.x + 16 + w, bar.y)
      if (i < 3 && rankIcons[i] && rankIcons[i].complete) {
        ctx.drawImage(rankIcons[i], chart.scales.y.right - 24, bar.y - 9, 18, 18)
      }
    })
    ctx.restore()
  }
}

export function centroAtivos(total) {
  return [
    { txt: String(total), cor: '#172b4d', font: `600 26px ${FONTE}`, dy: 4 },
    { txt: 'ATIVOS', cor: '#5e6c84', font: `500 11px ${FONTE}`, dy: 22 }
  ]
}

export function centroValor(total) {
  return [
    { txt: fmtCompacto(total), cor: '#172b4d', font: `600 20px ${FONTE}`, dy: 2 },
    { txt: 'TOTAL', cor: '#5e6c84', font: `500 11px ${FONTE}`, dy: 20 }
  ]
}

export function donutData(labels, valores, cores) {
  return {
    labels,
    datasets: [{
      data: valores,
      backgroundColor: cores,
      borderColor: '#fff',
      borderWidth: 3,
      hoverOffset: 8,
      hoverBorderColor: '#fff'
    }]
  }
}

export function donutOpcoes(total, opts = {}) {
  const rotulo = opts.tooltipFmt || ((v) => `${v} ativos (${pctFmt(v, total)})`)
  const linhas = opts.linhas || centroAtivos(total)
  return {
    cutout: '68%',
    responsive: true,
    maintainAspectRatio: false,
    layout: { padding: 6 },
    plugins: {
      legend: { display: false },
      donutCenterText: { linhas },
      tooltip: {
        backgroundColor: '#172b4d', titleColor: '#fff', bodyColor: '#fff',
        padding: 10, cornerRadius: 8, displayColors: false,
        titleFont: { size: 12, weight: '600' }, bodyFont: { size: 12 }, caretSize: 6,
        callbacks: { label: (c) => rotulo(c.raw) }
      }
    }
  }
}

function bluesGradient(ctx) {
  const area = ctx.chart.chartArea
  if (!area) return '#0a6ed1'
  const g = ctx.chart.ctx.createLinearGradient(area.left, 0, area.right, 0)
  g.addColorStop(0, '#8ec2ef')
  g.addColorStop(1, '#064a8f')
  return g
}

export function barrasData(labels, valores, opts = {}) {
  const rank = !!opts.ranking
  const fill = rank
    ? (c) => (c.dataIndex < 3 ? RANK_CORES[c.dataIndex] : bluesGradient(c))
    : bluesGradient
  return {
    labels,
    datasets: [{
      data: valores,
      backgroundColor: fill,
      borderRadius: 6,
      barThickness: 20,
      borderSkipped: false
    }]
  }
}

export function barrasOpcoes(valores, opts = {}) {
  const rank = !!opts.ranking
  const total = valores.reduce((a, b) => a + Number(b || 0), 0)
  return {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    layout: { padding: { right: rank ? (opts.moeda ? 108 : 72) : 8 } },
    plugins: {
      legend: { display: false },
      rankLabels: { ativo: rank, total, valorFmt: opts.moeda ? fmtCompacto : null },
      tooltip: {
        mode: 'nearest', intersect: true, position: 'nearest',
        backgroundColor: '#172b4d', titleColor: '#fff', bodyColor: '#fff',
        padding: 10, cornerRadius: 8, displayColors: false,
        titleFont: { size: 12, weight: '600' }, bodyFont: { size: 12 }, caretSize: 6,
        callbacks: {
          label: (c) => opts.moeda
            ? `R$ ${fmt(c.raw)} (${pctFmt(c.raw, total)})`
            : `${c.raw} ativos (${pctFmt(c.raw, total)})`
        }
      }
    },
    scales: {
      x: {
        grid: { color: '#eef0f3' }, border: { display: false },
        ticks: opts.moeda
          ? { color: '#5e6c84', maxTicksLimit: 6, callback: (v) => fmtCompacto(v) }
          : { color: '#5e6c84', precision: 0, stepSize: 1 }
      },
      y: {
        grid: { display: false }, border: { display: false },
        ticks: { color: '#172b4d', font: { weight: '500' }, padding: rank ? 28 : 6 }
      }
    }
  }
}

export function linhaMensal(dados) {
  const entries = Object.entries(dados || {})
  return {
    labels: entries.map((e) => mesLabel(e[0])),
    datasets: [{
      data: entries.map((e) => Number(e[1])),
      borderColor: '#2563eb',
      borderWidth: 3,
      tension: 0.45,
      fill: true,
      backgroundColor: (c) => {
        const area = c.chart.chartArea
        if (!area) return 'transparent'
        const g = c.chart.ctx.createLinearGradient(0, area.top, 0, area.bottom)
        g.addColorStop(0, 'rgba(37,99,235,.16)')
        g.addColorStop(1, 'rgba(37,99,235,0)')
        return g
      },
      pointRadius: 5,
      pointHoverRadius: 7,
      pointBackgroundColor: '#2563eb',
      pointBorderColor: '#fff',
      pointBorderWidth: 2,
      pointHoverBorderWidth: 3
    }]
  }
}

export const opcoesLinhaMensal = {
  responsive: true,
  maintainAspectRatio: false,
  layout: { padding: { top: 12, right: 8 } },
  interaction: { mode: 'index', intersect: false },
  plugins: {
    legend: { display: false },
    tooltip: {
      backgroundColor: '#fff', titleColor: '#5e6c84', bodyColor: '#172b4d',
      borderColor: '#e4e6ea', borderWidth: 1,
      padding: 12, cornerRadius: 10, displayColors: false,
      titleFont: { size: 11, weight: '500' }, bodyFont: { size: 14, weight: '600' }, caretSize: 0,
      callbacks: { label: (c) => 'R$ ' + fmt(c.raw) }
    }
  },
  scales: {
    x: { grid: { display: false }, border: { display: false }, ticks: { color: '#8993a4', font: { size: 12 }, padding: 8 } },
    y: {
      beginAtZero: true,
      grid: { color: '#eef0f3', drawTicks: false },
      border: { display: false, dash: [4, 4] },
      ticks: { color: '#8993a4', font: { size: 12 }, padding: 12, maxTicksLimit: 6, callback: (v) => fmtCompacto(v) }
    }
  }
}