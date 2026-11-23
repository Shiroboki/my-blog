<template>
  <section class="paths-page">
    <!-- 顶部路径导览与操作栏 -->
    <header class="card paths-header animate-rise">
      <div class="paths-header-top">
        <div>
          <div class="paths-eyebrow">LEARNING ROADMAPS</div>
          <h1 class="paths-heading">学习路径与思维导图</h1>
          <p class="paths-sub">
            将零散的文章、教程视频与官方文档串联成可交互知识地图，点击任意节点即可查看或播放挂载资源。
          </p>
        </div>

        <div class="paths-header-actions">
          <button type="button" class="btn btn-primary" @click="startCreateNewPath">
            + 新建思维导图
          </button>
          <button
            v-if="currentPath"
            type="button"
            class="btn"
            :class="{ 'is-editing-btn': editMode }"
            @click="toggleEditMode"
          >
            {{ editMode ? "退出编辑" : "编辑当前导图" }}
          </button>
          <a href="/admin/#/blog/learning-path" class="btn">后台管理</a>
        </div>
      </div>

      <div class="paths-switcher-row">
        <div class="paths-tabs" role="tablist" aria-label="学习路径切换">
          <button
            v-for="item in allPaths"
            :key="item.uid"
            type="button"
            class="path-pill"
            :class="{ active: selectedPathUid === item.uid }"
            @click="selectPath(item.uid)"
          >
            <span
              class="path-dot"
              :style="{ background: item.coverColor || '#0d9488' }"
              aria-hidden="true"
            />
            <span class="path-pill-title">{{ item.title }}</span>
            <span v-if="item.source === 'local'" class="path-source-badge">本机</span>
          </button>
        </div>

        <div class="paths-tools-right">
          <div v-if="activeTreeStats.nodeCount" class="progress-badge">
            <span>
              进度 {{ completedCountInCurrent }} / {{ activeTreeStats.nodeCount }}（{{ progressPercent }}%）
            </span>
            <div class="progress-mini-track" aria-hidden="true">
              <div class="progress-mini-fill" :style="{ width: `${progressPercent}%` }" />
            </div>
          </div>

          <input
            v-model="nodeSearchQuery"
            type="text"
            class="path-search-input"
            placeholder="搜索节点或资源..."
            aria-label="搜索思维导图节点或资源"
          />
        </div>
      </div>

      <div v-if="statusNotice" class="paths-notice" role="status">
        {{ statusNotice }}
      </div>
    </header>

    <div v-if="loading && !allPaths.length" class="card empty-paths-card">
      正在加载学习路径思维导图...
    </div>

    <div v-else-if="!currentPath || !workingTree" class="card empty-paths-card">
      <div class="empty-paths-title">还没有学习路径导图</div>
      <p class="empty-paths-desc">你可以立即新建一张思维导图，在节点中挂载文章、视频与网页资源。</p>
      <button type="button" class="btn btn-primary" @click="startCreateNewPath">
        创建第一张思维导图
      </button>
    </div>

    <!-- 主工作区：左侧思维导图画布 + 右侧节点详情/资源/编辑面板 -->
    <div v-else class="paths-workspace">
      <div class="workspace-canvas-col">
        <div v-if="editMode" class="card meta-edit-bar">
          <div class="meta-field meta-field--wide">
            <label class="meta-label">导图标题</label>
            <input v-model="draftMeta.title" type="text" class="meta-input" maxlength="128" placeholder="例如：Spring Boot 微服务实战路线" />
          </div>
          <div class="meta-field">
            <label class="meta-label">URL 别名（选填）</label>
            <input v-model="draftMeta.slug" type="text" class="meta-input" maxlength="128" placeholder="例如：spring-cloud-path" />
          </div>
          <div class="meta-field meta-field--wide">
            <label class="meta-label">路径简介</label>
            <input v-model="draftMeta.summary" type="text" class="meta-input" maxlength="500" placeholder="简短说明该学习路线的目标与适用读者" />
          </div>
          <div class="meta-save-group">
            <button
              type="button"
              class="btn btn-primary"
              :disabled="saving"
              @click="saveCurrentPath"
            >
              {{ saving ? "保存中..." : hasAdminToken ? "保存并同步全站" : "保存导图（本机）" }}
            </button>
            <button
              v-if="currentPath.source === 'local'"
              type="button"
              class="btn"
              @click="deleteCurrentLocalPath"
            >
              删除本机导图
            </button>
          </div>
        </div>

        <MindMapCanvas
          ref="canvasRef"
          :tree="workingTree"
          :selected-node-id="selectedNodeId"
          :completed-ids="completedIdsForCurrent"
          :accent-color="editMode ? draftMeta.coverColor : currentPath.coverColor"
          :search-query="nodeSearchQuery"
          :editable="editMode"
          @select-node="handleSelectNode"
          @toggle-collapse="handleToggleCollapse"
          @toggle-complete="handleToggleComplete"
          @expand-all="handleExpandAll"
          @collapse-branches="handleCollapseBranches"
          @add-child="handleAddChildNode"
          @add-sibling="handleAddSiblingNode"
          @remove-node="handleRemoveNode"
        />
      </div>

      <aside class="card inspector-col">
        <template v-if="selectedNode">
          <!-- 面包屑导航与节点头部 -->
          <div class="inspector-breadcrumb">
            <button
              v-for="(ancestor, idx) in selectedChain"
              :key="ancestor.id"
              type="button"
              class="crumb-btn"
              :class="{ 'is-current': ancestor.id === selectedNode.id }"
              @click="handleSelectNode(ancestor.id)"
            >
              <span>{{ ancestor.label }}</span>
              <span v-if="idx < selectedChain.length - 1" class="crumb-sep">/</span>
            </button>
          </div>

          <div v-if="!editMode" class="inspector-view">
            <div class="inspector-title-row">
              <h2 class="inspector-title">{{ selectedNode.label }}</h2>
              <span v-if="selectedNode.tag" class="inspector-tag">{{ selectedNode.tag }}</span>
            </div>

            <p class="inspector-desc">
              {{ selectedNode.description || "点击下方已挂载的文章、视频或参考网页开始学习。" }}
            </p>

            <div class="inspector-status-row">
              <button
                type="button"
                class="mastery-btn"
                :class="{ 'is-done': completedSetForCurrent.has(selectedNode.id) }"
                @click="handleToggleComplete(selectedNode.id)"
              >
                {{ completedSetForCurrent.has(selectedNode.id) ? "已掌握该节点（点击取消）" : "标记为已掌握" }}
              </button>
              <span class="res-total-label">
                挂载 {{ selectedNode.resources.length }} 项资源
              </span>
            </div>

            <div v-if="selectedNode.resources.length" class="res-filter-tabs">
              <button
                type="button"
                class="res-tab"
                :class="{ active: resourceFilter === 'all' }"
                @click="resourceFilter = 'all'"
              >
                全部 {{ selectedNode.resources.length }}
              </button>
              <button
                v-if="selectedResourceCounts.article"
                type="button"
                class="res-tab"
                :class="{ active: resourceFilter === 'article' }"
                @click="resourceFilter = 'article'"
              >
                文章 {{ selectedResourceCounts.article }}
              </button>
              <button
                v-if="selectedResourceCounts.video"
                type="button"
                class="res-tab"
                :class="{ active: resourceFilter === 'video' }"
                @click="resourceFilter = 'video'"
              >
                视频 {{ selectedResourceCounts.video }}
              </button>
              <button
                v-if="selectedResourceCounts.webpage"
                type="button"
                class="res-tab"
                :class="{ active: resourceFilter === 'webpage' }"
                @click="resourceFilter = 'webpage'"
              >
                网页 {{ selectedResourceCounts.webpage }}
              </button>
            </div>

            <div v-if="!filteredNodeResources.length" class="inspector-empty">
              该节点暂未挂载{{ resourceFilter === "all" ? "" : resourceTypeLabel(resourceFilter) }}资源，可点击上方「编辑当前导图」添加。
            </div>

            <div v-else class="resource-list">
              <article
                v-for="res in filteredNodeResources"
                :key="res.id"
                class="resource-card"
                :class="`is-${res.type}`"
              >
                <div class="resource-card-head">
                  <span class="resource-type-pill" :class="`is-${res.type}`">
                    {{ resourceTypeLabel(res.type) }}
                  </span>
                  <span v-if="res.type === 'video' && getVideoSource(res.url)" class="resource-host">
                    {{ getVideoSource(res.url)?.platform }}
                  </span>
                  <span v-else-if="res.type === 'webpage' && res.url" class="resource-host">
                    {{ extractDomainHost(res.url) }}
                  </span>
                  <span v-else-if="res.type === 'article'" class="resource-host">站内文章</span>
                </div>

                <h3 class="resource-title">{{ res.title }}</h3>

                <p v-if="res.note" class="resource-note">{{ res.note }}</p>
                <p
                  v-else-if="res.type === 'article' && lookupArticleSummary(res)"
                  class="resource-note"
                >
                  {{ lookupArticleSummary(res) }}
                </p>

                <!-- 站内文章跳转操作 -->
                <div v-if="res.type === 'article'" class="resource-actions">
                  <RouterLink :to="resolveArticleLink(res)" class="res-primary-link">
                    进入站内阅读
                  </RouterLink>
                  <a
                    :href="resolveArticleLink(res)"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="res-secondary-link"
                  >
                    新窗口打开
                  </a>
                </div>

                <!-- 视频播放器与外链操作 -->
                <div v-else-if="res.type === 'video'" class="resource-video-box">
                  <div class="resource-actions">
                    <button
                      v-if="canEmbedVideo(res.url)"
                      type="button"
                      class="res-primary-btn"
                      @click="toggleInlinePreview(res.id)"
                    >
                      {{ activePreviewResId === res.id ? "收起播放器" : "在导图内播放" }}
                    </button>
                    <a
                      v-if="normalizeSafeExternalUrl(res.url)"
                      :href="normalizeSafeExternalUrl(res.url)!"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="res-secondary-link"
                    >
                      打开视频页
                    </a>
                  </div>

                  <div
                    v-if="activePreviewResId === res.id && getVideoSource(res.url)"
                    class="embedded-media-wrap"
                  >
                    <video
                      v-if="getVideoSource(res.url)?.kind === 'native'"
                      :src="getVideoSource(res.url)?.embedUrl"
                      controls
                      playsinline
                      class="embedded-video"
                    />
                    <iframe
                      v-else-if="getVideoSource(res.url)?.kind === 'iframe'"
                      :src="getVideoSource(res.url)?.embedUrl"
                      title="嵌入视频播放器"
                      allowfullscreen
                      referrerpolicy="no-referrer"
                      class="embedded-iframe"
                    />
                  </div>
                </div>

                <!-- 网页预览与安全外链跳转 -->
                <div v-else class="resource-webpage-box">
                  <div class="resource-actions">
                    <a
                      v-if="normalizeSafeExternalUrl(res.url)"
                      :href="normalizeSafeExternalUrl(res.url)!"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="res-primary-link"
                    >
                      访问网页
                    </a>
                    <button
                      v-if="normalizeSafeExternalUrl(res.url)"
                      type="button"
                      class="res-secondary-btn"
                      @click="toggleInlinePreview(res.id)"
                    >
                      {{ activePreviewResId === res.id ? "收起内嵌预览" : "站内尝试预览" }}
                    </button>
                  </div>

                  <div
                    v-if="activePreviewResId === res.id && normalizeSafeExternalUrl(res.url)"
                    class="embedded-media-wrap"
                  >
                    <iframe
                      :src="normalizeSafeExternalUrl(res.url)!"
                      title="网页内嵌预览"
                      sandbox="allow-same-origin allow-scripts allow-popups allow-forms"
                      referrerpolicy="no-referrer"
                      class="embedded-iframe embedded-iframe--web"
                    />
                    <p class="embed-fallback-tip">
                      若目标网站启用了禁止内嵌策略（X-Frame-Options），可点击上方「访问网页」在新标签查看。
                    </p>
                  </div>
                </div>
              </article>
            </div>
          </div>

          <!-- 编辑模式：修改节点属性、结构与挂载文章/视频/网页 -->
          <div v-else class="inspector-edit">
            <div class="edit-tree-actions">
              <button type="button" class="mini-action-btn" @click="handleAddChildNode(selectedNode.id)">
                + 子节点
              </button>
              <button
                v-if="selectedNode.id !== workingTree.id"
                type="button"
                class="mini-action-btn"
                @click="handleAddSiblingNode(selectedNode.id)"
              >
                + 同级
              </button>
              <button
                v-if="selectedNode.id !== workingTree.id"
                type="button"
                class="mini-action-btn"
                @click="handleMoveNode(selectedNode.id, -1)"
              >
                上移
              </button>
              <button
                v-if="selectedNode.id !== workingTree.id"
                type="button"
                class="mini-action-btn"
                @click="handleMoveNode(selectedNode.id, 1)"
              >
                下移
              </button>
              <button
                v-if="selectedNode.id !== workingTree.id"
                type="button"
                class="mini-action-btn is-danger"
                @click="handleRemoveNode(selectedNode.id)"
              >
                删除节点
              </button>
            </div>

            <div class="edit-form-group">
              <label class="meta-label">节点名称</label>
              <input
                :value="selectedNode.label"
                type="text"
                maxlength="100"
                class="meta-input"
                placeholder="输入节点标题"
                @input="updateSelectedField('label', ($event.target as HTMLInputElement).value)"
              />
            </div>

            <div class="edit-form-row">
              <div class="edit-form-group">
                <label class="meta-label">标签徽章（选填）</label>
                <input
                  :value="selectedNode.tag || ''"
                  type="text"
                  maxlength="24"
                  class="meta-input"
                  placeholder="核心 / 实战 / 选学"
                  @input="updateSelectedField('tag', ($event.target as HTMLInputElement).value)"
                />
              </div>
            </div>

            <div class="edit-form-group">
              <label class="meta-label">节点说明</label>
              <textarea
                :value="selectedNode.description || ''"
                rows="2"
                maxlength="500"
                class="meta-textarea"
                placeholder="补充学习目标或要点说明"
                @input="updateSelectedField('description', ($event.target as HTMLTextAreaElement).value)"
              />
            </div>

            <div class="edit-divider" />

            <div class="mounter-header">
              <h3 class="mounter-title">挂载资源（{{ selectedNode.resources.length }}）</h3>
              <div class="mounter-type-tabs">
                <button
                  type="button"
                  class="type-tab"
                  :class="{ active: newResForm.type === 'article' }"
                  @click="switchResourceType('article')"
                >
                  + 文章
                </button>
                <button
                  type="button"
                  class="type-tab"
                  :class="{ active: newResForm.type === 'video' }"
                  @click="switchResourceType('video')"
                >
                  + 视频
                </button>
                <button
                  type="button"
                  class="type-tab"
                  :class="{ active: newResForm.type === 'webpage' }"
                  @click="switchResourceType('webpage')"
                >
                  + 网页
                </button>
              </div>
            </div>

            <form class="mounter-form" @submit.prevent="appendResourceToSelectedNode">
              <template v-if="newResForm.type === 'article'">
                <label class="meta-label">选择站内文章</label>
                <select v-model="newResForm.articleId" class="meta-select" @change="onSelectArticleOption">
                  <option value="">-- 请选择文章 --</option>
                  <option
                    v-for="art in articleCatalog"
                    :key="art.id"
                    :value="art.id"
                  >
                    {{ art.title }}
                  </option>
                </select>
              </template>

              <template v-else>
                <label class="meta-label">
                  {{ newResForm.type === "video" ? "视频地址" : "网页链接" }}
                </label>
                <input
                  v-model="newResForm.url"
                  type="text"
                  class="meta-input"
                  :placeholder="
                    newResForm.type === 'video'
                      ? '支持 Bilibili / YouTube 链接或 MP4 直链'
                      : 'https://...'
                  "
                />
              </template>

              <label class="meta-label">展示名称</label>
              <input
                v-model="newResForm.title"
                type="text"
                maxlength="160"
                class="meta-input"
                placeholder="输入挂载资源展示标题"
              />

              <label class="meta-label">学习备注（选填）</label>
              <input
                v-model="newResForm.note"
                type="text"
                maxlength="300"
                class="meta-input"
                placeholder="例如：重点看第 2 节或前置必读"
              />

              <button type="submit" class="btn btn-primary mount-submit-btn">
                挂载到当前节点
              </button>
            </form>

            <div v-if="selectedNode.resources.length" class="mounted-edit-list">
              <div
                v-for="(res, rIdx) in selectedNode.resources"
                :key="res.id"
                class="mounted-edit-row"
              >
                <span class="resource-type-pill" :class="`is-${res.type}`">
                  {{ resourceTypeLabel(res.type) }}
                </span>
                <span class="mounted-edit-title">{{ res.title }}</span>
                <div class="mounted-edit-ops">
                  <button
                    type="button"
                    class="mounted-op-btn"
                    :disabled="rIdx === 0"
                    @click="moveResourceOrder(rIdx, -1)"
                  >
                    ↑
                  </button>
                  <button
                    type="button"
                    class="mounted-op-btn"
                    :disabled="rIdx === selectedNode.resources.length - 1"
                    @click="moveResourceOrder(rIdx, 1)"
                  >
                    ↓
                  </button>
                  <button
                    type="button"
                    class="mounted-op-btn is-danger"
                    @click="removeResourceAt(rIdx)"
                  >
                    移除
                  </button>
                </div>
              </div>
            </div>
          </div>
        </template>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  blogAdminLearningPathApi,
  blogApi,
  getArticleRoute,
  type BlogArticleItem,
  type BlogLearningPathItem,
} from "@/api/blog";
import MindMapCanvas from "@/components/MindMapCanvas.vue";
import { useSiteInfo } from "@/composables/useSiteInfo";
import {
  addChildNode,
  addSiblingNode,
  cloneMindMapTree,
  collectAllNodeIds,
  countTreeStats,
  createEmptyMindMapTree,
  createUid,
  extractDomainHost,
  findNodeById,
  findNodeChain,
  getAdminSessionToken,
  moveNodeOrder,
  normalizeSafeExternalUrl,
  parseMindMapTree,
  parseVideoEmbed,
  removeNodeById,
  setAllCollapsed,
  toggleNodeCollapse,
  updateNodeById,
  type MindMapNode,
  type MindMapResource,
  type MindMapResourceType,
} from "@/utils/mindMap";
import { updatePageSeo } from "@/utils/seo";

