<template>
  <Teleport to="body">
    <div v-if="rendered" class="cmd-root">
      <!-- 背景磨砂遮罩层（独立平滑淡入淡出） -->
      <div
        class="cmd-backdrop"
        :class="{ 'is-visible': backdropVisible, 'is-interactive': modelValue }"
        @click="requestClose()"
      >
        <div
          ref="panelRef"
          class="cmd-panel card"
          :class="{ 'is-interactive': panelInteractive }"
          role="dialog"
          aria-modal="true"
          aria-label="全站快捷搜索"
          @click.stop
        >
          <div class="cmd-search-bar">
            <svg class="cmd-search-icon" viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="11" cy="11" r="7" />
              <path d="M20 20l-3.5-3.5" />
            </svg>
            <input
              ref="inputRef"
              v-model="query"
              type="text"
              class="cmd-input"
              placeholder="搜索文章正文、学习路径导图节点或归档年月..."
              @keydown="onInputKeydown"
            />
            <button
              v-if="query"
              type="button"
              class="cmd-clear-btn"
              title="清空"
              @click="query = ''"
            >
              清空
            </button>
            <kbd class="cmd-esc-hint" @click="requestClose()">ESC</kbd>
          </div>

          <div ref="listRef" class="cmd-results-body">
            <div v-if="loading && !hasAnyHits" class="cmd-empty-state">
              正在检索站内文章与知识图谱...
            </div>

            <template v-else-if="flatItems.length">
              <div
                v-for="group in groupedItems"
                :key="group.key"
                class="cmd-group"
              >
                <div class="cmd-group-title">{{ group.label }}</div>
                <button
                  v-for="item in group.items"
                  :key="item.uid"
                  type="button"
                  class="cmd-item"
                  :class="{ 'is-active': item.flatIndex === activeIndex }"
                  :data-cmd-idx="item.flatIndex"
                  @mouseenter="activeIndex = item.flatIndex"
                  @click="activateItem(item)"
                >
                  <span class="cmd-item-badge" :class="`badge--${item.kind}`">
                    {{ item.badge }}
                  </span>
                  <div class="cmd-item-main">
                    <div class="cmd-item-title-row">
                      <span class="cmd-item-title">{{ item.title }}</span>
                      <span v-if="item.meta" class="cmd-item-meta">{{ item.meta }}</span>
                    </div>
                    <p v-if="item.sub" class="cmd-item-sub">{{ item.sub }}</p>
                  </div>
                  <span class="cmd-item-enter">前往</span>
                </button>
              </div>
            </template>

            <div v-else class="cmd-empty-state">
              <div>未找到与 "{{ query }}" 匹配的文章或导图节点</div>
              <div class="cmd-suggest-row">
                <button
                  v-for="kw in QUICK_SUGGESTIONS"
                  :key="kw"
                  type="button"
                  class="cmd-suggest-chip"
                  @click="query = kw"
                >
                  试试 {{ kw }}
                </button>
              </div>
            </div>
          </div>

          <div class="cmd-footer">
            <div class="cmd-footer-keys">
              <span><kbd>↑</kbd><kbd>↓</kbd> 切换</span>
              <span><kbd>↵</kbd> 跳转</span>
              <span><kbd>ESC</kbd> 关闭</span>
            </div>
            <span class="cmd-footer-note">MyBlog Quick Search</span>
          </div>
        </div>
      </div>

      <!-- 共享容器形变过渡层：从导航栏搜索胶囊原位展开，关闭/跳转时原路飞回 -->
      <div
        v-if="morph"
        ref="morphShellRef"
        :key="morph.key"
        class="cmd-morph-shell"
        :class="[
          `is-${morph.phase}`,
          `is-${morph.stage}`,
          {
            'is-handing-off': morph.handingOff,
            'is-pill-shape': morphCurrentRect.isPill,
          },
        ]"
        :style="morphShellStyle"
        aria-hidden="true"
      >
        <div
          ref="pillCloneHostRef"
          class="cmd-morph-face cmd-morph-pill-face"
          :class="[`is-${morph.phase}`, `is-${morph.stage}`]"
        />
        <div
          ref="panelCloneHostRef"
          class="cmd-morph-face cmd-morph-panel-face"
          :class="[`is-${morph.phase}`, `is-${morph.stage}`]"
        />
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from "vue";
import { useRouter } from "vue-router";
import {
  blogApi,
  getArticleRoute,
  type BlogSearchResult,
} from "@/api/blog";
import {
  prefersReducedMotion,
  supportsSquircleCorner,
} from "@/composables/useArticleTransition";

