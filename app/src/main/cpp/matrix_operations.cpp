#include <jni.h>
#include <vector>
#include <string>
#include <stdexcept>    // For std::runtime_error
#include <android/log.h> // For logging

// --- Include Eigen ---
// Eigen/Core includes basic matrix/vector classes
// Eigen/Dense includes more operations, often useful
#include <Eigen/Core>
#include <Eigen/Dense>
// --- End Include Eigen ---


// Macro for logging (same as before)
#define LOG_TAG "MatrixCalculatorJNI_Eigen"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


// --- Helper Function: Java Array (jobjectArray of jdoubleArray) to Eigen::MatrixXd ---
Eigen::MatrixXd javaToEigenMatrix(JNIEnv *env, jobjectArray javaMatrix) {
    jsize rows = env->GetArrayLength(javaMatrix);
    if (rows == 0) {
        // Return an empty Eigen matrix (0x0)
        return Eigen::MatrixXd(0, 0);
    }

    // Get the first row to determine the number of columns
    jdoubleArray firstRow = (jdoubleArray)env->GetObjectArrayElement(javaMatrix, 0);
    if (firstRow == nullptr) {
        // Handle potential null row (shouldn't happen with valid input)
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "First row of Java matrix is null");
        return Eigen::MatrixXd(0, 0); // Return empty on error
    }
    jsize cols = env->GetArrayLength(firstRow);
    env->DeleteLocalRef(firstRow); // Clean up local reference

    if (cols == 0) {
        // Return an empty Eigen matrix (rows x 0)
        return Eigen::MatrixXd(rows, 0);
    }

    // Create the Eigen matrix
    Eigen::MatrixXd eigenMatrix(rows, cols);

    // Iterate through Java array and populate Eigen matrix
    for (jsize i = 0; i < rows; ++i) {
        jdoubleArray javaRow = (jdoubleArray)env->GetObjectArrayElement(javaMatrix, i);
        if (javaRow == nullptr) {
            env->ThrowNew(env->FindClass("java/lang/NullPointerException"), ("Row " + std::to_string(i) + " of Java matrix is null").c_str());
            return Eigen::MatrixXd(0,0); // Error
        }
        if (env->GetArrayLength(javaRow) != cols) {
            env->DeleteLocalRef(javaRow); // Clean up before throwing
            throw std::runtime_error("Inconsistent column count in matrix at row " + std::to_string(i));
        }

        jdouble *elements = env->GetDoubleArrayElements(javaRow, nullptr);
        if (elements == nullptr) {
            env->DeleteLocalRef(javaRow);
            env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "Failed to get double array elements");
            return Eigen::MatrixXd(0,0); // Error
        }

        // Efficiently copy the row using Eigen's Map (treats raw C array as Eigen object)
        eigenMatrix.row(i) = Eigen::Map<Eigen::VectorXd>(elements, cols);

        // Release the Java array elements (JNI_ABORT means don't copy back changes)
        env->ReleaseDoubleArrayElements(javaRow, elements, JNI_ABORT);
        env->DeleteLocalRef(javaRow); // Clean up local reference for the row
    }

    return eigenMatrix;
}


// --- Helper Function: Eigen::MatrixXd to Java Array (jobjectArray of jdoubleArray) ---
jobjectArray eigenToJavaMatrix(JNIEnv *env, const Eigen::MatrixXd& eigenMatrix) {
    jsize rows = eigenMatrix.rows();
    jsize cols = eigenMatrix.cols();

    // Find the class for double[]
    jclass doubleArrayClass = env->FindClass("[D");
    if (doubleArrayClass == nullptr) {
        LOGE("Failed to find class [D");
        return nullptr; // Error: class not found
    }

    // Create the outer Java array (array of double[])
    jobjectArray javaMatrix = env->NewObjectArray(rows, doubleArrayClass, nullptr);
    if (javaMatrix == nullptr) {
        LOGE("Failed to create outer jobjectArray");
        return nullptr; // Error: out of memory?
    }

    // Iterate through Eigen matrix rows and create/populate Java double[] rows
    for (jsize i = 0; i < rows; ++i) {
        jdoubleArray javaRow = env->NewDoubleArray(cols);
        if (javaRow == nullptr) {
            LOGE("Failed to create inner jdoubleArray for row %d", i);
            // Clean up already created rows? Difficult. Return partially filled/null for now.
            return javaMatrix; // Error: out of memory?
        }

        // Copy data from Eigen row to Java row
        // Use eigenMatrix.row(i).data() to get a pointer to the start of the row's data
        // Note: Eigen matrices are column-major by default, but .row(i) gives a row vector
        // which might be a temporary or require careful handling if not contiguous.
        // A safer loop (though potentially less optimal than a direct region copy if possible):
        // OR, use SetDoubleArrayRegion with a temporary buffer if needed:
        std::vector<double> row_buffer(cols);
        // Eigen::Map<Eigen::VectorXd>(row_buffer.data(), cols) = eigenMatrix.row(i); // Map doesn't work easily here for assignment
        Eigen::VectorXd eigenRow = eigenMatrix.row(i); // Get the row vector
        for(jsize j=0; j<cols; ++j) {
            row_buffer[j] = eigenRow(j); // Copy element by element
        }

        env->SetDoubleArrayRegion(javaRow, 0, cols, row_buffer.data());

        // Add the completed Java row to the outer Java array
        env->SetObjectArrayElement(javaMatrix, i, javaRow);

        // Clean up the local reference to the inner row array (important!)
        env->DeleteLocalRef(javaRow);
    }

    return javaMatrix;
}


