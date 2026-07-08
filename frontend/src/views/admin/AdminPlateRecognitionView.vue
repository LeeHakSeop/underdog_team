<script setup>
import { computed, onMounted, ref } from 'vue'
import { usePlateRecognitionStore } from '@/stores/adminStore/plateRecognitionStore'

const plateRecognitionStore = usePlateRecognitionStore()
const selectedFile = ref(null)
const previewUrl = ref('')
const selectedOcrType = ref('paddle')

const result = computed(() => plateRecognitionStore.result)

onMounted(() => {
  plateRecognitionStore.reset()
})

const selectFile = (event) => {
  const file = event.target.files?.[0]

  selectedFile.value = file || null
  previewUrl.value = file ? URL.createObjectURL(file) : ''
}

const submitRecognize = async () => {
  if (!selectedFile.value) {
    return
  }

  await plateRecognitionStore.recognize(selectedFile.value, selectedOcrType.value)
}
</script>

<template>
  <div class="page-stack">
    <section class="panel">
      <div class="section-title">
        <h2>AI 번호판 인식</h2>
        <span class="status-pill">{{ result?.message || '이미지 대기' }}</span>
      </div>

      <div class="recognition-layout">
        <div class="upload-area">
          <label class="model-select">
            <span>OCR 모델</span>
            <select v-model="selectedOcrType">
              <option value="paddle">PaddleOCR</option>
              <option value="crnn">CRNN</option>
            </select>
          </label>

          <label class="file-box" for="plateImage">
            <span>차량 이미지 선택</span>
            <input id="plateImage" accept="image/*" type="file" @change="selectFile" />
          </label>

          <img v-if="previewUrl" :src="previewUrl" alt="선택한 차량 이미지" class="preview-image" />

          <button
            class="primary-button"
            type="button"
            :disabled="!selectedFile || plateRecognitionStore.loading"
            @click="submitRecognize"
          >
            {{ plateRecognitionStore.loading ? '인식 중' : '번호판 인식' }}
          </button>

          <p v-if="plateRecognitionStore.error" class="error-message">
            {{ plateRecognitionStore.error }}
          </p>
        </div>

        <div class="result-area">
          <div class="result-grid">
            <div>
              <span>인식 번호판</span>
              <strong>{{ result?.aiResult?.plateNumber || '-' }}</strong>
            </div>
            <div>
              <span>OCR 모델</span>
              <strong>{{ result?.aiResult?.ocrType || selectedOcrType }}</strong>
            </div>
            <div>
              <span>인식 성공</span>
              <strong>{{ result?.aiResult?.detected ? '성공' : '-' }}</strong>
            </div>
            <div>
              <span>검토 필요</span>
              <strong>{{ result?.needReview ? '필요' : result ? '불필요' : '-' }}</strong>
            </div>
            <div>
              <span>최종 신뢰도</span>
              <strong>{{ result?.aiResult?.confidence ?? '-' }}</strong>
            </div>
          </div>

          <div class="section-title sub-title">
            <h3>차량 매칭 결과</h3>
          </div>

          <table class="data-table">
            <tbody>
              <tr>
                <th>차량 번호</th>
                <td>{{ result?.vehicle?.plateNumber || '-' }}</td>
              </tr>
              <tr>
                <th>차량 유형</th>
                <td>{{ result?.vehicle?.vehicleType || '-' }}</td>
              </tr>
              <tr>
                <th>등록 여부</th>
                <td>{{ result?.vehicle?.isRegistered ?? '-' }}</td>
              </tr>
              <tr>
                <th>차량 상태</th>
                <td>{{ result?.vehicle?.vehicleStatus || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.recognition-layout {
  display: grid;
  grid-template-columns: minmax(280px, 0.8fr) minmax(0, 1.2fr);
  gap: 14px;
}

.upload-area,
.result-area {
  display: grid;
  align-content: start;
  gap: 12px;
}

.file-box {
  display: grid;
  min-height: 90px;
  place-items: center;
  color: var(--blue-700);
  background: #f4f9ff;
  border: 1px dashed #8db5dc;
  border-radius: 4px;
  font-weight: 800;
}

.model-select {
  display: grid;
  gap: 6px;
}

.model-select span {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.model-select select {
  height: 36px;
  padding: 0 10px;
  color: var(--ink-900);
  background: #fff;
  border: 1px solid var(--line);
  font-weight: 800;
}

.file-box input {
  display: none;
}

.preview-image {
  width: 100%;
  max-height: 280px;
  object-fit: contain;
  background: #f6f9fd;
  border: 1px solid var(--line);
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.result-grid div {
  display: grid;
  gap: 4px;
  padding: 10px;
  background: #f6f9fd;
  border: 1px solid var(--line);
}

.result-grid span,
.data-table th {
  color: var(--ink-500);
  font-size: 12px;
  font-weight: 800;
}

.result-grid strong {
  font-size: 18px;
  font-weight: 900;
}

.sub-title {
  margin-top: 4px;
}

.sub-title h3 {
  margin: 0;
  font-size: 16px;
}

.error-message {
  margin: 0;
  color: #9f1d1d;
  font-weight: 800;
}

@media (max-width: 900px) {
  .recognition-layout,
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
