<script setup>
import { ref, watch } from 'vue'
import axios from 'axios'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseField from '@/components/base/BaseField.vue'

const props = defineProps({
  open: { type: Boolean, required: true },
  token: { type: String, default: null }
})
const emit = defineEmits(['sucesso'])

const nova = ref('')
const confirmacao = ref('')
const erro = ref('')
const carregando = ref(false)

watch(() => props.open, (v) => {
  if (v) { nova.value = ''; confirmacao.value = ''; erro.value = '' }
})

async function confirmar() {
  erro.value = ''
  if (nova.value.length < 6) { erro.value = 'A senha deve ter ao menos 6 caracteres.'; return }
  if (nova.value !== confirmacao.value) { erro.value = 'As senhas nao coincidem.'; return }

  carregando.value = true
  try {
    const { data } = await axios.post(
          '/api/auth/trocar-senha',
          { novaSenha: nova.value },
          { headers: { Authorization: `Bearer ${props.token}` } }
        )
        emit('sucesso', data.token)
      } catch (e) {
        erro.value = e.response?.data?.erro || 'Nao foi possivel alterar a senha.'
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

    <BaseField v-model="nova" label="Nova senha" type="password" placeholder="Minimo de 6 caracteres" />
    <BaseField v-model="confirmacao" label="Confirmar senha" type="password" placeholder="Repita a nova senha" @keyup.enter="confirmar" />

    <div v-if="erro" class="text-base text-perigo-forte bg-perigo-bg border border-perigo-borda rounded-raio-sm px-3 py-2">
      {{ erro }}
    </div>

    <template #footer>
      <BaseButton variant="primary" :disabled="carregando" @click="confirmar">
        {{ carregando ? 'Salvando...' : 'Salvar senha' }}
      </BaseButton>
    </template>
  </BaseModal>
</template>