interface PaletteItem {
  uid: string;
  kind: "article" | "path" | "archive" | "nav";
  badge: string;
  title: string;
  sub?: string;
  meta?: string;
  to: string;
  flatIndex: number;
}

interface SearchMorphBox {
  left: number;
  top: number;
  width: number;
  height: number;
  borderRadius: string;
  squircle: boolean;
  isPill: boolean;
}

interface SearchCloneSnapshot {
  node: HTMLElement;
  width: number;
  height: number;
  scrollTop: number;
}

interface SearchActiveMorph {
  key: number;
  phase: "enter" | "leave";
  stage: "init" | "run";
  handingOff: boolean;
  from: SearchMorphBox;
  to: SearchMorphBox;
  pillSnap: SearchCloneSnapshot | null;
  panelSnap: SearchCloneSnapshot | null;
}

const MORPH_MS = 360;

const props = defineProps<{
  modelValue: boolean;
  triggerEl?: HTMLElement | null;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void;
  (e: "origin-hidden", hidden: boolean): void;
}>();

const router = useRouter();
const QUICK_SUGGESTIONS = ["Spring", "Redis", "Docker", "MySQL"];
const QUICK_PAGES = [
  { title: "博客首页", sub: "浏览全部最新文章与热门精选", to: "/", badge: "页面", meta: "/" },
  { title: "学习路径思维导图", sub: "交互式结构化知识体系与资源挂载", to: "/paths", badge: "页面", meta: "/paths" },
  { title: "文章时间轴归档", sub: "按年份与月份脉络回溯技术沉淀", to: "/archives", badge: "页面", meta: "/archives" },
  { title: "友情链接", sub: "精选技术博客与常用参考站点", to: "/links", badge: "页面", meta: "/links" },
  { title: "关于本站", sub: "站点架构说明与博主简介", to: "/about", badge: "页面", meta: "/about" },
];

const panelRef = ref<HTMLElement | null>(null);
const inputRef = ref<HTMLInputElement | null>(null);
const listRef = ref<HTMLElement | null>(null);
const morphShellRef = ref<HTMLElement | null>(null);
const pillCloneHostRef = ref<HTMLElement | null>(null);
const panelCloneHostRef = ref<HTMLElement | null>(null);

const rendered = ref(false);
const backdropVisible = ref(false);
const panelInteractive = ref(false);
const morph = ref<SearchActiveMorph | null>(null);

const query = ref("");
const loading = ref(false);
const activeIndex = ref(0);
const searchResult = ref<BlogSearchResult>({
  articles: [],
  paths: [],
  archives: [],
});

let debounceTimer: number | null = null;
let searchSeq = 0;
let morphSeq = 0;
let rafA: number | null = null;
let rafB: number | null = null;
let handoffRaf: number | null = null;
let revealTimer: number | null = null;
let cleanupTimer: number | null = null;
let squircleSupportedCache: boolean | null = null;

function isSquircleSupported() {
  if (squircleSupportedCache === null) {
    squircleSupportedCache = supportsSquircleCorner();
  }
  return squircleSupportedCache;
}

const morphCurrentRect = computed<SearchMorphBox>(() => {
  const m = morph.value;
  if (!m) {
    return {
      left: 0,
      top: 0,
      width: 0,
      height: 0,
      borderRadius: "20px",
      squircle: false,
      isPill: false,
    };
  }
  return m.stage === "run" ? m.to : m.from;
});

const morphShellStyle = computed(() => {
  const rect = morphCurrentRect.value;
  return {
    left: `${rect.left}px`,
    top: `${rect.top}px`,
    width: `${rect.width}px`,
    height: `${rect.height}px`,
    borderRadius: rect.borderRadius,
    cornerShape: rect.squircle ? "squircle" : "round",
  };
});

