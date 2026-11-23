<template>
  <div class="app-shell">
    <!-- 文章详情页顶部极细线性阅读进度条 -->
    <div v-if="isArticleRoute" class="reading-progress-rail" aria-hidden="true">
      <div
        class="reading-progress-fill"
        :style="{ transform: `scaleX(${scrollProgress})` }"
      />
    </div>

    <!-- 背景氛围光斑层：让磨砂透明卡片具备真实色彩透射质感 -->
    <div class="aurora-backdrop" aria-hidden="true">
      <span class="orb orb-1" />
      <span class="orb orb-2" />
      <span class="orb orb-3" />
    </div>

    <div class="navbar-wrap" :class="{ 'is-compact': isScrolled }">
      <header class="container">
        <div
          class="navbar card"
          :class="{
            'is-compact': isScrolled,
            'has-article-title': isArticleAbsorbed,
            'is-inhaling': inhaleBurst,
          }"
        >
          <RouterLink to="/" class="brand">
            <span class="brand-emblem squircle" :class="{ 'is-catching': inhaleBurst }" aria-hidden="true">
              <svg viewBox="0 0 32 32" class="brand-logo-svg">
                <path
                  d="M8.5 22.5V10.5L16 17.5L23.5 10.5V22.5"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
                <circle cx="16" cy="9.2" r="1.75" class="brand-logo-dot" />
              </svg>
            </span>
            <div class="brand-slot">
              <Transition name="title-inhale" mode="out-in">
                <span v-if="isArticleAbsorbed" key="article" class="brand-texts">
                  <span class="brand-title brand-title--article">{{ navbarArticle!.title }}</span>
                  <span class="brand-sub brand-sub--reading">
                    正在阅读{{ navbarArticle?.category ? ` · ${navbarArticle.category}` : "" }}
                  </span>
                </span>
                <span v-else key="site" class="brand-texts">
                  <span class="brand-title">{{ siteInfo?.siteTitle || "MyBlog" }}</span>
                  <span v-if="siteInfo?.siteSubtitle" class="brand-sub">
                    {{ siteInfo.siteSubtitle }}
                  </span>
                </span>
              </Transition>
            </div>
          </RouterLink>

          <nav ref="navLinksRef" class="nav-links">
            <span
              class="nav-active-glider"
              :class="{ 'is-ready': navGlider.visible, 'is-instant': !navGlider.animated }"
              :style="{
                transform: `translate3d(${navGlider.left}px, ${navGlider.top}px, 0)`,
                width: `${navGlider.width}px`,
                height: `${navGlider.height}px`,
              }"
              aria-hidden="true"
            />
            <RouterLink
              :ref="(el) => setNavTabRef('Home', el)"
              to="/"
              class="nav-pill nav-pill--tab"
              :class="{ active: activeNavTab === 'Home' }"
              @mouseenter="prefetchNavTarget('Home')"
              @focus="prefetchNavTarget('Home')"
            >
              首页
            </RouterLink>
            <RouterLink
              :ref="(el) => setNavTabRef('Archives', el)"
              to="/archives"
              class="nav-pill nav-pill--tab"
              :class="{ active: activeNavTab === 'Archives' }"
              @mouseenter="prefetchNavTarget('Archives')"
              @focus="prefetchNavTarget('Archives')"
            >
              归档
            </RouterLink>
            <RouterLink
              :ref="(el) => setNavTabRef('LearningPaths', el)"
              to="/paths"
              class="nav-pill nav-pill--tab"
              :class="{ active: activeNavTab === 'LearningPaths' }"
              @mouseenter="prefetchNavTarget('LearningPaths')"
              @focus="prefetchNavTarget('LearningPaths')"
            >
              学习路径
            </RouterLink>
            <RouterLink
              :ref="(el) => setNavTabRef('Links', el)"
              to="/links"
              class="nav-pill nav-pill--tab"
              :class="{ active: activeNavTab === 'Links' }"
              @mouseenter="prefetchNavTarget('Links')"
              @focus="prefetchNavTarget('Links')"
            >
              友链
            </RouterLink>
            <RouterLink
              :ref="(el) => setNavTabRef('About', el)"
              to="/about"
              class="nav-pill nav-pill--tab"
              :class="{ active: activeNavTab === 'About' }"
              @mouseenter="prefetchNavTarget('About')"
              @focus="prefetchNavTarget('About')"
            >
              关于
            </RouterLink>
            <button
              ref="searchTriggerRef"
              type="button"
              class="nav-pill nav-search-btn"
              :class="{ 'is-morph-hidden': searchMorphHidden }"
              title="全站快捷搜索 (Ctrl+K / Cmd+K)"
              @click="cmdOpen = true"
            >
              <svg class="nav-search-icon" viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="11" cy="11" r="7" />
                <path d="M20 20l-3.5-3.5" />
              </svg>
              <span>搜索</span>
              <kbd class="nav-search-kbd">⌘K</kbd>
            </button>
            <a href="/admin/" class="nav-pill nav-pill--admin">后台管理</a>
          </nav>
        </div>
      </header>
    </div>

    <!-- 右上角台灯拽绳：平时微风自由摆动，向下拉拽切换深色/浅色模式 -->
    <LampPullSwitch :is-dark="isDark" @toggle="toggleTheme" />

    <!-- 文章卡片 <-> 详情页双向容器展开/归位无缝动画层 -->
    <ArticleMorphLayer />

    <!-- Cmd / Ctrl + K 全局快捷搜索面板（支持从导航搜索胶囊原位展开与归位无缝动画） -->
    <CommandPaletteModal
      v-model="cmdOpen"
      :trigger-el="searchTriggerRef"
      @origin-hidden="searchMorphHidden = $event"
    />

    <main class="container main-content">
      <div
        class="route-stage"
        :class="{ 'is-stage-transitioning': isStageTransitioning }"
        :style="stageMinHeight ? { minHeight: stageMinHeight } : undefined"
      >
        <RouterView v-slot="{ Component, route: viewRoute }">
          <Transition
            :name="routeTransitionName"
            type="transition"
            :css="!shouldSkipRouteFade"
            @before-leave="onRouteBeforeLeave"
            @enter="onRouteEnter"
            @after-enter="onRouteAfterEnter"
            @enter-cancelled="onRouteEnterCancelled"
            @after-leave="onRouteAfterLeave"
            @leave-cancelled="onRouteLeaveCancelled"
          >
            <component
              :is="Component"
              :key="getRouteViewKey(viewRoute)"
            />
          </Transition>
        </RouterView>
      </div>
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
import {
  computed,
  nextTick,
  onMounted,
  onUnmounted,
  reactive,
  ref,
  watch,
  type ComponentPublicInstance,
} from "vue";
import { useRoute } from "vue-router";
import ArticleMorphLayer from "@/components/ArticleMorphLayer.vue";
import CommandPaletteModal from "@/components/CommandPaletteModal.vue";
import LampPullSwitch from "@/components/LampPullSwitch.vue";
import { prefersReducedMotion, useArticleTransition } from "@/composables/useArticleTransition";
import { useNavbarArticle } from "@/composables/useNavbarArticle";
import {
  consumeLeavingScrollY,
  getRouteViewKey,
  prefetchNavTarget,
  scheduleIdleRoutePrefetch,
  useRouteFlow,
} from "@/composables/useRouteFlow";
import { useSiteInfo } from "@/composables/useSiteInfo";

