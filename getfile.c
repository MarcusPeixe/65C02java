#include <stdio.h>
#include <stdlib.h>

int main(int argc, const char **args) {
	if (argc < 3) {
		printf("Usage:\n./a.out INPUT_FILE OUTPUT_FILE\n");
		return -1;
	}
	FILE *fin = fopen(args[1], "rb");
	FILE *fout = fopen(args[2], "w");
	int counter = 0;
	while (1) {
		char c = fgetc(fin);
		if (feof(fin)) break;
		if ((counter++ & 0x07) == 0)
			fprintf(fout, "\n\t\t\t\t");
		fprintf(fout, "0x%02X, ", (unsigned int) c);
	}
	fclose(fin);
	fclose(fout);
}