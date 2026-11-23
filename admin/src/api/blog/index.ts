import request from "@/utils/request";
import type { OptionItem, PageResult } from "@/api/common";
import type {
  BlogArticleForm,
  BlogArticleItem,
  BlogArticleQuery,
  BlogCategoryForm,
  BlogCategoryItem,
  BlogCategoryQuery,
  BlogCommentItem,
  BlogCommentQuery,
  BlogCommentReplyForm,
  BlogConfigForm,
  BlogDashboardStats,
  BlogLearningPathForm,
  BlogLearningPathItem,
  BlogLearningPathQuery,
  BlogLinkForm,
  BlogLinkItem,
  BlogLinkQuery,
  BlogMcpCreatedToken,
  BlogMcpOverview,
  BlogMcpSettingsForm,
  BlogMcpTokenCreateForm,
  BlogTagForm,
  BlogTagItem,
  BlogTagQuery,
} from "./types";

const BLOG_BASE = "/api/v1/blog";

export const BlogArticleAPI = {
  getPage(params: BlogArticleQuery) {
    return request<unknown, PageResult<BlogArticleItem>>({
      url: `${BLOG_BASE}/articles`,
      method: "get",
      params,
    });
  },
  getForm(id: string) {
    return request<unknown, BlogArticleForm>({
      url: `${BLOG_BASE}/articles/${id}/form`,
      method: "get",
    });
  },
  create(data: BlogArticleForm) {
    return request({
      url: `${BLOG_BASE}/articles`,
      method: "post",
      data,
    });
  },
  update(id: string, data: BlogArticleForm) {
    return request({
      url: `${BLOG_BASE}/articles/${id}`,
      method: "put",
      data,
    });
  },
  updateStatus(id: string, status: number) {
    return request({
      url: `${BLOG_BASE}/articles/${id}/status`,
      method: "put",
      params: { status },
    });
  },
  updateTop(id: string, isTop: number) {
    return request({
      url: `${BLOG_BASE}/articles/${id}/top`,
      method: "put",
      params: { isTop },
    });
  },
  deleteByIds(ids: string) {
    return request({
      url: `${BLOG_BASE}/articles/${ids}`,
      method: "delete",
    });
  },
  exportMarkdown(id: string) {
    return request<Blob>({
      url: `${BLOG_BASE}/articles/${id}/export`,
      method: "get",
      responseType: "blob",
    });
  },
  importMarkdown(file: File) {
    const formData = new FormData();
    formData.append("file", file);
    return request<unknown, string>({
      url: `${BLOG_BASE}/articles/import`,
      method: "post",
      data: formData,
      headers: { "Content-Type": "multipart/form-data" },
    });
  },
};

export const BlogCategoryAPI = {
  getPage(params: BlogCategoryQuery) {
    return request<unknown, PageResult<BlogCategoryItem>>({
      url: `${BLOG_BASE}/categories`,
      method: "get",
      params,
    });
  },
  getOptions() {
    return request<unknown, OptionItem[]>({
      url: `${BLOG_BASE}/categories/options`,
      method: "get",
    });
  },
  getForm(id: string) {
    return request<unknown, BlogCategoryForm>({
      url: `${BLOG_BASE}/categories/${id}/form`,
      method: "get",
    });
  },
  create(data: BlogCategoryForm) {
    return request({
      url: `${BLOG_BASE}/categories`,
      method: "post",
      data,
    });
  },
  update(id: string, data: BlogCategoryForm) {
    return request({
      url: `${BLOG_BASE}/categories/${id}`,
      method: "put",
      data,
    });
  },
  deleteByIds(ids: string) {
    return request({
      url: `${BLOG_BASE}/categories/${ids}`,
      method: "delete",
    });
  },
};

export const BlogTagAPI = {
  getPage(params: BlogTagQuery) {
    return request<unknown, PageResult<BlogTagItem>>({
      url: `${BLOG_BASE}/tags`,
      method: "get",
      params,
    });
  },
  getOptions() {
    return request<unknown, OptionItem[]>({
      url: `${BLOG_BASE}/tags/options`,
      method: "get",
    });
  },
  getForm(id: string) {
    return request<unknown, BlogTagForm>({
      url: `${BLOG_BASE}/tags/${id}/form`,
      method: "get",
    });
  },
  create(data: BlogTagForm) {
    return request({
      url: `${BLOG_BASE}/tags`,
      method: "post",
      data,
    });
  },
  update(id: string, data: BlogTagForm) {
    return request({
      url: `${BLOG_BASE}/tags/${id}`,
      method: "put",
      data,
    });
  },
  deleteByIds(ids: string) {
    return request({
      url: `${BLOG_BASE}/tags/${ids}`,
      method: "delete",
    });
  },
};

