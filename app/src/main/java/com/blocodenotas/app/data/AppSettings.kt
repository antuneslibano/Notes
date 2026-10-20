package com.blocodenotas.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest
import java.security.SecureRandom

class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var theme by mutableStateOf(enumOr(prefs.getString("theme", null), ThemeMode.SYSTEM))
        private set
    var sortField by mutableStateOf(enumOr(prefs.getString("sortField", null), SortField.UPDATED))
        private set
    var sortAscending by mutableStateOf(prefs.getBoolean("sortAscending", false))
        private set
    var filter by mutableStateOf(enumOr(prefs.getString("filter", null), NoteFilter.ALL))
        private set
    var pinEnabled by mutableStateOf(prefs.getString("pinHash", null) != null)
        private set
    var biometricEnabled by mutableStateOf(prefs.getBoolean("biometric", false))
        private set

    val pinLength: Int get() = prefs.getInt("pinLength", 4)

    fun updateTheme(mode: ThemeMode) {
        theme = mode
        prefs.edit().putString("theme", mode.name).apply()
    }

    fun updateSort(field: SortField, ascending: Boolean) {
        sortField = field
        sortAscending = ascending
        prefs.edit().putString("sortField", field.name).putBoolean("sortAscending", ascending).apply()
    }

    fun updateFilter(value: NoteFilter) {
        filter = value
        prefs.edit().putString("filter", value.name).apply()
    }

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
        prefs.edit()
            .putString("pinSalt", salt)
            .putString("pinHash", hash(salt, pin))
            .putInt("pinLength", pin.length)
            .apply()
        pinEnabled = true
    }

    fun clearPin() {
        prefs.edit().remove("pinSalt").remove("pinHash").remove("pinLength").putBoolean("biometric", false).apply()
        pinEnabled = false
        biometricEnabled = false
    }

    fun checkPin(pin: String): Boolean {
        val salt = prefs.getString("pinSalt", null) ?: return false
        val stored = prefs.getString("pinHash", null) ?: return false
        return MessageDigest.isEqual(stored.toByteArray(), hash(salt, pin).toByteArray())
    }

    fun updateBiometric(enabled: Boolean) {
        biometricEnabled = enabled
        prefs.edit().putBoolean("biometric", enabled).apply()
    }

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
