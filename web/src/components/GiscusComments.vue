<template>
  <div class="giscus-wrapper">
    <div ref="containerRef" class="giscus-host" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from "vue";

const props = withDefaults(
  defineProps<{
    repo: string;
    repoId: string;
    category?: string;
    categoryId: string;
    mapping?: string;
    term?: string;
  }>(),
  {
    category: "",
    mapping: "pathname",
    term: "",
  },
);

const containerRef = ref<HTMLElement | null>(null);
let themeObserver: MutationObserver | null = null;

function resolveTheme() {
  return document.documentElement.classList.contains("dark") ? "dark_dimmed" : "light";
}

function sendGiscusTheme() {
  const iframe = containerRef.value?.querySelector<HTMLIFrameElement>("iframe.giscus-frame");
  if (!iframe?.contentWindow) return;
  iframe.contentWindow.postMessage(
    {
      giscus: {
        setConfig: {
          theme: resolveTheme(),
        },
      },
    },
    "https://giscus.app",
  );
}

function mountGiscus() {
  const host = containerRef.value;
  if (!host || !props.repo || !props.repoId || !props.categoryId) return;
  host.innerHTML = "";

  const script = document.createElement("script");
  script.src = "https://giscus.app/client.js";
  script.async = true;
  script.crossOrigin = "anonymous";
  script.setAttribute("data-repo", props.repo);
  script.setAttribute("data-repo-id", props.repoId);
  if (props.category) {
    script.setAttribute("data-category", props.category);
  }
  script.setAttribute("data-category-id", props.categoryId);
  script.setAttribute("data-mapping", props.mapping || "pathname");
  if (props.mapping === "specific" && props.term) {
    script.setAttribute("data-term", props.term);
  }
  script.setAttribute("data-strict", "0");
  script.setAttribute("data-reactions-enabled", "1");
  script.setAttribute("data-emit-metadata", "0");
  script.setAttribute("data-input-position", "top");
  script.setAttribute("data-theme", resolveTheme());
  script.setAttribute("data-lang", "zh-CN");
  script.setAttribute("data-loading", "lazy");

  host.appendChild(script);
}

watch(
  () => [props.repo, props.repoId, props.category, props.categoryId, props.mapping, props.term],
  () => {
    mountGiscus();
  },
);

onMounted(() => {
  mountGiscus();
  themeObserver = new MutationObserver(() => {
    sendGiscusTheme();
  });
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ["class"],
  });
});

onUnmounted(() => {
  themeObserver?.disconnect();
});
</script>

<style scoped>
.giscus-wrapper {
  width: 100%;
  min-height: 160px;
}

.giscus-host {
  width: 100%;
}
</style>
