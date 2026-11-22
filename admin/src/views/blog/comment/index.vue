<template>
  <div class="page-container">
    <el-card ref="tableWrapperRef" class="page-content" shadow="never">
      <div class="page-filter">
        <el-form ref="queryFormRef" :model="params" :inline="true">
          <el-form-item label="关键字" prop="keywords">
            <el-input
              v-model="params.keywords"
              placeholder="昵称/评论内容"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="params.status" placeholder="全部" clearable class="!w-[140px]">
              <el-option :value="0" label="待审核" />
              <el-option :value="1" label="已通过" />
              <el-option :value="2" label="已拒绝" />
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
            v-hasPerm="['blog:comment:delete']"
            type="danger"
            :disabled="!hasSelection"
            @click="handleDelete()"
          >
            批量删除
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
          highlight-current-row
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="评论者" width="160">
            <template #default="{ row }">
              <div class="flex items-center gap-1.5">
                <span>{{ row.nickname }}</span>
                <el-tag v-if="row.isAdmin === 1" size="small" type="primary">博主</el-tag>
              </div>
              <div v-if="row.email" class="text-xs text-gray-400">{{ row.email }}</div>
            </template>
          </el-table-column>
          <el-table-column label="所属文章" prop="articleTitle" min-width="180" show-overflow-tooltip />
          <el-table-column label="评论内容" min-width="280">
            <template #default="{ row }">
              <div
                v-if="(row as BlogCommentItem).quoteText"
                class="mb-1 text-xs text-teal-700 bg-teal-50/70 border border-dashed border-teal-300 rounded px-2 py-1 line-clamp-2"
              >
                引用原文：{{ (row as BlogCommentItem).quoteText }}
              </div>
              <div class="text-sm leading-relaxed">{{ (row as BlogCommentItem).content }}</div>
            </template>
          </el-table-column>
          <el-table-column label="IP 属地" prop="ipRegion" width="120" align="center" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 0" type="warning">待审核</el-tag>
              <el-tag v-else-if="row.status === 1" type="success">已通过</el-tag>
              <el-tag v-else type="danger">已拒绝</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="评论时间" prop="createTime" width="175" align="center" />
          <el-table-column label="操作" fixed="right" width="220" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status !== 1"
                v-hasPerm="['blog:comment:audit']"
                type="success"
                size="small"
                link
                @click="handleAudit(row.id, 1)"
              >
                通过
              </el-button>
              <el-button
                v-if="row.status !== 2"
                v-hasPerm="['blog:comment:audit']"
                type="warning"
                size="small"
                link
                @click="handleAudit(row.id, 2)"
              >
                拒绝
              </el-button>
              <el-button
                v-hasPerm="['blog:comment:reply']"
                type="primary"
                size="small"
                link
                @click="openReply(row as BlogCommentItem)"
              >
                回复
              </el-button>
              <el-button
                v-hasPerm="['blog:comment:delete']"
                type="danger"
                size="small"
                link
                @click="handleDelete(row.id)"
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

    <el-dialog v-model="replyVisible" title="博主回复评论" width="480px" append-to-body>
      <div class="mb-3 rounded bg-gray-50 p-3 text-sm text-gray-600 dark:bg-gray-800 dark:text-gray-300">
        回复 @{{ replyTarget?.nickname }}：{{ replyTarget?.content }}
      </div>
      <el-input
        v-model="replyContent"
        type="textarea"
        :rows="4"
        maxlength="1000"
        show-word-limit
        placeholder="请输入回复内容"
      />
      <template #footer>
        <el-button type="primary" :loading="replying" @click="submitReply">发送回复</el-button>
        <el-button @click="replyVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useFullscreen } from "@vueuse/core";
import { FullScreen, Refresh } from "@element-plus/icons-vue";
import type { FormInstance } from "element-plus";
import { BlogCommentAPI, type BlogCommentItem, type BlogCommentQuery } from "@/api/blog";
import { usePageTable, useTableSelection } from "@/composables";

defineOptions({
  name: "BlogComment",
  inheritAttrs: false,
});

const tableWrapperRef = ref<HTMLElement | null>(null);
const { toggle: toggleFullscreen } = useFullscreen(tableWrapperRef);
const queryFormRef = ref<FormInstance>();

const { loading, list, total, params, fetchData, handleQuery, handleResetQuery } = usePageTable<
  BlogCommentItem,
  BlogCommentQuery
>({
  initialParams: { pageNum: 1, pageSize: 10, keywords: "", status: undefined },
  request: (query) => BlogCommentAPI.getPage(query),
});

const { selectedIds, hasSelection, handleSelectionChange, clearSelection } =
  useTableSelection<BlogCommentItem>();

const replyVisible = ref(false);
const replying = ref(false);
const replyTarget = ref<BlogCommentItem | null>(null);
const replyContent = ref("");

async function handleAudit(id: string, status: number) {
  await BlogCommentAPI.audit(id, status);
  ElMessage.success(status === 1 ? "已通过审核" : "已拒绝该评论");
  await fetchData();
}

function openReply(row: BlogCommentItem) {
  replyTarget.value = row;
  replyContent.value = "";
  replyVisible.value = true;
}

async function submitReply() {
  if (!replyTarget.value || !replyContent.value.trim()) {
    ElMessage.warning("请输入回复内容");
    return;
  }
  replying.value = true;
  try {
    await BlogCommentAPI.reply({
      articleId: replyTarget.value.articleId,
      parentId: replyTarget.value.id,
      content: replyContent.value.trim(),
    });
    ElMessage.success("回复成功");
    replyVisible.value = false;
    await fetchData();
  } finally {
    replying.value = false;
  }
}

async function handleDelete(id?: string) {
  const ids = id ? [id] : selectedIds.value;
  if (!ids.length) return;
  await ElMessageBox.confirm("确认删除所选评论（含关联子回复）吗？", "提示", { type: "warning" });
  await BlogCommentAPI.deleteByIds(ids.join(","));
  ElMessage.success("删除成功");
  clearSelection();
  await fetchData();
}

onMounted(() => {
  fetchData();
});
</script>
