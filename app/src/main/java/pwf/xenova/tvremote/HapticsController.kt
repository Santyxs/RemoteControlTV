package pwf.xenova.tvremote

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast

/**
 * Controla la vibración al pulsar botones. El usuario puede activarla/desactivarla
 * desde Ajustes; la preferencia se guarda en SharedPreferences.
 */
class HapticsController(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()
        }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** Dispara una vibración corta de feedback si está habilitada y hay hardware. */
    fun vibrate() {
        if (!enabled) {
            Toast.makeText(appContext, "DEBUG: vibración desactivada en Ajustes", Toast.LENGTH_SHORT).show()
            return
        }
        val v = vibrator
        if (v == null) {
            Toast.makeText(appContext, "DEBUG: no se encontró servicio Vibrator", Toast.LENGTH_SHORT).show()
            return
        }
        if (!v.hasVibrator()) {
            Toast.makeText(appContext, "DEBUG: el dispositivo reporta que no tiene motor de vibración", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(35)
        }
        Toast.makeText(appContext, "DEBUG: vibrate() ejecutado", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val PREFS_NAME = "tvremote_prefs"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    }
}
