#include "value.h"
#include "memory.h"
#include <assert.h>

#define impl_print_value(value)                                         \
    switch (value.type) {                                               \
    case TYPE_NIL:    printf("nil"); break;                             \
    case TYPE_BOOL:   printf(AS_BOOL(value) ? "true" : "false"); break; \
    case TYPE_NUMBER: printf("%g", AS_NUMBER(value)); break;            \
    default: assert(false && "Unimplemented");                          \
    }

bool values_equal(Value a, Value b) {
    if (a.type != b.type) return false;
    switch (a.type) {
        case TYPE_NIL: return true;
        case TYPE_BOOL: return AS_BOOL(a) == AS_BOOL(b);
        case TYPE_NUMBER: return AS_NUMBER(a) == AS_NUMBER(b);
        default: assert(false && "Unimplemented");
    }
    return false; // Unreachable;
}

void value_print(Value value) {
    impl_print_value(value);
}

void value_println(Value value) {
    impl_print_value(value);
    printf("\n");
}

#undef impl_print_value

Value_Array value_array_make() {
    return (Value_Array){
        .count = 0,
        .capacity = DEFAULT_CAPACITY,
        .values = (Value*)malloc(DEFAULT_CAPACITY * sizeof(Value)),
    };
}

void value_array_append(Value_Array* array, Value value) {
    if (array->capacity < array->count + 1) {
        array->capacity = GROW_CAPACITY(array->capacity);
        array->values = GROW_ARRAY(Value, array->values, array->capacity);
    }
    array->values[array->count] = value;
    array->count++;
}

void value_array_delete(Value_Array* array) {
    free(array->values);
    array->values = NULL;
    array->count = 0;
    array->capacity = 0;
}