interface PathCollectionEntry extends BlogLearningPathItem {
  uid: string;
  source: "server" | "local";
}

const LOCAL_PATHS_KEY = "myblog_local_learning_paths_v1";
const PROGRESS_STORAGE_KEY = "myblog_path_progress_v1";

const route = useRoute();
const router = useRouter();
const { loadSiteInfo } = useSiteInfo();

const canvasRef = ref<InstanceType<typeof MindMapCanvas> | null>(null);
const loading = ref(false);
const saving = ref(false);
const editMode = ref(false);
const statusNotice = ref("");
const nodeSearchQuery = ref("");
const resourceFilter = ref<"all" | MindMapResourceType>("all");
const activePreviewResId = ref<string | null>(null);

const serverPaths = ref<PathCollectionEntry[]>([]);
const localPaths = ref<PathCollectionEntry[]>(loadLocalPaths());
const articleCatalog = ref<BlogArticleItem[]>([]);
const selectedPathUid = ref("");
const workingTree = ref<MindMapNode | null>(null);
const selectedNodeId = ref("");
const progressMap = ref<Record<string, string[]>>(loadProgressMap());
const hasAdminToken = ref(Boolean(getAdminSessionToken()));

const draftMeta = reactive({
  title: "",
  slug: "",
  summary: "",
  coverColor: "#0d9488",
});

