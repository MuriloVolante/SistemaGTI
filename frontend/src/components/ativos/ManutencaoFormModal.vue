<script setup>
import { ref, computed, watch } from 'vue'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseField from '@/components/base/BaseField.vue'
import ativosService from '@/services/ativos'
import usuariosService from '@/services/usuarios'
import { useToast } from '@/composables/useToast'

const props = defineProps({
  open: { type: Boolean, required: true },
  ativoId: { type: [Number, String], required: true }
})
const emit = defineEmits(['close', 'salvo'])
const { mostrar } = useToast()

const MIN_DESCRICAO = 5
const MAX_DESCRICAO = 1000
const MAX_TECNICO = 150
const MAX_CUSTO = 9999999999.99

const tipo = ref('CORRETIVA')
const dataManutencao = ref('')
const descricao = ref('')
const custo = ref('')
const garantia = ref(false)

const tecnicos = ref([])
const tecnicoSelecionado = ref('')
const tecnicoTerceiro = ref('')
const isTerceiro = ref(false)

const salvando = ref(false)

function dataLocalISO() {
  const d = new Date()
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().substring(0, 10)
}

function detalheErro(e) {
  if (e.response) return e.response.data?.erro || 'HTTP ' + e.response.status
  if (e.request) return 'sem resposta do servidor'
  return e.message || 'falha inesperada'
}

const hoje = computed(() => dataLocalISO())

watch(() => props.open, async (v) => {
  if (!v) return
  tipo.value = 'CORRETIVA'
  dataManutencao.value = dataLocalISO()
  descricao.value = ''
  custo.value = ''
  garantia.value = false
  tecnicoSelecionado.value = ''
  tecnicoTerceiro.value = ''
  isTerceiro.value = false
  try {
    const { data } = await usuariosService.listar()
    tecnicos.value = data.filter((u) => u.tipoAcesso === 'TI' && u.bloqueado !== true)
  } catch (e) {
    console.error('[manutencao] falha ao carregar tecnicos', e)
    mostrar('Erro ao carregar técnicos: ' + detalheErro(e), 4000)
  }
})

function validarManutencao() {
  const d = descricao.value.trim()
  if (!d) return 'Informe a descricao da manutencao.'
  if (d.length < MIN_DESCRICAO) return 'A descricao deve ter ao menos ' + MIN_DESCRICAO + ' caracteres.'
  if (d.length > MAX_DESCRICAO) return 'A descricao deve ter no maximo ' + MAX_DESCRICAO + ' caracteres.'

  if (!dataManutencao.value) return 'Informe a data.'
  if (dataManutencao.value > hoje.value) return 'A data da manutencao nao pode ser futura.'

  if (!['CORRETIVA', 'PREVENTIVA', 'PREDITIVA'].includes(tipo.value)) return 'Tipo de manutencao invalido.'

  const tec = isTerceiro.value ? tecnicoTerceiro.value.trim() : tecnicoSelecionado.value
  if (tec && tec.length > MAX_TECNICO) return 'Nome do tecnico deve ter no maximo ' + MAX_TECNICO + ' caracteres.'

  if (custo.value !== '' && custo.value !== null && custo.value !== undefined) {
    const valor = Number(String(custo.value).replace(',', '.'))
    if (!Number.isFinite(valor)) return 'Custo invalido.'
    if (valor < 0) return 'Custo nao pode ser negativo.'
    if (valor > MAX_CUSTO) return 'Custo excede o limite permitido.'
    if (!/^\d+([.,]\d{1,2})?$/.test(String(custo.value))) return 'Custo deve ter no maximo 2 casas decimais.'
  }

  return ''
}

async function salvar() {
  if (salvando.value) return

  const problema = validarManutencao()
  if (problema) { mostrar(problema); return }

  const tec = isTerceiro.value ? tecnicoTerceiro.value.trim() : tecnicoSelecionado.value

  salvando.value = true
  try {
    await ativosService.manutencoes.criar(props.ativoId, {
      tipo: tipo.value,
      dataManutencao: dataManutencao.value,
      descricao: descricao.value.trim(),
      tecnico: tec || null,
      custo: custo.value === '' || custo.value === null || custo.value === undefined
        ? null
        : String(custo.value).trim(),
      garantia: garantia.value
    })
    mostrar('Manutenção registrada. Status alterado para Em manutenção.')
    emit('salvo')
  } catch (e) {
    console.error('[manutencao] falha ao registrar', e)
    mostrar('Erro ao registrar manutenção: ' + detalheErro(e), 4000)
  } finally {
    salvando.value = false
  }
}
</script>

<template>
  <BaseModal :open="open" title="Registrar Manutenção" max-width="max-w-[560px]" @close="emit('close')">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-x-3">
      <div class="mb-4">
        <label class="field-label">Tipo de manutenção</label>
        <select v-model="tipo" class="field-input">
          <option value="CORRETIVA">Corretiva</option>
          <option value="PREVENTIVA">Preventiva</option>
          <option value="PREDITIVA">Preditiva</option>
        </select>
      </div>
      <BaseField v-model="dataManutencao" label="Data da manutenção" type="date" :max="hoje" />
    </div>

    <div class="mb-4">
      <label class="field-label">Descrição</label>
      <textarea
        v-model="descricao"
        rows="3"
        :maxlength="MAX_DESCRICAO"
        class="field-input resize-y"
        placeholder="Descreva a manutenção..."
      ></textarea>
      <div class="text-sm text-texto-fraco text-right mt-1">{{ descricao.length }}/{{ MAX_DESCRICAO }}</div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-x-3">
      <div class="mb-4">
        <label class="field-label">Técnico responsável</label>
        <select v-if="!isTerceiro" v-model="tecnicoSelecionado" class="field-input">
          <option value="">Selecione o técnico...</option>
          <option v-for="t in tecnicos" :key="t.id" :value="t.nomeCompleto || t.nomeUsuario">
            {{ t.nomeCompleto || t.nomeUsuario }}
          </option>
        </select>
        <input v-else v-model="tecnicoTerceiro" :maxlength="MAX_TECNICO" class="field-input" placeholder="Nome do técnico terceirizado" />
        <button
          type="button"
          class="mt-1.5 text-sm text-primaria-text bg-transparent border-none cursor-pointer p-0 hover:underline"
          @click="isTerceiro = !isTerceiro"
        >
          {{ isTerceiro ? '← Selecionar técnico interno' : '+ Técnico terceiro (externo)' }}
        </button>
      </div>
      <BaseField v-model="custo" label="Custo (R$)" type="number" min="0" step="0.01" placeholder="0,00" />
    </div>

    <label class="flex items-center gap-1.5 text-base text-texto cursor-pointer">
      <input v-model="garantia" type="checkbox" />
      Manutenção coberta pela garantia
    </label>

    <template #footer>
      <BaseButton @click="emit('close')">Cancelar</BaseButton>
      <BaseButton variant="primary" :disabled="salvando" @click="salvar">
        {{ salvando ? 'Registrando...' : 'Registrar' }}
      </BaseButton>
    </template>
  </BaseModal>
</template>