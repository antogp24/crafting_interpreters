@echo off

del /q src\Expr.java
del /q src\Stmt.java
javac -d bin -sourcepath . GenerateAst.java
if %ERRORLEVEL%==0 java -cp bin GenerateAst src

javac -d bin -sourcepath src src/*.java
if %ERRORLEVEL%==0 java -cp bin src.Lox
