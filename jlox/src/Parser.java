package src;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import static src.TokenType.*;

class Parser {
    private final List<Token> tokens;
    private int current = 0;
    private int loop_level = 0;

    private static class ParseError extends RuntimeException {};

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse_statements() {
        List<Stmt> statements = new ArrayList<>();

        while (!is_at_end()) {
            statements.add(declaration());
        }

        return statements;
    }

    Expr parse_expression() {
        try {
            return expression();
        } catch (ParseError e) {
            return null;
        }
    }

    private Stmt declaration() {
        try {
            if (match(FUN)) {
                if (check(LEFT_PAREN)) {
                    current--;
                    return expression_statement();
                }
                return fun_declaration();
            }
            if (match(VAR)) return var_declaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt fun_declaration() {
        Token name = consume(IDENTIFIER, "Expected function name.");
        List<Token> params = new ArrayList<>();

        consume(LEFT_PAREN, "Expected '(' after function name");
        if (!check(RIGHT_PAREN)) {
            do {
                if (params.size() >= 255) {
                    this.error(peek(), "Can't have more than 255 parameters.");
                }
                Token param_name = consume(IDENTIFIER, "Expected parameter name.");
                params.add(param_name);
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expected ')' in the function declaration.");

        consume(LEFT_BRACE, "Expected '{' in the function declaration.");
        List<Stmt> body = block_statement();

        return new Stmt.Function(name, params, body);
    }

    private Stmt var_declaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = null;

        if (match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after initializer.");

        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(IF)) return if_statement();
        if (match(WHILE)) return while_statement();
        if (match(FOR)) return for_statement();
        if (match(CONTINUE)) return continue_statement();
        if (match(BREAK)) return break_statement();
        if (match(PRINT)) return print_statement();
        if (match(PRINTLN)) return println_statement();
        if (match(RETURN)) return return_statement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block_statement());
        return expression_statement();
    }

    private Stmt continue_statement() {
        if (this.loop_level == 0) {
            String message = "Can't use continue statement outside a loop.";
            if (Lox.REPL) this.error(previous(), message);
            else Lox.error(previous(), message);
        }
        consume(SEMICOLON, "Expected ';' after continue statement.");
        return new Stmt.Continue();
    }

    private Stmt break_statement() {
        if (this.loop_level == 0) {
            String message = "Can't use break statement outside a loop.";
            if (Lox.REPL) this.error(previous(), message);
            else Lox.error(previous(), message);
        }
        consume(SEMICOLON, "Expected ';' after break statement.");
        return new Stmt.Break();
    }

    private void expect_do_or_block(String type) {
        switch (peek().type) {
            case LEFT_BRACE: break;
            case DO: {
                advance();
                if (peek().type == LEFT_BRACE) {
                    throw this.error(peek(), "Expected single statement after 'do' in " + type + ".");
                }
            } break;
            default: throw this.error(peek(), "Expected '{' or 'do' after " + type + ".");
        }
    }

    private Stmt for_statement() {
        this.loop_level += 1;

        boolean has_optional_parenthesis = false;
        if (check(LEFT_PAREN)) {
            advance();
            has_optional_parenthesis = true;
        }

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = var_declaration();
        } else {
            initializer = expression_statement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = parse_expression();
        }
        consume(SEMICOLON, "Expected ';' after loop condition '" + condition + "'.");

        Expr increment = null;
        if (!check(LEFT_BRACE) && !check(DO) && !check(RIGHT_PAREN)) {
            increment = parse_expression();
        }

        if (has_optional_parenthesis) {
            consume(RIGHT_PAREN, "Parenthesis are optional in the for statement: " +
                    "remove the trailing ')' or add the missing '('.");
        }

        expect_do_or_block("for loop");

        Stmt body = statement();

        if (increment != null) {
            Stmt.Expression inc_stmt = new Stmt.Expression(increment);
            if (body instanceof Stmt.Block block_stmt) {
                block_stmt.statements.add(inc_stmt);
            } else {
                body = new Stmt.Block(Arrays.asList(body, inc_stmt));
            }
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body, increment != null);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        this.loop_level -= 1;

        return body;
    }

    private Stmt while_statement() {
        this.loop_level += 1;

        Expr condition = parse_expression();
        expect_do_or_block("while loop");
        Stmt body = statement();

        this.loop_level -= 1;
        return new Stmt.While(condition, body, false);
    }

    private Stmt if_statement() {
        Expr condition = parse_expression();
        expect_do_or_block("if statement");
        Stmt then_branch = statement();

        List<Else_If> else_ifs = new ArrayList<>();
        while (check(ELSE)) {
            if (peek_next().type == IF) {
                current += 2;
                Expr else_if_condition = parse_expression();
                expect_do_or_block("else-if part of the if statement");
                Stmt else_if_then_branch = statement();
                Else_If else_if = new Else_If(else_if_condition, else_if_then_branch);
                else_ifs.add(else_if);
            } else {
                break;
            }
        }

        Stmt else_branch = null;
        if (match(ELSE)) {
            expect_do_or_block("else part of the if statement");
            else_branch = statement();
        }

        return new Stmt.If(condition, then_branch, else_ifs, else_branch);
    }

    private Stmt print_statement() {
        Expr value = parse_expression();
        consume(SEMICOLON, "Expected ';' after the printed value.");
        return new Stmt.Print(value, false);
    }

    private Stmt println_statement() {
        Expr value = parse_expression();
        consume(SEMICOLON, "Expected ';' after the printed value.");
        return new Stmt.Print(value, true);
    }

    private Stmt return_statement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = parse_expression();
        }
        consume(SEMICOLON, "Expected ';' after the return statement.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt expression_statement() {
        Expr value = parse_expression();
        consume(SEMICOLON, "Expected ';' after the expression statement.");
        return new Stmt.Expression(value);
    }

