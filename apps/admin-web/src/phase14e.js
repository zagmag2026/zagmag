import "./phase14e.css";
import "./phase14d.js";

const uploadedAssets = new Map();
const uploaderState = new WeakMap();
const priorFetch = globalThis.fetch.bind(globalThis);
const CLIENT_MAX_IMAGE_BYTES = 8 * 1024 * 1024;

let activeTextarea = null;
let activeShell = null;
let activeLabelObserver = null;
let activeMainObserver = null;
let observedMain = null;
let scanQueued = false;

function urls(textarea) {
  return String(textarea?.value || "").split(/\n|,/).map((value) => value.trim()).filter(Boolean).slice(0, 8);
}

function stateFor(textarea) {
  let state = uploaderState.get(textarea);
  if (!state) {
    state = { urls: urls(textarea), dirty: false };
    uploaderState.set(textarea, state);
  }
  return state;
}

function effectiveUrls(textarea) {
  const state = stateFor(textarea);
  return state.dirty ? [...state.urls] : urls(textarea);
}

function findPhotoTextarea() {
  const form = document.querySelector(".item-editor > form.form-card, .item-editor form.form-card");
  if (!form) return null;
  const marked = form.querySelector('textarea[data-zhagmag-item-photos="1"]');
  if (marked) return marked;
  const textareas = [...form.querySelectorAll("textarea")];
  return textareas.length === 1 ? textareas[0] : null;
}

