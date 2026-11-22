<template>
  <div ref="homePageRef" class="home-page" :class="{ 'is-morph-returning': isReturningToHome }">
    <!-- 顶部出版物式个人序言带（Masthead） -->
    <header v-if="siteInfo" class="masthead" :class="{ 'animate-rise': !skipRiseOnRestore }">
      <div class="masthead-left">
        <RouterLink to="/about" class="masthead-avatar-wrap" aria-label="关于博主">
          <img
            v-if="siteInfo.authorAvatar"
            :src="siteInfo.authorAvatar"
            :alt="siteInfo.authorName"
            class="masthead-avatar"
          />
          <span v-else class="masthead-avatar masthead-avatar--fallback">
            {{ (siteInfo.authorName || "M").slice(0, 1) }}
          </span>
        </RouterLink>

        <div class="masthead-intro">
          <div class="masthead-kicker">
            <span class="masthead-author">{{ siteInfo.authorName }}</span>
            <span class="masthead-sep" aria-hidden="true">/</span>
            <span class="masthead-sub">
              {{ siteInfo.siteSubtitle || "全栈工程与思考记录" }}
            </span>
          </div>
          <h1 class="masthead-title">{{ siteInfo.siteTitle || "MyBlog" }}</h1>
          <p v-if="siteInfo.authorBio" class="masthead-bio">{{ siteInfo.authorBio }}</p>
        </div>
      </div>

      <div class="masthead-right">
        <div class="masthead-metrics">
          <span><strong>{{ siteInfo.articleCount }}</strong> 篇文章</span>
          <span class="metric-dot" aria-hidden="true" />
          <span><strong>{{ siteInfo.totalViews }}</strong> 次阅读</span>
          <span class="metric-dot" aria-hidden="true" />
          <span><strong>{{ siteInfo.tagCount }}</strong> 个标签</span>
        </div>
        <div class="masthead-links">
          <RouterLink to="/archives" class="masthead-link">时间线</RouterLink>
          <RouterLink to="/about" class="masthead-link">关于博主</RouterLink>
          <a
            v-if="siteInfo.githubUrl"
            :href="siteInfo.githubUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="masthead-link"
          >
            {{ siteInfo.githubUrl.includes("gitee") ? "Gitee" : "GitHub" }}
          </a>
        </div>
      </div>
    </header>

    <div class="home-grid">
      <!-- 左侧主阅读流：筛选条 + 精选头条 + 线性编辑文章流 -->
      <section class="feed-col">
        <div ref="dockSentinelRef" class="dock-sentinel" aria-hidden="true" />

        <div class="filter-dock card" :class="{ 'is-stuck': isDockStuck }">
          <div class="dock-cats" role="tablist" aria-label="文章分类筛选">
            <button
              type="button"
              class="filter-tab"
              :class="{ active: !activeCategoryId && !activeTagId }"
              @click="clearCategory"
            >
              全部
            </button>
            <button
              v-for="cat in categories"
              :key="cat.id"
              type="button"
              class="filter-tab"
              :class="{ active: activeCategoryId === cat.id }"
              @click="selectCategory(cat.id)"
            >
              <span>{{ cat.name }}</span>
              <span class="tab-count">{{ cat.articleCount }}</span>
            </button>
            <button
              v-if="activeTagObj"
              type="button"
              class="filter-tab inline-tag-filter"
              title="点击取消标签筛选"
              @click="selectTag(activeTagObj.id)"
            >
              <span>#{{ activeTagObj.name }}</span>
              <span class="filter-dismiss">取消</span>
            </button>
          </div>

          <div
            ref="searchBoxRef"
            class="search-capsule"
            :class="{ 'is-expanded': searchExpanded }"
            @click="!searchExpanded && openSearch()"
          >
            <div class="search-expand-inner">
              <input
                ref="searchInputRef"
                v-model="keywords"
                type="text"
                class="search-input"
                placeholder="标题、摘要或正文（/）"
                :tabindex="searchExpanded ? 0 : -1"
                @keyup.enter="handleSearch"
              />
              <button
                v-if="keywords"
                type="button"
                class="clear-btn"
                title="清空搜索"
                @click.stop="clearKeywords"
              >
                清空
              </button>
            </div>

            <button
              type="button"
              class="search-circle-btn"
              :aria-expanded="searchExpanded"
              :title="searchExpanded ? '执行搜索' : '展开搜索（/）'"
              :aria-label="searchExpanded ? '执行搜索' : '展开搜索'"
              @click.stop="handleSearchButtonClick"
            >
              <svg viewBox="0 0 24 24" class="search-circle-icon" aria-hidden="true">
                <circle cx="11" cy="11" r="7" />
                <line x1="20" y1="20" x2="16.65" y2="16.65" />
              </svg>
            </button>
          </div>
        </div>

        <!-- 骨架屏 -->
        <template v-if="loading">
          <div class="card skeleton-lead">
            <div class="skeleton-line w-1/4" />
            <div class="skeleton-line w-3/4 h-lg" />
            <div class="skeleton-line w-full" />
            <div class="skeleton-line w-2/3" />
          </div>
          <div class="skeleton-feed">
            <div v-for="n in 3" :key="n" class="skeleton-row">
              <div class="skeleton-line w-1/4" />
              <div class="skeleton-line w-2/3 h-lg" />
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
          <!-- 精选头条（无筛选第1页第1篇）：克制纸感主卡，去掉多余方格嵌套 -->
          <article
            v-if="featuredArticle"
            data-morph-card="feed"
            :data-article-id="featuredArticle.id"
            class="featured-lead card card-interactive"
            :class="{
              'has-cover': Boolean(featuredArticle.coverImage),
              'animate-rise': !skipRiseOnRestore,
              'is-morph-hidden':
                hiddenHomeArticleId === featuredArticle.id && hiddenHomeOriginKind === 'feed',
            }"
            :style="{ '--stagger': 0 }"
            @pointerenter="warmupArticle(featuredArticle.id)"
            @click.capture="captureOriginOnCardClick(featuredArticle, 'feed', $event)"
          >
            <div class="lead-main">
              <div class="lead-meta">
                <span class="lead-badge">
                  {{ featuredArticle.isTop === 1 ? "置顶精选" : "最新推荐" }}
                </span>
                <span class="mono-meta">
                  {{ formatDate(featuredArticle.publishTime || featuredArticle.createTime) }}
                </span>
                <template v-if="featuredArticle.categoryName && featuredArticle.categoryId">
                  <span class="meta-dot" aria-hidden="true">·</span>
                  <button
                    type="button"
                    class="inline-cat-btn"
                    @click="selectCategory(featuredArticle.categoryId)"
                  >
                    {{ featuredArticle.categoryName }}
                  </button>
                </template>
                <span class="meta-dot" aria-hidden="true">·</span>
                <span class="mono-meta">约 {{ featuredArticle.readingMinutes || 1 }} 分钟阅读</span>
                <span v-if="!featuredArticle.coverImage" class="lead-issue">NO. 01</span>
              </div>

              <h2 class="lead-title">
                <RouterLink :to="`/article/${featuredArticle.id}`">
                  {{ featuredArticle.title }}
                </RouterLink>
              </h2>

              <p v-if="featuredArticle.summary" class="lead-summary">
                {{ featuredArticle.summary }}
              </p>

              <div class="lead-footer">
                <div class="hash-tags">
                  <button
                    v-for="tag in featuredArticle.tags || []"
                    :key="tag.id"
                    type="button"
                    class="hash-tag-btn"
                    @click="selectTag(tag.id)"
                  >
                    #{{ tag.name }}
                  </button>
                </div>

                <div class="lead-footer-right">
                  <span class="mono-stat">阅读 {{ featuredArticle.viewCount ?? 0 }}</span>
                  <RouterLink :to="`/article/${featuredArticle.id}`" class="editorial-read-link">
                    <span>阅读全文</span>
                    <svg viewBox="0 0 24 24" class="arrow-icon" aria-hidden="true">
                      <line x1="5" y1="12" x2="19" y2="12" />
                      <polyline points="12 5 19 12 12 19" />
                    </svg>
                  </RouterLink>
                </div>
              </div>
            </div>

            <RouterLink
              v-if="featuredArticle.coverImage"
              :to="`/article/${featuredArticle.id}`"
              class="lead-cover"
              :aria-label="featuredArticle.title"
            >
              <img :src="featuredArticle.coverImage" :alt="featuredArticle.title" loading="lazy" />
            </RouterLink>
          </article>

          <!-- 极简线性编辑文章列表（Clean Editorial Feed） -->
          <section v-if="streamArticles.length" class="editorial-feed">
            <div class="feed-section-head">
              <span class="feed-section-label">
                {{ showSpotlight ? "近期文章" : hasAnyFilter ? "筛选结果" : "全部文章" }}
              </span>
              <span class="feed-section-count">
                {{ formatOrdinal(streamArticles.length) }} 篇
              </span>
            </div>

            <div class="feed-list">
              <article
                v-for="(item, idx) in streamArticles"
                :key="item.id"
                data-morph-card="feed"
                :data-article-id="item.id"
                class="feed-row"
                :class="{
                  'has-thumb': Boolean(item.coverImage),
                  'animate-rise': !skipRiseOnRestore,
                  'is-morph-hidden':
                    hiddenHomeArticleId === item.id && hiddenHomeOriginKind === 'feed',
                }"
                :style="{ '--stagger': idx + 1 }"
                @pointerenter="warmupArticle(item.id)"
                @click.capture="captureOriginOnCardClick(item, 'feed', $event)"
              >
                <div class="feed-date-col">
                  <time class="feed-date">
                    {{ formatDate(item.publishTime || item.createTime) }}
                  </time>
                  <span v-if="item.readingMinutes" class="feed-duration">
                    约 {{ item.readingMinutes }} 分钟
                  </span>
                </div>

                <div class="feed-body-col">
                  <div class="feed-eyebrow">
                    <span class="feed-ordinal">
                      #{{ formatOrdinal(showSpotlight ? idx + 2 : idx + 1) }}
                    </span>
                    <span v-if="item.isTop === 1" class="feed-pin">置顶</span>
                    <button
                      v-if="item.categoryName && item.categoryId"
                      type="button"
                      class="inline-cat-btn"
                      @click="selectCategory(item.categoryId)"
                    >
                      {{ item.categoryName }}
                    </button>
                    <span class="feed-stats">
                      阅读 {{ item.viewCount ?? 0 }} · 评论 {{ item.commentCount ?? 0 }}
                    </span>
                  </div>

                  <h3 class="feed-title">
                    <RouterLink :to="`/article/${item.id}`">{{ item.title }}</RouterLink>
                  </h3>

                  <p v-if="item.summary" class="feed-summary">{{ item.summary }}</p>

                  <div class="feed-row-footer">
                    <div class="hash-tags">
                      <button
                        v-for="tag in (item.tags || []).slice(0, 4)"
                        :key="tag.id"
                        type="button"
                        class="hash-tag-btn"
                        @click="selectTag(tag.id)"
                      >
                        #{{ tag.name }}
                      </button>
                    </div>

                    <RouterLink :to="`/article/${item.id}`" class="editorial-read-link">
                      <span>阅读</span>
                      <svg viewBox="0 0 24 24" class="arrow-icon" aria-hidden="true">
                        <line x1="5" y1="12" x2="19" y2="12" />
                        <polyline points="12 5 19 12 12 19" />
                      </svg>
                    </RouterLink>
                  </div>
                </div>

                <RouterLink
                  v-if="item.coverImage"
                  :to="`/article/${item.id}`"
                  class="feed-thumb"
                  :aria-label="item.title"
                >
                  <img :src="item.coverImage" :alt="item.title" loading="lazy" />
                </RouterLink>
              </article>
            </div>
          </section>
        </template>

        <div v-if="total > pageSize" class="pagination-bar">
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

      <!-- 右侧轻盈固定边栏：热门文章 + 标签索引 -->
      <aside class="side-col">
        <div class="side-fixed-stack">
          <div
            v-if="siteInfo?.hotArticles?.length"
            class="side-card card"
            :class="{ 'animate-rise': !skipRiseOnRestore }"
            style="--stagger: 1"
          >
            <div class="side-title-row">
              <h4 class="side-title">热门文章</h4>
              <span class="side-kicker">TOP</span>
            </div>
            <ul class="hot-list">
              <li v-for="(hot, idx) in siteInfo.hotArticles" :key="hot.id">
                <RouterLink
                  :to="`/article/${hot.id}`"
                  data-morph-card="hot"
                  :data-article-id="hot.id"
                  class="hot-link"
                  :class="{
                    'is-morph-hidden':
                      hiddenHomeArticleId === hot.id && hiddenHomeOriginKind === 'hot',
                  }"
                  @pointerenter="warmupArticle(hot.id)"
                  @click="captureHotOriginOnClick(hot, $event)"
                >
                  <span class="hot-rank" :class="{ 'is-top': idx === 0 }">
                    {{ formatOrdinal(idx + 1) }}
                  </span>
                  <span class="hot-text">{{ hot.title }}</span>
                  <span class="hot-views">{{ hot.viewCount }}</span>
                </RouterLink>
              </li>
            </ul>
          </div>

          <div
            v-if="tags.length"
            class="side-card card"
            :class="{ 'animate-rise': !skipRiseOnRestore }"
            style="--stagger: 2"
          >
            <div class="side-title-row">
              <h4 class="side-title">标签索引</h4>
              <button
                v-if="activeTagId"
                type="button"
                class="side-clear-btn"
                @click="selectTag(activeTagId)"
              >
                重置
              </button>
              <span v-else class="side-kicker">TAGS</span>
            </div>
            <div class="tag-cloud">
              <button
                v-for="tag in tags"
                :key="tag.id"
                type="button"
                class="tag-chip"
                :class="{ active: activeTagId === tag.id }"
                @click="selectTag(tag.id)"
              >
                <span>#{{ tag.name }}</span>
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
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  blogApi,
  type BlogArticleItem,
  type BlogCategory,
  type BlogTag,
} from "@/api/blog";
import {
  launchLeaveMorph,
  peekFreshHomeSnapshot,
  prefetchArticleDetail,
  prepareEnterMorph,
  saveHomeSnapshot,
  useArticleTransition,
  type ArticlePreviewSeed,
  type MorphOriginKind,
} from "@/composables/useArticleTransition";
import { useSiteInfo } from "@/composables/useSiteInfo";

