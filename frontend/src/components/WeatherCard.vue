<script setup>
import { computed, ref } from 'vue'

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
    default: '부산항 날씨',
  },
  mode: {
    type: String,
    default: 'operator',
  },
})

const riskMeta = {
  NORMAL: {
    label: '작업 가능',
    shortLabel: '정상',
    tone: 'green',
    icon: 'sun',
    guide: '일반 작업 진행 가능',
  },
  CAUTION: {
    label: '주의 필요',
    shortLabel: '주의',
    tone: 'amber',
    icon: 'wind',
    guide: '현장 조건 확인 필요',
  },
  DANGER: {
    label: '관리자 확인',
    shortLabel: '위험',
    tone: 'red',
    icon: 'alert',
    guide: '작업 전 운영 판단 필요',
  },
  UNKNOWN: {
    label: '정보 없음',
    shortLabel: '정보 없음',
    tone: 'gray',
    icon: 'cloud',
    guide: '현장 공지 우선 확인',
  },
}

const criteriaRows = [
  { label: '정상', tone: 'green', text: '풍속 10m/s 미만 · 강수 5mm 미만 · 시정 5km 초과' },
  { label: '주의', tone: 'amber', text: '풍속 10m/s 이상 · 강수 5mm 이상 · 시정 5km 이하' },
  { label: '위험', tone: 'red', text: '풍속 15m/s 이상 · 강수 15mm 이상 · 시정 1km 이하' },
]

const mapExpanded = ref(false)
const risk = computed(() => riskMeta[props.weather?.riskLevel] || riskMeta.UNKNOWN)
const forecasts = computed(() => props.weather?.forecasts || [])
const isDriverMode = computed(() => props.mode === 'driver')
const isCarrierMode = computed(() => props.mode === 'carrier')
const isMapMode = computed(() => props.mode === 'map')
const isCompactMode = computed(() => ['driver', 'carrier', 'map'].includes(props.mode))
const isMapNormalCollapsed = computed(() => isMapMode.value && props.weather?.riskLevel === 'NORMAL' && !mapExpanded.value)

const weatherIcon = computed(() => {
  const value = String(props.weather?.weather || '').toLowerCase()
  if (props.weather?.riskLevel === 'DANGER') return 'alert'
  if (props.weather?.riskLevel === 'CAUTION') return 'wind'
  if (value.includes('rain')) return 'rain'
  if (value.includes('cloud')) return 'cloud'
  return risk.value.icon
})

const headline = computed(() => {
  if (!props.weather?.available) return '날씨 정보를 확인할 수 없습니다.'
  if (isDriverMode.value) {
    if (props.weather.riskLevel === 'DANGER') return '입차 전 관제 확인이 필요합니다.'
    if (props.weather.riskLevel === 'CAUTION') return '현장 주의사항을 먼저 확인하세요.'
    return '현재 작업지 기상은 작업 가능 상태입니다.'
  }
  if (isCarrierMode.value) {
    if (props.weather.riskLevel === 'DANGER') return '배차 지연 위험이 있습니다.'
    if (props.weather.riskLevel === 'CAUTION') return '일부 배차 지연 가능성이 있습니다.'
    return '현재 기상 기준으로 배차 진행 가능합니다.'
  }
  return props.weather?.guideMessage || props.weather?.description || props.weather?.weather || '-'
})

const roleGuide = computed(() => {
  if (!props.weather?.available) return props.weather?.guideMessage || '현장 공지와 관리자 안내를 먼저 확인하세요.'
  if (isDriverMode.value) {
    if (props.weather.riskLevel === 'DANGER') return '입차, 작업 시작 전 관리자 또는 관제 안내를 확인하세요.'
    if (props.weather.riskLevel === 'CAUTION') return '강풍, 강수, 저시정 여부를 확인하고 이동 여유 시간을 확보하세요.'
    return '현재 기준으로 일반 작업 진행이 가능합니다.'
  }
  if (isCarrierMode.value) {
    if (props.weather.riskLevel === 'DANGER') return '배차 순서와 현장 공지를 다시 확인하세요.'
    if (props.weather.riskLevel === 'CAUTION') return '출발 간격과 도착 시간을 여유 있게 조정하세요.'
    return '배차 지연 요인은 크지 않습니다.'
  }
  return props.weather?.guideMessage || risk.value.guide
})

const impactLabel = computed(() => {
  if (props.weather?.riskLevel === 'DANGER') return '운영 위험'
  if (props.weather?.riskLevel === 'CAUTION') return '주의 필요'
  if (props.weather?.riskLevel === 'NORMAL') return '진행 가능'
  return '확인 필요'
})

