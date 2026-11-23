<template>
  <section class="paths-page">
    <!-- 紧凑整合式顶部导览栏：路径切换、进度、搜索与创建/编辑入口统一在单卡片内 -->
    <header class="card paths-header animate-rise">
      <div class="header-main-row">
        <div class="header-title-wrap">
          <h1 class="paths-heading">学习路径</h1>
        </div>

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

        <div class="header-right-actions">
          <div class="search-pill-box">
            <svg viewBox="0 0 16 16" class="search-icon" aria-hidden="true">
              <circle cx="7" cy="7" r="4.5" />
              <line x1="10.4" y1="10.4" x2="13.8" y2="13.8" />
            </svg>
            <input
              v-model="nodeSearchQuery"
              type="text"
              class="path-search-input"
              placeholder="搜索节点或资源..."
              aria-label="搜索思维导图节点或资源"
            />
            <button
              v-if="nodeSearchQuery"
              type="button"
              class="search-clear-btn"
              title="清空搜索"
              @click="nodeSearchQuery = ''"
            >
              ×
            </button>
          </div>

          <button type="button" class="btn btn-primary btn-compact" @click="startCreateNewPath">
            + 新建思维导图
          </button>
          <button
            v-if="currentPath"
            type="button"
            class="btn btn-compact"
            :class="{ 'is-editing-btn': editMode }"
            @click="toggleEditMode"
          >
            {{ editMode ? "退出编辑" : "编辑当前导图" }}
          </button>
          <a href="/admin/#/blog/learning-path" class="btn btn-compact">后台管理</a>
        </div>
      </div>

      <!-- 编辑态内置属性编辑条（直接嵌入头部，切换编辑模式时画布零垂直跳动） -->
      <div v-if="currentPath && editMode" class="header-edit-strip">
        <div class="strip-field strip-field--title">
          <label class="strip-label">导图名称</label>
          <input
            v-model="draftMeta.title"
            type="text"
            class="strip-input"
            maxlength="128"
            placeholder="例如：Spring Boot 微服务实战路线"
          />
        </div>

        <div class="strip-field strip-field--colors">
          <label class="strip-label">主色</label>
          <div class="color-swatches">
            <button
              v-for="swatch in COLOR_SWATCHES"
              :key="swatch"
              type="button"
              class="swatch-dot"
              :class="{ active: draftMeta.coverColor === swatch }"
              :style="{ background: swatch }"
              :title="`使用主题色 ${swatch}`"
              @click="draftMeta.coverColor = swatch"
            />
          </div>
        </div>

        <button
          type="button"
          class="strip-more-btn"
          @click="showAdvancedMeta = !showAdvancedMeta"
        >
          {{ showAdvancedMeta ? "收起简介/别名" : "简介与别名" }}
        </button>

        <div class="strip-save-group">
          <button
            v-if="currentPath.source === 'local'"
            type="button"
            class="btn btn-compact btn-danger-ghost"
            @click="deleteCurrentLocalPath"
          >
            删除本机导图
          </button>
          <button
            type="button"
            class="btn btn-primary btn-compact"
            :disabled="saving"
            @click="saveCurrentPath"
          >
            {{ saving ? "保存中..." : hasAdminToken ? "保存并同步全站" : "保存导图（本机）" }}
          </button>
        </div>

        <div v-if="showAdvancedMeta" class="strip-advanced-row">
          <div class="strip-field">
            <label class="strip-label">URL 别名（选填）</label>
            <input
              v-model="draftMeta.slug"
              type="text"
              class="strip-input"
              maxlength="128"
              placeholder="例如：spring-cloud-path"
            />
          </div>
          <div class="strip-field strip-field--summary">
            <label class="strip-label">路径简介</label>
            <input
              v-model="draftMeta.summary"
              type="text"
              class="strip-input"
              maxlength="500"
              placeholder="简短说明该学习路线的目标与适用读者"
            />
          </div>
        </div>
      </div>

      <Transition name="notice-fade">
        <div v-if="statusNotice" class="paths-notice" role="status">
          <span>{{ statusNotice }}</span>
          <button type="button" class="notice-close" @click="statusNotice = ''">×</button>
        </div>
      </Transition>
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

    <!-- 主工作区：左侧全画幅交互导图 + 右侧可折叠详情与资源工作台 -->
    <div
      v-else
      class="paths-workspace"
      :class="{ 'is-inspector-collapsed': inspectorCollapsed }"
    >
      <div class="workspace-canvas-col">
        <MindMapCanvas
          ref="canvasRef"
          :tree="workingTree"
          :selected-node-id="selectedNodeId"
          :completed-ids="completedIdsForCurrent"
          :accent-color="editMode ? draftMeta.coverColor : currentPath.coverColor"
          :search-query="nodeSearchQuery"
          :editable="editMode"
          :inspector-collapsed="inspectorCollapsed"
          @select-node="handleSelectNode"
          @toggle-collapse="handleToggleCollapse"
          @toggle-complete="handleToggleComplete"
          @expand-all="handleExpandAll"
          @collapse-branches="handleCollapseBranches"
          @add-child="handleAddChildNode"
          @add-sibling="handleAddSiblingNode"
          @move-node="handleMoveNode"
          @remove-node="handleRemoveNode"
          @rename-node="handleRenameNode"
          @navigate-node="handleNavigateNode"
          @toggle-inspector="inspectorCollapsed = !inspectorCollapsed"
        />
      </div>

      <aside v-show="!inspectorCollapsed" class="card inspector-col">
        <Transition name="inspector-swap" mode="out-in">
          <div v-if="selectedNode" :key="`${selectedNode.id}-${editMode ? 'edit' : 'view'}`" class="inspector-inner">
            <!-- 顶部面包屑路径 -->
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

            <!-- 阅读态：简洁节点详情 + 挂载资源列表 -->
            <div v-if="!editMode" class="inspector-view">
              <div class="inspector-title-row">
                <h2 class="inspector-title">{{ selectedNode.label }}</h2>
                <span v-if="selectedNode.tag" class="inspector-tag">{{ selectedNode.tag }}</span>
              </div>

              <p v-if="selectedNode.description" class="inspector-desc">
                {{ selectedNode.description }}
              </p>

              <div class="inspector-status-row">
                <button
                  type="button"
                  class="mastery-btn"
                  :class="{ 'is-done': completedSetForCurrent.has(selectedNode.id) }"
                  @click="handleToggleComplete(selectedNode.id)"
                >
                  {{ completedSetForCurrent.has(selectedNode.id) ? "已掌握" : "标记已掌握" }}
                </button>
                <div v-if="activeTreeStats.nodeCount" class="progress-inline">
                  <span class="progress-text">
                    {{ completedCountInCurrent }}/{{ activeTreeStats.nodeCount }}
                  </span>
                  <div class="progress-mini-track" aria-hidden="true">
                    <div class="progress-mini-fill" :style="{ width: `${progressPercent}%` }" />
                  </div>
                </div>
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
                暂无{{ resourceFilter === "all" ? "" : resourceTypeLabel(resourceFilter) }}资源
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

                  <!-- 站内文章 -->
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

                  <!-- 视频资源：支持行内展开与剧场大屏弹窗双模式 -->
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
                      <button
                        v-if="canEmbedVideo(res.url)"
                        type="button"
                        class="res-secondary-btn"
                        @click="openCinemaModal(res)"
                      >
                        大屏播放
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

                  <!-- 网页资料 -->
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
                        若目标网站禁止内嵌（X-Frame-Options），请点击上方「访问网页」在新标签查看。
                      </p>
                    </div>
                  </div>
                </article>
              </div>
            </div>

            <!-- 编辑态：精简节点属性与三类资源一键挂载 -->
            <div v-else class="inspector-edit">
              <div class="edit-node-card">
                <div class="edit-row-two">
                  <div class="edit-form-group edit-form-group--grow">
                    <label class="meta-label">节点名称</label>
                    <input
                      :value="selectedNode.label"
                      type="text"
                      maxlength="100"
                      class="meta-input"
                      placeholder="双击画布节点也可直接改名"
                      @input="updateSelectedField('label', ($event.target as HTMLInputElement).value)"
                    />
                  </div>
                  <div class="edit-form-group edit-form-group--tag">
                    <label class="meta-label">标签</label>
                    <input
                      :value="selectedNode.tag || ''"
                      type="text"
                      maxlength="24"
                      class="meta-input"
                      placeholder="核心"
                      @input="updateSelectedField('tag', ($event.target as HTMLInputElement).value)"
                    />
                  </div>
                </div>

                <div class="edit-form-group">
                  <label class="meta-label">学习说明（选填）</label>
                  <textarea
                    :value="selectedNode.description || ''"
                    rows="2"
                    maxlength="500"
                    class="meta-textarea"
                    placeholder="补充本节点学习重点或目标"
                    @input="updateSelectedField('description', ($event.target as HTMLTextAreaElement).value)"
                  />
                </div>
              </div>

              <div class="mounter-block">
                <div class="mounter-header">
                  <h3 class="mounter-title">挂载学习资源</h3>
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
                    <select
                      v-model="newResForm.articleId"
                      class="meta-select"
                      aria-label="选择站内文章"
                      @change="onSelectArticleOption"
                    >
                      <option value="">-- 选择要关联的站内文章 --</option>
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
                    <input
                      v-model="newResForm.url"
                      type="text"
                      class="meta-input"
                      :placeholder="
                        newResForm.type === 'video'
                          ? '粘贴 Bilibili / YouTube 链接或 MP4 直链'
                          : '粘贴 https:// 参考网页链接'
                      "
                    />
                  </template>

                  <input
                    v-model="newResForm.title"
                    type="text"
                    maxlength="160"
                    class="meta-input"
                    :placeholder="
                      newResForm.type === 'article'
                        ? '展示标题（选中文章后自动填充）'
                        : '展示标题（留空将自动提取站点/平台名）'
                    "
                  />

                  <div class="mounter-submit-row">
                    <input
                      v-model="newResForm.note"
                      type="text"
                      maxlength="300"
                      class="meta-input"
                      placeholder="备注提示（选填）"
                    />
                    <button type="submit" class="btn btn-primary btn-compact mount-submit-btn">
                      挂载到当前节点
                    </button>
                  </div>
                </form>
              </div>

              <div v-if="selectedNode.resources.length" class="mounted-edit-list">
                <div class="mounted-list-label">已挂载（{{ selectedNode.resources.length }}）</div>
                <div
                  v-for="(res, rIdx) in selectedNode.resources"
                  :key="res.id"
                  class="mounted-edit-row"
                >
                  <span class="resource-type-pill" :class="`is-${res.type}`">
                    {{ resourceTypeLabel(res.type) }}
                  </span>
                  <span class="mounted-edit-title" :title="res.title">{{ res.title }}</span>
                  <div class="mounted-edit-ops">
                    <button
                      type="button"
                      class="mounted-op-btn"
                      :disabled="rIdx === 0"
                      title="上移"
                      @click="moveResourceOrder(rIdx, -1)"
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      class="mounted-op-btn"
                      :disabled="rIdx === selectedNode.resources.length - 1"
                      title="下移"
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
          </div>
        </Transition>
      </aside>
    </div>

    <!-- 剧场大屏视频播放弹窗 -->
    <Teleport to="body">
      <Transition name="cinema-fade">
        <div
          v-if="cinemaResource"
          class="cinema-backdrop"
          role="dialog"
          aria-modal="true"
          :aria-label="cinemaResource.title"
          @click.self="closeCinemaModal"
        >
          <div ref="cinemaDialogRef" class="cinema-dialog card" tabindex="-1">
            <div class="cinema-head">
              <div class="cinema-title-wrap">
                <span class="resource-type-pill is-video">视频播放</span>
                <h3 class="cinema-title">{{ cinemaResource.title }}</h3>
              </div>
              <div class="cinema-actions">
                <a
                  v-if="normalizeSafeExternalUrl(cinemaResource.url)"
                  :href="normalizeSafeExternalUrl(cinemaResource.url)!"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="res-secondary-link"
                >
                  打开原页面
                </a>
                <button type="button" class="cinema-close-btn" @click="closeCinemaModal">
                  关闭
                </button>
              </div>
            </div>
            <div class="cinema-player-stage">
              <video
                v-if="getVideoSource(cinemaResource.url)?.kind === 'native'"
                :src="getVideoSource(cinemaResource.url)?.embedUrl"
                controls
                autoplay
                playsinline
                class="cinema-frame"
              />
              <iframe
                v-else-if="getVideoSource(cinemaResource.url)?.kind === 'iframe'"
                :src="getVideoSource(cinemaResource.url)?.embedUrl"
                title="剧场模式视频播放器"
                allowfullscreen
                referrerpolicy="no-referrer"
                class="cinema-frame"
              />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
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
  findAdjacentSiblingId,
  findNextSiblingOrParent,
  findNodeById,
  findNodeChain,
  findParentNode,
  getAdminSessionToken,
  moveNodeOrder,
  normalizeSafeExternalUrl,
  parseMindMapTree,
  parseVideoEmbed,
  removeNodeById,
  setAllCollapsed,
  suggestResourceTitle,
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
const COLOR_SWATCHES = ["#0d9488", "#0284c7", "#4f46e5", "#d97706", "#db2777", "#059669"];