const route = useRoute();
const router = useRouter();
const { siteInfo, loadSiteInfo } = useSiteInfo();
const { hiddenHomeArticleId, hiddenHomeOriginKind, isReturningToHome } = useArticleTransition();

const homePageRef = ref<HTMLElement | null>(null);

const initialSnapshot = peekFreshHomeSnapshot(route.fullPath);
const skipRiseOnRestore = ref(Boolean(initialSnapshot));

const loading = ref(false);
const articles = ref<BlogArticleItem[]>(initialSnapshot?.articles.map((item) => ({ ...item })) ?? []);
const total = ref(initialSnapshot?.total ?? 0);
const pageNum = ref(1);
const pageSize = 10;

const keywords = ref("");
const activeCategoryId = ref<string | undefined>(undefined);
const activeTagId = ref<string | undefined>(undefined);

const categories = ref<BlogCategory[]>(initialSnapshot?.categories.map((cat) => ({ ...cat })) ?? []);
const tags = ref<BlogTag[]>(initialSnapshot?.tags.map((tag) => ({ ...tag })) ?? []);

const dockSentinelRef = ref<HTMLElement | null>(null);
const searchBoxRef = ref<HTMLElement | null>(null);
const searchInputRef = ref<HTMLInputElement | null>(null);
const searchExpanded = ref(false);
const isDockStuck = ref(false);
let dockObserver: IntersectionObserver | null = null;

