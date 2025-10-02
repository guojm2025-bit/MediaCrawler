<template>
  <div class="community-cards">
    <h3>公共牌</h3>
    <div class="cards-container">
      <div
        v-for="i in 5"
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
.community-cards {
  text-align: center;
  margin-bottom: 30px;
}

.community-cards h3 {
  color: #fff;
  margin-bottom: 15px;
  font-size: 1.5em;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.5);
}

.cards-container {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.card {
  width: 80px;
  height: 112px;
  background: #fff;
  border: 2px solid #333;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 1.2em;
  color: #333;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
  transition: transform 0.2s;
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
  font-size: 2em;
}

.card:hover {
  transform: translateY(-5px);
}

@media (max-width: 768px) {
  .card {
    width: 60px;
    height: 84px;
    font-size: 1em;
  }
}
</style>