const route = useRoute();
const router = useRouter();
const { loadSiteInfo } = useSiteInfo();

const canvasRef = ref<InstanceType<typeof MindMapCanvas> | null>(null);
const loading = ref(false);
const saving = ref(false);
const editMode = ref(false);
const showAdvancedMeta = ref(false);
const inspectorCollapsed = ref(false);
const statusNotice = ref("");
const nodeSearchQuery = ref("");
const resourceFilter = ref<"all" | MindMapResourceType>("all");
const activePreviewResId = ref<string | null>(null);
const cinemaResource = ref<MindMapResource | null>(null);
const cinemaDialogRef = ref<HTMLElement | null>(null);

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
  showAdvancedMeta.value = false;
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
  void nextTick(() => canvasRef.value?.fitToView());
}

function handleCollapseBranches() {
  if (!workingTree.value) return;
  workingTree.value = setAllCollapsed(workingTree.value, true);
  void nextTick(() => canvasRef.value?.fitToView());
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
  inspectorCollapsed.value = false;
  showAdvancedMeta.value = false;
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
  showAdvancedMeta.value = false;
  if (editMode.value) {
    editMode.value = false;
    if (currentPath.value) {
      activatePath(currentPath.value);
    }
    return;
  }
  inspectorCollapsed.value = false;
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
  void nextTick(() => {
    void canvasRef.value?.startInlineEdit(child.id, child.label);
  });
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
  void nextTick(() => {
    void canvasRef.value?.startInlineEdit(sibling.id, sibling.label);
  });
}

