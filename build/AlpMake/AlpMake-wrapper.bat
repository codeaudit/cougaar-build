@echo off
rem %1 is SOURCE DIR
rem %2 is DESTINATION DIR
rem %3 is any other flag for AlpMake.
rem %4 is any other flag for AlpMake.

del alp_compile.*

javac *.java
java AlpMake -s %1 -d %2    %3 %4 %5 %6 %7 %8 %9


alp_compile.bat
