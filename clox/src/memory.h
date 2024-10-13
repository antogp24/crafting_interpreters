#ifndef _memory_h
#define _memory_h

#include "common.h"
#include <stdlib.h>

#define DEFAULT_CAPACITY 8

#define MAKE_ARRAY(type, size) (type*)malloc((size) * sizeof(type))
#define MAKE_DEFAULT_ARRAY(type) MAKE_ARRAY(type, DEFAULT_CAPACITY)

#define GROW_CAPACITY(c) (c < DEFAULT_CAPACITY ? DEFAULT_CAPACITY : c * 2)

#define GROW_ARRAY(type, array, newCount) \
    (type*)reallocate(array, sizeof(type) * (newCount))

void* reallocate(void* array, size_t new_size);

#endif //  _memory_h