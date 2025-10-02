<template>
  <Transition name="toast">
    <div 
      v-if="show"
      :class="['message-toast', type]"
    >
      {{ message }}
    </div>
  </Transition>
</template>

<script setup>
defineProps({
  message: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'info',
    validator: (value) => ['info', 'success', 'warning', 'error'].includes(value)
  },
  show: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.message-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 15px 20px;
  border-radius: 5px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  z-index: 1000;
  max-width: 300px;
  color: white;
  font-weight: 500;
}

.message-toast.info {
  background: rgba(33, 150, 243, 0.9);
}

.message-toast.success {
  background: rgba(76, 175, 80, 0.9);
}

.message-toast.warning {
  background: rgba(255, 152, 0, 0.9);
}

.message-toast.error {
  background: rgba(244, 67, 54, 0.9);
}

/* 过渡动画 */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  transform: translateX(400px);
  opacity: 0;
}

.toast-leave-to {
  transform: translateX(400px);
  opacity: 0;
}

@media (max-width: 768px) {
  .message-toast {
    top: 10px;
    right: 10px;
    left: 10px;
    max-width: none;
  }
  
  .toast-enter-from,
  .toast-leave-to {
    transform: translateY(-100px);
  }
}
</style>