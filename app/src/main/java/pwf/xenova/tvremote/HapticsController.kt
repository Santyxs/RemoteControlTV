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
     * Dispara una vibración corta y fuerte al pulsar un botón. En API 29+ usa el
     * efecto predefinido EFFECT_HEAVY_CLICK, calibrado por el fabricante para
     * sentirse fuerte (más perceptible que un pulso genérico). En versiones más
     * viejas cae a un pulso largo con amplitud máxima explícita (255), que no
     * depende de la intensidad háptica configurada en el sistema.
     */
    fun vibrate() {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                v.vibrate(VibrationEffect.createOneShot(70, 255))
            else -> {
                @Suppress("DEPRECATION")
                v.vibrate(70)
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "tvremote_prefs"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    }
}
