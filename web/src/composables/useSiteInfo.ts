import { ref } from "vue";
import { blogApi, type BlogSiteInfo } from "@/api/blog";

const siteInfo = ref<BlogSiteInfo | null>(null);
const loading = ref(false);
let pendingPromise: Promise<BlogSiteInfo> | null = null;

export function useSiteInfo() {
  async function loadSiteInfo(force = false) {
    if (siteInfo.value && !force) return siteInfo.value;
    if (pendingPromise && !force) return pendingPromise;

    loading.value = true;
    pendingPromise = blogApi
      .getSiteInfo()
      .then((data) => {
        siteInfo.value = data;
        if (data.siteTitle && window.location.pathname === "/") {
          document.title = data.siteTitle;
        }
        return data;
      })
      .finally(() => {
        loading.value = false;
        pendingPromise = null;
      });
    return pendingPromise;
  }

  return {
    siteInfo,
    loading,
    loadSiteInfo,
  };
}
