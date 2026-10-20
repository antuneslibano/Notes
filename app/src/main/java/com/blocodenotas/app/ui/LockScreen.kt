package com.blocodenotas.app.ui

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.blocodenotas.app.Graph

object Biometrics {
    private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun isAvailable(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun prompt(activity: FragmentActivity, onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Minhas Notas")
            .setSubtitle("Use sua digital ou rosto")
            .setNegativeButtonText("Usar PIN")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
    }
}

@Composable
fun LockScreen(resumeTick: Int, onUnlock: () -> Unit) {
    val settings = Graph.settings
    val activity = LocalContext.current as FragmentActivity
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var autoPrompted by remember { mutableStateOf(false) }
    val available = remember { Biometrics.isAvailable(activity) }
    val biometric = settings.biometricEnabled && available

    BackHandler { activity.finish() }

    LaunchedEffect(resumeTick) {
        val resumed = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (biometric && !autoPrompted && resumed) {
            autoPrompted = true
            Biometrics.prompt(activity, onUnlock)
        }
    }

    fun press(digit: String) {
        if (pin.length >= 8) return
        error = false
        pin += digit
        if (pin.length == settings.pinLength) {
            if (settings.checkPin(pin)) {
                onUnlock()
            } else {
                error = true
                pin = ""
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Lock, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(20.dp))
            Text("Minhas notas", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(6.dp))
            Text(
                if (error) "PIN incorreto. Tente novamente." else "Digite seu PIN para continuar",
                color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            PinDots(filled = pin.length, total = settings.pinLength)
            Spacer(Modifier.height(36.dp))
            PinPad(
                onDigit = ::press,
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                extraIcon = if (biometric) Icons.Outlined.Fingerprint else null,
                onExtra = { Biometrics.prompt(activity, onUnlock) },
            )
        }
    }
}

@Composable
fun PinDots(filled: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(total) { i ->
            val on = i < filled
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                    .border(1.5.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
    }
}

@Composable
fun PinPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    extraIcon: ImageVector? = null,
    onExtra: () -> Unit = {},
    keySize: Dp = 76.dp,
    spacing: Dp = 22.dp,
) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    Column(verticalArrangement = Arrangement.spacedBy(spacing * 0.6f)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEach { d -> PadKey(keySize, onClick = { onDigit(d) }) { Text(d, fontSize = 26.sp, color = MaterialTheme.colorScheme.onBackground) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            if (extraIcon != null) {
                PadKey(keySize, onClick = onExtra, filled = false) {
                    Icon(extraIcon, "Usar biometria", Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Spacer(Modifier.size(keySize))
            }
            PadKey(keySize, onClick = { onDigit("0") }) { Text("0", fontSize = 26.sp, color = MaterialTheme.colorScheme.onBackground) }
            PadKey(keySize, onClick = onBackspace, filled = false) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, "Apagar", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun PadKey(size: Dp, onClick: () -> Unit, filled: Boolean = true, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
