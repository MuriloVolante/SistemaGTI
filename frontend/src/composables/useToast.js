import { ref } from 'vue'

const mensagem = ref('')
const visivel = ref(false)
let timer = null

export function useToast() {
  function mostrar(texto, duracao = 2500) {
    mensagem.value = texto
    visivel.value = true
    clearTimeout(timer)
    timer = setTimeout(() => { visivel.value = false }, duracao)
  }
  return { mensagem, visivel, mostrar }
}