const route = useRoute();
const { siteInfo, loadSiteInfo } = useSiteInfo();
const { navbarArticle } = useNavbarArticle();
const { shouldSkipRouteFade } = useArticleTransition();
const { routeTransitionName } = useRouteFlow();
const isArticleRoute = computed(() => route.name === "ArticleDetail");
const isDark = ref(localStorage.getItem("myblog_theme") === "dark");
const cmdOpen = ref(false);
const searchTriggerRef = ref<HTMLElement | null>(null);
const searchMorphHidden = ref(false);
const scrollProgress = ref(0);
const isScrolled = ref(false);
const inhaleBurst = ref(false);
const ringCircumference = 2 * Math.PI * 16;
let inhaleTimer: number | null = null;

const navLinksRef = ref<HTMLElement | null>(null);
const navTabEls = new Map<string, HTMLElement>();
const navGlider = reactive({
  left: 0,
  top: 0,
  width: 0,
  height: 0,
  visible: false,
  animated: false,
});
let navResizeObserver: ResizeObserver | null = null;

const activeNavTab = computed(() => {
  const name = route.name;
  if (
    name === "Home" ||
    name === "Archives" ||
    name === "LearningPaths" ||
    name === "Links" ||
    name === "About"
  ) {
    return name;
  }
  return null;
});

