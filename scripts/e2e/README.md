# 端到端冒烟测试

覆盖学员端与管理端的核心只读链路，用于每次发布前的快速回归：

- 学员端登录、首页快速开始顺序、热门学科跳转与选中、面试方向数量；
- 管理端登录页选项、数据看板统计卡、通知铃铛、面试题库工具栏、Markdown 解析预览、检索测试；
- 前端运行时异常检测。

不会确认导入、不会提交作答，不会写入业务数据（Markdown 预览只在 Redis 写临时 token）。

## 前置

1. 服务已启动：后端 `8080`、学员端 `5173`、管理端 `5174`；
2. Python 3.10+，安装 Playwright：

```bash
python -m pip install playwright
```

3. 本脚本默认使用本机 Microsoft Edge（`channel=msedge`），失败时回退到 Playwright 自带 Chromium；
   若未安装浏览器，可执行 `python -m playwright install chromium`。

## 运行

```bash
python scripts/e2e/smoke_test.py
```

可用环境变量覆盖地址与账号：

```bash
PORTAL_BASE=http://localhost:5173
ADMIN_BASE=http://localhost:5174/admin
STUDENT_ACCOUNT=13800138000
STUDENT_PASSWORD=Student123
ADMIN_ACCOUNT=admin
ADMIN_PASSWORD=Yszx@123456
BROWSER_CHANNEL=msedge
```

全部通过时退出码为 0，否则为 1。

## 单元测试

后端单元测试（22 个用例）覆盖 Markdown 导入、频控、幂等与看板统计：

```bash
cd backend
mvn test
```

> 如果本机未安装 Maven，可使用项目已有的 Maven 容器执行：
> `docker run --rm -v "$PWD:/app" -v "$HOME/.m2:/root/.m2" -w /app maven:3.9-eclipse-temurin-17 mvn test`
