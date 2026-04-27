package com.speleo.start.util

object PhoneFormatter {

    fun formatAsYouType(input: String): String {
        val raw = input.filter { it.isDigit() }
        val digits = if (raw.startsWith("7") || raw.startsWith("8")) raw.drop(1) else raw
        val limited = digits.take(10)

        if (limited.isEmpty()) return ""

        val sb = StringBuilder("+7")
        if (limited.isNotEmpty()) sb.append(" (")

        for (i in limited.indices) {
            if (i == 3) sb.append(") ")
            if (i == 6 || i == 8) sb.append("-")
            sb.append(limited[i])
        }
        return sb.toString()
    }
}