/* 云枢智学 · 管理端页面 */
(function () {
  'use strict';
  const { esc } = U;

  function adminPage(title, sub, actions, body, extra) {
    return Shell.admin(App.currentPath, '<div class="page-head"><div><h2 class="page-title">' + esc(title) + '</h2>' + (sub ? '<div class="page-sub">' + sub + '</div>' : '') + '</div>' +
      (actions ? '<div class="flex gap8">' + actions + '</div>' : '') + '</div>' + body + (extra || ''));
  }
  function stat(k, v, d, down) {
    return '<div class="admin-stat"><div class="k">' + k + '</div><div class="v">' + v + '</div>' + (d ? '<div class="d ' + (down ? 'down' : '') + '">' + d + '</div>' : '') + '</div>';
  }
  function table(headers, rows, empty) {
    if (!rows.length) return C.empty(empty || '暂无数据');
    return '<div class="table-wrap"><table class="tbl"><thead><tr>' + headers.map(h => '<th>' + h + '</th>').join('') + '</tr></thead><tbody>' + rows.join('') + '</tbody></table></div>';
  }
  function filterBar(items) {
    return '<div class="filters">' + items.map(i => i).join('') + '</div>';
  }
  function selectHtml(options, current, attrs) {
    return '<select class="select" ' + (attrs || '') + '>' + options.map(o => {
      const v = typeof o === 'string' ? o : o[0], t = typeof o === 'string' ? o : o[1];
      return '<option value="' + esc(v) + '"' + (String(v) === String(current) ? ' selected' : '') + '>' + esc(t) + '</option>';
    }).join('') + '</select>';
  }

  /* ================= 后台登录 ================= */
  Router.register('/admin/login', {
    title: '管理后台登录',
    admin: false,
    render() {
      return '<div class="auth-page"><section class="auth-brand"><div><a class="brand" href="#/" style="color:#fff"><span class="brand-mark" style="background:rgba(255,255,255,.14)">云</span><span>云枢智学<small style="color:rgba(255,255,255,.4)">ADMIN CONSOLE</small></span></a>' +
        '<h1>内容、题库、<br>学员与权限。</h1><p class="lead">后台负责把内容组织好，并把运营动作留痕。学员端与后台账号体系相互独立。</p></div><div style="font-size:12px;color:rgba(255,255,255,.36)">© 2026 云枢智学</div></section>' +
        '<section class="auth-form-wrap"><div class="auth-form"><h2>登录管理后台</h2><p class="lead" style="margin-top:8px;font-size:13.5px">请使用管理员账号登录。</p>' +
        '<form id="admin-login" class="mt24"><div class="field"><label>管理员账号</label><input class="input" id="ad-acc" value="admin" placeholder="请输入账号"></div>' +
        '<div class="field"><label>登录密码</label><input class="input" type="password" id="ad-pwd" value="admin123" placeholder="请输入密码"></div>' +
        '<label class="checkbox mb16"><input type="checkbox" checked>记住登录状态</label>' +
        '<button class="btn btn-primary btn-block btn-lg" type="submit">登录</button>' +
        '<p class="small muted" style="margin-top:14px">忘记密码？请联系超级管理员重置。</p>' +
        '<p class="small muted center" style="margin-top:26px">演示账号 admin / admin123　·　<a class="link" href="#/home">返回学员前台</a></p></form></div></section></div>';
    },
    after() {
      U.qs('#admin-login').addEventListener('submit', e => {
        e.preventDefault();
        const a = U.qs('#ad-acc').value.trim(), p = U.qs('#ad-pwd').value.trim();
        if (!a) return UI.toast('请输入管理员账号', 'err');
        if (!p) return UI.toast('请输入登录密码', 'err');
        if (p !== 'admin123') return UI.toast('账号或密码不正确', 'err');
        App.admin = DB.ADMIN_USERS[0];
        UI.toast('登录成功');
        setTimeout(() => Router.navigate('/admin/dashboard'), 200);
      });
    }
  });

  /* ================= 数据看板 ================= */
  Router.register('/admin/dashboard', {
    title: '数据看板', admin: true,
    render() {
      const span = App.chartSpan || 7;
      const data = DB.GROWTH[span];
      const max = Math.max.apply(null, data), min = Math.min.apply(null, data);
      const W = 620, H = 190, pad = 10;
      const pts = data.map((v, i) => {
        const x = pad + i * (W - pad * 2) / (data.length - 1);
        const y = H - pad - (v - min) / Math.max(1, max - min) * (H - pad * 2);
        return x.toFixed(1) + ',' + y.toFixed(1);
      }).join(' ');
      const qBySub = DB.SUBJECTS.map(s => ({ name: s.name, n: DB.QUESTIONS.filter(q => q.subjectId === s.id).length })).sort((a, b) => b.n - a.n).slice(0, 6);
      const maxQ = Math.max.apply(null, qBySub.map(x => x.n));
      return adminPage('数据看板', '概览平台内容与用户的关键指标，数据每 5 分钟刷新。',
        '<button class="btn btn-sm" data-refresh>刷新数据</button>',
        '<div class="admin-stats">' + stat('学科数量', '8', '较上月 +0') + stat('题目数量', DB.QUESTIONS.length, '较上月 +46') + stat('注册用户', '1,284', '较上月 +112') + stat('AI面试次数', '8,652', '较上月 +1,034') + '</div>' +
        '<div class="panel-grid mt16"><div class="panel"><div class="panel-head"><h3>用户增长趋势</h3><div class="chart-legend">' + [7, 30, 90].map(n => '<span class="chip ' + (n === span ? 'active' : '') + '" data-span="' + n + '" style="height:24px">' + n + ' 天</span>').join('') + '</div></div>' +
        '<div class="chart-box"><svg class="line-chart" viewBox="0 0 ' + W + ' ' + H + '" preserveAspectRatio="none"><defs><linearGradient id="g1" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#0E6B55" stop-opacity=".18"/><stop offset="1" stop-color="#0E6B55" stop-opacity="0"/></linearGradient></defs>' +
        '<polyline points="' + pts + ' ' + (W - pad) + ',' + (H - pad) + ' ' + pad + ',' + (H - pad) + '" fill="url(#g1)" stroke="none"/>' +
        '<polyline points="' + pts + '" fill="none" stroke="#0E6B55" stroke-width="2" stroke-linejoin="round"/></svg></div></div>' +
        '<div class="panel"><div class="panel-head"><h3>题目学科分布</h3></div><div class="chart-box"><div class="bar-list">' + qBySub.map(x => '<div class="bar-line"><span class="ellip">' + esc(x.name) + '</span><div class="track"><i style="width:' + (x.n / maxQ * 100) + '%"></i></div><span class="num">' + x.n + '</span></div>').join('') + '</div></div></div></div>' +
        '<div class="panel mt16"><div class="panel-head"><h3>快捷操作</h3></div><div class="panel-body flex gap8"><a class="btn btn-sm" href="#/admin/routes">管理学习路线</a><button class="btn btn-sm" data-new-route>创建新路线</button><a class="btn btn-sm" href="#/admin/courses">B站课程库</a></div></div>' +
        '<div class="panel mt16"><div class="panel-head"><h3>最近活动</h3><a class="link small" href="#/admin/logs">查看全部 →</a></div><div class="panel-body" style="padding:6px 20px 16px">' +
        table(['用户', '操作', '详情', '时间'], DB.LOGS.slice(0, 6).map(l => '<tr><td>' + esc(l.user) + '</td><td>' + esc(l.action) + '</td><td class="muted ellip">' + esc(l.target) + '</td><td class="muted mono">' + esc(l.time) + '</td></tr>')) + '</div></div>'
      );
    },
    after() {
      U.qsa('[data-span]').forEach(c => c.addEventListener('click', () => { App.chartSpan = Number(c.dataset.span); Router.render(); }));
      U.qs('[data-refresh]').addEventListener('click', () => { UI.toast('数据已刷新'); Router.render(); });
      U.qs('[data-new-route]').addEventListener('click', () => { App.openNewRoute = true; Router.navigate('/admin/routes?new=1'); });
    }
  });

  /* ================= 学科管理 ================= */
  Router.register('/admin/subjects', {
    title: '学科管理', admin: true,
    render() {
      const totalStages = DB.STAGES.length;
      const withStages = DB.SUBJECTS.filter(s => DB.stagesOf(s.id).length).length;
      return adminPage('学科管理', '学科是内容组织的第一层，阶段、题目、课程与路线都挂在学科下。',
        '<button class="btn btn-sm" data-add-subject>+ 新增学科</button>',
        '<div class="admin-stats">' + stat('总学科数', DB.SUBJECTS.length) + stat('有阶段的学科', withStages) + stat('阶段总数', totalStages) + stat('题目总数', DB.QUESTIONS.length) + '</div>' +
        '<div class="grid-cards mt16">' + DB.SUBJECTS.map((s, si) => {
          const stages = DB.stagesOf(s.id);
          const qn = DB.QUESTIONS.filter(q => q.subjectId === s.id).length;
          return '<div class="subject-card"><div class="subject-card-head"><div class="ico">' + s.icon + '</div><div style="flex:1"><h4 style="font-size:15px">' + esc(s.name) + '</h4><div class="small muted">排序 ' + s.order + ' · ' + stages.length + ' 个阶段 · ' + qn + ' 道题 · ' + DB.COURSES.filter(c => (c.subjectIds || []).includes(s.id)).length + ' 门课程</div></div>' +
            '<div class="tbl-actions"><a data-sub-up="' + s.id + '">上移</a><a data-sub-down="' + s.id + '">下移</a><a data-sub-edit="' + s.id + '">编辑</a><a class="danger" data-sub-del="' + s.id + '">删除</a></div></div>' +
            '<p class="small muted" style="margin:10px 0 6px">' + esc(s.desc) + '</p>' +
            '<div>' + stages.map(st => '<div class="stage-mini"><span>' + st.icon + '</span><span>' + esc(st.name) + '</span><span class="badge" style="height:18px">' + st.questionCount + ' 题</span><span class="acts"><a data-st-edit="' + st.id + '">编辑</a><a data-st-toggle="' + st.id + '">' + (st.enabled ? '禁用' : '启用') + '</a><a class="danger" data-st-del="' + st.id + '">删除</a></span></div>').join('') +
            '</div><button class="btn btn-sm mt8" data-add-stage="' + s.id + '">+ 添加阶段</button></div>';
        }).join('') + '</div>'
      );
    },
    after() {
      U.qs('[data-add-subject]').addEventListener('click', () => subjectModal());
      U.qsa('[data-sub-edit]').forEach(a => a.addEventListener('click', () => subjectModal(DB.subject(a.dataset.subEdit))));
      U.qsa('[data-sub-del]').forEach(a => a.addEventListener('click', () => {
        const s = DB.subject(a.dataset.subDel);
        const n = DB.stagesOf(s.id).length;
        UI.confirm({ title: '删除学科', text: n ? '该学科下有 ' + n + ' 个阶段与 ' + DB.QUESTIONS.filter(q => q.subjectId === s.id).length + ' 道题，删除后需重新关联。' : '该学科暂无阶段，可直接删除。', danger: true, okText: '删除', onOk() { DB.SUBJECTS.splice(DB.SUBJECTS.indexOf(s), 1); UI.toast('学科已删除'); Router.render(); } });
      }));
      U.qsa('[data-sub-up], [data-sub-down]').forEach(a => a.addEventListener('click', () => {
        const id = a.dataset.subUp || a.dataset.subDown, dir = a.dataset.subUp ? -1 : 1;
        const i = DB.SUBJECTS.findIndex(s => s.id === id), j = i + dir;
        if (j < 0 || j >= DB.SUBJECTS.length) return;
        const t = DB.SUBJECTS[i]; DB.SUBJECTS[i] = DB.SUBJECTS[j]; DB.SUBJECTS[j] = t;
        DB.SUBJECTS.forEach((s, k) => s.order = k + 1); Router.render();
      }));
      U.qsa('[data-add-stage]').forEach(b => b.addEventListener('click', () => stageModal(b.dataset.addStage)));
      U.qsa('[data-st-edit]').forEach(a => a.addEventListener('click', () => stageModal(null, DB.STAGES.find(s => s.id === a.dataset.stEdit))));
      U.qsa('[data-st-toggle]').forEach(a => a.addEventListener('click', () => { const st = DB.STAGES.find(s => s.id === a.dataset.stToggle); st.enabled = !st.enabled; UI.toast(st.enabled ? '阶段已启用' : '阶段已禁用'); Router.render(); }));
      U.qsa('[data-st-del]').forEach(a => a.addEventListener('click', () => {
        const st = DB.STAGES.find(s => s.id === a.dataset.stDel);
        UI.confirm({ title: '删除阶段', text: '该阶段关联 ' + st.questionCount + ' 道题，删除后题目将失去归属。', danger: true, okText: '删除', onOk() { DB.STAGES.splice(DB.STAGES.indexOf(st), 1); UI.toast('阶段已删除'); Router.render(); } });
      }));
    }
  });
  function subjectModal(sub) {
    const edit = !!sub;
    UI.modal({
      title: edit ? '编辑学科' : '新增学科', wide: true,
      body: '<div class="form-grid"><div class="field"><label>学科名称</label><input class="input" id="sm-name" value="' + esc(sub ? sub.name : '') + '"></div>' +
        '<div class="field"><label>学科图标（Emoji）</label><input class="input" id="sm-icon" value="' + esc(sub ? sub.icon : '🧩') + '"></div>' +
        '<div class="field"><label>排序号</label><input class="input" id="sm-order" type="number" value="' + (sub ? sub.order : DB.SUBJECTS.length + 1) + '"></div>' +
        '<div class="field"><label>主题颜色</label><div class="flex gap8" style="flex-wrap:wrap">' + ['#0F6B57', '#27507A', '#8A5A2B', '#5B5B7A', '#7A4B5B', '#3F6B4A', '#A15C2E', '#6B4B8A'].map(c => '<span data-color="' + c + '" style="width:26px;height:26px;border-radius:8px;background:' + c + ';cursor:pointer;border:2px solid ' + (sub && sub.color === c ? '#15181D' : 'transparent') + '"></span>').join('') + '</div></div>' +
        '<div class="field full"><label>学科描述</label><textarea class="textarea" id="sm-desc">' + esc(sub ? sub.desc : '') + '</textarea></div></div>',
      foot: '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-save>保存</button>',
      onMount(ov) {
        let color = sub ? sub.color : '#0F6B57';
        U.qsa('[data-color]', ov).forEach(c => c.addEventListener('click', () => { color = c.dataset.color; U.qsa('[data-color]', ov).forEach(x => x.style.border = '2px solid transparent'); c.style.border = '2px solid #15181D'; }));
        U.qs('[data-save]', ov).addEventListener('click', () => {
          const name = U.qs('#sm-name', ov).value.trim();
          if (!name) return UI.toast('学科名称必填', 'err');
          if (!U.qs('#sm-desc', ov).value.trim()) return UI.toast('学科描述必填', 'err');
          if (edit) { Object.assign(sub, { name: name, icon: U.qs('#sm-icon', ov).value.trim() || '🧩', order: Number(U.qs('#sm-order', ov).value) || 1, desc: U.qs('#sm-desc', ov).value.trim(), color: color }); }
          else { DB.SUBJECTS.push({ id: 's' + Date.now(), name: name, icon: U.qs('#sm-icon', ov).value.trim() || '🧩', color: color, desc: U.qs('#sm-desc', ov).value.trim(), order: Number(U.qs('#sm-order', ov).value) || DB.SUBJECTS.length + 1 }); }
          UI.close(); UI.toast(edit ? '学科已更新' : '学科已新增'); Router.render();
        });
      }
    });
  }
  function stageModal(subjectId, st) {
    const edit = !!st;
    UI.modal({
      title: edit ? '编辑阶段' : '添加阶段',
      body: '<div class="field"><label>阶段名称</label><input class="input" id="st-name" value="' + esc(st ? st.name : '') + '"></div>' +
        '<div class="form-grid"><div class="field"><label>图标（留空默认）</label><input class="input" id="st-icon" value="' + esc(st ? st.icon : '') + '" placeholder="🧩"></div>' +
        '<div class="field"><label>排序号</label><input class="input" id="st-order" type="number" value="' + (st ? st.order : 1) + '"></div></div>',
      foot: '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-save>保存</button>',
      onMount(ov) {
        U.qs('[data-save]', ov).addEventListener('click', () => {
          const name = U.qs('#st-name', ov).value.trim();
          if (!name) return UI.toast('阶段名称必填', 'err');
          if (edit) Object.assign(st, { name: name, icon: U.qs('#st-icon', ov).value.trim() || '🧩', order: Number(U.qs('#st-order', ov).value) || 1 });
          else DB.STAGES.push({ id: 'st_' + subjectId + '_' + Date.now(), subjectId: subjectId, name: name, icon: U.qs('#st-icon', ov).value.trim() || '🧩', order: Number(U.qs('#st-order', ov).value) || 1, enabled: true, questionCount: 0 });
          UI.close(); UI.toast(edit ? '阶段已更新' : '阶段已添加'); Router.render();
        });
      }
    });
  }

  /* ================= B站课程库 ================= */
  const courseSubjectNames = c => (c.subjectIds || []).map(id => (DB.subject(id) || { name: '其他学科' }).name).join('、') || '其他学科';
  Router.register('/admin/courses', {
    title: 'B站课程库', admin: true,
    render() {
      const f = App.courseF || (App.courseF = { kw: '', sub: '全部学科', src: '全部来源' });
      let list = DB.COURSES.slice();
      if (f.kw) list = list.filter(c => c.title.includes(f.kw));
      if (f.sub !== '全部学科') list = list.filter(c => (c.subjectIds || []).includes(f.sub));
      if (f.src !== '全部来源') list = list.filter(c => c.source === f.src);
      const totalMin = list.reduce((a, c) => a + c.duration, 0);
      const subs = new Set();
      list.forEach(c => (c.subjectIds || []).forEach(id => { if (id !== 'other') subs.add(id); }));
      return adminPage('B站课程库', '课程独立维护，一门课可归属多个学科，可被多条学习路线引用。',
        '<button class="btn btn-sm" data-import>批量导入</button><button class="btn btn-sm btn-primary" data-add-course>+ 新增课程</button>',
        '<div class="admin-stats">' + stat('课程总数', list.length) + stat('总时长', (totalMin / 60).toFixed(0) + ' 小时') + stat('关联学科数', subs.size) + '</div>' +
        '<div class="panel mt16"><div class="panel-body">' +
        filterBar([
          '<input class="input search-input" id="c-kw" placeholder="搜索课程标题" value="' + esc(f.kw) + '">',
          selectHtml(['全部学科'].concat(DB.SUBJECTS.map(s => [s.id, s.name])), f.sub, 'id="c-sub"'),
          selectHtml(['全部来源', 'B站', '其他'], f.src, 'id="c-src"')
        ]) +
        table(['序号', '封面', '课程名称', '时长', '学科', '来源', '状态', '操作'], list.map((c, i) => '<tr>' +
          '<td class="mono muted">' + (i + 1) + '</td>' +
          '<td><div style="width:56px;height:34px;border-radius:6px;background:' + c.color + ';color:rgba(255,255,255,.8);display:grid;place-items:center;font-size:10px;font-family:var(--font-mono)">' + c.source + '</div></td>' +
          '<td><div>' + esc(c.title) + '</div><div class="small muted mono">' + c.bv + '</div></td>' +
          '<td>' + Math.floor(c.duration / 60) + ' 小时 ' + (c.duration % 60) + ' 分</td>' +
          '<td class="small">' + esc(courseSubjectNames(c)) + '</td>' +
          '<td>' + C.badge(c.source, c.source === 'B站' ? 'badge-blue' : '') + '</td>' +
          '<td>' + C.badge(c.status, c.status === '上架' ? 'badge-jade' : 'badge-amber') + '</td>' +
          '<td><div class="tbl-actions"><a data-c-status="' + c.id + '">' + (c.status === '上架' ? '下架' : '上架') + '</a><a data-c-edit="' + c.id + '">编辑</a><a class="danger" data-c-del="' + c.id + '">删除</a></div></td></tr>'), '暂无匹配的课程，试试调整筛选条件') +
        C.pager(list.length, 1, 10, 'noop') + '</div></div>'
      );
    },
    after() {
      const f = App.courseF;
      U.qs('#c-kw').addEventListener('input', e => { f.kw = e.target.value; Router.render(); });
      U.qs('#c-sub').addEventListener('change', e => { f.sub = e.target.value; Router.render(); });
      U.qs('#c-src').addEventListener('change', e => { f.src = e.target.value; Router.render(); });
      U.qs('[data-add-course]').addEventListener('click', () => courseModal());
      U.qsa('[data-c-edit]').forEach(a => a.addEventListener('click', () => courseModal(DB.course(a.dataset.cEdit))));
      U.qsa('[data-c-status]').forEach(a => a.addEventListener('click', () => {
        const c = DB.course(a.dataset.cStatus);
        c.status = c.status === '上架' ? '下架' : '上架';
        UI.toast('课程已' + (c.status === '上架' ? '上架' : '下架'));
        Router.render();
      }));
      U.qsa('[data-c-del]').forEach(a => a.addEventListener('click', () => UI.confirm({ title: '删除课程', text: '删除后引用该课程的学习路线会同步减少一门课。', danger: true, onOk() { DB.COURSES.splice(DB.COURSES.indexOf(DB.course(a.dataset.cDel)), 1); UI.toast('课程已删除'); Router.render(); } })));
      U.qs('[data-import]').addEventListener('click', () => importCourses());
    }
  });
  function courseModal(c) {
    const edit = !!c;
    const selected = c ? (c.subjectIds || []) : ['s1'];
    const subjectBoxes = DB.SUBJECTS.map(s => '<label class="checkbox"><input type="checkbox" class="cm-sub" value="' + s.id + '"' + (selected.includes(s.id) ? ' checked' : '') + '>' + esc(s.name) + '</label>').join('') +
      '<label class="checkbox"><input type="checkbox" class="cm-sub" value="other"' + (selected.includes('other') ? ' checked' : '') + '>其他学科（兜底）</label>';
    UI.modal({
      title: edit ? '编辑课程' : '新增课程', wide: true,
      body: '<div class="form-grid"><div class="field full"><label>课程名称</label><input class="input" id="cm-title" value="' + esc(c ? c.title : '') + '"></div>' +
        '<div class="field"><label>时长（分钟）</label><input class="input" id="cm-dur" type="number" value="' + (c ? c.duration : 120) + '"></div>' +
        '<div class="field"><label>状态</label>' + selectHtml(['上架', '下架'], c ? c.status : '上架', 'id="cm-status"') + '</div>' +
        '<div class="field"><label>来源</label>' + selectHtml(['B站', '其他'], c ? c.source : 'B站', 'id="cm-src"') + '</div>' +
        '<div class="field full"><label>所属学科（可多选）</label><div class="chip-group" id="cm-subs">' + subjectBoxes + '</div></div>' +
        '<div class="field"><label>课程链接</label><input class="input" id="cm-url" value="' + esc(c ? 'https://www.bilibili.com/video/' + c.bv : '') + '" placeholder="https://www.bilibili.com/video/BV..."></div>' +
        '<div class="field"><label>封面图片 URL（可选）</label><input class="input" id="cm-cover" placeholder="https://..."></div>' +
        '<div class="field full"><label>课程简介（选填）</label><textarea class="textarea" id="cm-intro" style="min-height:70px">' + esc(c ? (c.intro || '') : '') + '</textarea></div></div>' +
        '<p class="small muted">所属学科可多选；「其他学科」用于暂不属于任何已有学科的课程。</p>',
      foot: '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-save>保存</button>',
      onMount(ov) {
        U.qs('[data-save]', ov).addEventListener('click', () => {
          const title = U.qs('#cm-title', ov).value.trim();
          if (!title) return UI.toast('课程名称必填', 'err');
          const dur = Number(U.qs('#cm-dur', ov).value);
          if (!dur) return UI.toast('时长必填', 'err');
          const subjectIds = U.qsa('.cm-sub', ov).filter(x => x.checked).map(x => x.value);
          if (!subjectIds.length) return UI.toast('请至少选择一个学科', 'err');
          const url = U.qs('#cm-url', ov).value.trim();
          const bv = (url.match(/BV[0-9A-Za-z]+/) || ['BV1' + Date.now().toString(36)])[0];
          const data = { title: title, duration: dur, subjectIds: subjectIds, status: U.qs('#cm-status', ov).value, source: U.qs('#cm-src', ov).value, bv: bv, intro: U.qs('#cm-intro', ov).value.trim() };
          if (edit) Object.assign(c, data);
          else DB.COURSES.push(Object.assign({ id: 'c' + Date.now(), views: 0, color: '#0F6B57' }, data));
          UI.close(); UI.toast(edit ? '课程已更新' : '课程已新增'); Router.render();
        });
      }
    });
  }
  function importCourses() {
    UI.modal({
      title: '批量导入课程', wide: true,
      body: '<p class="small muted">每行一条，格式：课程名称 | 时长(分钟) | 学科名称（多个用逗号分隔） | 来源 | 课程链接 | 封面URL(可选)</p>' +
        '<textarea class="textarea" id="imp-text" style="min-height:150px;margin-top:10px">多智能体编排实战 | 180 | AI应用与智能体开发,AI大模型开发 | B站 | https://www.bilibili.com/video/BV1aa4y1H7xQ\n大模型推理优化 | 240 | AI大模型开发 | B站 | https://www.bilibili.com/video/BV1bb4y1K7wR\n未命名课程 | | AI测试 | 其他 | </textarea>' +
        '<div id="imp-preview" class="mt16"></div>',
      foot: '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-parse>解析并导入</button>',
      onMount(ov) {
        U.qs('[data-parse]', ov).addEventListener('click', () => {
          const lines = U.qs('#imp-text', ov).value.split('\n').map(x => x.trim()).filter(Boolean);
          let ok = 0, warn = 0, bad = 0;
          lines.forEach(l => { const p = l.split('|').map(x => x.trim()); if (!p[0] || !p[1]) bad++; else if (!p[3]) warn++; else ok++; });
          U.qs('#imp-preview', ov).innerHTML = '<div class="result-card"><div class="result-head"><span class="badge badge-jade">有效 ' + ok + '</span><span class="badge badge-amber">警告 ' + warn + '</span><span class="badge badge-red">无效 ' + bad + '</span></div><div class="explain">警告行缺少来源字段，将默认按「B站」导入；无效行缺少名称或时长，已跳过。</div></div>';
          if (ok === 0) return UI.toast('没有可导入的有效行', 'err');
          lines.forEach(l => {
            const p = l.split('|').map(x => x.trim()); if (!p[0] || !p[1]) return;
            const ids = (p[2] || '').split(/[,，]/).map(n => n.trim()).map(n => (DB.SUBJECTS.find(s => s.name === n) || {}).id).filter(Boolean);
            DB.COURSES.push({
              id: 'c' + Math.random().toString(36).slice(2, 8), title: p[0], duration: Number(p[1]) || 60,
              subjectIds: ids.length ? ids : ['other'], status: '上架', source: p[3] || 'B站',
              bv: ((p[4] || '').match(/BV[0-9A-Za-z]+/) || ['BV' + Date.now().toString(36)])[0],
              intro: '', views: 0, color: '#0E6B55'
            });
          });
          UI.toast('已导入 ' + ok + ' 条课程');
          setTimeout(() => { UI.close(); Router.render(); }, 500);
        });
      }
    });
  }

  /* ================= 题库管理 ================= */
  const Q_STATUS = ['已上架', '待审核', '已下架', '已驳回'];
  Router.register('/admin/questions', {
    title: '题库管理', admin: true,
    render() {
      const f = App.qF || (App.qF = { kw: '', type: '全部类型', diff: '全部难度', status: '全部状态' });
      let list = DB.QUESTIONS.slice();
      if (f.kw) list = list.filter(q => q.stem.includes(f.kw));
      if (f.type !== '全部类型') list = list.filter(q => DB.TYPE_NAME[q.type] === f.type);
      if (f.diff !== '全部难度') list = list.filter(q => q.difficulty === f.diff);
      if (f.status !== '全部状态') list = list.filter(q => q.status === f.status);
      list = list.slice(0, 20);
      const count = s => DB.QUESTIONS.filter(q => q.status === s).length;
      return adminPage('题库管理', '题目挂在阶段下；新增后进入待审核，审核通过才在前台可见。',
        '<button class="btn btn-sm" data-import-q>导入 Excel</button><button class="btn btn-sm" data-export>导出 Excel</button><button class="btn btn-sm btn-primary" data-add-q>+ 新增题目</button>',
        '<div class="admin-stats">' + stat('题目总数', DB.QUESTIONS.length) + stat('已上架', count('已上架')) + stat('待审核', count('待审核')) + stat('单选题', DB.QUESTIONS.filter(q => q.type === 'single').length) + '</div>' +
        '<div class="panel mt16"><div class="panel-body">' +
        filterBar([
          '<input class="input search-input" id="q-kw" placeholder="搜索题干关键词" value="' + esc(f.kw) + '">',
          selectHtml(['全部类型', '单选', '多选', '判断', '简答'], f.type, 'id="q-type"'),
          selectHtml(['全部难度', '简单', '中等', '困难'], f.diff, 'id="q-diff"'),
          selectHtml(['全部状态'].concat(Q_STATUS), f.status, 'id="q-status"')
        ]) +
        '<div class="sel-bar hide" id="q-bar">已选择 <b id="q-n">0</b> 项 <button class="btn btn-sm" data-batch="上架">批量上架</button><button class="btn btn-sm" data-batch="下架">批量下架</button><button class="btn btn-sm btn-danger" data-batch="删除">批量删除</button></div>' +
        table(['<input type="checkbox" id="q-all">', 'ID', '题目', '阶段', '类型', '难度', '状态', '创建时间', '操作'],
          list.map(q => '<tr><td><input type="checkbox" class="q-check" value="' + q.id + '"></td>' +
            '<td class="mono muted">' + esc(q.id.slice(-6)) + '</td>' +
            '<td class="ellip" style="max-width:280px"><a data-q-detail="' + q.id + '">' + esc(q.stem) + '</a></td>' +
            '<td class="muted">' + esc((DB.STAGES.find(s => s.id === q.stageId) || { name: '—' }).name) + '</td>' +
            '<td>' + C.typeBadge(q.type) + '</td><td>' + C.diffBadge(q.difficulty) + '</td><td>' + C.statusBadge(q.status) + '</td>' +
            '<td class="mono muted">' + (q.createdAt || '2026-11-01') + '</td>' +
            '<td><div class="tbl-actions">' + qActions(q) + '</div></td></tr>'), '暂无匹配的题目，试试调整筛选条件') +
        C.pager(list.length, 1, 20, 'noop') + '</div></div>'
      );
      function qActions(q) {
        const a = [];
        if (q.status === '待审核') a.push('<a data-q-approve="' + q.id + '">通过</a><a data-q-reject="' + q.id + '">不通过</a>');
        if (q.status === '已上架') a.push('<a data-q-off="' + q.id + '">下架</a>');
        if (q.status === '已下架') a.push('<a data-q-on="' + q.id + '">上架</a>');
        if (q.status === '已驳回') a.push('<a data-q-resubmit="' + q.id + '">重新提交</a>');
        a.push('<a data-q-edit="' + q.id + '">编辑</a><a class="danger" data-q-del="' + q.id + '">删除</a>');
        return a.join('');
      }
    },
    after() {
      const f = App.qF;
      U.qs('#q-kw').addEventListener('input', e => { f.kw = e.target.value; Router.render(); });
      ['type', 'diff', 'status'].forEach(k => { const el = U.qs('#q-' + k); if (el) el.addEventListener('change', e => { f[k] = e.target.value; Router.render(); }); });
      U.qs('[data-add-q]').addEventListener('click', () => questionModal());
      U.qsa('[data-q-edit]').forEach(a => a.addEventListener('click', () => questionModal(DB.QUESTIONS.find(q => q.id === a.dataset.qEdit))));
      U.qsa('[data-q-detail]').forEach(a => a.addEventListener('click', () => {
        const q = DB.QUESTIONS.find(x => x.id === a.dataset.qDetail);
        UI.modal({
          title: '题目详情',
          body: '<div class="flex gap8 mb16">' + C.typeBadge(q.type) + C.diffBadge(q.difficulty) + C.statusBadge(q.status) + '<span class="small muted">' + esc((DB.STAGES.find(s => s.id === q.stageId) || { name: '' }).name) + '</span></div>' +
            '<p style="font-size:14px;line-height:1.8">' + esc(q.stem) + '</p>' +
            (q.options ? '<div class="mt16 small">' + q.options.map(o => '<div style="padding:6px 0">' + o.key + '. ' + esc(o.text) + '</div>').join('') + '</div>' : '<div class="mt16 small muted">简答题：参考答案与关键点见编辑表单</div>') +
            '<div class="result-card mt16"><div class="result-head">正确答案：' + esc((q.answer || []).join(' / ')) + '</div><div class="explain">' + esc(q.explanation) + '</div></div>' +
            '<div class="admin-stats" style="grid-template-columns:repeat(3,1fr);margin-top:16px">' + stat('答题次数', U.num(q.done)) + stat('正确次数', U.num(q.correct)) + stat('正确率', Math.round(q.correct / (q.done || 1) * 100) + '%') + '</div>',
          foot: '<button class="btn" data-close>关闭</button>'
        });
      }));
      const setStatus = (id, st, msg) => { const q = DB.QUESTIONS.find(x => x.id === id); q.status = st; UI.toast(msg); Router.render(); };
      U.qsa('[data-q-approve]').forEach(a => a.addEventListener('click', () => setStatus(a.dataset.qApprove, '已上架', '审核通过，题目已上架')));
      U.qsa('[data-q-reject]').forEach(a => a.addEventListener('click', () => UI.modal({ title: '审核不通过', body: '<div class="field"><label>驳回原因</label><textarea class="textarea" placeholder="例如：选项与题干不匹配"></textarea></div>', foot: '<button class="btn" data-close>取消</button><button class="btn btn-danger" data-ok>确认驳回</button>', onOk() { setStatus(a.dataset.qReject, '已驳回', '已驳回，运营可修改后重新提交'); } })));
      U.qsa('[data-q-off]').forEach(a => a.addEventListener('click', () => setStatus(a.dataset.qOff, '已下架', '题目已下架')));
      U.qsa('[data-q-on]').forEach(a => a.addEventListener('click', () => setStatus(a.dataset.qOn, '待审核', '已重新提交审核')));
      U.qsa('[data-q-resubmit]').forEach(a => a.addEventListener('click', () => setStatus(a.dataset.qResubmit, '待审核', '已重新提交审核')));
      U.qsa('[data-q-del]').forEach(a => a.addEventListener('click', () => UI.confirm({ title: '删除题目', text: '删除为逻辑删除，可在回收站恢复（原型演示不提供回收站）。', danger: true, onOk() { const q = DB.QUESTIONS.find(x => x.id === a.dataset.qDel); DB.QUESTIONS.splice(DB.QUESTIONS.indexOf(q), 1); UI.toast('题目已删除'); Router.render(); } })));
      const all = U.qs('#q-all');
      all.addEventListener('change', () => { U.qsa('.q-check').forEach(c => c.checked = all.checked); syncBar(); });
      U.qsa('.q-check').forEach(c => c.addEventListener('change', syncBar));
      function syncBar() { const n = U.qsa('.q-check:checked').length; U.qs('#q-bar').classList.toggle('hide', !n); U.qs('#q-n').textContent = n; }
      U.qsa('[data-batch]').forEach(b => b.addEventListener('click', () => {
        const ids = U.qsa('.q-check:checked').map(c => c.value);
        if (!ids.length) return UI.toast('请先勾选题目', 'warn');
        UI.confirm({ title: '批量' + b.dataset.batch, text: '将对 ' + ids.length + ' 道题执行该操作。', danger: b.dataset.batch === '删除', onOk() { ids.forEach(id => { const q = DB.QUESTIONS.find(x => x.id === id); if (!q) return; if (b.dataset.batch === '删除') DB.QUESTIONS.splice(DB.QUESTIONS.indexOf(q), 1); else q.status = b.dataset.batch === '上架' ? '已上架' : '已下架'; }); UI.toast('操作成功'); Router.render(); } });
      }));
      U.qs('[data-export]').addEventListener('click', () => UI.toast('已导出 Excel（原型演示）'));
      U.qs('[data-import-q]').addEventListener('click', () => UI.modal({ title: '导入 Excel', wide: true, body: '<div class="empty" style="padding:30px"><div class="ico">⇪</div><p>拖拽 .xlsx / .xls 文件到这里，或点击选择</p><button class="btn btn-sm mt16" data-pick>选择文件</button></div><p class="small muted">单次最多 500 道；表头：题目、阶段、类型、难度、选项A~D、正确答案、参考答案、关键点、解析。</p>', foot: '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-ok>开始导入</button>', onOk() { UI.toast('原型演示：已模拟导入 128 道题'); } }));
    }
  });
  function questionModal(q) {
    const edit = !!q;
    const type = q ? q.type : 'single';
    UI.modal({
      title: edit ? '编辑题目' : '新增题目', wide: true,
      body: '<div class="form-grid">' +
        '<div class="field"><label>所属阶段</label>' + selectHtml(DB.STAGES.map(s => [s.id, (DB.subject(s.subjectId) || {}).name + ' / ' + s.name]), q ? q.stageId : 'st_s1_2', 'id="qm-stage"') + '</div>' +
        '<div class="field"><label>类型</label>' + selectHtml([['single', '单选'], ['multi', '多选'], ['judge', '判断'], ['short', '简答']], type, 'id="qm-type"') + '</div>' +
        '<div class="field"><label>难度</label>' + selectHtml(['简单', '中等', '困难'], q ? q.difficulty : '中等', 'id="qm-diff"') + '</div>' +
        '<div class="field"><label>状态</label><input class="input" value="' + (edit ? q.status : '待审核') + '" disabled></div>' +
        '<div class="field full"><label>题目内容</label><textarea class="textarea" id="qm-stem">' + esc(q ? q.stem : '') + '</textarea></div></div>' +
        '<div id="qm-options"><label class="small" style="font-weight:500;color:var(--ink-2)">选项（勾选正确答案）</label>' +
        [0, 1, 2, 3].map(i => '<div class="flex gap8 mt8"><input type="checkbox" class="qm-ok" data-i="' + i + '"><input class="input" data-opt="' + i + '" placeholder="选项 ' + String.fromCharCode(65 + i) + '" value="' + esc(q && q.options && q.options[i] ? q.options[i].text : '') + '"></div>').join('') + '</div>' +
        '<div id="qm-short" class="hide"><div class="field"><label>参考答案</label><textarea class="textarea" id="qm-ref">' + esc(q && q.reference || '') + '</textarea></div><div class="field"><label>关键点（逗号分隔，3–5 条）</label><input class="input" id="qm-keys" value="' + esc(q && (q.keyPoints || []).join('，') || '') + '"></div></div>' +
        '<div class="field"><label>解析</label><textarea class="textarea" id="qm-exp">' + esc(q ? q.explanation : '') + '</textarea></div>',
      foot: '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-save>保存</button>',
      onMount(ov) {
        const showShort = () => {
          const t = U.qs('#qm-type', ov).value;
          U.qs('#qm-options', ov).classList.toggle('hide', t === 'short');
          U.qs('#qm-short', ov).classList.toggle('hide', t !== 'short');
        };
        U.qs('#qm-type', ov).addEventListener('change', showShort);
        if (q) { (q.answer || []).forEach(k => { const i = k.charCodeAt(0) - 65; const cb = U.qs('.qm-ok[data-i="' + i + '"]', ov); if (cb) cb.checked = true; }); }
        showShort();
        U.qs('[data-save]', ov).addEventListener('click', () => {
          const stem = U.qs('#qm-stem', ov).value.trim();
          if (!stem) return UI.toast('题目内容必填', 'err');
          const t = U.qs('#qm-type', ov).value;
          const opts = U.qsa('[data-opt]', ov).map((inp, i) => ({ key: String.fromCharCode(65 + i), text: inp.value.trim() })).filter(o => o.text);
          const ans = U.qsa('.qm-ok', ov).filter(c => c.checked).map((c, i) => String.fromCharCode(65 + Number(c.dataset.i)));
          if (t !== 'short' && opts.length < 2) return UI.toast('至少需要 2 个选项', 'err');
          if (t !== 'short' && !ans.length) return UI.toast('请勾选正确答案', 'err');
          const data = {
            stem: stem, type: t, difficulty: U.qs('#qm-diff', ov).value, explanation: U.qs('#qm-exp', ov).value.trim(),
            options: t === 'short' ? [{ key: 'A', text: '要点' }] : opts, answer: t === 'short' ? ['要点'] : ans,
            reference: U.qs('#qm-ref', ov) ? U.qs('#qm-ref', ov).value.trim() : '', keyPoints: U.qs('#qm-keys', ov) && U.qs('#qm-keys', ov).value ? U.qs('#qm-keys', ov).value.split(/[，,]/).map(x => x.trim()).filter(Boolean) : [],
            stageId: U.qs('#qm-stage', ov).value, status: edit ? q.status : '待审核', createdAt: edit ? q.createdAt : '2026-11-06'
          };
          data.subjectId = (DB.STAGES.find(s => s.id === data.stageId) || {}).subjectId;
          if (edit) Object.assign(q, data); else DB.QUESTIONS.unshift(Object.assign({ id: 'q' + Date.now(), done: 0, correct: 0, tags: [] }, data));
          UI.close(); UI.toast(edit ? '题目已更新' : '题目已提交审核'); Router.render();
        });
      }
    });
  }

  /* ================= 面试题库管理 ================= */
  Router.register('/admin/interview-bank', {
    title: '面试题库管理', admin: true,
    render() {
      const f = App.iqF || (App.iqF = { kw: '', kb: '全部方向', diff: '全部难度' });
      let list = DB.INTERVIEW_Q.slice();
      if (f.kw) list = list.filter(q => q.stem.includes(f.kw));
      if (f.kb !== '全部方向') list = list.filter(q => q.subjectId === f.kb);
      if (f.diff !== '全部难度') list = list.filter(q => q.difficulty === f.diff);
      list = list.slice(0, 12);
      return adminPage('面试题库管理', '面试题按方向（= 学科）归属；题干与参考答案只读，仅可维护主题与难度。',
        '<button class="btn btn-sm" data-import-md>手动导入</button><button class="btn btn-sm btn-primary" data-add-iq>录入题目</button>',
        '<div class="admin-stats">' + stat('面试题总数', DB.INTERVIEW_Q.length) + stat('方向数', DB.SUBJECTS.length) + stat('未分类', 0) + stat('本日新增', 6, '较昨日 +2') + '</div>' +
        '<div class="panel mt16"><div class="panel-body">' +
        filterBar([
          '<input class="input search-input" id="i-kw" placeholder="搜索题干" value="' + esc(f.kw) + '">',
          selectHtml(['全部方向'].concat(DB.SUBJECTS.map(s => [s.id, s.name])), f.kb, 'id="i-kb"'),
          selectHtml(['全部难度', '简单', '中等', '困难'], f.diff, 'id="i-diff"')
        ]) +
        table(['题目', '来源', '难度', '主题', '操作'], list.map(q => '<tr>' +
          '<td class="ellip" style="max-width:380px">' + esc(q.stem) + '</td>' +
          '<td><div class="small">' + esc((DB.subject(q.subjectId) || {}).name) + '</div><div class="small muted">' + esc(q.source) + '</div></td>' +
          '<td>' + C.diffBadge(q.difficulty) + '</td><td><span class="badge">' + esc(q.theme) + '</span></td>' +
          '<td><div class="tbl-actions"><a data-i-detail="' + q.id + '">详情</a><a data-i-edit="' + q.id + '">编辑</a><a class="danger" data-i-del="' + q.id + '">删除</a></div></td></tr>'), '没有符合条件的题目') +
        C.pager(list.length, 1, 12, 'noop') + '</div></div>'
      );
    },
    after() {
      const f = App.iqF;
      U.qs('#i-kw').addEventListener('input', e => { f.kw = e.target.value; Router.render(); });
      U.qs('#i-kb').addEventListener('change', e => { f.kb = e.target.value; Router.render(); });
      U.qs('#i-diff').addEventListener('change', e => { f.diff = e.target.value; Router.render(); });
      U.qs('[data-add-iq]').addEventListener('click', () => interviewModal());
      U.qsa('[data-i-edit]').forEach(a => a.addEventListener('click', () => interviewModal(DB.INTERVIEW_Q.find(q => q.id === a.dataset.iEdit))));
      U.qsa('[data-i-detail]').forEach(a => a.addEventListener('click', () => {
        const q = DB.INTERVIEW_Q.find(x => x.id === a.dataset.iDetail);
        UI.modal({ title: '面试题详情', body: '<div class="flex gap8 mb16"><span class="badge">' + esc(q.theme) + '</span>' + C.diffBadge(q.difficulty) + '<span class="small muted">' + esc(q.source) + '</span></div><p style="font-size:14px;line-height:1.8">' + esc(q.stem) + '</p><div class="result-card mt16"><div class="small muted">参考答案</div><div class="explain">' + esc(q.reference) + '</div><div class="keypoints">' + q.keyPoints.map(k => '<span class="badge badge-jade">' + esc(k) + '</span>').join('') + '</div></div>', foot: '<button class="btn" data-close>关闭</button>' });
      }));
      U.qsa('[data-i-del]').forEach(a => a.addEventListener('click', () => UI.confirm({ title: '删除面试题', text: '手工录入的题目删除后不可恢复。', danger: true, onOk() { const q = DB.INTERVIEW_Q.find(x => x.id === a.dataset.iDel); DB.INTERVIEW_Q.splice(DB.INTERVIEW_Q.indexOf(q), 1); UI.toast('已删除'); Router.render(); } })));
      U.qs('[data-import-md]').addEventListener('click', () => UI.modal({
        title: '手动导入 Markdown', wide: true,
        body: '<div class="field"><label>选择方向</label>' + selectHtml(DB.SUBJECTS.map(s => [s.id, s.name]), 's1', 'id="md-kb"') + '</div>' +
          '<div class="empty" style="padding:26px"><div class="ico">⇪</div><p>拖拽 .md 文件，或点击选择</p></div>' +
          '<p class="small muted">按 # ~ ### 标题切题，标题为题干，正文为参考答案；需达到 3 道以上才建议导入。</p>',
        foot: '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-ok>解析预览</button>',
        onOk() { UI.toast('已解析 36 道题，等待确认入库'); UI.close(); }
      }));
    }
  });
  function interviewModal(q) {
    const edit = !!q;
    UI.modal({
      title: edit ? '编辑面试题' : '录入面试题', wide: true,
      body: '<div class="form-grid"><div class="field"><label>所属方向</label>' + selectHtml(DB.SUBJECTS.map(s => [s.id, s.name]), q ? q.subjectId : 's1', 'id="im-kb"' + (edit ? ' disabled' : '')) + '</div>' +
        '<div class="field"><label>难度</label>' + selectHtml(['简单', '中等', '困难'], q ? q.difficulty : '中等', 'id="im-diff"') + '</div>' +
        '<div class="field"><label>主题</label><input class="input" id="im-theme" value="' + esc(q ? q.theme : '') + '" placeholder="如：核心概念"></div>' +
        '<div class="field"><label>标签（逗号分隔）</label><input class="input" id="im-tags" value=""></div>' +
        '<div class="field full"><label>题干</label><textarea class="textarea" id="im-stem" ' + (edit ? 'disabled' : '') + '>' + esc(q ? q.stem : '') + '</textarea></div>' +
        '<div class="field full"><label>参考答案（可留空）</label><textarea class="textarea" id="im-ref" ' + (edit ? 'disabled' : '') + '>' + esc(q ? q.reference : '') + '</textarea></div></div>' +
        (edit ? '<p class="small muted">题干与参考答案只读，仅可维护主题与难度。</p>' : ''),
      foot: '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-save>保存</button>',
      onMount(ov) {
        U.qs('[data-save]', ov).addEventListener('click', () => {
          if (edit) {
            q.theme = U.qs('#im-theme', ov).value.trim(); q.difficulty = U.qs('#im-diff', ov).value;
          } else {
            const stem = U.qs('#im-stem', ov).value.trim();
            if (!stem) return UI.toast('题干必填', 'err');
            DB.INTERVIEW_Q.unshift({ id: 'iq' + Date.now(), subjectId: U.qs('#im-kb', ov).value, stem: stem, reference: U.qs('#im-ref', ov).value.trim(), theme: U.qs('#im-theme', ov).value.trim() || '未分类', difficulty: U.qs('#im-diff', ov).value, source: '手工录入', keyPoints: ['概念与边界', '落地步骤', '验证指标'], done: 0 });
          }
          UI.close(); UI.toast(edit ? '已更新' : '题目已录入'); Router.render();
        });
      }
    });
  }

  /* ================= 学习路线管理 ================= */
  Router.register('/admin/routes', {
    title: '学习路线', admin: true,
    render(q) {
      const id = q.get('id');
      if (id) {
        const r = DB.ROUTES.find(x => x.id === id);
        if (!r) return adminPage('学习路线', '', '', C.empty('路线不存在'));
        const courses = r.courseIds.map(DB.course).filter(Boolean);
        const total = courses.reduce((a, c) => a + c.duration, 0);
        return adminPage((r.name) + ' - 课程管理', '调整课程顺序即调整学员的学习顺序。',
          '<a class="btn btn-sm" href="#/admin/routes">返回列表</a><button class="btn btn-sm btn-primary" data-add-course-route="' + r.id + '">从课程库添加</button>',
          '<div class="panel"><div class="panel-body"><div class="flex gap16"><div><div class="small muted">关联学科</div><b>' + esc((DB.subject(r.subjectId) || {}).name) + '</b></div><div><div class="small muted">状态</div>' + (r.enabled ? C.badge('已启用', 'badge-jade') : C.badge('已停用')) + '</div><div><div class="small muted">课程数 / 总时长</div><b>' + courses.length + ' 门 / ' + (total / 60).toFixed(1) + ' 小时</b></div><div><div class="small muted">适合学历</div><b>' + esc(r.degree) + '</b></div></div>' +
          '<p class="small muted mt16">' + esc(r.desc) + '</p></div></div>' +
          '<div class="panel mt16"><div class="panel-body">' + table(['序号', 'BV号', '课程名称', '时长', '播放量', '排序', '操作'], courses.map((c, i) => '<tr><td class="mono">' + (i + 1) + '</td><td class="mono muted">' + c.bv + '</td><td>' + esc(c.title) + '</td><td>' + Math.floor(c.duration / 60) + 'h' + (c.duration % 60) + 'm</td><td class="muted">' + U.num(c.views) + '</td>' +
            '<td><div class="tbl-actions"><a data-up="' + i + '">↑</a><a data-down="' + i + '">↓</a></div></td><td><a class="danger" data-rm="' + i + '">移除</a></td></tr>'), '暂无课程，点击右上角从课程库添加') + '</div></div>');
      }
      const creating = q.get('new') === '1';
      if (creating) setTimeout(() => routeModal(), 60);
      return adminPage('学习路线', '一条路线是一组有序的 B 站课程；课程来自课程库，可跨学科补充。',
        '<button class="btn btn-sm btn-primary" data-new-route>+ 新建路线</button>',
        '<div class="grid-cards" style="grid-template-columns:repeat(2,1fr)">' + DB.ROUTES.map(r => {
          const cs = r.courseIds.map(DB.course).filter(Boolean);
          const total = cs.reduce((a, c) => a + c.duration, 0);
          return '<div class="subject-card"><div class="subject-card-head"><div class="ico">🗺</div><div style="flex:1"><h4 style="font-size:15px">' + esc(r.name) + '</h4><div class="small muted">' + esc((DB.subject(r.subjectId) || {}).name) + ' · ' + cs.length + ' 门课 · ' + (total / 60).toFixed(1) + ' 小时</div></div>' +
            (r.enabled ? C.badge('已启用', 'badge-jade') : C.badge('已停用')) + '</div>' +
            '<p class="small muted" style="margin:10px 0">' + esc(r.desc) + '</p>' +
            '<div class="small muted">课程预览：' + cs.slice(0, 4).map(c => esc(c.title)).join('、') + (cs.length > 4 ? ' 等' : '') + '</div>' +
            '<div class="flex gap8 mt16"><a class="btn btn-sm" href="#/admin/routes?id=' + r.id + '">进入详情</a><button class="btn btn-sm" data-r-edit="' + r.id + '">编辑</button><button class="btn btn-sm" data-r-toggle="' + r.id + '">' + (r.enabled ? '停用' : '启用') + '</button><button class="btn btn-sm btn-ghost" data-r-prev="' + r.id + '">预览</button><button class="btn btn-sm btn-danger" data-r-del="' + r.id + '">删除</button></div></div>';
        }).join('') + '</div>'
      );
    },
    after(q) {
      const id = q.get('id');
      if (id) {
        const r = DB.ROUTES.find(x => x.id === id);
        U.qsa('[data-up]').forEach(a => a.addEventListener('click', () => { const i = Number(a.dataset.up); if (i === 0) return; const t = r.courseIds[i - 1]; r.courseIds[i - 1] = r.courseIds[i]; r.courseIds[i] = t; Router.render(); }));
        U.qsa('[data-down]').forEach(a => a.addEventListener('click', () => { const i = Number(a.dataset.down); if (i === r.courseIds.length - 1) return; const t = r.courseIds[i + 1]; r.courseIds[i + 1] = r.courseIds[i]; r.courseIds[i] = t; Router.render(); }));
        U.qsa('[data-rm]').forEach(a => a.addEventListener('click', () => { r.courseIds.splice(Number(a.dataset.rm), 1); UI.toast('已移除课程'); Router.render(); }));
        U.qs('[data-add-course-route]').addEventListener('click', () => {
          const avail = DB.COURSES.filter(c => !r.courseIds.includes(c.id));
          UI.modal({
            title: '从课程库添加', wide: true,
            body: '<div class="field"><input class="input" placeholder="搜索课程"></div><div style="max-height:380px;overflow:auto">' + avail.map(c => '<label class="checkbox" style="display:flex;padding:9px 4px;border-bottom:1px solid var(--line)"><input type="checkbox" class="add-c" value="' + c.id + '"><span style="flex:1">' + esc(c.title) + '</span><span class="small muted">' + esc(courseSubjectNames(c)) + '</span></label>').join('') + '</div>',
            foot: '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-ok>添加所选</button>',
            onOk() { U.qsa('.add-c:checked').forEach(c => { if (!r.courseIds.includes(c.value)) r.courseIds.push(c.value); }); UI.close(); UI.toast('已添加课程'); Router.render(); }
          });
        });
        return;
      }
      U.qs('[data-new-route]').addEventListener('click', () => routeModal());
      U.qsa('[data-r-edit]').forEach(a => a.addEventListener('click', () => routeModal(DB.ROUTES.find(r => r.id === a.dataset.rEdit))));
      U.qsa('[data-r-toggle]').forEach(a => a.addEventListener('click', () => { const r = DB.ROUTES.find(x => x.id === a.dataset.rToggle); r.enabled = !r.enabled; UI.toast(r.enabled ? '已启用' : '已停用'); Router.render(); }));
      U.qsa('[data-r-prev]').forEach(a => a.addEventListener('click', () => {
        const r = DB.ROUTES.find(x => x.id === a.dataset.rPrev);
        UI.modal({ title: '学员视角预览', wide: true, body: '<div class="eyebrow">学习路线</div><h2 class="serif" style="font-size:26px;margin:8px 0">' + esc(r.name) + '</h2><p class="small muted">' + esc(r.audience) + ' · ' + esc(r.desc) + '</p><div class="divider"></div>' + r.courseIds.map((id, i) => { const c = DB.course(id); return '<div class="between" style="padding:8px 0;border-bottom:1px dashed var(--line)"><span><span class="mono muted">' + U.pad(i + 1) + '</span> ' + esc(c ? c.title : '') + '</span><span class="small muted">' + (c ? c.bv : '') + '</span></div>'; }).join(''), foot: '<button class="btn" data-close>关闭</button>' });
      }));
      U.qsa('[data-r-del]').forEach(a => a.addEventListener('click', () => UI.confirm({ title: '删除学习路线', text: '删除后学员端将不再展示该路线。', danger: true, onOk() { const r = DB.ROUTES.find(x => x.id === a.dataset.rDel); DB.ROUTES.splice(DB.ROUTES.indexOf(r), 1); UI.toast('已删除'); Router.render(); } })));
    }
  });
  function routeModal(r) {
    const edit = !!r;
    UI.modal({
      title: edit ? '编辑学习路线' : '新建学习路线', wide: true,
      body: '<div class="form-grid"><div class="field"><label>路线名称</label><input class="input" id="rm-name" value="' + esc(r ? r.name : '') + '"></div>' +
        '<div class="field"><label>关联学科</label>' + selectHtml(DB.SUBJECTS.map(s => [s.id, s.name]), r ? r.subjectId : 's1', 'id="rm-sub"') + '</div>' +
        '<div class="field"><label>适合学历</label>' + selectHtml(['不限', '大专及以上', '本科及以上', '硕士及以上'], r ? r.degree : '本科及以上', 'id="rm-degree"') + '</div>' +
        '<div class="field"><label>适合人群</label><input class="input" id="rm-aud" value="' + esc(r ? r.audience : '') + '"></div>' +
        '<div class="field full"><label>路线描述</label><textarea class="textarea" id="rm-desc">' + esc(r ? r.desc : '') + '</textarea></div></div>' +
        '<label class="checkbox mb8"><input type="checkbox" id="rm-enable" ' + (r && !r.enabled ? '' : 'checked') + '>启用该路线</label>',
      foot: '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-save>保存</button>',
      onMount(ov) {
        U.qs('[data-save]', ov).addEventListener('click', () => {
          const name = U.qs('#rm-name', ov).value.trim();
          if (!name) return UI.toast('路线名称必填', 'err');
          if (edit) Object.assign(r, { name: name, subjectId: U.qs('#rm-sub', ov).value, degree: U.qs('#rm-degree', ov).value, audience: U.qs('#rm-aud', ov).value, desc: U.qs('#rm-desc', ov).value, enabled: U.qs('#rm-enable', ov).checked });
          else DB.ROUTES.push({ id: 'r' + Date.now(), name: name, subjectId: U.qs('#rm-sub', ov).value, degree: U.qs('#rm-degree', ov).value, audience: U.qs('#rm-aud', ov).value, desc: U.qs('#rm-desc', ov).value, enabled: U.qs('#rm-enable', ov).checked, courseIds: [] });
          UI.close(); UI.toast(edit ? '路线已更新' : '路线已创建'); Router.render();
        });
      }
    });
  }

  /* ================= 学员管理 ================= */
  Router.register('/admin/students', {
    title: '学员管理', admin: true,
    render() {
      const f = App.stF || (App.stF = { kw: '', status: '全部状态' });
      let list = DB.STUDENTS.slice();
      if (f.kw) list = list.filter(s => s.name.includes(f.kw) || s.phone.includes(f.kw));
      if (f.status !== '全部状态') list = list.filter(s => s.status === f.status);
      return adminPage('学员管理', '学员由前台注册产生，后台只做查看、禁用、导出与删除。',
        '<button class="btn btn-sm" data-export-students>导出学员</button>',
        '<div class="panel"><div class="panel-body">' +
        filterBar(['<input class="input search-input" id="s-kw" placeholder="搜索昵称 / 手机号" value="' + esc(f.kw) + '">', selectHtml(['全部状态', '已启用', '已禁用'], f.status, 'id="s-status"')]) +
        '<div class="sel-bar hide" id="s-bar">已选择 <b id="s-n">0</b> 项 <button class="btn btn-sm" data-sb="enable">批量启用</button><button class="btn btn-sm" data-sb="disable">批量禁用</button><button class="btn btn-sm btn-danger" data-sb="del">批量删除</button></div>' +
        table(['<input type="checkbox" id="s-all">', '学员', '手机号', '注册时间', '累计学习天数', '刷题 / 面试', '状态', '操作'],
          list.map(s => '<tr><td><input type="checkbox" class="s-check" value="' + s.id + '"></td>' +
            '<td><div class="flex gap8"><span class="avatar" style="width:26px;height:26px;font-size:11px">' + esc(s.name.charAt(0)) + '</span>' + esc(s.name) + '</div></td>' +
            '<td class="mono muted">' + esc(s.phone) + '</td><td class="muted">' + esc(s.registered) + '</td><td>' + s.days + ' 天</td>' +
            '<td class="muted">' + s.practice + ' 题 / ' + s.interviews + ' 次</td><td>' + C.statusBadge(s.status) + '</td>' +
            '<td><div class="tbl-actions"><a data-s-detail="' + s.id + '">查看详情</a><a data-s-toggle="' + s.id + '">' + (s.status === '已启用' ? '禁用' : '启用') + '</a><a class="danger" data-s-del="' + s.id + '">删除</a></div></td></tr>'), '没有找到匹配的学员') +
        C.pager(list.length, 1, 10, 'noop') + '</div></div>'
      );
    },
    after() {
      const f = App.stF;
      U.qs('[data-export-students]').addEventListener('click', () => UI.toast('已导出学员列表（原型演示）'));
      U.qs('#s-kw').addEventListener('input', e => { f.kw = e.target.value; Router.render(); });
      U.qs('#s-status').addEventListener('change', e => { f.status = e.target.value; Router.render(); });
      U.qsa('[data-s-detail]').forEach(a => a.addEventListener('click', () => {
        const s = DB.STUDENTS.find(x => x.id === a.dataset.sDetail);
        UI.drawer({
          title: '学员详情',
          body: '<div class="flex gap16"><div class="avatar avatar-lg">' + esc(s.name.charAt(0)) + '</div><div><h3 style="font-size:18px">' + esc(s.name) + '</h3><div class="flex gap8 mt8">' + C.statusBadge(s.status) + '<span class="small muted">注册于 ' + s.registered + '</span></div></div></div>' +
            '<div class="divider"></div><div style="display:grid;gap:10px">' +
            [['手机号', s.phone], ['邮箱', s.email], ['最后登录', s.lastLogin], ['累计学习天数', s.days + ' 天'], ['已学课程', s.courses + ' 门'], ['刷题总数', s.practice + ' 题'], ['平均正确率', Math.round(s.accuracy * 100) + '%'], ['累计面试', s.interviews + ' 次']].map(r => '<div class="between"><span class="small muted">' + r[0] + '</span><b>' + esc(r[1]) + '</b></div>').join('') +
            '</div><div class="divider"></div><p class="small muted">说明：「已学课程」取学员在路线中手动勾选「标记完成」的课程，不含观看进度与时长；机构不可查看面试作答原文。</p>',
          onMount(el) { }
        });
      }));
      U.qsa('[data-s-toggle]').forEach(a => a.addEventListener('click', () => { const s = DB.STUDENTS.find(x => x.id === a.dataset.sToggle); s.status = s.status === '已启用' ? '已禁用' : '已启用'; UI.toast('已' + (s.status === '已启用' ? '启用' : '禁用')); Router.render(); }));
      U.qsa('[data-s-del]').forEach(a => a.addEventListener('click', () => UI.confirm({ title: '删除学员', text: '逻辑删除，学员的历史作答与面试记录会保留归档。', danger: true, onOk() { const s = DB.STUDENTS.find(x => x.id === a.dataset.sDel); DB.STUDENTS.splice(DB.STUDENTS.indexOf(s), 1); UI.toast('已删除'); Router.render(); } })));
      const all = U.qs('#s-all');
      all.addEventListener('change', () => { U.qsa('.s-check').forEach(c => c.checked = all.checked); syncS(); });
      U.qsa('.s-check').forEach(c => c.addEventListener('change', syncS));
      function syncS() { const n = U.qsa('.s-check:checked').length; U.qs('#s-bar').classList.toggle('hide', !n); U.qs('#s-n').textContent = n; }
      U.qsa('[data-sb]').forEach(b => b.addEventListener('click', () => {
        const ids = U.qsa('.s-check:checked').map(c => c.value);
        if (!ids.length) return UI.toast('请先勾选学员', 'warn');
        UI.confirm({ title: '批量操作', text: '将对 ' + ids.length + ' 名学员执行该操作。', danger: b.dataset.sb === 'del', onOk() { ids.forEach(id => { const s = DB.STUDENTS.find(x => x.id === id); if (!s) return; if (b.dataset.sb === 'del') DB.STUDENTS.splice(DB.STUDENTS.indexOf(s), 1); else s.status = b.dataset.sb === 'enable' ? '已启用' : '已禁用'; }); UI.toast('操作成功'); Router.render(); } });
      }));
    }
  });

  /* ================= 用户管理 ================= */
  Router.register('/admin/users', {
    title: '用户管理', admin: true,
    render() {
      return adminPage('用户管理', '管理后台内部账号；超级管理员为系统内置唯一账号，不可新增或切换。',
        '<button class="btn btn-sm btn-primary" data-add-user>+ 新增用户</button>',
        '<div class="panel"><div class="panel-body">' +
        table(['用户', '手机号', '角色', '状态', '最后登录', '操作'], DB.ADMIN_USERS.map(u => '<tr>' +
          '<td><div class="flex gap8"><span class="avatar" style="width:26px;height:26px;font-size:11px">' + esc(u.name.charAt(0)) + '</span><span>' + esc(u.name) + (u.builtin ? ' <span class="badge" style="height:18px">内置</span>' : '') + '</span></div></td>' +
          '<td class="mono muted">' + esc(u.phone) + '</td><td>' + C.badge(u.role, u.role === '超级管理员' ? 'badge-ink' : '') + '</td>' +
          '<td>' + C.statusBadge(u.status) + '</td><td class="muted">' + esc(u.lastLogin) + '</td>' +
          '<td><div class="tbl-actions"><a data-u-edit="' + u.id + '">编辑</a><a data-u-pwd="' + u.id + '">重置密码</a>' + (u.builtin ? '' : '<a data-u-toggle="' + u.id + '">' + (u.status === '已启用' ? '禁用' : '启用') + '</a><a class="danger" data-u-del="' + u.id + '">删除</a>') + '</div></td></tr>')) + '</div></div>'
      );
    },
    after() {
      U.qs('[data-add-user]').addEventListener('click', () => userModal());
      U.qsa('[data-u-edit]').forEach(a => a.addEventListener('click', () => userModal(DB.ADMIN_USERS.find(u => u.id === a.dataset.uEdit))));
      U.qsa('[data-u-pwd]').forEach(a => a.addEventListener('click', () => UI.confirm({ title: '重置密码', text: '密码将重置为默认值，用户下次登录需修改。', onOk() { UI.toast('密码已重置为默认密码'); } })));
      U.qsa('[data-u-toggle]').forEach(a => a.addEventListener('click', () => { const u = DB.ADMIN_USERS.find(x => x.id === a.dataset.uToggle); u.status = u.status === '已启用' ? '已禁用' : '已启用'; UI.toast('已更新状态'); Router.render(); }));
      U.qsa('[data-u-del]').forEach(a => a.addEventListener('click', () => UI.confirm({ title: '删除用户', text: '删除后该账号将无法登录后台。', danger: true, onOk() { const u = DB.ADMIN_USERS.find(x => x.id === a.dataset.uDel); DB.ADMIN_USERS.splice(DB.ADMIN_USERS.indexOf(u), 1); UI.toast('已删除'); Router.render(); } })));
    }
  });
  function userModal(u) {
    const edit = !!u;
    UI.modal({
      title: edit ? '编辑用户' : '新增用户',
      body: '<div class="field"><label>用户名</label><input class="input" id="um-name" value="' + esc(u ? u.name : '') + '"></div>' +
        '<div class="form-grid"><div class="field"><label>手机号</label><input class="input" id="um-phone" value="' + esc(u ? u.phone : '') + '"></div>' +
        '<div class="field"><label>邮箱（选填）</label><input class="input" id="um-mail" value="' + esc(u ? u.email : '') + '"></div></div>' +
        '<div class="field"><label>角色</label>' + selectHtml([['内容审核', '内容审核'], ['运营人员', '运营人员']], u ? u.role : '运营人员', 'id="um-role"' + (u && u.builtin ? ' disabled' : '')) + '</div>' +
        '<div class="field"><label>' + (edit ? '新密码（留空则不修改）' : '密码（至少 6 位）') + '</label><input class="input" type="password" id="um-pwd"></div>' +
        '<label class="checkbox"><input type="checkbox" id="um-on" ' + (u && u.status === '已禁用' ? '' : 'checked') + '>启用该账号</label>',
      foot: '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-save>保存</button>',
      onMount(ov) {
        U.qs('[data-save]', ov).addEventListener('click', () => {
          const name = U.qs('#um-name', ov).value.trim(), phone = U.qs('#um-phone', ov).value.trim();
          if (!name) return UI.toast('用户名必填', 'err');
          if (!phone) return UI.toast('手机号必填', 'err');
          if (!edit && U.qs('#um-pwd', ov).value.length < 6) return UI.toast('密码至少 6 位', 'err');
          if (edit) Object.assign(u, { name: name, phone: phone, email: U.qs('#um-mail', ov).value, role: u.builtin ? u.role : U.qs('#um-role', ov).value, status: U.qs('#um-on', ov).checked ? '已启用' : '已禁用' });
          else DB.ADMIN_USERS.push({ id: 'a' + Date.now(), name: name, phone: phone, email: U.qs('#um-mail', ov).value, role: U.qs('#um-role', ov).value, status: U.qs('#um-on', ov).checked ? '已启用' : '已禁用', lastLogin: '—', builtin: false });
          UI.close(); UI.toast(edit ? '用户已更新' : '用户已新增'); Router.render();
        });
      }
    });
  }

  /* ================= 角色管理 ================= */
  Router.register('/admin/roles', {
    title: '角色管理', admin: true,
    render() {
      return adminPage('角色管理', '权限树共 9 组 35 项；保存时按勾选结果全量覆盖。',
        '<button class="btn btn-sm btn-primary" data-add-role>+ 新增角色</button>',
        '<div class="grid-cards" style="grid-template-columns:repeat(3,1fr)">' + DB.ROLES.map(r => {
          const users = DB.ADMIN_USERS.filter(u => u.role === r.name).length;
          const all = r.perms.length === DB.ALL_PERMS.length;
          return '<div class="subject-card"><div class="between"><div><h4 style="font-size:15px">' + esc(r.name) + '</h4><div class="small muted mono">' + esc(r.code) + '</div></div>' + (r.builtin ? C.badge('内置', 'badge-ink') : '') + '</div>' +
            '<p class="small muted" style="margin:10px 0">' + esc(r.desc) + '</p>' +
            '<div class="flex gap8"><span class="badge">' + (all ? '全部权限' : r.perms.length + ' 项权限') + '</span><span class="badge">' + users + ' 个用户</span></div>' +
            '<div class="flex gap8 mt16"><button class="btn btn-sm" data-r-perm="' + r.id + '">编辑权限</button>' + (r.builtin ? '' : '<button class="btn btn-sm btn-danger" data-r-del="' + r.id + '">删除</button>') + '</div></div>';
        }).join('') + '</div>'
      );
    },
    after() {
      U.qs('[data-add-role]').addEventListener('click', () => UI.modal({
        title: '新增角色',
        body: '<div class="field"><label>角色名称</label><input class="input" id="rl-name"></div><div class="field"><label>角色编码（唯一）</label><input class="input" id="rl-code" placeholder="如 auditor"></div><div class="field"><label>角色描述</label><textarea class="textarea" id="rl-desc"></textarea></div>',
        foot: '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-save>保存</button>',
        onMount(ov) {
          U.qs('[data-save]', ov).addEventListener('click', () => {
            const n = U.qs('#rl-name', ov).value.trim(), c = U.qs('#rl-code', ov).value.trim();
            if (!n || !c) return UI.toast('名称与编码必填', 'err');
            DB.ROLES.push({ id: 'r' + Date.now(), name: n, code: c, desc: U.qs('#rl-desc', ov).value, perms: [], builtin: false });
            UI.close(); UI.toast('角色已创建'); Router.render();
          });
        }
      }));
      U.qsa('[data-r-perm]').forEach(b => b.addEventListener('click', () => {
        const r = DB.ROLES.find(x => x.id === b.dataset.rPerm);
        const body = DB.PERM_TREE.map(g => '<div style="margin-bottom:14px"><label class="checkbox" style="font-weight:600"><input type="checkbox" class="pg" data-g="' + g.group + '" ' + (g.items.every(i => r.perms.includes(g.group + '·' + i)) ? 'checked' : '') + '>' + g.group + '</label>' +
          '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:6px;padding-left:24px;margin-top:6px">' + g.items.map(i => '<label class="checkbox"><input type="checkbox" class="pp" data-p="' + g.group + '·' + i + '" ' + (r.perms.includes(g.group + '·' + i) ? 'checked' : '') + '>' + i + '</label>').join('') + '</div></div>').join('');
        UI.modal({
          title: '编辑权限 - ' + r.name, wide: true, body: '<p class="small muted">权限树 9 组 35 项，保存为全量覆盖。</p><div class="mt16">' + body + '</div>',
          foot: '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-save>保存</button>',
          onMount(ov) {
            U.qsa('.pg', ov).forEach(pg => pg.addEventListener('change', () => { U.qsa('.pp', ov).filter(p => p.dataset.p.startsWith(pg.dataset.g + '·')).forEach(p => p.checked = pg.checked); }));
            U.qs('[data-save]', ov).addEventListener('click', () => {
              r.perms = U.qsa('.pp', ov).filter(p => p.checked).map(p => p.dataset.p);
              UI.close(); UI.toast('权限已保存'); Router.render();
            });
          }
        });
      }));
      U.qsa('[data-r-del]').forEach(b => b.addEventListener('click', () => {
        const r = DB.ROLES.find(x => x.id === b.dataset.rDel);
        const n = DB.ADMIN_USERS.filter(u => u.role === r.name).length;
        if (n) return UI.toast('该角色下还有 ' + n + ' 个用户，无法删除', 'err');
        UI.confirm({ title: '删除角色', text: '删除后不可恢复。', danger: true, onOk() { DB.ROLES.splice(DB.ROLES.indexOf(r), 1); UI.toast('角色已删除'); Router.render(); } });
      }));
    }
  });

  /* ================= 操作日志 ================= */
  Router.register('/admin/logs', {
    title: '操作日志', admin: true,
    render() {
      const f = App.logF || (App.logF = { kw: '', type: '全部类型' });
      let list = DB.LOGS.slice();
      if (f.kw) list = list.filter(l => (l.user + l.action + l.target).includes(f.kw));
      if (f.type !== '全部类型') list = list.filter(l => l.type === f.type);
      return adminPage('操作日志', '记录后台的关键操作，便于追溯内容变更。',
        '<button class="btn btn-sm" data-export-log>导出日志</button><button class="btn btn-sm btn-danger" data-clear-log>清空日志</button>',
        '<div class="panel"><div class="panel-body">' +
        filterBar(['<input class="input search-input" id="l-kw" placeholder="搜索用户 / 操作 / 目标" value="' + esc(f.kw) + '">',
          selectHtml(['全部类型', '登录', '新增·创建', '修改·编辑', '删除', '禁用·启用', '审核'], f.type, 'id="l-type"')]) +
        table(['用户', '操作', '目标', 'IP 地址', '时间'], list.map(l => '<tr data-log="' + l.id + '" style="cursor:pointer"><td>' + esc(l.user) + '</td><td>' + esc(l.action) + '</td><td class="muted ellip">' + esc(l.target) + '</td><td class="mono muted">' + esc(l.ip) + '</td><td class="mono muted">' + esc(l.time) + '</td></tr>'), '暂无匹配的日志记录') +
        C.pager(list.length, 1, 10, 'noop') + '</div></div>'
      );
    },
    after() {
      const f = App.logF;
      U.qs('#l-kw').addEventListener('input', e => { f.kw = e.target.value; Router.render(); });
      U.qs('#l-type').addEventListener('change', e => { f.type = e.target.value; Router.render(); });
      U.qsa('[data-log]').forEach(tr => tr.addEventListener('click', () => {
        const l = DB.LOGS.find(x => x.id === tr.dataset.log);
        UI.modal({ title: '日志详情', body: '<div style="display:grid;gap:10px">' + [['操作用户', l.user], ['操作类型', l.type], ['操作对象', l.target], ['IP 地址', l.ip], ['操作时间', l.time], ['日志 ID', l.id]].map(r => '<div class="between"><span class="small muted">' + r[0] + '</span><b>' + esc(r[1]) + '</b></div>').join('') + '</div>', foot: '<button class="btn" data-close>关闭</button>' });
      }));
      U.qs('[data-export-log]').addEventListener('click', () => UI.toast('已导出 CSV（原型演示）'));
      U.qs('[data-clear-log]').addEventListener('click', () => UI.confirm({ title: '清空日志', text: '清空后不可恢复，建议先导出。', danger: true, okText: '清空', onOk() { DB.LOGS.length = 0; UI.toast('日志已清空'); Router.render(); } }));
    }
  });

  /* ================= 系统设置 ================= */
  Router.register('/admin/settings', {
    title: '系统设置', admin: true,
    render() {
      return adminPage('系统设置', '配置平台的基本信息与默认展示内容。',
        '',
        '<div class="panel" style="max-width:720px"><div class="panel-head"><h3>基本信息设置</h3><span class="small muted">修改后立即生效</span></div><div class="panel-body">' +
        '<div class="field"><label>平台名称</label><input class="input" id="set-name" value="云枢智学"></div>' +
        '<div class="field"><label>平台描述</label><textarea class="textarea" id="set-desc">AI 赋能的智能面试训练平台</textarea></div>' +
        '<div class="form-grid"><div class="field"><label>Logo</label><div class="flex gap12"><div class="brand-mark" style="width:52px;height:52px;font-size:24px">云</div><button class="btn btn-sm" data-upload-logo>上传 Logo</button></div><div class="hint">建议 256×256</div></div>' +
        '<div class="field"><label>默认头像</label><div class="flex gap12"><div class="avatar avatar-lg">默</div><button class="btn btn-sm" data-upload-avatar>上传头像</button></div><div class="hint">用户未设置头像时显示</div></div></div>' +
        '<div class="flex gap8 mt16"><button class="btn" data-reset-set>重置</button><button class="btn btn-primary" data-save-set>保存设置</button></div></div></div>'
      );
    },
    after() {
      U.qs('[data-save-set]').addEventListener('click', () => { if (!U.qs('#set-name').value.trim()) return UI.toast('平台名称必填', 'err'); UI.toast('设置已保存'); });
      U.qs('[data-reset-set]').addEventListener('click', () => { U.qs('#set-name').value = '云枢智学'; U.qs('#set-desc').value = 'AI 赋能的智能面试训练平台'; UI.toast('已恢复默认值'); });
      U.qsa('[data-upload-logo], [data-upload-avatar]').forEach(b => b.addEventListener('click', () => UI.toast('原型演示：图片上传未接入', 'warn')));
    }
  });
})();
