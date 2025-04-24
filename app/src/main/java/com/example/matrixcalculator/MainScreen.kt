package com.example.matrixcalculator

import androidx.compose.foundation.layout.* // Keep necessary imports
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.matrixcalculator.ui.theme.MatrixCalculatorTheme // Import your theme

@Composable
fun MainScreen(modifier: Modifier = Modifier) {

  // State for the matrices and result
  val matrixAState = remember { MatrixState() }
  val matrixBState = remember { MatrixState() }
  val resultMatrixState = remember { MatrixState() }

  // UI flow control state
  var currentStep by remember { mutableStateOf(InputStep.DIMENSIONS) }

  // Operation state
  var selectedOperation by remember { mutableStateOf<MatrixOperation?>(null) }

  // Error message state
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Keyboard and Focus controllers
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  // --- Helper Functions (resetAll, validateOperation, performCalculation - Keep as before) ---
  fun resetAll() {
    matrixAState.reset()
    matrixBState.reset()
    resultMatrixState.reset()
    selectedOperation = null
    errorMessage = null
    currentStep = InputStep.DIMENSIONS
    focusManager.clearFocus()
    keyboardController?.hide()
  }

  fun validateOperation(): Boolean {
    errorMessage = null
    val op = selectedOperation
    if (op == null) {
      errorMessage = "No operation selected."
      return false
    }
    val matrixAData = matrixAState.getImmutableDoubleCopy()
    val matrixBData = matrixBState.getImmutableDoubleCopy()
    if (matrixAData == null) {
      errorMessage = "Matrix A contains non-numeric or empty values."
      resetAll()
      return false
    }
    if (matrixBData == null) {
      errorMessage = "Matrix B contains non-numeric or empty values."
      resetAll()
      return false
    }
    when (op) {
      MatrixOperation.ADD, MatrixOperation.SUBTRACT -> {
        if (matrixAState.rows != matrixBState.rows || matrixAState.cols != matrixBState.cols) {
          errorMessage = "Addition/Subtraction requires matrices of the same dimensions (${matrixAState.rows}x${matrixAState.cols} vs ${matrixBState.rows}x${matrixBState.cols})."
          resetAll()
          return false
        }
      }
      MatrixOperation.MULTIPLY -> {
        if (matrixAState.cols != matrixBState.rows) {
          errorMessage = "Multiplication requires A.cols == B.rows (${matrixAState.cols} != ${matrixBState.rows})."
          resetAll()
          return false
        }
      }
    }
    return true
  }

  fun performCalculation() {
    val matrixAData = matrixAState.getImmutableDoubleCopy()!!
    val matrixBData = matrixBState.getImmutableDoubleCopy()!!
    val op = selectedOperation!!
    val aArray = matrixAData.map { it.toDoubleArray() }.toTypedArray()
    val bArray = matrixBData.map { it.toDoubleArray() }.toTypedArray()
    try {
      val resultArray: Array<DoubleArray> = when (op) {
        MatrixOperation.ADD -> MatrixCalculatorBridge.addMatrices(aArray, bArray)
        MatrixOperation.SUBTRACT -> MatrixCalculatorBridge.subtractMatrices(aArray, bArray)
        MatrixOperation.MULTIPLY -> MatrixCalculatorBridge.multiplyMatrices(aArray, bArray)
      }
      val resultList = resultArray.map { it.toList() }
      resultMatrixState.setFromDoubleList(resultList)
      currentStep = InputStep.SHOW_RESULT
      errorMessage = null
    } catch (e: UnsatisfiedLinkError) {
      println("Calculation Error (JNI Link): ${e.message}")
      errorMessage = "Calculation failed: Native library not linked correctly."
    } catch (e: Exception) {
      println("Calculation Error: ${e.message}")
      errorMessage = "Calculation failed: ${e.message}"
    }
  }
  // --- End Helper Functions ---


  // Main UI Structure
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {

    Text("Matrix Calculator", style = MaterialTheme.typography.headlineMedium)

    if (errorMessage != null) {
      Text(
        text = "Error: $errorMessage",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 8.dp)
      )
    }

    // --- Step 1: Dimension Input ---
    if (currentStep == InputStep.DIMENSIONS) {
      DimensionInput(
        label = "Matrix A Dimensions (rows x cols)",
        matrixState = matrixAState,
        onDone = { focusManager.moveFocus(FocusDirection.Next) },
        modifier = Modifier.fillMaxWidth(0.8f) // Apply modifier
      )
      DimensionInput(
        label = "Matrix B Dimensions (rows x cols)",
        matrixState = matrixBState,
        onDone = { keyboardController?.hide() },
        modifier = Modifier.fillMaxWidth(0.8f) // Apply modifier
      )
      Button(
        onClick = {
          val aOk = matrixAState.updateDimensionsFromString(matrixAState.dimensionInput)
          val bOk = matrixBState.updateDimensionsFromString(matrixBState.dimensionInput)
          if (aOk && bOk) {
            currentStep = InputStep.MATRIX_A_INPUT
            errorMessage = null
          } else {
            errorMessage = "Invalid dimensions format. Use 'rows x cols' (e.g., 3x4)."
            if (!aOk) matrixAState.reset()
            if (!bOk) matrixBState.reset()
          }
        },
        enabled = matrixAState.dimensionInput.isNotBlank() && matrixBState.dimensionInput.isNotBlank()
      ) {
        Text("Set Dimensions & Continue")
      }
    }

    // --- Step 2: Matrix A Input (Use Sequential Input) ---
    if (currentStep == InputStep.MATRIX_A_INPUT) {
      SequentialMatrixInput( // <-- Use the new composable
        matrixIdentifier = "Matrix A",
        matrixState = matrixAState,
        onDone = {
          println("Sequential Matrix A input finished.") // Log completion
          currentStep = InputStep.MATRIX_B_INPUT // Move to next step
        },
        modifier = Modifier.fillMaxWidth() // Allow it to take width
      )
    }

    // --- Step 3: Matrix B Input (Use Sequential Input) ---
    if (currentStep == InputStep.MATRIX_B_INPUT) {
      SequentialMatrixInput( // <-- Use the new composable
        matrixIdentifier = "Matrix B",
        matrixState = matrixBState,
        onDone = {
          println("Sequential Matrix B input finished.") // Log completion
          keyboardController?.hide()
          currentStep = InputStep.OPERATION_SELECTION // Move to next step
        },
        modifier = Modifier.fillMaxWidth() // Allow it to take width
      )
    }

    // --- Step 4: Operation Selection ---
    if (currentStep == InputStep.OPERATION_SELECTION) {
      Text("Select Operation", style = MaterialTheme.typography.titleMedium)
      // Display matrices before operation selection for context
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        MatrixDisplay(matrixState = matrixAState) // Display final Matrix A
        MatrixDisplay(matrixState = matrixBState) // Display final Matrix B
      }
      OperationSelection(
        selectedOperation = selectedOperation,
        onOperationSelected = { selectedOperation = it }
      )
      Button(
        onClick = {
          if (validateOperation()) {
            performCalculation()
          }
        },
        enabled = selectedOperation != null
      ) {
        Text("Calculate")
      }
    }

    // --- Step 5: Show Result ---
    if (currentStep == InputStep.SHOW_RESULT) {
      Text("Result Matrix (${resultMatrixState.rows} x ${resultMatrixState.cols})", style = MaterialTheme.typography.titleMedium)
      Text(
        "(${matrixAState.rows}x${matrixAState.cols}) ${selectedOperation?.symbol ?: "?"} (${matrixBState.rows}x${matrixBState.cols}) =",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp)
      )
      MatrixDisplay(matrixState = resultMatrixState)
      Button(onClick = ::resetAll) {
        Text("Calculate New Matrices")
      }
    }

    // --- Reset Button ---
    if (currentStep != InputStep.DIMENSIONS) {
      Spacer(Modifier.height(24.dp))
      Button(onClick = ::resetAll) {
        Text("Reset All")
      }
    }
  }
}

