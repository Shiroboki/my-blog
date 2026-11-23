<template>
  <div class="detail-page">
    <div v-if="loading" class="card state-card state-card--centered animate-rise">文章加载中...</div>
    <div v-else-if="errorMsg" class="card state-card state-card--centered animate-rise">
      <span class="error-state-badge">404</span>
      <p class="error-state-text">{{ errorMsg }}</p>
      <div class="error-state-actions">
        <button type="button" class="btn btn-primary" @click="goBack">
          {{ hasBackHistory ? "返回上一页" : "返回首页" }}
        </button>
        <RouterLink to="/archives" class="btn">浏览文章归档</RouterLink>
      </div>
    </div>

    <div v-else-if="article" :key="article.id" class="detail-grid">
      <!-- 左侧宽敞阅读主内容区与讨论区 -->
      <main class="article-main">
        <article
          ref="articleSheetRef"
          class="card article-sheet"
          :class="{
            'animate-rise': !seededFromMorph,
            'is-morph-hidden': hiddenDetailSheet,
          }"
        >
          <div class="sheet-topbar">
            <div class="sheet-topbar-left">
              <button type="button" class="back-btn" @click="goBack">
                {{ hasBackHistory ? "返回上一页" : "返回首页" }}
              </button>
              <RouterLink v-if="hasBackHistory" to="/" class="back-home-link">
                首页
              </RouterLink>
            </div>
            <div class="sheet-topbar-right">
              <RouterLink
                v-if="article.categoryName && article.categoryId"
                :to="{ path: '/', query: { categoryId: article.categoryId } }"
                class="pill topbar-cat-pill"
              >
                {{ article.categoryName }}
              </RouterLink>
              <span v-if="article.readingMinutes" class="topbar-reading-time">
                约 {{ article.readingMinutes }} 分钟
              </span>
            </div>
          </div>

          <header class="sheet-header">
            <div ref="titleSentinelRef" class="sheet-title-slot">
              <h1 class="sheet-title" :class="{ 'is-absorbed': titleAbsorbed }">
                {{ article.title }}
              </h1>
            </div>

            <p v-if="article.summary" class="sheet-lead">{{ article.summary }}</p>

            <div class="sheet-byline">
              <div class="byline-author">
                <img
                  v-if="siteInfo?.authorAvatar"
                  :src="siteInfo.authorAvatar"
                  :alt="siteInfo.authorName"
                  class="byline-avatar"
                />
                <span v-else class="byline-avatar byline-avatar--fallback">
                  {{ (siteInfo?.authorName || "博").slice(0, 1) }}
                </span>
                <span class="byline-name">{{ siteInfo?.authorName || "博主" }}</span>
              </div>
              <span class="byline-sep" aria-hidden="true">·</span>
              <span>{{ formatReadableDate(article.publishTime || article.createTime) }}</span>
              <span v-if="article.wordCount" class="byline-sep" aria-hidden="true">·</span>
              <span v-if="article.wordCount">{{ article.wordCount }} 字</span>
              <span class="byline-sep" aria-hidden="true">·</span>
              <span>阅读 {{ article.viewCount }}</span>
              <template v-if="annotationThreads.length">
                <span class="byline-sep" aria-hidden="true">·</span>
                <button
                  type="button"
                  class="byline-annotation-link"
                  @click="openFirstAnnotation"
                >
                  划词批注 {{ annotationThreads.length }} 处
                </button>
              </template>
              <template v-else-if="article.allowComment !== 0">
                <span class="byline-sep" aria-hidden="true">·</span>
                <span class="byline-tip">选中正文可划线批注</span>
              </template>
            </div>

            <div v-if="article.tags?.length" class="sheet-tags">
              <RouterLink
                v-for="tag in article.tags"
                :key="tag.id"
                :to="{ path: '/', query: { tagId: tag.id } }"
                class="pill"
              >
                # {{ tag.name }}
              </RouterLink>
            </div>

            <img
              v-if="article.coverImage"
              :src="article.coverImage"
              :alt="article.title"
              class="sheet-cover"
              @click="openLightbox(article.coverImage, article.title)"
            />
          </header>

          <div class="sheet-divider" aria-hidden="true" />

          <div v-if="contentLoading" class="sheet-body-skeleton" aria-hidden="true">
            <span class="body-skel-line w-full" />
            <span class="body-skel-line w-11-12" />
            <span class="body-skel-line w-4-5" />
            <span class="body-skel-line w-full" />
            <span class="body-skel-line w-2-3" />
          </div>

          <div
            v-else
            ref="markdownBodyRef"
            class="markdown-body"
            v-html="rendered.html"
            @click="handleMarkdownClick"
            @mouseover="handleMarkdownHover"
            @mouseout="handleMarkdownLeave"
            @mouseup="scheduleSelectionCheck"
            @touchend="scheduleSelectionCheck"
            @keyup="scheduleSelectionCheck"
          />

          <!-- 文末连贯作者落款与纯黑白线条圆形互动按钮 -->
          <div class="article-signoff">
            <div class="signoff-row">
              <div class="signoff-author">
                <img
                  v-if="siteInfo?.authorAvatar"
                  :src="siteInfo.authorAvatar"
                  :alt="siteInfo.authorName"
                  class="signoff-avatar"
                />
                <span v-else class="signoff-avatar signoff-avatar--fallback">
                  {{ (siteInfo?.authorName || "博").slice(0, 1) }}
                </span>
                <div class="signoff-meta">
                  <div class="signoff-name">{{ siteInfo?.authorName || "博主" }}</div>
                  <p class="signoff-bio">
                    {{ siteInfo?.authorBio || "感谢阅读，欢迎划词批注或前往下方讨论区交流。" }}
                  </p>
                </div>
              </div>

              <div class="signoff-actions">
                <div class="mono-circle-item">
                  <button
                    type="button"
                    class="mono-circle-btn"
                    :class="{ 'is-liked': hasLiked, 'is-bursting': likeBurst }"
                    :disabled="liking"
                    :title="hasLiked ? '已点赞支持' : '点赞支持'"
                    :aria-label="hasLiked ? '已点赞支持' : '点赞支持'"
                    @click="handleLike"
                  >
                    <svg viewBox="0 0 24 24" class="mono-icon" aria-hidden="true">
                      <path d="M7 10v12" />
                      <path d="M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z" />
                    </svg>
                  </button>
                  <span class="mono-circle-caption">{{ article.likeCount }}</span>
                </div>

                <div class="mono-circle-item">
                  <button
                    type="button"
                    class="mono-circle-btn"
                    :class="{ 'is-active': shareCopied }"
                    :title="shareCopied ? '链接已复制' : '复制链接分享'"
                    :aria-label="shareCopied ? '链接已复制' : '分享文章'"
                    @click="copyArticleUrl"
                  >
                    <svg v-if="shareCopied" viewBox="0 0 24 24" class="mono-icon" aria-hidden="true">
                      <polyline points="20 6 9 17 4 12" />
                    </svg>
                    <svg v-else viewBox="0 0 24 24" class="mono-icon" aria-hidden="true">
                      <circle cx="18" cy="5" r="3" />
                      <circle cx="6" cy="12" r="3" />
                      <circle cx="18" cy="19" r="3" />
                      <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
                      <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
                    </svg>
                  </button>
                  <span class="mono-circle-caption">{{ shareCopied ? "已复制" : "分享" }}</span>
                </div>
              </div>
            </div>
            <p v-if="likeHint" class="like-hint">{{ likeHint }}</p>
          </div>

          <nav v-if="article.prevArticle || article.nextArticle" class="neighbor-nav">
            <RouterLink
              v-if="article.prevArticle"
              :to="getArticleRoute(article.prevArticle)"
              class="neighbor-card"
            >
              <span class="neighbor-label">上一篇</span>
              <span class="neighbor-title">{{ article.prevArticle.title }}</span>
            </RouterLink>
            <div v-else />
            <RouterLink
              v-if="article.nextArticle"
              :to="getArticleRoute(article.nextArticle)"
              class="neighbor-card neighbor-card--right"
            >
              <span class="neighbor-label">下一篇</span>
              <span class="neighbor-title">{{ article.nextArticle.title }}</span>
            </RouterLink>
          </nav>
        </article>

        <section
          id="article-comments"
          class="card comment-section"
          :class="{ 'animate-rise': !seededFromMorph }"
          style="--stagger: 1"
        >
          <div class="comment-section-head">
            <h3 class="section-title">
              {{ giscusConfigured ? "GitHub 讨论区" : `评论交流（${article.commentCount}）` }}
            </h3>
            <span v-if="giscusConfigured" class="section-sub-badge">
              使用 GitHub 账号直接参与讨论
            </span>
            <span v-else-if="siteInfo?.giscusEnabled === '1'" class="section-sub-badge">
              GitHub 讨论区参数未完善，当前使用极简本地评论
            </span>
          </div>

          <div v-if="article.allowComment === 0" class="comment-closed">该文章暂未开启评论</div>

          <template v-else-if="giscusConfigured">
            <GiscusComments
              :repo="siteInfo!.giscusRepo!"
              :repo-id="siteInfo!.giscusRepoId!"
              :category="siteInfo?.giscusCategory"
              :category-id="siteInfo!.giscusCategoryId!"
              :mapping="siteInfo?.giscusMapping || 'pathname'"
              :term="String(article.id)"
            />

            <div v-if="inlineQuotedComments.length" class="inline-summary-block">
              <h4 class="inline-summary-title">
                正文划词批注（{{ inlineQuotedComments.length }}）
              </h4>
              <div class="comment-list">
                <CommentItem
                  v-for="item in inlineQuotedComments"
                  :key="item.id"
                  :comment="item"
                  @reply="openReplyInAnnotation"
                  @locate="locateAnnotationComment"
                />
              </div>
            </div>
          </template>

          <template v-else>
            <form class="comment-form" @submit.prevent="submitComment">
              <div v-if="replyTarget" class="reply-banner">
                <span>正在回复 @{{ replyTarget.nickname }}</span>
                <button type="button" class="cancel-reply" @click="replyTarget = null">
                  取消回复
                </button>
              </div>
              <textarea
                v-model="commentForm.content"
                required
                rows="3"
                maxlength="1000"
                placeholder="写下你的想法，只需填写内容即可发布（支持 Ctrl + Enter 提交）..."
                class="textarea-field"
                @keydown.ctrl.enter.prevent="submitComment"
                @keydown.meta.enter.prevent="submitComment"
              />
              <div class="minimal-form-bar">
                <input
                  v-model="commentForm.nickname"
                  type="text"
                  maxlength="64"
                  placeholder="昵称（选填，默认匿名读者）"
                  class="input-field input-field--nickname"
                />
                <div class="minimal-form-actions">
                  <span v-if="commentNotice" class="notice-text">{{ commentNotice }}</span>
                  <button type="submit" class="btn btn-primary" :disabled="submittingComment">
                    {{ submittingComment ? "提交中..." : "发表评论" }}
                  </button>
                </div>
              </div>
            </form>

            <div v-if="!comments.length" class="no-comments">还没有评论，来写下第一条想法吧</div>
            <div v-else class="comment-list">
              <CommentItem
                v-for="item in comments"
                :key="item.id"
                :comment="item"
                @reply="handleReply"
                @locate="locateAnnotationComment"
              />
            </div>
          </template>
        </section>
      </main>

      <!-- 桌面端右侧栏：互动工具 + 文章大纲固定悬浮 -->
      <aside class="toc-aside">
        <div class="side-sticky-stack">
          <div
            class="card aside-actions-card"
            :class="{ 'animate-rise': !seededFromMorph }"
            style="--stagger: 1"
          >
            <div class="aside-mono-row">
              <div class="mono-circle-item">
                <button
                  type="button"
                  class="mono-circle-btn"
                  :class="{ 'is-liked': hasLiked, 'is-bursting': likeBurst }"
                  :disabled="liking"
                  :title="hasLiked ? '已点赞支持' : '点赞支持'"
                  :aria-label="hasLiked ? '已点赞支持' : '点赞支持'"
                  @click="handleLike"
                >
                  <svg viewBox="0 0 24 24" class="mono-icon" aria-hidden="true">
                    <path d="M7 10v12" />
                    <path d="M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z" />
                  </svg>
                </button>
                <span class="mono-circle-caption">{{ article.likeCount }}</span>
              </div>

              <div class="mono-circle-item">
                <button
                  type="button"
                  class="mono-circle-btn"
                  :class="{ 'is-active': shareCopied }"
                  :title="shareCopied ? '链接已复制' : '复制链接分享'"
                  :aria-label="shareCopied ? '链接已复制' : '分享文章'"
                  @click="copyArticleUrl"
                >
                  <svg v-if="shareCopied" viewBox="0 0 24 24" class="mono-icon" aria-hidden="true">
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                  <svg v-else viewBox="0 0 24 24" class="mono-icon" aria-hidden="true">
                    <circle cx="18" cy="5" r="3" />
                    <circle cx="6" cy="12" r="3" />
                    <circle cx="18" cy="19" r="3" />
                    <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
                    <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
                  </svg>
                </button>
                <span class="mono-circle-caption">{{ shareCopied ? "已复制" : "分享" }}</span>
              </div>
            </div>

            <div class="aside-quick-row">
              <button
                type="button"
                class="aside-quick-btn"
                :class="{ 'is-active': annotationThreads.length > 0 }"
                :disabled="!annotationThreads.length"
                :title="annotationThreads.length ? '查看正文划词批注' : '选中正文文字即可批注'"
                @click="openFirstAnnotation"
              >
                批注 {{ annotationThreads.length }}
              </button>

              <button
                type="button"
                class="aside-quick-btn"
                @click="scrollToComments"
              >
                直达讨论区
              </button>
            </div>
          </div>

          <div
            v-if="rendered.toc.length"
            class="card toc-rail"
            :class="{ 'animate-rise': !seededFromMorph }"
            style="--stagger: 2"
          >
            <div class="toc-rail-title">文章大纲</div>
            <ul class="toc-list">
              <li
                v-for="heading in rendered.toc"
                :key="heading.id"
                :style="{ paddingLeft: `${Math.max(0, heading.level - tocBaseLevel) * 14}px` }"
              >
                <a
                  :href="`#${heading.id}`"
                  class="toc-link"
                  :class="{ active: activeHeadingId === heading.id }"
                  @click.prevent="scrollToHeading(heading.id)"
                >
                  {{ heading.text }}
                </a>
              </li>
            </ul>
          </div>
        </div>
      </aside>

      <!-- 移动端悬浮目录唤起按钮 + 磨砂抽屉 -->
      <button
        v-if="rendered.toc.length"
        type="button"
        class="mobile-toc-btn card"
        @click="mobileTocOpen = true"
      >
        目录
      </button>

      <Transition name="route-fade">
        <div v-if="mobileTocOpen" class="mobile-toc-mask" @click.self="mobileTocOpen = false">
          <div class="card mobile-toc-drawer">
            <div class="mobile-toc-header">
              <h4 class="toc-title">文章大纲</h4>
              <button type="button" class="pill" @click="mobileTocOpen = false">关闭</button>
            </div>
            <ul class="toc-list">
              <li
                v-for="heading in rendered.toc"
                :key="heading.id"
                :style="{ paddingLeft: `${Math.max(0, heading.level - tocBaseLevel) * 14}px` }"
              >
                <a
                  :href="`#${heading.id}`"
                  class="toc-link"
                  :class="{ active: activeHeadingId === heading.id }"
                  @click.prevent="scrollToHeading(heading.id)"
                >
                  {{ heading.text }}
                </a>
              </li>
            </ul>
          </div>
        </div>
      </Transition>

      <!-- 划词浮动操作按钮 -->
      <Transition name="route-fade">
        <span
          v-if="selectionMenu && selectionMenu.anchor.tooLong"
          class="selection-floating-pill selection-too-long"
          :style="{ left: `${selectionMenu.left}px`, top: `${selectionMenu.top}px` }"
          @mousedown.prevent
        >
          单次划词最多 500 字，请缩减选区
        </span>
        <button
          v-else-if="selectionMenu"
          type="button"
          class="btn btn-primary selection-floating-pill selection-action-btn"
          :style="{ left: `${selectionMenu.left}px`, top: `${selectionMenu.top}px` }"
          @mousedown.prevent
          @click="openInlineComposerFromSelection"
        >
          评论所选文字
        </button>
      </Transition>

      <!-- 划词批注查看浮层 -->
      <Transition name="route-fade">
        <div v-if="viewerOpen" class="annotation-mask" @click.self="closeAnnotationViewer">
          <div class="card annotation-modal">
            <div class="annotation-modal-header">
              <div>
                <h4 class="annotation-modal-title">
                  划词批注（共 {{ activeViewerComments.length }} 条主题）
                </h4>
                <p v-if="activeViewerAnchor?.quoteText" class="annotation-quote-preview">
                  引用原文：“{{ activeViewerAnchor.quoteText }}”
                </p>
              </div>
              <button type="button" class="pill" @click="closeAnnotationViewer">关闭</button>
            </div>

            <div class="annotation-thread-list">
              <div v-for="thread in activeViewerComments" :key="thread.id" class="annotation-thread">
                <CommentItem
                  :comment="thread"
                  @reply="openReplyInAnnotation"
                  @locate="jumpToCommentSection"
                />
                <div class="annotation-thread-actions">
                  <button
                    type="button"
                    class="annotation-text-btn"
                    @click="jumpToCommentSection(thread)"
                  >
                    在文末评论区查看
                  </button>
                </div>
              </div>
            </div>

            <div
              v-if="article.allowComment !== 0 && activeViewerAnchor"
              class="annotation-modal-footer"
            >
              <button
                type="button"
                class="btn btn-primary"
                @click="openComposerFromViewer"
              >
                针对此段补充新评论
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 划词评论发表浮层（极简） -->
      <Transition name="route-fade">
        <div v-if="inlineComposerOpen" class="annotation-mask" @click.self="closeInlineComposer">
          <div class="card annotation-modal annotation-modal--compact">
            <div class="annotation-modal-header">
              <h4 class="annotation-modal-title">
                {{ inlineReplyTarget ? `回复 @${inlineReplyTarget.nickname}` : "发表划词评论" }}
              </h4>
              <button type="button" class="pill" @click="closeInlineComposer">取消</button>
            </div>

            <div v-if="activeComposerAnchor?.quoteText" class="composer-quote-box">
              <span class="composer-quote-label">引用原文</span>
              <p class="composer-quote-content">{{ activeComposerAnchor.quoteText }}</p>
            </div>

            <form class="comment-form" @submit.prevent="submitInlineComment">
              <textarea
                ref="inlineTextareaRef"
                v-model="inlineContent"
                required
                rows="4"
                maxlength="1000"
                placeholder="直接写下针对这段文字的批注（支持 Ctrl + Enter 提交）..."
                class="textarea-field"
                @keydown.ctrl.enter.prevent="submitInlineComment"
                @keydown.meta.enter.prevent="submitInlineComment"
              />
              <div class="minimal-form-bar">
                <input
                  v-model="commentForm.nickname"
                  type="text"
                  maxlength="64"
                  placeholder="昵称（选填，默认匿名读者）"
                  class="input-field input-field--nickname"
                />
                <div class="minimal-form-actions">
                  <span v-if="inlineNotice" class="notice-text">{{ inlineNotice }}</span>
                  <button type="submit" class="btn btn-primary" :disabled="submittingInline">
                    {{ submittingInline ? "提交中..." : "提交批注" }}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </div>

    <!-- 正文与封面图片灯箱放大预览 -->
    <Teleport to="body">
      <Transition name="route-fade">
        <div
          v-if="lightboxImage"
          class="lightbox-mask"
          role="dialog"
          aria-modal="true"
          aria-label="图片放大预览"
          @click.self="closeLightbox"
        >
          <button
            type="button"
            class="lightbox-close-btn"
            aria-label="关闭图片预览"
            @click="closeLightbox"
          >
            关闭（Esc）
          </button>
          <div class="lightbox-stage" @click.self="closeLightbox">
            <img
              :src="lightboxImage.src"
              :alt="lightboxImage.alt"
              class="lightbox-img"
              :class="{ 'is-zoomed': lightboxImage.zoomed }"
              @click="lightboxImage.zoomed = !lightboxImage.zoomed"
            />
          </div>
          <p class="lightbox-caption">
            <span v-if="lightboxImage.alt">{{ lightboxImage.alt }} · </span>
            点击图片可{{ lightboxImage.zoomed ? "还原适应窗口" : "放大细节" }}
          </p>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { onBeforeRouteLeave, useRoute, useRouter } from "vue-router";
