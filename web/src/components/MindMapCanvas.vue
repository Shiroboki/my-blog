<template>
  <div class="mindmap-frame card" :class="{ 'is-editing-canvas': editable }">
    <div
      ref="viewportRef"
      class="mindmap-viewport"
      :class="{ 'is-panning': panning, 'is-fullscreen-canvas': inspectorCollapsed }"
      tabindex="0"
      aria-label="学习路径交互式思维导图画布"
      @pointerdown="startPan"
      @pointermove="movePan"
      @pointerup="endPan"
      @pointercancel="endPan"
      @dblclick="handleViewportDblClick"
      @wheel="handleWheel"
      @keydown="handleKeydown"
    >
      <div class="mindmap-dot-grid" aria-hidden="true" />

      <div
        class="mindmap-stage"
        :style="{
          width: `${layout.width}px`,
          height: `${layout.height}px`,
          transform: `translate3d(${panX}px, ${panY}px, 0) scale(${scale})`,
        }"
      >
        <svg
          class="mindmap-edges"
          :width="layout.width"
          :height="layout.height"
          :viewBox="`0 0 ${layout.width} ${layout.height}`"
          aria-hidden="true"
        >
          <path
            v-for="edge in layout.edges"
            :key="edge.id"
            :d="edge.path"
            class="edge-curve"
            :class="{
              'is-active': activeChainIds.has(edge.fromId) && activeChainIds.has(edge.toId),
              'is-dim': hasSearch && !matchedIds.has(edge.toId) && !matchedIds.has(edge.fromId),
            }"
            :style="{
              '--branch-color': getBranchColor(edge.branchIndex, accentColor),
            }"
          />
          <path
            v-for="edge in activeEdges"
            :key="`${edge.id}-flow`"
            :d="edge.path"
            class="edge-flow"
            :style="{
              '--branch-color': getBranchColor(edge.branchIndex, accentColor),
            }"
          />
        </svg>

        <div
          v-for="item in layout.nodes"
          :key="item.id"
          class="map-node"
          :class="{
            'is-root': item.depth === 0,
            'is-branch': item.depth === 1,
            'is-leaf': item.depth >= 2,
            'is-selected': selectedNodeId === item.id,
            'is-completed': completedSet.has(item.id),
            'is-search-hit': hasSearch && matchedIds.has(item.id),
            'is-search-dim': hasSearch && !matchedIds.has(item.id),
            'is-inline-editing': editingNodeId === item.id,
          }"
          :style="{
            left: `${item.x}px`,
            top: `${item.y}px`,
            width: `${item.width}px`,
            minHeight: `${item.height}px`,
            '--branch-color': getBranchColor(item.branchIndex, accentColor),
          }"
          role="button"
          tabindex="0"
          :aria-pressed="selectedNodeId === item.id"
          @click.stop="handleNodeClick(item.id)"
          @dblclick.stop="handleNodeDblClick(item.id, item.node.label)"
        >
          <!-- 节点顶部：选中/编辑操作浮条 -->
          <div
            v-if="editable && selectedNodeId === item.id && editingNodeId !== item.id"
            class="node-float-bar"
            @click.stop
            @pointerdown.stop
          >
            <button
              type="button"
              class="float-op-btn"
              title="新增子节点 (Tab)"
              @click="$emit('addChild', item.id)"
            >
              <svg viewBox="0 0 16 16" class="float-op-icon" aria-hidden="true">
                <line x1="8" y1="3" x2="8" y2="13" />
                <line x1="3" y1="8" x2="13" y2="8" />
              </svg>
              <span>子节点</span>
            </button>
            <button
              v-if="item.depth > 0"
              type="button"
              class="float-op-btn"
              title="新增同级节点 (Enter)"
              @click="$emit('addSibling', item.id)"
            >
              <svg viewBox="0 0 16 16" class="float-op-icon" aria-hidden="true">
                <path d="M3 4v4h6" />
                <line x1="11" y1="5" x2="11" y2="11" />
                <line x1="8" y1="8" x2="14" y2="8" />
              </svg>
              <span>同级</span>
            </button>
            <button
              type="button"
              class="float-op-btn"
              title="重命名 (F2 / 双击)"
              @click="startInlineEdit(item.id, item.node.label)"
            >
              <svg viewBox="0 0 16 16" class="float-op-icon" aria-hidden="true">
                <path d="M11.2 2.8a1.5 1.5 0 0 1 2.1 2.1L5.8 12.4 3 13l.6-2.8z" />
              </svg>
            </button>
            <button
              v-if="item.depth > 0"
              type="button"
              class="float-op-btn"
              title="上移节点"
              @click="$emit('moveNode', item.id, -1)"
            >
              <svg viewBox="0 0 16 16" class="float-op-icon" aria-hidden="true">
                <polyline points="4 10 8 6 12 10" />
              </svg>
            </button>
            <button
              v-if="item.depth > 0"
              type="button"
              class="float-op-btn"
              title="下移节点"
              @click="$emit('moveNode', item.id, 1)"
            >
              <svg viewBox="0 0 16 16" class="float-op-icon" aria-hidden="true">
                <polyline points="4 6 8 10 12 6" />
              </svg>
            </button>
            <button
              v-if="item.depth > 0"
              type="button"
              class="float-op-btn is-danger"
              title="删除节点 (Del)"
              @click="$emit('removeNode', item.id)"
            >
              <svg viewBox="0 0 16 16" class="float-op-icon" aria-hidden="true">
                <line x1="4" y1="4" x2="12" y2="12" />
                <line x1="12" y1="4" x2="4" y2="12" />
              </svg>
            </button>
          </div>

          <div class="node-head">
            <button
              type="button"
              class="node-check-btn"
              :class="{ 'is-done': completedSet.has(item.id) }"
              :title="completedSet.has(item.id) ? '取消已掌握标记' : '标记节点为已掌握'"
              :aria-label="completedSet.has(item.id) ? '取消已掌握标记' : '标记节点为已掌握'"
              @click.stop="$emit('toggleComplete', item.id)"
            >
              <svg viewBox="0 0 16 16" class="check-svg" aria-hidden="true">
                <polyline points="3.5 8.5 6.5 11.5 12.5 5" />
              </svg>
            </button>

            <input
              v-if="editingNodeId === item.id"
              ref="inlineInputRef"
              v-model="inlineDraft"
              type="text"
              maxlength="100"
              class="node-inline-input"
              @click.stop
              @pointerdown.stop
              @keydown.stop="handleInlineKeydown($event, item.id)"
              @blur="commitInlineEdit(item.id)"
            />
            <span v-else class="node-title">{{ item.node.label }}</span>

            <span v-if="item.node.tag && editingNodeId !== item.id" class="node-tag">
              {{ item.node.tag }}
            </span>
          </div>

          <div
            v-if="
              item.resourceCounts.article ||
              item.resourceCounts.video ||
              item.resourceCounts.webpage
            "
            class="node-res-bar"
          >
            <span
              v-if="item.resourceCounts.article"
              class="res-badge res-badge--article"
              :title="`已挂载 ${item.resourceCounts.article} 篇文章`"
            >
              <svg viewBox="0 0 16 16" class="res-mini-icon" aria-hidden="true">
                <rect x="3" y="2" width="10" height="12" rx="1.5" />
                <line x1="5.5" y1="6" x2="10.5" y2="6" />
                <line x1="5.5" y1="9.2" x2="9" y2="9.2" />
              </svg>
              <span>{{ item.resourceCounts.article }}</span>
            </span>
            <span
              v-if="item.resourceCounts.video"
              class="res-badge res-badge--video"
              :title="`已挂载 ${item.resourceCounts.video} 个视频`"
            >
              <svg viewBox="0 0 16 16" class="res-mini-icon" aria-hidden="true">
                <polygon points="5,3.5 13,8 5,12.5" />
              </svg>
              <span>{{ item.resourceCounts.video }}</span>
            </span>
            <span
              v-if="item.resourceCounts.webpage"
              class="res-badge res-badge--webpage"
              :title="`已挂载 ${item.resourceCounts.webpage} 个网页`"
            >
              <svg viewBox="0 0 16 16" class="res-mini-icon" aria-hidden="true">
                <circle cx="8" cy="8" r="5.5" />
                <line x1="2.8" y1="8" x2="13.2" y2="8" />
                <ellipse cx="8" cy="8" rx="2.5" ry="5.5" />
              </svg>
              <span>{{ item.resourceCounts.webpage }}</span>
            </span>
          </div>

          <!-- 右侧外缘：收起/展开 或 编辑模式快捷添加子节点 -->
          <button
            v-if="item.node.children.length"
            type="button"
            class="node-collapse-pill"
            :class="{ 'is-collapsed': item.node.collapsed }"
            :title="item.node.collapsed ? '展开子节点 (Space)' : '收起子节点 (Space)'"
            @click.stop="$emit('toggleCollapse', item.id)"
          >
            {{ item.node.collapsed ? `+${item.hiddenChildCount}` : "-" }}
          </button>

          <button
            v-if="editable && editingNodeId !== item.id"
            type="button"
            class="node-edge-add"
            :class="{ 'has-collapse': item.node.children.length > 0 }"
            title="快速添加子节点 (Tab)"
            @click.stop="$emit('addChild', item.id)"
          >
            +
          </button>
        </div>
      </div>

      <!-- 左下角操作提示浮标 -->
      <div class="canvas-hint-hud">
        <template v-if="editable">
          <span>双击节点改名 · Tab 加子节点 · Enter 加同级 · Del 删除</span>
        </template>
        <template v-else>
          <span>拖拽平移 · 方向键切换节点 · 滚轮缩放/Ctrl+滚轮</span>
        </template>
      </div>

      <!-- 右下角悬浮玻璃控制栏（XMind / Figma 风格） -->
      <div class="canvas-control-hud" @pointerdown.stop>
        <button
          type="button"
          class="hud-btn"
          title="展开全部节点"
          @click="$emit('expandAll')"
        >
          全部展开
        </button>
        <button
          type="button"
          class="hud-btn"
          title="折叠二级分支"
          @click="$emit('collapseBranches')"
        >
          收起分支
        </button>
        <span class="hud-sep" aria-hidden="true" />
        <button type="button" class="hud-icon-btn" title="缩小画布" @click="zoomBy(-0.14)">
          <svg viewBox="0 0 16 16" class="hud-svg" aria-hidden="true">
            <line x1="3.5" y1="8" x2="12.5" y2="8" />
          </svg>
        </button>
        <button
          type="button"
          class="hud-zoom-readout"
          title="点击重置自适应居中"
          @click="fitToView()"
        >
          {{ Math.round(scale * 100) }}%
        </button>
        <button type="button" class="hud-icon-btn" title="放大画布" @click="zoomBy(0.14)">
          <svg viewBox="0 0 16 16" class="hud-svg" aria-hidden="true">
            <line x1="8" y1="3.5" x2="8" y2="12.5" />
            <line x1="3.5" y1="8" x2="12.5" y2="8" />
          </svg>
        </button>
        <button type="button" class="hud-btn" title="自适应画布居中" @click="fitToView()">
          居中
        </button>
        <span class="hud-sep" aria-hidden="true" />
        <button
          type="button"
          class="hud-btn"
          :class="{ 'is-active-hud': inspectorCollapsed }"
          :title="inspectorCollapsed ? '显示右侧节点详情' : '收起右侧详情以专注导图'"
          @click="$emit('toggleInspector')"
        >
          {{ inspectorCollapsed ? "显示详情" : "全览画布" }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import {
  collectMatchingNodeIds,
  computeMindMapLayout,
  findAncestorIds,
  getBranchColor,
  type MindMapNode,
} from "@/utils/mindMap";

const props = withDefaults(
  defineProps<{
    tree: MindMapNode;
    selectedNodeId: string;
    completedIds: string[];
    accentColor?: string;
    searchQuery?: string;
    editable?: boolean;
    inspectorCollapsed?: boolean;
  }>(),
  {
    accentColor: "#0d9488",
    searchQuery: "",
    editable: false,
    inspectorCollapsed: false,
  },
);

const emit = defineEmits<{
  selectNode: [id: string];
  toggleCollapse: [id: string];
  toggleComplete: [id: string];
  expandAll: [];
  collapseBranches: [];
  addChild: [id: string];
  addSibling: [id: string];
  moveNode: [id: string, delta: -1 | 1];
  removeNode: [id: string];
  renameNode: [id: string, label: string];
  navigateNode: [direction: "left" | "right" | "up" | "down"];
  toggleInspector: [];
}>();

const viewportRef = ref<HTMLElement | null>(null);
const inlineInputRef = ref<HTMLInputElement[] | HTMLInputElement | null>(null);

const scale = ref(1);
const panX = ref(18);
const panY = ref(18);
const panning = ref(false);
const editingNodeId = ref<string | null>(null);
const inlineDraft = ref("");

let activePointerId: number | null = null;
let dragStartClientX = 0;
let dragStartClientY = 0;
let dragOriginPanX = 0;
let dragOriginPanY = 0;

const layout = computed(() => computeMindMapLayout(props.tree));
const activeChainIds = computed(() => findAncestorIds(props.tree, props.selectedNodeId));
const activeEdges = computed(() =>
  layout.value.edges.filter(
    (edge) => activeChainIds.value.has(edge.fromId) && activeChainIds.value.has(edge.toId),
  ),
);
const completedSet = computed(() => new Set(props.completedIds));
const hasSearch = computed(() => Boolean(props.searchQuery.trim()));
const matchedIds = computed(() => collectMatchingNodeIds(props.tree, props.searchQuery));

function clampScale(next: number): number {
  return Math.min(1.65, Math.max(0.4, next));
}

function zoomAtPoint(nextScale: number, anchorX: number, anchorY: number) {
  const clamped = clampScale(nextScale);
  const oldScale = scale.value;
  if (clamped === oldScale) return;
  const ratio = clamped / oldScale;
  panX.value = anchorX - (anchorX - panX.value) * ratio;
  panY.value = anchorY - (anchorY - panY.value) * ratio;
  scale.value = clamped;
}

function zoomBy(delta: number) {
  const el = viewportRef.value;
  if (!el) {
    scale.value = clampScale(scale.value + delta);
    return;
  }
  zoomAtPoint(scale.value + delta, el.clientWidth / 2, el.clientHeight / 2);
}

function fitToView() {
  const el = viewportRef.value;
  if (!el) return;
  const vw = Math.max(320, el.clientWidth);
  const vh = Math.max(320, el.clientHeight);
  const lw = layout.value.width;
  const lh = layout.value.height;
  const paddingX = 48;
  const paddingY = 88;
  const nextScale = clampScale(
    Math.min(1.06, (vw - paddingX) / lw, (vh - paddingY) / lh),
  );
  scale.value = nextScale;
  panX.value = Math.max(16, (vw - lw * nextScale) / 2);
  panY.value = Math.max(28, (vh - lh * nextScale) / 2 - 6);
}

function panNodeIntoView(nodeId: string) {
  const el = viewportRef.value;
  const target = layout.value.nodes.find((n) => n.id === nodeId);
  if (!el || !target) return;
  const s = scale.value;
  const left = panX.value + target.x * s;
  const right = panX.value + (target.x + target.width) * s;
  const top = panY.value + target.y * s;
  const bottom = panY.value + (target.y + target.height) * s;
  const pad = 42;
  const vw = el.clientWidth;
  const vh = el.clientHeight;

  if (left < pad) panX.value += pad - left;
  else if (right > vw - pad) panX.value -= right - (vw - pad);

  if (top < pad) panY.value += pad - top;
  else if (bottom > vh - pad - 44) panY.value -= bottom - (vh - pad - 44);
}

function startPan(e: PointerEvent) {
  if (e.button !== 0) return;
  const target = e.target as HTMLElement | null;
  if (target?.closest(".map-node, button, input, .canvas-control-hud")) return;
  viewportRef.value?.focus({ preventScroll: true });
  activePointerId = e.pointerId;
  viewportRef.value?.setPointerCapture?.(e.pointerId);
  panning.value = true;
  dragStartClientX = e.clientX;
  dragStartClientY = e.clientY;
  dragOriginPanX = panX.value;
  dragOriginPanY = panY.value;
}

function movePan(e: PointerEvent) {
  if (!panning.value || e.pointerId !== activePointerId) return;
  panX.value = dragOriginPanX + (e.clientX - dragStartClientX);
  panY.value = dragOriginPanY + (e.clientY - dragStartClientY);
}

function endPan(e: PointerEvent) {
  if (!panning.value || e.pointerId !== activePointerId) return;
  if (viewportRef.value?.hasPointerCapture?.(e.pointerId)) {
    viewportRef.value.releasePointerCapture(e.pointerId);
  }
  activePointerId = null;
  panning.value = false;
}

function handleViewportDblClick(e: MouseEvent) {
  const target = e.target as HTMLElement | null;
  if (target?.closest(".map-node, button, input")) return;
  fitToView();
}

function handleWheel(e: WheelEvent) {
  if (e.ctrlKey || e.metaKey) {
    e.preventDefault();
    const el = viewportRef.value;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const factor = e.deltaY < 0 ? 1.09 : 0.91;
    zoomAtPoint(scale.value * factor, e.clientX - rect.left, e.clientY - rect.top);
    return;
  }
  // 支持触控板自由平移画布
  if (Math.abs(e.deltaX) > 2 || Math.abs(e.deltaY) > 2) {
    e.preventDefault();
    panX.value -= e.deltaX * 0.75;
    panY.value -= e.deltaY * 0.75;
  }
}

function handleNodeClick(nodeId: string) {
  emit("selectNode", nodeId);
  viewportRef.value?.focus({ preventScroll: true });
}

function handleNodeDblClick(nodeId: string, currentLabel: string) {
  if (!props.editable) {
    emit("toggleCollapse", nodeId);
    return;
  }
  void startInlineEdit(nodeId, currentLabel);
}

async function startInlineEdit(nodeId: string, currentLabel?: string) {
  const found = layout.value.nodes.find((n) => n.id === nodeId);
  editingNodeId.value = nodeId;
  inlineDraft.value = currentLabel ?? found?.node.label ?? "";
  await nextTick();
  const inputEl = Array.isArray(inlineInputRef.value)
    ? inlineInputRef.value[0]
    : inlineInputRef.value;
  if (inputEl) {
    inputEl.focus();
    inputEl.select();
  }
}

function cancelInlineEdit() {
  editingNodeId.value = null;
  inlineDraft.value = "";
  viewportRef.value?.focus({ preventScroll: true });
}

function commitInlineEdit(nodeId: string) {
  if (editingNodeId.value !== nodeId) return;
  const trimmed = inlineDraft.value.trim();
  editingNodeId.value = null;
  if (trimmed) {
    emit("renameNode", nodeId, trimmed);
  }
  viewportRef.value?.focus({ preventScroll: true });
}

function handleInlineKeydown(e: KeyboardEvent, nodeId: string) {
  if (e.key === "Enter") {
    e.preventDefault();
    commitInlineEdit(nodeId);
  } else if (e.key === "Escape") {
    e.preventDefault();
    cancelInlineEdit();
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (editingNodeId.value) return;
  const target = e.target as HTMLElement | null;
  if (target && ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName)) return;

  const current = layout.value.nodes.find((n) => n.id === props.selectedNodeId);
  if (!current) return;

  if (e.key === "ArrowLeft") {
    e.preventDefault();
    emit("navigateNode", "left");
    return;
  }
  if (e.key === "ArrowRight") {
    e.preventDefault();
    emit("navigateNode", "right");
    return;
  }
  if (e.key === "ArrowUp") {
    e.preventDefault();
    emit("navigateNode", "up");
    return;
  }
  if (e.key === "ArrowDown") {
    e.preventDefault();
    emit("navigateNode", "down");
    return;
  }
  if (e.key === " " && current.node.children.length) {
    e.preventDefault();
    emit("toggleCollapse", current.id);
    return;
  }

  if (!props.editable) return;

  if (e.key === "Tab") {
    e.preventDefault();
    emit("addChild", current.id);
  } else if (e.key === "Enter") {
    e.preventDefault();
    if (current.depth === 0) {
      emit("addChild", current.id);
    } else {
      emit("addSibling", current.id);
    }
  } else if (e.key === "F2") {
    e.preventDefault();
    void startInlineEdit(current.id, current.node.label);
  } else if ((e.key === "Delete" || e.key === "Backspace") && current.depth > 0) {
    e.preventDefault();
    emit("removeNode", current.id);
  }
}

watch(
  () => props.tree.id,
  async () => {
    editingNodeId.value = null;
    await nextTick();
    fitToView();
  },
);

watch(
  () => props.inspectorCollapsed,
  async () => {
    await nextTick();
    setTimeout(() => fitToView(), 80);
  },
);

watch(
  () => props.selectedNodeId,
  async (nextId) => {
    if (!nextId) return;
    await nextTick();
    panNodeIntoView(nextId);
  },
);

let resizeObserver: ResizeObserver | null = null;
let lastObservedWidth = 0;

onMounted(() => {
  nextTick(() => {
    const el = viewportRef.value;
    if (el) {
      lastObservedWidth = el.clientWidth;
    }
    fitToView();
    requestAnimationFrame(() => {
      fitToView();
    });
    if (el && typeof ResizeObserver !== "undefined") {
      resizeObserver = new ResizeObserver((entries) => {
        const w = entries[0]?.contentRect?.width ?? 0;
        if (w > 0 && Math.abs(w - lastObservedWidth) > 20) {
          lastObservedWidth = w;
          fitToView();
        }
      });
      resizeObserver.observe(el);
    }
  });
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  resizeObserver = null;
});

defineExpose({
  fitToView,
  startInlineEdit,
});
</script>

<style scoped>
.mindmap-frame {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-glass-strong);
}

