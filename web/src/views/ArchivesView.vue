<template>
  <div class="card archives-card animate-rise">
    <h1 class="page-heading">文章归档</h1>
    <div v-if="loading" class="state-text">加载中...</div>
    <div v-else-if="!groups.length" class="state-text">暂无已发布文章</div>
    <div v-else class="timeline">
      <section v-for="(group, gIdx) in groups" :key="group.yearMonth" class="month-group animate-rise" :style="{ '--stagger': gIdx }">
        <h2 class="month-title">{{ group.yearMonth }}（{{ group.articles.length }}）</h2>
        <ul class="archive-list">
          <li v-for="art in group.articles" :key="art.id" class="archive-item">
            <span class="archive-date">{{ (art.publishTime || "").slice(5, 10) }}</span>
            <RouterLink :to="`/article/${art.id}`" class="archive-link">{{ art.title }}</RouterLink>
            <span v-if="art.categoryName" class="pill">{{ art.categoryName }}</span>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { blogApi, type BlogArchiveGroup } from "@/api/blog";

const loading = ref(false);
const groups = ref<BlogArchiveGroup[]>([]);

onMounted(async () => {
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

.page-heading {
  margin: 0 0 24px;
  font-size: 24px;
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
  .archive-item {
    corner-shape: squircle;
  }
}

.archive-item:hover {
  background: var(--bg-subtle);
  transform: translateX(4px);
}

.archive-date {
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
</style>
