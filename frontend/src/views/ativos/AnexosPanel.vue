<script setup>
import { ref, onMounted } from 'vue'
import BaseButton from '@/components/base/BaseButton.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import VisorAnexo from '@/components/VisorAnexo.vue'
import ativosService from '@/services/ativos'
import { useToast } from '@/composables/useToast'

const props = defineProps({ ativoId: { type: [Number, String], required: true } })
const { mostrar } = useToast()

const MAX_ANEXO = 15 * 1024 * 1024
const EXT_PERMITIDAS = /\.(pdf|jpg|jpeg|png|docx)$/i
const EXT_IMAGEM = ['jpg', 'jpeg', 'png']

const anexos = ref([])
const carregando = ref(true)
const inputArquivo = ref(null)

const visor = ref({ open: false, url: '', nome: '' })

async function carregar() {
  carregando.value = true
  try {
    const { data } = await ativosService.anexos.listar(props.ativoId)
    anexos.value = data
  } catch {
    anexos.value = []
  } finally {
    carregando.value = false
  }
}

async function aoSelecionar(e) {
  const arquivos = Array.from(e.target.files || [])
  e.target.value = ''
  for (const f of arquivos) {
    if (!EXT_PERMITIDAS.test(f.name)) {
      mostrar(`"${f.name}" não permitido. Extensões aceitas: PDF, JPG, JPEG, PNG, DOCX.`)
      continue
    }
    if (f.size > MAX_ANEXO) { mostrar(`"${f.name}" excede 15MB.`); continue }
    const fd = new FormData()
    fd.append('arquivo', f)
    try {
      await ativosService.anexos.enviar(props.ativoId, fd)
    } catch (err) {
      if (err.response?.status === 413) mostrar(`"${f.name}" excede 15MB e não foi anexado.`)
      else mostrar(`Falha ao anexar "${f.name}".`)
    }
  }
  await carregar()
}

async function remover(id) {
  try {
    await ativosService.anexos.excluir(id)
    mostrar('Anexo removido.')
    await carregar()
  } catch {
    mostrar('Erro ao remover anexo.')
  }
}

async function abrir(a) {
  try {
    const { data } = await ativosService.anexos.buscar(a.id)
    const url = URL.createObjectURL(data)
    const ext = a.nome.split('.').pop().toLowerCase()

    if (ext === 'pdf') {
      window.open(url, '_blank')
    } else if (EXT_IMAGEM.includes(ext)) {
      visor.value = { open: true, url, nome: a.nome }
    } else {
      const link = document.createElement('a')
      link.href = url
      link.download = a.nome
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    }
  } catch {
    mostrar('Erro ao abrir anexo.')
  }
}

function fecharVisor() {
  if (visor.value.url.startsWith('blob:')) URL.revokeObjectURL(visor.value.url)
  visor.value = { open: false, url: '', nome: '' }
}

onMounted(carregar)
</script>

<template>
  <div class="p-5">
    <input ref="inputArquivo" type="file" accept=".pdf,.jpg,.jpeg,.png,.docx" multiple class="hidden" @change="aoSelecionar" />
    <BaseButton size="sm" @click="inputArquivo.click()">+ Adicionar Anexo</BaseButton>

    <EmptyState v-if="carregando" texto="Carregando..." />
    <EmptyState v-else-if="!anexos.length" texto="Nenhum anexo." />
    <div v-else class="flex flex-wrap gap-2 mt-4">
      <div
        v-for="a in anexos"
        :key="a.id"
        :title="a.nome"
        class="flex items-center gap-2 max-w-[220px] px-2.5 py-1.5 bg-surface-alt border border-borda rounded-raio-sm text-sm cursor-pointer hover:border-borda-forte"
        @click="abrir(a)"
      >
        <span class="truncate text-texto">{{ a.nome }}</span>
        <span class="text-texto-fraco hover:text-perigo-forte" @click.stop="remover(a.id)">×</span>
      </div>
    </div>

    <VisorAnexo :open="visor.open" :url="visor.url" :nome="visor.nome" @close="fecharVisor" />
  </div>
</template>