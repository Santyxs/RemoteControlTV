package pwf.xenova.tvremote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pwf.xenova.tvremote.ui.theme.*

private enum class WizardStep {
    WELCOME, IR_CHECK, BRAND_SELECT, PREPARE_TV, FIRST_TEST, DID_RESPOND,
    AUTO_SEARCH, CONFIRM_POWER, CHECK_VOLUME, CHECK_CHANNEL, CHECK_MUTE, DONE
}

@Composable
fun SetupWizardScreen(
    irController: IrRemoteController,
    hapticsController: HapticsController,
    onFinished: () -> Unit
) {
    var step by remember { mutableStateOf(WizardStep.WELCOME) }
    var selectedBrand by remember { mutableStateOf<TvBrand?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var workingBrand by remember { mutableStateOf<TvBrand?>(null) }

    // Búsqueda automática
    var autoCandidates by remember { mutableStateOf(listOf<TvBrand>()) }
    var autoIndex by remember { mutableStateOf(0) }
    var autoSearching by remember { mutableStateOf(false) }
    var autoExhausted by remember { mutableStateOf(false) }

    // Verificación paso a paso (vol / canal / mute)
    var checkFailedNote by remember { mutableStateOf(false) }

    fun startAutoSearch() {
        autoCandidates = SETUP_BRAND_ORDER.filter { it != selectedBrand }
        autoIndex = 0
        autoSearching = true
        autoExhausted = false
        step = WizardStep.AUTO_SEARCH
    }

    // Motor de la búsqueda automática: manda POWER del candidato actual y espera
    LaunchedEffect(step, autoIndex, autoSearching) {
        if (step == WizardStep.AUTO_SEARCH && autoSearching) {
            if (autoIndex < autoCandidates.size) {
                irController.brand = autoCandidates[autoIndex]
                hapticsController.vibrate()
                irController.send(RemoteAction.POWER)
                delay(1800)
                if (autoSearching) autoIndex++
            } else {
                autoSearching = false
                autoExhausted = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            .padding(24.dp)
    ) {
        when (step) {
            WizardStep.WELCOME -> WelcomeStep { step = WizardStep.IR_CHECK }

            WizardStep.IR_CHECK -> IrCheckStep(
                isAvailable = irController.isIrAvailable,
                onContinue = { step = WizardStep.BRAND_SELECT },
                onExit = onFinished
            )

            WizardStep.BRAND_SELECT -> BrandSelectStep(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                selected = selectedBrand,
                onSelect = { selectedBrand = it },
                onContinue = { step = WizardStep.PREPARE_TV }
            )

            WizardStep.PREPARE_TV -> PrepareTvStep { step = WizardStep.FIRST_TEST }

            WizardStep.FIRST_TEST -> FirstTestStep {
                irController.brand = selectedBrand ?: TvBrand.GENERIC_NEC
                hapticsController.vibrate()
                irController.send(RemoteAction.POWER)
                step = WizardStep.DID_RESPOND
            }

            WizardStep.DID_RESPOND -> DidRespondStep(
                onYes = {
                    workingBrand = selectedBrand
                    irController.brand = selectedBrand ?: TvBrand.GENERIC_NEC
                    checkFailedNote = false
                    step = WizardStep.CHECK_VOLUME
                },
                onNo = { startAutoSearch() }
            )

            WizardStep.AUTO_SEARCH -> AutoSearchStep(
                index = autoIndex,
                total = autoCandidates.size,
                exhausted = autoExhausted,
                onWorked = { autoSearching = false; step = WizardStep.CONFIRM_POWER },
                onUseGeneric = {
                    workingBrand = TvBrand.GENERIC_NEC
                    irController.brand = TvBrand.GENERIC_NEC
                    checkFailedNote = false
                    step = WizardStep.CHECK_VOLUME
                },
                onRetryFromStart = {
                    autoIndex = 0
                    autoSearching = true
                    autoExhausted = false
                }
            )

            WizardStep.CONFIRM_POWER -> ConfirmPowerStep(
                onYes = {
                    val candidate = autoCandidates.getOrNull(autoIndex) ?: TvBrand.GENERIC_NEC
                    workingBrand = candidate
                    irController.brand = candidate
                    checkFailedNote = false
                    step = WizardStep.CHECK_VOLUME
                },
                onNo = {
                    autoIndex++
                    autoSearching = true
                    step = WizardStep.AUTO_SEARCH
                }
            )

            WizardStep.CHECK_VOLUME -> CheckActionStep(
                title = "Comprobemos el volumen",
                instruction = "Pulsa el botón para comprobar que podemos controlar el volumen.",
                buttonLabel = "VOL +",
                question = "¿Ha subido el volumen?",
                showFailNote = checkFailedNote,
                onPress = {
                    hapticsController.vibrate()
                    irController.send(RemoteAction.VOL_UP)
                },
                onYes = { checkFailedNote = false; step = WizardStep.CHECK_CHANNEL },
                onNo = { checkFailedNote = true },
                onContinueAnyway = { checkFailedNote = false; step = WizardStep.CHECK_CHANNEL }
            )

            WizardStep.CHECK_CHANNEL -> CheckActionStep(
                title = "Comprobemos los canales",
                instruction = "Pulsa el botón para comprobar que podemos cambiar de canal.",
                buttonLabel = "CH +",
                question = "¿Ha cambiado el canal?",
                showFailNote = checkFailedNote,
                onPress = {
                    hapticsController.vibrate()
                    irController.send(RemoteAction.CH_UP)
                },
                onYes = { checkFailedNote = false; step = WizardStep.CHECK_MUTE },
                onNo = { checkFailedNote = true },
                onContinueAnyway = { checkFailedNote = false; step = WizardStep.CHECK_MUTE }
            )

            WizardStep.CHECK_MUTE -> CheckActionStep(
                title = "Comprobemos MUTE",
                instruction = "Pulsa el botón para comprobar el silencio.",
                buttonLabel = "🔇 MUTE",
                question = "¿El televisor ha silenciado el sonido?",
                showFailNote = checkFailedNote,
                onPress = {
                    hapticsController.vibrate()
                    irController.send(RemoteAction.MUTE)
                },
                onYes = { checkFailedNote = false; step = WizardStep.DONE },
                onNo = { checkFailedNote = true },
                onContinueAnyway = { checkFailedNote = false; step = WizardStep.DONE }
            )

            WizardStep.DONE -> DoneStep(
                brandName = (workingBrand ?: selectedBrand)?.displayName ?: "Genérico",
                onFinish = onFinished
            )
        }
    }
}

// ---------- Componentes visuales compartidos del asistente ----------

@Composable
private fun WizardTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
}

@Composable
private fun WizardBody(text: String) {
    Text(text, color = TextSecondary, fontSize = 15.sp, lineHeight = 21.sp)
}

@Composable
private fun WizardPrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.horizontalGradient(listOf(DpadTeal, DpadCyan, DpadBlue)))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WizardSecondaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(ButtonBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------- Paso 1: Bienvenida ----------

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        WizardTitle("Configura tu televisor")
        Spacer(Modifier.height(16.dp))
        WizardBody("Vamos a configurar tu mando universal. Solo necesitarás apuntar el teléfono hacia tu televisor.")
        Spacer(Modifier.height(40.dp))
        WizardPrimaryButton("Empezar", onStart)
    }
}