// --- Previews (Might need adjustment if they relied on MatrixInput directly) ---
@Preview(showBackground = true, widthDp = 380)
@Composable
fun MainScreenPreview() {
  MatrixCalculatorTheme {
    MainScreen()
  }
}

// Preview for Sequential Input might look like this:
@Preview(showBackground = true, name = "Sequential Input Step Preview")
@Composable
fun SequentialInputStepPreview() {
  MatrixCalculatorTheme {
    val matrixA = remember { MatrixState().apply { updateDimensionsFromString("2x2")} }
    // Simulate being in Matrix A input step
    Column(modifier = Modifier.padding(16.dp)){
      SequentialMatrixInput(
        matrixIdentifier = "Matrix A",
        matrixState = matrixA,
        onDone = {}
      )
    }
  }
}

// Keep Result Preview
@Preview(showBackground = true, name = "Result Step Preview")
@Composable
fun ResultStepPreview() {
  MatrixCalculatorTheme {
    val resultMatrix = remember { MatrixState().apply { setFromDoubleList(listOf(listOf(1.0, 2.0), listOf(3.0, 4.0))) } }
    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally){
      Text("Result Matrix (2 x 2)", style = MaterialTheme.typography.titleMedium)
      Text("(2x2) + (2x2) =", style = MaterialTheme.typography.bodyMedium)
      MatrixDisplay(matrixState = resultMatrix)
      Button(onClick = { /* */ }) { Text("Calculate New Matrices") }
    }
  }
}
