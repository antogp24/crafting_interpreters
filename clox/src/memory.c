#include "memory.h"
#include <stdio.h>

void* reallocate(void* array, size_t new_size) {
    if (new_size == 0) {
        free(array);
        return NULL;
    }
    void* new_array = realloc(array, new_size);
    if (new_array == NULL) {
        fprintf(stderr, "Couldn't reallocate an array of new_size:%zu\n", new_size);
        exit(1);
    }
    return new_array;
}
