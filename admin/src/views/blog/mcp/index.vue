<template>
  <div class="page-container">
    <div v-loading="loading" class="mcp-grid">
      <!-- 顶部服务状态与安全开关卡片 -->
      <el-card shadow="never" class="mcp-card">
        <template #header>
          <div class="card-head">
            <div>
              <span class="card-title">MyBlog MCP Server 接入总览</span>
              <span class="card-sub">Streamable HTTP 协议端点，支持 IDE / Agent 读写博客与思维导图</span>
            </div>
            <div class="head-actions">
              <el-tag :type="overview.mcpEnabled ? 'success' : 'info'" effect="plain">
                {{ overview.mcpEnabled ? "MCP 已开启" : "MCP 已停用" }}
              </el-tag>
              <el-tag :type="overview.bootstrapConfigured ? 'warning' : 'info'" effect="plain">
                {{ overview.bootstrapConfigured ? "Bootstrap Token 已注入" : "仅使用数据库密钥" }}
              </el-tag>
              <el-button :icon="Refresh" @click="loadOverview">刷新</el-button>
            </div>
          </div>
        </template>

        <div class="settings-row">
          <div class="setting-item">
            <div class="setting-info">
              <div class="setting-label">启用 MCP 接入网关</div>
              <div class="setting-desc">
                关闭后，所有 API 密钥（含环境变量启动密钥）将在认证层立即被拒绝
              </div>
            </div>
            <el-switch
              v-model="overview.mcpEnabled"
              v-hasPerm="['blog:mcp:update']"
              :loading="savingSettings"
              @change="(val: string | number | boolean) => handleSettingChange('mcpEnabled', Boolean(val))"
            />
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <div class="setting-label">允许 MCP 工具直接公开发布文章</div>
              <div class="setting-desc">
                关闭时为安全草稿模式（mcp_allow_publish=0），AI 新建或更新文章不会直接公开上线
              </div>
            </div>
            <el-switch
              v-model="overview.allowPublish"
              v-hasPerm="['blog:mcp:update']"
              :loading="savingSettings"
              @change="(val: string | number | boolean) => handleSettingChange('allowPublish', Boolean(val))"
            />
          </div>
        </div>
      </el-card>

      <!-- 客户端快速接入配置 -->
      <el-card shadow="never" class="mcp-card">
        <template #header>
          <div class="card-head">
            <div>
              <span class="card-title">客户端接入配置（Antigravity / Cursor / Claude Desktop）</span>
              <span class="card-sub">端点地址：{{ fullEndpointUrl }}</span>
            </div>
            <div class="head-actions">
              <el-button type="primary" plain @click="copyConfigSnippet">复制 JSON 配置</el-button>
            </div>
          </div>
        </template>

        <div class="config-tabs">
          <el-radio-group v-model="configMode" size="small">
            <el-radio-button value="bearer">Authorization: Bearer (推荐)</el-radio-button>
            <el-radio-button value="header">X-MCP-Key Header</el-radio-button>
          </el-radio-group>
          <span class="config-hint">
            将配置中的 &lt;YOUR_MCP_API_KEY&gt; 替换为下方新建的 API 密钥即可连接
          </span>
        </div>

        <pre class="code-preview"><code>{{ clientConfigJson }}</code></pre>
      </el-card>

      <!-- API 密钥生命周期管理 -->
      <el-card shadow="never" class="mcp-card">
        <template #header>
          <div class="card-head">
            <div>
              <span class="card-title">MCP API 密钥管理</span>
              <span class="card-sub">数据库仅保存 SHA-256 摘要，明文密钥仅在创建时展示一次</span>
            </div>
            <el-button
              v-hasPerm="['blog:mcp:create']"
              type="primary"
              :icon="Plus"
              @click="openCreateDialog"
            >
              创建密钥
            </el-button>
          </div>
        </template>

        <el-table :data="overview.tokens" border style="width: 100%">
          <el-table-column prop="name" label="名称" min-width="150" />
          <el-table-column prop="tokenPrefix" label="密钥掩码" min-width="170">
            <template #default="{ row }">
              <code class="mono-text">{{ (row as BlogMcpTokenItem).tokenPrefix }}</code>
            </template>
          </el-table-column>
          <el-table-column label="作用域" min-width="160">
            <template #default="{ row }">
              <div class="scope-tags">
                <el-tag
                  v-for="s in splitScopes((row as BlogMcpTokenItem).scopes)"
                  :key="s"
                  size="small"
                  :type="scopeTagType(s)"
                  effect="plain"
                >
                  {{ s }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="95" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="(row as BlogMcpTokenItem).status === 1"
                :disabled="!hasUpdatePerm"
                @change="(val: string | number | boolean) => handleToggleTokenStatus(row as BlogMcpTokenItem, Boolean(val))"
              />
            </template>
          </el-table-column>
          <el-table-column prop="callCount" label="调用次数" width="100" align="center" />
          <el-table-column label="最近调用" min-width="165">
            <template #default="{ row }">
              {{ (row as BlogMcpTokenItem).lastUsedTime || "-" }}
            </template>
          </el-table-column>
          <el-table-column label="过期时间" min-width="165">
            <template #default="{ row }">
              {{ (row as BlogMcpTokenItem).expireTime || "永不过期" }}
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
          <el-table-column label="操作" width="95" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                v-hasPerm="['blog:mcp:delete']"
                link
                type="danger"
                @click="handleDeleteToken(row as BlogMcpTokenItem)"
              >
                吊销
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 注册工具与资源目录 -->
      <el-card shadow="never" class="mcp-card">
        <template #header>
          <div class="card-head">
            <div>
              <span class="card-title">已暴露 MCP Tools ({{ overview.tools.length }}) &amp; Resources (2)</span>
              <span class="card-sub">随服务启动自动注册至 Streamable HTTP 服务端点</span>
            </div>
          </div>
        </template>

        <el-table :data="overview.tools" border style="width: 100%">
          <el-table-column prop="name" label="Tool 名称" min-width="210">
            <template #default="{ row }">
              <code class="mono-text">{{ (row as BlogMcpToolDescriptor).name }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="模块" width="95" align="center" />
          <el-table-column prop="scope" label="所需权限" width="110" align="center">
            <template #default="{ row }">
              <el-tag
                size="small"
                :type="(row as BlogMcpToolDescriptor).scope === 'write' ? 'warning' : 'success'"
              >
                {{ (row as BlogMcpToolDescriptor).scope }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="功能说明" min-width="320" />
        </el-table>

        <div class="resource-strip">
          <div class="resource-strip-title">内置只读 Resources：</div>
          <el-tag effect="plain">blog://overview（站点概况 JSON）</el-tag>
          <el-tag effect="plain">blog://taxonomy（分类与标签目录 JSON）</el-tag>
        </div>
      </el-card>
    </div>

    <!-- 创建 API 密钥对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建 MCP API 密钥" width="460px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="密钥名称" prop="name">
          <el-input v-model="createForm.name" placeholder="例如：Antigravity Workstation" maxlength="80" />
        </el-form-item>
        <el-form-item label="权限作用域" prop="scopeList">
          <el-checkbox-group v-model="createForm.scopeList">
            <el-checkbox value="read" disabled>read（必选只读检索）</el-checkbox>
            <el-checkbox value="write">write（允许写文章与导图节点）</el-checkbox>
            <el-checkbox value="admin">admin（全量管理权限）</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="有效期(天)" prop="expireDays">
          <el-input-number
            v-model="createForm.expireDays"
            :min="1"
            :max="3650"
            placeholder="留空永不过期"
            style="width: 200px"
          />
          <span class="form-unit-hint">留空表示永不过期</span>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingToken" @click="submitCreateToken">
          生成密钥
        </el-button>
      </template>
    </el-dialog>

    <!-- 仅展示一次的明文密钥确认对话框 -->
    <el-dialog
      v-model="plainDialogVisible"
      title="请立即妥善保存您的 MCP API 密钥"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="出于安全考虑，明文密钥仅在此处展示一次，关闭弹窗后无法再次找回。"
        style="margin-bottom: 14px"
      />
      <div class="plain-token-box">
        <code class="plain-token-code">{{ createdPlainToken }}</code>
        <el-button type="primary" size="small" @click="copyText(createdPlainToken, '密钥已复制')">
          复制密钥
        </el-button>
      </div>
      <div class="plain-snippet-label">可直接粘贴至 MCP 客户端的配置片段：</div>
      <pre class="code-preview"><code>{{ createdClientConfigJson }}</code></pre>
      <template #footer>
        <el-button type="primary" @click="plainDialogVisible = false">我已妥善保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Plus, Refresh } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import {
  BlogMcpAPI,
  type BlogMcpOverview,
  type BlogMcpTokenItem,
  type BlogMcpToolDescriptor,
} from "@/api/blog";
import { useUserStore } from "@/stores";

defineOptions({
  name: "BlogMcp",
  inheritAttrs: false,
});

const userStore = useUserStore();
const loading = ref(false);
const savingSettings = ref(false);
const creatingToken = ref(false);
const createDialogVisible = ref(false);
const plainDialogVisible = ref(false);
const createdPlainToken = ref("");
const configMode = ref<"bearer" | "header">("bearer");
const createFormRef = ref<FormInstance>();

const overview = reactive<BlogMcpOverview>({
  mcpEnabled: true,
  allowPublish: false,
  endpointPath: "/api/v1/mcp",
  bootstrapConfigured: false,
  tools: [],
  tokens: [],
});

const createForm = reactive<{
  name: string;
  scopeList: string[];
  expireDays?: number;
  remark: string;
}>({
  name: "",
  scopeList: ["read", "write"],
  expireDays: undefined,
  remark: "",
});

const createRules: FormRules = {
  name: [{ required: true, message: "请输入密钥名称", trigger: "blur" }],
};

const hasUpdatePerm = computed(() => {
  const perms = userStore.userInfo?.perms || [];
  const roles = userStore.userInfo?.roles || [];
  return roles.includes("ROOT") || perms.includes("blog:mcp:update") || perms.includes("*:*:*");
});

const fullEndpointUrl = computed(() => {
  const origin = typeof window !== "undefined" ? window.location.origin : "http://localhost:9123";
  return `${origin}${overview.endpointPath || "/api/v1/mcp"}`;
});

function buildClientConfig(tokenValue: string) {
  const headers =
    configMode.value === "bearer"
      ? { Authorization: `Bearer ${tokenValue}` }
      : { "X-MCP-Key": tokenValue };
  return JSON.stringify(
    {
      mcpServers: {
        myblog: {
          serverUrl: fullEndpointUrl.value,
          headers,
        },
      },
    },
    null,
    2,
  );
}

const clientConfigJson = computed(() => buildClientConfig("<YOUR_MCP_API_KEY>"));
const createdClientConfigJson = computed(() =>
  buildClientConfig(createdPlainToken.value || "<YOUR_MCP_API_KEY>"),
);

function splitScopes(raw?: string): string[] {
  return (raw || "read")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
}

function scopeTagType(scope: string) {
  if (scope === "admin") return "danger";
  if (scope === "write") return "warning";
  return "success";
}

async function loadOverview() {
  loading.value = true;
  try {
    const res = await BlogMcpAPI.getOverview();
    overview.mcpEnabled = res.mcpEnabled;
    overview.allowPublish = res.allowPublish;
    overview.endpointPath = res.endpointPath || "/api/v1/mcp";
    overview.bootstrapConfigured = res.bootstrapConfigured;
    overview.tools = res.tools || [];
    overview.tokens = res.tokens || [];
  } finally {
    loading.value = false;
  }
}

async function handleSettingChange(field: "mcpEnabled" | "allowPublish", value: boolean) {
  savingSettings.value = true;
  try {
    await BlogMcpAPI.updateSettings({ [field]: value });
    ElMessage.success("MCP 安全策略已更新");
  } catch {
    await loadOverview();
  } finally {
    savingSettings.value = false;
  }
}

function openCreateDialog() {
  createForm.name = "";
  createForm.scopeList = ["read", "write"];
  createForm.expireDays = undefined;
  createForm.remark = "";
  createDialogVisible.value = true;
}

async function submitCreateToken() {
  const valid = await createFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  creatingToken.value = true;
  try {
    const scopes = Array.from(new Set(["read", ...createForm.scopeList])).join(",");
    const res = await BlogMcpAPI.createToken({
      name: createForm.name.trim(),
      scopes,
      expireDays: createForm.expireDays,
      remark: createForm.remark.trim() || undefined,
    });
    createDialogVisible.value = false;
    createdPlainToken.value = res.plainToken;
    plainDialogVisible.value = true;
    await loadOverview();
  } finally {
    creatingToken.value = false;
  }
}

async function handleToggleTokenStatus(row: BlogMcpTokenItem, enabled: boolean) {
  const nextStatus = enabled ? 1 : 0;
  try {
    await BlogMcpAPI.updateTokenStatus(row.id, nextStatus);
    row.status = nextStatus;
    ElMessage.success(enabled ? "密钥已启用" : "密钥已禁用");
  } catch {
    await loadOverview();
  }
}

async function handleDeleteToken(row: BlogMcpTokenItem) {
  await ElMessageBox.confirm(`确认永久吊销密钥「${row.name}」吗？调用方将立即失效。`, "警告", {
    type: "warning",
  });
  await BlogMcpAPI.deleteToken(row.id);
  ElMessage.success("密钥已吊销");
  await loadOverview();
}

async function copyText(text: string, msg: string) {
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success(msg);
  } catch {
    ElMessage.warning("浏览器禁止剪贴板写入，请手动选中复制");
  }
}

function copyConfigSnippet() {
  void copyText(clientConfigJson.value, "客户端 JSON 配置已复制");
}

onMounted(() => {
  void loadOverview();
});
</script>

<style scoped lang="scss">
.mcp-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.card-sub {
  display: block;
  margin-top: 2px;
  font-size: 12.5px;
  color: var(--el-text-color-secondary);
}

.head-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.settings-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-blank);
}

.setting-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.setting-desc {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--el-text-color-secondary);
  line-height: 1.45;
}

.config-tabs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.config-hint {
  font-size: 12.5px;
  color: var(--el-text-color-secondary);
}

.code-preview {
  margin: 0;
  padding: 14px 16px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12.5px;
  line-height: 1.55;
  overflow-x: auto;
  color: var(--el-text-color-primary);
}

.mono-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12.5px;
}

.scope-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.resource-strip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--el-border-color-lighter);
}

.resource-strip-title {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.form-unit-hint {
  margin-left: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.plain-token-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-color-warning-light-5);
  margin-bottom: 14px;
}

.plain-token-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  word-break: break-all;
  color: var(--el-text-color-primary);
}

.plain-snippet-label {
  margin-bottom: 8px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}
</style>