import { blogApi, getArticleRoute, type BlogArticleDetail, type BlogComment } from "@/api/blog";
import CommentItem from "@/components/CommentItem.vue";
import GiscusComments from "@/components/GiscusComments.vue";
import {
  cacheArticleDetail,
  getCachedArticleDetail,
  launchEnterMorph,
  peekPendingEnter,
  prepareLeaveMorph,
  updateActiveEnterSheetSnapshot,
  useArticleTransition,
} from "@/composables/useArticleTransition";
import { useNavbarArticle } from "@/composables/useNavbarArticle";
import { useSiteInfo } from "@/composables/useSiteInfo";
import { updatePageSeo } from "@/router";
import { renderMarkdown } from "@/utils/markdown";
import {
  applyInlineAnnotations,
  extractSelectionAnchor,
  type AnnotationThread,
  type SelectionAnchor,
} from "@/utils/selectionAnnotation";

const LIKED_STORAGE_KEY = "myblog_liked_ids";

function readLikedIds(): string[] {
  try {
    const raw = localStorage.getItem(LIKED_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.map(String) : [];
  } catch {
    return [];
  }
}

const route = useRoute();
const router = useRouter();
const { siteInfo, loadSiteInfo } = useSiteInfo();
const { setNavbarArticle } = useNavbarArticle();
const { hiddenDetailSheet } = useArticleTransition();
const loading = ref(false);
const contentLoading = ref(false);
const seededFromMorph = ref(false);
const errorMsg = ref("");
const article = ref<BlogArticleDetail | null>(null);
const articleSheetRef = ref<HTMLElement | null>(null);
const comments = ref<BlogComment[]>([]);

const giscusConfigured = computed(() =>
  Boolean(
    siteInfo.value?.giscusEnabled === "1" &&
      siteInfo.value?.giscusRepo?.trim() &&
      siteInfo.value?.giscusRepoId?.trim() &&
      siteInfo.value?.giscusCategoryId?.trim(),
  ),
);

const inlineQuotedComments = computed(() =>
  comments.value.filter((item) => Boolean(item.quoteText)),
);

const hasBackHistory = computed(() => {
  void route.fullPath;
  return typeof window !== "undefined" && Boolean(window.history.state?.back);
});

function goBack() {
  if (hasBackHistory.value) {
    router.back();
  } else {
    router.push("/");
  }
}

function formatReadableDate(raw?: string) {
  if (!raw) return "";
  const matched = raw.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!matched) return raw.slice(0, 10);
  return `${matched[1]}年${Number(matched[2])}月${Number(matched[3])}日`;
}