function handleMoveNode(targetId: string, delta: -1 | 1) {
  if (!workingTree.value) return;
  workingTree.value = moveNodeOrder(workingTree.value, targetId, delta);
}

function handleRemoveNode(targetId: string) {
  if (!workingTree.value || targetId === workingTree.value.id) return;
  const fallbackFocusId = findNextSiblingOrParent(workingTree.value, targetId);
  workingTree.value = removeNodeById(workingTree.value, targetId);
  selectedNodeId.value = fallbackFocusId;
}

function handleRenameNode(targetId: string, label: string) {
  if (!workingTree.value) return;
  workingTree.value = updateNodeById(workingTree.value, targetId, (node) => ({
    ...node,
    label,
  }));
}

function handleNavigateNode(direction: "left" | "right" | "up" | "down") {
  if (!workingTree.value || !selectedNode.value) return;
  const current = selectedNode.value;

  if (direction === "left") {
    const parent = findParentNode(workingTree.value, current.id);
    if (parent) handleSelectNode(parent.id);
    return;
  }

  if (direction === "right") {
    if (!current.children.length) return;
    if (current.collapsed) {
      workingTree.value = toggleNodeCollapse(workingTree.value, current.id);
    }
    handleSelectNode(current.children[0].id);
    return;
  }

  const nextId = findAdjacentSiblingId(
    workingTree.value,
    current.id,
    direction === "up" ? -1 : 1,
  );
  if (nextId) handleSelectNode(nextId);
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

  let resource: MindMapResource;
  if (newResForm.type === "article") {
    if (!newResForm.articleId) {
      statusNotice.value = "请选择要挂载的站内文章";
      return;
    }
    const matched = articleCatalog.value.find((item) => item.id === newResForm.articleId);
    const title = newResForm.title.trim() || matched?.title || "站内文章";
    resource = {
      id: createUid("res"),
      type: "article",
      title,
      articleId: newResForm.articleId,
      articleSlug: newResForm.articleSlug || matched?.slug || undefined,
      note: newResForm.note.trim() || undefined,
    };
  } else {
    const safeUrl = normalizeSafeExternalUrl(newResForm.url);
    if (!safeUrl) {
      statusNotice.value = "请输入合法的 http:// 或 https:// 链接";
      return;
    }
    const title = newResForm.title.trim() || suggestResourceTitle(newResForm.type, safeUrl);
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
      showAdvancedMeta.value = false;
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
  showAdvancedMeta.value = false;
  statusNotice.value = "已保存为本机自定义导图；如需全站访客可见，请登录「后台管理」后点击「保存并同步全站」";
}

function deleteCurrentLocalPath() {
  if (!currentPath.value || currentPath.value.source !== "local") return;
  localPaths.value = localPaths.value.filter((item) => item.uid !== currentPath.value?.uid);
  persistLocalPaths();
  editMode.value = false;
  showAdvancedMeta.value = false;
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

function openCinemaModal(res: MindMapResource) {
  activePreviewResId.value = null;
  cinemaResource.value = res;
  void nextTick(() => {
    cinemaDialogRef.value?.focus();
  });
}

function closeCinemaModal() {
  cinemaResource.value = null;
}

function handleGlobalEsc(e: KeyboardEvent) {
  if (e.key === "Escape" && cinemaResource.value) {
    closeCinemaModal();
  }
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

    const routeKey =
      preferredIdOrSlug || (typeof route.params.idOrSlug === "string" ? route.params.idOrSlug : "");
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
  window.addEventListener("keydown", handleGlobalEsc);
  void loadSiteInfo();
  void loadLearningPaths();
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", handleGlobalEsc);
});
</script>

<style scoped>
.paths-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.paths-header {
  padding: 14px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.header-main-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 14px;
}

.header-title-wrap {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-shrink: 0;
}

.paths-eyebrow {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--accent);
}

