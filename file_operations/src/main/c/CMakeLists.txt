# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.10.2)

project("rootoperations")

add_library(rootoperations SHARED rootoperations.c)

find_library(log-lib log)

target_link_libraries(rootoperations ${log-lib})
