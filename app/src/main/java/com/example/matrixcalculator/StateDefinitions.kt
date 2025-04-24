package com.example.matrixcalculator

// Define the different stages of the UI
enum class InputStep {
  DIMENSIONS,
  MATRIX_A_INPUT,
  MATRIX_B_INPUT,
  OPERATION_SELECTION,
  SHOW_RESULT
}

// Define supported operations
enum class MatrixOperation(val symbol: String) {
  ADD("+"),
  SUBTRACT("-"),
  MULTIPLY("*")
}


