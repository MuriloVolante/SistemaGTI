<script setup>
import { ref, watch } from 'vue'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseField from '@/components/base/BaseField.vue'
import integracaoService from '@/services/integracao'

const props = defineProps({ open: { type: Boolean, required: true } })
const emit = defineEmits(['close', 'criada'])

const ESCOPOS = [
  { valor: 'ATIVOS', label: 'Ativos', ajuda: 'Inventário de equipamentos' },
  { valor: 'USUARIOS', label: 'Usuários', ajuda: 'Cadastro de pessoas do sistema' },
  { valor: 'CHAMADOS', label: 'Chamados', ajuda: 'Solicitações abertas e fechadas' },
  { valor: 'DASHBOARD', label: 'Painel', ajuda: 'Números consolidados' }
]

const VALIDADES = [
  { valor: '', label: 'Sem validade' },
  { valor: '30', label: '30 dias' },
  { valor: '90', label: '90 dias' },
  { valor: '365', label: '1 ano' }
]

const nome = ref('')
const escopos = ref([])
const validade = ref('90')
const ips = ref('')
const erro = ref('')
const salvando = ref(false)

watch(() => props.open, (aberto) => {
  if (aberto) {
    nome.value = ''
    escopos.value = []
    validade.value = '90'
    ips.value = ''
    erro.value = ''
    salvando.value = false
  }
})

function alternarEscopo(valor) {
  const i = escopos.value.indexOf(valor)
  if (i >= 0) escopos.value.splice(i, 1)
  else escopos.value.push(valor)
}

async function salvar() {
  erro.value = ''
  if (!nome.value.trim()) {
    erro.value = 'Dê um nome para identificar quem vai usar esta chave.'
    return
  }
  if (escopos.value.length === 0) {
    erro.value = 'Selecione pelo menos um conjunto de dados.'
    return
  }
  salvando.value = true
  try {
    const { data } = await integracaoService.criar({
      nome: nome.value.trim(),
      escopos: escopos.value,
      diasValidade: validade.value ? Number(validade.value) : null,
      ipsPermitidos: ips.value.split(/[\n,;]/).map((i) => i.trim()).filter(Boolean)
    })
    emit('criada', data)
  } catch (e) {
    erro.value = e?.response?.data?.erro || 'Não foi possível criar a chave.'
  } finally {
    salvando.value = false
  }
}
</script>

<template>
  <BaseModal :open="open" title="Nova chave de API" max-width="max-w-[560px]" @close="emit('close')">
    <BaseField
      v-model="nome"
      label="Nome da chave"
      placeholder="Ex: Power BI do RH, Sistema da portaria"
    />
    <p class="-mt-3 mb-4 text-sm text-texto-sub">
      Use o nome de quem vai consumir os dados. É assim que você vai identificar essa chave depois.
    </p>

    <label class="field-label">Conjuntos de dados liberados</label>
    <div class="grid grid-cols-2 gap-2 mb-4">
      <button
        v-for="e in ESCOPOS"
        :key="e.valor"
        type="button"
        class="text-left px-3 py-2 rounded-raio-sm border transition-colors"
        :class="escopos.includes(e.valor)
          ? 'border-primaria bg-info-bg'
          : 'border-borda bg-surface hover:bg-surface-alt'"
        @click="alternarEscopo(e.valor)"
      >
        <span class="block text-base font-medium text-texto">{{ e.label }}</span>
        <span class="block text-xs text-texto-sub">{{ e.ajuda }}</span>
      </button>
    </div>

    <BaseField v-model="validade" label="Validade" as="select">
      <option v-for="v in VALIDADES" :key="v.valor" :value="v.valor">{{ v.label }}</option>
    </BaseField>

    <BaseField
      v-model="ips"
      label="Endereços de rede permitidos"
      as="textarea"
      rows="2"
      opcional
      placeholder="192.168.0.50, 192.168.1.0/24"
    />
    <p class="-mt-3 mb-2 text-sm text-texto-sub">
      Se preenchido, a chave só funciona a partir desses endereços. Deixe vazio para liberar de qualquer lugar.
    </p>

    <p v-if="erro" class="text-sm text-perigo-text bg-perigo-bg rounded-raio-sm px-3 py-2">{{ erro }}</p>

    <template #footer>
      <BaseButton @click="emit('close')">Cancelar</BaseButton>
      <BaseButton variant="primary" :disabled="salvando" @click="salvar">
        {{ salvando ? 'Criando...' : 'Criar chave' }}
      </BaseButton>
    </template>
  </BaseModal>
</template>