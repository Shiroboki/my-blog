<template>
  <div v-loading="loading" class="page-container blog-overview">
    <el-row :gutter="16" class="kpi-grid">
      <el-col :xs="12" :sm="8" :lg="4">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">已发布文章</div>
          <div class="kpi-value">{{ stats.publishedCount }}</div>
          <div class="kpi-sub">草稿 {{ stats.draftCount }} 篇</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">累计阅读量</div>
          <div class="kpi-value">{{ stats.totalViews }}</div>
          <div class="kpi-sub">全站公开文章汇总</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">累计获赞数</div>
          <div class="kpi-value">{{ stats.totalLikes }}</div>
          <div class="kpi-sub">访客点赞互动总计</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">已过审评论</div>
          <div class="kpi-value">{{ stats.approvedCommentCount }}</div>
          <div class="kpi-sub">待审核 {{ stats.pendingCommentCount }} 条</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">分类与标签</div>
          <div class="kpi-value">{{ stats.categoryCount }} / {{ stats.tagCount }}</div>
          <div class="kpi-sub">内容组织维度</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :lg="4">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-label">已启用友链</div>
          <div class="kpi-value">{{ stats.linkCount }}</div>
          <div class="kpi-sub">外部推荐站点</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="quick-bar">
      <div class="quick-bar__inner">
        <div class="quick-bar__title">快捷操作</div>
        <div class="quick-bar__actions">
          <el-button type="primary" @click="goTo('/blog/article')">文章管理与写作</el-button>
          <el-button @click="goTo('/blog/comment')">评论管理</el-button>
          <el-button @click="goTo('/blog/link')">友链管理</el-button>
          <el-button @click="goTo('/blog/setting')">站点设置</el-button>
          <el-button @click="openExternal('/')">访问博客前台</el-button>
          <el-button @click="openExternal('/rss.xml')">查看 RSS</el-button>
          <el-button @click="loadStats">刷新统计</el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>热门文章阅读与点赞排行</span>
              <el-text type="info" size="small">按阅读量取前 8 篇已发布文章</el-text>
            </div>
          </template>
          <ECharts v-if="stats.topArticles.length" :options="topChartOptions" height="320px" />
          <el-empty v-else description="暂无已发布文章数据" :image-size="72" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-header">
              <span>分类文章分布</span>
              <el-text type="info" size="small">各分类下已发布文章数量</el-text>
            </div>
          </template>
          <ECharts v-if="stats.categoryStats.length" :options="categoryChartOptions" height="320px" />
          <el-empty v-else description="暂无分类分布数据" :image-size="72" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="panel-header">
          <span>待审核评论快速处理</span>
          <el-button type="primary" link @click="goTo('/blog/comment')">进入全部评论</el-button>
        </div>
      </template>
      <el-table v-if="stats.pendingComments.length" :data="stats.pendingComments" border>
        <el-table-column label="访客昵称" prop="nickname" width="130" />
        <el-table-column label="所属文章" prop="articleTitle" min-width="180" show-overflow-tooltip />
        <el-table-column label="评论内容" prop="content" min-width="260" show-overflow-tooltip />
        <el-table-column label="归属地" prop="ipRegion" width="130" />
        <el-table-column label="提交时间" prop="createTime" width="175" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-hasPerm="['blog:comment:audit']"
              type="success"
              size="small"
              link
              @click="handleAudit((row as BlogCommentItem).id, 1)"
            >
              通过
            </el-button>
            <el-button
              v-hasPerm="['blog:comment:audit']"
              type="danger"
              size="small"
              link
              @click="handleAudit((row as BlogCommentItem).id, 2)"
            >
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="当前没有待审核评论" :image-size="72" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import type { EChartsCoreOption } from "echarts/core";
import { useRouter } from "vue-router";
import {
  BlogCommentAPI,
  BlogDashboardAPI,
  type BlogCommentItem,
  type BlogDashboardStats,
} from "@/api/blog";

defineOptions({
  name: "BlogOverview",
  inheritAttrs: false,
});

const router = useRouter();
const loading = ref(false);

const stats = reactive<BlogDashboardStats>({
  publishedCount: 0,
  draftCount: 0,
  categoryCount: 0,
  tagCount: 0,
  totalViews: 0,
  totalLikes: 0,
  approvedCommentCount: 0,
  pendingCommentCount: 0,
  linkCount: 0,
  topArticles: [],
  categoryStats: [],
  pendingComments: [],
});

const topChartOptions = computed<EChartsCoreOption>(() => {
  const titles = stats.topArticles.map((item) =>
    item.title.length > 14 ? `${item.title.slice(0, 14)}...` : item.title
  );
  const views = stats.topArticles.map((item) => Number(item.viewCount ?? 0));
  const likes = stats.topArticles.map((item) => Number(item.likeCount ?? 0));

  return {
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
    },
    legend: {
      top: 0,
      data: ["阅读量", "点赞数"],
    },
    grid: {
      left: 16,
      right: 16,
      bottom: 16,
      top: 38,
      containLabel: true,
    },
    xAxis: {
      type: "category",
      data: titles,
      axisLabel: {
        interval: 0,
        rotate: titles.length > 5 ? 20 : 0,
      },
    },
    yAxis: {
      type: "value",
      minInterval: 1,
    },
    series: [
      {
        name: "阅读量",
        type: "bar",
        barMaxWidth: 28,
        data: views,
        itemStyle: { borderRadius: [6, 6, 0, 0] },
      },
      {
        name: "点赞数",
        type: "bar",
        barMaxWidth: 28,
        data: likes,
        itemStyle: { borderRadius: [6, 6, 0, 0] },
      },
    ],
  };
});

const categoryChartOptions = computed<EChartsCoreOption>(() => ({
  tooltip: {
    trigger: "item",
    formatter: "{b}: {c} 篇 ({d}%)",
  },
  legend: {
    bottom: 0,
  },
  series: [
    {
      name: "文章数",
      type: "pie",
      radius: ["44%", "70%"],
      center: ["50%", "44%"],
      itemStyle: {
        borderRadius: 8,
        borderColor: "#fff",
        borderWidth: 2,
      },
      label: {
        formatter: "{b}\n{c} 篇",
      },
      data: stats.categoryStats.map((item) => ({
        name: item.name,
        value: Number(item.value ?? 0),
      })),
    },
  ],
}));

async function loadStats() {
  loading.value = true;
  try {
    const data = await BlogDashboardAPI.getStats();
    Object.assign(stats, data);
  } finally {
    loading.value = false;
  }
}

async function handleAudit(id: string, status: number) {
  await BlogCommentAPI.audit(id, status);
  ElMessage.success(status === 1 ? "已通过审核" : "已拒绝该评论");
  await loadStats();
}

function goTo(path: string) {
  router.push(path);
}

function openExternal(url: string) {
  window.open(url, "_blank", "noopener");
}

onMounted(() => {
  loadStats();
});
</script>

<style scoped lang="scss">
.blog-overview {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.kpi-grid {
  row-gap: 16px;
}

.kpi-card {
  border-radius: 10px;
}

.kpi-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.kpi-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.2;
}

.kpi-sub {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.quick-bar__inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.quick-bar__title {
  font-weight: 600;
  font-size: 14px;
}

.quick-bar__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chart-row {
  row-gap: 16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 600;
}
</style>
