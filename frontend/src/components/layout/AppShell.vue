<script setup>
import { computed, h, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const isCollapsed = ref(localStorage.getItem('portGateSidebar') === 'collapsed')

const icons = {
  home: [
    ['path', { d: 'M3 10.5 12 3l9 7.5' }],
    ['path', { d: 'M5 9.5V21h14V9.5' }],
    ['path', { d: 'M9 21v-7h6v7' }],
  ],
  cctv: [
    ['path', { d: 'M4 6h11l3 3v5H4z' }],
    ['path', { d: 'M18 10h3v3h-3' }],
    ['path', { d: 'M9 14v4' }],
    ['path', { d: 'M6 21h10' }],
    ['path', { d: 'M12 14l4 7' }],
  ],
  dashboard: [
    ['path', { d: 'M4 4h7v7H4z' }],
    ['path', { d: 'M13 4h7v4h-7z' }],
    ['path', { d: 'M13 10h7v10h-7z' }],
    ['path', { d: 'M4 13h7v7H4z' }],
  ],
  map: [
    ['path', { d: 'M4 6.5 9 4l6 2.5L20 4v13.5l-5 2-6-2.5-5 2z' }],
    ['path', { d: 'M9 4v13' }],
    ['path', { d: 'M15 6.5v13' }],
  ],
  clipboard: [
    ['path', { d: 'M9 4h6l1 2h3v15H5V6h3z' }],
    ['path', { d: 'M9 4h6v4H9z' }],
    ['path', { d: 'M8 12h8' }],
    ['path', { d: 'M8 16h6' }],
  ],
  truck: [
    ['path', { d: 'M3 7h11v9H3z' }],
    ['path', { d: 'M14 10h4l3 3v3h-7z' }],
    ['circle', { cx: '7', cy: '18', r: '2' }],
    ['circle', { cx: '18', cy: '18', r: '2' }],
  ],
  container: [
    ['path', { d: 'M3 7h18v11H3z' }],
    ['path', { d: 'M7 7v11' }],
    ['path', { d: 'M11 7v11' }],
    ['path', { d: 'M15 7v11' }],
    ['path', { d: 'M3 11h18' }],
  ],
  bell: [
    ['path', { d: 'M6 17h12l-1.5-2.5V10a4.5 4.5 0 0 0-9 0v4.5z' }],
    ['path', { d: 'M10 20h4' }],
    ['path', { d: 'M12 4V2.5' }],
  ],
  request: [
    ['path', { d: 'M5 4h10l4 4v12H5z' }],
    ['path', { d: 'M15 4v4h4' }],
    ['path', { d: 'M9 14h6' }],
    ['path', { d: 'M12 11v6' }],
  ],
  approval: [
    ['path', { d: 'M5 4h14v16H5z' }],
    ['path', { d: 'm8 12 3 3 5-6' }],
  ],
  driver: [
    ['circle', { cx: '12', cy: '7', r: '3' }],
    ['path', { d: 'M5 21a7 7 0 0 1 14 0' }],
    ['path', { d: 'm16 12 2 2 4-5' }],
  ],
  list: [
    ['path', { d: 'M8 6h13' }],
    ['path', { d: 'M8 12h13' }],
    ['path', { d: 'M8 18h13' }],
    ['path', { d: 'M3 6h1' }],
    ['path', { d: 'M3 12h1' }],
    ['path', { d: 'M3 18h1' }],
  ],
  users: [
    ['path', { d: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' }],
    ['circle', { cx: '9', cy: '7', r: '4' }],
    ['path', { d: 'M22 21v-2a4 4 0 0 0-3-3.87' }],
    ['path', { d: 'M16 3.13a4 4 0 0 1 0 7.75' }],
  ],
  scan: [
    ['path', { d: 'M4 7V4h3' }],
    ['path', { d: 'M17 4h3v3' }],
    ['path', { d: 'M20 17v3h-3' }],
    ['path', { d: 'M7 20H4v-3' }],
    ['path', { d: 'M7 12h10' }],
  ],
  menu: [
    ['path', { d: 'M4 7h16' }],
    ['path', { d: 'M4 12h16' }],
    ['path', { d: 'M4 17h16' }],
  ],
  logout: [
    ['path', { d: 'M10 5H5v14h5' }],
    ['path', { d: 'M14 8l4 4-4 4' }],
    ['path', { d: 'M8 12h10' }],
  ],
}

const MenuIcon = (props) =>
  h(
    'svg',
    {
      class: 'menu-icon',
      viewBox: '0 0 24 24',
      fill: 'none',
      stroke: 'currentColor',
      'stroke-width': '1.8',
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
      'aria-hidden': 'true',
    },
    icons[props.name]?.map(([tag, attrs]) => h(tag, attrs)) || icons.home.map(([tag, attrs]) => h(tag, attrs)),
  )

const menus = {
  CARRIER: [
    { label: '홈', path: '/carrier/dashboard', icon: 'home' },
    { label: '승인·회원 관리', path: '/carrier/driver-approval', icon: 'approval' },
    { label: '트레일러 배정 및 작업지시', path: '/carrier/input', icon: 'clipboard' },
    { label: '작업정보 조회', path: '/carrier/inquiry', icon: 'list' },
  ],
  DRIVER: [
    { label: '작업 홈', path: '/driver/dashboard', icon: 'driver' },
    { label: '작업 현황', path: '/driver/work-status', icon: 'list' },
    { label: '내 차량', path: '/driver/vehicles', icon: 'truck' },
  ],
  ADMIN: [
    { label: '상황 관제판', path: '/admin/main', icon: 'cctv' },
    { label: '통계 요약', path: '/admin/dashboard', icon: 'dashboard' },

    { label: '운영 맵', path: '/admin/yard-map', icon: 'map' },
    { label: 'AI 인식 검증', path: '/admin/plate-recognition', icon: 'scan' },
    { label: '가입 회원 관리', path: '/admin/members', icon: 'users' },

    { label: '작업 관리', path: '/admin/work-orders', icon: 'clipboard' },
    { label: '알림/이벤트', path: '/admin/events', icon: 'bell' },
  ],
}

const roleLabels = {
  CARRIER: '운송사 담당자',
  DRIVER: '화물 기사',
  ADMIN: '관리자',
}

const currentUser = computed(() => JSON.parse(localStorage.getItem('portGateUser') || 'null'))
const pathRole = computed(() => (route.path.split('/')[1] || 'admin').toUpperCase())
const activeRole = computed(() => String(route.meta.role || currentUser.value?.roleCode || pathRole.value).toUpperCase())
const activeMenus = computed(() => menus[activeRole.value] || menus.ADMIN)
const pageTitle = computed(() => route.meta.title || '항만 게이트 시스템')

const menuLabelOverrides = {
  '/admin/main': '상황 관제판',
  '/admin/dashboard': '데이터 현황',
  '/admin/yard-map': '야드 맵',
  '/admin/plate-recognition': 'AI 번호판 인식',
  '/admin/members': '회원 관리',
  '/admin/work-orders': '작업 관리',
  '/admin/events': '알림/이벤트',
  '/carrier/dashboard': '대시보드',
  '/carrier/driver-approval': '기사 승인/회원 관리',
  '/carrier/input': '트레일러 배정 및 작업지시',
  '/carrier/inquiry': '작업정보 조회',
  '/driver/dashboard': '작업 홈',
  '/driver/work-status': '작업 현황',
  '/driver/vehicles': '내 차량',
}

const roleLabelOverrides = {
  CARRIER: '운송사 담당자',
  DRIVER: '화물 기사',
  ADMIN: '관리자',
}

const getMenuLabel = (item) => menuLabelOverrides[item.path] || item.label
const activeRoleLabel = computed(() => roleLabelOverrides[activeRole.value] || '관리자')
const accountDisplayName = computed(() =>
  currentUser.value?.displayName ||
  currentUser.value?.userName ||
  currentUser.value?.loginId ||
  currentUser.value?.username ||
  '-',
)

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
  localStorage.setItem('portGateSidebar', isCollapsed.value ? 'collapsed' : 'expanded')
}

const logout = () => {
  localStorage.removeItem('portGateUser')
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<template>
  <div class="app-shell" :class="{ collapsed: isCollapsed }">
    <aside class="sidebar">
      <button
        class="brand-toggle"
        type="button"
        :aria-label="isCollapsed ? '사이드바 열기' : '사이드바 닫기'"
        @click="toggleSidebar"
      >
        <span class="brand-mark"><MenuIcon name="menu" /></span>
        <span class="brand-text">
          <b>Port Gate</b>
          <small>Container Sector Guide</small>
        </span>
      </button>

      <div class="role-badge">
        <small>현재 화면</small>
        <strong>{{ accountDisplayName }} / {{ activeRoleLabel }}</strong>
      </div>

      <nav class="side-nav">
        <template v-for="item in activeMenus" :key="item.path">
          <RouterLink
            :to="item.path"
            class="side-link"
            :title="getMenuLabel(item)"
          >
            <span class="side-icon"><MenuIcon :name="item.icon" /></span>
            <span class="side-label">{{ getMenuLabel(item) }}</span>
          </RouterLink>
        </template>
      </nav>

      <div class="side-footer">
        <button class="logout-button" type="button" title="로그아웃" @click="logout">
          <MenuIcon name="logout" />
          <span class="logout-label">로그아웃</span>
        </button>
      </div>
    </aside>

    <div class="main-area">
      <header class="topbar">
        <div>
          <small>항만 게이트 차량 출입 및 컨테이너 상차 섹터 안내</small>
          <h1>{{ pageTitle }}</h1>
        </div>
      </header>

      <main class="content">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: grid;
  width: 100%;
  max-width: 100vw;
  min-height: 100vh;
  grid-template-columns: 230px minmax(0, 1fr);
  overflow-x: clip;
  transition: grid-template-columns 0.16s ease;
}

.app-shell.collapsed {
  grid-template-columns: 54px minmax(0, 1fr);
}

.sidebar {
  position: sticky;
  top: 0;
  display: flex;
  height: 100vh;
  min-width: 0;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  padding: 12px;
  color: #ffffff;
  background: #26384d;
  border-right: 1px solid #172636;
}

.brand-toggle {
  display: flex;
  width: 100%;
  min-height: 38px;
  align-items: center;
  gap: 8px;
  padding: 0 0 10px;
  color: #ffffff;
  text-align: left;
  background: transparent;
  border: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.18);
}

.brand-mark {
  display: inline-flex;
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: #1b5d8d;
  border: 1px solid #6f98bd;
  border-radius: 1px;
}

.brand-text {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}

.brand-text b,
.brand-text small {
  display: block;
}

.brand-text b {
  font-size: 15px;
  font-weight: 700;
}

.brand-text small {
  color: #c5d2df;
  font-size: 11px;
}

.menu-icon {
  width: 17px;
  height: 17px;
}

.role-badge,
.side-note {
  padding: 8px;
  background: #30455d;
  border: 1px solid #53677c;
  border-radius: 1px;
}

.role-badge small,
.side-note span {
  display: block;
  color: #c5d2df;
}

.role-badge strong {
  display: block;
  margin-top: 2px;
  font-size: 14px;
  font-weight: 700;
}

.side-nav {
  display: grid;
  gap: 3px;
}

.side-link {
  display: flex;
  min-height: 34px;
  min-width: 0;
  align-items: center;
  gap: 8px;
  padding: 0 8px;
  color: #dceaff;
  border: 1px solid transparent;
  border-radius: 1px;
  font-weight: 700;
}

.side-icon {
  display: inline-flex;
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  align-items: center;
  justify-content: center;
  background: #1e3042;
  border: 1px solid #53677c;
  border-radius: 1px;
}

.side-label {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}

.side-link.router-link-active,
.side-link:hover {
  color: #102a5c;
  background: #eaf1f8;
  border-color: #9fb0c0;
}

.side-link.router-link-active .side-icon,
.side-link:hover .side-icon {
  color: #ffffff;
  background: var(--blue-700);
}

.side-footer {
  display: grid;
  gap: 8px;
  margin-top: auto;
}

.side-note b {
  display: block;
  margin-bottom: 6px;
}

.logout-button {
  display: flex;
  min-height: 34px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  color: #ffffff;
  background: #b8403a;
  border: 1px solid #d58a86;
  border-radius: 1px;
  font-weight: 700;
}

.logout-button:hover {
  background: #bb2e27;
}

.app-shell.collapsed .sidebar {
  align-items: center;
  padding: 12px 8px;
}

.app-shell.collapsed .brand-toggle {
  justify-content: center;
  padding-bottom: 10px;
}

.app-shell.collapsed .brand-text,
.app-shell.collapsed .role-badge,
.app-shell.collapsed .side-note,
.app-shell.collapsed .side-label,
.app-shell.collapsed .logout-label {
  display: none;
}

.app-shell.collapsed .side-link {
  justify-content: center;
  width: 34px;
  padding: 0;
}

.app-shell.collapsed .logout-button {
  width: 34px;
  padding: 0;
}

.main-area {
  min-width: 0;
  max-width: 100%;
  overflow-x: hidden;
}

.topbar {
  display: flex;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 14px;
  background: linear-gradient(#ffffff, #edf2f6);
  border-bottom: 1px solid var(--line);
}

.topbar small {
  color: var(--ink-500);
  font-size: 11px;
  font-weight: 700;
}

.topbar h1 {
  margin: 2px 0 0;
  font-size: 18px;
  font-weight: 700;
}

.content {
  min-width: 0;
  max-width: 100%;
  padding: 10px;
  overflow-x: hidden;
}

@media (min-width: 1100px) and (max-width: 1320px) and (max-height: 760px) {
  .app-shell,
  .app-shell.collapsed {
    grid-template-columns: 54px minmax(0, 1fr);
  }

  .sidebar,
  .app-shell.collapsed .sidebar {
    align-items: center;
    padding: 10px 8px;
  }

  .brand-toggle {
    justify-content: center;
    padding-bottom: 10px;
  }

  .brand-text,
  .role-badge,
  .side-note,
  .side-label,
  .logout-label {
    display: none;
  }

  .side-link {
    justify-content: center;
    width: 34px;
    padding: 0;
  }

  .logout-button {
    width: 34px;
    padding: 0;
  }

  .topbar {
    min-height: 46px;
    padding: 6px 12px;
  }

  .topbar small {
    display: none;
  }

  .topbar h1 {
    margin: 0;
    font-size: 19px;
  }

  .content {
    padding: 8px;
  }
}

@media (max-width: 900px) {
  .app-shell,
  .app-shell.collapsed {
    grid-template-columns: 1fr;
  }

  .sidebar,
  .app-shell.collapsed .sidebar {
    align-items: stretch;
    position: static;
    height: auto;
    min-height: 0;
    max-height: none;
    overflow-y: visible;
  }

  .side-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .app-shell.collapsed .side-nav,
  .app-shell.collapsed .side-footer {
    display: none;
  }

  .side-footer {
    margin-top: 8px;
  }

  .topbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
