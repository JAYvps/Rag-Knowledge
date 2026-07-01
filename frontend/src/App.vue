<!-- ============ src/App.vue ============ -->
<!--
  页面过渡：纯 CSS transition，不用 GSAP 避免与子组件动画冲突
-->
<template>
  <router-view v-slot="{ Component, route }">
    <Transition name="page-fade" mode="out-in">
      <component :is="Component" :key="route.path" />
    </Transition>
  </router-view>
</template>

<script setup>
</script>

<style>
.page-fade-enter-active {
  transition: opacity 0.35s ease, transform 0.35s ease;
}

.page-fade-leave-active {
  transition: opacity 0.2s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.page-fade-leave-to {
  opacity: 0;
}
</style>
