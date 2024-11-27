#include "hash_table.h"
#include "memory.h"

#include <stdio.h>
#include <stdlib.h>

void hash_table_init(Hash_Table* table) {
    table->entries = NULL;
    table->count = 0;
    table->capacity = 0;
}

Hash_Table hash_table_make() {
    Hash_Table table;
    hash_table_init(&table);
    return table;
}

static Hash_Table_Entry* hash_table_find_entry(Hash_Table_Entry* entries, uint32_t capacity, Object_String* key) {
    Hash_Table_Entry* tombstone = NULL;
    uint32_t index = key->hash % capacity;
    
    while (true) {
        Hash_Table_Entry* entry = entries + index;
        if (entry->key == NULL) {
            if (IS_NIL(entry->value))
                return tombstone != NULL ? tombstone : entry;
            else if (tombstone == NULL)
                tombstone = entry;
        } else if (entry->key == key) {
            return entry;
        }
        index = (index + 1) % capacity;
    }
    return NULL; /* unreachable */
}

static void hash_table_grow(Hash_Table* table) {
    uint32_t old_capacity = table->capacity;
    table->capacity = GROW_CAPACITY(table->capacity);
    Hash_Table_Entry* entries = (Hash_Table_Entry*)calloc(table->capacity, sizeof(Hash_Table_Entry));
    
    if (old_capacity == 0) {
        table->entries = entries;
        return;
    }
    
    table->count = 0;
    for (uint32_t i = 0; i < table->capacity; i++) {
        Hash_Table_Entry* entry = table->entries + i;
        if (entry->key == NULL) continue;
        
        Hash_Table_Entry* dest = hash_table_find_entry(entries, old_capacity, entry->key);
        dest->key = entry->key;
        dest->value = entry->value;
        table->count++;
    }
    
    free(table->entries);
    table->entries = entries;
}

bool hash_table_set(Hash_Table* table, Object_String* key, Value value) {
    if (table->count + 1 > table->capacity * HASH_TABLE_MAX_LOAD) {
        hash_table_grow(table);
    }
    Hash_Table_Entry* entry = hash_table_find_entry(table->entries, table->capacity, key);
    
    bool is_new_key = entry->key == NULL;
    if (is_new_key && IS_NIL(entry->value)) table->count++;
    
    entry->key = key;
    entry->value = value;
    
    return is_new_key;
}

bool hash_table_get(Hash_Table* table, Object_String* key, Value* value) {
    if (table->count == 0) return false;
    
    Hash_Table_Entry* entry = hash_table_find_entry(table->entries, table->capacity, key);
    if (entry->key == NULL) return false;
    
    *value = entry->value;
    return true;
}

bool hash_table_delete(Hash_Table* table, Object_String* key) {
    if (table->count == 0) return false;
    
    Hash_Table_Entry* entry = hash_table_find_entry(table->entries, table->capacity, key);
    if (entry->key == NULL) return false;

    // Place a tombstone
    entry->key = NULL;
    entry->value = VALUE_BOOL(true);
    
    return true;
}

void hash_table_add_all(Hash_Table* from, Hash_Table* to) {
    for (uint32_t i = 0; i < from->capacity; i++) {
        Hash_Table_Entry* entry = from->entries + i;
        if (entry == NULL) continue;
        hash_table_set(to, entry->key, entry->value);
    }
}

Object_String* hash_table_find_string(Hash_Table* table, const char* items, uint32_t length, uint32_t hash) {
    if (table->count == 0) return NULL;
    
    uint32_t index = hash % table->capacity;
    while (true) {
        Hash_Table_Entry* entry = table->entries + index;
        
        if (entry->key == NULL) {
            if (IS_NIL(entry->value)) return NULL;
        } else if (entry->key->length == length && entry->key->hash == hash && memcmp(entry->key->items, items, length) == 0) {
            return entry->key;
        }
        
        index = (index + 1) % table->capacity;
    }
}

void hash_table_println(Hash_Table* table) {
    printf("Hash_Table{");
    for (uint32_t i = 0; i < table->count; i++) {
        if (i > 0) printf(", ");
        printf("(K: %s, V: ", table->entries[i].key->items);
        value_print(table->entries[i].value);
        printf(")");
        table->entries[i].key;
    }
    printf("}");
}

void hash_table_destroy(Hash_Table* table) {
    if (table->entries != NULL) {
        free(table->entries);
        table->entries = NULL;
    }
    table->count = 0;
    table->capacity = 0;
}
