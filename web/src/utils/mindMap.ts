export type MindMapResourceType = "article" | "video" | "webpage";

export interface MindMapResource {
  id: string;
  type: MindMapResourceType;
  title: string;
  articleId?: string;
  articleSlug?: string;
  url?: string;
  note?: string;
}

export interface MindMapNode {
  id: string;
  label: string;
  description?: string;
  tag?: string;
  collapsed?: boolean;
  resources: MindMapResource[];
  children: MindMapNode[];
}

export interface LayoutNode {
  id: string;
  parentId: string | null;
  depth: number;
  branchIndex: number;
  node: MindMapNode;
  x: number;
  y: number;
  width: number;
  height: number;
  hiddenChildCount: number;
  resourceCounts: {
    article: number;
    video: number;
    webpage: number;
  };
}

export interface LayoutEdge {
  id: string;
  fromId: string;
  toId: string;
  branchIndex: number;
  path: string;
}

export interface MindMapLayoutResult {
  nodes: LayoutNode[];
  edges: LayoutEdge[];
  width: number;
  height: number;
}

export interface ParsedVideoSource {
  platform: string;
  kind: "iframe" | "native" | "external";
  embedUrl?: string;
  externalUrl: string;
}

export const BRANCH_PALETTE = [
  "#0d9488",
  "#0284c7",
  "#4f46e5",
  "#d97706",
  "#db2777",
  "#059669",
];

export function getBranchColor(branchIndex: number, rootAccent = "#0d9488"): string {
  if (branchIndex < 0) return rootAccent;
  return BRANCH_PALETTE[branchIndex % BRANCH_PALETTE.length];
}

