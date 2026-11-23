<template>
  <el-dialog
    :model-value="modelValue"
    :title="pathId ? '编辑学习路径与思维导图' : '新建学习路径与思维导图'"
    width="1240px"
    top="3vh"
    :close-on-click-modal="false"
    destroy-on-close
    @update:model-value="(val: boolean) => emit('update:modelValue', val)"
  >
    <div v-loading="loadingDetail" class="studio-shell">
      <el-form
        ref="metaFormRef"
        :model="metaForm"
        :rules="metaRules"
        label-width="74px"
        class="meta-form"
      >
        <div class="meta-grid">
          <el-form-item label="路径标题" prop="title">
            <el-input v-model="metaForm.title" placeholder="例如：Java 后端架构进阶路线" />
          </el-form-item>
          <el-form-item label="访问标识" prop="slug">
            <el-input v-model="metaForm.slug" placeholder="可选，如 java-architect" />
          </el-form-item>
          <el-form-item label="主题色" prop="coverColor">
            <div class="color-picker-row">
              <input v-model="metaForm.coverColor" type="color" class="color-swatch" />
              <el-input v-model="metaForm.coverColor" style="width: 112px" />
            </div>
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="metaForm.sort" :min="0" :max="9999" style="width: 110px" />
          </el-form-item>
          <el-form-item label="发布状态" prop="status">
            <el-switch
              v-model="metaForm.status"
              :active-value="1"
              :inactive-value="0"
              active-text="启用"
              inactive-text="停用"
            />
          </el-form-item>
        </div>
        <el-form-item label="路径简介" prop="summary" class="summary-item">
          <el-input
            v-model="metaForm.summary"
            placeholder="简要说明这条学习路径适合什么阶段、涵盖哪些核心主题"
          />
        </el-form-item>
      </el-form>

      <div class="studio-workspace">
        <!-- 左侧思维导图实时预览画布 -->
        <section class="canvas-pane">
          <div class="pane-toolbar">
            <div class="toolbar-group">
              <el-button size="small" type="primary" plain @click="handleAddChild">
                + 子节点
              </el-button>
              <el-button size="small" @click="handleAddSibling">
                + 同级节点
              </el-button>
              <el-button
                size="small"
                :disabled="isRootSelected"
                @click="handleMoveNode(-1)"
              >
                上移
              </el-button>
              <el-button
                size="small"
                :disabled="isRootSelected"
                @click="handleMoveNode(1)"
              >
                下移
              </el-button>
              <el-button
                size="small"
                type="danger"
                plain
                :disabled="isRootSelected"
                @click="handleRemoveNode"
              >
                删除节点
              </el-button>
            </div>

            <div class="toolbar-group">
              <span class="stats-pill">
                {{ treeStats.nodeCount }} 节点 / {{ treeStats.resourceCount }} 资源
              </span>
              <el-button size="small" @click="adjustZoom(-0.12)">-</el-button>
              <el-button size="small" @click="resetZoom">{{ Math.round(zoom * 100) }}%</el-button>
              <el-button size="small" @click="adjustZoom(0.12)">+</el-button>
              <el-button size="small" text type="primary" @click="openJsonDrawer">
                JSON
              </el-button>
            </div>
          </div>

          <div class="canvas-scroll">
            <div
              class="canvas-stage"
              :style="{
                width: `${layout.width * zoom}px`,
                height: `${layout.height * zoom}px`,
              }"
            >
              <div
                class="canvas-world"
                :style="{
                  width: `${layout.width}px`,
                  height: `${layout.height}px`,
                  transform: `scale(${zoom})`,
                }"
              >
                <svg
                  class="edge-layer"
                  :width="layout.width"
                  :height="layout.height"
                  :viewBox="`0 0 ${layout.width} ${layout.height}`"
                >
                  <path
                    v-for="edge in layout.edges"
                    :key="edge.id"
                    :d="edge.path"
                    :stroke="getBranchColor(edge.branchIndex, metaForm.coverColor || '#0d9488')"
                    stroke-width="2.2"
                    fill="none"
                    stroke-linecap="round"
                  />
                </svg>

                <button
                  v-for="item in layout.nodes"
                  :key="item.id"
                  type="button"
                  class="studio-node"
                  :class="{
                    'is-root': item.depth === 0,
                    'is-selected': item.id === selectedNodeId,
                  }"
                  :style="{
                    left: `${item.x}px`,
                    top: `${item.y}px`,
                    width: `${item.width}px`,
                    minHeight: `${item.height}px`,
                    '--branch-color': getBranchColor(item.branchIndex, metaForm.coverColor || '#0d9488'),
                  }"
                  @click="selectNode(item.id)"
                >
                  <span class="node-accent" />
                  <div class="node-title">{{ item.node.label }}</div>
                  <div v-if="item.node.description" class="node-desc">
                    {{ item.node.description }}
                  </div>
                  <div v-if="item.node.tag || item.node.resources.length" class="node-badges">
                    <span v-if="item.node.tag" class="mini-tag">{{ item.node.tag }}</span>
                    <span v-if="item.resourceCounts.article" class="res-badge is-article">
                      文章 {{ item.resourceCounts.article }}
                    </span>
                    <span v-if="item.resourceCounts.video" class="res-badge is-video">
                      视频 {{ item.resourceCounts.video }}
                    </span>
                    <span v-if="item.resourceCounts.webpage" class="res-badge is-webpage">
                      网页 {{ item.resourceCounts.webpage }}
                    </span>
                  </div>
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- 右侧节点属性与资源挂载器 -->
        <aside class="inspector-pane">
          <div class="inspector-section">
            <div class="section-header">
              <span>当前节点属性</span>
              <el-tag size="small" effect="plain">
                {{ isRootSelected ? "根节点" : "分支节点" }}
              </el-tag>
            </div>

            <template v-if="selectedNode">
              <div class="field-group">
                <label class="field-label">节点名称</label>
                <el-input
                  :model-value="selectedNode.label"
                  placeholder="输入知识节点名称"
                  @update:model-value="(val: string) => patchSelectedNode({ label: val })"
                />
              </div>
              <div class="field-row">
                <div class="field-group">
                  <label class="field-label">阶段标签</label>
                  <el-input
                    :model-value="selectedNode.tag || ''"
                    placeholder="如 核心 / 必学 / 实战"
                    @update:model-value="(val: string) => patchSelectedNode({ tag: val })"
                  />
                </div>
              </div>
              <div class="field-group">
                <label class="field-label">节点说明</label>
                <el-input
                  type="textarea"
                  :rows="2"
                  :model-value="selectedNode.description || ''"
                  placeholder="简要描述本节点需要掌握的核心要点"
                  @update:model-value="(val: string) => patchSelectedNode({ description: val })"
                />
              </div>
            </template>
          </div>

          <div class="inspector-section resources-section">
            <div class="section-header">
              <span>已挂载资源 ({{ selectedNode?.resources.length || 0 }})</span>
              <span class="section-sub">支持文章 / 视频 / 网页</span>
            </div>

            <div v-if="selectedNode?.resources.length" class="mounted-list">
              <div
                v-for="(res, idx) in selectedNode.resources"
                :key="res.id"
                class="mounted-item"
              >
                <div class="mounted-top">
                  <span class="res-type-tag" :class="`is-${res.type}`">
                    {{ resourceTypeLabel(res.type) }}
                  </span>
                  <span class="mounted-title" :title="res.title">{{ res.title }}</span>
                  <div class="mounted-ops">
                    <el-button
                      link
                      size="small"
                      :disabled="idx === 0"
                      @click="moveResource(idx, -1)"
                    >
                      上
                    </el-button>
                    <el-button
                      link
                      size="small"
                      :disabled="idx === selectedNode.resources.length - 1"
                      @click="moveResource(idx, 1)"
                    >
                      下
                    </el-button>
                    <el-button
                      link
                      type="danger"
                      size="small"
                      @click="removeResource(res.id)"
                    >
                      移除
                    </el-button>
                  </div>
                </div>
                <div class="mounted-target">
                  <template v-if="res.type === 'article'">
                    站内文章：/article/{{ res.articleSlug || res.articleId }}
                  </template>
                  <template v-else>
                    {{ res.url }}
                  </template>
                </div>
                <div v-if="res.note" class="mounted-note">{{ res.note }}</div>
              </div>
            </div>
            <div v-else class="mounted-empty">当前节点暂未挂载任何学习资料</div>

            <!-- 新增资源挂载表单 -->
            <div class="mounter-box">
              <div class="mounter-tabs">
                <button
                  v-for="tab in resourceTypeTabs"
                  :key="tab.value"
                  type="button"
                  class="mounter-tab"
                  :class="{ active: newRes.type === tab.value }"
                  @click="switchResourceTab(tab.value)"
                >
                  {{ tab.label }}
                </button>
              </div>

              <template v-if="newRes.type === 'article'">
                <el-select
                  v-model="newRes.articleId"
                  filterable
                  remote
                  clearable
                  placeholder="搜索已发布博客文章..."
                  :remote-method="searchPublishedArticles"
                  :loading="articleLoading"
                  style="width: 100%; margin-bottom: 8px"
                  @change="handleArticleSelect"
                >
                  <el-option
                    v-for="art in articleOptions"
                    :key="art.id"
                    :label="art.title"
                    :value="art.id"
                  >
                    <div class="article-option">
                      <span>{{ art.title }}</span>
                      <small>/article/{{ art.slug || art.id }}</small>
                    </div>
                  </el-option>
                </el-select>
                <el-input
                  v-model="newRes.title"
                  placeholder="显示标题（选填，默认使用文章标题）"
                  style="margin-bottom: 8px"
                />
              </template>

              <template v-else-if="newRes.type === 'video'">
                <el-input
                  v-model="newRes.title"
                  placeholder="视频标题（如：Spring Boot 自动装配精讲）"
                  style="margin-bottom: 8px"
                />
                <el-input
                  v-model="newRes.url"
                  placeholder="B站 BV链接 / YouTube / .mp4 直链"
                  style="margin-bottom: 8px"
                />
              </template>

              <template v-else>
                <el-input
                  v-model="newRes.title"
                  placeholder="网页名称（如：Spring Framework 官方文档）"
                  style="margin-bottom: 8px"
                />
                <el-input
                  v-model="newRes.url"
                  placeholder="https:// 网页链接"
                  style="margin-bottom: 8px"
                />
              </template>

              <div class="mounter-footer">
                <el-input
                  v-model="newRes.note"
                  placeholder="学习提示/备注（可选）"
                />
                <el-button type="primary" @click="mountResourceToNode">
                  挂载资源
                </el-button>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">
        保存学习路径
      </el-button>
    </template>

    <!-- 可选 JSON 树编辑器抽屉 -->
    <el-drawer
      v-model="jsonDrawerVisible"
      title="思维导图 treeJson 原始结构"
      size="520px"
      append-to-body
    >
      <p class="json-tip">
        可直接复制备份，或粘贴合法的思维导图树 JSON 并应用到当前编辑器。
      </p>
      <el-input v-model="jsonDraft" type="textarea" :rows="22" class="json-textarea" />
      <div class="json-actions">
        <el-button @click="jsonDrawerVisible = false">关闭</el-button>
        <el-button type="primary" @click="applyJsonDraft">应用到当前导图</el-button>
      </div>
    </el-drawer>
  </el-dialog>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from "element-plus";
