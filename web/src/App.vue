<template>
  <div class="app-shell">
    <!-- 背景氛围光斑层：让磨砂透明卡片具备真实色彩透射质感 -->
    <div class="aurora-backdrop" aria-hidden="true">
      <span class="orb orb-1" />
      <span class="orb orb-2" />
      <span class="orb orb-3" />
    </div>

    <div class="navbar-wrap" :class="{ 'is-compact': isScrolled }">
      <header class="container">
        <div class="navbar card" :class="{ 'is-compact': isScrolled }">
          <RouterLink to="/" class="brand">
            <span class="brand-emblem">文</span>
            <span class="brand-texts">
              <span class="brand-title">{{ siteInfo?.siteTitle || "MyBlog" }}</span>
              <span v-if="siteInfo?.siteSubtitle" class="brand-sub">{{ siteInfo.siteSubtitle }}</span>
            </span>
          </RouterLink>

          <nav class="nav-links">
            <RouterLink to="/" class="nav-pill" active-class="active">首页</RouterLink>
            <RouterLink to="/archives" class="nav-pill" active-class="active">归档</RouterLink>
            <RouterLink to="/links" class="nav-pill" active-class="active">友链</RouterLink>
            <RouterLink to="/about" class="nav-pill" active-class="active">关于</RouterLink>
            <a href="/admin/" class="nav-pill nav-pill--admin">后台管理</a>
            <button
              type="button"
              class="theme-btn"
              :title="isDark ? '切换浅色模式' : '切换深色模式'"
              @click="toggleTheme"
            >
              <span>{{ isDark ? "浅色" : "深色" }}</span>
            </button>
          </nav>
        </div>
      </header>
    </div>

    <main class="container main-content">
      <RouterView v-slot="{ Component }">
        <Transition name="route-fade" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>

    <footer class="container footer-wrap">
      <div class="footer card">
        <span>
          © {{ new Date().getFullYear() }} {{ siteInfo?.authorName || "MyBlog" }} · 基于
          youlai-boot & Vue 3 构建
        </span>
        <div class="footer-right">
          <RouterLink to="/links" class="footer-link">友情链接</RouterLink>
          <a href="/rss.xml" target="_blank" rel="noopener" class="footer-link">RSS 订阅</a>
          <a href="/sitemap.xml" target="_blank" rel="noopener" class="footer-link">站点地图</a>
          <span v-if="siteInfo?.icpNumber">{{ siteInfo.icpNumber }}</span>
        </div>
      </div>
    </footer>

    <!-- 回到顶部进度环按钮 -->
    <Transition name="route-fade">
      <button
        v-show="scrollProgress > 0.08"
        type="button"
        class="back-top-btn card"
        title="回到顶部"
        @click="scrollToTop"
      >
        <svg class="progress-ring" viewBox="0 0 40 40">
          <circle class="ring-track" cx="20" cy="20" r="16" />
          <circle
            class="ring-value"
            cx="20"
            cy="20"
            r="16"
            :stroke-dasharray="ringCircumference"
            :stroke-dashoffset="ringCircumference * (1 - scrollProgress)"
          />
        </svg>
        <span class="back-top-arrow">顶部</span>
      </button>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { useSiteInfo } from "@/composables/useSiteInfo";

const { siteInfo, loadSiteInfo } = useSiteInfo();
const isDark = ref(localStorage.getItem("myblog_theme") === "dark");
const scrollProgress = ref(0);
const isScrolled = ref(false);
const ringCircumference = 2 * Math.PI * 16;

function applyTheme() {
  document.documentElement.classList.toggle("dark", isDark.value);
}

function toggleTheme() {
  isDark.value = !isDark.value;
  localStorage.setItem("myblog_theme", isDark.value ? "dark" : "light");
  applyTheme();
}

function handleScroll() {
  const y = window.scrollY;
  if (!isScrolled.value && y > 48) {
    isScrolled.value = true;
  } else if (isScrolled.value && y < 16) {
    isScrolled.value = false;
  }
  const doc = document.documentElement;
  const maxScroll = doc.scrollHeight - window.innerHeight;
  if (maxScroll <= 0) {
    scrollProgress.value = 0;
    return;
  }
  scrollProgress.value = Math.min(1, Math.max(0, y / maxScroll));
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: "smooth" });
}

onMounted(() => {
  applyTheme();
  loadSiteInfo();
  window.addEventListener("scroll", handleScroll, { passive: true });
  handleScroll();
});

onUnmounted(() => {
  window.removeEventListener("scroll", handleScroll);
});
</script>

