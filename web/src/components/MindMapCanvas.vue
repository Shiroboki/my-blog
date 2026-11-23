<template>
  <div class="mindmap-frame card">
    <div class="mindmap-toolbar">
      <div class="toolbar-left">
        <span class="toolbar-hint">拖拽画布平移 · 点击节点查看挂载资源</span>
      </div>
      <div class="toolbar-controls">
        <button type="button" class="tool-btn" title="展开全部节点" @click="$emit('expandAll')">
          全部展开
        </button>
        <button type="button" class="tool-btn" title="折叠二级分支" @click="$emit('collapseBranches')">
          收起分支
        </button>
        <span class="tool-sep" aria-hidden="true" />
        <button type="button" class="tool-btn" title="缩小画布" @click="zoomBy(-0.14)">-</button>
        <span class="zoom-readout">{{ Math.round(scale * 100) }}%</span>
        <button type="button" class="tool-btn" title="放大画布" @click="zoomBy(0.14)">+</button>
        <button type="button" class="tool-btn" title="自动居中适应画布" @click="fitToView()">
          适应视图
        </button>
      </div>
    </div>

    <div
      ref="viewportRef"
      class="mindmap-viewport"
      :class="{ 'is-panning': panning }"
      @pointerdown="startPan"
      @pointermove="movePan"
      @pointerup="endPan"
      @pointercancel="endPan"
      @wheel="handleWheel"
    >
      <div class="mindmap-dot-grid" aria-hidden="true" />

      <div
        class="mindmap-stage"
        :style="{
          width: `${layout.width}px`,
          height: `${layout.height}px`,
          transform: `translate(${panX}px, ${panY}px) scale(${scale})`,
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
            :class="{ 'is-active': activeChainIds.has(edge.fromId) && activeChainIds.has(edge.toId) }"
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
            'is-selected': selectedNodeId === item.id,
            'is-completed': completedSet.has(item.id),
            'is-search-hit': hasSearch && matchedIds.has(item.id),
            'is-search-dim': hasSearch && !matchedIds.has(item.id),
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
          @click.stop="$emit('selectNode', item.id)"
          @keydown.enter.prevent="$emit('selectNode', item.id)"
        >
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

            <span class="node-title">{{ item.node.label }}</span>

            <span v-if="item.node.tag" class="node-tag">{{ item.node.tag }}</span>
          </div>

          <p v-if="item.node.description" class="node-desc-preview">
            {{ item.node.description }}
          </p>

          <div
            v-if="
              item.resourceCounts.article ||
              item.resourceCounts.video ||
              item.resourceCounts.webpage
            "
            class="node-res-bar"
          >
            <span v-if="item.resourceCounts.article" class="res-badge res-badge--article">
              <svg viewBox="0 0 16 16" class="res-mini-icon" aria-hidden="true">
                <rect x="3" y="2" width="10" height="12" rx="1.5" />
                <line x1="5.5" y1="6" x2="10.5" y2="6" />
                <line x1="5.5" y1="9.2" x2="9" y2="9.2" />
              </svg>
              文章 {{ item.resourceCounts.article }}
            </span>
            <span v-if="item.resourceCounts.video" class="res-badge res-badge--video">
              <svg viewBox="0 0 16 16" class="res-mini-icon" aria-hidden="true">
                <polygon points="5,3.5 13,8 5,12.5" />
              </svg>
              视频 {{ item.resourceCounts.video }}
            </span>
            <span v-if="item.resourceCounts.webpage" class="res-badge res-badge--webpage">
              <svg viewBox="0 0 16 16" class="res-mini-icon" aria-hidden="true">
                <circle cx="8" cy="8" r="5.5" />
                <line x1="2.8" y1="8" x2="13.2" y2="8" />
                <ellipse cx="8" cy="8" rx="2.5" ry="5.5" />
              </svg>
              网页 {{ item.resourceCounts.webpage }}
            </span>
          </div>

          <button
            v-if="item.node.children.length"
            type="button"
            class="node-collapse-pill"
            :title="item.node.collapsed ? '展开子节点' : '收起子节点'"
            @click.stop="$emit('toggleCollapse', item.id)"
          >
            {{ item.node.collapsed ? `+${item.hiddenChildCount}` : "-" }}
          </button>

          <div
            v-if="editable && selectedNodeId === item.id"
            class="node-quick-ops"
            @click.stop
          >
            <button type="button" class="quick-op-btn" @click="$emit('addChild', item.id)">
              +子节点
            </button>
            <button
              v-if="item.depth > 0"
              type="button"
              class="quick-op-btn"
              @click="$emit('addSibling', item.id)"
            >
              +同级
            </button>
            <button
              v-if="item.depth > 0"
              type="button"
              class="quick-op-btn quick-op-btn--danger"
              @click="$emit('removeNode', item.id)"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";
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
  }>(),
  {
    accentColor: "#0d9488",
    searchQuery: "",
    editable: false,
  },
);

