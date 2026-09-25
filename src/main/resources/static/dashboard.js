"use strict";

const state = {
    serverId: null,
    serverName: null,
    userId: null,
};

// --- tiny helpers ----------------------------------------------------------

async function api(path, options = {}) {
    const res = await fetch(path, { credentials: "include", ...options });
    if (res.status === 401) {
        window.location.href = "/index.html";
        throw new Error("Not authenticated");
    }
    if (!res.ok) {
        let message = `Request failed (${res.status})`;
        try {
            const body = await res.json();
            if (body && body.message) message = body.message;
        } catch (ignored) { /* non-JSON error body */ }
        throw new Error(message);
    }
    return res.status === 204 ? null : res.json();
}

function toast(message, kind = "info") {
    const container = document.getElementById("toast-container");
    const el = document.createElement("div");
    el.className = `toast toast-${kind}`;
    el.textContent = message;
    container.appendChild(el);
    setTimeout(() => el.classList.add("show"), 10);
    setTimeout(() => {
        el.classList.remove("show");
        setTimeout(() => el.remove(), 300);
    }, 3200);
}

function avatarUrl(user) {
    if (user.avatar) {
        const ext = user.avatar.startsWith("a_") ? "gif" : "png";
        return `https://cdn.discordapp.com/avatars/${user.id}/${user.avatar}.${ext}`;
    }
    // New username system uses (id >> 22) % 6; legacy uses discriminator % 5.
    let index;
    if (user.discriminator && user.discriminator !== "0") {
        index = parseInt(user.discriminator, 10) % 5;
    } else {
        index = Number((BigInt(user.id) >> 22n) % 6n);
    }
    return `https://cdn.discordapp.com/embed/avatars/${index}.png`;
}

function displayName(user) {
    if (user.globalName) return user.globalName;
    if (user.discriminator && user.discriminator !== "0") {
        return `${user.username}#${user.discriminator}`;
    }
    return user.username;
}

// --- servers ---------------------------------------------------------------

async function loadCurrentUser() {
    try {
        const me = await api("/api/me");
        document.getElementById("current-user").textContent = me.username;
    } catch (ignored) { /* header is non-critical */ }
}

async function loadServers() {
    const list = document.getElementById("server-list");
    list.innerHTML = "";
    let servers;
    try {
        servers = await api("/api/servers");
    } catch (e) {
        toast(e.message, "error");
        return;
    }

    if (!servers.length) {
        list.innerHTML = `<li class="muted server-empty">No servers where you're an admin and the bot is present. Invite the bot to a server to manage it.</li>`;
        return;
    }

    servers.forEach(server => {
        const li = document.createElement("li");
        const btn = document.createElement("button");
        btn.className = "server-button";
        btn.title = server.name;

        if (server.icon) {
            const img = document.createElement("img");
            img.src = `https://cdn.discordapp.com/icons/${server.id}/${server.icon}.png`;
            img.alt = server.name;
            img.className = "server-icon";
            btn.appendChild(img);
        } else {
            const badge = document.createElement("span");
            badge.className = "server-icon server-icon-fallback";
            badge.textContent = server.name.slice(0, 2).toUpperCase();
            btn.appendChild(badge);
        }
        const label = document.createElement("span");
        label.className = "server-label";
        label.textContent = server.name;
        btn.appendChild(label);

        btn.onclick = () => {
            document.querySelectorAll(".server-button").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            loadMembers(server.id, server.name);
        };
        li.appendChild(btn);
        list.appendChild(li);
    });
}

// --- members ---------------------------------------------------------------

async function loadMembers(serverId, serverName) {
    state.serverId = serverId;
    state.serverName = serverName;

    const heading = document.getElementById("server-name-heading");
    const count = document.getElementById("member-count");
    const memberList = document.getElementById("member-list");
    const emptyState = document.getElementById("empty-state");

    document.getElementById("toolbar").hidden = false;
    heading.textContent = serverName;
    count.textContent = "Loading members…";
    memberList.innerHTML = "";
    emptyState.textContent = "";

    let members;
    try {
        members = await api(`/api/servers/${serverId}/users`);
    } catch (e) {
        count.textContent = "";
        toast(e.message, "error");
        return;
    }

    count.textContent = `${members.length} member${members.length === 1 ? "" : "s"}`;
    if (!members.length) {
        emptyState.textContent = "No members to show.";
        return;
    }

    members.forEach(user => {
        const li = document.createElement("li");
        li.className = "member-row";

        const img = document.createElement("img");
        img.src = avatarUrl(user);
        img.alt = "avatar";
        img.className = "member-avatar";

        const name = document.createElement("span");
        name.className = "member-name";
        name.textContent = displayName(user);

        const actions = document.createElement("div");
        actions.className = "member-actions";
        actions.appendChild(actionButton("➕ Add role", "btn-small",
            () => openRoleModal(user.id, "add")));
        actions.appendChild(actionButton("➖ Remove role", "btn-small",
            () => openRoleModal(user.id, "remove")));
        actions.appendChild(actionButton("👢 Kick", "btn-small btn-danger",
            () => kickUser(user)));

        li.append(img, name, actions);
        memberList.appendChild(li);
    });
}

