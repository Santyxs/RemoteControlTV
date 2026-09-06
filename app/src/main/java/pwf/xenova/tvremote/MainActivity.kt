package pwf.xenova.tvremote

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pwf.xenova.tvremote.ui.theme.*

/**
 * Modifier compartido: click con una pequeña animación de "hundido" al presionar
 * (feedback táctil) además del ripple normal de Material.
 */
@Composable
fun Modifier.pressableScale(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.90f else 1f, label = "pressScale")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val irController = IrRemoteController(this)
        val hapticsController = HapticsController(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                RemoteScreen(
                    irController = irController,
                    hapticsController = hapticsController,
                    onAction = { action ->
                        try {
                            hapticsController.vibrate()
                            when (val result = irController.send(action)) {
                                is SendResult.Success -> {}
                                is SendResult.NoHardware -> Toast.makeText(
                                    this, "Este teléfono no tiene emisor infrarrojo", Toast.LENGTH_SHORT
                                ).show()
                                is SendResult.NoCode -> Toast.makeText(
                                    this, "Código no disponible para esta marca", Toast.LENGTH_SHORT
                                ).show()
                                is SendResult.Exception -> Toast.makeText(
                                    this, "No se pudo enviar la señal", Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            // Ultima red de seguridad: cualquier fallo inesperado al procesar
                            // una pulsación (vibracion, IR, lo que sea) nunca debe cerrar la app
                            Toast.makeText(
                                this, "No se pudo enviar la señal", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RemoteScreen(
    irController: IrRemoteController,
    hapticsController: HapticsController,
    onAction: (RemoteAction) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var brand by remember { mutableStateOf(irController.brand) }
    var hapticsEnabled by remember { mutableStateOf(hapticsController.enabled) }

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
                else -> SettingsTabContent(
                    brand = brand,
                    onBrandChange = { brand = it; irController.brand = it },
                    hapticsEnabled = hapticsEnabled,
                    onHapticsChange = { hapticsEnabled = it; hapticsController.enabled = it }
                )
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

        // CH.LIST debajo de POWER, INFO debajo de HOME
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RoundLabelButton("CH LIST", fontSize = 9.5.sp) { onAction(RemoteAction.CH_LIST) }
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

        Spacer(Modifier.height(12.dp))

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

    Text("Canal", color = TextSecondary, fontSize = 17.sp, fontWeight = FontWeight.Bold)

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
            .shadow(6.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(ButtonBgLight, ButtonBg)))
            .pressableScale { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun SettingsTabContent(
    brand: TvBrand,
    onBrandChange: (TvBrand) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit
) {
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

    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ButtonBg)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Vibración al pulsar", color = TextPrimary, fontSize = 15.sp)
        Switch(
            checked = hapticsEnabled,
            onCheckedChange = onHapticsChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue)
        )
    }

    Spacer(Modifier.height(16.dp))

    UpdateSection()
}

@Composable
fun UpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var downloadUrl by remember { mutableStateOf<String?>(null) }
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ButtonBg)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Actualizaciones", color = TextPrimary, fontSize = 15.sp)

            val isDownloading = downloadState is DownloadState.Downloading

            if (downloadUrl != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDownloading) ButtonBgLight else AccentBlue)
                        .clickable(enabled = !isDownloading) {
                            val url = downloadUrl!!
                            downloadState = DownloadState.Downloading(0)
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    ApkDownloader.downloadApk(context, url) { state ->
                                        downloadState = state
                                    }
                                }
                                val finalState = downloadState
                                if (finalState is DownloadState.Done) {
                                    ApkDownloader.installApk(context, finalState.file)
                                } else if (finalState is DownloadState.Failed) {
                                    status = "Error al descargar: ${finalState.message}"
                                }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    val label = when (val s = downloadState) {
                        is DownloadState.Downloading -> "Descargando ${s.progress}%"
                        else -> "Descargar e instalar"
                    }
                    Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (checking) ButtonBgLight else AccentBlue)
                        .clickable(enabled = !checking) {
                            checking = true
                            status = null
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    UpdateChecker.checkForUpdate(
                                        BuildConfig.BUILD_NUMBER.toIntOrNull() ?: 0
                                    )
                                }
                                checking = false
                                when (result) {
                                    is UpdateCheckResult.UpdateAvailable -> {
                                        status = "Hay una versión nueva disponible"
                                        downloadUrl = result.downloadUrl
                                    }
                                    is UpdateCheckResult.UpToDate ->
                                        status = "Ya tienes la última versión"
                                    is UpdateCheckResult.Error ->
                                        status = "Error: ${result.message}"
                                }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        if (checking) "Buscando…" else "Buscar",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (status != null) {
            Spacer(Modifier.height(10.dp))
            Text(status!!, color = TextSecondary, fontSize = 12.sp)
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
fun OutlinedIconPill(icon: ImageVector, label: String?, tint: Color = TextPrimary, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp, 48.dp)
            .shadow(3.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(ButtonBg.copy(alpha = 0.5f))
            .border(1.dp, ButtonBgLight, RoundedCornerShape(24.dp))
            .pressableScale { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}

@Composable
fun OutlinedTextPill(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .shadow(3.dp, RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(ButtonBg.copy(alpha = 0.5f))
            .border(1.dp, ButtonBgLight, RoundedCornerShape(24.dp))
            .pressableScale { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun RoundLabelButton(text: String, fontSize: androidx.compose.ui.unit.TextUnit = 11.sp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(66.dp)
            .shadow(6.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(ButtonBgLight, ButtonBg)))
            .pressableScale { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextPrimary, fontSize = fontSize, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(66.dp)
            .shadow(6.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(ButtonBgLight, ButtonBg)))
            .pressableScale { onClick() },
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
        modifier = Modifier.size(226.dp),
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
            val armW = w * 0.36f
            val half = armW / 2f
            val outer = w * 0.56f
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
                style = Stroke(width = half * 0.30f)
            )
        }

        // Botones direccionales: posicionados a una distancia fija del centro,
        // pegados al OK y dentro del contorno de la cruz (no en el borde del Box)
        Box(Modifier.fillMaxSize()) {
            RoundIconButtonSmall(
                icon = Icons.Filled.KeyboardArrowUp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-80).dp)
            ) { onAction(RemoteAction.UP) }

            RoundIconButtonSmall(
                icon = Icons.Filled.KeyboardArrowDown,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 80.dp)
            ) { onAction(RemoteAction.DOWN) }

            RoundIconButtonSmall(
                icon = Icons.Filled.KeyboardArrowLeft,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (-80).dp)
            ) { onAction(RemoteAction.LEFT) }

            RoundIconButtonSmall(
                icon = Icons.Filled.KeyboardArrowRight,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 80.dp)
            ) { onAction(RemoteAction.RIGHT) }

            // OK central
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(76.dp)
                    .shadow(10.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(DpadTeal, DpadCyan, DpadBlue, DpadDeepBlue)
                        )
                    )
                    .pressableScale { onAction(RemoteAction.OK) },
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
            .shadow(6.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(ButtonBgLight, ButtonBg)))
            .pressableScale { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
fun VerticalRockerPill(label: String, onPlus: () -> Unit, onMinus: () -> Unit) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .height(205.dp)
            .shadow(6.dp, RoundedCornerShape(32.dp), clip = false)
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.verticalGradient(listOf(ButtonBgLight, ButtonBg))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .repeatingPress { onPlus() },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .repeatingPress { onMinus() },
            contentAlignment = Alignment.Center
        ) {
            Text("–", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/**
 * Modifier que dispara [onAction] al pulsar, y sigue repitiendo mientras el dedo
 * se mantenga presionado (como los botones de VOL/CH de un mando físico).
 */
fun Modifier.repeatingPress(
    initialDelayMillis: Long = 400,
    repeatMillis: Long = 100,
    onAction: () -> Unit
): Modifier = composed {
    val currentAction by rememberUpdatedState(onAction)
    pointerInput(Unit) {
        coroutineScope {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                currentAction()
                val job = launch {
                    delay(initialDelayMillis)
                    while (isActive) {
                        currentAction()
                        delay(repeatMillis)
                    }
                }
                waitForUpOrCancellation()
                job.cancel()
            }
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
