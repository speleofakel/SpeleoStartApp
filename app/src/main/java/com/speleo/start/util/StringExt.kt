package com.speleo.start.util

import java.util.Locale // ← добавь этот импорт

object StringExt {
    /**
     * Первая буква заглавная, остальные строчные.
     * "иванов" → "Иванов", "ИВАНОВ" → "Иванов"
     */
    fun String.toTitleCase(): String {
        if (isEmpty()) return this
        return this[0].uppercase(Locale.ROOT) + substring(1).lowercase(Locale.ROOT)
    }
}