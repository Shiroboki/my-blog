const DEFAULT_SITE_NAME = "MyBlog";
const DEFAULT_HOME_TITLE = "MyBlog · 个人技术博客";
const DEFAULT_DESCRIPTION = "基于 Spring Boot + Vue + MySQL + Redis 构建的个人技术博客";

let configuredSiteTitle = "";
let configuredSiteSubtitle = "";

export interface SeoMetaOptions {
  title?: string;
  description?: string;
  image?: string;
  path?: string;
  type?: "website" | "article";
}

function upsertMetaTag(attrName: "name" | "property", attrValue: string, content: string) {
  let meta = document.querySelector(`meta[${attrName}="${attrValue}"]`) as HTMLMetaElement | null;
  if (!meta) {
    meta = document.createElement("meta");
    meta.setAttribute(attrName, attrValue);
    document.head.appendChild(meta);
  }
  meta.content = content;
}

function removeMetaTag(attrName: "name" | "property", attrValue: string) {
  document.querySelector(`meta[${attrName}="${attrValue}"]`)?.remove();
}

function upsertCanonical(href: string) {
  let link = document.querySelector('link[rel="canonical"]') as HTMLLinkElement | null;
  if (!link) {
    link = document.createElement("link");
    link.rel = "canonical";
    document.head.appendChild(link);
  }
  link.href = href;
}

export function setSeoSiteConfig(siteTitle?: string, siteSubtitle?: string) {
  configuredSiteTitle = siteTitle?.trim() || "";
  configuredSiteSubtitle = siteSubtitle?.trim() || "";
}

export function updatePageSeo(
  titleOrOptions?: string | SeoMetaOptions,
  description?: string,
) {
  const opts: SeoMetaOptions =
    typeof titleOrOptions === "string" || titleOrOptions === undefined
      ? { title: titleOrOptions, description }
      : titleOrOptions;

  const siteLabel = configuredSiteTitle || DEFAULT_SITE_NAME;
  const pageTitle = opts.title?.trim();
  const fullTitle = pageTitle
    ? `${pageTitle} - ${siteLabel}`
    : configuredSiteTitle || DEFAULT_HOME_TITLE;

  const cleanDesc = (
    opts.description?.trim() ||
    configuredSiteSubtitle ||
    DEFAULT_DESCRIPTION
  ).slice(0, 180);

  const canonicalUrl =
    typeof window !== "undefined"
      ? `${window.location.origin}${opts.path || window.location.pathname}`
      : opts.path || "/";

  document.title = fullTitle;
  upsertMetaTag("name", "description", cleanDesc);
  upsertMetaTag("property", "og:site_name", siteLabel);
  upsertMetaTag("property", "og:type", opts.type || "website");
  upsertMetaTag("property", "og:title", fullTitle);
  upsertMetaTag("property", "og:description", cleanDesc);
  upsertMetaTag("property", "og:url", canonicalUrl);

  const imageUrl = opts.image?.trim();
  if (imageUrl) {
    const absImage =
      typeof window !== "undefined" && imageUrl.startsWith("/")
        ? `${window.location.origin}${imageUrl}`
        : imageUrl;
    upsertMetaTag("property", "og:image", absImage);
  } else {
    removeMetaTag("property", "og:image");
  }

  upsertCanonical(canonicalUrl);
}