.paths-heading {
  margin: 0;
  font-size: 19px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.paths-tabs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  flex: 1 1 auto;
  min-width: 180px;
}

.path-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  border: none;
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 12.5px;
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
  font-weight: 600;
}

.path-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.path-source-badge {
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 10px;
  background: rgba(245, 158, 11, 0.16);
  color: #b45309;
}

html.dark .path-source-badge {
  color: #fbbf24;
}

.header-right-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.btn-compact {
  padding: 6px 13px;
  font-size: 12.5px;
}

.btn-danger-ghost {
  color: #ef4444;
}

.is-editing-btn {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}

.search-pill-box {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  width: 13px;
  height: 13px;
  fill: none;
  stroke: var(--text-muted);
  stroke-width: 1.8;
  pointer-events: none;
}

.path-search-input {
  width: 174px;
  padding: 6px 26px 6px 29px;
  border-radius: 999px;
  border: none;
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-primary);
  font-size: 12.5px;
  outline: none;
  transition: box-shadow 0.2s ease, width 0.22s ease;
}

.path-search-input:focus {
  width: 214px;
  box-shadow: inset 0 0 0 1px var(--accent), 0 0 0 3px var(--accent-soft);
}

.search-clear-btn {
  position: absolute;
  right: 7px;
  width: 16px;
  height: 16px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.header-sub-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 16px;
  padding-top: 8px;
  border-top: 1px solid var(--border-subtle);
}

