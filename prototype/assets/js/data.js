/* 云枢智学 · 原型演示数据（全部为假数据，仅用于交互演示） */
(function () {
  'use strict';

  // 固定种子，保证每次刷新数据一致
  let _s = 20261106;
  function rnd() { _s = (_s * 16807) % 2147483647; return (_s - 1) / 2147483646; }
  function pick(arr) { return arr[Math.floor(rnd() * arr.length)]; }
  function int(a, b) { return a + Math.floor(rnd() * (b - a + 1)); }

  /* ---------------- 学科 ---------------- */
  const SUBJECTS = [
    { id: 's1', name: 'AI应用与智能体开发', icon: '🤖', color: '#0F6B57', desc: '从 Prompt 工程到 Agent 工作流，把大模型做成能落地的应用。', order: 1 },
    { id: 's2', name: 'AI大模型开发', icon: '🧠', color: '#27507A', desc: '模型微调、推理优化与训练数据工程，面向算法与平台岗位。', order: 2 },
    { id: 's3', name: 'AI具身智能机器人开发', icon: '🦾', color: '#8A5A2B', desc: '感知、决策、控制一体化的具身智能系统实践。', order: 3 },
    { id: 's4', name: 'AI嵌入式_机器人开发', icon: '🔧', color: '#5B5B7A', desc: '端侧算力、传感器接入与实时系统的软硬协同。', order: 4 },
    { id: 's5', name: 'AI测试', icon: '🧪', color: '#7A4B5B', desc: '大模型应用的质量体系：评测集、回归与线上监控。', order: 5 },
    { id: 's6', name: 'AI运维', icon: '🛠️', color: '#3F6B4A', desc: '推理服务部署、成本控制与稳定性保障。', order: 6 },
    { id: 's7', name: 'AI视觉设计', icon: '🎨', color: '#A15C2E', desc: '生成式视觉工作流与设计交付标准。', order: 7 },
    { id: 's8', name: 'AI视频与短剧创作', icon: '🎬', color: '#6B4B8A', desc: '从分镜到成片的 AI 视频生产链路。', order: 8 }
  ];

  /* ---------------- 阶段 ---------------- */
  const STAGE_PLAN = {
    s1: ['开发环境与 Python 基础', '大模型基础与 Prompt 工程', 'RAG 与知识库构建', 'Agent 框架与工具调用', '项目实战与部署'],
    s2: ['深度学习基础', '模型微调与数据工程', '推理优化与评测'],
    s3: ['感知与控制基础', '具身决策模型', '真机调试与仿真'],
    s4: ['嵌入式基础', '端侧模型部署', '实时系统与联调'],
    s5: ['测试基础与用例设计', '大模型评测体系', '自动化与线上质量'],
    s6: ['Linux 与容器基础', '推理服务部署', '监控、告警与成本'],
    s7: ['视觉基础与工具链', '生成式视觉工作流', '设计交付与规范'],
    s8: ['分镜与叙事', 'AI 视频生成链路', '成片与分发']
  };
  const STAGES = [];
  SUBJECTS.forEach(sub => {
    (STAGE_PLAN[sub.id] || []).forEach((name, i) => {
      STAGES.push({
        id: 'st_' + sub.id + '_' + (i + 1),
        subjectId: sub.id,
        name: name,
        icon: ['🧩', '⚙️', '🧱', '🚀', '📦', '🔍'][i % 6],
        order: i + 1,
        enabled: true,
        questionCount: 0
      });
    });
  });

  /* ---------------- 题目 ---------------- */
  const TYPE_NAME = { single: '单选', multi: '多选', judge: '判断', short: '简答' };
  const seedQuestions = [
    {
      stageId: 'st_s1_2', type: 'single', difficulty: '简单', tags: ['Prompt', '基础'],
      stem: '在设计一个客服场景的 Prompt 时，下列哪种做法最有助于降低模型答非所问的概率？',
      options: [
        { key: 'A', text: '把温度参数调到最高，让回答更自然' },
        { key: 'B', text: '明确角色、任务边界与拒答规则，并给出输出格式' },
        { key: 'C', text: '把公司全部文档一次性塞进系统提示词' },
        { key: 'D', text: '让模型自由发挥，避免约束影响体验' }
      ],
      answer: ['B'],
      explanation: '角色、边界、拒答规则与输出格式属于结构化约束，能显著降低跑题率；温度只影响随机性，与跑题无关。',
      done: 1284, correct: 1042
    },
    {
      stageId: 'st_s1_3', type: 'multi', difficulty: '中等', tags: ['RAG', '检索'],
      stem: '一个 RAG 问答系统的召回率偏低，下列哪些措施可能有效？（多选）',
      options: [
        { key: 'A', text: '对文档做更细粒度的切分并保留标题层级' },
        { key: 'B', text: '引入向量 + 关键词的混合检索' },
        { key: 'C', text: '把 topK 直接调小到 1' },
        { key: 'D', text: '对 query 做改写与同义词扩展' }
      ],
      answer: ['A', 'B', 'D'],
      explanation: '细粒度切分、混合检索、查询改写都能提升召回；缩小 topK 只会让候选更少，通常进一步降低召回。',
      done: 913, correct: 621
    },
    {
      stageId: 'st_s1_4', type: 'short', difficulty: '困难', tags: ['Agent', '工具调用'],
      stem: '请说明 Agent 中「工具调用」的完整链路，并指出至少两个容易出错的边界情况。',
      reference: '完整链路为：模型根据用户意图选择工具 → 生成结构化参数 → 运行时校验参数 → 执行工具 → 将结果回填上下文 → 模型决定继续调用或给出最终回答。常见边界：参数类型与必填校验失败、工具超时或返回空、循环调用导致死循环、工具结果过长挤占上下文。',
      keyPoints: ['意图识别与工具选择', '结构化参数生成与校验', '执行结果回填上下文', '超时与重试', '死循环防护'],
      answer: ['要点'],
      explanation: '考察对 Agent 执行闭环的理解，重点是参数校验、失败重试与循环控制三类工程边界。',
      done: 402, correct: 238
    },
    {
      stageId: 'st_s1_1', type: 'judge', difficulty: '简单', tags: ['环境'],
      stem: '在同一个项目中，为避免依赖冲突，应为每个项目建立独立的虚拟环境。',
      options: [{ key: 'A', text: '正确' }, { key: 'B', text: '错误' }],
      answer: ['A'],
      explanation: '独立虚拟环境可隔离依赖版本，是 Python 工程的基本实践。',
      done: 2103, correct: 1968
    },
    {
      stageId: 'st_s1_5', type: 'single', difficulty: '中等', tags: ['部署'],
      stem: '把 Agent 应用部署到生产环境时，下列哪项最应优先保障？',
      options: [
        { key: 'A', text: '界面动效' },
        { key: 'B', text: '密钥管理、限流与可观测性' },
        { key: 'C', text: '模型参数量' },
        { key: 'D', text: '提示词长度' }
      ],
      answer: ['B'],
      explanation: '生产环境的首要风险是密钥泄露、流量冲击与故障不可定位，三者分别对应密钥管理、限流和可观测性。',
      done: 660, correct: 512
    },
    {
      stageId: 'st_s2_2', type: 'single', difficulty: '中等', tags: ['微调'],
      stem: '在指令微调中，验证集 loss 持续下降但人工评测分数停滞，最可能的原因是？',
      options: [
        { key: 'A', text: '学习率过大' },
        { key: 'B', text: '评测集与训练分布不一致，或指标不敏感' },
        { key: 'C', text: '显存不足' },
        { key: 'D', text: 'batch size 太小' }
      ],
      answer: ['B'],
      explanation: 'loss 与人类偏好并不等价，常见原因是评测集分布偏移或称职指标无法反映真实质量。',
      done: 388, correct: 241
    },
    {
      stageId: 'st_s5_2', type: 'short', difficulty: '中等', tags: ['评测'],
      stem: '请设计一个大模型应用的回归评测方案，说明数据集、指标与执行频率。',
      reference: '数据集应覆盖核心场景、边界输入与历史缺陷；指标分自动指标（准确率、引用命中率、拒答率）与人工评分（有用性、忠实度）；执行频率为主线每次发布必跑全量、日常每日跑冒烟集。',
      keyPoints: ['场景与边界覆盖', '自动指标', '人工评分', '冒烟集与全量集', '发布门禁'],
      answer: ['要点'],
      explanation: '评测方案的关键是把「可自动化的部分」与「必须人工判断的部分」分开，并挂到发布流程上。',
      done: 176, correct: 94
    }
  ];
  seedQuestions.forEach((q, i) => {
    q.id = 'q' + (i + 1);
    const st = STAGES.find(s => s.id === q.stageId);
    q.subjectId = st ? st.subjectId : 's1';
    q.status = '已上架';
  });
  // 演示不同的审核状态
  seedQuestions[1].status = '待审核';
  seedQuestions[5].status = '已下架';
  seedQuestions[6].status = '已驳回';

  const QUESTIONS = seedQuestions.slice();
  const GEN_STEMS = [
    { t: 'single', s: '关于「{stage}」的工程实践，下列说法最准确的是？', o: ['只在本地验证即可上线', '需要明确的输入输出约束与失败兜底', '与业务目标无关', '不需要任何评测'], a: ['B'], e: '工程实践的核心是约束与兜底，其余选项均忽视了生产环境的真实约束。' },
    { t: 'judge', s: '「{stage}」阶段的目标是建立可复用的方法，而不是追求单次效果最优。', o: [{ key: 'A', text: '正确' }, { key: 'B', text: '错误' }], a: ['A'], e: '可复用、可评估、可回归，才是阶段目标。' },
    { t: 'multi', s: '在「{stage}」中，下列哪些属于必要的交付物？（多选）', o: ['设计说明与边界条件', '可复现的评测结果', '个人偏好配置', '失败与回滚方案'], a: ['A', 'B', 'D'], e: '交付物需要可复现、可回滚，个人偏好不属于交付标准。' },
    { t: 'single', s: '「{stage}」出现效果波动时，第一步应当做什么？', o: ['直接更换模型', '先定位波动来源，固定变量复现', '增加提示词长度', '降低验收标准'], a: ['B'], e: '先复现、再定位，是排查波动的基本顺序。' }
  ];

  STAGES.forEach(stage => {
    const existing = QUESTIONS.filter(q => q.stageId === stage.id).length;
    const target = 6 + (stage.order % 3);
    for (let i = existing; i < target; i++) {
      const tpl = GEN_STEMS[i % GEN_STEMS.length];
      const options = tpl.o.map((text, idx) => typeof text === 'string'
        ? { key: String.fromCharCode(65 + idx), text: text }
        : text);
      const q = {
        id: 'q' + stage.id.slice(3) + '_' + i,
        stageId: stage.id,
        subjectId: stage.subjectId,
        type: tpl.t,
        difficulty: ['简单', '中等', '困难'][i % 3],
        tags: [stage.name.split(' ')[0]],
        stem: tpl.s.replace('{stage}', stage.name),
        options: options,
        answer: tpl.a,
        explanation: tpl.e,
        done: int(60, 900),
        correct: int(30, 500),
        status: '已上架',
        createdAt: '2026-10-' + String(int(8, 28)).padStart(2, '0')
      };
      QUESTIONS.push(q);
    }
  });
  STAGES.forEach(stage => { stage.questionCount = QUESTIONS.filter(q => q.stageId === stage.id).length; });

  /* ---------------- 课程库 ---------------- */
  const COURSES = [
    { id: 'c1', title: 'Python 从零到工程化', bv: 'BV1GJ411x7h7', subjectId: 's1', duration: 320, views: 128400, source: 'B站', color: '#0F6B57' },
    { id: 'c2', title: '大模型应用开发入门：Prompt 到 RAG', bv: 'BV1cx4y1k7Zf', subjectId: 's1', duration: 268, views: 96300, source: 'B站', color: '#27507A' },
    { id: 'c3', title: 'LangGraph 多智能体实战', bv: 'BV1qz421c7Qm', subjectId: 's1', duration: 415, views: 54200, source: 'B站', color: '#8A5A2B' },
    { id: 'c4', title: 'RAG 系统从原型到上线', bv: 'BV1Hm4y1L7pK', subjectId: 's1', duration: 236, views: 71800, source: 'B站', color: '#5B5B7A' },
    { id: 'c5', title: 'Agent 工具体系设计', bv: 'BV1Ns421M7dR', subjectId: 's1', duration: 188, views: 39600, source: 'B站', color: '#7A4B5B' },
    { id: 'c6', title: 'Transformer 原理精讲', bv: 'BV1x44y1C7aB', subjectId: 's2', duration: 512, views: 210500, source: 'B站', color: '#27507A' },
    { id: 'c7', title: 'LoRA 微调全流程实操', bv: 'BV1Wm4y1x7Tg', subjectId: 's2', duration: 344, views: 88400, source: 'B站', color: '#0F6B57' },
    { id: 'c8', title: 'vLLM 推理服务部署', bv: 'BV1Yz421B7nQ', subjectId: 's6', duration: 176, views: 47100, source: 'B站', color: '#3F6B4A' },
    { id: 'c9', title: '大模型评测方法与实践', bv: 'BV1Kp4y1w7cV', subjectId: 's5', duration: 205, views: 33900, source: 'B站', color: '#7A4B5B' },
    { id: 'c10', title: '机器人运动学基础', bv: 'BV1bd4y1z7Ls', subjectId: 's3', duration: 298, views: 28600, source: 'B站', color: '#8A5A2B' },
    { id: 'c11', title: '端侧模型量化与加速', bv: 'BV1Qr421t7Hj', subjectId: 's4', duration: 254, views: 31200, source: 'B站', color: '#5B5B7A' },
    { id: 'c12', title: 'Midjourney 商业出图工作流', bv: 'BV1Tm4y1R7Xy', subjectId: 's7', duration: 168, views: 61200, source: 'B站', color: '#A15C2E' },
    { id: 'c13', title: 'AI 短片分镜与运镜', bv: 'BV1Ww4y1q7Zp', subjectId: 's8', duration: 142, views: 45800, source: 'B站', color: '#6B4B8A' },
    { id: 'c14', title: 'Kubernetes 基础与运维', bv: 'BV1Cz4y1P7mN', subjectId: 's6', duration: 386, views: 77300, source: '其他', color: '#3F6B4A' }
  ];

  // 课程按旧版：多学科归属、简介、上下架状态
  const COURSE_EXTRA = {
    c1: { subjects: ['s1'], intro: '从环境搭建到工程化实践，打好 Python 基础。' },
    c2: { subjects: ['s1'], intro: '从 Prompt 到 RAG，完成第一个大模型应用。' },
    c3: { subjects: ['s1', 's2'], intro: '多智能体编排与工具调用实战。' },
    c4: { subjects: ['s1'], intro: '检索、切分、重排与评测的完整链路。' },
    c5: { subjects: ['s1'], status: '下架', intro: '工具抽象与权限边界设计。' },
    c6: { subjects: ['s2'], intro: '从注意力机制到训练细节。' },
    c7: { subjects: ['s2'], intro: '数据准备、训练与评估的完整微调流程。' },
    c8: { subjects: ['s6', 's1'], intro: '推理服务的部署、扩缩容与成本控制。' },
    c9: { subjects: ['s5', 's2'], intro: '评测集设计、指标与回归方法。' },
    c10: { subjects: ['s3'], intro: '运动学与动力学基础。' },
    c11: { subjects: ['s4'], intro: '量化、剪枝与端侧推理。' },
    c12: { subjects: ['s7', 's8'], intro: '商业出图的风格控制与工作流。' },
    c13: { subjects: ['s8'], intro: '分镜、运镜与成片节奏。' },
    c14: { subjects: ['s6'], intro: '容器编排与集群运维基础。' }
  };
  COURSES.forEach(c => {
    const ex = COURSE_EXTRA[c.id] || {};
    c.subjectIds = ex.subjects || [c.subjectId];
    c.intro = ex.intro || (c.title + '：面向 ' + ((SUBJECTS.find(s => s.id === c.subjectIds[0]) || {}).name || '') + ' 方向的系统课程。');
    c.status = ex.status || '上架';
    delete c.subjectId;
  });

  /* ---------------- 学习路线 ---------------- */
  const ROUTES = [
    { id: 'r1', subjectId: 's1', name: 'AI 全栈开发工程师', audience: '零基础转行 / 应届生', degree: '大专及以上', enabled: true, desc: '从 Python 工程能力起步，串联 Prompt、RAG、Agent 与部署，形成一条可以直接做项目的完整路径。', courseIds: ['c1', 'c2', 'c4', 'c3', 'c5'] },
    { id: 'r2', subjectId: 's1', name: '智能体应用进阶', audience: '有开发经验，想转入 Agent 方向', degree: '本科及以上', enabled: true, desc: '面向已有后端或前端经验的同学，重点补齐工具调用、状态编排与线上治理。', courseIds: ['c2', 'c3', 'c5'] },
    { id: 'r3', subjectId: 's2', name: '大模型算法入门', audience: '算法方向在校生', degree: '本科及以上', enabled: false, desc: '从 Transformer 原理到微调实操，建立模型侧的基本功。', courseIds: ['c6', 'c7'] },
    { id: 'r4', subjectId: 's5', name: 'AI 质量保障', audience: '测试工程师转型', degree: '大专及以上', enabled: true, desc: '把评测集、回归与线上监控串成一套可执行的质量体系。', courseIds: ['c9', 'c8'] },
    { id: 'r5', subjectId: 's8', name: 'AI 短片创作', audience: '内容创作者', degree: '不限', enabled: true, desc: '从分镜到成片，用 AI 工具完成一条完整短片。', courseIds: ['c13', 'c12'] }
  ];

  /* ---------------- 面试题（方向 = 学科） ---------------- */
  const INTERVIEW_TOPICS = ['核心概念', '工程实践', '性能优化', '故障排查', '方案设计'];
  const INTERVIEW_Q = [];
  SUBJECTS.forEach(sub => {
    for (let i = 0; i < 12; i++) {
      const topic = INTERVIEW_TOPICS[i % INTERVIEW_TOPICS.length];
      INTERVIEW_Q.push({
        id: 'iq_' + sub.id + '_' + i,
        subjectId: sub.id,
        topic: topic,
        difficulty: ['简单', '中等', '困难'][i % 3],
        theme: topic,
        source: i % 3 === 0 ? '手工录入' : (i % 3 === 1 ? '文档解析' : '手动导入 Markdown'),
        stem: [
          '请说明你理解的 ' + sub.name + ' 中「' + topic + '」的关键点，并举一个具体例子。',
          '如果线上出现与 ' + topic + ' 相关的问题，你会按什么顺序排查？',
          '请设计一个方案，验证 ' + sub.name + ' 中「' + topic + '」的效果。'
        ][i % 3],
        reference: '回答应覆盖概念定义、适用边界、落地步骤与验证方式四层；能结合具体项目给出参数或指标更佳。',
        keyPoints: ['概念与边界', '落地步骤', '验证指标', '失败处理'],
        done: int(20, 300)
      });
    }
  });

  /* ---------------- 学员 ---------------- */
  const STUDENTS = [
    { id: 'u1', name: '林知远', phone: '138****6621', email: 'lin@example.com', registered: '2026-09-12', lastLogin: '2026-11-06 21:04', status: '已启用', days: 26, courses: 9, practice: 218, accuracy: 0.78, interviews: 7 },
    { id: 'u2', name: '陈默', phone: '139****3012', email: 'chen@example.com', registered: '2026-09-20', lastLogin: '2026-11-06 18:22', status: '已启用', days: 18, courses: 6, practice: 143, accuracy: 0.71, interviews: 4 },
    { id: 'u3', name: '周一鸣', phone: '137****8842', email: 'zhou@example.com', registered: '2026-10-02', lastLogin: '2026-11-05 22:41', status: '已启用', days: 12, courses: 4, practice: 96, accuracy: 0.66, interviews: 2 },
    { id: 'u4', name: '苏晚', phone: '136****1278', email: 'su@example.com', registered: '2026-10-08', lastLogin: '2026-11-06 12:15', status: '已启用', days: 21, courses: 11, practice: 302, accuracy: 0.84, interviews: 9 },
    { id: 'u5', name: '何嘉树', phone: '135****9930', email: 'he@example.com', registered: '2026-10-11', lastLogin: '2026-10-30 09:12', status: '已禁用', days: 3, courses: 1, practice: 12, accuracy: 0.5, interviews: 0 },
    { id: 'u6', name: '许知夏', phone: '133****4402', email: 'xu@example.com', registered: '2026-10-15', lastLogin: '2026-11-06 20:31', status: '已启用', days: 15, courses: 7, practice: 187, accuracy: 0.75, interviews: 5 },
    { id: 'u7', name: '高远', phone: '188****2201', email: 'gao@example.com', registered: '2026-10-19', lastLogin: '2026-11-04 15:08', status: '已启用', days: 8, courses: 3, practice: 54, accuracy: 0.62, interviews: 1 },
    { id: 'u8', name: '沈以宁', phone: '186****7765', email: 'shen@example.com', registered: '2026-10-22', lastLogin: '2026-11-06 09:47', status: '已启用', days: 19, courses: 8, practice: 241, accuracy: 0.81, interviews: 6 }
  ];

  /* ---------------- 后台账号 ---------------- */
  const ADMIN_USERS = [
    { id: 'a1', name: '超级管理员', phone: '130****0001', email: 'admin@yunshu.ai', role: '超级管理员', status: '已启用', lastLogin: '2026-11-06 21:12', builtin: true },
    { id: 'a2', name: '王审核', phone: '130****0002', email: 'review@yunshu.ai', role: '内容审核', status: '已启用', lastLogin: '2026-11-06 10:30', builtin: false },
    { id: 'a3', name: '李运营', phone: '130****0003', email: 'ops@yunshu.ai', role: '运营人员', status: '已启用', lastLogin: '2026-11-06 19:58', builtin: false },
    { id: 'a4', name: '赵运营', phone: '130****0004', email: 'ops2@yunshu.ai', role: '运营人员', status: '已禁用', lastLogin: '2026-10-28 14:02', builtin: false }
  ];

  /* ---------------- 角色与权限 ---------------- */
  const PERM_TREE = [
    { group: '数据看板', items: ['查看'] },
    { group: '学科管理', items: ['查看', '新增', '编辑', '删除'] },
    { group: 'B站课程库', items: ['查看', '新增', '编辑', '删除', '导入'] },
    { group: '题库管理', items: ['查看', '新增', '编辑', '删除', '导入', '导出', '审核'] },
    { group: '面试题库管理', items: ['查看', '上传', '编辑', '删除', '检索测试'] },
    { group: '学习路线', items: ['查看', '新增', '编辑', '删除'] },
    { group: '学员管理', items: ['查看', '编辑', '删除'] },
    { group: '用户管理', items: ['查看', '新增', '编辑', '禁用'] },
    { group: '系统管理', items: ['日志查看', '系统设置'] }
  ];
  const ALL_PERMS = [];
  PERM_TREE.forEach(g => g.items.forEach(it => ALL_PERMS.push(g.group + '·' + it)));

  const ROLES = [
    { id: 'r_super', name: '超级管理员', code: 'super_admin', desc: '系统内置唯一账号，拥有全部权限。', perms: ALL_PERMS.slice(), builtin: true },
    {
      id: 'r_review', name: '内容审核', code: 'content_reviewer', desc: '只把关不生产：查看全部内容并审核题目。', builtin: false,
      perms: ['学科管理·查看', 'B站课程库·查看', '题库管理·查看', '题库管理·审核', '面试题库管理·查看', '学习路线·查看', '数据看板·查看']
    },
    {
      id: 'r_ops', name: '运营人员', code: 'operator', desc: '除系统管理外的全部操作，不含审核权限。', builtin: false,
      perms: ALL_PERMS.filter(p => !p.startsWith('系统管理') && p !== '题库管理·审核')
    }
  ];

  /* ---------------- 操作日志 ---------------- */
  const LOGS = [
    { id: 'L10241', user: '李运营', action: '新增题目', type: '新增·创建', target: '题库 · q100231', ip: '10.12.3.44', time: '2026-11-06 21:02:11' },
    { id: 'L10240', user: '王审核', action: '审核通过', type: '审核', target: '题库 · q100228', ip: '10.12.3.51', time: '2026-11-06 20:47:03' },
    { id: 'L10239', user: '李运营', action: '编辑学习路线', type: '修改·编辑', target: '学习路线 · AI 全栈开发工程师', ip: '10.12.3.44', time: '2026-11-06 19:58:22' },
    { id: 'L10238', user: '超级管理员', action: '登录后台', type: '登录', target: '管理端', ip: '10.12.3.10', time: '2026-11-06 19:30:00' },
    { id: 'L10237', user: '李运营', action: '新增课程', type: '新增·创建', target: 'B站课程库 · LangGraph 多智能体实战', ip: '10.12.3.44', time: '2026-11-06 18:12:45' },
    { id: 'L10236', user: '王审核', action: '审核驳回', type: '审核', target: '题库 · q100219', ip: '10.12.3.51', time: '2026-11-06 17:40:12' },
    { id: 'L10235', user: '李运营', action: '禁用学员', type: '禁用·启用', target: '学员 · 何嘉树', ip: '10.12.3.44', time: '2026-11-06 16:22:08' },
    { id: 'L10234', user: '超级管理员', action: '修改角色权限', type: '修改·编辑', target: '角色 · 运营人员', ip: '10.12.3.10', time: '2026-11-06 15:05:33' },
    { id: 'L10233', user: '李运营', action: '导入课程', type: '新增·创建', target: 'B站课程库 · 批量导入 12 条', ip: '10.12.3.44', time: '2026-11-06 11:18:09' },
    { id: 'L10232', user: '赵运营', action: '登录后台', type: '登录', target: '管理端', ip: '10.12.3.77', time: '2026-11-05 22:41:02' },
    { id: 'L10231', user: '王审核', action: '上架题目', type: '修改·编辑', target: '题库 · q100226', ip: '10.12.3.51', time: '2026-11-05 20:16:50' },
    { id: 'L10230', user: '李运营', action: '删除错题记录', type: '删除', target: '学员 · 陈默', ip: '10.12.3.44', time: '2026-11-05 18:03:27' }
  ];

  /* ---------------- 图表数据 ---------------- */
  const WEEK = [
    { d: '10-31', v: 12 }, { d: '11-01', v: 18 }, { d: '11-02', v: 9 }, { d: '11-03', v: 22 },
    { d: '11-04', v: 15 }, { d: '11-05', v: 7 }, { d: '11-06', v: 10 }
  ];
  const GROWTH = {
    7: [128, 141, 139, 158, 172, 169, 186],
    30: Array.from({ length: 30 }, (_, i) => 60 + Math.round(28 * Math.sin(i / 4) + i * 4.2)),
    90: Array.from({ length: 90 }, (_, i) => 40 + Math.round(18 * Math.sin(i / 9) + i * 1.6))
  };

  window.DB = {
    SUBJECTS, STAGES, QUESTIONS, COURSES, ROUTES, INTERVIEW_Q,
    STUDENTS, ADMIN_USERS, ROLES, PERM_TREE, ALL_PERMS, LOGS, WEEK, GROWTH,
    TYPE_NAME
  };
  window.DB.subject = id => SUBJECTS.find(s => s.id === id);
  window.DB.stagesOf = sid => STAGES.filter(s => s.subjectId === sid).sort((a, b) => a.order - b.order);
  window.DB.qOfStage = sid => QUESTIONS.filter(q => q.stageId === sid);
  window.DB.routeOf = sid => ROUTES.find(r => r.subjectId === sid);
  window.DB.course = id => COURSES.find(c => c.id === id);
})();
