import type { BlogComment } from "@/api/blog";

export interface SelectionAnchor {
  quoteText: string;
  quoteStart: number;
  quoteEnd: number;
  tooLong?: boolean;
}

export interface AnnotationThread {
  comment: BlogComment;
  start: number;
  end: number;
  quoteText: string;
}

interface TextNodeEntry {
  node: Text;
  start: number;
  end: number;
}

const MAX_QUOTE_LENGTH = 500;

function isIgnoredElement(el: Element | null): boolean {
  return Boolean(el?.closest(".code-header, .inline-annotation-ignore"));
}

function collectMarkdownTextNodes(root: HTMLElement): {
  nodes: TextNodeEntry[];
  fullText: string;
} {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
    acceptNode(node) {
      if (!node.nodeValue) return NodeFilter.FILTER_REJECT;
      if (isIgnoredElement(node.parentElement)) return NodeFilter.FILTER_REJECT;
      return NodeFilter.FILTER_ACCEPT;
    },
  });

  const nodes: TextNodeEntry[] = [];
  const parts: string[] = [];
  let cursor = 0;

  while (walker.nextNode()) {
    const textNode = walker.currentNode as Text;
    const len = textNode.length;
    if (len === 0) continue;
    nodes.push({
      node: textNode,
      start: cursor,
      end: cursor + len,
    });
    parts.push(textNode.nodeValue || "");
    cursor += len;
  }

  return { nodes, fullText: parts.join("") };
}

export function extractSelectionAnchor(root: HTMLElement, range: Range): SelectionAnchor | null {
  if (range.collapsed) return null;
  if (!root.contains(range.commonAncestorContainer)) return null;

  const { nodes, fullText } = collectMarkdownTextNodes(root);
  if (!nodes.length) return null;

  let absStart = Number.POSITIVE_INFINITY;
  let absEnd = -1;

  for (const entry of nodes) {
    const nodeRange = document.createRange();
    nodeRange.selectNodeContents(entry.node);
    try {
      if (range.compareBoundaryPoints(Range.END_TO_START, nodeRange) >= 0) {
        continue;
      }
      if (range.compareBoundaryPoints(Range.START_TO_END, nodeRange) <= 0) {
        break;
      }

      const localStart =
        range.compareBoundaryPoints(Range.START_TO_START, nodeRange) <= 0
          ? 0
          : range.startContainer === entry.node
            ? range.startOffset
            : 0;
      const localEnd =
        range.compareBoundaryPoints(Range.END_TO_END, nodeRange) >= 0
          ? entry.node.length
          : range.endContainer === entry.node
            ? range.endOffset
            : entry.node.length;

      if (localEnd > localStart) {
        absStart = Math.min(absStart, entry.start + localStart);
        absEnd = Math.max(absEnd, entry.start + localEnd);
      }
    } catch {
      return null;
    }
  }

  if (!Number.isFinite(absStart) || absEnd <= absStart) {
    return null;
  }

  const raw = fullText.slice(absStart, absEnd);
  if (!raw.trim()) {
    return null;
  }

  const leadingWs = raw.length - raw.trimStart().length;
  const trailingWs = raw.length - raw.trimEnd().length;
  const quoteStart = absStart + leadingWs;
  const quoteEnd = absEnd - trailingWs;
  const quoteText = fullText.slice(quoteStart, quoteEnd);

  if (!quoteText) return null;
  return {
    quoteText,
    quoteStart,
    quoteEnd,
    tooLong: quoteText.length > MAX_QUOTE_LENGTH,
  };
}

export function unwrapInlineAnnotations(root: HTMLElement): void {
  const marks = root.querySelectorAll("mark.inline-quote-mark");
  marks.forEach((mark) => {
    const parent = mark.parentNode;
    if (!parent) return;
    while (mark.firstChild) {
      parent.insertBefore(mark.firstChild, mark);
    }
    parent.removeChild(mark);
  });
  root.normalize();
}

export function applyInlineAnnotations(
  root: HTMLElement,
  comments: BlogComment[],
): AnnotationThread[] {
  unwrapInlineAnnotations(root);
  const { nodes, fullText } = collectMarkdownTextNodes(root);
  if (!nodes.length || !comments.length) return [];

  const resolved: AnnotationThread[] = [];
  for (const comment of comments) {
    const quote = comment.quoteText?.trim();
    if (!quote) continue;

    let start = -1;
    let end = -1;
    if (
      typeof comment.quoteStart === "number" &&
      typeof comment.quoteEnd === "number" &&
      comment.quoteStart >= 0 &&
      comment.quoteEnd > comment.quoteStart &&
      comment.quoteEnd <= fullText.length &&
      fullText.slice(comment.quoteStart, comment.quoteEnd) === quote
    ) {
      start = comment.quoteStart;
      end = comment.quoteEnd;
    } else {
      const idx = fullText.indexOf(quote);
      if (idx >= 0) {
        start = idx;
        end = idx + quote.length;
      }
    }

    if (start >= 0 && end > start) {
      resolved.push({ comment, start, end, quoteText: quote });
    }
  }

  if (!resolved.length) return [];

  const boundaries = Array.from(
    new Set(resolved.flatMap((item) => [item.start, item.end])),
  ).sort((a, b) => a - b);

  interface Segment {
    start: number;
    end: number;
    commentIds: string[];
  }

  const segments: Segment[] = [];
  for (let i = 0; i < boundaries.length - 1; i++) {
    const s = boundaries[i];
    const e = boundaries[i + 1];
    if (e <= s) continue;
    const ids = resolved
      .filter((item) => item.start <= s && item.end >= e)
      .map((item) => String(item.comment.id));
    if (ids.length) {
      segments.push({ start: s, end: e, commentIds: ids });
    }
  }

  for (let i = segments.length - 1; i >= 0; i--) {
    const seg = segments[i];
    for (let j = nodes.length - 1; j >= 0; j--) {
      const entry = nodes[j];
      if (entry.end <= seg.start || entry.start >= seg.end) continue;

      const localStart = Math.max(seg.start, entry.start) - entry.start;
      const localEnd = Math.min(seg.end, entry.end) - entry.start;
      if (localEnd <= localStart) continue;

      if (localEnd < entry.node.length) {
        entry.node.splitText(localEnd);
      }

      let targetNode: Text;
      if (localStart > 0) {
        targetNode = entry.node.splitText(localStart);
        entry.end = entry.start + localStart;
      } else {
        targetNode = entry.node;
        entry.end = entry.start;
      }

      const mark = document.createElement("mark");
      mark.className = "inline-quote-mark";
      mark.dataset.commentIds = seg.commentIds.join(",");
      mark.dataset.primaryId = seg.commentIds[0];
      mark.title = `查看 ${seg.commentIds.length} 条划词评论`;

      const parent = targetNode.parentNode;
      if (parent) {
        parent.replaceChild(mark, targetNode);
        mark.appendChild(targetNode);
      }
    }
  }

  return resolved;
}
