<script setup>
import { RouterView, useRoute } from 'vue-router'
import AppShell from './components/layout/AppShell.vue'

const route = useRoute()
</script>

<template>
  <AppShell v-if="route.meta.shell !== false">
    <RouterView />
  </AppShell>
  <RouterView v-else />
</template>
