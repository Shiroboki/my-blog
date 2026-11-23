<template>
  <section class="archives-page">
    <!-- 顶部归档总览与筛选控制栏 -->
    <header class="card archives-header animate-rise">
      <div class="archives-head-top">
        <div class="head-title-block">
          <h1 class="page-heading">文章归档</h1>
        </div>

        <div class="archive-filter-bar">
          <div class="search-box">
            <svg viewBox="0 0 16 16" class="search-icon" aria-hidden="true">
              <circle cx="7" cy="7" r="4.5" />
              <line x1="10.4" y1="10.4" x2="13.8" y2="13.8" />
            </svg>
            <input
              v-model="keyword"
              type="text"
              class="archive-search-input"
              placeholder="筛选标题或分类..."
              aria-label="筛选归档文章"
            />
            <button
              v-if="keyword"
              type="button"
              class="search-clear-btn"
              title="清空搜索"
              @click="keyword = ''"
            >
              ×
            </button>
          </div>

          <button
            v-if="filteredGroups.length > 1"
            type="button"
            class="collapse-all-btn"
            @click="toggleAllGroups"
          >
            {{ allExpanded ? "全部收起" : "全部展开" }}
          </button>

          <button
            v-if="isFiltered"
            type="button"
            class="archive-reset-btn"
            @click="resetArchiveFilters"
          >
            重置筛选
          </button>
        </div>
      </div>

      <div v-if="availableCategories.length" class="archive-cats">
        <button
          type="button"
          class="cat-pill"
          :class="{ active: !selectedCategory }"
          @click="selectedCategory = ''"
        >
          <span>全部分类</span>
          <span class="cat-count">{{ totalArticles }}</span>
        </button>
        <button
          v-for="cat in availableCategories"
          :key="cat.name"
          type="button"
          class="cat-pill"
          :class="{ active: selectedCategory === cat.name }"
          @click="selectedCategory = selectedCategory === cat.name ? '' : cat.name"
        >
          <span>{{ cat.name }}</span>
          <span class="cat-count">{{ cat.count }}</span>
        </button>
      </div>
    </header>

    <!-- 加载/空状态 -->
    <div v-if="loading" class="card state-card">
      <div class="state-pulse" aria-hidden="true" />
      <span>正在梳理时间轴归档...</span>
    </div>

    <div v-else-if="!filteredGroups.length" class="card state-card">
      <p class="state-title">
        {{ groups.length ? "当前筛选条件下没有匹配的文章" : "暂无已发布文章" }}
      </p>
      <button v-if="isFiltered" type="button" class="btn btn-primary" @click="resetArchiveFilters">
        查看全部归档
      </button>
    </div>

    <!-- 主体：左侧吸顶时间轴刻度轨 + 右侧连贯竖向时间线 -->
    <div v-else class="archive-layout">
      <!-- 左侧时间轴导览轨 -->
      <aside class="rail-aside" aria-label="归档时间轴导航">
        <div class="card rail-card">
          <div ref="railListRef" class="rail-timeline">
            <!-- 贯穿轨道底层细线 + 随进度滑动的渐变高亮线 -->
            <div class="rail-track" aria-hidden="true">
              <div
                class="rail-track-fill"
                :style="{ height: `${railProgressHeight}px` }"
              />
            </div>

            <!-- 平滑跟随激活月份的高亮背景游标 -->
            <div
              v-show="indicatorBox.visible"
              class="rail-active-slider"
              :style="{
                transform: `translate3d(0, ${indicatorBox.top}px, 0)`,
                height: `${indicatorBox.height}px`,
              }"
              aria-hidden="true"
            />

            <div v-for="sec in yearSections" :key="sec.year" class="rail-year-block">
              <button
                type="button"
                class="rail-year-btn"
                :class="{ 'is-active-year': activeYear === sec.year }"
                @click="scrollToMonth(sec.months[0].yearMonth)"
              >
                <span class="rail-year-diamond" aria-hidden="true" />
                <span class="rail-year-text">{{ sec.year }}</span>
                <span class="rail-year-count">{{ sec.count }} 篇</span>
              </button>

              <div class="rail-months-wrap">
                <button
                  v-for="m in sec.months"
                  :key="m.yearMonth"
                  :ref="(el) => setRailMonthRef(m.yearMonth, el as HTMLElement | null)"
                  type="button"
                  class="rail-month-btn"
                  :class="{
                    'is-active': activeYearMonth === m.yearMonth,
                    'is-hovered': hoveredYearMonth === m.yearMonth,
                  }"
                  @click="scrollToMonth(m.yearMonth)"
                >
                  <span class="rail-node-dot" aria-hidden="true" />
                  <span class="rail-month-name">{{ m.monthLabel }}</span>
                  <span class="rail-month-bar" aria-hidden="true">
                    <span
                      class="rail-month-bar-fill"
                      :style="{ width: `${Math.max(18, Math.round((m.count / maxMonthCount) * 100))}%` }"
                    />
                  </span>
                  <span class="rail-month-count">{{ m.count }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 右侧时间线文章流 -->
      <div class="card stream-card">
        <div class="stream-inner">
          <!-- 贯穿左侧缘的竖向时间主轴脊柱线 -->
          <div class="stream-spine" aria-hidden="true" />

          <TransitionGroup name="timeline-group" tag="div" class="timeline-groups">
            <section
              v-for="(group, gIdx) in filteredGroups"
              :key="group.yearMonth"
              :ref="(el) => setGroupSectionRef(group.yearMonth, el as HTMLElement | null)"
              :data-year-month="group.yearMonth"
              class="month-section"
              :class="{
                'is-active-group': activeYearMonth === group.yearMonth,
                'is-collapsed': collapsedMonths.has(group.yearMonth),
              }"
              :style="{ '--group-stagger': gIdx }"
            >
              <!-- 月份里程碑节点头部 -->
              <button
                type="button"
                class="month-milestone"
                :aria-expanded="!collapsedMonths.has(group.yearMonth)"
                @click="toggleMonthCollapse(group.yearMonth)"
              >
                <span class="milestone-ring" aria-hidden="true">
                  <span class="milestone-core" />
                </span>

                <span class="milestone-ym">{{ formatYearMonthLabel(group.yearMonth) }}</span>

                <span class="milestone-count">{{ group.articles.length }}</span>

                <svg viewBox="0 0 16 16" class="milestone-chevron" aria-hidden="true">
                  <polyline points="4 6 8 10 12 6" />
                </svg>
              </button>

              <!-- 丝滑风琴折叠面板（CSS Grid rows 0fr <-> 1fr 连续高度动画） -->
              <div class="month-collapse-shell">
                <div class="month-collapse-clip">
                  <TransitionGroup name="timeline-item" tag="ul" class="archive-list">
                    <li
                      v-for="(art, aIdx) in group.articles"
                      :key="art.id"
                      class="archive-row"
                      :style="{ '--item-stagger': Math.min(aIdx, 12) }"
                      @mouseenter="hoveredYearMonth = group.yearMonth"
                      @mouseleave="hoveredYearMonth = ''"
                    >
                      <span class="row-branch-dot" aria-hidden="true" />
                      <span class="row-branch-tick" aria-hidden="true" />

                      <RouterLink :to="getArticleRoute(art)" class="archive-entry-card">
                        <time class="entry-date" :datetime="art.publishTime">
                          {{ formatDayLabel(art.publishTime) }}
                        </time>

                        <span class="entry-title">
                          <template
                            v-for="(seg, sIdx) in splitHighlight(art.title, keyword)"
                            :key="sIdx"
                          >
                            <mark v-if="seg.hit" class="title-mark">{{ seg.text }}</mark>
                            <template v-else>{{ seg.text }}</template>
                          </template>
                        </span>

                        <span v-if="art.categoryName" class="entry-cat">
                          {{ art.categoryName }}
                        </span>

                        <span class="entry-arrow" aria-hidden="true">
                          <svg viewBox="0 0 16 16" class="arrow-svg">
                            <line x1="3" y1="8" x2="12.5" y2="8" />
                            <polyline points="8.5 4 12.5 8 8.5 12" />
                          </svg>
                        </span>
                      </RouterLink>
                    </li>
                  </TransitionGroup>
                </div>
              </div>
            </section>
          </TransitionGroup>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { blogApi, getArticleRoute, type BlogArchiveGroup } from "@/api/blog";
