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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import ec.edu.puce.scantech.domain.model.Asset
import ec.edu.puce.scantech.ui.components.EmptyState
import ec.edu.puce.scantech.ui.components.ErrorState
import ec.edu.puce.scantech.ui.components.LoadingIndicator
import ec.edu.puce.scantech.ui.inventory.SyncBadge

/**
 * [RDA-1 Tema 4: Persistencia con rememberSaveable y Ciclo de Vida]
 * [RDA-1 Tema 5: UI Declarativa - State Hoisting (Stateful vs Stateless)]
 * [RDA-3 Temas 9 y 10: MVVM + UDF con Sealed Interface]
 *
 * Pantalla de Ficha Técnica del Activo Tecnológico.
 *
 * Conceptos Teóricos Aplicados:
 * 1. Desacoplamiento de ViewModel: La pantalla consume exclusivamente su propio [AssetDetailViewModel],
 *    eliminando el acoplamiento cruzado con `InventoryViewModel`.
 * 2. Máquina de Estados Finita: La UI es función pura del estado [AssetDetailUiState] evaluado de forma
 *    exhaustiva con `when(state)`.
 * 3. State Hoisting: Se separa el contenedor [AssetDetailScreen] (stateful) del contenido [AssetDetailContent] (stateless).
import ec.edu.puce.scantech.data.local.SyncStatus
import ec.edu.puce.scantech.data.model.EstadoOperativo
import ec.edu.puce.scantech.ui.inventory.EstadoOperativoBadge
import ec.edu.puce.scantech.ui.inventory.InventoryViewModel
import ec.edu.puce.scantech.ui.theme.ScanTechTheme

/**
 * Ficha Técnica del activo (#6) — composable **Stateful**.
 *
 * Obtiene el activo desde el ViewModel, guarda el borrador de observaciones con
 * [rememberSaveable] y delega todo el dibujo en [AssetDetailContent] (elevación de estado).
 *
 * [onEditar] y [onEliminar] son opcionales: mientras sean `null` los botones se muestran
 * deshabilitados. Se conectan cuando existan el Formulario (#55) y el diálogo de
 * confirmación de baja (#13).
 */
