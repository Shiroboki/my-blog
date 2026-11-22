# MyBlog（基于 youlai-boot + Vue 3 的个人博客系统）

采用 **MySQL + Redis + Spring Boot (`youlai-boot`) + Vue 3** 搭建的前后端分离个人博客系统，复用 `youlai-boot` 的登录认证、RBAC 权限、图形验证码、限流防重、IP 属地解析与文件上传基础设施。

## 目录结构

- `backend/`：`youlai-boot` 后端 + 博客模块（`com.youlai.boot.module.blog`）
- `admin/`：`vue3-element-admin` 管理后台 + 博客管理页面（文章/分类/标签/评论/站点设置）
- `web/`：面向读者的博客前台（Vue 3 + Markdown 代码高亮 + 目录导航 + 评论互动 + 归档）
- `nginx/default.conf`：统一网关配置（`/` 前台、`/admin/` 后台、`/api/` 与 `/uploads/` 后端代理）
- `docker-compose.yml`：一键编排 MySQL 8.0 + Redis 7.2 + Spring Boot + Nginx

## 一键启动（Docker Compose）

1. **构建后端与前端产物**（如代码有修改）：
   ```bash
   # 后端打包
   docker run --rm -v myblog-m2:/root/.m2 -v $(pwd)/backend:/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn package
   # 前台与后台构建
   cd web && npm install && npm run build && cd ..
   cd admin && pnpm install && pnpm run build && cd ..
   ```
2. **启动全套服务**：
   ```bash
   docker compose up -d
   ```
3. **访问地址**：
   - 博客前台：<http://localhost:9123/>
   - 管理后台：<http://localhost:9123/admin/>（默认账号 `admin` / `123456`）
   - 后端 Swagger：<http://localhost:8000/swagger-ui.html>

> 公网部署前请修改 `.env` 中的 `MYSQL_ROOT_PASSWORD`、`REDIS_PASSWORD` 与 `JWT_SECRET_KEY`，并在登录后台后立即修改管理员密码。