const infoMessage = computed(() => {
  if (props.error) return props.error
  if (!props.weather?.available) return props.weather?.errorMessage || props.weather?.message || '부산항 날씨 정보를 확인할 수 없습니다.'
  if (props.weather?.fallbackUsed) return props.weather?.errorMessage || '실시간 호출에 실패해 마지막 정상 데이터를 표시합니다.'
  return props.weather?.message || '-'
})

const formatNumber = (value, unit, digits = 1) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return `${Number(value).toFixed(digits)} ${unit}`
}

const formatVisibility = (value) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return `${(Number(value) / 1000).toFixed(1)} km`
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

const formatTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(11, 16)
}
</script>

<template>
  <section class="weather-card" :class="[mode, risk.tone, { expanded: mapExpanded }]">
    <div class="weather-hero">
      <span class="weather-icon" :class="weatherIcon" aria-hidden="true">
        <i></i>
      </span>

      <div class="weather-summary">
        <span class="weather-kicker">{{ weather?.sourceLabel || title }}</span>
        <strong>{{ headline }}</strong>
        <small>{{ roleGuide }}</small>
      </div>

      <span class="status-pill weather-risk" :class="risk.tone">
        {{ isDriverMode ? risk.label : risk.shortLabel }}
      </span>
    </div>

    <div v-if="loading" class="weather-empty">
      부산항 날씨 정보를 불러오는 중입니다.
    </div>

    <div v-else-if="error" class="weather-empty warning">
      {{ error }}
    </div>

    <div v-else-if="weather" class="weather-body">
      <div class="weather-current">
        <div>
          <span>현재 기온</span>
          <strong>{{ formatNumber(weather.temperature, '℃') }}</strong>
        </div>
        <p>
          <b v-if="isCarrierMode || isDriverMode">{{ impactLabel }}</b>
          {{ infoMessage }}
        </p>
      </div>

      <div v-if="isCompactMode" class="weather-inline">
        <span>{{ formatNumber(weather.temperature, '℃') }}</span>
        <span>강수 {{ formatNumber(weather.rainfall, 'mm') }}</span>
        <span>풍속 {{ formatNumber(weather.windSpeed, 'm/s') }}</span>
        <span>시정 {{ formatVisibility(weather.visibility) }}</span>
        <button v-if="isMapMode" type="button" class="weather-detail-toggle" @click="mapExpanded = !mapExpanded">
          {{ mapExpanded ? '접기' : '상세' }}
        </button>
      </div>

      <div class="weather-guide" :class="risk.tone">
        <strong>{{ impactLabel }}</strong>
        <p>{{ roleGuide }}</p>
      </div>

      <details v-if="(isDriverMode || isCarrierMode) && forecasts.length" class="forecast-panel">
        <summary>
          <span>{{ isCarrierMode ? '배차 참고 예보 보기' : '작업 예보 보기' }}</span>
          <small>3시간 단위</small>
        </summary>
        <div class="weather-subhead">
          <strong>앞으로의 기상 예보</strong>
          <small>최대 5개 구간</small>
        </div>
        <div class="forecast-strip" aria-label="시간별 예보">
          <article
            v-for="item in forecasts"
            :key="item.forecastAt"
            class="forecast-item"
            :class="riskMeta[item.riskLevel]?.tone || 'gray'"
          >
            <time>{{ formatTime(item.forecastAt) }}</time>
            <strong>{{ riskMeta[item.riskLevel]?.shortLabel || '정보 없음' }}</strong>
            <span>{{ formatNumber(item.temperature, '℃') }}</span>
            <small>풍속 {{ formatNumber(item.windSpeed, 'm/s') }}</small>
          </article>
        </div>
      </details>

      <details v-if="(isDriverMode || isMapMode) && !isMapNormalCollapsed" class="criteria-panel">
        <summary>
          <span>위험도 기준 보기</span>
          <small>정상 / 주의 / 위험</small>
        </summary>
        <div class="weather-subhead">
          <strong>부산항 기상 판단 기준</strong>
          <small>가장 높은 단계 우선 적용</small>
        </div>
        <div class="criteria-grid">
          <article
            v-for="row in criteriaRows"
            :key="row.label"
            class="criteria-item"
            :class="row.tone"
          >
            <b>{{ row.label }}</b>
            <span>{{ row.text }}</span>
          </article>
        </div>
      </details>

      <small v-if="!isMapNormalCollapsed" class="weather-updated">
        업데이트 {{ formatDateTime(weather.updatedAt) }}
        <template v-if="weather.stale"> · 마지막 정상 데이터</template>
      </small>
    </div>

    <div v-else class="weather-empty">
      부산항 날씨 정보가 없습니다.
    </div>
  </section>
</template>

