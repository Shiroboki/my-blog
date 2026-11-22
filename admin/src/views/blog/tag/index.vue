<template>
  <div class="page-container">
    <el-card ref="tableWrapperRef" class="page-content" shadow="never">
      <div class="page-filter">
        <el-form ref="queryFormRef" :model="params" :inline="true">
          <el-form-item label="关键字" prop="keywords">
            <el-input
              v-model="params.keywords"
              placeholder="标签名称/别名"
              clearable
              @keyup.enter="handleQuery"
            />
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
            v-hasPerm="['blog:tag:create']"
            type="primary"
            @click="openDialog()"
          >
            新增标签
          </el-button>
          <el-button
            v-hasPerm="['blog:tag:delete']"
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
          highlight-current-row
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="标签名称" min-width="160">
            <template #default="{ row }">
              <el-tag
                :style="row.color ? { borderColor: row.color, color: row.color } : undefined"
                effect="plain"
              >
                {{ row.name }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="URL 别名" prop="slug" min-width="150" />
          <el-table-column label="颜色" prop="color" width="140" align="center">
            <template #default="{ row }">
              <span v-if="row.color" class="inline-flex items-center gap-2">
                <span
                  class="inline-block h-3 w-3 rounded-full"
                  :style="{ backgroundColor: row.color }"
                />
                <span>{{ row.color }}</span>
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="已发布文章数" prop="articleCount" width="130" align="center" />
          <el-table-column label="创建时间" prop="createTime" width="180" align="center" />
          <el-table-column label="操作" fixed="right" width="160" align="center">
            <template #default="{ row }">
              <el-button
                v-hasPerm="['blog:tag:update']"
                type="primary"
                size="small"
                link
                @click="openDialog(row.id)"
              >
                编辑
              </el-button>
              <el-button
                v-hasPerm="['blog:tag:delete']"
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

    <el-dialog
      v-model="dialogVisible"
      :title="formData.id ? '编辑标签' : '新增标签'"
      width="440px"
      append-to-body
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="formData.name" placeholder="例如：Spring Boot" />
        </el-form-item>
        <el-form-item label="URL 别名" prop="slug">
          <el-input v-model="formData.slug" placeholder="例如：spring-boot" />
        </el-form-item>
        <el-form-item label="标签颜色" prop="color">
          <div class="flex items-center gap-3">
            <el-color-picker v-model="formData.color" />
            <el-input v-model="formData.color" placeholder="#409EFF" class="!w-[180px]" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useFullscreen } from "@vueuse/core";
import { FullScreen, Refresh } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";
import { BlogTagAPI, type BlogTagForm, type BlogTagItem, type BlogTagQuery } from "@/api/blog";
import { usePageTable, useTableSelection } from "@/composables";

defineOptions({
  name: "BlogTag",
  inheritAttrs: false,
});

const tableWrapperRef = ref<HTMLElement | null>(null);
const { toggle: toggleFullscreen } = useFullscreen(tableWrapperRef);
const queryFormRef = ref<FormInstance>();
const formRef = ref<FormInstance>();

const { loading, list, total, params, fetchData, handleQuery, handleResetQuery } = usePageTable<
  BlogTagItem,
  BlogTagQuery
>({
  initialParams: { pageNum: 1, pageSize: 10, keywords: "" },
  request: (query) => BlogTagAPI.getPage(query),
});

const { selectedIds, hasSelection, handleSelectionChange, clearSelection } =
  useTableSelection<BlogTagItem>();

const dialogVisible = ref(false);
const saving = ref(false);
const formData = reactive<BlogTagForm>({
  id: undefined,
  name: "",
  slug: "",
  color: "#409EFF",
});

const rules: FormRules = {
  name: [{ required: true, message: "请输入标签名称", trigger: "blur" }],
  slug: [{ required: true, message: "请输入 URL 别名", trigger: "blur" }],
};

async function openDialog(id?: string) {
  resetForm();
  if (id) {
    const data = await BlogTagAPI.getForm(id);
    Object.assign(formData, data);
  }
  dialogVisible.value = true;
}

function resetForm() {
  formData.id = undefined;
  formData.name = "";
  formData.slug = "";
  formData.color = "#409EFF";
  formRef.value?.clearValidate();
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    if (formData.id) {
      await BlogTagAPI.update(formData.id, { ...formData });
      ElMessage.success("修改成功");
    } else {
      await BlogTagAPI.create({ ...formData });
      ElMessage.success("新增成功");
    }
    dialogVisible.value = false;
    await fetchData();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(id?: string) {
  const ids = id ? [id] : selectedIds.value;
  if (!ids.length) return;
  await ElMessageBox.confirm("确认删除所选标签吗？", "提示", { type: "warning" });
  await BlogTagAPI.deleteByIds(ids.join(","));
  ElMessage.success("删除成功");
  clearSelection();
  await fetchData();
}

onMounted(() => {
  fetchData();
});
</script>
