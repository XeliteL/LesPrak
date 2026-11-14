// Сборка материалов этапа в DOCX по «Методическим указаниям к оформлению отчетов, КР и ВКР» (ГОСТ 7.32-2017)
const fs = require("fs")
const path = require("path")
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType, Table, TableRow, TableCell,
  WidthType, BorderStyle, ShadingType, LevelFormat, Footer, PageNumber, TableOfContents, SectionType, TabStopType,
  ImageRun, PageOrientation,
} = require("docx")

// ---------- настройки этапов ----------
// Запуск: node build.js [номер этапа] [путь к docx]
const STAGES = {
  2: {
    subtitle: "Материалы исследовательского этапа",
    files: ["00-environment.md", "01-domain.md", "02-history.md", "03-business-process.md", "translation.md"],
    // подписи таблиц в порядке появления
    captions: [
      "Состав инструментального окружения",
      "Структура репозитория практики",
      "Нормативные акты, регулирующие работу агентства недвижимости",
      "Ожидания клиентов от агента по недвижимости",
      "Участники предметной области",
      "Шаги бизнес-процесса и возможность их автоматизации",
      "Глоссарий терминов переведённого фрагмента",
    ],
    sources: "sources.md",
    onlyCited: false,
  },
  3: {
    subtitle: "Материалы аналитического этапа",
    files: ["01-bpmn.md", "02-automation.md", "03-data-model.md", "04-algorithms.md", "05-implementation.md"],
    captions: [
      "Элементы модели процесса в нотации BPMN 2.0",
      "Автоматизируемые операции бизнес-процесса",
      "Сущности логической модели данных",
      "Входные и выходные данные алгоритмов",
      "Оценка вычислительной сложности алгоритмов",
      "Время работы алгоритмов при росте объёма данных",
      "Модули процедурной реализации",
      "Модульные тесты",
    ],
    sources: "../stage2/sources.md", // общий список источников практики
    onlyCited: true,
  },
  4: {
    subtitle: "Материалы организационного и объектно-ориентированного этапа",
    files: ["01-oop-design.md", "02-java.md", "03-comparison.md", "04-team.md", "05-career.md"],
    captions: [
      "Пакеты объектно-ориентированной реализации",
      "Интерфейсы и их реализации",
      "Сопоставление процедурной и объектно-ориентированной реализаций",
      "Время работы реализаций на 1 000 000 записей",
      "Состав Scrum-команды проекта",
      "Требуемые компетенции участников команды",
      "Показатели рынка труда в сфере информационных технологий",
      "Образовательные программы, соответствующие тематике практики",
      "Траектория профессионального роста",
    ],
    sources: ["../stage2/sources.md", "sources.md"], // общий список и источники этапа 4
    onlyCited: true,
  },
  5: {
    subtitle: "Материалы проектного этапа и тестирования",
    files: ["01-physical-model.md", "02-testing.md", "03-storage.md", "04-solution.md"],
    captions: [
      "Выбор типов данных",
      "Основные тестовые сценарии",
      "Выявленные и устранённые ошибки",
      "Показатели качества кода",
      "Структура репозитория",
    ],
    sources: ["../stage2/sources.md", "../stage4/sources.md"],
    onlyCited: true,
  },
}
const STAGE = process.argv[2] || "2"
const CONF = STAGES[STAGE]
if (!CONF) { console.error(`нет настроек для этапа ${STAGE}`); process.exit(1) }
const SRC = path.resolve(__dirname, `../../docs/stage${STAGE}`)
const OUT = process.argv[3] || path.join(SRC, `Этап_${STAGE}.docx`)
const TITLE_TOPIC = "Риелторские услуги по продаже жилой недвижимости"

const FONT = "Times New Roman"
const PT14 = 28, PT12 = 24, PT16 = 32, PT18 = 36 // half-points
const MM = (mm) => Math.round((mm / 25.4) * 1440) // mm -> twips
const INDENT = MM(12.5) // 1,25 см
const LIST_TEXT = MM(22.5) // 2,25 см
const LINE15 = 360, LINE1 = 240
const PAGE_W = 11906, PAGE_H = 16838
const MARGIN = { top: MM(20), bottom: MM(20), left: MM(30), right: MM(10) }
const TEXT_W = PAGE_W - MARGIN.left - MARGIN.right

const FILES = CONF.files.map((f, i) => [f, i + 1])
const CAPTIONS = CONF.captions

