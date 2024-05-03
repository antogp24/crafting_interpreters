package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

    static boolean had_error = false;

    public static void main(String args[]) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            run_file(args[0]);
        } else {
            run_prompt();
        }
    }

    private static void run_file(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (had_error) System.exit(65);
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

        System.out.print("Tokens: { ");
        for (Token token : tokens) {
            System.out.print(token.toString() + " ");
        }
        System.out.println("} ");

        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();
        AstPrinter.print(expression);
    }
    
    static void error(int line, String message) {
        report(line, "", message);
    }

    static void report(int line, String where, String message) {
        System.err.printf("[%i] Error%s: %s\n", line, where, message);
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