.current-summary {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-muted);
}

.progress-inline {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.progress-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11.5px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.progress-mini-track {
  width: 108px;
  height: 5px;
  border-radius: 999px;
  background: var(--bg-subtle);
  overflow: hidden;
}

.progress-mini-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent), var(--accent-2));
  transition: width 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}

.header-edit-strip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 14px;
  padding-top: 10px;
  border-top: 1px solid var(--border-subtle);
}

.strip-field {
  display: flex;
  align-items: center;
  gap: 7px;
}

.strip-field--title {
  flex: 1 1 240px;
}

.strip-field--summary {
  flex: 2 1 280px;
}

.strip-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  white-space: nowrap;
}

.strip-input {
  flex: 1;
  min-width: 130px;
  padding: 5px 10px;
  border-radius: var(--radius-chip);
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-primary);
  font-size: 12.5px;
  outline: none;
}

.strip-input:focus {
  border-color: var(--accent);
}

.color-swatches {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.swatch-dot {
  width: 17px;
  height: 17px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.swatch-dot.active {
  transform: scale(1.16);
  box-shadow: 0 0 0 2px var(--bg-glass-strong), 0 0 0 4px var(--text-primary);
}

.strip-more-btn {
  border: none;
  background: transparent;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  color: var(--text-muted);
  cursor: pointer;
}

.strip-more-btn:hover {
  background: var(--bg-subtle);
  color: var(--accent);
}

.strip-save-group {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.strip-advanced-row {
  flex-basis: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  padding-top: 2px;
}

.paths-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 12px;
  border-radius: var(--radius-chip);
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 12.5px;
}

.notice-close {
  border: none;
  background: transparent;
  color: inherit;
  font-size: 15px;
  cursor: pointer;
  line-height: 1;
}

.notice-fade-enter-active,
.notice-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.notice-fade-enter-from,
.notice-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
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
  grid-template-columns: minmax(0, 1fr) 396px;
  gap: 16px;
  align-items: start;
}

.paths-workspace.is-inspector-collapsed {
  grid-template-columns: minmax(0, 1fr);
}

.workspace-canvas-col {
  min-width: 0;
}

.inspector-col {
  position: sticky;
  top: 84px;
  padding: 18px 20px;
  background: var(--bg-glass-strong);
  max-height: calc(100vh - 108px);
  overflow-y: auto;
}

.inspector-inner {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.inspector-swap-enter-active,
.inspector-swap-leave-active {
  transition: opacity 0.16s ease, transform 0.16s cubic-bezier(0.22, 1, 0.36, 1);
}

.inspector-swap-enter-from {
  opacity: 0;
  transform: translateY(5px);
}

.inspector-swap-leave-to {
  opacity: 0;
  transform: translateY(-4px);
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
  font-size: 11.5px;
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
  font-size: 18px;
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
  margin: 6px 0 12px;
  font-size: 13px;
  line-height: 1.65;
  color: var(--text-secondary);
}

.inspector-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.mastery-btn {
  padding: 5px 12px;
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
  padding: 24px 14px;
  border-radius: var(--radius-control);
  background: var(--bg-subtle);
  text-align: center;
  font-size: 12.5px;
  color: var(--text-muted);
}

.resource-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.resource-card {
  padding: 12px 13px;
  border-radius: var(--radius-control);
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  display: flex;
  flex-direction: column;
  gap: 7px;
  transition: border-color 0.18s ease, transform 0.18s ease;
}

.resource-card:hover {
  border-color: color-mix(in srgb, var(--accent) 32%, var(--border-subtle));
  transform: translateY(-1px);
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
  font-size: 13.5px;
  font-weight: 700;
  line-height: 1.45;
  color: var(--text-primary);
}

.resource-note {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-secondary);
}

.resource-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
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
  margin-top: 8px;
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
  height: 260px;
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
  gap: 12px;
}

