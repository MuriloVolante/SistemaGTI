<script setup>
import { ref, computed, watch } from 'vue'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseField from '@/components/base/BaseField.vue'
import ExclusaoModal from '@/components/ExclusaoModal.vue'
import usuariosService from '@/services/usuarios'
import { useToast } from '@/composables/useToast'

const props = defineProps({
  open: { type: Boolean, required: true },
  usuario: { type: Object, default: null }
})
const emit = defineEmits(['close', 'salvo'])
const { mostrar } = useToast()

const MAX_NOME_USUARIO = 50
const MAX_NOME_COMPLETO = 150
const MAX_DESCRICAO = 255
const MAX_EMAIL = 150
const MIN_SENHA = 8
const MAX_SENHA = 64
const SENHA_PADRAO = 'gti1234'
const RE_NOME_USUARIO = /^[a-z0-9][a-z0-9._-]{2,49}$/
const RE_EMAIL = /^[^\s@]+@[^\s@]+\.[A-Za-z]{2,}$/

const form = ref({})
const senha = ref('')
const senha2 = ref('')
const resetarSenha = ref(false)
const erroSenha = ref('')
const erros = ref({})
const salvando = ref(false)
const exclusaoAberta = ref(false)

const editando = computed(() => !!props.usuario?.id)
const podeSalvar = computed(
  () =>
    !salvando.value &&
    !!(form.value.nomeUsuario || '').trim() &&
    !!(form.value.nomeCompleto || '').trim()
)

watch(() => props.open, (v) => {
  if (!v) return
  senha.value = ''
  senha2.value = ''
  resetarSenha.value = false
  erroSenha.value = ''
  erros.value = {}
  form.value = {
    nomeUsuario: props.usuario?.nomeUsuario || '',
    nomeCompleto: props.usuario?.nomeCompleto || '',
    descricao: props.usuario?.descricao || '',
    email: props.usuario?.email || '',
    tipoAcesso: props.usuario?.tipoAcesso || 'COMUM'
  }
})

function validar() {
  const e = {}
  const u = (form.value.nomeUsuario || '').trim().toLowerCase()
  const n = (form.value.nomeCompleto || '').trim()
  const d = (form.value.descricao || '').trim()
  const m = (form.value.email || '').trim().toLowerCase()

  if (!u) e.nomeUsuario = 'Nome de usuario obrigatorio.'
  else if (!RE_NOME_USUARIO.test(u))
    e.nomeUsuario = 'Use de 3 a 50 caracteres: letras minusculas, numeros, ponto, hifen ou underline.'

  if (!n) e.nomeCompleto = 'Nome completo obrigatorio.'
  else if (n.length < 3) e.nomeCompleto = 'Nome completo deve ter ao menos 3 caracteres.'
  else if (n.length > MAX_NOME_COMPLETO) e.nomeCompleto = 'Nome completo deve ter no maximo 150 caracteres.'

  if (d.length > MAX_DESCRICAO) e.descricao = 'Descricao deve ter no maximo 255 caracteres.'

  if (m) {
    if (m.length > MAX_EMAIL) e.email = 'E-mail deve ter no maximo 150 caracteres.'
    else if (!RE_EMAIL.test(m)) e.email = 'E-mail invalido.'
  }

  if (!['COMUM', 'TI'].includes(form.value.tipoAcesso)) e.tipoAcesso = 'Tipo de acesso invalido.'

  if (senha.value || senha2.value) {
    if (senha.value.length < MIN_SENHA) e.senha = 'A senha deve ter no minimo 8 caracteres.'
    else if (senha.value.length > MAX_SENHA) e.senha = 'A senha deve ter no maximo 64 caracteres.'
    else if (senha.value === SENHA_PADRAO) e.senha = 'A senha nao pode ser igual a senha padrao.'
    else if (senha.value !== senha2.value) e.senha = 'As senhas nao coincidem.'
  }

  erros.value = e
  return Object.keys(e).length === 0
}

