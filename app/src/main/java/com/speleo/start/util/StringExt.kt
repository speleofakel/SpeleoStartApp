package com.speleo.start.util

import java.util.Locale

object StringExt {
    fun String.toTitleCase(): String {
        if (isEmpty()) return this
        return this[0].uppercase(Locale.getDefault()) + substring(1).lowercase(Locale.getDefault())
    }
}