package com.example.project_mobileapps.features.admin.medicine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.Medicine
import com.example.project_mobileapps.ui.components.CircularBackButton

@Composable
fun ManageMedicineScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageMedicineViewModel = viewModel(factory = ManageMedicineViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        // ... (TopBar tetap sama)
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Obat")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.medicines) { medicine ->
                MedicineItemCard(medicine, onDelete = { viewModel.deleteMedicine(medicine.id) })
            }
        }

        if (showDialog) {
            AddMedicineDialog(
                onDismiss = { showDialog = false },
                onConfirm = { name, cat, form -> // Ubah param ke form
                    viewModel.addMedicine(name, cat, form)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun MedicineItemCard(medicine: Medicine, onDelete: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        ListItem(
            leadingContent = { Icon(Icons.Default.Medication, null) },
            headlineContent = { Text(medicine.name, style = MaterialTheme.typography.titleMedium) },
            // Tampilkan Kategori dan Bentuk (Form)
            supportingContent = { Text("${medicine.category} • ${medicine.form}") },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
fun AddMedicineDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var form by remember { mutableStateOf("") } // Ganti stock jadi form

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Obat Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Obat") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Kategori (ex: Antibiotik)") })
                // Input Bentuk Obat
                OutlinedTextField(value = form, onValueChange = { form = it }, label = { Text("Bentuk (ex: Tablet/Sirup)") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, category, form) }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}