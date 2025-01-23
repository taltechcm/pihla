package ee.taltech.aireapplication.helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.zeugmasolutions.localehelper.currentLocale
import java.util.Locale

object SettingsRepository {
    fun setLangString(context: Context, key: String, value: String) {
        val appSharedPrefs: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val prefsEditor: SharedPreferences.Editor = appSharedPrefs.edit()
        val finalKey = getFinalLocaleKey(context.currentLocale, key)
        prefsEditor.putString(finalKey, value)
        prefsEditor.apply()
    }

    fun getLangString(context: Context, key: String, defaultValue: String): String {
        val appSharedPrefs: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val finalKey = getFinalLocaleKey(context.currentLocale, key)
        val res = appSharedPrefs.getString(finalKey, defaultValue)
        return res ?: defaultValue
    }


    fun setString(context: Context, key: String, value: String) {
        val appSharedPrefs: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val prefsEditor: SharedPreferences.Editor = appSharedPrefs.edit()
        prefsEditor.putString(key, value)
        prefsEditor.apply()
    }

    fun getString(context: Context, key: String, defaultValue: String): String {
        val appSharedPrefs: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val res = appSharedPrefs.getString(key, defaultValue)
        return res ?: defaultValue
    }


    private fun getFinalLocaleKey(lang: Locale, key: String) = key + "_" + lang.language

    fun setBoolean(context: Context, key: String, value: Boolean) {
        val appSharedPrefs: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val prefsEditor: SharedPreferences.Editor = appSharedPrefs.edit()

        prefsEditor.putBoolean(key, value)
        prefsEditor.apply()
    }


    fun setInt(context: Context, key: String, value: Int) {
        val appSharedPrefs: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val prefsEditor: SharedPreferences.Editor = appSharedPrefs.edit()
        prefsEditor.putInt(key, value)
        prefsEditor.apply()
    }


    fun getBoolean(context: Context, key: String, defaultValue: Boolean): Boolean {
        val appSharedPrefs: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val res = appSharedPrefs.getBoolean(key, defaultValue)
        return res
    }

    fun getInt(context: Context, key: String, defaultValue: Int): Int {
        val appSharedPrefs: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val res = appSharedPrefs.getInt(key, defaultValue)
        return res
    }

}
