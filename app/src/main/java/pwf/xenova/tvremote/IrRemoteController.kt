package pwf.xenova.tvremote

import android.content.Context
import android.hardware.ConsumerIrManager

/**
 * Acciones soportadas por el mando universal.
 */
enum class RemoteAction {
    POWER, VOL_UP, VOL_DOWN, MUTE,
    CH_UP, CH_DOWN,
    UP, DOWN, LEFT, RIGHT, OK,
    MENU, EXIT, BACK, HOME, GUIDE, HDMI, INFO, TOOLS, SOURCE,
    NUM_0, NUM_1, NUM_2, NUM_3, NUM_4, NUM_5, NUM_6, NUM_7, NUM_8, NUM_9
}

/**
 * Marcas soportadas. Cada una define su propio set de códigos NEC.
 * Los códigos aquí son ejemplos genéricos de protocolo NEC amplio uso;
 * en un mando real conviene permitir "aprendizaje" o ampliar esta tabla
 * con los códigos exactos del modelo del usuario.
 */
enum class TvBrand(val displayName: String) {
    SAMSUNG("Samsung"),
    LG("LG"),
    SONY("Sony"),
    GENERIC_NEC("Genérico (NEC)")
}

/**
 * Envuelve ConsumerIrManager y traduce RemoteAction -> pulsos IR (protocolo NEC).
 */
class IrRemoteController(context: Context) {

    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    /** true si el hardware existe y está disponible en este teléfono */
    val isIrAvailable: Boolean
        get() = irManager?.hasIrEmitter() == true

    var brand: TvBrand = TvBrand.SAMSUNG

    /** Frecuencia portadora típica NEC en Hz */
    private val carrierFrequency = 38000