function openSearch(selectAll = false) {
  searchExpanded.value = true;
  nextTick(() => {
    searchInputRef.value?.focus();
    if (selectAll) {
      searchInputRef.value?.select();
    }
  });
}

function handleSearchButtonClick() {
  if (!searchExpanded.value) {
    openSearch(false);
    return;
  }
  const hasActiveQuery = typeof route.query.q === "string" && route.query.q.trim().length > 0;
  if (keywords.value.trim() || hasActiveQuery) {
    handleSearch();
    return;
  }
  searchExpanded.value = false;
  searchInputRef.value?.blur();
}

function handleDocumentClick(e: MouseEvent) {
  if (!searchExpanded.value || keywords.value.trim()) return;
  const target = e.target as Node | null;
  if (target && searchBoxRef.value?.contains(target)) return;
  searchExpanded.value = false;
}

function handleGlobalKeydown(e: KeyboardEvent) {
  const target = e.target as HTMLElement | null;
  const inEditable =
    target?.tagName === "INPUT" ||
    target?.tagName === "TEXTAREA" ||
    Boolean(target?.isContentEditable);

  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
    e.preventDefault();
    openSearch(true);
    return;
  }

  if (e.key === "/" && !inEditable) {
    e.preventDefault();
    openSearch(true);
    return;
  }

  if (e.key === "Escape" && (document.activeElement === searchInputRef.value || searchExpanded.value)) {
    searchInputRef.value?.blur();
    if (!keywords.value.trim()) {
      searchExpanded.value = false;
    }
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

function formatDate(val?: string) {
  if (!val) return "";
  return val.slice(0, 10);
}

function isPlainNavigationClick(e: MouseEvent) {
  return !e.metaKey && !e.ctrlKey && !e.shiftKey && !e.altKey && e.button === 0;
}

function warmupArticle(id: string) {
  void prefetchArticleDetail(id, () => blogApi.getArticleDetail(id));
}

function captureOriginOnCardClick(
  preview: ArticlePreviewSeed,
  originKind: MorphOriginKind,
  e: MouseEvent,
) {
  if (!isPlainNavigationClick(e)) return;
  const targetEl = e.target as HTMLElement | null;
  if (!targetEl?.closest('a[href^="/article/"]')) return;
  const cardEl = e.currentTarget as HTMLElement | null;
  if (!cardEl) return;

  warmupArticle(preview.id);
  saveHomeSnapshot({
    queryKey: route.fullPath,
    articles: articles.value,
    total: total.value,
    categories: categories.value,
    tags: tags.value,
    scrollY: window.scrollY,
  });
  prepareEnterMorph(preview, originKind, cardEl, homePageRef.value);
}

function captureHotOriginOnClick(
  hot: { id: string; title: string; publishTime?: string; viewCount: number },
  e: MouseEvent,
) {
  if (!isPlainNavigationClick(e)) return;
  const linkEl = e.currentTarget as HTMLElement | null;
  if (!linkEl) return;

  warmupArticle(hot.id);
  saveHomeSnapshot({
    queryKey: route.fullPath,
    articles: articles.value,
    total: total.value,
    categories: categories.value,
    tags: tags.value,
    scrollY: window.scrollY,
  });
  prepareEnterMorph(
    {
      id: hot.id,
      title: hot.title,
      publishTime: hot.publishTime,
      viewCount: Number(hot.viewCount || 0),
    },
    "hot",
    linkEl,
    homePageRef.value,
  );
}

async function fetchArticles(options: { silent?: boolean } = {}) {
  if (!options.silent) {
    loading.value = true;
  }
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
    for (const item of articles.value.slice(0, 5)) {
      warmupArticle(item.id);
    }
  } finally {
    if (!options.silent) {
      loading.value = false;
    }
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
  nextTick(() => {
    searchInputRef.value?.focus();
  });
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
  searchExpanded.value = false;
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
    if (keywords.value.trim()) {
      searchExpanded.value = true;
    }
    activeCategoryId.value = typeof q.categoryId === "string" ? q.categoryId : undefined;
    activeTagId.value = typeof q.tagId === "string" ? q.tagId : undefined;
    pageNum.value = typeof q.page === "string" ? Math.max(1, Number(q.page) || 1) : 1;
  },
  { immediate: true },
);

