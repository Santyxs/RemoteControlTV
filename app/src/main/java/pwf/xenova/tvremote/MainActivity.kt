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
            Spacer(Modifier.height(16.dp))

            BrandSelector(brand) { brand = it; irController.brand = it }

            Spacer(Modifier.height(12.dp))

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
        listOf(GradientPink, GradientPurple, GradientBlue, GradientOrange, GradientPink)
    )

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Contorno gradiente en forma de cruz redondeada
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val armW = w * 0.34f
            val corner = armW / 2f

            val path = Path().apply {
                // Cruz redondeada: usamos un path simple con esquinas redondeadas
                val cx = w / 2f
                val cy = h / 2f
                val half = armW / 2f
                val outer = w * 0.44f

                moveTo(cx - half, cy - outer)
                lineTo(cx + half, cy - outer)
                lineTo(cx + half, cy - half)
                lineTo(cx + outer, cy - half)
                lineTo(cx + outer, cy + half)
                lineTo(cx + half, cy + half)
                lineTo(cx + half, cy + outer)
                lineTo(cx - half, cy + outer)
                lineTo(cx - half, cy + half)
                lineTo(cx - outer, cy + half)
                lineTo(cx - outer, cy - half)
                lineTo(cx - half, cy - half)
                close()
            }

            drawPath(
                path = path,
                brush = gradientBrush,
                style = Stroke(width = corner * 0.18f)
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
                            listOf(GradientOrange, Color(0xFFE0447A), GradientPurple, GradientBlue)
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
            .size(66.dp)
            .clip(CircleShape)
            .background(ButtonBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextPrimary)
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
            Triple("Mirroring", Icons.Filled.Cast, 2),
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