const newResForm = reactive<{
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

const allPaths = computed<PathCollectionEntry[]>(() => [
  ...serverPaths.value,
  ...localPaths.value,
]);

const currentPath = computed<PathCollectionEntry | null>(
  () => allPaths.value.find((item) => item.uid === selectedPathUid.value) ?? allPaths.value[0] ?? null,
);

const selectedNode = computed<MindMapNode | null>(() => {
  if (!workingTree.value) return null;
  return findNodeById(workingTree.value, selectedNodeId.value) ?? workingTree.value;
});

const selectedChain = computed<MindMapNode[]>(() => {
  if (!workingTree.value || !selectedNode.value) return [];
  return findNodeChain(workingTree.value, selectedNode.value.id);
});

const activeTreeStats = computed(() =>
  workingTree.value
    ? countTreeStats(workingTree.value)
    : { nodeCount: 0, resourceCount: 0, breakdown: { article: 0, video: 0, webpage: 0 } },
);

const completedIdsForCurrent = computed<string[]>(() => {
  const key = currentPath.value?.uid;
  return key ? progressMap.value[key] || [] : [];
});

const completedSetForCurrent = computed(() => new Set(completedIdsForCurrent.value));

const completedCountInCurrent = computed(() => {
  if (!workingTree.value) return 0;
  const existing = new Set(collectAllNodeIds(workingTree.value));
  return completedIdsForCurrent.value.filter((id) => existing.has(id)).length;
});

const progressPercent = computed(() => {
  const total = activeTreeStats.value.nodeCount;
  if (!total) return 0;
  return Math.round((completedCountInCurrent.value / total) * 100);
});

const selectedResourceCounts = computed(() => {
  const counts = { article: 0, video: 0, webpage: 0 };
  for (const res of selectedNode.value?.resources || []) {
    counts[res.type] += 1;
  }
  return counts;
});

const filteredNodeResources = computed<MindMapResource[]>(() => {
  const list = selectedNode.value?.resources || [];
  if (resourceFilter.value === "all") return list;
  return list.filter((item) => item.type === resourceFilter.value);
});

function loadLocalPaths(): PathCollectionEntry[] {
  try {
    const raw = localStorage.getItem(LOCAL_PATHS_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.map((item) => ({
      ...item,
      uid: String(item.uid || `local-${item.id}`),
      source: "local" as const,
    }));
  } catch {
    return [];
  }
}

function persistLocalPaths() {
  try {
    localStorage.setItem(LOCAL_PATHS_KEY, JSON.stringify(localPaths.value));
  } catch {
    // 忽略本地存储空间限制
  }
}

function loadProgressMap(): Record<string, string[]> {
  try {
    const raw = localStorage.getItem(PROGRESS_STORAGE_KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

function persistProgressMap() {
  try {
    localStorage.setItem(PROGRESS_STORAGE_KEY, JSON.stringify(progressMap.value));
  } catch {
    // 忽略本地存储异常
  }
}

function activatePath(entry: PathCollectionEntry | null) {
  if (!entry) {
    workingTree.value = null;
    selectedNodeId.value = "";
    return;
  }
  selectedPathUid.value = entry.uid;
  const parsed = parseMindMapTree(entry.treeJson, entry.title);
  workingTree.value = parsed;
  selectedNodeId.value = parsed.id;
  draftMeta.title = entry.title;
  draftMeta.slug = entry.slug || "";
  draftMeta.summary = entry.summary || "";
  draftMeta.coverColor = entry.coverColor || "#0d9488";
  resourceFilter.value = "all";
  activePreviewResId.value = null;

  updatePageSeo({
    title: `${entry.title} - 学习路径`,
    description: entry.summary || `${entry.title} 交互式思维导图学习路径`,
    path: entry.slug ? `/paths/${encodeURIComponent(entry.slug)}` : `/paths/${entry.id}`,
    type: "website",
  });
}

function selectPath(uid: string) {
  editMode.value = false;
  statusNotice.value = "";
  const found = allPaths.value.find((item) => item.uid === uid);
  if (!found) return;
  activatePath(found);
  if (found.source === "server") {
    const targetPath = found.slug ? `/paths/${encodeURIComponent(found.slug)}` : `/paths/${found.id}`;
    if (route.path !== targetPath) {
      void router.replace(targetPath);
    }
  }
}

function handleSelectNode(nodeId: string) {
  selectedNodeId.value = nodeId;
  resourceFilter.value = "all";
  activePreviewResId.value = null;
}

function handleToggleCollapse(nodeId: string) {
  if (!workingTree.value) return;
  workingTree.value = toggleNodeCollapse(workingTree.value, nodeId);
}

function handleExpandAll() {
  if (!workingTree.value) return;
  workingTree.value = setAllCollapsed(workingTree.value, false);
  nextTick(() => canvasRef.value?.fitToView());
}

function handleCollapseBranches() {
  if (!workingTree.value) return;
  workingTree.value = setAllCollapsed(workingTree.value, true);
  nextTick(() => canvasRef.value?.fitToView());
}

function handleToggleComplete(nodeId: string) {
  const key = currentPath.value?.uid;
  if (!key) return;
  const current = progressMap.value[key] || [];
  const exists = current.includes(nodeId);
  progressMap.value = {
    ...progressMap.value,
    [key]: exists ? current.filter((id) => id !== nodeId) : [...current, nodeId],
  };
  persistProgressMap();
}

function startCreateNewPath() {
  hasAdminToken.value = Boolean(getAdminSessionToken());
  statusNotice.value = "";
  const id = createUid("local-path");
  const uid = `local-${id}`;
  const tree = createEmptyMindMapTree("我的新学习路径");
  const entry: PathCollectionEntry = {
    id,
    uid,
    source: "local",
    title: "我的新学习路径",
    slug: "",
    summary: "点击任意节点可挂载站内文章、视频教程或参考网页。",
    coverColor: "#0d9488",
    treeJson: JSON.stringify(tree),
    nodeCount: 3,
    resourceCount: 0,
    sort: allPaths.value.length + 1,
    status: 1,
  };
  localPaths.value = [entry, ...localPaths.value];
  persistLocalPaths();
  activatePath(entry);
  editMode.value = true;
}

function toggleEditMode() {
  hasAdminToken.value = Boolean(getAdminSessionToken());
  statusNotice.value = "";
  if (editMode.value) {
    editMode.value = false;
    if (currentPath.value) {
      activatePath(currentPath.value);
    }
    return;
  }
  if (currentPath.value) {
    draftMeta.title = currentPath.value.title;
    draftMeta.slug = currentPath.value.slug || "";
    draftMeta.summary = currentPath.value.summary || "";
    draftMeta.coverColor = currentPath.value.coverColor || "#0d9488";
    workingTree.value = cloneMindMapTree(
      parseMindMapTree(currentPath.value.treeJson, currentPath.value.title),
    );
  }
  editMode.value = true;
}

function handleAddChildNode(parentId: string) {
  if (!workingTree.value) return;
  const child: MindMapNode = {
    id: createUid("node"),
    label: "新知识点",
    description: "",
    tag: "",
    collapsed: false,
    resources: [],
    children: [],
  };
  workingTree.value = addChildNode(workingTree.value, parentId, child);
  selectedNodeId.value = child.id;
}

function handleAddSiblingNode(targetId: string) {
  if (!workingTree.value) return;
  const sibling: MindMapNode = {
    id: createUid("node"),
    label: "新同级节点",
    description: "",
    tag: "",
    collapsed: false,
    resources: [],
    children: [],
  };
  workingTree.value = addSiblingNode(workingTree.value, targetId, sibling);
  selectedNodeId.value = sibling.id;
}

function handleMoveNode(targetId: string, delta: -1 | 1) {
  if (!workingTree.value) return;
  workingTree.value = moveNodeOrder(workingTree.value, targetId, delta);
}

function handleRemoveNode(targetId: string) {
  if (!workingTree.value || targetId === workingTree.value.id) return;
  workingTree.value = removeNodeById(workingTree.value, targetId);
  selectedNodeId.value = workingTree.value.id;
}

function updateSelectedField(field: "label" | "description" | "tag", value: string) {
  if (!workingTree.value || !selectedNode.value) return;
  const targetId = selectedNode.value.id;
  workingTree.value = updateNodeById(workingTree.value, targetId, (node) => ({
    ...node,
    [field]: value,
  }));
}

function switchResourceType(type: MindMapResourceType) {
  newResForm.type = type;
  newResForm.title = "";
  newResForm.articleId = "";
  newResForm.articleSlug = "";
  newResForm.url = "";
  newResForm.note = "";
}

function onSelectArticleOption() {
  const matched = articleCatalog.value.find((item) => item.id === newResForm.articleId);
  if (!matched) return;
  newResForm.title = matched.title;
  newResForm.articleSlug = matched.slug || "";
}

function appendResourceToSelectedNode() {
  if (!workingTree.value || !selectedNode.value) return;
  statusNotice.value = "";
  const title = newResForm.title.trim();
  if (!title) {
    statusNotice.value = "请先填写挂载资源的展示名称";
    return;
  }

  let resource: MindMapResource;
  if (newResForm.type === "article") {
    if (!newResForm.articleId) {
      statusNotice.value = "请选择要挂载的站内文章";
      return;
    }
    resource = {
      id: createUid("res"),
      type: "article",
      title,
      articleId: newResForm.articleId,
      articleSlug: newResForm.articleSlug || undefined,
      note: newResForm.note.trim() || undefined,
    };
  } else {
    const safeUrl = normalizeSafeExternalUrl(newResForm.url);
    if (!safeUrl) {
      statusNotice.value = "请输入合法的 http:// 或 https:// 链接";
      return;
    }
    resource = {
      id: createUid("res"),
      type: newResForm.type,
      title,
      url: safeUrl,
      note: newResForm.note.trim() || undefined,
    };
  }

  const targetId = selectedNode.value.id;
  workingTree.value = updateNodeById(workingTree.value, targetId, (node) => ({
    ...node,
    resources: [...node.resources, resource],
  }));
  switchResourceType(newResForm.type);
}

function moveResourceOrder(index: number, delta: -1 | 1) {
  if (!workingTree.value || !selectedNode.value) return;
  const targetIdx = index + delta;
  const list = [...selectedNode.value.resources];
  if (targetIdx < 0 || targetIdx >= list.length) return;
  [list[index], list[targetIdx]] = [list[targetIdx], list[index]];
  workingTree.value = updateNodeById(workingTree.value, selectedNode.value.id, (node) => ({
    ...node,
    resources: list,
  }));
}

function removeResourceAt(index: number) {
  if (!workingTree.value || !selectedNode.value) return;
  const list = selectedNode.value.resources.filter((_, idx) => idx !== index);
  workingTree.value = updateNodeById(workingTree.value, selectedNode.value.id, (node) => ({
    ...node,
    resources: list,
  }));
}

async function saveCurrentPath() {
  if (!currentPath.value || !workingTree.value) return;
  const cleanTitle = draftMeta.title.trim();
  if (!cleanTitle) {
    statusNotice.value = "请填写学习路径标题";
    return;
  }

  const serializedTree = JSON.stringify(workingTree.value);
  const stats = countTreeStats(workingTree.value);
  const token = getAdminSessionToken();
  hasAdminToken.value = Boolean(token);

  if (token) {
    saving.value = true;
    statusNotice.value = "";
    try {
      const payload = {
        title: cleanTitle,
        slug: draftMeta.slug.trim() || undefined,
        summary: draftMeta.summary.trim() || undefined,
        coverColor: draftMeta.coverColor,
        treeJson: serializedTree,
        sort: currentPath.value.sort ?? 1,
        status: 1,
      };
      const isServer = currentPath.value.source === "server";
      const resultId = await blogAdminLearningPathApi.saveWithToken(
        payload,
        token,
        isServer ? currentPath.value.id : undefined,
      );
      if (!isServer) {
        localPaths.value = localPaths.value.filter((item) => item.uid !== currentPath.value?.uid);
        persistLocalPaths();
      }
      await loadLearningPaths(isServer ? currentPath.value.id : String(resultId || ""));
      editMode.value = false;
      statusNotice.value = "学习路径已保存并同步发布到全站";
      return;
    } catch (err) {
      statusNotice.value =
        err instanceof Error
          ? `${err.message}（若登录已过期，请先前往后台重新登录）`
          : "保存到服务端失败";
    } finally {
      saving.value = false;
    }
    return;
  }

  // 未登录后台时：更新或转为本机浏览器保存的自定义导图，保障前台立即可创作体验
  if (currentPath.value.source === "local") {
    localPaths.value = localPaths.value.map((item) =>
      item.uid === currentPath.value?.uid
        ? {
            ...item,
            title: cleanTitle,
            slug: draftMeta.slug.trim() || undefined,
            summary: draftMeta.summary.trim() || undefined,
            coverColor: draftMeta.coverColor,
            treeJson: serializedTree,
            nodeCount: stats.nodeCount,
            resourceCount: stats.resourceCount,
          }
        : item,
    );
    persistLocalPaths();
    activatePath(localPaths.value.find((item) => item.uid === currentPath.value?.uid) || null);
  } else {
    const newId = createUid("local-path");
    const newUid = `local-${newId}`;
    const copyEntry: PathCollectionEntry = {
      id: newId,
      uid: newUid,
      source: "local",
      title: `${cleanTitle}（副本）`,
      slug: "",
      summary: draftMeta.summary.trim() || undefined,
      coverColor: draftMeta.coverColor,
      treeJson: serializedTree,
      nodeCount: stats.nodeCount,
      resourceCount: stats.resourceCount,
      sort: allPaths.value.length + 1,
      status: 1,
    };
    localPaths.value = [copyEntry, ...localPaths.value];
    persistLocalPaths();
    activatePath(copyEntry);
  }
  editMode.value = false;
  statusNotice.value = "已保存为本机自定义导图；如需全站访客可见，请登录「后台管理」后点击「保存并同步全站」";
}

function deleteCurrentLocalPath() {
  if (!currentPath.value || currentPath.value.source !== "local") return;
  localPaths.value = localPaths.value.filter((item) => item.uid !== currentPath.value?.uid);
  persistLocalPaths();
  editMode.value = false;
  activatePath(allPaths.value[0] ?? null);
  statusNotice.value = "已移除该本机导图";
}

function resourceTypeLabel(type: MindMapResourceType): string {
  if (type === "article") return "文章";
  if (type === "video") return "视频";
  return "网页";
}

function getVideoSource(url?: string) {
  return parseVideoEmbed(url);
}

function canEmbedVideo(url?: string): boolean {
  const parsed = parseVideoEmbed(url);
  return Boolean(parsed && parsed.kind !== "external");
}

function toggleInlinePreview(resourceId: string) {
  activePreviewResId.value = activePreviewResId.value === resourceId ? null : resourceId;
}

function resolveArticleLink(res: MindMapResource): string {
  const matched = articleCatalog.value.find((a) => a.id === res.articleId);
  if (matched) {
    return getArticleRoute(matched);
  }
  return getArticleRoute({
    id: res.articleId || "1",
    slug: res.articleSlug,
  });
}

function lookupArticleSummary(res: MindMapResource): string {
  const matched = articleCatalog.value.find((a) => a.id === res.articleId);
  return matched?.summary || "";
}

async function loadLearningPaths(preferredIdOrSlug?: string) {
  loading.value = true;
  try {
    const [pathsRes, articlesRes] = await Promise.all([
      blogApi.getLearningPaths(),
      blogApi.getArticles({ pageNum: 1, pageSize: 100 }),
    ]);
    serverPaths.value = (pathsRes || []).map((item) => ({
      ...item,
      uid: `srv-${item.id}`,
      source: "server" as const,
    }));
    articleCatalog.value = articlesRes?.list ?? [];

    const routeKey = preferredIdOrSlug || (typeof route.params.idOrSlug === "string" ? route.params.idOrSlug : "");
    let matched: PathCollectionEntry | undefined;
    if (routeKey) {
      matched = allPaths.value.find((p) => p.id === routeKey || p.slug === routeKey);
    }
    activatePath(matched ?? allPaths.value[0] ?? null);
  } finally {
    loading.value = false;
  }
}

watch(
  () => route.params.idOrSlug,
  (nextKey) => {
    if (typeof nextKey !== "string" || !nextKey) return;
    const found = allPaths.value.find((item) => item.id === nextKey || item.slug === nextKey);
    if (found && found.uid !== selectedPathUid.value) {
      activatePath(found);
    }
  },
);

onMounted(() => {
  void loadSiteInfo();
  void loadLearningPaths();
});
</script>

<style scoped>
.paths-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.paths-header {
  padding: 24px 28px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.paths-header-top {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.paths-eyebrow {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--accent);
  margin-bottom: 4px;
}

.paths-heading {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.paths-sub {
  margin: 0;
  font-size: 13.5px;
  color: var(--text-secondary);
  max-width: 68ch;
}

.paths-header-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.is-editing-btn {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}

.paths-switcher-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid var(--border-subtle);
}

.paths-tabs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.path-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 13px;
  border-radius: 999px;
  border: none;
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: box-shadow 0.2s ease, background-color 0.2s ease, color 0.2s ease;
}

.path-pill:hover {
  box-shadow: inset 0 0 0 1px var(--text-muted);
  color: var(--text-primary);
}

.path-pill.active {
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent) 45%, transparent);
  background: var(--accent-soft);
  color: var(--accent);
}

.path-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.path-source-badge {
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 10.5px;
  background: rgba(245, 158, 11, 0.16);
  color: #b45309;
}

html.dark .path-source-badge {
  color: #fbbf24;
}

.paths-tools-right {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.progress-badge {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11.5px;
  color: var(--text-muted);
}

.progress-mini-track {
  width: 140px;
  height: 4px;
  border-radius: 999px;
  background: var(--bg-subtle);
  overflow: hidden;
}

.progress-mini-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent), var(--accent-2));
  transition: width 0.25s ease;
}

.path-search-input {
  width: 196px;
  padding: 6px 12px;
  border-radius: var(--radius-control);
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.path-search-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.paths-notice {
  padding: 9px 13px;
  border-radius: var(--radius-control);
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 13px;
}

.empty-paths-card {
  padding: 64px 24px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.empty-paths-title {
  font-size: 18px;
  font-weight: 700;
}

.empty-paths-desc {
  margin: 0;
  font-size: 14px;
  color: var(--text-muted);
}

.paths-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 390px;
  gap: 18px;
  align-items: start;
}

.workspace-canvas-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.meta-edit-bar {
  padding: 14px 16px;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 10px 12px;
}

.meta-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1 1 150px;
}

.meta-field--wide {
  flex: 2 1 220px;
}

.meta-label {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--text-muted);
}

