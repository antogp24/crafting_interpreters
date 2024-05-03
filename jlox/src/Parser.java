package src;

import java.util.List;
import static src.TokenType.*;

class Parser {
    private final List<Token> tokens;
    private int current = 0;

    private static class ParseError extends RuntimeException {};

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError e) {
            return null;
        }
    }

    private Expr expression() {
        return comma_operator();
    }

    private Expr comma_operator() {
        Expr expr = equality();

        while (match(COMMA)) {
            expr = equality();
        }

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
