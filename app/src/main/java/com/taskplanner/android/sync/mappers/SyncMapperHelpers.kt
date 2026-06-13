package com.taskplanner.android.sync.mappers

import com.google.firebase.Timestamp

object SyncMapperHelpers {
    
    fun timestamp(epochMillis: Long?): Timestamp? {
        if (epochMillis == null) return null
        val seconds = epochMillis / 1000L
        val nanos = ((epochMillis % 1000L) * 1_000_000L).toInt()
        return Timestamp(seconds, nanos)
    }

    
    fun epochMillisFromAny(value: Any?): Long? {
        return when (value) {
            null -> null
            is Timestamp -> value.seconds * 1000L + (value.nanoseconds / 1_000_000L)
            is java.util.Date -> value.time
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            else -> null
        }
    }

    fun stringFromAny(value: Any?): String? = value as? String

    fun boolFromAny(value: Any?): Boolean? = value as? Boolean

    fun intFromAny(value: Any?): Int? {
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Short -> value.toInt()
            is Double -> value.toInt()
            is Number -> value.toInt()
            else -> null
        }
    }

    fun doubleFromAny(value: Any?): Double? {
        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Long -> value.toDouble()
            is Int -> value.toDouble()
            is Number -> value.toDouble()
            else -> null
        }
    }
}
