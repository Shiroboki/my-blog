import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": new URL("./src", import.meta.url).pathname,
    },
  },
  server: {
    host: "0.0.0.0",
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8000",
        changeOrigin: true,
      },
      "/uploads": {
        target: "http://localhost:8000",
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes("node_modules/vue") || id.includes("node_modules/vue-router")) {
            return "vendor-vue";
          }
          if (id.includes("node_modules/highlight.js")) {
            return "vendor-highlight";
          }
          if (id.includes("node_modules/marked") || id.includes("node_modules/dompurify")) {
            return "vendor-markdown";
          }
        },
      },
    },
  },
});
