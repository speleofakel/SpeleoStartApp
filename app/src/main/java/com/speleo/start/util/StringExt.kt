package com.speleo.start.util

import java.util.Locale

object StringExt {

    private val RUSSIAN_LOCALE = Locale("ru", "RU")

    /**
     * Преобразует строку в Title Case (каждое слово с заглавной буквы).
     * Поддерживает:
     * - Русский язык (ё, й корректно)
     * - Дефисные фамилии (Салтыков-Щедрин → Салтыков-Щедрин, но не Салтыков-Щедрин → Салтыков-Щедрин)
     * - Инициалы (Иванов И.И. → Иванов И.И., не меняет)
     */
    fun String.toTitleCase(): String {
        if (isEmpty()) return this

        val result = StringBuilder()
        var capitalizeNext = true

        for (i in indices) {
            val ch = this[i]

            when {
                // Буква
                ch.isLetter() -> {
                    if (capitalizeNext) {
                        result.append(ch.uppercase(RUSSIAN_LOCALE))
                        capitalizeNext = false
                    } else {
                        result.append(ch.lowercase(RUSSIAN_LOCALE))
                    }
                }
                // Дефис — следующая буква должна быть заглавной (но не весь суффикс)
                ch == '-' -> {
                    result.append(ch)
                    capitalizeNext = true
                }
                // Пробел, точка после инициала
                ch == ' ' || ch == '.' -> {
                    result.append(ch)
                    capitalizeNext = true
                }
                // Остальные символы
                else -> {
                    result.append(ch)
                    // Не сбрасываем capitalizeNext для цифр и спецсимволов
                }
            }
        }

        return result.toString()
    }

    /**
     * Обрезает пробелы по краям и применяет Title Case.
     * Используется перед сохранением в БД.
     */
    fun String.normalizeName(): String {
        return this.trim().toTitleCase()
    }
}