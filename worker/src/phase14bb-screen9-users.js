import core from "./phase14ba-screen5-item-management.js";
import { getSessionUser, hasStaffPermission } from "./auth.ts";

function apiJson(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
      "referrer-policy": "no-referrer"
    }
  });
}

function requiredPermission(pathname) {
  if (pathname === "/api/admin/dashboard") return "DASHBOARD";
  if (
    pathname.startsWith("/api/admin/item-management") ||
    pathname.startsWith("/api/admin/related-items") ||
    pathname.startsWith("/api/admin/items") ||
    pathname.startsWith("/api/admin/categories") ||
    pathname.startsWith("/api/admin/category-fields")
  ) return "ITEMS";
  if (pathname.startsWith("/api/admin/customers")) return "CUSTOMERS";
  if (pathname.startsWith("/api/admin/bookings")) return "BOOKINGS";
  if (pathname.startsWith("/api/admin/pickups")) return "PICKUPS";
  if (pathname.startsWith("/api/admin/returns")) return "RETURNS";
  if (pathname.startsWith("/api/admin/reports")) return "REPORTS";
  return null;
}

function ownerOnly(pathname) {
  return (
    pathname.startsWith("/api/admin/users") ||
    pathname.startsWith("/api/admin/settings") ||
    pathname.startsWith("/api/admin/whatsapp-templates") ||
    pathname.startsWith("/api/admin/audit-logs")
  );
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (!url.pathname.startsWith("/api/admin/")) {
      return core.fetch(request, env, ctx);
    }

    const permission = requiredPermission(url.pathname);
    const protectedOwnerOnly = ownerOnly(url.pathname);
    if (!permission && !protectedOwnerOnly) {
      return core.fetch(request, env, ctx);
    }

    const user = await getSessionUser(env, request);
    if (!user) {
      return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
    }

    if (protectedOwnerOnly && user.role !== "OWNER") {
      return apiJson({ ok: false, error: "FORBIDDEN", message: "Owner access is required." }, 403);
    }

    if (permission && !hasStaffPermission(user, permission)) {
      return apiJson({ ok: false, error: "FORBIDDEN", message: "This module is not enabled for your Staff account." }, 403);
    }

    return core.fetch(request, env, ctx);
  }
};
