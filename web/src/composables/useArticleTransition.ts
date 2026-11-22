import { computed, ref } from "vue";
import type {
  BlogArticleDetail,
  BlogArticleItem,
  BlogCategory,
  BlogTag,
  BlogTagRef,
} from "@/api/blog";

export interface RectBox {
  left: number;
  top: number;
  width: number;
  height: number;
  borderRadius: string;
  squircle: boolean;
  isCardSurface: boolean;
}

export interface ArticlePreviewSeed {
  id: string;
  title: string;
  summary?: string;
  coverImage?: string;
  categoryId?: string;
  categoryName?: string;
  tags?: BlogTagRef[];
  isTop?: number;
  readingMinutes?: number;
  wordCount?: number;
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
  publishTime?: string;
  createTime?: string;
}

export type MorphOriginKind = "feed" | "hot";

export interface ClonedNodeSnapshot {
  node: HTMLElement;
  width: number;
  height: number;
  offsetTop: number;
  left?: number;
  top?: number;
}

export interface ActiveMorph {
  key: number;
  phase: "enter" | "leave";
  articleId: string;
  preview: ArticlePreviewSeed;
  from: RectBox;
  to: RectBox;
  stage: "init" | "run";
  handingOff: boolean;
  compactSnapshot: ClonedNodeSnapshot | null;
  sheetSnapshot: ClonedNodeSnapshot | null;
  underlaySnapshot: ClonedNodeSnapshot | null;
}

export interface HomeFeedSnapshot {
  queryKey: string;
  articles: BlogArticleItem[];
  total: number;
  categories: BlogCategory[];
  tags: BlogTag[];
  scrollY: number;
  savedAt: number;
}

interface PendingEnterState {
  articleId: string;
  preview: ArticlePreviewSeed;
  originKind: MorphOriginKind;
  from: RectBox;
  compactSnapshot: ClonedNodeSnapshot | null;
  underlaySnapshot: ClonedNodeSnapshot | null;
}

interface PendingLeaveState {
  articleId: string;
  preview: ArticlePreviewSeed;
  originKind: MorphOriginKind;
  from: RectBox;
  sheetSnapshot: ClonedNodeSnapshot | null;
}

export const MORPH_DURATION_MS = 380;

const pendingEnter = ref<PendingEnterState | null>(null);
const pendingLeave = ref<PendingLeaveState | null>(null);
const activeMorph = ref<ActiveMorph | null>(null);
const hiddenHomeArticleId = ref<string | null>(null);
const hiddenHomeOriginKind = ref<MorphOriginKind | null>(null);
const hiddenDetailSheet = ref(false);
const isReturningToHome = ref(false);
const lastOriginKindByArticle = new Map<string, MorphOriginKind>();

let homeSnapshot: HomeFeedSnapshot | null = null;
const articleDetailCache = new Map<string, BlogArticleDetail>();
const articleInflightCache = new Map<string, Promise<BlogArticleDetail>>();

let morphSeq = 0;
let rafA: number | null = null;
let rafB: number | null = null;
let handoffRaf: number | null = null;
let revealTimer: number | null = null;
let cleanupTimer: number | null = null;

export function supportsSquircleCorner(): boolean {
  return typeof CSS !== "undefined" && typeof CSS.supports === "function"
    ? CSS.supports("corner-shape", "squircle")
    : false;
}

