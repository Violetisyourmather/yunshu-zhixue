import os
import re
import sys
import json

import fitz

ZERO = dict.fromkeys(map(ord, ['​', '‌', '‍', '﻿', '­']), None)
SKIP_WORDS = {
    '导学', '总结', '小结', '前言', '目录', '课程介绍', '写在前面', '结语', '本章', '回顾',
    '面试题', '代码块', '代码实现', '代码', '概述', '结构', '实现', '原理', '流程', '案例',
    '补充', '扩展', '简介', '介绍',
}
QUESTION_HINTS = ('如何', '什么', '为什么', '怎么', '哪些', '区别', '了解', '介绍', '谈', '是否',
                  '能不能', '有没有', '吗', '作用', '场景', '原理', '机制', '方案', '设计', '流程')
ANSWER_MARKERS = ('🤔', '我们', '是的', '候选人', '参考', '答案', '了解。')
QUESTION_RE = re.compile(r'^\s*(\d{1,3})\s*[.、．]\s*(.+?)\s*$')
HEADING_PREFIX_RE = re.compile(r'^\s*\d+(?:\.\d+)*\s*[.、．]?\s*')
BULLET_PREFIX_RE = re.compile(r'^\s*[•·▪◦●○]\s*(.+)$')
BULLETS = {'•', '·', '▪', '◦', '●', '○', '-', '－', '*'}
TITLE_SKIP_SUFFIX = ('参考回答', '参考答案', '答案')


def strip_numbering(text):
    previous = None
    current = text.strip()
    while previous != current:
        previous = current
        current = HEADING_PREFIX_RE.sub('', current).strip()
    return current


def cjk(ch):
    if not ch:
        return False
    return ('一' <= ch <= '鿿') or ('　' <= ch <= '〿') or ('＀' <= ch <= '￯')


def clean_text(s):
    return s.translate(ZERO).replace(' ', ' ').strip()


def extract_lines(path):
    doc = fitz.open(path)
    lines = []
    for pno, page in enumerate(doc):
        data = page.get_text('dict')
        for block in data.get('blocks', []):
            for line in block.get('lines', []):
                spans = line.get('spans', [])
                text = clean_text(''.join(sp.get('text', '') for sp in spans))
                if not text:
                    continue
                size = max((sp.get('size', 0) for sp in spans), default=0)
                mono = any((sp.get('flags', 0) & 8) for sp in spans)
                lines.append({'text': text, 'size': round(size, 1), 'mono': mono, 'page': pno + 1})
    return len(doc), lines


def detect_title(lines):
    if not lines:
        return None
    max_size = max(l['size'] for l in lines)
    candidates = [l['text'] for l in lines if abs(l['size'] - max_size) < 0.1 and len(l['text']) <= 40]
    if not candidates:
        return None
    counts = {}
    for c in candidates:
        counts[c] = counts.get(c, 0) + 1
    return max(counts, key=counts.get)


def body_size(lines):
    counts = {}
    for l in lines:
        if l['mono']:
            continue
        counts[l['size']] = counts.get(l['size'], 0) + 1
    if not counts:
        return 12.0
    return max(counts, key=counts.get)


def remove_noise(lines, title):
    out = []
    pending_bullet = False
    for l in lines:
        t = l['text']
        if l['mono']:
            continue
        if title and t == title:
            continue
        if t in BULLETS:
            pending_bullet = True
            continue
        bullet = False
        m = BULLET_PREFIX_RE.match(t)
        if m:
            t = '- ' + m.group(1).strip()
            bullet = True
        if re.fullmatch(r'\d{1,3}[.、．]?', t):
            continue
        if not re.search(r'[0-9A-Za-z一-鿿]', t):
            continue
        if t in SKIP_WORDS:
            continue
        if pending_bullet and not t.startswith('- '):
            t = '- ' + t
        pending_bullet = False
        out.append({'text': t, 'size': l['size'], 'page': l['page'], 'bullet': bullet})
    return out


def is_candidate_marker(t):
    return t.startswith('候选人')


def is_answer_marker_line(t):
    return t in ('参考答案：', '参考答案:', '候选人：', '候选人:', '答案：', '答案:')