import { useSiteInfo } from "@/composables/useSiteInfo";
import { updatePageSeo } from "@/utils/seo";

interface YearSection {
  year: string;
  count: number;
  months: {
    yearMonth: string;
    monthLabel: string;
    count: number;
  }[];
}

const { loadSiteInfo } = useSiteInfo();

const loading = ref(false);
const groups = ref<BlogArchiveGroup[]>([]);
const keyword = ref("");
const selectedCategory = ref("");
const activeYearMonth = ref("");
const hoveredYearMonth = ref("");
const collapsedMonths = ref<Set<string>>(new Set());

const railListRef = ref<HTMLElement | null>(null);
const railMonthEls = new Map<string, HTMLElement>();
const groupSectionEls = new Map<string, HTMLElement>();

const indicatorBox = reactive({
  visible: false,
  top: 0,
  height: 32,
});

let scrollRafId: number | null = null;
let scrollLockUntil = 0;

function setRailMonthRef(yearMonth: string, el: HTMLElement | null) {
  if (el) {
    railMonthEls.set(yearMonth, el);
  } else {
    railMonthEls.delete(yearMonth);
  }
}

function setGroupSectionRef(yearMonth: string, el: HTMLElement | null) {
  if (el) {
    groupSectionEls.set(yearMonth, el);
  } else {
    groupSectionEls.delete(yearMonth);
  }
}

