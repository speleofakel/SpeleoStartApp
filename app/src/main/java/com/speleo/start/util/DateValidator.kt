package com.speleo.start.util

import java.text.SimpleDateFormat
import java.util.Locale

object DateValidator {

    fun isValid(date: String): Boolean {
        if (date.length != 10) return false
        return try {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(date)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun formatAsYouType(input: String): String {
        val digits = input.filter { it.isDigit() }.take(8)
        val sb = StringBuilder()
        for (i in digits.indices) {
            if (i == 2 || i == 4) sb.append(".")
            sb.append(digits[i])
        }
        return sb.toString()
    }
}