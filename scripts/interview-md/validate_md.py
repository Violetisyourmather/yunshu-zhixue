import csv
import os
import re
import sys

folder = sys.argv[1]
csv_path = sys.argv[2]
report_path = sys.argv[3]

HEADING = re.compile(r'^#{1,3}\s+(.+?)\s*$')
rows = []
report = ['# Markdown 导出校验报告', '']
summary = []
for name in sorted(os.listdir(folder)):
    if not name.endswith('.md') or name in ('转换报告.md', '校验报告.md'):
        continue
    path = os.path.join(folder, name)
    text = open(path, encoding='utf-8').read()
    blocks = []
    current_title = None
    current_lines = []
    for line in text.splitlines():
        m = HEADING.match(line)
        if m and line.startswith('### '):
            if current_title is not None:
                blocks.append((current_title, '\n'.join(current_lines).strip()))
            current_title = m.group(1)
            current_lines = []
        elif current_title is not None:
            current_lines.append(line)
    if current_title is not None:
        blocks.append((current_title, '\n'.join(current_lines).strip()))

    empty = 0
    max_answer = 0
    max_title = 0
    bad_hash = 0
    for index, (title, answer) in enumerate(blocks, start=1):
        if not title.strip():
            bad_hash += 1
        max_title = max(max_title, len(title))
        max_answer = max(max_answer, len(answer))
        is_empty = (not answer) or answer == '（原文未提供参考答案）'
        if is_empty:
            empty += 1
        if any(line.startswith('#') for line in answer.splitlines()):
            bad_hash += 1
        rows.append([name, index, title, len(answer), '是' if is_empty else '否'])
    size_kb = os.path.getsize(path) / 1024
    status = '可导入'
    if len(blocks) > 500:
        status = '超出 500 题'
    if size_kb > 2048:
        status = '超出 2MB'
    if bad_hash:
        status = '存在未转义标题'
    summary.append((name, len(blocks), empty, max_title, max_answer, round(size_kb, 1), status, bad_hash))

report.append('| 文件 | 题数 | 空答案 | 最长题干 | 最长答案 | 大小(KB) | 结论 | 异常 |')
report.append('|---|---|---|---|---|---|---|---|')
for item in summary:
    report.append('| ' + ' | '.join(str(x) for x in item) + ' |')
report.append('')
report.append('说明：空答案在导入时会写入「（原文未提供参考答案）」占位；建议在校对清单中筛出空答案/极短答案重点复核。')
open(report_path, 'w', encoding='utf-8').write('\n'.join(report))

with open(csv_path, 'w', encoding='utf-8-sig', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(['文件', '题号', '题干', '答案字数', '空答案'])
    writer.writerows(rows)
print('validated', len(summary), 'files,', len(rows), 'questions')
