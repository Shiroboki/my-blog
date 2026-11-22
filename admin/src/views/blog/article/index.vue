<template>
  <div class="page-container">
    <el-card ref="tableWrapperRef" class="page-content" shadow="never">
      <div class="page-filter">
        <el-form ref="queryFormRef" :model="params" :inline="true">
          <el-form-item label="关键字" prop="keywords">
            <el-input
              v-model="params.keywords"
              placeholder="文章标题/摘要"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="params.categoryId" placeholder="全部分类" clearable class="!w-[150px]">
              <el-option
                v-for="item in categoryOptions"
                :key="String(item.value)"
                :label="item.label"
                :value="String(item.value)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标签" prop="tagId">
            <el-select v-model="params.tagId" placeholder="全部标签" clearable class="!w-[150px]">
              <el-option
                v-for="item in tagOptions"
                :key="String(item.value)"
                :label="item.label"
                :value="String(item.value)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="params.status" placeholder="全部" clearable class="!w-[120px]">
              <el-option :value="1" label="已发布" />
              <el-option :value="0" label="草稿" />
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
            v-hasPerm="['blog:article:create']"
            type="primary"
            @click="openEditor()"
          >
            写文章
          </el-button>
          <el-button
            v-hasPerm="['blog:article:create']"
            :loading="importing"
            @click="triggerImport"
          >
            导入 Markdown
          </el-button>
          <input
            ref="importInputRef"
            type="file"
            accept=".md,.markdown,text/markdown"
            class="hidden"
            @change="handleImportFile"
          />
          <el-button
            v-hasPerm="['blog:article:delete']"
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
          <el-table-column label="文章标题" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="flex items-center gap-2">
                <el-tag v-if="row.isTop === 1" size="small" type="danger" effect="dark">置顶</el-tag>
                <span class="font-medium">{{ row.title }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="分类" prop="categoryName" width="120" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.categoryName" size="small" effect="plain">
                {{ row.categoryName }}
              </el-tag>
              <span v-else class="text-gray-400">未分类</span>
            </template>
          </el-table-column>
          <el-table-column label="标签" min-width="180">
            <template #default="{ row }">
              <div class="flex flex-wrap gap-1">
                <el-tag
                  v-for="tag in row.tags || []"
                  :key="tag.id"
                  size="small"
                  effect="plain"
                  :style="tag.color ? { borderColor: tag.color, color: tag.color } : undefined"
                >
                  {{ tag.name }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="发布状态" width="105" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                inline-prompt
                active-text="发布"
                inactive-text="草稿"
                @change="(val) => handleToggleStatus(row as BlogArticleItem, Boolean(val))"
              />
            </template>
          </el-table-column>
          <el-table-column label="置顶" width="85" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.isTop === 1"
                @change="(val) => handleToggleTop(row as BlogArticleItem, Boolean(val))"
              />
            </template>
          </el-table-column>
          <el-table-column label="互动数据" width="160" align="center">
            <template #default="{ row }">
              <span class="text-xs text-gray-500">
                阅读 {{ row.viewCount ?? 0 }} · 赞 {{ row.likeCount ?? 0 }} · 评 {{ row.commentCount ?? 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="发布时间" prop="publishTime" width="170" align="center">
            <template #default="{ row }">
              {{ row.publishTime || "-" }}
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="230" align="center">
            <template #default="{ row }">
              <el-button
                v-if="(row as BlogArticleItem).status === 1"
                type="info"
                size="small"
                link
                @click="handlePreview((row as BlogArticleItem).id)"
              >
                预览
              </el-button>
              <el-button
                size="small"
                link
                @click="handleExport(row as BlogArticleItem)"
              >
                导出
              </el-button>
              <el-button
                v-hasPerm="['blog:article:update']"
                type="primary"
                size="small"
                link
                @click="openEditor((row as BlogArticleItem).id)"
              >
                编辑
              </el-button>
              <el-button
                v-hasPerm="['blog:article:delete']"
                type="danger"
                size="small"
                link
                @click="handleDelete((row as BlogArticleItem).id)"
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

    <el-dialog
      v-model="editorVisible"
      :title="formData.id ? '编辑文章' : '写新文章'"
      width="92%"
      top="3vh"
      append-to-body
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :xs="24" :md="16">
            <el-form-item label="文章标题" prop="title">
              <el-input v-model="formData.title" placeholder="请输入清晰吸引人的文章标题" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="URL 别名" prop="slug">
              <el-input v-model="formData.slug" placeholder="选填，例如 spring-boot-blog" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :xs="24" :md="8">
            <el-form-item label="所属分类" prop="categoryId">
              <el-select v-model="formData.categoryId" placeholder="选择分类" clearable class="w-full">
                <el-option
                  v-for="item in categoryOptions"
                  :key="String(item.value)"
                  :label="item.label"
                  :value="String(item.value)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="10">
            <el-form-item label="文章标签" prop="tagIds">
              <el-select
                v-model="formData.tagIds"
                multiple
                collapse-tags
                placeholder="选择标签"
                class="w-full"
              >
                <el-option
                  v-for="item in tagOptions"
                  :key="String(item.value)"
                  :label="item.label"
                  :value="String(item.value)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="6">
            <el-form-item label="发布设置">
              <div class="flex items-center gap-4">
                <el-checkbox v-model="isTopChecked">置顶</el-checkbox>
                <el-checkbox v-model="allowCommentChecked">允许评论</el-checkbox>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :xs="24" :md="18">
            <el-form-item label="文章摘要" prop="summary">
              <el-input
                v-model="formData.summary"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="留空将在保存时自动从 Markdown 正文提取前 160 字"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="6">
            <el-form-item label="封面图" prop="coverImage">
              <SingleImageUpload
                v-model="formData.coverImage"
                :style="{ width: '160px', height: '90px' }"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="文章正文" prop="content">
          <div class="w-full">
            <MarkdownEditor v-model="formData.content" height="420px" />
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex flex-wrap items-center justify-between gap-2 w-full">
          <span class="text-xs text-gray-500">
            {{ draftStatusText || "支持本地草稿自动暂存" }}
          </span>
          <div class="flex items-center gap-2">
            <el-button :loading="saving" @click="handleSubmit(0)">存为草稿</el-button>
            <el-button type="primary" :loading="saving" @click="handleSubmit(1)">发布文章</el-button>
            <el-button @click="editorVisible = false">取消</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useFullscreen } from "@vueuse/core";
import { FullScreen, Refresh } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";
import type { OptionItem } from "@/api/common";
import {
  BlogArticleAPI,
  BlogCategoryAPI,
  BlogTagAPI,
  type BlogArticleForm,
  type BlogArticleItem,
  type BlogArticleQuery,
} from "@/api/blog";
import SingleImageUpload from "@/components/Upload/SingleImageUpload.vue";
import { usePageTable, useTableSelection } from "@/composables";
import MarkdownEditor from "./components/MarkdownEditor.vue";

defineOptions({
  name: "BlogArticle",
  inheritAttrs: false,
});

const tableWrapperRef = ref<HTMLElement | null>(null);
const { toggle: toggleFullscreen } = useFullscreen(tableWrapperRef);
const queryFormRef = ref<FormInstance>();
const formRef = ref<FormInstance>();

const categoryOptions = ref<OptionItem[]>([]);
const tagOptions = ref<OptionItem[]>([]);

const { loading, list, total, params, fetchData, handleQuery, handleResetQuery } = usePageTable<
  BlogArticleItem,
  BlogArticleQuery
>({
  initialParams: {
    pageNum: 1,
    pageSize: 10,
    keywords: "",
    categoryId: undefined,
    tagId: undefined,
    status: undefined,
  },
  request: (query) => BlogArticleAPI.getPage(query),
});

const { selectedIds, hasSelection, handleSelectionChange, clearSelection } =
  useTableSelection<BlogArticleItem>();

const editorVisible = ref(false);
const saving = ref(false);
const importing = ref(false);
const importInputRef = ref<HTMLInputElement | null>(null);
const draftStatusText = ref("");
const autoSaveReady = ref(false);
let draftSaveTimer: number | null = null;

interface LocalArticleDraft {
  title: string;
  slug?: string;
  summary?: string;
  content: string;
  coverImage?: string;
  categoryId?: string;
  isTop: number;
  allowComment: number;
  tagIds: string[];
  savedAt: number;
}

const formData = reactive<BlogArticleForm>({
  id: undefined,
  title: "",
  slug: "",
  summary: "",
  content: "",
  coverImage: "",
  categoryId: undefined,
  status: 1,
  isTop: 0,
  allowComment: 1,
  tagIds: [],
});

function getDraftKey(id?: string) {
  return `myblog_article_draft_${id || "new"}`;
}

function clearLocalDraft(id?: string) {
  try {
    localStorage.removeItem(getDraftKey(id));
  } catch {
    // 忽略存储配额等异常
  }
  draftStatusText.value = "";
}

function formatDraftTime(timestamp: number) {
  return new Date(timestamp).toLocaleTimeString("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

function saveLocalDraftNow() {
  if (!editorVisible.value || !autoSaveReady.value) return;
  if (!formData.title.trim() && !formData.content.trim()) return;
  try {
    const now = Date.now();
    const draft: LocalArticleDraft = {
      title: formData.title,
      slug: formData.slug,
      summary: formData.summary,
      content: formData.content,
      coverImage: formData.coverImage,
      categoryId: formData.categoryId,
      isTop: formData.isTop,
      allowComment: formData.allowComment,
      tagIds: [...(formData.tagIds || [])],
      savedAt: now,
    };
    localStorage.setItem(getDraftKey(formData.id), JSON.stringify(draft));
    draftStatusText.value = `草稿已自动暂存 (${formatDraftTime(now)})`;
  } catch {
    // 忽略存储空间不足
  }
}

watch(
  () => ({
    title: formData.title,
    slug: formData.slug,
    summary: formData.summary,
    content: formData.content,
    coverImage: formData.coverImage,
    categoryId: formData.categoryId,
    isTop: formData.isTop,
    allowComment: formData.allowComment,
    tagIds: formData.tagIds,
  }),
  () => {
    if (!editorVisible.value || !autoSaveReady.value) return;
    if (draftSaveTimer !== null) {
      window.clearTimeout(draftSaveTimer);
    }
    draftSaveTimer = window.setTimeout(() => {
      saveLocalDraftNow();
      draftSaveTimer = null;
    }, 600);
  },
  { deep: true }
);

const isTopChecked = computed({
  get: () => formData.isTop === 1,
  set: (val: boolean) => {
    formData.isTop = val ? 1 : 0;
  },
});

const allowCommentChecked = computed({
  get: () => formData.allowComment === 1,
  set: (val: boolean) => {
    formData.allowComment = val ? 1 : 0;
  },
});

const rules: FormRules = {
  title: [{ required: true, message: "请输入文章标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入文章正文", trigger: "blur" }],
};

async function loadOptions() {
  const [cats, tags] = await Promise.all([
    BlogCategoryAPI.getOptions(),
    BlogTagAPI.getOptions(),
  ]);
  categoryOptions.value = cats;
  tagOptions.value = tags;
}

async function openEditor(id?: string) {
  autoSaveReady.value = false;
  resetForm();
  await loadOptions();
  if (id) {
    const detail = await BlogArticleAPI.getForm(id);
    Object.assign(formData, {
      ...detail,
      categoryId: detail.categoryId ? String(detail.categoryId) : undefined,
      tagIds: (detail.tagIds || []).map((t) => String(t)),
    });
  }

  try {
    const rawDraft = localStorage.getItem(getDraftKey(id));
    if (rawDraft) {
      const draft = JSON.parse(rawDraft) as LocalArticleDraft;
      const hasDiff =
        draft.title !== formData.title ||
        draft.content !== formData.content ||
        (draft.summary || "") !== (formData.summary || "");
      if (hasDiff && (draft.title || draft.content)) {
        const shouldRestore = await ElMessageBox.confirm(
          `检测到 ${formatDraftTime(draft.savedAt)} 自动暂存的未提交草稿，是否恢复？`,
          "发现本地草稿",
          {
            confirmButtonText: "恢复草稿",
            cancelButtonText: "丢弃",
            type: "info",
          }
        )
          .then(() => true)
          .catch(() => false);
        if (shouldRestore) {
          Object.assign(formData, {
            title: draft.title ?? formData.title,
            slug: draft.slug ?? formData.slug,
            summary: draft.summary ?? formData.summary,
            content: draft.content ?? formData.content,
            coverImage: draft.coverImage ?? formData.coverImage,
            categoryId: draft.categoryId,
            isTop: draft.isTop ?? formData.isTop,
            allowComment: draft.allowComment ?? formData.allowComment,
            tagIds: Array.isArray(draft.tagIds) ? draft.tagIds : [],
          });
          draftStatusText.value = `已恢复本地草稿 (${formatDraftTime(draft.savedAt)})`;
        } else {
          clearLocalDraft(id);
        }
      }
    }
  } catch {
    // 忽略损坏的本地草稿
  }

  editorVisible.value = true;
  nextTick(() => {
    autoSaveReady.value = true;
  });
}

function resetForm() {
  autoSaveReady.value = false;
  if (draftSaveTimer !== null) {
    window.clearTimeout(draftSaveTimer);
    draftSaveTimer = null;
  }
  draftStatusText.value = "";
  formData.id = undefined;
  formData.title = "";
  formData.slug = "";
  formData.summary = "";
  formData.content = "";
  formData.coverImage = "";
  formData.categoryId = undefined;
  formData.status = 1;
  formData.isTop = 0;
  formData.allowComment = 1;
  formData.tagIds = [];
  formRef.value?.clearValidate();
}

async function handleSubmit(targetStatus: number) {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  const draftId = formData.id;
  try {
    const payload: BlogArticleForm = {
      ...formData,
      status: targetStatus,
    };
    if (formData.id) {
      await BlogArticleAPI.update(formData.id, payload);
      ElMessage.success(targetStatus === 1 ? "保存并发布成功" : "已保存为草稿");
    } else {
      await BlogArticleAPI.create(payload);
      ElMessage.success(targetStatus === 1 ? "发布成功" : "已保存为草稿");
    }
    clearLocalDraft(draftId);
    editorVisible.value = false;
    await fetchData();
  } finally {
    saving.value = false;
  }
}

function handlePreview(id: string) {
  window.open(`/article/${id}`, "_blank", "noopener");
}

async function handleExport(row: BlogArticleItem) {
  const response = await BlogArticleAPI.exportMarkdown(row.id);
  const blob = response.data;
  const rawName = `${row.slug || row.title || "article"}.md`;
  const filename = rawName.replace(/[\\/:*?"<>|]+/g, "-");
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
  ElMessage.success("导出完成");
}

function triggerImport() {
  importInputRef.value?.click();
}

async function handleImportFile(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  importing.value = true;
  try {
    await BlogArticleAPI.importMarkdown(file);
    ElMessage.success("Markdown 导入成功，已存为草稿");
    await Promise.all([loadOptions(), fetchData()]);
  } finally {
    importing.value = false;
    input.value = "";
  }
}

async function handleToggleStatus(row: BlogArticleItem, published: boolean) {
  const status = published ? 1 : 0;
  await BlogArticleAPI.updateStatus(row.id, status);
  row.status = status;
  ElMessage.success(published ? "文章已发布" : "已转为草稿");
}

async function handleToggleTop(row: BlogArticleItem, top: boolean) {
  const isTop = top ? 1 : 0;
  await BlogArticleAPI.updateTop(row.id, isTop);
  row.isTop = isTop;
  ElMessage.success(top ? "已置顶" : "已取消置顶");
}

async function handleDelete(id?: string) {
  const ids = id ? [id] : selectedIds.value;
  if (!ids.length) return;
  await ElMessageBox.confirm("确认删除所选文章吗？关联评论也会一并删除。", "提示", {
    type: "warning",
  });
  await BlogArticleAPI.deleteByIds(ids.join(","));
  ElMessage.success("删除成功");
  clearSelection();
  await fetchData();
}

onMounted(async () => {
  await Promise.all([loadOptions(), fetchData()]);
});

onBeforeUnmount(() => {
  if (draftSaveTimer !== null) {
    window.clearTimeout(draftSaveTimer);
    draftSaveTimer = null;
  }
});
</script>
