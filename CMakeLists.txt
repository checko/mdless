cmake_minimum_required(VERSION 3.14)
project(mdless VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Compiler warnings
if(CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang")
    add_compile_options(-Wall -Wextra -Wpedantic)
endif()

# Source files
set(SOURCES
    src/main.cpp
    src/markdown.cpp
    src/terminal.cpp
    src/viewer.cpp
)

# Create executable
add_executable(mdless ${SOURCES})

# Install target
install(TARGETS mdless DESTINATION bin)
