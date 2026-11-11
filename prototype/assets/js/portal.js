/* 云枢智学 · 学员端页面 */
(function () {
  'use strict';

  const { esc } = U;
  const page = inner => {
    const p = App.currentPath || '/';
    let active = '';
    if (p === '/home') active = '/home';
    else if (p.indexOf('/routes') === 0) active = '/routes';
    else if (p.indexOf('/bank') === 0 || p.indexOf('/practice') === 0) active = '/bank';
    else if (p.indexOf('/interview') === 0) active = '/interview';
    return Shell.portal(active, '<div class="page"><div class="wrap">' + inner + '</div></div>');
  };
  const need = (cond, text) => cond ? '' : '';
  const qid = q => esc(q.id);
  const MIC_SVG = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="9" y="3" width="6" height="11" rx="3"></rect><path d="M5 11a7 7 0 0 0 14 0"></path><line x1="12" y1="18" x2="12" y2="21"></line><line x1="8" y1="21" x2="16" y2="21"></line></svg>';

  /* ================= 身份选择（入口） ================= */
  Router.register('/', {
    title: '选择身份',
    render() {
      return '<div class="landing">' +
        '<div class="wrap landing-top">' +
        '<a class="brand" href="#/"><span class="brand-mark">云</span><span>云枢智学<small>YUNSHU ACADEMY</small></span></a>' +
        '<span class="badge">可交互原型 · v1.0</span>' +
        '</div>' +
        '<div class="wrap landing-main">' +
        '<div class="eyebrow">Access</div>' +
        '<h1>选择你的身份</h1>' +
        '<p class="lead">同一个平台的两端：学员在这里学与练，运营在后台管内容与数据。两端账号独立，进入后可以随时切换。</p>' +
        '<div class="entry-grid">' +
        '<a class="entry-card entry-student" href="#/home">' +
        '<span class="entry-no">01</span>' +
        '<div class="entry-head"><span class="entry-icon">🎓</span><span class="badge badge-jade">学员前台 · Portal</span></div>' +
        '<h3>学生端</h3>' +
        '<p>按学习路线学课、按阶段刷题、做 AI 面试，拿到六维度报告与历史记录。</p>' +
        '<div class="entry-meta">学习路线 · 题库练习 · AI 面试 · 错题本</div>' +
        '<div class="entry-hint">演示账号已登录，可直接体验完整闭环</div>' +
        '<div class="entry-cta">进入学员端 <span>→</span></div>' +
        '</a>' +
        '<a class="entry-card entry-admin" href="#/admin/login">' +
        '<span class="entry-no">02</span>' +
        '<div class="entry-head"><span class="entry-icon">◫</span><span class="badge">运营后台 · Admin</span></div>' +
        '<h3>管理端</h3>' +
        '<p>维护学科、B 站课程库与题库，管理学员，查看数据看板与操作日志。</p>' +
        '<div class="entry-meta">内容管理 · 题库审核 · 学员管理 · 角色权限</div>' +
        '<div class="entry-hint">演示账号 admin / admin123</div>' +
        '<div class="entry-cta">进入管理后台 <span>→</span></div>' +
        '</a>' +
        '</div>' +
        '</div>' +
        '<div class="wrap landing-foot"><span>© 2026 云枢智学 · 演示数据，仅用于交互验证</span><span>学员端与管理端账号体系独立</span></div>' +
        '</div>';
    }
  });

  /* ================= 首页 ================= */
  Router.register('/home', {
    title: '首页',
    render() {
      const hotSubjects = ['s1', 's2', 's3', 's5', 's7', 's8'].map(id => DB.subject(id));
      const metrics = [
        ['96', '精选面试题'], ['1,284', '注册学员'], ['8,652', 'AI 面试次数']
      ];
      const quick = [
        ['🗺', '规划学习路线', '按方向看完整课程顺序', '#/routes'],
        ['🎲', '随机练习 10 题', '从题库里随机抽一组，先热个身', '#/practice?random=10'],
        ['↻', '继续上次学习', '回到未完成的练习，接着答', '#/practice?resume=1'],
        ['🎙', '开始 AI 面试', '按方向与难度生成一场模拟面试', '#/interview']
      ];
      const features = [
        ['01', '学习路线', '每个方向一条有序课程路线，点开即跳 B 站，不用自己拼凑资料。', '查看路线 →', '#/routes'],
        ['02', '海量题库', '按学科与阶段组织的判断题、单选、多选与简答，答完即时判分并给出解析。', '进入题库 →', '#/bank'],
        ['03', '刷题练习', '顺序或随机练习，答题卡跳题、计时、断点续做，答错自动进错题本。', '开始练习 →', '#/bank'],
        ['04', 'AI 模拟面试', '选择方向、难度与题数，逐题作答，AI 按要点给出 0–10 分与反馈。', '配置面试 →', '#/interview']
      ];
      return page(
        '<section class="hero">' +
        '<div class="hero-copy">' +
        '<div class="eyebrow">AI 面试训练平台 · YUNSHU ACADEMY</div>' +
        '<h1>把一条学习路线<br>走到<em>面试桌上</em></h1>' +
        '<p class="lead">学完不等于会答。这里用「路线 + 刷题 + AI 面试」把掌握程度拆开来看：哪一阶段没学透、哪一类题总出错、面试里哪个维度在拖分。</p>' +
        '<div class="hero-actions"><a class="btn btn-primary btn-lg" href="#/bank">海量题库</a><a class="btn btn-lg" href="#/interview">开始 AI 面试</a></div>' +
        '<div class="hero-meta">' + metrics.map(m => '<div><div class="num">' + m[0] + '</div><div class="lbl">' + m[1] + '</div></div>').join('') + '</div>' +
        '</div>' +
        '<div class="hero-art"><div class="halo"></div>' +
        '<div class="float-tag t1">● 面试进行中 · 第 4 / 10 题</div>' +
        '<div class="float-tag t2">错题本 · 今日新增 3 道</div>' +
        '<div class="score-card">' +
        '<div class="top"><span>AI 面试报告</span><span>2026-11-06</span></div>' +
        '<div class="ring"><div class="big">82.4</div><div style="padding-bottom:8px"><div class="badge badge-jade">较上次 +6.1</div><div class="small muted" style="margin-top:6px">AI应用与智能体开发 · 中等</div></div></div>' +
        '<div class="dims">' +
        [['技术准确性', 84], ['原理理解', 78], ['工程实践', 81], ['表达清晰', 73]].map(d => '<div class="dim"><span>' + d[0] + '</span><div class="bar"><i style="width:' + d[1] + '%"></i></div><span class="mono">' + (d[1] / 10).toFixed(1) + '</span></div>').join('') +
        '</div></div></div>' +
        '</section>' +

        '<section class="section"><div class="section-head"><div><div class="eyebrow">Quick Start</div><h2>快速开始</h2></div></div>' +
        '<div class="quick-grid">' + quick.map(q => '<a class="quick-card" href="' + q[3] + '"><div class="ico">' + q[0] + '</div><h4>' + q[1] + '</h4><p>' + q[2] + '</p><span class="go">→</span></a>').join('') + '</div></section>' +

        '<section class="section"><div class="section-head"><div><div class="eyebrow">Core</div><h2>四个模块，一条闭环</h2><p>学 → 练 → 测 → 评，每一步都留下可回看的数据。</p></div></div>' +
        '<div class="feature-grid">' + features.map(f => '<a class="feature-card" href="' + f[4] + '"><span class="n">' + f[0] + '</span><h4>' + f[1] + '</h4><p>' + f[2] + '</p><span class="foot">' + f[3] + '</span></a>').join('') + '</div></section>' +

        '<section class="section"><div class="section-head"><div><div class="eyebrow">Progress</div><h2>你的学习节奏</h2></div></div>' +
        '<div class="stats-panel"><div class="stats-left">' +
        '<div class="stat-line"><span class="k">连续学习天数</span><span class="v">26 天</span></div>' +
        '<div class="stat-line"><span class="k">平均正确率</span><span class="v">78%</span></div>' +
        '<div class="stat-line"><span class="k">累计练习</span><span class="v">218 题</span></div>' +
        '<div class="small muted">今日已刷 10 题 · 再答 5 题达成今日目标</div>' +
        '</div><div class="stats-right"><div class="between"><span class="small muted">近 7 天练习量</span><span class="badge badge-jade">今日 10 题</span></div>' +
        '<div class="bars">' + DB.WEEK.map((w, i) => '<div class="col ' + (i === DB.WEEK.length - 1 ? 'today' : '') + '"><div class="bar" style="height:' + (w.v / 25 * 100) + '%"><span>' + w.v + '</span></div><span class="d">' + w.d + '</span></div>').join('') + '</div>' +
        '</div></div></section>' +

        '<section class="section"><div class="section-head"><div><div class="eyebrow">Hot Subjects</div><h2>热门学科</h2></div><a class="link small" href="#/bank">全部题库 →</a></div>' +
        '<div class="stage-grid">' + hotSubjects.map(sub => {
          const stageCount = DB.stagesOf(sub.id).length;
          const qCount = DB.QUESTIONS.filter(q => q.subjectId === sub.id).length;
          return '<a class="stage-card" href="#/bank?s=' + sub.id + '"><div class="ico">' + sub.icon + '</div><div><h4>' + esc(sub.name) + '</h4><p>' + stageCount + ' 个阶段 · ' + qCount + ' 道题目</p></div><span class="arrow">→</span></a>';
        }).join('') + '</div></section>' +

        '<section class="section"><div class="interview-band">' +
        '<div><div class="eyebrow" style="color:#7FD3B8">AI Interview</div><h2>面试不是考试，<br>是一次可以重来的体检</h2>' +
        '<p class="lead">每一题给出 0–10 分与要点命中，整场结束再汇总六个维度。你可以随时重来，直到某个维度的分数不再拖后腿。</p>' +
        '<ul class="sell-points"><li>智能出题：按方向与难度组卷</li><li>实时评分：每题即时给分</li><li>面试报告：六维度能力画像</li><li>知识覆盖：八大学科方向</li></ul>' +
        '<div style="margin-top:26px"><a class="btn btn-primary" href="#/interview">立即体验</a></div></div>' +
        '<div class="demo-chat">' +
        '<div class="demo-msg ai">请说明 Agent 中「工具调用」的完整链路，并指出两个容易出错的边界。</div>' +
        '<div class="demo-msg me">模型先选择工具并生成结构化参数，运行时校验后执行，再把结果回填上下文决定下一步。边界主要是参数校验失败和死循环……</div>' +
        '<div class="demo-msg ai">要点命中 3 / 4 · 补充追问：如果工具超时，你会怎么处理？<div class="demo-score">本题得分 8.5 / 10</div></div>' +
        '</div></div></section>'
      );
    },
    after() {
      if (!localStorage.getItem('ys_onboard') && !/nou=1/.test(location.search)) {
        setTimeout(() => {
          const ov = document.createElement('div');
          ov.className = 'overlay';
          ov.innerHTML = '<div class="modal" style="width:min(520px,100%)">' +
            '<div class="modal-head"><h3>三步开始</h3><button class="close-x" data-close>✕</button></div>' +
            '<div class="modal-body"><div class="steps">' +
            '<div class="step active"><span class="n">1</span><span>选择技术方向：先看路线，知道自己要学什么</span></div>' +
            '<div class="step"><span class="n">2</span><span>开始刷题练习：一题一判，答错自动进错题本</span></div>' +
            '<div class="step"><span class="n">3</span><span>AI 面试检验：拿到六维度报告，再回来补薄弱项</span></div>' +
            '</div></div>' +
            '<div class="modal-foot"><button class="btn" data-skip>跳过引导</button><a class="btn btn-primary" href="#/routes" data-start>开始学习</a></div></div>';
          document.body.appendChild(ov);
          const done = () => { localStorage.setItem('ys_onboard', '1'); ov.remove(); };
          ov.addEventListener('click', e => { if (e.target === ov || e.target.closest('[data-close]') || e.target.closest('[data-skip]') || e.target.closest('[data-start]')) done(); });
        }, 420);
      }
    }
  });

  /* ================= 登录 / 注册 / 找回 ================= */
  function authShell(formHtml, title, sub) {
    return '<div class="auth-page">' +
      '<section class="auth-brand"><div><a class="brand" href="#/" style="color:#fff"><span class="brand-mark" style="background:rgba(255,255,255,.14)">云</span><span>云枢智学<small style="color:rgba(255,255,255,.4)">YUNSHU ACADEMY</small></span></a>' +
      '<h1>学过的每一步，<br>都应该被看见。</h1><p class="lead">学习路线负责方向，题库负责手感，AI 面试负责把「感觉会了」翻译成可以比较的分数。</p>' +
      '<p class="quote">面试报告会保留每一次历史，你可以删掉某一次，但删不掉趋势。</p></div>' +
      '<div style="font-size:12px;color:rgba(255,255,255,.36)">© 2026 云枢智学 · 原型演示</div></section>' +
      '<section class="auth-form-wrap"><div class="auth-form"><h2>' + title + '</h2><p class="lead" style="margin-top:8px;font-size:13.5px">' + sub + '</p>' + formHtml + '</div></section>' +
      '</div>';
  }

  Router.register('/login', {
    title: '登录',
    render() {
      return authShell(
        '<div class="tabs"><div class="tab active" data-tab="pwd">账号密码登录</div><div class="tab" data-tab="code">手机验证码登录</div></div>' +
        '<form id="login-form">' +
        '<div id="pane-pwd"><div class="field"><label>手机号 / 邮箱</label><input class="input" id="lg-account" placeholder="请输入手机号或邮箱" value="13800006621"></div>' +
        '<div class="field"><label>密码</label><input class="input" type="password" id="lg-pwd" placeholder="请输入密码" value="123456"></div></div>' +
        '<div id="pane-code" class="hide"><div class="field"><label>手机号</label><input class="input" id="lg-phone" placeholder="11 位手机号"></div>' +
        '<div class="field"><label>验证码</label><div class="flex gap8"><input class="input" id="lg-code" placeholder="6 位验证码"><button type="button" class="btn btn-sm" data-sms style="white-space:nowrap">获取验证码</button></div></div></div>' +
        '<div class="between mb16"><label class="checkbox"><input type="checkbox" checked>记住我</label><a class="link small" href="#/forgot">忘记密码？</a></div>' +
        '<button class="btn btn-primary btn-block btn-lg" type="submit">登录</button>' +
        '<div class="center small muted" style="margin:16px 0">其他方式登录</div>' +
        '<div class="social"><button type="button" class="btn" data-social="微信">微信</button><button type="button" class="btn" data-social="QQ">QQ</button><button type="button" class="btn" data-social="GitHub">GitHub</button></div>' +
        '<p class="small muted center" style="margin-top:22px">还没有账号？<a class="link" href="#/register">立即注册</a>　·　<a class="link" href="#/admin/login">管理后台</a></p>' +
        '</form>'
        , '欢迎回来', '登录后继续你的学习路线与面试记录。');
    },
    after() {
      const tabs = U.qsa('.tab');
      tabs.forEach(t => t.addEventListener('click', () => {
        tabs.forEach(x => x.classList.remove('active')); t.classList.add('active');
        const code = t.dataset.tab === 'code';
        U.qs('#pane-pwd').classList.toggle('hide', code);
        U.qs('#pane-code').classList.toggle('hide', !code);
      }));
      const sms = U.qs('[data-sms]');
      if (sms) sms.addEventListener('click', () => {
        if (sms.classList.contains('is-disabled')) return;
        sms.classList.add('is-disabled'); let n = 60;
        sms.textContent = n + 's 后重发';
        const t = setInterval(() => { n--; sms.textContent = n + 's 后重发'; if (n <= 0) { clearInterval(t); sms.classList.remove('is-disabled'); sms.textContent = '获取验证码'; } }, 1000);
        UI.toast('验证码已发送（演示：任意 6 位数字均可）');
      });
      U.qsa('[data-social]').forEach(b => b.addEventListener('click', () => UI.toast('原型演示：' + b.dataset.social + ' 登录未接入', 'warn')));
      U.qs('#login-form').addEventListener('submit', e => {
        e.preventDefault();
        const active = U.qs('.tab.active').dataset.tab;
        if (active === 'pwd') {
          const a = U.qs('#lg-account').value.trim(), p = U.qs('#lg-pwd').value.trim();
          if (!a) return UI.toast('请输入手机号或邮箱', 'err');
          if (!p) return UI.toast('请输入密码', 'err');
        } else {
          const ph = U.qs('#lg-phone').value.trim(), c = U.qs('#lg-code').value.trim();
          if (!/^1\d{10}$/.test(ph)) return UI.toast('请输入 11 位手机号', 'err');
          if (!/^\d{6}$/.test(c)) return UI.toast('请输入 6 位验证码', 'err');
        }
        UI.toast('登录成功，正在进入学习台');
        setTimeout(() => Router.navigate('/home'), 420);
      });
    }
  });

  Router.register('/register', {
    title: '注册',
    render() {
      return authShell(
        '<form id="reg-form">' +
        '<div class="field"><label>手机号</label><input class="input" id="rg-phone" placeholder="请输入 11 位手机号"></div>' +
        '<div class="field"><label>验证码</label><div class="flex gap8"><input class="input" id="rg-code" placeholder="6 位验证码"><button type="button" class="btn btn-sm" data-sms style="white-space:nowrap">获取验证码</button></div></div>' +
        '<div class="field"><label>设置密码</label><input class="input" type="password" id="rg-pwd" placeholder="6-20 位，建议包含大小写与数字"><div class="pw-meter"><i></i><i></i><i></i></div><div class="hint" id="pw-hint">强度：弱</div></div>' +
        '<div class="field"><label>确认密码</label><input class="input" type="password" id="rg-pwd2" placeholder="再次输入密码"></div>' +
        '<label class="checkbox" style="margin:6px 0 18px"><input type="checkbox" id="rg-agree">我已阅读并同意《用户协议》和《隐私政策》</label>' +
        '<button class="btn btn-primary btn-block btn-lg" type="submit">创建账号</button>' +
        '<p class="small muted center" style="margin-top:18px">已有账号？<a class="link" href="#/login">去登录</a></p>' +
        '</form>'
        , '创建账号', '注册后会自动进入个人中心，补齐资料再开始训练。');
    },
    after() {
      const pwd = U.qs('#rg-pwd'), meter = U.qsa('.pw-meter i'), hint = U.qs('#pw-hint');
      pwd.addEventListener('input', () => {
        const v = pwd.value; let s = 0;
        if (v.length >= 6) s++;
        if (/[A-Z]/.test(v) && /[a-z]/.test(v)) s++;
        if (/\d/.test(v) && /[^A-Za-z0-9]/.test(v)) s++;
        meter.forEach((m, i) => { m.style.background = i < s ? ['#A83A38', '#B06A2A', '#0E6B55'][s - 1] || 'var(--line)' : 'var(--line)'; });
        hint.textContent = '强度：' + (['弱', '中', '强'][s - 1] || '弱');
      });
      const sms = U.qs('[data-sms]');
      sms.addEventListener('click', () => {
        if (sms.classList.contains('is-disabled')) return;
        sms.classList.add('is-disabled'); let n = 60; sms.textContent = n + 's 后重发';
        const t = setInterval(() => { n--; sms.textContent = n + 's 后重发'; if (n <= 0) { clearInterval(t); sms.classList.remove('is-disabled'); sms.textContent = '获取验证码'; } }, 1000);
        UI.toast('验证码已发送（演示：任意 6 位数字均可）');
      });
      U.qs('#reg-form').addEventListener('submit', e => {
        e.preventDefault();
        const ph = U.qs('#rg-phone').value.trim(), code = U.qs('#rg-code').value.trim();
        const p = pwd.value, p2 = U.qs('#rg-pwd2').value;
        if (!/^1\d{10}$/.test(ph)) return UI.toast('请输入 11 位手机号', 'err');
        if (!/^\d{6}$/.test(code)) return UI.toast('请输入 6 位验证码', 'err');
        if (p.length < 6 || p.length > 20) return UI.toast('密码长度需为 6-20 位', 'err');
        if (p !== p2) return UI.toast('两次输入的密码不一致', 'err');
        if (!U.qs('#rg-agree').checked) return UI.toast('请先勾选同意《用户协议》和《隐私政策》', 'err');
        UI.toast('注册成功，已为你创建学习账号');
        setTimeout(() => Router.navigate('/profile'), 500);
      });
    }
  });

  Router.register('/forgot', {
    title: '找回密码',
    render() {
      return authShell(
        '<div class="steps" style="margin:0 0 22px">' +
        '<div class="step active" data-step="1"><span class="n">1</span><span>验证手机</span></div>' +
        '<div class="step" data-step="2"><span class="n">2</span><span>重置密码</span></div>' +
        '<div class="step" data-step="3"><span class="n">3</span><span>完成</span></div></div>' +
        '<div id="fg-1"><div class="field"><label>手机号</label><input class="input" id="fg-phone" placeholder="11 位手机号"></div>' +
        '<div class="field"><label>验证码</label><div class="flex gap8"><input class="input" id="fg-code" placeholder="6 位验证码"><button type="button" class="btn btn-sm" data-sms>获取验证码</button></div></div>' +
        '<button class="btn btn-primary btn-block" data-fg-next="1">下一步</button></div>' +
        '<div id="fg-2" class="hide"><div class="field"><label>新密码</label><input class="input" type="password" id="fg-pwd" placeholder="至少 6 位"></div>' +
        '<div class="field"><label>确认密码</label><input class="input" type="password" id="fg-pwd2" placeholder="再次输入"></div>' +
        '<button class="btn btn-primary btn-block" data-fg-next="2">重置密码</button></div>' +
        '<div id="fg-3" class="hide"><div class="card" style="padding:26px;text-align:center"><div style="font-size:30px">✓</div><h4 style="margin:10px 0 6px">密码已重置</h4><p class="small muted">请使用新密码重新登录。</p><a class="btn btn-primary mt16" href="#/login">立即登录</a></div></div>' +
        '<p class="small muted center" style="margin-top:22px"><a class="link" href="#/login">返回登录</a></p>'
        , '找回密码', '通过手机号验证后重置密码。');
    },
    after() {
      const sms = U.qs('[data-sms]');
      sms.addEventListener('click', () => {
        if (sms.classList.contains('is-disabled')) return;
        sms.classList.add('is-disabled'); let n = 60; sms.textContent = n + 's 后重发';
        const t = setInterval(() => { n--; sms.textContent = n + 's 后重发'; if (n <= 0) { clearInterval(t); sms.classList.remove('is-disabled'); sms.textContent = '获取验证码'; } }, 1000);
        UI.toast('验证码已发送（演示：任意 6 位数字均可）');
      });
      const step = n => {
        U.qsa('.step').forEach(s => s.classList.toggle('active', Number(s.dataset.step) <= n));
        U.qs('#fg-1').classList.toggle('hide', n !== 1);
        U.qs('#fg-2').classList.toggle('hide', n !== 2);
        U.qs('#fg-3').classList.toggle('hide', n !== 3);
      };
      U.qs('[data-fg-next="1"]').addEventListener('click', () => {
        if (!/^1\d{10}$/.test(U.qs('#fg-phone').value.trim())) return UI.toast('请输入 11 位手机号', 'err');
        if (!/^\d{6}$/.test(U.qs('#fg-code').value.trim())) return UI.toast('请输入 6 位验证码', 'err');
        step(2);
      });
      U.qs('[data-fg-next="2"]').addEventListener('click', () => {
        const a = U.qs('#fg-pwd').value, b = U.qs('#fg-pwd2').value;
        if (a.length < 6) return UI.toast('新密码至少 6 位', 'err');
        if (a !== b) return UI.toast('两次输入的密码不一致', 'err');
        step(3);
      });
    }
  });

  /* ================= 学习路线 ================= */
  function completedMap() { if (!App.completed) App.completed = Store.get('ys_completed', {}); return App.completed; }
  Router.register('/routes', {
    title: '学习路线',
    render(q) {
      const sid = q.get('s') || 's1';
      const sub = DB.subject(sid);
      const route = DB.ROUTES.find(r => r.subjectId === sid && r.enabled) || DB.ROUTES.find(r => r.subjectId === sid);
      const courses = route ? route.courseIds.map(DB.course).filter(c => c && c.status === '上架') : [];
      const total = courses.reduce((a, c) => a + c.duration, 0);
      const doneMap = completedMap();
      const doneCount = courses.filter(c => doneMap[c.id]).length;
      return page(
        '<div class="between mb24"><div><div class="eyebrow">Learning Route</div><h1 class="serif" style="font-size:38px;margin-top:6px">学习路线</h1><p class="lead" style="margin-top:6px">选一个方向，按顺序把课程走完。课程都来自 B 站，点击即跳转。</p></div><a class="btn" href="#/bank">去刷题 →</a></div>' +
        '<div class="subject-tiles">' + DB.SUBJECTS.map(s => '<a class="subject-tile ' + (s.id === sid ? 'active' : '') + '" href="#/routes?s=' + s.id + '"><div class="ico">' + s.icon + '</div><div class="nm">' + esc(s.name) + '</div><div class="cnt">' + DB.COURSES.filter(c => (c.subjectIds || []).includes(s.id)).length + ' 门</div></a>').join('') + '</div>' +
        (route ? '<div class="route-head"><div><div class="badge badge-jade">' + (route.enabled ? '已启用' : '已停用') + '</div><h2 style="margin-top:10px">' + esc(route.name) + '</h2><p class="small muted" style="margin-top:6px">适合人群：' + esc(route.audience) + '　·　' + esc(route.desc) + '</p></div>' +
          '<div class="route-stats"><div><div class="v">' + courses.length + '</div><div class="k">门课程</div></div><div><div class="v">' + (total / 60).toFixed(1) + 'h</div><div class="k">总计时长</div></div><div><div class="v" id="done-count">' + doneCount + '</div><div class="k">已完成</div></div></div></div>' +
          '<div class="mt24">' + courses.map((c, i) => '<div class="course-row"><span class="idx">' + U.pad(i + 1) + '</span><div class="course-cover" style="background:' + c.color + '">' + c.source + '</div><div style="min-width:0"><h4>' + esc(c.title) + '</h4><div class="sub"><span>' + Math.floor(c.duration / 60) + ' 小时 ' + (c.duration % 60) + ' 分</span></div></div><label class="checkbox" style="margin-left:auto"><input type="checkbox" data-done="' + c.id + '"' + (doneMap[c.id] ? ' checked' : '') + '>标记完成</label><button class="btn btn-sm btn-soft" data-open-course="' + c.id + '">去B站学习 ↗</button></div>').join('') + '</div>'
          : C.empty('该学科暂无学习路线，课程正在筹备中')) +
        '<div class="small muted mt16">勾选「标记完成」计入已学课程；平台不记录观看进度与时长。</div>'
      );
    },
    after() {
      U.qsa('[data-open-course]').forEach(b => b.addEventListener('click', () => {
        const c = DB.course(b.dataset.openCourse);
        UI.toast('正在跳转 B 站学习：' + c.title);
        setTimeout(() => window.open('https://www.bilibili.com/video/' + c.bv, '_blank'), 300);
      }));
      U.qsa('[data-done]').forEach(cb => cb.addEventListener('change', () => {
        const m = completedMap();
        if (cb.checked) m[cb.dataset.done] = 1; else delete m[cb.dataset.done];
        Store.set('ys_completed', m);
        const n = Object.keys(m).filter(id => DB.course(id)).length;
        const el = U.qs('#done-count'); if (el) el.textContent = n;
        UI.toast(cb.checked ? '已标记完成，计入已学课程' : '已取消完成');
      }));
    }
  });

  /* ================= 题库中心 ================= */
  function bankFilter() {
    if (!App.bankF) App.bankF = { type: '全部', diff: '全部', kw: '' };
    return App.bankF;
  }
  function questionList(stageId) {
    const f = bankFilter();
    let list = stageId ? DB.qOfStage(stageId) : DB.QUESTIONS.slice();
    if (f.type !== '全部') list = list.filter(q => DB.TYPE_NAME[q.type] === f.type);
    if (f.diff !== '全部') list = list.filter(q => q.difficulty === f.diff);
    if (f.kw.trim()) list = list.filter(q => (q.stem + (q.tags || []).join('')).toLowerCase().includes(f.kw.trim().toLowerCase()));
    return list;
  }
  function qCard(q, i) {
    const rate = q.done ? Math.round(q.correct / q.done * 100) : 0;
    return '<div class="qcard"><div class="meta"><span class="badge">第 ' + (i + 1) + ' 题</span>' + C.typeBadge(q.type) + C.diffBadge(q.difficulty) + '<span>' + esc((q.tags || [])[0] || '') + '</span></div>' +
      '<div class="stem">' + esc(q.stem) + '</div>' +
      '<div class="foot"><span>' + U.num(q.done) + ' 人做过</span><span>正确率 ' + rate + '%</span><span class="acts"><button class="btn btn-sm" data-explain="' + q.id + '">查看解析</button><button class="btn btn-sm btn-primary" data-practice-one="' + q.id + '">开始练习</button></span></div></div>';
  }
  function bankMarkup(q) {
    const sid = q.get('s') || 's1';
    const stid = q.get('st') || '';
    const sub = DB.subject(sid);
    const stages = DB.stagesOf(sid);
    const stage = stid ? DB.STAGES.find(s => s.id === stid) : null;
    const rail = DB.SUBJECTS.map(s => '<a class="subject-item ' + (s.id === sid ? 'active' : '') + '" href="#/bank?s=' + s.id + '"><span class="ico">' + s.icon + '</span><span class="nm">' + esc(s.name) + '</span><span class="cnt">' + DB.stagesOf(s.id).length + '</span></a>').join('');
    let right = '';
    if (!stage) {
      right = '<div class="bank-head"><div class="ico-lg">' + sub.icon + '</div><div><h2>' + esc(sub.name) + '</h2><p>' + esc(sub.desc) + '</p></div><div style="margin-left:auto" class="pill">共 ' + stages.length + ' 个阶段</div></div>' +
        '<div class="stage-list">' + (stages.length ? stages.map(st => '<div class="stage-row"><div class="ico">' + st.icon + '</div><div><h4>' + esc(st.name) + '</h4><p>共 ' + st.questionCount + ' 道题</p></div><div class="acts"><a class="btn btn-sm" href="#/bank?s=' + sid + '&st=' + st.id + '">查看题目</a><button class="btn btn-sm btn-primary" data-practice-stage="' + st.id + '">开始练习</button></div></div>').join('') : C.empty('该学科暂无阶段，题目正在录入中')) + '</div>';
    } else {
      const list = questionList(stage.id);
      const f = bankFilter();
      right = '<div class="crumb"><a href="#/bank?s=' + sid + '">‹ 返回阶段列表</a><span>/</span><b>' + esc(sub.name) + '</b><span>/</span><b>' + esc(stage.name) + '</b></div>' +
        '<div class="bank-head" style="padding:18px 20px"><div class="ico-lg">' + stage.icon + '</div><div><h2 style="font-size:20px">' + esc(stage.name) + '</h2><p>' + stage.questionCount + ' 道题 · 按题型与难度筛选后开始练习</p></div><button class="btn btn-primary" style="margin-left:auto" data-practice-stage="' + stage.id + '">开始练习</button></div>' +
        '<div class="toolbar"><div class="search"><input class="input" id="q-kw" placeholder="搜索题干关键词" value="' + esc(f.kw) + '"></div>' +
        '<div class="chip-group" data-fg="type">' + ['全部', '单选', '多选', '判断', '简答'].map(t => '<span class="chip ' + (f.type === t ? 'active' : '') + '" data-f="type" data-v="' + t + '">' + t + '</span>').join('') + '</div>' +
        '<div class="chip-group" data-fg="diff">' + ['全部', '简单', '中等', '困难'].map(t => '<span class="chip ' + (f.diff === t ? 'active' : '') + '" data-f="diff" data-v="' + t + '">' + t + '</span>').join('') + '</div></div>' +
        '<div id="q-list">' + (list.length ? list.map(qCard).join('') : C.empty('该阶段暂无题目，题目正在录入中')) + '</div>';
    }
    return '<div class="bank"><aside class="subject-rail">' + rail + '</aside><section>' + right + '</section></div>' +
      '<div class="mt24 small muted">说明：题库按「学科 → 阶段」组织；练习为自由作答、随做随判，不设交卷。</div>';
  }
  Router.register('/bank', {
    title: '题库中心',
    render(q) { return page(bankMarkup(q)); },
    after(q) {
      const stid = q.get('st') || '';
      U.qsa('.chip').forEach(ch => ch.addEventListener('click', () => {
        bankFilter()[ch.dataset.f] = ch.dataset.v;
        Router.render();
      }));
      const kw = U.qs('#q-kw');
      if (kw) {
        let t; kw.addEventListener('input', () => {
          clearTimeout(t); t = setTimeout(() => { bankFilter().kw = kw.value; const list = questionList(q.get('st')); const box = U.qs('#q-list'); if (box) box.innerHTML = list.length ? list.map(qCard).join('') : C.empty('没有匹配的题目'); bindQ(); }, 180);
        });
      }
      U.qsa('[data-practice-stage]').forEach(b => b.addEventListener('click', () => startPractice({ stageId: b.dataset.practiceStage })));
      bindQ();
      function bindQ() {
        U.qsa('[data-explain]').forEach(b => b.addEventListener('click', () => {
          const qq = DB.QUESTIONS.find(x => x.id === b.dataset.explain);
          UI.modal({
            title: '题目解析', body: '<div class="small muted">' + esc(DB.STAGES.find(s => s.id === qq.stageId).name) + ' · ' + DB.TYPE_NAME[qq.type] + ' · ' + qq.difficulty + '</div>' +
              '<p style="margin:12px 0;font-size:14px;line-height:1.8">' + esc(qq.stem) + '</p>' +
              (qq.options ? '<div class="small muted">' + qq.options.map(o => o.key + '. ' + esc(o.text)).join('<br>') + '</div>' : '') +
              '<div class="result-card"><div class="result-head">正确答案：' + esc((qq.answer || []).join(' / ')) + '</div><div class="explain">' + esc(qq.explanation) + '</div></div>',
            foot: '<button class="btn" data-close>关闭</button>'
          });
        }));
        U.qsa('[data-practice-one]').forEach(b => b.addEventListener('click', () => startPractice({ ids: [b.dataset.practiceOne] })));
      }
    }
  });

  /* ================= 刷题练习 ================= */
  function savePractice() { if (App.practice) Store.set('ys_practice', App.practice); }
  function loadPractice() { return Store.get('ys_practice', null); }

  function startPractice(opts) {
    let qs = [];
    let title = '练习';
    if (opts.ids && opts.ids.length) {
      qs = opts.ids.map(id => DB.QUESTIONS.find(q => q.id === id)).filter(Boolean);
      title = '错题重做';
    } else if (opts.stageId) {
      const st = DB.STAGES.find(s => s.id === opts.stageId);
      qs = DB.qOfStage(opts.stageId);
      title = st ? st.name : '练习';
    } else if (opts.random) {
      qs = DB.QUESTIONS.slice().sort(() => Math.random() - 0.5).slice(0, Number(opts.random) || 10);
      title = '随机练习';
    }
    if (!qs.length) return UI.toast('该范围暂无题目', 'warn');
    App.skipResume = true;
    showModeModal(qs, title, opts.stageId || '');
  }

  function showModeModal(qs, title, stageId) {
    UI.modal({
      title: '选择练习模式',
      body: '<p class="small muted">本组共 ' + qs.length + ' 道题，选择顺序后开始计时。</p>' +
        '<div style="display:grid;gap:10px;margin-top:16px">' +
        '<button class="opt-card" data-mode="order" style="text-align:left"><h4>顺序练习</h4><p>按题库顺序逐题作答</p></button>' +
        '<button class="opt-card" data-mode="random" style="text-align:left"><h4>随机练习</h4><p>打乱题目顺序作答，可指定题量</p></button></div>' +
        '<div style="margin-top:14px"><span class="small muted">随机题量</span><div class="chip-group" id="mode-counts" style="margin-top:8px">' +
        [5, 10, 15, 20].map(n => '<span class="chip ' + (n === 10 ? 'active' : '') + '" data-cnt="' + n + '">' + n + ' 题</span>').join('') + '</div></div>',
      foot: '<button class="btn" data-close>取消</button>',
      onMount(ov) {
        let cnt = 10;
        U.qsa('[data-cnt]', ov).forEach(c => c.addEventListener('click', () => {
          cnt = Number(c.dataset.cnt);
          U.qsa('[data-cnt]', ov).forEach(x => x.classList.toggle('active', x === c));
        }));
        U.qsa('[data-mode]', ov).forEach(b => b.addEventListener('click', () => {
          UI.close();
          const shuffled = qs.map(q => q.id).sort(() => Math.random() - 0.5);
          const ids = b.dataset.mode === 'random' ? shuffled.slice(0, Math.min(cnt, shuffled.length)) : qs.map(q => q.id);
          App.practice = { stageId: stageId, title: title, mode: b.dataset.mode, ids: ids, idx: 0, entries: {}, elapsed: 0, savedAt: Date.now() };
          savePractice();
          Router.navigate('/practice');
        }));
      }
    });
  }

  function practiceQuestion() { return DB.QUESTIONS.find(q => q.id === App.practice.ids[App.practice.idx]); }
  function entryOf(id) { return App.practice.entries[id] || (App.practice.entries[id] = { picked: [], text: '', graded: false, correct: null, score: null }); }

  function practiceMarkup() {
    const p = App.practice; const q = practiceQuestion(); const e = entryOf(q.id);
    const total = p.ids.length; const idx = p.idx;
    const answered = p.ids.filter(id => p.entries[id] && p.entries[id].graded).length;
    const dots = p.ids.map((id, i) => {
      const en = p.entries[id];
      let cls = '';
      if (en && en.graded) cls = en.correct ? 'right' : 'wrong';
      if (i === idx) cls = 'current';
      return '<button class="dot ' + cls + '" data-dot="' + i + '">' + (i + 1) + '</button>';
    }).join('');
    const body = q.type === 'short'
      ? '<div class="short-input"><textarea class="textarea" id="p-text" placeholder="用 3–5 句话写清你的思路与结论" ' + (e.graded ? 'disabled' : '') + '>' + esc(e.text || '') + '</textarea>' +
      (!e.graded ? '<button class="btn btn-primary" style="align-self:flex-start" data-submit>提交答案</button>' : '') + '</div>'
      : '<div class="options">' + (q.options || []).map(o => {
        const picked = (e.picked || []).includes(o.key);
        let cls = picked ? 'selected' : '';
        if (e.graded) {
          if ((q.answer || []).includes(o.key)) cls = 'correct';
          else if (picked) cls = 'wrong';
        }
        return '<div class="option ' + cls + ' ' + (e.graded ? 'locked' : '') + '" data-opt="' + o.key + '"><span class="key">' + o.key + '</span><span>' + esc(o.text) + '</span></div>';
      }).join('') + '</div>' +
      (!e.graded ? '<button class="btn btn-primary mt16" style="align-self:flex-start" data-submit>提交答案</button>' : '');

    let result = '';
    if (e.graded) {
      if (q.type === 'short') {
        result = '<div class="result-card"><div class="result-head"><span class="badge ' + (e.score >= 6 ? 'badge-jade' : 'badge-red') + '">' + (e.score >= 6 ? '回答通过' : '未通过') + '</span><span class="score-badge">得分 ' + e.score + ' / 10</span><span class="small muted">≥6 分视为通过</span></div>' +
          '<div class="explain"><b>评语：</b>' + esc(e.comment || '') + '</div>' +
          '<div class="keypoints">' + (e.hits || []).map(k => '<span class="badge badge-jade">✓ ' + esc(k) + '</span>').join('') + (e.miss || []).map(k => '<span class="badge">✕ ' + esc(k) + '</span>').join('') + '</div>' +
          '<div class="divider"></div><div class="small muted">参考答案</div><div class="explain">' + esc(q.reference || '') + '</div></div>';
      } else {
        result = '<div class="result-card"><div class="result-head"><span class="badge ' + (e.correct ? 'badge-jade' : 'badge-red') + '">' + (e.correct ? '回答正确' : '回答错误') + '</span><span class="small muted">正确答案：' + esc((q.answer || []).join(' / ')) + '</span></div>' +
          '<div class="explain">' + esc(q.explanation) + '</div></div>';
      }
    }
    return '<aside class="p-side"><a class="small muted" href="#/bank?s=' + (q.subjectId) + '&st=' + q.stageId + '">← 返回题库</a>' +
      '<div class="p-title">' + esc(p.title) + '</div>' +
      '<div class="between"><span class="small muted">进度 ' + (idx + 1) + ' / ' + total + '</span><span class="small muted">已答 ' + answered + '</span></div>' +
      '<div class="p-progress"><i style="width:' + ((idx + 1) / total * 100) + '%"></i></div>' +
      '<div class="between"><span class="p-timer" id="p-timer">' + U.hms(p.elapsed) + '</span><span class="small muted">计时中</span></div>' +
      '<div class="answer-card">' + dots + '</div>' +
      '<div class="legend"><span><i style="background:var(--blue)"></i>当前</span><span><i style="background:var(--jade)"></i>正确</span><span><i style="background:var(--red)"></i>错误</span><span><i style="background:#fff;border:1px solid var(--line-2)"></i>未答</span></div>' +
      '<button class="btn btn-block btn-sm mt16" data-end>结束练习</button></aside>' +
      '<section class="p-main"><div class="q-type-row"><span class="badge badge-ink">' + DB.TYPE_NAME[q.type] + '</span>' + C.diffBadge(q.difficulty) + '<span>·</span><span>' + esc((q.tags || [])[0] || '') + '</span><span style="margin-left:auto">第 ' + (idx + 1) + ' 题</span></div>' +
      '<h3 class="q-stem">' + esc(q.stem) + '</h3>' + body + result +
      '<div class="p-footer"><button class="btn" data-prev ' + (idx === 0 ? 'disabled' : '') + '>← 上一题</button>' +
      '<button class="btn btn-ghost" data-explain-q>查看解析</button>' +
      '<span class="spacer"></span><span class="small muted">Enter 提交 / 下一题，← → 切题，? 快捷键</span>' +
      '<button class="btn btn-primary" data-next>' + (idx === total - 1 ? '完成' : '下一题 →') + '</button></div></section>';
  }

  function paintPractice() {
    const root = U.qs('#p-root');
    if (!root) return;
    root.innerHTML = practiceMarkup();
    bindPractice();
  }
  function resumePrompt() {
    const p = App.practice;
    const answered = p.ids.filter(id => p.entries[id] && p.entries[id].graded).length;
    UI.modal({
      title: '检测到未完成的练习',
      body: '<p class="lead">上次练到第 ' + (p.idx + 1) + ' / ' + p.ids.length + ' 题，已答 ' + answered + ' 题，用时 ' + U.hms(p.elapsed) + '。是否继续？</p>',
      foot: '<button class="btn" data-restart>重新开始</button><button class="btn btn-primary" data-keep>继续</button>',
      onMount(ov) {
        U.qs('[data-keep]', ov).addEventListener('click', () => UI.close());
        U.qs('[data-restart]', ov).addEventListener('click', () => {
          Store.del('ys_practice'); App.practice = null; UI.close(); Router.navigate('/bank');
        });
      }
    });
  }
  function bindPractice() {
    const p = App.practice, q = practiceQuestion(), e = entryOf(q.id);
    U.qsa('[data-opt]').forEach(o => o.addEventListener('click', () => {
      if (e.graded) return;
      const k = o.dataset.opt;
      if (q.type === 'multi') {
        const i = e.picked.indexOf(k); i >= 0 ? e.picked.splice(i, 1) : e.picked.push(k);
      } else { e.picked = [k]; }
      U.qsa('[data-opt]').forEach(x => x.classList.toggle('selected', e.picked.includes(x.dataset.opt)));
    }));
    const ta = U.qs('#p-text');
    if (ta) ta.addEventListener('input', () => { e.text = ta.value; });
    const sub = U.qs('[data-submit]');
    if (sub) sub.addEventListener('click', () => submitAnswer());
    U.qsa('[data-dot]').forEach(d => d.addEventListener('click', () => { p.idx = Number(d.dataset.dot); savePractice(); paintPractice(); }));
    U.qs('[data-prev]').addEventListener('click', () => { if (p.idx > 0) { p.idx--; savePractice(); paintPractice(); } });
    U.qs('[data-next]').addEventListener('click', () => nextQuestion());
    U.qs('[data-end]').addEventListener('click', endPractice);
    U.qs('[data-explain-q]').addEventListener('click', () => {
      if (!e.graded) return UI.toast('提交后可查看解析', 'warn');
      UI.modal({
        title: '本题解析',
        body: '<p style="font-size:14px;line-height:1.8">' + esc(q.stem) + '</p><div class="result-card"><div class="result-head">正确答案：' + esc((q.answer || []).join(' / ')) + '</div><div class="explain">' + esc(q.explanation) + '</div></div>',
        foot: '<button class="btn" data-close>关闭</button>'
      });
    });
  }

  function submitAnswer() {
    const p = App.practice, q = practiceQuestion(), e = entryOf(q.id);
    if (e.graded) return;
    if (q.type === 'short') {
      if (!e.text || !e.text.trim()) return UI.toast('先写下你的答案', 'warn');
      const btn = U.qs('[data-submit]');
      if (btn) { btn.outerHTML = '<div class="grading"><span class="spinner"></span>AI 评分中…</div>'; }
      setTimeout(() => {
        const len = e.text.trim().length;
        const hits = (q.keyPoints || []).filter((k, i) => (len > 40 + i * 12) || Math.random() > 0.55);
        const miss = (q.keyPoints || []).filter(k => !hits.includes(k));
        const score = Math.max(2, Math.min(10, Math.round((hits.length / Math.max(1, (q.keyPoints || []).length)) * 8 + (len > 60 ? 2 : 0))));
        Object.assign(e, { graded: true, score: score, correct: score >= 6, hits: hits, miss: miss, comment: score >= 6 ? '要点覆盖较完整，建议补一句边界条件。' : '关键点覆盖不足，先讲清概念再补落地步骤。' });
        savePractice(); paintPractice();
      }, 1200);
      return;
    }
    if (!e.picked.length) return UI.toast('请选择一个答案', 'warn');
    const ans = (q.answer || []).slice().sort().join(',');
    const mine = e.picked.slice().sort().join(',');
    Object.assign(e, { graded: true, correct: ans === mine });
    savePractice(); paintPractice();
  }

  function nextQuestion() {
    const p = App.practice, e = entryOf(practiceQuestion().id);
    if (!e.graded) return UI.toast('先提交本题答案', 'warn');
    if (p.idx === p.ids.length - 1) return endPractice();
    p.idx++; savePractice();
    paintPractice();
    U.qs('.p-main') && U.qs('.p-main').scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
  function endPractice() {
    const p = App.practice;
    const unfinished = p.ids.filter(id => !(p.entries[id] && p.entries[id].graded)).length;
    UI.confirm({
      title: '结束本次练习？',
      text: unfinished ? '还有 ' + unfinished + ' 道题未完成，结束后本次进度将清除。' : '全部题目已完成，结束后将返回题库中心。',
      okText: '结束练习', danger: true,
      onOk() { Store.del('ys_practice'); App.practice = null; U.qs('#p-root').innerHTML = ''; Router.navigate('/bank'); }
    });
  }

  Router.register('/practice', {
    title: '刷题练习',
    render(q) {
      if (q.get('resume')) {
        const saved = loadPractice();
        if (saved) App.practice = saved;
      }
      if (!App.practice) {
        const saved = loadPractice();
        if (saved) { App.practice = saved; App.resumed = true; }
      }
      if (!App.practice && q.get('demo')) {
        const st = DB.STAGES[0];
        const ids = DB.qOfStage(st.id).map(x => x.id);
        const entries = {};
        entries[ids[0]] = { picked: ['B'], graded: true, correct: true };
        App.practice = { stageId: st.id, title: st.name, mode: 'order', ids: ids, idx: 1, entries: entries, elapsed: 137, savedAt: Date.now() };
      }
      if (!App.practice) {
        return page('<div class="card" style="padding:40px;max-width:620px;margin:0 auto"><h2 class="serif" style="font-size:26px">还没有进行中的练习</h2><p class="lead" style="margin-top:8px">先到题库中心选一个阶段，再决定顺序或随机。</p><a class="btn btn-primary mt24" href="#/bank">去题库中心</a></div>');
      }
      const root = '<div class="practice" id="p-root">' + practiceMarkup() + '</div>';
      return page(root);
    },
    after() {
      if (!App.practice) return;
      if (App.resumed && !App.skipResume) { App.resumed = false; resumePrompt(); }
      App.skipResume = false;
      bindPractice();
      Router.setTimer(() => { App.practice.elapsed++; const t = U.qs('#p-timer'); if (t) t.textContent = U.hms(App.practice.elapsed); if (App.practice.elapsed % 10 === 0) savePractice(); }, 1000);
      App.practiceKey && document.removeEventListener('keydown', App.practiceKey);
      App.practiceKey = e => {
        if (e.key === 'ArrowLeft') { U.qs('[data-prev]') && U.qs('[data-prev]').click(); }
        if (e.key === 'ArrowRight') { U.qs('[data-next]') && U.qs('[data-next]').click(); }
        if (e.key === 'Enter' && !e.shiftKey && document.activeElement.tagName !== 'TEXTAREA') { e.preventDefault(); const s = U.qs('[data-submit]'); s ? s.click() : (U.qs('[data-next]') && U.qs('[data-next]').click()); }
        if (e.key === '?') { e.preventDefault(); UI.modal({ title: '快捷键', body: '<div class="small" style="line-height:2">← 上一题<br>→ 下一题<br>Enter 提交答案 / 下一题<br>? 显示本面板</div>', foot: '<button class="btn" data-close>知道了</button>' }); }
      };
      document.addEventListener('keydown', App.practiceKey);
    }
  });

  /* ================= 错题本 ================= */
  const WRONG_SEED = [
    { qid: 'q1', count: 4, last: '2026-11-05', mine: 'A', corrected: 1, mastered: false },
    { qid: 'q2', count: 2, last: '2026-11-04', mine: 'A,C', corrected: 2, mastered: false },
    { qid: 'q5', count: 3, last: '2026-11-03', mine: 'A', corrected: 0, mastered: false },
    { qid: 'q6', count: 1, last: '2026-10-30', mine: 'A', corrected: 0, mastered: false }
  ];
  function wrongItems() {
    if (!App.wrong) App.wrong = WRONG_SEED.map(w => Object.assign({}, w));
    return App.wrong;
  }
  Router.register('/wrong', {
    title: '错题本',
    render(q) {
      const items = wrongItems();
      const total = items.length;
      const pending = items.filter(i => !i.mastered).length;
      const mastered = total - pending;
      const rate = total ? Math.round(mastered / total * 100) : 0;
      const st = App.wbStatus || '全部';
      const list = items.filter(i => st === '全部' || (st === '待复习' ? !i.mastered : i.mastered));
      return page(
        '<div class="between mb24"><div><div class="eyebrow">Wrong Book</div><h1 class="serif" style="font-size:34px;margin-top:6px">错题本</h1><p class="lead" style="margin-top:4px">连续答对 3 次才算已掌握，避免“蒙对一次就消失”。</p></div><button class="btn btn-primary" data-redo>重做待复习题</button></div>' +
        '<div class="wb-stats"><div class="admin-stat"><div class="k">错题总数</div><div class="v">' + total + '</div></div><div class="admin-stat"><div class="k">待复习</div><div class="v">' + pending + '</div></div><div class="admin-stat"><div class="k">已掌握</div><div class="v">' + mastered + '</div></div><div class="admin-stat"><div class="k">掌握率</div><div class="v">' + rate + '%</div></div></div>' +
        '<div class="mt24 panel"><div class="panel-head"><h3>高频错题 Top 3</h3><span class="small muted">按错误次数降序</span></div><div class="panel-body" style="display:grid;gap:8px">' +
        items.filter(i => !i.mastered).sort((a, b) => b.count - a.count).slice(0, 3).map(i => {
          const qq = DB.QUESTIONS.find(x => x.id === i.qid); return '<div class="between" style="padding:8px 0;border-bottom:1px dashed var(--line)"><span class="small">' + esc(qq.stem.slice(0, 46)) + '…</span><span class="badge badge-red">错 ' + i.count + ' 次</span></div>';
        }).join('') + '</div></div>' +
        '<div class="toolbar mt24"><div class="chip-group">' + ['全部', '待复习', '已掌握'].map(s => '<span class="chip ' + (st === s ? 'active' : '') + '" data-wb="' + s + '">' + s + '</span>').join('') + '</div><span class="small muted">共 ' + list.length + ' 条</span></div>' +
        '<div id="wb-list">' + (list.length ? list.map(i => {
          const qq = DB.QUESTIONS.find(x => x.id === i.qid);
          const lvl = i.count >= 3 ? '高频易错' : i.count === 2 ? '多次错误' : '偶尔错误';
          return '<div class="wb-item ' + (i.mastered ? 'done' : '') + '"><input type="checkbox" class="wb-check" data-wbcheck="' + i.qid + '"><div style="flex:1"><div class="wb-meta"><span class="badge ' + (i.count >= 3 ? 'badge-red' : 'badge-amber') + '">' + lvl + '</span><span>已纠正 ' + i.corrected + '/3 次</span><span>最近答错 ' + i.last + '</span><span>错 ' + i.count + ' 次</span></div>' +
            '<div class="stem">' + esc(qq.stem) + '</div><div class="small muted">你的答案：' + esc(i.mine) + '　正确答案：' + esc((qq.answer || []).join(' / ')) + '</div>' +
            '<div class="wb-acts"><button class="btn btn-sm" data-redo-one="' + i.qid + '">重新做题</button>' + (i.mastered ? '' : '<button class="btn btn-sm" data-master="' + i.qid + '">标记已掌握</button>') + '<button class="btn btn-sm btn-ghost" data-remove="' + i.qid + '">移除</button></div></div></div>';
        }).join('') : C.empty('暂无错题', '✓')) + '</div>' +
        (list.length ? '<div class="batchbar hide" id="wb-bar"><span>已选择 <b id="wb-n">0</b> 项</span><button class="btn btn-sm" data-batch="master">批量标记已掌握</button><button class="btn btn-sm btn-danger" data-batch="remove">批量移除</button><button class="btn btn-sm btn-ghost" style="color:#fff" data-batch="clear">取消选择</button></div>' : '')
      );
    },
    after() {
      U.qsa('[data-wb]').forEach(c => c.addEventListener('click', () => { App.wbStatus = c.dataset.wb; Router.render(); }));
      U.qsa('[data-redo-one]').forEach(b => b.addEventListener('click', () => startPractice({ ids: [b.dataset.redoOne] })));
      U.qsa('[data-master]').forEach(b => b.addEventListener('click', () => { wrongItems().find(i => i.qid === b.dataset.master).mastered = true; UI.toast('已标记为掌握'); Router.render(); }));
      U.qsa('[data-remove]').forEach(b => b.addEventListener('click', () => UI.confirm({ title: '移除错题', text: '移除后该题不再出现在错题本，可随时重新答错加入。', danger: true, onOk() { App.wrong = wrongItems().filter(i => i.qid !== b.dataset.remove); UI.toast('已移除'); Router.render(); } })));
      U.qs('[data-redo]') && U.qs('[data-redo]').addEventListener('click', () => {
        const ids = wrongItems().filter(i => !i.mastered).map(i => i.qid);
        ids.length ? startPractice({ ids: ids }) : UI.toast('没有待复习的错题', 'warn');
      });
      const checks = U.qsa('[data-wbcheck]');
      checks.forEach(c => c.addEventListener('change', () => {
        const n = U.qsa('[data-wbcheck]:checked').length;
        const bar = U.qs('#wb-bar'); if (!bar) return;
        bar.classList.toggle('hide', n === 0); if (U.qs('#wb-n')) U.qs('#wb-n').textContent = n;
      }));
      U.qsa('[data-batch]').forEach(b => b.addEventListener('click', () => {
        const ids = U.qsa('[data-wbcheck]:checked').map(c => c.dataset.wbcheck);
        if (!ids.length) return UI.toast('请先勾选错题', 'warn');
        const act = b.dataset.batch;
        if (act === 'clear') { U.qsa('[data-wbcheck]').forEach(c => c.checked = false); U.qs('#wb-bar').classList.add('hide'); return; }
        UI.confirm({
          title: act === 'master' ? '批量标记已掌握' : '批量移除', text: '将对 ' + ids.length + ' 道错题执行该操作。', danger: act === 'remove',
          onOk() {
            if (act === 'master') wrongItems().forEach(i => { if (ids.includes(i.qid)) i.mastered = true; });
            else App.wrong = wrongItems().filter(i => !ids.includes(i.qid));
            UI.toast('操作成功'); Router.render();
          }
        });
      }));
    }
  });

  /* ================= 面试配置 ================= */
  Router.register('/interview', {
    title: 'AI 面试',
    render() {
      const dir = App.ivDir || 's1';
      const diff = App.ivDiff || '中等';
      const count = App.ivCount || 10;
      const used = App.ivUsed || 2;
      return page(
        '<div class="between mb24"><div><div class="eyebrow">AI Interview</div><h1 class="serif" style="font-size:38px;margin-top:6px">开始一场模拟面试</h1><p class="lead" style="margin-top:6px">选择方向、难度与题数；每题限时 5 分钟，提交后立即给分。</p></div><a class="btn" href="#/interview/history">面试记录 →</a></div>' +
        '<div class="card"><div class="card-body">' +
        '<h4 style="font-size:14px">技术方向</h4><div class="dir-grid mt16">' + DB.SUBJECTS.map(s => '<div class="dir-card ' + (s.id === dir ? 'active' : '') + '" data-dir="' + s.id + '"><div class="ico">' + s.icon + '</div><div class="nm">' + esc(s.name) + '</div></div>').join('') + '</div>' +
        '<div class="divider"></div><h4 style="font-size:14px">面试难度</h4><div class="opt-row mt16">' +
        [['简单', '基础面试', '适合刚学完一个阶段'], ['中等', '进阶面试', '适合做过 1–2 个项目'], ['困难', '高级面试', '适合准备正式跳槽']].map(d => '<div class="opt-card ' + (d[0] === diff ? 'active' : '') + '" data-diff="' + d[0] + '"><h4>' + d[0] + '</h4><p>' + d[2] + '</p></div>').join('') + '</div>' +
        '<div class="divider"></div><div style="display:grid;grid-template-columns:1fr 1fr;gap:24px">' +
        '<div><h4 style="font-size:14px">面试题目数量</h4><div class="chip-group mt16">' + [3, 5, 8, 10, 15].map(n => '<span class="chip ' + (n === count ? 'active' : '') + '" data-count="' + n + '">' + n + ' 题</span>').join('') + '</div></div>' +
        '<div><h4 style="font-size:14px">回答超时时间</h4><div class="mt16"><span class="pill">固定 5 分钟 / 题 · 不可修改</span></div></div></div>' +
        '<div class="divider"></div><div class="quota-box"><div style="font-size:26px">🎟</div><div><div style="font-size:13px;font-weight:500">今日剩余 AI 面试次数：' + (5 - used) + ' / 5</div><div class="small muted">每日 5 次，次日 0 点重置；中断不扣次数，提交才扣。</div></div><button class="btn btn-primary btn-lg" style="margin-left:auto" data-start-iv>开始 AI 面试</button></div>' +
        '</div></div>'
      );
    },
    after() {
      U.qsa('[data-dir]').forEach(b => b.addEventListener('click', () => { App.ivDir = b.dataset.dir; Router.render(); }));
      U.qsa('[data-diff]').forEach(b => b.addEventListener('click', () => { App.ivDiff = b.dataset.diff; Router.render(); }));
      U.qsa('[data-count]').forEach(b => b.addEventListener('click', () => { App.ivCount = Number(b.dataset.count); Router.render(); }));
      U.qs('[data-start-iv]').addEventListener('click', () => {
        const used = App.ivUsed || 2;
        if (used >= 5) return UI.toast('今日次数已用完，明天再来', 'warn');
        App.ivUsed = used + 1;
        startInterview();
      });
    }
  });

  function startInterview() {
    const dir = App.ivDir || 's1';
    const diff = App.ivDiff || '中等';
    let pool = DB.INTERVIEW_Q.filter(q => q.subjectId === dir);
    if (diff !== '中等') pool = pool.filter(q => q.difficulty === diff).concat(pool);
    const count = App.ivCount || 10;
    const list = pool.slice(0, Math.max(count, 3)).map((q, i) => Object.assign({}, q));
    App.interview = {
      dir: dir, diff: diff, count: list.length, idx: 0, qs: list, messages: [],
      scores: [], startedAt: Date.now(), elapsed: 0, qRemain: 300, phase: 'ask'
    };
    const first = list[0];
    App.interview.messages.push({ role: 'ai', text: '你好，我是本次的 AI 面试官。方向：' + DB.subject(dir).name + '，难度：' + diff + '。我们开始第 1 题。', t: U.now() });
    App.interview.messages.push({ role: 'ai', text: first.stem, t: U.now(), q: true });
    Router.navigate('/interview/session');
  }

  /* ================= 面试进行中 ================= */
  function ivMarkup() {
    const s = App.interview;
    const sub = DB.subject(s.dir);
    const q = s.qs[s.idx];
    return '<div class="iv-topbar"><a class="btn btn-sm btn-ghost" href="#/interview" data-exit>← 退出</a><span class="iv-live"></span><span class="small">' + esc(sub.name) + ' · ' + s.diff + '</span><span class="pill" style="height:24px">在线</span><span style="margin-left:auto" class="mono" id="iv-timer">' + U.hms(s.elapsed) + '</span><span class="small muted">题目 ' + (s.idx + 1) + ' / ' + s.count + '</span><button class="btn btn-sm" data-mute>🔊 播报中</button><button class="btn btn-sm btn-danger" data-end-iv>结束面试</button></div>' +
      '<div style="display:grid;grid-template-columns:1fr 300px;gap:18px"><div><div class="chat" id="iv-chat">' + s.messages.map(m => msgHtml(m)).join('') + '<div id="iv-tail"></div></div>' +
      '<div class="iv-input"><textarea class="textarea" id="iv-text" placeholder="输入你的回答，Enter 发送，Shift+Enter 换行"></textarea><button class="btn iv-mic" data-mic title="语音转文字" aria-label="语音转文字">' + MIC_SVG + '</button><button class="btn btn-primary" data-send style="height:74px;width:88px">发送</button></div>' +
      '<div class="small muted mt8">本题倒计时 <span class="mono" id="iv-remain">' + U.ms(s.qRemain) + '</span>　·　回答内容平台运营可见</div></div>' +
      '<aside class="iv-side"><div class="between"><h4 style="font-size:13px">面试题目</h4><span class="badge">' + (s.scores.length) + '/' + s.count + ' 已答</span></div>' +
      '<div class="iv-q-list mt16">' + s.qs.map((qq, i) => '<div class="iv-q ' + (i === s.idx ? 'active' : '') + '" data-jump="' + i + '"><span class="idx">' + U.pad(i + 1) + '</span><span class="ellip" style="max-width:150px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + esc(qq.stem.slice(0, 12)) + '</span><span class="sc" style="color:' + (s.scores[i] != null ? U.scoreColor(s.scores[i]) : 'var(--ink-4)') + '">' + (s.scores[i] != null ? s.scores[i].toFixed(1) : '—') + '</span></div>').join('') + '</div>' +
      '<div class="divider"></div><div class="small muted">面试统计</div><div class="mt8" style="display:grid;gap:8px"><div class="between small"><span>已回答</span><b>' + s.scores.length + ' / ' + s.count + '</b></div><div class="between small"><span>平均分</span><b>' + (s.scores.length ? (s.scores.reduce((a, b) => a + b, 0) / s.scores.length).toFixed(1) : '—') + '</b></div><div class="between small"><span>用时</span><b id="iv-used">' + U.hms(s.elapsed) + '</b></div></div></aside></div>';
  }
  function msgHtml(m) {
    if (m.role === 'ai') {
      return '<div class="msg"><div class="who">AI 面试官 <span class="replay" data-replay>重播</span></div><div class="bubble">' + esc(m.text) + '</div></div>';
    }
    return '<div class="msg me"><div class="who">我 · ' + m.t + '</div><div class="bubble">' + esc(m.text) + '</div></div>';
  }
  function pushMsg(m) {
    App.interview.messages.push(m);
    const box = U.qs('#iv-chat'); if (!box) return;
    const tail = U.qs('#iv-tail');
    tail.insertAdjacentHTML('beforebegin', msgHtml(m));
    box.scrollTop = box.scrollHeight;
  }
  function typing(on, label) {
    const tail = U.qs('#iv-tail'); if (!tail) return;
    tail.innerHTML = on ? '<div class="msg"><div class="who">AI 面试官</div><div class="bubble"><span class="small muted" style="margin-right:8px">' + label + '</span><span class="typing"><i></i><i></i><i></i></span></div></div>' : '';
    const box = U.qs('#iv-chat'); if (box) box.scrollTop = box.scrollHeight;
  }
  Router.register('/interview/session', {
    title: '模拟面试',
    render(q) {
      if (!App.interview && q.get('demo')) {
        const list = DB.INTERVIEW_Q.filter(x => x.subjectId === 's1').slice(0, 3);
        App.interview = {
          dir: 's1', diff: '中等', count: list.length, idx: 1, qs: list,
          messages: [
            { role: 'ai', text: '你好，我是本次的 AI 面试官。方向：AI应用与智能体开发，难度：中等。我们开始第 1 题。', t: '21:41' },
            { role: 'ai', text: list[0].stem, t: '21:41', q: true },
            { role: 'me', text: '我会先定位链路中的关键节点：模型选择工具、生成结构化参数、运行时校验、执行与结果回填。边界情况主要是参数校验失败和工具超时。', t: '21:42' },
            { role: 'ai', text: '本题得分 8.2 / 10。命中要点：概念与边界、落地步骤。回答结构清楚，可以再补一个失败重试的具体做法。', t: '21:42' },
            { role: 'ai', text: '第 2 题：' + list[1].stem, t: '21:43', q: true }
          ],
          scores: [8.2], startedAt: Date.now(), elapsed: 184, qRemain: 214, phase: 'ask'
        };
      }
      if (!App.interview) {
        return page('<div class="card" style="padding:40px;max-width:620px;margin:0 auto"><h2 class="serif" style="font-size:26px">还没有进行中的面试</h2><p class="lead" style="margin-top:8px">先配置方向与难度，再开始。</p><a class="btn btn-primary mt24" href="#/interview">去配置</a></div>');
      }
      return page('<div class="iv-wrap">' + ivMarkup() + '</div>');
    },
    after() {
      const s = App.interview;
      if (!s) return;
      const chat = U.qs('#iv-chat'); if (chat) chat.scrollTop = chat.scrollHeight;
      Router.setTimer(() => {
        s.elapsed++; s.qRemain--;
        if (U.qs('#iv-timer')) U.qs('#iv-timer').textContent = U.hms(s.elapsed);
        if (U.qs('#iv-used')) U.qs('#iv-used').textContent = U.hms(s.elapsed);
        if (U.qs('#iv-remain')) U.qs('#iv-remain').textContent = U.ms(Math.max(0, s.qRemain));
        if (s.qRemain <= 0) { s.qRemain = 300; submitInterview('（本题已超时，系统自动提交）'); }
      }, 1000);
      const ta = U.qs('#iv-text');
      ta.addEventListener('keydown', e => {
        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendAnswer(); }
      });
      U.qs('[data-send]').addEventListener('click', sendAnswer);
      const mic = U.qs('[data-mic]');
      const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
      let recog = null, listening = false, baseText = '';
      const micIdle = () => { mic.classList.remove('rec'); mic.innerHTML = MIC_SVG; };
      const stopRec = () => { if (recog && listening) { try { recog.stop(); } catch (e) { } } };
      window.addEventListener('hashchange', stopRec, { once: true });
      mic.addEventListener('click', () => {
        if (listening) { stopRec(); return; }
        if (!SR) { UI.toast('当前浏览器不支持语音识别，请用 Chrome 或 Edge 打开', 'err'); return; }
        try {
          recog = new SR();
          recog.lang = 'zh-CN';
          recog.continuous = true;
          recog.interimResults = true;
          baseText = ta.value.trim();
          recog.onstart = () => {
            listening = true;
            mic.classList.add('rec'); mic.innerHTML = '<span class="rec-dot"></span>';
            UI.toast('开始听写，说话内容会实时转成文字');
          };
          recog.onresult = ev => {
            let finalText = '', interim = '';
            for (let i = ev.resultIndex; i < ev.results.length; i++) {
              const r = ev.results[i];
              if (r.isFinal) finalText += r[0].transcript; else interim += r[0].transcript;
            }
            if (finalText) baseText = baseText ? baseText + ' ' + finalText : finalText;
            ta.value = baseText + (interim ? (baseText ? ' ' : '') + interim : '');
          };
          recog.onerror = ev => {
            listening = false; micIdle();
            const map = {
              'not-allowed': '麦克风权限被拒绝，请在地址栏允许后重试',
              'service-not-allowed': '浏览器禁止了语音识别服务',
              'network': '语音识别服务不可用，请检查网络或换用 Chrome / Edge',
              'no-speech': '没有听到声音，请靠近麦克风重试',
              'audio-capture': '没有检测到麦克风设备',
              'aborted': ''
            };
            const msg = map[ev.error] != null ? map[ev.error] : '语音识别出错：' + ev.error;
            if (msg) UI.toast(msg, 'err');
          };
          recog.onend = () => { listening = false; micIdle(); };
          recog.start();
        } catch (err) {
          listening = false; micIdle();
          UI.toast('无法启动语音识别：' + (err && err.message ? err.message : '未知错误'), 'err');
        }
      });
      U.qs('[data-mute]').addEventListener('click', e => { e.currentTarget.textContent = e.currentTarget.textContent.includes('播报') ? '🔇 已静音' : '🔊 播报中'; UI.toast('语音播报为原型模拟'); });
      U.qs('[data-end-iv]').addEventListener('click', () => UI.confirm({ title: '结束面试', text: '已作答的题目会保留，结束后面试报告将按已答内容生成。', okText: '结束并生成报告', danger: true, onOk() { finishInterview(); } }));
      U.qs('[data-exit]').addEventListener('click', e => { e.preventDefault(); UI.confirm({ title: '退出面试', text: '退出后本次会话保存在本地，下次可继续。', okText: '退出', onOk() { Router.navigate('/interview'); } }); });
      U.qsa('[data-jump]').forEach(j => j.addEventListener('click', () => UI.toast('原型演示：面试中不支持跳题')));
      U.qsa('[data-replay]').forEach(r => r.addEventListener('click', () => UI.toast('正在重播（原型模拟）')));
      function sendAnswer() {
        stopRec();
        const v = ta.value.trim();
        if (!v) return;
        submitInterview(v);
        ta.value = '';
      }
    }
  });

  function submitInterview(text) {
    const s = App.interview;
    if (s.phase === 'grading' || s.phase === 'done' || s.idx >= s.qs.length) return;
    s.phase = 'grading';
    pushMsg({ role: 'me', text: text === '（本题已超时，系统自动提交）' ? text : text, t: U.now() });
    typing(true, 'AI 面试官正在分析你的回答…');
    setTimeout(() => {
      const q = s.qs[s.idx];
      const base = Math.max(4, Math.min(9.4, 4.6 + text.length / 55 + (Math.random() * 1.6 - 0.8)));
      const score = Math.round(base * 10) / 10;
      s.scores[s.idx] = score;
      typing(true, '评分中…');
      setTimeout(() => {
        typing(false);
        const hit = (q.keyPoints || []).slice(0, Math.max(2, Math.round(score / 3)));
        pushMsg({ role: 'ai', text: '本题得分 ' + score.toFixed(1) + ' / 10。命中要点：' + hit.join('、') + '。' + (score >= 8 ? '回答完整，继续保持。' : score >= 6 ? '基本到位，可以再补一个具体例子。' : '建议先给结论，再补实现细节。'), t: U.now() });
        s.idx++;
        s.qRemain = 300;
        s.phase = s.idx >= s.qs.length ? 'done' : 'ask';
        if (s.idx >= s.qs.length) {
          pushMsg({ role: 'ai', text: '本次面试结束。你的综合表现已生成报告，可以查看六个维度的得分与改进建议。', t: U.now() });
          setTimeout(() => UI.modal({
            title: '面试已完成',
            body: '<p class="lead">是否查看本次面试报告？</p>',
            foot: '<button class="btn" data-close>稍后</button><button class="btn btn-primary" data-ok>查看报告</button>',
            onOk() { finishInterview(); }
          }), 500);
        } else {
          pushMsg({ role: 'ai', text: '第 ' + (s.idx + 1) + ' 题：' + s.qs[s.idx].stem, t: U.now(), q: true });
          const side = U.qs('.iv-side'); if (side) side.outerHTML = ivMarkup().match(/<aside class="iv-side">[\s\S]*<\/aside>/)[0];
        }
      }, 700);
    }, 900);
  }

  /* ================= 面试报告 ================= */
  function buildReport() {
    const s = App.interview;
    const avg = s.scores.length ? s.scores.reduce((a, b) => a + b, 0) / s.scores.length : 6.5;
    const clamp = v => Math.max(3.6, Math.min(9.8, v));
    const dims = [
      ['技术准确性', clamp(avg + 0.4), 30], ['知识广度', clamp(avg - 0.3), 10], ['原理理解', clamp(avg + 0.1), 20],
      ['工程实践', clamp(avg - 0.2), 20], ['逻辑结构', clamp(avg + 0.2), 10], ['表达清晰', clamp(avg - 0.5), 10]
    ];
    const total = dims.reduce((a, d) => a + d[1] * d[2] / 100, 0) * 10;
    const report = {
      id: 'rp' + Date.now(), dir: s.dir, diff: s.diff, date: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
      duration: s.elapsed, count: s.scores.length, scores: s.scores.slice(), dims: dims,
      total: Math.round(total * 10) / 10, prev: App.lastReport ? App.lastReport.total : 74.6
    };
    App.lastReport = report;
    App.reports = App.reports || [];
    App.reports.unshift(report);
    Store.set('ys_reports', App.reports.slice(0, 8));
    return report;
  }
  function finishInterview() {
    const r = buildReport();
    App.interview = null;
    Router.navigate('/interview/report?id=' + r.id);
  }
  const DEMO_REPORTS = [
    { id: 'rp_demo1', dir: 's1', diff: '中等', date: '2026-11-06 20:14:22', duration: 1580, count: 10, total: 82.4, prev: 76.3, scores: [8.2, 8.8, 7.9, 8.4, 8.1, 8.6, 7.7, 8.9, 8.0, 8.3] },
    { id: 'rp_demo2', dir: 's2', diff: '困难', date: '2026-11-04 19:02:10', duration: 1740, count: 8, total: 71.8, prev: 74.2, scores: [6.8, 7.2, 7.0, 7.6, 6.9, 7.4, 7.1, 7.2] },
    { id: 'rp_demo3', dir: 's5', diff: '简单', date: '2026-10-30 21:33:05', duration: 940, count: 5, total: 76.5, prev: null, scores: [7.8, 7.4, 8.0, 7.2, 7.6] }
  ];
  function findReport(id) {
    const list = (App.reports || []).concat(Store.get('ys_reports', []), DEMO_REPORTS);
    return list.find(r => r.id === id) || list[0];
  }
  function reportDims(r) {
    if (r.dims) return r.dims;
    const avg = r.total / 10;
    return [['技术准确性', avg + 0.3, 30], ['知识广度', avg - 0.4, 10], ['原理理解', avg + 0.1, 20], ['工程实践', avg - 0.2, 20], ['逻辑结构', avg + 0.2, 10], ['表达清晰', avg - 0.5, 10]];
  }
  Router.register('/interview/report', {
    title: '面试报告',
    render(q) {
      const r = findReport(q.get('id'));
      const dims = reportDims(r);
      const sub = DB.subject(r.dir);
      const trend = r.prev == null ? null : r.total - r.prev;
      const diffText = trend == null ? '首次面试' : trend > 0 ? '相比上次 +' + trend.toFixed(1) : trend < 0 ? '相比上次 ' + trend.toFixed(1) : '与上次持平';
      const dimHtml = dims.map(d => {
        const color = d[1] >= 8 ? 'var(--jade)' : d[1] >= 6 ? 'var(--amber)' : 'var(--red)';
        return '<div class="dim-card"><div class="t"><span>' + d[0] + '</span></div><div class="v" style="color:' + color + '">' + (d[1] * 10).toFixed(0) + '<span style="font-size:12px;color:var(--ink-3);margin-left:2px">/100</span></div><div class="track"><i style="width:' + (d[1] * 10) + '%;background:' + color + '"></i></div></div>';
      }).join('');
      const circle = 2 * Math.PI * 62;
      return page(
        '<div class="between mb24"><div><div class="eyebrow">Interview Report</div><h1 class="serif" style="font-size:34px;margin-top:6px">面试报告</h1></div><div class="flex gap8"><a class="btn" href="#/interview/history">历史记录</a><button class="btn btn-primary" data-again>再来一次</button></div></div>' +
        '<div class="report-head"><div class="ring-lg"><svg width="150" height="150" viewBox="0 0 150 150"><circle cx="75" cy="75" r="62" fill="none" stroke="var(--paper-2)" stroke-width="10"/><circle cx="75" cy="75" r="62" fill="none" stroke="var(--jade)" stroke-width="10" stroke-linecap="round" stroke-dasharray="' + circle + '" stroke-dashoffset="' + (circle * (1 - r.total / 100)) + '" transform="rotate(-90 75 75)"/></svg><div class="val"><b>' + r.total.toFixed(1) + '</b><span>综合评分</span></div></div>' +
        '<div style="flex:1"><div class="flex gap8"><span class="badge badge-ink">' + esc(sub.name) + '</span><span class="badge">' + r.diff + '</span><span class="badge">' + r.date + '</span></div><h2 class="serif" style="font-size:26px;margin-top:12px">' + (r.total >= 80 ? '整体表现不错，可以开始投递了' : r.total >= 65 ? '基础达标，还有两处明显短板' : '建议先补基础，再回来面一次') + '</h2>' +
        '<div class="flex gap20 mt16 small muted"><span>回答准确度 <b style="color:var(--ink)">' + (r.total / 10).toFixed(1) + '</b></span><span>面试时长 <b style="color:var(--ink)">' + Math.round(r.duration / 60) + ' 分钟</b></span><span>答题数量 <b style="color:var(--ink)">' + r.count + ' 题</b></span><span class="' + (trend >= 0 ? 'badge badge-jade' : 'badge badge-red') + '">' + diffText + '</span></div></div></div>' +
        '<section class="section" style="margin-top:36px"><div class="section-head"><div><div class="eyebrow">Dimensions</div><h2 style="font-size:24px">能力分析</h2></div><span class="small muted">六个维度，按权重计入综合评分</span></div><div class="dim-grid">' + dimHtml + '</div></section>' +
        '<div class="pros-cons mt24"><div class="pc-card good"><h4 style="margin-bottom:8px">优点</h4><ul><li>技术事实准确，术语使用规范</li><li>能结合项目讲清实现步骤</li><li>回答有结论先行的习惯</li></ul></div>' +
        '<div class="pc-card bad"><h4 style="margin-bottom:8px">待提升</h4><ul><li>边界条件讲得少，缺少失败处理</li><li>原理层面停留在使用层，未讲清取舍</li><li>语速偏快，关键结论容易被带过</li></ul></div></div>' +
        '<section class="section" style="margin-top:36px"><div class="section-head"><div><div class="eyebrow">Advice</div><h2 style="font-size:24px">改进建议</h2></div></div><div class="panel"><div class="panel-body"><ul class="advice">' +
        '<li><span class="n">01</span><span>每道题都补一句「失败时会怎样」，例如工具超时、参数校验失败、检索为空。</span></li>' +
        '<li><span class="n">02</span><span>把「怎么用」升级成「为什么这样设计」，至少讲清一个取舍。</span></li>' +
        '<li><span class="n">03</span><span>回答控制在 90 秒内，先给结论，再给两个支撑点。</span></li></ul></div></div></section>' +
        '<section class="section" style="margin-top:36px"><div class="section-head"><div><div class="eyebrow">Next</div><h2 style="font-size:24px">能力提升建议</h2></div></div><div class="upgrade-grid">' +
        '<div class="upgrade-card"><h4>推荐题目</h4><p>去「' + esc(sub.name) + '」的 Agent 阶段，重点练工具调用与边界处理相关的 12 道题。</p><a class="link small" href="#/bank?s=' + sub.id + '">去刷题 →</a></div>' +
        '<div class="upgrade-card"><h4>推荐课程</h4><p>路线里第 3 门「LangGraph 多智能体实战」，补齐状态编排的工程细节。</p><a class="link small" href="#/routes?s=' + sub.id + '">看路线 →</a></div>' +
        '<div class="upgrade-card"><h4>推荐再次面试</h4><p>建议 3 天后再面一次同方向、同难度，重点看「原理理解」是否提升。</p><a class="link small" href="#/interview">再面一次 →</a></div></div></section>' +
        '<div class="flex gap8 mt24"><a class="btn" href="#/interview/history">查看历史记录</a><button class="btn" data-replay-iv>查看面试回放</button><span class="small muted" style="margin-left:auto">报告编号 ' + r.id + '</span></div>'
      );
    },
    after() {
      U.qs('[data-again]').addEventListener('click', () => Router.navigate('/interview'));
      U.qs('[data-replay-iv]').addEventListener('click', () => UI.modal({ title: '面试回放', body: '<p class="lead">回放会按时间顺序重放这场面试的问答与评分。</p><div class="result-card"><div class="small muted">原型演示：回放为静态示意</div></div>', foot: '<button class="btn" data-close>关闭</button>' }));
    }
  });

  /* ================= 面试记录 ================= */
  Router.register('/interview/history', {
    title: '面试记录',
    render() {
      const list = (App.reports || []).concat(Store.get('ys_reports', []), DEMO_REPORTS).slice(0, 10);
      const avg = list.length ? (list.reduce((a, r) => a + r.total, 0) / list.length).toFixed(1) : '0';
      const max = list.length ? Math.max.apply(null, list.map(r => r.total)).toFixed(1) : '0';
      const dur = list.reduce((a, r) => a + r.duration, 0);
      return page(
        '<div class="between mb24"><div><div class="eyebrow">History</div><h1 class="serif" style="font-size:34px;margin-top:6px">面试记录</h1></div><a class="btn btn-primary" href="#/interview">再面一次</a></div>' +
        '<div class="wb-stats"><div class="admin-stat"><div class="k">面试总次数</div><div class="v">' + list.length + '</div></div><div class="admin-stat"><div class="k">平均分数</div><div class="v">' + avg + '</div></div><div class="admin-stat"><div class="k">最高分数</div><div class="v">' + max + '</div></div><div class="admin-stat"><div class="k">累计时长</div><div class="v">' + Math.round(dur / 60) + ' 分</div></div></div>' +
        '<div class="mt24" style="display:grid;gap:8px">' + list.map(r => {
          const color = U.scoreColor(r.total / 10);
          return '<a class="course-row" href="#/interview/report?id=' + r.id + '"><div style="width:46px;height:46px;border-radius:50%;border:2px solid ' + color + ';display:grid;place-items:center;font-family:var(--font-mono);font-size:12px;color:' + color + '">' + r.total.toFixed(1) + '</div><div><h4>' + esc(DB.subject(r.dir).name) + '</h4><div class="sub"><span>' + r.date + '</span><span>' + Math.round(r.duration / 60) + ' 分钟</span><span>' + r.count + ' 题</span><span>' + r.diff + '</span></div></div><span style="margin-left:auto" class="link small">查看报告 →</span></a>';
        }).join('') + '</div>' +
        C.pager(list.length, 1, 10, 'noop')
      );
    }
  });

  /* ================= 个人中心 ================= */
  Router.register('/profile', {
    title: '个人中心',
    render() {
      const u = App.user;
      return page(
        '<div class="eyebrow">Profile</div><h1 class="serif" style="font-size:34px;margin:6px 0 22px">个人中心</h1>' +
        '<div class="profile-card"><div class="avatar avatar-lg">' + esc(u.avatar) + '</div><div><div class="name">' + esc(u.name) + '</div><div class="flex gap8 mt8"><span class="badge badge-jade">学员</span><span class="small muted">加入于 ' + u.joined + '</span></div></div>' +
        '<div class="profile-stats"><div><div class="v">218</div><div class="k">做题数</div></div><div><div class="v">7</div><div class="k">面试数</div></div><div><div class="v">4</div><div class="k">错题数</div></div></div></div>' +
        '<div style="display:grid;grid-template-columns:1.4fr 1fr;gap:18px;margin-top:18px;align-items:start">' +
        '<div class="panel"><div class="panel-head"><h3>基本信息</h3><span class="small muted">手机号不可修改</span></div><div class="panel-body">' +
        '<div class="form-grid"><div class="field"><label>用户名</label><input class="input" id="pf-name" value="' + esc(u.name) + '"></div>' +
        '<div class="field"><label>性别</label><select class="select" id="pf-gender"><option' + (u.gender === '保密' ? ' selected' : '') + '>保密</option><option' + (u.gender === '男' ? ' selected' : '') + '>男</option><option' + (u.gender === '女' ? ' selected' : '') + '>女</option></select></div>' +
        '<div class="field"><label>手机号</label><input class="input" value="' + u.phone + '" disabled></div>' +
        '<div class="field"><label>邮箱</label><input class="input" id="pf-email" value="' + esc(u.email) + '"></div></div>' +
        '<div class="field"><label>头像</label><div class="flex gap12"><div class="avatar avatar-lg">' + esc(u.avatar) + '</div><button class="btn btn-sm" data-act="upload">上传头像</button><span class="small muted">JPG / PNG，建议 200×200</span></div></div>' +
        '<button class="btn btn-primary" data-save-profile>保存修改</button></div></div>' +
        '<div class="panel"><div class="panel-head"><h3>学习数据</h3></div><div class="panel-body"><div style="display:grid;gap:14px">' +
        [['已完成题目', '218 题'], ['平均正确率', '78%'], ['AI 面试次数', '7 次'], ['已学课程', Object.keys(Store.get('ys_completed', {})).filter(id => DB.course(id)).length + ' 门']].map(r => '<div class="between"><span class="small muted">' + r[0] + '</span><b>' + r[1] + '</b></div>').join('') +
        '</div><div class="divider"></div><p class="small muted">统计口径：作答记录与面试提交记录，仅统计服务端完成的数据。</p></div></div></div>'
      );
    },
    after() {
      U.qs('[data-save-profile]').addEventListener('click', () => {
        const n = U.qs('#pf-name').value.trim();
        if (!n) return UI.toast('用户名不能为空', 'err');
        App.user.name = n; App.user.email = U.qs('#pf-email').value; App.user.gender = U.qs('#pf-gender').value; App.user.avatar = n.charAt(0);
        UI.toast('资料已保存');
        setTimeout(() => Router.render(), 400);
      });
      U.qs('[data-act="upload"]').addEventListener('click', () => UI.toast('原型演示：头像上传未接入存储', 'warn'));
    }
  });
})();
