<script setup>
import { computed } from 'vue'
import { Doughnut } from 'vue-chartjs'
import { donutData, donutOpcoes, donutCenterText, pctFmt } from '@/utils/graficos'

const props = defineProps({
  itens: { type: Array, required: true },
  total: { type: Number, required: true },
  linhas: { type: Array, default: null },
  tooltipFmt: { type: Function, default: null },
  valorFmt: { type: Function, default: null },
  vazio: { type: String, default: 'Nenhum registro encontrado.' }
})

const visiveis = computed(() => props.itens.filter((i) => Number(i.valor) > 0))

const dados = computed(() =>
  donutData(
    visiveis.value.map((i) => i.label),
    visiveis.value.map((i) => Number(i.valor)),
    visiveis.value.map((i) => i.cor)
  )
)

const opcoes = computed(() =>
  donutOpcoes(props.total, { linhas: props.linhas, tooltipFmt: props.tooltipFmt })
)

function exibirValor(v) {
  return props.valorFmt ? props.valorFmt(v) : String(v)
}
</script>

<template>
  <div class="flex items-center gap-5 h-full">
    <div class="relative h-full aspect-square flex-shrink-0">
      <Doughnut :data="dados" :options="opcoes" :plugins="[donutCenterText]" />
    </div>
    <div class="flex-1 min-w-0 flex flex-col gap-0.5">
      <div v-if="!total" class="text-base text-texto-fraco italic">{{ vazio }}</div>
      <div
        v-for="i in itens"
        v-else
        :key="i.label"
        class="flex items-center gap-2 px-2 py-[7px] rounded-raio-sm text-base hover:bg-surface-alt"
      >
        <span class="w-[9px] h-[9px] rounded-full flex-shrink-0" :style="{ background: i.cor }"></span>
        <span class="flex-1 min-w-0 truncate text-texto">{{ i.label }}</span>
        <span class="font-semibold text-texto tabular-nums">{{ exibirValor(i.valor) }}</span>
        <span class="w-[52px] text-right text-texto-fraco text-sm tabular-nums">{{ pctFmt(i.valor, total) }}</span>
      </div>
    </div>
  </div>
</template>