<style scoped>
.weather-card {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 12px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-left: 5px solid var(--blue-700);
  border-radius: 2px;
}

.weather-card.green { border-left-color: var(--green-600); }
.weather-card.amber { border-left-color: var(--amber-500); }
.weather-card.red { border-left-color: var(--red-500); }
.weather-card.gray { border-left-color: #7b8794; }

.weather-hero {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.weather-icon {
  position: relative;
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  background: #eaf2f9;
  border: 1px solid #9eb8d0;
  border-radius: 50%;
}

.green .weather-icon { background: #e5f7ef; border-color: #9fd6ba; }
.amber .weather-icon { background: #fff5dc; border-color: #efd28f; }
.red .weather-icon { background: #ffeceb; border-color: #f4bdb9; }

.weather-icon i { position: relative; display: block; }
.weather-icon.sun i {
  width: 20px;
  height: 20px;
  background: #d28b16;
  border-radius: 50%;
  box-shadow: 0 -13px 0 -8px #d28b16, 0 13px 0 -8px #d28b16, 13px 0 0 -8px #d28b16, -13px 0 0 -8px #d28b16;
}

.weather-icon.cloud i {
  width: 28px;
  height: 14px;
  background: #6c7d8d;
  border-radius: 999px;
}

.weather-icon.cloud i::before {
  position: absolute;
  left: 5px;
  top: -8px;
  width: 13px;
  height: 13px;
  background: #6c7d8d;
  border-radius: 50%;
  content: '';
}

.weather-icon.rain i {
  width: 28px;
  height: 13px;
  background: #23639c;
  border-radius: 999px;
}

.weather-icon.rain i::after {
  position: absolute;
  left: 5px;
  top: 17px;
  width: 4px;
  height: 8px;
  background: #23639c;
  box-shadow: 9px 0 #23639c, 18px 0 #23639c;
  transform: skew(-18deg);
  content: '';
}

.weather-icon.wind i {
  width: 28px;
  height: 3px;
  background: #b47c1c;
  border-radius: 999px;
  box-shadow: 0 9px #b47c1c, -7px 18px #b47c1c;
}

.weather-icon.alert i {
  width: 0;
  height: 0;
  border-right: 13px solid transparent;
  border-bottom: 25px solid #b8403a;
  border-left: 13px solid transparent;
}

.weather-icon.alert i::after {
  position: absolute;
  left: -1px;
  top: 8px;
  width: 3px;
  height: 9px;
  background: #ffffff;
  box-shadow: 0 12px 0 -1px #ffffff;
  content: '';
}

.weather-summary { display: grid; gap: 3px; min-width: 0; }
.weather-kicker { color: var(--ink-500); font-size: 12px; font-weight: 900; }
.weather-summary strong { color: var(--ink-900); font-size: 16px; font-weight: 900; overflow-wrap: anywhere; }
.weather-summary small { color: var(--ink-500); font-size: 11px; font-weight: 800; }
.weather-risk { min-width: 78px; min-height: 28px; font-size: 13px; }

.weather-body { display: grid; gap: 12px; }
.weather-current {
  display: grid;
  grid-template-columns: minmax(120px, 0.2fr) minmax(0, 1fr);
  gap: 8px;
  align-items: stretch;
}

.weather-current div, .weather-current p {
  margin: 0;
  padding: 9px 10px;
  background: #f6f9fd;
  border: 1px solid var(--line);
}

.weather-current div { display: grid; gap: 4px; }
.weather-current span { color: var(--ink-500); font-size: 12px; font-weight: 900; }
.weather-current strong { color: #173b60; font-size: 22px; font-weight: 900; }
.weather-current p { color: var(--ink-700); font-size: 12px; font-weight: 800; line-height: 1.45; }
.weather-current p b { display: block; margin-bottom: 3px; color: var(--ink-900); font-size: 13px; font-weight: 900; }

.weather-inline { display: flex; flex-wrap: wrap; gap: 6px; min-width: 0; }
.weather-inline span {
  display: inline-flex;
  min-height: 28px;
  align-items: center;
  padding: 4px 8px;
  color: var(--ink-700);
  background: #f6f9fd;
  border: 1px solid #c7d1dc;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.weather-inline span:first-child { color: #173b60; font-size: 14px; }

.weather-guide {
  display: grid;
  gap: 4px;
  padding: 9px 10px;
  background: #f8fafc;
  border: 1px solid #c7d1dc;
}

.weather-guide strong { color: var(--ink-900); font-size: 12px; font-weight: 900; }
.weather-guide p { margin: 0; color: var(--ink-700); font-size: 12px; font-weight: 800; line-height: 1.45; }
.weather-guide.amber { background: #fff9ed; }
.weather-guide.red { background: #fff4f4; }

.weather-detail-toggle {
  min-height: 28px;
  padding: 4px 8px;
  color: #173b60;
  background: #ffffff;
  border: 1px solid #9eb8d0;
  font-size: 12px;
  font-weight: 900;
}

.weather-subhead {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.weather-subhead strong { color: var(--ink-900); font-size: 14px; font-weight: 900; }
.weather-subhead small { color: var(--ink-500); font-size: 12px; font-weight: 800; }

.forecast-panel, .criteria-panel { display: grid; gap: 8px; padding-top: 2px; }

.forecast-panel summary, .criteria-panel summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 32px;
  padding: 6px 9px;
  color: var(--ink-700);
  background: #f8fafc;
  border: 1px solid #c7d1dc;
  cursor: pointer;
  list-style: none;
}

.forecast-panel summary::-webkit-details-marker,
.criteria-panel summary::-webkit-details-marker { display: none; }

.forecast-panel summary::after,
.criteria-panel summary::after { color: var(--blue-700); font-size: 14px; font-weight: 900; content: '+'; }

.forecast-panel[open] summary::after,
.criteria-panel[open] summary::after { content: '-'; }

.forecast-panel summary span,
.criteria-panel summary span { font-size: 12px; font-weight: 900; }

.forecast-panel summary small,
.criteria-panel summary small { color: var(--ink-500); font-size: 11px; font-weight: 800; }

.forecast-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
}

.forecast-item {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 7px 8px;
  background: #f8fafc;
  border: 1px solid #c7d1dc;
  border-top: 4px solid #7b8794;
}

.forecast-item.green { border-top-color: var(--green-600); }
.forecast-item.amber { border-top-color: var(--amber-500); }
.forecast-item.red { border-top-color: var(--red-500); }
.forecast-item time, .forecast-item small { color: var(--ink-500); font-size: 11px; font-weight: 800; }
.forecast-item strong, .forecast-item span { color: var(--ink-900); font-size: 13px; font-weight: 900; }

.criteria-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.criteria-item {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 7px 8px;
  background: #f8fafc;
  border: 1px solid #c7d1dc;
  border-left: 4px solid #7b8794;
}

.criteria-item.green { border-left-color: var(--green-600); }
.criteria-item.amber { border-left-color: var(--amber-500); }
.criteria-item.red { border-left-color: var(--red-500); }
.criteria-item b { color: var(--ink-900); font-size: 12px; font-weight: 900; }
.criteria-item span { color: var(--ink-500); font-size: 11px; font-weight: 800; line-height: 1.35; }
.weather-updated { color: var(--ink-500); font-size: 12px; font-weight: 800; }

.driver.weather-card, .carrier.weather-card { gap: 8px; padding: 10px 12px; }
.driver .weather-body, .carrier .weather-body { gap: 8px; }
.driver .weather-summary small, .carrier .weather-summary small { display: none; }
.driver .weather-current, .carrier .weather-current, .map .weather-current { display: none; }

.weather-empty {
  padding: 16px;
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

.map {
  position: absolute;
  right: 12px;
  bottom: 34px;
  z-index: 520;
  width: min(330px, calc(100% - 24px));
  box-shadow: 0 2px 10px #1f293326;
}

.map.green:not(.expanded) {
  width: min(300px, calc(100% - 24px));
  gap: 6px;
  padding: 8px 10px;
}

.map.green:not(.expanded) .weather-hero {
  grid-template-columns: 30px minmax(0, 1fr) auto;
  gap: 7px;
}

.map.green:not(.expanded) .weather-icon {
  width: 30px;
  height: 30px;
}

.map.green:not(.expanded) .weather-summary strong,
.map.green:not(.expanded) .weather-summary small,
.map.green:not(.expanded) .weather-guide,
.map.green:not(.expanded) .weather-updated {
  display: none;
}

.map.green:not(.expanded) .weather-risk {
  min-width: 56px;
  min-height: 24px;
  font-size: 12px;
}

.map.green:not(.expanded) .weather-inline { gap: 4px; }
.map.green:not(.expanded) .weather-inline span {
  min-height: 24px;
  padding: 3px 6px;
  font-size: 11px;
}

.map .criteria-grid { grid-template-columns: 1fr; }

@media (max-width: 760px) {
  .weather-hero,
  .criteria-grid {
    grid-template-columns: 1fr;
  }

  .weather-risk { width: fit-content; }
  .forecast-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .map { position: static; width: auto; margin: 8px; }
}

@media (max-width: 480px) {
  .forecast-strip,
  .criteria-grid {
    grid-template-columns: 1fr;
  }
}
</style>