const likedIds = ref<string[]>(readLikedIds());
const liking = ref(false);
const likeBurst = ref(false);
const likeHint = ref("");
const hasLiked = computed(() =>
  Boolean(article.value && likedIds.value.includes(article.value.id)),
);
const shareCopied = ref(false);
let shareTimer: number | null = null;

const replyTarget = ref<BlogComment | null>(null);
const submittingComment = ref(false);
const commentNotice = ref("");
const commentForm = reactive({
  nickname: localStorage.getItem("myblog_nickname") || "",
  content: "",
});

const activeHeadingId = ref("");
const mobileTocOpen = ref(false);
const titleSentinelRef = ref<HTMLElement | null>(null);
const titleAbsorbed = ref(false);
const markdownBodyRef = ref<HTMLElement | null>(null);
const inlineTextareaRef = ref<HTMLTextAreaElement | null>(null);

const annotationThreads = ref<AnnotationThread[]>([]);
const selectionMenu = ref<{ anchor: SelectionAnchor; left: number; top: number } | null>(null);
const inlineComposerOpen = ref(false);
const activeComposerAnchor = ref<SelectionAnchor | null>(null);
const inlineReplyTarget = ref<BlogComment | null>(null);
const inlineContent = ref("");
const inlineNotice = ref("");
const submittingInline = ref(false);

