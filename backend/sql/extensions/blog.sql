-- 个人博客模块扩展：建表、菜单权限与初始示例数据，可重复执行
-- 需先执行 ../youlai-admin.sql

USE youlai_admin;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 表结构：blog_category（文章分类表）
-- ----------------------------
DROP TABLE IF EXISTS `blog_category`;
CREATE TABLE `blog_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL COMMENT '分类名称',
  `slug` varchar(64) NOT NULL COMMENT 'URL别名',
  `description` varchar(255) DEFAULT NULL COMMENT '分类描述',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序（升序）',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除(0未删 1已删)',
  PRIMARY KEY (`id`),
  KEY `idx_blog_category_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客分类表';

-- ----------------------------
-- 表结构：blog_tag（文章标签表）
-- ----------------------------
DROP TABLE IF EXISTS `blog_tag`;
CREATE TABLE `blog_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL COMMENT '标签名称',
  `slug` varchar(64) NOT NULL COMMENT 'URL别名',
  `color` varchar(32) DEFAULT NULL COMMENT '标签颜色',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除(0未删 1已删)',
  PRIMARY KEY (`id`),
  KEY `idx_blog_tag_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客标签表';

-- ----------------------------
-- 表结构：blog_article（文章表）
-- ----------------------------
DROP TABLE IF EXISTS `blog_article`;
CREATE TABLE `blog_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(200) NOT NULL COMMENT '文章标题',
  `slug` varchar(128) DEFAULT NULL COMMENT '文章别名',
  `summary` varchar(500) DEFAULT NULL COMMENT '文章摘要',
  `content` longtext NOT NULL COMMENT 'Markdown 正文',
  `cover_image` varchar(500) DEFAULT NULL COMMENT '封面图URL',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态(0草稿 1已发布)',
  `is_top` tinyint NOT NULL DEFAULT 0 COMMENT '是否置顶(0否 1是)',
  `allow_comment` tinyint NOT NULL DEFAULT 1 COMMENT '是否允许评论(0否 1是)',
  `view_count` bigint NOT NULL DEFAULT 0 COMMENT '浏览量',
  `like_count` bigint NOT NULL DEFAULT 0 COMMENT '点赞数',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除(0未删 1已删)',
  PRIMARY KEY (`id`),
  KEY `idx_blog_article_category` (`category_id`),
  KEY `idx_blog_article_status_top_pub` (`status`, `is_top`, `publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客文章表';

-- ----------------------------
-- 表结构：blog_article_tag（文章标签关联表）
-- ----------------------------
DROP TABLE IF EXISTS `blog_article_tag`;
CREATE TABLE `blog_article_tag` (
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`article_id`, `tag_id`),
  KEY `idx_blog_article_tag_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客文章标签关联表';

-- ----------------------------
-- 表结构：blog_comment（评论表）
-- ----------------------------
DROP TABLE IF EXISTS `blog_comment`;
CREATE TABLE `blog_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父评论ID（0为一级评论）',
  `nickname` varchar(64) NOT NULL COMMENT '昵称',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `website` varchar(255) DEFAULT NULL COMMENT '个人主页',
  `content` varchar(1000) NOT NULL COMMENT '评论内容',
  `quote_text` varchar(500) DEFAULT NULL COMMENT '划词引用原文',
  `quote_start` int DEFAULT NULL COMMENT '划词起始字符偏移',
  `quote_end` int DEFAULT NULL COMMENT '划词结束字符偏移',
  `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP地址',
  `ip_region` varchar(128) DEFAULT NULL COMMENT 'IP属地',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态(0待审核 1已通过 2已拒绝)',
  `is_admin` tinyint NOT NULL DEFAULT 0 COMMENT '是否博主回复(0否 1是)',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除(0未删 1已删)',
  PRIMARY KEY (`id`),
  KEY `idx_blog_comment_article_status` (`article_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客评论表';

-- ----------------------------
-- 表结构：blog_config（博客站点配置表）
-- ----------------------------
DROP TABLE IF EXISTS `blog_config`;
CREATE TABLE `blog_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(64) NOT NULL COMMENT '配置键',
  `config_value` text NOT NULL COMMENT '配置值',
  `config_desc` varchar(255) DEFAULT NULL COMMENT '配置描述',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除(0未删 1已删)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_blog_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客站点配置表';

-- ----------------------------
-- 表结构：blog_link（友情链接表）
-- ----------------------------
DROP TABLE IF EXISTS `blog_link`;
CREATE TABLE `blog_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL COMMENT '站点名称',
  `url` varchar(255) NOT NULL COMMENT '站点地址',
  `avatar` varchar(500) DEFAULT NULL COMMENT '站点头像/Logo',
  `description` varchar(255) DEFAULT NULL COMMENT '站点简介',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序（升序）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态(1启用 0停用)',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除(0未删 1已删)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客友情链接表';

