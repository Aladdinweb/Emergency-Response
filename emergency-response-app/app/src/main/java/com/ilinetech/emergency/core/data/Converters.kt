package com.ilinetech.emergency.core.data

import androidx.room.TypeConverter
import com.ilinetech.emergency.core.data.entities.AlertDirection
import com.ilinetech.emergency.core.data.entities.AlertTransport

/**
 * © ILINE TECH BY FERAK ALADDIN
 * Stores enums as their name() string — keeps the schema human-readable
 * when inspecting the .db file directly (useful during field debugging).
 */
class Converters {

    @TypeConverter
    fun fromAlertDirection(value: AlertDirection): String = value.name

    @TypeConverter
    fun toAlertDirection(value: String): AlertDirection = AlertDirection.valueOf(value)

    @TypeConverter
    fun fromAlertTransport(value: AlertTransport): String = value.name

    @TypeConverter
    fun toAlertTransport(value: String): AlertTransport = AlertTransport.valueOf(value)
}