function setNavTabRef(key: string, el: Element | ComponentPublicInstance | null) {
  const htmlEl = el && "$el" in el ? ((el.$el as HTMLElement) || null) : ((el as HTMLElement) || null);
  if (htmlEl) {
    navTabEls.set(key, htmlEl);
  } else {
    navTabEls.delete(key);
  }
}

function syncNavGlider(animate = true) {
  const key = activeNavTab.value;
  const wrap = navLinksRef.value;
  const tabEl = key ? navTabEls.get(key) : undefined;
  if (!wrap || !tabEl) {
    navGlider.visible = false;
    return;
  }
  const wrapRect = wrap.getBoundingClientRect();
  const tabRect = tabEl.getBoundingClientRect();
  navGlider.animated = animate && navGlider.visible;
  navGlider.left = Math.round((tabRect.left - wrapRect.left) * 10) / 10;
  navGlider.top = Math.round((tabRect.top - wrapRect.top) * 10) / 10;
  navGlider.width = Math.round(tabRect.width * 10) / 10;
  navGlider.height = Math.round(tabRect.height * 10) / 10;
  navGlider.visible = true;
}

const isStageTransitioning = ref(false);
const stageMinHeight = ref("");

function onRouteBeforeLeave(el: Element) {
  if (shouldSkipRouteFade.value || prefersReducedMotion()) return;
  const htmlEl = el as HTMLElement;
  const prevH = htmlEl.getBoundingClientRect().height;
  const scrollOffset = consumeLeavingScrollY();
  isStageTransitioning.value = true;
  if (prevH > 0) {
    stageMinHeight.value = `${Math.ceil(prevH)}px`;
  }
  if (scrollOffset > 1) {
    htmlEl.style.marginTop = `-${scrollOffset}px`;
  }
}

function onRouteEnter(el: Element) {
  if (shouldSkipRouteFade.value || prefersReducedMotion()) return;
  const htmlEl = el as HTMLElement;
  window.requestAnimationFrame(() => {
    const nextH = htmlEl.getBoundingClientRect().height;
    if (nextH > 0) {
      stageMinHeight.value = `${Math.ceil(nextH)}px`;
    }
  });
}

function onRouteAfterEnter() {
  isStageTransitioning.value = false;
  stageMinHeight.value = "";
}

function onRouteEnterCancelled() {
  isStageTransitioning.value = false;
  stageMinHeight.value = "";
}

function onRouteAfterLeave(el: Element) {
  (el as HTMLElement).style.marginTop = "";
}

function onRouteLeaveCancelled(el: Element) {
  (el as HTMLElement).style.marginTop = "";
}

watch(activeNavTab, () => {
  void nextTick(() => syncNavGlider(true));
});

const isArticleAbsorbed = computed(() =>
  Boolean(navbarArticle.value?.absorbed && navbarArticle.value?.title),
);

watch(isArticleAbsorbed, (next, prev) => {
  if (next && !prev) {
    inhaleBurst.value = false;
    requestAnimationFrame(() => {
      inhaleBurst.value = true;
    });
    if (inhaleTimer !== null) {
      window.clearTimeout(inhaleTimer);
    }
    inhaleTimer = window.setTimeout(() => {
      inhaleBurst.value = false;
      inhaleTimer = null;
    }, 480);
  }
});

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

