<script setup>
import { ref, computed, watch } from 'vue'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseField from '@/components/base/BaseField.vue'
import ConfirmDialog from '@/components/base/ConfirmDialog.vue'
import AutocompleteResponsavel from '@/components/AutocompleteResponsavel.vue'
import ativosService from '@/services/ativos'
import tiposService from '@/services/tipos'
import usuariosService from '@/services/usuarios'
import centrosCustoService from '@/services/centrosCusto'
import { useToast } from '@/composables/useToast'

const props = defineProps({
  open: { type: Boolean, required: true },
  ativoId: { type: Number, default: null }
})
const emit = defineEmits(['close', 'salvo'])
const { mostrar } = useToast()

const MAX_ANEXO = 15 * 1024 * 1024
const MAX_PATRIMONIO = 50
const MAX_MARCA_MODELO = 150
const MAX_CENTRO_CUSTO = 100
const MAX_CAMPO_DINAMICO = 1000
const MAX_VALOR = 9999999999.99

const tipos = ref([])
const usuarios = ref([])
const centrosCusto = ref([])
const novoCentroCustoAberto = ref(false)
const novoCentroCustoNome = ref('')
const camposDoTipo = ref([])
const valoresCampos = ref({})

const form = ref({})
const nomeResponsavel = ref('')
const autocompleteRef = ref(null)

const anexosPersistidos = ref([])
const anexosPendentes = ref([])
const inputArquivo = ref(null)

const salvando = ref(false)
const avisoValorAberto = ref(false)

const editando = computed(() => !!props.ativoId)

watch(() => props.open, async (v) => {
  if (!v) return
  await Promise.all([carregarTipos(), carregarUsuarios(), carregarCentrosCusto()])
  if (editando.value) await carregarAtivo()
  else await resetar()
})

async function resetar() {
  form.value = {
    tipoId: '', patrimonio: '', status: 'ATIVO', marcaModelo: '',
    centroCusto: '', dataCompra: '', responsavelId: null,
    garantiaAte: '', valorAquisicao: ''
  }
  nomeResponsavel.value = ''
  camposDoTipo.value = []
  valoresCampos.value = {}
  anexosPersistidos.value = []
  anexosPendentes.value = []
  novoCentroCustoAberto.value = false
  novoCentroCustoNome.value = ''
  try {
    const { data } = await ativosService.proximaMatricula()
    form.value.patrimonio = data.matricula
  } catch { /* sugestão é só um facilitador, segue sem ela */ }
}

async function carregarTipos() {
  try {
    const { data } = await tiposService.listar()
    tipos.value = data
  } catch { mostrar('Erro ao carregar tipos.') }
}

async function carregarUsuarios() {
  try {
    const { data } = await usuariosService.listarAtivos()
    usuarios.value = data
  } catch { mostrar('Erro ao carregar usuários.') }
}

async function carregarCentrosCusto() {
  try {
    const { data } = await centrosCustoService.listar()
    centrosCusto.value = data
  } catch { mostrar('Erro ao carregar centros de custo.') }
}

function aoSelecionarCentroCusto() {
  if (form.value.centroCusto === '__novo__') {
    form.value.centroCusto = ''
    novoCentroCustoAberto.value = true
  }
}

function aoDigitarPatrimonio(v) {
  if (/\D/.test(v)) mostrar('Patrimônio deve conter apenas números.')
  form.value.patrimonio = v.replace(/\D/g, '')
}

async function criarCentroCusto() {
  const nome = novoCentroCustoNome.value.trim()
  if (!nome) { mostrar('Informe o nome do centro de custo.'); return }
  if (nome.length > MAX_CENTRO_CUSTO) { mostrar('Centro de custo deve ter no maximo ' + MAX_CENTRO_CUSTO + ' caracteres.'); return }
  try {
    const { data } = await centrosCustoService.criar(novoCentroCustoNome.value.trim())
    centrosCusto.value.push(data)
    form.value.centroCusto = data.nome
    novoCentroCustoAberto.value = false
    novoCentroCustoNome.value = ''
    mostrar('Centro de custo adicionado.')
  } catch (e) {
    mostrar('Erro: ' + (e.response?.data?.erro || 'Tente novamente.'))
  }
}

async function carregarCamposDinamicos() {
  valoresCampos.value = {}
  if (!form.value.tipoId) { camposDoTipo.value = []; return }
  try {
    const { data } = await tiposService.campos.listar(form.value.tipoId)
    camposDoTipo.value = data
    data.forEach((c) => { valoresCampos.value[c.id] = c.tipoDado === 'BOOLEAN' ? false : '' })
  } catch { camposDoTipo.value = [] }
}

