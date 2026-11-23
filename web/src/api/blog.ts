import axios from "axios";

interface ApiResult<T = unknown> {
  code: string;
  data: T;
  msg: string;
}

const http = axios.create({
  baseURL: "/api/v1/blog/public",
  timeout: 15000,
});

http.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResult;
    if (res.code === "00000") {
      return res.data as never;
    }
    return Promise.reject(new Error(res.msg || "请求失败"));
  },
  (error) => {
    const msg = error?.response?.data?.msg || error?.message || "网络异常";
    return Promise.reject(new Error(msg));
  },
);

export interface BlogTagRef {
  id: string;
  name: string;
  slug: string;
  color?: string;
}

export interface BlogArticleItem {
  id: string;
  title: string;
  slug?: string;
  summary?: string;
  coverImage?: string;
  categoryId?: string;
  categoryName?: string;
  isTop: number;
  allowComment: number;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  wordCount?: number;
  readingMinutes?: number;
  publishTime?: string;
  createTime?: string;
  tags?: BlogTagRef[];
}

export interface BlogLinkItem {
  id: string;
  name: string;
  url: string;
  avatar?: string;
  description?: string;
  sort?: number;
  status?: number;
  createTime?: string;
}

export interface BlogLearningPathItem {
  id: string;
  title: string;
  slug?: string;
  summary?: string;
  coverColor?: string;
  treeJson: string;
  nodeCount: number;
  resourceCount: number;
  sort?: number;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface BlogLearningPathSavePayload {
  id?: string;
  title: string;
  slug?: string;
  summary?: string;
  coverColor?: string;
  treeJson: string;
  sort?: number;
  status?: number;
}

export interface BlogArticleNeighbor {
  id: string;
  title: string;
  slug?: string;
}

export interface BlogArticleDetail extends BlogArticleItem {
  content: string;
  prevArticle?: BlogArticleNeighbor | null;
  nextArticle?: BlogArticleNeighbor | null;
}

export interface BlogCategory {
  id: string;
  name: string;
  slug: string;
  description?: string;
  articleCount: number;
}

export interface BlogTag {
  id: string;
  name: string;
  slug: string;
  color?: string;
  articleCount: number;
}

export interface BlogComment {
  id: string;
  articleId: string;
  parentId: string;
  nickname: string;
  website?: string;
  content: string;
  quoteText?: string;
  quoteStart?: number;
  quoteEnd?: number;
  ipRegion?: string;
  status: number;
  isAdmin: number;
  createTime: string;
  children?: BlogComment[];
}

export interface BlogHotArticleItem {
  id: string;
  title: string;
  slug?: string;
  viewCount: number;
  publishTime?: string;
}

export interface BlogSiteInfo {
  siteTitle: string;
  siteSubtitle?: string;
  authorName: string;
  authorBio?: string;
  authorAvatar?: string;
  githubUrl?: string;
  icpNumber?: string;
  commentAudit: string;
  giscusEnabled?: string;
  giscusRepo?: string;
  giscusRepoId?: string;
  giscusCategory?: string;
  giscusCategoryId?: string;
  giscusMapping?: string;
  aboutContent?: string;
  articleCount: number;
  categoryCount: number;
  tagCount: number;
  totalViews: number;
  hotArticles: BlogHotArticleItem[];
}

export interface BlogArchiveGroup {
  yearMonth: string;
  articles: {
    id: string;
    title: string;
    slug?: string;
    categoryName?: string;
    publishTime?: string;
  }[];
}

export interface BlogSearchArticleHit {
  id: string;
  slug?: string;
  title: string;
  categoryName?: string;
  publishTime?: string;
  snippet?: string;
}

export interface BlogSearchPathHit {
  id: string;
  slug?: string;
  title: string;
  summary?: string;
  matchedNodeId?: string;
  matchedNodeLabel?: string;
}

export interface BlogSearchArchiveHit {
  yearMonth: string;
  count: number;
}

export interface BlogSearchResult {
  articles: BlogSearchArticleHit[];
  paths: BlogSearchPathHit[];
  archives: BlogSearchArchiveHit[];
}

export interface PageResult<T> {
  list: T[];
  total: number;
}

export function getArticleRoute(article: { id: string; slug?: string }): string {
  const cleanSlug = article.slug?.trim();
  return cleanSlug ? `/article/${encodeURIComponent(cleanSlug)}` : `/article/${article.id}`;
}

export const blogApi = {
  getSiteInfo(): Promise<BlogSiteInfo> {
    return http.get("/site-info");
  },
  getArticles(params: {
    pageNum?: number;
    pageSize?: number;
    keywords?: string;
    categoryId?: string;
    tagId?: string;
  }): Promise<PageResult<BlogArticleItem>> {
    return http.get("/articles", { params });
  },
  getArticleDetail(
    idOrSlug: string,
    options?: { trackView?: boolean },
  ): Promise<BlogArticleDetail> {
    return http.get(`/articles/${encodeURIComponent(idOrSlug)}`, {
      params: options?.trackView === false ? { trackView: false } : undefined,
    });
  },
  likeArticle(id: string): Promise<number> {
    return http.post(`/articles/${id}/like`);
  },
  getComments(articleId: string): Promise<BlogComment[]> {
    return http.get(`/articles/${articleId}/comments`);
  },
  submitComment(data: {
    articleId: string;
    parentId?: string;
    nickname?: string;
    email?: string;
    website?: string;
    content: string;
    quoteText?: string;
    quoteStart?: number;
    quoteEnd?: number;
  }): Promise<BlogComment> {
    return http.post("/comments", data);
  },
  getCategories(): Promise<BlogCategory[]> {
    return http.get("/categories");
  },
  getTags(): Promise<BlogTag[]> {
    return http.get("/tags");
  },
  getArchives(): Promise<BlogArchiveGroup[]> {
    return http.get("/archives");
  },
  getLinks(): Promise<BlogLinkItem[]> {
    return http.get("/links");
  },
  getLearningPaths(): Promise<BlogLearningPathItem[]> {
    return http.get("/learning-paths");
  },
  getLearningPath(idOrSlug: string): Promise<BlogLearningPathItem> {
    return http.get(`/learning-paths/${encodeURIComponent(idOrSlug)}`);
  },
  searchPublic(q: string, limit = 8): Promise<BlogSearchResult> {
    return http.get("/search", { params: { q, limit } });
  },
};

export const blogAdminLearningPathApi = {
  async saveWithToken(
    payload: BlogLearningPathSavePayload,
    token: string,
    id?: string,
  ): Promise<string | void> {
    const url = id
      ? `/api/v1/blog/learning-paths/${encodeURIComponent(id)}`
      : "/api/v1/blog/learning-paths";
    const response = await axios.request<ApiResult<string | void>>({
      url,
      method: id ? "PUT" : "POST",
      data: payload,
      timeout: 15000,
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    const res = response.data;
    if (res.code === "00000") {
      return res.data;
    }
    throw new Error(res.msg || "保存学习路径失败");
  },
};