export function prefersReducedMotion(): boolean {
  return (
    typeof window !== "undefined" &&
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

function sanitizeDomClone(source: HTMLElement): HTMLElement {
  const clone = source.cloneNode(true) as HTMLElement;
  clone.removeAttribute("id");
  clone.querySelectorAll("[id]").forEach((el) => el.removeAttribute("id"));
  clone.querySelectorAll("script, iframe").forEach((el) => el.remove());
  clone.classList.remove("animate-rise", "is-morph-hidden");
  clone.style.animation = "none";
  clone.style.opacity = "1";
  clone.style.visibility = "visible";
  return clone;
}

function captureNodeSnapshot(
  source: HTMLElement | null,
  offsetTop = 0,
): ClonedNodeSnapshot | null {
  if (!source) return null;
  const rect = source.getBoundingClientRect();
  return {
    node: sanitizeDomClone(source),
    width: rect.width,
    height: rect.height,
    offsetTop,
    left: rect.left,
    top: rect.top,
  };
}

export function measureElementBox(el: HTMLElement): RectBox {
  const rect = el.getBoundingClientRect();
  const style = window.getComputedStyle(el);
  const squircle =
    supportsSquircleCorner() && style.getPropertyValue("corner-shape").trim() !== "round";
  return {
    left: rect.left,
    top: rect.top,
    width: Math.max(rect.width, 40),
    height: Math.max(rect.height, 32),
    borderRadius: style.borderRadius || "16px",
    squircle,
    isCardSurface: el.classList.contains("card"),
  };
}

export function measureVisibleSheetBox(el: HTMLElement): RectBox {
  const rect = el.getBoundingClientRect();
  const style = window.getComputedStyle(el);
  const vh = window.innerHeight;
  const clampedTop = rect.bottom < 84 ? 78 : Math.max(rect.top, 76);
  const clampedBottom =
    rect.bottom < 84 ? Math.max(440, vh - 14) : Math.min(rect.bottom, vh - 12);
  const height = Math.max(280, clampedBottom - clampedTop);

  return {
    left: rect.left,
    top: clampedTop,
    width: Math.max(rect.width, 280),
    height,
    borderRadius: style.borderRadius || "20px",
    squircle: supportsSquircleCorner(),
    isCardSurface: true,
  };
}

function clearTimers() {
  if (rafA !== null) {
    window.cancelAnimationFrame(rafA);
    rafA = null;
  }
  if (rafB !== null) {
    window.cancelAnimationFrame(rafB);
    rafB = null;
  }
  if (handoffRaf !== null) {
    window.cancelAnimationFrame(handoffRaf);
    handoffRaf = null;
  }
  if (revealTimer !== null) {
    window.clearTimeout(revealTimer);
    revealTimer = null;
  }
  if (cleanupTimer !== null) {
    window.clearTimeout(cleanupTimer);
    cleanupTimer = null;
  }
}

export function cancelActiveMorph() {
  clearTimers();
  activeMorph.value = null;
  hiddenDetailSheet.value = false;
  hiddenHomeArticleId.value = null;
  hiddenHomeOriginKind.value = null;
  isReturningToHome.value = false;
}

export function saveHomeSnapshot(snapshot: Omit<HomeFeedSnapshot, "savedAt">) {
  homeSnapshot = {
    ...snapshot,
    articles: snapshot.articles.map((item) => ({ ...item })),
    categories: snapshot.categories.map((cat) => ({ ...cat })),
    tags: snapshot.tags.map((tag) => ({ ...tag })),
    savedAt: Date.now(),
  };
}

export function getHomeReturnScrollY(expectedQueryKey?: string): number | null {
  if (!homeSnapshot) return null;
  if (Date.now() - homeSnapshot.savedAt > 10 * 60 * 1000) return null;
  if (expectedQueryKey !== undefined && homeSnapshot.queryKey !== expectedQueryKey) return null;
  return homeSnapshot.scrollY;
}

export function peekFreshHomeSnapshot(queryKey: string): HomeFeedSnapshot | null {
  if (!homeSnapshot) return null;
  if (Date.now() - homeSnapshot.savedAt > 10 * 60 * 1000) return null;
  if (homeSnapshot.queryKey !== queryKey) return null;
  return homeSnapshot;
}

export function cacheArticleDetail(detail: BlogArticleDetail) {
  articleDetailCache.set(detail.id, { ...detail });
}

export function getCachedArticleDetail(id: string): BlogArticleDetail | null {
  return articleDetailCache.get(id) ?? null;
}

export function prefetchArticleDetail(
  id: string,
  fetcher: () => Promise<BlogArticleDetail>,
): Promise<BlogArticleDetail> {
  const cached = articleDetailCache.get(id);
  if (cached) return Promise.resolve(cached);
  const inflight = articleInflightCache.get(id);
  if (inflight) return inflight;

  const task = fetcher()
    .then((detail) => {
      cacheArticleDetail(detail);
      return detail;
    })
    .finally(() => {
      articleInflightCache.delete(id);
    });
  articleInflightCache.set(id, task);
  return task;
}

export function prepareEnterMorph(
  preview: ArticlePreviewSeed,
  originKind: MorphOriginKind,
  sourceEl: HTMLElement,
  homeRootEl: HTMLElement | null,
) {
  if (prefersReducedMotion()) {
    pendingEnter.value = null;
    return;
  }
  cancelActiveMorph();
  pendingLeave.value = null;
  lastOriginKindByArticle.set(preview.id, originKind);

  const from = measureElementBox(sourceEl);
  const compactSnapshot = captureNodeSnapshot(sourceEl, 0);
  const underlaySnapshot = captureNodeSnapshot(homeRootEl, 0);
  if (underlaySnapshot && homeRootEl) {
    const safeId = CSS.escape(preview.id);
    const matched = underlaySnapshot.node.querySelector<HTMLElement>(
      `[data-morph-card="${originKind}"][data-article-id="${safeId}"]`,
    );
    if (matched) {
      matched.style.visibility = "hidden";
    }

    // 保留吸顶筛选条与固定侧栏在克隆首页中的真实视口偏移
    const liveDock = homeRootEl.querySelector<HTMLElement>(".filter-dock");
    const liveSentinel = homeRootEl.querySelector<HTMLElement>(".dock-sentinel");
    const clonedDock = underlaySnapshot.node.querySelector<HTMLElement>(".filter-dock");
    if (liveDock && liveSentinel && clonedDock) {
      const dockDelta = Math.max(0, liveDock.getBoundingClientRect().top - liveSentinel.getBoundingClientRect().top);
      if (dockDelta > 0.5) {
        clonedDock.style.transform = `translateY(${dockDelta}px)`;
      }
    }

    const liveSideStack = homeRootEl.querySelector<HTMLElement>(".side-fixed-stack");
    const liveSideCol = homeRootEl.querySelector<HTMLElement>(".side-col");
    const clonedSideStack = underlaySnapshot.node.querySelector<HTMLElement>(".side-fixed-stack");
    if (liveSideStack && liveSideCol && clonedSideStack) {
      const sideDelta = Math.max(0, liveSideStack.getBoundingClientRect().top - liveSideCol.getBoundingClientRect().top);
      if (sideDelta > 0.5) {
        clonedSideStack.style.transform = `translateY(${sideDelta}px)`;
      }
    }
  }

  pendingEnter.value = {
    articleId: preview.id,
    preview: { ...preview },
    originKind,
    from,
    compactSnapshot,
    underlaySnapshot,
  };
}

export function peekPendingEnter(articleId: string): PendingEnterState | null {
  if (!pendingEnter.value || pendingEnter.value.articleId !== articleId) return null;
  return pendingEnter.value;
}

export function launchEnterMorph(articleId: string, toEl: HTMLElement): boolean {
  const pending = pendingEnter.value;
  if (!pending || pending.articleId !== articleId || prefersReducedMotion()) {
    pendingEnter.value = null;
    hiddenDetailSheet.value = false;
    return false;
  }

  pendingEnter.value = null;
  clearTimers();

  if (window.scrollY > 1) {
    window.scrollTo({ top: 0, behavior: "instant" });
  }

  const toBox = measureVisibleSheetBox(toEl);
  const sheetSnapshot = captureNodeSnapshot(toEl, 0);
  hiddenDetailSheet.value = true;

  activeMorph.value = {
    key: ++morphSeq,
    phase: "enter",
    articleId,
    preview: pending.preview,
    from: pending.from,
    to: toBox,
    stage: "init",
    handingOff: false,
    compactSnapshot: pending.compactSnapshot,
    sheetSnapshot,
    underlaySnapshot: pending.underlaySnapshot,
  };

  rafA = window.requestAnimationFrame(() => {
    rafB = window.requestAnimationFrame(() => {
      if (activeMorph.value) {
        activeMorph.value = { ...activeMorph.value, stage: "run" };
      }
    });
  });

  revealTimer = window.setTimeout(() => {
    hiddenDetailSheet.value = false;
    handoffRaf = window.requestAnimationFrame(() => {
      handoffRaf = null;
      if (activeMorph.value) {
        activeMorph.value = { ...activeMorph.value, handingOff: true };
      }
    });
  }, MORPH_DURATION_MS);

  cleanupTimer = window.setTimeout(() => {
    activeMorph.value = null;
    hiddenDetailSheet.value = false;
  }, MORPH_DURATION_MS + 80);

  return true;
}

export function updateActiveEnterSheetSnapshot(toEl: HTMLElement) {
  if (!activeMorph.value || activeMorph.value.phase !== "enter") return;
  const sheetSnapshot = captureNodeSnapshot(toEl, 0);
  if (sheetSnapshot) {
    activeMorph.value = {
      ...activeMorph.value,
      sheetSnapshot,
    };
  }
}

export function prepareLeaveMorph(preview: ArticlePreviewSeed, fromEl: HTMLElement) {
  if (prefersReducedMotion()) {
    pendingLeave.value = null;
    return;
  }
  cancelActiveMorph();
  pendingEnter.value = null;

  const originKind = lastOriginKindByArticle.get(preview.id) ?? "feed";
  const rawRect = fromEl.getBoundingClientRect();
  const fromBox = measureVisibleSheetBox(fromEl);
  const offsetTop = rawRect.bottom < 84 ? 0 : rawRect.top - fromBox.top;

  pendingLeave.value = {
    articleId: preview.id,
    preview: { ...preview },
    originKind,
    from: fromBox,
    sheetSnapshot: captureNodeSnapshot(fromEl, offsetTop),
  };
}

export function launchLeaveMorph(
  resolveTarget: (articleId: string, preferredKind: MorphOriginKind) => {
    el: HTMLElement;
    kind: MorphOriginKind;
  } | null,
): boolean {
  const pending = pendingLeave.value;
  pendingLeave.value = null;

  if (!pending || prefersReducedMotion()) {
    isReturningToHome.value = false;
    return false;
  }

  const target = resolveTarget(pending.articleId, pending.originKind);
  if (!target) {
    isReturningToHome.value = false;
    return false;
  }

  const toBox = measureElementBox(target.el);
  if (
    toBox.width <= 0 ||
    toBox.height <= 0 ||
    toBox.top + toBox.height < 40 ||
    toBox.top > window.innerHeight + 140
  ) {
    isReturningToHome.value = false;
    return false;
  }

  clearTimers();
  const compactSnapshot = captureNodeSnapshot(target.el, 0);
  hiddenHomeArticleId.value = pending.articleId;
  hiddenHomeOriginKind.value = target.kind;
  isReturningToHome.value = true;

  activeMorph.value = {
    key: ++morphSeq,
    phase: "leave",
    articleId: pending.articleId,
    preview: pending.preview,
    from: pending.from,
    to: toBox,
    stage: "init",
    handingOff: false,
    compactSnapshot,
    sheetSnapshot: pending.sheetSnapshot,
    underlaySnapshot: null,
  };

  rafA = window.requestAnimationFrame(() => {
    rafB = window.requestAnimationFrame(() => {
      if (activeMorph.value) {
        activeMorph.value = { ...activeMorph.value, stage: "run" };
      }
    });
  });

  revealTimer = window.setTimeout(() => {
    hiddenHomeArticleId.value = null;
    hiddenHomeOriginKind.value = null;
    isReturningToHome.value = false;
    handoffRaf = window.requestAnimationFrame(() => {
      handoffRaf = null;
      if (activeMorph.value) {
        activeMorph.value = { ...activeMorph.value, handingOff: true };
      }
    });
  }, MORPH_DURATION_MS);

  cleanupTimer = window.setTimeout(() => {
    activeMorph.value = null;
    hiddenHomeArticleId.value = null;
    hiddenHomeOriginKind.value = null;
    isReturningToHome.value = false;
  }, MORPH_DURATION_MS + 80);

  return true;
}

export function useArticleTransition() {
  const shouldSkipRouteFade = computed(() =>
    Boolean(pendingEnter.value || pendingLeave.value || activeMorph.value),
  );

  return {
    activeMorph,
    hiddenHomeArticleId,
    hiddenHomeOriginKind,
    hiddenDetailSheet,
    isReturningToHome,
    shouldSkipRouteFade,
  };
}
