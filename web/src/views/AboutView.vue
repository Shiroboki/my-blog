<template>
  <section class="about-page">
    <div class="card about-card" :class="{ 'animate-rise': !hydratedFromCache }">
      <div v-if="siteInfo" class="about-author">
        <div class="about-avatar-ring">
          <img
            v-if="siteInfo.authorAvatar"
            :src="siteInfo.authorAvatar"
            :alt="siteInfo.authorName"
            class="about-avatar"
          />
        </div>
        <div>
          <h2 class="about-name">{{ siteInfo.authorName }}</h2>
          <p class="about-bio">{{ siteInfo.authorBio }}</p>
        </div>
      </div>
      <div class="markdown-body" v-html="rendered.html" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted } from "vue";
import { useSiteInfo } from "@/composables/useSiteInfo";
import { renderMarkdown } from "@/utils/markdown";

const { siteInfo, loadSiteInfo } = useSiteInfo();
const hydratedFromCache = Boolean(siteInfo.value);

const rendered = computed(() =>
  renderMarkdown(siteInfo.value?.aboutContent || "# 关于本站\n\n暂无介绍内容。"),
);

onMounted(() => {
  loadSiteInfo();
});
</script>

<style scoped>
.about-page {
  width: 100%;
}

.about-card {
  padding: 30px 34px;
}

.about-author {
  display: flex;
  align-items: center;
  gap: 18px;
  padding-bottom: 22px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--border-subtle);
}

.about-avatar-ring {
  width: 74px;
  height: 74px;
  padding: 3px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--accent-2), #f472b6);
  flex-shrink: 0;
}

.about-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--bg-glass-strong);
}

.about-name {
  margin: 0 0 4px;
  font-size: 20px;
}

.about-bio {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}
</style>
