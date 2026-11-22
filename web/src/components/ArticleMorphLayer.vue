<template>
  <Teleport to="body">
    <div v-if="activeMorph" :key="activeMorph.key" class="morph-layer" aria-hidden="true">
      <!-- 离开首页时：原地保留一份不透明底色的冻结首页并柔和退场，防止双层叠字重影与闪断 -->
      <div
        v-if="activeMorph.phase === 'enter' && activeMorph.underlaySnapshot"
        class="morph-underlay-mask"
        :class="`is-${activeMorph.stage}`"
      >
        <div
          ref="underlayHostRef"
          class="morph-underlay"
          :class="`is-${activeMorph.stage}`"
          :style="underlayStyle"
        />
      </div>

      <div
        class="morph-card"
        :class="[
          `is-${activeMorph.phase}`,
          `is-${activeMorph.stage}`,
          {
            'is-handing-off': activeMorph.handingOff,
            'is-flat-surface': !currentRect.isCardSurface,
          },
        ]"
        :style="cardStyle"
      >
        <!-- 首页真实卡片克隆层 -->
        <div
          ref="compactHostRef"
          class="morph-face morph-compact-face"
          :class="[`is-${activeMorph.phase}`, `is-${activeMorph.stage}`]"
        />

        <!-- 详情真实阅读纸面克隆层 -->
        <div
          ref="sheetHostRef"
          class="morph-face morph-sheet-face"
          :class="[`is-${activeMorph.phase}`, `is-${activeMorph.stage}`]"
        />
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import {
  useArticleTransition,
  type ClonedNodeSnapshot,
} from "@/composables/useArticleTransition";

const { activeMorph } = useArticleTransition();

const underlayHostRef = ref<HTMLElement | null>(null);
const compactHostRef = ref<HTMLElement | null>(null);
const sheetHostRef = ref<HTMLElement | null>(null);

const currentRect = computed(() => {
  const m = activeMorph.value;
  if (!m) {
    return {
      left: 0,
      top: 0,
      width: 0,
      height: 0,
      borderRadius: "20px",
      squircle: false,
      isCardSurface: true,
    };
  }
  return m.stage === "run" ? m.to : m.from;
});

const underlayStyle = computed(() => {
  const snap = activeMorph.value?.underlaySnapshot;
  if (!snap) return {};
  return {
    left: `${snap.left ?? 0}px`,
    top: `${snap.top ?? 0}px`,
    width: `${snap.width}px`,
  };
});

const cardStyle = computed(() => {
  const rect = currentRect.value;
  return {
    left: `${rect.left}px`,
    top: `${rect.top}px`,
    width: `${rect.width}px`,
    height: `${rect.height}px`,
    borderRadius: rect.borderRadius,
    cornerShape: rect.squircle ? "squircle" : "round",
  };
});

function mountClonedNode(
  host: HTMLElement | null,
  snap: ClonedNodeSnapshot | null,
  mode: "underlay" | "compact" | "sheet",
) {
  if (!host) return;
  host.innerHTML = "";
  if (!snap) return;

  const clone = snap.node.cloneNode(true) as HTMLElement;
  clone.style.margin = "0";
  clone.style.boxSizing = "border-box";

  if (mode === "compact") {
    clone.style.width = `${snap.width}px`;
    clone.style.height = `${snap.height}px`;
    clone.style.background = "transparent";
    clone.style.borderColor = "transparent";
    clone.style.boxShadow = "none";
  } else if (mode === "sheet") {
    clone.style.width = `${snap.width}px`;
    clone.style.background = "transparent";
    clone.style.border = "none";
    clone.style.boxShadow = "none";
    clone.style.borderRadius = "0";
    if (snap.offsetTop) {
      clone.style.transform = `translateY(${snap.offsetTop}px)`;
    }
  }

  host.appendChild(clone);
}

watch(
  () => activeMorph.value?.key,
  () => {
    const m = activeMorph.value;
    if (!m) return;
    mountClonedNode(underlayHostRef.value, m.underlaySnapshot, "underlay");
    mountClonedNode(compactHostRef.value, m.compactSnapshot, "compact");
    mountClonedNode(sheetHostRef.value, m.sheetSnapshot, "sheet");
  },
  { immediate: true, flush: "post" },
);

watch(
  () => activeMorph.value?.sheetSnapshot,
  (snap) => {
    if (!snap || activeMorph.value?.phase !== "enter") return;
    mountClonedNode(sheetHostRef.value, snap, "sheet");
  },
  { flush: "post" },
);
</script>

<style scoped>
.morph-layer {
  position: fixed;
  inset: 0;
  z-index: 46;
  pointer-events: none;
  contain: layout style;
}

.morph-underlay-mask {
  position: fixed;
  inset: 0;
  z-index: 43;
  background: var(--bg-page);
  pointer-events: none;
  opacity: 1;
  transition: opacity 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}

.morph-underlay-mask.is-run {
  opacity: 0;
}

.morph-underlay {
  position: fixed;
  pointer-events: none;
  transform-origin: center 35vh;
  transform: scale(1);
  transition: transform 0.38s cubic-bezier(0.22, 1, 0.36, 1);
}

.morph-underlay.is-run {
  transform: scale(0.985);
}

.morph-card {
  position: fixed;
  z-index: 46;
  overflow: hidden;
  background-color: var(--morph-surface);
  border: 1px solid var(--border-glass);
  box-shadow: var(--shadow-glass), var(--specular-inset);
  contain: layout style paint;
  will-change: left, top, width, height, border-radius, opacity;
  opacity: 1;
  transition:
    left 0.38s cubic-bezier(0.22, 1, 0.36, 1),
    top 0.38s cubic-bezier(0.22, 1, 0.36, 1),
    width 0.38s cubic-bezier(0.22, 1, 0.36, 1),
    height 0.38s cubic-bezier(0.22, 1, 0.36, 1),
    border-radius 0.38s cubic-bezier(0.22, 1, 0.36, 1),
    background-color 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.06s linear;
}

.morph-card.is-flat-surface {
  background-color: var(--bg-page);
  border-color: transparent;
  box-shadow: none;
}

.morph-card.is-handing-off {
  opacity: 0;
}

.morph-face {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

/* 进入详情：底层卡片柔和隐退，顶层纸面带 100% 不透明同色基底同步浮现 */
.morph-compact-face.is-enter {
  z-index: 1;
  opacity: 1;
  transition: opacity 0.22s ease 0.04s;
}

.morph-compact-face.is-enter.is-run {
  opacity: 0;
}

.morph-sheet-face.is-enter {
  z-index: 2;
  background-color: inherit;
  opacity: 0;
  transition: opacity 0.2s cubic-bezier(0.22, 1, 0.36, 1);
}

.morph-sheet-face.is-enter.is-run {
  opacity: 1;
}

/* 返回首页：底层纸面柔和隐退，顶层卡片带 100% 不透明同色基底同步浮现 */
.morph-sheet-face.is-leave {
  z-index: 1;
  opacity: 1;
  transition: opacity 0.22s ease 0.04s;
}

.morph-sheet-face.is-leave.is-run {
  opacity: 0;
}

.morph-compact-face.is-leave {
  z-index: 2;
  background-color: inherit;
  opacity: 0;
  transition: opacity 0.2s cubic-bezier(0.22, 1, 0.36, 1);
}

.morph-compact-face.is-leave.is-run {
  opacity: 1;
}
</style>
