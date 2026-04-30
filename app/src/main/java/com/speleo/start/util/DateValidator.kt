package com.speleo.start.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateValidator {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun isValid(date: String): Boolean {
        if (date.length != 10) return false
        return try {
            dateFormat.isLenient = false
            dateFormat.parse(date)
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

    fun isRealDate(date: String): Boolean {
        if (date.length != 10) return false

        return try {
            val parts = date.split(".")
            if (parts.size != 3) return false

            val day = parts[0].toIntOrNull() ?: return false
            val month = parts[1].toIntOrNull() ?: return false
            val year = parts[2].toIntOrNull() ?: return false

            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            if (year < 1900 || year > currentYear) return false

            if (month !in 1..12) return false

            val maxDay = when (month) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11 -> 30
                2 -> {
                    val isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
                    if (isLeap) 29 else 28
                }
                else -> return false
            }
            day in 1..maxDay
        } catch (e: Exception) {
            false
        }
    }

    fun calculateAge(birthDateString: String?): Int? {
        if (birthDateString.isNullOrBlank()) return null
        if (birthDateString.length != 10) return null

        return try {
            dateFormat.isLenient = false
            val birthDate = dateFormat.parse(birthDateString) ?: return null

            val today = Calendar.getInstance()
            val birthCal = Calendar.getInstance().apply { time = birthDate }

            var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            if (age < 0) return null
            age
        } catch (e: Exception) {
            null
        }
    }

    fun canBeMentor(birthDateString: String?): Boolean {
        val age = calculateAge(birthDateString) ?: return false
        return age >= 18
    }

    fun getAgeColorMark(birthDateString: String?): AgeMark {
        val age = calculateAge(birthDateString) ?: return AgeMark.UNKNOWN
        return when {
            age < 14 -> AgeMark.UNDER_14
            age in 14..17 -> AgeMark.MINOR
            age in 18..70 -> AgeMark.ADULT
            else -> AgeMark.SENIOR
        }
    }

    enum class AgeMark(val label: String, val colorHex: String) {
        UNDER_14("<14", "#FFC107"),
        MINOR("14+", "#FFC107"),
        ADULT("18+", "#00A86B"),
        SENIOR("70+", "#FF8C00"),
        UNKNOWN("?", "#9E9E9E")
    }
}