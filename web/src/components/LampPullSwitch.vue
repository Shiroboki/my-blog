<template>
  <div
    class="lamp-pull-anchor"
    :class="{
      'is-dark': isDark,
      'show-hint': showHint || isDragging || isHovered,
    }"
  >
    <span class="lamp-hint" role="status" aria-hidden="true">
      {{ isArmed ? "松手切换" : isDark ? "下拉开灯" : "下拉关灯" }}
    </span>
    <svg
      class="lamp-svg"
      viewBox="0 0 56 140"
      role="switch"
      tabindex="0"
      :aria-checked="isDark"
      :aria-label="isDark ? '浅色模式已关闭，向下拉拽绳子开灯切换浅色模式' : '已开启浅色模式，向下拉拽绳子关灯切换深色模式'"
      @focus="isHovered = true"
      @blur="isHovered = false"
      @keydown.enter.prevent="triggerClickPull"
      @keydown.space.prevent="triggerClickPull"
    >
      <title>{{ isDark ? "向下拽拉绳开灯（浅色模式）" : "向下拽拉绳关灯（深色模式）" }}</title>
      <defs>
        <radialGradient id="lampWarmBeam" cx="50%" cy="0%" r="75%">
          <stop offset="0%" stop-color="#fbbf24" stop-opacity="0.42" />
          <stop offset="55%" stop-color="#f59e0b" stop-opacity="0.14" />
          <stop offset="100%" stop-color="#f59e0b" stop-opacity="0" />
        </radialGradient>
        <linearGradient id="shadeBodyLight" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#1e293b" />
          <stop offset="55%" stop-color="#334155" />
          <stop offset="100%" stop-color="#0f172a" />
        </linearGradient>
        <linearGradient id="shadeBodyDark" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#475569" />
          <stop offset="60%" stop-color="#1e293b" />
          <stop offset="100%" stop-color="#0f172a" />
        </linearGradient>
      </defs>

      <!-- 开灯时的柔光光锥（深色模式下熄灭） -->
      <path
        v-if="!isDark"
        d="M16 27 L40 27 L54 78 L2 78 Z"
        fill="url(#lampWarmBeam)"
        class="lamp-beam"
      />

      <!-- 顶部固定支架与小台灯灯罩 -->
      <g class="lamp-head" @click="triggerClickPull">
        <line x1="28" y1="0" x2="28" y2="10" class="lamp-rod" />
        <ellipse cx="28" cy="27" rx="4.2" ry="3.2" :class="isDark ? 'lamp-bulb is-off' : 'lamp-bulb is-on'" />
        <path
          d="M14 27 C14 15, 20.5 10, 28 10 C35.5 10, 42 15, 42 27 Z"
          :fill="isDark ? 'url(#shadeBodyDark)' : 'url(#shadeBodyLight)'"
          class="lamp-shade"
        />
        <line x1="15.5" y1="25.5" x2="40.5" y2="25.5" class="lamp-shade-rim" />
      </g>

      <!-- 自由摆动 & 可向下拖拽的拉绳 -->
      <g
        class="cord-pivot"
        :class="{ 'is-dragging': isDragging, 'is-armed': isArmed }"
        :transform="`rotate(${totalAngle.toFixed(2)} 28 24)`"
        @pointerdown="onPointerDown"
        @pointermove="onPointerMove"
        @pointerup="onPointerUp"
        @pointercancel="onPointerCancel"
        @pointerenter="handlePointerEnter"
        @pointerleave="isHovered = false"
      >
        <!-- 扩大抓取点击热区，避免细绳难选中 -->
        <rect
          x="13"
          y="24"
          width="28"
          :height="cordLength + 20"
          fill="transparent"
          stroke="none"
          class="cord-hit-area"
        />

        <!-- 绳线主体 -->
        <line
          x1="28"
          y1="24"
          x2="28"
          :y2="24 + cordLength"
          class="cord-line"
        />

        <!-- 底部金属珠环 + 坠头 -->
        <g :transform="`translate(28, ${24 + cordLength})`">
          <circle cx="0" cy="0" r="2.2" class="cord-ferrule" />
          <path
            d="M-3.8 2.5 C-3.8 0.5, 3.8 0.5, 3.8 2.5 L4.6 10.2 C4.6 12.6, 2.5 14, 0 14 C-2.5 14, -4.6 12.6, -4.6 10.2 Z"
            class="cord-knob"
          />
          <line x1="-1.6" y1="4.4" x2="-1.9" y2="9.8" class="cord-knob-highlight" />
        </g>
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";

defineProps<{
  isDark: boolean;
}>();

const emit = defineEmits<{
  toggle: [];
}>();

const BASE_CORD_LENGTH = 50;
const TOGGLE_STRETCH_THRESHOLD = 22;