// ---------- источники ----------
const sources = {}
const srcOrder = []
for (const l of [CONF.sources].flat().flatMap((f) => fs.readFileSync(path.join(SRC, f), "utf8").split("\n"))) {
  const m = l.match(/^(\d+)\.\s+(.*)$/)
  if (m) {
    sources[m[1]] = m[2].replace(/\s*\*\*\[проверить[^\]]*\]\*\*/g, "").trim()
    srcOrder.push(m[1])
  }
}
const translationSrc = srcOrder.find((n) => /NAR Settlement/.test(sources[n]))

// ---------- разбор markdown ----------
function prepare(file, num) {
  const lines = fs.readFileSync(path.join(SRC, file), "utf8").split("\n")
    .filter((l) => !l.startsWith(">") && l.trim() !== "---")
  let sub = 0, subsub = 0
  return lines.map((l) => {
    let m
    if ((m = l.match(/^# (?:\d+\.\s*)?(.*)$/))) return { h: 1, num, text: `${num} ${m[1]}` }
    if ((m = l.match(/^## (?:\d+\.\d+\.\s*)?(.*)$/))) { sub++; subsub = 0; return { h: 2, text: `${num}.${sub} ${m[1]}` } }
    if ((m = l.match(/^### (.*)$/))) { subsub++; return { h: 3, text: `${num}.${sub}.${subsub} ${m[1]}` } }
    if (file === "translation.md" && l.startsWith("**Источник:**") && translationSrc) return `${l} [${translationSrc}]`
    return l
  })
}
const sections = FILES.map(([f, n]) => prepare(f, n))

// перенумерация ссылок по порядку первого упоминания
const citeRe = /\[(\d+(?:\s*[;,]\s*\d+)*)\]/g // [1], [1; 2], [1, 2]
const map = {}
let next = 1
for (const sec of sections) for (const l of sec) if (typeof l === "string")
  for (const m of l.matchAll(citeRe)) for (const n of m[1].split(/[;,]/).map((s) => s.trim()))
    if (sources[n] && !map[n]) map[n] = next++
const uncited = srcOrder.filter((n) => !map[n])
if (!CONF.onlyCited) for (const n of uncited) map[n] = next++
const remap = (s) => s.replace(citeRe, (all, g) => {
  const ns = g.split(/[;,]/).map((x) => x.trim())
  if (!ns.every((n) => sources[n])) return all
  return "[" + ns.map((n) => map[n]).sort((a, b) => a - b).join(", ") + "]"
})
// типографика основного текста: диапазоны чисел через дефис
const typo = (s) => s.replace(/(\d)\s?–\s?(\d)/g, "$1-$2")

// ---------- инлайн-разметка ----------
function runs(text, o = {}) {
  const size = o.size || PT14
  const res = []
  const re = /(\*\*[^*]+\*\*|`[^`]+`|\[[^\]]+\]\((https?:[^)]+)\))/g
  let last = 0, m
  const push = (t, x = {}) => t && res.push(new TextRun({ text: t, font: FONT, size, ...(o.run || {}), ...x }))
  while ((m = re.exec(text))) {
    push(text.slice(last, m.index))
    const tok = m[0]
    if (tok.startsWith("**")) push(tok.slice(2, -2), { bold: true })
    else if (tok.startsWith("`")) push(tok.slice(1, -1), { font: "Courier New", size: size - 2 })
    else { const mm = tok.match(/^\[([^\]]+)\]\(([^)]+)\)$/); push(`${mm[1]} (${mm[2]})`) }
    last = m.index + tok.length
  }
  push(text.slice(last))
  return res
}

let firstH1 = true
let pendingBefore = 0 // «интервал перед 6 мм» для строки после таблицы
const para = (text, o = {}) => {
  const before = pendingBefore; pendingBefore = 0
  return new Paragraph({
    alignment: o.align || AlignmentType.JUSTIFIED,
    indent: o.numbering ? { left: LIST_TEXT, hanging: LIST_TEXT - INDENT } : o.noIndent ? undefined : { firstLine: INDENT },
    spacing: { line: LINE15, before, after: 0 },
    widowControl: true,
    numbering: o.numbering,
    children: runs(text, o),
  })
}

// маркированный список: со строчной буквы, «;» в конце, последний пункт — «.»
const PROPER = /^(\*\*)?(Росреестр|Росфинмониторинг|Минцифры|Госдум|ЦИАН|Циан|Домклик|Авито|Яндекс|Этажи|Сбер|РГР|NAR|MLS)/
function bulletText(t, last) {
  if (/^(\*\*)?[A-Za-z]/.test(t)) return t // английский текст оставляем как в оригинале
  t = t.replace(/[.;,]\s*$/, "")
  if (!PROPER.test(t)) t = t.replace(/^(\*\*)?([А-ЯЁ])(?=[а-яё])/, (x) => x.toLowerCase())
  return t + (last ? "." : ";")
}

const border = { style: BorderStyle.SINGLE, size: 4, color: "000000" }
const borders = { top: border, bottom: border, left: border, right: border }

function tableBlock(rows, caption) {
  const cells = rows.map((r) => r.replace(/^\||\|$/g, "").split("|").map((c) => c.trim()))
  const head = cells[0], data = cells.slice(2), n = head.length
  const lens = head.map((_, i) => Math.max(...cells.filter((_, k) => k !== 1).map((r) => Math.min((r[i] || "").length, 60)), 4))
  const tot = lens.reduce((a, b) => a + b, 0)
  const widths = lens.map((l) => Math.max(1000, Math.round((l / tot) * TEXT_W)))
  widths[widths.indexOf(Math.max(...widths))] += TEXT_W - widths.reduce((a, b) => a + b, 0)
  const row = (r, header) => new TableRow({
    tableHeader: header, cantSplit: true,
    children: r.map((c, i) => new TableCell({
      width: { size: widths[i], type: WidthType.DXA }, borders,
      margins: { top: 40, bottom: 40, left: 80, right: 80 },
      children: [new Paragraph({
        spacing: { line: LINE1, before: 0, after: 0 },
        alignment: header ? AlignmentType.CENTER : AlignmentType.LEFT,
        children: runs(typo(remap(c)), { size: PT12, run: header ? { bold: true } : {} }),
      })],
    })),
  })
  pendingBefore = MM(6)
  return [
    new Paragraph({
      spacing: { line: LINE1, before: MM(6), after: 0 }, keepNext: true, widowControl: true,
      children: [new TextRun({ text: caption, font: FONT, size: PT12, italics: true })],
    }),
    new Table({ width: { size: TEXT_W, type: WidthType.DXA }, columnWidths: widths, rows: [row(head, true), ...data.map((r) => row(r, false))] }),
  ]
}

const heading = (text, level, o = {}) => {
  const conf = {
    1: { size: PT18, before: 0, caps: true },
    2: { size: PT16, before: MM(15) },
    3: { size: PT14, before: MM(15) },
  }[level]
  pendingBefore = 0
  return new Paragraph({
    heading: [HeadingLevel.HEADING_1, HeadingLevel.HEADING_2, HeadingLevel.HEADING_3][level - 1],
    alignment: o.center ? AlignmentType.CENTER : AlignmentType.LEFT,
    indent: o.center ? undefined : { left: INDENT },
    spacing: { line: LINE15, before: conf.before, after: MM(10) },
    keepNext: true, keepLines: true, pageBreakBefore: level === 1 && !(firstH1 && !(firstH1 = false)),
    children: [new TextRun({ text: typo(conf.caps ? text.toUpperCase() : text), font: FONT, size: conf.size, bold: true, color: "000000" })],
  })
}

let tableIdx = 0
function render(sec) {
  const out = []
  let secNum = 0, tabNo = 0, figNo = 0, listInst = 0
  for (let i = 0; i < sec.length;) {
    const l = sec[i]
    if (typeof l === "object") {
      if (l.h === 1) { secNum = l.num; tabNo = 0; figNo = 0 }
      out.push(heading(l.text, l.h)); i++; continue
    }
    if (l.trim() === "") { i++; continue }
    if (l.startsWith("```")) {
      i++
      while (i < sec.length && !(typeof sec[i] === "string" && sec[i].startsWith("```"))) {
        const t = typeof sec[i] === "string" ? sec[i] : ""
        out.push(new Paragraph({ spacing: { line: LINE1, before: 0, after: 0 }, keepLines: true,
          children: [new TextRun({ text: t || " ", font: "Courier New", size: PT12 - 4 })] }))
        i++
      }
      i++; pendingBefore = MM(6); continue
    }
    if (l.startsWith("|")) {
      const rows = []
      while (i < sec.length && typeof sec[i] === "string" && sec[i].startsWith("|")) rows.push(sec[i++])
      tabNo++
      out.push(...tableBlock(rows, `Таблица ${secNum}.${tabNo} — ${CAPTIONS[tableIdx++] || ""}`))
      continue
    }
    let m
    if ((m = l.match(/^!\[(.*)\]\(([^)]+)\)(\{landscape\})?\s*$/))) {
      figNo++
      pendingBefore = 0
      const landscape = !!m[3]
      const file = path.resolve(SRC, m[2])
      const fig = []
      if (fs.existsSync(file)) {
        const buf = fs.readFileSync(file)
        const [w, h] = [buf.readUInt32BE(16), buf.readUInt32BE(20)] // заголовок IHDR файла PNG
        // доступная область, мм: книжная 170 × 225, альбомная 257 × 150 (с запасом на подпись)
        const [maxW, maxH] = landscape ? [257, 150] : [170, 225]
        const k = Math.min(maxW / w, maxH / h)
        const px = (mm) => Math.round((mm / 25.4) * 96)
        fig.push(new Paragraph({
          alignment: AlignmentType.CENTER, spacing: { before: MM(6), line: LINE1 }, keepNext: true,
          children: [new ImageRun({ type: "png", data: buf, transformation: { width: px(w * k), height: px(h * k) } })],
        }))
      } else {
        fig.push(new Paragraph({
          alignment: AlignmentType.CENTER, spacing: { before: MM(6), line: LINE1 }, keepNext: true,
          border: { top: { style: BorderStyle.DASHED, size: 6, color: "999999", space: 40 }, bottom: { style: BorderStyle.DASHED, size: 6, color: "999999", space: 40 } },
          children: [new TextRun({ text: `[вставить рисунок: ${m[2]}]`, font: FONT, size: PT14, italics: true, color: "808080" })],
        }))
      }
      fig.push(new Paragraph({
        alignment: AlignmentType.CENTER, spacing: { line: LINE1, before: MM(3), after: MM(6) },
        children: [new TextRun({ text: `Рисунок ${secNum}.${figNo} — ${m[1]}`, font: FONT, size: PT12, bold: true })],
      }))
      if (landscape) out.push({ landscape: fig })
      else out.push(...fig)
      i++; continue
    }
    if ((m = l.match(/^\*\*\[Рисунок \d+ — (.*)\]\*\*$/))) {
      figNo++
      pendingBefore = 0
      out.push(new Paragraph({
        alignment: AlignmentType.CENTER, spacing: { before: MM(6), line: LINE1 }, keepNext: true,
        border: { top: { style: BorderStyle.DASHED, size: 6, color: "999999", space: 40 }, bottom: { style: BorderStyle.DASHED, size: 6, color: "999999", space: 40 } },
        children: [new TextRun({ text: "[вставить скриншот]", font: FONT, size: PT14, italics: true, color: "808080" })],
      }))
      out.push(new Paragraph({
        alignment: AlignmentType.CENTER, spacing: { line: LINE1, before: MM(3), after: MM(6) },
        children: [new TextRun({ text: `Рисунок ${secNum}.${figNo} — ${m[1]}`, font: FONT, size: PT12, bold: true })],
      }))
      i++; continue
    }
    if (/^- /.test(l) || /^\d+\. /.test(l)) {
      const ordered = /^\d+\. /.test(l)
      const items = []
      while (i < sec.length && typeof sec[i] === "string" && (ordered ? /^\d+\. /.test(sec[i]) : /^- /.test(sec[i]))) items.push(sec[i++].replace(/^(- |\d+\. )/, ""))
      listInst++
      items.forEach((t, k) => {
        const txt = ordered ? t : bulletText(t, k === items.length - 1)
        out.push(para(typo(remap(txt)), { numbering: { reference: ordered ? "num" : "dash", level: 0, instance: listInst } }))
      })
      continue
    }
    out.push(para(typo(remap(l))))
    i++
  }
  return out
}

// ---------- титул, содержание, список источников ----------
const center = (t, o = {}) => new Paragraph({
  alignment: AlignmentType.CENTER, spacing: { line: LINE15, ...(o.spacing || {}) },
  children: [new TextRun({ text: t, font: FONT, size: PT14, bold: o.bold })],
})
const right = (t, o = {}) => new Paragraph({
  spacing: { line: LINE15, ...(o.spacing || {}) }, indent: { left: MM(95) },
  children: [new TextRun({ text: t, font: FONT, size: PT14 })],
})
const titlePage = [
  center("[Титульный лист по утверждённой форме кафедры]", { spacing: { after: MM(40) } }),
  center("ОТЧЁТ ПО УЧЕБНОЙ ПРАКТИКЕ", { bold: true }),
  center(CONF.subtitle, { bold: true }),
  center(`Тема: «${TITLE_TOPIC}»`, { spacing: { before: MM(8) } }),
  right("Студент: ____________________", { spacing: { before: MM(50) } }),
  right("Группа: ____________________"),
  right("Руководитель: _______________"),
  center("Москва 2026", { spacing: { before: MM(50) } }),
]
const toc = [
  new Paragraph({
    pageBreakBefore: true, alignment: AlignmentType.CENTER, spacing: { line: LINE15, after: MM(10) },
    children: [new TextRun({ text: "СОДЕРЖАНИЕ", font: FONT, size: PT18, bold: true })],
  }),
  new TableOfContents("Содержание", { hyperlink: true, headingStyleRange: "1-3" }),
]
const inv = Object.fromEntries(Object.entries(map).map(([o, n]) => [n, o]))
const bib = [
  heading("СПИСОК ИСПОЛЬЗОВАННЫХ ИСТОЧНИКОВ", 1, { center: true }),
  ...Array.from({ length: next - 1 }, (_, k) => k + 1).map((n) => new Paragraph({
    alignment: AlignmentType.JUSTIFIED, spacing: { line: LINE15 }, widowControl: true,
    numbering: { reference: "bib", level: 0 },
    children: runs(sources[inv[n]]),
  })),
]

const listLevel = (format, text) => [{
  level: 0, format, text, alignment: AlignmentType.LEFT,
  style: {
    paragraph: { indent: { left: LIST_TEXT, hanging: LIST_TEXT - INDENT }, tabStops: [{ type: "left", position: LIST_TEXT }] },
    run: { font: FONT, size: PT14 },
  },
}]

const pageProps = { page: { size: { width: PAGE_W, height: PAGE_H }, margin: MARGIN } }
const numFooter = new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER,
  children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: PT12 })] })] })
