package dev.banic.wuhancoronavirusinfo

import android.content.Context
import android.content.SharedPreferences
import java.util.*

val Locale.flagEmoji: String
    get() {
        val firstLetter = Character.codePointAt(country, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(country, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    }

fun Context.getSharedPreferences(
    appWidgetId: Int
): SharedPreferences = this.getSharedPreferences(
    getSharedPreferencesName(appWidgetId),
    Context.MODE_PRIVATE
)

fun Context.getSharedPreferencesName(
    appWidgetId: Int
): String = "${packageName}-$appWidgetId"