function handleCommandShortcut(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
    e.preventDefault();
    cmdOpen.value = !cmdOpen.value;
  }
}

onMounted(() => {
  applyTheme();
  loadSiteInfo();
  window.addEventListener("scroll", handleScroll, { passive: true });
  window.addEventListener("keydown", handleCommandShortcut);
  window.addEventListener("resize", handleNavResize, { passive: true });
  handleScroll();

  void nextTick(() => syncNavGlider(false));
  if (typeof ResizeObserver !== "undefined" && navLinksRef.value) {
    navResizeObserver = new ResizeObserver(() => {
      syncNavGlider(false);
    });
    navResizeObserver.observe(navLinksRef.value);
  }
  void document.fonts?.ready?.then(() => {
    syncNavGlider(false);
  });
  scheduleIdleRoutePrefetch();
});

onUnmounted(() => {
  window.removeEventListener("scroll", handleScroll);
  window.removeEventListener("keydown", handleCommandShortcut);
  window.removeEventListener("resize", handleNavResize);
  navResizeObserver?.disconnect();
  if (inhaleTimer !== null) {
    window.clearTimeout(inhaleTimer);
  }
});

function handleNavResize() {
  syncNavGlider(false);
}
</script>

<style scoped>
.app-shell {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.reading-progress-rail {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 2.5px;
  z-index: 75;
  pointer-events: none;
  background: transparent;
}

.reading-progress-fill {
  width: 100%;
  height: 100%;
  transform-origin: left center;
  background: linear-gradient(90deg, var(--accent), var(--accent-2));
  transition: transform 0.08s linear;
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
  filter: blur(96px);
  opacity: 0.13;
  will-change: transform;
}

html.dark .orb {
  opacity: 0.14;
}

.orb-1 {
  width: 480px;
  height: 480px;
  top: -160px;
  left: -100px;
  background: radial-gradient(circle, rgba(20, 184, 166, 0.42), transparent 70%);
  animation: floatOrbA 22s ease-in-out infinite alternate;
}

.orb-2 {
  width: 520px;
  height: 520px;
  top: 26%;
  right: -160px;
  background: radial-gradient(circle, rgba(148, 163, 184, 0.32), transparent 70%);
  animation: floatOrbB 26s ease-in-out infinite alternate;
}

.orb-3 {
  width: 400px;
  height: 400px;
  bottom: -160px;
  left: 30%;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.22), transparent 70%);
  animation: floatOrbA 28s ease-in-out infinite alternate-reverse;
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
  border-radius: var(--radius-card);
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

.navbar.has-article-title {
  border-color: color-mix(in srgb, var(--accent) 36%, var(--border-glass));
}

.navbar.is-inhaling {
  animation: navInhale 0.46s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes navInhale {
  0% {
    transform: scale(1) translateY(0);
  }
  38% {
    transform: scale(0.992) translateY(1.5px);
    box-shadow:
      0 12px 34px -10px color-mix(in srgb, var(--accent) 34%, transparent),
      inset 0 1px 0 rgba(255, 255, 255, 0.95);
  }
  100% {
    transform: scale(1) translateY(0);
  }
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
  flex: 1;
  min-width: 0;
}

.brand-emblem {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-control);
  background: #111827;
  color: #f8fafc;
  border: 1px solid rgba(255, 255, 255, 0.14);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 3px 10px -4px rgba(15, 23, 42, 0.3);
  transition:
    transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1),
    background-color 0.25s ease,
    border-color 0.25s ease,
    box-shadow 0.25s ease;
}

.brand-logo-svg {
  width: 22px;
  height: 22px;
  display: block;
}

.brand-logo-dot {
  fill: var(--accent);
}