.mindmap-viewport {
  position: relative;
  height: clamp(500px, calc(100vh - 210px), 740px);
  min-height: 480px;
  overflow: hidden;
  cursor: grab;
  user-select: none;
  touch-action: none;
  outline: none;
}

.mindmap-viewport.is-fullscreen-canvas {
  height: clamp(540px, calc(100vh - 190px), 780px);
}

.mindmap-viewport.is-panning {
  cursor: grabbing;
}

.mindmap-dot-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: radial-gradient(circle, var(--border-glass) 1.2px, transparent 1.2px);
  background-size: 22px 22px;
}

.mindmap-stage {
  position: relative;
  transform-origin: 0 0;
  transition: transform 0.36s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

.mindmap-viewport.is-panning .mindmap-stage {
  transition: none;
}

.mindmap-edges {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: visible;
}

.edge-curve {
  fill: none;
  stroke: color-mix(in srgb, var(--branch-color) 42%, var(--border-glass));
  stroke-width: 2;
  opacity: 0.72;
  transition:
    d 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    stroke 0.22s ease,
    stroke-width 0.22s ease,
    opacity 0.22s ease;
}

.edge-curve.is-active {
  stroke: var(--branch-color);
  stroke-width: 2.8;
  opacity: 1;
}

.edge-curve.is-dim {
  opacity: 0.2;
}

.edge-flow {
  fill: none;
  stroke: var(--branch-color);
  stroke-width: 3;
  stroke-linecap: round;
  stroke-dasharray: 6 12;
  opacity: 0.78;
  animation: branchFlow 1.4s linear infinite;
}

@keyframes branchFlow {
  from {
    stroke-dashoffset: 36;
  }
  to {
    stroke-dashoffset: 0;
  }
}

.map-node {
  position: absolute;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  padding: 9px 12px;
  border-radius: var(--radius-control);
  border: 1px solid color-mix(in srgb, var(--branch-color) 28%, var(--border-glass));
  border-left: 3.5px solid var(--branch-color);
  background: var(--bg-glass-strong);
  box-shadow: 0 6px 18px -12px rgba(15, 23, 42, 0.22);
  cursor: pointer;
  outline: none;
  transition:
    left 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    top 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.2s ease;
}

html.dark .map-node {
  box-shadow: 0 10px 24px -12px rgba(0, 0, 0, 0.55);
}

.map-node:hover {
  transform: translateY(-1.5px);
  border-color: var(--branch-color);
  z-index: 2;
}

.map-node.is-root {
  padding: 11px 14px;
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--branch-color) 16%, var(--bg-glass-strong)),
    var(--bg-glass-strong)
  );
  border-width: 1.5px;
  border-left-width: 4.5px;
}

