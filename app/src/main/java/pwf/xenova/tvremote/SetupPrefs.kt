package pwf.xenova.tvremote

import android.content.Context

/**
 * Guarda si el usuario ya completó el asistente de configuración del mando,
 * para no volver a mostrarlo cada vez que abre la app.
 */
object SetupPrefs {
    private const val PREFS_NAME = "tvremote_prefs"
    private const val KEY_SETUP_COMPLETE = "setup_complete"

    fun isSetupComplete(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SETUP_COMPLETE, false)

    fun setSetupComplete(context: Context, complete: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SETUP_COMPLETE, complete)
            .apply()
    }
}
