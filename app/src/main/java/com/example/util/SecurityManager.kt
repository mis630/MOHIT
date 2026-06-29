package com.example.util

import android.content.Context
import android.content.SharedPreferences

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("srt_billing_security", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN = "srt_app_pin"
        private const val KEY_LOCK_ENABLED = "srt_lock_enabled"
        private const val DEFAULT_PIN = "1234"
    }

    // Initialize with a default PIN initially so they always have protection initially, or allow set-up
    init {
        if (getPin() == null) {
            setPin(DEFAULT_PIN)
            setLockEnabled(true)
        }
    }

    fun getPin(): String? {
        return prefs.getString(KEY_PIN, DEFAULT_PIN)
    }

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun isLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCK_ENABLED, true)
    }

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    fun verifyPin(pinInput: String): Boolean {
        return getPin() == pinInput
    }
}
