package com.aramasaati.app

import java.util.Calendar
import java.util.Locale

/** Gün içindeki dakika cinsinden saat aralığı (0..1439). Gece yarısını geçebilir (22:00–07:00). */
data class TimeWindow(val start: Int, val end: Int) {

    fun contains(minute: Int): Boolean = when {
        start == end -> true                       // başlangıç = bitiş → bütün gün
        start < end -> minute >= start && minute < end
        else -> minute >= start || minute < end    // gece yarısını geçen aralık
    }

    fun label(): String = "${fmt(start)} – ${fmt(end)}"

    companion object {
        fun fmt(m: Int): String = String.format(Locale.US, "%02d:%02d", m / 60, m % 60)
    }
}

enum class RuleMode {
    /** Seçili günlerde SADECE bu saatlerde arayabilir, kalan her zaman engellenir. */
    ALLOW_ONLY,

    /** Seçili günlerde BU saatlerde arayamaz (ör. öğle arası), kalan zamanlarda arayabilir. */
    BLOCK_DURING
}

data class Rule(
    val id: String,
    val name: String,
    val numbers: List<String>,
    val mode: RuleMode,
    val windows: List<TimeWindow>,
    val days: Set<Int>,          // Calendar.DAY_OF_WEEK değerleri (1=Pazar ... 7=Cumartesi)
    val enabled: Boolean
) {

    fun matches(number: String): Boolean {
        val n = PhoneUtil.normalize(number)
        if (n.isEmpty()) return false
        return numbers.any { PhoneUtil.normalize(it) == n }
    }

    fun isBlockedAt(cal: Calendar): Boolean {
        if (!enabled) return false
        val day = cal.get(Calendar.DAY_OF_WEEK)
        val minute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val inWindow = day in days && windows.any { it.contains(minute) }
        return when (mode) {
            RuleMode.ALLOW_ONLY -> !inWindow
            RuleMode.BLOCK_DURING -> inWindow
        }
    }

    fun describe(): String {
        val w = windows.joinToString(", ") { it.label() }
        val d = Days.label(days)
        return when (mode) {
            RuleMode.ALLOW_ONLY -> "Sadece $w arayabilir · $d"
            RuleMode.BLOCK_DURING -> "$w arası arayamaz · $d"
        }
    }
}

object Days {
    /** Pazartesiden başlayan sıra */
    val ORDER = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )
    private val NAMES = mapOf(
        Calendar.MONDAY to "Pzt", Calendar.TUESDAY to "Sal", Calendar.WEDNESDAY to "Çar",
        Calendar.THURSDAY to "Per", Calendar.FRIDAY to "Cum", Calendar.SATURDAY to "Cmt",
        Calendar.SUNDAY to "Paz"
    )
    private val WEEKDAYS = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
    )
    private val WEEKEND = setOf(Calendar.SATURDAY, Calendar.SUNDAY)

    fun name(day: Int): String = NAMES[day] ?: "?"

    fun label(days: Set<Int>): String = when {
        days.size == 7 -> "Her gün"
        days == WEEKDAYS -> "Hafta içi"
        days == WEEKEND -> "Hafta sonu"
        days.isEmpty() -> "Gün seçilmedi"
        else -> ORDER.filter { it in days }.joinToString(" ") { name(it) }
    }
}

object PhoneUtil {
    /** +90 532 ..., 0532 ..., 532 ... hepsini aynı kabul etmek için son 10 haneyi alır. */
    fun normalize(s: String): String {
        val digits = s.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }
}
