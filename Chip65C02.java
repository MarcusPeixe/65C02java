class Chip65C02 {

	// Nested classes
	interface AddressingMode {
		public void getAddress(Chip65C02 thisChip);
	}

	static class Implied implements AddressingMode {
		@Override
		public void getAddress(Chip65C02 c) {
			System.out.println("Implied addressing mode.");
		}
	}

	static class Accumulator implements AddressingMode {
		@Override
		public void getAddress(Chip65C02 c) {
			System.out.println("Accumulator addressing mode.");
		}
	}

	static class Immediate implements AddressingMode {
		@Override
		public void getAddress(Chip65C02 c) {
			c.address = c.pc;
			c.incPC();
		}
	}

	static class Zeropage implements AddressingMode {
		@Override
		public void getAddress(Chip65C02 c) {
			c.address = to8b(c.mapper.read(c.pc));
			c.incPC();
		}
	}

	static class ZeropageX implements AddressingMode {
		@Override
		public void getAddress(Chip65C02 c) {
			c.address = to8b(to8b(c.mapper.read(c.pc)) + c.x);
			c.incPC();
		}
	}

	static class ZeropageY implements AddressingMode {
		@Override
		public void getAddress(Chip65C02 c) {
			c.address = to8b(to8b(c.mapper.read(c.pc)) + c.y);
			c.incPC();
		}
	}

	static class Relative implements AddressingMode {
		@Override
		public void getAddress(Chip65C02 c) {
			// Signed relative address
			c.address = toS16b(to8b(c.mapper.read(c.pc)));
			c.incPC();
		}
	}

	static class Absolute implements AddressingMode {
		@Override
		public void getAddress(Chip65C02 c) {
			c.address = to8b(c.mapper.read(c.pc));
			c.incPC();
			c.address |= toHI(c.mapper.read(c.pc));
			c.incPC();
		}
	}

	static class AbsoluteX implements AddressingMode {
		@Override
		public void getAddress(Chip65C02 c) {
			c.address = to8b(c.mapper.read(c.pc));
			c.incPC();
			c.address |= toHI(c.mapper.read(c.pc));
			c.incPC();
			
			int firstpage = getHI(c.address);
			c.address = toU16b(c.address + c.x);
			int lastpage = getHI(c.address);

			if (firstpage != lastpage) c.penaltyADDR = true;
		}
	}

	static class AbsoluteY implements AddressingMode {
		@Override
		public void getAddress(Chip65C02 c) {
			c.address = to8b(c.mapper.read(c.pc));
			c.incPC();
			c.address |= toHI(c.mapper.read(c.pc));
			c.incPC();
			
			int firstpage = getHI(c.address);
			c.address = toU16b(c.address + c.y);
			int lastpage = getHI(c.address);
			
			if (firstpage != lastpage) c.penaltyADDR = true;
		}
	}

	static class Indirect implements AddressingMode {
		@Override
		public void getAddress(Chip65C02 c) {
			int a1 = to8b(c.mapper.read(c.pc));
			c.incPC();
			a1 |= toHI(c.mapper.read(c.pc));
			c.incPC();
			// Page wrap bug
			// int a2 = to8b(a1 + 1) | toHI(a1);
			int a2 = toU16b(a1 + 1);

			c.address = to8b(c.mapper.read(a1)) | toHI(c.mapper.read(a2));
		}
	}

	static class IndirectX implements AddressingMode {
		@Override
		public void getAddress(Chip65C02 c) {
			int a1 = to8b(c.mapper.read(c.pc)) + c.x;
			c.incPC();
			
			c.address = to8b(c.mapper.read(to8b(a1))) | toHI(c.mapper.read(to8b(a1 + 1)));
		}
	}

	static class IndirectY implements AddressingMode {
		@Override
		public void getAddress(Chip65C02 c) {
			int a1 = to8b(c.mapper.read(c.pc));
			c.incPC();
			int a2 = to8b(a1 + 1);
			
			c.address = to8b(c.mapper.read(to8b(a1))) | toHI(c.mapper.read(a2));
			int firstpage = getHI(c.address);
			c.address = toU16b(c.address + c.y);
			int lastpage = getHI(c.address);

			if (firstpage != lastpage) c.penaltyADDR = true;
		}
	}

	interface OPcode {
		public void execute(Chip65C02 thisChip);
	}

	static class ADC implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.penaltyOP = true;

			int value = to8b(c.mapper.read(c.address));
			int result = c.a + value;
			
			if (c.getCarry()) result++;

			c.updateCarry(result);
			c.updateZero(result);
			c.updateOverflow(result, c.a, value);
			c.updateSign(result);

			if (c.getDecimal()) {
				c.setCarry(false);
				if (to4b(result) > 0x09) {
					result += 0x06;
				}
				if (getMStoLS(result) > 0x09) {
					result += 0x60;
					c.setCarry(true);
				}
				c.clockCycles++;
			}

			c.a = to8b(result);
		}
	}

	static class AND implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.penaltyOP = true;

			int value = to8b(c.mapper.read(c.address));
			int result = c.a & value;

			c.updateZero(result);
			c.updateSign(result);

			c.a = to8b(result);
		}
	}

	static class ASL implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			// c.penaltyOP = true;

			int value = to8b(c.mapper.read(c.address));
			int result = value << 1;

			c.updateCarry(result);
			c.updateZero(result);
			c.updateSign(result);

			c.mapper.write(c.address, to8b(result));
			// c.a = to8b(result);
		}
	}

	static class BCC implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			if (!c.getCarry()) {
				int oldpc = c.pc;
				c.pc += c.address;
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BCS implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			if (c.getCarry()) {
				int oldpc = c.pc;
				c.pc += c.address;
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BEQ implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			if (c.getZero()) {
				int oldpc = c.pc;
				c.pc += c.address;
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BIT implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			int value = to8b(c.mapper.read(c.address));
			int result = c.a & value;

			c.updateZero(result);
			c.setOverflow((value & FLAG_V) != 0);
			c.setSign((value & FLAG_S) != 0);
		}
	}

	static class BMI implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			if (c.getSign()) {
				int oldpc = c.pc;
				c.pc += c.address;
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BNE implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			if (!c.getZero()) {
				int oldpc = c.pc;
				c.pc += c.address;
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BPL implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			if (!c.getSign()) {
				int oldpc = c.pc;
				c.pc += c.address;
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BRK implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			System.out.printf("Starting breakInterrupt sequence!\n");
			c.incPC();
			c.push(getHItoLO(c.pc));
			c.push(to8b(c.pc));
			c.push(c.status | FLAG_B);
			c.setInterrupt(true);
			c.mapper.onVectorPull(IRQ_VECTOR);
			int vector = to8b(c.mapper.read(IRQ_VECTOR));
			vector |= toHI(c.mapper.read(IRQ_VECTOR + 1));
			c.pc = vector;
		}
	}

	static class BVC implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			if (!c.getOverflow()) {
				int oldpc = c.pc;
				c.pc += c.address;
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BVS implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			if (c.getOverflow()) {
				int oldpc = c.pc;
				c.pc += c.address;
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class CLC implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.setCarry(false);
		}
	}

	static class CLD implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.setDecimal(false);
		}
	}

	static class CLI implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.setInterrupt(false);
		}
	}

	static class CLV implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.setOverflow(false);
		}
	}

	static class CMP implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.penaltyOP = true;
			int value = to8b(c.mapper.read(c.address));
			int result = c.a - value;

			c.setCarry(c.a >= value);
			c.updateZero(result);
			c.updateSign(result);
		}
	}

	static class CPX implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			// c.penaltyOP = true;
			int value = to8b(c.mapper.read(c.address));
			int result = c.x - value;

			c.setCarry(c.x >= value);
			c.updateZero(result);
			c.updateSign(result);
		}
	}

	static class CPY implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			// c.penaltyOP = true;
			int value = to8b(c.mapper.read(c.address));
			int result = c.y - value;

			c.setCarry(c.y >= value);
			c.updateZero(result);
			c.updateSign(result);
		}
	}

	static class DEC implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			int value = to8b(c.mapper.read(c.address));
			int result = value - 1;

			c.updateZero(result);
			c.updateSign(result);

			c.mapper.write(c.address, to8b(result));
		}
	}

	static class DEX implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			int result = to8b(c.x - 1);

			c.updateZero(result);
			c.updateSign(result);

			c.x = result;
		}
	}

	static class DEY implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			int result = to8b(c.y - 1);

			c.updateZero(result);
			c.updateSign(result);

			c.y = result;
		}
	}

	static class EOR implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.penaltyOP = true;

			int value = to8b(c.mapper.read(c.address));
			int result = c.a ^ value;

			c.updateZero(result);
			c.updateSign(result);

			c.a = to8b(result);
		}
	}

	static class INC implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			int value = to8b(c.mapper.read(c.address));
			int result = value + 1;

			c.updateZero(result);
			c.updateSign(result);

			c.mapper.write(c.address, to8b(result));
		}
	}

	static class INX implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			int result = to8b(c.x + 1);

			c.updateZero(result);
			c.updateSign(result);

			c.x = result;
		}
	}

	static class INY implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			int result = to8b(c.y + 1);

			c.updateZero(result);
			c.updateSign(result);
		
			c.y = result;
		}
	}

	static class JMP implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.pc = c.address;
		}
	}

	static class JSR implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			int value = toU16b(c.pc - 1);
			c.push(getHItoLO(value));
			c.push(to8b(value));
			c.pc = c.address;
		}
	}

	static class LDA implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.penaltyOP = true;
			int value = to8b(c.mapper.read(c.address));

			c.updateSign(value);
			c.updateZero(value);

			c.a = value;
		}
	}

	static class LDX implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.penaltyOP = true;
			int value = to8b(c.mapper.read(c.address));

			c.updateSign(value);
			c.updateZero(value);

			c.x = value;
		}
	}

	static class LDY implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.penaltyOP = true;
			int value = to8b(c.mapper.read(c.address));

			c.updateSign(value);
			c.updateZero(value);

			c.y = value;
		}
	}

	static class LSR implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			// c.penaltyOP = true;

			int value = to8b(c.mapper.read(c.address));
			int result = value >> 1;

			c.setCarry((value & 1) != 0);
			c.updateZero(result);
			c.updateSign(result);

			c.mapper.write(c.address, to8b(result));
			// c.a = to8b(result);
		}
	}

	static class NOP implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.penaltyOP = true;
			if (
				c.opcode == 0x02 ||
				c.opcode == 0x22 ||
				c.opcode == 0x42 ||
				c.opcode == 0x62 ||
				c.opcode == 0x82 ||
				c.opcode == 0xC2 ||
				c.opcode == 0xE2 ||
				c.opcode == 0x44 ||
				c.opcode == 0x54 ||
				c.opcode == 0xD4 ||
				c.opcode == 0xF4
			) c.incPC();
			else if (
				c.opcode == 0x5C ||
				c.opcode == 0xDC ||
				c.opcode == 0xFC
			) c.pc = toU16b(c.pc + 2);
			else c.penaltyOP = false;
		}
	}

	static class ORA implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.penaltyOP = true;

			int value = to8b(c.mapper.read(c.address));
			int result = c.a | value;

			c.updateZero(result);
			c.updateSign(result);

			c.a = to8b(result);
		}
	}

	static class PHA implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.push(c.a);
		}
	}

	static class PHP implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.push(c.status | FLAG_B | FLAG_U);
		}
	}

	static class PLA implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.a = c.pull();

			c.updateZero(c.a);
			c.updateSign(c.a);
		}
	}

	static class PLP implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.status = c.pull() | FLAG_U;
		}
	}

	static class ROL implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			// c.penaltyOP = true;

			int value = to8b(c.mapper.read(c.address));
			int result = value << 1;
			if (c.getCarry()) result |= 1;

			c.updateCarry(result);
			c.updateZero(result);
			c.updateSign(result);

			c.mapper.write(c.address, to8b(result));
			// c.a = to8b(result);
		}
	}

	static class ROR implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			// c.penaltyOP = true;

			int value = to8b(c.mapper.read(c.address));
			int result = value >> 1;
			if (c.getCarry()) result |= 0b1000_0000;

			c.setCarry((value & 1) != 0);
			c.updateZero(result);
			c.updateSign(result);

			c.mapper.write(c.address, to8b(result));
			// c.a = to8b(result);
		}
	}

	static class RTI implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.status = c.pull();
			c.pc = c.pull() | toHI(c.pull());
			c.setInterrupt(false);
			// c.pc = value;
		}
	}

	static class RTS implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.pc = c.pull() | toHI(c.pull());
			c.incPC();
		}
	}

	static class SBC implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.penaltyOP = true;

			int value = to8b(c.mapper.read(c.address));
			int result = to8b(c.a - value);
			
			if (!c.getCarry()) result++;

			c.updateCarry(result);
			c.updateZero(result);
			c.updateOverflow(result, c.a, value);
			c.updateSign(result);

			if (c.getDecimal()) {
				c.setCarry(false);
				result -= 0x66;
				if (to4b(result) > 0x09) {
					result += 0x06;
				}
				if (getMStoLS(result) > 0x09) {
					result += 0x60;
					c.setCarry(true);
				}
				c.clockCycles++;
			}

			c.a = to8b(result);
		}
	}

	static class SEC implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.setCarry(true);
		}
	}

	static class SED implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.setDecimal(true);
		}
	}

	static class SEI implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.setInterrupt(true);
		}
	}

	static class STA implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.mapper.write(c.address, to8b(c.a));
		}
	}

	static class STX implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.mapper.write(c.address, to8b(c.x));
		}
	}

	static class STY implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.mapper.write(c.address, to8b(c.y));
		}
	}

	static class TAX implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.x = c.a;

			c.updateZero(c.x);
			c.updateSign(c.x);
		}
	}

	static class TAY implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.y = c.a;

			c.updateZero(c.y);
			c.updateSign(c.y);
		}
	}

	static class TSX implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.x = c.sp;

			c.updateZero(c.x);
			c.updateSign(c.x);
		}
	}

	static class TXA implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.a = c.x;

			c.updateZero(c.a);
			c.updateSign(c.a);
		}
	}

	static class TXS implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.sp = c.x;
		}
	}

	static class TYA implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			c.a = c.y;

			c.updateZero(c.a);
			c.updateSign(c.a);
		}
	}

	// Constants
	private static final int FLAG_C = 0b0000_0001;
	private static final int FLAG_Z = 0b0000_0010;
	private static final int FLAG_I = 0b0000_0100;
	private static final int FLAG_D = 0b0000_1000;

	private static final int FLAG_B = 0b0001_0000;
	private static final int FLAG_U = 0b0010_0000;
	private static final int FLAG_V = 0b0100_0000;
	private static final int FLAG_S = 0b1000_0000;

	private static final int NMI_VECTOR = 0xFFFA;
	private static final int RES_VECTOR = 0xFFFC;
	private static final int IRQ_VECTOR = 0xFFFE;

	private static final int STACK_HI = 0x0100;

	// Internal registers
	private int a, x, y, sp, status; // 8-bit
	private int pc; // 16-bit

	// Pins
	private boolean interruptRequest, nonMaskableInterrupt, reset;
	private boolean ready, sync;

	// Other variables
	private int opcode, address, clockCycles;
	private boolean penaltyOP, penaltyADDR;

	// Handle IO
	private Mapper mapper;

	// Constructors
	Chip65C02(Mapper mapper) {
		this.mapper = mapper;
		ready = false;
		reset = true;
		sync = false;
		interruptRequest = false;
		nonMaskableInterrupt = false;
		a = 0;
		x = 0;
		y = 0;
		status = 0b00110100;
		sp = 0xFF;
		pc = 0;
	}

	// Numbers
	private void incPC() {
		pc = toU16b(pc + 1);
	}

	private void push(int value) {
		mapper.write(STACK_HI | to8b(sp), to8b(value));
		sp = to8b(sp - 1);
	}

	private int pull() {
		sp = to8b(sp + 1);
		return to8b(mapper.read(STACK_HI | sp));
	}

	// Getters and setters
	public Mapper getMapper() {
		return mapper;
	}
	public void setMapper(Mapper mapper) {
		this.mapper = mapper;
	}

	public void setInterruptRequest(boolean interruptRequest) {
		this.interruptRequest = interruptRequest;
	}

	public void setNonMaskableInterrupt(boolean nonMaskableInterrupt) {
		this.nonMaskableInterrupt = nonMaskableInterrupt;
	}

	public void setReset(boolean reset) {
		this.reset = reset;
	}

	public boolean getReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	public boolean getSync() {
		return sync;
	}

	// Methods
	public void setOverflow() {
		setOverflow(true);
	}

	public void tickClock() {
		status = to8b(status | FLAG_U);
		a = to8b(a);
		x = to8b(x);
		y = to8b(y);
		sp = to8b(sp);
		pc = toU16b(pc);
		
		if (!ready) {
			System.out.printf("Not ready!\n");
			return;
		}
		if (clockCycles > 1) {
			sync = false;
			clockCycles--;
			System.out.printf("Clock cycles left = %d.\n", clockCycles);
			return;
		}

		System.out.printf("	PC = $%04X / PS = $%02X / SP = $%02X\n", pc, status, sp);
		System.out.printf("	ACC = $%02X / X = $%02X / Y = $%02X\n", a, x, y);

		if (reset) {
			System.out.printf("Starting reset sequence!\n");
			setReset(false);
			clockCycles = 7;
			status = status & 0b1100_0011 | 0b00110100;
			mapper.onVectorPull(RES_VECTOR);
			int vector = to8b(mapper.read(RES_VECTOR));
			vector |= toHI(mapper.read(RES_VECTOR + 1));
			pc = vector;
			return;
		}
		if (interruptRequest) {
			System.out.printf("Starting interruptRequest sequence!\n");
			setInterruptRequest(false);
			if (!getInterrupt()) {
				incPC();
				push(getHItoLO(pc));
				push(to8b(pc));
				push(status & ~FLAG_B);
				setInterrupt(true);
				mapper.onVectorPull(IRQ_VECTOR);
				int vector = to8b(mapper.read(IRQ_VECTOR));
				vector |= toHI(mapper.read(IRQ_VECTOR + 1));
				pc = vector;
				return;
			}
		}
		else if (nonMaskableInterrupt) {
			System.out.printf("Starting nonMaskableInterrupt sequence!\n");
			setNonMaskableInterrupt(false);
			incPC();
			push(getHItoLO(pc));
			push(to8b(pc));
			push(status & ~FLAG_B);
			mapper.onVectorPull(NMI_VECTOR);
			int vector = to8b(mapper.read(IRQ_VECTOR));
			vector |= toHI(mapper.read(IRQ_VECTOR + 1));
			pc = vector;
			return;
		}

		System.out.printf("Fetching new instruction!\n");
		sync = true;
		penaltyOP = false;
		penaltyADDR = false;

		opcode = to8b(mapper.read(pc));
		incPC();
		System.out.printf("OPcode = 0x%02X\n", opcode);

		modes[opcode].getAddress(this);
		codes[opcode].execute(this);

		clockCycles = delays[opcode];

		if (penaltyADDR && penaltyOP) clockCycles++;

		System.out.printf("Total delay = %d clock cycles!\n", clockCycles);
	}

	// Get and set flags
	private boolean getCarry() {
		return (status & FLAG_C) != 0;
	}
	private void setCarry(boolean bit) {
		if (bit) status |= FLAG_C;
		else status &= ~FLAG_C;
	}

	private boolean getZero() {
		return (status & FLAG_Z) != 0;
	}
	private void setZero(boolean bit) {
		if (bit) status |= FLAG_Z;
		else status &= ~FLAG_Z;
	}

	private boolean getInterrupt() {
		return (status & FLAG_I) != 0;
	}
	private void setInterrupt(boolean bit) {
		if (bit) status |= FLAG_I;
		else status &= ~FLAG_I;
	}

	private boolean getDecimal() {
		return (status & FLAG_D) != 0;
	}
	private void setDecimal(boolean bit) {
		if (bit) status |= FLAG_D;
		else status &= ~FLAG_D;
	}

	private boolean getOverflow() {
		return (status & FLAG_V) != 0;
	}
	private void setOverflow(boolean bit) {
		if (bit) status |= FLAG_V;
		else status &= ~FLAG_V;
	}

	private boolean getSign() {
		return (status & FLAG_S) != 0;
	}
	private void setSign(boolean bit) {
		if (bit) status |= FLAG_S;
		else status &= ~FLAG_S;
	}

	// Update flags accordingly
	private void updateCarry(int value) {
		setCarry(getHI(value) != 0);
	}

	private void updateZero(int value) {
		setZero(to8b(value) == 0);
	}

	private void updateOverflow(int value, int numA, int numB) {
		setOverflow(((value ^ numA) & (value ^ numB) & 0b1000_0000) != 0);
	}

	private void updateSign(int value) {
		setSign((value & 0b1000_0000) != 0);
	}

	// Truncate values
	private static int to8b(int value) {
		return value & 0x00FF;
	}

	private static int toU16b(int value) {
		return value & 0xFFFF;
	}

	private static int toS16b(int value) {
		return (value & 0b1000_0000) != 0 ? (value | 0xFF00): (value);
	}

	private static int getHI(int value) {
		return value & 0xFF00;
	}

	private static int getHItoLO(int value) {
		return (value & 0xFF00) >> 8;
	}

	private static int toHI(int value) {
		return to8b(value) << 8;
	}

	private static int to4b(int value) {
		return value & 0x0F;
	}

	private static int getMS(int value) {
		return value & 0xF0;
	}

	private static int getMStoLS(int value) {
		return (value & 0xF0) >> 4;
	}

	private static int toMS(int value) {
		return to4b(value) << 4;
	}

	// OP codes and addressing modes
	private static final AddressingMode[] modes = {
		new Implied(), new IndirectX(), new Implied(), new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), new Implied(), new Immediate(), new Accumulator(), new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
		new Relative(), new IndirectY(), new Implied(), new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new Implied(), new AbsoluteY(), new Implied(), new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(),
		new Absolute(), new IndirectX(), new Implied(), new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), new Implied(), new Immediate(), new Accumulator(), new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
		new Relative(), new IndirectY(), new Implied(), new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new Implied(), new AbsoluteY(), new Implied(), new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(),
		new Implied(), new IndirectX(), new Implied(), new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), new Implied(), new Immediate(), new Accumulator(), new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
		new Relative(), new IndirectY(), new Implied(), new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new Implied(), new AbsoluteY(), new Implied(), new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(),
		new Implied(), new IndirectX(), new Implied(), new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), new Implied(), new Immediate(), new Accumulator(), new Immediate(), new Indirect(), new Absolute(), new Absolute(), new Absolute(),
		new Relative(), new IndirectY(), new Implied(), new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new Implied(), new AbsoluteY(), new Implied(), new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(),
		new Immediate(), new IndirectX(), new Immediate(), new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), new Implied(), new Immediate(), new Implied(), new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
		new Relative(), new IndirectY(), new Implied(), new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageY(), new ZeropageY(), new Implied(), new AbsoluteY(), new Implied(), new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteY(), new AbsoluteY(),
		new Immediate(), new IndirectX(), new Immediate(), new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), new Implied(), new Immediate(), new Implied(), new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
		new Relative(), new IndirectY(), new Implied(), new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageY(), new ZeropageY(), new Implied(), new AbsoluteY(), new Implied(), new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteY(), new AbsoluteY(),
		new Immediate(), new IndirectX(), new Immediate(), new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), new Implied(), new Immediate(), new Implied(), new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
		new Relative(), new IndirectY(), new Implied(), new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new Implied(), new AbsoluteY(), new Implied(), new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(),
		new Immediate(), new IndirectX(), new Immediate(), new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), new Implied(), new Immediate(), new Implied(), new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
		new Relative(), new IndirectY(), new Implied(), new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new Implied(), new AbsoluteY(), new Implied(), new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(),
	};

	// OP codes, addressing modes and clock cycles
	// private static final AddressingMode[] modes = {
	// 	null, new IndirectX(), null, new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), null, new Immediate(), null, new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
	// 	new Relative(), new IndirectY(), null, new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new ZeropageX(), null, new AbsoluteY(), null, new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(),
	// 	new Absolute(), new IndirectX(), null, new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), null, new Immediate(), null, new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
	// 	new Relative(), new IndirectY(), null, new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new ZeropageX(), null, new AbsoluteY(), null, new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(),
	// 	null, new IndirectX(), null, new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), null, new Immediate(), null, new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
	// 	new Relative(), new IndirectY(), null, new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new ZeropageX(), null, new AbsoluteY(), null, new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(),
	// 	null, new IndirectX(), null, new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), null, new Immediate(), null, new Immediate(), new Indirect(), new Absolute(), new Absolute(), new Absolute(),
	// 	new Relative(), new IndirectY(), null, new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new ZeropageX(), null, new AbsoluteY(), null, new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(),
	// 	new Immediate(), new IndirectX(), new Immediate(), new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), null, new Immediate(), null, new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
	// 	new Relative(), new IndirectY(), null, new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageY(), new ZeropageY(), null, new AbsoluteY(), null, new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteY(), new AbsoluteY(),
	// 	new Immediate(), new IndirectX(), new Immediate(), new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), null, new Immediate(), null, new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
	// 	new Relative(), new IndirectY(), null, new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageY(), new ZeropageY(), null, new AbsoluteY(), null, new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteY(), new AbsoluteY(),
	// 	new Immediate(), new IndirectX(), new Immediate(), new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), null, new Immediate(), null, new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
	// 	new Relative(), new IndirectY(), null, new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new ZeropageX(), null, new AbsoluteY(), null, new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(),
	// 	new Immediate(), new IndirectX(), new Immediate(), new IndirectX(), new Zeropage(), new Zeropage(), new Zeropage(), new Zeropage(), null, new Immediate(), null, new Immediate(), new Absolute(), new Absolute(), new Absolute(), new Absolute(),
	// 	new Relative(), new IndirectY(), null, new IndirectY(), new ZeropageX(), new ZeropageX(), new ZeropageX(), new ZeropageX(), null, new AbsoluteY(), null, new AbsoluteY(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(), new AbsoluteX(),
	// };

	private static final OPcode[] codes = {
		new BRK(), new ORA(), new NOP(), new NOP(), new NOP(), new ORA(), new ASL(), new NOP(), new PHP(), new ORA(), new ASL(), new NOP(), new NOP(), new ORA(), new ASL(), new NOP(), 
		new BPL(), new ORA(), new NOP(), new NOP(), new NOP(), new ORA(), new ASL(), new NOP(), new CLC(), new ORA(), new NOP(), new NOP(), new NOP(), new ORA(), new ASL(), new NOP(), 
		new JSR(), new AND(), new NOP(), new NOP(), new BIT(), new AND(), new ROL(), new NOP(), new PLP(), new AND(), new ROL(), new NOP(), new BIT(), new AND(), new ROL(), new NOP(), 
		new BMI(), new AND(), new NOP(), new NOP(), new NOP(), new AND(), new ROL(), new NOP(), new SEC(), new AND(), new NOP(), new NOP(), new NOP(), new AND(), new ROL(), new NOP(), 
		new RTI(), new EOR(), new NOP(), new NOP(), new NOP(), new EOR(), new LSR(), new NOP(), new PHA(), new EOR(), new LSR(), new NOP(), new JMP(), new EOR(), new LSR(), new NOP(), 
		new BVC(), new EOR(), new NOP(), new NOP(), new NOP(), new EOR(), new LSR(), new NOP(), new CLI(), new EOR(), new NOP(), new NOP(), new NOP(), new EOR(), new LSR(), new NOP(), 
		new RTS(), new ADC(), new NOP(), new NOP(), new NOP(), new ADC(), new ROR(), new NOP(), new PLA(), new ADC(), new ROR(), new NOP(), new JMP(), new ADC(), new ROR(), new NOP(), 
		new BVS(), new ADC(), new NOP(), new NOP(), new NOP(), new ADC(), new ROR(), new NOP(), new SEI(), new ADC(), new NOP(), new NOP(), new NOP(), new ADC(), new ROR(), new NOP(), 
		new NOP(), new STA(), new NOP(), new NOP(), new STY(), new STA(), new STX(), new NOP(), new DEY(), new NOP(), new TXA(), new NOP(), new STY(), new STA(), new STX(), new NOP(), 
		new BCC(), new STA(), new NOP(), new NOP(), new STY(), new STA(), new STX(), new NOP(), new TYA(), new STA(), new TXS(), new NOP(), new NOP(), new STA(), new NOP(), new NOP(), 
		new LDY(), new LDA(), new LDX(), new NOP(), new LDY(), new LDA(), new LDX(), new NOP(), new TAY(), new LDA(), new TAX(), new NOP(), new LDY(), new LDA(), new LDX(), new NOP(), 
		new BCS(), new LDA(), new NOP(), new NOP(), new LDY(), new LDA(), new LDX(), new NOP(), new CLV(), new LDA(), new TSX(), new NOP(), new LDY(), new LDA(), new LDX(), new NOP(), 
		new CPY(), new CMP(), new NOP(), new NOP(), new CPY(), new CMP(), new DEC(), new NOP(), new INY(), new CMP(), new DEX(), new NOP(), new CPY(), new CMP(), new DEC(), new NOP(), 
		new BNE(), new CMP(), new NOP(), new NOP(), new NOP(), new CMP(), new DEC(), new NOP(), new CLD(), new CMP(), new NOP(), new NOP(), new NOP(), new CMP(), new DEC(), new NOP(), 
		new CPX(), new SBC(), new NOP(), new NOP(), new CPX(), new SBC(), new INC(), new NOP(), new INX(), new SBC(), new NOP(), new SBC(), new CPX(), new SBC(), new INC(), new NOP(), 
		new BEQ(), new SBC(), new NOP(), new NOP(), new NOP(), new SBC(), new INC(), new NOP(), new SED(), new SBC(), new NOP(), new NOP(), new NOP(), new SBC(), new INC(), new NOP(),
	};

	private static final int[] delays = {
		7, 6, 2, 1, 3, 3, 5, 5, 3, 2, 2, 1, 4, 4, 6, 6,  
		2, 5, 2, 1, 4, 4, 6, 6, 2, 4, 2, 1, 4, 4, 7, 7,  
		6, 6, 2, 1, 3, 3, 5, 5, 4, 2, 2, 1, 4, 4, 6, 6,  
		2, 5, 2, 1, 4, 4, 6, 6, 2, 4, 2, 1, 4, 4, 7, 7,  
		6, 6, 2, 1, 3, 3, 5, 5, 3, 2, 2, 1, 3, 4, 6, 6,  
		2, 5, 2, 1, 4, 4, 6, 6, 2, 4, 2, 1, 8, 4, 7, 7,  
		6, 6, 2, 1, 3, 3, 5, 5, 4, 2, 2, 1, 5, 4, 6, 6,  
		2, 5, 2, 1, 4, 4, 6, 6, 2, 4, 2, 1, 4, 4, 7, 7,  
		2, 6, 2, 1, 3, 3, 3, 3, 2, 2, 2, 1, 4, 4, 4, 4,  
		2, 6, 2, 1, 4, 4, 4, 4, 2, 5, 2, 1, 5, 5, 5, 5,  
		2, 6, 2, 1, 3, 3, 3, 3, 2, 2, 2, 1, 4, 4, 4, 4,  
		2, 5, 2, 1, 4, 4, 4, 4, 2, 4, 2, 1, 4, 4, 4, 4,  
		2, 6, 2, 1, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,  
		2, 5, 2, 1, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,  
		2, 6, 2, 1, 3, 3, 5, 5, 2, 2, 2, 1, 4, 4, 6, 6,  
		2, 5, 2, 1, 4, 4, 6, 6, 2, 4, 2, 1, 4, 4, 7, 7,  
	};
}