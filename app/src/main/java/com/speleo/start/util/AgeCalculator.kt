package com.speleo.start.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AgeCalculator {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    /**
     * Вычисляет точный возраст в годах с учётом дня и месяца рождения.
     * Возвращает null, если дата невалидна или не задана.
     */
    fun calculateAge(birthDateString: String?): Int? {
        if (birthDateString.isNullOrBlank() || birthDateString.length != 10) return null

        return try {
            dateFormat.isLenient = false
            val birthDate = dateFormat.parse(birthDateString) ?: return null

            val today = Calendar.getInstance()
            val birthCal = Calendar.getInstance().apply { time = birthDate }

            var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Проверяет, может ли человек быть ментором (≥18 лет).
     * Динамический расчёт — не зависит от захардкоженного флага.
     */
    fun canBeMentor(birthDateString: String?): Boolean {
        val age = calculateAge(birthDateString) ?: return false
        return age >= 18
    }

    /**
     * Возвращает цветовую метку возраста для UI.
     * @return Pair<метка_текст, цвет_hex>
     */
    fun getAgeColorMark(birthDateString: String?): AgeMark {
        val age = calculateAge(birthDateString) ?: return AgeMark.UNKNOWN

        return when {
            age < 14 -> AgeMark.UNDER_14
            age in 14..17 -> AgeMark.MINOR
            age in 18..70 -> AgeMark.ADULT
            age > 70 -> AgeMark.SENIOR
            else -> AgeMark.UNKNOWN
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