import {
  BlogArticleAPI,
  BlogLearningPathAPI,
  type BlogArticleItem,
  type BlogLearningPathForm,
  type MindMapNode,
  type MindMapResource,
  type MindMapResourceType,
} from "@/api/blog";
import {
  addChildNode,
  addSiblingNode,
  cloneMindMapTree,
  computeMindMapLayout,
  countTreeStats,
  createEmptyMindMapTree,
  createUid,
  findNodeById,
  getBranchColor,
  moveNodeOrder,
  normalizeSafeExternalUrl,
  parseMindMapTree,
  removeNodeById,
  updateNodeById,
} from "../mindMapUtils";

const props = defineProps<{
  modelValue: boolean;
  pathId?: string;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  saved: [];
}>();

const metaFormRef = ref<FormInstance>();
const loadingDetail = ref(false);
const saving = ref(false);
const zoom = ref(0.92);

const metaForm = reactive<BlogLearningPathForm>({
  id: undefined,
  title: "",
  slug: "",
  summary: "",
  coverColor: "#0d9488",
  treeJson: "",
  sort: 0,
  status: 1,
});

const metaRules: FormRules = {
  title: [{ required: true, message: "请输入学习路径标题", trigger: "blur" }],
};

const treeRoot = ref<MindMapNode>(createEmptyMindMapTree());
const selectedNodeId = ref<string>(treeRoot.value.id);

