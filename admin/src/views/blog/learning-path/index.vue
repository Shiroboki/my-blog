<template>
  <div class="page-container">
    <el-card ref="tableWrapperRef" class="page-content" shadow="never">
      <div class="page-filter">
        <el-form ref="queryFormRef" :model="params" :inline="true">
          <el-form-item label="关键字" prop="keywords">
            <el-input
              v-model="params.keywords"
              placeholder="路径标题 / Slug / 简介"
              clearable
              style="width: 220px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="params.status" placeholder="全部状态" clearable style="width: 130px">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">搜索</el-button>
            <el-button @click="handleResetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-button
            v-hasPerm="['blog:learning-path:create']"
            type="primary"
            @click="openStudio()"
          >
            新增学习路径
          </el-button>
          <el-button
            v-hasPerm="['blog:learning-path:delete']"
            type="danger"
            :disabled="!hasSelection"
            @click="handleDelete()"
          >
            删除
          </el-button>
        </div>
        <div class="page-toolbar__right">
          <el-tooltip content="刷新" placement="top">
            <el-button class="page-icon-btn" @click="fetchData">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="全屏" placement="top">
            <el-button class="page-icon-btn" @click="toggleFullscreen">
              <el-icon><FullScreen /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <div class="page-table-wrapper">
        <el-table
          v-loading="loading"
          :data="list"
          class="page-table"
          border
          height="100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" align="center" />
          <el-table-column label="学习路径" min-width="220">
            <template #default="{ row }">
              <div class="path-title-cell">
                <span
                  class="color-dot"
                  :style="{ background: (row as BlogLearningPathItem).coverColor || '#0d9488' }"
                />
                <div class="path-meta">
                  <div class="path-title">{{ (row as BlogLearningPathItem).title }}</div>
                  <div class="path-slug">/paths/{{ (row as BlogLearningPathItem).slug }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            prop="summary"
            label="路径简介"
            min-width="240"
            show-overflow-tooltip
          />
          <el-table-column label="规模" width="150" align="center">
            <template #default="{ row }">
              <span>
                {{ (row as BlogLearningPathItem).nodeCount }} 节点 /
                {{ (row as BlogLearningPathItem).resourceCount }} 资源
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="82" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="(row as BlogLearningPathItem).status === 1 ? 'success' : 'info'">
                {{ (row as BlogLearningPathItem).status === 1 ? "启用" : "停用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" width="170" />
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button
                v-hasPerm="['blog:learning-path:update']"
                link
                type="primary"
                @click="openStudio((row as BlogLearningPathItem).id)"
              >
                编辑导图
              </el-button>
              <el-button
                link
                type="info"
                @click="openPublicPreview(row as BlogLearningPathItem)"
              >
                预览
              </el-button>
              <el-button
                v-hasPerm="['blog:learning-path:delete']"
                link
                type="danger"
                @click="handleDelete((row as BlogLearningPathItem).id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <pagination
        v-if="total > 0"
        v-model:total="total"
        v-model:page="params.pageNum"
        v-model:limit="params.pageSize"
        @pagination="fetchData"
      />
    </el-card>

    <MindMapStudioDialog
      v-model="studioVisible"
      :path-id="editingPathId"
      @saved="fetchData"
    />
  </div>
</template>

<script setup lang="ts">
import { useFullscreen } from "@vueuse/core";
import { FullScreen, Refresh } from "@element-plus/icons-vue";
import type { FormInstance } from "element-plus";
import {
  BlogLearningPathAPI,
  type BlogLearningPathItem,
  type BlogLearningPathQuery,
} from "@/api/blog";
import { usePageTable, useTableSelection } from "@/composables";
import MindMapStudioDialog from "./components/MindMapStudioDialog.vue";

defineOptions({
  name: "BlogLearningPath",
  inheritAttrs: false,
});

const tableWrapperRef = ref<HTMLElement | null>(null);
const { toggle: toggleFullscreen } = useFullscreen(tableWrapperRef);
const queryFormRef = ref<FormInstance>();

const { loading, list, total, params, fetchData, handleQuery, handleResetQuery } = usePageTable<
  BlogLearningPathItem,
  BlogLearningPathQuery
>({
  initialParams: { pageNum: 1, pageSize: 10, keywords: "", status: undefined },
  request: (query) => BlogLearningPathAPI.getPage(query),
});

const { selectedIds, hasSelection, handleSelectionChange, clearSelection } =
  useTableSelection<BlogLearningPathItem>();

const studioVisible = ref(false);
const editingPathId = ref<string | undefined>(undefined);

function openStudio(id?: string) {
  editingPathId.value = id;
  studioVisible.value = true;
}

function openPublicPreview(item: BlogLearningPathItem) {
  const target = `/paths/${encodeURIComponent(item.slug || item.id)}`;
  window.open(target, "_blank", "noopener,noreferrer");
}

async function handleDelete(id?: string) {
  const ids = id ? [id] : selectedIds.value;
  if (!ids.length) return;
  await ElMessageBox.confirm("确认删除所选学习路径及其思维导图吗？", "提示", {
    type: "warning",
  });
  await BlogLearningPathAPI.deleteByIds(ids.join(","));
  ElMessage.success("删除成功");
  clearSelection();
  await fetchData();
}

onMounted(() => {
  void fetchData();
});
</script>

<style scoped lang="scss">
.path-title-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.color-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.path-meta {
  min-width: 0;
}

.path-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.path-slug {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
