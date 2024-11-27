#ifndef __OBJECT_H
#define __OBJECT_H

#include "common.h"
#include "value.h"

typedef enum {
    OBJECT_NIL = 0,
    OBJECT_STRING,
    OBJECT_COUNT,
} Object_Type;

struct Object {
    Object_Type type;
    Object* next;
};

void object_free(Object* object);
bool is_value_of_object_type(Value value, Object_Type type);
Object* object_allocate(size_t sizeof_T, Object_Type type);
#define OBJECT_TYPE(value) (AS_OBJECT(value)->type)
#define OBJECT_ALLOCATE(T, object_type) ((T*)object_allocate(sizeof(T), object_type))

// String
// ------------------------------------------------------------------------------- //
struct Object_String {
    Object object;
    char* items;
    uint32_t length;
    uint32_t hash;
    bool is_constant;
};

uint32_t hash_string(char* key, uint32_t length);
Object_String *object_string_copy(const char* items, uint32_t length, bool is_constant);
Object_String* object_string_allocate(char* items, uint32_t length, uint32_t hash, bool is_constant);
#define AS_STRING(value)   ((Object_String*)AS_OBJECT(value))
#define AS_CSTRING(value) (((Object_String*)AS_OBJECT(value))->items)
#define IS_STRING(value)  is_value_of_object_type(value, OBJECT_STRING)

// ------------------------------------------------------------------------------- //

#endif // __OBJECT_H