// ---------- Paso 2: Comprobación del emisor IR ----------

@Composable
private fun IrCheckStep(isAvailable: Boolean, onContinue: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        if (isAvailable) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = DpadTeal, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text("El teléfono tiene emisor infrarrojo.", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp))
            WizardPrimaryButton("Continuar", onContinue)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFFE05C5C), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text("Este teléfono no tiene emisor infrarrojo.", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            WizardBody("La aplicación necesita un emisor IR para controlar televisores.")
            Spacer(Modifier.height(40.dp))
            WizardSecondaryButton("Continuar de todos modos", onContinue)
            Spacer(Modifier.height(12.dp))
            WizardSecondaryButton("Salir del asistente", onExit)
        }
    }
}

// ---------- Paso 3: Elegir marca ----------

@Composable
private fun BrandSelectStep(
    query: String,
    onQueryChange: (String) -> Unit,
    selected: TvBrand?,
    onSelect: (TvBrand) -> Unit,
    onContinue: () -> Unit
) {
    val filtered = remember(query) {
        SETUP_BRAND_ORDER.filter { it != TvBrand.GENERIC_NEC }
            .filter { it.displayName.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WizardTitle("¿Qué marca es tu TV?")
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ButtonBg)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextFieldPlaceholder(query, onQueryChange, "Buscar marca")
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { brand ->
                val isSelected = brand == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) AccentBlue.copy(alpha = 0.25f) else ButtonBg)
                        .clickable { onSelect(brand) }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(brand.displayName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = AccentBlue)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        if (selected != null) {
            WizardPrimaryButton("Continuar", onContinue)
        }
    }
}

@Composable
private fun BasicTextFieldPlaceholder(value: String, onChange: (String) -> Unit, placeholder: String) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(TextPrimary),
        keyboardOptions = KeyboardOptions.Default,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, color = TextSecondary, fontSize = 15.sp)
                }
                innerTextField()
            }
        }
    )
}

// ---------- Paso 4: Preparar el televisor ----------

