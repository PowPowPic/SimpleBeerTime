package com.powder.simplebeertime.util

import com.powder.simplebeertime.data.preferences.AdTimeSlot
import java.util.Calendar

object AdTimeUtils {
    /**
     * 朝帯：06:00 ～ 17:59
     * 夜帯：18:00 ～ 翌05:59
     */
    fun getCurrentAdTimeSlot(): AdTimeSlot {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (hour in 6..17) {
            AdTimeSlot.MORNING
        } else {
            AdTimeSlot.EVENING
        }
    }
}
