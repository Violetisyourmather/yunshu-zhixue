/* 云枢智学 · 原型内核：状态、路由、外壳、通用组件 */
(function () {
  'use strict';

  const U = {
    esc(s) {
      return String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    },
    num(n) { return Number(n || 0).toLocaleString('zh-CN'); },
    pct(n) { return Math.round((n || 0) * 100) + '%'; },
    money(n) { return n; },
    pad(n) { return String(n).padStart(2, '0'); },
    hms(sec) {
      sec = Math.max(0, Math.floor(sec || 0));
      return U.pad(Math.floor(sec / 3600)) + ':' + U.pad(Math.floor(sec / 60) % 60) + ':' + U.pad(sec % 60);
    },
    ms(sec) {
      sec = Math.max(0, Math.floor(sec || 0));
      return U.pad(Math.floor(sec / 60)) + ':' + U.pad(sec % 60);
    },
    now() {
      const d = new Date();
      return U.pad(d.getHours()) + ':' + U.pad(d.getMinutes());
    },
    date(iso) { return iso; },
    scoreColor(v) { return v >= 8 ? 'var(--jade)' : v >= 6 ? 'var(--amber)' : 'var(--red)'; },
    scoreClass(v) { return v >= 8 ? 'badge-jade' : v >= 6 ? 'badge-amber' : 'badge-red'; },
    qs(sel, root) { return (root || document).querySelector(sel); },
    qsa(sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); },
    on(el, ev, fn) { if (el) el.addEventListener(ev, fn); }
  };

  const App = {
    user: { name: '林知远', phone: '138****6621', email: 'lin@example.com', gender: '保密', avatar: '林', joined: '2026-09-12' },
    admin: null,
    practice: null,
    interview: null,
    report: null,
    lastReport: null,
    timers: [],
    afterRender: null,
    currentPath: '/'
  };

  /* ---------- 存储（原型内允许：用于续做 / 续答） ---------- */
  const Store = {
    get(k, d) { try { const v = localStorage.getItem(k); return v ? JSON.parse(v) : d; } catch (e) { return d; } },
    set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch (e) { } },
    del(k) { try { localStorage.removeItem(k); } catch (e) { } }
  };

  /* ---------- Toast ---------- */
  function toast(msg, type) {
    let box = document.querySelector('.toasts');
    if (!box) { box = document.createElement('div'); box.className = 'toasts'; document.body.appendChild(box); }
    const el = document.createElement('div');
    el.className = 'toast ' + (type || 'ok');
    el.innerHTML = '<span>' + (type === 'err' ? '✕' : type === 'warn' ? '!' : '✓') + '</span><span>' + U.esc(msg) + '</span>';
    box.appendChild(el);
    setTimeout(() => { el.style.transition = 'opacity .2s'; el.style.opacity = '0'; setTimeout(() => el.remove(), 220); }, 2200);
  }

  /* ---------- 弹窗 ---------- */
  let activeOverlay = null;
  function closeOverlay() {
    if (activeOverlay) { activeOverlay.remove(); activeOverlay = null; document.body.style.overflow = ''; }
  }
  function modal(opts) {
    closeOverlay();
    const ov = document.createElement('div');
    ov.className = 'overlay';
    ov.innerHTML = '<div class="modal ' + (opts.wide ? 'wide' : '') + '" role="dialog">' +
      '<div class="modal-head"><h3>' + U.esc(opts.title || '') + '</h3><button class="close-x" data-close>✕</button></div>' +
      '<div class="modal-body">' + (opts.body || '') + '</div>' +
      (opts.foot === false ? '' : '<div class="modal-foot">' + (opts.foot || '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-ok>确定</button>') + '</div>') +
      '</div>';
    document.body.appendChild(ov);
    document.body.style.overflow = 'hidden';
    activeOverlay = ov;
    ov.addEventListener('click', e => {
      if (e.target === ov || e.target.closest('[data-close]')) { closeOverlay(); if (opts.onCancel) opts.onCancel(); }
      if (e.target.closest('[data-ok]') && opts.onOk) opts.onOk(ov);
    });
    if (opts.onMount) opts.onMount(ov);
    return ov;
  }
  function confirmDialog(opts) {
    return modal({
      title: opts.title || '请确认',
      body: '<p class="lead">' + U.esc(opts.text || '') + '</p>',
      foot: '<button class="btn" data-close>取消</button><button class="btn ' + (opts.danger ? 'btn-danger' : 'btn-primary') + '" data-ok>' + U.esc(opts.okText || '确认') + '</button>',
      onOk: () => { closeOverlay(); if (opts.onOk) opts.onOk(); }
    });
  }
  function drawer(opts) {
    if (activeOverlay) closeOverlay();
    const ov = document.createElement('div');
    ov.className = 'drawer-overlay';
    const dw = document.createElement('aside');
    dw.className = 'drawer';
    dw.innerHTML = '<div class="drawer-head"><h3 class="serif" style="font-size:19px">' + U.esc(opts.title || '') + '</h3><button class="close-x" data-close>✕</button></div><div class="drawer-body">' + (opts.body || '') + '</div>';
    ov.appendChild(dw);
    document.body.appendChild(ov);
    document.body.style.overflow = 'hidden';
    activeOverlay = ov;
    ov.addEventListener('click', e => { if (e.target === ov || e.target.closest('[data-close]')) closeOverlay(); });
    if (opts.onMount) opts.onMount(dw);
  }

  /* ---------- 常用片段 ---------- */
  const C = {
    badge(text, cls) { return '<span class="badge ' + (cls || '') + '">' + U.esc(text) + '</span>'; },
    statusBadge(status) {
      const map = { '已上架': 'badge-jade', '已启用': 'badge-jade', '已掌握': 'badge-jade', '已下架': 'badge-amber', '待审核': 'badge-blue', '已驳回': 'badge-red', '已禁用': 'badge-red', '草稿': '', '待复习': 'badge-amber' };
      return '<span class="badge ' + (map[status] || '') + '">' + U.esc(status) + '</span>';
    },
    typeBadge(t) { return '<span class="badge">' + U.esc(DB.TYPE_NAME[t] || t) + '</span>'; },
    diffBadge(d) { return '<span class="badge ' + (d === '困难' ? 'badge-red' : d === '中等' ? 'badge-amber' : '') + '">' + U.esc(d) + '</span>'; },
    empty(text, icon) { return '<div class="empty"><div class="ico">' + (icon || '◌') + '</div><p>' + U.esc(text) + '</p></div>'; },
    pager(total, page, size, act) {
      const pages = Math.max(1, Math.ceil(total / size));
      let html = '<div class="pager"><span>共 ' + total + ' 条</span>';
      for (let i = 1; i <= Math.min(pages, 6); i++) {
        html += '<button class="' + (i === page ? 'on' : '') + '" data-act="' + act + '" data-page="' + i + '">' + i + '</button>';
      }
      html += '</div>';
      return html;
    }
  };

  /* ---------- 路由 ---------- */
  const Routes = {};
  function register(path, def) { Routes[path] = def; }
  function parseHash() {
    const raw = (location.hash || '').replace(/^#/, '') || '/';
    const [path, qs] = raw.split('?');
    return { path: path || '/', query: new URLSearchParams(qs || '') };
  }
  function navigate(path) {
    if (location.hash === '#' + path) { render(); return; }
    location.hash = path;
  }
  function clearTimers() {
    App.timers.forEach(t => clearInterval(t));
    App.timers = [];
  }
  function setTimer(fn, ms) {
    const id = setInterval(fn, ms);
    App.timers.push(id);
    return id;
  }
  function render() {
    clearTimers();
    const { path, query } = parseHash();
    App.currentPath = path;
    const def = Routes[path];
    if (!def) { document.getElementById('app').innerHTML = '<div class="page"><div class="wrap">' + C.empty('页面不存在') + '</div></div>'; return; }
    if (def.admin && !App.admin) {
      if (/demo=1/.test(location.search) || /demo=1/.test(location.hash)) { App.admin = DB.ADMIN_USERS[0]; }
      else { navigate('/admin/login'); return; }
    }
    const html = def.render(query);
    const active = document.activeElement;
    const aid = active && active.id ? active.id : null;
    const caret = active && typeof active.selectionStart === 'number' ? active.selectionStart : null;
    document.getElementById('app').innerHTML = html;
    if (aid) {
      const el = document.getElementById(aid);
      if (el) { el.focus(); try { if (caret != null && el.setSelectionRange) el.setSelectionRange(caret, caret); } catch (err) { } }
    }
    window.scrollTo(0, 0);
    if (def.after) def.after(query);
    document.title = (def.title ? def.title + ' · ' : '') + '云枢智学';
  }

  /* ---------- 外壳 ---------- */
  function portalShell(active, content) {
    const nav = [
      ['/home', '首页'], ['/routes', '学习路线'], ['/bank', '题库'], ['/interview', 'AI面试']
    ].map(([href, label]) => '<a href="#' + href + '" class="' + (active === href ? 'active' : '') + '">' + label + '</a>').join('');
    const initial = App.user ? App.user.name.charAt(0) : '游';
    return '' +
      '<header class="site-header"><div class="wrap">' +
      '<a class="brand" href="#/home"><span class="brand-mark">云</span><span>云枢智学<small>YUNSHU ACADEMY</small></span></a>' +
      '<nav class="main-nav">' + nav + '</nav>' +
      '<div class="header-right">' +
      '<button class="btn btn-sm btn-ghost" data-act="search">⌕ 搜索</button>' +
      '<div style="position:relative">' +
      '<button class="user-chip" data-act="user-menu"><span class="avatar">' + initial + '</span><span class="small">' + U.esc(App.user ? App.user.name : '登录') + '</span><span style="color:var(--ink-4)">▾</span></button>' +
      '<div class="user-menu hide" style="position:absolute;right:0;top:44px;width:186px;background:var(--surface);border:1px solid var(--line);border-radius:12px;box-shadow:var(--shadow-lg);padding:6px;z-index:60">' +
      '<a class="menu-item" style="color:var(--ink-2)" href="#/profile">个人中心</a>' +
      '<a class="menu-item" style="color:var(--ink-2)" href="#/wrong">我的错题</a>' +
      '<a class="menu-item" style="color:var(--ink-2)" href="#/interview/history">面试记录</a>' +
      '<div style="height:1px;background:var(--line);margin:6px 0"></div>' +
      '<a class="menu-item" style="color:var(--ink-2)" href="#/login">退出登录</a>' +
      '</div></div></div>' +
      '</div></header>' +
      '<main id="view">' + content + '</main>' +
      '<footer class="site-footer"><div class="wrap">' +
      '<div class="footer-grid">' +
      '<div><div class="brand" style="margin-bottom:10px"><span class="brand-mark">云</span><span>云枢智学<small>YUNSHU ACADEMY</small></span></div><p class="small muted" style="max-width:260px">用一条清晰的学习路线，把「学过」变成「能被验证的掌握」。</p></div>' +
      '<div><h5>产品</h5><a href="#/routes">学习路线</a><a href="#/bank">题库中心</a><a href="#/interview">AI 面试</a></div>' +
      '<div><h5>支持</h5><a href="#" data-act="todo">帮助中心</a><a href="#" data-act="todo">意见反馈</a><a href="#" data-act="todo">联系我们</a></div>' +
      '<div><h5>关于</h5><a href="#" data-act="todo">关于我们</a><a href="#" data-act="todo">用户协议</a><a href="#" data-act="todo">隐私政策</a></div>' +
      '</div>' +
      '<div class="footer-bottom"><span>© 2026 云枢智学 · 原型演示，数据均为虚构</span><span class="flex gap16"><a href="#/" style="color:var(--ink-3)">切换身份</a><a href="#/admin/login" style="color:var(--ink-3)">管理后台 →</a></span></div>' +
      '</div></footer>';
  }

  const ADMIN_MENU = [
    { g: '概览', items: [['/admin/dashboard', '▤', '数据看板']] },
    { g: '内容管理', items: [['/admin/subjects', '◫', '学科管理'], ['/admin/courses', '▶', 'B站课程库'], ['/admin/questions', '✎', '题库管理'], ['/admin/interview-bank', '✦', '面试题库管理'], ['/admin/routes', '⇢', '学习路线']] },
    { g: '学员管理', items: [['/admin/students', '☺', '学员管理']] },
    { g: '系统管理', items: [['/admin/users', '⚙', '用户管理'], ['/admin/roles', '❖', '角色管理'], ['/admin/logs', '▤', '操作日志'], ['/admin/settings', '◈', '系统设置']] }
  ];
  function adminShell(active, content) {
    const menu = ADMIN_MENU.map(g =>
      '<div class="menu-group"><div class="g">' + g.g + '</div>' +
      g.items.map(([href, ic, label]) => '<a class="menu-item ' + (active === href ? 'active' : '') + '" href="#' + href + '"><span class="i">' + ic + '</span>' + label + '</a>').join('') +
      '</div>').join('');
    const name = App.admin ? App.admin.name : '超级管理员';
    let title = '管理后台';
    ADMIN_MENU.forEach(g => g.items.forEach(it => { if (it[0] === active) title = g.g + ' / ' + it[2]; }));
    return '<div class="admin-shell">' +
      '<aside class="admin-side"><div class="admin-brand"><span class="brand-mark">云</span><span style="font-size:13px">云枢智学<small style="display:block;font-size:9px;letter-spacing:.14em;color:rgba(255,255,255,.4)">ADMIN CONSOLE</small></span></div>' + menu + '</aside>' +
      '<div class="admin-main"><div class="admin-top"><h1 id="admin-title">' + title + '</h1><div class="right"><button class="bell" data-act="notice"></button>' +
      '<div style="position:relative"><button class="user-chip" data-act="admin-menu"><span class="avatar" style="width:24px;height:24px;font-size:11px">' + name.charAt(0) + '</span><span class="small">' + U.esc(name) + '</span></button>' +
      '<div class="admin-menu hide" style="position:absolute;right:0;top:42px;width:170px;background:var(--surface);border:1px solid var(--line);border-radius:12px;box-shadow:var(--shadow-lg);padding:6px;z-index:60">' +
      '<a class="menu-item" style="color:var(--ink-2)" href="#/home">返回学员前台</a>' +
      '<a class="menu-item" style="color:var(--ink-2)" href="#/admin/login" data-act="logout">退出登录</a></div></div>' +
      '</div></div><div class="admin-content">' + content + '</div></div></div>';
  }

  /* ---------- 全局事件 ---------- */
  document.addEventListener('click', e => {
    const go = e.target.closest('[data-go]');
    if (go) { navigate(go.getAttribute('data-go')); return; }
    const close = e.target.closest('[data-close]');
    if (close && !close.closest('.modal-foot')) { /* modal 内部按钮由 overlay 处理 */ }
    const um = e.target.closest('[data-act="user-menu"]');
    if (um) { const p = um.nextElementSibling; U.qsa('.user-menu, .admin-menu').forEach(x => { if (x !== p) x.classList.add('hide'); }); p.classList.toggle('hide'); return; }
    const am = e.target.closest('[data-act="admin-menu"]');
    if (am) { const p = am.nextElementSibling; p.classList.toggle('hide'); return; }
    if (!e.target.closest('.user-menu') && !e.target.closest('.admin-menu') && !e.target.closest('[data-act="user-menu"]') && !e.target.closest('[data-act="admin-menu"]')) {
      U.qsa('.user-menu, .admin-menu').forEach(x => x.classList.add('hide'));
    }
    if (e.target.closest('[data-act="todo"]')) { e.preventDefault(); toast('原型演示：该入口为占位，不开发对应页面', 'warn'); }
    if (e.target.closest('[data-act="logout"]')) { App.admin = null; }
    if (e.target.closest('[data-act="search"]')) { toast('原型演示：全局搜索暂未纳入本期', 'warn'); }
    if (e.target.closest('[data-act="notice"]')) { toast('3 条待处理：2 道题待审核、1 条课程链接失效', 'warn'); }
  });
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') { closeOverlay(); U.qsa('.user-menu, .admin-menu').forEach(x => x.classList.add('hide')); }
  });
  window.addEventListener('hashchange', render);

  window.App = App;
  window.U = U;
  window.UI = { toast, modal, confirm: confirmDialog, close: closeOverlay, drawer };
  window.C = C;
  window.Router = { register, navigate, render, parseHash, setTimer };
  window.Shell = { portal: portalShell, admin: adminShell };
  window.Store = Store;
})();
