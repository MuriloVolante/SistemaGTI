<script setup>
import { ref, onMounted } from 'vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ativosService from '@/services/ativos'
import { formatData } from '@/utils/formato'

const props = defineProps({ ativoId: { type: [Number, String], required: true } })

const itens = ref([])
const carregando = ref(true)
const erro = ref(false)

onMounted(async () => {
  try {
    const { data } = await ativosService.historico(props.ativoId)
    itens.value = data
  } catch {
    erro.value = true
  } finally {
    carregando.value = false
  }
})
</script>

<template>
  <div class="p-5">
    <EmptyState v-if="carregando" texto="Carregando..." />
    <EmptyState v-else-if="erro" texto="Erro ao carregar histórico." />
    <EmptyState v-else-if="!itens.length" texto="Sem histórico registrado." />
    <div v-else class="space-y-4">
      <div v-for="(h, i) in itens" :key="i" class="flex gap-3">
        <div class="w-2 h-2 rounded-full bg-primaria mt-1.5 flex-shrink-0"></div>
        <div>
          <div class="text-base text-texto">{{ h.descricao }}</div>
          <div class="text-sm text-texto-sub mt-0.5">{{ formatData(h.criadoEm) }} — {{ h.tipoEvento || '' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>