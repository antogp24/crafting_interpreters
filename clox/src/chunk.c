#include "chunk.h"
#include "memory.h"
#include <stdio.h>

Chunk chunk_make() {
    return (Chunk){
        .count = 0,
        .capacity = DEFAULT_CAPACITY,
        .code = MAKE_DEFAULT_ARRAY(uint8_t),
        .lines = rle_lines_make(),
        .columns = rle_lines_make(),
        .constants = value_array_make(),
    };
}

int chunk_constant_add(Chunk* chunk, Value value) {
    value_array_append(&chunk->constants, value);
    return chunk->constants.count - 1;
}

void chunk_write(Chunk* chunk, uint8_t byte, Line_Number line, Line_Number column) {
    rle_lines_append(&chunk->lines, line);
    rle_lines_append(&chunk->columns, column);

    if (chunk->capacity < chunk->count + 1) {
        chunk->capacity = GROW_CAPACITY(chunk->capacity);
        chunk->code = GROW_ARRAY(uint8_t, chunk->code, chunk->capacity);
    }
    chunk->code[chunk->count] = byte;
    chunk->count++;
}

void chunk_write_constant(Chunk* chunk, Value value, Line_Number line, Line_Number column) {
    int index = chunk_constant_add(chunk, value);
    if (index < UINT8_MAX) {
        chunk_write(chunk, OP_CONSTANT, line, column);
        chunk_write(chunk, index, line, column);
    } else {
        chunk_write(chunk, OP_CONSTANT_LONG, line, column);
        chunk_write(chunk, ((index >>  0) & 0xff), line, column);
        chunk_write(chunk, ((index >>  8) & 0xff), line, column);
        chunk_write(chunk, ((index >> 16) & 0xff), line, column);
    }
}

#define OP_FMT "%-16s"
#define OP_PRINT(op_name) printf(OP_FMT, op_name)

uint32_t chunk_disassemble_instruction(Chunk* chunk, uint32_t offset) {
    uint32_t increment = offset + 1;
    
    printf("%04i: ", offset + 1);

    // Printing the line.
    if (offset > 0 && rle_lines_get(&chunk->lines, offset) == rle_lines_get(&chunk->lines, offset-1)) {
        printf("[   | ");
    } else {
        printf("[%4d:", rle_lines_get(&chunk->lines, offset));
    }

    // Printing the column.
    if (offset > 0 && rle_lines_get(&chunk->columns, offset) == rle_lines_get(&chunk->columns, offset-1)) {
        printf("   |] ");
    } else {
        printf("%4d] ", rle_lines_get(&chunk->columns, offset));
    }
    
    switch (chunk->code[offset]) {
        case OP_ADD:            OP_PRINT("OP_ADD");           break;
        case OP_SUBSTRACT:      OP_PRINT("OP_SUBSTRACT");     break;
        case OP_MULTIPLY:       OP_PRINT("OP_MULTIPLY");      break;
        case OP_DIVIDE:         OP_PRINT("OP_DIVIDE");        break;
        case OP_NOT:            OP_PRINT("OP_NOT");           break;
        case OP_EQUAL:          OP_PRINT("OP_EQUAL");         break;
        case OP_NOT_EQUAL:      OP_PRINT("OP_NOT_EQUAL");     break;
        case OP_LESS:           OP_PRINT("OP_LESS");          break;
        case OP_LESS_EQUAL:     OP_PRINT("OP_LESS_EQUAL");    break;
        case OP_GREATER:        OP_PRINT("OP_GREATER");       break;
        case OP_GREATER_EQUAL:  OP_PRINT("OP_GREATER_EQUAL"); break;
        case OP_NEGATE:         OP_PRINT("OP_NEGATE");        break;
        case OP_RETURN:         OP_PRINT("OP_RETURN");        break;
        case OP_NIL:            OP_PRINT("OP_NIL");           break;
        case OP_TRUE:           OP_PRINT("OP_TRUE");          break;
        case OP_FALSE:          OP_PRINT("OP_FALSE");         break;
        
        case OP_CONSTANT: {
            const uint8_t index = chunk->code[offset+1];
            printf(OP_FMT" %4d '", "OP_CONSTANT", index);
            value_print(chunk->constants.values[index]);
            printf("'");
            increment++;
        } break;
        
        case OP_CONSTANT_LONG: {
            const uint32_t index = chunk->code[offset+1] | (chunk->code[offset+2] << 8) | (chunk->code[offset+3] << 16);
            printf(OP_FMT" %4d '", "OP_CONSTANT_LONG", index);
            value_print(chunk->constants.values[index]);
            printf("'");
            increment += 3;
        } break;
    }
    printf("\n");
    
    return increment;
}

#undef OP_FMT
#undef OP_PRINT

void chunk_disassemble(Chunk* chunk, const char *name) {
    printf("Chunk %s:\n", name);
    printf("===------code-------===\n");
    for (uint32_t i = 0; i < chunk->count;) {
        i = chunk_disassemble_instruction(chunk, i);
    }
    printf("===-----------------===\n");
    printf("count:%u, capacity:%u\n", chunk->count, chunk->capacity);
    printf("===-----------------===\n");
}

void chunk_delete(Chunk* chunk) {
    value_array_delete(&chunk->constants);
    rle_lines_delete(&chunk->lines);
    rle_lines_delete(&chunk->columns);
    free(chunk->code);
    chunk->code = NULL;
    chunk->capacity = 0;
    chunk->count = 0;
}
