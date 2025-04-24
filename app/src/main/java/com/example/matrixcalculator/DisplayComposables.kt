package com.example.matrixcalculator

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale // Import Locale for String.format

@Composable
fun MatrixDisplay(
  modifier: Modifier = Modifier,
  matrixState: MatrixState
) {
  if (!matrixState.isValid()) {
    Text("Matrix is empty.", modifier = modifier)
    return
  }

  // Attempt to format numbers nicely, fallback to original strings if conversion fails
  val displayData: List<List<String>> = try {
    matrixState.getImmutableDoubleCopy()?.map { row ->
      row.map { num ->
        // Format to 2 decimal places, handle potential NaNs or infinities
        if (num.isFinite()) String.format(Locale.US, "%.2f", num) else num.toString()
      }
    } ?: matrixState.elements.map { it.toList() } // Fallback if not all numbers
  } catch (e: Exception) {
    // Fallback in case of unexpected errors during formatting
    matrixState.elements.map { it.toList() }
  }


  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier // Apply modifier passed from caller
      .padding(vertical = 8.dp)
      .border(1.dp, MaterialTheme.colorScheme.primary)
      .padding(8.dp)
  ) {
    for (r in 0 until matrixState.rows) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (c in 0 until matrixState.cols) {
          // Check bounds before accessing displayData
          val textToShow = displayData.getOrNull(r)?.getOrNull(c) ?: "ERR" // Error text if out of bounds
          Text(
            text = textToShow,
            modifier = Modifier
              .defaultMinSize(minWidth = 60.dp)
              .padding(horizontal = 4.dp),
            textAlign = TextAlign.Center,
            fontSize = 16.sp // Adjust font size for readability
          )
        }
      }
    }
  }
}