async function salvar() {
  if (salvando.value) return

  if (!validar()) {
    erroSenha.value = erros.value.senha || ''
    const primeiro =
      erros.value.nomeUsuario || erros.value.nomeCompleto ||
      erros.value.descricao || erros.value.email || erros.value.tipoAcesso
    if (primeiro) mostrar(primeiro)
    return
  }

  erroSenha.value = ''

  const body = {
    nomeUsuario: form.value.nomeUsuario.trim().toLowerCase(),
    nomeCompleto: form.value.nomeCompleto.trim(),
    descricao: (form.value.descricao || '').trim() || null,
    email: (form.value.email || '').trim().toLowerCase() || null,
    tipoAcesso: form.value.tipoAcesso
  }
  if (senha.value) {
    body.senha = senha.value
    body.precisaTrocarSenha = resetarSenha.value
  }

  salvando.value = true
  try {
    if (editando.value) await usuariosService.atualizar(props.usuario.id, body)
    else await usuariosService.criar(body)
    mostrar(editando.value ? 'Usuário atualizado.' : 'Usuário criado com sucesso.')
    emit('salvo')
  } catch (e) {
    mostrar('Erro: ' + (e.response?.data?.erro || 'Tente novamente.'))
  } finally {
    salvando.value = false
  }
}
</script>

<template>
  <BaseModal :open="open" :title="editando ? 'Editar usuário' : 'Novo usuário'" @close="emit('close')">
    <BaseField
      v-model="form.nomeUsuario"
      label="Nome de usuário *"
      placeholder="ex: murilo.silva"
      :maxlength="MAX_NOME_USUARIO"
      :erro="erros.nomeUsuario"
      autocapitalize="off"
      autocorrect="off"
      spellcheck="false"
    />
    <BaseField
      v-model="form.nomeCompleto"
      label="Nome completo *"
      placeholder="Nome completo"
      :maxlength="MAX_NOME_COMPLETO"
      :erro="erros.nomeCompleto"
    />

    <div class="mb-4">
      <label class="field-label">Descrição</label>
      <textarea
        v-model="form.descricao"
        rows="3"
        :maxlength="MAX_DESCRICAO"
        class="field-input resize-y"
        placeholder="Cargo, setor, observações..."
      ></textarea>
      <div v-if="erros.descricao" class="text-sm text-perigo-forte mt-1">{{ erros.descricao }}</div>
    </div>

    <div class="mb-4">
      <label class="field-label">Tipo de acesso</label>
      <select v-model="form.tipoAcesso" class="field-input">
        <option value="COMUM">Comum</option>
        <option value="TI">TI</option>
      </select>
    </div>

    <BaseField
      v-model="form.email"
      label="E-mail"
      type="email"
      placeholder="email@exemplo.com"
      :maxlength="MAX_EMAIL"
      :erro="erros.email"
      autocapitalize="off"
      autocorrect="off"
      spellcheck="false"
      opcional
    />

    <template v-if="editando">
      <hr class="border-borda my-5" />
      <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-3">Redefinir senha</div>

      <div class="grid grid-cols-2 gap-3">
        <BaseField
          v-model="senha"
          label="Nova senha"
          type="password"
          placeholder="Digite a senha"
          :maxlength="MAX_SENHA"
          autocomplete="new-password"
        />
        <BaseField
          v-model="senha2"
          label="Confirmar senha"
          type="password"
          placeholder="Repita a senha"
          :maxlength="MAX_SENHA"
          autocomplete="new-password"
        />
      </div>

      <div v-if="erroSenha" class="text-sm text-perigo-forte -mt-2 mb-3">{{ erroSenha }}</div>

      <div class="flex items-center justify-between">
        <label class="flex items-center gap-1.5 text-base text-texto cursor-pointer">
          <input v-model="resetarSenha" type="checkbox" />
          Trocar senha no próximo login
        </label>
        <BaseButton size="sm" variant="danger" @click="exclusaoAberta = true">Excluir usuário</BaseButton>
      </div>
    </template>

    <template #footer>
      <BaseButton @click="emit('close')">Cancelar</BaseButton>
      <BaseButton variant="primary" :disabled="!podeSalvar" @click="salvar">
        {{ editando ? 'Salvar' : 'Criar' }}
      </BaseButton>
    </template>
  </BaseModal>

  <ExclusaoModal
    v-if="editando"
    :open="exclusaoAberta"
    :titulo="`Excluir usuário &quot;${usuario.nomeUsuario}&quot;`"
    :impacto-url="`/usuarios/${usuario.id}/impacto-exclusao`"
    :delete-url="`/usuarios/${usuario.id}`"
    :palavra-confirmacao="usuario.nomeUsuario"
    @close="exclusaoAberta = false"
    @sucesso="emit('salvo')"
  />
</template>