const stretch = ref(0);
const idleAngle = ref(0);
const swingAngle = ref(0);
const isDragging = ref(false);
const isHovered = ref(false);
const showHint = ref(false);

const LAMP_HINT_STORAGE_KEY = "myblog_lamp_hint_seen";
let hintTimer: number | null = null;

const totalAngle = computed(() => idleAngle.value + swingAngle.value);
const cordLength = computed(() => BASE_CORD_LENGTH + Math.max(-5, stretch.value));
const isArmed = computed(() => isDragging.value && stretch.value >= TOGGLE_STRETCH_THRESHOLD);

let stretchVel = 0;
let swingVel = 0;
let rafId: number | null = null;
let lastTimestamp = 0;
let elapsed = 0;
let reducedMotion = false;

let dragStartX = 0;
let dragStartY = 0;
let maxDragDistance = 0;
let toggledDuringDrag = false;
let activePointerId: number | null = null;

function stepPhysics(now: number) {
  if (!lastTimestamp) {
    lastTimestamp = now;
  }
  const dt = Math.min(0.045, Math.max(0.001, (now - lastTimestamp) / 1000));
  lastTimestamp = now;
  elapsed += dt;

  if (reducedMotion) {
    idleAngle.value = 0;
  } else {
    // 平时柔和的自然微风双频摆动
    idleAngle.value = Math.sin(elapsed * 1.42) * 3.1 + Math.sin(elapsed * 0.78 + 0.9) * 1.35;
  }

  if (!isDragging.value) {
    // 垂直弹性回弹（轻度欠阻尼，产生自然弹跳感）
    const springForceY = -360 * stretch.value - 17 * stretchVel;
    stretchVel += springForceY * dt;
    stretch.value += stretchVel * dt;
    if (stretch.value < -5) {
      stretch.value = -5;
      stretchVel *= -0.35;
    }

    // 水平摆角衰减回摆
    const springForceA = -42 * swingAngle.value - 2.6 * swingVel;
    swingVel += springForceA * dt;
    swingAngle.value += swingVel * dt;

    if (Math.abs(stretch.value) < 0.02 && Math.abs(stretchVel) < 0.1) {
      stretch.value = 0;
      stretchVel = 0;
    }
    if (Math.abs(swingAngle.value) < 0.02 && Math.abs(swingVel) < 0.1) {
      swingAngle.value = 0;
      swingVel = 0;
    }
  }

  rafId = window.requestAnimationFrame(stepPhysics);
}

function handlePointerEnter() {
  isHovered.value = true;
  nudgeCord();
}

function nudgeCord() {
  if (isDragging.value || reducedMotion) return;
  swingVel += swingAngle.value >= 0 ? -36 : 36;
}

function triggerClickPull() {
  stretchVel = 290;
  swingVel += swingAngle.value >= 0 ? 44 : -44;
  showHint.value = false;
  emit("toggle");
}

function onPointerDown(e: PointerEvent) {
  if (e.button !== 0 && e.pointerType === "mouse") return;
  const target = e.currentTarget as Element | null;
  activePointerId = e.pointerId;
  target?.setPointerCapture?.(e.pointerId);

  isDragging.value = true;
  dragStartX = e.clientX;
  dragStartY = e.clientY;
  maxDragDistance = 0;
  toggledDuringDrag = false;
  stretchVel = 0;
  swingVel = 0;
}

function onPointerMove(e: PointerEvent) {
  if (!isDragging.value || e.pointerId !== activePointerId) return;
  const dy = e.clientY - dragStartY;
  const dx = e.clientX - dragStartX;
  maxDragDistance = Math.max(maxDragDistance, Math.hypot(dx, dy));

  // 向下拽拉时带柔韧阻尼拉伸
  const nextStretch = dy <= 0 ? Math.max(-3, dy * 0.12) : Math.min(46, dy * 0.72);
  stretch.value = nextStretch;
  swingAngle.value = Math.max(-24, Math.min(24, -dx * 0.24));

  if (!toggledDuringDrag && nextStretch >= TOGGLE_STRETCH_THRESHOLD) {
    toggledDuringDrag = true;
    showHint.value = false;
    emit("toggle");
  }
}

function finishDrag(e: PointerEvent) {
  if (!isDragging.value || e.pointerId !== activePointerId) return;
  const target = e.currentTarget as Element | null;
  if (target?.hasPointerCapture?.(e.pointerId)) {
    target.releasePointerCapture(e.pointerId);
  }
  activePointerId = null;
  isDragging.value = false;

  if (!toggledDuringDrag && maxDragDistance < 8) {
    triggerClickPull();
    return;
  }

  // 松手瞬间回弹，并给摆角一点惯性
  swingVel += (swingAngle.value >= 0 ? -1 : 1) * Math.max(24, Math.abs(swingAngle.value) * 2.2);
}

