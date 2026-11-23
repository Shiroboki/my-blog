<template>
  <div class="md-editor border border-gray-200 rounded-md overflow-hidden dark:border-gray-700">
    <div
      class="flex flex-wrap items-center justify-between gap-2 border-b border-gray-200 bg-gray-50 px-3 py-2 dark:border-gray-700 dark:bg-gray-800"
    >
      <div class="flex flex-wrap items-center gap-1.5">
        <el-button size="small" @click="insertWrap('## ', '', '二级标题')">H2</el-button>
        <el-button size="small" @click="insertWrap('**', '**', '加粗文字')">B</el-button>
        <el-button size="small" @click="insertWrap('`', '`', 'code')">行内代码</el-button>
        <el-button size="small" @click="insertWrap('\n```java\n', '\n```\n', '// 代码块')">
          代码块
        </el-button>
        <el-button size="small" @click="insertWrap('\n> ', '', '引用内容')">引用</el-button>
        <el-button size="small" @click="insertWrap('[', '](https://example.com)', '链接文字')">
          链接
        </el-button>
        <el-button size="small" :loading="uploading" @click="triggerImageUpload">
          上传图片
        </el-button>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          class="hidden"
          @change="handleFileSelect"
        />
      </div>

      <el-radio-group v-model="mode" size="small">
        <el-radio-button value="edit">编辑</el-radio-button>
        <el-radio-button value="split">分栏预览</el-radio-button>
        <el-radio-button value="preview">仅预览</el-radio-button>
      </el-radio-group>
    </div>

    <div
      class="grid"
      :class="mode === 'split' ? 'grid-cols-1 md:grid-cols-2' : 'grid-cols-1'"
      :style="{ height: height }"
    >
      <textarea
        v-show="mode !== 'preview'"
        ref="textareaRef"
        v-model="modelValue"
        class="h-full w-full resize-none border-0 bg-white p-4 font-mono text-sm leading-6 text-gray-800 outline-none dark:bg-gray-900 dark:text-gray-100"
        :placeholder="placeholder"
        @paste="handlePaste"
      />
      <div
        v-show="mode !== 'edit'"
        class="md-preview blog-md-preview h-full overflow-y-auto border-t border-gray-200 bg-white p-4 text-sm leading-7 text-gray-800 md:border-l md:border-t-0 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100"
        v-html="renderedHtml"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import DOMPurify from "dompurify";
import hljs from "highlight.js";
import { Marked, type Tokens } from "marked";
import FileAPI from "@/api/file";

interface Props {
  height?: string;
  placeholder?: string;
}

withDefaults(defineProps<Props>(), {
  height: "460px",
  placeholder: "请输入 Markdown 正文，支持拖拽/粘贴图片自动上传...",
});

const modelValue = defineModel<string>({ default: "" });
const mode = ref<"edit" | "split" | "preview">("split");
const uploading = ref(false);
const textareaRef = ref<HTMLTextAreaElement | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);

const markedInstance = new Marked({
  gfm: true,
  breaks: true,
  renderer: {
    code({ text, lang }: Tokens.Code) {
      const cleanLang = (lang || "").trim().toLowerCase().replace(/[^a-z0-9_+-]/g, "");
      const language = cleanLang && hljs.getLanguage(cleanLang) ? cleanLang : "plaintext";
      const highlighted = hljs.highlight(text, { language }).value;
      return `<div class="code-block"><div class="code-header"><span class="code-lang">${language}</span></div><pre><code class="hljs language-${language}">${highlighted}</code></pre></div>`;
    },
  },
});

const renderedHtml = computed(() => {
  const raw = markedInstance.parse(modelValue.value || "") as string;
  return DOMPurify.sanitize(raw);
});

function insertWrap(prefix: string, suffix: string, fallback: string) {
  const el = textareaRef.value;
  if (!el) {
    modelValue.value += `${prefix}${fallback}${suffix}`;
    return;
  }
  const start = el.selectionStart ?? modelValue.value.length;
  const end = el.selectionEnd ?? start;
  const selected = modelValue.value.slice(start, end) || fallback;
  const next =
    modelValue.value.slice(0, start) + prefix + selected + suffix + modelValue.value.slice(end);
  modelValue.value = next;
  nextTick(() => {
    el.focus();
    const cursorPos = start + prefix.length + selected.length;
    el.setSelectionRange(cursorPos, cursorPos);
  });
}

function triggerImageUpload() {
  fileInputRef.value?.click();
}

