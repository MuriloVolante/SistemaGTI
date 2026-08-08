<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import usuariosService from '@/services/usuarios'
import LayoutBlank from '@/layouts/LayoutBlank.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseField from '@/components/base/BaseField.vue'
import TrocaSenhaModal from './TrocaSenhaModal.vue'

const router = useRouter()
const auth = useAuthStore()

const nomeUsuario = ref('')
const senha = ref('')
const erro = ref('')
const carregando = ref(false)
const trocaAberta = ref(false)
const dadosLogin = ref(null)

async function entrar() {
  erro.value = ''
  carregando.value = true
  try {
    const { data } = await usuariosService.login(nomeUsuario.value, senha.value)
    dadosLogin.value = data
    if (data.precisaTrocarSenha) {
      trocaAberta.value = true
      return
    }
    auth.setSessao(data.token, data)
    router.push(auth.isTI ? '/home' : '/meus-chamados')
  } catch (e) {
    erro.value = e.response?.data?.erro || 'Usuario ou senha invalidos.'
  } finally {
    carregando.value = false
  }
}

function aoTrocar(novoToken) {
  trocaAberta.value = false
  auth.setSessao(novoToken, dadosLogin.value)
  router.push(auth.isTI ? '/home' : '/meus-chamados')
}
</script>

<template>
  <LayoutBlank>
    <div class="w-full max-w-[360px] bg-surface border border-borda rounded-raio shadow-sombra-md p-8">
      <div class="text-center mb-7">
        <div class="text-4xl font-bold text-texto">GTI</div>
        <div class="text-base text-texto-sub mt-1">Gestao de TI</div>
      </div>

      <BaseField v-model="nomeUsuario" label="Usuario" placeholder="Digite seu usuario" @keyup.enter="entrar" />
      <BaseField v-model="senha" label="Senha" type="password" placeholder="Digite sua senha" @keyup.enter="entrar" />

      <div v-if="erro" class="text-base text-perigo-forte bg-perigo-bg border border-perigo-borda rounded-raio-sm px-3 py-2 mb-4">
        {{ erro }}
      </div>

      <BaseButton variant="primary" block :disabled="carregando" @click="entrar">
        {{ carregando ? 'Entrando...' : 'Entrar' }}
      </BaseButton>
    </div>

    <TrocaSenhaModal
          :open="trocaAberta"
          :token="dadosLogin?.token"
          @sucesso="aoTrocar"
        />
  </LayoutBlank>
</template>