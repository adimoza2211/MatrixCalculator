package com.example.matrixcalculator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme // Import necessary MaterialTheme components
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OperationSelection(
  modifier: Modifier = Modifier,
  selectedOperation: MatrixOperation?,
  onOperationSelected: (MatrixOperation) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    modifier = modifier // Apply modifier passed from caller
      .fillMaxWidth()
      .padding(vertical = 8.dp)
  ) {
    MatrixOperation.entries.forEach { operation ->
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
        RadioButton(
          selected = (selectedOperation == operation),
          onClick = { onOperationSelected(operation) }
        )
        Text(
          text = operation.symbol,
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.padding(start = 4.dp)
        )
      }
    }
  }
}