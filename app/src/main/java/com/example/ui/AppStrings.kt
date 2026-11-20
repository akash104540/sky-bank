package com.example.ui

object AppStrings {
    fun formatBdt(amount: Long, bengali: Boolean): String {
        val formatted = formatIndianCommas(amount)
        return if (bengali) "৳${toBengaliDigits(formatted)}" else "৳$formatted"
    }

    fun toBengaliDigits(input: String): String {
        val map = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(map[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun formatIndianCommas(value: Long): String {
        val neg = value < 0
        val s = kotlin.math.abs(value).toString()
        if (s.length <= 3) return if (neg) "-$s" else s
        val lastThree = s.takeLast(3)
        val rest = s.dropLast(3)
        val sb = StringBuilder()
        var count = 0
        for (i in rest.length - 1 downTo 0) {
            sb.append(rest[i])
            count++
            if (count == 2 && i != 0) {
                sb.append(',')
                count = 0
            }
        }
        val prefix = sb.reverse().toString()
        return (if (neg) "-" else "") + "$prefix,$lastThree"
    }

    fun formatClock(minuteOfDay: Long, bengali: Boolean): String {
        val totalMin = (minuteOfDay % 1440).toInt()
        val hour24 = totalMin / 60
        val min = totalMin % 60
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val raw = "%02d:%02d %s".format(hour12, min, amPm)
        return if (bengali) toBengaliDigits(raw) else raw
    }
}
