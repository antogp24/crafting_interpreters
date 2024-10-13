@echo off

cl >nul 2>&1 && (
    echo Using MSVC cl
) || (
    call vcvars64.bat >nul 2>&1 && (
        echo Initialized vcvars64.bat
    ) || (
        echo MSVC compiler isn't installed
        exit /b 1
    )
)

pushd bin
    set CLFLAGS=/nologo /Zi /Fe:clox.exe /std:c11
    set LDFLAGS=/INCREMENTAL:NO /DEBUG:FULL /SUBSYSTEM:CONSOLE
    cl ..\src\*.c %CLFLAGS% /link %LDFLAGS%
popd
