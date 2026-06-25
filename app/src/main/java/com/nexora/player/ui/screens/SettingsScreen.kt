package com.nexora.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.player.R
import com.nexora.player.data.model.AppLanguage
import com.nexora.player.data.model.AppThemeMode

// ── Terms & Conditions text ──────────────────────────────────────────────────

private val TERMS_AND_CONDITIONS = """
Términos y Condiciones de NexoraPlayer

Última actualización: 25/06/2025

1. Aceptación
Al descargar o usar NexoraPlayer aceptas estos términos en su totalidad. Si no estás de acuerdo, desinstala la aplicación.

2. Uso permitido
NexoraPlayer está diseñado exclusivamente para reproducir archivos multimedia almacenados localmente en tu dispositivo. Queda prohibido usar la aplicación para reproducir, distribuir o almacenar contenido que infrinja derechos de autor o cualquier ley aplicable.

3. Contenido del usuario
El contenido multimedia que reproduces es de tu exclusiva responsabilidad. NexoraPlayer no almacena, distribuye, ni monetiza ningún archivo de tu dispositivo.

4. Búsqueda de letras en línea
La función de letras accede a servicios de terceros. Esta funcionalidad es opcional y está sujeta a la disponibilidad de dichos servicios externos. No garantizamos la exactitud o disponibilidad permanente de las letras obtenidas.

5. Modificaciones de la app
El desarrollador puede actualizar, modificar o descontinuar NexoraPlayer en cualquier momento sin previo aviso. Las actualizaciones pueden cambiar funcionalidades existentes.

6. Limitación de responsabilidad
NexoraPlayer se proporciona «tal cual», sin garantías de ningún tipo. El desarrollador no se hace responsable por pérdidas de datos, daños al dispositivo ni por el contenido de servicios de terceros.

7. Propiedad intelectual
El código fuente, diseño e interfaz de NexoraPlayer son propiedad de Ghost Developer. Queda prohibida su reproducción o distribución sin autorización expresa.

8. Cambios a estos términos
Nos reservamos el derecho de actualizar estos términos. Continuando el uso de la app tras una actualización, aceptas los nuevos términos.

Contacto: t.me/Gh0stDeveloper
""".trimIndent()

private val PRIVACY_POLICY = """
Política de Privacidad de NexoraPlayer

Última actualización: 2025

En NexoraPlayer tu privacidad es prioridad. Esta política describe qué datos se usan y cómo.

¿Qué datos NO recopilamos?
• Nombre, correo, teléfono ni ningún dato personal identificable.
• Historial de reproducción ni listas de canciones.
• Datos de ubicación.
• Información de contactos ni otras apps instaladas.

Acceso a archivos del dispositivo
La aplicación solicita permiso de lectura de almacenamiento únicamente para listar y reproducir los archivos de audio y video que eliges. Estos archivos nunca salen de tu dispositivo a través de nuestra app.

Letras en línea
Cuando activas la búsqueda de letras, se envía el título de la canción y el nombre del artista a servicios externos (no a nuestros servidores). No vinculamos estas consultas a tu identidad ni las almacenamos.

Servicio de ecualizador
El ecualizador opera completamente de forma local. No se transmite ningún audio a servidores externos.

Permisos requeridos
• Leer almacenamiento externo: para acceder a tus archivos de música y video.
• Internet (opcional): únicamente para búsqueda de letras si la activas.
• Notificaciones: para mostrar los controles de reproducción en la barra de notificaciones.

Cambios a esta política
Cualquier cambio será publicado dentro de la aplicación en futuras actualizaciones.

Contacto: t.me/Gh0stDeveloper
""".trimIndent()

