package ec.edu.puce.scantech.ui.detail

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ec.edu.puce.scantech.R
import ec.edu.puce.scantech.data.local.SyncStatus
import ec.edu.puce.scantech.data.model.EstadoOperativo
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.domain.validacion.ValidadorActivo
import ec.edu.puce.scantech.ui.components.EmptyState
import ec.edu.puce.scantech.ui.components.ErrorState
import ec.edu.puce.scantech.ui.components.LoadingIndicator
import ec.edu.puce.scantech.ui.inventory.EstadoOperativoBadge
import ec.edu.puce.scantech.ui.inventory.SyncBadge
import ec.edu.puce.scantech.ui.theme.ScanTechTheme

/**
 * [RDA-1 Tema 4: Persistencia con rememberSaveable y Ciclo de Vida]
 * [RDA-1 Tema 5: UI Declarativa - State Hoisting (Stateful vs Stateless)]
 * [RDA-2 Tema 7 y 8: Sistema Material Design 3, Jerarquía Tipográfica y Tokens Semánticos]
 * [RDA-3 Temas 9 y 10: MVVM + UDF con Sealed Interface y ViewModel Desacoplado]
 * [RDA-3 Temas 11 y 12: Offline-First con Room como SSOT y Baja Lógica]
 *
 * Pantalla de Ficha Técnica del Activo Tecnológico (Módulo de Detalle & Auditoría).
 *
 * Conceptos Teóricos Aplicados:
 * 1. Desacoplamiento de ViewModel (SoC - Tarea #58): La pantalla consume de forma primaria su
 *    propio [AssetDetailViewModel], eliminando el acoplamiento cruzado con `InventoryViewModel`.
 * 2. Máquina de Estados Finita: La UI es función pura del estado [AssetDetailUiState] evaluado de forma
 *    exhaustiva (`when (state)`: Loading, Error, Success).
 * 3. State Hoisting (RDA-1 Tema 5): Se separa el contenedor [AssetDetailScreen] (stateful) del
 *    contenido reutilizable [AssetDetailContent] (stateless).
 * 4. Resiliencia ante Rotación (RDA-1 Tema 4): El borrador de observaciones vive en el estado del
 *    [AssetDetailViewModel] respaldado por SavedStateHandle (sobrevive rotación y muerte del proceso).
 * 5. Material You & Tokens Semánticos (RDA-2 Tema 7 y 8): Cero colores fijos; todos los contrastes
 *    se calculan sobre `MaterialTheme.colorScheme`.
 */
