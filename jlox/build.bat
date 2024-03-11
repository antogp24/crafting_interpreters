@echo off

javac -d bin -sourcepath src src/*.java
if %ERRORLEVEL%==0 java -cp bin src.Lox
