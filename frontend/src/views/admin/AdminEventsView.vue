<script setup>
import { events, exceptions } from '../../data/mockData'
</script>

<template>
  <div class="page-stack">

    <section class="grid-2">
      <article class="panel">
        <div class="section-title">
          <h2>작업 이벤트</h2>
          <span class="status-pill">{{ events.length }}건</span>
        </div>
        <div class="timeline">
          <div v-for="event in events" :key="event.time + event.type" class="timeline-row">
            <time>{{ event.time }}</time>
            <div>
              <b>{{ event.type }} · {{ event.target }}</b>
              <span>{{ event.message }}</span>
            </div>
          </div>
        </div>
      </article>

      <article class="panel">
        <div class="section-title">
          <h2>예외 알림</h2>
          <span class="status-pill red">{{ exceptions.length }}건</span>
        </div>
        <div class="timeline">
          <div v-for="item in exceptions" :key="item.type + item.vehicleNo" class="timeline-row alert">
            <time>{{ item.status }}</time>
            <div>
              <b>{{ item.type }} · {{ item.vehicleNo }}</b>
              <span>{{ item.message }}</span>
            </div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.timeline {
  display: grid;
  gap: 10px;
}

.timeline-row {
  display: grid;
  grid-template-columns: 76px 1fr;
  gap: 12px;
  padding: 12px;
  background: #f6f9fd;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.timeline-row.alert {
  background: #fff8f5;
  border-color: #f0cec5;
}

.timeline-row time {
  color: var(--blue-700);
  font-weight: 900;
}

.timeline-row b,
.timeline-row span {
  display: block;
}

.timeline-row span {
  margin-top: 4px;
  color: var(--ink-500);
  font-size: 13px;
  font-weight: 700;
}
</style>
