import type { PageQuery } from "@/api/common";

export interface BlogTagRef {
  id: string;
  name: string;
  slug: string;
  color?: string;
}

export interface BlogArticleQuery extends PageQuery {
  keywords?: string;
  categoryId?: string;
  tagId?: string;
  status?: number;
}

export interface BlogArticleItem {
  id: string;
  title: string;
  slug?: string;
  summary?: string;
  coverImage?: string;
  categoryId?: string;
  categoryName?: string;
  status: number;
  isTop: number;
  allowComment: number;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  publishTime?: string;
  createTime?: string;
  updateTime?: string;
  tags?: BlogTagRef[];
}

export interface BlogArticleForm {
  id?: string;
  title: string;
  slug?: string;
  summary?: string;
  content: string;
  coverImage?: string;
  categoryId?: string;
  status: number;
  isTop: number;
  allowComment: number;
  tagIds?: string[];
}

export interface BlogCategoryQuery extends PageQuery {
  keywords?: string;
}

export interface BlogCategoryItem {
  id: string;
  name: string;
  slug: string;
  description?: string;
  sort: number;
  articleCount: number;
  createTime?: string;
}

export interface BlogCategoryForm {
  id?: string;
  name: string;
  slug: string;
  description?: string;
  sort: number;
}

export interface BlogTagQuery extends PageQuery {
  keywords?: string;
}

export interface BlogTagItem {
  id: string;
  name: string;
  slug: string;
  color?: string;
  articleCount: number;
  createTime?: string;
}

export interface BlogTagForm {
  id?: string;
  name: string;
  slug: string;
  color?: string;
}

export interface BlogCommentQuery extends PageQuery {
  articleId?: string;
  keywords?: string;
  status?: number;
}

export interface BlogCommentItem {
  id: string;
  articleId: string;
  articleTitle?: string;
  parentId: string;
  nickname: string;
  email?: string;
  website?: string;
  content: string;
  quoteText?: string;
  quoteStart?: number;
  quoteEnd?: number;
  ipRegion?: string;
  status: number;
  isAdmin: number;
  createTime?: string;
}

export interface BlogCommentReplyForm {
  articleId: string;
  parentId: string;
  content: string;
}

export interface BlogConfigForm {
  siteTitle: string;
  siteSubtitle?: string;
  authorName: string;
  authorBio?: string;
  authorAvatar?: string;
  githubUrl?: string;
  icpNumber?: string;
  commentAudit: string;
  giscusEnabled: string;
  giscusRepo?: string;
  giscusRepoId?: string;
  giscusCategory?: string;
  giscusCategoryId?: string;
  giscusMapping?: string;
  aboutContent?: string;
}

export interface BlogLinkQuery extends PageQuery {
  keywords?: string;
  status?: number;
}

export interface BlogLinkItem {
  id: string;
  name: string;
  url: string;
  avatar?: string;
  description?: string;
  sort: number;
  status: number;
  createTime?: string;
}

export interface BlogLinkForm {
  id?: string;
  name: string;
  url: string;
  avatar?: string;
  description?: string;
  sort: number;
  status: number;
}

export interface BlogDashboardStats {
  publishedCount: number | string;
  draftCount: number | string;
  categoryCount: number | string;
  tagCount: number | string;
  totalViews: number | string;
  totalLikes: number | string;
  approvedCommentCount: number | string;
  pendingCommentCount: number | string;
  linkCount: number | string;
  topArticles: {
    id: string;
    title: string;
    viewCount: number | string;
    likeCount: number | string;
    publishTime?: string;
  }[];
  categoryStats: {
    name: string;
    value: number | string;
  }[];
  pendingComments: BlogCommentItem[];
}

