#include "compiler.h"
#include "scanner.h"
#include "chunk.h"
#include <stdio.h>
#include <math.h>
#include <stdarg.h>
#include <assert.h>

static void parser_parse_precedence(Parser* parser, Precedence precedence);
static void parser_expression(Parser* parser);

static void grouping(Parser*);
static void unary(Parser*);
static void binary(Parser*);
static void number(Parser*);
static void literal(Parser*);

static void parser_error_at(Parser* parser, Token* token, const char *message);

#define FLAGS_HAVE(flags, f) ((flags & f) == f)
#define is_panicing() FLAGS_HAVE(parser->flags, PARSER_PANIC_MODE)
#define had_error() FLAGS_HAVE(parser->flags, PARSER_HAD_ERROR)

#define parse_precedence(precedence) parser_parse_precedence(parser, precedence)
#define expression() parser_expression(parser)

#define error_at(token, message) parser_error_at(parser, token, message)
#define error_at_current(message) error_at(&parser->current, message)
#define error(message) error_at(&parser->previous, message)

const ParseRule rules[] = {
  [TOKEN_LEFT_PAREN]    = {grouping, NULL,   PREC_NONE},
  [TOKEN_RIGHT_PAREN]   = {NULL,     NULL,   PREC_NONE},
  [TOKEN_LEFT_CURLY]    = {NULL,     NULL,   PREC_NONE}, 
  [TOKEN_RIGHT_CURLY]   = {NULL,     NULL,   PREC_NONE},
  [TOKEN_COMMA]         = {NULL,     NULL,   PREC_NONE},
  [TOKEN_DOT]           = {NULL,     NULL,   PREC_NONE},
  [TOKEN_MINUS]         = {unary,    binary, PREC_TERM},
  [TOKEN_PLUS]          = {unary,    binary, PREC_TERM},
  [TOKEN_SEMICOLON]     = {NULL,     NULL,   PREC_NONE},
  [TOKEN_SLASH]         = {NULL,     binary, PREC_FACTOR},
  [TOKEN_STAR]          = {NULL,     binary, PREC_FACTOR},
  [TOKEN_BANG]          = {unary,    NULL,   PREC_NONE},
  [TOKEN_BANG_EQUAL]    = {NULL,     binary, PREC_EQUALITY},
  [TOKEN_EQUAL]         = {NULL,     NULL,   PREC_NONE},
  [TOKEN_EQUAL_EQUAL]   = {NULL,     binary, PREC_COMPARISON},
  [TOKEN_GREATER]       = {NULL,     binary, PREC_COMPARISON},
  [TOKEN_GREATER_EQUAL] = {NULL,     binary, PREC_COMPARISON},
  [TOKEN_LESS]          = {NULL,     binary, PREC_COMPARISON},
  [TOKEN_LESS_EQUAL]    = {NULL,     binary, PREC_COMPARISON},
  [TOKEN_IDENTIFIER]    = {NULL,     NULL,   PREC_NONE},
  [TOKEN_STRING]        = {NULL,     NULL,   PREC_NONE},
  [TOKEN_NUMBER]        = {number,   NULL,   PREC_NONE},
  [TOKEN_AND]           = {NULL,     NULL,   PREC_NONE},
  [TOKEN_CLASS]         = {NULL,     NULL,   PREC_NONE},
  [TOKEN_ELSE]          = {NULL,     NULL,   PREC_NONE},
  [TOKEN_FALSE]         = {literal,  NULL,   PREC_NONE},
  [TOKEN_FOR]           = {NULL,     NULL,   PREC_NONE},
  [TOKEN_FUN]           = {NULL,     NULL,   PREC_NONE},
  [TOKEN_IF]            = {NULL,     NULL,   PREC_NONE},
  [TOKEN_NIL]           = {literal,  NULL,   PREC_NONE},
  [TOKEN_OR]            = {NULL,     NULL,   PREC_NONE},
  [TOKEN_PRINT]         = {NULL,     NULL,   PREC_NONE},
  [TOKEN_RETURN]        = {NULL,     NULL,   PREC_NONE},
  [TOKEN_SUPER]         = {NULL,     NULL,   PREC_NONE},
  [TOKEN_THIS]          = {NULL,     NULL,   PREC_NONE},
  [TOKEN_TRUE]          = {literal,  NULL,   PREC_NONE},
  [TOKEN_VAR]           = {NULL,     NULL,   PREC_NONE},
  [TOKEN_WHILE]         = {NULL,     NULL,   PREC_NONE},
  [TOKEN_ERROR]         = {NULL,     NULL,   PREC_NONE},
  [TOKEN_EOF]           = {NULL,     NULL,   PREC_NONE},
};

