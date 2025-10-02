<template>
  <div class="action-buttons">
    <button 
      :disabled="!isMyTurn" 
      class="action-btn fold"
      @click="handleAction('fold')"
    >
      弃牌
    </button>
    
    <button 
      :disabled="!isMyTurn" 
      class="action-btn check"
      @click="handleAction('check')"
    >
      看牌
    </button>
    
    <button 
      :disabled="!isMyTurn" 
      class="action-btn call"
      @click="handleAction('call')"
    >
      跟注
    </button>
    
    <button 
      :disabled="!isMyTurn" 
      class="action-btn raise"
      @click="handleRaise"
    >
      加注
    </button>
    
    <button 
      :disabled="!isMyTurn" 
      class="action-btn allin"
      @click="handleAction('allin')"
    >
      全下
    </button>
    
    <div class="bet-input">
      <input 
        v-model.number="betAmount"
        type="number" 
        :disabled="!isMyTurn"
        :min="currentBet + 1"
        placeholder="下注金额"
      >
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  isMyTurn: {
    type: Boolean,
    default: false
  },
  currentBet: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['action'])

const betAmount = ref(0)

const handleAction = (actionType) => {
  emit('action', { type: actionType })
}

const handleRaise = () => {
  if (betAmount.value <= 0) {
    alert('请输入有效的加注金额')
    return
  }
  emit('action', { type: 'raise', amount: betAmount.value })
  betAmount.value = 0
}
</script>

<style scoped>
.action-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
  margin: 30px 0;
}

.action-btn {
  padding: 12px 20px;
  border: none;
  border-radius: 25px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 1em;
  min-width: 80px;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
}

.action-btn:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.fold { 
  background: #f44336; 
  color: white; 
}

.fold:not(:disabled):hover { 
  background: #d32f2f; 
}

.check { 
  background: #2196F3; 
  color: white; 
}

.check:not(:disabled):hover { 
  background: #1976d2; 
}

.call { 
  background: #4CAF50; 
  color: white; 
}

.call:not(:disabled):hover { 
  background: #388e3c; 
}

.raise { 
  background: #FF9800; 
  color: white; 
}

.raise:not(:disabled):hover { 
  background: #f57c00; 
}

.allin { 
  background: #9C27B0; 
  color: white; 
}

.allin:not(:disabled):hover { 
  background: #7b1fa2; 
}

.bet-input {
  display: flex;
  align-items: center;
}

.bet-input input {
  padding: 12px 15px;
  border: 2px solid #ddd;
  border-radius: 25px;
  font-size: 1em;
  width: 150px;
  text-align: center;
}

.bet-input input:disabled {
  background: #f5f5f5;
  opacity: 0.5;
}

.bet-input input:focus {
  outline: none;
  border-color: #2196F3;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.2);
}

@media (max-width: 768px) {
  .action-buttons {
    gap: 8px;
  }
  
  .action-btn {
    padding: 10px 16px;
    font-size: 0.9em;
    min-width: 70px;
  }
  
  .bet-input input {
    width: 120px;
    padding: 10px 12px;
  }
}
</style>