.brand-emblem.is-catching {
  animation: emblemCatch 0.44s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes emblemCatch {
  0% {
    transform: scale(1);
  }
  42% {
    transform: scale(0.9);
  }
  75% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}

html.dark .brand-emblem {
  background: #f1f5f9;
  color: #0f172a;
  border-color: rgba(255, 255, 255, 0.55);
  box-shadow: 0 4px 12px -4px rgba(0, 0, 0, 0.55);
}

.brand:hover .brand-emblem {
  transform: scale(1.04);
  border-color: var(--accent);
}

.brand-slot {
  height: 38px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
}

.brand-texts {
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 100%;
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

.brand-title--article {
  font-size: 15.5px;
  letter-spacing: -0.01em;
}

.brand-sub {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.3;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.brand-sub--reading {
  color: var(--accent);
  font-weight: 500;
}

.title-inhale-enter-active {
  transition:
    opacity 0.3s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.28s ease;
}

.title-inhale-leave-active {
  transition:
    opacity 0.16s ease,
    transform 0.18s ease,
    filter 0.16s ease;
}

.title-inhale-enter-from {
  opacity: 0;
  transform: translateY(14px) scale(0.95);
  filter: blur(4px);
}

.title-inhale-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.96);
  filter: blur(3px);
}

.nav-links {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.nav-active-glider {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 0;
  border-radius: 999px;
  background: var(--accent-soft);
  box-shadow:
    inset 0 0 0 1px color-mix(in srgb, var(--accent) 42%, transparent),
    0 4px 12px -6px color-mix(in srgb, var(--accent) 28%, transparent);
  pointer-events: none;
  opacity: 0;
  transition:
    transform 0.36s cubic-bezier(0.22, 1, 0.36, 1),
    width 0.36s cubic-bezier(0.22, 1, 0.36, 1),
    height 0.36s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.2s ease;
}

.nav-active-glider.is-ready {
  opacity: 1;
}

.nav-active-glider.is-instant {
  transition: opacity 0.2s ease;
}

.nav-pill {
  position: relative;
  z-index: 1;
  padding: 6px 13px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  white-space: nowrap;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.nav-pill:hover {
  color: var(--accent);
  background: color-mix(in srgb, var(--bg-subtle) 85%, transparent);
}

.nav-pill--tab.active {
  color: var(--accent);
  background: transparent;
}

.nav-pill--admin {
  border: none;
  box-shadow: inset 0 0 0 1px var(--border-subtle);
}

.nav-search-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: none;
  background: var(--bg-subtle);
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  cursor: pointer;
}

.nav-search-btn:hover {
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent) 45%, transparent);
}

.nav-search-btn.is-morph-hidden {
  opacity: 0 !important;
  pointer-events: none;
  transition: opacity 0.08s linear;
}

.nav-search-icon {
  width: 14px;
  height: 14px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.nav-search-kbd {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10.5px;
  padding: 1px 5px;
  border-radius: 5px;
  background: var(--bg-glass-strong);
  color: var(--text-muted);
}

.main-content {
  position: relative;
  z-index: 1;
  flex: 1;
  width: 100%;
  padding-top: 20px;
  padding-bottom: 44px;
}

.route-stage {
  position: relative;
}

.route-stage.is-stage-transitioning {
  overflow: clip;
  transition: min-height 0.36s cubic-bezier(0.22, 1, 0.36, 1);
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
  border-radius: var(--radius-card);
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
  corner-shape: round;
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

@media (max-width: 1400px) {
  .navbar-wrap {
    padding-right: 68px;
  }
}

@media (max-width: 820px) {
  .brand-sub {
    display: none;
  }
}

@media (max-width: 640px) {
  .navbar-wrap {
    padding-right: 44px;
  }

  .navbar {
    padding: 8px 10px;
    gap: 6px;
  }

  .nav-links {
    width: 100%;
    flex-wrap: wrap;
    justify-content: flex-start;
    gap: 4px;
  }

  .nav-pill {
    padding: 4px 8px;
    font-size: 12.5px;
  }

  .nav-search-kbd {
    display: none;
  }
}
</style>
