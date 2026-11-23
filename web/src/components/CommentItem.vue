<template>
  <div :id="`comment-${comment.id}`" class="comment-item" :class="{ 'comment-item--child': depth > 0 }">
    <div class="comment-header">
      <div class="comment-author">
        <span class="comment-avatar" :style="avatarStyle">{{ avatarChar }}</span>
        <a
          v-if="comment.website"
          :href="comment.website"
          target="_blank"
          rel="noopener noreferrer nofollow"
          class="comment-name comment-name--link"
        >
          {{ comment.nickname }}
        </a>
        <span v-else class="comment-name">{{ comment.nickname }}</span>
        <span v-if="comment.isAdmin === 1" class="admin-badge">博主</span>
        <span v-if="comment.ipRegion" class="region-badge">{{ comment.ipRegion }}</span>
      </div>
      <div class="comment-meta">
        <span>{{ comment.createTime }}</span>
        <button type="button" class="reply-btn" @click="$emit('reply', comment)">回复</button>
      </div>
    </div>
    <div v-if="comment.quoteText" class="comment-quote">
      <span class="comment-quote-label">划词引用</span>
      <span class="comment-quote-text">{{ comment.quoteText }}</span>
      <button type="button" class="comment-locate-btn" @click="$emit('locate', comment)">
        定位原文
      </button>
    </div>
    <p class="comment-body">{{ comment.content }}</p>

    <div v-if="comment.children && comment.children.length" class="comment-children">
      <CommentItem
        v-for="child in comment.children"
        :key="child.id"
        :comment="child"
        :depth="depth + 1"
        @reply="$emit('reply', $event)"
        @locate="$emit('locate', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { BlogComment } from "@/api/blog";

const props = withDefaults(
  defineProps<{
    comment: BlogComment;
    depth?: number;
  }>(),
  {
    depth: 0,
  },
);

defineEmits<{
  reply: [comment: BlogComment];
  locate: [comment: BlogComment];
}>();

const AVATAR_GRADIENTS = [
  "linear-gradient(135deg, #0d9488, #0284c7)",
  "linear-gradient(135deg, #0284c7, #6366f1)",
  "linear-gradient(135deg, #0f766e, #059669)",
  "linear-gradient(135deg, #d97706, #ea580c)",
  "linear-gradient(135deg, #7c3aed, #db2777)",
];

const avatarChar = computed(() => {
  const name = (props.comment.nickname || "").trim();
  return (name ? name.slice(0, 1) : "客").toUpperCase();
});

const avatarStyle = computed(() => {
  const name = props.comment.nickname || "";
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = (hash * 31 + name.charCodeAt(i)) % AVATAR_GRADIENTS.length;
  }
  return {
    background: AVATAR_GRADIENTS[Math.abs(hash) % AVATAR_GRADIENTS.length],
  };
});
</script>

<style scoped>
.comment-item {
  padding: 14px 0;
  border-bottom: 1px solid var(--border-subtle);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-item--child {
  margin-top: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid var(--border-subtle);
  background: var(--bg-subtle);
}

.comment-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.comment-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-avatar {
  width: 26px;
  height: 26px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #ffffff;
  flex-shrink: 0;
}

.comment-name {
  font-weight: 600;
  font-size: 14px;
}

.comment-name--link {
  color: var(--accent);
}

.admin-badge {
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 999px;
  background: var(--accent);
  color: #fff;
}

html.dark .admin-badge {
  color: #0b1120;
}

.region-badge {
  font-size: 12px;
  color: var(--text-muted);
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--text-muted);
}

.reply-btn {
  border: none;
  background: transparent;
  color: var(--accent);
  cursor: pointer;
  padding: 0;
  font-size: 12px;
}

.comment-quote {
  margin-top: 8px;
  padding: 7px 10px;
  border-radius: 10px;
  border-left: 3px dotted var(--accent);
  background: var(--bg-subtle);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.comment-quote-label {
  color: var(--accent);
  font-weight: 600;
  flex-shrink: 0;
}

.comment-quote-text {
  color: var(--text-muted);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-locate-btn {
  border: none;
  background: transparent;
  color: var(--accent);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  flex-shrink: 0;
}

.comment-locate-btn:hover {
  text-decoration: underline;
}

.comment-body {
  margin: 8px 0 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  color: var(--text-secondary);
}

.comment-children {
  margin-top: 8px;
  padding-left: 12px;
}

@supports (corner-shape: squircle) {
  .comment-item--child,
  .comment-avatar,
  .comment-quote {
    corner-shape: squircle;
  }
}
</style>