const layout = computed(() => computeMindMapLayout(treeRoot.value));
const treeStats = computed(() => countTreeStats(treeRoot.value));
const selectedNode = computed(
  () => findNodeById(treeRoot.value, selectedNodeId.value) ?? treeRoot.value,
);
const isRootSelected = computed(() => selectedNode.value.id === treeRoot.value.id);

const articleOptions = ref<BlogArticleItem[]>([]);
const articleLoading = ref(false);

const resourceTypeTabs: { label: string; value: MindMapResourceType }[] = [
  { label: "站内文章", value: "article" },
  { label: "视频课程", value: "video" },
  { label: "网页资料", value: "webpage" },
];

const newRes = reactive<{
  type: MindMapResourceType;
  title: string;
  articleId: string;
  articleSlug: string;
  url: string;
  note: string;
}>({
  type: "article",
  title: "",
  articleId: "",
  articleSlug: "",
  url: "",
  note: "",
});

const jsonDrawerVisible = ref(false);
const jsonDraft = ref("");

function resourceTypeLabel(type: MindMapResourceType): string {
  if (type === "article") return "文章";
  if (type === "video") return "视频";
  return "网页";
}

function resetStudioState() {
  metaForm.id = undefined;
  metaForm.title = "";
  metaForm.slug = "";
  metaForm.summary = "";
  metaForm.coverColor = "#0d9488";
  metaForm.sort = 0;
  metaForm.status = 1;
  treeRoot.value = createEmptyMindMapTree("核心学习主题");
  selectedNodeId.value = treeRoot.value.id;
  zoom.value = 0.92;
  resetResourceDraft("article");
}