globalThis.fetch = async (input, init = {}) => {
  let itemTextarea = null;
  let itemMutation = false;
  try {
    const url = typeof input === "string" ? input : input?.url || "";
    const method = String(init.method || (typeof input !== "string" ? input?.method : "GET") || "GET").toUpperCase();
    itemMutation = (method === "POST" || method === "PUT") && /\/api\/admin\/items(?:\/[^/?#]+)?(?:\?|$)/.test(url) && typeof init.body === "string";
    if (itemMutation) {
      const body = JSON.parse(init.body);
      itemTextarea = findPhotoTextarea();
      const authoritativeImageUrls = itemTextarea
        ? effectiveUrls(itemTextarea)
        : (Array.isArray(body.imageUrls) ? body.imageUrls.map((value) => String(value).trim()).filter(Boolean).slice(0, 8) : []);
      body.imageUrls = authoritativeImageUrls;
      body.cloudinaryAssets = authoritativeImageUrls
        .map((photoUrl) => ({ url: photoUrl, publicId: uploadedAssets.get(String(photoUrl)) || "" }))
        .filter((row) => row.publicId);
      init = { ...init, body: JSON.stringify(body) };
    }
  } catch {}
  const response = await priorFetch(input, init);
  if (itemMutation && response.ok && itemTextarea) {
    const state = stateFor(itemTextarea);
    state.dirty = false;
    window.setTimeout(() => {
      if (!document.contains(itemTextarea)) return;
      state.urls = urls(itemTextarea);
      if (activeTextarea === itemTextarea && activeShell) renderUploader(itemTextarea, activeShell);
    }, 0);
  }
  return response;
};

function setReactTextarea(textarea, values) {
  const normalized = [...new Set(values.map((value) => String(value).trim()).filter(Boolean))].slice(0, 8);
  const state = stateFor(textarea);
  state.urls = normalized;
  state.dirty = true;
  const setter = Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, "value")?.set;
  setter?.call(textarea, normalized.join("\n"));
  textarea.dispatchEvent(new Event("input", { bubbles: true }));
  textarea.dispatchEvent(new Event("change", { bubbles: true }));
}

function looksLikeImage(file) {
  if (String(file?.type || "").toLowerCase().startsWith("image/")) return true;
  return /\.(?:jpe?g|png|webp|gif|heic|heif|avif)$/i.test(String(file?.name || ""));
}

function validateFileBeforeSigning(file) {
  if (!looksLikeImage(file)) throw new Error(`${file.name || "Selected file"} is not a supported image.`);
  if (Number(file.size || 0) > CLIENT_MAX_IMAGE_BYTES) throw new Error(`${file.name || "Selected image"} is larger than 8 MB.`);
}

function validateSignaturePayload(signed) {
  if (!signed || signed.ok !== true || !signed.cloudName || !signed.apiKey || !signed.timestamp || !signed.folder || !signed.signature) {
    throw new Error("Image upload configuration response is incomplete. Please try again.");
  }
}

async function signedUpload(file) {
  validateFileBeforeSigning(file);
  const signRes = await fetch("/api/admin/cloudinary/signature", {
    method: "POST",
    credentials: "include",
    headers: { "content-type": "application/json" },
    body: "{}"
  });
  const signed = await signRes.json().catch(() => ({}));
  if (!signRes.ok) throw new Error(signed.message || "Unable to prepare image upload.");
  validateSignaturePayload(signed);
  const maxBytes = Number(signed.maxBytes || CLIENT_MAX_IMAGE_BYTES);
  if (file.size > maxBytes) throw new Error(`${file.name} is larger than ${Math.round(maxBytes / 1024 / 1024)} MB.`);

  const body = new FormData();
  body.set("file", file);
  body.set("api_key", String(signed.apiKey));
  body.set("timestamp", String(signed.timestamp));
  body.set("folder", String(signed.folder));
  body.set("signature", String(signed.signature));

  let uploadRes;
  try {
    uploadRes = await priorFetch(`https://api.cloudinary.com/v1_1/${encodeURIComponent(String(signed.cloudName))}/image/upload`, { method: "POST", body });
  } catch {
    throw new Error("Cloudinary could not be reached. Check the internet connection and try again.");
  }
  const result = await uploadRes.json().catch(() => ({}));
  if (!uploadRes.ok || !result.secure_url || !result.public_id) {
    throw new Error(result?.error?.message || `Upload failed for ${file.name}.`);
  }
  uploadedAssets.set(String(result.secure_url), String(result.public_id));
  return String(result.secure_url);
}

function setUploaderError(shell, message = "") {
  const node = shell?.querySelector(".phase14e-error");
  if (!node) return;
  node.textContent = message;
  node.hidden = !message;
}

function setUploadLabel(shell, text) {
  const node = shell?.querySelector(".phase14e-upload-label");
  if (node) node.textContent = text;
}

function renderUploader(textarea, shell) {
  if (!textarea || !shell) return;
  const list = effectiveUrls(textarea);
  const grid = shell.querySelector(".phase14e-images");
  const empty = shell.querySelector(".phase14e-empty");
  const choose = shell.querySelector(".phase14e-upload-btn");
  const input = shell.querySelector('input[type="file"]');
  if (!grid || !empty || !choose || !input) return;
  const blocked = list.length >= 8 || input.disabled;
  choose.classList.toggle("disabled", blocked);
  choose.setAttribute("aria-disabled", blocked ? "true" : "false");
  grid.innerHTML = "";
  empty.hidden = list.length > 0;
  list.forEach((url, index) => {
    const card = document.createElement("article"); card.className = "phase14e-image";
    const preview = document.createElement("div"); preview.className = "phase14e-preview";
    const img = document.createElement("img"); img.src = url; img.alt = `Item photo ${index + 1}`; img.loading = "lazy"; preview.append(img);
    if (index === 0) {
      const badge = document.createElement("span"); badge.className = "phase14e-primary"; badge.textContent = "Primary"; preview.append(badge);
    }
    const actions = document.createElement("div"); actions.className = "phase14e-actions";
    if (index > 0) {
      const primary = document.createElement("button"); primary.type = "button"; primary.textContent = "Make Primary";
      primary.onclick = () => {
        const next = effectiveUrls(textarea);
        const [picked] = next.splice(index, 1);
        next.unshift(picked);
        setReactTextarea(textarea, next);
        renderUploader(textarea, shell);
      };
      actions.append(primary);
    }
    const remove = document.createElement("button"); remove.type = "button"; remove.className = "remove"; remove.textContent = "Remove";
    remove.onclick = () => {
      const next = effectiveUrls(textarea);
      next.splice(index, 1);
      setReactTextarea(textarea, next);
      renderUploader(textarea, shell);
    };
    actions.append(remove);
    card.append(preview, actions);
    grid.append(card);
  });
}

function mountShell(textarea, shell) {
  if (!textarea?.isConnected || !shell) return;
  const label = textarea.closest("label");
  if (!label) return;
  if (!shell.isConnected || shell.parentElement !== label) textarea.insertAdjacentElement("afterend", shell);
}

function createUploaderShell(textarea) {
  const shell = document.createElement("div");
  shell.className = "phase14e-uploader";
  shell.dataset.phase14eExternal = "1";
  shell.innerHTML = `<div class="phase14e-upload-head"><div><strong>Item Photos</strong><small>Cloudinary secure upload · first image is Primary</small></div><label class="phase14e-upload-btn"><span class="phase14e-upload-label">+ Choose Photos</span><input type="file" accept="image/*,.heic,.heif,.avif" multiple></label></div><div class="phase14e-error" role="alert" hidden></div><div class="phase14e-images"></div><div class="phase14e-empty">No image selected. Item will still show with a clean placeholder.</div><small class="phase14e-help">Up to 8 images · max 8 MB each · one image is enough · images are optional.</small>`;

  const fileInput = shell.querySelector('input[type="file"]');
  fileInput.addEventListener("change", async () => {
    setUploaderError(shell, "");
    const current = effectiveUrls(textarea);
    const remaining = Math.max(0, 8 - current.length);
    const selected = [...(fileInput.files || [])];
    const files = selected.slice(0, remaining);
    if (!files.length) {
      if (selected.length && remaining === 0) setUploaderError(shell, "Maximum 8 item photos are allowed.");
      fileInput.value = "";
      return;
    }

    fileInput.disabled = true;
    renderUploader(textarea, shell);
    try {
      for (let index = 0; index < files.length; index += 1) {
        setUploadLabel(shell, `Uploading ${index + 1}/${files.length}…`);
        const uploadedUrl = await signedUpload(files[index]);
        current.push(uploadedUrl);
        // Commit each successful upload immediately so a later file failure cannot
        // orphan already-uploaded photos from the Item form/save payload.
        setReactTextarea(textarea, current.slice(0, 8));
        mountShell(textarea, shell);
        renderUploader(textarea, shell);
      }
      if (selected.length > files.length) setUploaderError(shell, `Only ${files.length} photo(s) were added because the 8-photo limit was reached.`);
    } catch (error) {
      setUploaderError(shell, error instanceof Error ? error.message : "Image upload failed.");
    } finally {
      fileInput.value = "";
      fileInput.disabled = false;
      setUploadLabel(shell, "+ Choose Photos");
      mountShell(textarea, shell);
      renderUploader(textarea, shell);
    }
  });

  textarea.addEventListener("input", (event) => {
    if (event.isTrusted) {
      const state = stateFor(textarea);
      state.urls = urls(textarea);
      state.dirty = true;
    }
    renderUploader(textarea, shell);
  });
  return shell;
}

function disconnectActive() {
  activeLabelObserver?.disconnect();
  activeLabelObserver = null;
  if (activeShell?.isConnected) activeShell.remove();
  activeTextarea = null;
  activeShell = null;
}

function observeReactOwnedLabel(textarea, shell) {
  activeLabelObserver?.disconnect();
  const label = textarea.closest("label");
  if (!label) return;
  activeLabelObserver = new MutationObserver(() => {
    if (activeTextarea !== textarea) return;
    if (!textarea.isConnected) {
      scheduleScan();
      return;
    }
    if (!shell.isConnected) queueMicrotask(() => {
      if (activeTextarea === textarea && textarea.isConnected) {
        mountShell(textarea, shell);
        renderUploader(textarea, shell);
      }
    });
  });
  activeLabelObserver.observe(label, { childList: true });
}

function enhancePhotoField(textarea) {
  if (activeTextarea === textarea && activeShell) {
    mountShell(textarea, activeShell);
    renderUploader(textarea, activeShell);
    return;
  }
  disconnectActive();
  activeTextarea = textarea;
  textarea.dataset.zhagmagItemPhotos = "1";
  textarea.classList.add("phase14e-hidden-url");
  stateFor(textarea);
  activeShell = createUploaderShell(textarea);
  mountShell(textarea, activeShell);
  observeReactOwnedLabel(textarea, activeShell);
  renderUploader(textarea, activeShell);
}

function scan() {
  scanQueued = false;
  const textarea = findPhotoTextarea();
  if (!textarea) {
    disconnectActive();
    return;
  }
  enhancePhotoField(textarea);
}

function scheduleScan() {
  if (scanQueued) return;
  scanQueued = true;
  requestAnimationFrame(scan);
}

function ensureMainObserver() {
  const main = document.querySelector(".shell > main");
  if (main === observedMain) return;
  activeMainObserver?.disconnect();
  observedMain = main;
  activeMainObserver = null;
  if (!main) return;
  activeMainObserver = new MutationObserver(scheduleScan);
  activeMainObserver.observe(main, { childList: true });
}

document.addEventListener("click", (event) => {
  const target = event.target instanceof Element ? event.target : null;
  if (!target?.closest(".tabs")) return;
  requestAnimationFrame(() => {
    ensureMainObserver();
    scan();
  });
}, false);
window.addEventListener("phase14ar-admin-ready", () => {
  ensureMainObserver();
  scheduleScan();
});
requestAnimationFrame(() => {
  ensureMainObserver();
  scan();
});
