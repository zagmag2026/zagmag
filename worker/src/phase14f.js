import core from "./phase14e.js";

const PUBLIC_SHELL_MARKER = "worker-r1";

async function servePublicShell(request, env) {
  const sourceUrl = new URL(request.url);
  const assetUrl = new URL("/index.html", sourceUrl.origin);
  const assetRequest = new Request(assetUrl.toString(), {
    method: request.method === "HEAD" ? "HEAD" : "GET",
    headers: { accept: request.headers.get("accept") || "text/html" }
  });
  const assetResponse = await env.ASSETS.fetch(assetRequest);
  const headers = new Headers(assetResponse.headers);
  headers.set("cache-control", "no-store, no-cache, must-revalidate, max-age=0");
  headers.set("cdn-cache-control", "no-store");
  headers.set("cloudflare-cdn-cache-control", "no-store");
  headers.set("pragma", "no-cache");
  headers.set("expires", "0");
  headers.set("x-zhagmag-shell", PUBLIC_SHELL_MARKER);
  headers.delete("etag");
  return new Response(request.method === "HEAD" ? null : assetResponse.body, {
    status: assetResponse.status,
    statusText: assetResponse.statusText,
    headers
  });
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if ((request.method === "GET" || request.method === "HEAD") && (url.pathname === "/" || url.pathname === "/index.html")) {
      return servePublicShell(request, env);
    }
    return core.fetch(request, env, ctx);
  }
};
