package src;

public class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        if (type == TokenType.STRING) {
            return "(" + type + " " + lexeme + " " + literal + ")";
        }
        return "(" + type + " '" + lexeme + "' " + literal + ")";
    }
}