function resolveMorphReturnCard(articleId: string, preferredKind: MorphOriginKind) {
  const safeId = CSS.escape(articleId);
  const preferred = document.querySelector<HTMLElement>(
    `[data-morph-card="${preferredKind}"][data-article-id="${safeId}"]`,
  );
  if (preferred) {
    return { el: preferred, kind: preferredKind };
  }
  const fallbackKind: MorphOriginKind = preferredKind === "feed" ? "hot" : "feed";
  const fallback = document.querySelector<HTMLElement>(
    `[data-morph-card="${fallbackKind}"][data-article-id="${safeId}"]`,
  );
  return fallback ? { el: fallback, kind: fallbackKind } : null;
}

onMounted(async () => {
  window.addEventListener("keydown", handleGlobalKeydown);
  document.addEventListener("click", handleDocumentClick);
  if (dockSentinelRef.value) {
    dockObserver = new IntersectionObserver(
      ([entry]) => {
        isDockStuck.value = !entry.isIntersecting;
      },
      { rootMargin: "-79px 0px 0px 0px", threshold: 0 },
    );
    dockObserver.observe(dockSentinelRef.value);
  }

  void import("@/views/ArticleDetailView.vue");

  if (initialSnapshot) {
    window.scrollTo({ top: initialSnapshot.scrollY, behavior: "instant" });
    await nextTick();
    launchLeaveMorph(resolveMorphReturnCard);

    const [, catList, tagList] = await Promise.all([
      loadSiteInfo(),
      blogApi.getCategories(),
      blogApi.getTags(),
    ]);
    categories.value = catList;
    tags.value = tagList;
    await fetchArticles({ silent: true });
    return;
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
  document.removeEventListener("click", handleDocumentClick);
  dockObserver?.disconnect();
});
</script>

<style scoped>
.home-page {
  width: 100%;
}

.is-morph-hidden {
  opacity: 0 !important;
  visibility: hidden !important;
  pointer-events: none !important;
  transition: none !important;
}

/* 顶部出版物序言区（Masthead） */
.masthead {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 32px;
  padding: 10px 4px 24px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--border-subtle);
}

