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

const form = ref({})
const senha = ref('')
const senha2 = ref('')
const resetarSenha = ref(false)
const erroSenha = ref('')
const salvando = ref(false)
const exclusaoAberta = ref(false)

const editando = computed(() => !!props.usuario?.id)

watch(() => props.open, (v) => {
  if (!v) return
  senha.value = ''
  senha2.value = ''
  resetarSenha.value = false
  erroSenha.value = ''
  form.value = {
    nomeUsuario: props.usuario?.nomeUsuario || '',
    nomeCompleto: props.usuario?.nomeCompleto || '',
    descricao: props.usuario?.descricao || '',
    email: props.usuario?.email || '',
    tipoAcesso: props.usuario?.tipoAcesso || 'COMUM'
  }
})

async function salvar() {
  erroSenha.value = ''
  if (!form.value.nomeUsuario.trim()) { mostrar('Nome de usuário obrigatório.'); return }
  if (!form.value.nomeCompleto.trim()) { mostrar('Nome completo obrigatório.'); return }

  if (senha.value || senha2.value) {
    if (senha.value !== senha2.value) { erroSenha.value = 'As senhas não coincidem.'; return }
    if (senha.value.length < 8) { erroSenha.value = 'A senha deve ter no mínimo 8 caracteres.'; return }
  }

  const body = { ...form.value }
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
    <BaseField v-model="form.nomeUsuario" label="Nome de usuário *" placeholder="ex: murilo.silva" />
    <BaseField v-model="form.nomeCompleto" label="Nome completo *" placeholder="Nome completo" />

    <div class="mb-4">
      <label class="field-label">Descrição</label>
      <textarea v-model="form.descricao" rows="3" class="field-input resize-y" placeholder="Cargo, setor, observações..."></textarea>
    </div>

    <div class="mb-4">
      <label class="field-label">Tipo de acesso</label>
      <select v-model="form.tipoAcesso" class="field-input">
        <option value="COMUM">Comum</option>
        <option value="TI">TI</option>
      </select>
    </div>

    <BaseField v-model="form.email" label="E-mail" type="email" placeholder="email@exemplo.com" opcional />

    <template v-if="editando">
      <hr class="border-borda my-5" />
      <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-3">Redefinir senha</div>

      <div class="grid grid-cols-2 gap-3">
        <BaseField v-model="senha" label="Nova senha" type="password" placeholder="Digite a senha" />
        <BaseField v-model="senha2" label="Confirmar senha" type="password" placeholder="Repita a senha" />
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
      <BaseButton variant="primary" :disabled="salvando" @click="salvar">
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