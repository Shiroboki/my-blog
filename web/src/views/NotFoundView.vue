<template>
  <div class="not-found-page">
    <section class="card not-found-card animate-rise">
      <span class="not-found-code">404</span>
      <h1 class="not-found-title">页面不存在或内容已下线</h1>
      <p class="not-found-desc">
        你访问的地址可能已变更或暂未公开发布，可以从首页或文章归档继续浏览。
      </p>
      <div class="not-found-actions">
        <RouterLink to="/" class="btn btn-primary">返回首页</RouterLink>
        <RouterLink to="/archives" class="btn">浏览文章归档</RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.not-found-page {
  width: 100%;
}

.not-found-card {
  max-width: 640px;
  margin: 40px auto;
  padding: 52px 32px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.not-found-code {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 14px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.not-found-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
}

.not-found-desc {
  margin: 0;
  max-width: 44ch;
  font-size: 14.5px;
  line-height: 1.75;
  color: var(--text-secondary);
}

.not-found-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 8px;
}
</style>
