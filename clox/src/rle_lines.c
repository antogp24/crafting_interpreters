#include "rle_lines.h"
#include "memory.h"
#include <assert.h>

RLE_Lines rle_lines_make() {
    return (RLE_Lines){
        .count = 0,
        .capacity = DEFAULT_CAPACITY,
        .array = MAKE_DEFAULT_ARRAY(Line_Number),
    };
}

void rle_lines_append(RLE_Lines* lines, Line_Number line) {
    assert(lines->count % 2 == 0);
    
    if (lines->capacity < lines->count + 2) {
        do {
            lines->capacity = GROW_CAPACITY(lines->capacity);
        } while (lines->capacity < lines->count + 2);
        lines->array = GROW_ARRAY(Line_Number, lines->array, lines->capacity);
    }
    
    if (lines->count == 0) {
        lines->array[0] = 1;
        lines->array[1] = line;
        lines->count += 2;
    } else if (lines->array[lines->count-1] == line) {
        lines->array[lines->count-2]++;
    } else {
        lines->array[lines->count] = 1;
        lines->array[lines->count+1] = line;
        lines->count += 2;
    }
}

void rle_lines_delete(RLE_Lines* lines) {
    free(lines->array);
    lines->array = NULL;
    lines->count = 0;
    lines->capacity = 0;
}

void rle_lines_print(RLE_Lines* lines, bool newline) {
    for (size_t i = 0; i < lines->count; i += 2) {
        printf("%i<%i>", lines->array[i], lines->array[i+1]);
    }
    if (newline) printf("\n");
}

Line_Number rle_lines_get(RLE_Lines* lines, size_t index) {
    size_t count = 0;
    for (int i = 0; i < lines->count; i += 2) {
        if (count + lines->array[i] > index) {
            return lines->array[i+1];
        }
        count += lines->array[i];
    }
    return -1;
}
