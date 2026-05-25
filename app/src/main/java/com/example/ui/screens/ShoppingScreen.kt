package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.ShoppingListItem
import com.example.ui.ShoppingViewModel
import com.example.ui.utils.PrintUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HoverIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    icon: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.2f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .hoverable(interactionSource),
        enabled = enabled,
        interactionSource = interactionSource,
        colors = colors,
        content = icon
    )
}

@Composable
fun HoverRectangularIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.15f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .hoverable(interactionSource)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) containerColor
                else containerColor.copy(alpha = 0.3f)
            ),
        enabled = enabled,
        interactionSource = interactionSource,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Transparent,
            contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.4f),
            disabledContainerColor = Color.Transparent,
            disabledContentColor = contentColor.copy(alpha = 0.4f)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    viewModel: ShoppingViewModel,
    isDarkModeActive: Boolean,
    onToggleDarkMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val productName by viewModel.productName.collectAsState()
    val productQuantity by viewModel.productQuantity.collectAsState()
    val productPrice by viewModel.productPrice.collectAsState()
    val activeItems by viewModel.activeItems.collectAsState()
    val savedLists by viewModel.savedLists.collectAsState()
    val suggestions by viewModel.filteredSuggestions.collectAsState()
    val totalAmount by viewModel.totalActiveAmount.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var listNameToSave by remember { mutableStateOf("") }
    var showSavedListsSection by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingListItem?>(null) }
    var showNewListConfirmDialog by remember { mutableStateOf(false) }
    var proceedToClearAfterSaving by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000) // Beautiful 2 seconds loading
        showSplash = false
    }

    if (showSplash) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkModeActive) Color(0xFF0D3C6C) else Color(0xFFECF4FC)),
            contentAlignment = Alignment.Center
        ) {
            BoasComprasLogo(
                isDark = isDarkModeActive,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            // High Density Header matching the web app styling
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BoasComprasEmblem(
                            isDark = isDarkModeActive,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Boas Compras",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.testTag("app_title")
                            )
                            Text(
                                text = "por André Copelli",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    // Circular buttons row on the right of header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Dark Mode toggle button (modo escuro)
                        HoverIconButton(
                            onClick = onToggleDarkMode,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f))
                                .testTag("dark_mode_toggle"),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            )
                        ) {
                            Icon(
                                imageVector = if (isDarkModeActive) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkModeActive) "Modo Claro" else "Modo Escuro",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 1b. Novo Botão Redondo de Criar Nova Lista
                        HoverIconButton(
                            onClick = {
                                if (activeItems.isNotEmpty()) {
                                    showNewListConfirmDialog = true
                                } else {
                                    Toast.makeText(context, "Sua lista já está vazia e pronta!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f))
                                .testTag("new_list_button"),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Criar Nova Lista",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // 2. Saved Lists history toggle button (acesso às listas salvas)
                        HoverIconButton(
                            onClick = { showSavedListsSection = !showSavedListsSection },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (showSavedListsSection) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                                )
                                .testTag("saved_lists_expandable"),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = if (showSavedListsSection) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Listas Salvas",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            val context = LocalContext.current
            // Elegant M3 Lavender Bottom Footers
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 20.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TOTAL DA COMPRA",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                        Text(
                            text = viewModel.formatCurrency(totalAmount),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("total_price_label")
                        )
                    }

                    // Single row of 4 rectangular buttons (symbols only, elongated rounded corners)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Salvar (Save)
                        HoverRectangularIconButton(
                            onClick = {
                                if (activeItems.isNotEmpty()) {
                                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    listNameToSave = "Compras - " + sdf.format(Date())
                                    showSaveDialog = true
                                } else {
                                    Toast.makeText(context, "Sua lista está vazia para salvar!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            icon = Icons.Default.Save,
                            contentDescription = "Salvar Lista",
                            enabled = activeItems.isNotEmpty(),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_list_button_footer")
                        )

                        // 2. Imprimir (Print)
                        HoverRectangularIconButton(
                            onClick = {
                                if (activeItems.isNotEmpty()) {
                                    PrintUtils.printShoppingList(
                                        context = context,
                                        listName = "Boas Compras",
                                        items = activeItems,
                                        totalAmount = totalAmount
                                    )
                                } else {
                                    Toast.makeText(context, "Sua lista está vazia para imprimir!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            icon = Icons.Default.Print,
                            contentDescription = "Imprimir Lista",
                            enabled = activeItems.isNotEmpty(),
                            modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .testTag("print_list_button_footer")
                        )

                        // 3. Compartilhar (Share)
                        HoverRectangularIconButton(
                            onClick = {
                                if (activeItems.isNotEmpty()) {
                                    val shareText = buildString {
                                        append("🛒 *Minha Lista de Compras: Boas Compras*\n\n")
                                        activeItems.forEachIndexed { i, item ->
                                            val status = if (item.isPurchased) "✅" else "⬜"
                                            val qtyStr = viewModel.formatQuantity(item.quantity)
                                            val priceStr = viewModel.formatCurrency(item.unitPrice)
                                            val totalStr = viewModel.formatCurrency(item.totalPrice)
                                            append("$status ${i + 1}. ${item.name} ($qtyStr x $priceStr) = $totalStr\n")
                                        }
                                        append("\n💰 *TOTAL DA COMPRA: ${viewModel.formatCurrency(totalAmount)}*")
                                    }
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Boas Compras - Lista")
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar Lista"))
                                } else {
                                    Toast.makeText(context, "Sua lista está vazia para compartilhar!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            icon = Icons.Default.Share,
                            contentDescription = "Compartilhar Lista",
                            enabled = activeItems.isNotEmpty(),
                            modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .testTag("share_list_button_footer")
                        )

                        // 4. Salvar em PDF (PDF Export)
                        HoverRectangularIconButton(
                            onClick = {
                                if (activeItems.isNotEmpty()) {
                                    PrintUtils.exportListAsPdf(
                                        context = context,
                                        listName = "Boas Compras",
                                        items = activeItems,
                                        totalAmount = totalAmount
                                    )
                                } else {
                                    Toast.makeText(context, "Sua lista está vazia para exportar PDF!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            icon = Icons.Default.PictureAsPdf,
                            contentDescription = "Salvar em PDF",
                            enabled = activeItems.isNotEmpty(),
                            modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .testTag("pdf_list_button_footer")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Dense Material 3 Styled Form for High Density Input
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // ProductName TextField
                    Text(
                        text = "PRODUTO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    TextField(
                        value = productName,
                        onValueChange = { viewModel.onProductNameChange(it) },
                        placeholder = { Text("Ex: Feijão, Arroz, Leite...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("product_name_input")
                    )

                    // Autocomplete horizontal suggestions list
                    AnimatedVisibility(
                        visible = suggestions.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(suggestions) { suggestion ->
                                    SuggestionChip(
                                        label = suggestion,
                                        onClick = {
                                            viewModel.selectSuggestion(suggestion)
                                            focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quantity input (col-span-5 equivalent)
                        Column(modifier = Modifier.weight(5f)) {
                            Text(
                                text = "QTD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                            TextField(
                                value = productQuantity,
                                onValueChange = { viewModel.onProductQuantityChange(it) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("product_qty_input")
                            )
                        }

                        // Unit price input (col-span-12 equivalent)
                        Column(modifier = Modifier.weight(7f)) {
                            Text(
                                text = "PREÇO UNITÁRIO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                            TextField(
                                value = productPrice,
                                onValueChange = { viewModel.onProductPriceChange(it) },
                                placeholder = { Text("0,00") },
                                singleLine = true,
                                prefix = { Text("R$ ") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (productName.isNotBlank()) {
                                            viewModel.addCurrentItem()
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        }
                                    }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("product_price_input")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dense Styled main Add Button
                    Button(
                        onClick = {
                            viewModel.addCurrentItem()
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        enabled = productName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_product_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Adicionar ao Carrinho",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sliding overlay sheet for Historical Saved Lists
            AnimatedVisibility(
                visible = showSavedListsSection,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Histórico de Listas Salvas",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            IconButton(onClick = { showSavedListsSection = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar Histórico", modifier = Modifier.size(18.dp))
                            }
                        }

                        if (savedLists.isEmpty()) {
                            Text(
                                "Nenhuma lista histórica salva.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val listScroll = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                                    .verticalScroll(listScroll),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                savedLists.forEach { saved ->
                                    SavedListRow(
                                        list = saved,
                                        formattedAmount = viewModel.formatCurrency(saved.totalAmount),
                                        onLoad = {
                                            viewModel.loadSavedList(saved)
                                            showSavedListsSection = false
                                        },
                                        onDelete = { viewModel.deleteSavedList(saved.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Current Items Screen Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "MINHA LISTA",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                    )
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text("${activeItems.size} itens")
                    }
                }

                if (activeItems.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearActiveList() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("clear_list_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limpar", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bordered high-density list container with elegant subtle shadow & tone separating the layout
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                if (activeItems.isEmpty()) {
                    EmptyListPlaceholder(isDarkModeActive)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("active_items_list")
                    ) {
                        itemsIndexed(activeItems, key = { _, item -> item.id }) { index, item ->
                            ShoppingItemRow(
                                item = item,
                                index = index,
                                formattedPrice = viewModel.formatCurrency(item.unitPrice),
                                formattedTotal = viewModel.formatCurrency(item.totalPrice),
                                formattedQty = viewModel.formatQuantity(item.quantity),
                                onTogglePurchased = { viewModel.toggleItemPurchased(item) },
                                onDelete = { viewModel.deleteItem(item.id) },
                                onEditClick = { editingItem = item }
                            )

                            // Nice divider under items unless it is the last item
                            if (index < activeItems.size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Save List Modal Dialog
    if (showSaveDialog) {
        Dialog(onDismissRequest = { showSaveDialog = false }) {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Qual o nome desta lista?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = listNameToSave,
                        onValueChange = { listNameToSave = it },
                        label = { Text("Nome da Lista") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSaveDialog = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.saveActiveList(listNameToSave)
                                showSaveDialog = false
                                if (proceedToClearAfterSaving) {
                                    viewModel.clearActiveList()
                                    proceedToClearAfterSaving = false
                                }
                            }
                        ) {
                            Text("Confirmar")
                        }
                    }
                }
            }
        }
    }

    // New List Confirmation Pop-up
    if (showNewListConfirmDialog) {
        Dialog(onDismissRequest = { showNewListConfirmDialog = false }) {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nova Lista",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Deseja salvar a sua lista de compras atual antes de iniciar uma nova?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                listNameToSave = "Compras - " + sdf.format(Date())
                                proceedToClearAfterSaving = true
                                showSaveDialog = true
                                showNewListConfirmDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().testTag("confirm_save_and_new")
                        ) {
                            Text("Sim, Salvar e Iniciar Nova")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                viewModel.clearActiveList()
                                showNewListConfirmDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().testTag("confirm_clear_only"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Text("Não Salvar, Apenas Limpar")
                        }
                        
                        TextButton(
                            onClick = { showNewListConfirmDialog = false },
                            modifier = Modifier.fillMaxWidth().testTag("confirm_cancel_new")
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }
    }

    // Edit Item Modal Dialog
    editingItem?.let { itemToEdit ->
        var editQty by remember { mutableStateOf(viewModel.formatQuantity(itemToEdit.quantity)) }
        var editPrice by remember { mutableStateOf(if (itemToEdit.unitPrice == 0.0) "" else String.format(Locale.US, "%.2f", itemToEdit.unitPrice).replace(".", ",")) }

        Dialog(onDismissRequest = { editingItem = null }) {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Editar Item",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = itemToEdit.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Qtd
                    OutlinedTextField(
                        value = editQty,
                        onValueChange = { qty ->
                            val sanitized = qty.replace(",", ".")
                            if (sanitized.isEmpty() || sanitized.toDoubleOrNull() != null || sanitized == ".") {
                                editQty = qty
                            }
                        },
                        label = { Text("Quantidade") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().testTag("edit_item_qty_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Preço Unitário
                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { price ->
                            val sanitized = price.replace(",", ".")
                            if (sanitized.isEmpty() || sanitized.toDoubleOrNull() != null || sanitized == ".") {
                                editPrice = price
                            }
                        },
                        label = { Text("Preço Unitário") },
                        prefix = { Text("R$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth().testTag("edit_item_price_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editingItem = null }, modifier = Modifier.testTag("edit_item_cancel")) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val qtyDouble = editQty.replace(",", ".").toDoubleOrNull() ?: 1.0
                                val priceDouble = editPrice.replace(",", ".").toDoubleOrNull() ?: 0.0
                                viewModel.updateItemDetails(itemToEdit, qtyDouble, priceDouble)
                                editingItem = null
                            },
                            modifier = Modifier.testTag("edit_item_save")
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun SuggestionChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingListItem,
    index: Int,
    formattedPrice: String,
    formattedTotal: String,
    formattedQty: String,
    onTogglePurchased: () -> Unit,
    onDelete: () -> Unit,
    onEditClick: () -> Unit
) {
    val isDone = item.isPurchased

    // Zebra stripes matching High Density CSS alternation bg-[#F7F2FA]
    val rowBg = if (index % 2 == 1) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .testTag("item_row_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checked indicator
            Checkbox(
                checked = isDone,
                onCheckedChange = { onTogglePurchased() },
                modifier = Modifier.testTag("item_checkbox_${item.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Title & Counts
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEditClick() }
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("item_name_${item.id}")
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$formattedQty x $formattedPrice",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Total Price on Right
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = formattedTotal,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Black,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary
                )
            }

            // Trash can delete button styled as custom beautiful trash bin with open red lid
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("item_delete_${item.id}")
            ) {
                CustomTrashIcon(
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyListPlaceholder(isDark: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoasComprasEmblem(
            isDark = isDark,
            modifier = Modifier.size(90.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sua lista está vazia",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Adicione produtos, quantidades e valores acima para calcular instantaneamente o valor final.",
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun SavedListRow(
    list: com.example.data.database.SavedList,
    formattedAmount: String,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val formattedDate = sdf.format(Date(list.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Criada em $formattedDate • Total: $formattedAmount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onLoad,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Carregar", style = MaterialTheme.typography.labelMedium)
                }

                IconButton(onClick = onDelete) {
                    CustomTrashIcon(
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomTrashIcon(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(24.dp)
            .testTag("custom_trash_icon")
    ) {
        val width = size.width
        val height = size.height
        
        // Trash bin body coordinates
        val bodyLeft = width * 0.20f
        val bodyTop = height * 0.40f
        val bodyRight = width * 0.80f
        val bodyBottom = height * 0.92f
        
        val bodyPath = Path().apply {
            moveTo(bodyLeft, bodyTop)
            lineTo(bodyRight, bodyTop)
            lineTo(bodyRight - width * 0.04f, bodyBottom - height * 0.06f)
            quadraticTo(
                bodyRight - width * 0.04f, bodyBottom,
                bodyRight - width * 0.10f, bodyBottom
            )
            lineTo(bodyLeft + width * 0.10f, bodyBottom)
            quadraticTo(
                bodyLeft + width * 0.04f, bodyBottom,
                bodyLeft + width * 0.04f, bodyBottom - height * 0.06f
            )
            close()
        }
        
        // Draw body background (grey-blue)
        drawPath(
            path = bodyPath,
            color = Color(0xFF8BA3B5)
        )
        
        // Draw body outline (black)
        drawPath(
            path = bodyPath,
            color = Color(0xFF141218),
            style = Stroke(
                width = 1.8f.dp.toPx(),
                join = StrokeJoin.Round
            )
        )
        
        // Draw three white vertical lines representing stripes
        val strokeWidth = 1.5f.dp.toPx()
        drawLine(
            color = Color.White,
            start = Offset(width * 0.38f, bodyTop + height * 0.14f),
            end = Offset(width * 0.38f, bodyBottom - height * 0.16f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Stripe 2 (center)
        drawLine(
            color = Color.White,
            start = Offset(width * 0.50f, bodyTop + height * 0.14f),
            end = Offset(width * 0.50f, bodyBottom - height * 0.16f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Stripe 3
        drawLine(
            color = Color.White,
            start = Offset(width * 0.62f, bodyTop + height * 0.14f),
            end = Offset(width * 0.62f, bodyBottom - height * 0.16f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // Now, let's draw the tilted open lid!
        val lidAngle = -18f // Tilted open lid
        
        // Rotate lid drawing
        withTransform({
            rotate(degrees = lidAngle, pivot = Offset(width * 0.15f, height * 0.38f))
        }) {
            // Draw lid body: flat rectangle with tapered edges
            val lidLeft = width * 0.12f
            val lidTop = height * 0.28f
            val lidRight = width * 0.88f
            val lidBottom = height * 0.38f
            
            val lidPath = Path().apply {
                moveTo(lidLeft, lidBottom)
                lineTo(lidLeft - width * 0.02f, lidTop + height * 0.04f)
                quadraticTo(
                    lidLeft - width * 0.02f, lidTop,
                    lidLeft + width * 0.04f, lidTop
                )
                lineTo(lidRight - width * 0.04f, lidTop)
                quadraticTo(
                    lidRight + width * 0.02f, lidTop,
                    lidRight + width * 0.02f, lidTop + height * 0.04f
                )
                lineTo(lidRight, lidBottom)
                close()
            }
            
            // Fill lid with reddish/orange color (#FD8956)
            drawPath(
                path = lidPath,
                color = Color(0xFFFD8956)
            )
            
            // Lid outline (black)
            drawPath(
                path = lidPath,
                color = Color(0xFF141218),
                style = Stroke(
                    width = 1.8f.dp.toPx(),
                    join = StrokeJoin.Round
                )
            )
            
            // Draw lid handle at the top-center of the lid
            val handleLeft = width * 0.42f
            val handleTop = height * 0.18f
            val handleRight = width * 0.58f
            val handleBottom = lidTop
            
            val handlePath = Path().apply {
                moveTo(handleLeft, handleBottom)
                lineTo(handleLeft, handleTop + height * 0.04f)
                quadraticTo(
                    handleLeft, handleTop,
                    handleLeft + width * 0.04f, handleTop
                )
                lineTo(handleRight - width * 0.04f, handleTop)
                quadraticTo(
                    handleRight, handleTop,
                    handleRight, handleTop + height * 0.04f
                )
                lineTo(handleRight, handleBottom)
            }
            
            // Draw handle background (grey)
            drawPath(
                path = handlePath,
                color = Color(0xFFB0BEC5)
            )
            // Draw handle outline (black)
            drawPath(
                path = handlePath,
                color = Color(0xFF141218),
                style = Stroke(
                    width = 1.8f.dp.toPx(),
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun BoasComprasEmblem(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val circleColor = if (isDark) Color(0xFFECF4FC) else Color(0xFF0D3C6C)
    val clipboardColor = if (isDark) Color(0xFF0D3C6C) else Color(0xFFECF4FC)

    Canvas(
        modifier = modifier
    ) {
        val w = size.width
        val h = size.height
        val radius = w * 0.42f
        val cx = w / 2f
        val cy = h / 2f

        // 1. Draw circular background badge
        drawCircle(
            color = circleColor,
            radius = radius,
            center = Offset(cx, cy)
        )

        // 2. Draw the tilted clipboard inside the circle
        // Angle of tilt: around -18 degrees
        withTransform({
            rotate(degrees = -18f, pivot = Offset(cx, cy))
            scale(scaleX = 0.95f, scaleY = 0.95f, pivot = Offset(cx, cy))
        }) {
            // Clipboard base board (offset from center)
            val cbWidth = w * 0.36f
            val cbHeight = h * 0.52f
            val cbLeft = cx - cbWidth / 2f
            val cbTop = cy - cbHeight / 2.2f

            // Draw paper sheet boundary
            val paperPath = Path().apply {
                val pLeft = cbLeft + cbWidth * 0.08f
                val pRight = cbLeft + cbWidth * 0.92f
                val pTop = cbTop + cbHeight * 0.16f
                val pBottom = cbTop + cbHeight * 0.92f
                moveTo(pLeft, pTop)
                lineTo(pRight, pTop)
                lineTo(pRight, pBottom)
                lineTo(pLeft, pBottom)
                close()
            }

            // Draw clipboard body
            val boardPath = Path().apply {
                moveTo(cbLeft, cbTop + cbHeight * 0.08f)
                lineTo(cbLeft + cbWidth * 0.15f, cbTop + cbHeight * 0.08f)
                lineTo(cbLeft + cbWidth * 0.15f, cbTop)
                lineTo(cbLeft + cbWidth * 0.85f, cbTop)
                lineTo(cbLeft + cbWidth * 0.85f, cbTop + cbHeight * 0.08f)
                lineTo(cbLeft + cbWidth, cbTop + cbHeight * 0.08f)
                lineTo(cbLeft + cbWidth, cbTop + cbHeight)
                lineTo(cbLeft, cbTop + cbHeight)
                close()
            }

            drawPath(
                path = boardPath,
                color = clipboardColor
            )

            // Fill paper
            drawPath(
                path = paperPath,
                color = circleColor
            )

            // Draw metal clip handle on top
            val handlePath = Path().apply {
                val hLeft = cx - cbWidth * 0.22f
                val hRight = cx + cbWidth * 0.22f
                val hTop = cbTop - cbHeight * 0.06f
                val hBottom = cbTop + cbHeight * 0.08f
                
                moveTo(hLeft, hBottom)
                lineTo(hLeft, hTop)
                lineTo(hRight, hTop)
                lineTo(hRight, hBottom)
            }
            
            drawPath(
                path = handlePath,
                color = clipboardColor,
                style = Stroke(width = cbWidth * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Horizontal stripes for item rows on the paper
            val stripeXStart = cbLeft + cbWidth * 0.22f
            val stripeXEnd = cbLeft + cbWidth * 0.78f
            val firstStripeY = cbTop + cbHeight * 0.32f
            val stripeSpacing = cbHeight * 0.13f
            val stripeWidthVal = cbWidth * 0.07f

            for (i in 0 until 4) {
                val y = firstStripeY + i * stripeSpacing
                drawLine(
                    color = clipboardColor,
                    start = Offset(stripeXStart, y),
                    end = Offset(stripeXEnd, y),
                    strokeWidth = stripeWidthVal,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun BoasComprasLogo(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDark) Color(0xFF0D3C6C) else Color(0xFFECF4FC)
    val textColor = if (isDark) Color.White else Color(0xFF0D3C6C)

    Column(
        modifier = modifier
            .background(bgColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BoasComprasEmblem(
            isDark = isDark,
            modifier = Modifier.size(170.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "BOAS COMPRAS",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = textColor
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "SUA LISTA DE COMPRAS DEFINITIVA",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = textColor.copy(alpha = 0.75f)
            )
        )
    }
}