async function carregarAtivo() {
  try {
    const { data: a } = await ativosService.buscar(props.ativoId)
    form.value = {
      tipoId: a.tipo?.id || '',
      patrimonio: a.patrimonio || '',
      status: a.status || 'ATIVO',
      marcaModelo: a.marcaModelo || '',
      centroCusto: a.centroCusto || '',
      dataCompra: a.dataCompra || '',
      responsavelId: a.responsavel?.id || null,
      garantiaAte: a.garantiaAte || '',
      valorAquisicao: a.valorAquisicao || ''
    }
    nomeResponsavel.value = a.responsavel?.nomeCompleto || ''
    autocompleteRef.value?.definirTexto(nomeResponsavel.value)

    await carregarCamposDinamicos()

    const { data: valores } = await ativosService.valores(props.ativoId)
    valores.forEach((v) => {
      const campo = camposDoTipo.value.find((c) => c.id === v.campo.id)
      if (!campo) return
      valoresCampos.value[campo.id] = campo.tipoDado === 'BOOLEAN' ? v.valor === 'true' : v.valor
    })

    anexosPendentes.value = []
    const { data: anexos } = await ativosService.anexos.listar(props.ativoId)
    anexosPersistidos.value = anexos
  } catch {
    mostrar('Erro ao carregar ativo.')
  }
}

function aoSelecionarArquivos(e) {
  const arquivos = Array.from(e.target.files || [])
  arquivos.forEach((f) => {
    const pdf = f.type === 'application/pdf' && f.name.toLowerCase().endsWith('.pdf')
    if (!pdf) { mostrar(`"${f.name}" não é PDF.`); return }
    if (f.size === 0) { mostrar(`"${f.name}" está vazio.`); return }
    if (f.size > MAX_ANEXO) { mostrar(`"${f.name}" excede 15MB.`); return }
    anexosPendentes.value.push(f)
  })
  e.target.value = ''
}

function removerPendente(i) {
  anexosPendentes.value.splice(i, 1)
}

async function removerPersistido(id) {
  try {
    await ativosService.anexos.excluir(id)
    anexosPersistidos.value = anexosPersistidos.value.filter((a) => a.id !== id)
    mostrar('Anexo removido.')
  } catch { mostrar('Erro ao remover anexo.') }
}

function abrirPendente(f) {
  const url = URL.createObjectURL(new Blob([f], { type: 'application/pdf' }))
  const aba = window.open(url, '_blank', 'noopener,noreferrer')
  if (!aba) URL.revokeObjectURL(url)
  else setTimeout(() => URL.revokeObjectURL(url), 60000)
}

async function abrirPersistido(id) {
  try {
    const { data } = await ativosService.anexos.buscar(id)
    window.open(URL.createObjectURL(data), '_blank')
  } catch { mostrar('Erro ao abrir anexo.') }
}

function montarBody() {
  const camposDinamicos = {}
  camposDoTipo.value.forEach((c) => {
    const v = valoresCampos.value[c.id]
    if (c.tipoDado === 'BOOLEAN') camposDinamicos[c.id] = v ? 'true' : 'false'
    else if (String(v ?? '').trim()) camposDinamicos[c.id] = String(v).trim()
  })
  return { ...form.value, camposDinamicos }
}

function hoje() {
  return new Date().toISOString().slice(0, 10)
}