// ── Main screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    themeMode: AppThemeMode,
    dynamicColor: Boolean,
    hiddenAudioCount: Int,
    onlineMusicSearchEnabled: Boolean,
    currentLanguage: AppLanguage,
    onThemeChange: (AppThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onOnlineMusicSearchChange: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onRestoreHiddenAudio: () -> Unit
) {
    val uriHandler  = LocalUriHandler.current
    val showTerms   = remember { mutableStateOf(false) }
    val showPrivacy = remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {

        // ── Page title ───────────────────────────────────────────────────────
        Text(
            text     = stringResource(R.string.settings_title),
            style    = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp, end = 20.dp)
        )

        Spacer(Modifier.height(4.dp))

        // ════════════════════════════════════════════════════════════════════
        // PERSONALIZACIÓN
        // ════════════════════════════════════════════════════════════════════

        SectionHeader("PERSONALIZACIÓN")

        SettingsGroup {
            // Language
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsIcon(
                        icon  = Icons.Filled.Language,
                        color = Color(0xFF007AFF)
                    )
                    Text(
                        stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = currentLanguage == AppLanguage.SYSTEM,
                        onClick  = { onLanguageChange(AppLanguage.SYSTEM) },
                        shape    = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) { Text(stringResource(AppLanguage.SYSTEM.labelRes), fontSize = 13.sp) }
                    SegmentedButton(
                        selected = currentLanguage == AppLanguage.SPANISH,
                        onClick  = { onLanguageChange(AppLanguage.SPANISH) },
                        shape    = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) { Text(stringResource(AppLanguage.SPANISH.labelRes), fontSize = 13.sp) }
                    SegmentedButton(
                        selected = currentLanguage == AppLanguage.ENGLISH,
                        onClick  = { onLanguageChange(AppLanguage.ENGLISH) },
                        shape    = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) { Text(stringResource(AppLanguage.ENGLISH.labelRes), fontSize = 13.sp) }
                }
            }

            RowDivider()

            // Theme
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsIcon(
                        icon  = Icons.Filled.Brightness4,
                        color = Color(0xFF5856D6)
                    )
                    Text(
                        stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.SYSTEM,
                        onClick  = { onThemeChange(AppThemeMode.SYSTEM) },
                        shape    = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) { Text(stringResource(R.string.settings_system), fontSize = 13.sp) }
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.LIGHT,
                        onClick  = { onThemeChange(AppThemeMode.LIGHT) },
                        shape    = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) { Text(stringResource(R.string.settings_light), fontSize = 13.sp) }
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.DARK,
                        onClick  = { onThemeChange(AppThemeMode.DARK) },
                        shape    = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) { Text(stringResource(R.string.settings_dark), fontSize = 13.sp) }
                }
            }

            RowDivider()

            // Dynamic color
            SettingsToggleRow(
                icon        = Icons.Filled.Palette,
                iconColor   = Color(0xFFFF9500),
                title       = stringResource(R.string.settings_dynamic_color),
                subtitle    = stringResource(R.string.settings_dynamic_color_desc),
                checked     = dynamicColor,
                onCheckedChange = onDynamicColorChange
            )
        }

        // ════════════════════════════════════════════════════════════════════
        // BIBLIOTECA
        // ════════════════════════════════════════════════════════════════════

        SectionHeader("BIBLIOTECA")

        SettingsGroup {
            // Online search
            SettingsToggleRow(
                icon        = Icons.Filled.Search,
                iconColor   = Color(0xFF30B0C7),
                title       = "Búsqueda en línea",
                subtitle    = if (onlineMusicSearchEnabled)
                                  "Búsqueda online activada."
                              else
                                  "Solo se usarán datos locales.",
                checked     = onlineMusicSearchEnabled,
                onCheckedChange = onOnlineMusicSearchChange
            )

            RowDivider()

            // Hidden audio
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsIcon(
                        icon  = Icons.Filled.VisibilityOff,
                        color = Color(0xFFFF3B30)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_library_privacy),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            stringResource(R.string.settings_hidden_audio_count, hiddenAudioCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    stringResource(R.string.settings_hidden_audio_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 44.dp)
                )
                OutlinedButton(
                    onClick  = onRestoreHiddenAudio,
                    enabled  = hiddenAudioCount > 0,
                    modifier = Modifier.padding(start = 44.dp),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.settings_restore_hidden), fontSize = 13.sp)
                }
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // SOBRE LA APP
        // ════════════════════════════════════════════════════════════════════

        SectionHeader("SOBRE LA APP")

        SettingsGroup {
            // Developer
            SettingsInfoRow(
                icon      = Icons.Filled.Person,
                iconColor = Color(0xFF34C759),
                title     = "Desarrollador",
                value     = "Ghost Developer"
            )

            RowDivider()

            // App info
            SettingsInfoRow(
                icon      = Icons.Filled.Info,
                iconColor = Color(0xFF007AFF),
                title     = "NexoraPlayer",
                value     = stringResource(R.string.settings_free_notice)
            )

            RowDivider()

            // GitHub
            SettingsLinkRow(
                icon      = Icons.Filled.Code,
                iconColor = Color(0xFF1C1C1E),
                title     = stringResource(R.string.settings_github),
                subtitle  = "github.com/Gh0stDeveloper",
                onClick   = { uriHandler.openUri("https://github.com/Gh0stDeveloper") }
            )

            RowDivider()

            // Telegram
            SettingsLinkRow(
                icon      = Icons.AutoMirrored.Filled.OpenInNew,
                iconColor = Color(0xFF007AFF),
                title     = stringResource(R.string.settings_profile),
                subtitle  = "t.me/Gh0stDeveloper",
                onClick   = { uriHandler.openUri("https://t.me/Gh0stDeveloper") }
            )
        }

        // ════════════════════════════════════════════════════════════════════
        // LEGAL
        // ════════════════════════════════════════════════════════════════════

        SectionHeader("LEGAL")

        SettingsGroup {
            // Terms & Conditions
            SettingsExpandableRow(
                icon      = Icons.Filled.Gavel,
                iconColor = Color(0xFF5856D6),
                title     = "Términos y condiciones",
                expanded  = showTerms.value,
                onToggle  = { showTerms.value = !showTerms.value },
                content   = TERMS_AND_CONDITIONS
            )

            RowDivider()

            // Privacy Policy
            SettingsExpandableRow(
                icon      = Icons.Filled.Lock,
                iconColor = Color(0xFF34C759),
                title     = "Política de privacidad",
                expanded  = showPrivacy.value,
                onToggle  = { showPrivacy.value = !showPrivacy.value },
                content   = PRIVACY_POLICY
            )
        }

        // ════════════════════════════════════════════════════════════════════
        // STATUS
        // ════════════════════════════════════════════════════════════════════

        SectionHeader("ESTADO")

        SettingsGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(R.string.settings_status_title),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.settings_status_line1),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.settings_status_line2),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.settings_status_line3),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