function resetResourceDraft(type: MindMapResourceType) {
  newRes.type = type;
  newRes.title = "";
  newRes.articleId = "";
  newRes.articleSlug = "";
  newRes.url = "";
  newRes.note = "";
}

function switchResourceTab(type: MindMapResourceType) {
  resetResourceDraft(type);
}

async function searchPublishedArticles(keywords = "") {
  articleLoading.value = true;
  try {
    const res = await BlogArticleAPI.getPage({
      pageNum: 1,
      pageSize: 30,
      keywords: keywords.trim() || undefined,
      status: 1,
    });
    articleOptions.value = res.list || [];
  } catch {
    articleOptions.value = [];
  } finally {
    articleLoading.value = false;
  }
}

function handleArticleSelect(articleId: string) {
  const matched = articleOptions.value.find((item) => item.id === articleId);
  if (!matched) {
    newRes.articleSlug = "";
    return;
  }
  newRes.articleSlug = matched.slug || matched.id;
  if (!newRes.title.trim()) {
    newRes.title = matched.title;
  }
}

async function loadPathDetail(id: string) {
  loadingDetail.value = true;
  try {
    const form = await BlogLearningPathAPI.getForm(id);
    metaForm.id = form.id;
    metaForm.title = form.title || "";
    metaForm.slug = form.slug || "";
    metaForm.summary = form.summary || "";
    metaForm.coverColor = form.coverColor || "#0d9488";
    metaForm.sort = Number(form.sort ?? 0);
    metaForm.status = Number(form.status ?? 1);
    treeRoot.value = parseMindMapTree(form.treeJson, form.title || "学习路径");
    selectedNodeId.value = treeRoot.value.id;
  } finally {
    loadingDetail.value = false;
  }
}

watch(
  () => [props.modelValue, props.pathId] as const,
  ([visible, id]) => {
    if (!visible) return;
    resetStudioState();
    void searchPublishedArticles("");
    if (id) {
      void loadPathDetail(id);
    }
  },
  { immediate: true },
);

function selectNode(id: string) {
  selectedNodeId.value = id;
}

function patchSelectedNode(patch: Partial<Pick<MindMapNode, "label" | "description" | "tag">>) {
  treeRoot.value = updateNodeById(treeRoot.value, selectedNode.value.id, (node) => ({
    ...node,
    ...patch,
  }));
}

function handleAddChild() {
  const nextNode: MindMapNode = {
    id: createUid("node"),
    label: "新知识点",
    description: "",
    tag: "",
    collapsed: false,
    resources: [],
    children: [],
  };
  treeRoot.value = addChildNode(treeRoot.value, selectedNode.value.id, nextNode);
  selectedNodeId.value = nextNode.id;
}

function handleAddSibling() {
  const nextNode: MindMapNode = {
    id: createUid("node"),
    label: "同级主题",
    description: "",
    tag: "",
    collapsed: false,
    resources: [],
    children: [],
  };
  treeRoot.value = addSiblingNode(treeRoot.value, selectedNode.value.id, nextNode);
  selectedNodeId.value = nextNode.id;
}