export function createUid(prefix = "id"): string {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`;
}

export function createEmptyMindMapTree(
  rootLabel = "新学习路径",
  rootDescription = "从核心节点出发逐步拆解知识分支，为每个节点挂载文章、视频与参考网页。",
): MindMapNode {
  return {
    id: createUid("node"),
    label: rootLabel,
    description: rootDescription,
    tag: "主线",
    collapsed: false,
    resources: [],
    children: [
      {
        id: createUid("node"),
        label: "前置基础",
        description: "掌握核心概念与入门环境搭建。",
        tag: "核心",
        collapsed: false,
        resources: [],
        children: [],
      },
      {
        id: createUid("node"),
        label: "进阶实战",
        description: "结合实际业务场景深入实践与调试。",
        tag: "实战",
        collapsed: false,
        resources: [],
        children: [],
      },
    ],
  };
}

function normalizeRawNode(raw: unknown): MindMapNode {
  const obj = (raw && typeof raw === "object" ? raw : {}) as Record<string, unknown>;
  const resourcesRaw = Array.isArray(obj.resources) ? obj.resources : [];
  const childrenRaw = Array.isArray(obj.children) ? obj.children : [];

  const resources: MindMapResource[] = resourcesRaw
    .filter((item): item is Record<string, unknown> => Boolean(item && typeof item === "object"))
    .map((item) => {
      const rawType = String(item.type || "webpage").toLowerCase();
      const type: MindMapResourceType =
        rawType === "article" || rawType === "video" || rawType === "webpage" ? rawType : "webpage";
      return {
        id: typeof item.id === "string" && item.id.trim() ? item.id.trim() : createUid("res"),
        type,
        title: typeof item.title === "string" ? item.title : "未命名资源",
        articleId: typeof item.articleId === "string" ? item.articleId : undefined,
        articleSlug: typeof item.articleSlug === "string" ? item.articleSlug : undefined,
        url: typeof item.url === "string" ? item.url : undefined,
        note: typeof item.note === "string" ? item.note : undefined,
      };
    });

  return {
    id: typeof obj.id === "string" && obj.id.trim() ? obj.id.trim() : createUid("node"),
    label: typeof obj.label === "string" && obj.label.trim() ? obj.label.trim() : "未命名节点",
    description: typeof obj.description === "string" ? obj.description : undefined,
    tag: typeof obj.tag === "string" ? obj.tag : undefined,
    collapsed: Boolean(obj.collapsed),
    resources,
    children: childrenRaw.map((child) => normalizeRawNode(child)),
  };
}

export function parseMindMapTree(treeJson?: string | null, fallbackTitle = "学习路径"): MindMapNode {
  if (!treeJson || !treeJson.trim()) {
    return createEmptyMindMapTree(fallbackTitle);
  }
  try {
    const parsed = JSON.parse(treeJson);
    return normalizeRawNode(parsed);
  } catch {
    return createEmptyMindMapTree(fallbackTitle);
  }
}

export function cloneMindMapTree(node: MindMapNode): MindMapNode {
  return normalizeRawNode(JSON.parse(JSON.stringify(node)));
}

function estimateNodeSize(node: MindMapNode, depth: number): { width: number; height: number } {
  const width = depth === 0 ? 228 : 210;
  let units = 0;
  for (const ch of node.label || "") {
    units += ch.charCodeAt(0) > 127 ? 2 : 1.08;
  }
  const maxUnitsPerLine = depth === 0 ? 23 : 21;
  const lines = Math.min(3, Math.max(1, Math.ceil(units / maxUnitsPerLine)));
  const hasFooter = Boolean(node.tag?.trim()) || node.resources.length > 0;
  const hasDesc = Boolean(node.description?.trim());
  const height =
    24 +
    lines * 20 +
    (hasDesc ? 18 : 0) +
    (hasFooter ? 26 : 4);
  return { width, height: Math.max(58, Math.min(138, height)) };
}

function countHiddenDescendants(node: MindMapNode): number {
  if (!node.children.length) return 0;
  let total = node.children.length;
  for (const child of node.children) {
    total += countHiddenDescendants(child);
  }
  return total;
}

function summarizeResources(resources: MindMapResource[]) {
  const counts = { article: 0, video: 0, webpage: 0 };
  for (const item of resources) {
    if (item.type === "article") counts.article += 1;
    else if (item.type === "video") counts.video += 1;
    else counts.webpage += 1;
  }
  return counts;
}

interface MeasureTree {
  node: MindMapNode;
  depth: number;
  branchIndex: number;
  width: number;
  height: number;
  subtreeHeight: number;
  childrenTotalHeight: number;
  children: MeasureTree[];
}

const LAYOUT_GAP_X = 72;
const LAYOUT_GAP_Y = 20;
const LAYOUT_PAD_X = 48;
const LAYOUT_PAD_Y = 48;

function measureSubtree(
  node: MindMapNode,
  depth: number,
  branchIndex: number,
): MeasureTree {
  const { width, height } = estimateNodeSize(node, depth);
  const visibleChildren = node.collapsed ? [] : node.children;
  const childTrees = visibleChildren.map((child, idx) =>
    measureSubtree(child, depth + 1, depth === 0 ? idx : branchIndex),
  );

  let childrenTotalHeight = 0;
  for (let i = 0; i < childTrees.length; i++) {
    childrenTotalHeight += childTrees[i].subtreeHeight;
    if (i > 0) childrenTotalHeight += LAYOUT_GAP_Y;
  }

  const subtreeHeight = Math.max(height, childrenTotalHeight);
  return {
    node,
    depth,
    branchIndex,
    width,
    height,
    subtreeHeight,
    childrenTotalHeight,
    children: childTrees,
  };
}

export function computeMindMapLayout(root: MindMapNode): MindMapLayoutResult {
  const measured = measureSubtree(root, 0, -1);
  const nodes: LayoutNode[] = [];
  const edges: LayoutEdge[] = [];
  let maxX = 0;
  let maxY = 0;

  function place(
    item: MeasureTree,
    parentId: string | null,
    parentAnchor: { x: number; y: number } | null,
    x: number,
    topY: number,
  ) {
    const nodeY = topY + (item.subtreeHeight - item.height) / 2;
    const nodeWidth = item.width;
    const nodeHeight = item.height;

    maxX = Math.max(maxX, x + nodeWidth);
    maxY = Math.max(maxY, nodeY + nodeHeight);

    nodes.push({
      id: item.node.id,
      parentId,
      depth: item.depth,
      branchIndex: item.branchIndex,
      node: item.node,
      x,
      y: nodeY,
      width: nodeWidth,
      height: nodeHeight,
      hiddenChildCount: item.node.collapsed ? countHiddenDescendants(item.node) : 0,
      resourceCounts: summarizeResources(item.node.resources),
    });

    const leftAnchor = { x, y: nodeY + nodeHeight / 2 };
    if (parentId && parentAnchor) {
      const dx = Math.max(24, (leftAnchor.x - parentAnchor.x) * 0.54);
      const path = `M ${parentAnchor.x} ${parentAnchor.y} C ${parentAnchor.x + dx} ${parentAnchor.y}, ${leftAnchor.x - dx} ${leftAnchor.y}, ${leftAnchor.x} ${leftAnchor.y}`;
      edges.push({
        id: `${parentId}__${item.node.id}`,
        fromId: parentId,
        toId: item.node.id,
        branchIndex: item.branchIndex,
        path,
      });
    }

    if (!item.children.length) return;
    const rightAnchor = { x: x + nodeWidth, y: nodeY + nodeHeight / 2 };
    const childX = x + nodeWidth + LAYOUT_GAP_X;
    let cursorY = topY + Math.max(0, (item.subtreeHeight - item.childrenTotalHeight) / 2);

    for (const child of item.children) {
      place(child, item.node.id, rightAnchor, childX, cursorY);
      cursorY += child.subtreeHeight + LAYOUT_GAP_Y;
    }
  }

  place(measured, null, null, LAYOUT_PAD_X, LAYOUT_PAD_Y);

  return {
    nodes,
    edges,
    width: Math.max(520, maxX + LAYOUT_PAD_X),
    height: Math.max(320, maxY + LAYOUT_PAD_Y),
  };
}

export function findNodeById(root: MindMapNode, id: string): MindMapNode | null {
  if (root.id === id) return root;
  for (const child of root.children) {
    const found = findNodeById(child, id);
    if (found) return found;
  }
  return null;
}

export function findNodeChain(root: MindMapNode, targetId: string): MindMapNode[] {
  if (root.id === targetId) return [root];
  for (const child of root.children) {
    const sub = findNodeChain(child, targetId);
    if (sub.length) {
      return [root, ...sub];
    }
  }
  return [];
}

export function findAncestorIds(root: MindMapNode, targetId: string): Set<string> {
  return new Set(findNodeChain(root, targetId).map((n) => n.id));
}

export function collectAllNodeIds(root: MindMapNode): string[] {
  const list = [root.id];
  for (const child of root.children) {
    list.push(...collectAllNodeIds(child));
  }
  return list;
}

export function countTreeStats(root: MindMapNode): {
  nodeCount: number;
  resourceCount: number;
  breakdown: { article: number; video: number; webpage: number };
} {
  let nodeCount = 1;
  const breakdown = summarizeResources(root.resources);
  let resourceCount = root.resources.length;

  for (const child of root.children) {
    const sub = countTreeStats(child);
    nodeCount += sub.nodeCount;
    resourceCount += sub.resourceCount;
    breakdown.article += sub.breakdown.article;
    breakdown.video += sub.breakdown.video;
    breakdown.webpage += sub.breakdown.webpage;
  }
  return { nodeCount, resourceCount, breakdown };
}

export function updateNodeById(
  root: MindMapNode,
  targetId: string,
  updater: (node: MindMapNode) => MindMapNode,
): MindMapNode {
  if (root.id === targetId) {
    return updater({ ...root, resources: [...root.resources], children: [...root.children] });
  }
  return {
    ...root,
    children: root.children.map((child) => updateNodeById(child, targetId, updater)),
  };
}

export function addChildNode(
  root: MindMapNode,
  parentId: string,
  newChild: MindMapNode,
): MindMapNode {
  return updateNodeById(root, parentId, (node) => ({
    ...node,
    collapsed: false,
    children: [...node.children, newChild],
  }));
}

export function addSiblingNode(
  root: MindMapNode,
  targetId: string,
  newSibling: MindMapNode,
): MindMapNode {
  if (root.id === targetId) {
    return addChildNode(root, root.id, newSibling);
  }
  const idx = root.children.findIndex((c) => c.id === targetId);
  if (idx >= 0) {
    const nextChildren = [...root.children];
    nextChildren.splice(idx + 1, 0, newSibling);
    return { ...root, children: nextChildren };
  }
  return {
    ...root,
    children: root.children.map((c) => addSiblingNode(c, targetId, newSibling)),
  };
}

export function moveNodeOrder(
  root: MindMapNode,
  targetId: string,
  direction: -1 | 1,
): MindMapNode {
  const idx = root.children.findIndex((c) => c.id === targetId);
  if (idx >= 0) {
    const swapIdx = idx + direction;
    if (swapIdx < 0 || swapIdx >= root.children.length) return root;
    const nextChildren = [...root.children];
    [nextChildren[idx], nextChildren[swapIdx]] = [nextChildren[swapIdx], nextChildren[idx]];
    return { ...root, children: nextChildren };
  }
  return {
    ...root,
    children: root.children.map((c) => moveNodeOrder(c, targetId, direction)),
  };
}

export function removeNodeById(root: MindMapNode, targetId: string): MindMapNode {
  if (root.id === targetId) return root;
  return {
    ...root,
    children: root.children
      .filter((c) => c.id !== targetId)
      .map((c) => removeNodeById(c, targetId)),
  };
}

export function toggleNodeCollapse(root: MindMapNode, targetId: string): MindMapNode {
  return updateNodeById(root, targetId, (node) => ({
    ...node,
    collapsed: !node.collapsed,
  }));
}

export function setAllCollapsed(root: MindMapNode, collapsed: boolean, depth = 0): MindMapNode {
  const shouldCollapse = collapsed ? depth >= 1 && root.children.length > 0 : false;
  return {
    ...root,
    collapsed: shouldCollapse,
    children: root.children.map((c) => setAllCollapsed(c, collapsed, depth + 1)),
  };
}

export function matchesNodeSearch(node: MindMapNode, query: string): boolean {
  const kw = query.trim().toLowerCase();
  if (!kw) return false;
  if (node.label.toLowerCase().includes(kw)) return true;
  if (node.description?.toLowerCase().includes(kw)) return true;
  if (node.tag?.toLowerCase().includes(kw)) return true;
  return node.resources.some(
    (res) =>
      res.title.toLowerCase().includes(kw) || Boolean(res.note?.toLowerCase().includes(kw)),
  );
}

export function collectMatchingNodeIds(root: MindMapNode, query: string): Set<string> {
  const matches = new Set<string>();
  const kw = query.trim();
  if (!kw) return matches;

  function walk(node: MindMapNode) {
    if (matchesNodeSearch(node, kw)) {
      matches.add(node.id);
    }
    for (const child of node.children) {
      walk(child);
    }
  }
  walk(root);
  return matches;
}

export function normalizeSafeExternalUrl(rawUrl?: string): string | null {
  if (!rawUrl || !rawUrl.trim()) return null;
  const trimmed = rawUrl.trim();
  if (/^https?:\/\//i.test(trimmed) || trimmed.startsWith("/uploads/")) {
    return trimmed;
  }
  if (trimmed.includes(":")) {
    return null;
  }
  return `https://${trimmed}`;
}