const totalArticles = computed(() =>
  groups.value.reduce((sum, g) => sum + g.articles.length, 0),
);

const availableCategories = computed(() => {
  const counts = new Map<string, number>();
  for (const group of groups.value) {
    for (const art of group.articles) {
      if (art.categoryName) {
        counts.set(art.categoryName, (counts.get(art.categoryName) || 0) + 1);
      }
    }
  }
  return Array.from(counts.entries()).map(([name, count]) => ({ name, count }));
});

const isFiltered = computed(() => Boolean(keyword.value.trim() || selectedCategory.value));

const filteredGroups = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  const cat = selectedCategory.value;
  if (!kw && !cat) return groups.value;

  return groups.value
    .map((group) => ({
      ...group,
      articles: group.articles.filter((art) => {
        if (cat && art.categoryName !== cat) return false;
        if (!kw) return true;
        return (
          art.title.toLowerCase().includes(kw) ||
          (art.categoryName || "").toLowerCase().includes(kw)
        );
      }),
    }))
    .filter((group) => group.articles.length > 0);
});

const yearSections = computed<YearSection[]>(() => {
  const map = new Map<string, YearSection>();
  for (const g of filteredGroups.value) {
    const [yearRaw, monthRaw] = g.yearMonth.split("-");
    const year = yearRaw || g.yearMonth;
    const monthNum = Number.parseInt(monthRaw || "", 10);
    const monthLabel = Number.isNaN(monthNum) ? g.yearMonth : `${monthNum} 月`;

    let sec = map.get(year);
    if (!sec) {
      sec = { year, count: 0, months: [] };
      map.set(year, sec);
    }
    sec.count += g.articles.length;
    sec.months.push({
      yearMonth: g.yearMonth,
      monthLabel,
      count: g.articles.length,
    });
  }
  return Array.from(map.values());
});

const maxMonthCount = computed(() => {
  let max = 1;
  for (const g of filteredGroups.value) {
    max = Math.max(max, g.articles.length);
  }
  return max;
});

const activeYear = computed(() => activeYearMonth.value.split("-")[0] || "");

