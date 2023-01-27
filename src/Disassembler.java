import java.util.*;

class FirstPass {
	final static boolean[] isJmp = {
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		true,  false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		true,  false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		true,  false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		false, false, false, false, false, false, false, false, false, false, false, false, true,  false, false, true,
		true,  false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		true,  false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		true,  false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		true,  false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		true,  false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		true,  false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
		true,  false, false, false, false, false, false, false, false, false, false, false, false, false, false, true,
	};
	
	final static boolean[] endsCode = {
		true,  false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
		true,  false, false, false, false, false, false, false, false, false, false, false, true,  false, false, false,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
		true,  false, false, false, false, false, false, false, false, false, false, false, true,  false, false, false,
		false, false, false, false, false, false, false, false, false, false, false, false, true,  false, false, false,
		true,  false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
		false, false, false, false, false, false, false, false, false, false, false, true,  false, false, false, false,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
		false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
	};

	final static int[] InstrSize = {
		1, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
		3, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
		1, 2, 1, 2, 3, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
		1, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 1, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 1, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 1, 3, 1, 3, 3, 3, 3, 3,
	};
	Map<Integer, String> symbolTable = new HashMap<>();
	Map<Integer, Boolean> isCodeArea = new HashMap<>();
	Queue<Integer> entryPoints = new LinkedList<>();

	public void registerLabel(int pos) {
		if (!symbolTable.containsKey(pos) || symbolTable.get(pos).equals("*")) {
//			System.out.printf("Registering label at $%04X\n", pos);
			symbolTable.put(pos, String.format("L%04X", pos));
			isCodeArea.put(pos, true);
			entryPoints.add(pos);
		}
	}

	public void registerLabel(String label, int pos) {
		if (!symbolTable.containsKey(pos) || symbolTable.get(pos).equals("*")) {
//			System.out.printf("Registering \"%s\" at $%04X\n", label, pos);
			symbolTable.put(pos, label);
			isCodeArea.put(pos, true);
			entryPoints.add(pos);
		}
	}

	public void endCodeArea(int pos) {
		if (!isCodeArea.containsKey(pos)) {
			isCodeArea.put(pos, false);
		}
	}

	public int getNextLabel(int current) {
		int[] smallestKey = {0x10000};
		symbolTable.forEach((key, value) -> {
			if (key > current && key < smallestKey[0]) {
				smallestKey[0] = key;
			}
		});
		return smallestKey[0];
	}

	public int getStartingLabel(int offset) {
		int[] startingPoint = {0};
		symbolTable.forEach((key, value) -> {
			if (key <= offset && key > startingPoint[0]) {
				startingPoint[0] = key;
			}
		});
		return startingPoint[0];
	}

	public boolean getAreaType(int current) {
		int[] thisArea = {0};
		isCodeArea.forEach((key, value) -> {
			if (key <= current && key > thisArea[0]) {
				thisArea[0] = key;
			}
		});
		return isCodeArea.getOrDefault(thisArea[0], false);
	}

	public FirstPass(int[] memory, int pc) {
		int nmiVec = Disassembler.makeU16b(memory[0xFFFA], memory[0xFFFB]);
		int resetVec = Disassembler.makeU16b(memory[0xFFFC], memory[0xFFFD]);
		int irqVec = Disassembler.makeU16b(memory[0xFFFE], memory[0xFFFF]);

		registerLabel("RESET", resetVec);
		registerLabel("IRQ", irqVec);
		registerLabel("NMI", nmiVec);
		registerLabel("*", pc);

		int skips = 0, loops = 0;

//		System.out.println("=== FIRST PASS ===");
		while (!entryPoints.isEmpty()) {
			int start = entryPoints.remove();
			int nextLabel = getNextLabel(start);
//			System.out.printf("Starting at $%04X\n", start);

			for (int pointer = start; pointer < 0x10000; ) {
				int op = memory[pointer];
//				System.out.printf("$%04X: %02X\n", pointer, op);
				if (isJmp[op]) {
					int target;
					switch (Disassembler.addressingModes[op]) {
						case 3:
							target = Disassembler.makeU16b(memory[pointer + 1], memory[pointer + 2]);
							break;
						case 11:
							target = Chip65C02.toU16b(Chip65C02.toS16b(memory[pointer + 2]) + pointer + 3);
							break;
						case 12:
							target = Chip65C02.toU16b(Chip65C02.toS16b(memory[pointer + 1]) + pointer + 2);
							break;
						default:
							throw new IllegalStateException("Unexpected value: " + Disassembler.addressingModes[op]);
					}
//					registerLabel(target);
//					System.out.printf("    Registered $%04X\n", target);
					if (target > pointer) {
						skips++;
						registerLabel("SKP" + skips, target);
					}
					else {
						loops++;
						registerLabel("LP" + loops, target);
					}
				}

				if (nextLabel <= pointer + InstrSize[op]) {
//					System.out.printf("Next label found at $%04X\n", nextLabel);
//					endCodeArea(pointer);
					break;
//					pointer = nextLabel;
//					nextLabel = getNextLabel(pointer);
				}
				else if (endsCode[op]) {
					endCodeArea(pointer + 1);
					break;
				}
				else {
					pointer += InstrSize[op];
				}
			}
		}
	}
}