Parser parser_make(Scanner* scanner) {
    return (Parser) {
        .scanner = scanner,
        .flags = 0,
    };
}

static void parser_error_at(Parser* parser, Token* token, const char *message) {
    if (is_panicing()) return;
    parser->flags |= PARSER_PANIC_MODE;

    fprintf(stderr, "Error [%d:%d]", token->line, token->column);

    if (token->type == TOKEN_EOF) {
	    fprintf(stderr, " at end");
    } else if (token->type != TOKEN_ERROR) {
        fprintf(stderr, " at '%.*s'", (int)token->length, token->start);
    }

    fprintf(stderr, ": %s\n", message);
    parser->flags |= PARSER_HAD_ERROR;
}

#define advance() parser_advance(parser)
static void parser_advance(Parser* parser) {
    parser->previous = parser->current;
    parser->current = scanner_scan_token(parser->scanner);

#ifdef DEBUG_COMPILER_PARSING
    printf("[%i:%i] ", parser->current.line, parser->current.column);
    printf("%10s '%.*s'\n", token_type_cstring(parser->current.type), (int)parser->current.length, parser->current.start);
#endif

    while (parser->current.type == TOKEN_ERROR) {
        error_at_current(parser->current.start); // Use the lexeme of the current token as the message.
        parser->current = scanner_scan_token(parser->scanner);
    }
}

#define consume(type, message, ...) parser_consume(parser, type, message, ##__VA_ARGS__)
static void parser_consume(Parser* parser, TokenType type, const char* format, ...) {
    if (parser->current.type == type) {
        advance();
        return;
    }

    char message[1024];
    va_list args;
    va_start(args, format);
    vsnprintf(message, 1024, format, args);
    error_at_current(message);
    va_end(args);
}

static Chunk* compiling_chunk = NULL;

static Chunk* get_current_chunk() {
    return compiling_chunk;
}

#define default_emit_byte(byte) emit_byte(byte, parser->previous.line, parser->previous.column)
static void emit_byte(uint8_t byte, Line_Number line, Line_Number column) {
    Chunk* current_chunk = get_current_chunk();
    chunk_write(current_chunk, byte, line, column);
}

#define emit_constant(value) parser_emit_constant(parser, value)
static void parser_emit_constant(Parser* parser, Value value) {
    Chunk* current_chunk = get_current_chunk();
    chunk_write_constant(current_chunk, value, parser->previous.line, parser->previous.column);
}

#define default_emit_bytes(line, column, n, ...) emit_bytes(parser->previous.line, parser->previous.column, n, ##__VA_ARGS__)
static void emit_bytes(Line_Number line, Line_Number column, size_t n, ...) {
    va_list args;
    va_start(args, n);
    for (size_t i = 0; i < n; i++) {
        uint8_t byte = va_arg(args, uint8_t);
        emit_byte(byte, line, column);
    }
    va_end(args);
}


static void parser_parse_precedence(Parser* parser, Precedence precedence) {
    advance();
    const ParseFn prefix_rule = rules[parser->previous.type].prefix;
    if (prefix_rule == NULL) {
        error("Expected expression");
        return;
    }
    prefix_rule(parser);

    while (precedence <= rules[parser->current.type].precedence) {
        advance();
        const ParseFn infix_rule = rules[parser->previous.type].infix;
        assert(infix_rule != NULL);
        infix_rule(parser);
    }
}

