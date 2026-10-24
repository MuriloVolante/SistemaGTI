<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Menu } from 'lucide-vue-next'
import AppSidebar from '@/components/AppSidebar.vue'
import { MODULOS } from '@/config/modulos'

const route = useRoute()
const mod = computed(() => MODULOS[route.meta.modulo] || null)
const menuAberto = ref(false)

watch(() => route.fullPath, () => { menuAberto.value = false })
</script>

<template>
  <div class="flex h-screen overflow-hidden">
    <AppSidebar v-if="mod" class="hidden md:flex" :titulo="mod.titulo" :links="mod.links" />

    <template v-if="mod && menuAberto">
      <div class="md:hidden fixed inset-0 z-[90] bg-[rgba(9,30,66,.54)]" @click="menuAberto = false"></div>
      <AppSidebar
        class="md:hidden fixed inset-y-0 left-0 z-[91] shadow-sombra-md"
        :titulo="mod.titulo"
        :links="mod.links"
        expandida
      />
    </template>

    <div class="flex-1 overflow-y-auto">
      <div v-if="mod" class="md:hidden sticky top-0 z-[20] flex items-center gap-3 bg-surface border-b border-borda px-4 py-3">
        <button
          type="button"
          class="flex items-center justify-center w-8 h-8 flex-shrink-0 border-none bg-transparent text-texto cursor-pointer rounded-raio-sm hover:bg-surface-alt"
          @click="menuAberto = true"
        >
          <Menu :size="20" />
        </button>
        <div class="text-xs font-bold uppercase tracking-wider text-texto-fraco">{{ mod.titulo }}</div>
      </div>

      <div class="max-w-none p-4 md:p-8">
        <slot />
      </div>
    </div>
  </div>
</template>