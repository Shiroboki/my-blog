<template>
  <div class="card archives-card animate-rise">
    <div class="archives-head">
      <div>
        <h1 class="page-heading">文章归档</h1>
        <p class="page-sub">按时间脉络浏览共 {{ totalArticles }} 篇已发布文章</p>
      </div>
      <div class="archive-filter-bar">
        <input
          v-model="keyword"
          type="text"
          class="archive-search-input"
          placeholder="筛选标题或分类..."
          aria-label="筛选归档文章"
        />
        <button
          v-if="keyword || selectedCategory"
          type="button"
          class="archive-reset-btn"
          @click="resetArchiveFilters"
        >
          重置
        </button>
      </div>
    </div>

    <div v-if="availableCategories.length > 1" class="archive-cats">
      <button
        type="button"
        class="pill"
        :class="{ active: !selectedCategory }"
        @click="selectedCategory = ''"
      >
        全部分类
      </button>
      <button
        v-for="cat in availableCategories"
        :key="cat"
        type="button"
        class="pill"
        :class="{ active: selectedCategory === cat }"
        @click="selectedCategory = selectedCategory === cat ? '' : cat"
      >
        {{ cat }}
      </button>
    </div>

    <div v-if="loading" class="state-text">加载中...</div>
    <div v-else-if="!filteredGroups.length" class="state-text">
      {{ groups.length ? "当前筛选条件下没有匹配的文章" : "暂无已发布文章" }}
    </div>
    <div v-else class="timeline">
      <section
        v-for="(group, gIdx) in filteredGroups"
        :key="group.yearMonth"
        class="month-group animate-rise"
        :style="{ '--stagger': gIdx }"
      >
        <h2 class="month-title">{{ group.yearMonth }}（{{ group.articles.length }}）</h2>
        <ul class="archive-list">
          <li v-for="art in group.articles" :key="art.id" class="archive-item">
            <span class="archive-date">{{ (art.publishTime || "").slice(5, 10) }}</span>
            <RouterLink :to="getArticleRoute(art)" class="archive-link">
              {{ art.title }}
            </RouterLink>
            <button
              v-if="art.categoryName"
              type="button"
              class="pill"
              @click="selectedCategory = selectedCategory === art.categoryName ? '' : art.categoryName"
            >
              {{ art.categoryName }}
            </button>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { blogApi, getArticleRoute, type BlogArchiveGroup } from "@/api/blog";
import { useSiteInfo } from "@/composables/useSiteInfo";

const { loadSiteInfo } = useSiteInfo();

const loading = ref(false);
const groups = ref<BlogArchiveGroup[]>([]);
const keyword = ref("");
const selectedCategory = ref("");

const totalArticles = computed(() =>
  groups.value.reduce((sum, g) => sum + g.articles.length, 0),
);

const availableCategories = computed(() => {
  const seen = new Set<string>();
  for (const group of groups.value) {
    for (const art of group.articles) {
      if (art.categoryName) {
        seen.add(art.categoryName);
      }
    }
  }
  return Array.from(seen);
});

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

function resetArchiveFilters() {
  keyword.value = "";
  selectedCategory.value = "";
}

onMounted(async () => {
  void loadSiteInfo();
  loading.value = true;
  try {
    groups.value = await blogApi.getArchives();
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.archives-card {
  padding: 30px 34px;
}

.archives-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px 20px;
  margin-bottom: 18px;
}

.page-heading {
  margin: 0 0 4px;
  font-size: 24px;
}

.page-sub {
  margin: 0;
  font-size: 13.5px;
  color: var(--text-muted);
}

.archive-filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.archive-search-input {
  width: 220px;
  padding: 7px 12px;
  border-radius: var(--radius-control);
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.archive-search-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.archive-search-input::placeholder {
  color: var(--text-muted);
}

.archive-reset-btn {
  border: none;
  background: transparent;
  color: var(--accent);
  font-size: 12.5px;
  cursor: pointer;
  padding: 4px 6px;
}

.archive-cats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 22px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--border-subtle);
}

.state-text {
  padding: 36px 0;
  text-align: center;
  color: var(--text-muted);
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 26px;
}

.month-title {
  margin: 0 0 12px;
  font-size: 17px;
  color: var(--accent);
}

.archive-list {
  list-style: none;
  padding: 0 0 0 16px;
  margin: 0;
  border-left: 2px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.archive-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 6px 10px;
  border-radius: var(--radius-control);
  transition: background-color 0.2s ease, transform 0.2s ease;
}

@supports (corner-shape: squircle) {
  .archive-item,
  .archive-search-input {
    corner-shape: squircle;
  }
}

.archive-item:hover {
  background: var(--bg-subtle);
  transform: translateX(4px);
}

.archive-date {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.archive-link {
  font-weight: 500;
}

.archive-link:hover {
  color: var(--accent);
}

@media (max-width: 640px) {
  .archives-card {
    padding: 24px 20px;
  }

  .archive-search-input {
    width: 100%;
    flex: 1;
  }

  .archive-filter-bar {
    width: 100%;
  }
}
</style>