public class Disassembler {
	public static FirstPass data;

//	IMM, ACC, IMP, ABS, ABX, ABY, IND, INX, ZPG, ZPX, ZPY, ZPR, REL, IZP, IZX, IZY
//	  0    1    2    3    4    5    6    7    8    9   10   11   12   13   14   15
	final static int[] addressingModes = {
		2,  14, 2,  2,  8,  8,  8,  8,  2,  0,  1,  2,  3,  3,  3,  11,
		12, 15, 13, 2,  8,  9,  9,  8,  2,  5,  1,  2,  3,  4,  4,  11,
		3,  14, 2,  2,  8,  8,  8,  8,  2,  0,  1,  2,  3,  3,  3,  11,
		12, 15, 13, 2,  9,  9,  9,  8,  2,  5,  1,  2,  4,  4,  4,  11,
		2,  14, 2,  2,  2,  8,  8,  8,  2,  0,  1,  2,  3,  3,  3,  11,
		12, 15, 13, 2,  2,  9,  9,  8,  2,  5,  2,  2,  2,  4,  4,  11,
		2,  14, 2,  2,  8,  8,  8,  8,  2,  0,  1,  2,  6,  3,  3,  11,
		12, 15, 13, 2,  9,  9,  9,  8,  2,  5,  2,  2,  7,  4,  4,  11,
		12, 14, 2,  2,  8,  8,  8,  8,  2,  0,  2,  2,  3,  3,  3,  11,
		12, 15, 13, 2,  9,  9,  10, 8,  2,  5,  2,  2,  3,  4,  4,  11,
		0,  14, 0,  2,  8,  8,  8,  8,  2,  0,  2,  2,  3,  3,  3,  11,
		12, 15, 13, 2,  9,  9,  10, 8,  2,  5,  2,  2,  4,  4,  5,  11,
		0,  14, 2,  2,  8,  8,  8,  8,  2,  0,  2,  2,  3,  3,  3,  11,
		12, 15, 13, 2,  2,  9,  9,  8,  2,  5,  2,  2,  2,  4,  4,  11,
		0,  14, 2,  2,  8,  8,  8,  8,  2,  0,  2,  2,  3,  3,  3,  11,
		12, 15, 13, 2,  2,  9,  9,  8,  2,  5,  2,  2,  2,  4,  4,  11,
	};

//		Impl, IndX, Impl, Impl, Zrpg, Zrpg, Zrpg, Zrpg, Impl, Imme, Accu, Impl, Abso, Abso, Abso, ZRel,
//		Rltv, IndY, ZpIn, Impl, Zrpg, ZrpX, ZrpX, Zrpg, Impl, AbsY, Accu, Impl, Abso, AbsX, AbsX, ZRel,
//		Abso, IndX, Impl, Impl, Zrpg, Zrpg, Zrpg, Zrpg, Impl, Imme, Accu, Impl, Abso, Abso, Abso, ZRel,
//		Rltv, IndY, ZpIn, Impl, ZrpX, ZrpX, ZrpX, Zrpg, Impl, AbsY, Accu, Impl, AbsX, AbsX, AbsX, ZRel,
//		Impl, IndX, Impl, Impl, Impl, Zrpg, Zrpg, Zrpg, Impl, Imme, Accu, Impl, Abso, Abso, Abso, ZRel,
//		Rltv, IndY, ZpIn, Impl, Impl, ZrpX, ZrpX, Zrpg, Impl, AbsY, Impl, Impl, Impl, AbsX, AbsX, ZRel,
//		Impl, IndX, Impl, Impl, Zrpg, Zrpg, Zrpg, Zrpg, Impl, Imme, Accu, Impl, Indr, Abso, Abso, ZRel,
//		Rltv, IndY, ZpIn, Impl, ZrpX, ZrpX, ZrpX, Zrpg, Impl, AbsY, Impl, Impl, AbIX, AbsX, AbsX, ZRel,
//		Rltv, IndX, Impl, Impl, Zrpg, Zrpg, Zrpg, Zrpg, Impl, Imme, Impl, Impl, Abso, Abso, Abso, ZRel,
//		Rltv, IndY, ZpIn, Impl, ZrpX, ZrpX, ZrpY, Zrpg, Impl, AbsY, Impl, Impl, Abso, AbsX, AbsX, ZRel,
//		Imme, IndX, Imme, Impl, Zrpg, Zrpg, Zrpg, Zrpg, Impl, Imme, Impl, Impl, Abso, Abso, Abso, ZRel,
//		Rltv, IndY, ZpIn, Impl, ZrpX, ZrpX, ZrpY, Zrpg, Impl, AbsY, Impl, Impl, AbsX, AbsX, AbsY, ZRel,
//		Imme, IndX, Impl, Impl, Zrpg, Zrpg, Zrpg, Zrpg, Impl, Imme, Impl, Impl, Abso, Abso, Abso, ZRel,
//		Rltv, IndY, ZpIn, Impl, Impl, ZrpX, ZrpX, Zrpg, Impl, AbsY, Impl, Impl, Impl, AbsX, AbsX, ZRel,
//		Imme, IndX, Impl, Impl, Zrpg, Zrpg, Zrpg, Zrpg, Impl, Imme, Impl, Impl, Abso, Abso, Abso, ZRel,
//		Rltv, IndY, ZpIn, Impl, Impl, ZrpX, ZrpX, Zrpg, Impl, AbsY, Impl, Impl, Impl, AbsX, AbsX, ZRel,

