# Comprehensive Markdown Test

This document exercises the major features of mdless: headings, paragraphs, lists, blockquotes, fenced code, horizontal rules, links/images (alt), tables, unicode width, tabs, and blank lines.

## Headings

# H1 Title
## H2 Subtitle
### H3 Section

(Blank line follows)


Paragraph one. This paragraph should wrap cleanly to the terminal width and preserve spaces between words. It also includes inline `code`, *emphasis*, and **strong**.

Paragraph two with a URL-like text: [link text](https://example.com) and an image alt: ![logo](image.png)

---

## Lists

- Bullet one wraps across lines to test indent and hanging behavior when the terminal width is narrow.
  - Nested bullet two
  - Nested bullet three
- Bullet four

1. Ordered one
2. Ordered two
   1. Nested two point one

---

## Blockquote

> A blockquote paragraph should show a leading `> ` and wrap within the remaining width.
>
> It can span multiple lines and include inline `code` and *emphasis*.

---

## Fenced Code

```kotlin
fun main() {
    println("Hello, world!")
}
```

Tabs in code:

```
Col1\tCol2\tCol3
foo\tbar\tbaz
```

---

## Horizontal Rule

Below is an HR followed by text:

---

After HR.

---

## Table

| Column A           | Column B | Column C |
|:-------------------|:--------:|---------:|
| Alpha              |  Beta    |   Gamma  |
| Long text wrapping | centered |     42   |
| Café / Résumé   |   😀     |    文    |

---

## Unicode Width

CJK and emoji mixing: 文A字B混C合D 😀👍🏽 1️⃣ 2️⃣ 3️⃣

Long CJK: 這是一段很長的中文文字，用來測試自動換行功能，並確保對齊正確。

(Blank line below)


End.
