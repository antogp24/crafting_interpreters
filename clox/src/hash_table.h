#ifndef __HASH_TABLE_H
#define __HASH_TABLE_H

#include "common.h"
#include "value.h"
#include "object.h"

#define HASH_TABLE_MAX_LOAD 0.75

typedef struct {
    Object_String* key;
    Value value;
} Hash_Table_Entry;

typedef struct {
    uint32_t count;
    uint32_t capacity;
    Hash_Table_Entry* entries;
} Hash_Table;

void hash_table_init(Hash_Table* table);
Hash_Table hash_table_make();
Object_String* hash_table_find_string(Hash_Table* table, const char* items, uint32_t length, uint32_t hash);
bool hash_table_set(Hash_Table* table, Object_String* key, Value value);
bool hash_table_get(Hash_Table* table, Object_String* key, Value* value);
bool hash_table_delete(Hash_Table* table, Object_String* key);
void hash_table_add_all(Hash_Table* from, Hash_Table* to);
void hash_table_destroy(Hash_Table* table);
void hash_table_println(Hash_Table* table);

#endif //  __HASH_TABLE_H