.meta-input,
.meta-select,
.meta-textarea {
  width: 100%;
  padding: 7px 10px;
  border-radius: var(--radius-chip);
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.meta-input:focus,
.meta-select:focus,
.meta-textarea:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.meta-save-group {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.inspector-col {
  padding: 20px;
  background: var(--bg-glass-strong);
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 760px;
  overflow-y: auto;
}

.inspector-breadcrumb {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.crumb-btn {
  border: none;
  background: transparent;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
  cursor: pointer;
}

.crumb-btn:hover,
.crumb-btn.is-current {
  color: var(--accent);
  font-weight: 600;
}

.crumb-sep {
  opacity: 0.45;
}

.inspector-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.inspector-title {
  margin: 0;
  font-size: 19px;
  font-weight: 800;
  line-height: 1.35;
}

.inspector-tag {
  flex-shrink: 0;
  padding: 2px 9px;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 600;
  background: var(--accent-soft);
  color: var(--accent);
}

.inspector-desc {
  margin: 8px 0 12px;
  font-size: 13.5px;
  line-height: 1.65;
  color: var(--text-secondary);
}

.inspector-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.mastery-btn {
  padding: 6px 12px;
  border-radius: 999px;
  border: none;
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: box-shadow 0.2s ease, background-color 0.2s ease, color 0.2s ease;
}

.mastery-btn.is-done {
  background: var(--accent);
  box-shadow: inset 0 0 0 1px var(--accent);
  color: #ffffff;
}

html.dark .mastery-btn.is-done {
  color: #06131a;
}

.res-total-label {
  font-size: 12px;
  color: var(--text-muted);
}

.res-filter-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.res-tab {
  padding: 4px 10px;
  border-radius: 999px;
  border: none;
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  background: transparent;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
}

.res-tab.active {
  background: var(--accent-soft);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent) 40%, transparent);
  color: var(--accent);
  font-weight: 600;
}

.inspector-empty {
  padding: 26px 14px;
  border-radius: var(--radius-control);
  background: var(--bg-subtle);
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}

.resource-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resource-card {
  padding: 13px 14px;
  border-radius: var(--radius-control);
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resource-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.resource-type-pill {
  padding: 1px 7px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 600;
}

.resource-type-pill.is-article {
  background: rgba(13, 148, 136, 0.14);
  color: #0f766e;
}

.resource-type-pill.is-video {
  background: rgba(2, 132, 199, 0.14);
  color: #0369a1;
}

.resource-type-pill.is-webpage {
  background: rgba(99, 102, 241, 0.14);
  color: #4338ca;
}

html.dark .resource-type-pill.is-article {
  color: #5eead4;
}

html.dark .resource-type-pill.is-video {
  color: #7dd3fc;
}

html.dark .resource-type-pill.is-webpage {
  color: #a5b4fc;
}

.resource-host {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: var(--text-muted);
}

.resource-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
  color: var(--text-primary);
}

.resource-note {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.55;
  color: var(--text-secondary);
}

.resource-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}

