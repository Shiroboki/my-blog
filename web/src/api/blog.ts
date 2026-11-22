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

export interface BlogArticleDetail extends BlogArticleItem {
  content: string;
  prevArticle?: { id: string; title: string } | null;
  nextArticle?: { id: string; title: string } | null;
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
  hotArticles: { id: string; title: string; viewCount: number; publishTime?: string }[];
}

export interface BlogArchiveGroup {
  yearMonth: string;
  articles: {
    id: string;
    title: string;
    categoryName?: string;
    publishTime?: string;
  }[];
}

export interface PageResult<T> {
  list: T[];
  total: number;
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
  getArticleDetail(id: string): Promise<BlogArticleDetail> {
    return http.get(`/articles/${id}`);
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
};
