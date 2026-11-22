<template>
  <div class="home-page">
    <div class="home-grid">
      <!-- 左侧内容区：统一筛选胶囊 + 主视觉焦点文章 + 精致文章网格 -->
      <section class="feed-col">
        <div ref="dockSentinelRef" class="dock-sentinel" aria-hidden="true" />
        <div class="filter-dock card" :class="{ 'is-stuck': isDockStuck }">
          <div class="dock-cats" role="tablist" aria-label="文章分类筛选">
            <button
              type="button"
              class="pill"
              :class="{ active: !activeCategoryId && !activeTagId }"
              @click="clearCategory"
            >
              全部
            </button>
            <button
              v-for="cat in categories"
              :key="cat.id"
              type="button"
              class="pill"
              :class="{ active: activeCategoryId === cat.id }"
              @click="selectCategory(cat.id)"
            >
              {{ cat.name }} · {{ cat.articleCount }}
            </button>
            <button
              v-if="activeTagObj"
              type="button"
              class="pill inline-tag-filter"
              title="点击取消标签筛选"
              @click="selectTag(activeTagObj.id)"
            >
              <span
                v-if="activeTagObj.color"
                class="tag-dot"
                :style="{ backgroundColor: activeTagObj.color }"
              />
              <span>标签：{{ activeTagObj.name }}</span>
              <span class="filter-dismiss">取消</span>
            </button>
          </div>

          <div class="search-capsule">
            <span class="search-icon">搜索</span>
            <input
              ref="searchInputRef"
              v-model="keywords"
              type="text"
              class="search-input"
              placeholder="标题、摘要或正文（按 / 聚焦）"
              @keyup.enter="handleSearch"
            />
            <button
              v-if="keywords"
              type="button"
              class="clear-btn"
              title="清空搜索"
              @click="clearKeywords"
            >
              清空
            </button>
            <button type="button" class="btn btn-primary search-submit" @click="handleSearch">
              搜索
            </button>
          </div>
        </div>

        <!-- 骨架屏闪烁加载 -->
        <template v-if="loading">
          <div class="card skeleton-card skeleton-card--featured">
            <div class="skeleton-line w-1/3" />
            <div class="skeleton-line w-3/4 h-lg" />
            <div class="skeleton-line w-full" />
            <div class="skeleton-line w-2/3" />
          </div>
          <div class="article-grid is-two-col">
            <div v-for="n in 2" :key="n" class="card skeleton-card">
              <div class="skeleton-line w-1/3" />
              <div class="skeleton-line w-3/4 h-lg" />
              <div class="skeleton-line w-full" />
            </div>
          </div>
        </template>

        <div v-else-if="!articles.length" class="empty-card card animate-rise">
          <div class="empty-title">暂无符合条件的文章</div>
          <p class="empty-desc">
            {{ hasAnyFilter ? "当前筛选或搜索词下没有匹配结果，可以重置条件浏览全部文章。" : "还没有公开发布的文章。" }}
          </p>
          <button v-if="hasAnyFilter" type="button" class="btn btn-primary" @click="resetFilters">
            查看全部文章
          </button>
        </div>

        <template v-else>
          <!-- 第一视觉重心：焦点大图/焦点导读主卡（无筛选第一页置顶展示） -->
          <article
            v-if="featuredArticle"
            class="featured-card card card-interactive animate-rise"
            @pointermove="onPointerMove"
          >
            <div class="featured-main">
              <div class="featured-eyebrow">
                <span class="featured-badge">
                  <span class="featured-dot" />
                  <span>{{ featuredArticle.isTop === 1 ? "置顶精选" : "最新焦点" }}</span>
                </span>
                <button
                  v-if="featuredArticle.categoryName && featuredArticle.categoryId"
                  type="button"
                  class="pill"
                  @click="selectCategory(featuredArticle.categoryId)"
                >
                  {{ featuredArticle.categoryName }}
                </button>
                <span class="date-text">
                  {{ formatDate(featuredArticle.publishTime || featuredArticle.createTime) }}
                </span>
              </div>

              <h1 class="featured-title">
                <RouterLink :to="`/article/${featuredArticle.id}`">
                  {{ featuredArticle.title }}
                </RouterLink>
              </h1>

              <p v-if="featuredArticle.summary" class="featured-summary">
                {{ featuredArticle.summary }}
              </p>

              <div class="featured-footer">
                <div class="tag-row">
                  <button
                    v-for="tag in featuredArticle.tags || []"
                    :key="tag.id"
                    type="button"
                    class="pill"
                    @click="selectTag(tag.id)"
                  >
                    <span
                      v-if="tag.color"
                      class="tag-dot"
                      :style="{ backgroundColor: tag.color }"
                    />
                    <span>{{ tag.name }}</span>
                  </button>
                </div>

                <RouterLink :to="`/article/${featuredArticle.id}`" class="btn btn-primary featured-cta">
                  阅读全文
                </RouterLink>
              </div>
            </div>

            <RouterLink
              :to="`/article/${featuredArticle.id}`"
              class="featured-visual"
              :aria-label="featuredArticle.title"
            >
              <div v-if="featuredArticle.coverImage" class="featured-cover-box">
                <img :src="featuredArticle.coverImage" :alt="featuredArticle.title" />
                <span class="cover-reading-badge">
                  约 {{ featuredArticle.readingMinutes || 1 }} 分钟阅读
                </span>
              </div>
              <div v-else class="featured-plaque">
                <div class="plaque-head">
                  <span class="plaque-kicker">导读概览</span>
                  <span class="plaque-num">01</span>
                </div>
                <div class="plaque-metrics">
                  <div class="plaque-cell">
                    <strong>{{ featuredArticle.wordCount ?? 0 }}</strong>
                    <span>总字数</span>
                  </div>
                  <div class="plaque-cell">
                    <strong>{{ featuredArticle.readingMinutes || 1 }} 分</strong>
                    <span>预计阅读</span>
                  </div>
                  <div class="plaque-cell">
                    <strong>{{ featuredArticle.viewCount ?? 0 }}</strong>
                    <span>浏览</span>
                  </div>
                  <div class="plaque-cell">
                    <strong>{{ Number(featuredArticle.likeCount ?? 0) + Number(featuredArticle.commentCount ?? 0) }}</strong>
                    <span>赞与评</span>
                  </div>
                </div>
              </div>
            </RouterLink>
          </article>

          <!-- 次级文章流：超过1篇自动切换均衡双列杂志网格，单篇全宽呼应不留白 -->
          <div
            v-if="streamArticles.length"
            class="article-grid"
            :class="{ 'is-two-col': streamArticles.length >= 2 }"
          >
            <article
              v-for="(item, idx) in streamArticles"
              :key="item.id"
              class="article-card card card-interactive animate-rise"
              :style="{ '--stagger': idx + 1 }"
              @pointermove="onPointerMove"
            >
              <RouterLink v-if="item.coverImage" :to="`/article/${item.id}`" class="article-cover">
                <img :src="item.coverImage" :alt="item.title" loading="lazy" />
              </RouterLink>

              <div class="article-body">
                <div class="article-meta-top">
                  <span class="article-ordinal">
                    {{ formatOrdinal(showSpotlight ? idx + 2 : idx + 1) }}
                  </span>
                  <span v-if="item.isTop === 1" class="top-badge">置顶</span>
                  <button
                    v-if="item.categoryName && item.categoryId"
                    type="button"
                    class="pill"
                    @click="selectCategory(item.categoryId)"
                  >
                    {{ item.categoryName }}
                  </button>
                  <span class="date-text">{{ formatDate(item.publishTime || item.createTime) }}</span>
                  <span v-if="item.readingMinutes" class="date-text">
                    约 {{ item.readingMinutes }} 分钟
                  </span>
                </div>

                <h2 class="article-title">
                  <RouterLink :to="`/article/${item.id}`">{{ item.title }}</RouterLink>
                </h2>

                <p v-if="item.summary" class="article-summary">{{ item.summary }}</p>

                <div class="article-footer">
                  <div class="tag-row">
                    <button
                      v-for="tag in (item.tags || []).slice(0, 3)"
                      :key="tag.id"
                      type="button"
                      class="pill"
                      @click="selectTag(tag.id)"
                    >
                      <span
                        v-if="tag.color"
                        class="tag-dot"
                        :style="{ backgroundColor: tag.color }"
                      />
                      <span>{{ tag.name }}</span>
                    </button>
                  </div>

                  <div class="footer-right">
                    <div class="stats-row">
                      <span>阅读 {{ item.viewCount ?? 0 }}</span>
                      <span>点赞 {{ item.likeCount ?? 0 }}</span>
                      <span>评论 {{ item.commentCount ?? 0 }}</span>
                    </div>
                    <RouterLink :to="`/article/${item.id}`" class="read-more">
                      全文
                    </RouterLink>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </template>

        <div v-if="total > pageSize" class="pagination-bar card">
          <button type="button" class="btn" :disabled="pageNum <= 1" @click="changePage(pageNum - 1)">
            上一页
          </button>
          <span class="page-info">第 {{ pageNum }} / {{ totalPages }} 页（共 {{ total }} 篇）</span>
          <button
            type="button"
            class="btn"
            :disabled="pageNum >= totalPages"
            @click="changePage(pageNum + 1)"
          >
            下一页
          </button>
        </div>
      </section>

      <!-- 右侧固定栏：个人卡片、热门文章、标签云整体固定在右侧 -->
      <aside class="side-col">
        <div class="side-fixed-stack">
          <div
            v-if="siteInfo"
            class="author-card card card-interactive animate-rise"
            style="--stagger: 1"
            @pointermove="onPointerMove"
          >
            <div class="author-header">
              <div class="avatar-orbit">
                <img
                  v-if="siteInfo.authorAvatar"
                  :src="siteInfo.authorAvatar"
                  :alt="siteInfo.authorName"
                  class="author-avatar"
                />
                <div v-else class="author-avatar author-avatar--fallback">
                  {{ (siteInfo.authorName || "B").slice(0, 1) }}
                </div>
              </div>
              <div class="author-intro">
                <h3 class="author-name">{{ siteInfo.authorName }}</h3>
                <p v-if="siteInfo.authorBio" class="author-bio">{{ siteInfo.authorBio }}</p>
              </div>
            </div>

            <div class="author-stats">
              <div class="stat-tile">
                <strong>{{ siteInfo.articleCount }}</strong>
                <span>文章</span>
              </div>
              <div class="stat-tile">
                <strong>{{ siteInfo.categoryCount }}</strong>
                <span>分类</span>
              </div>
              <div class="stat-tile">
                <strong>{{ siteInfo.tagCount }}</strong>
                <span>标签</span>
              </div>
              <div class="stat-tile">
                <strong>{{ siteInfo.totalViews }}</strong>
                <span>总阅读</span>
              </div>
            </div>

            <div class="author-actions">
              <RouterLink to="/about" class="btn author-sub-btn">关于博主</RouterLink>
              <a
                v-if="siteInfo.githubUrl"
                :href="siteInfo.githubUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="btn btn-primary author-sub-btn"
              >
                GitHub
              </a>
            </div>
          </div>

          <div
            v-if="siteInfo?.hotArticles?.length"
            class="side-card card card-interactive animate-rise"
            style="--stagger: 2"
            @pointermove="onPointerMove"
          >
            <h4 class="side-title">热门文章</h4>
            <ul class="hot-list">
              <li v-for="(hot, idx) in siteInfo.hotArticles" :key="hot.id">
                <RouterLink :to="`/article/${hot.id}`" class="hot-link">
                  <span class="hot-rank" :class="`hot-rank--${idx + 1}`">{{ idx + 1 }}</span>
                  <span class="hot-text">{{ hot.title }}</span>
                  <span class="hot-views">{{ hot.viewCount }}</span>
                </RouterLink>
              </li>
            </ul>
          </div>

          <div
            v-if="tags.length"
            class="side-card card card-interactive animate-rise"
            style="--stagger: 3"
            @pointermove="onPointerMove"
          >
            <div class="side-title-row">
              <h4 class="side-title">标签云</h4>
              <button
                v-if="activeTagId"
                type="button"
                class="side-clear-btn"
                @click="selectTag(activeTagId)"
              >
                重置标签
              </button>
            </div>
            <div class="tag-cloud">
              <button
                v-for="tag in tags"
                :key="tag.id"
                type="button"
                class="pill"
                :class="{ active: activeTagId === tag.id }"
                @click="selectTag(tag.id)"
              >
                <span
                  v-if="tag.color"
                  class="tag-dot"
                  :style="{ backgroundColor: tag.color }"
                />
                <span>{{ tag.name }}</span>
                <span class="tag-count">{{ tag.articleCount }}</span>
              </button>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  blogApi,
  type BlogArticleItem,
  type BlogCategory,
  type BlogTag,
} from "@/api/blog";
import { useSiteInfo } from "@/composables/useSiteInfo";