// ── Reusable components ──────────────────────────────────────────────────────

/** Small all-caps section label above a group, iOS style. */
@Composable
private fun SectionHeader(label: String) {
    Text(
        text     = label,
        style    = MaterialTheme.typography.labelSmall.copy(
            fontWeight   = FontWeight.Medium,
            letterSpacing = 0.8.sp
        ),
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 32.dp, top = 22.dp, bottom = 6.dp, end = 20.dp)
    )
}

/** Rounded card that groups related settings rows. */
@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(16.dp),
        color     = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/** Inset divider between rows (doesn't touch the left edge, like iOS). */
@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(start = 58.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f)
    )
}

/** Colored square icon (iOS-style app icon look). */
@Composable
private fun SettingsIcon(icon: ImageVector, color: Color) {
    Box(
        modifier         = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = Color.White,
            modifier           = Modifier.size(17.dp)
        )
    }
}

/** Row with a switch on the right. */
@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsIcon(icon = icon, color = iconColor)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/** Row with a trailing value text (non-tappable info). */
@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsIcon(icon = icon, color = iconColor)
        Text(
            title,
            style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** Row with a chevron that opens a URL or triggers an action. */
@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsIcon(icon = icon, color = iconColor)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector        = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
            modifier           = Modifier.size(20.dp)
        )
    }
}

/** Row that expands to show a block of text (T&C, Privacy Policy). */
@Composable
private fun SettingsExpandableRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header row (tappable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsIcon(icon = icon, color = iconColor)
            Text(
                title,
                style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            // Chevron rotates to indicate expanded state
            Icon(
                imageVector        = Icons.Filled.ChevronRight,
                contentDescription = if (expanded) "Contraer" else "Expandir",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                modifier           = Modifier
                    .size(20.dp)
                    // Simple visual state — down when expanded
                    .then(
                        if (expanded) Modifier.padding(top = 0.dp) else Modifier
                    )
            )
        }

        // Expandable content
        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text      = content,
                    style     = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