.masthead-left {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
  flex: 1 1 420px;
}

.masthead-avatar-wrap {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  background: var(--bg-glass-strong);
  box-shadow: 0 2px 10px -4px rgba(15, 23, 42, 0.14);
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1), border-color 0.2s ease;
}

.masthead-avatar-wrap:hover {
  transform: scale(1.04);
  border-color: var(--accent);
}

.masthead-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.masthead-avatar--fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.masthead-intro {
  min-width: 0;
}

.masthead-kicker {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  margin-bottom: 4px;
  font-size: 12.5px;
  color: var(--text-muted);
}

.masthead-author {
  font-weight: 600;
  color: var(--text-secondary);
}

.masthead-sep {
  opacity: 0.45;
}

.masthead-title {
  margin: 0;
  font-size: clamp(23px, 2.5vw, 30px);
  font-weight: 800;
  letter-spacing: -0.025em;
  line-height: 1.2;
  color: var(--text-primary);
}

.masthead-bio {
  margin: 7px 0 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-secondary);
  max-width: 60ch;
}

.masthead-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.masthead-metrics {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.masthead-metrics strong {
  color: var(--text-primary);
  font-weight: 700;
}

.metric-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--text-muted);
  opacity: 0.45;
}

.masthead-links {
  display: flex;
  align-items: center;
  gap: 14px;
}