async function uploadAndInsert(file: File) {
  uploading.value = true;
  try {
    const info = await FileAPI.uploadFile(file);
    insertWrap(`![${info.name || "image"}](${info.url})`, "", "");
    ElMessage.success("图片上传成功");
  } finally {
    uploading.value = false;
  }
}

async function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  await uploadAndInsert(file);
  input.value = "";
}

async function handlePaste(event: ClipboardEvent) {
  const items = event.clipboardData?.items;
  if (!items) return;
  for (const item of items) {
    if (item.type.startsWith("image/")) {
      const file = item.getAsFile();
      if (file) {
        event.preventDefault();
        await uploadAndInsert(file);
        return;
      }
    }
  }
}
</script>

<style lang="scss">
.blog-md-preview {
  --md-code-bg: #f6f8fa;
  --md-code-border: rgb(15 23 42 / 10%);
  --md-code-divider: rgb(15 23 42 / 8%);
  --md-code-lang: #64748b;
  --md-hljs-fg: #24292e;
  --md-hljs-keyword: #d73a49;
  --md-hljs-entity: #6f42c1;
  --md-hljs-constant: #005cc5;
  --md-hljs-string: #032f62;
  --md-hljs-variable: #e36209;
  --md-hljs-comment: #6a737d;
  --md-hljs-tag: #22863a;
}

html.dark .blog-md-preview {
  --md-code-bg: #0d111a;
  --md-code-border: rgb(255 255 255 / 10%);
  --md-code-divider: rgb(255 255 255 / 8%);
  --md-code-lang: #8b9bb4;
  --md-hljs-fg: #c9d1d9;
  --md-hljs-keyword: #ff7b72;
  --md-hljs-entity: #d2a8ff;
  --md-hljs-constant: #79c0ff;
  --md-hljs-string: #a5d6ff;
  --md-hljs-variable: #ffa657;
  --md-hljs-comment: #8b949e;
  --md-hljs-tag: #7ee787;
}

.blog-md-preview h1,
.blog-md-preview h2,
.blog-md-preview h3 {
  margin: 16px 0 8px;
  font-weight: 600;
}

.blog-md-preview h1 {
  font-size: 1.5rem;
}

.blog-md-preview h2 {
  padding-bottom: 4px;
  font-size: 1.25rem;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.blog-md-preview p {
  margin: 8px 0;
}

.blog-md-preview .code-block {
  margin: 12px 0;
  overflow: hidden;
  background: var(--md-code-bg);
  border: 1px solid var(--md-code-border);
  border-radius: 10px;
}

.blog-md-preview .code-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  font-size: 11.5px;
  color: var(--md-code-lang);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid var(--md-code-divider);
}

.blog-md-preview pre {
  padding: 12px 14px;
  margin: 0;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.65;
  color: var(--md-hljs-fg);
  background: transparent;
}

.blog-md-preview code.hljs {
  color: var(--md-hljs-fg);
  background: transparent;
}

.blog-md-preview .hljs-comment,
.blog-md-preview .hljs-quote {
  color: var(--md-hljs-comment);
}

.blog-md-preview .hljs-keyword,
.blog-md-preview .hljs-selector-tag,
.blog-md-preview .hljs-type,
.blog-md-preview .hljs-doctag {
  color: var(--md-hljs-keyword);
}

.blog-md-preview .hljs-title,
.blog-md-preview .hljs-title\.class_,
.blog-md-preview .hljs-title\.function_ {
  color: var(--md-hljs-entity);
}

.blog-md-preview .hljs-attr,
.blog-md-preview .hljs-attribute,
.blog-md-preview .hljs-number,
.blog-md-preview .hljs-literal,
.blog-md-preview .hljs-meta {
  color: var(--md-hljs-constant);
}

.blog-md-preview .hljs-string,
.blog-md-preview .hljs-regexp {
  color: var(--md-hljs-string);
}

.blog-md-preview .hljs-built_in,
.blog-md-preview .hljs-variable,
.blog-md-preview .hljs-symbol {
  color: var(--md-hljs-variable);
}

.blog-md-preview .hljs-name,
.blog-md-preview .hljs-selector-class {
  color: var(--md-hljs-tag);
}

.blog-md-preview code:not(.hljs) {
  padding: 2px 6px;
  font-size: 0.9em;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.blog-md-preview blockquote {
  padding: 6px 12px;
  margin: 10px 0;
  background: var(--el-fill-color-lighter);
  border-left: 4px solid var(--el-color-primary);
}

.blog-md-preview ul,
.blog-md-preview ol {
  padding-left: 20px;
  margin: 8px 0;
}

.blog-md-preview img {
  max-width: 100%;
  margin: 8px 0;
  border-radius: 6px;
}
</style>
