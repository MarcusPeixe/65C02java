class TestChip {
	
	static class MyMapper implements Mapper {

		private static final int RAM_SIZE = 0x0001_0000;
		private int[] ram;

		MyMapper() {
			ram = new int[RAM_SIZE];
			// final int[] program = {
			// 	0xE6, 0x02, 0xA5, 0x02, 0xC9, 0x0A, 0x30, 0xF8,
			// 	0x00, 0xEA, 0x00, 0xEA, 0x4C, 0x0C, 0x02
			// };
			final int[] program = {
				0XA2, 0X08, 0X8A, 0X95, 0X08, 0XE8, 0XD0, 0XFA,
				0XF0, 0XFE
			};
			for (int i = 0; i < RAM_SIZE; i++) {
				ram[i] = 0x00;
			}
			for (int i = 0; i < program.length; i++) {
				ram[0x0200 + i] = program[i];
			}
			ram[0xFFFC] = 0x00;
			ram[0xFFFD] = 0x02;

			ram[0xFFFE] = 0x0C;
			ram[0xFFFF] = 0x02;
		}

		public int read(int addr) {
			System.out.printf("Reading from $%04X\n", addr);
			return ram[addr];
		}
		public void write(int addr, int value) {
			System.out.printf("Writing value 0x%02X to $%04X\n", value, addr);
			ram[addr] = value;
		}

		public void onVectorPull(int addr) {
			System.out.printf("Pulling vector $%04X\n", addr);
		}

		public void memDump(int start, int end, int columns) {
			if (columns < 1) return;
			int startCbytes = start / columns * columns;
			int endCbytes = (end + columns - 1) / columns * columns;
			// System.out.printf("$%04X = ", startCbytes);
			for (int i = startCbytes; i < start; i++) {
				if (i % 256 == 0)
					System.out.printf("\nPAGE CROSS: ");
				if (i % columns == 0)
					System.out.printf("\n$%04X = ", i);
				System.out.printf("-- ");
			}
			for (int i = start; i <= end; i++) {
				if (i % 256 == 0)
					System.out.printf("\nPAGE CROSS: ");
				if (i % columns == 0)
					System.out.printf("\n$%04X = ", i);
				System.out.printf("%02X ", ram[i]);
			}
			for (int i = end + 1; i < endCbytes; i++) {
				if (i % 256 == 0)
					System.out.printf("\nPAGE CROSS: ");
				if (i % columns == 0)
					System.out.printf("\n$%04X = ", i);
				System.out.printf("-- ");
			}
			System.out.println();
		}
	}

	public static void main(String[] args) {
		MyMapper mapper = new MyMapper();
		Chip65C02 chip = new Chip65C02(mapper);
		chip.setReady(true);
		int loopCount = 260;
		for (int i = 0; i < (loopCount * 10 + 2); i++) {
			chip.tickClock();
		}
		mapper.memDump(0x0000, 0x02ff, 16);
	}
}