const route = useRoute();
const router = useRouter();
const { siteInfo, loadSiteInfo } = useSiteInfo();

const loading = ref(false);
const articles = ref<BlogArticleItem[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = 10;

const keywords = ref("");
const activeCategoryId = ref<string | undefined>(undefined);
const activeTagId = ref<string | undefined>(undefined);

const categories = ref<BlogCategory[]>([]);
const tags = ref<BlogTag[]>([]);

const dockSentinelRef = ref<HTMLElement | null>(null);
const searchInputRef = ref<HTMLInputElement | null>(null);
const isDockStuck = ref(false);
let dockObserver: IntersectionObserver | null = null;

function handleGlobalKeydown(e: KeyboardEvent) {
  const target = e.target as HTMLElement | null;
  const inEditable =
    target?.tagName === "INPUT" ||
    target?.tagName === "TEXTAREA" ||
    Boolean(target?.isContentEditable);

  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
    e.preventDefault();
    searchInputRef.value?.focus();
    searchInputRef.value?.select();
    return;
  }

  if (e.key === "/" && !inEditable) {
    e.preventDefault();
    searchInputRef.value?.focus();
    searchInputRef.value?.select();
    return;
  }

  if (e.key === "Escape" && document.activeElement === searchInputRef.value) {
    searchInputRef.value?.blur();
  }
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

const activeTagObj = computed(() =>
  activeTagId.value ? tags.value.find((t) => t.id === activeTagId.value) : undefined,
);

const hasAnyFilter = computed(() =>
  Boolean(activeCategoryId.value || activeTagId.value || keywords.value.trim()),
);

const showSpotlight = computed(
  () => !hasAnyFilter.value && pageNum.value === 1 && articles.value.length > 0,
);

const featuredArticle = computed(() => (showSpotlight.value ? articles.value[0] : null));

const streamArticles = computed(() =>
  showSpotlight.value ? articles.value.slice(1) : articles.value,
);

function formatOrdinal(num: number) {
  return num < 10 ? `0${num}` : String(num);
}

function onPointerMove(e: PointerEvent) {
  const target = e.currentTarget as HTMLElement | null;
  if (!target) return;
  const rect = target.getBoundingClientRect();
  target.style.setProperty("--mx", `${e.clientX - rect.left}px`);
  target.style.setProperty("--my", `${e.clientY - rect.top}px`);
}

function formatDate(val?: string) {
  if (!val) return "";
  return val.slice(0, 10);
}

async function fetchArticles() {
  loading.value = true;
  try {
    const res = await blogApi.getArticles({
      pageNum: pageNum.value,
      pageSize,
      keywords: keywords.value.trim() || undefined,
      categoryId: activeCategoryId.value,
      tagId: activeTagId.value,
    });
    articles.value = res.list ?? [];
    total.value = res.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function syncRouteQuery() {
  router.replace({
    path: "/",
    query: {
      ...(keywords.value.trim() ? { q: keywords.value.trim() } : {}),
      ...(activeCategoryId.value ? { categoryId: activeCategoryId.value } : {}),
      ...(activeTagId.value ? { tagId: activeTagId.value } : {}),
      ...(pageNum.value > 1 ? { page: String(pageNum.value) } : {}),
    },
  });
}

function handleSearch() {
  pageNum.value = 1;
  syncRouteQuery();
  fetchArticles();
}

function clearKeywords() {
  keywords.value = "";
  handleSearch();
}

function clearCategory() {
  activeCategoryId.value = undefined;
  activeTagId.value = undefined;
  pageNum.value = 1;
  syncRouteQuery();
  fetchArticles();
}

function selectCategory(id: string) {
  activeCategoryId.value = activeCategoryId.value === id ? undefined : id;
  pageNum.value = 1;
  syncRouteQuery();
  fetchArticles();
}

function selectTag(id: string) {
  activeTagId.value = activeTagId.value === id ? undefined : id;
  pageNum.value = 1;
  syncRouteQuery();
  fetchArticles();
}

function resetFilters() {
  keywords.value = "";
  activeCategoryId.value = undefined;
  activeTagId.value = undefined;
  pageNum.value = 1;
  syncRouteQuery();
  fetchArticles();
}

function changePage(nextPage: number) {
  pageNum.value = nextPage;
  syncRouteQuery();
  fetchArticles();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

watch(
  () => route.query,
  (q) => {
    keywords.value = typeof q.q === "string" ? q.q : "";
    activeCategoryId.value = typeof q.categoryId === "string" ? q.categoryId : undefined;
    activeTagId.value = typeof q.tagId === "string" ? q.tagId : undefined;
    pageNum.value = typeof q.page === "string" ? Math.max(1, Number(q.page) || 1) : 1;
  },
  { immediate: true },
);

onMounted(async () => {
  window.addEventListener("keydown", handleGlobalKeydown);
  if (dockSentinelRef.value) {
    dockObserver = new IntersectionObserver(
      ([entry]) => {
        isDockStuck.value = !entry.isIntersecting;
      },
      { rootMargin: "-79px 0px 0px 0px", threshold: 0 },
    );
    dockObserver.observe(dockSentinelRef.value);
  }

  const [, catList, tagList] = await Promise.all([
    loadSiteInfo(),
    blogApi.getCategories(),
    blogApi.getTags(),
  ]);
  categories.value = catList;
  tags.value = tagList;
  await fetchArticles();
});

onUnmounted(() => {
  window.removeEventListener("keydown", handleGlobalKeydown);
  dockObserver?.disconnect();
});
</script>

<style scoped>
.home-page {
  width: 100%;
}

/* 左侧内容区 + 右侧固定栏 */
.home-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  align-items: stretch;
}

.feed-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.dock-sentinel {
  height: 1px;
  margin-bottom: -19px;
  pointer-events: none;
}

/* 统一筛选与搜索吸顶条（包含内联标签状态，不额外挤出第二行） */
.filter-dock {
  position: sticky;
  top: 78px;
  z-index: 25;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 20px;
  transition:
    background-color 0.25s ease,
    box-shadow 0.25s ease,
    border-color 0.25s ease;
}

.filter-dock.is-stuck {
  background: var(--bg-glass-strong);
  backdrop-filter: blur(26px) saturate(185%);
  -webkit-backdrop-filter: blur(26px) saturate(185%);
  border-color: color-mix(in srgb, var(--accent) 28%, var(--border-glass));
  box-shadow:
    0 14px 32px -12px rgba(15, 23, 42, 0.16),
    var(--specular-inset);
}

html.dark .filter-dock.is-stuck {
  box-shadow:
    0 16px 34px -10px rgba(0, 0, 0, 0.6),
    var(--specular-inset);
}

.dock-cats {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
  padding: 2px 0;
}

.dock-cats::-webkit-scrollbar {
  display: none;
}

.dock-cats .pill {
  flex-shrink: 0;
}

.inline-tag-filter {
  background: var(--accent-soft);
  color: var(--accent);
  border-color: color-mix(in srgb, var(--accent) 42%, transparent);
  font-weight: 600;
}

.filter-dismiss {
  opacity: 0.72;
  font-size: 11px;
  padding-left: 2px;
}

.search-capsule {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 4px 4px 12px;
  border-radius: 14px;
  background: var(--bg-subtle);
  border: 1px solid var(--border-subtle);
  min-width: 240px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.search-capsule:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.search-icon {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  color: var(--text-primary);
  outline: none;
  font-size: 13px;
}

.clear-btn {
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  padding: 3px 6px;
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.search-submit {
  padding: 6px 13px;
  border-radius: 11px;
  font-size: 13px;
}

/* 主视觉焦点卡（Featured Spotlight Card） */
.featured-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 232px;
  gap: 24px;
  padding: 26px 28px;
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--accent) 12%, var(--bg-glass-strong)) 0%,
    var(--bg-glass-strong) 58%,
    color-mix(in srgb, var(--accent-2) 10%, var(--bg-glass-strong)) 100%
  );
  border-color: color-mix(in srgb, var(--accent) 26%, var(--border-glass));
  box-shadow:
    0 20px 44px -18px color-mix(in srgb, var(--accent) 26%, transparent),
    var(--specular-inset);
}

.featured-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-glass-hover), var(--specular-inset);
}

