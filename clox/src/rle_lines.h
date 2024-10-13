#ifndef _rle_lines_h
#define _rle_lines_h

#include "common.h"
#include "inttypes.h"

#define PRI_LN PRIu32
typedef uint32_t Line_Number;

typedef struct {
    Line_Number *array;
    size_t count;
    size_t capacity;
} RLE_Lines;

RLE_Lines rle_lines_make();
void rle_lines_append(RLE_Lines* lines, Line_Number line);
Line_Number rle_lines_get(RLE_Lines* lines, size_t index);
void rle_lines_print(RLE_Lines* lines, bool newline);
void rle_lines_delete(RLE_Lines* lines);

#endif //  _rle_lines_h