defineEmits<{
  selectNode: [id: string];
  toggleCollapse: [id: string];
  toggleComplete: [id: string];
  expandAll: [];
  collapseBranches: [];
  addChild: [id: string];
  addSibling: [id: string];
  removeNode: [id: string];
}>();

const viewportRef = ref<HTMLElement | null>(null);
const scale = ref(1);
const panX = ref(18);
const panY = ref(18);
const panning = ref(false);

let activePointerId: number | null = null;
let dragStartClientX = 0;
let dragStartClientY = 0;
let dragOriginPanX = 0;
let dragOriginPanY = 0;

const layout = computed(() => computeMindMapLayout(props.tree));
const activeChainIds = computed(() => findAncestorIds(props.tree, props.selectedNodeId));
const completedSet = computed(() => new Set(props.completedIds));
const hasSearch = computed(() => Boolean(props.searchQuery.trim()));
const matchedIds = computed(() => collectMatchingNodeIds(props.tree, props.searchQuery));

function clampScale(next: number): number {
  return Math.min(1.5, Math.max(0.42, next));
}

function zoomBy(delta: number) {
  const el = viewportRef.value;
  const oldScale = scale.value;
  const nextScale = clampScale(oldScale + delta);
  if (!el || nextScale === oldScale) {
    scale.value = nextScale;
    return;
  }
  const cx = el.clientWidth / 2;
  const cy = el.clientHeight / 2;
  const ratio = nextScale / oldScale;
  panX.value = cx - (cx - panX.value) * ratio;
  panY.value = cy - (cy - panY.value) * ratio;
  scale.value = nextScale;
}

function fitToView() {
  const el = viewportRef.value;
  if (!el) return;
  const vw = Math.max(320, el.clientWidth);
  const vh = Math.max(320, el.clientHeight);
  const lw = layout.value.width;
  const lh = layout.value.height;
  const padding = 28;
  const nextScale = clampScale(
    Math.min(1, (vw - padding) / lw, (vh - padding) / lh),
  );
  scale.value = nextScale;
  panX.value = Math.max(12, (vw - lw * nextScale) / 2);
  panY.value = Math.max(12, (vh - lh * nextScale) / 2);
}

function startPan(e: PointerEvent) {
  if (e.button !== 0) return;
  const target = e.target as HTMLElement | null;
  if (target?.closest(".map-node, button")) return;
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

function handleWheel(e: WheelEvent) {
  if (!e.ctrlKey && !e.metaKey) return;
  e.preventDefault();
  const delta = e.deltaY < 0 ? 0.08 : -0.08;
  zoomBy(delta);
}

watch(
  () => props.tree.id,
  async () => {
    await nextTick();
    fitToView();
  },
);

onMounted(() => {
  nextTick(() => {
    fitToView();
  });
});

defineExpose({
  fitToView,
});
</script>

<style scoped>
.mindmap-frame {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-glass-strong);
}

.mindmap-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 12px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--bg-glass);
}

.toolbar-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.toolbar-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.tool-btn {
  padding: 4px 10px;
  border-radius: var(--radius-chip);
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease, background-color 0.18s ease;
}

.tool-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.tool-sep {
  width: 1px;
  height: 14px;
  background: var(--border-subtle);
  margin: 0 2px;
}

.zoom-readout {
  min-width: 42px;
  text-align: center;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.mindmap-viewport {
  position: relative;
  height: 580px;
  min-height: 460px;
  overflow: hidden;
  cursor: grab;
  user-select: none;
  touch-action: none;
}

.mindmap-viewport.is-panning {
  cursor: grabbing;
}

.mindmap-dot-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: radial-gradient(circle, var(--border-glass) 1.25px, transparent 1.25px);
  background-size: 22px 22px;
}

