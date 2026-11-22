<template>
  <div class="page-container">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">个人博客站点设置</span>
          <div class="flex items-center gap-2">
            <el-button tag="a" href="/" target="_blank" plain>查看前台首页</el-button>
            <el-button
              v-hasPerm="['blog:config:update']"
              type="primary"
              :loading="saving"
              @click="handleSave"
            >
              保存设置
            </el-button>
          </div>
        </div>
      </template>

      <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px" class="max-w-4xl">
        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="站点主标题" prop="siteTitle">
              <el-input v-model="formData.siteTitle" placeholder="例如：MyBlog · 个人技术博客" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="博主昵称" prop="authorName">
              <el-input v-model="formData.authorName" placeholder="例如：博主" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="站点副标题" prop="siteSubtitle">
          <el-input v-model="formData.siteSubtitle" placeholder="一句话介绍站点定位" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :xs="24" :md="14">
            <el-form-item label="博主简介" prop="authorBio">
              <el-input
                v-model="formData.authorBio"
                type="textarea"
                :rows="4"
                placeholder="显示在博客侧边栏的个人简介"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="10">
            <el-form-item label="博主头像" prop="authorAvatar">
              <SingleImageUpload
                v-model="formData.authorAvatar"
                :style="{ width: '104px', height: '104px' }"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="GitHub 主页" prop="githubUrl">
              <el-input v-model="formData.githubUrl" placeholder="https://github.com/..." />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="ICP 备案号" prop="icpNumber">
              <el-input v-model="formData.icpNumber" placeholder="选填，显示在页脚" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="评论先审后发" prop="commentAudit">
          <el-switch
            v-model="auditSwitch"
            inline-prompt
            active-text="开启"
            inactive-text="关闭"
          />
          <span class="ml-3 text-xs text-gray-500">
            开启后，访客新提交的本地划词/匿名评论需在「评论管理」审核通过后展示
          </span>
        </el-form-item>

        <el-divider content-position="left">GitHub 讨论区（Giscus）配置</el-divider>

        <el-form-item label="启用 Giscus" prop="giscusEnabled">
          <el-switch
            v-model="giscusSwitch"
            inline-prompt
            active-text="开启"
            inactive-text="关闭"
          />
          <span class="ml-3 text-xs text-gray-500">
            开启后文章底部将加载 GitHub Discussions 讨论区（正文保留极简划词点线批注）；参数可从
            <el-link href="https://giscus.app/zh-CN" target="_blank" type="primary">
              giscus.app
            </el-link>
            生成获取
          </span>
        </el-form-item>

        <el-form-item label="快捷解析脚本">
          <div class="w-full flex flex-col gap-2">
            <el-input
              v-model="giscusSnippet"
              type="textarea"
              :rows="3"
              placeholder="可选：直接粘贴从 giscus.app 复制的 <script ...> 代码段，点击下方按钮自动提取所有参数"
            />
            <div>
              <el-button size="small" type="primary" plain @click="parseGiscusSnippet">
                识别脚本并自动填充
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :xs="24" :md="12">
            <el-form-item label="GitHub 仓库" prop="giscusRepo">
              <el-input v-model="formData.giscusRepo" placeholder="例如：owner/myblog-comments" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="Repository ID" prop="giscusRepoId">
              <el-input v-model="formData.giscusRepoId" placeholder="例如：R_kgDO..." />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :xs="24" :md="8">
            <el-form-item label="Discussion 分类" prop="giscusCategory">
              <el-input v-model="formData.giscusCategory" placeholder="例如：Announcements" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="Category ID" prop="giscusCategoryId">
              <el-input v-model="formData.giscusCategoryId" placeholder="例如：DIC_kwDO..." />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="页面映射方式" prop="giscusMapping">
              <el-select v-model="formData.giscusMapping" class="w-full">
                <el-option value="pathname" label="路径 pathname" />
                <el-option value="url" label="完整地址 url" />
                <el-option value="title" label="页面标题 title" />
                <el-option value="og:title" label="og:title" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">关于页内容</el-divider>

        <el-form-item label="关于我页面" prop="aboutContent">
          <div class="w-full">
            <MarkdownEditor v-model="aboutContentValue" height="340px" />
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from "element-plus";
import { BlogConfigAPI, type BlogConfigForm } from "@/api/blog";
import SingleImageUpload from "@/components/Upload/SingleImageUpload.vue";
import MarkdownEditor from "../article/components/MarkdownEditor.vue";

defineOptions({
  name: "BlogSetting",
  inheritAttrs: false,
});

const formRef = ref<FormInstance>();
const loading = ref(false);
const saving = ref(false);

const giscusSnippet = ref("");

const formData = reactive<BlogConfigForm>({
  siteTitle: "",
  siteSubtitle: "",
  authorName: "",
  authorBio: "",
  authorAvatar: "",
  githubUrl: "",
  icpNumber: "",
  commentAudit: "0",
  giscusEnabled: "0",
  giscusRepo: "",
  giscusRepoId: "",
  giscusCategory: "",
  giscusCategoryId: "",
  giscusMapping: "pathname",
  aboutContent: "",
});

const auditSwitch = computed({
  get: () => formData.commentAudit === "1",
  set: (val: boolean) => {
    formData.commentAudit = val ? "1" : "0";
  },
});

const giscusSwitch = computed({
  get: () => formData.giscusEnabled === "1",
  set: (val: boolean) => {
    formData.giscusEnabled = val ? "1" : "0";
  },
});

function parseGiscusSnippet() {
  const raw = giscusSnippet.value.trim();
  if (!raw) {
    ElMessage.warning("请先粘贴 giscus.app 脚本代码");
    return;
  }
  const extract = (attr: string) => {
    const match = raw.match(new RegExp(`${attr}\\s*=\\s*["']([^"']+)["']`, "i"));
    return match?.[1]?.trim() || "";
  };
  const repo = extract("data-repo");
  const repoId = extract("data-repo-id");
  const category = extract("data-category");
  const categoryId = extract("data-category-id");
  const mapping = extract("data-mapping");

  if (!repo && !repoId && !categoryId) {
    ElMessage.warning("未能识别出 data-repo / data-repo-id / data-category-id 属性");
    return;
  }
  if (repo) formData.giscusRepo = repo;
  if (repoId) formData.giscusRepoId = repoId;
  if (category) formData.giscusCategory = category;
  if (categoryId) formData.giscusCategoryId = categoryId;
  if (mapping) formData.giscusMapping = mapping;
  formData.giscusEnabled = "1";
  ElMessage.success("已识别 Giscus 参数并开启讨论区，请点击右上角保存设置");
}

const aboutContentValue = computed({
  get: () => formData.aboutContent ?? "",
  set: (val: string) => {
    formData.aboutContent = val;
  },
});

const rules: FormRules = {
  siteTitle: [{ required: true, message: "请输入站点主标题", trigger: "blur" }],
  authorName: [{ required: true, message: "请输入博主昵称", trigger: "blur" }],
};

async function fetchConfig() {
  loading.value = true;
  try {
    const data = await BlogConfigAPI.get();
    Object.assign(formData, data);
  } finally {
    loading.value = false;
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    await BlogConfigAPI.save({ ...formData });
    ElMessage.success("站点设置已保存");
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  fetchConfig();
});
</script>
