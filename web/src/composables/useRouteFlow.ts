import { computed, ref } from "vue";
import type { RouteLocationNormalizedLoaded } from "vue-router";
import {
  blogApi,
  type BlogArchiveGroup,
  type BlogArticleItem,
  type BlogLearningPathItem,
  type BlogLinkItem,
} from "@/api/blog";
import { prefersReducedMotion, useArticleTransition } from "@/composables/useArticleTransition";
import { useSiteInfo } from "@/composables/useSiteInfo";

const NAV_TAB_ORDER: Record<string, number> = {
  Home: 0,
  Archives: 1,
  LearningPaths: 2,
  Links: 3,
  About: 4,
};

const CACHE_TTL_MS = 5 * 60 * 1000;

interface CachedEntry<T> {
  data: T;
  savedAt: number;
}

const routeTransitionName = ref("route-flow-fade");
const leavingScrollY = ref(0);

let archivesCache: CachedEntry<BlogArchiveGroup[]> | null = null;
let archivesInflight: Promise<BlogArchiveGroup[]> | null = null;

let learningPathsCache: CachedEntry<{
  paths: BlogLearningPathItem[];
  articles: BlogArticleItem[];
}> | null = null;
let learningPathsInflight: Promise<{
  paths: BlogLearningPathItem[];
  articles: BlogArticleItem[];
}> | null = null;

let linksCache: CachedEntry<BlogLinkItem[]> | null = null;
let linksInflight: Promise<BlogLinkItem[]> | null = null;

const prefetchedChunks = new Set<string>();
let idlePrefetchScheduled = false;

export function getNavTabOrder(name: unknown): number {
  if (typeof name !== "string") return -1;
  return NAV_TAB_ORDER[name] ?? -1;
}

export function getRouteViewKey(route: RouteLocationNormalizedLoaded): string {
  if (route.name === "LearningPaths") {
    return "LearningPaths";
  }
  if (route.name === "ArticleDetail") {
    return `ArticleDetail:${route.fullPath}`;
  }
  return String(route.name || route.path);
}

export function updateRouteFlowOnNavigate(
  to: RouteLocationNormalizedLoaded,
  from: RouteLocationNormalizedLoaded,
) {
  const { shouldSkipRouteFade } = useArticleTransition();
  leavingScrollY.value = typeof window !== "undefined" ? window.scrollY : 0;

  if (prefersReducedMotion() || shouldSkipRouteFade.value) {
    routeTransitionName.value = "morph-instant";
    return;
  }

  const fromOrder = getNavTabOrder(from.name);
  const toOrder = getNavTabOrder(to.name);

  if (fromOrder >= 0 && toOrder >= 0 && fromOrder !== toOrder) {
    routeTransitionName.value = toOrder > fromOrder ? "route-flow-forward" : "route-flow-backward";
    return;
  }

  if (to.name === "ArticleDetail" && from.name !== "ArticleDetail") {
    routeTransitionName.value = "route-drill-in";
    return;
  }

  if (from.name === "ArticleDetail" && to.name !== "ArticleDetail") {
    routeTransitionName.value = "route-drill-out";
    return;
  }

  if (to.name === "ArticleDetail" && from.name === "ArticleDetail") {
    routeTransitionName.value = "route-flow-forward";
    return;
  }

  routeTransitionName.value = "route-flow-fade";
}

export function consumeLeavingScrollY(): number {
  const y = leavingScrollY.value;
  leavingScrollY.value = 0;
  return y;
}

export function peekCachedArchives(): BlogArchiveGroup[] | null {
  if (!archivesCache) return null;
  if (Date.now() - archivesCache.savedAt > CACHE_TTL_MS) return null;
  return archivesCache.data;
}

export async function ensureArchives(forceRefresh = false): Promise<BlogArchiveGroup[]> {
  if (!forceRefresh) {
    const cached = peekCachedArchives();
    if (cached) return cached;
  }
  if (archivesInflight) return archivesInflight;
  archivesInflight = blogApi
    .getArchives()
    .then((data) => {
      archivesCache = { data: data || [], savedAt: Date.now() };
      return archivesCache.data;
    })
    .finally(() => {
      archivesInflight = null;
    });
  return archivesInflight;
}

export function peekCachedLearningPaths(): {
  paths: BlogLearningPathItem[];
  articles: BlogArticleItem[];
} | null {
  if (!learningPathsCache) return null;
  if (Date.now() - learningPathsCache.savedAt > CACHE_TTL_MS) return null;
  return learningPathsCache.data;
}

export async function ensureLearningPaths(forceRefresh = false): Promise<{
  paths: BlogLearningPathItem[];
  articles: BlogArticleItem[];
}> {
  if (!forceRefresh) {
    const cached = peekCachedLearningPaths();
    if (cached) return cached;
  }
  if (learningPathsInflight) return learningPathsInflight;
  learningPathsInflight = Promise.all([
    blogApi.getLearningPaths(),
    blogApi.getArticles({ pageNum: 1, pageSize: 100 }),
  ])
    .then(([pathsRes, articlesRes]) => {
      learningPathsCache = {
        data: {
          paths: pathsRes || [],
          articles: articlesRes?.list ?? [],
        },
        savedAt: Date.now(),
      };
      return learningPathsCache.data;
    })
    .finally(() => {
      learningPathsInflight = null;
    });
  return learningPathsInflight;
}

export function peekCachedLinks(): BlogLinkItem[] | null {
  if (!linksCache) return null;
  if (Date.now() - linksCache.savedAt > CACHE_TTL_MS) return null;
  return linksCache.data;
}

export async function ensureLinks(forceRefresh = false): Promise<BlogLinkItem[]> {
  if (!forceRefresh) {
    const cached = peekCachedLinks();
    if (cached) return cached;
  }
  if (linksInflight) return linksInflight;
  linksInflight = blogApi
    .getLinks()
    .then((data) => {
      linksCache = { data: data || [], savedAt: Date.now() };
      return linksCache.data;
    })
    .finally(() => {
      linksInflight = null;
    });
  return linksInflight;
}

function warmRouteChunk(routeName: string) {
  if (prefetchedChunks.has(routeName)) return;
  prefetchedChunks.add(routeName);
  if (routeName === "Archives") {
    void import("@/views/ArchivesView.vue");
  } else if (routeName === "LearningPaths") {
    void import("@/views/LearningPathsView.vue");
  } else if (routeName === "Links") {
    void import("@/views/LinksView.vue");
  } else if (routeName === "About") {
    void import("@/views/AboutView.vue");
  }
}

export function prefetchNavTarget(routeName: string) {
  warmRouteChunk(routeName);
  if (routeName === "Archives") {
    void ensureArchives();
  } else if (routeName === "LearningPaths") {
    void ensureLearningPaths();
  } else if (routeName === "Links") {
    void ensureLinks();
  } else if (routeName === "About") {
    void useSiteInfo().loadSiteInfo();
  }
}

export function scheduleIdleRoutePrefetch() {
  if (idlePrefetchScheduled || typeof window === "undefined") return;
  idlePrefetchScheduled = true;
  const run = () => {
    warmRouteChunk("Archives");
    warmRouteChunk("LearningPaths");
    warmRouteChunk("Links");
    warmRouteChunk("About");
  };
  if (typeof window.requestIdleCallback === "function") {
    window.requestIdleCallback(run, { timeout: 1800 });
  } else {
    globalThis.setTimeout(run, 1000);
  }
}

export function useRouteFlow() {
  return {
    routeTransitionName: computed(() => routeTransitionName.value),
  };
}