	final static String[] mnemonics = {
		"BRK", "ORA", "NOP", "NOP", "TSB", "ORA", "ASL", "RMB0", "PHP", "ORA", "ASL", "NOP", "TSB", "ORA", "ASL", "BBR0",
		"BPL", "ORA", "ORA", "NOP", "TRB", "ORA", "ASL", "RMB1", "CLC", "ORA", "INC", "NOP", "TRB", "ORA", "ASL", "BBR1",
		"JSR", "AND", "NOP", "NOP", "BIT", "AND", "ROL", "RMB2", "PLP", "AND", "ROL", "NOP", "BIT", "AND", "ROL", "BBR2",
		"BMI", "AND", "AND", "NOP", "BIT", "AND", "ROL", "RMB3", "SEC", "AND", "DEC", "NOP", "BIT", "AND", "ROL", "BBR3",
		"RTI", "EOR", "NOP", "NOP", "NOP", "EOR", "LSR", "RMB4", "PHA", "EOR", "LSR", "NOP", "JMP", "EOR", "LSR", "BBR4",
		"BVC", "EOR", "EOR", "NOP", "NOP", "EOR", "LSR", "RMB5", "CLI", "EOR", "PHY", "NOP", "NOP", "EOR", "LSR", "BBR5",
		"RTS", "ADC", "NOP", "NOP", "STZ", "ADC", "ROR", "RMB6", "PLA", "ADC", "ROR", "NOP", "JMP", "ADC", "ROR", "BBR6",
		"BVS", "ADC", "ADC", "NOP", "STZ", "ADC", "ROR", "RMB7", "SEI", "ADC", "PLY", "NOP", "JMP", "ADC", "ROR", "BBR7",
		"BRA", "STA", "NOP", "NOP", "STY", "STA", "STX", "SMB0", "DEY", "BIT", "TXA", "NOP", "STY", "STA", "STX", "BBS0",
		"BCC", "STA", "STA", "NOP", "STY", "STA", "STX", "SMB1", "TYA", "STA", "TXS", "NOP", "STZ", "STA", "STZ", "BBS1",
		"LDY", "LDA", "LDX", "NOP", "LDY", "LDA", "LDX", "SMB2", "TAY", "LDA", "TAX", "NOP", "LDY", "LDA", "LDX", "BBS2",
		"BCS", "LDA", "LDA", "NOP", "LDY", "LDA", "LDX", "SMB3", "CLV", "LDA", "TSX", "NOP", "LDY", "LDA", "LDX", "BBS3",
		"CPY", "CMP", "NOP", "NOP", "CPY", "CMP", "DEC", "SMB4", "INY", "CMP", "DEX", "WAI", "CPY", "CMP", "DEC", "BBS4",
		"BNE", "CMP", "CMP", "NOP", "NOP", "CMP", "DEC", "SMB5", "CLD", "CMP", "PHX", "STP", "NOP", "CMP", "DEC", "BBS5",
		"CPX", "SBC", "NOP", "NOP", "CPX", "SBC", "INC", "SMB6", "INX", "SBC", "NOP", "SBC", "CPX", "SBC", "INC", "BBS6",
		"BEQ", "SBC", "SBC", "NOP", "NOP", "SBC", "INC", "SMB7", "SED", "SBC", "PLX", "NOP", "NOP", "SBC", "INC", "BBS7",
	};

