#include <stdio.h>
#include <assert.h>
#include <string.h>

#include "common.h"
#include "memory.h"
#include "chunk.h"
#include "vm.h"

#define REPL_MAX 1024

static void run_repl();
static char* read_file(const char *path);
static void run_file(const char* input_file_name, const char *output_file_name);
static void run_file_possibly_flag(const char* input_file_name, const char *output_file_name);
static void run_file_with_options(int argc, char **argv);
static void usage(FILE* stream);

static char* program_name;

int main(int argc, char *argv[]) {    
    vm_init();
    
    program_name = argv[0];
    
    switch (argc-1) {
        case 0: run_repl(); break;
        case 1: run_file_possibly_flag(argv[1], "a.out"); break;
        default: run_file_with_options(argc-1, &argv[1]);
    }

    vm_destroy();
    return 0;
}

// Helper Functions.
// ---------------------------------------------------------------------------------------------- //

static void run_repl() {
    char line[REPL_MAX];

    for (;;) {
        printf("> ");
        
        if (!fgets(line, sizeof(line), stdin) || line[1] == '\0') {
            printf("\n");
            break;
        }

        vm_interpret(line);
    }
}

static char* read_file(const char *path) {
    FILE* file = fopen(path, "rb");
    assert(file != NULL);
    
    fseek(file, 0L, SEEK_END);
    long size = ftell(file);
    fseek(file, 0L, SEEK_SET);
    
    char* buffer = MAKE_ARRAY(char, size + 1);
    assert(buffer != NULL);
    
    size_t read = fread(buffer, sizeof(char), size, file);
    buffer[size] = '\0';
    
    return buffer;
}

static void run_file_possibly_flag(const char* input_file_name, const char *output_file_name) {
    if (*input_file_name == '-') {
        switch (input_file_name[1]) {
            case 'h': usage(stdout); exit(0);
            case 'o': {
    			fprintf(stderr, "Error: Flag '-o' expects output file name as an argument.\n");
    			usage(stderr);
    			exit(1);
            }
            default: {
    			fprintf(stderr, "Error: Unexpected flag '-%c'.\n", input_file_name[1]);
    			usage(stderr);
    			exit(1);
            }
        }
    }
    run_file(input_file_name, output_file_name);
}

static void run_file(const char* input_file_name, const char *output_file_name) {
    char *source = read_file(input_file_name);

    printf("Started running %s\n", input_file_name);
    
    Interpret_Result result = vm_interpret(source);
    free(source);
    
    if (result == INTERPRET_COMPILE_ERROR) exit(65);
    if (result == INTERPRET_RUNTIME_ERROR) exit(70);
    
    printf("Finished running %s\n", input_file_name);
}

// This functions assumes that the arguments have been shifted to the right.
static void run_file_with_options(int argc, char** argv) {
    char* output_file_name = NULL;
    char* input_file_name = NULL;

    for (int i = 0; i < argc; i++) {
		const char * const flag = argv[i];

		switch (flag[0]) {
		    // Is a documented flag, possibly with arguments
		    case '-': {
		        switch (flag[1]) {
		            case 'o': {
		                if (i+1 >= argc || *argv[i+1] == '-') {
                            fprintf(stderr, "Error: Flag '-o' expects output file name as an argument.\n");
                            usage(stderr);
                            exit(1);
			            }
                        output_file_name = argv[i + 1];
                        i++;
		            } break;
		            
		            default: {
                        fprintf(stderr, "Error: Unexpected flag '-%c'.\n", flag[1]);
                        usage(stderr);
    			        exit(1);
		            } break;
		        }
		    } break;
		    
		    // Must be the input_file_name.
		    default: {
    	        if (input_file_name != NULL) {
                    fprintf(stderr, "Error: Unexpected flag '%s', the input file was already '%s'.\n", flag, input_file_name);
                    usage(stderr);
    			    exit(1);
                }
                input_file_name = (char*)flag;
            } break;
		}
	}
    
    if (output_file_name == NULL) {
        output_file_name = "a.out";
    }
    if (input_file_name == NULL) {
        fprintf(stderr, "Error: Expected input file name.\n");
        usage(stderr);
        exit(1);
    }
    
    run_file(input_file_name, output_file_name);
}

static void usage(FILE* stream) {
    fprintf(stream, "Usage %s <options> file.lox\n", program_name);
    fprintf(stream, "Options:\n");
    fprintf(stream, "    -o output_file_name        Name of the executable.\n");
    fprintf(stream, "    -h                         Print this help of the usage.\n");
}
