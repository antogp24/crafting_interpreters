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
            if (match(VAR)) return var_declaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
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
        if (match(BREAK)) return break_statement();
        if (match(PRINT)) return print_statement();
        if (match(PRINTLN)) return println_statement();
        if (match(LEFT_BRACE)) return block_statement();
        return expression_statement();
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

    private Stmt for_statement() {
        this.loop_level += 1;

        if (check(LEFT_PAREN)) advance();

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

        if (check(RIGHT_PAREN)) advance();

        switch (peek().type) {
            case LEFT_BRACE: break;
            case DO: {
                advance();
                if (peek().type == LEFT_BRACE) {
                    throw this.error(peek(), "Expected single statement after 'do' in for loop.");
                }
            } break;
            default: throw this.error(peek(), "Expected '{' or 'do' after loop increment '" + increment + "'.");
        }

        Stmt body = statement();
        if (body instanceof Stmt.Block block_stmt) {
            body = new Stmt.Block(block_stmt.statements);
        }

        if (increment != null) {
            Stmt.Expression inc_stmt = new Stmt.Expression(increment);
            if (body instanceof Stmt.Block block_stmt) {
                block_stmt.statements.add(inc_stmt);
            } else {
                body = new Stmt.Block(Arrays.asList(body, inc_stmt));
            }
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        this.loop_level -= 1;

        return body;
    }

    private Stmt while_statement() {
        this.loop_level += 1;

        Expr condition = parse_expression();
        switch (peek().type) {
            case LEFT_BRACE: break;
            case DO: {
                advance();
                if (peek().type == LEFT_BRACE) {
                    throw this.error(peek(), "Expected single statement after 'do' in while loop.");
                }
            } break;
            default: throw this.error(peek(), "Expected '{' or 'do' after while loop.");
        }
        Stmt body = statement();
        if (body instanceof Stmt.Block block_body) {
            body = new Stmt.Block(block_body.statements);
        }

        this.loop_level -= 1;
        return new Stmt.While(condition, body);
    }

    private Stmt if_statement() {
        Expr condition = parse_expression();
        switch (peek().type) {
            case LEFT_BRACE: break;
            case DO: {
                advance();
                if (peek().type == LEFT_BRACE) {
                    throw this.error(peek(), "Expected single statement after 'do' in if statement.");
                }
            } break;
            default: throw this.error(peek(), "Expected '{' or 'do' after the if statement.");
        }
        Stmt then_branch = statement();

        Stmt else_branch = null;
        if (match(ELSE)) {
            else_branch = statement();
        }

        return new Stmt.If(condition, then_branch, else_branch);
    }

    private Stmt print_statement() {
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after the printed value.");
        return new Stmt.Print(value, false);
    }

    private Stmt println_statement() {
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after the printed value.");
        return new Stmt.Print(value, true);
    }

    private Stmt expression_statement() {
        Expr value = parse_expression();
        consume(SEMICOLON, "Expected ';' after the expression statement.");
        return new Stmt.Expression(value);
    }

    private Stmt.Block block_statement() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !is_at_end()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expected '}' to end the block statement.");
        return new Stmt.Block(statements);
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
        Expr expr = bitwise_comparison();

        boolean is_comparison = false;
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = bitwise_comparison();
            expr = new Expr.Binary(expr, operator, right);
            is_comparison = true;
        }

        if (is_comparison && match(QUESTION_MARK)) return ternary_operator(expr);

        return expr;
    }

    private Expr bitwise_comparison() {
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
            return new Expr.Variable(previous());
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
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
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

    private Token previous() {
        return this.tokens.get(current - 1);
    }
}