static void literal(Parser* parser) {
    // The literal has already been consumed.
    switch (parser->previous.type) {
        case TOKEN_NIL:   default_emit_byte(OP_NIL);   break;
        case TOKEN_TRUE:  default_emit_byte(OP_TRUE);  break;
        case TOKEN_FALSE: default_emit_byte(OP_FALSE); break;
        default: assert(false && "Unreachable");
    }
}

static void number(Parser* parser) {
    // The number has already been consumed.
    double value = atof(parser->previous.start);
    emit_constant(VALUE_NUMBER(value));
}

static void grouping(Parser* parser) {
    // It is assumed '(' has already been consumed.
    expression();
    consume(TOKEN_RIGHT_PAREN, "Expect ')' after expression.");
}

static void unary(Parser* parser) {
    // The unary operator has already been consumed.
    const Token operator = parser->previous;
    parse_precedence(PREC_UNARY);
    #define EMIT_BYTE(byte) emit_byte(byte, operator.line, operator.column)
    switch (operator.type) {
        case TOKEN_MINUS: EMIT_BYTE(OP_NEGATE); break;
        case TOKEN_BANG:  EMIT_BYTE(OP_NOT);    break;
        case TOKEN_PLUS:  /* Do nothing */      break;
        default: assert(false && "Unreachable");
    }
    #undef EMIT_BYTE
}

static void binary(Parser* parser) {
    // The left hand side and the operator have already been consumed.
    // The left hand side is already on the stack.
    const Token operator = parser->previous;
    const ParseRule rule = rules[operator.type];
    
    // Puts right hand side on the stack.
    parse_precedence((Precedence)(rule.precedence+1));
    
    #define EMIT_BYTE(byte) emit_byte(byte, operator.line, operator.column)
    switch (operator.type) {
        // Arithmetic Operators.
        case TOKEN_PLUS:  EMIT_BYTE(OP_ADD);       break;
        case TOKEN_MINUS: EMIT_BYTE(OP_SUBSTRACT); break;
        case TOKEN_STAR:  EMIT_BYTE(OP_MULTIPLY);  break;
        case TOKEN_SLASH: EMIT_BYTE(OP_DIVIDE);    break;
        
        // Logical Operators.
        case TOKEN_EQUAL_EQUAL:   EMIT_BYTE(OP_EQUAL);         break;
        case TOKEN_BANG_EQUAL:    EMIT_BYTE(OP_NOT_EQUAL);     break;
        case TOKEN_LESS:          EMIT_BYTE(OP_LESS);          break;
        case TOKEN_LESS_EQUAL:    EMIT_BYTE(OP_LESS_EQUAL);    break;
        case TOKEN_GREATER:       EMIT_BYTE(OP_GREATER);       break;
        case TOKEN_GREATER_EQUAL: EMIT_BYTE(OP_GREATER_EQUAL); break;
        
        default: assert(false && "Unreachable");
    }
    #undef EMIT_BYTE
}

static void parser_expression(Parser* parser) {
    parse_precedence(PREC_NONE+1);
}

static void compiler_end(Parser* parser) {
    default_emit_byte(OP_RETURN);

    #ifdef DEBUG_PRINT_PARSED_CODE
        if (!had_error()) {
            Chunk *current_chunk = get_current_chunk();
            chunk_disassemble(current_chunk, "code");
        }
    #endif
}

bool compile(const char* source_code, Chunk* chunk) {
    Scanner scanner = scanner_make(source_code);
    Parser parser = parser_make(&scanner);
    compiling_chunk = chunk;
    
    parser_advance(&parser);
    parser_expression(&parser);
    parser_consume(&parser, TOKEN_EOF, "Expected end of expression.");
    
    compiler_end(&parser);
    return !FLAGS_HAVE(parser.flags, PARSER_HAD_ERROR);
}
