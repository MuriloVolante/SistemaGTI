<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import BaseButton from '@/components/base/BaseButton.vue'
import { formatDataHora } from '@/utils/chamados'

const props = defineProps({
  mensagens: { type: Array, default: () => [] },
  meuId: { type: [Number, String], default: null },
  estado: { type: String, default: 'input' }, // input | bloqueado | concluido
  textoBloqueio: { type: String, default: '' },
  textoConcluido: { type: String, default: 'Chamado concluído. A conversa não aceita mais mensagens.' },
  mostrarConcluir: { type: Boolean, default: true }
})
const emit = defineEmits(['enviar', 'concluir'])

const texto = ref('')
const lista = ref(null)

const temTexto = computed(() => !!texto.value.trim())

watch(() => props.mensagens, async () => {
  await nextTick()
  if (lista.value) lista.value.scrollTop = lista.value.scrollHeight
}, { deep: true })

function enviar() {
  if (!temTexto.value) return
  emit('enviar', texto.value.trim())
  texto.value = ''
}

function concluir() {
  if (!temTexto.value) return
  emit('concluir', texto.value.trim())
  texto.value = ''
}
</script>

<template>
  <div class="bg-surface border border-borda rounded-raio shadow-sombra flex flex-col h-[520px] overflow-hidden">
    <div class="px-4 py-3 bg-surface-alt border-b border-borda text-base font-semibold text-texto">Conversa</div>

    <div ref="lista" class="flex-1 overflow-y-auto p-4 space-y-3">
      <div v-if="!mensagens.length" class="text-center text-texto-fraco text-base py-8">Sem mensagens ainda.</div>

      <template v-for="(m, i) in mensagens" :key="i">
        <div
          v-if="m.tipo === 'ENCERRAMENTO' || m.tipo === 'REABERTURA'"
          class="border rounded-raio-sm px-3 py-2.5"
          :class="m.tipo === 'ENCERRAMENTO' ? 'bg-sucesso-bg border-green-200' : 'bg-aviso-bg border-aviso-borda'"
        >
          <div class="text-xs font-semibold uppercase tracking-wide mb-1"
               :class="m.tipo === 'ENCERRAMENTO' ? 'text-sucesso-text' : 'text-aviso-text'">
            {{ m.tipo === 'ENCERRAMENTO' ? 'Encerramento' : 'Reabertura' }} ·
            {{ m.autor?.nomeCompleto || m.autor?.nomeUsuario || '-' }}
          </div>
          <div class="text-base text-texto whitespace-pre-wrap">{{ m.mensagem }}</div>
          <div class="text-sm text-texto-sub mt-1">{{ formatDataHora(m.criadoEm) }}</div>
        </div>

        <div v-else class="flex" :class="m.autor?.id === meuId ? 'justify-end' : 'justify-start'">
          <div
            class="max-w-[75%] rounded-raio px-3 py-2"
            :class="m.autor?.id === meuId ? 'bg-primaria text-white' : 'bg-surface-alt border border-borda text-texto'"
          >
            <div v-if="m.autor?.id !== meuId" class="text-xs font-semibold mb-0.5">
              {{ m.autor?.nomeCompleto || m.autor?.nomeUsuario || '-' }}
            </div>
            <div class="text-base whitespace-pre-wrap">{{ m.mensagem }}</div>
            <div class="text-xs mt-1" :class="m.autor?.id === meuId ? 'text-white/70' : 'text-texto-sub'">
              {{ formatDataHora(m.criadoEm) }}
            </div>
          </div>
        </div>
      </template>
    </div>

    <div class="border-t border-borda p-3">
      <div v-if="estado === 'concluido'" class="text-center text-base text-texto-sub py-2">
        {{ textoConcluido }}
      </div>
      <div v-else-if="estado === 'bloqueado'" class="text-center text-base text-texto-sub py-2">
        {{ textoBloqueio }}
      </div>
      <div v-else>
        <textarea
          v-model="texto"
          rows="2"
          placeholder="Digite sua mensagem..."
          class="field-input resize-none mb-2"
          @keydown.enter.exact.prevent="enviar"
        ></textarea>
        <div class="flex justify-end gap-2">
          <BaseButton variant="primary" :disabled="!temTexto" @click="enviar">Enviar</BaseButton>
          <span v-if="mostrarConcluir" :title="temTexto ? '' : 'É necessário escrever um parecer no chat antes de concluir.'">
            <BaseButton variant="danger" :disabled="!temTexto" @click="concluir">Concluir</BaseButton>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>