const flatItems = computed<PaletteItem[]>(() => {
  const list: PaletteItem[] = [];
  const kw = query.value.trim().toLowerCase();
  let idx = 0;

  for (const art of searchResult.value.articles || []) {
    list.push({
      uid: `art-${art.id}`,
      kind: "article",
      badge: "文章",
      title: art.title,
      sub: art.snippet || undefined,
      meta: [art.categoryName, formatDate(art.publishTime)].filter(Boolean).join(" · "),
      to: getArticleRoute({ id: art.id, slug: art.slug }),
      flatIndex: idx++,
    });
  }

  for (const p of searchResult.value.paths || []) {
    const baseRoute = p.slug ? `/paths/${encodeURIComponent(p.slug)}` : `/paths/${p.id}`;
    const to = p.matchedNodeId
      ? `${baseRoute}?node=${encodeURIComponent(p.matchedNodeId)}`
      : baseRoute;
    list.push({
      uid: `path-${p.id}-${p.matchedNodeId || "root"}`,
      kind: "path",
      badge: p.matchedNodeLabel ? "节点" : "路径",
      title: p.matchedNodeLabel ? `${p.title} / ${p.matchedNodeLabel}` : p.title,
      sub: p.matchedNodeLabel ? `聚焦导图节点「${p.matchedNodeLabel}」` : p.summary || "打开交互式思维导图",
      meta: "思维导图",
      to,
      flatIndex: idx++,
    });
  }

  for (const arc of searchResult.value.archives || []) {
    list.push({
      uid: `arc-${arc.yearMonth}`,
      kind: "archive",
      badge: "归档",
      title: `${arc.yearMonth} 技术归档`,
      sub: `查看 ${arc.yearMonth} 发布的 ${arc.count} 篇文章`,
      meta: `${arc.count} 篇`,
      to: `/archives#${arc.yearMonth}`,
      flatIndex: idx++,
    });
  }

  if (!kw || QUICK_PAGES.some((page) => page.title.toLowerCase().includes(kw))) {
    for (const page of QUICK_PAGES) {
      if (kw && !page.title.toLowerCase().includes(kw) && !page.sub.toLowerCase().includes(kw)) {
        continue;
      }
      list.push({
        uid: `nav-${page.to}`,
        kind: "nav",
        badge: page.badge,
        title: page.title,
        sub: page.sub,
        meta: page.meta,
        to: page.to,
        flatIndex: idx++,
      });
    }
  }

  return list;
});

const groupedItems = computed(() => {
  const groups: Array<{ key: string; label: string; items: PaletteItem[] }> = [];
  const definitions: Array<{ key: PaletteItem["kind"]; label: string }> = [
    { key: "article", label: "文章" },
    { key: "path", label: "学习路径 & 导图节点" },
    { key: "archive", label: "时间轴归档" },
    { key: "nav", label: "快捷导航" },
  ];
  for (const def of definitions) {
    const items = flatItems.value.filter((item) => item.kind === def.key);
    if (items.length) {
      groups.push({ key: def.key, label: def.label, items });
    }
  }
  return groups;
});

const hasAnyHits = computed(() => flatItems.value.length > 0);

function formatDate(raw?: string) {
  return raw ? raw.slice(0, 10) : "";
}

function clearMorphTimers() {
  if (rafA !== null) {
    window.cancelAnimationFrame(rafA);
    rafA = null;
  }
  if (rafB !== null) {
    window.cancelAnimationFrame(rafB);
    rafB = null;
  }
  if (handoffRaf !== null) {
    window.cancelAnimationFrame(handoffRaf);
    handoffRaf = null;
  }
  if (revealTimer !== null) {
    window.clearTimeout(revealTimer);
    revealTimer = null;
  }
  if (cleanupTimer !== null) {
    window.clearTimeout(cleanupTimer);
    cleanupTimer = null;
  }
}

function measurePillBox(el: HTMLElement | null | undefined): SearchMorphBox | null {
  if (!el) return null;
  const rect = el.getBoundingClientRect();
  if (rect.width < 8 || rect.height < 8) return null;
  const style = window.getComputedStyle(el);
  return {
    left: rect.left,
    top: rect.top,
    width: rect.width,
    height: rect.height,
    borderRadius: style.borderRadius || "999px",
    squircle: false,
    isPill: true,
  };
}

function measurePanelBox(el: HTMLElement): SearchMorphBox {
  const rect = el.getBoundingClientRect();
  const style = window.getComputedStyle(el);
  return {
    left: rect.left,
    top: rect.top,
    width: rect.width,
    height: rect.height,
    borderRadius: style.borderRadius || "20px",
    squircle: isSquircleSupported(),
    isPill: false,
  };
}

