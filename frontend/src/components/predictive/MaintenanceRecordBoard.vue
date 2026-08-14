<script setup>
import { computed, onMounted, ref, watch } from 'vue'

const STORAGE_KEY = 'pmMaintenanceCommentsV1'
const records = ref([])
const equipmentOptions = ref([])
const comments = ref([])
const loading = ref(true)
const loadError = ref('')
const selectedEquipment = ref('ALL')
const selectedRecordId = ref('')
const newCommentType = ref('OPINION')
const newCommentText = ref('')
const editingCommentId = ref('')
const editingCommentText = ref('')

const commentTypeLabels = {
  OPINION: '의견',
  FOLLOW_UP: '재점검 필요',
  CONFIRMED: '조치 확인',
}

const parseCollectedAt = (value) => {
  const [date, rawTime] = value.split(' ')
  const [hour, minute] = rawTime.split(':')
  return new Date(`${date}T${hour.padStart(2, '0')}:${minute}:00`).getTime()
}

const formatDateTime = (timestamp) => {
  if (!timestamp) return '-'
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(timestamp))
}

const filteredRecords = computed(() => {
  if (selectedEquipment.value === 'ALL') return records.value
  return records.value.filter((record) => record.equipmentId === selectedEquipment.value)
})

const selectedRecord = computed(() =>
  records.value.find((record) => record.id === selectedRecordId.value),
)

const selectedComments = computed(() =>
  comments.value
    .filter((comment) => comment.recordId === selectedRecordId.value)
    .sort((a, b) => b.createdAt - a.createdAt),
)

const totalCommentCount = computed(() => comments.value.length)

const currentAdminName = () => {
  try {
    const user = JSON.parse(localStorage.getItem('portGateUser') || 'null')
    return user?.name || user?.userName || user?.loginId || '관리자'
  } catch {
    return '관리자'
  }
}

const persistComments = () => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(comments.value))
}

const addComment = () => {
  const content = newCommentText.value.trim()
  if (!content || !selectedRecord.value) return

  const now = Date.now()
  comments.value.push({
    id: `${now}-${Math.random().toString(36).slice(2, 8)}`,
    recordId: selectedRecord.value.id,
    equipmentId: selectedRecord.value.equipmentId,
    type: newCommentType.value,
    content,
    author: currentAdminName(),
    createdAt: now,
    updatedAt: now,
  })
  persistComments()
  newCommentText.value = ''
  newCommentType.value = 'OPINION'
}

const beginEdit = (comment) => {
  editingCommentId.value = comment.id
  editingCommentText.value = comment.content
}

const cancelEdit = () => {
  editingCommentId.value = ''
  editingCommentText.value = ''
}

const saveEdit = (comment) => {
  const content = editingCommentText.value.trim()
  if (!content) return
  comment.content = content
  comment.updatedAt = Date.now()
  persistComments()
  cancelEdit()
}

const removeComment = (comment) => {
  if (!window.confirm('이 코멘트를 삭제할까요?')) return
  comments.value = comments.value.filter((item) => item.id !== comment.id)
  persistComments()
}

const buildMaintenanceRecords = (payload) => {
  const result = []
  const equipmentIds = Object.keys(payload.equipment).sort()
  equipmentOptions.value = equipmentIds

  equipmentIds.forEach((equipmentId) => {
    const rows = payload.equipment[equipmentId].map((row) =>
      Object.fromEntries(payload.columns.map((column, index) => [column, row[index]])),
    )
    const maintenanceRows = rows.filter((row) => row.maintenanceEvent === 1)

    maintenanceRows.forEach((maintenance) => {
      const maintenanceAt = parseCollectedAt(maintenance.collectedAt)
      const failureCandidates = rows.filter(
        (row) => row.failureEvent === 1 && parseCollectedAt(row.collectedAt) <= maintenanceAt,
      )
      const failure = failureCandidates.at(-3) || failureCandidates[0]
      const failureAt = failure ? parseCollectedAt(failure.collectedAt) : 0

      result.push({
        id: `${equipmentId}-${maintenance.collectedAt}`,
        equipmentId,
        failureAt,
        maintenanceAt,
        status: 'COMPLETED',
        issueSummary: failure
          ? `통신 성공률 ${failure.successRate}% · 패킷 손실률 ${failure.packetLossRate}%`
          : '고장 이벤트와 연결된 상세 정보 없음',
        actionSummary: `정비 후 통신 성공률 ${maintenance.successRate}% · 응답 시간 ${maintenance.responseTimeMs}ms`,
        failureMetrics: failure
          ? [
              { label: '통신 성공률', value: `${failure.successRate}%` },
              { label: '응답 시간', value: `${failure.responseTimeMs}ms` },
              { label: '패킷 손실률', value: `${failure.packetLossRate}%` },
              { label: '오류 횟수', value: `${failure.errorCount}회` },
            ]
          : [],
        maintenanceMetrics: [
          { label: '통신 성공률', value: `${maintenance.successRate}%` },
          { label: '응답 시간', value: `${maintenance.responseTimeMs}ms` },
          { label: '패킷 손실률', value: `${maintenance.packetLossRate}%` },
          { label: '오류 횟수', value: `${maintenance.errorCount}회` },
        ],
      })
    })
  })

  records.value = result.sort((a, b) => b.maintenanceAt - a.maintenanceAt)
  selectedRecordId.value = records.value[0]?.id || ''
}