<style scoped>
.app-shell {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.aurora-backdrop {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(84px);
  opacity: 0.45;
  will-change: transform;
}

html.dark .orb {
  opacity: 0.28;
}

.orb-1 {
  width: 480px;
  height: 480px;
  top: -120px;
  left: -80px;
  background: radial-gradient(circle, rgba(20, 184, 166, 0.55), transparent 70%);
  animation: floatOrbA 18s ease-in-out infinite alternate;
}

.orb-2 {
  width: 520px;
  height: 520px;
  top: 22%;
  right: -130px;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.5), transparent 70%);
  animation: floatOrbB 22s ease-in-out infinite alternate;
}

.orb-3 {
  width: 440px;
  height: 440px;
  bottom: -140px;
  left: 28%;
  background: radial-gradient(circle, rgba(244, 114, 182, 0.34), transparent 70%);
  animation: floatOrbA 24s ease-in-out infinite alternate-reverse;
}

@keyframes floatOrbA {
  0% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  100% {
    transform: translate3d(55px, 40px, 0) scale(1.1);
  }
}

@keyframes floatOrbB {
  0% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  100% {
    transform: translate3d(-60px, -45px, 0) scale(1.08);
  }
}

.navbar-wrap {
  position: sticky;
  top: 0;
  z-index: 50;
  padding-top: 10px;
}

.navbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 18px;
  min-height: 58px;
  border-radius: 20px;
  transition:
    background-color 0.28s ease,
    border-color 0.28s ease,
    box-shadow 0.28s ease;
}

.navbar.is-compact {
  background: var(--bg-glass-strong);
  backdrop-filter: blur(26px) saturate(185%);
  -webkit-backdrop-filter: blur(26px) saturate(185%);
  border-color: color-mix(in srgb, var(--accent) 26%, var(--border-glass));
  box-shadow:
    0 14px 36px -12px rgba(15, 23, 42, 0.16),
    var(--specular-inset);
}

html.dark .navbar.is-compact {
  box-shadow:
    0 16px 38px -10px rgba(0, 0, 0, 0.65),
    var(--specular-inset);
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
}

.brand-emblem {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--accent), var(--accent-2));
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  flex-shrink: 0;
  box-shadow: 0 6px 16px -4px color-mix(in srgb, var(--accent) 55%, transparent);
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

html.dark .brand-emblem {
  color: #06131a;
}

.brand:hover .brand-emblem {
  transform: rotate(15deg) scale(1.08);
}

.brand-texts {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
  line-height: 1.25;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-sub {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.3;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-pill {
  padding: 6px 13px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  white-space: nowrap;
  transition: all 0.2s ease;
}

.nav-pill:hover,
.nav-pill.active {
  color: var(--accent);
  background: var(--accent-soft);
}

.nav-pill--admin {
  border: 1px dashed var(--border-subtle);
}

.theme-btn {
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  border-radius: 999px;
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1), border-color 0.2s ease, color 0.2s ease;
}

.theme-btn:hover {
  transform: scale(1.04);
  border-color: var(--accent);
  color: var(--accent);
}

.main-content {
  position: relative;
  z-index: 1;
  flex: 1;
  width: 100%;
  padding-top: 20px;
  padding-bottom: 44px;
}

.footer-wrap {
  position: relative;
  z-index: 1;
  width: 100%;
  padding-bottom: 22px;
}

.footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 16px;
  padding: 16px 22px;
  border-radius: 20px;
  font-size: 13px;
  color: var(--text-muted);
}

.footer-right {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
}

.footer-link {
  color: var(--text-secondary);
  transition: color 0.2s ease;
}

.footer-link:hover {
  color: var(--accent);
}

.back-top-btn {
  position: fixed;
  right: 24px;
  bottom: 26px;
  z-index: 60;
  width: 46px;
  height: 46px;
  padding: 0;
  border-radius: 50%;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.back-top-btn:hover {
  transform: translateY(-3px) scale(1.05);
}

.progress-ring {
  position: absolute;
  inset: 3px;
  width: 38px;
  height: 38px;
  transform: rotate(-90deg);
}

.ring-track {
  fill: none;
  stroke: var(--border-subtle);
  stroke-width: 2.5;
}

.ring-value {
  fill: none;
  stroke: var(--accent);
  stroke-width: 2.5;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.12s linear;
}

.back-top-arrow {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-primary);
}

@media (max-width: 820px) {
  .brand-sub {
    display: none;
  }
}

@media (max-width: 640px) {
  .navbar {
    padding: 8px 12px;
    gap: 8px;
  }

  .nav-links {
    width: 100%;
    justify-content: space-between;
    gap: 4px;
  }

  .nav-pill {
    padding: 5px 10px;
    font-size: 13px;
  }
}
</style>