function measureInFlightBox(el: HTMLElement, isPill = false): SearchMorphBox {
  const rect = el.getBoundingClientRect();
  const style = window.getComputedStyle(el);
  return {
    left: rect.left,
    top: rect.top,
    width: rect.width,
    height: rect.height,
    borderRadius: style.borderRadius || "20px",
    squircle: !isPill && isSquircleSupported(),
    isPill,
  };
}

function sanitizeSearchClone(el: HTMLElement, preserveScrollTop = 0): SearchCloneSnapshot {
  const rect = el.getBoundingClientRect();
  const clone = el.cloneNode(true) as HTMLElement;
  clone.removeAttribute("id");
  clone.querySelectorAll("[id]").forEach((node) => node.removeAttribute("id"));
  clone.classList.remove("is-morph-hidden", "is-interactive");
  clone.style.animation = "none";
  clone.style.transition = "none";
  clone.style.opacity = "1";
  clone.style.visibility = "visible";

  // 同步当前输入文本与列表滚动位置，确保收起瞬间零闪烁
  const clonedInput = clone.querySelector<HTMLInputElement>(".cmd-input");
  if (clonedInput) {
    clonedInput.value = query.value;
    clonedInput.setAttribute("value", query.value);
  }
  return {
    node: clone,
    width: rect.width,
    height: rect.height,
    scrollTop: preserveScrollTop,
  };
}

function mountClone(
  host: HTMLElement | null,
  snap: SearchCloneSnapshot | null,
  kind: "pill" | "panel",
) {
  if (!host) return;
  host.innerHTML = "";
  if (!snap) return;

  const node = snap.node.cloneNode(true) as HTMLElement;
  node.style.margin = "0";
  node.style.boxSizing = "border-box";
  node.style.flexShrink = "0";
  node.style.border = "none";
  node.style.boxShadow = "none";
  node.style.background = "transparent";
  node.style.width = `${snap.width}px`;
  node.style.height = `${snap.height}px`;
  if (kind === "panel") {
    node.style.borderRadius = "0";
  }

  host.appendChild(node);
  if (kind === "panel" && snap.scrollTop > 0) {
    const clonedBody = node.querySelector<HTMLElement>(".cmd-results-body");
    if (clonedBody) {
      clonedBody.scrollTop = snap.scrollTop;
    }
  }
}

watch(
  () => [morph.value?.key, morph.value?.panelSnap] as const,
  () => {
    const m = morph.value;
    if (!m) return;
    mountClone(pillCloneHostRef.value, m.pillSnap, "pill");
    mountClone(panelCloneHostRef.value, m.panelSnap, "panel");
  },
  { flush: "post" },
);

async function startEnterMorph() {
  clearMorphTimers();

  const trigger = props.triggerEl ?? null;
  const midFlightFrom = morphShellRef.value
    ? measureInFlightBox(morphShellRef.value, false)
    : null;
  const triggerBox = measurePillBox(trigger);
  const pillSnap = trigger ? sanitizeSearchClone(trigger, 0) : null;

  rendered.value = true;
  activeIndex.value = 0;
  void runSearch(query.value);

  if (prefersReducedMotion() || !triggerBox) {
    morph.value = null;
    backdropVisible.value = true;
    panelInteractive.value = true;
    emit("origin-hidden", false);
    await nextTick();
    inputRef.value?.focus();
    inputRef.value?.select();
    return;
  }

  panelInteractive.value = false;
  backdropVisible.value = false;
  emit("origin-hidden", true);

  await nextTick();
  const panelEl = panelRef.value;
  if (!panelEl) return;

  const toBox = measurePanelBox(panelEl);
  const panelSnap = sanitizeSearchClone(panelEl, 0);
  const fromBox = midFlightFrom ?? triggerBox;

  morph.value = {
    key: ++morphSeq,
    phase: "enter",
    stage: "init",
    handingOff: false,
    from: fromBox,
    to: toBox,
    pillSnap,
    panelSnap,
  };

  rafA = window.requestAnimationFrame(() => {
    backdropVisible.value = true;
    rafB = window.requestAnimationFrame(() => {
      if (morph.value && morph.value.phase === "enter") {
        morph.value = { ...morph.value, stage: "run" };
      }
    });
  });

  revealTimer = window.setTimeout(() => {
    panelInteractive.value = true;
    void nextTick(() => {
      inputRef.value?.focus();
      inputRef.value?.select();
    });
    handoffRaf = window.requestAnimationFrame(() => {
      handoffRaf = null;
      if (morph.value) {
        morph.value = { ...morph.value, handingOff: true };
      }
    });
  }, MORPH_MS);

  cleanupTimer = window.setTimeout(() => {
    morph.value = null;
  }, MORPH_MS + 70);
}

