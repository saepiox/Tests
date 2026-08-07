/*
 * SaepioX local stand-in SPA.
 * Client-side hash routing + in-memory synthetic data. Test double only.
 */
(function () {
  "use strict";

  var state = {
    user: null,
    // local working copies of synthetic data
    agreements: [],
    users: [],
    selectedAgreement: null,
    selectedUser: null,
  };

  function $(id) { return document.getElementById(id); }
  function show(el) { if (el) el.hidden = false; }
  function hide(el) { if (el) el.hidden = true; }

  function api(method, url, body) {
    return fetch(url, {
      method: method,
      headers: { "Content-Type": "application/json" },
      body: body ? JSON.stringify(body) : undefined,
    }).then(function (r) {
      return r.json().then(function (j) { return { ok: r.ok, status: r.status, data: j }; });
    });
  }

  // -- auth ----------------------------------------------------------------
  function doLogin() {
    var login = $("login").value;
    var password = $("password").value;
    api("POST", "/api/login", { login: login, password: password }).then(function (res) {
      if (res.ok && res.data.ok) {
        state.user = res.data;
        try { localStorage.setItem("saepiox_user", JSON.stringify(res.data)); } catch (e) {}
        $("login-error").hidden = true;
        enterShell();
      } else {
        $("login-error").hidden = false;
      }
    });
  }

  function doLogout() {
    state.user = null;
    try { localStorage.removeItem("saepiox_user"); } catch (e) {}
    hide($("view-shell"));
    show($("view-login"));
    $("login").value = "";
    $("password").value = "";
    location.hash = "#!login";
  }

  function enterShell() {
    hide($("view-login"));
    show($("view-shell"));
    $("who").textContent = state.user.displayName + " (" + state.user.source + ")";
    // seed local synthetic data
    api("GET", "/api/agreements").then(function (res) {
      state.agreements = (res.data.agreements || []).slice();
      renderDashboard();
      renderAgreements();
    });
    state.users = [
      { email: "tj@saepiox.com", userName: "tj", name: "TJ (QA Admin)" },
    ];
    renderUsers();
    if (!location.hash || location.hash === "#!login") location.hash = "#!dashboard";
    else route();
  }

  // -- routing -------------------------------------------------------------
  var PAGES = ["dashboard", "agreement", "user-admin", "tryg"];
  function route() {
    if (!state.user) { show($("view-login")); hide($("view-shell")); return; }
    var name = (location.hash || "#!dashboard").replace("#!", "");
    if (PAGES.indexOf(name) === -1) name = "dashboard";
    PAGES.forEach(function (p) {
      var page = $("page-" + p);
      if (page) page.hidden = (p !== name);
    });
    document.querySelectorAll(".topbar nav a").forEach(function (a) {
      a.classList.toggle("active", a.getAttribute("data-route") === name);
    });
  }

  // -- dashboard -----------------------------------------------------------
  function renderDashboard() {
    var tb = $("dashboard-rows");
    tb.innerHTML = "";
    state.agreements.forEach(function (a) {
      var tr = document.createElement("tr");
      tr.innerHTML = "<td>" + a.name + "</td><td>" + a.type + "</td><td>" +
        a.currency + "</td><td>" + a.risk + "</td>";
      tb.appendChild(tr);
    });
  }

  // -- agreements ----------------------------------------------------------
  function renderAgreements(filter) {
    var tb = $("agreement-rows");
    tb.innerHTML = "";
    state.agreements
      .filter(function (a) { return !filter || a.name.toLowerCase().indexOf(filter.toLowerCase()) !== -1; })
      .forEach(function (a) {
        var tr = document.createElement("tr");
        tr.innerHTML = "<td>" + a.name + "</td><td>" + a.type + "</td><td>" + a.currency + "</td>";
        tr.addEventListener("click", function () { selectAgreement(a); });
        tb.appendChild(tr);
      });
  }
  function selectAgreement(a) {
    state.selectedAgreement = a;
    $("name").value = a.name;
    $("type").value = a.type;
    $("currency").value = a.currency;
    $("risk").value = a.risk;
    $("term").value = a.term;
  }
  function addAgreement() {
    state.selectedAgreement = null;
    $("name").disabled = false;
    $("name").value = "";
    $("type").value = "";
    $("currency").value = "";
    $("risk").value = "";
    $("term").value = "";
    $("name").focus();
  }
  function saveAgreement() {
    var name = $("name").value.trim();
    if (!name) return;
    var existing = state.agreements.filter(function (a) { return a.name === name; })[0];
    var rec = existing || {};
    rec.name = name;
    rec.type = $("type").value || "ASSET_MANAGER";
    rec.currency = $("currency").value || "DKK";
    rec.risk = $("risk").value || "0";
    rec.term = $("term").value || "0";
    if (!existing) state.agreements.push(rec);
    $("name").disabled = true;
    renderAgreements($("search").value);
    renderDashboard();
  }
  function deleteAgreement() {
    if (!state.selectedAgreement) return;
    state.agreements = state.agreements.filter(function (a) { return a !== state.selectedAgreement; });
    state.selectedAgreement = null;
    renderAgreements($("search").value);
    renderDashboard();
  }

  // -- users ---------------------------------------------------------------
  function renderUsers() {
    var tb = $("user-rows");
    tb.innerHTML = "";
    state.users.forEach(function (u) {
      var tr = document.createElement("tr");
      tr.innerHTML = "<td>" + u.email + "</td><td>" + u.userName + "</td><td>" + u.name + "</td>";
      tr.addEventListener("click", function () { state.selectedUser = u; });
      tb.appendChild(tr);
    });
  }
  function addUser() {
    var email = $("email").value.trim();
    var userName = $("userName").value.trim();
    var name = $("user-name").value.trim();
    if (!email && !userName && !name) return;
    state.users.push({ email: email, userName: userName, name: name });
    $("email").value = "";
    $("userName").value = "";
    $("user-name").value = "";
    $("user-password").value = "";
    renderUsers();
  }
  function deleteUser() {
    if (!state.selectedUser) return;
    state.users = state.users.filter(function (u) { return u !== state.selectedUser; });
    state.selectedUser = null;
    renderUsers();
  }

  // -- Tryg integration (NEW) ---------------------------------------------
  function loadTryg() {
    var status = $("tryg-status");
    status.textContent = "loading";
    status.className = "pill loading";
    api("GET", "/api/tryg/policies").then(function (res) {
      var tb = $("tryg-rows");
      tb.innerHTML = "";
      (res.data.policies || []).forEach(function (p) {
        var tr = document.createElement("tr");
        tr.innerHTML = "<td>" + p.policyId + "</td><td>" + p.product + "</td><td>" +
          p.currency + "</td><td>" + p.premium + "</td><td>" + p.status + "</td>";
        tb.appendChild(tr);
      });
      status.textContent = "loaded " + (res.data.policies || []).length;
      status.className = "pill ok";
    });
  }

  // -- wire up -------------------------------------------------------------
  function init() {
    $("button-submit").addEventListener("click", doLogin);
    $("password").addEventListener("keydown", function (e) { if (e.key === "Enter") doLogin(); });
    $("logout").addEventListener("click", doLogout);
    $("add").addEventListener("click", addAgreement);
    $("update").addEventListener("click", saveAgreement);
    $("delete").addEventListener("click", deleteAgreement);
    $("search").addEventListener("keydown", function (e) {
      if (e.key === "Enter") renderAgreements($("search").value);
    });
    $("add-user").addEventListener("click", addUser);
    $("update-user").addEventListener("click", addUser);
    $("delete-user").addEventListener("click", deleteUser);
    $("tryg-connect").addEventListener("click", loadTryg);
    window.addEventListener("hashchange", route);
    if (!restoreSession()) route();
  }

  // Restore an existing session after a full page reload (the harness navigates
  // via navigate().to(host + "#!page"), which reloads the page). Mirrors a real
  // SPA that keeps its session token client-side.
  function restoreSession() {
    try {
      var u = JSON.parse(localStorage.getItem("saepiox_user"));
      if (u) { state.user = u; enterShell(); return true; }
    } catch (e) {}
    return false;
  }

  document.addEventListener("DOMContentLoaded", init);
})();
