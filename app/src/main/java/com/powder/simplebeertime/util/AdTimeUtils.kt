package com.powder.simplebeertime.util

import com.powder.simplebeertime.data.preferences.AdTimeSlot
import java.util.Calendar

object AdTimeUtils {

    fun getCurrentAdTimeSlot(): AdTimeSlot {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..5 -> AdTimeSlot.SLOT_00_06
            in 6..11 -> AdTimeSlot.SLOT_06_12
            in 12..17 -> AdTimeSlot.SLOT_12_18
            else -> AdTimeSlot.SLOT_18_24
        }
    }
}