.masthead-link {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  position: relative;
  transition: color 0.18s ease;
}

.masthead-link:hover {
  color: var(--accent);
}

/* 主双栏结构：左宽阅读列表 + 右 264px 轻盈边栏 */
.home-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 264px;
  gap: 34px;
  align-items: stretch;
}

.feed-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.dock-sentinel {
  height: 1px;
  margin-bottom: -21px;
  pointer-events: none;
}

/* 筛选与折叠搜索吸顶条 */
.filter-dock {
  position: sticky;
  top: 78px;
  z-index: 25;
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 7px 10px;
  border-radius: var(--radius-dock);
  transition:
    background-color 0.25s ease,
    box-shadow 0.25s ease,
    border-color 0.25s ease;
}

.filter-dock.is-stuck {
  background: var(--bg-glass-strong);
  backdrop-filter: blur(22px) saturate(160%);
  -webkit-backdrop-filter: blur(22px) saturate(160%);
  border-color: color-mix(in srgb, var(--text-primary) 12%, var(--border-glass));
  box-shadow: var(--shadow-glass-hover);
}

.dock-cats {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}

.dock-cats::-webkit-scrollbar {
  display: none;
}

.filter-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  padding: 6px 12px;
  border-radius: var(--radius-chip);
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    border-color 0.18s ease;
}

.filter-tab:hover {
  color: var(--text-primary);
  background: var(--bg-subtle);
}

.filter-tab.active {
  color: var(--text-primary);
  font-weight: 600;
  background: var(--bg-subtle);
  border-color: var(--border-subtle);
}

.tab-count {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: var(--text-muted);
}

.inline-tag-filter {
  background: var(--accent-soft);
  color: var(--accent);
  border-color: color-mix(in srgb, var(--accent) 32%, transparent);
  font-weight: 600;
}

.filter-dismiss {
  opacity: 0.72;
  font-size: 11px;
  padding-left: 2px;
}

