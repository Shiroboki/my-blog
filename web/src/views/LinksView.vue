<template>
  <section class="links-wrap">
    <div class="card links-header animate-rise">
      <h1 class="page-heading">友情链接</h1>
      <p class="page-sub">同路人的技术站点、文档参考与优质博客收藏</p>
    </div>

    <div v-if="loading" class="card empty-box">正在加载友链列表...</div>
    <div v-else-if="!links.length" class="card empty-box">暂无友链，可在管理后台添加启用友链</div>

    <div v-else class="links-grid">
      <a
        v-for="link in links"
        :key="link.id"
        :href="link.url"
        target="_blank"
        rel="noopener noreferrer"
        class="card card-interactive link-card"
      >
        <div class="link-avatar-wrap">
          <img
            v-if="link.avatar"
            :src="link.avatar"
            :alt="link.name"
            class="link-avatar"
          />
          <span v-else class="link-monogram" :style="monogramStyle(link.name)">
            {{ (link.name || "友").slice(0, 1).toUpperCase() }}
          </span>
        </div>
        <div class="link-body">
          <div class="link-title-row">
            <span class="link-name">{{ link.name }}</span>
            <span class="link-host">{{ extractHost(link.url) }}</span>
          </div>
          <p class="link-desc">{{ link.description || "点击前往访问该站点" }}</p>
        </div>
      </a>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { blogApi, type BlogLinkItem } from "@/api/blog";

const links = ref<BlogLinkItem[]>([]);
const loading = ref(false);

const PALETTES = [
  "linear-gradient(135deg, #0d9488, #0284c7)",
  "linear-gradient(135deg, #0284c7, #6366f1)",
  "linear-gradient(135deg, #0f766e, #059669)",
  "linear-gradient(135deg, #d97706, #ea580c)",
  "linear-gradient(135deg, #7c3aed, #db2777)",
];

function monogramStyle(name: string) {
  let hash = 0;
  for (let i = 0; i < (name || "").length; i++) {
    hash = (hash * 31 + name.charCodeAt(i)) % PALETTES.length;
  }
  return { background: PALETTES[Math.abs(hash) % PALETTES.length] };
}

function extractHost(url: string) {
  try {
    return new URL(url).hostname;
  } catch {
    return url;
  }
}

onMounted(async () => {
  loading.value = true;
  try {
    links.value = await blogApi.getLinks();
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.links-wrap {
  max-width: 960px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.links-header {
  padding: 26px 30px;
}

.page-heading {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 800;
}

.page-sub {
  margin: 0;
  font-size: 14px;
  color: var(--text-muted);
}

.empty-box {
  padding: 56px 24px;
  text-align: center;
  color: var(--text-muted);
}

.links-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.link-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.3s ease;
}

.link-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-glass-hover);
}

.link-avatar-wrap {
  flex-shrink: 0;
}

.link-avatar,
.link-monogram {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-dock);
  display: flex;
  align-items: center;
  justify-content: center;
}

@supports (corner-shape: squircle) {
  .link-avatar,
  .link-monogram {
    corner-shape: squircle;
  }
}

.link-avatar {
  object-fit: cover;
  border: 1px solid var(--border-subtle);
}

.link-monogram {
  color: #ffffff;
  font-size: 20px;
  font-weight: 800;
}

.link-body {
  min-width: 0;
  flex: 1;
}

.link-title-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.link-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.link-host {
  font-size: 12px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.link-desc {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 720px) {
  .links-grid {
    grid-template-columns: 1fr;
  }
}
</style>
