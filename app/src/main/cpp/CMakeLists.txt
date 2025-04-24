cmake_minimum_required(VERSION 3.31.6) # Match your installed version

# --- Set CMake Policy to allow deprecated FetchContent_Populate usage ---
# Do this *before* calling FetchContent_Populate
cmake_policy(SET CMP0169 OLD)

project("matrixcalculator")

# --- Set C++ Standard ---
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# --- Fetch Eigen using FetchContent (Populate Only) ---
include(FetchContent)

FetchContent_Declare(
        Eigen3
        GIT_REPOSITORY https://gitlab.com/libeigen/eigen.git
        GIT_TAG 3.4.0 # Or latest stable tag
        GIT_SHALLOW TRUE
)

# Use Populate only (allowed now because of CMP0169 OLD)
# This just downloads/extracts Eigen, does NOT run Eigen's CMakeLists.txt
FetchContent_Populate(Eigen3)

# Get the path to the downloaded source directory
FetchContent_GetProperties(Eigen3 SOURCE_DIR EIGEN3_SOURCE_DIR)
# --- End Fetch Eigen ---


# Define your JNI library
add_library(matrix_operations SHARED matrix_operations.cpp)

# --- Manually add Eigen include directory ---
target_include_directories(matrix_operations
        PRIVATE
        ${EIGEN3_SOURCE_DIR} # Add Eigen's root source dir as an include path
)

# Find and link the Android log library
find_library(log-lib log)

# Link necessary libraries (NO Eigen3::Eigen here)
target_link_libraries(matrix_operations
        PRIVATE
        ${log-lib}    # Link log library
)