const emptyFooter = new Footer({ children: [new Paragraph({ children: [] })] })

// основной текст; альбомные рисунки выносятся в отдельные разделы документа
function bodySections() {
  const items = [...sections.flatMap(render), ...(next > 1 ? bib : [])]
  const res = []
  let cur = []
  const flush = () => { if (cur.length) res.push({ properties: { ...pageProps, type: SectionType.NEXT_PAGE }, footers: { default: numFooter }, children: cur }); cur = [] }
  for (const it of items) {
    if (it && it.landscape) {
      flush()
      res.push({
        properties: { type: SectionType.NEXT_PAGE, page: { size: { width: PAGE_W, height: PAGE_H, orientation: PageOrientation.LANDSCAPE }, margin: MARGIN } },
        footers: { default: numFooter }, children: it.landscape,
      })
    } else cur.push(it)
  }
  flush()
  return res
}

const doc = new Document({
  features: { updateFields: true },
  styles: {
    default: { document: { run: { font: FONT, size: PT14 }, paragraph: { spacing: { line: LINE15 } } } },
    paragraphStyles: [
      // строки содержания: без полужирного, по левому краю, с отточием перед номером страницы
      ...[1, 2, 3].map((n) => ({
        id: `TOC${n}`, name: `toc ${n}`, basedOn: "Normal", next: "Normal",
        run: { font: FONT, size: PT14, bold: false },
        paragraph: { spacing: { line: LINE15, before: 0, after: 0 }, indent: { left: 0, firstLine: 0 },
          tabStops: [{ type: TabStopType.RIGHT, position: TEXT_W, leader: "dot" }] },
      })),
      ...[1, 2, 3].map((n) => ({
      id: `Heading${n}`, name: `Heading ${n}`, basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { font: FONT, size: [PT18, PT16, PT14][n - 1], bold: true, color: "000000" },
      paragraph: { outlineLevel: n - 1, keepNext: true },
    })),
    ],
  },
  numbering: {
    config: [
      { reference: "dash", levels: listLevel(LevelFormat.BULLET, "–") },
      { reference: "num", levels: listLevel(LevelFormat.DECIMAL, "%1.") },
      // список источников: номер с абзацного отступа 1,25 см, перенос строки — от левого поля (образец Д.1)
      { reference: "bib", levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 0, firstLine: INDENT }, tabStops: [{ type: "left", position: LIST_TEXT }] }, run: { font: FONT, size: PT14 } } }] },
    ],
  },
  sections: [
    // титул и содержание — в общей нумерации, но без номера страницы
    { properties: pageProps, footers: { default: emptyFooter }, children: [...titlePage, ...toc] },
    ...bodySections(),
  ],
})

Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(OUT, buf)
  console.log(`written ${OUT}: tables ${tableIdx}/${CAPTIONS.length}, sources ${next - 1}`)
  if (uncited.length) console.log("uncited sources:", uncited.join(", "))
})
