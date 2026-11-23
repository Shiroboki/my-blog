import DOMPurify from "dompurify";
import hljs from "highlight.js/lib/core";
import bash from "highlight.js/lib/languages/bash";
import css from "highlight.js/lib/languages/css";
import dockerfile from "highlight.js/lib/languages/dockerfile";
import go from "highlight.js/lib/languages/go";
import java from "highlight.js/lib/languages/java";
import javascript from "highlight.js/lib/languages/javascript";
import json from "highlight.js/lib/languages/json";
import markdownLang from "highlight.js/lib/languages/markdown";
import nginx from "highlight.js/lib/languages/nginx";
import plaintext from "highlight.js/lib/languages/plaintext";
import python from "highlight.js/lib/languages/python";
import scss from "highlight.js/lib/languages/scss";
import sql from "highlight.js/lib/languages/sql";
import typescript from "highlight.js/lib/languages/typescript";
import xml from "highlight.js/lib/languages/xml";
import yaml from "highlight.js/lib/languages/yaml";
import { Marked, type Tokens } from "marked";

hljs.registerLanguage("bash", bash);
hljs.registerLanguage("css", css);
hljs.registerLanguage("dockerfile", dockerfile);
hljs.registerLanguage("go", go);
hljs.registerLanguage("java", java);
hljs.registerLanguage("javascript", javascript);
hljs.registerLanguage("json", json);
hljs.registerLanguage("markdown", markdownLang);
hljs.registerLanguage("nginx", nginx);
hljs.registerLanguage("plaintext", plaintext);
hljs.registerLanguage("python", python);
hljs.registerLanguage("scss", scss);
hljs.registerLanguage("sql", sql);
hljs.registerLanguage("typescript", typescript);
hljs.registerLanguage("xml", xml);
hljs.registerLanguage("yaml", yaml);

export interface TocItem {
  id: string;
  text: string;
  level: number;
}

export interface RenderedMarkdown {
  html: string;
  toc: TocItem[];
}

export interface RenderMarkdownOptions {
  stripLeadingTitle?: string;
}

function removeDuplicateLeadingHeading(markdown: string, articleTitle?: string): string {
  const source = markdown || "";
  if (!articleTitle?.trim()) return source;
  const trimmedTitle = articleTitle.trim().toLowerCase();
  const lines = source.split(/\r?\n/);
  let firstNonEmptyIdx = -1;
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].trim()) {
      firstNonEmptyIdx = i;
      break;
    }
  }
  if (firstNonEmptyIdx === -1) return source;
  const match = lines[firstNonEmptyIdx].match(/^#\s+(.+?)\s*#*$/);
  if (match && match[1].trim().toLowerCase() === trimmedTitle) {
    lines.splice(firstNonEmptyIdx, 1);
    while (lines[firstNonEmptyIdx] !== undefined && !lines[firstNonEmptyIdx].trim()) {
      lines.splice(firstNonEmptyIdx, 1);
    }
    return lines.join("\n");
  }
  return source;
}

function escapeAttr(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

export function renderMarkdown(
  markdown: string,
  options?: RenderMarkdownOptions,
): RenderedMarkdown {
  const toc: TocItem[] = [];
  let headingSeq = 0;
  const normalizedSource = removeDuplicateLeadingHeading(markdown, options?.stripLeadingTitle);

  const marked = new Marked({
    gfm: true,
    breaks: true,
    renderer: {
      code({ text, lang }: Tokens.Code) {
        const cleanLang = (lang || "").trim().toLowerCase().replace(/[^a-z0-9_+-]/g, "");
        const language = cleanLang && hljs.getLanguage(cleanLang) ? cleanLang : "plaintext";
        const highlighted = hljs.highlight(text, { language }).value;
        return `<div class="code-block"><div class="code-header"><span class="code-lang">${language}</span><button type="button" class="code-copy-btn">复制代码</button></div><pre><code class="hljs language-${language}">${highlighted}</code></pre></div>`;
      },
      heading({ tokens, depth }: Tokens.Heading) {
        const text = this.parser.parseInline(tokens);
        const plain = text.replace(/<[^>]+>/g, "").trim();
        const id = `heading-${++headingSeq}`;
        if (depth >= 1 && depth <= 3 && plain) {
          toc.push({ id, text: plain, level: depth });
        }
        return `<h${depth} id="${id}">${text}</h${depth}>`;
      },
      link({ href, title, tokens }: Tokens.Link) {
        const text = this.parser.parseInline(tokens);
        const rawHref = href || "#";
        const isExternal = /^https?:\/\//i.test(rawHref);
        const titleAttr = title ? ` title="${escapeAttr(title)}"` : "";
        const relAttr = isExternal ? ' target="_blank" rel="noopener noreferrer"' : "";
        return `<a href="${escapeAttr(rawHref)}"${titleAttr}${relAttr}>${text}</a>`;
      },
    },
  });

  const raw = marked.parse(normalizedSource) as string;
  return {
    html: DOMPurify.sanitize(raw, {
      ADD_TAGS: ["button"],
      ADD_ATTR: ["type", "target", "rel"],
    }),
    toc,
  };
}
