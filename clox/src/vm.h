#ifndef _vm_h
#define _vm_h

#include "common.h"
#include "chunk.h"

typedef enum {
    INTERPRET_OK = 0,
    INTERPRET_COMPILE_ERROR,
    INTERPRET_RUNTIME_ERROR,
    INTERPRET_RESULT_COUNT,
} Interpret_Result;

#define STACK_MAX 0xFFFFFF

typedef struct {
    Chunk* chunk;
    uint8_t* ip;
    Value stack[STACK_MAX];
    Value* stack_top;
} VM;

void vm_init();
Interpret_Result vm_interpret(const char* source_code);
Value vm_stack_peek(uint32_t distance);
void vm_stack_push(Value value);
Value vm_stack_pop();
void vm_destroy();

#endif // _vm_h
