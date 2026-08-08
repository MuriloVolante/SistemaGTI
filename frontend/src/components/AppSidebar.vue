<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronRight, ChevronLeft, ArrowLeft, Settings } from 'lucide-vue-next'
import InfoIcon from '@/components/base/InfoIcon.vue'
import { useAuthStore } from '@/stores/auth'

defineProps({
  titulo: { type: String, required: true },
  links: { type: Array, required: true } // [{ href, label, iconComponent, tip }]
})

const route = useRoute()
const auth = useAuthStore()
const colapsada = ref(true)

const mostrarConfiguracoes = computed(
  () => auth.isTI && route.meta.modulo !== 'configuracoes'
)
</script>

<template>
  <aside
    class="flex-shrink-0 bg-surface border-r border-borda flex flex-col py-5 h-screen overflow-y-auto transition-[width] duration-[180ms]"
    :class="colapsada ? 'w-14' : 'w-[220px]'"
  >
    <div class="flex items-center px-5 mb-3" :class="colapsada ? 'justify-center px-0' : 'justify-between'">
      <div v-if="!colapsada" class="text-xs font-bold uppercase tracking-wider text-texto-fraco">{{ titulo }}</div>
      <button
        type="button"
        class="flex items-center justify-center w-6 h-6 flex-shrink-0 border-none bg-transparent text-texto-sub cursor-pointer rounded-raio-sm hover:bg-surface-alt hover:text-texto"
        @click="colapsada = !colapsada"
      >
        <component :is="colapsada ? ChevronRight : ChevronLeft" :size="16" />
      </button>
    </div>

    <nav class="flex-1 flex flex-col gap-px">
      <router-link
        v-for="l in links"
        :key="l.href"
        :to="l.href"
        class="flex items-center gap-2.5 text-base font-medium text-texto no-underline border-l-[3px] border-transparent transition-colors hover:bg-surface-alt"
        :class="[
          colapsada ? 'justify-center gap-0 py-2 px-0' : 'py-2 px-5',
          route.path === l.href && 'bg-primaria-light text-primaria-text border-primaria font-semibold'
        ]"
      >
        <component :is="l.iconComponent" :size="16" class="flex-shrink-0" />
        <span v-if="!colapsada" class="flex-1">{{ l.label }}</span>
        <InfoIcon v-if="l.tip && !colapsada" :texto="l.tip" />
      </router-link>
    </nav>

    <div class="pt-4 border-t border-borda mt-auto sticky bottom-0 bg-surface">
          <router-link
            v-if="mostrarConfiguracoes"
            to="/configuracoes/integracao"
            class="flex items-center gap-2.5 text-base font-medium text-texto no-underline hover:bg-surface-alt"
            :class="colapsada ? 'justify-center gap-0 py-2 px-0' : 'py-2 px-5'"
          >
            <Settings :size="16" class="flex-shrink-0" />
            <span v-if="!colapsada">Configurações</span>
          </router-link>

          <router-link
            to="/home"
            class="flex items-center gap-2.5 text-base font-medium text-texto no-underline hover:bg-surface-alt"
            :class="colapsada ? 'justify-center gap-0 py-2 px-0' : 'py-2 px-5'"
          >
            <ArrowLeft :size="16" class="flex-shrink-0" />
            <span v-if="!colapsada">Voltar ao início</span>
          </router-link>
        </div>
  </aside>
</template>