.featured-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.featured-eyebrow {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.featured-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 4px 11px;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--accent), var(--accent-2));
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

html.dark .featured-badge {
  color: #06131a;
}

.featured-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.featured-title {
  margin: 0 0 12px;
  font-size: clamp(22px, 2.35vw, 28px);
  font-weight: 800;
  line-height: 1.32;
  letter-spacing: -0.02em;
}

.featured-title a {
  transition: color 0.2s ease;
}

.featured-card:hover .featured-title a {
  color: var(--accent);
}

.featured-summary {
  margin: 0 0 20px;
  font-size: 14.5px;
  line-height: 1.78;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.featured-footer {
  margin-top: auto;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.featured-cta {
  padding: 8px 18px;
  font-size: 13px;
  border-radius: 999px;
}

.featured-visual {
  display: flex;
  align-items: stretch;
  min-width: 0;
}

.featured-cover-box {
  position: relative;
  width: 100%;
  min-height: 196px;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid var(--border-subtle);
}

.featured-cover-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.55s cubic-bezier(0.22, 1, 0.36, 1);
}

.featured-card:hover .featured-cover-box img {
  transform: scale(1.05);
}

.cover-reading-badge {
  position: absolute;
  right: 10px;
  bottom: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  backdrop-filter: blur(8px);
  color: #f8fafc;
  font-size: 11px;
  font-weight: 600;
}

.featured-plaque {
  width: 100%;
  border-radius: 18px;
  padding: 16px;
  background: var(--bg-subtle);
  border: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 12px;
  transition: border-color 0.25s ease, transform 0.3s ease;
}

.featured-card:hover .featured-plaque {
  border-color: color-mix(in srgb, var(--accent) 42%, transparent);
}

.plaque-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.plaque-kicker {
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
}

.plaque-num {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
  color: color-mix(in srgb, var(--accent) 45%, var(--text-muted));
}

.plaque-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.plaque-cell {
  padding: 9px 10px;
  border-radius: 12px;
  background: var(--bg-glass-strong);
  border: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.plaque-cell strong {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.plaque-cell span {
  font-size: 11px;
  color: var(--text-muted);
}

/* 骨架屏 */
.skeleton-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-card--featured {
  min-height: 220px;
}

.skeleton-line {
  height: 14px;
  border-radius: 8px;
  background: linear-gradient(90deg, var(--bg-subtle) 25%, rgba(148, 163, 184, 0.2) 50%, var(--bg-subtle) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skeleton-line.h-lg {
  height: 22px;
}
.skeleton-line.w-1\/3 {
  width: 33%;
}
.skeleton-line.w-2\/3 {
  width: 66%;
}
.skeleton-line.w-3\/4 {
  width: 75%;
}
.skeleton-line.w-full {
  width: 100%;
}

.empty-card {
  padding: 56px 20px;
  text-align: center;
  color: var(--text-muted);
}

.empty-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.empty-desc {
  margin: 0 0 18px;
  font-size: 14px;
}

/* 文章网格容器 */
.article-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.article-grid.is-two-col {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.article-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-top: 2px solid transparent;
}

.article-card:hover {
  transform: translateY(-3px);
  border-top-color: var(--accent);
  border-color: color-mix(in srgb, var(--accent) 32%, var(--border-glass));
  box-shadow: var(--shadow-glass-hover), var(--specular-inset);
}

.article-cover {
  display: block;
  overflow: hidden;
}

.article-cover img {
  width: 100%;
  height: 175px;
  object-fit: cover;
  display: block;
  transition: transform 0.55s cubic-bezier(0.22, 1, 0.36, 1);
}

.article-card:hover .article-cover img {
  transform: scale(1.045);
}

.article-body {
  padding: 20px 22px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.article-meta-top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.article-ordinal {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
}

.top-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 9px;
  border-radius: 999px;
  background: linear-gradient(135deg, #f43f5e, #f97316);
  color: #fff;
}

.date-text {
  font-size: 12px;
  color: var(--text-muted);
}

.article-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.42;
}

.article-title a {
  transition: color 0.2s ease;
}

.article-card:hover .article-title a {
  color: var(--accent);
}

.article-summary {
  margin: 0 0 16px;
  color: var(--text-secondary);
  font-size: 13.5px;
  line-height: 1.72;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-footer {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stats-row {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: var(--text-muted);
}

.read-more {
  font-size: 13px;
  font-weight: 600;
  color: var(--accent);
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-radius: 20px;
}

.page-info {
  font-size: 13px;
  color: var(--text-muted);
}

/* 右侧整栏固定：个人卡片 + 热门文章 + 标签云 */
.side-col {
  min-width: 0;
}

.side-fixed-stack {
  position: sticky;
  top: 78px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: calc(100vh - 96px);
  overflow-y: auto;
  scrollbar-width: none;
  padding: 4px 6px 12px;
  margin: -4px -6px;
}

.side-fixed-stack::-webkit-scrollbar {
  display: none;
}

.author-card {
  padding: 18px 18px 16px;
}

.author-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.avatar-orbit {
  position: relative;
  width: 62px;
  height: 62px;
  padding: 3px;
  border-radius: 50%;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--accent), var(--accent-2), #f472b6);
  transition: transform 0.55s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.author-card:hover .avatar-orbit {
  transform: rotate(10deg) scale(1.05);
}

.author-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  border: 2px solid var(--bg-glass-strong);
}

.author-avatar--fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-subtle);
  font-size: 24px;
  font-weight: 700;
}

.author-intro {
  min-width: 0;
}

.author-name {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 700;
}

.author-bio {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.author-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  margin-bottom: 12px;
}

.stat-tile {
  padding: 8px 4px;
  border-radius: 13px;
  background: var(--bg-subtle);
  border: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 11px;
  color: var(--text-muted);
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.stat-tile:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--accent) 45%, transparent);
}

.stat-tile strong {
  font-size: 15px;
  color: var(--text-primary);
}

.author-actions {
  display: flex;
  gap: 8px;
}

.author-sub-btn {
  flex: 1;
  padding: 7px 10px;
  font-size: 13px;
}

.side-card {
  padding: 15px 16px;
}

.side-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.side-title-row .side-title {
  margin: 0;
}

.side-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 700;
}

.side-clear-btn {
  border: none;
  background: transparent;
  color: var(--accent);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.hot-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.hot-link {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 6px 8px;
  border-radius: 11px;
  font-size: 13px;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.hot-link:hover {
  background: var(--accent-soft);
  transform: translateX(3px);
}

.hot-link:hover .hot-text {
  color: var(--accent);
}

.hot-rank {
  width: 20px;
  height: 20px;
  border-radius: 7px;
  background: var(--bg-subtle);
  color: var(--text-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.hot-rank--1 {
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  color: #fff;
}

.hot-rank--2 {
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  color: #fff;
}

.hot-rank--3 {
  background: linear-gradient(135deg, #10b981, #14b8a6);
  color: #fff;
}

.hot-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-views {
  font-size: 12px;
  color: var(--text-muted);
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.tag-count {
  opacity: 0.7;
  font-size: 11px;
}

@media (max-width: 960px) {
  .home-grid {
    grid-template-columns: 1fr;
  }

  .side-fixed-stack {
    position: static;
    max-height: none;
    overflow-y: visible;
    padding: 0;
    margin: 0;
  }
}

@media (max-width: 740px) {
  .featured-card {
    grid-template-columns: 1fr;
    padding: 20px;
    gap: 16px;
  }

  .article-grid.is-two-col {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .filter-dock {
    position: static;
    flex-direction: column;
    align-items: stretch;
  }

  .search-capsule {
    width: 100%;
    min-width: 0;
  }
}
</style>
