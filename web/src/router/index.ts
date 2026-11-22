import { createRouter, createWebHistory } from "vue-router";
import HomeView from "@/views/HomeView.vue";

const DEFAULT_TITLE = "MyBlog · 个人技术博客";
const DEFAULT_DESCRIPTION = "基于 Spring Boot + Vue + MySQL + Redis 构建的个人技术博客";

export function updatePageSeo(title?: string, description?: string) {
  document.title = title ? `${title} - MyBlog` : DEFAULT_TITLE;
  let meta = document.querySelector('meta[name="description"]') as HTMLMetaElement | null;
  if (!meta) {
    meta = document.createElement("meta");
    meta.name = "description";
    document.head.appendChild(meta);
  }
  meta.content = (description || DEFAULT_DESCRIPTION).slice(0, 180);
}

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) return savedPosition;
    return { top: 0 };
  },
  routes: [
    {
      path: "/",
      name: "Home",
      component: HomeView,
      meta: { title: "", description: DEFAULT_DESCRIPTION },
    },
    {
      path: "/article/:id",
      name: "ArticleDetail",
      component: () => import("@/views/ArticleDetailView.vue"),
      meta: { title: "文章详情", description: DEFAULT_DESCRIPTION },
    },
    {
      path: "/archives",
      name: "Archives",
      component: () => import("@/views/ArchivesView.vue"),
      meta: { title: "文章归档", description: "按时间线整理的个人博客文章归档列表" },
    },
    {
      path: "/links",
      name: "Links",
      component: () => import("@/views/LinksView.vue"),
      meta: { title: "友情链接", description: "博客友情链接与常用技术资源导航" },
    },
    {
      path: "/about",
      name: "About",
      component: () => import("@/views/AboutView.vue"),
      meta: { title: "关于本站", description: "关于个人技术博客与技术栈说明" },
    },
  ],
});

router.afterEach((to) => {
  const title = typeof to.meta?.title === "string" ? to.meta.title : "";
  const description = typeof to.meta?.description === "string" ? to.meta.description : DEFAULT_DESCRIPTION;
  updatePageSeo(title, description);
});

export default router;
