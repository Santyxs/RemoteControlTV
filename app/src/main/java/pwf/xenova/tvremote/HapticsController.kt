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
     * Dispara una vibración de feedback al pulsar un botón: un doble pulso rápido
     * a amplitud máxima (255), que se percibe más que un pulso único aunque dure
     * lo mismo en total — útil en teléfonos con motores hápticos suaves. No
     * depende de la intensidad configurada en el sistema.
     */
    fun vibrate() {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // timings: espera 0, vibra 40, pausa 30, vibra 40
            val timings = longArrayOf(0, 40, 30, 40)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 40, 30, 40), -1)
        }
    }

    companion object {
        private const val PREFS_NAME = "tvremote_prefs"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    }
}
