<template>
  <div class="page-container">
    <el-card ref="tableWrapperRef" class="page-content" shadow="never">
      <div class="page-filter">
        <el-form ref="queryFormRef" :model="params" :inline="true">
          <el-form-item label="关键字" prop="keywords">
            <el-input
              v-model="params.keywords"
              placeholder="站点名称/地址/简介"
              clearable
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
            v-hasPerm="['blog:link:create']"
            type="primary"
            @click="openDialog()"
          >
            新增友链
          </el-button>
          <el-button
            v-hasPerm="['blog:link:delete']"
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
          <el-table-column label="图标" width="76" align="center">
            <template #default="{ row }">
              <el-avatar :size="30" :src="(row as BlogLinkItem).avatar">
                {{ (row as BlogLinkItem).name?.slice(0, 1) || "友" }}
              </el-avatar>
            </template>
          </el-table-column>
          <el-table-column label="站点名称" prop="name" min-width="150" />
          <el-table-column label="站点地址" prop="url" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <el-link
                :href="(row as BlogLinkItem).url"
                target="_blank"
                type="primary"
                :underline="false"
              >
                {{ (row as BlogLinkItem).url }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="站点简介" prop="description" min-width="220" show-overflow-tooltip />
          <el-table-column label="排序" prop="sort" width="90" align="center" />
          <el-table-column label="状态" prop="status" width="100" align="center">
            <template #default="{ row }">
              <el-tag
                :type="(row as BlogLinkItem).status === 1 ? 'success' : 'info'"
                effect="plain"
              >
                {{ (row as BlogLinkItem).status === 1 ? "启用" : "停用" }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="180" align="center" />
          <el-table-column label="操作" fixed="right" width="160" align="center">
            <template #default="{ row }">
              <el-button
                v-hasPerm="['blog:link:update']"
                type="primary"
                size="small"
                link
                @click="openDialog((row as BlogLinkItem).id)"
              >
                编辑
              </el-button>
              <el-button
                v-hasPerm="['blog:link:delete']"
                type="danger"
                size="small"
                link
                @click="handleDelete((row as BlogLinkItem).id)"
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
      :title="formData.id ? '编辑友链' : '新增友链'"
      width="540px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="站点名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入站点名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="站点地址" prop="url">
          <el-input v-model="formData.url" placeholder="https://example.com" maxlength="255" />
        </el-form-item>
        <el-form-item label="站点头像" prop="avatar">
          <el-input v-model="formData.avatar" placeholder="https://example.com/logo.png（可选）" maxlength="500" />
        </el-form-item>
        <el-form-item label="站点简介" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="简短介绍该站点内容或主题"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
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
import { BlogLinkAPI, type BlogLinkForm, type BlogLinkItem, type BlogLinkQuery } from "@/api/blog";
import { usePageTable, useTableSelection } from "@/composables";

defineOptions({
  name: "BlogLink",
  inheritAttrs: false,
});

const tableWrapperRef = ref<HTMLElement | null>(null);
const { toggle: toggleFullscreen } = useFullscreen(tableWrapperRef);
const queryFormRef = ref<FormInstance>();
const formRef = ref<FormInstance>();

const { loading, list, total, params, fetchData, handleQuery, handleResetQuery } = usePageTable<
  BlogLinkItem,
  BlogLinkQuery
>({
  initialParams: { pageNum: 1, pageSize: 10, keywords: "", status: undefined },
  request: (query) => BlogLinkAPI.getPage(query),
});

const { selectedIds, hasSelection, handleSelectionChange, clearSelection } =
  useTableSelection<BlogLinkItem>();

const dialogVisible = ref(false);
const saving = ref(false);
const formData = reactive<BlogLinkForm>({
  id: undefined,
  name: "",
  url: "",
  avatar: "",
  description: "",
  sort: 0,
  status: 1,
});

const rules: FormRules = {
  name: [{ required: true, message: "请输入站点名称", trigger: "blur" }],
  url: [
    { required: true, message: "请输入站点地址", trigger: "blur" },
    { pattern: /^https?:\/\/.+/, message: "请输入以 http:// 或 https:// 开头的网址", trigger: "blur" },
  ],
};

async function openDialog(id?: string) {
  resetForm();
  if (id) {
    const data = await BlogLinkAPI.getForm(id);
    Object.assign(formData, data);
  }
  dialogVisible.value = true;
}

function resetForm() {
  formData.id = undefined;
  formData.name = "";
  formData.url = "";
  formData.avatar = "";
  formData.description = "";
  formData.sort = 0;
  formData.status = 1;
  formRef.value?.clearValidate();
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    if (formData.id) {
      await BlogLinkAPI.update(formData.id, { ...formData });
      ElMessage.success("修改成功");
    } else {
      await BlogLinkAPI.create({ ...formData });
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
  await ElMessageBox.confirm("确认删除所选友链吗？", "提示", { type: "warning" });
  await BlogLinkAPI.deleteByIds(ids.join(","));
  ElMessage.success("删除成功");
  clearSelection();
  await fetchData();
}

onMounted(() => {
  fetchData();
});
</script>
