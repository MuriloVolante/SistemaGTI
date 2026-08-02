<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import UserTopbar from '@/components/UserTopbar.vue'

defineProps({
  abas: { type: Array, required: true },
  ativa: { type: String, required: true }
})
const emit = defineEmits(['change'])

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div>
    <UserTopbar :abas="abas" :ativa="ativa" @change="emit('change', $event)" @logout="logout" />
    <div class="max-w-[720px] mx-auto p-8">
      <slot />
    </div>
  </div>
</template>