const railProgressHeight = computed(() => {
  if (!indicatorBox.visible) return 0;
  return Math.max(0, indicatorBox.top + indicatorBox.height / 2 - 6);
});

const allExpanded = computed(() => collapsedMonths.value.size === 0);

function formatYearMonthLabel(ym: string): string {
  const [y, m] = ym.split("-");
  const monthNum = Number.parseInt(m || "", 10);
  if (!y || Number.isNaN(monthNum)) return ym;
  return `${y} 年 ${monthNum} 月`;
}

function formatDayLabel(publishTime?: string): string {
  if (!publishTime) return "-- --";
  const mmdd = publishTime.slice(5, 10);
  return mmdd || "-- --";
}

function splitHighlight(text: string, query: string): { text: string; hit: boolean }[] {
  const q = query.trim();
  if (!q) return [{ text, hit: false }];
  const lowerText = text.toLowerCase();
  const lowerQ = q.toLowerCase();
  const segments: { text: string; hit: boolean }[] = [];
  let cursor = 0;

  while (cursor < text.length) {
    const idx = lowerText.indexOf(lowerQ, cursor);
    if (idx < 0) {
      segments.push({ text: text.slice(cursor), hit: false });
      break;
    }
    if (idx > cursor) {
      segments.push({ text: text.slice(cursor, idx), hit: false });
    }
    segments.push({ text: text.slice(idx, idx + q.length), hit: true });
    cursor = idx + q.length;
  }
  return segments;
}

function syncRailIndicator() {
  const targetYm = activeYearMonth.value;
  const container = railListRef.value;
  const targetEl = targetYm ? railMonthEls.get(targetYm) : undefined;
  if (!container || !targetEl) {
    indicatorBox.visible = false;
    return;
  }
  const containerRect = container.getBoundingClientRect();
  const targetRect = targetEl.getBoundingClientRect();
  indicatorBox.top = Math.round(targetRect.top - containerRect.top);
  indicatorBox.height = Math.round(targetRect.height);
  indicatorBox.visible = true;
}

function toggleMonthCollapse(yearMonth: string) {
  const next = new Set(collapsedMonths.value);
  if (next.has(yearMonth)) {
    next.delete(yearMonth);
  } else {
    next.add(yearMonth);
  }
  collapsedMonths.value = next;
  activeYearMonth.value = yearMonth;
  void nextTick(syncRailIndicator);
}

function toggleAllGroups() {
  if (collapsedMonths.value.size === 0) {
    collapsedMonths.value = new Set(filteredGroups.value.map((g) => g.yearMonth));
  } else {
    collapsedMonths.value = new Set();
  }
  void nextTick(syncRailIndicator);
}

function scrollToMonth(yearMonth: string) {
  if (!yearMonth) return;
  if (collapsedMonths.value.has(yearMonth)) {
    const next = new Set(collapsedMonths.value);
    next.delete(yearMonth);
    collapsedMonths.value = next;
  }
  activeYearMonth.value = yearMonth;
  scrollLockUntil = Date.now() + 650;
  void nextTick(() => {
    syncRailIndicator();
    const targetEl = groupSectionEls.get(yearMonth);
    if (!targetEl) return;
    const headerOffset = 88;
    const top = Math.max(0, window.scrollY + targetEl.getBoundingClientRect().top - headerOffset);
    window.scrollTo({ top, behavior: "smooth" });
  });
}

function updateActiveFromScroll() {
  if (Date.now() < scrollLockUntil) return;
  const list = filteredGroups.value;
  if (!list.length) return;

  const anchorY = 165;
  let picked = list[0].yearMonth;

  for (const g of list) {
    const el = groupSectionEls.get(g.yearMonth);
    if (!el) continue;
    const rect = el.getBoundingClientRect();
    if (rect.top <= anchorY) {
      picked = g.yearMonth;
    } else {
      break;
    }
  }

  // 页面滚至底部时确保最后节点可被激活
  if (window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 24) {
    picked = list[list.length - 1].yearMonth;
  }

  if (picked !== activeYearMonth.value) {
    activeYearMonth.value = picked;
    void nextTick(syncRailIndicator);
  }
}