-- ----------------------------
-- 菜单权限（扩展模块号段：93）
-- ----------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES
(93, 0, '0', '博客管理', 'C', '', '/blog', 'Layout', NULL, NULL, 1, 2, 'el-icon-Notebook', '/blog/overview', now(), now(), '{"badge":"BLOG"}'),
(9307, 93, '0,93', '博客概览', 'M', 'BlogOverview', 'overview', 'blog/overview/index', NULL, 1, 1, 0, 'el-icon-DataAnalysis', NULL, now(), now(), NULL),
(930701, 9307, '0,93,9307', '概览查看', 'B', NULL, '', NULL, 'blog:dashboard:view', NULL, 1, 1, '', NULL, now(), now(), NULL),
(9301, 93, '0,93', '文章管理', 'M', 'BlogArticle', 'article', 'blog/article/index', NULL, 1, 1, 1, 'el-icon-Document', NULL, now(), now(), NULL),
(930101, 9301, '0,93,9301', '文章查询', 'B', NULL, '', NULL, 'blog:article:list', NULL, 1, 1, '', NULL, now(), now(), NULL),
(930102, 9301, '0,93,9301', '文章新增', 'B', NULL, '', NULL, 'blog:article:create', NULL, 1, 2, '', NULL, now(), now(), NULL),
(930103, 9301, '0,93,9301', '文章修改', 'B', NULL, '', NULL, 'blog:article:update', NULL, 1, 3, '', NULL, now(), now(), NULL),
(930104, 9301, '0,93,9301', '文章删除', 'B', NULL, '', NULL, 'blog:article:delete', NULL, 1, 4, '', NULL, now(), now(), NULL),
(9302, 93, '0,93', '分类管理', 'M', 'BlogCategory', 'category', 'blog/category/index', NULL, 1, 1, 2, 'el-icon-FolderOpened', NULL, now(), now(), NULL),
(930201, 9302, '0,93,9302', '分类查询', 'B', NULL, '', NULL, 'blog:category:list', NULL, 1, 1, '', NULL, now(), now(), NULL),
(930202, 9302, '0,93,9302', '分类新增', 'B', NULL, '', NULL, 'blog:category:create', NULL, 1, 2, '', NULL, now(), now(), NULL),
(930203, 9302, '0,93,9302', '分类修改', 'B', NULL, '', NULL, 'blog:category:update', NULL, 1, 3, '', NULL, now(), now(), NULL),
(930204, 9302, '0,93,9302', '分类删除', 'B', NULL, '', NULL, 'blog:category:delete', NULL, 1, 4, '', NULL, now(), now(), NULL),
(9303, 93, '0,93', '标签管理', 'M', 'BlogTag', 'tag', 'blog/tag/index', NULL, 1, 1, 3, 'el-icon-CollectionTag', NULL, now(), now(), NULL),
(930301, 9303, '0,93,9303', '标签查询', 'B', NULL, '', NULL, 'blog:tag:list', NULL, 1, 1, '', NULL, now(), now(), NULL),
(930302, 9303, '0,93,9303', '标签新增', 'B', NULL, '', NULL, 'blog:tag:create', NULL, 1, 2, '', NULL, now(), now(), NULL),
(930303, 9303, '0,93,9303', '标签修改', 'B', NULL, '', NULL, 'blog:tag:update', NULL, 1, 3, '', NULL, now(), now(), NULL),
(930304, 9303, '0,93,9303', '标签删除', 'B', NULL, '', NULL, 'blog:tag:delete', NULL, 1, 4, '', NULL, now(), now(), NULL),
(9304, 93, '0,93', '评论管理', 'M', 'BlogComment', 'comment', 'blog/comment/index', NULL, 1, 1, 4, 'el-icon-ChatDotRound', NULL, now(), now(), NULL),
(930401, 9304, '0,93,9304', '评论查询', 'B', NULL, '', NULL, 'blog:comment:list', NULL, 1, 1, '', NULL, now(), now(), NULL),
(930402, 9304, '0,93,9304', '评论审核', 'B', NULL, '', NULL, 'blog:comment:audit', NULL, 1, 2, '', NULL, now(), now(), NULL),
(930403, 9304, '0,93,9304', '评论回复', 'B', NULL, '', NULL, 'blog:comment:reply', NULL, 1, 3, '', NULL, now(), now(), NULL),
(930404, 9304, '0,93,9304', '评论删除', 'B', NULL, '', NULL, 'blog:comment:delete', NULL, 1, 4, '', NULL, now(), now(), NULL),
(9305, 93, '0,93', '站点设置', 'M', 'BlogSetting', 'setting', 'blog/setting/index', NULL, 1, 1, 5, 'el-icon-Setting', NULL, now(), now(), NULL),
(930501, 9305, '0,93,9305', '设置查询', 'B', NULL, '', NULL, 'blog:config:list', NULL, 1, 1, '', NULL, now(), now(), NULL),
(930502, 9305, '0,93,9305', '设置保存', 'B', NULL, '', NULL, 'blog:config:update', NULL, 1, 2, '', NULL, now(), now(), NULL),
(9306, 93, '0,93', '友链管理', 'M', 'BlogLink', 'link', 'blog/link/index', NULL, 1, 1, 6, 'el-icon-Link', NULL, now(), now(), NULL),
(930601, 9306, '0,93,9306', '友链查询', 'B', NULL, '', NULL, 'blog:link:list', NULL, 1, 1, '', NULL, now(), now(), NULL),
(930602, 9306, '0,93,9306', '友链新增', 'B', NULL, '', NULL, 'blog:link:create', NULL, 1, 2, '', NULL, now(), now(), NULL),
(930603, 9306, '0,93,9306', '友链修改', 'B', NULL, '', NULL, 'blog:link:update', NULL, 1, 3, '', NULL, now(), now(), NULL),
(930604, 9306, '0,93,9306', '友链删除', 'B', NULL, '', NULL, 'blog:link:delete', NULL, 1, 4, '', NULL, now(), now(), NULL);