function actionButton(text, className, onClick) {
    const btn = document.createElement("button");
    btn.className = `btn ${className}`;
    btn.textContent = text;
    btn.onclick = onClick;
    return btn;
}

async function kickUser(user) {
    if (!confirm(`Kick ${displayName(user)} from ${state.serverName}?`)) return;
    try {
        const res = await api(`/api/servers/${state.serverId}/users/${user.id}`, { method: "DELETE" });
        toast(res.message || "User kicked.", "success");
        loadMembers(state.serverId, state.serverName);
    } catch (e) {
        toast(e.message, "error");
    }
}

// --- role add / remove modal ----------------------------------------------

async function openRoleModal(userId, mode) {
    state.userId = userId;
    const modal = document.getElementById("role-modal");
    const select = document.getElementById("role-select");
    const title = document.getElementById("role-modal-title");
    const confirmBtn = document.getElementById("confirm-role-btn");

    title.textContent = mode === "add" ? "Add role" : "Remove role";
    confirmBtn.textContent = mode === "add" ? "Add role" : "Remove role";
    modal.dataset.mode = mode;
    select.innerHTML = `<option>Loading…</option>`;
    modal.hidden = false;

    const excluded = new Set(["@everyone"]);
    try {
        const [allRoles, userRoles] = await Promise.all([
            api(`/api/servers/${state.serverId}/roles`),
            api(`/api/servers/${state.serverId}/users/${userId}/roles`),
        ]);
        const userRoleIds = new Set(userRoles.map(r => r.id));
        const options = allRoles
            .filter(r => !excluded.has(r.name))
            .filter(r => mode === "add" ? !userRoleIds.has(r.id) : userRoleIds.has(r.id));

        select.innerHTML = "";
        if (!options.length) {
            select.innerHTML = `<option value="">No roles available</option>`;
            confirmBtn.disabled = true;
            return;
        }
        confirmBtn.disabled = false;
        options.forEach(r => {
            const opt = document.createElement("option");
            opt.value = r.id;
            opt.textContent = r.name;
            select.appendChild(opt);
        });
    } catch (e) {
        toast(e.message, "error");
        closeModal("role-modal");
    }
}

document.getElementById("confirm-role-btn").onclick = async () => {
    const modal = document.getElementById("role-modal");
    const roleId = document.getElementById("role-select").value;
    const mode = modal.dataset.mode || "add";
    if (!state.serverId || !state.userId || !roleId) return;

    try {
        const res = await api(
            `/api/servers/${state.serverId}/users/${state.userId}/roles/${roleId}`,
            { method: mode === "add" ? "PATCH" : "DELETE" }
        );
        toast(res.message || "Done.", "success");
        closeModal("role-modal");
    } catch (e) {
        toast(e.message, "error");
    }
};

// --- create role modal -----------------------------------------------------

let permissionsCache = null;

async function renderPermissionChecklist() {
    const container = document.getElementById("perm-checklist");
    container.innerHTML = `<span class="muted">Loading permissions…</span>`;
    try {
        if (!permissionsCache) {
            permissionsCache = await api("/api/permissions");
        }
    } catch (e) {
        container.innerHTML = `<span class="muted">Could not load permissions.</span>`;
        return;
    }
    container.innerHTML = "";
    permissionsCache.forEach(perm => {
        const label = document.createElement("label");
        label.className = "perm-item";
        label.dataset.label = perm.label.toLowerCase();

        const cb = document.createElement("input");
        cb.type = "checkbox";
        cb.value = perm.name;

        const span = document.createElement("span");
        span.textContent = perm.label;

        label.append(cb, span);
        container.appendChild(label);
    });
}

document.getElementById("perm-filter").oninput = (e) => {
    const q = e.target.value.trim().toLowerCase();
    document.querySelectorAll("#perm-checklist .perm-item").forEach(item => {
        item.hidden = q && !item.dataset.label.includes(q);
    });
};

document.getElementById("create-role-btn").onclick = () => {
    document.getElementById("new-role-name").value = "";
    document.getElementById("perm-filter").value = "";
    document.getElementById("create-role-modal").hidden = false;
    renderPermissionChecklist();
};

document.getElementById("confirm-create-role-btn").onclick = async () => {
    const name = document.getElementById("new-role-name").value.trim();
    if (!name) {
        toast("Role name is required.", "error");
        return;
    }
    const permissions = Array.from(
        document.querySelectorAll("#perm-checklist input[type=checkbox]:checked")
    ).map(cb => cb.value);

    try {
        const role = await api(`/api/servers/${state.serverId}/roles`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name, permissions }),
        });
        toast(`Role created: ${role.name}`, "success");
        closeModal("create-role-modal");
    } catch (e) {
        toast(e.message, "error");
    }
};

// --- modal + logout wiring -------------------------------------------------

function closeModal(id) {
    document.getElementById(id).hidden = true;
}

document.querySelectorAll("[data-close-modal]").forEach(btn => {
    btn.onclick = () => closeModal(btn.dataset.closeModal);
});

document.getElementById("logout-btn").onclick = async () => {
    try {
        await api("/api/logout", { method: "POST" });
    } catch (ignored) { /* clear session client-side regardless */ }
    window.location.href = "/index.html";
};

// --- boot ------------------------------------------------------------------

loadCurrentUser();
loadServers();
