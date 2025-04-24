package com.example.matrixcalculator

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList // Explicit import

class MatrixState {
  var rows by mutableIntStateOf(0)
  var cols by mutableIntStateOf(0)
  // Use SnapshotStateList for better Compose integration with list modifications
  val elements: SnapshotStateList<MutableList<String>> = mutableStateListOf()

  // Keep track of the raw dimension input string for the TextField
  var dimensionInput by mutableStateOf("")

  fun isValid(): Boolean {
    return (rows > 0 && cols > 0)
  }

  // Attempts to parse the dimension string (e.g., "3x4") and set dimensions
  // Returns true on success, false on failure
  fun updateDimensionsFromString(input: String): Boolean {
    dimensionInput = input // Store the raw input
    val parts = input.trim().split('x', 'X', '*') // Allow different separators
    if (parts.size == 2) {
      try {
        val r = parts[0].trim().toInt()
        val c = parts[1].trim().toInt()
        if (r > 0 && c > 0) {
          // Only resize if dimensions actually change
          if (r != rows || c != cols) {
            rows = r
            cols = c
            elements.clear()
            // Correctly add 'rows' number of lists
            repeat(rows) {
              // Each inner list should have 'cols' elements
              elements.add(MutableList(cols) { "" })
            }
          }
          return true
        }
      } catch (e: NumberFormatException) {
        // Invalid number format
      }
    }
    // If parsing failed or dimensions are invalid, reset
    clearDimensions()
    return false
  }

  private fun clearDimensions() {
    if (rows != 0 || cols != 0) {
      rows = 0
      cols = 0
      elements.clear()
      // Keep dimensionInput as is, let the user correct it
    }
  }

  fun updateElement(r: Int, c: Int, value: String) {
    println("updateElement Start: r=$r, c=$c, value='$value', rows=$rows, cols=$cols, elements.size=${elements.size}") // Log input
    // Check bounds carefully
    if (r >= 0 && r < rows && c >= 0 && c < cols) {
      // Ensure the outer list has the row index
      if (r < elements.size) {
        // Ensure the inner list has the column index (should be guaranteed by structure)
        if (c < elements[r].size) {
          println("updateElement: Updating elements[$r][$c] = '$value'") // Log update action
          elements[r][c] = value
        } else {
          println("updateElement Error: Column index $c out of bounds for inner list size ${elements[r].size}") // Log bounds error
        }
      } else {
        println("updateElement Error: Row index $r out of bounds for outer list size ${elements.size}") // Log bounds error
      }
    } else {
      println("updateElement Error: Row $r or Col $c out of bounds (Rows: $rows, Cols: $cols)") // Log bounds error
    }
    println("updateElement End: Element at ($r,$c) is now '${getElement(r,c)}'") // Log result (calls getElement)
  }

  fun getElement(r: Int, c: Int): String {
    // Check bounds carefully for getting elements too
    return if (r >= 0 && r < rows && c >= 0 && c < cols && r < elements.size && c < elements[r].size) {
      elements[r][c]
    } else {
      "" // Return empty string or handle error appropriately if bounds are invalid
    }
  }

  // Get an immutable copy (List<List<Double>>) for calculations
  // Returns null if any element is not a valid number
  fun getImmutableDoubleCopy(): List<List<Double>>? {
    val result = mutableListOf<List<Double>>()
    try {
      for (rowList in elements) {
        val doubleRow = rowList.map { it.toDoubleOrNull() ?: throw NumberFormatException("Invalid number: $it") }
        result.add(doubleRow)
      }
      return result.toList() // Make immutable
    } catch (e: NumberFormatException) {
      println("Error converting matrix to double: ${e.message}")
      return null // Indicate failure
    }
  }

  // Creates a MatrixState from a List<List<Double>> result
  fun setFromDoubleList(data: List<List<Double>>) {
    val r = data.size
    val c = if (r > 0) data[0].size else 0

    rows = r
    cols = c
    dimensionInput = "${r}x${c}" // Update dimension string
    elements.clear()
    data.forEach { rowData ->
      elements.add(rowData.map { it.toString() }.toMutableList())
    }
  }


  fun reset() {
    rows = 0
    cols = 0
    elements.clear()
    dimensionInput = ""
  }
}