function handleWindowScroll() {
  if (scrollRafId !== null) return;
  scrollRafId = window.requestAnimationFrame(() => {
    scrollRafId = null;
    updateActiveFromScroll();
  });
}

function handleWindowResize() {
  void nextTick(syncRailIndicator);
}

function resetArchiveFilters() {
  keyword.value = "";
  selectedCategory.value = "";
}

watch(
  filteredGroups,
  async (nextList) => {
    if (!nextList.length) {
      activeYearMonth.value = "";
      indicatorBox.visible = false;
      return;
    }
    if (!nextList.some((g) => g.yearMonth === activeYearMonth.value)) {
      activeYearMonth.value = nextList[0].yearMonth;
    }
    await nextTick();
    syncRailIndicator();
  },
  { immediate: true },
);

onMounted(async () => {
  window.addEventListener("scroll", handleWindowScroll, { passive: true });
  window.addEventListener("resize", handleWindowResize, { passive: true });
  void loadSiteInfo();
  updatePageSeo({
    title: "文章归档",
    description: "按时间轴脉络浏览全部已发布技术文章与随笔记录。",
    path: "/archives",
    type: "website",
  });

  loading.value = true;
  try {
    groups.value = await blogApi.getArchives();
    if (groups.value.length) {
      activeYearMonth.value = groups.value[0].yearMonth;
      await nextTick();
      syncRailIndicator();
    }
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  window.removeEventListener("scroll", handleWindowScroll);
  window.removeEventListener("resize", handleWindowResize);
  if (scrollRafId !== null) {
    window.cancelAnimationFrame(scrollRafId);
  }
});
</script>

<style scoped>
.archives-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.archives-header {
  padding: 18px 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.archives-head-top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px 18px;
}

.page-heading {
  margin: 0;
  font-size: 21px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.archive-filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.search-box {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 11px;
  width: 13.5px;
  height: 13.5px;
  fill: none;
  stroke: var(--text-muted);
  stroke-width: 1.8;
  pointer-events: none;
}

.archive-search-input {
  width: 210px;
  padding: 7px 26px 7px 31px;
  border-radius: 999px;
  border: none;
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  transition: width 0.22s ease, box-shadow 0.2s ease;
}

.archive-search-input:focus {
  width: 244px;
  box-shadow: inset 0 0 0 1px var(--accent), 0 0 0 3px var(--accent-soft);
}

.archive-search-input::placeholder {
  color: var(--text-muted);
}

.search-clear-btn {
  position: absolute;
  right: 8px;
  width: 16px;
  height: 16px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
}

.collapse-all-btn,
.archive-reset-btn {
  border: none;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--bg-subtle);
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
}

.collapse-all-btn:hover {
  color: var(--accent);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent) 45%, transparent);
}

.archive-reset-btn {
  color: var(--accent);
  background: var(--accent-soft);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent) 35%, transparent);
}

.archive-cats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle);
}

.cat-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  border: none;
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition:
    transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.2s ease,
    color 0.2s ease,
    background-color 0.2s ease;
}

.cat-pill:hover {
  transform: translateY(-1px);
  color: var(--text-primary);
  box-shadow: inset 0 0 0 1px var(--text-muted);
}

.cat-pill.active {
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 600;
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent) 45%, transparent);
}

.cat-count {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  opacity: 0.72;
}