def merge_answer(lines, emphasize_sizes=()):
    out = []
    for raw in lines:
        t = raw['text'].strip()
        if not t:
            continue
        if is_answer_marker_line(t):
            continue
        if t.startswith('🤔'):
            t = t[1:].strip()
            if not t:
                continue
        if raw['size'] in emphasize_sizes and len(t) <= 40 and not t.endswith(('。', '，', '；', '：')):
            out.append('**' + t + '**')
            continue
        if t.startswith(('难易程度', '出现频率')) or t.startswith('- ') or re.match(r'^\d+[.、．]', t):
            out.append(t)
            continue
        if not out:
            out.append(t)
            continue
        prev = out[-1]
        if prev.startswith(('难易程度', '出现频率')) or re.match(r'^[（(]\d+[）)]', t):
            out.append(t)
        elif prev.endswith('**') or prev.endswith(('。', '！', '？', '：', '；', '!', '?', ':', ';')):
            out.append(t)
        elif prev.startswith('- ') and (t.endswith(('：', ':')) or len(t) <= 12):
            out.append(t)
        elif cjk(prev[-1]) and cjk(t[0]):
            out[-1] = prev + t
        else:
            out[-1] = prev + ' ' + t
    return '\n'.join(out).strip()


def parse_qa(lines):
    markers = [i for i, l in enumerate(lines) if is_candidate_marker(l['text'])]
    qas = []
    for mi, m in enumerate(markers):
        qidx = None
        for j in range(m - 1, max(-1, m - 6), -1):
            if QUESTION_RE.match(lines[j]['text']):
                qidx = j
                break
        if qidx is None:
            continue
        title = QUESTION_RE.match(lines[qidx]['text']).group(2).strip()
        if not title:
            continue
        start = m + 1
        end = len(lines)
        if mi + 1 < len(markers):
            nm = markers[mi + 1]
            nq = None
            for j in range(nm - 1, max(-1, nm - 6), -1):
                if QUESTION_RE.match(lines[j]['text']):
                    nq = j
                    break
            if nq is not None and nq > start:
                end = nq
        qas.append((title, merge_answer(lines[start:end])))
    return qas


def heading_levels(lines, body):
    title_size = max((l['size'] for l in lines), default=body)
    levels = sorted({l['size'] for l in lines if l['size'] > body + 0.4 and l['size'] < title_size - 0.1}, reverse=True)
    if not levels:
        levels = sorted({l['size'] for l in lines if l['size'] > body + 0.4 and l['size'] <= title_size}, reverse=True)
    return levels


def level_titles(lines, size):
    titles = []
    for l in lines:
        if l['size'] != size or l.get('bullet'):
            continue
        t = strip_numbering(l['text'])
        if not t or re.fullmatch(r'[\d.]+', t):
            continue
        titles.append(t)
    return titles


def score_level(titles):
    if not titles:
        return -999
    count = len(titles)
    qmark = sum(1 for t in titles if t.endswith(('？', '?'))) / count
    hint = sum(1 for t in titles if any(w in t for w in QUESTION_HINTS)) / count
    marker = sum(1 for t in titles if t.startswith(ANSWER_MARKERS)) / count
    generic = sum(1 for t in titles if t.strip() in SKIP_WORDS) / count
    return qmark * 3 + hint * 2 - marker * 4 - generic * 2 + min(count, 50) * 0.02


def parse_heading_level(lines, size, emphasize_sizes):
    indexes = []
    for i, l in enumerate(lines):
        if l['size'] != size or l.get('bullet'):
            continue
        t = strip_numbering(l['text'])
        if not t or re.fullmatch(r'[\d.]+', t):
            continue
        indexes.append(i)
    qas = []
    skipped = []
    for hi, idx in enumerate(indexes):
        raw_title = strip_numbering(lines[idx]['text']).lstrip('#').replace('#', '＃')
        if not raw_title:
            continue
        end = indexes[hi + 1] if hi + 1 < len(indexes) else len(lines)
        if raw_title in SKIP_WORDS or raw_title.endswith(TITLE_SKIP_SUFFIX):
            skipped.append(raw_title)
            continue
        answer_lines = [l for l in lines[idx + 1:end]
                        if strip_numbering(l['text']) != raw_title]
        answer = merge_answer(answer_lines, emphasize_sizes)
        qas.append((raw_title, answer))
    return qas, skipped


def empty_ratio(qas):
    if not qas:
        return 1.0
    return sum(1 for _, a in qas if not a) / len(qas)


def quality(qas):
    valid = sum(1 for t, a in qas if len(a) >= 20)
    empty = sum(1 for t, a in qas if not a)
    return valid - empty * 0.5


