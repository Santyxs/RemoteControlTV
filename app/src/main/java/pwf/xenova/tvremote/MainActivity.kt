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
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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

        // TOOLS debajo de POWER, INFO debajo de HOME
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RoundLabelButton("TOOLS") { onAction(RemoteAction.TOOLS) }
            RoundLabelButton("INFO") { onAction(RemoteAction.INFO) }
        }

        // Dpad centrado
        DPad(onAction)

        // Back / Exit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RoundIconButton(Icons.Filled.Undo) { onAction(RemoteAction.BACK) }
            RoundLabelButton("EXIT") { onAction(RemoteAction.EXIT) }
        }

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
    }
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
        modifier = Modifier.size(190.dp),
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

        // Botones direccionales: posicionados a una distancia fija del centro,
        // pegados al OK y dentro del contorno de la cruz (no en el borde del Box)
        Box(Modifier.fillMaxSize()) {
            RoundIconButtonSmall(
                icon = Icons.Filled.KeyboardArrowUp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-66).dp)
            ) { onAction(RemoteAction.UP) }

            RoundIconButtonSmall(
                icon = Icons.Filled.KeyboardArrowDown,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 66.dp)
            ) { onAction(RemoteAction.DOWN) }

            RoundIconButtonSmall(
                icon = Icons.Filled.KeyboardArrowLeft,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (-66).dp)
            ) { onAction(RemoteAction.LEFT) }

            RoundIconButtonSmall(
                icon = Icons.Filled.KeyboardArrowRight,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 66.dp)
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
            .height(230.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(ButtonBg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .clickable { onPlus() },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = TextPrimary, fontSize = 22.sp)
        }
        Text(label, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
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