.state-card {
  padding: 56px 24px;
  text-align: center;
  color: var(--text-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.state-title {
  margin: 0;
  font-size: 15px;
}

.state-pulse {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--accent-soft);
  box-shadow: 0 0 0 0 color-mix(in srgb, var(--accent) 40%, transparent);
  animation: statePulse 1.4s ease-out infinite;
}

@keyframes statePulse {
  to {
    box-shadow: 0 0 0 14px transparent;
  }
}

/* 主布局：左侧时间轴刻度轨 + 右侧时间流 */
.archive-layout {
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.rail-aside {
  position: sticky;
  top: 84px;
}

.rail-card {
  padding: 16px 14px 18px;
  background: var(--bg-glass-strong);
  animation: railReveal 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes railReveal {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.rail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
  padding: 0 4px 10px;
  border-bottom: 1px solid var(--border-subtle);
}

.rail-title {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-primary);
}

.rail-range {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10.5px;
  color: var(--text-muted);
}

.rail-timeline {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rail-track {
  position: absolute;
  left: 11px;
  top: 8px;
  bottom: 8px;
  width: 2px;
  border-radius: 999px;
  background: var(--border-subtle);
  overflow: hidden;
  pointer-events: none;
}

.rail-track-fill {
  width: 100%;
  background: linear-gradient(180deg, var(--accent), var(--accent-2));
  border-radius: 999px;
  transition: height 0.38s cubic-bezier(0.22, 1, 0.36, 1);
}

.rail-active-slider {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  border-radius: var(--radius-control);
  background: var(--accent-soft);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent) 28%, transparent);
  pointer-events: none;
  transition:
    transform 0.36s cubic-bezier(0.22, 1, 0.36, 1),
    height 0.28s cubic-bezier(0.22, 1, 0.36, 1);
  z-index: 0;
}

.rail-year-block {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.rail-year-btn {
  width: 100%;
  border: none;
  background: transparent;
  padding: 4px 6px 4px 5px;
  display: flex;
  align-items: center;
  gap: 9px;
  cursor: pointer;
  text-align: left;
  color: var(--text-secondary);
  transition: color 0.18s ease;
}

.rail-year-diamond {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  transform: rotate(45deg) scale(0.84);
  background: var(--bg-glass-strong);
  box-shadow: inset 0 0 0 2px var(--border-glass);
  flex-shrink: 0;
  transition: box-shadow 0.22s ease, transform 0.22s ease, background-color 0.22s ease;
}

.rail-year-btn.is-active-year .rail-year-diamond {
  background: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
  transform: rotate(45deg) scale(0.95);
}

.rail-year-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13.5px;
  font-weight: 800;
  color: var(--text-primary);
}

.rail-year-count {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-muted);
}

.rail-months-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rail-month-btn {
  position: relative;
  width: 100%;
  border: none;
  background: transparent;
  padding: 6px 8px 6px 7px;
  border-radius: var(--radius-control);
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 12.5px;
  transition: color 0.18s ease, transform 0.18s ease;
}

.rail-month-btn:hover,
.rail-month-btn.is-hovered {
  color: var(--accent);
  transform: translateX(2px);
}

.rail-month-btn.is-active {
  color: var(--accent);
  font-weight: 700;
}

.rail-node-dot {
  position: relative;
  width: 8px;
  height: 8px;
  margin-left: 1px;
  border-radius: 50%;
  background: var(--bg-glass-strong);
  box-shadow: inset 0 0 0 2px var(--text-muted);
  flex-shrink: 0;
  transition: background-color 0.24s ease, box-shadow 0.24s ease, transform 0.24s ease;
}

.rail-month-btn:hover .rail-node-dot,
.rail-month-btn.is-hovered .rail-node-dot {
  box-shadow: inset 0 0 0 2px var(--accent);
  transform: scale(1.15);
}

.rail-month-btn.is-active .rail-node-dot {
  background: var(--accent);
  box-shadow: 0 0 0 4px var(--accent-soft);
  transform: scale(1.22);
}

.rail-month-name {
  min-width: 36px;
  font-variant-numeric: tabular-nums;
}

.rail-month-bar {
  flex: 1;
  height: 4px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--border-subtle) 75%, transparent);
  overflow: hidden;
}

.rail-month-bar-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent), var(--accent-2));
  opacity: 0.55;
  transition: width 0.35s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.2s ease;
}

.rail-month-btn.is-active .rail-month-bar-fill {
  opacity: 1;
}

