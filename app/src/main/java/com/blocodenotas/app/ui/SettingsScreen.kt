package com.blocodenotas.app.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.blocodenotas.app.Graph
import com.blocodenotas.app.data.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val repo = Graph.repo
    val settings = Graph.settings
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val biometricAvailable = remember { Biometrics.isAvailable(activity) }

    var settingPin by remember { mutableStateOf(false) }
    var confirmDisablePin by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<String?>(null) }

    val stamp = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) writeToUri(context, uri, repo.exportBackup(), "Backup salvo")
    }
    val exportTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) writeToUri(context, uri, repo.exportText(), "Notas exportadas")
    }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            }.getOrNull()
            if (text.isNullOrBlank()) toast(context, "Não foi possível ler o arquivo") else pendingRestore = text
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsGroup("Aparência") {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { settings.updateTheme(mode) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = settings.theme == mode, onClick = { settings.updateTheme(mode) })
                        Spacer(Modifier.width(8.dp))
                        Text(mode.label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            SettingsGroup("Segurança") {
                SwitchRow(
                    icon = Icons.Outlined.Lock,
                    title = "Bloquear com PIN",
                    subtitle = "Pede um PIN sempre que o app for aberto",
                    checked = settings.pinEnabled,
                    onChange = { on -> if (on) settingPin = true else confirmDisablePin = true },
                )
                if (settings.pinEnabled) {
                    ActionRow(Icons.Outlined.Password, "Alterar PIN", null) { settingPin = true }
                    SwitchRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = "Desbloquear com biometria",
                        subtitle = if (biometricAvailable) "Digital ou reconhecimento facial" else "Não disponível neste aparelho",
                        checked = settings.biometricEnabled && biometricAvailable,
                        enabled = biometricAvailable,
                        onChange = { settings.updateBiometric(it) },
                    )
                }
            }

            SettingsGroup("Backup e restauração") {
                Text(
                    "Suas notas ficam salvas apenas neste celular. Faça um backup de vez em quando e guarde o arquivo em um lugar seguro.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                ActionRow(Icons.Outlined.Save, "Fazer backup", "Salva todas as notas em um arquivo .json") {
                    Graph.skipNextLock = true
                    exportJson.launch("minhas-notas-backup-$stamp.json")
                }
                ActionRow(Icons.Outlined.Restore, "Restaurar backup", "Recupera notas de um arquivo de backup") {
                    Graph.skipNextLock = true
                    importJson.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                }
                ActionRow(Icons.Outlined.TextSnippet, "Exportar como texto", "Gera um arquivo .txt legível com todas as notas") {
                    Graph.skipNextLock = true
                    exportTxt.launch("minhas-notas-$stamp.txt")
                }
            }

            SettingsGroup("Sobre") {
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
                }
                val active = repo.notes.count { !it.isDeleted }
                ActionRow(Icons.Outlined.Info, "Minhas Notas $version", "$active notas · ${repo.folders.size} pastas · funciona 100% offline", null)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (settingPin) {
        SetPinDialog(
            onDone = { pin ->
                settings.setPin(pin)
                toast(context, "PIN definido")
            },
            onDismiss = { settingPin = false },
        )
    }
    if (confirmDisablePin) {
        ConfirmDialog(
            title = "Desativar bloqueio?",
            message = "Qualquer pessoa com acesso ao celular poderá abrir suas notas.",
            confirmLabel = "Desativar",
            onConfirm = { settings.clearPin() },
            onDismiss = { confirmDisablePin = false },
        )
    }
    pendingRestore?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restaurar backup") },
            text = {
                Text(
                    "Mesclar mantém suas notas atuais e adiciona as do backup. " +
                        "Substituir apaga as notas atuais e deixa somente as do backup.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    restore(context, text, replace = false)
                    pendingRestore = null
                }) { Text("Mesclar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    restore(context, text, replace = true)
                    pendingRestore = null
                }) { Text("Substituir", color = MaterialTheme.colorScheme.error) }
            },
        )
    }
}

private fun restore(context: Context, text: String, replace: Boolean) {
    runCatching { Graph.repo.importBackup(text, replace) }
        .onSuccess { toast(context, "$it notas restauradas") }
        .onFailure { toast(context, "Arquivo de backup inválido") }
}

private fun writeToUri(context: Context, uri: Uri, text: String, success: String) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            ?: error("sem acesso")
    }
        .onSuccess { toast(context, success) }
        .onFailure { toast(context, "Não foi possível salvar o arquivo") }
}

private fun toast(context: Context, message: String) =
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) { content() }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String?, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

/** Define um PIN novo em duas etapas: digitar e confirmar. */
@Composable
private fun SetPinDialog(onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (first == null) "Crie um PIN" else "Confirme o PIN") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    message ?: if (first == null) "Use de 4 a 8 números." else "Digite o mesmo PIN novamente.",
                    color = if (message != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (pin.isEmpty()) " " else "•".repeat(pin.length),
                    fontSize = 32.sp,
                    letterSpacing = 6.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))
                PinPad(
                    onDigit = { d -> if (pin.length < 8) { pin += d; message = null } },
                    onBackspace = { pin = pin.dropLast(1) },
                    keySize = 60.dp,
                    spacing = 14.dp,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = pin.length >= 4,
                onClick = {
                    val chosen = first
                    if (chosen == null) {
                        first = pin
                        pin = ""
                    } else if (chosen == pin) {
                        onDone(pin)
                        onDismiss()
                    } else {
                        message = "Os PINs não conferem. Comece de novo."
                        first = null
                        pin = ""
                    }
                },
            ) { Text(if (first == null) "Continuar" else "Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
