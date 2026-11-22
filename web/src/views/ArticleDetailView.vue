<template>
  <div v-if="loading" class="card state-card animate-rise">文章加载中...</div>
  <div v-else-if="errorMsg" class="card state-card animate-rise">
    <p class="error-state-text">{{ errorMsg }}</p>
    <button type="button" class="btn btn-primary" @click="goBack">
      {{ hasBackHistory ? "返回上一页" : "返回首页" }}
    </button>
  </div>
  <div v-else-if="article" class="detail-grid">
    <main class="article-main">
      <article class="card article-sheet animate-rise">
        <div class="sheet-topbar">
          <div class="sheet-topbar-left">
            <button type="button" class="back-btn" @click="goBack">
              {{ hasBackHistory ? "返回上一页" : "返回首页" }}
            </button>
            <RouterLink v-if="hasBackHistory" to="/" class="back-home-link">
              回到首页
            </RouterLink>
          </div>
          <RouterLink to="/archives" class="back-home-link">文章归档</RouterLink>
        </div>

        <header class="sheet-header">
          <div class="sheet-meta">
            <RouterLink
              v-if="article.categoryName && article.categoryId"
              :to="{ path: '/', query: { categoryId: article.categoryId } }"
              class="pill"
            >
              {{ article.categoryName }}
            </RouterLink>
            <span>发布于 {{ article.publishTime || article.createTime }}</span>
            <span>阅读 {{ article.viewCount }}</span>
            <span>评论 {{ article.commentCount }}</span>
            <span v-if="article.wordCount">{{ article.wordCount }} 字</span>
            <span v-if="article.readingMinutes">预计阅读 {{ article.readingMinutes }} 分钟</span>
            <button
              v-if="annotationThreads.length"
              type="button"
              class="pill meta-annotation-btn"
              @click="openFirstAnnotation"
            >
              划词批注 {{ annotationThreads.length }} 处
            </button>
          </div>
          <h1 class="sheet-title">{{ article.title }}</h1>
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
          />
        </header>

        <div
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

        <div class="like-box">
          <button
            type="button"
            class="btn btn-primary like-btn"
            :class="{ 'is-bursting': likeBurst, 'is-liked': hasLiked }"
            :disabled="liking"
            @click="handleLike"
          >
            <span>{{ hasLiked ? "已点赞支持" : "点赞支持" }}（{{ article.likeCount }}）</span>
          </button>
          <span v-if="likeHint" class="like-hint">{{ likeHint }}</span>
        </div>

        <nav v-if="article.prevArticle || article.nextArticle" class="neighbor-nav">
          <RouterLink
            v-if="article.prevArticle"
            :to="`/article/${article.prevArticle.id}`"
            class="neighbor-card"
          >
            <span class="neighbor-label">上一篇</span>
            <span class="neighbor-title">{{ article.prevArticle.title }}</span>
          </RouterLink>
          <div v-else />
          <RouterLink
            v-if="article.nextArticle"
            :to="`/article/${article.nextArticle.id}`"
            class="neighbor-card neighbor-card--right"
          >
            <span class="neighbor-label">下一篇</span>
            <span class="neighbor-title">{{ article.nextArticle.title }}</span>
          </RouterLink>
        </nav>
      </article>

      <section class="card comment-section animate-rise" style="--stagger: 1">
        <div class="comment-section-head">
          <h3 class="section-title">
            {{ giscusConfigured ? "GitHub 讨论区" : `评论交流（${article.commentCount}）` }}
          </h3>
          <span v-if="giscusConfigured" class="section-sub-badge">
            使用 GitHub 账号免填资料直接参与讨论
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
            <h4 class="inline-summary-title">正文划词批注（{{ inlineQuotedComments.length }}）</h4>
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
              <button type="button" class="cancel-reply" @click="replyTarget = null">取消回复</button>
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

    <!-- 桌面端右侧目录吸顶（内部可滚动，带阅读高亮） -->
    <aside v-if="rendered.toc.length" class="toc-aside">
      <div class="card toc-card animate-rise" style="--stagger: 1">
        <h4 class="toc-title">文章目录</h4>
        <ul class="toc-list">
          <li
            v-for="heading in rendered.toc"
            :key="heading.id"
            :style="{ paddingLeft: `${(heading.level - 1) * 12}px` }"
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
            <h4 class="toc-title">文章目录</h4>
            <button type="button" class="pill" @click="mobileTocOpen = false">关闭</button>
          </div>
          <ul class="toc-list">
            <li
              v-for="heading in rendered.toc"
              :key="heading.id"
              :style="{ paddingLeft: `${(heading.level - 1) * 12}px` }"
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

    <!-- 划词浮动操作胶囊 -->
    <Transition name="route-fade">
      <div
        v-if="selectionMenu"
        class="selection-floating-pill card"
        :style="{ left: `${selectionMenu.left}px`, top: `${selectionMenu.top}px` }"
        @mousedown.prevent
      >
        <span v-if="selectionMenu.anchor.tooLong" class="selection-too-long">
          单次划词最多 500 字，请缩减选区
        </span>
        <button
          v-else
          type="button"
          class="btn btn-primary selection-action-btn"
          @click="openInlineComposerFromSelection"
        >
          评论所选文字
        </button>
      </div>
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

          <div v-if="article.allowComment !== 0 && activeViewerAnchor" class="annotation-modal-footer">
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

    <!-- 划词评论发表浮层 -->
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
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { blogApi, type BlogArticleDetail, type BlogComment } from "@/api/blog";
import CommentItem from "@/components/CommentItem.vue";
import GiscusComments from "@/components/GiscusComments.vue";
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
const loading = ref(false);
const errorMsg = ref("");
const article = ref<BlogArticleDetail | null>(null);
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