const viewerOpen = ref(false);
const activeViewerIds = ref<string[]>([]);
const activeViewerAnchor = computed<SelectionAnchor | null>(() => {
  const first = activeViewerComments.value[0];
  if (!first?.quoteText) return null;
  return {
    quoteText: first.quoteText,
    quoteStart: first.quoteStart ?? 0,
    quoteEnd: first.quoteEnd ?? first.quoteText.length,
  };
});
const activeViewerComments = computed(() =>
  comments.value.filter((item) => activeViewerIds.value.includes(item.id)),
);

const lightboxImage = ref<{ src: string; alt: string; zoomed: boolean } | null>(null);
let selectionTimer: number | null = null;
let flashTimer: number | null = null;
let activeLoadToken = 0;

function openLightbox(src: string, alt = "") {
  if (!src) return;
  lightboxImage.value = { src, alt, zoomed: false };
}

function closeLightbox() {
  lightboxImage.value = null;
}

function applyArticleSeo(target: BlogArticleDetail) {
  updatePageSeo({
    title: target.title,
    description: target.summary || target.content.slice(0, 140) || target.title,
    image: target.coverImage,
    path: getArticleRoute(target),
    type: "article",
  });
}

const rendered = computed(() =>
  renderMarkdown(article.value?.content || "", {
    stripLeadingTitle: article.value?.title,
  }),
);

const tocBaseLevel = computed(() => {
  const items = rendered.value.toc;
  if (!items.length) return 1;
  return Math.min(...items.map((item) => item.level));
});

function syncInlineAnnotations() {
  if (!markdownBodyRef.value) {
    annotationThreads.value = [];
    return;
  }
  annotationThreads.value = applyInlineAnnotations(markdownBodyRef.value, comments.value);
}

function updateActiveHeading() {
  const toc = rendered.value.toc;
  if (!toc.length) return;
  let current = toc[0].id;
  for (const item of toc) {
    const el = document.getElementById(item.id);
    if (el && el.getBoundingClientRect().top <= 128) {
      current = item.id;
    }
  }
  activeHeadingId.value = current;
}

function updateTitleAbsorption() {
  const sentinel = titleSentinelRef.value;
  if (!sentinel || !article.value) {
    titleAbsorbed.value = false;
    setNavbarArticle(null);
    return;
  }
  const rect = sentinel.getBoundingClientRect();
  const midY = rect.top + rect.height * 0.48;
  if (!titleAbsorbed.value && midY <= 78) {
    titleAbsorbed.value = true;
  } else if (titleAbsorbed.value && midY >= 98) {
    titleAbsorbed.value = false;
  }
  setNavbarArticle({
    title: article.value.title,
    category: article.value.categoryName,
    absorbed: titleAbsorbed.value,
  });
}

async function copyArticleUrl() {
  try {
    await navigator.clipboard.writeText(window.location.href);
    shareCopied.value = true;
    if (shareTimer !== null) {
      window.clearTimeout(shareTimer);
    }
    shareTimer = window.setTimeout(() => {
      shareCopied.value = false;
      shareTimer = null;
    }, 1800);
  } catch {
    // 忽略剪贴板权限拒绝
  }
}

function scrollToComments() {
  document.getElementById("article-comments")?.scrollIntoView({ behavior: "smooth" });
}