// --- JNI Implementation using Eigen ---

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_matrixcalculator_MatrixCalculatorBridge_addMatrices(
        JNIEnv *env,
        jobject /* this */, // For static methods in Kotlin companion object
        jobjectArray matrixA,
        jobjectArray matrixB) {
    try {
        Eigen::MatrixXd a = javaToEigenMatrix(env, matrixA);
        Eigen::MatrixXd b = javaToEigenMatrix(env, matrixB);

        // --- Dimension Check (Crucial before Eigen operation) ---
        if (a.rows() != b.rows() || a.cols() != b.cols()) {
            throw std::runtime_error("Matrix dimensions must match for addition (" +
                                     std::to_string(a.rows()) + "x" + std::to_string(a.cols()) + " vs " +
                                     std::to_string(b.rows()) + "x" + std::to_string(b.cols()) + ")");
        }

        // --- Eigen Operation ---
        Eigen::MatrixXd result = a + b;

        LOGD("Eigen addition successful: %ldx%ld", result.rows(), result.cols());
        return eigenToJavaMatrix(env, result);

    } catch (const std::exception& e) {
        LOGE("Error in addMatrices: %s", e.what());
        // Throw a Java exception back to Kotlin (more informative than just returning null)
        jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
        if (exceptionClass != nullptr) {
            env->ThrowNew(exceptionClass, e.what());
        }
        return nullptr; // Indicate error
    } catch (...) {
        // Catch any other unexpected C++ exceptions
        LOGE("Unknown error in addMatrices");
        jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
        if (exceptionClass != nullptr) {
            env->ThrowNew(exceptionClass, "Unknown C++ exception during matrix addition.");
        }
        return nullptr;
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_matrixcalculator_MatrixCalculatorBridge_subtractMatrices(
        JNIEnv *env,
        jobject /* this */,
        jobjectArray matrixA,
        jobjectArray matrixB) {
    try {
        Eigen::MatrixXd a = javaToEigenMatrix(env, matrixA);
        Eigen::MatrixXd b = javaToEigenMatrix(env, matrixB);

        // --- Dimension Check ---
        if (a.rows() != b.rows() || a.cols() != b.cols()) {
            throw std::runtime_error("Matrix dimensions must match for subtraction (" +
                                     std::to_string(a.rows()) + "x" + std::to_string(a.cols()) + " vs " +
                                     std::to_string(b.rows()) + "x" + std::to_string(b.cols()) + ")");
        }

        // --- Eigen Operation ---
        Eigen::MatrixXd result = a - b;

        LOGD("Eigen subtraction successful: %ldx%ld", result.rows(), result.cols());
        return eigenToJavaMatrix(env, result);

    } catch (const std::exception& e) {
        LOGE("Error in subtractMatrices: %s", e.what());
        jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
        if (exceptionClass != nullptr) {
            env->ThrowNew(exceptionClass, e.what());
        }
        return nullptr;
    } catch (...) {
        LOGE("Unknown error in subtractMatrices");
        jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
        if (exceptionClass != nullptr) {
            env->ThrowNew(exceptionClass, "Unknown C++ exception during matrix subtraction.");
        }
        return nullptr;
    }
}


extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_matrixcalculator_MatrixCalculatorBridge_multiplyMatrices(
        JNIEnv *env,
        jobject /* this */,
        jobjectArray matrixA,
        jobjectArray matrixB) {
    try {
        Eigen::MatrixXd a = javaToEigenMatrix(env, matrixA);
        Eigen::MatrixXd b = javaToEigenMatrix(env, matrixB);

        // --- Dimension Check ---
        // Matrix multiplication A * B requires A.cols == B.rows
        if (a.cols() != b.rows()) {
            throw std::runtime_error("Matrix dimensions incompatible for multiplication (A.cols " +
                                     std::to_string(a.cols()) + " != B.rows " + std::to_string(b.rows()) + ")");
        }

        // --- Eigen Operation ---
        Eigen::MatrixXd result = a * b;

        LOGD("Eigen multiplication successful: %ldx%ld", result.rows(), result.cols());
        return eigenToJavaMatrix(env, result);

    } catch (const std::exception& e) {
        LOGE("Error in multiplyMatrices: %s", e.what());
        jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
        if (exceptionClass != nullptr) {
            env->ThrowNew(exceptionClass, e.what());
        }
        return nullptr;
    } catch (...) {
        LOGE("Unknown error in multiplyMatrices");
        jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
        if (exceptionClass != nullptr) {
            env->ThrowNew(exceptionClass, "Unknown C++ exception during matrix multiplication.");
        }
        return nullptr;
    }
}