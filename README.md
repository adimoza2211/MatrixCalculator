# Matrix Calculator - Android App

## Description

This is an Android application built with Kotlin and Jetpack Compose that allows users to perform basic matrix operations (addition, subtraction, multiplication) on two matrices. The core matrix calculations are implemented in C++ using the high-performance Eigen library and integrated into the Android app via the Java Native Interface (JNI).

## Features

* **Dimension Input:** Takes the dimensions (rows x columns) for two matrices (A and B).
* **Sequential Element Input:** Guides the user to input matrix elements one by one (`[row][col]`).
* **Supported Operations:**
    * Matrix Addition (+)
    * Matrix Subtraction (-)
    * Matrix Multiplication (*)
* **Input Validation:**
    * Checks for valid dimension format (`rows x cols`).
    * Validates dimension compatibility for the selected operation (e.g., matching dimensions for addition/subtraction, inner dimensions matching for multiplication).
    * Checks for non-numeric input before calculation.
* **Native Calculation:** Utilizes a C++ backend via JNI for performing matrix operations using the Eigen library.
* **Result Display:** Shows the resulting matrix after a successful calculation.
* **Reset Functionality:** Allows the user to clear all inputs and start over.

## Technology Stack

* **UI:** Jetpack Compose (Kotlin)
* **Language:** Kotlin (Android), C++ (Native Calculation)
* **Native Integration:** JNI (Java Native Interface)
* **Matrix Library:** Eigen (C++ Template Library for Linear Algebra)
* **Build System:** Gradle (Android), CMake (C++)
* **Architecture:** Basic MVVM-like state management within Composable functions (`MatrixState`, `MainScreen` state variables).

## Setup and Build

1.  **Prerequisites:**
    * Android Studio (latest stable version recommended)
    * Android NDK (install via Android Studio SDK Manager -> SDK Tools -> NDK (Side by side))
    * CMake (install via Android Studio SDK Manager -> SDK Tools -> CMake)
2.  **Clone the Repository:**
    ```bash
    git clone <your-repository-url>
    cd <repository-directory>
    ```
3.  **Open in Android Studio:** Open the cloned project folder in Android Studio.
4.  **Sync Gradle:** Allow Android Studio to sync the project with Gradle. This should automatically detect the NDK and CMake configurations in `app/build.gradle.kts`.
5.  **Build and Run:**
    * Select a target device (emulator or physical device).
    * Click the "Run 'app'" button (green play icon) or go to **Build -> Make Project**.
    * Gradle will trigger the CMake build process, which includes:
        * Fetching the Eigen library using `FetchContent`.
        * Compiling the C++ code (`matrix_operations.cpp`) using the NDK toolchain.
        * Linking against Eigen.
        * Packaging the native library (`libmatrix_operations.so`) into the APK.

## Native Code (JNI / Eigen)

* The core matrix operations are delegated to C++ for potential performance benefits and to demonstrate JNI usage.
* **`app/src/main/cpp/matrix_operations.cpp`**: Contains the JNI wrapper functions that convert Java arrays (`double[][]`) to Eigen matrices (`Eigen::MatrixXd`), perform the calculations using Eigen's operators (`+`, `-`, `*`), and convert the result back to a Java array.
* **`app/src/main/cpp/CMakeLists.txt`**: Configures the C++ build using CMake. It handles:
    * Setting the C++ standard (C++17).
    * Fetching the Eigen library using `FetchContent` (configured for header-only usage by bypassing Eigen's own build targets).
    * Defining the native shared library (`matrix_operations`).
    * Linking necessary libraries (like the Android log library).
* **`MatrixCalculatorBridge.kt`**: The Kotlin object that declares the `external fun` interfaces corresponding to the JNI functions and loads the native library (`System.loadLibrary("matrix_operations")`).

## Usage

1.  Launch the app.
2.  Enter the dimensions for Matrix A and Matrix B in the format "rows x cols" (e.g., "3x3").
3.  Click "Set Dimensions & Continue".
4.  The app will prompt you to enter the value for each element sequentially, starting with `Matrix A [0][0]`.
5.  Type the value and press the "Done" key on the keyboard or click the "Next Element" / "Finish Matrix" button.
6.  Repeat for all elements of Matrix A, then Matrix B.
7.  Once both matrices are entered, select the desired operation (+, -, *).
8.  Click "Calculate".
9.  If the inputs and dimensions are valid for the operation, the result matrix will be displayed. Otherwise, an error message will appear.
10. Click "Calculate New Matrices" or "Reset All" to start over.

## Future Improvements

* Add more matrix operations (e.g., determinant, inverse, transpose).
* Improve error handling and user feedback for invalid numeric input within the sequential input fields.
* Enhance UI/UX (e.g., visually display the matrix being filled during sequential input).
* Implement unit tests for `MatrixState` and potentially integration tests.
* Consider using Kotlin Coroutines for offloading JNI calls if they become time-consuming (unlikely for basic operations).
