<script setup>
defineProps({ colunas: { type: Array, required: true } })
</script>

<template>
  <table class="w-full border-collapse text-base">
    <thead>
      <tr>
        <th
          v-for="col in colunas"
          :key="col.key"
          :style="col.width ? { width: col.width } : {}"
          class="px-4 py-[9px] text-left text-xs font-semibold uppercase tracking-wide text-texto-sub bg-surface-alt border-b border-borda"
          :class="col.align === 'right' && 'text-right'"
        >
          {{ col.label }}
        </th>
      </tr>
    </thead>
    <tbody>
      <slot />
    </tbody>
  </table>
</template>