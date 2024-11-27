#ifndef _value_h
#define _value_h

#include "common.h"

typedef struct Object Object;
typedef struct Object_String Object_String;

typedef enum {
    TYPE_NIL = 0,
    TYPE_BOOL,
    TYPE_NUMBER,
    TYPE_OBJECT,
    TYPE_COUNT
} ValueType;

typedef struct {
    ValueType type;
    union {
        bool boolean;
        double number;
        Object* object;
    } as;
} Value;

#define VALUE_BOOL(x)    ((Value){.type=TYPE_BOOL,   .as={.boolean = x}})
#define VALUE_NIL        ((Value){.type=TYPE_NIL,    .as={.number = 0}})
#define VALUE_NUMBER(x)  ((Value){.type=TYPE_NUMBER, .as={.number = x}})
#define VALUE_OBJECT(x)  ((Value){.type=TYPE_OBJECT, .as={.object = (Object*)x}})

#define AS_BOOL(value)   ((value).as.boolean)
#define AS_NUMBER(value) ((value).as.number)
#define AS_OBJECT(value) ((value).as.object)

#define IS_BOOL(value)   ((value).type == TYPE_BOOL)
#define IS_NIL(value)    ((value).type == TYPE_NIL)
#define IS_NUMBER(value) ((value).type == TYPE_NUMBER)
#define IS_OBJECT(value) ((value).type == TYPE_OBJECT)

bool values_equal(Value a, Value b);
void value_object_print(Value value);
void value_print(Value value);
void value_println(Value value);

typedef struct {
    int capacity;
    int count;
    Value *values;
} Value_Array;

Value_Array value_array_make();
void value_array_append(Value_Array* array, Value value);
void value_array_delete(Value_Array* array);

#endif // _value_h
