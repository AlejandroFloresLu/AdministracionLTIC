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
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Activo #${asset.qrCode}",
                        fontWeight = FontWeight.Bold
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
            Text(
                text = stringResource(R.string.observations_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
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

@Composable
fun DetailFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
