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
        class="md-preview h-full overflow-y-auto border-t border-gray-200 bg-white p-4 text-sm leading-7 text-gray-800 md:border-l md:border-t-0 dark:border-gray-700 dark:bg-gray-900 dark:text-gray-100"
        v-html="renderedHtml"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import DOMPurify from "dompurify";
import hljs from "highlight.js";
import "highlight.js/styles/github-dark.css";
import { Marked, type Tokens } from "marked";
import FileAPI from "@/api/file";

interface Props {
  height?: string;
  placeholder?: string;
}

const props = withDefaults(defineProps<Props>(), {
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
      const language = lang && hljs.getLanguage(lang) ? lang : "plaintext";
      const highlighted = hljs.highlight(text, { language }).value;
      return `<pre><code class="hljs language-${language}">${highlighted}</code></pre>`;
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

<style scoped lang="scss">
.md-preview {
  :deep(h1),
  :deep(h2),
  :deep(h3) {
    margin: 16px 0 8px;
    font-weight: 600;
  }

  :deep(h1) {
    font-size: 1.5rem;
  }

  :deep(h2) {
    font-size: 1.25rem;
    border-bottom: 1px solid var(--el-border-color-lighter);
    padding-bottom: 4px;
  }

  :deep(p) {
    margin: 8px 0;
  }

  :deep(pre) {
    margin: 12px 0;
    padding: 12px;
    border-radius: 6px;
    overflow-x: auto;
    background: #0d1117;
  }

  :deep(code:not(.hljs)) {
    padding: 2px 6px;
    border-radius: 4px;
    background: var(--el-fill-color-light);
    font-size: 0.9em;
  }

  :deep(blockquote) {
    margin: 10px 0;
    padding: 6px 12px;
    border-left: 4px solid var(--el-color-primary);
    background: var(--el-fill-color-lighter);
  }

  :deep(ul),
  :deep(ol) {
    padding-left: 20px;
    margin: 8px 0;
  }

  :deep(img) {
    max-width: 100%;
    border-radius: 6px;
    margin: 8px 0;
  }
}
</style>
