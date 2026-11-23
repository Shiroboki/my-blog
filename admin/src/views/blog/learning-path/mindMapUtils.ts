import type {
  MindMapNode,
  MindMapResource,
  MindMapResourceType,
} from "@/api/blog";

export interface AdminLayoutNode {
  id: string;
  parentId: string | null;
  depth: number;
  branchIndex: number;
  node: MindMapNode;
  x: number;
  y: number;
  width: number;
  height: number;
  resourceCounts: {
    article: number;
    video: number;
    webpage: number;
  };
}

export interface AdminLayoutEdge {
  id: string;
  branchIndex: number;
  path: string;
}

export interface AdminMindMapLayout {
  nodes: AdminLayoutNode[];
  edges: AdminLayoutEdge[];
  width: number;
  height: number;
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
  rootDescription = "围绕主题拆解阶段节点，为每个节点挂载博客文章、视频课程与参考网页。",
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
        label: "基础准备",
        description: "建立核心概念并完成基础环境搭建。",
        tag: "入门",
        collapsed: false,
        resources: [],
        children: [],
      },
      {
        id: createUid("node"),
        label: "核心实践",
        description: "结合项目案例巩固关键技能。",
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

export function parseMindMapTree(treeJson?: string | null, fallbackTitle = "新学习路径"): MindMapNode {
  if (!treeJson || !treeJson.trim()) {
    return createEmptyMindMapTree(fallbackTitle);
  }
  try {
    return normalizeRawNode(JSON.parse(treeJson));
  } catch {
    return createEmptyMindMapTree(fallbackTitle);
  }
}

export function cloneMindMapTree(node: MindMapNode): MindMapNode {
  return normalizeRawNode(JSON.parse(JSON.stringify(node)));
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

function estimateNodeSize(node: MindMapNode, depth: number): { width: number; height: number } {
  const width = depth === 0 ? 204 : 188;
  let units = 0;
  for (const ch of node.label || "") {
    units += ch.charCodeAt(0) > 127 ? 2 : 1.08;
  }
  const maxUnitsPerLine = depth === 0 ? 20 : 18;
  const lines = Math.min(3, Math.max(1, Math.ceil(units / maxUnitsPerLine)));
  const hasFooter = Boolean(node.tag?.trim()) || node.resources.length > 0;
  const hasDesc = Boolean(node.description?.trim());
  const height = 22 + lines * 18 + (hasDesc ? 16 : 0) + (hasFooter ? 22 : 4);
  return { width, height: Math.max(54, Math.min(128, height)) };
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

const GAP_X = 56;
const GAP_Y = 16;
const PAD_X = 28;
const PAD_Y = 28;

function measureSubtree(node: MindMapNode, depth: number, branchIndex: number): MeasureTree {
  const { width, height } = estimateNodeSize(node, depth);
  const childTrees = node.children.map((child, idx) =>
    measureSubtree(child, depth + 1, depth === 0 ? idx : branchIndex),
  );

  let childrenTotalHeight = 0;
  for (let i = 0; i < childTrees.length; i++) {
    childrenTotalHeight += childTrees[i].subtreeHeight;
    if (i > 0) childrenTotalHeight += GAP_Y;
  }

  return {
    node,
    depth,
    branchIndex,
    width,
    height,
    subtreeHeight: Math.max(height, childrenTotalHeight),
    childrenTotalHeight,
    children: childTrees,
  };
}

export function computeMindMapLayout(root: MindMapNode): AdminMindMapLayout {
  const measured = measureSubtree(root, 0, -1);
  const nodes: AdminLayoutNode[] = [];
  const edges: AdminLayoutEdge[] = [];
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
    maxX = Math.max(maxX, x + item.width);
    maxY = Math.max(maxY, nodeY + item.height);

    nodes.push({
      id: item.node.id,
      parentId,
      depth: item.depth,
      branchIndex: item.branchIndex,
      node: item.node,
      x,
      y: nodeY,
      width: item.width,
      height: item.height,
      resourceCounts: summarizeResources(item.node.resources),
    });

    const leftAnchor = { x, y: nodeY + item.height / 2 };
    if (parentId && parentAnchor) {
      const dx = Math.max(20, (leftAnchor.x - parentAnchor.x) * 0.54);
      const path = `M ${parentAnchor.x} ${parentAnchor.y} C ${parentAnchor.x + dx} ${parentAnchor.y}, ${leftAnchor.x - dx} ${leftAnchor.y}, ${leftAnchor.x} ${leftAnchor.y}`;
      edges.push({
        id: `${parentId}__${item.node.id}`,
        branchIndex: item.branchIndex,
        path,
      });
    }

    if (!item.children.length) return;
    const rightAnchor = { x: x + item.width, y: nodeY + item.height / 2 };
    const childX = x + item.width + GAP_X;
    let cursorY = topY + Math.max(0, (item.subtreeHeight - item.childrenTotalHeight) / 2);

    for (const child of item.children) {
      place(child, item.node.id, rightAnchor, childX, cursorY);
      cursorY += child.subtreeHeight + GAP_Y;
    }
  }

  place(measured, null, null, PAD_X, PAD_Y);

  return {
    nodes,
    edges,
    width: Math.max(480, maxX + PAD_X),
    height: Math.max(300, maxY + PAD_Y),
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

export function countTreeStats(root: MindMapNode): {
  nodeCount: number;
  resourceCount: number;
} {
  let nodeCount = 1;
  let resourceCount = root.resources.length;
  for (const child of root.children) {
    const sub = countTreeStats(child);
    nodeCount += sub.nodeCount;
    resourceCount += sub.resourceCount;
  }
  return { nodeCount, resourceCount };
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
