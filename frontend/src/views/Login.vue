<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import usuariosService from '@/services/usuarios'
import LayoutBlank from '@/layouts/LayoutBlank.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseField from '@/components/base/BaseField.vue'
import TrocaSenhaModal from './TrocaSenhaModal.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const MAX_USUARIO = 50
const MAX_SENHA = 128

const nomeUsuario = ref('')
const senha = ref('')
const erro = ref('')
const carregando = ref(false)
const trocaAberta = ref(false)
const dadosLogin = ref(null)

const usuarioLimpo = computed(() => nomeUsuario.value.trim())
const podeEnviar = computed(
  () => usuarioLimpo.value.length > 0 && senha.value.length > 0 && !carregando.value
)

function rotaInternaSegura(valor) {
  if (typeof valor !== 'string') return null
  if (!valor.startsWith('/')) return null
  if (valor.startsWith('//') || valor.startsWith('/\\')) return null
  return valor
}

function destinoPosLogin() {
  return rotaInternaSegura(route.query.redirect) || (auth.isTI ? '/home' : '/meus-chamados')
}

function limparErro() {
  if (erro.value) erro.value = ''
}

async function entrar() {
  if (carregando.value) return
  erro.value = ''

  if (!usuarioLimpo.value) {
    erro.value = 'Informe o usuario.'
    return
  }
  if (!senha.value) {
    erro.value = 'Informe a senha.'
    return
  }
  if (usuarioLimpo.value.length > MAX_USUARIO) {
    erro.value = 'Usuario deve ter no maximo ' + MAX_USUARIO + ' caracteres.'
    return
  }
  if (senha.value.length > MAX_SENHA) {
    erro.value = 'Senha deve ter no maximo ' + MAX_SENHA + ' caracteres.'
    return
  }

  carregando.value = true
  try {
    const { data } = await usuariosService.login(usuarioLimpo.value, senha.value)
    dadosLogin.value = data
    if (data.precisaTrocarSenha) {
      trocaAberta.value = true
      return
    }
    auth.setSessao(data.token, data)
    router.push(destinoPosLogin())
  } catch (e) {
    const status = e.response?.status
    if (status === 429) {
      erro.value = e.response?.data?.erro || 'Muitas tentativas. Tente novamente mais tarde.'
    } else if (status === 400 || status === 401) {
      erro.value = e.response?.data?.erro || 'Usuario ou senha invalidos.'
    } else {
      erro.value = 'Nao foi possivel entrar. Tente novamente.'
    }
  } finally {
    carregando.value = false
  }
}

function aoTrocar(novoToken) {
  trocaAberta.value = false
  auth.setSessao(novoToken, dadosLogin.value)
  router.push(destinoPosLogin())
}
</script>

<template>
  <LayoutBlank>
    <div class="w-full max-w-[360px] bg-surface border border-borda rounded-raio shadow-sombra-md p-5 md:p-8">
      <div class="text-center mb-7">
        <div class="text-4xl font-bold text-texto">GTI</div>
        <div class="text-base text-texto-sub mt-1">Gestao de TI</div>
      </div>

      <BaseField
        v-model="nomeUsuario"
        label="Usuario"
        placeholder="Digite seu usuario"
        :maxlength="MAX_USUARIO"
        autocomplete="username"
        autocapitalize="off"
        autocorrect="off"
        spellcheck="false"
        @input="limparErro"
        @keyup.enter="entrar"
      />
      <BaseField
        v-model="senha"
        label="Senha"
        type="password"
        placeholder="Digite sua senha"
        :maxlength="MAX_SENHA"
        autocomplete="current-password"
        @input="limparErro"
        @keyup.enter="entrar"
      />

      <div v-if="erro" class="text-base text-perigo-forte bg-perigo-bg border border-perigo-borda rounded-raio-sm px-3 py-2 mb-4">
        {{ erro }}
      </div>

      <BaseButton variant="primary" block :disabled="!podeEnviar" @click="entrar">
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