    private List<Stmt> block_statement() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !is_at_end()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expected '}' to end the block statement.");
        return statements;
    }

    private Expr expression() {
        return comma_operator();
    }

    private Expr comma_operator() {
        Expr expr = assignment();

        while (match(COMMA)) {
            expr = assignment();
        }

        return expr;
    }

    private Expr assignment() {
        Expr expr = logical_or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            this.error(equals, "Invalid assignment target '" + expr.toString() + "'.");
        }
        return expr;
    }

    private Expr logical_or() {
        Expr expr =  logical_and();

        boolean is_or = false;
        while (match(OR)) {
            Token operator = previous();
            Expr right = logical_and();
            expr = new Expr.Logical(expr, operator, right);
            is_or = true;
        }

        if (is_or && match(QUESTION_MARK)) return ternary_operator(expr);

        return expr;
    }

    private Expr logical_and() {
        Expr expr = equality();

        boolean is_and = false;
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
            is_and = true;
        }

        if (is_and && match(QUESTION_MARK)) return ternary_operator(expr);

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        boolean is_equality = false;
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
            is_equality = true;
        }

        if (is_equality && match(QUESTION_MARK)) return ternary_operator(expr);

        return expr;
    }

    private Expr comparison() {
        Expr expr = bitwise_ops();

        boolean is_comparison = false;
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = bitwise_ops();
            expr = new Expr.Binary(expr, operator, right);
            is_comparison = true;
        }

        if (is_comparison && match(QUESTION_MARK)) return ternary_operator(expr);

        return expr;
    }

    private Expr bitwise_ops() {
        Expr expr = bitwise_shift();

        while (match(BITWISE_XOR, BITWISE_OR, BITWISE_AND)) {
            Token operator = previous();
            Expr right = bitwise_shift();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr bitwise_shift() {
        Expr expr = term();

        while (match(LEFT_SHIFT, RIGHT_SHIFT)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }


    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, BITWISE_NOT, MINUS, PLUS)) {
            Token operator = previous();
            Expr right = unary();
            Expr expr = new Expr.Unary(operator, right);
            if (operator.type == BANG && match(QUESTION_MARK)) return ternary_operator(expr);
            return expr;
        }

        return call();
    }

    private Expr call() {
        Expr expr = lambda();

        while (true) {
            if (match(LEFT_PAREN)) {
                List<Expr> arguments = new ArrayList<>();
                if (!check(RIGHT_PAREN)) {
                    do {
                        if (arguments.size() >= 255) {
                            this.error(peek(), "Can't have more than 255 arguments.");
                        }
                        // Always call 1 level of precedence above the comma operator.
                        Expr argument_expr = assignment();
                        arguments.add(argument_expr);
                    } while (match(COMMA));
                }
                Token paren = consume(RIGHT_PAREN, "Expect ')' after function call.");
                expr = new Expr.Call(expr, paren, arguments);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr lambda() {
        if (match(FUN)) {
            Token token = previous();
            List<Token> params = new ArrayList<>();

            consume(LEFT_PAREN, "Expected '(' after 'fun' in lambda.");
            if (!check(RIGHT_PAREN)) {
                do {
                    if (params.size() >= 255) {
                        this.error(peek(), "Can't have more than 255 parameters.");
                    }
                    Token param_name = consume(IDENTIFIER, "Expected parameter name.");
                    params.add(param_name);
                } while (match(COMMA));
            }
            consume(RIGHT_PAREN, "Expected ')' after parameters in lambda.");

            consume(LEFT_BRACE, "Expected '{' after ')' in lambda.");
            List<Stmt> statements = block_statement();

            return new Expr.Lambda(token, params, statements);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            boolean is_function = (peek().type == LEFT_PAREN);
            return new Expr.Variable(previous(), is_function);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after the expression.");
            Expr grouping = new Expr.Grouping(expr);
            if (match(QUESTION_MARK)) return ternary_operator(grouping);
            return grouping;
        }

        throw this.error(peek(), "Expect expression.");
    }

    private Expr ternary_operator(Expr condition) {
        Expr if_true = expression();
        consume(COLON, "Expected a colon in the ternary operator.");
        Expr otherwise = expression();
        return new Expr.Ternary(condition, if_true, otherwise);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw this.error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!is_at_end()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case CONTINUE:
                case BREAK:
                case FUN:
                case FOR:
                case IF:
                case PRINT:
                case PRINTLN:
                case RETURN:
                case VAR:
                case WHILE:
                    return;
            }

            advance();
        }
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (is_at_end()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!is_at_end()) current++;
        return previous();
    }

    private boolean is_at_end() {
        return peek().type == EOF;
    }

    private Token peek() {
        return this.tokens.get(current);
    }

    private Token peek_next() {
        return this.tokens.get(current + 1);
    }

    private Token previous() {
        return this.tokens.get(current - 1);
    }
}
