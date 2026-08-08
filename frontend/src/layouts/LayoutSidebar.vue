<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import { MODULOS } from '@/config/modulos'

const route = useRoute()
const mod = computed(() => MODULOS[route.meta.modulo] || null)
</script>

<template>
  <div class="flex h-screen overflow-hidden">
    <AppSidebar v-if="mod" :titulo="mod.titulo" :links="mod.links" />
    <div class="flex-1 overflow-y-auto">
      <div class="max-w-none p-8">
        <slot />
      </div>
    </div>
  </div>
</template>