-- 授权给系统管理员角色（role_id=2）
INSERT IGNORE INTO `sys_role_menu` VALUES
(2, 93),
(2, 9307), (2, 930701),
(2, 9301), (2, 930101), (2, 930102), (2, 930103), (2, 930104),
(2, 9302), (2, 930201), (2, 930202), (2, 930203), (2, 930204),
(2, 9303), (2, 930301), (2, 930302), (2, 930303), (2, 930304),
(2, 9304), (2, 930401), (2, 930402), (2, 930403), (2, 930404),
(2, 9305), (2, 930501), (2, 930502),
(2, 9306), (2, 930601), (2, 930602), (2, 930603), (2, 930604);

-- ----------------------------
-- 初始站点配置
-- ----------------------------
INSERT IGNORE INTO `blog_config` (`config_key`, `config_value`, `config_desc`, `create_time`, `update_time`, `is_deleted`) VALUES
('site_title', 'MyBlog · 个人技术博客', '站点主标题', now(), now(), 0),
('site_subtitle', '基于 Spring Boot + Vue + MySQL + Redis 构建', '站点副标题', now(), now(), 0),
('author_name', '博主', '博主昵称', now(), now(), 0),
('author_bio', '记录全栈开发、架构实践与日常思考，保持输出与好奇心。', '博主简介', now(), now(), 0),
('author_avatar', 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '博主头像', now(), now(), 0),
('github_url', 'https://github.com/youlaitech/youlai-boot', 'GitHub 主页', now(), now(), 0),
('icp_number', '', 'ICP 备案号', now(), now(), 0),
('comment_audit', '0', '评论是否需要审核(0直接展示 1先审后发)', now(), now(), 0),
('giscus_enabled', '0', '是否启用 Giscus GitHub 讨论区(0否 1是)', now(), now(), 0),
('giscus_repo', '', 'Giscus GitHub 仓库(owner/repo)', now(), now(), 0),
('giscus_repo_id', '', 'Giscus Repository ID', now(), now(), 0),
('giscus_category', '', 'Giscus Discussion 分类名称', now(), now(), 0),
('giscus_category_id', '', 'Giscus Category ID', now(), now(), 0),
('giscus_mapping', 'pathname', 'Giscus 页面映射方式', now(), now(), 0),
('about_content', '# 关于本站\n\n这是一个基于 **youlai-boot**（Spring Boot + MySQL + Redis + Spring Security）与 **Vue 3** 搭建的个人博客系统。\n\n## 技术栈\n\n- **后端**：Spring Boot 4 · MyBatis-Plus · Spring Security · Redis · MySQL 8\n- **管理后台**：Vue 3 · TypeScript · Element Plus（基于 `vue3-element-admin`）\n- **博客前台**：Vue 3 · Vite · Markdown 代码高亮与目录导航\n\n欢迎在文章下方留言交流！', '关于页面 Markdown 内容', now(), now(), 0);