/* 折叠式圆形搜索按钮：点击后向左平滑弹出输入框 */
.search-capsule {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  flex-shrink: 0;
  margin-left: auto;
  width: 38px;
  height: 38px;
  padding: 2px;
  gap: 0;
  border-radius: 19px;
  corner-shape: round;
  background: var(--bg-subtle);
  border: 1px solid var(--border-subtle);
  overflow: hidden;
  cursor: pointer;
  transition:
    width 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    padding 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    gap 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    border-radius 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.search-capsule:not(.is-expanded):hover {
  border-color: var(--text-muted);
}

.search-capsule:not(.is-expanded):hover .search-circle-btn {
  color: var(--text-primary);
}

.search-capsule.is-expanded {
  width: 288px;
  padding: 2px 2px 2px 14px;
  gap: 6px;
  border-radius: var(--radius-control);
  cursor: default;
}

.search-capsule.is-expanded:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.search-expand-inner {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  gap: 4px;
  opacity: 0;
  transform: translateX(8px);
  pointer-events: none;
  transition:
    opacity 0.18s ease,
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}

.search-capsule.is-expanded .search-expand-inner {
  opacity: 1;
  transform: translateX(0);
  pointer-events: auto;
  transition-delay: 0.06s;
}

.search-input {
  flex: 1;
  width: 100%;
  min-width: 0;
  border: none;
  background: transparent;
  color: var(--text-primary);
  outline: none;
  font-size: 13px;
}

.search-input::placeholder {
  color: var(--text-muted);
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

.clear-btn:hover {
  color: var(--text-primary);
}

.search-circle-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  border-radius: 16px;
  corner-shape: round;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition:
    border-radius 0.3s cubic-bezier(0.22, 1, 0.36, 1),
    background-color 0.22s ease,
    color 0.22s ease,
    transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.22s ease;
}

.search-circle-btn:active {
  transform: scale(0.92);
}

.search-capsule.is-expanded .search-circle-btn {
  border-radius: var(--radius-chip);
  background: linear-gradient(135deg, var(--accent), var(--accent-2));
  color: #ffffff;
}

html.dark .search-capsule.is-expanded .search-circle-btn {
  color: #06131a;
}

.search-circle-icon {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* 精选头条卡（克制纸感，无炫彩渐变背景/四方格） */
.featured-lead {
  padding: 28px 32px;
  background: var(--morph-surface);
  border-radius: var(--radius-card-sm);
}

.featured-lead.has-cover {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 28px;
  align-items: center;
}

.lead-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.lead-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.lead-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 9px;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-soft);
}

.mono-meta,
.mono-stat {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.meta-dot {
  color: var(--text-muted);
  opacity: 0.45;
  font-size: 12px;
}

.inline-cat-btn {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.18s ease;
}

.inline-cat-btn:hover {
  color: var(--accent);
}

.lead-issue {
  margin-left: auto;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--text-muted);
  opacity: 0.72;
}

.lead-title {
  margin: 0 0 12px;
  font-size: clamp(22px, 2.25vw, 27px);
  font-weight: 800;
  line-height: 1.32;
  letter-spacing: -0.022em;
  color: var(--text-primary);
}

.lead-title a {
  transition: color 0.18s ease;
}

.featured-lead:hover .lead-title a {
  color: var(--accent);
}

.lead-summary {
  margin: 0 0 20px;
  font-size: 14.5px;
  line-height: 1.78;
  color: var(--text-secondary);
  max-width: 66ch;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.lead-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid var(--border-subtle);
}

.lead-footer-right {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-left: auto;
}

.lead-cover {
  display: block;
  border-radius: var(--radius-inner);
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  aspect-ratio: 4 / 3;
}

.lead-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

.featured-lead:hover .lead-cover img {
  transform: scale(1.04);
}

/* 标签纯文本链接 */
.hash-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hash-tag-btn {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--text-muted);
  font-size: 12.5px;
  cursor: pointer;
  transition: color 0.18s ease;
}

.hash-tag-btn:hover {
  color: var(--accent);
}

.editorial-read-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  transition: color 0.18s ease;
}

.arrow-icon {
  width: 14px;
  height: 14px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
  transition: transform 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}

.featured-lead:hover .editorial-read-link,
.feed-row:hover .editorial-read-link {
  color: var(--accent);
}

.featured-lead:hover .arrow-icon,
.feed-row:hover .arrow-icon {
  transform: translateX(3px);
}

/* 极简线性编辑文章列表 */
.editorial-feed {
  display: flex;
  flex-direction: column;
}

.feed-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 4px 10px;
  border-bottom: 1px solid var(--border-subtle);
}

.feed-section-label {
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--text-muted);
}

.feed-section-count {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  color: var(--text-muted);
}

.feed-list {
  display: flex;
  flex-direction: column;
}

