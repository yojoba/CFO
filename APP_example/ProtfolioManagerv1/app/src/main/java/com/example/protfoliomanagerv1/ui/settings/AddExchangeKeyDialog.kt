package com.example.protfoliomanagerv1.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.protfoliomanagerv1.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExchangeKeyDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String?) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var selectedExchange by remember { mutableStateOf("binance") }
    var apiKey by remember { mutableStateOf("") }
    var apiSecret by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var showApiSecret by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    val exchanges = listOf(
        "binance" to Color(0xFFF3BA2F),
        "bybit" to Color(0xFFF7A600),
        "mexc" to GradientBlue
    )
    
    AlertDialog(
        onDismissRequest = if (!isLoading) onDismiss else { {} },
        confirmButton = {},
        containerColor = CryptoDarkSurface,
        tonalElevation = 8.dp,
        text = {
            Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Title
            Text(
                text = "Add Exchange API Key",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Exchange Selection
            Text(
                text = "Select Exchange",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                exchanges.forEach { (exchange, color) ->
                    FilterChip(
                        selected = selectedExchange == exchange,
                        onClick = { selectedExchange = exchange },
                        label = {
                            Text(
                                text = exchange.uppercase(),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.3f),
                            selectedLabelColor = color,
                            containerColor = CryptoDarkSurfaceVariant,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Label Field (Optional)
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label (Optional)") },
                placeholder = { Text("e.g., Main Account") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GradientBlue,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                    focusedLabelColor = GradientBlue,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = GradientBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // API Key Field
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it.trim() },
                label = { Text("API Key") },
                placeholder = { Text("Enter your API key") },
                singleLine = true,
                trailingIcon = {
                    if (apiKey.isNotEmpty()) {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Text(
                                text = if (showApiKey) "👁" else "👁‍🗨",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                },
                visualTransformation = if (showApiKey) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GradientBlue,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                    focusedLabelColor = GradientBlue,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = GradientBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // API Secret Field
            OutlinedTextField(
                value = apiSecret,
                onValueChange = { apiSecret = it.trim() },
                label = { Text("API Secret") },
                placeholder = { Text("Enter your API secret") },
                singleLine = true,
                trailingIcon = {
                    if (apiSecret.isNotEmpty()) {
                        IconButton(onClick = { showApiSecret = !showApiSecret }) {
                            Text(
                                text = if (showApiSecret) "👁" else "👁‍🗨",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                },
                visualTransformation = if (showApiSecret) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GradientBlue,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                    focusedLabelColor = GradientBlue,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = GradientBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Help Text
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GradientBlue.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = GradientBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your API keys will be encrypted and stored securely. We'll test the connection before saving.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            
            // Error Message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElectricRed.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        onAdd(
                            selectedExchange,
                            apiKey,
                            apiSecret,
                            label.ifBlank { null }
                        )
                    },
                    enabled = !isLoading && apiKey.isNotBlank() && apiSecret.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        disabledContainerColor = TextSecondary.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CryptoDarkBackground,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Add Key",
                            fontWeight = FontWeight.Bold,
                            color = CryptoDarkBackground
                        )
                    }
                }
            }
        }
        }
    )
}