	public static void disassemble(Chip65C02 chip, int[] memory) {
		data = new FirstPass(memory, chip.getProgramCounter());
	}

	public static String scroll(Chip65C02 chip, int[] memory, int offset) {
		List<String> lines = new LinkedList<>();
		int start = data.getStartingLabel(offset);
		boolean skipping = true;

		int nextLabel = data.getNextLabel(start);
		String thisLabel = data.symbolTable.get(start);

		int BrkCount = 0;

		for (int pointer = start; pointer < 0x10000;) {
			int op = memory[pointer];
			if (skipping) {
				if (offset < pointer + FirstPass.InstrSize[op]) {
					skipping = false;
				}
				else {
					pointer += FirstPass.InstrSize[op];
					thisLabel = null;
					continue;
				}
			}

			if (op == 0 && !data.getAreaType(pointer)) {
				if (BrkCount > 0) {
					if (BrkCount == 1) {
						lines.add("         ...");
						if (lines.size() >= 16) break;
						BrkCount = 2;
					}

					if (nextLabel <= pointer + 1) {
						pointer = nextLabel;
						thisLabel = data.symbolTable.get(nextLabel);
						nextLabel = data.getNextLabel(pointer);
					}
					else {
						pointer += 1;
						thisLabel = null;
					}
					continue;
				}
				else {
					BrkCount = 1;
				}
			}
			else {
				BrkCount = 0;
			}

			String mnemonic = mnemonics[op];
			String operand;
			String area;
			String prefix;
			String programCounter;
			int addr;

//	IMM, ACC, IMP, ABS, ABX, ABY, IND, INX, ZPG, ZPX, ZPY, ZPR, REL, IZP, IZX, IZY
//	  0    1    2    3    4    5    6    7    8    9   10   11   12   13   14   15
			switch (addressingModes[op]) {
				case 0:
					operand = String.format("#$%02X", memory[Chip65C02.toU16b(pointer + 1)]);
					break;
				case 1:
					operand = "A";
					break;
				case 2:
					operand = "";
					break;
				case 3:
					addr = Disassembler.makeU16b(memory[Chip65C02.toU16b(pointer + 1)],
									memory[Chip65C02.toU16b(pointer + 2)]);
					if (FirstPass.isJmp[op] && data.symbolTable.containsKey(addr)) {
						operand = data.symbolTable.get(addr);
					}
					else {
						operand = String.format("$%04X", addr);
					}
					break;
				case 4:
					operand = String.format("$%02X%02X,X", memory[Chip65C02.toU16b(pointer + 2)],
							memory[Chip65C02.toU16b(pointer + 1)]);
					break;
				case 5:
					operand = String.format("$%02X%02X,Y", memory[Chip65C02.toU16b(pointer + 2)],
							memory[Chip65C02.toU16b(pointer + 1)]);
					break;
				case 6:
					operand = String.format("($%02X%02X)", memory[Chip65C02.toU16b(pointer + 2)],
							memory[Chip65C02.toU16b(pointer + 1)]);
					break;
				case 7:
					operand = String.format("($%02X%02X,X)", memory[Chip65C02.toU16b(pointer + 2)],
							memory[Chip65C02.toU16b(pointer + 1)]);
					break;
				case 8:
					operand = String.format("$%02X", memory[Chip65C02.toU16b(pointer + 1)]);
					break;
				case 9:
					operand = String.format("$%02X,X", memory[Chip65C02.toU16b(pointer + 1)]);
					break;
				case 10:
					operand = String.format("$%02X,Y", memory[Chip65C02.toU16b(pointer + 1)]);
					break;
				case 11:
					addr = Chip65C02.toU16b(Chip65C02.toS16b(
									memory[Chip65C02.toU16b(pointer + 2)]) + pointer + 3);
					if (data.symbolTable.containsKey(addr)) {
						operand = String.format("$%02X,%s", memory[Chip65C02.toU16b(pointer + 1)],
							data.symbolTable.get(addr));
					}
					else {
						operand = String.format("$%04X", addr);
					}
					break;
				case 12:
					addr = Chip65C02.toU16b(Chip65C02.toS16b(
							memory[Chip65C02.toU16b(pointer + 1)]) + pointer + 2);
					if (data.symbolTable.containsKey(addr)) {
						operand = data.symbolTable.get(addr);
					}
					else {
						operand = String.format("$%04X", addr);
					}
					break;
				case 13:
					operand = String.format("($%02X)", memory[Chip65C02.toU16b(pointer + 1)]);
					break;
				case 14:
					operand = String.format("($%02X,X)", memory[Chip65C02.toU16b(pointer + 1)]);
					break;
				case 15:
					operand = String.format("($%02X),Y", memory[Chip65C02.toU16b(pointer + 1)]);
					break;
				default:
					throw new IllegalStateException("Unexpected value: " + addressingModes[op]);
			}

			StringBuilder bytes = new StringBuilder();
			for (int i = 0; i < 3; i++) {

				if (i >= FirstPass.InstrSize[op]) {
					bytes.append("     ");
				}
				else if (nextLabel <= pointer + i) {
					if (chip.getProgramCounter() == pointer + i)
						bytes.append(" *");
					else
						bytes.append("  ");
					bytes.append(".. ");
				}
				else {
					if (offset == pointer + i)
						bytes.append(">");
					else
						bytes.append(" ");

					if (chip.getProgramCounter() == pointer + i)
						bytes.append("*");
					else
						bytes.append(" ");
					bytes.append(String.format("%02X ", memory[Chip65C02.toU16b(pointer + i)]));
				}
			}

			if (data.getAreaType(pointer) /*|| data.isCodeArea.containsKey(pointer)*/) {
				area = "|";
			}
			else {
				area = " ";
			}

			if (thisLabel == null || thisLabel.equals("*")) {
				prefix = "      ";
			}
			else {
				int spaces = 5 - thisLabel.length();
				prefix = thisLabel + ":" + String.join("", Collections.nCopies(spaces, " "));
			}

			if (chip.getProgramCounter() == pointer) {
				programCounter = "* ";
			}
			else {
				programCounter = "  ";
			}

			String line = String.format(
					"$%04X: %s %s    %s %s%s %s",
					pointer, bytes, area, prefix, programCounter, mnemonic, operand
			);
			lines.add(line);
			if (lines.size() >= 16) break;

			if (nextLabel <= pointer + FirstPass.InstrSize[op]) {
				pointer = nextLabel;
				thisLabel = data.symbolTable.get(nextLabel);
				nextLabel = data.getNextLabel(pointer);
			}
			else {
				pointer += FirstPass.InstrSize[op];
				thisLabel = null;
			}
		}

		return String.join("\n", lines);
	}

	public static int makeU16b(int b1, int b2) {
		return Chip65C02.toU16b(Chip65C02.to8b(b1) | Chip65C02.toHI(b2));
	}
}