.feed-row {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 20px;
  padding: 22px 14px;
  margin: 0 -14px;
  border-radius: var(--radius-dock);
  border-bottom: 1px solid var(--border-subtle);
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.feed-row.has-thumb {
  grid-template-columns: 112px minmax(0, 1fr) 136px;
}

.feed-row:last-child {
  border-bottom-color: transparent;
}

.feed-row:hover {
  background: var(--bg-glass-strong);
  border-bottom-color: transparent;
}

.feed-date-col {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 3px;
}

.feed-date {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.feed-duration {
  font-size: 12px;
  color: var(--text-muted);
  opacity: 0.78;
}

.feed-body-col {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.feed-eyebrow {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.feed-ordinal {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--text-muted);
}

.feed-pin {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 5px;
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  background: var(--bg-subtle);
}

.feed-stats {
  margin-left: auto;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.feed-title {
  margin: 0 0 8px;
  font-size: 19px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: -0.012em;
  color: var(--text-primary);
}

.feed-title a {
  transition: color 0.18s ease;
}

.feed-row:hover .feed-title a {
  color: var(--accent);
}

.feed-summary {
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.72;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.feed-row-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.feed-thumb {
  display: block;
  width: 136px;
  height: 88px;
  border-radius: var(--radius-control);
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  align-self: center;
}

.feed-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}

.feed-row:hover .feed-thumb img {
  transform: scale(1.05);
}

/* 骨架屏 & 空状态 */
.skeleton-lead {
  padding: 28px 32px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-feed {
  display: flex;
  flex-direction: column;
}

.skeleton-row {
  padding: 20px 8px;
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-line {
  height: 13px;
  border-radius: 8px;
  background: linear-gradient(90deg, var(--bg-subtle) 25%, rgba(148, 163, 184, 0.18) 50%, var(--bg-subtle) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skeleton-line.h-lg {
  height: 22px;
}
.skeleton-line.w-1\/4 {
  width: 24%;
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

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 4px 0;
  border-top: 1px solid var(--border-subtle);
}

.page-info {
  font-size: 13px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

/* 右侧轻盈固定边栏 */
.side-col {
  min-width: 0;
}

.side-fixed-stack {
  position: sticky;
  top: 78px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: calc(100vh - 96px);
  overflow-y: auto;
  scrollbar-width: none;
  padding: 2px 4px 10px;
  margin: -2px -4px;
}

.side-fixed-stack::-webkit-scrollbar {
  display: none;
}

.side-card {
  padding: 16px 18px;
  border-radius: var(--radius-card-sm);
}

.side-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.side-title {
  margin: 0;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary);
}

.side-kicker {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10.5px;
  letter-spacing: 0.08em;
  color: var(--text-muted);
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
  gap: 4px;
}

.hot-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 8px;
  margin: 0 -8px;
  border-radius: var(--radius-chip);
  font-size: 13px;
  color: var(--text-secondary);
  transition: background-color 0.18s ease, color 0.18s ease;
}

.hot-link:hover {
  background: var(--bg-subtle);
  color: var(--text-primary);
}

.hot-rank {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11.5px;
  font-weight: 700;
  color: var(--text-muted);
  flex-shrink: 0;
  width: 20px;
}

.hot-rank.is-top {
  color: var(--accent);
}

.hot-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-views {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11.5px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: var(--radius-chip);
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease, background-color 0.18s ease;
}

.tag-chip:hover {
  color: var(--text-primary);
  border-color: var(--text-muted);
}

.tag-chip.active {
  color: var(--accent);
  background: var(--accent-soft);
  border-color: color-mix(in srgb, var(--accent) 42%, transparent);
  font-weight: 600;
}

.tag-count {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10.5px;
  opacity: 0.65;
}

@media (max-width: 960px) {
  .home-grid {
    grid-template-columns: 1fr;
    gap: 24px;
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
  .masthead {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
    padding-bottom: 18px;
  }

  .masthead-right {
    align-items: flex-start;
  }

  .featured-lead {
    padding: 22px 20px;
  }

  .featured-lead.has-cover {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .feed-row,
  .feed-row.has-thumb {
    grid-template-columns: 1fr;
    gap: 8px;
    padding: 18px 10px;
    margin: 0 -10px;
  }

  .feed-date-col {
    flex-direction: row;
    align-items: center;
    gap: 10px;
    padding-top: 0;
  }

  .feed-thumb {
    width: 100%;
    height: 170px;
  }
}

@supports (corner-shape: squircle) {
  .filter-tab,
  .search-capsule.is-expanded,
  .search-capsule.is-expanded .search-circle-btn,
  .lead-cover,
  .feed-row,
  .feed-thumb,
  .hot-link,
  .tag-chip {
    corner-shape: squircle;
  }
}

@media (max-width: 640px) {
  .filter-dock {
    position: static;
    gap: 8px;
  }

  .search-capsule.is-expanded {
    width: min(230px, 68vw);
  }
}
</style>