watch(filteredRecords, (items) => {
  if (!items.some((item) => item.id === selectedRecordId.value)) {
    selectedRecordId.value = items[0]?.id || ''
  }
})

onMounted(async () => {
  try {
    comments.value = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    const response = await fetch('/data/antenna-maintenance-demo.json')
    if (!response.ok) throw new Error('정비 기록 데이터를 불러오지 못했습니다.')
    buildMaintenanceRecords(await response.json())
  } catch (error) {
    loadError.value = error.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="maintenance-board">
    <div class="board-header">
      <div>
        <span class="eyebrow">MAINTENANCE HISTORY</span>
        <h2>안테나 정비 기록</h2>
        <p>문제 발생부터 정비 완료까지의 기록과 관리자 코멘트를 확인합니다.</p>
      </div>
      <span class="prototype-note">화면 검토용 · 코멘트는 이 브라우저에 임시 저장</span>
    </div>

    <div v-if="loading" class="message-state">정비 기록을 불러오는 중입니다.</div>
    <div v-else-if="loadError" class="message-state error">{{ loadError }}</div>

    <template v-else>
      <div class="record-summary">
        <div><span>전체 정비 기록</span><strong>{{ records.length }}건</strong></div>
        <div><span>코멘트</span><strong>{{ totalCommentCount }}건</strong></div>
        <label>
          <span>안테나별 보기</span>
          <select v-model="selectedEquipment">
            <option value="ALL">전체 안테나</option>
            <option v-for="equipment in equipmentOptions" :key="equipment" :value="equipment">
              {{ equipment }}
            </option>
          </select>
        </label>
      </div>

      <div class="record-layout">
        <aside class="record-list" aria-label="정비 기록 목록">
          <div class="list-heading">
            <strong>정비 기록</strong>
            <span>{{ filteredRecords.length }}건</span>
          </div>
          <button
            v-for="record in filteredRecords"
            :key="record.id"
            type="button"
            class="record-item"
            :class="{ active: selectedRecordId === record.id }"
            @click="selectedRecordId = record.id"
          >
            <span class="record-item-top">
              <strong>{{ record.equipmentId }}</strong>
              <span class="status-pill green">정비 완료</span>
            </span>
            <span>{{ formatDateTime(record.maintenanceAt) }}</span>
            <small>코멘트 {{ comments.filter((item) => item.recordId === record.id).length }}건</small>
          </button>
          <div v-if="!filteredRecords.length" class="empty-list">
            선택한 안테나에는 정비 기록이 없습니다.
          </div>
        </aside>

        <article v-if="selectedRecord" class="record-detail panel">
          <div class="detail-title">
            <div>
              <span>{{ selectedRecord.equipmentId }}</span>
              <h3>고장·정비 기록</h3>
            </div>
            <span class="status-pill green">조치 완료</span>
          </div>

          <div class="event-flow">
            <section class="event-card failure">
              <span class="event-label">문제 발생</span>
              <strong>{{ formatDateTime(selectedRecord.failureAt) }}</strong>
              <p>{{ selectedRecord.issueSummary }}</p>
              <div class="metric-row">
                <span v-for="metric in selectedRecord.failureMetrics" :key="metric.label">
                  {{ metric.label }} <b>{{ metric.value }}</b>
                </span>
              </div>
            </section>

            <div class="event-arrow" aria-hidden="true">→</div>

            <section class="event-card repaired">
              <span class="event-label">정비 완료</span>
              <strong>{{ formatDateTime(selectedRecord.maintenanceAt) }}</strong>
              <p>{{ selectedRecord.actionSummary }}</p>
              <div class="metric-row">
                <span v-for="metric in selectedRecord.maintenanceMetrics" :key="metric.label">
                  {{ metric.label }} <b>{{ metric.value }}</b>
                </span>
              </div>
            </section>
          </div>

          <section class="comment-section">
            <div class="comment-heading">
              <div>
                <h3>관리자 코멘트</h3>
                <p>이 정비 기록에 대한 의견과 후속 확인 내용을 시간순으로 남깁니다.</p>
              </div>
              <strong>{{ selectedComments.length }}건</strong>
            </div>

            <form class="comment-form" @submit.prevent="addComment">
              <select v-model="newCommentType" aria-label="코멘트 유형">
                <option v-for="(label, code) in commentTypeLabels" :key="code" :value="code">
                  {{ label }}
                </option>
              </select>
              <textarea
                v-model="newCommentText"
                rows="3"
                maxlength="500"
                placeholder="정비 결과나 추가로 확인할 내용을 입력하세요."
                aria-label="새 코멘트"
              ></textarea>
              <button type="submit" class="primary-button" :disabled="!newCommentText.trim()">
                코멘트 등록
              </button>
            </form>

            <div class="comment-list">
              <article v-for="comment in selectedComments" :key="comment.id" class="comment-item">
                <div class="comment-meta">
                  <span class="comment-type">{{ commentTypeLabels[comment.type] || '의견' }}</span>
                  <strong>{{ comment.author }}</strong>
                  <time>{{ formatDateTime(comment.createdAt) }}</time>
                </div>

                <template v-if="editingCommentId === comment.id">
                  <textarea v-model="editingCommentText" rows="3" maxlength="500"></textarea>
                  <div class="comment-actions">
                    <button type="button" class="primary-button" @click="saveEdit(comment)">저장</button>
                    <button type="button" class="ghost-button" @click="cancelEdit">취소</button>
                  </div>
                </template>
                <template v-else>
                  <p>{{ comment.content }}</p>
                  <small v-if="comment.updatedAt !== comment.createdAt">
                    수정 {{ formatDateTime(comment.updatedAt) }}
                  </small>
                  <div class="comment-actions">
                    <button type="button" class="ghost-button" @click="beginEdit(comment)">수정</button>
                    <button type="button" class="delete-button" @click="removeComment(comment)">삭제</button>
                  </div>
                </template>
              </article>
              <div v-if="!selectedComments.length" class="empty-comments">
                아직 등록된 코멘트가 없습니다.
              </div>
            </div>
          </section>
        </article>

        <div v-else class="record-detail-empty">
          왼쪽에서 확인할 정비 기록을 선택하세요.
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.maintenance-board {
  display: grid;
  gap: 10px;
}

.board-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px;
  background: linear-gradient(135deg, #ffffff, #e9f1f7);
  border: 1px solid var(--line);
}

.board-header h2,
.board-header p {
  margin: 0;
}

.board-header h2 {
  margin-top: 2px;
  color: #183b5d;
  font-size: 20px;
}

.board-header p {
  margin-top: 4px;
  color: var(--ink-500);
}

.eyebrow {
  color: var(--blue-700);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.prototype-note {
  max-width: 260px;
  padding: 7px 9px;
  color: #6b5317;
  background: #fff8e8;
  border: 1px solid #dec47d;
  font-size: 11px;
}

.message-state,
.record-detail-empty {
  display: grid;
  min-height: 260px;
  place-items: center;
  color: var(--ink-500);
  background: #f5f8fb;
  border: 1px dashed var(--line);
}

.message-state.error {
  color: var(--red-500);
}

.record-summary {
  display: grid;
  grid-template-columns: 160px 160px minmax(220px, 1fr);
  border: 1px solid var(--line);
}

.record-summary > div,
.record-summary label {
  min-height: 64px;
  padding: 9px 10px;
  background: #ffffff;
  border-right: 1px solid #d4dde6;
}

.record-summary span {
  display: block;
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 700;
}

.record-summary strong {
  display: block;
  margin-top: 4px;
  color: #20364f;
  font-size: 18px;
}

.record-summary label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  border-right: 0;
}

.record-summary label span {
  flex: 0 0 auto;
}

.record-summary select,
.comment-form select,
.comment-form textarea,
.comment-item textarea {
  min-height: 34px;
  padding: 6px 8px;
  background: #ffffff;
  border: 1px solid #aeb9c5;
}

.record-layout {
  display: grid;
  grid-template-columns: 290px minmax(0, 1fr);
  gap: 10px;
  min-width: 0;
}

.record-list {
  min-height: 580px;
  max-height: 760px;
  overflow-y: auto;
  background: #ffffff;
  border: 1px solid var(--line);
}

.list-heading {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  min-height: 40px;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  color: #20364f;
  background: #e3eaf2;
  border-bottom: 1px solid var(--line);
}

.record-item {
  display: grid;
  width: 100%;
  gap: 4px;
  padding: 10px;
  color: #29445f;
  text-align: left;
  background: #ffffff;
  border: 0;
  border-bottom: 1px solid #d4dde6;
}

.record-item:hover,
.record-item.active {
  background: #edf5fc;
}

.record-item.active {
  box-shadow: inset 4px 0 #23639c;
}

.record-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.record-item > span:not(.record-item-top),
.record-item small {
  color: var(--ink-500);
  font-size: 11px;
}

.empty-list,
.empty-comments {
  padding: 24px 12px;
  color: var(--ink-500);
  text-align: center;
}

.record-detail {
  min-width: 0;
}

.detail-title,
.comment-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.detail-title {
  margin: -10px -10px 10px;
  padding: 9px 10px;
  background: #e3eaf2;
  border-bottom: 1px solid var(--line);
}

.detail-title span:first-child {
  color: var(--blue-700);
  font-size: 11px;
  font-weight: 700;
}

.detail-title h3,
.comment-heading h3 {
  margin: 1px 0 0;
  color: #20364f;
  font-size: 15px;
}

.event-flow {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: stretch;
  gap: 8px;
}

.event-card {
  padding: 10px;
  border: 1px solid;
}

.event-card.failure {
  background: #fff4f3;
  border-color: #dfaaa6;
}

.event-card.repaired {
  background: #f0f8f4;
  border-color: #a9d0bb;
}

.event-label {
  display: block;
  margin-bottom: 4px;
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 700;
}

.event-card p {
  margin: 6px 0;
  color: var(--ink-700);
}

.event-arrow {
  display: grid;
  place-items: center;
  color: #63778a;
  font-size: 22px;
}

.metric-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.metric-row span {
  padding: 3px 5px;
  color: #42566a;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(120, 140, 158, 0.35);
  font-size: 10px;
}

.comment-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.comment-heading p {
  margin: 3px 0 0;
  color: var(--ink-500);
  font-size: 11px;
}

.comment-heading > strong {
  color: var(--blue-700);
}

.comment-form {
  display: grid;
  grid-template-columns: 130px minmax(0, 1fr) auto;
  align-items: start;
  gap: 6px;
  margin-top: 10px;
  padding: 8px;
  background: #f5f8fb;
  border: 1px solid #c4d0dc;
}

.comment-form textarea,
.comment-item textarea {
  width: 100%;
  resize: vertical;
}

.comment-form .primary-button {
  min-height: 58px;
}

.comment-list {
  display: grid;
  gap: 6px;
  margin-top: 8px;
}

.comment-item {
  padding: 9px;
  background: #ffffff;
  border: 1px solid #c9d2dc;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--ink-500);
  font-size: 11px;
}

.comment-type {
  padding: 2px 5px;
  color: #29445f;
  background: #eaf2f9;
  border: 1px solid #abc2d7;
  font-weight: 700;
}

.comment-meta time {
  margin-left: auto;
}

.comment-item p {
  margin: 8px 0 5px;
  color: #23384d;
  white-space: pre-wrap;
}

.comment-item > small {
  color: var(--ink-500);
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  gap: 5px;
  margin-top: 7px;
}

.delete-button {
  min-height: 30px;
  padding: 0 10px;
  color: #a23a35;
  background: #ffffff;
  border: 1px solid #dfaaa6;
  font-weight: 700;
}

@media (max-width: 900px) {
  .board-header {
    align-items: stretch;
    flex-direction: column;
  }

  .record-layout {
    grid-template-columns: 1fr;
  }

  .record-list {
    min-height: 0;
    max-height: 280px;
  }
}

@media (max-width: 700px) {
  .record-summary,
  .event-flow,
  .comment-form {
    grid-template-columns: 1fr;
  }

  .record-summary > div,
  .record-summary label {
    border-right: 0;
    border-bottom: 1px solid #d4dde6;
  }

  .record-summary label {
    align-items: stretch;
    flex-direction: column;
  }

  .event-arrow {
    transform: rotate(90deg);
  }

  .comment-form .primary-button {
    min-height: 34px;
  }
}
</style>
