@ECHO OFF

:: main class and jar file
SET MAIN=EphemerisParser
SET EXEC=ephparse.jar

:: source, class, lib, and root directories
SET SRCDIR=src
SET BINDIR=bin
SET LIBDIR=lib
SET COMPILE_ROOT=%cd%
SET MAINSRC=%SRCDIR%\%MAIN%.java

ECHO setting compilers and flags

:: jar and java commands
SET JARX=jar -xf
SET JARC=jar -cfm
SET JC=javac

:: build list of libs
SET LIBS=
FOR /F %%D in ('dir /b libs\*.jar') DO CALL SET "LIBS=%%LIBS%%%COMPILE_ROOT%\libs\%%D;"

:: manifest filename
SET MANIFEST=Manifest.txt

:: set some flags
SET LFLAGS=-cp %LIBS%%BINDIR%;.
SET SFLAGS=-sourcepath %SRCDIR%
SET DFLAGS=-d %BINDIR%

:: make build directory
ECHO creating build directory
MD %BINDIR%

:: build code from source
ECHO compiling source code from .\%SRCDIR% to .\%BINDIR%
CALL %JC% %LFLAGS% %SFLAGS% %DFLAGS% %MAINSRC%

:: head into build directory to unpack jars
ECHO entering .\%BINDIR%
CD %BINDIR%

FOR /F %%D in ('dir /b ..\libs\*.jar') DO (ECHO unpacking %%D & CALL %JARX% ..\libs\%%D)

:: dump main class info into manifest
ECHO writing jar manifest file
ECHO Main-Class: %MAIN%>%MANIFEST%

:: compile jar file
CALL %JARC% %COMPILE_ROOT%\%EXEC% %MANIFEST% *

:: go back up and clean up
ECHO exiting build directory
CD %COMPILE_ROOT%
ECHO removing build directory
RD /S /Q %BINDIR%