export const BlogCommentAPI = {
  getPage(params: BlogCommentQuery) {
    return request<unknown, PageResult<BlogCommentItem>>({
      url: `${BLOG_BASE}/comments`,
      method: "get",
      params,
    });
  },
  audit(id: string, status: number) {
    return request({
      url: `${BLOG_BASE}/comments/${id}/audit`,
      method: "put",
      params: { status },
    });
  },
  reply(data: BlogCommentReplyForm) {
    return request({
      url: `${BLOG_BASE}/comments/reply`,
      method: "post",
      data,
    });
  },
  deleteByIds(ids: string) {
    return request({
      url: `${BLOG_BASE}/comments/${ids}`,
      method: "delete",
    });
  },
};

export const BlogConfigAPI = {
  get() {
    return request<unknown, BlogConfigForm>({
      url: `${BLOG_BASE}/configs`,
      method: "get",
    });
  },
  save(data: BlogConfigForm) {
    return request({
      url: `${BLOG_BASE}/configs`,
      method: "put",
      data,
    });
  },
};

export const BlogLinkAPI = {
  getPage(params: BlogLinkQuery) {
    return request<unknown, PageResult<BlogLinkItem>>({
      url: `${BLOG_BASE}/links`,
      method: "get",
      params,
    });
  },
  getForm(id: string) {
    return request<unknown, BlogLinkForm>({
      url: `${BLOG_BASE}/links/${id}/form`,
      method: "get",
    });
  },
  create(data: BlogLinkForm) {
    return request({
      url: `${BLOG_BASE}/links`,
      method: "post",
      data,
    });
  },
  update(id: string, data: BlogLinkForm) {
    return request({
      url: `${BLOG_BASE}/links/${id}`,
      method: "put",
      data,
    });
  },
  deleteByIds(ids: string) {
    return request({
      url: `${BLOG_BASE}/links/${ids}`,
      method: "delete",
    });
  },
};

export const BlogDashboardAPI = {
  getStats() {
    return request<unknown, BlogDashboardStats>({
      url: `${BLOG_BASE}/dashboard`,
      method: "get",
    });
  },
};

export const BlogLearningPathAPI = {
  getPage(queryParams: BlogLearningPathQuery) {
    return request<unknown, PageResult<BlogLearningPathItem>>({
      url: `${BLOG_BASE}/learning-paths`,
      method: "get",
      params: queryParams,
    });
  },

  getForm(id: string) {
    return request<unknown, BlogLearningPathForm>({
      url: `${BLOG_BASE}/learning-paths/${id}/form`,
      method: "get",
    });
  },

  create(data: BlogLearningPathForm) {
    return request<unknown, string>({
      url: `${BLOG_BASE}/learning-paths`,
      method: "post",
      data,
    });
  },

  update(id: string, data: BlogLearningPathForm) {
    return request({
      url: `${BLOG_BASE}/learning-paths/${id}`,
      method: "put",
      data,
    });
  },

  deleteByIds(ids: string) {
    return request({
      url: `${BLOG_BASE}/learning-paths/${ids}`,
      method: "delete",
    });
  },
};

export const BlogMcpAPI = {
  getOverview() {
    return request<unknown, BlogMcpOverview>({
      url: `${BLOG_BASE}/mcp/overview`,
      method: "get",
    });
  },

  updateSettings(data: BlogMcpSettingsForm) {
    return request({
      url: `${BLOG_BASE}/mcp/settings`,
      method: "put",
      data,
    });
  },

  createToken(data: BlogMcpTokenCreateForm) {
    return request<unknown, BlogMcpCreatedToken>({
      url: `${BLOG_BASE}/mcp/tokens`,
      method: "post",
      data,
    });
  },

  updateTokenStatus(id: string, status: number) {
    return request({
      url: `${BLOG_BASE}/mcp/tokens/${id}/status`,
      method: "patch",
      params: { status },
    });
  },

  deleteToken(id: string) {
    return request({
      url: `${BLOG_BASE}/mcp/tokens/${id}`,
      method: "delete",
    });
  },
};

export * from "./types";