async function loadArticle(routeKey: string) {
  const token = ++activeLoadToken;
  errorMsg.value = "";
  likeHint.value = "";
  commentNotice.value = "";
  replyTarget.value = null;
  mobileTocOpen.value = false;
  selectionMenu.value = null;
  viewerOpen.value = false;
  inlineComposerOpen.value = false;
  lightboxImage.value = null;
  titleAbsorbed.value = false;
  setNavbarArticle(null);

  const pendingMorph = peekPendingEnter(routeKey);
  const cachedDetail = getCachedArticleDetail(routeKey);
  const hadCachedContent = Boolean(cachedDetail?.content);

  if (pendingMorph) {
    seededFromMorph.value = true;
    loading.value = false;
    contentLoading.value = !cachedDetail;
    hiddenDetailSheet.value = true;

    const seed: BlogArticleDetail = cachedDetail
      ? { ...cachedDetail }
      : {
          id: pendingMorph.preview.id,
          slug: pendingMorph.preview.slug,
          title: pendingMorph.preview.title,
          summary: pendingMorph.preview.summary,
          coverImage: pendingMorph.preview.coverImage,
          categoryId: pendingMorph.preview.categoryId,
          categoryName: pendingMorph.preview.categoryName,
          tags: pendingMorph.preview.tags || [],
          isTop: pendingMorph.preview.isTop ?? 0,
          readingMinutes: pendingMorph.preview.readingMinutes,
          wordCount: pendingMorph.preview.wordCount,
          viewCount: pendingMorph.preview.viewCount ?? 0,
          likeCount: pendingMorph.preview.likeCount ?? 0,
          commentCount: pendingMorph.preview.commentCount ?? 0,
          publishTime: pendingMorph.preview.publishTime,
          createTime: pendingMorph.preview.createTime || "",
          content: "",
          allowComment: 1,
        };
    article.value = seed;
    comments.value = [];
    applyArticleSeo(seed);
    await nextTick();
    if (token !== activeLoadToken) return;
    if (articleSheetRef.value) {
      launchEnterMorph(routeKey, articleSheetRef.value);
    } else {
      hiddenDetailSheet.value = false;
    }
  } else if (cachedDetail) {
    seededFromMorph.value = false;
    loading.value = false;
    contentLoading.value = false;
    article.value = { ...cachedDetail };
    comments.value = [];
    applyArticleSeo(cachedDetail);
  } else {
    seededFromMorph.value = false;
    loading.value = true;
    contentLoading.value = false;
    article.value = null;
    comments.value = [];
  }

  try {
    const isNumericId = /^\d+$/.test(routeKey);
    let detail: BlogArticleDetail;
    let commentList: BlogComment[];

    if (isNumericId) {
      [detail, commentList] = await Promise.all([
        blogApi.getArticleDetail(routeKey, { trackView: true }),
        blogApi.getComments(routeKey),
      ]);
    } else {
      detail = await blogApi.getArticleDetail(routeKey, { trackView: true });
      if (token !== activeLoadToken) return;
      commentList = await blogApi.getComments(detail.id);
    }

    if (token !== activeLoadToken) return;
    article.value = detail;
    cacheArticleDetail(detail);
    comments.value = commentList;
    contentLoading.value = false;
    applyArticleSeo(detail);
    await nextTick();
    if (token !== activeLoadToken) return;
    if (!hadCachedContent && articleSheetRef.value) {
      updateActiveEnterSheetSnapshot(articleSheetRef.value);
    }
    syncInlineAnnotations();
    updateActiveHeading();
    updateTitleAbsorption();
  } catch (err) {
    if (token !== activeLoadToken) return;
    if (!article.value) {
      errorMsg.value = err instanceof Error ? err.message : "文章不存在或尚未发布";
    }
    contentLoading.value = false;
  } finally {
    if (token === activeLoadToken) {
      loading.value = false;
    }
  }
}

function scheduleSelectionCheck() {
  if (selectionTimer !== null) {
    window.clearTimeout(selectionTimer);
  }
  selectionTimer = window.setTimeout(() => {
    selectionTimer = null;
    if (!article.value || article.value.allowComment === 0 || !markdownBodyRef.value) {
      selectionMenu.value = null;
      return;
    }
    const sel = window.getSelection();
    if (!sel || sel.rangeCount === 0 || sel.isCollapsed) {
      selectionMenu.value = null;
      return;
    }
    const range = sel.getRangeAt(0);
    const anchor = extractSelectionAnchor(markdownBodyRef.value, range);
    if (!anchor) {
      selectionMenu.value = null;
      return;
    }
    const clientRects = Array.from(range.getClientRects()).filter(
      (item) => item.width > 0 || item.height > 0,
    );
    const boundingRect = range.getBoundingClientRect();
    const anchorRect = clientRects[clientRects.length - 1] || boundingRect;
    if (!anchorRect.width && !anchorRect.height) {
      selectionMenu.value = null;
      return;
    }
    const buttonWidth = anchor.tooLong ? 228 : 118;
    const buttonHeight = 36;
    const left = Math.min(Math.max(anchorRect.right + 6, 16), window.innerWidth - buttonWidth - 16);
    const top =
      anchorRect.bottom + 8 + buttonHeight <= window.innerHeight - 12
        ? anchorRect.bottom + 8
        : Math.max(76, anchorRect.top - buttonHeight - 8);
    selectionMenu.value = { anchor, left, top };
  }, 16);
}

function openInlineComposerFromSelection() {
  if (!selectionMenu.value || selectionMenu.value.anchor.tooLong) return;
  activeComposerAnchor.value = { ...selectionMenu.value.anchor };
  inlineReplyTarget.value = null;
  inlineContent.value = "";
  inlineNotice.value = "";
  selectionMenu.value = null;
  inlineComposerOpen.value = true;
  window.getSelection()?.removeAllRanges();
  nextTick(() => {
    inlineTextareaRef.value?.focus();
  });
}

function openComposerFromViewer() {
  if (!activeViewerAnchor.value) return;
  activeComposerAnchor.value = { ...activeViewerAnchor.value };
  inlineReplyTarget.value = null;
  inlineContent.value = "";
  inlineNotice.value = "";
  viewerOpen.value = false;
  inlineComposerOpen.value = true;
  nextTick(() => {
    inlineTextareaRef.value?.focus();
  });
}

function openReplyInAnnotation(comment: BlogComment) {
  inlineReplyTarget.value = comment;
  activeComposerAnchor.value = comment.quoteText
    ? {
        quoteText: comment.quoteText,
        quoteStart: comment.quoteStart ?? 0,
        quoteEnd: comment.quoteEnd ?? comment.quoteText.length,
      }
    : activeViewerAnchor.value
      ? { ...activeViewerAnchor.value }
      : null;
  inlineContent.value = "";
  inlineNotice.value = "";
  viewerOpen.value = false;
  inlineComposerOpen.value = true;
  nextTick(() => {
    inlineTextareaRef.value?.focus();
  });
}

function closeInlineComposer() {
  inlineComposerOpen.value = false;
  inlineReplyTarget.value = null;
}

function openAnnotationByIds(ids: string[]) {
  if (!ids.length) return;
  activeViewerIds.value = ids;
  viewerOpen.value = true;
  selectionMenu.value = null;
  if (markdownBodyRef.value) {
    markdownBodyRef.value.querySelectorAll("mark.inline-quote-mark").forEach((el) => {
      const elIds = ((el as HTMLElement).dataset.commentIds || "").split(",");
      el.classList.toggle("is-active", elIds.some((id) => ids.includes(id)));
    });
  }
}

function closeAnnotationViewer() {
  viewerOpen.value = false;
  markdownBodyRef.value?.querySelectorAll("mark.inline-quote-mark.is-active").forEach((el) => {
    el.classList.remove("is-active");
  });
}

function openFirstAnnotation() {
  const first = annotationThreads.value[0];
  if (!first) return;
  locateAnnotationComment(first.comment);
  openAnnotationByIds([first.comment.id]);
}

function locateAnnotationComment(comment: BlogComment) {
  if (!markdownBodyRef.value) return;
  const targets = Array.from(markdownBodyRef.value.querySelectorAll("mark.inline-quote-mark")).filter(
    (el) => ((el as HTMLElement).dataset.commentIds || "").split(",").includes(comment.id),
  ) as HTMLElement[];
  if (!targets.length) return;
  targets[0].scrollIntoView({ behavior: "smooth", block: "center" });
  if (flashTimer !== null) {
    window.clearTimeout(flashTimer);
  }
  targets.forEach((el) => {
    el.classList.remove("is-flash");
    void el.offsetWidth;
    el.classList.add("is-flash");
  });
  flashTimer = window.setTimeout(() => {
    targets.forEach((el) => el.classList.remove("is-flash"));
    flashTimer = null;
  }, 1500);
}