function handleMoveNode(direction: -1 | 1) {
  if (isRootSelected.value) return;
  treeRoot.value = moveNodeOrder(treeRoot.value, selectedNode.value.id, direction);
}

function handleRemoveNode() {
  if (isRootSelected.value) return;
  treeRoot.value = removeNodeById(treeRoot.value, selectedNode.value.id);
  selectedNodeId.value = treeRoot.value.id;
}

function adjustZoom(delta: number) {
  zoom.value = Math.min(1.5, Math.max(0.55, Number((zoom.value + delta).toFixed(2))));
}

function resetZoom() {
  zoom.value = 0.92;
}

function mountResourceToNode() {
  if (newRes.type === "article") {
    if (!newRes.articleId) {
      ElMessage.warning("请先选择一篇博客文章");
      return;
    }
    const matched = articleOptions.value.find((item) => item.id === newRes.articleId);
    const title = newRes.title.trim() || matched?.title || "站内文章";
    const item: MindMapResource = {
      id: createUid("res"),
      type: "article",
      title,
      articleId: newRes.articleId,
      articleSlug: newRes.articleSlug || matched?.slug || newRes.articleId,
      note: newRes.note.trim() || undefined,
    };
    appendResource(item);
    resetResourceDraft("article");
    return;
  }

  const safeUrl = normalizeSafeExternalUrl(newRes.url);
  if (!safeUrl) {
    ElMessage.warning("请输入有效的 http(s):// 或 /uploads/ 资源链接");
    return;
  }
  const title =
    newRes.title.trim() || (newRes.type === "video" ? "视频讲解" : "网页参考资料");
  const item: MindMapResource = {
    id: createUid("res"),
    type: newRes.type,
    title,
    url: safeUrl,
    note: newRes.note.trim() || undefined,
  };
  appendResource(item);
  resetResourceDraft(newRes.type);
}

function appendResource(resource: MindMapResource) {
  treeRoot.value = updateNodeById(treeRoot.value, selectedNode.value.id, (node) => ({
    ...node,
    resources: [...node.resources, resource],
  }));
}

function moveResource(index: number, direction: -1 | 1) {
  const list = [...selectedNode.value.resources];
  const swapIdx = index + direction;
  if (swapIdx < 0 || swapIdx >= list.length) return;
  [list[index], list[swapIdx]] = [list[swapIdx], list[index]];
  treeRoot.value = updateNodeById(treeRoot.value, selectedNode.value.id, (node) => ({
    ...node,
    resources: list,
  }));
}

function removeResource(resourceId: string) {
  treeRoot.value = updateNodeById(treeRoot.value, selectedNode.value.id, (node) => ({
    ...node,
    resources: node.resources.filter((res) => res.id !== resourceId),
  }));
}

function openJsonDrawer() {
  jsonDraft.value = JSON.stringify(treeRoot.value, null, 2);
  jsonDrawerVisible.value = true;
}

function applyJsonDraft() {
  try {
    const parsed = JSON.parse(jsonDraft.value);
    if (!parsed || typeof parsed !== "object") {
      ElMessage.error("JSON 根节点必须为对象");
      return;
    }
    treeRoot.value = parseMindMapTree(jsonDraft.value, metaForm.title || "学习路径");
    selectedNodeId.value = treeRoot.value.id;
    jsonDrawerVisible.value = false;
    ElMessage.success("已应用思维导图结构");
  } catch {
    ElMessage.error("JSON 格式无效，请检查语法");
  }
}

async function handleSubmit() {
  await metaFormRef.value?.validate();
  saving.value = true;
  try {
    const normalizedTree = cloneMindMapTree(treeRoot.value);
    const payload: BlogLearningPathForm = {
      id: metaForm.id,
      title: metaForm.title.trim(),
      slug: metaForm.slug?.trim() || undefined,
      summary: metaForm.summary?.trim() || undefined,
      coverColor: metaForm.coverColor?.trim() || "#0d9488",
      treeJson: JSON.stringify(normalizedTree),
      sort: Number(metaForm.sort || 0),
      status: metaForm.status ?? 1,
    };
    if (metaForm.id) {
      await BlogLearningPathAPI.update(metaForm.id, payload);
      ElMessage.success("学习路径已更新");
    } else {
      await BlogLearningPathAPI.create(payload);
      ElMessage.success("学习路径创建成功");
    }
    emit("update:modelValue", false);
    emit("saved");
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped lang="scss">
.studio-shell {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-form {
  padding: 12px 14px 4px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-fill-color-blank);
}

.meta-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr auto auto auto;
  gap: 10px;
  align-items: start;
}

.summary-item {
  margin-bottom: 10px;
}

.color-picker-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-swatch {
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
}

.studio-workspace {
  display: grid;
  grid-template-columns: minmax(420px, 1fr) 396px;
  gap: 14px;
  min-height: 540px;
}

.canvas-pane,
.inspector-pane {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.pane-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
}

.toolbar-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.stats-pill {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--el-fill-color);
}

