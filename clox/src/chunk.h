#ifndef _chunk_h
#define _chunk_h

#include "common.h"
#include "value.h"
#include "rle_lines.h"

typedef enum {
    OP_NIL = 0,
    OP_TRUE,
    OP_FALSE,
    OP_CONSTANT,
    OP_CONSTANT_LONG,
    OP_NOT,
    OP_EQUAL,
    OP_NOT_EQUAL,
    OP_GREATER,
    OP_GREATER_EQUAL,
    OP_LESS,
    OP_LESS_EQUAL,
    OP_NEGATE,
    OP_ADD,
    OP_SUBSTRACT,
    OP_MULTIPLY,
    OP_DIVIDE,
    OP_RETURN,
    OP_COUNT,
} OpCode;

typedef struct {
    uint8_t* code;
    uint32_t count;
    uint32_t capacity;
    Value_Array constants;
    RLE_Lines lines;
    RLE_Lines columns;
} Chunk;

Chunk chunk_make();
void chunk_write(Chunk* chunk, uint8_t byte, Line_Number line, Line_Number column);
void chunk_write_constant(Chunk* chunk, Value value, Line_Number line, Line_Number column);
int chunk_constant_add(Chunk* chunk, Value value);
uint32_t chunk_disassemble_instruction(Chunk* chunk, uint32_t offset);
void chunk_disassemble(Chunk* chunk, const char* name);
void chunk_delete(Chunk* chunk);

#endif // _chunk_h