async function startLeaveMorph() {
  clearMorphTimers();

  const trigger = props.triggerEl ?? null;
  const livePanel = panelRef.value;
  const midFlight = morphShellRef.value;

  if (prefersReducedMotion() || !trigger || !rendered.value) {
    morph.value = null;
    backdropVisible.value = false;
    panelInteractive.value = false;
    rendered.value = false;
    emit("origin-hidden", false);
    return;
  }

  const fromBox =
    morph.value && midFlight
      ? measureInFlightBox(midFlight, false)
      : livePanel
        ? measurePanelBox(livePanel)
        : null;

  if (!fromBox) {
    morph.value = null;
    backdropVisible.value = false;
    panelInteractive.value = false;
    rendered.value = false;
    emit("origin-hidden", false);
    return;
  }

  const panelSnap = livePanel
    ? sanitizeSearchClone(livePanel, listRef.value?.scrollTop ?? 0)
    : morph.value?.panelSnap ?? null;
  const pillSnap = sanitizeSearchClone(trigger, 0);

  panelInteractive.value = false;
  emit("origin-hidden", true);

  // 等待一次布局刷新（兼容点击搜索结果发起路由跳转后的导航栏目标位置测量）
  await nextTick();
  const toBox = measurePillBox(trigger);
  if (!toBox) {
    morph.value = null;
    backdropVisible.value = false;
    rendered.value = false;
    emit("origin-hidden", false);
    return;
  }

  morph.value = {
    key: ++morphSeq,
    phase: "leave",
    stage: "init",
    handingOff: false,
    from: fromBox,
    to: toBox,
    pillSnap,
    panelSnap,
  };

  rafA = window.requestAnimationFrame(() => {
    backdropVisible.value = false;
    rafB = window.requestAnimationFrame(() => {
      // 再次校准导航栏目标胶囊坐标，确保页面滚动回顶部后精准归位
      const latestTo = measurePillBox(trigger);
      if (morph.value && morph.value.phase === "leave") {
        morph.value = {
          ...morph.value,
          to: latestTo ?? morph.value.to,
          stage: "run",
        };
      }
    });
  });

  revealTimer = window.setTimeout(() => {
    emit("origin-hidden", false);
    handoffRaf = window.requestAnimationFrame(() => {
      handoffRaf = null;
      if (morph.value) {
        morph.value = { ...morph.value, handingOff: true };
      }
    });
  }, MORPH_MS);

  cleanupTimer = window.setTimeout(() => {
    morph.value = null;
    rendered.value = false;
    emit("origin-hidden", false);
  }, MORPH_MS + 70);
}

function requestClose() {
  if (!props.modelValue) return;
  emit("update:modelValue", false);
}

function activateItem(item: PaletteItem) {
  void router.push(item.to);
  requestClose();
}

async function runSearch(kw: string) {
  const seq = ++searchSeq;
  loading.value = true;
  try {
    const res = await blogApi.searchPublic(kw, 8);
    if (seq === searchSeq) {
      searchResult.value = res || { articles: [], paths: [], archives: [] };
      activeIndex.value = 0;

      // 若仍在展开动画期间结果返回，同步重测终态尺寸避免归位瞬跳
      if (morph.value && morph.value.phase === "enter") {
        await nextTick();
        if (panelRef.value && morph.value && morph.value.phase === "enter") {
          morph.value = {
            ...morph.value,
            to: measurePanelBox(panelRef.value),
            panelSnap: sanitizeSearchClone(panelRef.value, listRef.value?.scrollTop ?? 0),
          };
        }
      }
    }
  } catch {
    if (seq === searchSeq) {
      searchResult.value = { articles: [], paths: [], archives: [] };
    }
  } finally {
    if (seq === searchSeq) {
      loading.value = false;
    }
  }
}

