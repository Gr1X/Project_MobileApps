// File: features/admin/medicine/ManageMedicineScreen.kt
package com.example.project_mobileapps.features.admin.medicine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu // [PENTING] Import Icon Menu
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.Medicine
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.themes.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMedicineScreen(
    onOpenDrawer: () -> Unit, // [UBAH] Dari onNavigateBack ke onOpenDrawer
    viewModel: ManageMedicineViewModel = viewModel(factory = ManageMedicineViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()

    // State Lokal
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }

    // Filter Logic
    val filteredMedicines = remember(uiState.medicines, searchQuery) {
        if (searchQuery.isBlank()) uiState.medicines
        else uiState.medicines.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    if (medicineToDelete != null) {
        ConfirmationBottomSheet(
            title = "Hapus Obat?",
            text = "Apakah Anda yakin ingin menghapus '${medicineToDelete?.name}'?",
            onDismiss = { medicineToDelete = null },
            onConfirm = {
                medicineToDelete?.id?.let { viewModel.deleteMedicine(it) }
                medicineToDelete = null
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    // [UBAH] Menggunakan Icon Menu (Hamburger)
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Tambah Obat") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Header Judul
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Manajemen Obat",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = TextPrimary
                )
            }

            // Search Bar
            SearchBarCustom(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            // Content List
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredMedicines.isEmpty()) {
                EmptyStateMedicine(isSearching = searchQuery.isNotEmpty())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMedicines) { medicine ->
                        MedicineItemCardEnhanced(
                            medicine = medicine,
                            onDeleteClick = { medicineToDelete = medicine }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddMedicineDialogEnhanced(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, cat, form ->
                    viewModel.addMedicine(name, cat, form)
                    showAddDialog = false
                }
            )
        }
    }
}

// ... (SISA COMPONENT SEPERTI MedicineItemCardEnhanced, SearchBarCustom, dll TETAP SAMA)
// Pastikan component pendukung di bawah tetap ada di file Anda
@Composable
fun MedicineItemCardEnhanced(medicine: Medicine, onDeleteClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryChip(text = medicine.category)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${medicine.form}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Outlined.Delete, "Hapus", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun CategoryChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
fun SearchBarCustom(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(16.dp).background(Color.White, RoundedCornerShape(12.dp)),
        placeholder = { Text("Cari nama obat atau kategori...") },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
        singleLine = true
    )
}

@Composable
fun AddMedicineDialogEnhanced(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var form by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }
    var isCategoryError by remember { mutableStateOf(false) }
    var isFormError by remember { mutableStateOf(false) }

    fun validateAndSubmit() {
        isNameError = name.isBlank(); isCategoryError = category.isBlank(); isFormError = form.isBlank()
        if (!isNameError && !isCategoryError && !isFormError) onConfirm(name, category, form)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Obat Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ValidatedTextField(name, { name = it; isNameError = false }, "Nama Obat", "Contoh: Paracetamol 500mg", isNameError, "Nama obat tidak boleh kosong")
                ValidatedTextField(category, { category = it; isCategoryError = false }, "Kategori", "Contoh: Analgesik", isCategoryError, "Kategori wajib diisi")
                ValidatedTextField(form, { form = it; isFormError = false }, "Bentuk Sediaan", "Contoh: Tablet", isFormError, "Bentuk obat wajib diisi")
            }
        },
        confirmButton = { Button(onClick = { validateAndSubmit() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Simpan Data") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = Color.Gray) } },
        containerColor = Color.White, shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ValidatedTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, isError: Boolean, errorText: String) {
    Column {
        OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, placeholder = { Text(placeholder, color = Color.LightGray) }, isError = isError, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next), shape = RoundedCornerShape(8.dp))
        if (isError) Text(text = errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
    }
}

@Composable
fun EmptyStateMedicine(isSearching: Boolean) {
    Column(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.Inventory2, null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = if (isSearching) "Obat tidak ditemukan" else "Belum ada data obat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(text = if (isSearching) "Coba kata kunci lain" else "Tekan tombol + untuk menambahkan", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
    }
}