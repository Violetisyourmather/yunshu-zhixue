"""学员端 / 管理端核心链路冒烟测试（只读，不写业务数据）。

前置：
1. 后端 8080、学员端 5173、管理端 5174 已启动；
2. `python -m pip install playwright`，并使用 Edge / Chrome / Chromium。

运行：
    python scripts/e2e/smoke_test.py

可用环境变量覆盖：
    PORTAL_BASE / ADMIN_BASE / STUDENT_ACCOUNT / STUDENT_PASSWORD /
    ADMIN_ACCOUNT / ADMIN_PASSWORD / BROWSER_CHANNEL
"""

import json
import os
import sys

from playwright.sync_api import Error as PlaywrightError
from playwright.sync_api import sync_playwright

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

PORTAL_BASE = os.getenv('PORTAL_BASE', 'http://localhost:5173').rstrip('/')
ADMIN_BASE = os.getenv('ADMIN_BASE', 'http://localhost:5174/admin').rstrip('/')
STUDENT_ACCOUNT = os.getenv('STUDENT_ACCOUNT', '13800138000')
STUDENT_PASSWORD = os.getenv('STUDENT_PASSWORD', 'Student123')
ADMIN_ACCOUNT = os.getenv('ADMIN_ACCOUNT', 'admin')
ADMIN_PASSWORD = os.getenv('ADMIN_PASSWORD', 'Yszx@123456')
BROWSER_CHANNEL = os.getenv('BROWSER_CHANNEL', 'msedge')

RESULTS = []


def check(name, condition, detail=''):
    RESULTS.append({'name': name, 'ok': bool(condition), 'detail': detail})
    print(('PASS ' if condition else 'FAIL ') + name + ((' | ' + detail) if detail else ''))


def launch_browser(playwright):
    try:
        return playwright.chromium.launch(channel=BROWSER_CHANNEL, headless=True)
    except PlaywrightError:
        return playwright.chromium.launch(headless=True)


def portal_login(page):
    page.goto(f'{PORTAL_BASE}/login', wait_until='networkidle')
    page.fill('#login-account', STUDENT_ACCOUNT)
    page.fill('#login-password', STUDENT_PASSWORD)
    page.locator('button[type=submit]').click()
    page.wait_for_url(f'{PORTAL_BASE}/', timeout=15000)


def admin_login(page):
    page.goto(f'{ADMIN_BASE}/login', wait_until='networkidle')
    remember = page.get_by_text('记住登录状态').count() > 0
    forgot = page.get_by_text('忘记密码？请联系超级管理员重置').count() > 0
    page.get_by_placeholder('请输入用户名或手机号').fill(ADMIN_ACCOUNT)
    page.get_by_placeholder('6~20 位密码').fill(ADMIN_PASSWORD)
    page.locator('.login-submit').click()
    page.wait_for_url('**/dashboard', timeout=15000)
    return remember, forgot


def run():
    with sync_playwright() as playwright:
        browser = launch_browser(playwright)
        context = browser.new_context(viewport={'width': 1500, 'height': 1000})
        context.add_init_script("localStorage.setItem('yszx_portal_guide_finished','1')")

        portal = context.new_page()
        page_errors = []
        portal.on('pageerror', lambda error: page_errors.append(str(error)))

        # ---------- 学员端 ----------
        portal_login(portal)
        check('学员端登录', portal.url.rstrip('/') == PORTAL_BASE, portal.url)

        portal.wait_for_selector('.quick-grid', timeout=10000)
        quick_titles = portal.locator('.quick-card h3').all_inner_texts()
        check(
            '首页快速开始顺序',
            quick_titles == ['继续上次学习', '规划学习路线', '随机练习 10 题', '开始 AI 面试'],
            ' / '.join(quick_titles),
        )

        portal.wait_for_selector('.stage-grid a.stage-card', timeout=10000)
        first_subject = portal.locator('.stage-grid a.stage-card h3').first.inner_text().strip()
        portal.locator('.stage-grid a.stage-card').first.click()
        portal.wait_for_url('**/bank**', timeout=10000)
        portal.wait_for_timeout(800)
        active = ''
        if portal.locator('.subject-item.active .nm').count():
            active = portal.locator('.subject-item.active .nm').inner_text().strip()
        check('热门学科跳转并选中', active == first_subject, f'{first_subject} -> {active}')

        portal.goto(f'{PORTAL_BASE}/interview', wait_until='networkidle')
        portal.wait_for_selector('.direction-card', timeout=10000)
        directions = portal.locator('.direction-card').all_inner_texts()
        check('面试方向数量 >= 9', len(directions) >= 9, f'count={len(directions)}')
        check('包含 Java后端开发', any('Java后端开发' in item for item in directions))

        # ---------- 管理端 ----------
        admin = context.new_page()
        admin.on('pageerror', lambda error: page_errors.append(str(error)))
        remember, forgot = admin_login(admin)
        check('登录页记住登录状态', remember)
        check('登录页忘记密码提示', forgot)

        admin.wait_for_timeout(1200)
        stat_cards = admin.locator('.stat-card').count()
        check('数据看板统计卡 = 4', stat_cards == 4, f'count={stat_cards}')
        check('通知铃铛存在', admin.locator('.bell-button').count() == 1)

        admin.goto(f'{ADMIN_BASE}/content/interview-bank', wait_until='networkidle')
        admin.wait_for_timeout(1200)
        actions = admin.locator('.admin-page-actions button').all_inner_texts()
        check('面试题库工具栏', all(
            any(label in item for item in actions)
            for label in ('Excel 导入', 'Markdown 导入', '检索测试')
        ), ' / '.join(actions))

        markdown_file = os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            'interview-md', '消息中间件篇.md')
        if os.path.exists(markdown_file):
            admin.get_by_role('button', name='Markdown 导入').click()
            dialog = admin.locator('.el-dialog').filter(has_text='Markdown 导入面试题')
            dialog.wait_for(state='visible')
            dialog.locator('input[type=file]').set_input_files(markdown_file)
            dialog.get_by_role('button', name='解析预览').click()
            admin.wait_for_selector('text=解析出 14 道题', timeout=20000)
            check('Markdown 解析预览', True, '消息中间件篇 14 题')
            dialog.get_by_role('button', name='取消', exact=True).click()
            admin.wait_for_timeout(400)
        else:
            check('Markdown 解析预览', False, '缺少 scripts/interview-md/消息中间件篇.md')

        admin.get_by_role('button', name='检索测试').click()
        search_dialog = admin.locator('.el-dialog').filter(has_text='检索测试').last
        search_dialog.wait_for(state='visible')
        search_dialog.get_by_placeholder('输入关键词，如：RAG').fill('RabbitMQ')
        search_dialog.get_by_role('button', name='检索').click()
        admin.wait_for_timeout(1200)
        hits = search_dialog.locator('.el-table__body-wrapper tbody tr').count()
        check('面试题库检索测试', hits > 0, f'hits={hits}')

        check('前端运行时无异常', not page_errors, '; '.join(page_errors[:3]))

        context.close()
        browser.close()

    failed = [item for item in RESULTS if not item['ok']]
    print('\nSUMMARY ' + json.dumps({'total': len(RESULTS), 'failed': len(failed)}, ensure_ascii=True))
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(run())