function scrollActiveIntoView() {
  void nextTick(() => {
    const activeEl = listRef.value?.querySelector<HTMLElement>(`[data-cmd-idx="${activeIndex.value}"]`);
    activeEl?.scrollIntoView({ block: "nearest" });
  });
}

function onInputKeydown(e: KeyboardEvent) {
  if (e.key === "Escape") {
    e.preventDefault();
    requestClose();
    return;
  }
  const total = flatItems.value.length;
  if (!total) return;

  if (e.key === "ArrowDown") {
    e.preventDefault();
    activeIndex.value = (activeIndex.value + 1) % total;
    scrollActiveIntoView();
  } else if (e.key === "ArrowUp") {
    e.preventDefault();
    activeIndex.value = (activeIndex.value - 1 + total) % total;
    scrollActiveIntoView();
  } else if (e.key === "Enter") {
    e.preventDefault();
    const chosen = flatItems.value[activeIndex.value];
    if (chosen) {
      activateItem(chosen);
    }
  }
}

function handleModalEsc(e: KeyboardEvent) {
  if (props.modelValue && e.key === "Escape") {
    e.preventDefault();
    requestClose();
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      window.addEventListener("keydown", handleModalEsc);
      void startEnterMorph();
    } else {
      window.removeEventListener("keydown", handleModalEsc);
      void startLeaveMorph();
    }
  },
);

watch(query, (val) => {
  if (!props.modelValue) return;
  if (debounceTimer !== null) {
    window.clearTimeout(debounceTimer);
  }
  debounceTimer = window.setTimeout(() => {
    void runSearch(val);
  }, 160);
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", handleModalEsc);
  clearMorphTimers();
  emit("origin-hidden", false);
  if (debounceTimer !== null) {
    window.clearTimeout(debounceTimer);
  }
});
</script>

<style scoped>
.cmd-root {
  position: fixed;
  inset: 0;
  z-index: 120;
  pointer-events: none;
}

.cmd-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(9, 13, 20, 0.52);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: min(12vh, 92px) 14px 20px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}

.cmd-backdrop.is-visible {
  opacity: 1;
}

.cmd-backdrop.is-interactive {
  pointer-events: auto;
}

.cmd-panel {
  width: min(640px, 100%);
  max-height: min(74vh, 620px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-glass-strong);
  box-shadow:
    0 28px 64px -16px rgba(15, 23, 42, 0.34),
    var(--specular-inset);
  opacity: 0;
  pointer-events: none;
}

.cmd-panel.is-interactive {
  opacity: 1;
  pointer-events: auto;
}

/* 共享元素容器形变壳（从导航栏胶囊 -> 搜索面板 -> 原路返回胶囊） */
.cmd-morph-shell {
  position: fixed;
  z-index: 125;
  overflow: hidden;
  pointer-events: none;
  background-color: var(--bg-glass-strong);
  border: none;
  box-shadow:
    0 28px 64px -16px rgba(15, 23, 42, 0.34),
    inset 0 0 0 1px var(--border-glass),
    var(--specular-inset);
  contain: layout style paint;
  will-change: left, top, width, height, border-radius, opacity;
  opacity: 1;
  transition:
    left 0.36s cubic-bezier(0.22, 1, 0.36, 1),
    top 0.36s cubic-bezier(0.22, 1, 0.36, 1),
    width 0.36s cubic-bezier(0.22, 1, 0.36, 1),
    height 0.36s cubic-bezier(0.22, 1, 0.36, 1),
    border-radius 0.36s cubic-bezier(0.22, 1, 0.36, 1),
    background-color 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.06s linear;
}

.cmd-morph-shell.is-pill-shape {
  background-color: var(--bg-subtle);
  box-shadow:
    inset 0 0 0 1px var(--border-subtle),
    0 6px 18px -8px rgba(15, 23, 42, 0.16);
}

.cmd-morph-shell.is-handing-off {
  opacity: 0;
}