@Composable
fun AssetDetailScreen(
    assetId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AssetDetailViewModel = viewModel(
        factory = AssetDetailViewModel.provideFactory(assetId),
        key = "AssetDetailViewModel_$assetId"
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Feedback del guardado de observaciones (evento de un solo uso → Snackbar)
    val snackbarHostState = remember { SnackbarHostState() }
    val mensajeGuardadas = stringResource(R.string.observations_saved)
    val plantillaError = stringResource(R.string.observations_save_error)
    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            val mensaje = when (evento) {
                ObservacionesEvento.Guardadas -> mensajeGuardadas
                is ObservacionesEvento.Error -> plantillaError.format(evento.mensaje)
            }
            snackbarHostState.showSnackbar(mensaje)
        }
    }

    when (val state = uiState) {
        is AssetDetailUiState.Loading -> {
            Scaffold { innerPadding ->
                LoadingIndicator(
                    modifier = Modifier.padding(innerPadding),
                    mensaje = "Cargando ficha técnica del hardware..."
                )
            }
        }

        is AssetDetailUiState.Error -> {
            Scaffold(
                topBar = {
                    DetailTopAppBar(
                        title = stringResource(R.string.asset_detail_short_title),
                        onNavigateBack = onNavigateBack
                    )
                }
            ) { innerPadding ->
                ErrorState(
                    mensaje = state.mensaje,
                    onRetry = onNavigateBack,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        is AssetDetailUiState.Success -> {
            AssetDetailContent(
                asset = state.asset,
                observaciones = state.observacionesDraft,
                puedeGuardarObservaciones = state.hayCambiosObservaciones && !state.isSavingObservaciones,
                guardandoObservaciones = state.isSavingObservaciones,
                snackbarHostState = snackbarHostState,
                onNavigateBack = onNavigateBack,
                onNavigateToEdit = { onNavigateToEdit(state.asset.id) },
                onObservacionesChange = viewModel::onObservacionesChange,
                onSaveObservations = viewModel::guardarObservaciones,
                onDiscardObservations = {
                    viewModel.descartarObservaciones()
                    onNavigateBack()
                },
                onDeleteConfirmed = {
                    viewModel.eliminarActivo(onSuccess = onNavigateBack)
                },
                modifier = modifier
            )
        }
    }
}

/**
 * Ficha Técnica del activo — composable **Stateless** (Elevación de Estado).
 *
 * Solo dibuja lo que recibe y notifica eventos hacia arriba; no conoce la implementación del ViewModel.
 * Agrupa todos los campos del hardware en tarjetas semánticas: identificación, especificaciones técnicas,
 * ubicación física, inventario y registro de observaciones de auditoría.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailContent(
    asset: Asset,
    observaciones: String,
    puedeGuardarObservaciones: Boolean,
    guardandoObservaciones: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onObservacionesChange: (String) -> Unit,
    onSaveObservations: () -> Unit,
    onDiscardObservations: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    estadoOperativo: EstadoOperativo? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.asset_detail_short_title),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Botón para navegar al formulario en modo edición (Tarea #55)
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.edit_asset),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Botón para dar de baja lógica al activo (RDA-3 Tema 11 y 12)
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.delete_asset)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta superior: Identificación, QR, Estado Operativo y Estado de Sincronización
            AssetHeaderCard(
                asset = asset,
                estadoOperativo = estadoOperativo
            )

            // Tarjeta de Características Técnicas
            DetailSectionCard(
                title = stringResource(R.string.detail_section_specs),
                icon = Icons.Rounded.Memory
            ) {
                Text(
                    text = asset.technicalSpecs,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DetailFieldRow(
                    label = stringResource(R.string.detail_label_color),
                    value = asset.color.orNotRegistered()
                )
                DetailFieldRow(
                    label = stringResource(R.string.detail_label_dimensions),
                    value = asset.dimensions.orNotRegistered()
                )
            }

            // Tarjeta de Ubicación Física
            DetailSectionCard(
                title = stringResource(R.string.detail_section_location),
                icon = Icons.Rounded.Place
            ) {
                DetailFieldRow(
                    label = stringResource(R.string.detail_label_block),
                    value = asset.block.orNotRegistered()
                )
                DetailFieldRow(
                    label = stringResource(R.string.detail_label_shelf),
                    value = asset.shelf.orNotRegistered()
                )
                DetailFieldRow(
                    label = stringResource(R.string.detail_label_locker),
                    value = asset.locker.orNotRegistered()
                )
                // Caso de práctica 6: columna nueva de Room (versión 3)
                DetailFieldRow(
                    label = stringResource(R.string.detail_label_responsable),
                    value = asset.responsable
                )
            }

            // Tarjeta de Inventario y Disponibilidad
            DetailSectionCard(
                title = stringResource(R.string.detail_section_inventory),
                icon = Icons.Rounded.Inventory2
            ) {
                DetailFieldRow(
                    label = stringResource(R.string.detail_label_category),
                    value = asset.category
                )
                DetailFieldRow(
                    label = stringResource(R.string.detail_label_units),
                    value = "${asset.units} unidades"
                )
            }

            // Sección de Observaciones de Auditoría Física
            ObservacionesSection(
                observaciones = observaciones,
                puedeGuardar = puedeGuardarObservaciones,
                guardando = guardandoObservaciones,
                onObservacionesChange = onObservacionesChange,
                onGuardar = onSaveObservations,
                onDescartar = onDiscardObservations
            )
        }
    }

    // Diálogo de confirmación para borrado lógico (RDA-3 Temas 11 y 12)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Dar de baja activo") },
            text = {
                Text(text = "¿Estás seguro de que deseas dar de baja el activo '${asset.name}'? Esta acción se registrará localmente y se sincronizará con el servidor.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = "Dar de baja")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = "Cancelar")
                }
            }
        )
    }
}

/** Tarjeta superior: nombre, código QR y estados del activo. */
@Composable
private fun AssetHeaderCard(
    asset: Asset,
    estadoOperativo: EstadoOperativo?,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = asset.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.QrCode2,
                    contentDescription = stringResource(R.string.detail_label_qr),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = asset.qrCode,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (estadoOperativo != null) {
                    EstadoOperativoBadge(estado = estadoOperativo)
                }
                SyncBadge(status = asset.syncStatus)
            }
        }
    }
}