export function extractDomainHost(rawUrl?: string): string {
  if (!rawUrl) return "";
  try {
    return new URL(rawUrl, window.location.origin).hostname;
  } catch {
    return rawUrl;
  }
}

export function parseVideoEmbed(rawUrl?: string): ParsedVideoSource | null {
  const url = normalizeSafeExternalUrl(rawUrl);
  if (!url) return null;

  const bvMatch = url.match(/bilibili\.com\/video\/(BV[0-9A-Za-z]+)/i);
  if (bvMatch) {
    const bvid = bvMatch[1];
    return {
      platform: "Bilibili",
      kind: "iframe",
      embedUrl: `https://player.bilibili.com/player.html?bvid=${encodeURIComponent(bvid)}&page=1&high_quality=1&autoplay=0`,
      externalUrl: url,
    };
  }

  const avMatch = url.match(/bilibili\.com\/video\/av(\d+)/i);
  if (avMatch) {
    return {
      platform: "Bilibili",
      kind: "iframe",
      embedUrl: `https://player.bilibili.com/player.html?aid=${avMatch[1]}&page=1&high_quality=1&autoplay=0`,
      externalUrl: url,
    };
  }

  const ytWatchMatch = url.match(/(?:youtube\.com\/watch\?.*v=|youtu\.be\/|youtube\.com\/shorts\/)([A-Za-z0-9_-]{6,})/i);
  if (ytWatchMatch) {
    return {
      platform: "YouTube",
      kind: "iframe",
      embedUrl: `https://www.youtube.com/embed/${encodeURIComponent(ytWatchMatch[1])}`,
      externalUrl: url,
    };
  }

  const cleanPath = url.split("?")[0].toLowerCase();
  if (/\.(mp4|webm|ogg|m4v)$/.test(cleanPath) || url.startsWith("/uploads/")) {
    return {
      platform: "直链视频",
      kind: "native",
      embedUrl: url,
      externalUrl: url,
    };
  }

  return {
    platform: extractDomainHost(url) || "在线视频",
    kind: "external",
    externalUrl: url,
  };
}

const ADMIN_TOKEN_KEY = "vea:auth:access_token";
const ADMIN_REMEMBER_KEY = "vea:auth:remember_me";

function readAdminStoredString(storage: Storage, key: string): string {
  const raw = storage.getItem(key);
  if (!raw) return "";
  try {
    const parsed = JSON.parse(raw);
    return typeof parsed === "string" ? parsed : "";
  } catch {
    return raw;
  }
}

export function getAdminSessionToken(): string {
  if (typeof window === "undefined") return "";
  try {
    const rememberRaw = localStorage.getItem(ADMIN_REMEMBER_KEY);
    const remember = rememberRaw ? JSON.parse(rememberRaw) === true : false;
    if (remember) {
      return readAdminStoredString(localStorage, ADMIN_TOKEN_KEY);
    }
    return (
      readAdminStoredString(sessionStorage, ADMIN_TOKEN_KEY) ||
      readAdminStoredString(localStorage, ADMIN_TOKEN_KEY)
    );
  } catch {
    return "";
  }
}