    /**
     * Códigos NEC (address, command) por marca y acción.
     * Address/command genéricos; sustituir por los del mando real capturado
     * con una app "IR learner" si el control remoto original está disponible.
     */
    private val codeTable: Map<TvBrand, Map<RemoteAction, Pair<Int, Int>>> = mapOf(
        TvBrand.SAMSUNG to mapOf(
            RemoteAction.POWER to (0x07 to 0x02),
            RemoteAction.VOL_UP to (0x07 to 0x07),
            RemoteAction.VOL_DOWN to (0x07 to 0x0B),
            RemoteAction.MUTE to (0x07 to 0x0F),
            RemoteAction.CH_UP to (0x07 to 0x12),
            RemoteAction.CH_DOWN to (0x07 to 0x10),
            RemoteAction.UP to (0x07 to 0x60),
            RemoteAction.DOWN to (0x07 to 0x61),
            RemoteAction.LEFT to (0x07 to 0x65),
            RemoteAction.RIGHT to (0x07 to 0x62),
            RemoteAction.OK to (0x07 to 0x68),
            RemoteAction.MENU to (0x07 to 0x1A),
            RemoteAction.EXIT to (0x07 to 0x2D),
            RemoteAction.BACK to (0x07 to 0x58),
            RemoteAction.HOME to (0x07 to 0x79),
            RemoteAction.GUIDE to (0x07 to 0x4F),
            RemoteAction.HDMI to (0x07 to 0x22),
            RemoteAction.INFO to (0x07 to 0x1F),
            RemoteAction.TOOLS to (0x07 to 0x4B),
            RemoteAction.SOURCE to (0x07 to 0x22),
            RemoteAction.NUM_1 to (0x07 to 0x04),
            RemoteAction.NUM_2 to (0x07 to 0x05),
            RemoteAction.NUM_3 to (0x07 to 0x06),
            RemoteAction.NUM_4 to (0x07 to 0x08),
            RemoteAction.NUM_5 to (0x07 to 0x09),
            RemoteAction.NUM_6 to (0x07 to 0x0A),
            RemoteAction.NUM_7 to (0x07 to 0x0C),
            RemoteAction.NUM_8 to (0x07 to 0x0D),
            RemoteAction.NUM_9 to (0x07 to 0x0E),
            RemoteAction.NUM_0 to (0x07 to 0x11)
        ),
        TvBrand.LG to mapOf(
            RemoteAction.POWER to (0x04 to 0x08),
            RemoteAction.VOL_UP to (0x04 to 0x02),
            RemoteAction.VOL_DOWN to (0x04 to 0x03),
            RemoteAction.MUTE to (0x04 to 0x09),
            RemoteAction.CH_UP to (0x04 to 0x00),
            RemoteAction.CH_DOWN to (0x04 to 0x01),
            RemoteAction.UP to (0x04 to 0x40),
            RemoteAction.DOWN to (0x04 to 0x41),
            RemoteAction.LEFT to (0x04 to 0x42),
            RemoteAction.RIGHT to (0x04 to 0x43),
            RemoteAction.OK to (0x04 to 0x44),
            RemoteAction.MENU to (0x04 to 0x43),
            RemoteAction.EXIT to (0x04 to 0x5A),
            RemoteAction.BACK to (0x04 to 0x28),
            RemoteAction.HOME to (0x04 to 0x7A),
            RemoteAction.GUIDE to (0x04 to 0xAA),
            RemoteAction.HDMI to (0x04 to 0x0B),
            RemoteAction.INFO to (0x04 to 0xAA),
            RemoteAction.TOOLS to (0x04 to 0xF0),
            RemoteAction.SOURCE to (0x04 to 0x0B),
            RemoteAction.NUM_1 to (0x04 to 0x10),
            RemoteAction.NUM_2 to (0x04 to 0x11),
            RemoteAction.NUM_3 to (0x04 to 0x12),
            RemoteAction.NUM_4 to (0x04 to 0x13),
            RemoteAction.NUM_5 to (0x04 to 0x14),
            RemoteAction.NUM_6 to (0x04 to 0x15),
            RemoteAction.NUM_7 to (0x04 to 0x16),
            RemoteAction.NUM_8 to (0x04 to 0x17),
            RemoteAction.NUM_9 to (0x04 to 0x18),
            RemoteAction.NUM_0 to (0x04 to 0x19)
        ),
        TvBrand.SONY to mapOf(
            RemoteAction.POWER to (0x01 to 0x15),
            RemoteAction.VOL_UP to (0x01 to 0x12),
            RemoteAction.VOL_DOWN to (0x01 to 0x13),
            RemoteAction.MUTE to (0x01 to 0x14),
            RemoteAction.CH_UP to (0x01 to 0x10),
            RemoteAction.CH_DOWN to (0x01 to 0x11),
            RemoteAction.UP to (0x01 to 0x74),
            RemoteAction.DOWN to (0x01 to 0x75),
            RemoteAction.LEFT to (0x01 to 0x34),
            RemoteAction.RIGHT to (0x01 to 0x33),
            RemoteAction.OK to (0x01 to 0x65),
            RemoteAction.MENU to (0x01 to 0x3B),
            RemoteAction.EXIT to (0x01 to 0x51),
            RemoteAction.BACK to (0x01 to 0x50),
            RemoteAction.HOME to (0x01 to 0x60),
            RemoteAction.GUIDE to (0x01 to 0x89),
            RemoteAction.HDMI to (0x01 to 0xA5),
            RemoteAction.INFO to (0x01 to 0x25),
            RemoteAction.TOOLS to (0x01 to 0x81),
            RemoteAction.SOURCE to (0x01 to 0xA5),
            RemoteAction.NUM_1 to (0x01 to 0x00),
            RemoteAction.NUM_2 to (0x01 to 0x01),
            RemoteAction.NUM_3 to (0x01 to 0x02),
            RemoteAction.NUM_4 to (0x01 to 0x03),
            RemoteAction.NUM_5 to (0x01 to 0x04),
            RemoteAction.NUM_6 to (0x01 to 0x05),
            RemoteAction.NUM_7 to (0x01 to 0x06),
            RemoteAction.NUM_8 to (0x01 to 0x07),
            RemoteAction.NUM_9 to (0x01 to 0x08),
            RemoteAction.NUM_0 to (0x01 to 0x09)
        ),
        TvBrand.GENERIC_NEC to mapOf(
            RemoteAction.POWER to (0x00 to 0x0C),
            RemoteAction.VOL_UP to (0x00 to 0x10),
            RemoteAction.VOL_DOWN to (0x00 to 0x11),
            RemoteAction.MUTE to (0x00 to 0x0D),
            RemoteAction.CH_UP to (0x00 to 0x20),
            RemoteAction.CH_DOWN to (0x00 to 0x21),
            RemoteAction.NUM_1 to (0x00 to 0x01),
            RemoteAction.NUM_2 to (0x00 to 0x02),
            RemoteAction.NUM_3 to (0x00 to 0x03),
            RemoteAction.NUM_4 to (0x00 to 0x04),
            RemoteAction.NUM_5 to (0x00 to 0x05),
            RemoteAction.NUM_6 to (0x00 to 0x06),
            RemoteAction.NUM_7 to (0x00 to 0x07),
            RemoteAction.NUM_8 to (0x00 to 0x08),
            RemoteAction.NUM_9 to (0x00 to 0x09),
            RemoteAction.NUM_0 to (0x00 to 0x0A)
        )
    )

    /**
     * Envía una acción por IR. Devuelve false si no hay hardware o no existe
     * código para esa acción en la marca seleccionada.
     */
    fun send(action: RemoteAction): Boolean {
        val manager = irManager ?: return false
        if (!manager.hasIrEmitter()) return false

        val pair = codeTable[brand]?.get(action) ?: codeTable[TvBrand.GENERIC_NEC]?.get(action)
            ?: return false

        val pattern = buildNecPattern(pair.first, pair.second)
        manager.transmit(carrierFrequency, pattern)
        return true
    }

    /**
     * Construye el patrón de pulsos (microsegundos) de una trama NEC estándar:
     * header 9ms mark + 4.5ms space, 8 bits address (+ inverso), 8 bits command
     * (+ inverso), cada bit 562us mark + 562/1687us space, y un stop bit final.
     */
    private fun buildNecPattern(address: Int, command: Int): IntArray {
        val pulses = mutableListOf<Int>()
        val unit = 562

        // Header
        pulses.add(unit * 16) // 9ms mark
        pulses.add(unit * 8)  // 4.5ms space

        fun addByte(byte: Int) {
            for (i in 0 until 8) {
                val bit = (byte shr i) and 1
                pulses.add(unit)
                pulses.add(if (bit == 1) unit * 3 else unit)
            }
        }

        val addressInv = address.inv() and 0xFF
        val commandInv = command.inv() and 0xFF

        addByte(address and 0xFF)
        addByte(addressInv)
        addByte(command and 0xFF)
        addByte(commandInv)

        // Stop bit
        pulses.add(unit)

        return pulses.toIntArray()
    }
}
