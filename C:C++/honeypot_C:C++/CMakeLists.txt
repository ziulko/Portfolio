cmake_minimum_required(VERSION 3.24)
project(honeypot_C_C__)

set(CMAKE_CXX_STANDARD 23)

add_executable(honeypot_C_C__ main.cpp logger.hpp honeypot.hpp logger.cpp honeypot.cpp)
