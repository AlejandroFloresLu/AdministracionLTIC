package ec.edu.puce.scantech.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ec.edu.puce.scantech.R
import ec.edu.puce.scantech.data.local.SyncStatus
import ec.edu.puce.scantech.data.model.ActivoLTIC
import ec.edu.puce.scantech.data.model.EstadoOperativo
import ec.edu.puce.scantech.ui.components.EmptyState
import ec.edu.puce.scantech.ui.components.ErrorState
import ec.edu.puce.scantech.ui.components.LoadingIndicator
import ec.edu.puce.scantech.ui.theme.StatusAdvertenciaColor
import ec.edu.puce.scantech.ui.theme.StatusCriticoColor
import ec.edu.puce.scantech.ui.theme.StatusOperativoColor
import ec.edu.puce.scantech.ui.theme.SyncPendingColor
import ec.edu.puce.scantech.ui.theme.SyncSyncedColor

/**
 * Pantalla principal del Catálogo de Inventario de ScanTech LTIC.
 *
 * Implementa de manera estricta las actividades del Documento Maestro y las buenas prácticas del curso:
 * - Tarea #9: Búsqueda por texto con State Hoisting (el estado proviene del ViewModel / jerarquía superior).
 * - Tarea #10: Filtros por categoría mediante [LazyRow] con [FilterChip] (Prohibido Row con forEach).
 * - UDF & Sealed Interface: Reacción pasiva al [InventoryUiState] mediante when exhaustivo.
 * - Rendimiento de listas: Listado principal con [LazyColumn] para reciclaje de vistas a 60 FPS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDashboardScreen(
    viewModel: InventoryViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val categories = listOf(
        stringResource(R.string.category_all),
        stringResource(R.string.category_laptops),
        stringResource(R.string.category_servers),
        stringResource(R.string.category_networks),
        stringResource(R.string.category_peripherals)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.inventory_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToScanner,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = stringResource(R.string.scan_action)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tarea #9: Barra de búsqueda con State Hoisting estricto
            SearchBarSection(
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onClearSearch = viewModel::clearSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tarea #10: Filtro horizontal optimizado con LazyRow y FilterChip
            CategoryFilterSection(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelect = viewModel::onCategorySelect,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Contenido dinámico reactivo a la máquina de estados InventoryUiState
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is InventoryUiState.Idle -> {
                        // Estado inicial pasivo
                        LoadingIndicator(mensaje = stringResource(R.string.loading_assets))
                    }

                    is InventoryUiState.Loading -> {
                        LoadingIndicator(mensaje = stringResource(R.string.loading_assets))
                    }

                    is InventoryUiState.Error -> {
                        ErrorState(
                            mensaje = state.mensaje,
                            onRetry = { viewModel.loadActivos() }
                        )
                    }

                    is InventoryUiState.Success -> {
                        if (state.filteredActivos.isEmpty()) {
                            EmptyState(
                                mensaje = stringResource(R.string.empty_inventory_description),
                                onResetFilters = {
                                    viewModel.clearSearchQuery()
                                    viewModel.onCategorySelect("Todos")
                                }
                            )
                        } else {
                            // Lista reciclada de alto rendimiento: LazyColumn (nunca Column + forEach)
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 88.dp // Espacio para el FAB
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = state.filteredActivos,
                                    key = { it.id } // Clave única para recomposiciones inteligentes
                                ) { activo ->
                                    ActivoCard(
                                        activo = activo,
                                        onClick = { onNavigateToDetail(activo.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarea #9: Componente de Búsqueda con State Hoisting.
 * Cumple la regla de NO poseer 'remember' mutable interno para la consulta.
 */
@Composable
fun SearchBarSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = stringResource(R.string.search_placeholder),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = onClearSearch) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = stringResource(R.string.clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

/**
 * Tarea #10: Lista horizontal de Filtros por Categoría.
 * OBLIGATORIO: Implementado con [LazyRow] para garantizar reciclaje de memoria a 60 FPS.
 * Prohibido usar Row con forEach.
 */
@Composable
fun CategoryFilterSection(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = categories,
            key = { it }
        ) { category ->
            val isSelected = category.equals(selectedCategory, ignoreCase = true)

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelect(category) },
                label = {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

/**
 * Card individual del Activo para el LazyColumn del Catálogo General.
 * Refleja fielmente el wireframe de la diapositiva 5:
 * Row: Icono representativo + Texto del activo + Badges de estado operativo y sincronización.
 */
@Composable
fun ActivoCard(
    activo: ActivoLTIC,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de la categoría en contenedor tonal circular
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(activo.categoria),
                    contentDescription = activo.categoria,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información descriptiva del activo
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = activo.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Serie: ${activo.serie} • ID: ${activo.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Indicador de sincronización Offline-First
                SyncBadge(status = activo.syncStatus)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Badge semántico de Estado Operativo (Operativo / Advertencia / Crítico)
            EstadoOperativoBadge(estado = activo.estadoOperativo)
        }
    }
}

/**
 * Badge visual que indica el estado operativo (Operativo / Advertencia / Crítico).
 */
@Composable
fun EstadoOperativoBadge(
    estado: EstadoOperativo,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, labelRes) = when (estado) {
        EstadoOperativo.OPERATIVO -> Triple(
            StatusOperativoColor.copy(alpha = 0.15f),
            StatusOperativoColor,
            R.string.status_operativo
        )
        EstadoOperativo.ADVERTENCIA -> Triple(
            StatusAdvertenciaColor.copy(alpha = 0.2f),
            StatusAdvertenciaColor,
            R.string.status_advertencia
        )
        EstadoOperativo.CRITICO -> Triple(
            StatusCriticoColor.copy(alpha = 0.2f),
            StatusCriticoColor,
            R.string.status_critico
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(labelRes),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Badge que muestra el estado de sincronización local (Room vs Server).
 */
@Composable
fun SyncBadge(
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    val (icon, color, textRes) = when (status) {
        SyncStatus.SYNCED -> Triple(
            Icons.Rounded.CloudDone,
            SyncSyncedColor,
            R.string.sync_status_synced
        )
        SyncStatus.PENDING_CREATE -> Triple(
            Icons.Rounded.CloudUpload,
            SyncPendingColor,
            R.string.sync_status_pending_create
        )
        SyncStatus.PENDING_UPDATE -> Triple(
            Icons.Rounded.CloudUpload,
            SyncPendingColor,
            R.string.sync_status_pending_update
        )
        SyncStatus.PENDING_DELETE -> Triple(
            Icons.Rounded.CloudUpload,
            MaterialTheme.colorScheme.error,
            R.string.sync_status_pending_delete
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * Retorna el icono M3 adecuado para cada categoría de hardware LTIC.
 */
fun getCategoryIcon(categoria: String): ImageVector {
    return when (categoria.trim().lowercase()) {
        "laptops", "laptop" -> Icons.Rounded.Laptop
        "servidores", "servidor" -> Icons.Rounded.Dns
        "redes", "red" -> Icons.Rounded.Router
        "periféricos", "perifericos" -> Icons.Rounded.Devices
        else -> Icons.Rounded.Computer
    }
}
