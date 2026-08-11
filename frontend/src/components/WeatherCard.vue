<script setup>
import { computed } from 'vue'

const props = defineProps({
  weather: {
    type: Object,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
  title: {
    type: String,
    default: '부산항 현재 날씨',
  },
})

const riskMeta = {
  NORMAL: { label: '정상', tone: 'green' },
  CAUTION: { label: '주의', tone: 'amber' },
  DANGER: { label: '위험', tone: 'red' },
  UNKNOWN: { label: '정보 없음', tone: 'gray' },
}

const risk = computed(() => {
  return riskMeta[props.weather?.riskLevel] || riskMeta.UNKNOWN
})

const formatNumber = (value, unit, digits = 1) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }

  return `${Number(value).toFixed(digits)} ${unit}`
}

const formatVisibility = (value) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }

  return `${(Number(value) / 1000).toFixed(1)} km`
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <section class="panel weather-card">
    <div class="section-title weather-title">
      <h2>{{ title }}</h2>
      <span class="status-pill" :class="risk.tone">{{ risk.label }}</span>
    </div>

    <div v-if="loading" class="weather-empty">
      날씨 정보를 불러오는 중입니다.
    </div>

    <div v-else-if="error" class="weather-empty warning">
      {{ error }}
    </div>

    <div v-else-if="weather" class="weather-content">
      <div class="weather-main">
        <strong>{{ formatNumber(weather.temperature, '℃') }}</strong>
        <span>{{ weather.description || weather.weather || '-' }}</span>
      </div>

      <dl class="weather-grid">
        <div>
          <dt>강수</dt>
          <dd>{{ formatNumber(weather.rainfall, 'mm') }}</dd>
        </div>
        <div>
          <dt>풍속</dt>
          <dd>{{ formatNumber(weather.windSpeed, 'm/s') }}</dd>
        </div>
        <div>
          <dt>시정</dt>
          <dd>{{ formatVisibility(weather.visibility) }}</dd>
        </div>
        <div>
          <dt>습도</dt>
          <dd>{{ formatNumber(weather.humidity, '%', 0) }}</dd>
        </div>
      </dl>

      <p class="weather-message">{{ weather.message || '-' }}</p>
      <small>
        업데이트 {{ formatDateTime(weather.updatedAt) }}
        <template v-if="weather.stale"> · 캐시 데이터</template>
      </small>
    </div>

    <div v-else class="weather-empty">
      날씨 정보가 없습니다.
    </div>
  </section>
</template>

<style scoped>
.weather-card {
  overflow: hidden;
}

.weather-title {
  align-items: center;
}

.weather-content {
  display: grid;
  gap: 12px;
}

.weather-main {
  display: flex;
  min-width: 0;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.weather-main strong {
  color: #173b60;
  font-size: 28px;
  font-weight: 900;
}

.weather-main span {
  color: var(--ink-700);
  font-weight: 800;
  overflow-wrap: anywhere;
}

.weather-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.weather-grid div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 10px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 2px;
}

.weather-grid dt {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 900;
}

.weather-grid dd {
  margin: 0;
  color: var(--ink-900);
  font-weight: 900;
}

.weather-message {
  margin: 0;
  color: var(--ink-700);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.45;
}

.weather-content small {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.weather-empty {
  padding: 20px;
  color: var(--ink-500);
  background: #f8fbfe;
  border: 1px solid var(--line);
  font-weight: 800;
  text-align: center;
}

.weather-empty.warning {
  color: #9f1d1d;
  background: #fff4f4;
  border-color: #e4a6a6;
}

@media (max-width: 760px) {
  .weather-main {
    align-items: flex-start;
    flex-direction: column;
  }

  .weather-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .weather-grid {
    grid-template-columns: 1fr;
  }
}
</style>
