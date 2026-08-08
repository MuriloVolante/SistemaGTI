<script setup>
import { ref, watch } from 'vue'
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

watch(() => props.open, async (v) => {
  if (!v) return
  tipo.value = 'CORRETIVA'
  dataManutencao.value = new Date().toISOString().substring(0, 10)
  descricao.value = ''
  custo.value = ''
  garantia.value = false
  tecnicoSelecionado.value = ''
  tecnicoTerceiro.value = ''
  isTerceiro.value = false
  try {
    const { data } = await usuariosService.listar()
    tecnicos.value = data.filter((u) => u.tipoAcesso === 'TI' && u.ativo !== false)
  } catch {
    mostrar('Erro ao carregar técnicos.')
  }
})

async function salvar() {
  if (!descricao.value.trim()) { mostrar('Informe a descrição da manutenção.'); return }
  if (!dataManutencao.value) { mostrar('Informe a data.'); return }

  salvando.value = true
  try {
    await ativosService.manutencoes.criar(props.ativoId, {
      tipo: tipo.value,
      dataManutencao: dataManutencao.value,
      descricao: descricao.value.trim(),
      tecnico: isTerceiro.value ? tecnicoTerceiro.value.trim() : tecnicoSelecionado.value,
      custo: custo.value || null,
      garantia: garantia.value
    })
    mostrar('Manutenção registrada. Status alterado para Em manutenção.')
    emit('salvo')
  } catch {
    mostrar('Erro ao registrar manutenção.')
  } finally {
    salvando.value = false
  }
}
</script>

<template>
  <BaseModal :open="open" title="Registrar Manutenção" max-width="max-w-[560px]" @close="emit('close')">
    <div class="grid grid-cols-2 gap-3">
      <div class="mb-4">
        <label class="field-label">Tipo de manutenção</label>
        <select v-model="tipo" class="field-input">
          <option value="CORRETIVA">Corretiva</option>
          <option value="PREVENTIVA">Preventiva</option>
          <option value="PREDITIVA">Preditiva</option>
        </select>
      </div>
      <BaseField v-model="dataManutencao" label="Data da manutenção" type="date" />
    </div>

    <div class="mb-4">
      <label class="field-label">Descrição</label>
      <textarea v-model="descricao" rows="3" class="field-input resize-y" placeholder="Descreva a manutenção..."></textarea>
    </div>

    <div class="grid grid-cols-2 gap-3">
      <div class="mb-4">
        <label class="field-label">Técnico responsável</label>
        <select v-if="!isTerceiro" v-model="tecnicoSelecionado" class="field-input">
          <option value="">Selecione o técnico...</option>
          <option v-for="t in tecnicos" :key="t.id" :value="t.nomeCompleto || t.nomeUsuario">
            {{ t.nomeCompleto || t.nomeUsuario }}
          </option>
        </select>
        <input v-else v-model="tecnicoTerceiro" class="field-input" placeholder="Nome do técnico terceirizado" />
        <button
          type="button"
          class="mt-1.5 text-sm text-primaria-text bg-transparent border-none cursor-pointer p-0 hover:underline"
          @click="isTerceiro = !isTerceiro"
        >
          {{ isTerceiro ? '← Selecionar técnico interno' : '+ Técnico terceiro (externo)' }}
        </button>
      </div>
      <BaseField v-model="custo" label="Custo (R$)" type="number" placeholder="0,00" />
    </div>

    <label class="flex items-center gap-1.5 text-base text-texto cursor-pointer">
      <input v-model="garantia" type="checkbox" />
      Manutenção coberta pela garantia
    </label>

    <template #footer>
      <BaseButton @click="emit('close')">Cancelar</BaseButton>
      <BaseButton variant="primary" :disabled="salvando" @click="salvar">Registrar</BaseButton>
    </template>
  </BaseModal>
</template>