-- ----------------------------
-- 初始示例分类、标签、文章与评论
-- ----------------------------
INSERT IGNORE INTO `blog_category` (`id`, `name`, `slug`, `description`, `sort`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES
(1, '技术笔记', 'tech', '后端架构、数据库与前端工程实践记录', 1, now(), now(), 2, 2, 0),
(2, '随笔思考', 'life', '读书笔记、效率工具与个人随笔', 2, now(), now(), 2, 2, 0);

INSERT IGNORE INTO `blog_tag` (`id`, `name`, `slug`, `color`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES
(1, 'Spring Boot', 'spring-boot', '#6DB33F', now(), now(), 2, 2, 0),
(2, 'Vue 3', 'vue3', '#42B883', now(), now(), 2, 2, 0),
(3, 'MySQL', 'mysql', '#4479A1', now(), now(), 2, 2, 0),
(4, 'Redis', 'redis', '#DC382D', now(), now(), 2, 2, 0);

INSERT IGNORE INTO `blog_article` (`id`, `title`, `slug`, `summary`, `content`, `cover_image`, `category_id`, `status`, `is_top`, `allow_comment`, `view_count`, `like_count`, `publish_time`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES
(1, '欢迎来到我的个人博客：基于 youlai-boot 全栈搭建记录', 'welcome-to-myblog', '记录如何基于 youlai-boot（MySQL + Redis + Spring Boot）与 Vue 3 快速落地前后端分离的个人博客，避免重复造轮子。', '# 为什么选择 youlai-boot 作为博客底座？\n\n搭建个人博客时，账号鉴权、RBAC 权限、验证码、Redis 会话、接口限流与文件上传都是通用基础设施。直接复用 **youlai-boot** 可以把精力集中在博客内容本身。\n\n## 已集成的核心能力\n\n1. **MySQL 持久化**：文章、分类、标签、评论与站点配置独立建模。\n2. **Redis 加速与防刷**：\n   - 站点统计与已发布文章详情缓存\n   - 阅读量（UV）与点赞防重计数\n   - 匿名评论提交滑动窗口限流\n3. **Markdown 写作**：后台支持实时预览与图片上传。\n\n```java\n@RestController\n@RequestMapping(\"/api/v1/blog/public\")\npublic class BlogPublicController {\n    // 匿名可读，受 @RateLimit 保护\n}\n```\n\n> 提示：你可以登录后台 `/admin/`（默认账号 `admin` / `123456`）开始发布你的第一篇文章，并修改站点信息。', NULL, 1, 1, 1, 1, 42, 5, now(), now(), now(), 2, 2, 0),
(2, 'Redis 在个人博客中的三个实用场景', 'redis-in-personal-blog', '聊聊如何用 Redis 的 String、过期键防重以及滑动窗口限流，让个人博客兼顾性能与防刷保护。', '# Redis 在个人博客中的三个实用场景\n\n很多博客系统引入 Redis 只用来存登录 Session，其实还有三个非常顺手的用法：\n\n## 1. 热点文章详情与站点信息缓存\n\n博客是典型的**读多写少**场景。把文章正文和站点统计信息缓存到 Redis，编辑或删除时主动失效，能大幅减少数据库查询压力。\n\n## 2. 阅读量与点赞的短时防刷\n\n通过 `blog:article:view:uv:{articleId}:{ip}` 设置过期键，同一访客短时间内反复刷新页面不会让阅读量无限暴涨。\n\n## 3. 评论接口限流\n\n利用 Redis 滑动窗口限制单 IP 在 60 秒内的评论请求次数，简单有效地拦截垃圾灌水。', NULL, 1, 1, 0, 1, 18, 2, DATE_SUB(now(), INTERVAL 1 DAY), now(), now(), 2, 2, 0);

INSERT IGNORE INTO `blog_article_tag` (`article_id`, `tag_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4),
(2, 1), (2, 4);

INSERT IGNORE INTO `blog_comment` (`id`, `article_id`, `parent_id`, `nickname`, `email`, `website`, `content`, `ip_address`, `ip_region`, `status`, `is_admin`, `create_time`, `update_time`, `is_deleted`) VALUES
(1, 1, 0, '访客小王', 'visitor@example.com', NULL, '界面很清爽，期待后续更多技术文章！', '127.0.0.1', '本地回环', 1, 0, now(), now(), 0),
(2, 1, 1, '博主', NULL, NULL, '感谢关注，欢迎常来交流！', '127.0.0.1', '本地回环', 1, 1, now(), now(), 0);

INSERT IGNORE INTO `blog_link` (`id`, `name`, `url`, `avatar`, `description`, `sort`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES
(1, '有来技术文档', 'https://www.youlai.tech', NULL, 'youlai-boot 与 vue3-element-admin 官方文档', 1, 1, now(), now(), 2, 2, 0),
(2, 'Spring Boot', 'https://spring.io/projects/spring-boot', NULL, 'Java 企业级应用开发主流框架', 2, 1, now(), now(), 2, 2, 0);