.edit-node-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.edit-row-two {
  display: flex;
  gap: 8px;
}

.edit-form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.edit-form-group--grow {
  flex: 1;
  min-width: 0;
}

.edit-form-group--tag {
  width: 96px;
  flex-shrink: 0;
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
  font-size: 12.5px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.meta-input:focus,
.meta-select:focus,
.meta-textarea:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.mounter-block {
  border-top: 1px solid var(--border-subtle);
  padding-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
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
  font-size: 13.5px;
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
  font-size: 11.5px;
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
  padding: 10px;
  border-radius: var(--radius-control);
  background: var(--bg-subtle);
  border: 1px solid var(--border-subtle);
}

.mounter-submit-row {
  display: flex;
  gap: 7px;
  align-items: center;
}

.mount-submit-btn {
  flex-shrink: 0;
}

.mounted-edit-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mounted-list-label {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--text-muted);
}

.mounted-edit-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 9px;
  border-radius: var(--radius-chip);
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
}

.mounted-edit-title {
  flex: 1;
  min-width: 0;
  font-size: 12px;
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

/* 剧场大屏视频播放弹窗 */
.cinema-backdrop {
  position: fixed;
  inset: 0;
  z-index: 120;
  background: rgba(15, 23, 42, 0.72);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.cinema-dialog {
  width: min(920px, 100%);
  padding: 16px 18px 18px;
  background: var(--bg-glass-strong);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cinema-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.cinema-title-wrap {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.cinema-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.cinema-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cinema-close-btn {
  padding: 5px 12px;
  border-radius: 999px;
  border: none;
  box-shadow: inset 0 0 0 1px var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.cinema-player-stage {
  border-radius: 12px;
  overflow: hidden;
  background: #0f172a;
}

.cinema-frame {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 9;
  border: none;
}

.cinema-fade-enter-active,
.cinema-fade-leave-active {
  transition: opacity 0.22s ease;
}

.cinema-fade-enter-from,
.cinema-fade-leave-to {
  opacity: 0;
}

@supports (corner-shape: squircle) {
  .resource-card,
  .mounter-form,
  .mounted-edit-row,
  .meta-input,
  .meta-select,
  .meta-textarea,
  .strip-input {
    corner-shape: squircle;
  }
}

@media (max-width: 1080px) {
  .paths-workspace {
    grid-template-columns: 1fr;
  }

  .inspector-col {
    position: static;
    max-height: none;
  }

  .header-right-actions {
    margin-left: 0;
  }
}
</style>