function onPointerUp(e: PointerEvent) {
  finishDrag(e);
}

function onPointerCancel(e: PointerEvent) {
  finishDrag(e);
}

onMounted(() => {
  reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  rafId = window.requestAnimationFrame(stepPhysics);
  try {
    if (!localStorage.getItem(LAMP_HINT_STORAGE_KEY)) {
      showHint.value = true;
      localStorage.setItem(LAMP_HINT_STORAGE_KEY, "1");
      hintTimer = window.setTimeout(() => {
        showHint.value = false;
        hintTimer = null;
      }, 3600);
    }
  } catch {
    // 忽略本地存储异常
  }
});

onUnmounted(() => {
  if (rafId !== null) {
    window.cancelAnimationFrame(rafId);
  }
  if (hintTimer !== null) {
    window.clearTimeout(hintTimer);
  }
});
</script>

<style scoped>
.lamp-pull-anchor {
  position: fixed;
  top: 0;
  right: 22px;
  z-index: 65;
  width: 56px;
  height: 140px;
  pointer-events: none;
  user-select: none;
}

.lamp-hint {
  position: absolute;
  top: 78px;
  right: 48px;
  padding: 4px 9px;
  border-radius: 999px;
  border: 1px solid var(--border-glass);
  background: var(--bg-glass-strong);
  box-shadow: var(--shadow-glass);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  transform: translateX(6px);
  transition: opacity 0.22s ease, transform 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}

.lamp-pull-anchor.show-hint .lamp-hint {
  opacity: 1;
  transform: translateX(0);
}

.lamp-svg {
  width: 100%;
  height: 100%;
  overflow: visible;
  outline: none;
}

.lamp-svg:focus-visible .cord-knob {
  stroke: var(--accent);
  stroke-width: 2;
}

.lamp-beam {
  pointer-events: none;
  transition: opacity 0.3s ease;
}

.lamp-head {
  pointer-events: auto;
  cursor: pointer;
}

.lamp-rod {
  stroke: var(--text-secondary);
  stroke-width: 2.2;
  stroke-linecap: round;
}

.lamp-shade {
  stroke: rgba(15, 23, 42, 0.25);
  stroke-width: 0.8;
}

.lamp-pull-anchor.is-dark .lamp-shade {
  stroke: rgba(255, 255, 255, 0.18);
}

.lamp-shade-rim {
  stroke: rgba(255, 255, 255, 0.22);
  stroke-width: 1;
}

.lamp-bulb.is-on {
  fill: #fde68a;
  filter: drop-shadow(0 0 6px rgba(251, 191, 36, 0.9));
}

.lamp-bulb.is-off {
  fill: #475569;
  opacity: 0.72;
}

.cord-pivot {
  pointer-events: auto;
  cursor: grab;
  touch-action: none;
}

.cord-pivot.is-dragging {
  cursor: grabbing;
}

.cord-hit-area {
  fill: transparent;
  stroke: none;
}

.cord-line {
  stroke: var(--text-secondary);
  stroke-width: 1.7;
  stroke-linecap: round;
  transition: stroke 0.2s ease;
}

.cord-ferrule {
  fill: var(--text-muted);
}

.cord-knob {
  fill: #334155;
  stroke: rgba(255, 255, 255, 0.24);
  stroke-width: 0.9;
  filter: drop-shadow(0 3px 5px rgba(15, 23, 42, 0.24));
  transition: fill 0.2s ease, stroke 0.2s ease, filter 0.2s ease;
}

.lamp-pull-anchor.is-dark .cord-knob {
  fill: #cbd5e1;
  stroke: rgba(255, 255, 255, 0.55);
  filter: drop-shadow(0 3px 8px rgba(0, 0, 0, 0.55));
}

.cord-knob-highlight {
  stroke: rgba(255, 255, 255, 0.48);
  stroke-width: 1.1;
  stroke-linecap: round;
}

.lamp-pull-anchor.is-dark .cord-knob-highlight {
  stroke: rgba(255, 255, 255, 0.85);
}

.cord-pivot:hover .cord-line,
.cord-pivot.is-dragging .cord-line {
  stroke: var(--text-primary);
}

.cord-pivot:hover .cord-knob,
.cord-pivot.is-dragging .cord-knob {
  fill: #1e293b;
}

.lamp-pull-anchor.is-dark .cord-pivot:hover .cord-knob,
.lamp-pull-anchor.is-dark .cord-pivot.is-dragging .cord-knob {
  fill: #f1f5f9;
}

.cord-pivot.is-armed .cord-knob {
  transform: scale(1.06);
}

@media (max-width: 640px) {
  .lamp-pull-anchor {
    right: 8px;
    width: 48px;
    height: 124px;
  }

  .lamp-hint {
    display: none;
  }
}
</style>
