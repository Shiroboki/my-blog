import { createRouter, createWebHistory } from "vue-router";
import { getHomeReturnScrollY } from "@/composables/useArticleTransition";
import { updatePageSeo } from "@/utils/seo";
import HomeView from "@/views/HomeView.vue";

export { updatePageSeo };

const DEFAULT_DESCRIPTION = "基于 Spring Boot + Vue + MySQL + Redis 构建的个人技术博客";

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, savedPosition) {
    if (to.path === "/" && from.path.startsWith("/article/")) {
      const cachedScrollY = getHomeReturnScrollY(to.fullPath);
      if (cachedScrollY !== null) {
        return { top: cachedScrollY, behavior: "instant" };
      }
      if (savedPosition) {
        return { ...savedPosition, behavior: "instant" };
      }
      return { top: 0, behavior: "instant" };
    }
    if (to.path.startsWith("/article/")) {
      return { top: 0, behavior: "instant" };
    }
    if (savedPosition) return savedPosition;
    if (to.path === from.path) return false;
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
      path: "/paths/:idOrSlug?",
      name: "LearningPaths",
      component: () => import("@/views/LearningPathsView.vue"),
      meta: {
        title: "学习路径",
        description: "结合思维导图串联文章、视频教程与网页文档的系统化学习路线",
      },
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
    {
      path: "/:pathMatch(.*)*",
      name: "NotFound",
      component: () => import("@/views/NotFoundView.vue"),
      meta: { title: "页面不存在", description: "访问的页面不存在或内容已下线" },
    },
  ],
});

router.afterEach((to) => {
  const title = typeof to.meta?.title === "string" ? to.meta.title : "";
  const description =
    typeof to.meta?.description === "string" ? to.meta.description : DEFAULT_DESCRIPTION;
  updatePageSeo({
    title,
    description,
    path: to.path,
    type: to.name === "ArticleDetail" ? "article" : "website",
  });
});

export default router;
