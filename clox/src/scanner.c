#include "scanner.h"
#include <string.h>

Scanner scanner_make(const char *source_code) {
    return (Scanner){
        .start = source_code,
        .current = source_code,
        .line = 1,
        .column = 0,
    };
}

#define token_make(type) scanner_token_make(scanner, type)
static Token scanner_token_make(Scanner* scanner, TokenType type) {
    return (Token){
        .type = type,
        .start = scanner->start,
        .length = (size_t)(scanner->current - scanner->start),
        .line = scanner->line,
        .column = scanner->column,
    };
}

#define is_at_end() scanner_is_at_end(scanner)
static bool scanner_is_at_end(Scanner* scanner) {
    return *scanner->current == '\0';
}

#define token_error(message) scanner_token_error(scanner, message)
static Token scanner_token_error(Scanner* scanner, const char *message) {
    return (Token){
        .type = TOKEN_ERROR,
        .start = message,
        .length = strlen(message),
        .line = scanner->line,
        .column = scanner->column,
    };
}

#define advance() scanner_advance(scanner)
static char scanner_advance(Scanner* scanner) {
    scanner->current++;
    scanner->column++;
    return scanner->current[-1];
}

#define match(expected) scanner_match(scanner, expected)
static bool scanner_match(Scanner *scanner, char expected) {
    if (is_at_end()) return false;
    if (*scanner->current != expected) return false;

    scanner->current++;
    return true;
}

#define peek() scanner_peek(scanner)
static char scanner_peek(Scanner *scanner) {
    return *scanner->current;
}

#define peek_next() scanner_peek_next(scanner)
static char scanner_peek_next(Scanner *scanner) {
    if (is_at_end()) return '\0';
    return scanner->current[1];
}

static bool is_digit(char c) {
    return c >= '0' && c <= '9';
}

static bool is_alpha(char c) {
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z') ||
           (c == '_');
}

static bool is_alphanum(char c) {
    return is_digit(c) || is_alpha(c);
}

static void skip_whitespace(Scanner* scanner) {
    for (;;) {
        char c = peek();
        switch (c) {
            case ' ':
            case '\r':
            case '\t': advance(); break;
            case '\n': {
                scanner->line++;
                scanner->column = -1;
                advance();
            } break;
            case '/': {
                if (peek_next() == '/') {
                    while (peek() != '\n' && !is_at_end()) advance();
                } else {
                    return;
                }
            } break;
            default: return;
        }
    }
}

static Token scan_string(Scanner* scanner) {
    while (peek() != '"' && !is_at_end()) {
        if (peek() == '\n') {
            scanner->line++;
            scanner->column = -1;
        }
        advance();
    }

    if (is_at_end()) return token_error("Unterminated string.");

    advance();
    return token_make(TOKEN_STRING);
}

static Token scan_number(Scanner* scanner) {
    while (is_digit(peek())) advance();

    if (peek() == '.' && is_digit(peek_next())) {
        advance();
        while (is_digit(peek())) advance();
    }

    return token_make(TOKEN_NUMBER);
}

#define check_keyword(offset, rest, type) scanner_check_keyword(scanner, offset, sizeof(rest)-1, rest, type)
static TokenType scanner_check_keyword(Scanner* scanner, uint32_t offset, uint32_t length, const char *rest, TokenType type) {
    if (scanner->current - scanner->start == offset + length &&
        memcmp(scanner->start + offset, rest, length) == 0) {
        return type;
    }
    return TOKEN_IDENTIFIER;
}

static TokenType identifier_type(Scanner* scanner) {
    switch (scanner->start[0]) {
        case 'a': return check_keyword(1, "nd", TOKEN_AND);
        case 'c': return check_keyword(1, "lass", TOKEN_CLASS);
        case 'e': return check_keyword(1, "lse", TOKEN_ELSE);
        case 'f': {
            if (scanner->current - scanner->start > 1) {
                switch (scanner->start[1]) {
                    case 'a': return check_keyword(2, "lse", TOKEN_FALSE);
                    case 'o': return check_keyword(2, "r", TOKEN_FOR);
                    case 'u': return check_keyword(2, "n", TOKEN_FUN);
                }
            }
        } break;
        case 'i': return check_keyword(1, "f", TOKEN_IF);
        case 'n': return check_keyword(1, "il", TOKEN_NIL);
        case 'o': return check_keyword(1, "r", TOKEN_OR);
        case 'p': return check_keyword(1, "rint", TOKEN_PRINT);
        case 'r': return check_keyword(1, "eturn", TOKEN_RETURN);
        case 's': return check_keyword(1, "uper", TOKEN_SUPER);
        case 't': {
            if (scanner->current - scanner->start > 1) {
                switch (scanner->start[1]) {
                    case 'h': return check_keyword(2, "is", TOKEN_THIS);
                    case 'r': return check_keyword(2, "ue", TOKEN_TRUE);
                }
            }
        } break;
        case 'v': return check_keyword(1, "ar", TOKEN_VAR);
        case 'w': return check_keyword(1, "hile", TOKEN_WHILE);
    }
    return TOKEN_IDENTIFIER;
}