@Composable
fun AssetDetailScreen(
    assetId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: AssetDetailViewModel = viewModel(
        factory = AssetDetailViewModel.provideFactory(context, assetId),
        key = "AssetDetailViewModel_$assetId"
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                        title = stringResource(R.string.asset_detail_title),
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
                onNavigateBack = onNavigateBack,
                onNavigateToEdit = { onNavigateToEdit(state.asset.id) },
                onSaveObservations = { nuevasObs ->
                    viewModel.updateObservaciones(nuevasObs)
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
 * Componente Stateless con elevación de estado para la Ficha Técnica.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailContent(
    asset: Asset,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onSaveObservations: (String) -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Persistencia del borrador de observaciones ante rotaciones de pantalla (RDA-1 Tema 4)
    var observacionesText by rememberSaveable(asset.id) {
        mutableStateOf(asset.observations ?: "")
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    modifier: Modifier = Modifier,
    onEditar: ((assetId: String) -> Unit)? = null,
    onEliminar: ((assetId: String) -> Unit)? = null
) {
    val asset = viewModel.getAssetById(assetId)

    if (asset == null) {
        AssetNotFoundContent(
            assetId = assetId,
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )
        return
    }

    val estadoOperativo = viewModel.getActivoById(assetId)?.estadoOperativo

    // Borrador de observaciones: sobrevive a la rotación de pantalla.
    var observaciones by rememberSaveable(asset.id) {
        mutableStateOf(asset.observations.orEmpty())
    }

    AssetDetailContent(
        asset = asset,
        estadoOperativo = estadoOperativo,
        observaciones = observaciones,
        onObservacionesChange = { observaciones = it },
        onGuardarObservaciones = {
            viewModel.updateObservaciones(asset.id, observaciones)
            onNavigateBack()
        },
        onDescartar = onNavigateBack,
        onNavigateBack = onNavigateBack,
        onEditar = onEditar?.let { editar -> { editar(asset.id) } },
        onEliminar = onEliminar?.let { eliminar -> { eliminar(asset.id) } },
        modifier = modifier
    )
}

/**
 * Ficha Técnica del activo — composable **Stateless**.
 *
 * Solo dibuja lo que recibe y avisa hacia arriba con eventos; no conoce al ViewModel.
 * Agrupa todos los campos del activo en tarjetas: identificación, características
 * técnicas, ubicación e inventario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailContent(
    asset: Asset,
    estadoOperativo: EstadoOperativo?,
    observaciones: String,
    onObservacionesChange: (String) -> Unit,
    onGuardarObservaciones: () -> Unit,
    onDescartar: () -> Unit,
    onNavigateBack: () -> Unit,
    onEditar: (() -> Unit)?,
    onEliminar: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Activo #${asset.qrCode}",
                        fontWeight = FontWeight.Bold
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
                            contentDescription = "Editar Activo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Botón para dar de baja lógica al activo
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Dar de baja",
                            tint = MaterialTheme.colorScheme.error
                    IconButton(
                        onClick = { onEditar?.invoke() },
                        enabled = onEditar != null
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.edit_asset)
                        )
                    }
                    IconButton(
                        onClick = { onEliminar?.invoke() },
                        enabled = onEliminar != null,
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
            // Ficha Técnica con especificaciones completas del hardware
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.specifications_header),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        SyncBadge(status = asset.syncStatus)
                    }

                    DetailFieldRow(label = "Nombre:", value = asset.name)
                    DetailFieldRow(label = "Código QR:", value = asset.qrCode)
                    DetailFieldRow(label = "Categoría:", value = asset.category)
                    DetailFieldRow(label = "Especificaciones:", value = asset.technicalSpecs)

                    if (!asset.color.isNullOrBlank()) {
                        DetailFieldRow(label = "Color:", value = asset.color)
                    }
                    if (!asset.dimensions.isNullOrBlank()) {
                        DetailFieldRow(label = "Dimensiones:", value = asset.dimensions)
                    }

                    DetailFieldRow(label = "Unidades:", value = "${asset.units} unidades")

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = asset.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Campo interactivo para registrar observaciones físicas
            AssetHeaderCard(asset = asset, estadoOperativo = estadoOperativo)

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
            }

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
                    value = asset.units.toString()
                )
            }

            ObservacionesSection(
                observaciones = observaciones,
                onObservacionesChange = onObservacionesChange,
                onGuardar = onGuardarObservaciones,
                onDescartar = onDescartar
            )
        }
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

            OutlinedTextField(
                value = observacionesText,
                onValueChange = { observacionesText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.observations_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )

            // Botón para actualizar observaciones
            Button(
                onClick = { onSaveObservations(observacionesText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Guardar Observaciones",
                    fontWeight = FontWeight.Bold
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

    // Diálogo de confirmación para borrado lógico (RDA-3 Temas 11 y 12)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Dar de baja activo") },
            text = {
                Text(text = "¿Estás seguro de que deseas dar de baja el activo '${asset.name}'? Esta acción se encolará y sincronizará con el servidor.")
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

/** Campo de observaciones físicas con las acciones Descartar y Guardar. */
@Composable
private fun ObservacionesSection(
    observaciones: String,
    onObservacionesChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onDescartar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.observations_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = observaciones,
            onValueChange = onObservacionesChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.observations_placeholder),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDescartar,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = stringResource(R.string.discard_changes))
            }

            Button(
                onClick = onGuardar,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.save_changes),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Estado vacío cuando el id recibido no corresponde a ningún activo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetNotFoundContent(
    assetId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.asset_detail_short_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        EmptyState(
            mensaje = stringResource(R.string.asset_not_found, assetId),
            modifier = Modifier.padding(innerPadding)
        )
    }
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
            estadoOperativo = EstadoOperativo.CRITICO,
            observaciones = "Fuente redundante descalibrada.",
            onObservacionesChange = {},
            onGuardarObservaciones = {},
            onDescartar = {},
            onNavigateBack = {},
            onEditar = {},
            onEliminar = {}
        )
    }
}