function jumpToCommentSection(comment: BlogComment) {
  closeAnnotationViewer();
  nextTick(() => {
    const el = document.getElementById(`comment-${comment.id}`);
    el?.scrollIntoView({ behavior: "smooth", block: "center" });
  });
}

function handleMarkdownHover(e: MouseEvent) {
  const mark = (e.target as HTMLElement | null)?.closest("mark.inline-quote-mark") as HTMLElement | null;
  const primaryId = mark?.dataset.primaryId;
  if (!primaryId || !markdownBodyRef.value) return;
  markdownBodyRef.value.querySelectorAll("mark.inline-quote-mark").forEach((el) => {
    el.classList.toggle("is-hovered", (el as HTMLElement).dataset.primaryId === primaryId);
  });
}

function handleMarkdownLeave() {
  markdownBodyRef.value?.querySelectorAll("mark.inline-quote-mark.is-hovered").forEach((el) => {
    el.classList.remove("is-hovered");
  });
}

async function handleMarkdownClick(e: MouseEvent) {
  const btn = (e.target as HTMLElement | null)?.closest(".code-copy-btn") as HTMLButtonElement | null;
  if (btn) {
    const codeText = btn.closest(".code-block")?.querySelector("pre code")?.textContent ?? "";
    if (!codeText) return;
    try {
      await navigator.clipboard.writeText(codeText);
      btn.textContent = "已复制";
      btn.classList.add("copied");
      window.setTimeout(() => {
        btn.textContent = "复制代码";
        btn.classList.remove("copied");
      }, 1800);
    } catch {
      btn.textContent = "复制失败";
    }
    return;
  }

  const img = (e.target as HTMLElement | null)?.closest("img") as HTMLImageElement | null;
  if (img && img.src) {
    openLightbox(img.src, img.alt || article.value?.title || "");
    return;
  }

  const sel = window.getSelection();
  if (sel && !sel.isCollapsed && sel.toString().trim().length > 0) {
    return;
  }

  const mark = (e.target as HTMLElement | null)?.closest("mark.inline-quote-mark") as HTMLElement | null;
  if (!mark) return;
  const ids = (mark.dataset.commentIds || "").split(",").filter(Boolean);
  if (ids.length) {
    openAnnotationByIds(ids);
  }
}

async function handleLike() {
  if (!article.value || liking.value) return;
  if (hasLiked.value) {
    likeHint.value = "你已经点赞过这篇文章了";
    return;
  }
  liking.value = true;
  likeHint.value = "";
  try {
    const count = await blogApi.likeArticle(article.value.id);
    article.value.likeCount = count;
    if (!likedIds.value.includes(article.value.id)) {
      likedIds.value = [...likedIds.value, article.value.id];
      try {
        localStorage.setItem(LIKED_STORAGE_KEY, JSON.stringify(likedIds.value));
      } catch {
        // 忽略本地存储异常
      }
    }
    likeHint.value = "感谢点赞支持";
    likeBurst.value = true;
    setTimeout(() => {
      likeBurst.value = false;
    }, 600);
  } catch (err) {
    likeHint.value = err instanceof Error ? err.message : "点赞失败";
  } finally {
    liking.value = false;
  }
}

function handleReply(comment: BlogComment) {
  replyTarget.value = comment;
  const formEl = document.querySelector(".comment-form");
  formEl?.scrollIntoView({ behavior: "smooth", block: "center" });
}

async function submitComment() {
  if (!article.value || submittingComment.value) return;
  submittingComment.value = true;
  commentNotice.value = "";
  try {
    if (commentForm.nickname.trim()) {
      localStorage.setItem("myblog_nickname", commentForm.nickname.trim());
    } else {
      localStorage.removeItem("myblog_nickname");
    }

    const created = await blogApi.submitComment({
      articleId: article.value.id,
      parentId: replyTarget.value?.id || "0",
      nickname: commentForm.nickname.trim() || undefined,
      content: commentForm.content.trim(),
    });
    commentForm.content = "";
    replyTarget.value = null;
    if (created.status === 1) {
      commentNotice.value = "评论已发布";
      comments.value = await blogApi.getComments(article.value.id);
      article.value.commentCount = Number(article.value.commentCount || 0) + 1;
    } else {
      commentNotice.value = "评论已提交，等待博主审核后展示";
    }
  } catch (err) {
    commentNotice.value = err instanceof Error ? err.message : "评论提交失败";
  } finally {
    submittingComment.value = false;
  }
}

async function submitInlineComment() {
  if (!article.value || submittingInline.value) return;
  submittingInline.value = true;
  inlineNotice.value = "";
  try {
    if (commentForm.nickname.trim()) {
      localStorage.setItem("myblog_nickname", commentForm.nickname.trim());
    } else {
      localStorage.removeItem("myblog_nickname");
    }

    const anchor = inlineReplyTarget.value ? null : activeComposerAnchor.value;
    const created = await blogApi.submitComment({
      articleId: article.value.id,
      parentId: inlineReplyTarget.value?.id || "0",
      nickname: commentForm.nickname.trim() || undefined,
      content: inlineContent.value.trim(),
      quoteText: anchor?.quoteText,
      quoteStart: anchor?.quoteStart,
      quoteEnd: anchor?.quoteEnd,
    });

    inlineContent.value = "";
    const targetToLocate = inlineReplyTarget.value || created;
    inlineReplyTarget.value = null;

    if (created.status === 1) {
      comments.value = await blogApi.getComments(article.value.id);
      article.value.commentCount = Number(article.value.commentCount || 0) + 1;
      inlineComposerOpen.value = false;
      await nextTick();
      syncInlineAnnotations();
      locateAnnotationComment(targetToLocate);
    } else {
      inlineNotice.value = "批注已提交，等待博主审核后展示";
    }
  } catch (err) {
    inlineNotice.value = err instanceof Error ? err.message : "批注提交失败";
  } finally {
    submittingInline.value = false;
  }
}

function handleWindowScroll() {
  updateActiveHeading();
  updateTitleAbsorption();
  if (selectionMenu.value) {
    selectionMenu.value = null;
  }
}

function handleDocumentMouseDown(e: MouseEvent) {
  const el = e.target as HTMLElement | null;
  if (el?.closest(".selection-floating-pill, .markdown-body")) {
    return;
  }
  selectionMenu.value = null;
}

function handleDocumentKeydown(e: KeyboardEvent) {
  if (e.key !== "Escape") return;
  if (lightboxImage.value) {
    closeLightbox();
    return;
  }
  if (inlineComposerOpen.value) {
    closeInlineComposer();
    return;
  }
  if (viewerOpen.value) {
    closeAnnotationViewer();
    return;
  }
  if (mobileTocOpen.value) {
    mobileTocOpen.value = false;
  }
}

function scrollToHeading(id: string) {
  mobileTocOpen.value = false;
  activeHeadingId.value = id;
  document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
}

watch(
  () => route.params.id,
  (id) => {
    if (typeof id === "string" && id) {
      loadArticle(id);
    }
  },
  { immediate: true },
);

watch(
  [() => rendered.value.html, () => comments.value],
  () => {
    syncInlineAnnotations();
  },
  { deep: true, flush: "post" },
);

onBeforeRouteLeave((to) => {
  if (to.path === "/" && article.value && articleSheetRef.value) {
    prepareLeaveMorph(article.value, articleSheetRef.value);
  }
});

