<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Camera, X } from 'lucide-vue-next'
import LayoutBlank from '@/layouts/LayoutBlank.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseField from '@/components/base/BaseField.vue'
import ativosService from '@/services/ativos'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const { mostrar } = useToast()

const codigo = ref(route.query.patrimonio || '')
const buscando = ref(false)
const cameraAberta = ref(false)
const videoRef = ref(null)

let stream = null
let detector = null
let scanTimer = null

async function buscar(valor) {
  const v = (valor ?? codigo.value).trim()
  if (!v) { mostrar('Informe o código de patrimônio.'); return }
  buscando.value = true
  try {
    const { data } = await ativosService.buscarPorPatrimonio(v)
    router.push({ name: 'ativos-detalhe', params: { id: data.id } })
  } catch (e) {
    mostrar(e.response?.data?.erro || 'Ativo não encontrado.')
  } finally {
    buscando.value = false
  }
}

async function abrirCamera() {
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
    cameraAberta.value = true
    requestAnimationFrame(() => {
      if (videoRef.value) videoRef.value.srcObject = stream
    })
    if ('BarcodeDetector' in window) {
      detector = new window.BarcodeDetector({ formats: ['qr_code'] })
      scanTimer = setInterval(escanearNativo, 400)
    } else {
      const { default: jsQR } = await import('jsqr')
      scanTimer = setInterval(() => escanearFallback(jsQR), 400)
    }
  } catch {
    mostrar('Não foi possível acessar a câmera.')
  }
}

async function escanearNativo() {
  if (!videoRef.value) return
  try {
    const codigos = await detector.detect(videoRef.value)
    if (codigos.length) processarLeitura(codigos[0].rawValue)
  } catch {}
}

function escanearFallback(jsQR) {
  if (!videoRef.value || !videoRef.value.videoWidth) return
  const canvas = document.createElement('canvas')
  canvas.width = videoRef.value.videoWidth
  canvas.height = videoRef.value.videoHeight
  const ctx = canvas.getContext('2d')
  ctx.drawImage(videoRef.value, 0, 0, canvas.width, canvas.height)
  const imagem = ctx.getImageData(0, 0, canvas.width, canvas.height)
  const resultado = jsQR(imagem.data, imagem.width, imagem.height)
  if (resultado) processarLeitura(resultado.data)
}

function processarLeitura(texto) {
  fecharCamera()
  try {
    const url = new URL(texto)
    buscar(url.searchParams.get('patrimonio') || texto)
  } catch {
    buscar(texto)
  }
}

function fecharCamera() {
  clearInterval(scanTimer)
  scanTimer = null
  if (stream) { stream.getTracks().forEach((t) => t.stop()); stream = null }
  cameraAberta.value = false
}

onBeforeUnmount(fecharCamera)

if (route.query.patrimonio) buscar(route.query.patrimonio)
</script>

<template>
  <LayoutBlank>
    <div class="w-full max-w-[360px] bg-surface border border-borda rounded-raio shadow-sombra-md p-5 md:p-8">
      <div class="text-center mb-7">
        <div class="text-4xl font-bold text-texto">GTI</div>
        <div class="text-base text-texto-sub mt-1">Consulta de ativo</div>
      </div>

      <template v-if="!cameraAberta">
        <BaseField v-model="codigo" label="Patrimônio" placeholder="Digite o código" @keyup.enter="buscar()" />
        <BaseButton variant="primary" block :disabled="buscando" @click="buscar()">
          <Search :size="16" /> Buscar
        </BaseButton>
        <BaseButton class="mt-2" block @click="abrirCamera">
          <Camera :size="16" /> Ler QR Code
        </BaseButton>
      </template>

      <template v-else>
        <video ref="videoRef" autoplay playsinline class="w-full rounded-raio-sm mb-3"></video>
        <BaseButton block @click="fecharCamera">
          <X :size="16" /> Cancelar
        </BaseButton>
      </template>
    </div>
  </LayoutBlank>
</template>