<script setup>
import { computed, ref, watch } from 'vue'
import axios from 'axios'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseField from '@/components/base/BaseField.vue'

const props = defineProps({
  open: { type: Boolean, required: true },
  token: { type: String, default: null }
})
const emit = defineEmits(['sucesso'])

const MIN_SENHA = 8
const MAX_SENHA = 64
const SENHA_PADRAO = 'gti1234'

const nova = ref('')
const confirmacao = ref('')
const erro = ref('')
const carregando = ref(false)

const podeEnviar = computed(
  () => nova.value.length > 0 && confirmacao.value.length > 0 && !carregando.value
)

function limpar() {
  nova.value = ''
  confirmacao.value = ''
  erro.value = ''
}

function limparErro() {
  if (erro.value) erro.value = ''
}

watch(() => props.open, () => limpar())

function validar() {
  if (!nova.value) return 'Informe a nova senha.'
  if (nova.value.trim().length === 0) return 'A senha nao pode conter apenas espacos.'
  if (nova.value.length < MIN_SENHA) return 'A senha deve ter ao menos ' + MIN_SENHA + ' caracteres.'
  if (nova.value.length > MAX_SENHA) return 'A senha deve ter no maximo ' + MAX_SENHA + ' caracteres.'
  if (nova.value === SENHA_PADRAO) return 'A nova senha deve ser diferente da senha padrao.'
  if (nova.value !== confirmacao.value) return 'As senhas nao coincidem.'
  return ''
}

async function confirmar() {
  if (carregando.value) return

  const problema = validar()
  if (problema) {
    erro.value = problema
    return
  }

  erro.value = ''
  carregando.value = true
  try {
    const { data } = await axios.post(
      '/api/auth/trocar-senha',
      { novaSenha: nova.value },
      { headers: { Authorization: `Bearer ${props.token}` }, timeout: 15000 }
    )
    if (!data?.token) {
      erro.value = 'Resposta invalida do servidor.'
      return
    }
    limpar()
    emit('sucesso', data.token)
  } catch (e) {
    const status = e.response?.status
    if (status === 400 || status === 401) {
      erro.value = e.response?.data?.erro || 'Nao foi possivel alterar a senha.'
    } else {
      erro.value = 'Nao foi possivel alterar a senha. Tente novamente.'
    }
  } finally {
    carregando.value = false
  }
}
</script>

<template>
  <BaseModal :open="open" title="Defina uma nova senha" max-width="max-w-[400px]">
    <p class="text-base text-texto-sub mb-5 leading-relaxed">
      Este e o primeiro acesso com a senha padrao. Escolha uma nova senha para continuar.
    </p>

    <BaseField
      v-model="nova"
      label="Nova senha"
      type="password"
      placeholder="Mínimo de 8 caracteres"
      :maxlength="MAX_SENHA"
      autocomplete="new-password"
      @input="limparErro"
      @keyup.enter="confirmar"
    />
    <BaseField
      v-model="confirmacao"
      label="Confirmar senha"
      type="password"
      placeholder="Repita a nova senha"
      :maxlength="MAX_SENHA"
      autocomplete="new-password"
      @input="limparErro"
      @keyup.enter="confirmar"
    />

    <div v-if="erro" class="text-base text-perigo-forte bg-perigo-bg border border-perigo-borda rounded-raio-sm px-3 py-2">
      {{ erro }}
    </div>

    <template #footer>
      <BaseButton variant="primary" :disabled="!podeEnviar" @click="confirmar">
        {{ carregando ? 'Salvando...' : 'Salvar senha' }}
      </BaseButton>
    </template>
  </BaseModal>
</template>