onMounted(() => {
  void loadSiteInfo().then(() => {
    if (article.value) {
      applyArticleSeo(article.value);
    }
  });
  window.addEventListener("scroll", handleWindowScroll, { passive: true });
  document.addEventListener("mousedown", handleDocumentMouseDown);
  window.addEventListener("keydown", handleDocumentKeydown);
});

onUnmounted(() => {
  setNavbarArticle(null);
  window.removeEventListener("scroll", handleWindowScroll);
  document.removeEventListener("mousedown", handleDocumentMouseDown);
  window.removeEventListener("keydown", handleDocumentKeydown);
  if (selectionTimer !== null) {
    window.clearTimeout(selectionTimer);
  }
  if (flashTimer !== null) {
    window.clearTimeout(flashTimer);
  }
  if (shareTimer !== null) {
    window.clearTimeout(shareTimer);
  }
});
</script>

<style scoped>
.detail-page {
  width: 100%;
}

.state-card {
  padding: 56px 20px;
  text-align: center;
  color: var(--text-muted);
}

.state-card--centered {
  max-width: 720px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.error-state-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12.5px;
  font-weight: 700;
}

.error-state-text {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
}

.error-state-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}

/* 宽敞两栏阅读布局：左 920px+ 主内容/讨论区 + 右 252px 固定悬浮互动与大纲 */
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 252px;
  gap: 26px;
  align-items: stretch;
}

.article-main {
  display: flex;
  flex-direction: column;
  gap: 22px;
  min-width: 0;
}

/* 宽敞透气的阅读纸面 */
.article-sheet {
  padding: 32px 38px 38px;
  background: var(--morph-surface);
}

.article-sheet.is-morph-hidden {
  opacity: 0 !important;
  visibility: hidden !important;
  pointer-events: none;
  transition: none !important;
}

.sheet-body-skeleton {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 8px 0 24px;
}

.body-skel-line {
  display: block;
  height: 14px;
  border-radius: 8px;
  background: var(--bg-subtle);
}

.body-skel-line.w-full {
  width: 100%;
}
.body-skel-line.w-11-12 {
  width: 92%;
}
.body-skel-line.w-4-5 {
  width: 80%;
}
.body-skel-line.w-2-3 {
  width: 66%;
}

.sheet-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 24px;
}

.sheet-topbar-left,
.sheet-topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.back-btn {
  padding: 5px 13px;
  border-radius: 999px;
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, background-color 0.2s ease;
}

.back-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}

.back-home-link {
  font-size: 12.5px;
  color: var(--text-muted);
  transition: color 0.2s ease;
}

.back-home-link:hover {
  color: var(--accent);
}

.topbar-cat-pill {
  font-size: 12px;
  padding: 3px 10px;
}

.topbar-reading-time {
  font-size: 12.5px;
  color: var(--text-muted);
}

/* 开篇 Hero 头部 */
.sheet-header {
  margin-bottom: 24px;
}

.sheet-title-slot {
  margin: 0 0 14px;
}

.sheet-title {
  margin: 0;
  font-size: clamp(25px, 2.8vw, 33px);
  font-weight: 800;
  line-height: 1.32;
  letter-spacing: -0.025em;
  color: var(--text-primary);
  transform-origin: left center;
  transition:
    opacity 0.26s ease,
    transform 0.34s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.26s ease;
}

.sheet-title.is-absorbed {
  opacity: 0.14;
  transform: translateY(-10px) scale(0.975);
  filter: blur(2px);
}

.sheet-lead {
  margin: 0 0 18px;
  padding-left: 14px;
  border-left: 2px solid color-mix(in srgb, var(--accent) 45%, transparent);
  font-size: 15px;
  line-height: 1.78;
  color: var(--text-secondary);
}

.sheet-byline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 14px;
}

.byline-author {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--text-primary);
  font-weight: 600;
}

.byline-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border-subtle);
}

.byline-avatar--fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
}

.byline-sep {
  opacity: 0.55;
}

.byline-annotation-link {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--accent);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.byline-annotation-link:hover {
  text-decoration: underline;
}

.byline-tip {
  color: var(--text-muted);
  opacity: 0.85;
}

.sheet-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.sheet-cover {
  width: 100%;
  max-height: 360px;
  object-fit: cover;
  border-radius: 16px;
  margin-top: 20px;
  cursor: zoom-in;
  transition: transform 0.25s ease;
}

.sheet-cover:hover {
  transform: scale(1.006);
}

.markdown-body :deep(img) {
  cursor: zoom-in;
}

.sheet-divider {
  height: 1px;
  margin: 0 0 28px;
  background: linear-gradient(90deg, var(--border-subtle), transparent 90%);
}

/* 文末连贯作者落款卡 */
.article-signoff {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.signoff-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.signoff-author {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  flex: 1 1 260px;
}

.signoff-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid var(--border-subtle);
}

.signoff-avatar--fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 700;
}

.signoff-meta {
  min-width: 0;
  flex: 1;
}

.signoff-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.signoff-bio {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.5;
}

.signoff-actions {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex-shrink: 0;
}

/* 纯黑白色线条圆形表情按钮 */
.mono-circle-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.mono-circle-btn {
  width: 44px;
  height: 44px;
  padding: 0;
  border-radius: 50%;
  border: 1.5px solid rgba(17, 17, 17, 0.28);
  background: #ffffff;
  color: #111111;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition:
    transform 0.22s cubic-bezier(0.34, 1.56, 0.64, 1),
    border-color 0.2s ease,
    background-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease;
}

.mono-circle-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  border-color: #111111;
  box-shadow: 0 6px 16px -6px rgba(0, 0, 0, 0.22);
}

.mono-circle-btn:active:not(:disabled) {
  transform: scale(0.94);
}

.mono-circle-btn.is-liked,
.mono-circle-btn.is-active {
  background: #111111;
  color: #ffffff;
  border-color: #111111;
}

.mono-circle-btn.is-bursting {
  animation: likePop 0.55s cubic-bezier(0.34, 1.56, 0.64, 1);
}

html.dark .mono-circle-btn {
  background: #111111;
  color: #f8fafc;
  border-color: rgba(255, 255, 255, 0.3);
}

html.dark .mono-circle-btn:hover:not(:disabled) {
  border-color: #ffffff;
  box-shadow: 0 6px 16px -6px rgba(0, 0, 0, 0.68);
}

html.dark .mono-circle-btn.is-liked,
html.dark .mono-circle-btn.is-active {
  background: #ffffff;
  color: #111111;
  border-color: #ffffff;
}

.mono-icon {
  width: 19px;
  height: 19px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.75;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.mono-circle-caption {
  font-size: 11.5px;
  font-weight: 500;
  color: var(--text-muted);
  line-height: 1;
}

@keyframes likePop {
  0% {
    transform: scale(1);
  }
  40% {
    transform: scale(1.12) rotate(-3deg);
  }
  100% {
    transform: scale(1);
  }
}

.like-hint {
  margin: 0;
  font-size: 13px;
  color: var(--accent);
}

/* 上下篇导航（支持 2 行完整标题） */
.neighbor-nav {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.neighbor-card {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 14px 16px;
  border-radius: var(--radius-inner);
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.neighbor-card:hover {
  transform: translateY(-2px);
  border-color: var(--accent);
}

.neighbor-card--right {
  text-align: right;
}

.neighbor-label {
  font-size: 12px;
  color: var(--text-muted);
}

.neighbor-title {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 讨论区卡片 */
.comment-section {
  padding: 28px 36px;
  background: var(--bg-glass-strong);
}

.comment-section-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  font-size: 18px;
}

.section-sub-badge {
  font-size: 12px;
  color: var(--text-muted);
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--bg-subtle);
  border: 1px solid var(--border-subtle);
}

.inline-summary-block {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border-subtle);
}

.inline-summary-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 700;
}

