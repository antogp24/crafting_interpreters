package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

    static private final Interpreter interpreter = new Interpreter();
    static boolean REPL;
    static boolean had_error = false;
    static boolean had_runtime_error = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            Lox.REPL = false;
            run_file(args[0]);
        } else {
            Lox.REPL = true;
            run_prompt();
        }
    }

    private static void run_file(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (had_error) System.exit(65);
        if (had_runtime_error) System.exit(70);
    }

    private static void run_prompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while (true) {
            final String color = "\033[96m";
            final String reset = "\033[0m";
            System.out.print(color + "lox>" + reset + " ");

            String line = reader.readLine();
            if (line == null || line.isEmpty()) break;
            run(line);
            had_error = false;
        }
    }

    private static void run(String source) throws IOException {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.get_tokens();

        if (had_error) return;

        if (Lox.REPL) {
            System.out.print("Tokens: { ");
            for (Token token : tokens) {
                System.out.print(token.toString() + " ");
            }
            System.out.println("} ");
        }

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse_statements();

        if (had_error) return;

        for (Stmt stmt : statements) {
            if (Lox.REPL && stmt instanceof Stmt.Expression stmt_expr) {
                System.out.print("Ast Expr: ");
                AstPrinter.print(stmt_expr.expression);
            }
            if (Lox.REPL && stmt instanceof Stmt.Print print_stmt) {
                System.out.print(print_stmt.newline ? "Ast Println: " : "Ast Print: ");
                AstPrinter.print(print_stmt.expression);
            }
        }

        interpreter.interpret(statements);
    }
    
    static void error(int line, String message) {
        report(line, "", message);
    }

    static void runtime_error(RuntimeLoxError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        had_runtime_error = true;
    }

    static void report(int line, String where, String message) {
        System.err.printf("[line %d] Error%s: %s\n", line, where, message);
        had_error = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }
}