.canvas-scroll {
  flex: 1;
  min-height: 480px;
  max-height: 560px;
  overflow: auto;
  background-color: var(--el-fill-color-extra-light);
  background-image: radial-gradient(var(--el-border-color) 1px, transparent 1px);
  background-size: 18px 18px;
}

.canvas-stage {
  position: relative;
}

.canvas-world {
  position: relative;
  transform-origin: 0 0;
}

.edge-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.studio-node {
  position: absolute;
  text-align: left;
  padding: 8px 10px 8px 13px;
  border-radius: 12px;
  border: 1.5px solid var(--el-border-color);
  background: var(--el-bg-color);
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.05);
  overflow: hidden;

  &:hover {
    border-color: var(--branch-color);
    transform: translateY(-1px);
  }

  &.is-root {
    border-width: 2px;
    border-color: var(--branch-color);
    box-shadow: 0 4px 16px rgba(13, 148, 136, 0.14);
  }

  &.is-selected {
    border-color: var(--branch-color);
    box-shadow: 0 0 0 3px rgba(13, 148, 136, 0.22);
  }
}

.node-accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--branch-color);
}

.node-title {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--el-text-color-primary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.node-desc {
  margin-top: 3px;
  font-size: 11px;
  line-height: 1.35;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-badges {
  margin-top: 5px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.mini-tag,
.res-badge {
  font-size: 10px;
  line-height: 1.3;
  padding: 1px 5px;
  border-radius: 999px;
  font-weight: 500;
}

.mini-tag {
  background: var(--el-fill-color);
  color: var(--el-text-color-regular);
}

.res-badge.is-article {
  background: rgba(13, 148, 136, 0.12);
  color: #0f766e;
}

.res-badge.is-video {
  background: rgba(217, 119, 6, 0.14);
  color: #b45309;
}

.res-badge.is-webpage {
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
}

.inspector-pane {
  padding: 12px;
  gap: 14px;
  overflow-y: auto;
  max-height: 610px;
}

.inspector-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.section-sub {
  font-size: 11px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.resources-section {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
  flex: 1;
}

.mounted-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 185px;
  overflow-y: auto;
}

.mounted-item {
  padding: 7px 9px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-extra-light);
}

.mounted-top {
  display: flex;
  align-items: center;
  gap: 6px;
}

.res-type-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 6px;
  flex-shrink: 0;

  &.is-article {
    background: rgba(13, 148, 136, 0.14);
    color: #0f766e;
  }

  &.is-video {
    background: rgba(217, 119, 6, 0.15);
    color: #b45309;
  }

  &.is-webpage {
    background: rgba(37, 99, 235, 0.13);
    color: #1d4ed8;
  }
}

.mounted-title {
  flex: 1;
  font-size: 12.5px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mounted-ops {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.mounted-target,
.mounted-note {
  margin-top: 3px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mounted-empty {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  padding: 10px;
  text-align: center;
  border-radius: 8px;
  background: var(--el-fill-color-light);
}

.mounter-box {
  margin-top: 4px;
  padding: 10px;
  border-radius: 9px;
  border: 1px dashed var(--el-border-color);
  background: var(--el-fill-color-blank);
}

.mounter-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-bottom: 10px;
}

.mounter-tab {
  padding: 5px 8px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 7px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: all 0.15s ease;

  &.active {
    border-color: var(--el-color-primary);
    background: rgba(13, 148, 136, 0.12);
    color: var(--el-color-primary);
    font-weight: 600;
  }
}

.mounter-footer {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.article-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;

  small {
    color: var(--el-text-color-secondary);
  }
}

.json-tip {
  margin: 0 0 10px;
  font-size: 12.5px;
  color: var(--el-text-color-secondary);
}

.json-textarea {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.json-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 1120px) {
  .meta-grid {
    grid-template-columns: 1fr 1fr;
  }

  .studio-workspace {
    grid-template-columns: 1fr;
  }
}
</style>