@Composable
private fun PrepareTvStep(onReady: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        WizardTitle("Prepara tu televisor")
        Spacer(Modifier.height(16.dp))
        WizardBody("Enciende el televisor y apunta la parte superior del teléfono directamente hacia él.")
        Spacer(Modifier.height(32.dp))
        Text("📱  →  📺", fontSize = 40.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        WizardBody("Mantén el teléfono a menos de 5 metros.")
        Spacer(Modifier.height(40.dp))
        WizardPrimaryButton("Listo", onReady)
    }
}

// ---------- Paso 5: Primera prueba ----------

@Composable
private fun FirstTestStep(onTest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        WizardTitle("Probemos el mando")
        Spacer(Modifier.height(16.dp))
        WizardBody("Vamos a enviar una señal de prueba al televisor.")
        Spacer(Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color(0xFFE05C5C))
                .clickable { onTest() },
            contentAlignment = Alignment.Center
        ) {
            Text("🔴  Probar POWER", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------- Paso 6: ¿Ha respondido? ----------

@Composable
private fun DidRespondStep(onYes: () -> Unit, onNo: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        WizardTitle("¿Ha respondido el televisor?")
        Spacer(Modifier.height(16.dp))
        WizardBody("Hemos enviado una señal de encendido/apagado.")
        Spacer(Modifier.height(40.dp))
        WizardPrimaryButton("✓  Sí, ha respondido", onYes)
        Spacer(Modifier.height(12.dp))
        WizardSecondaryButton("✕  No, continuar buscando", onNo)
    }
}

// ---------- Paso 7: Búsqueda automática ----------

@Composable
private fun AutoSearchStep(
    index: Int,
    total: Int,
    exhausted: Boolean,
    onWorked: () -> Unit,
    onUseGeneric: () -> Unit,
    onRetryFromStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        WizardTitle("Buscando la configuración correcta")
        Spacer(Modifier.height(16.dp))

        if (exhausted) {
            WizardBody("No se encontró una coincidencia entre los perfiles disponibles.")
            Spacer(Modifier.height(32.dp))
            WizardPrimaryButton("Usar modo genérico", onUseGeneric)
            Spacer(Modifier.height(12.dp))
            WizardSecondaryButton("Reintentar desde el principio", onRetryFromStart)
        } else {
            WizardBody("Probando códigos compatibles…")
            Spacer(Modifier.height(24.dp))

            val progress = if (total > 0) (index + 1).coerceAtMost(total) / total.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = DpadCyan,
                trackColor = ButtonBg
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Código ${(index + 1).coerceAtMost(total)} de $total",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(40.dp))
            WizardPrimaryButton("¡Funcionó! Detener aquí", onWorked)
        }
    }
}

// ---------- Paso 8: Confirmar POWER ----------

@Composable
private fun ConfirmPowerStep(onYes: () -> Unit, onNo: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        WizardTitle("¡Se ha encontrado la configuración!")
        Spacer(Modifier.height(16.dp))
        WizardBody("El televisor ha respondido.")
        Spacer(Modifier.height(40.dp))
        WizardPrimaryButton("Sí, funciona", onYes)
        Spacer(Modifier.height(12.dp))
        WizardSecondaryButton("No, seguir buscando", onNo)
    }
}

// ---------- Pasos 9, 10, 11: comprobar volumen / canal / mute (mismo patrón) ----------

@Composable
private fun CheckActionStep(
    title: String,
    instruction: String,
    buttonLabel: String,
    question: String,
    showFailNote: Boolean,
    onPress: () -> Unit,
    onYes: () -> Unit,
    onNo: () -> Unit,
    onContinueAnyway: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        WizardTitle(title)
        Spacer(Modifier.height(16.dp))
        WizardBody(instruction)
        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.horizontalGradient(listOf(DpadTeal, DpadCyan, DpadBlue)))
                .clickable { onPress() },
            contentAlignment = Alignment.Center
        ) {
            Text(buttonLabel, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(28.dp))
        Text(question, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ButtonBg)
                    .clickable { onYes() },
                contentAlignment = Alignment.Center
            ) {
                Text("Sí", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ButtonBg)
                    .clickable { onNo() },
                contentAlignment = Alignment.Center
            ) {
                Text("No", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showFailNote) {
            Spacer(Modifier.height(20.dp))
            WizardBody("Algunos televisores no responden igual a todos los botones. Puedes reintentar o continuar de todos modos.")
            Spacer(Modifier.height(12.dp))
            WizardSecondaryButton("Continuar de todos modos", onContinueAnyway)
        }
    }
}

// ---------- Paso 12: Configuración completada ----------

@Composable
private fun DoneStep(brandName: String, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        WizardTitle("¡Televisor configurado!")
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = DpadTeal, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(brandName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        WizardBody("Hemos conectado tu móvil con tu televisor.")
        Spacer(Modifier.height(40.dp))
        WizardPrimaryButton("Listo", onFinish)
    }
}
