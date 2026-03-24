cmake_minimum_required(VERSION 3.14)
project(mdless VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Compiler warnings
if(CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang")
    add_compile_options(-Wall -Wextra -Wpedantic)
endif()

# Platform-specific flags
if(WIN32)
    # Windows: Enable UTF-8 support
    add_compile_options(-finput-charset=UTF-8 -fexec-charset=UTF-8)
    # Link against kernel32 for Windows API
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -lkernel32")
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

# Copy MinGW DLLs to build directory for Windows
if(WIN32 AND CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang")
    # Find g++.exe to get the bin directory
    get_filename_component(GCC_BIN_DIR "${CMAKE_CXX_COMPILER}" DIRECTORY)
    
    # Copy required MinGW DLLs
    if(EXISTS "${GCC_BIN_DIR}/libgcc_s_seh-1.dll")
        add_custom_command(TARGET mdless POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
                "${GCC_BIN_DIR}/libgcc_s_seh-1.dll"
                $<TARGET_FILE_DIR:mdless>
        )
    endif()
    
    if(EXISTS "${GCC_BIN_DIR}/libstdc++-6.dll")
        add_custom_command(TARGET mdless POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
                "${GCC_BIN_DIR}/libstdc++-6.dll"
                $<TARGET_FILE_DIR:mdless>
        )
    endif()
endif()