.minimal-form-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.input-field--nickname {
  max-width: 240px;
  flex: 1 1 180px;
}

.minimal-form-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

.comment-closed,
.no-comments {
  padding: 18px;
  border-radius: var(--radius-inner);
  background: var(--bg-subtle);
  color: var(--text-muted);
  text-align: center;
  font-size: 14px;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.reply-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: var(--radius-control);
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 13px;
}

.cancel-reply {
  border: none;
  background: transparent;
  color: inherit;
  text-decoration: underline;
  cursor: pointer;
}

.input-field,
.textarea-field {
  width: 100%;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-inner);
  background: var(--bg-subtle);
  color: var(--text-primary);
  padding: 10px 14px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.input-field:focus,
.textarea-field:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.notice-text {
  font-size: 13px;
  color: var(--accent);
}

/* 右侧固定悬浮互动卡片 + 文章大纲卡片 */
.toc-aside {
  min-width: 0;
}

.side-sticky-stack {
  position: sticky;
  top: 78px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: calc(100vh - 96px);
  overflow-y: auto;
  scrollbar-width: none;
  padding: 4px 6px 12px;
  margin: -4px -6px;
}

.side-sticky-stack::-webkit-scrollbar {
  display: none;
}

.aside-actions-card {
  padding: 14px 12px 12px;
  border-radius: var(--radius-card);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.aside-mono-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-evenly;
  gap: 12px;
}

.aside-quick-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.aside-quick-btn {
  padding: 7px 8px;
  border-radius: var(--radius-control);
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, background-color 0.2s ease;
}

.aside-quick-btn:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}

.aside-quick-btn:disabled {
  opacity: 0.48;
  cursor: default;
}

.aside-quick-btn.is-active {
  border-color: color-mix(in srgb, var(--accent) 42%, transparent);
  background: var(--accent-soft);
  color: var(--accent);
}

.toc-rail {
  padding: 16px;
  border-radius: var(--radius-card);
}

.toc-rail-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  letter-spacing: 0.04em;
  margin-bottom: 10px;
}

.toc-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 700;
}

.toc-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toc-link {
  position: relative;
  display: block;
  font-size: 13px;
  line-height: 1.5;
  padding: 5px 8px;
  border-radius: 8px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.18s ease, background-color 0.18s ease;
}

.toc-link:hover {
  color: var(--text-primary);
  background: var(--bg-subtle);
}

.toc-link.active {
  color: var(--accent);
  font-weight: 600;
  background: var(--accent-soft);
}

.mobile-toc-btn {
  display: none;
  position: fixed;
  right: 24px;
  bottom: 82px;
  z-index: 60;
  padding: 9px 14px;
  border-radius: 999px;
  corner-shape: round;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.mobile-toc-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  background: rgba(15, 23, 42, 0.38);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 16px;
}

.mobile-toc-drawer {
  width: 100%;
  max-width: 520px;
  max-height: 70vh;
  overflow-y: auto;
  padding: 20px;
  background: var(--bg-glass-strong);
}

.mobile-toc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

/* 划词交互按钮（直接定位在选区右下方，不带外层方形卡片背景） */
.selection-floating-pill {
  position: fixed;
  z-index: 85;
  margin: 0;
}

.selection-action-btn {
  padding: 7px 14px;
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1.3;
  border-radius: 999px;
  box-shadow: 0 8px 22px -4px color-mix(in srgb, var(--accent) 55%, transparent);
}

.selection-too-long {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--bg-glass-strong);
  backdrop-filter: blur(16px);
  border: 1px solid var(--border-glass);
  box-shadow: var(--shadow-glass);
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
}

.annotation-mask {
  position: fixed;
  inset: 0;
  z-index: 95;
  background: rgba(15, 23, 42, 0.42);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
}

.annotation-modal {
  width: 100%;
  max-width: 620px;
  max-height: 82vh;
  overflow-y: auto;
  padding: 24px 26px;
  background: var(--bg-glass-strong);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.annotation-modal--compact {
  max-width: 560px;
}

.annotation-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.annotation-modal-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.annotation-quote-preview {
  margin: 8px 0 0;
  padding: 8px 12px;
  border-left: 3px dotted var(--accent);
  border-radius: 0 10px 10px 0;
  background: var(--bg-subtle);
  font-size: 13px;
  color: var(--text-secondary);
}

.annotation-thread-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.annotation-thread {
  padding: 2px 12px 10px;
  border-radius: 14px;
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
}

.annotation-thread-actions {
  display: flex;
  justify-content: flex-end;
}

.annotation-text-btn {
  border: none;
  background: transparent;
  color: var(--accent);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.annotation-text-btn:hover {
  text-decoration: underline;
}

.annotation-modal-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 6px;
}

.composer-quote-box {
  padding: 10px 14px;
  border-left: 3px dotted var(--accent);
  border-radius: 0 12px 12px 0;
  background: var(--bg-subtle);
}

.composer-quote-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  margin-bottom: 4px;
}

.composer-quote-content {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
  max-height: 96px;
  overflow-y: auto;
}

@media (max-width: 920px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .toc-aside {
    display: none;
  }

  .mobile-toc-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
}

@media (max-width: 640px) {
  .article-sheet {
    padding: 22px 20px 26px;
  }

  .comment-section {
    padding: 20px;
  }

  .neighbor-nav {
    grid-template-columns: 1fr;
  }
}

@supports (corner-shape: squircle) {
  .sheet-cover,
  .mobile-toc-drawer,
  .annotation-modal,
  .annotation-thread,
  .neighbor-card,
  .comment-closed,
  .no-comments,
  .reply-banner,
  .input-field,
  .textarea-field,
  .aside-quick-btn,
  .toc-link {
    corner-shape: squircle;
  }

  .selection-action-btn,
  .lightbox-close-btn {
    corner-shape: round;
  }
}

.lightbox-mask {
  position: fixed;
  inset: 0;
  z-index: 110;
  background: rgba(9, 13, 20, 0.84);
  backdrop-filter: blur(8px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 28px 20px 20px;
  gap: 14px;
}

.lightbox-close-btn {
  position: fixed;
  top: 20px;
  right: 24px;
  z-index: 112;
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(17, 24, 39, 0.72);
  color: #f8fafc;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.lightbox-close-btn:hover {
  background: rgba(15, 23, 42, 0.92);
  border-color: rgba(255, 255, 255, 0.45);
}

.lightbox-stage {
  max-width: 92vw;
  max-height: calc(100vh - 110px);
  overflow: auto;
  display: flex;
  padding: 8px;
}

.lightbox-img {
  margin: auto;
  max-width: 88vw;
  max-height: calc(100vh - 130px);
  border-radius: 12px;
  object-fit: contain;
  cursor: zoom-in;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.55);
  transition: max-width 0.22s ease, max-height 0.22s ease;
}

.lightbox-img.is-zoomed {
  max-width: none;
  max-height: none;
  cursor: zoom-out;
}

.lightbox-caption {
  margin: 0;
  font-size: 12.5px;
  color: rgba(226, 232, 240, 0.82);
  text-align: center;
}
</style>
