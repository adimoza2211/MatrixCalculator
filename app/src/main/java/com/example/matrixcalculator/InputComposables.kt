package com.example.matrixcalculator

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


@Composable
fun DimensionInput(
  modifier: Modifier = Modifier,
  label: String,
  matrixState: MatrixState,
  onDone: () -> Unit
) {
  OutlinedTextField(
    value = matrixState.dimensionInput,
    onValueChange = { newValue ->
      matrixState.dimensionInput = newValue
    },
    label = { Text(label) },
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Text,
      imeAction = ImeAction.Done
    ),
    keyboardActions = KeyboardActions(onDone = { onDone() }),
    singleLine = true,
    modifier = modifier
  )
}




@Composable
fun SequentialMatrixInput(
  modifier: Modifier = Modifier,
  matrixIdentifier: String, // e.g., "Matrix A" or "Matrix B"
  matrixState: MatrixState,
  onDone: () -> Unit // Called when all elements are entered
) {
  // State for the current cell being edited
  var currentRow by remember(matrixState.rows, matrixState.cols) { mutableIntStateOf(0) }
  var currentCol by remember(matrixState.rows, matrixState.cols) { mutableIntStateOf(0) }

  // State for the value currently being typed in the text field
  // Initialize with the existing value if available (e.g., if user comes back)
  var currentInputValue by remember(matrixState.rows, matrixState.cols, currentRow, currentCol) {
    mutableStateOf(matrixState.getElement(currentRow, currentCol))
  }

  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  val totalRows = matrixState.rows
  val totalCols = matrixState.cols

  // Check if dimensions are valid before proceeding
  if (totalRows <= 0 || totalCols <= 0) {
    Text("Invalid dimensions for $matrixIdentifier. Please reset and set dimensions first.")
    return // Don't show input if dimensions are bad
  }

  // Function to handle submitting the current element and moving to the next
  fun submitAndMoveNext() {
    // Basic validation (allow empty, '-', '.', or numbers)
    if (currentInputValue.isEmpty() || currentInputValue == "-" || currentInputValue == "." || currentInputValue == "-." || currentInputValue.toDoubleOrNull() != null) {
      // Update the MatrixState with the submitted value
      matrixState.updateElement(currentRow, currentCol, currentInputValue)

      // Move to the next element
      if (currentCol < totalCols - 1) {
        // Move to next column in the same row
        currentCol++
      } else if (currentRow < totalRows - 1) {
        // Move to the first column of the next row
        currentCol = 0
        currentRow++
      } else {
        // Last element entered, hide keyboard and call onDone
        keyboardController?.hide()
        onDone()
        return // Exit function after calling onDone
      }

      // Update the input field with the value of the *new* current cell
      currentInputValue = matrixState.getElement(currentRow, currentCol)

    } else {
      // Handle invalid input if needed (e.g., show a temporary error message)
      println("Invalid input detected: '$currentInputValue'")
      // Optionally clear the field or keep the invalid input for user correction
      // currentInputValue = "" // Clear field on invalid input
    }
  }

  Column(
    modifier = modifier.padding(vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      "Enter element for $matrixIdentifier [$currentRow][$currentCol]",
      style = MaterialTheme.typography.titleMedium
    )

    OutlinedTextField(
      value = currentInputValue,
      onValueChange = { newValue ->
        // Just update the local state for the current input field
        currentInputValue = newValue
      },
      label = { Text("Value for [$currentRow][$currentCol]") },
      keyboardOptions = KeyboardOptions(
        // Use NumberPassword again, hoping it works better in this simpler setup
        keyboardType = KeyboardType.NumberPassword,
        imeAction = ImeAction.Done // Use Done since there's only one field active
      ),
      keyboardActions = KeyboardActions(
        onDone = { submitAndMoveNext() } // Submit on keyboard Done action
      ),
      singleLine = true,
      modifier = Modifier.fillMaxWidth(0.6f) // Adjust width as needed
    )

    Button(onClick = { submitAndMoveNext() }) {
      // Determine button text based on whether it's the last element
      if (currentRow == totalRows - 1 && currentCol == totalCols - 1) {
        Text("Finish $matrixIdentifier")
      } else {
        Text("Next Element ([${currentRow}][${currentCol+1}] or [${currentRow+1}][0])") // Indicate next target
      }
    }
  }
}








@Composable
fun MatrixInput(
  modifier: Modifier = Modifier,
  matrixState: MatrixState,
  onDone: () -> Unit // This onDone is now only triggered by the last field's keyboard action IF enabled
) {
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val rowCount = matrixState.rows
  val colCount = matrixState.cols

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier
      .padding(vertical = 8.dp)
      .border(1.dp, MaterialTheme.colorScheme.outline)
      .padding(8.dp)
  ) {
    if (rowCount == 0 || colCount == 0) {
      Text("Enter dimensions first.")
      return@Column
    }
    for (r in 0 until rowCount) {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (c in 0 until colCount) {
          // val isLastElement = r == rowCount - 1 && c == colCount - 1 // Not needed if actions disabled

          OutlinedTextField(
            value = matrixState.getElement(r, c),
            onValueChange = { newValue ->
              val callId = System.nanoTime() // Keep unique ID logging
              println("MatrixInput [$callId] onValueChange: r=$r, c=$c, newValue='$newValue'")

              // Restore the original filter or keep it bypassed if you prefer
              // Original filter:
              if (newValue.isEmpty() || newValue == "-" || newValue == "." || newValue == "-." || newValue.toDoubleOrNull() != null) {
                matrixState.updateElement(r, c, newValue)
              } else {
                println("MatrixInput [$callId] onValueChange BLOCKED: r=$r, c=$c, newValue='$newValue'")
              }
              // --- OR ---
              // Filter bypassed (use only one of these):
              // matrixState.updateElement(r, c, newValue)

            },
            modifier = Modifier
              .weight(1f)
              .defaultMinSize(minWidth = 60.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            // --- Temporarily Disable Keyboard Options/Actions ---
            /*
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                // Set ImeAction to Default or Done for all fields now
                imeAction = ImeAction.Done // Or ImeAction.Default
            ),
            keyboardActions = KeyboardActions(
               // Remove onNext and onDone actions for testing
               // onNext = { focusManager.moveFocus(FocusDirection.Next) },
               // onDone = {
               //     if (isLastElement) {
               //         keyboardController?.hide()
               //         onDone() // Only call onDone if it was the last element
               //     } else {
               //          focusManager.moveFocus(FocusDirection.Next) // Default behavior if not last
               //     }
               // }
            ),
            */
            // --- End Temporary Disable ---
            singleLine = true,
            placeholder = { Text("0") }
          )
        }
      }
    }
    // Add an explicit button to proceed, as the keyboard 'Done' action on the last field is now disabled
    Button(
      modifier = Modifier.padding(top = 8.dp),
      onClick = {
        println("MatrixInput Explicit Confirm Button Clicked")
        keyboardController?.hide() // Hide keyboard on button click
        onDone() // Call the original onDone lambda passed from MainScreen
      }
    ) {
      // Adjust button text based on which matrix this instance is for (A or B)
      // This requires passing an identifier or using separate composables,
      // for now, using generic text.
      Text("Confirm Matrix Input")
    }
  }
}