.map-node.is-branch {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--branch-color) 6%, var(--bg-glass-strong)),
    var(--bg-glass-strong)
  );
}

.map-node.is-leaf {
  border-left-width: 3px;
}

.map-node.is-selected {
  z-index: 4;
  border-color: var(--branch-color);
  box-shadow:
    0 0 0 3px color-mix(in srgb, var(--branch-color) 22%, transparent),
    0 12px 26px -12px rgba(15, 23, 42, 0.28);
}

.map-node.is-search-hit {
  z-index: 3;
  box-shadow:
    0 0 0 3px color-mix(in srgb, #f59e0b 45%, transparent),
    0 10px 22px -10px rgba(245, 158, 11, 0.35);
}

.map-node.is-search-dim {
  opacity: 0.3;
}

.node-float-bar {
  position: absolute;
  left: 0;
  bottom: calc(100% + 8px);
  z-index: 10;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 3px 5px;
  border-radius: 10px;
  border: none;
  background: var(--bg-glass-strong);
  backdrop-filter: blur(14px);
  box-shadow: inset 0 0 0 1px var(--border-glass), 0 10px 26px -10px rgba(15, 23, 42, 0.28);
  white-space: nowrap;
  animation: floatBarIn 0.16s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes floatBarIn {
  from {
    opacity: 0;
    transform: translateY(4px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.float-op-btn {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  padding: 3px 6px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  cursor: pointer;
  transition: background-color 0.14s ease, color 0.14s ease;
}

.float-op-btn:hover {
  background: var(--accent-soft);
  color: var(--accent);
}

.float-op-btn.is-danger:hover {
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
}

.float-op-icon {
  width: 12px;
  height: 12px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.node-head {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.node-check-btn {
  width: 16px;
  height: 16px;
  padding: 0;
  border-radius: 50%;
  border: none;
  box-shadow: inset 0 0 0 1.5px var(--border-glass);
  background: var(--bg-subtle);
  color: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: pointer;
  opacity: 0.55;
  transition:
    opacity 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease,
    color 0.18s ease;
}

.map-node:hover .node-check-btn,
.map-node.is-selected .node-check-btn,
.node-check-btn.is-done {
  opacity: 1;
}

.node-check-btn:hover {
  box-shadow: inset 0 0 0 1.5px var(--branch-color);
}

.node-check-btn.is-done {
  background: var(--branch-color);
  box-shadow: inset 0 0 0 1.5px var(--branch-color);
  color: #ffffff;
}

.check-svg {
  width: 10px;
  height: 10px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.node-title {
  flex: 1;
  min-width: 0;
  font-size: 13.5px;
  font-weight: 700;
  line-height: 1.35;
  color: var(--text-primary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.map-node.is-root .node-title {
  font-size: 14.5px;
  font-weight: 800;
}

.node-inline-input {
  flex: 1;
  min-width: 0;
  padding: 2px 6px;
  border-radius: 6px;
  border: 1.5px solid var(--branch-color);
  background: var(--bg-page);
  color: var(--text-primary);
  font-size: 13.5px;
  font-weight: 600;
  outline: none;
}

.node-tag {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 5px;
  font-size: 10.5px;
  font-weight: 600;
  background: color-mix(in srgb, var(--branch-color) 14%, transparent);
  color: var(--branch-color);
}

.node-res-bar {
  display: flex;
  align-items: center;
  gap: 5px;
  padding-left: 23px;
}

.res-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 1px 5px;
  border-radius: 5px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10.5px;
  font-weight: 600;
}

.res-badge--article {
  color: #0d9488;
  background: rgba(13, 148, 136, 0.11);
}

.res-badge--video {
  color: #0284c7;
  background: rgba(2, 132, 199, 0.11);
}

.res-badge--webpage {
  color: #6366f1;
  background: rgba(99, 102, 241, 0.11);
}

html.dark .res-badge--article {
  color: #5eead4;
}

html.dark .res-badge--video {
  color: #7dd3fc;
}

html.dark .res-badge--webpage {
  color: #a5b4fc;
}

.res-mini-icon {
  width: 11px;
  height: 11px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.res-badge--video .res-mini-icon {
  fill: currentColor;
  stroke: none;
}

.node-collapse-pill {
  position: absolute;
  right: -11px;
  top: 50%;
  transform: translateY(-50%);
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  border-radius: 999px;
  border: none;
  background: var(--bg-glass-strong);
  color: var(--branch-color);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1.5px var(--branch-color), 0 2px 6px rgba(15, 23, 42, 0.14);
  transition: transform 0.18s ease, background-color 0.18s ease, color 0.18s ease;
}

.node-collapse-pill:hover {
  transform: translateY(-50%) scale(1.08);
  background: var(--branch-color);
  color: #ffffff;
}

.node-edge-add {
  position: absolute;
  right: -11px;
  top: 50%;
  transform: translateY(-50%) scale(0.86);
  width: 20px;
  height: 20px;
  padding: 0;
  border-radius: 999px;
  border: none;
  background: var(--branch-color);
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 3px 10px -2px color-mix(in srgb, var(--branch-color) 65%, transparent);
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.node-edge-add.has-collapse {
  top: 18%;
}

.map-node:hover .node-edge-add,
.map-node.is-selected .node-edge-add {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(-50%) scale(1);
}

.canvas-hint-hud {
  position: absolute;
  left: 14px;
  top: 12px;
  z-index: 6;
  padding: 5px 11px;
  border-radius: 999px;
  background: var(--bg-glass);
  backdrop-filter: blur(12px);
  box-shadow: inset 0 0 0 1px var(--border-subtle), 0 6px 18px -10px rgba(15, 23, 42, 0.14);
  font-size: 11px;
  color: var(--text-muted);
  pointer-events: none;
  white-space: nowrap;
}

.canvas-control-hud {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 14px;
  z-index: 6;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 5px 7px;
  border-radius: 999px;
  background: var(--bg-glass-strong);
  backdrop-filter: blur(16px);
  box-shadow: inset 0 0 0 1px var(--border-glass), 0 10px 26px -10px rgba(15, 23, 42, 0.22);
  white-space: nowrap;
}

.hud-btn,
.hud-icon-btn,
.hud-zoom-readout {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  height: 26px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.hud-icon-btn {
  width: 26px;
  padding: 0;
}

.hud-zoom-readout {
  min-width: 46px;
  padding: 0 5px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-variant-numeric: tabular-nums;
}

.hud-btn:hover,
.hud-icon-btn:hover,
.hud-zoom-readout:hover,
.hud-btn.is-active-hud {
  background: var(--accent-soft);
  color: var(--accent);
}

.hud-svg {
  width: 13px;
  height: 13px;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
}

.hud-sep {
  width: 1px;
  height: 14px;
  background: var(--border-subtle);
  margin: 0 2px;
}

@supports (corner-shape: squircle) {
  .map-node {
    corner-shape: squircle;
  }
}

@media (max-width: 780px) {
  .mindmap-viewport {
    height: 430px;
    min-height: 360px;
  }

  .canvas-hint-hud {
    display: none;
  }

  .canvas-control-hud {
    left: 10px;
    right: 10px;
    transform: none;
    justify-content: space-between;
  }
}
</style>
