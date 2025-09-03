# Unicode Width Test — 多语言測試 — 😀👍🏽

This page mixes ASCII, CJK, emoji, and combining accents to exercise width handling.

## Paragraphs

- English and CJK mixed: Hello, 世界！歡迎使用 mdless。
- Combining accents: Café, Résumé, coöperate, ā, ō.
- Emoji: 😀 😁 😂 🤣 😜; with skin tones: 👍 👍🏻 👍🏽 👍🏿; keycaps: 1️⃣ 2️⃣ 3️⃣.
- Mixed: 文A字B混C合D — widths should align when wrapped.

---

## Tabs

Below lines contain tabs (→ shows intended columns):

```
Col1	Col2	Col3
alpha	beta	gamma
longlong	βeta	文
```

> Note: Tabs should expand consistently to the configured width.

---

## Lists

- Top-level bullet
  - 子項目 Alpha 😀
  - 子項目 Beta 文
- Another bullet with combining: Résumé item

1. Ordered 一
2. Ordered 二
   1. Nested 2.1 with 😀

---

## Blockquote

> 引用段落 with CJK and emoji 😀😀😀.
>
> Second line contains combining accents: Ré, é, å.

---

## Code Block

```
// Unicode identifiers (for display only)
fun greet(name: String) {
    println("Hello, 世界: $name 😀")
}
```

---

## Table

| 標題A | 標題B | 標題C |
|:------|:-----:|------:|
| 文字  | 😀    | 右對齊 |
| 長文本混合CJK和ASCII | Beta β | 👍🏽 |
| Café | coöperate | ā ō |

---

## Long CJK Line

這是一段很長的中文文字，用來測試自動換行功能。請確認換行後的寬度與終端的欄位對齊，不會溢出或錯位。😀😀😀

---

## Blank Lines

Above and below this text are intentional blank lines.


End of test.
