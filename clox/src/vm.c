#include "vm.h"
#include "compiler.h"
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <assert.h>

static VM vm;

#define IP_INDEX ((uint32_t)(vm.ip - vm.chunk->code))
#define RESET_STACK() do { vm.stack_top = vm.stack; } while(0)

static void runtime_error(const char *format, ...) {
    const uint32_t instruction = IP_INDEX - 1;
    const Line_Number line = rle_lines_get(&vm.chunk->lines, instruction);
    const Line_Number column = rle_lines_get(&vm.chunk->columns, instruction);
    fprintf(stderr, "Runtime Error at [%"PRI_LN":%"PRI_LN"]: ", line, column);

    va_list args;
    va_start(args, format);
    vfprintf(stderr, format, args);
    va_end(args);
    fprintf(stderr, "\n");

    RESET_STACK();
}

void vm_init() {
    RESET_STACK();
}

static Interpret_Result run() {
    #define READ_BYTE() (*vm.ip++)
    #define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])
    #define READ_CONSTANT_LONG(b0, b1, b2) (vm.chunk->constants.values[b0 | (b1 << 8) | (b2 << 16)])
    
    #define BINARY_OP(op, VALUE_T) do {                    \
        if (!IS_NUMBER(vm_stack_peek(0)) ||                \
            !IS_NUMBER(vm_stack_peek(1)))                  \
        {                                                  \
            runtime_error("Operands must be numbers");     \
            return INTERPRET_RUNTIME_ERROR;                \
        }                                                  \
        double b = AS_NUMBER(vm_stack_pop());              \
        double a = AS_NUMBER(vm_stack_pop());              \
        vm_stack_push(VALUE_T(a op b));                    \
    } while(0)

    if (vm.chunk->count == 0) return INTERPRET_OK;
        
    for (uint8_t instruction;;) {
        #ifdef DEBUG_TRACE_EXECUTION
            chunk_disassemble_instruction(vm.chunk, IP_INDEX);
        #endif
        
        switch (instruction = READ_BYTE())
        {
            case OP_ADD:       BINARY_OP(+, VALUE_NUMBER); break;
            case OP_SUBSTRACT: BINARY_OP(-, VALUE_NUMBER); break;
            case OP_MULTIPLY:  BINARY_OP(*, VALUE_NUMBER); break;
            case OP_DIVIDE:    BINARY_OP(/, VALUE_NUMBER); break;

            case OP_NIL:   vm_stack_push(VALUE_NIL);         break;
            case OP_TRUE:  vm_stack_push(VALUE_BOOL(true));  break;
            case OP_FALSE: vm_stack_push(VALUE_BOOL(false)); break;
            
            case OP_NOT: {
                if (!IS_BOOL(vm_stack_peek(0))) {
                    runtime_error("Only booleans can be negated.");
                    return INTERPRET_RUNTIME_ERROR;
                }
                bool not_result = !AS_BOOL(vm_stack_pop());
                vm_stack_push(VALUE_BOOL(not_result));
            } break;
            
            case OP_EQUAL: {
                Value b = vm_stack_pop();
                Value a = vm_stack_pop();
                bool result = values_equal(a, b);
                vm_stack_push(VALUE_BOOL(result));
            } break;
            
            case OP_NOT_EQUAL: {
                Value b = vm_stack_pop();
                Value a = vm_stack_pop();
                bool result = !values_equal(a, b);
                vm_stack_push(VALUE_BOOL(result));
            } break;

            case OP_LESS:          BINARY_OP(<,  VALUE_BOOL); break;
            case OP_LESS_EQUAL:    BINARY_OP(<=, VALUE_BOOL); break;
            case OP_GREATER:       BINARY_OP(>,  VALUE_BOOL); break;
            case OP_GREATER_EQUAL: BINARY_OP(>=, VALUE_BOOL); break;

            case OP_NEGATE: {
                if (!IS_NUMBER(vm_stack_peek(0))) {
                    runtime_error("Only numbers can be negated.");
                    return INTERPRET_RUNTIME_ERROR;
                }
                double negated = -AS_NUMBER(vm_stack_pop());
                vm_stack_push(VALUE_NUMBER(negated));
            } break;

            case OP_CONSTANT: {
                Value constant = READ_CONSTANT();
                vm_stack_push(constant);
            } break;

            case OP_CONSTANT_LONG: {
                const uint8_t b0 = READ_BYTE();
                const uint8_t b1 = READ_BYTE();
                const uint8_t b2 = READ_BYTE(); 
                Value constant = READ_CONSTANT_LONG(b0, b1, b2);
                vm_stack_push(constant);
            } break;

            case OP_RETURN: {
                Value returned = vm_stack_pop();
                value_println(returned);
            } return INTERPRET_OK;
        }
    }
    
    return INTERPRET_OK;
    
    #undef READ_BYTE
    #undef READ_CONSTANT
    #undef READ_CONSTANT_LONG
    #undef BINARY_OP
}

Interpret_Result vm_interpret(const char* source_code) {
    Chunk chunk = chunk_make();

    if (!compile(source_code, &chunk)) {
        chunk_delete(&chunk);
        return INTERPRET_COMPILE_ERROR;
    }

    vm.chunk = &chunk;
    vm.ip = chunk.code;

    Interpret_Result result = run();

    chunk_delete(&chunk);
    return result;
}

Value vm_stack_peek(uint32_t distance) {
    uint32_t len = vm.stack_top - vm.stack;
    assert((len - 1 - distance) >= 0);
    return vm.stack[len - 1 - distance];
    // return vm.stack_top[-1 - distance];
}

void vm_stack_push(Value value) {
    if (vm.stack_top > vm.stack + STACK_MAX) {
        fprintf(stderr, "Stack Overflow Error: Exceded the %d limit.\n", STACK_MAX);
        exit(1);
    }
    vm.stack_top->type = value.type;
    vm.stack_top->as = value.as;
    vm.stack_top++;
}

Value vm_stack_pop() {
    vm.stack_top--;
    return *vm.stack_top;
}

void vm_destroy() {
}
