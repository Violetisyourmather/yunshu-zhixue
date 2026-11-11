# MyBatis XML 映射

仅用于 MyBatis-Plus 难以表达的场景：看板趋势、正确率/掌握率等聚合统计、复杂联表查询。

约定：

- 文件按模块命名，如 `StatsMapper.xml`、`QuestionMapper.xml`；
- 简单 CRUD 一律用 MyBatis-Plus，不写 XML；
- 跨模块取数不得通过 SQL 直连对方表，须经对方 Service（02 §3.2 依赖规则）。
