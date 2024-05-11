package src;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static src.TokenType.*;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;   // Points to first char in the current lexeme.
    private int current = 0; // Points to the current char in the source.
    private int line = 1;

    private static final Map<String, TokenType> keywords = new HashMap<>();
    static {
        keywords.put("and",      AND);
        keywords.put("break",    BREAK);
        keywords.put("class",    CLASS);
        keywords.put("continue", CONTINUE);
        keywords.put("do",       DO);
        keywords.put("else",     ELSE);
        keywords.put("false",    FALSE);
        keywords.put("for",      FOR);
        keywords.put("fun",      FUN);
        keywords.put("if",       IF);
        keywords.put("nil",      NIL);
        keywords.put("or",       OR);
        keywords.put("print",    PRINT);
        keywords.put("println",  PRINTLN);
        keywords.put("return",   RETURN);
        keywords.put("super",    SUPER);
        keywords.put("this",     THIS);
        keywords.put("true",     TRUE);
        keywords.put("var",      VAR);
        keywords.put("while",    WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> get_tokens() {
        while (current < source.length()) {
            this.start = this.current;
            scan_token();
        }

        tokens.add(new Token(TokenType.EOF, "", null, this.line));
        return tokens;
    }

    void scan_token() {
        int c = advance();

        switch (c) {
            case '(': add_token(LEFT_PAREN); break;
            case ')': add_token(RIGHT_PAREN); break;
            case '{': add_token(LEFT_BRACE); break;
            case '}': add_token(RIGHT_BRACE); break;
            case '&': add_token(match('&') ? AND : BITWISE_AND); break;
            case '|': add_token(match('|') ? OR : BITWISE_OR); break;
            case '^': add_token(BITWISE_XOR); break;
            case '~': add_token(BITWISE_NOT); break;
            case ',': add_token(COMMA); break;
            case '.': add_token(DOT); break;
            case '-': add_token(MINUS); break;
            case '+': add_token(PLUS); break;
            case ';': add_token(SEMICOLON); break;
            case '*': add_token(STAR); break;
            case '?': add_token(QUESTION_MARK); break;
            case ':': add_token(COLON); break;
            case '=': add_token(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '!': add_token(match('=') ? BANG_EQUAL : BANG); break;
            case '<': {
                if (match('=')) {
                    add_token(LESS_EQUAL);
                } else if (match('<')) {
                    add_token(LEFT_SHIFT);
                } else {
                    add_token(LESS);
                }
            } break;
            case '>': {
                if (match('=')) {
                    add_token(GREATER_EQUAL);
                } else if (match('>')) {
                    add_token(RIGHT_SHIFT);
                } else {
                    add_token(GREATER);
                }
            } break;
            case '/':
                if (match('/')) {
                    // ignore single line comment
                    while (peek() != '\n' && !is_at_end()) current += 1;
                } else if (peek() == '*') {
                    scan_multiline_comment();
                } else {
                    add_token(SLASH);
                }
            break;

            // ignore whitespace
            case ' ':
            case '\t':
            case '\r':
            break;

            case '\n': line += 1; break;
            case '\"': scan_string_literal(); break;

            default:
                if (is_digit(c)) {
                    scan_number_literal();
                } else if (is_alpha(c)) {
                    scan_identifier();
                } else {
                    Lox.error(line, "Unexpected codepoint '" + c + "'.");
                }
            break;
        }
    }

    int peek() {
        if (current >= source.length()) return '\0';
        return source.codePointAt(current);
    }

    int peek_next() {
        if (current + 1 >= source.length()) return '\0';
        return source.codePointAt(current + 1);
    }

    int advance() {
        int c = source.codePointAt(current);
        current += 1;
        return c;
    }

    void add_token(TokenType type) {
        add_token(type, null);
    }

    void add_token(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    boolean match(char next) {
        if (is_at_end()) return false;
        if (source.charAt(current) != next) return false;

        current += 1;
        return true;
    }

    boolean is_digit(int c) {
        return c >= '0' && c <= '9';
    }

    boolean is_alpha(int c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               (c == '_') ||
                Character.isAlphabetic(c);
    }

    boolean is_alpha_numeric(int c) {
        return is_digit(c) || is_alpha(c);
    }

    void scan_string_literal() {
        while (peek() != '\"' && !is_at_end()) {
            if (peek() == '\n') this.line += 1;
            if (peek() == '\\') {
                current += 1;
                switch (peek()) {
                    case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                    case 't': case 'b': case 'n': case 'r': case 'f': case '\'': case '\"': case '\\':
                        break;
                    default:
                        Lox.error(line, "Unrecognized escape sequence \\" + (char)peek() + ".");
                }
            }
            current += 1;
        }

        if (is_at_end()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // To skip lexing the closing "
        current += 1;

        String value = source.substring(start + 1, current - 1);
        add_token(STRING, value);
    }
    
    void scan_number_literal() {
        while (is_digit(peek())) current += 1;

        if (peek() == '.' && is_digit(peek_next())) {
            advance();
            while (is_digit(peek())) current += 1;
        }

        double value = Double.parseDouble(source.substring(start, current));
        add_token(NUMBER, value);
    }

    void scan_identifier() {
        while (is_alpha_numeric(peek())) {
            current += 1;
        }

        String name = source.substring(start, current);
        TokenType type = keywords.get(name);
        if (type == null) type = IDENTIFIER;

        add_token(type);
    }

    void scan_multiline_comment() {
        int nested_count = 1;
        for (;; current += 1) {
            if (peek() == '\n') {
                line += 1;
                continue;
            }
            if (peek() == '/' && peek_next() == '*') {
                nested_count += 1;
            }
            if (peek() == '*' && peek_next() == '/') {
                nested_count -= 1;
            }
            if (nested_count == 0) {
                current += 2;
                break;
            }
        }
    }

    boolean is_at_end() {
        return current >= source.length();
    }
}