import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUIStore = defineStore('ui', () => {
  // Toast消息状态
  const toastMessage = ref('')
  const toastType = ref('info')
  const showToast = ref(false)
  
  // 显示Toast消息
  const showToastMessage = (message, type = 'info', duration = 3000) => {
    toastMessage.value = message
    toastType.value = type
    showToast.value = true
    
    // 自动隐藏
    setTimeout(() => {
      showToast.value = false
    }, duration)
  }
  
  // 隐藏Toast消息
  const hideToast = () => {
    showToast.value = false
  }
  
  return {
    // 状态
    toastMessage,
    toastType,
    showToast,
    
    // 方法
    showToast: showToastMessage,
    hideToast
  }
})