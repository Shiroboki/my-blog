import { ref } from "vue";
import { blogApi, type BlogSiteInfo } from "@/api/blog";
import { setSeoSiteConfig } from "@/utils/seo";

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
        setSeoSiteConfig(data.siteTitle, data.siteSubtitle);
        if (window.location.pathname === "/" && data.siteTitle) {
          document.title = data.siteTitle;
        } else if (data.siteTitle && document.title.endsWith(" - MyBlog")) {
          document.title = document.title.replace(/ - MyBlog$/, ` - ${data.siteTitle}`);
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
