<template>
  <div class="page-container">
    <el-card ref="tableWrapperRef" class="page-content" shadow="never">
      <div class="page-filter">
        <el-form ref="queryFormRef" :model="params" :inline="true">
          <el-form-item label="关键字" prop="keywords">
            <el-input
              v-model="params.keywords"
              placeholder="分类名称/别名"
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
            v-hasPerm="['blog:category:create']"
            type="primary"
            @click="openDialog()"
          >
            新增分类
          </el-button>
          <el-button
            v-hasPerm="['blog:category:delete']"
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
          <el-table-column label="分类名称" prop="name" min-width="150" />
          <el-table-column label="URL 别名" prop="slug" min-width="140" />
          <el-table-column label="描述" prop="description" min-width="220" show-overflow-tooltip />
          <el-table-column label="已发布文章数" prop="articleCount" width="130" align="center">
            <template #default="{ row }">
              <el-tag type="info" effect="plain">{{ row.articleCount ?? 0 }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="排序" prop="sort" width="90" align="center" />
          <el-table-column label="创建时间" prop="createTime" width="180" align="center" />
          <el-table-column label="操作" fixed="right" width="160" align="center">
            <template #default="{ row }">
              <el-button
                v-hasPerm="['blog:category:update']"
                type="primary"
                size="small"
                link
                @click="openDialog(row.id)"
              >
                编辑
              </el-button>
              <el-button
                v-hasPerm="['blog:category:delete']"
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
      :title="formData.id ? '编辑分类' : '新增分类'"
      width="480px"
      append-to-body
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="formData.name" placeholder="例如：技术笔记" />
        </el-form-item>
        <el-form-item label="URL 别名" prop="slug">
          <el-input v-model="formData.slug" placeholder="例如：tech" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="分类简介"
          />
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
import { BlogCategoryAPI, type BlogCategoryForm, type BlogCategoryItem, type BlogCategoryQuery } from "@/api/blog";
import { usePageTable, useTableSelection } from "@/composables";

defineOptions({
  name: "BlogCategory",
  inheritAttrs: false,
});

const tableWrapperRef = ref<HTMLElement | null>(null);
const { toggle: toggleFullscreen } = useFullscreen(tableWrapperRef);
const queryFormRef = ref<FormInstance>();
const formRef = ref<FormInstance>();

const { loading, list, total, params, fetchData, handleQuery, handleResetQuery } = usePageTable<
  BlogCategoryItem,
  BlogCategoryQuery
>({
  initialParams: { pageNum: 1, pageSize: 10, keywords: "" },
  request: (query) => BlogCategoryAPI.getPage(query),
});

const { selectedIds, hasSelection, handleSelectionChange, clearSelection } =
  useTableSelection<BlogCategoryItem>();

const dialogVisible = ref(false);
const saving = ref(false);
const formData = reactive<BlogCategoryForm>({
  id: undefined,
  name: "",
  slug: "",
  description: "",
  sort: 0,
});

const rules: FormRules = {
  name: [{ required: true, message: "请输入分类名称", trigger: "blur" }],
  slug: [{ required: true, message: "请输入 URL 别名", trigger: "blur" }],
};

async function openDialog(id?: string) {
  resetForm();
  if (id) {
    const data = await BlogCategoryAPI.getForm(id);
    Object.assign(formData, data);
  }
  dialogVisible.value = true;
}

function resetForm() {
  formData.id = undefined;
  formData.name = "";
  formData.slug = "";
  formData.description = "";
  formData.sort = 0;
  formRef.value?.clearValidate();
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    if (formData.id) {
      await BlogCategoryAPI.update(formData.id, { ...formData });
      ElMessage.success("修改成功");
    } else {
      await BlogCategoryAPI.create({ ...formData });
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
  await ElMessageBox.confirm("确认删除所选分类吗？", "提示", { type: "warning" });
  await BlogCategoryAPI.deleteByIds(ids.join(","));
  ElMessage.success("删除成功");
  clearSelection();
  await fetchData();
}

onMounted(() => {
  fetchData();
});
</script>