function salvar() {
  if (salvando.value) return

  const f = form.value
  if (!f.tipoId) { mostrar('Selecione o tipo de ativo.'); return }

  const patrimonio = (f.patrimonio || '').trim()
  if (!patrimonio) { mostrar('Patrimônio obrigatório.'); return }
  if (patrimonio.length > MAX_PATRIMONIO) { mostrar('Patrimônio deve ter no maximo ' + MAX_PATRIMONIO + ' digitos.'); return }

  const marca = (f.marcaModelo || '').trim()
  if (!marca) { mostrar('Marca/Modelo obrigatório.'); return }
  if (marca.length < 3) { mostrar('Marca/Modelo deve ter ao menos 3 caracteres.'); return }
  if (marca.length > MAX_MARCA_MODELO) { mostrar('Marca/Modelo deve ter no maximo ' + MAX_MARCA_MODELO + ' caracteres.'); return }

  if (!(f.centroCusto || '').trim()) { mostrar('Centro de custo obrigatório.'); return }

  if (!f.dataCompra) { mostrar('Data de compra obrigatória.'); return }
  if (f.dataCompra > hoje()) { mostrar('Data de compra nao pode ser futura.'); return }
  if (f.garantiaAte && f.garantiaAte < f.dataCompra) { mostrar('Garantia nao pode ser anterior a data de compra.'); return }

  if (f.valorAquisicao !== '' && f.valorAquisicao !== null && f.valorAquisicao !== undefined) {
    const valor = Number(f.valorAquisicao)
    if (!Number.isFinite(valor) || valor <= 0) { mostrar('Valor de aquisicao deve ser maior que zero.'); return }
    if (valor > MAX_VALOR) { mostrar('Valor de aquisicao excede o limite permitido.'); return }
    if (!/^\d+([.,]\d{1,2})?$/.test(String(f.valorAquisicao))) { mostrar('Valor de aquisicao deve ter no maximo 2 casas decimais.'); return }
  }

  for (const c of camposDoTipo.value) {
    const v = valoresCampos.value[c.id]
    if (c.obrigatorio && c.tipoDado !== 'BOOLEAN' && !String(v ?? '').trim()) {
      mostrar('Campo obrigatorio nao preenchido: ' + c.nomeDoCampo)
      return
    }
    if (String(v ?? '').length > MAX_CAMPO_DINAMICO) {
      mostrar('Campo "' + c.nomeDoCampo + '" deve ter no maximo ' + MAX_CAMPO_DINAMICO + ' caracteres.')
      return
    }
  }

  if (!f.valorAquisicao) { avisoValorAberto.value = true; return }
  enviar()
}

async function enviar() {
  if (salvando.value) return
  salvando.value = true
  try {
    const body = montarBody()
    let ativoId = props.ativoId
    if (editando.value) await ativosService.atualizar(props.ativoId, body)
    else {
      const { data } = await ativosService.criar(body)
      ativoId = data.id
    }

    for (const f of anexosPendentes.value) {
      const fd = new FormData()
      fd.append('arquivo', f)
      try {
        await ativosService.anexos.enviar(ativoId, fd)
      } catch (e) {
        if (e.response?.status === 413) mostrar(`"${f.name}" excede 15MB e não foi anexado.`)
        else mostrar(`Falha ao anexar "${f.name}".`)
      }
    }
    anexosPendentes.value = []

    mostrar(editando.value ? 'Ativo atualizado.' : 'Ativo criado com sucesso.')
    emit('salvo')
  } catch (e) {
    mostrar('Erro: ' + (e.response?.data?.erro || 'Tente novamente.'))
  } finally {
    salvando.value = false
  }
}
</script>