.res-primary-link,
.res-primary-btn {
  padding: 5px 12px;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, var(--accent), var(--accent-2));
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

html.dark .res-primary-link,
html.dark .res-primary-btn {
  color: #06131a;
}

.res-secondary-link,
.res-secondary-btn {
  padding: 5px 11px;
  border-radius: 999px;
  border: none;
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  background: var(--bg-glass-strong);
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
}

.res-secondary-link:hover,
.res-secondary-btn:hover {
  box-shadow: inset 0 0 0 1px var(--accent);
  color: var(--accent);
}

.embedded-media-wrap {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.embedded-video,
.embedded-iframe {
  width: 100%;
  aspect-ratio: 16 / 9;
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  background: #0f172a;
}

.embedded-iframe--web {
  aspect-ratio: auto;
  height: 280px;
  background: #ffffff;
}

.embed-fallback-tip {
  margin: 0;
  font-size: 11px;
  color: var(--text-muted);
}

.inspector-edit {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.edit-tree-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.mini-action-btn {
  padding: 4px 9px;
  border-radius: var(--radius-chip);
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
}

.mini-action-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.mini-action-btn.is-danger:hover {
  border-color: #ef4444;
  color: #ef4444;
}

.edit-form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.edit-divider {
  height: 1px;
  background: var(--border-subtle);
  margin: 4px 0;
}

.mounter-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.mounter-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
}

.mounter-type-tabs {
  display: flex;
  gap: 5px;
}

.type-tab {
  padding: 3px 9px;
  border-radius: 999px;
  border: none;
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
}

.type-tab.active {
  box-shadow: inset 0 0 0 1px var(--accent);
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 600;
}

.mounter-form {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 12px;
  border-radius: var(--radius-control);
  background: var(--bg-subtle);
  border: 1px solid var(--border-subtle);
}

.mount-submit-btn {
  margin-top: 4px;
  width: 100%;
}

.mounted-edit-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mounted-edit-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 9px;
  border-radius: var(--radius-chip);
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
}

.mounted-edit-title {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mounted-edit-ops {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.mounted-op-btn {
  border: 1px solid var(--border-subtle);
  background: var(--bg-glass-strong);
  color: var(--text-secondary);
  border-radius: 5px;
  padding: 2px 6px;
  font-size: 11px;
  cursor: pointer;
}

.mounted-op-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

.mounted-op-btn.is-danger:hover {
  color: #ef4444;
  border-color: #ef4444;
}

@supports (corner-shape: squircle) {
  .resource-card,
  .mounter-form,
  .mounted-edit-row,
  .path-search-input,
  .meta-input,
  .meta-select,
  .meta-textarea {
    corner-shape: squircle;
  }
}

@media (max-width: 1080px) {
  .paths-workspace {
    grid-template-columns: 1fr;
  }

  .inspector-col {
    max-height: none;
  }
}
</style>