static Token scan_identifier(Scanner* scanner) {
    while (is_alphanum(peek())) advance();
    return token_make(identifier_type(scanner));
}

Token scanner_scan_token(Scanner* scanner) {
    skip_whitespace(scanner);

    scanner->start = scanner->current;

    if (is_at_end()) return token_make(TOKEN_EOF);

    char c = advance();
    if (is_digit(c)) return scan_number(scanner);
    if (is_alpha(c)) return scan_identifier(scanner);

    switch (c) {
        case '(': return token_make(TOKEN_LEFT_PAREN);
        case ')': return token_make(TOKEN_RIGHT_PAREN);
        case '{': return token_make(TOKEN_LEFT_CURLY);
        case '}': return token_make(TOKEN_RIGHT_CURLY);
        case ';': return token_make(TOKEN_SEMICOLON);
        case ',': return token_make(TOKEN_COMMA);
        case '.': return token_make(TOKEN_DOT);
        case '-': return token_make(TOKEN_MINUS);
        case '+': return token_make(TOKEN_PLUS);
        case '/': return token_make(TOKEN_SLASH);
        case '*': return token_make(TOKEN_STAR);
        case '!': return token_make(match('=') ? TOKEN_BANG_EQUAL : TOKEN_BANG);
        case '=': return token_make(match('=') ? TOKEN_EQUAL_EQUAL : TOKEN_EQUAL);
        case '<': return token_make(match('=') ? TOKEN_LESS_EQUAL : TOKEN_LESS);
        case '>': return token_make(match('=') ? TOKEN_GREATER_EQUAL : TOKEN_GREATER);
        case '"': return scan_string(scanner);
    }
    
    return token_error("Unexpected character.");
}

const char* token_type_cstring(TokenType type) {
    switch (type) {
        case TOKEN_LEFT_PAREN: return "LEFT_PAREN";
        case TOKEN_RIGHT_PAREN: return "RIGHT_PAREN";
        case TOKEN_LEFT_CURLY: return "LEFT_CURLY";
        case TOKEN_RIGHT_CURLY: return "RIGHT_CURLY";
        case TOKEN_COMMA: return "COMMA";
        case TOKEN_DOT: return "DOT";
        case TOKEN_MINUS: return "MINUS";
        case TOKEN_PLUS: return "PLUS";
        case TOKEN_SEMICOLON: return "SEMICOLON";
        case TOKEN_SLASH: return "SLASH";
        case TOKEN_STAR: return "STAR";
        case TOKEN_BANG: return "BANG";
        case TOKEN_BANG_EQUAL: return "BANG_EQUAL";
        case TOKEN_EQUAL: return "EQUAL";
        case TOKEN_EQUAL_EQUAL: return "EQUAL_EQUAL";
        case TOKEN_GREATER: return "GREATER";
        case TOKEN_GREATER_EQUAL: return "GREATER_EQUAL";
        case TOKEN_LESS: return "LESS";
        case TOKEN_LESS_EQUAL: return "LESS_EQUAL";
        case TOKEN_IDENTIFIER: return "IDENTIFIER";
        case TOKEN_STRING: return "STRING";
        case TOKEN_NUMBER: return "NUMBER";
        case TOKEN_AND: return "AND";
        case TOKEN_CLASS: return "CLASS";
        case TOKEN_ELSE: return "ELSE";
        case TOKEN_FALSE: return "FALSE";
        case TOKEN_FOR: return "FOR";
        case TOKEN_FUN: return "FUN";
        case TOKEN_IF: return "IF";
        case TOKEN_NIL: return "NIL";
        case TOKEN_OR: return "OR";
        case TOKEN_PRINT: return "PRINT";
        case TOKEN_RETURN: return "RETURN";
        case TOKEN_SUPER: return "SUPER";
        case TOKEN_THIS: return "THIS";
        case TOKEN_TRUE: return "TRUE";
        case TOKEN_VAR: return "VAR";
        case TOKEN_WHILE: return "WHILE";
        case TOKEN_ERROR: return "ERROR";
        case TOKEN_EOF: return "EOF";
        default: return NULL;
    }
}