<template>
  <BaseModal :open="open" :title="editando ? 'Editar ativo' : 'Novo ativo'" max-width="max-w-[620px]" @close="emit('close')">
    <div class="mb-4">
      <label class="field-label">Tipo de ativo *</label>
      <select v-model="form.tipoId" class="field-input" @change="carregarCamposDinamicos">
        <option value="">Selecione...</option>
        <option v-for="t in tipos" :key="t.id" :value="t.id">{{ t.nome }}</option>
      </select>
    </div>

    <div class="grid grid-cols-2 gap-3">
      <BaseField
        :model-value="form.patrimonio"
        @update:model-value="aoDigitarPatrimonio"
        label="Patrimônio *"
        placeholder="ex: 1"
        inputmode="numeric"
        :maxlength="MAX_PATRIMONIO"
      />
      <div class="mb-4">
        <label class="field-label">Status *</label>
        <select v-model="form.status" class="field-input">
          <option value="ATIVO">Ativo</option>
          <option value="ESTOQUE">Estoque</option>
          <option value="MANUTENCAO">Manutenção</option>
          <option value="DESCARTADO">Descartado</option>
        </select>
      </div>
    </div>

    <BaseField v-model="form.marcaModelo" label="Marca / Modelo *" placeholder="ex: Dell Latitude 5420" :maxlength="MAX_MARCA_MODELO" />

    <div class="mb-4">
      <label class="field-label">Centro de custo *</label>
      <select v-model="form.centroCusto" class="field-input" @change="aoSelecionarCentroCusto">
        <option value="">Selecione...</option>
        <option v-for="c in centrosCusto" :key="c.id" :value="c.nome">{{ c.nome }}</option>
        <option value="__novo__">+ Adicionar novo...</option>
      </select>
      <div v-if="novoCentroCustoAberto" class="flex gap-2 mt-2">
        <input
          v-model="novoCentroCustoNome"
          class="field-input"
          :maxlength="MAX_CENTRO_CUSTO"
          placeholder="Nome do novo centro de custo"
          @keyup.enter="criarCentroCusto"
        />
        <BaseButton size="sm" @click="criarCentroCusto">Adicionar</BaseButton>
      </div>
    </div>

    <BaseField v-model="form.dataCompra" label="Data de compra *" type="date" :max="hoje()" />

    <div class="mb-4">
      <label class="field-label">Responsável</label>
      <AutocompleteResponsavel
        ref="autocompleteRef"
        v-model="form.responsavelId"
        :nome-inicial="nomeResponsavel"
        :usuarios="usuarios"
      />
    </div>

    <hr class="border-borda my-5" />
    <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-3">Campos opcionais</div>

    <BaseField v-model="form.garantiaAte" label="Garantia até" type="date" />
    <BaseField v-model="form.valorAquisicao" label="Valor de aquisição (R$)" type="number" min="0" step="0.01" placeholder="ex: 3500.00" />

    <template v-if="camposDoTipo.length">
      <hr class="border-borda my-5" />
      <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-3">Campos do tipo</div>

      <div v-for="c in camposDoTipo" :key="c.id" class="mb-4">
        <label v-if="c.tipoDado === 'BOOLEAN'" class="flex items-center gap-1.5 text-base text-texto cursor-pointer">
          <input v-model="valoresCampos[c.id]" type="checkbox" />
          {{ c.nomeDoCampo }}
        </label>
        <template v-else>
          <label class="field-label">{{ c.nomeDoCampo }} {{ c.obrigatorio ? '*' : '' }}</label>
          <input
            v-model="valoresCampos[c.id]"
            :type="c.tipoDado === 'DATE' ? 'date' : c.tipoDado === 'INT' ? 'number' : 'text'"
            :maxlength="c.tipoDado === 'TEXT' || !c.tipoDado ? MAX_CAMPO_DINAMICO : undefined"
            :placeholder="c.nomeDoCampo"
            class="field-input"
          />
        </template>
      </div>
    </template>

    <hr class="border-borda my-5" />
    <div class="text-xs font-semibold uppercase tracking-wide text-texto-sub mb-3">Anexos (PDF)</div>

    <input ref="inputArquivo" type="file" accept="application/pdf" multiple class="hidden" @change="aoSelecionarArquivos" />
    <BaseButton size="sm" @click="inputArquivo.click()">+ Adicionar Anexo</BaseButton>

    <div class="flex flex-wrap gap-2 mt-3">
      <div
        v-for="a in anexosPersistidos"
        :key="'p' + a.id"
        class="flex items-center gap-2 max-w-[220px] px-2.5 py-1.5 bg-surface-alt border border-borda rounded-raio-sm text-sm cursor-pointer hover:border-borda-forte"
        @click="abrirPersistido(a.id)"
      >
        <span class="truncate text-texto">{{ a.nome }}</span>
        <span class="text-texto-fraco hover:text-perigo-forte" @click.stop="removerPersistido(a.id)">×</span>
      </div>

      <div
        v-for="(f, i) in anexosPendentes"
        :key="'n' + i"
        class="flex items-center gap-2 max-w-[220px] px-2.5 py-1.5 bg-primaria-light border border-primaria rounded-raio-sm text-sm cursor-pointer"
        @click="abrirPendente(f)"
      >
        <span class="truncate text-primaria-text">{{ f.name }}</span>
        <span class="text-primaria-text hover:text-perigo-forte" @click.stop="removerPendente(i)">×</span>
      </div>
    </div>

    <template #footer>
      <BaseButton @click="emit('close')">Cancelar</BaseButton>
      <BaseButton variant="primary" :disabled="salvando" @click="salvar">
        {{ salvando ? 'Salvando...' : (editando ? 'Salvar' : 'Criar') }}
      </BaseButton>
    </template>
  </BaseModal>

  <ConfirmDialog
    :open="avisoValorAberto"
    titulo="Ativo sem valor de aquisição"
    texto="Sem o valor de aquisição não é possível calcular a depreciação deste ativo. Você pode continuar sem valor ou informá-lo agora."
    confirmar-label="Informar valor"
    @confirm="avisoValorAberto = false"
    @cancel="avisoValorAberto = false; enviar()"
  />
</template>