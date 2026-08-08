<script setup>
import { ref, computed, watch } from 'vue'
import http from '@/services/http'
import { useToast } from '@/composables/useToast'
import BaseButton from '@/components/base/BaseButton.vue'

const props = defineProps({
  open: { type: Boolean, required: true },
  titulo: { type: String, default: 'Excluir permanentemente' },
  impactoUrl: { type: String, default: '' },
  deleteUrl: { type: String, default: '' },
  palavraConfirmacao: { type: String, default: '' }
})
const emit = defineEmits(['close', 'sucesso'])
const { mostrar } = useToast()

const LABELS = {
  usuarios: ['usuário', 'usuários'],
  tipos: ['tipo de ativo', 'tipos de ativo'],
  campos: ['campo dinâmico', 'campos dinâmicos'],
  ativos: ['ativo', 'ativos'],
  camposPreenchidos: ['valor de campo preenchido', 'valores de campos preenchidos'],
  anexos: ['anexo', 'anexos'],
  historico: ['registro de histórico', 'registros de histórico'],
  manutencoes: ['manutenção', 'manutenções'],
  chamados: ['chamado', 'chamados'],
  mensagens: ['mensagem de chamado', 'mensagens de chamado']
}

const DESVINCULOS = {
  chamadosDesvinculados: ['chamado ficará sem técnico', 'chamados ficarão sem técnico'],
  ativosDesvinculados: ['ativo ficará sem responsável', 'ativos ficarão sem responsável']
}

const carregando = ref(false)
const itens = ref([])
const desvinculos = ref([])
const digitado = ref('')
const excluindo = ref(false)

const confere = computed(() => digitado.value.trim() === props.palavraConfirmacao)
const mostrarErro = computed(() => digitado.value.trim() !== '' && !confere.value)

watch(() => props.open, async (v) => {
  if (!v) return
  digitado.value = ''
  itens.value = []
  desvinculos.value = []
  carregando.value = true
  try {
    const { data } = await http.get(props.impactoUrl)
    const listaItens = []
    const listaDesv = []
    for (const [k, valor] of Object.entries(data)) {
      if (!valor) continue
      if (LABELS[k]) listaItens.push({ valor, texto: valor === 1 ? LABELS[k][0] : LABELS[k][1] })
      else if (DESVINCULOS[k]) listaDesv.push({ valor, texto: valor === 1 ? DESVINCULOS[k][0] : DESVINCULOS[k][1] })
    }
    itens.value = listaItens
    desvinculos.value = listaDesv
  } catch (e) {
    mostrar(e.response?.data?.erro || 'Erro ao calcular impacto.')
    emit('close')
  } finally {
    carregando.value = false
  }
})

async function excluir() {
  excluindo.value = true
  try {
    await http.delete(props.deleteUrl)
    mostrar('Excluído permanentemente.')
    emit('sucesso')
    emit('close')
  } catch (e) {
    mostrar(e.response?.data?.erro || 'Erro ao excluir.')
  } finally {
    excluindo.value = false
  }
}
</script>

<template>
  <div v-if="open" class="fixed inset-0 bg-[rgba(9,30,66,.54)] z-[110] flex items-center justify-center p-4" @click.self="emit('close')">
    <div class="bg-surface rounded-raio shadow-sombra-md p-6 w-full max-w-[480px] max-h-[90vh] overflow-y-auto">
      <div class="text-xl font-semibold mb-4 text-texto">{{ titulo }}</div>

      <div class="bg-perigo-bg border border-perigo-borda text-perigo-text rounded-raio-sm px-3.5 py-3 text-base leading-relaxed mb-5">
        Esta exclusão é <strong>permanente</strong> e não pode ser desfeita.
        Os registros abaixo serão apagados do banco de dados.
      </div>

      <div v-if="carregando" class="text-base text-texto-sub py-6 text-center">Calculando impacto...</div>

      <div v-else>
        <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">Será excluído</div>
        <ul class="list-disc pl-5 text-base text-texto mb-4 space-y-1">
          <li v-for="(i, idx) in itens" :key="idx"><strong>{{ i.valor }}</strong> {{ i.texto }}</li>
          <li v-if="!itens.length" class="list-none text-texto-fraco">Nenhum registro dependente.</li>
        </ul>

        <div v-if="desvinculos.length" class="mb-4">
          <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-2">Será desvinculado (não excluído)</div>
          <ul class="list-disc pl-5 text-base text-texto-sub space-y-1">
            <li v-for="(d, idx) in desvinculos" :key="idx"><strong>{{ d.valor }}</strong> {{ d.texto }}</li>
          </ul>
        </div>

        <div class="mb-4">
          <label class="field-label">
            Digite <span class="font-mono normal-case tracking-normal text-texto">{{ palavraConfirmacao }}</span> para confirmar
          </label>
          <input v-model="digitado" type="text" autocomplete="off" class="field-input" :class="mostrarErro && 'border-perigo-borda'" />
          <div v-if="mostrarErro" class="text-sm text-perigo-forte mt-1">
            O texto não confere. A exclusão só é liberada com o texto exato.
          </div>
        </div>
      </div>

      <div class="flex justify-end gap-2 pt-4 border-t border-borda">
        <BaseButton @click="emit('close')">Cancelar</BaseButton>
        <BaseButton variant="danger" :disabled="!confere || excluindo" @click="excluir">
          {{ excluindo ? 'Excluindo...' : 'Excluir permanentemente' }}
        </BaseButton>
      </div>
    </div>
  </div>
</template>