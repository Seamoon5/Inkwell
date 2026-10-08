package com.seamoon5.inkwell.data

/**
 * Checklist items are stored in one TEXT column as one item per line so the
 * database stays simple and the whole checklist still exports as plain text.
 * Format per line: "1|Buy milk" (1 = done, 0 = not done). "| " is not allowed
 * inside item text, so "|" is sanitised on write.
 */
object Checklist {

    data class Item(val done: Boolean, val text: String)

    fun parse(raw: String): List<Item> = raw
        .split("\n")
        .filter { it.isNotBlank() }
        .map { line ->
            val idx = line.indexOf('|')
            if (idx <= 0) Item(false, line) else Item(line.substring(0, idx) == "1", line.substring(idx + 1))
        }

    fun serialize(items: List<Item>): String = items.joinToString("\n") {
        (if (it.done) "1" else "0") + "|" + it.text.replace("|", "/")
    }

    fun toggle(items: List<Item>, index: Int): List<Item> =
        items.mapIndexed { i, item -> if (i == index) item.copy(done = !item.done) else item }

    fun removeAt(items: List<Item>, index: Int): List<Item> =
        items.filterIndexed { i, _ -> i != index }

    fun add(items: List<Item>, text: String): List<Item> =
        if (text.isBlank()) items else items + Item(false, text.trim())

    fun doneCount(items: List<Item>) = items.count { it.done }

    /** Plain-text rendering used by the share/export action. */
    fun toText(items: List<Item>): String = items.joinToString("\n") {
        (if (it.done) "[x] " else "[ ] ") + it.text
    }
}