/** Tarjeta con título e ícono que agrupa un conjunto de campos relacionados. */
@Composable
private fun DetailSectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

/**
 * Campo multilínea de observaciones físicas con contador de caracteres y las acciones
 * Descartar y Guardar. Stateless: el texto y el estado de guardado vienen del ViewModel.
 */
@Composable
private fun ObservacionesSection(
    observaciones: String,
    puedeGuardar: Boolean,
    guardando: Boolean,
    onObservacionesChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onDescartar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = observaciones,
            onValueChange = onObservacionesChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.observations_label)) },
            placeholder = {
                Text(
                    text = stringResource(R.string.observations_placeholder),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            supportingText = {
                Text(
                    text = stringResource(
                        R.string.observations_counter,
                        observaciones.length,
                        ValidadorActivo.MAX_OBSERVACIONES
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDescartar,
                enabled = !guardando,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = stringResource(R.string.discard_changes))
            }

            Button(
                onClick = onGuardar,
                enabled = puedeGuardar,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.observations_save),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Barra de navegación superior para pantallas de detalle genéricas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopAppBar(
    title: String,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/** Fila etiqueta / valor dentro de una tarjeta de la ficha. */
@Composable
fun DetailFieldRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

/** Texto a mostrar cuando un campo opcional está vacío. */
@Composable
private fun String?.orNotRegistered(): String =
    if (isNullOrBlank()) stringResource(R.string.detail_not_registered) else this

@Preview(name = "Ficha técnica · claro", showBackground = true, heightDp = 1400)
@Preview(
    name = "Ficha técnica · oscuro",
    showBackground = true,
    heightDp = 1400,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AssetDetailContentPreview() {
    ScanTechTheme {
        AssetDetailContent(
            asset = Asset(
                id = "LTIC-000123",
                qrCode = "LTIC-000123",
                name = "Servidor HP ProLiant DL360 Gen10",
                category = "Servidores",
                technicalSpecs = "Marca: HP\nModelo: ProLiant DL360 Gen10\nSerie: XYZ987SER\n" +
                    "RAM: 64GB ECC\nCPU: Dual Intel Xeon Silver 4210R",
                color = "Negro",
                dimensions = "1U · 43.5 × 70.7 × 4.3 cm",
                block = "Data Center",
                shelf = "Rack 04",
                locker = "U12",
                units = 1,
                observations = "Fuente redundante descalibrada.",
                syncStatus = SyncStatus.PENDING_UPDATE
            ),
            observaciones = "Fuente redundante descalibrada.",
            puedeGuardarObservaciones = false,
            guardandoObservaciones = false,
            estadoOperativo = EstadoOperativo.CRITICO,
            onNavigateBack = {},
            onNavigateToEdit = {},
            onObservacionesChange = {},
            onSaveObservations = {},
            onDiscardObservations = {},
            onDeleteConfirmed = {}
        )
    }
}

@Preview(name = "Campo de observaciones · claro", showBackground = true)
@Preview(
    name = "Campo de observaciones · oscuro",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ObservacionesSectionPreview() {
    ScanTechTheme {
        Surface {
            ObservacionesSection(
                observaciones = "Empaque abierto; bisagra izquierda floja.",
                puedeGuardar = true,
                guardando = false,
                onObservacionesChange = {},
                onGuardar = {},
                onDescartar = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
