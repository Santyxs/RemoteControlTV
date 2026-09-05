package pwf.xenova.tvremote

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

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

    /**
     * Dispara una vibración corta y fuerte al pulsar un botón. Usa amplitud máxima
     * explícita (255) en vez de DEFAULT_AMPLITUDE, para que no dependa de la
     * intensidad háptica configurada en el sistema del teléfono.
     */
    fun vibrate() {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(40, 255))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(40)
        }
    }

    companion object {
        private const val PREFS_NAME = "tvremote_prefs"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    }
}