.mindmap-stage {
  position: relative;
  transform-origin: 0 0;
  transition: transform 0.12s cubic-bezier(0.22, 1, 0.36, 1);
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
  opacity: 0.65;
  transition: stroke 0.2s ease, stroke-width 0.2s ease, opacity 0.2s ease;
}

.edge-curve.is-active {
  stroke: var(--branch-color);
  stroke-width: 2.8;
  opacity: 1;
}

.map-node {
  position: absolute;
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 10px 12px;
  border-radius: var(--radius-control);
  border: 1.5px solid color-mix(in srgb, var(--branch-color) 28%, var(--border-glass));
  border-left: 4px solid var(--branch-color);
  background: var(--bg-glass-strong);
  box-shadow: 0 6px 18px -10px rgba(15, 23, 42, 0.18);
  cursor: pointer;
  outline: none;
  transition:
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
}

.map-node.is-root {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--branch-color) 16%, var(--bg-glass-strong)),
    var(--bg-glass-strong)
  );
  border-width: 1.5px;
  border-left-width: 4px;
}

.map-node.is-selected {
  border-color: var(--branch-color);
  box-shadow:
    0 0 0 3px color-mix(in srgb, var(--branch-color) 22%, transparent),
    0 10px 24px -10px rgba(15, 23, 42, 0.22);
}

.map-node.is-search-hit {
  box-shadow:
    0 0 0 3px color-mix(in srgb, #f59e0b 45%, transparent),
    0 10px 22px -10px rgba(245, 158, 11, 0.35);
}

.map-node.is-search-dim {
  opacity: 0.34;
}

.node-head {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.node-check-btn {
  width: 17px;
  height: 17px;
  padding: 0;
  border-radius: 50%;
  border: 1.5px solid var(--border-glass);
  background: var(--bg-subtle);
  color: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: pointer;
  transition: background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.node-check-btn:hover {
  border-color: var(--branch-color);
}

.node-check-btn.is-done {
  background: var(--branch-color);
  border-color: var(--branch-color);
  color: #ffffff;
}

.check-svg {
  width: 11px;
  height: 11px;
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

.node-tag {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 5px;
  font-size: 10.5px;
  font-weight: 600;
  background: color-mix(in srgb, var(--branch-color) 14%, transparent);
  color: var(--branch-color);
}

.node-desc-preview {
  margin: 0;
  font-size: 11.5px;
  line-height: 1.4;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-res-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
  margin-top: 2px;
}

.res-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 1px 6px;
  border-radius: 5px;
  font-size: 10.5px;
  font-weight: 500;
  background: var(--bg-subtle);
  color: var(--text-secondary);
}

.res-badge--article {
  color: #0d9488;
  background: rgba(13, 148, 136, 0.1);
}

.res-badge--video {
  color: #0284c7;
  background: rgba(2, 132, 199, 0.1);
}

.res-badge--webpage {
  color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
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
  right: -12px;
  top: 50%;
  transform: translateY(-50%);
  min-width: 22px;
  height: 22px;
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
}

.node-collapse-pill:hover {
  background: var(--branch-color);
  color: #ffffff;
}

.node-quick-ops {
  position: absolute;
  left: 0;
  top: calc(100% + 6px);
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 5px;
  border-radius: 8px;
  border: 1px solid var(--border-glass);
  background: var(--bg-glass-strong);
  box-shadow: 0 8px 20px -8px rgba(15, 23, 42, 0.24);
  white-space: nowrap;
}

.quick-op-btn {
  border: none;
  background: var(--bg-subtle);
  color: var(--text-secondary);
  padding: 3px 7px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
}

.quick-op-btn:hover {
  color: var(--accent);
}

.quick-op-btn--danger:hover {
  color: #ef4444;
}

@supports (corner-shape: squircle) {
  .tool-btn,
  .map-node {
    corner-shape: squircle;
  }
}

@media (max-width: 720px) {
  .mindmap-viewport {
    height: 430px;
    min-height: 360px;
  }
}
</style>