def convert(path, outdir):
    name = os.path.splitext(os.path.basename(path))[0]
    pages, raw = extract_lines(path)
    title = detect_title(raw)
    body = body_size(raw)
    cleaned = remove_noise(raw, title)
    marker_count = sum(1 for l in cleaned if is_candidate_marker(l['text']))

    qa_candidates = parse_qa(cleaned)
    levels = heading_levels(cleaned, body)
    scored = []
    level_results = []
    if levels:
        for size in levels:
            titles = level_titles(cleaned, size)
            score = score_level(titles)
            marker_ratio = (sum(1 for t in titles if t.startswith(ANSWER_MARKERS)) / len(titles)) if titles else 1.0
            emphasize = {s for s in levels if s < size}
            qas, skipped = parse_heading_level(cleaned, size, emphasize)
            scored.append((score, size, len(titles)))
            level_results.append({
                'size': size, 'qas': qas, 'skipped': skipped, 'score': score,
                'marker_ratio': marker_ratio, 'empty': empty_ratio(qas),
            })
        best_score = max(s[0] for s in scored)
        if best_score <= 0:
            size = max((s[1] for s in scored if s[2] >= 3), default=levels[0])
        else:
            size = max(scored, key=lambda s: (s[0], s[1], s[2]))[1]
        primary = next(r for r in level_results if r['size'] == size)
        chosen = primary
        # 空答案过多时回退到更细/更粗一层，但排除“答案句被当成标题”的层级
        if primary['empty'] > 0.25:
            alternatives = [
                r for r in level_results
                if r['size'] != size and r['score'] > 0.3 and r['marker_ratio'] < 0.2
                and len(r['qas']) >= 5 and r['empty'] < primary['empty']
            ]
            if alternatives:
                chosen = max(alternatives, key=lambda r: (quality(r['qas']), -r['empty']))
        heading_candidates = chosen['qas']
        skipped = chosen['skipped']
        chosen_level = chosen['size']
    else:
        heading_candidates, skipped, chosen_level = [], [], None

    if quality(heading_candidates) >= quality(qa_candidates):
        mode = 'heading'
        qas = heading_candidates
    else:
        mode = 'qa'
        qas = qa_candidates
        skipped = []

    if len(qas) > 500:
        qas = qas[:500]

    dedup = []
    for t, a in qas:
        if dedup and dedup[-1][0] == t:
            if a and a not in dedup[-1][1]:
                dedup[-1] = (t, dedup[-1][1] + ('\n' if dedup[-1][1] else '') + a)
            continue
        dedup.append((t, a))
    qas = dedup

    os.makedirs(outdir, exist_ok=True)
    outpath = os.path.join(outdir, name + '.md')
    parts = []
    empty = 0
    total_len = 0
    for t, a in qas:
        t = t.replace('\n', ' ').strip().lstrip('#').strip().replace('#', '＃')
        a = a.replace('​', '').strip()
        if not a:
            empty += 1
        total_len += len(a)
        a = '\n'.join(('＃' + line[1:] if line.startswith('#') else line) for line in a.splitlines())
        parts.append('### ' + t + '\n\n' + a + '\n')
    open(outpath, 'w', encoding='utf-8').write('\n'.join(parts))

    return {
        'file': os.path.basename(path),
        'pages': pages,
        'mode': mode,
        'level': chosen_level,
        'levels': levels,
        'questions': len(qas),
        'empty_answers': empty,
        'avg_answer_len': round(total_len / len(qas), 1) if qas else 0,
        'markers': marker_count,
        'skipped': skipped[:6],
        'samples': [t for t, _ in qas[:3]],
        'out': outpath,
    }


def main():
    src, outdir, report = sys.argv[1], sys.argv[2], sys.argv[3]
    results = []
    for name in sorted(os.listdir(src)):
        if name.lower().endswith('.pdf'):
            try:
                results.append(convert(os.path.join(src, name), outdir))
            except Exception as e:
                results.append({'file': name, 'error': str(e)})
    lines = ['# 面试题 PDF 转换报告', '', '| 文件 | 页数 | 模式 | 层级 | 题数 | 空答案 | 平均答案长度 | 样例 |', '|---|---|---|---|---|---|---|---|']
    for r in results:
        if 'error' in r:
            lines.append(f"| {r['file']} | - | 失败 | - | - | - | - | {r['error']} |")
        else:
            sample = ' / '.join(r['samples']).replace('|', '\\|')
            lines.append(f"| {r['file']} | {r['pages']} | {r['mode']} | {r['level']} | {r['questions']} | {r['empty_answers']} | {r['avg_answer_len']} | {sample} |")
    lines.append('')
    lines.append('## 明细')
    for r in results:
        if 'error' in r:
            continue
        lines.append(f"### {r['file']}")
        lines.append(f"- 模式：{r['mode']}；标题层级 {r['level']}；页数 {r['pages']}；题数 {r['questions']}；空答案 {r['empty_answers']}；平均答案长度 {r['avg_answer_len']}；候选人标记 {r['markers']}")
        if r['levels']:
            lines.append(f"- 可选标题字号层级：{r['levels']}")
        if r['skipped']:
            lines.append(f"- 跳过的章节：{'、'.join(r['skipped'])}")
        lines.append('')
    open(report, 'w', encoding='utf-8').write('\n'.join(lines))
    open(os.path.join(outdir, '转换摘要.json'), 'w', encoding='utf-8').write(json.dumps(results, ensure_ascii=False, indent=1))


main()