.cmd-morph-face {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.cmd-morph-pill-face {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 展开动画：胶囊图标文字柔和消隐，搜索面板同步浮现 */
.cmd-morph-pill-face.is-enter {
  z-index: 1;
  opacity: 1;
  transition: opacity 0.16s ease 0.02s;
}

.cmd-morph-pill-face.is-enter.is-run {
  opacity: 0;
}

.cmd-morph-panel-face.is-enter {
  z-index: 2;
  opacity: 0;
  transition: opacity 0.22s cubic-bezier(0.22, 1, 0.36, 1) 0.04s;
}

.cmd-morph-panel-face.is-enter.is-run {
  opacity: 1;
}

/* 收起动画：搜索面板柔和淡出，导航栏搜索胶囊同步恢复 */
.cmd-morph-panel-face.is-leave {
  z-index: 1;
  opacity: 1;
  transition: opacity 0.18s ease 0.02s;
}

.cmd-morph-panel-face.is-leave.is-run {
  opacity: 0;
}

.cmd-morph-pill-face.is-leave {
  z-index: 2;
  opacity: 0;
  transition: opacity 0.2s cubic-bezier(0.22, 1, 0.36, 1) 0.08s;
}

.cmd-morph-pill-face.is-leave.is-run {
  opacity: 1;
}

.cmd-search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.cmd-search-icon {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: var(--accent);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
  flex-shrink: 0;
}

.cmd-input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 15px;
  outline: none;
}

.cmd-input::placeholder {
  color: var(--text-muted);
}

.cmd-clear-btn {
  border: none;
  background: var(--bg-subtle);
  color: var(--text-muted);
  border-radius: 6px;
  padding: 3px 8px;
  font-size: 11.5px;
  cursor: pointer;
}

.cmd-clear-btn:hover {
  color: var(--text-primary);
}

.cmd-esc-hint {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: var(--text-muted);
  background: var(--bg-subtle);
  border-radius: 6px;
  padding: 2px 7px;
  cursor: pointer;
  flex-shrink: 0;
}

.cmd-results-body {
  flex: 1;
  overflow-y: auto;
  padding: 10px 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cmd-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cmd-group-title {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--text-muted);
  padding: 2px 10px;
}

.cmd-item {
  width: 100%;
  border: none;
  background: transparent;
  padding: 9px 10px;
  border-radius: var(--radius-control);
  display: flex;
  align-items: flex-start;
  gap: 10px;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.14s ease, transform 0.14s ease;
}

.cmd-item.is-active {
  background: var(--accent-soft);
}

.cmd-item-badge {
  flex-shrink: 0;
  margin-top: 2px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  background: var(--bg-subtle);
  color: var(--text-secondary);
}

.badge--article {
  background: rgba(13, 148, 136, 0.12);
  color: var(--accent);
}

.badge--path {
  background: rgba(2, 132, 199, 0.12);
  color: var(--accent-2);
}

.badge--archive {
  background: rgba(217, 119, 6, 0.12);
  color: #d97706;
}

.cmd-item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cmd-item-title-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.cmd-item-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cmd-item.is-active .cmd-item-title {
  color: var(--accent);
}

.cmd-item-meta {
  flex-shrink: 0;
  font-size: 11.5px;
  color: var(--text-muted);
}

.cmd-item-sub {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-muted);
  line-height: 1.45;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.cmd-item-enter {
  flex-shrink: 0;
  align-self: center;
  font-size: 11.5px;
  color: var(--accent);
  opacity: 0;
  transition: opacity 0.14s ease;
}

.cmd-item.is-active .cmd-item-enter {
  opacity: 1;
}

.cmd-empty-state {
  padding: 36px 16px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13.5px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.cmd-suggest-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
}

.cmd-suggest-chip {
  border: none;
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  border-radius: 999px;
  padding: 4px 11px;
  font-size: 12px;
  cursor: pointer;
}

.cmd-suggest-chip:hover {
  color: var(--accent);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent) 45%, transparent);
}

.cmd-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 16px;
  border-top: 1px solid var(--border-subtle);
  font-size: 11.5px;
  color: var(--text-muted);
}

.cmd-footer-keys {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cmd-footer-keys kbd {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10.5px;
  padding: 1px 5px;
  border-radius: 4px;
  background: var(--bg-subtle);
  margin-right: 3px;
}

.cmd-footer-note {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10.5px;
}

@media (max-width: 640px) {
  .cmd-backdrop {
    padding: 56px 10px 14px;
  }

  .cmd-footer-note {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .cmd-backdrop,
  .cmd-morph-shell,
  .cmd-morph-face {
    transition: none !important;
  }
}
</style>
