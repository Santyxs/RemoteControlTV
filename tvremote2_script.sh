#!/bin/bash
set -e

mkdir -p app/src/main/java/pwf/xenova/tvremote
cat > app/src/main/java/pwf/xenova/tvremote/IrRemoteController.kt << 'ZZEOF'
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
ZZEOF

mkdir -p app/src/main/java/pwf/xenova/tvremote
cat > app/src/main/java/pwf/xenova/tvremote/MainActivity.kt << 'ZZEOF'
package pwf.xenova.tvremote

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pwf.xenova.tvremote.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val irController = IrRemoteController(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                RemoteScreen(
                    irController = irController,
                    onAction = { action ->
                        val sent = irController.send(action)
                        if (!sent) {
                            Toast.makeText(
                                this,
                                if (!irController.isIrAvailable)
                                    "Este teléfono no tiene emisor infrarrojo"
                                else
                                    "Código no disponible para esta marca",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RemoteScreen(irController: IrRemoteController, onAction: (RemoteAction) -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var brand by remember { mutableStateOf(irController.brand) }

    Scaffold(
        containerColor = BgBottom,
        bottomBar = { BottomNav(selectedTab) { selectedTab = it } }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (selectedTab) {
                0 -> RemoteTabContent(onAction)
                1 -> ControlTabContent(onAction)
                else -> SettingsTabContent(brand) { brand = it; irController.brand = it }
            }
        }
    }
}

@Composable
fun RemoteTabContent(onAction: (RemoteAction) -> Unit) {
    Spacer(Modifier.height(20.dp))

    // Fila superior: power / guide / hdmi / home
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedIconPill(Icons.Filled.PowerSettingsNew, null) { onAction(RemoteAction.POWER) }
        OutlinedTextPill("GUIDE") { onAction(RemoteAction.GUIDE) }
        OutlinedTextPill("HDMI") { onAction(RemoteAction.HDMI) }
        OutlinedIconPill(Icons.Filled.Home, null) { onAction(RemoteAction.HOME) }
    }

    Spacer(Modifier.height(28.dp))

    // Fila TOOLS - Dpad - INFO
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RoundLabelButton("TOOLS") { onAction(RemoteAction.TOOLS) }
        DPad(onAction)
        RoundLabelButton("INFO") { onAction(RemoteAction.INFO) }
    }

    Spacer(Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RoundIconButton(Icons.Filled.Undo) { onAction(RemoteAction.BACK) }
        RoundLabelButton("EXIT") { onAction(RemoteAction.EXIT) }
    }

    Spacer(Modifier.height(28.dp))

    // Fila VOL - MENU/MUTE/SOURCE - CH
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        VerticalRockerPill(
            label = "VOL",
            onPlus = { onAction(RemoteAction.VOL_UP) },
            onMinus = { onAction(RemoteAction.VOL_DOWN) }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RoundLabelButton("MENU") { onAction(RemoteAction.MENU) }
            RoundIconButton(Icons.Filled.VolumeOff) { onAction(RemoteAction.MUTE) }
            RoundIconButton(Icons.Filled.Input) { onAction(RemoteAction.SOURCE) }
        }

        VerticalRockerPill(
            label = "CH",
            onPlus = { onAction(RemoteAction.CH_UP) },
            onMinus = { onAction(RemoteAction.CH_DOWN) }
        )
    }

    Spacer(Modifier.height(20.dp))
}

@Composable
fun ControlTabContent(onAction: (RemoteAction) -> Unit) {
    Spacer(Modifier.height(32.dp))

    Text("Canal", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)

    Spacer(Modifier.height(20.dp))

    val numberRows = listOf(
        listOf(RemoteAction.NUM_1 to "1", RemoteAction.NUM_2 to "2", RemoteAction.NUM_3 to "3"),
        listOf(RemoteAction.NUM_4 to "4", RemoteAction.NUM_5 to "5", RemoteAction.NUM_6 to "6"),
        listOf(RemoteAction.NUM_7 to "7", RemoteAction.NUM_8 to "8", RemoteAction.NUM_9 to "9")
    )

    numberRows.forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            row.forEach { (action, label) ->
                NumberKey(label) { onAction(action) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        NumberKey("0") { onAction(RemoteAction.NUM_0) }
    }

    Spacer(Modifier.height(32.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        VerticalRockerPill(
            label = "CH",
            onPlus = { onAction(RemoteAction.CH_UP) },
            onMinus = { onAction(RemoteAction.CH_DOWN) }
        )
    }
}

@Composable
fun NumberKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(ButtonBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingsTabContent(brand: TvBrand, onBrandChange: (TvBrand) -> Unit) {
    Spacer(Modifier.height(32.dp))

    Text("Ajustes", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

    Spacer(Modifier.height(28.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ButtonBg)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Marca de televisión", color = TextPrimary, fontSize = 15.sp)
        BrandSelector(brand, onBrandChange)
    }
}

@Composable
fun BrandSelector(current: TvBrand, onSelected: (TvBrand) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(current.displayName, color = TextSecondary, fontSize = 13.sp)
            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextSecondary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TvBrand.entries.forEach { b ->
                DropdownMenuItem(text = { Text(b.displayName) }, onClick = {
                    onSelected(b); expanded = false
                })
            }
        }
    }
}

@Composable
fun OutlinedIconPill(icon: ImageVector, label: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp, 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Transparent)
            .border(1.dp, ButtonBgLight, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextPrimary)
    }
}

@Composable
fun OutlinedTextPill(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, ButtonBgLight, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RoundLabelButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(ButtonBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(ButtonBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextPrimary)
    }
}

@Composable
fun DPad(onAction: (RemoteAction) -> Unit) {
    val gradientBrush = Brush.sweepGradient(
        listOf(DpadCyan, DpadBlue, DpadDeepBlue, DpadTeal, DpadCyan)
    )

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Contorno gradiente en forma de cruz con esquinas redondeadas:
        // unión de dos rectángulos redondeados (vertical + horizontal), técnica
        // fiable en vez de calcular arcos a mano.
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val armW = w * 0.34f
            val half = armW / 2f
            val outer = w * 0.44f
            val cornerRadius = androidx.compose.ui.geometry.CornerRadius(half * 0.6f, half * 0.6f)

            val verticalBar = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = cx - half, top = cy - outer,
                        right = cx + half, bottom = cy + outer,
                        cornerRadius = cornerRadius
                    )
                )
            }
            val horizontalBar = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = cx - outer, top = cy - half,
                        right = cx + outer, bottom = cy + half,
                        cornerRadius = cornerRadius
                    )
                )
            }
            val crossPath = Path()
            crossPath.op(verticalBar, horizontalBar, androidx.compose.ui.graphics.PathOperation.Union)

            drawPath(
                path = crossPath,
                brush = gradientBrush,
                style = Stroke(width = half * 0.28f)
            )
        }

        // Botones direccionales
        Box(Modifier.fillMaxSize()) {
            RoundIconButtonSmall(
                icon = Icons.Filled.KeyboardArrowUp,
                modifier = Modifier.align(Alignment.TopCenter)
            ) { onAction(RemoteAction.UP) }

            RoundIconButtonSmall(
                icon = Icons.Filled.KeyboardArrowDown,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) { onAction(RemoteAction.DOWN) }

            RoundIconButtonSmall(
                icon = Icons.Filled.KeyboardArrowLeft,
                modifier = Modifier.align(Alignment.CenterStart)
            ) { onAction(RemoteAction.LEFT) }

            RoundIconButtonSmall(
                icon = Icons.Filled.KeyboardArrowRight,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) { onAction(RemoteAction.RIGHT) }

            // OK central
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(DpadTeal, DpadCyan, DpadBlue, DpadDeepBlue)
                        )
                    )
                    .clickable { onAction(RemoteAction.OK) },
                contentAlignment = Alignment.Center
            ) {
                Text("OK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun RoundIconButtonSmall(icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(ButtonBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun VerticalRockerPill(label: String, onPlus: () -> Unit, onMinus: () -> Unit) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .height(190.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(ButtonBg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(63.dp)
                .clickable { onPlus() },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = TextPrimary, fontSize = 22.sp)
        }
        Text(label, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(63.dp)
                .clickable { onMinus() },
            contentAlignment = Alignment.Center
        ) {
            Text("–", color = TextPrimary, fontSize = 22.sp)
        }
    }
}

@Composable
fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = BgBottom) {
        val items = listOf(
            Triple("Remote", Icons.Filled.SettingsRemote, 0),
            Triple("Control", Icons.Filled.GridView, 1),
            Triple("Settings", Icons.Filled.Settings, 3)
        )
        items.forEach { (label, icon, index) ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = { Icon(imageVector = icon, contentDescription = null) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentBlue,
                    selectedTextColor = AccentBlue,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
ZZEOF

echo "Flechas mas pequenas, teclado numerico en Control, TV en Settings. Compilando..."
./gradlew assembleDebug
rm -- "$0"