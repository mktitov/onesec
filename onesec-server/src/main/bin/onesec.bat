@echo off
cd %ONESEC_HOME%

REM   Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
REM   reserved.


if not "%OS%"=="Windows_NT" goto win9xStart
:winNTStart
@setlocal

rem %~dp0 is name of current script under NT
set DEFAULT_ONESEC_HOME=%~dp0

rem : operator works similar to make : operator
set DEFAULT_ONESEC_HOME=%DEFAULT_ONESEC_HOME%\..

if "%ONESEC_HOME%"=="" set ONESEC_HOME=%DEFAULT_ONESEC_HOME%
set DEFAULT_ONESEC_HOME=

rem Need to check if we are using the 4NT shell...
if "%@eval[2+2]" == "4" goto setup4NT

rem On NT/2K grab all arguments at once
set OSSSERV_CMD_LINE_ARGS=%*
goto doneStart

:setup4NT
set OSSSERV_CMD_LINE_ARGS=%$
goto doneStart

:win9xStart
rem Slurp the command line arguments.  This loop allows for an unlimited number of 
rem agruments (up to the command line limit, anyway).

set OSSSERV_CMD_LINE_ARGS=

:setupArgs
if %1a==a goto doneStart
set OSSSERV_CMD_LINE_ARGS=%OSSSERV_CMD_LINE_ARGS% %1
shift
goto setupArgs

:doneStart
rem This label provides a place for the argument list loop to break out 
rem and for NT handling to skip to.

rem find ONESEC_HOME
if not "%ONESEC_HOME%"=="" goto checkJava

rem check for ant in Program Files on system drive
if not exist "%SystemDrive%\Program Files\ant" goto checkSystemDrive
set ONESEC_HOME=%SystemDrive%\Program Files\simcon
goto checkJava

:checkSystemDrive
rem check for ant in root directory of system drive
if not exist %SystemDrive%\simcon\nul goto checkCDrive
set ONESEC_HOME=%SystemDrive%\simcon
goto checkJava

:checkCDrive
rem check for ant in C:\simcon for Win9X users
if not exist C:\simcon\nul goto noSimConHome
set ONESEC_HOME=C:\simcon
goto checkJava

:noSimConHome
echo ONESEC_HOME is not set and simcon could not be located. Please set ONESEC_HOME.
goto end

:checkJava
set _JAVACMD=%JAVACMD%
set LOCALCLASSPATH=%CLASSPATH%
for %%i in ("lib\*.jar") do call "%ONESEC_HOME%\lcp.bat" %%i

if "%JAVA_HOME%" == "" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java
if not exist "%_JAVACMD%.exe" echo Error: "%_JAVACMD%.exe" not found - check JAVA_HOME && goto end
if exist "%JAVA_HOME%\lib\tools.jar" call "%ONESEC_HOME%\lcp.bat" %JAVA_HOME%\lib\tools.jar
if exist "%JAVA_HOME%\lib\classes.zip" call "%ONESEC_HOME%\lcp.bat" %JAVA_HOME%\lib\classes.zip
goto runSimCon

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java
echo.
echo Warning: JAVA_HOME environment variable is not set.
echo   If build fails because sun.* classes could not be found
echo   you will need to set the JAVA_HOME environment variable
echo   to the installation directory of java.
echo.

:runSimCon
echo ONESEC_HOME (%ONESEC_HOME%)
cd %ONESEC_HOME%
set CLASSPATH="%LOCALCLASSPATH%"
rem echo %CLASSPATH%
"%_JAVACMD%"  -Donesec.home="%ONESEC_HOME%" org.onesec.server.helpers.StartServer %OSSSERV_CMD_LINE_ARGS%

set LOCALCLASSPATH=
set _JAVACMD=
set OSSSERV_CMD_LINE_ARGS=

if not "%OS%"=="Windows_NT" goto mainEnd
:winNTend
@endlocal

:mainEnd