const likedIds = ref<string[]>(readLikedIds());
const liking = ref(false);
const likeBurst = ref(false);
const likeHint = ref("");
const hasLiked = computed(() => Boolean(article.value && likedIds.value.includes(article.value.id)));

const replyTarget = ref<BlogComment | null>(null);
const submittingComment = ref(false);
const commentNotice = ref("");
const commentForm = reactive({
  nickname: localStorage.getItem("myblog_nickname") || "",
  content: "",
});

const activeHeadingId = ref("");
const mobileTocOpen = ref(false);
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

let selectionTimer: number | null = null;
let flashTimer: number | null = null;

const rendered = computed(() => renderMarkdown(article.value?.content || ""));

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
    if (el && el.getBoundingClientRect().top <= 120) {
      current = item.id;
    }
  }
  activeHeadingId.value = current;
}

async function loadArticle(id: string) {
  loading.value = true;
  errorMsg.value = "";
  likeHint.value = "";
  commentNotice.value = "";
  replyTarget.value = null;
  mobileTocOpen.value = false;
  selectionMenu.value = null;
  viewerOpen.value = false;
  inlineComposerOpen.value = false;
  try {
    const [detail, commentList] = await Promise.all([
      blogApi.getArticleDetail(id),
      blogApi.getComments(id),
    ]);
    article.value = detail;
    comments.value = commentList;
    updatePageSeo(detail.title, detail.summary || detail.content.slice(0, 140));
    await nextTick();
    syncInlineAnnotations();
    updateActiveHeading();
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : "文章加载失败";
  } finally {
    loading.value = false;
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
    const rect = range.getBoundingClientRect();
    if (!rect.width && !rect.height) {
      selectionMenu.value = null;
      return;
    }
    const left = Math.min(Math.max(rect.left + rect.width / 2, 110), window.innerWidth - 110);
    const top = rect.top >= 88 ? rect.top - 48 : rect.bottom + 10;
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

onMounted(() => {
  loadSiteInfo();
  window.addEventListener("scroll", handleWindowScroll, { passive: true });
  document.addEventListener("mousedown", handleDocumentMouseDown);
});

onUnmounted(() => {
  window.removeEventListener("scroll", handleWindowScroll);
  document.removeEventListener("mousedown", handleDocumentMouseDown);
  if (selectionTimer !== null) {
    window.clearTimeout(selectionTimer);
  }
  if (flashTimer !== null) {
    window.clearTimeout(flashTimer);
  }
});
</script>

<style scoped>
.state-card {
  padding: 56px 20px;
  text-align: center;
  color: var(--text-muted);
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 24px;
  align-items: stretch;
}

.article-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.article-sheet {
  padding: 30px 34px;
}

.sheet-header {
  margin-bottom: 24px;
}

.sheet-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.sheet-title {
  margin: 0 0 14px;
  font-size: 28px;
  line-height: 1.35;
}

.sheet-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sheet-cover {
  width: 100%;
  max-height: 360px;
  object-fit: cover;
  border-radius: 18px;
  margin-top: 18px;
}

.like-box {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.like-btn {
  border-radius: 999px;
  padding: 11px 24px;
}

.like-btn.is-liked {
  background: var(--bg-subtle);
  color: var(--accent);
  border: 1px solid var(--accent);
  box-shadow: none;
}

.like-btn.is-bursting {
  animation: likePop 0.55s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes likePop {
  0% {
    transform: scale(1);
  }
  40% {
    transform: scale(1.16) rotate(-4deg);
  }
  100% {
    transform: scale(1);
  }
}

.like-hint {
  font-size: 13px;
  color: var(--accent);
}

.neighbor-nav {
  margin-top: 26px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.neighbor-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 16px;
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
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-section {
  padding: 26px 30px;
}

.comment-section-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 16px;
}

.comment-section-head .section-title {
  margin: 0;
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

.section-title {
  margin: 0 0 16px;
  font-size: 18px;
}

.comment-closed,
.no-comments {
  padding: 20px 0;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.reply-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 14px;
  border-radius: 12px;
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

.form-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.input-field,
.textarea-field {
  width: 100%;
  border: 1px solid var(--border-subtle);
  border-radius: 14px;
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

.form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.notice-text {
  font-size: 13px;
  color: var(--accent);
}

.toc-aside {
  min-width: 0;
}

.toc-card {
  position: sticky;
  top: 78px;
  max-height: calc(100vh - 96px);
  overflow-y: auto;
  scrollbar-width: none;
  padding: 18px;
}

.error-state-text {
  margin: 0 0 16px;
}

.sheet-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  margin-bottom: 18px;
  border-bottom: 1px solid var(--border-subtle);
}

.sheet-topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 13px;
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
  font-size: 13px;
  color: var(--text-muted);
  transition: color 0.2s ease;
}

.back-home-link:hover {
  color: var(--accent);
}

.toc-card::-webkit-scrollbar {
  display: none;
}

.toc-title {
  margin: 0 0 12px;
  font-size: 14px;
}

.toc-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.toc-link {
  display: block;
  font-size: 13px;
  padding: 5px 10px;
  border-radius: 10px;
  border-left: 2px solid transparent;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: background-color 0.18s ease, color 0.18s ease, border-color 0.18s ease;
}

.toc-link:hover,
.toc-link.active {
  color: var(--accent);
  background: var(--accent-soft);
  border-left-color: var(--accent);
  font-weight: 500;
}

.mobile-toc-btn {
  display: none;
  position: fixed;
  right: 24px;
  bottom: 82px;
  z-index: 60;
  padding: 9px 14px;
  border-radius: 999px;
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

.meta-annotation-btn {
  color: var(--accent);
  border-color: rgba(13, 148, 136, 0.35);
}

.selection-floating-pill {
  position: fixed;
  transform: translateX(-50%);
  z-index: 85;
  padding: 5px;
  border-radius: 999px;
  background: var(--bg-glass-strong);
  box-shadow: var(--shadow-glass-hover);
}

.selection-action-btn {
  padding: 6px 14px;
  font-size: 13px;
  border-radius: 999px;
}

.selection-too-long {
  display: inline-block;
  padding: 5px 12px;
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

@media (max-width: 960px) {
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
  .article-sheet,
  .comment-section {
    padding: 20px;
  }

  .form-row,
  .neighbor-nav {
    grid-template-columns: 1fr;
  }
}
</style>
