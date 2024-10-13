#ifndef _compiler_h
#define _compiler_h

#include "common.h"
#include "chunk.h"
#include "scanner.h"

#define PARSER_HAD_ERROR  0b01
#define PARSER_PANIC_MODE 0b10

typedef struct {
	Token current;
	Token previous;
	Scanner* scanner;
	uint32_t flags;
} Parser;

typedef enum {
    PREC_NONE = 0,
    PREC_ASSIGNMENT, // =
    PREC_OR,         // or
    PREC_AND,        // and
    PREC_EQUALITY,   // == !=
    PREC_COMPARISON, // < > <= >=
    PREC_TERM,       // + -
    PREC_FACTOR,     // * /
    PREC_UNARY,      // + - !
    PREC_CALL,       // . ()
    PREC_PRIMARY,
    PREC_COUNT,
} Precedence;

typedef void (*ParseFn)(Parser*);

typedef struct {
    ParseFn prefix;
    ParseFn infix;
    Precedence precedence;
} ParseRule;

bool compile(const char *source_code, Chunk* chunk);

#endif //  _compiler_h