.rail-month-count {
  min-width: 18px;
  text-align: right;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: var(--text-muted);
}

/* 右侧竖向连贯时间线 */
.stream-card {
  padding: 26px 28px 30px 26px;
}

.stream-inner {
  position: relative;
}

.stream-spine {
  position: absolute;
  left: 9px;
  top: 12px;
  bottom: 14px;
  width: 2px;
  border-radius: 999px;
  background: linear-gradient(
    180deg,
    var(--accent),
    color-mix(in srgb, var(--accent-2) 55%, var(--border-subtle)) 45%,
    var(--border-subtle)
  );
  transform-origin: top center;
  animation: spineGrow 0.65s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes spineGrow {
  from {
    transform: scaleY(0);
    opacity: 0.2;
  }
  to {
    transform: scaleY(1);
    opacity: 1;
  }
}

.timeline-groups {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.month-section {
  position: relative;
  animation: groupSlideIn 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: calc(var(--group-stagger, 0) * 65ms);
}

@keyframes groupSlideIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.month-milestone {
  position: relative;
  width: 100%;
  border: none;
  background: transparent;
  padding: 4px 10px 4px 28px;
  margin-bottom: 8px;
  border-radius: var(--radius-control);
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.18s ease;
}

.month-milestone:hover {
  background: var(--bg-subtle);
}

.milestone-ring {
  position: absolute;
  left: 2px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--bg-glass-strong);
  box-shadow: inset 0 0 0 2px var(--accent), 0 0 0 4px var(--bg-glass-strong);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.24s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.24s ease;
}

.milestone-core {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  transition: transform 0.24s ease;
}

.month-section.is-active-group .milestone-ring {
  transform: translateY(-50%) scale(1.16);
  box-shadow:
    inset 0 0 0 2px var(--accent),
    0 0 0 5px var(--accent-soft);
}

.milestone-label-wrap {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.milestone-ym {
  font-size: 17px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.milestone-iso {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11.5px;
  color: var(--text-muted);
}

.milestone-count {
  margin-left: auto;
  padding: 2px 9px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11.5px;
  font-weight: 600;
}

.milestone-chevron {
  width: 14px;
  height: 14px;
  fill: none;
  stroke: var(--text-muted);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
  transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1), stroke 0.2s ease;
}

.month-section.is-collapsed .milestone-chevron {
  transform: rotate(-90deg);
}

.month-collapse-shell {
  display: grid;
  grid-template-rows: 1fr;
  transition: grid-template-rows 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}

.month-section.is-collapsed .month-collapse-shell {
  grid-template-rows: 0fr;
}

.month-collapse-clip {
  overflow: hidden;
  transition: opacity 0.24s ease;
}

.month-section.is-collapsed .month-collapse-clip {
  opacity: 0;
}

.archive-list {
  position: relative;
  list-style: none;
  margin: 0;
  padding: 4px 0 6px 28px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.archive-row {
  position: relative;
  display: flex;
  align-items: center;
  animation: itemCascadeIn 0.38s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: calc(var(--item-stagger, 0) * 35ms);
}

@keyframes itemCascadeIn {
  from {
    opacity: 0;
    transform: translateX(-8px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.row-branch-dot {
  position: absolute;
  left: -22px;
  top: 50%;
  transform: translateY(-50%);
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--bg-glass-strong);
  box-shadow: inset 0 0 0 2px color-mix(in srgb, var(--accent) 45%, var(--border-glass));
  transition:
    transform 0.22s cubic-bezier(0.34, 1.56, 0.64, 1),
    background-color 0.22s ease,
    box-shadow 0.22s ease;
}

.row-branch-tick {
  position: absolute;
  left: -14px;
  top: 50%;
  width: 11px;
  height: 1.5px;
  background: color-mix(in srgb, var(--border-subtle) 85%, transparent);
  transform-origin: left center;
  transition: width 0.22s ease, background-color 0.22s ease;
}

.archive-row:hover .row-branch-dot {
  background: var(--accent);
  box-shadow: 0 0 0 4px var(--accent-soft);
  transform: translateY(-50%) scale(1.22);
}

.archive-row:hover .row-branch-tick {
  width: 14px;
  background: var(--accent);
}

.archive-entry-card {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-control);
  border: 1px solid transparent;
  background: color-mix(in srgb, var(--bg-subtle) 65%, transparent);
  transition:
    transform 0.24s cubic-bezier(0.22, 1, 0.36, 1),
    background-color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.archive-row:hover .archive-entry-card {
  transform: translateX(4px);
  background: var(--bg-subtle);
  border-color: color-mix(in srgb, var(--accent) 26%, var(--border-subtle));
  box-shadow: 0 6px 18px -12px rgba(15, 23, 42, 0.16);
}

.entry-date {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
  padding: 2px 7px;
  border-radius: 6px;
  background: var(--bg-glass-strong);
}

.entry-title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.45;
  transition: color 0.18s ease;
}

.archive-row:hover .entry-title {
  color: var(--accent);
}

.title-mark {
  padding: 0 3px;
  border-radius: 4px;
  background: color-mix(in srgb, var(--accent) 22%, transparent);
  color: var(--accent);
}

.entry-cat {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--bg-glass-strong);
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  color: var(--text-secondary);
  font-size: 11.5px;
  font-weight: 500;
}

.entry-arrow {
  display: inline-flex;
  align-items: center;
  color: var(--accent);
  opacity: 0;
  transform: translateX(-6px);
  transition: opacity 0.22s ease, transform 0.22s cubic-bezier(0.22, 1, 0.36, 1);
  flex-shrink: 0;
}

.archive-row:hover .entry-arrow {
  opacity: 1;
  transform: translateX(0);
}

.arrow-svg {
  width: 14px;
  height: 14px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* 筛选重排连续过渡动画 */
.timeline-group-move,
.timeline-item-move {
  transition: transform 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}

.timeline-group-enter-active,
.timeline-group-leave-active,
.timeline-item-enter-active,
.timeline-item-leave-active {
  transition: opacity 0.24s ease, transform 0.24s cubic-bezier(0.22, 1, 0.36, 1);
}

.timeline-group-enter-from,
.timeline-group-leave-to,
.timeline-item-enter-from,
.timeline-item-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

.timeline-group-leave-active {
  position: absolute;
  width: 100%;
  pointer-events: none;
}

.timeline-item-leave-active {
  position: absolute;
  left: 28px;
  width: calc(100% - 28px);
  pointer-events: none;
}

@supports (corner-shape: squircle) {
  .archive-entry-card,
  .rail-active-slider {
    corner-shape: squircle;
  }
}

@media (max-width: 860px) {
  .archive-layout {
    grid-template-columns: 1fr;
  }

  .rail-aside {
    position: sticky;
    top: 72px;
    z-index: 8;
  }

  .rail-card {
    padding: 12px 14px;
  }

  .rail-header,
  .rail-track,
  .rail-active-slider,
  .rail-year-btn,
  .rail-month-bar {
    display: none;
  }

  .rail-timeline,
  .rail-year-block,
  .rail-months-wrap {
    flex-direction: row;
    align-items: center;
    gap: 6px;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .rail-month-btn {
    width: auto;
    white-space: nowrap;
    padding: 5px 11px;
    border-radius: 999px;
    background: var(--bg-subtle);
    box-shadow: inset 0 0 0 1px var(--border-subtle);
  }

  .rail-month-btn.is-active {
    background: var(--accent-soft);
    box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent) 45%, transparent);
  }

  .rail-month-name {
    min-width: auto;
  }

  .stream-card {
    padding: 20px 16px;
  }

  .archive-search-input {
    width: 160px;
  }

  .archive-search-input:focus {
    width: 190px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .stream-spine,
  .month-section,
  .archive-row,
  .rail-card,
  .rail-active-slider,
  .rail-track-fill,
  .month-collapse-shell {
    animation: none !important;
    transition: none !important;
  }
}
</style>
