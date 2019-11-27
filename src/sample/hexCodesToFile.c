#include <stdio.h>
#include <stdlib.h>

int main(int argc, char const *argv[])
{
	if (argc < 2) exit(1);
	FILE *f = fopen(argv[1], "wb");
	int byte;
	while (scanf(" %X", &byte) == 1) {
		fputc(byte, f);
	}
	fclose(f);
	return 0;
}