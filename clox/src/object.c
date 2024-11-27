#include "object.h"
#include "vm.h"
#include "hash_table.h"

#include <stdlib.h>
#include <assert.h>

// This function is necessary instead of a macro
// because otherwise value would evaluate twice
// and having a value with a side effect
//
//      Example:
//          is_object_type(vm_stack_pop(), OBJECT_STRING)
//
//      The last code would pop two values off the stack
//      if it was a macro, because value is evaluated twice.
//
bool is_value_of_object_type(Value value, Object_Type type) {
    return IS_OBJECT(value) && OBJECT_TYPE(value) == type;
}

Object* object_allocate(size_t sizeof_T, Object_Type type) {
    Object* object = (Object*)malloc(sizeof_T);
    object->type = type;
    
    object->next = vm.allocated_objects;
    vm.allocated_objects = object;
    return object;
}

Object_String* object_string_allocate(char* items, uint32_t length, uint32_t hash, bool is_constant) {
    Object_String* object_string = OBJECT_ALLOCATE(Object_String, OBJECT_STRING);
    object_string->items = items;
    object_string->length = length;
    object_string->hash = hash;
    object_string->is_constant = is_constant;
    
    hash_table_set(&vm.intern_strings, object_string, VALUE_NIL);
    
    return object_string;
}

Object_String *object_string_copy(const char* items, uint32_t length, bool is_constant) {
    char* buffer = (char*)malloc(length + 1);
    memcpy(buffer, items, length);
    buffer[length] = '\0';
    
    uint32_t hash = hash_string(buffer, length);
    if (!is_constant) {
        Object_String* interned = hash_table_find_string(&vm.intern_strings, buffer, length, hash);
        if (interned != NULL) {
            free(buffer);
            return interned;
        }
    }
    return object_string_allocate(buffer, length, hash, is_constant);
}

// FNV-1a
uint32_t hash_string(char* key, uint32_t length) {
    uint32_t hash = 2166136261u;
    
    for (uint32_t i = 0; i < length; i++) {
        hash ^= key[i];
        hash *= 16777619;
    }
    
    return hash;
}

void object_free(Object* object) {
    switch (object->type) {
        case OBJECT_STRING: {
            Object_String* str = (Object_String*)object;
            if (!str->is_constant) free(str->items);
            str->items = NULL;
            str->length = 0;
            free(str);
        } break;
        default: assert(false && "Unimplemented");
    }
}
