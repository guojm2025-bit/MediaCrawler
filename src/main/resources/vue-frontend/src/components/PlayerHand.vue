<template>
  <div class="player-hand">
    <h3>你的手牌</h3>
    <div class="cards-container">
      <div
        v-for="i in 2"
        :key="i"
        :class="[
          'card',
          i <= cards.length ? getCardClass(cards[i-1]) : 'placeholder'
        ]"
      >
        {{ i <= cards.length ? cards[i-1].display : '?' }}
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  cards: {
    type: Array,
    default: () => []
  }
})

const getCardClass = (card) => {
  if (!card) return 'placeholder'
  return card.suitName === '红桃' || card.suitName === '方块' ? 'red' : 'black'
}
</script>

<style scoped>
.player-hand {
  text-align: center;
  margin: 30px 0;
}

.player-hand h3 {
  color: #fff;
  margin-bottom: 15px;
  font-size: 1.5em;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.5);
}

.cards-container {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.card {
  width: 100px;
  height: 140px;
  background: #fff;
  border: 3px solid #333;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 1.4em;
  color: #333;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.4);
  transition: all 0.3s ease;
  position: relative;
}

.card.red {
  color: #d32f2f;
}

.card.black {
  color: #333;
}

.card.placeholder {
  background: #666;
  color: #999;
  font-size: 2.5em;
}

.card:hover {
  transform: translateY(-10px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.5);
}

@media (max-width: 768px) {
  .card {
    width: 80px;
    height: 112px;
    font-size: 1.2em;
  }
}
</style>