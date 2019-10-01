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
		}
	}

	static class DEY implements OPcode {
		@Override
		public void execute(Chip65C02 c) {
			int result = to8b(c.y - 1);

			c.updateZero(result);
			c.updateSign(result);
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

	private static final int NMI_VECTOR = 0xFA;
	private static final int RES_VECTOR = 0xFC;
	private static final int IRQ_VECTOR = 0xFE;

	private static final int STACK_HI = 0x0100;

	// Internal registers
	private int a, x, y, sp, status; // 8-bit
	private int pc; // 16-bit

	// Pins
	private boolean interruptRequest, nonMaskableInterrupt;
	private boolean memoryLock, ready, sync;

	// Other variables
	private int opcode, address, clockCycles;
	private boolean penaltyOP, penaltyADDR;

	// Handle IO
	private Mapper mapper;

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
		return mapper.read(STACK_HI | sp);
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

	public boolean getReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	public boolean getSync() {
		return sync;
	}

	public boolean getMemoryLock() {
		return memoryLock;
	}

	// Methods
	public void setOverflow() {
		setOverflow(true);
	}

	public void reset() {

	}

	public void tickClock() {

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

	// private static final OPcode[] codes = {
	// 	new BRK(), new ORA(), new NOP(), new SLO(), new NOP(), new ORA(), new ASL(), new SLO(), new PHP(), new ORA(), new ASL(), new NOP(), new NOP(), new ORA(), new ASL(), new SLO(), 
	// 	new BPL(), new ORA(), new NOP(), new SLO(), new NOP(), new ORA(), new ASL(), new SLO(), new CLC(), new ORA(), new NOP(), new SLO(), new NOP(), new ORA(), new ASL(), new SLO(), 
	// 	new JSR(), new AND(), new NOP(), new RLA(), new BIT(), new AND(), new ROL(), new RLA(), new PLP(), new AND(), new ROL(), new NOP(), new BIT(), new AND(), new ROL(), new RLA(), 
	// 	new BMI(), new AND(), new NOP(), new RLA(), new NOP(), new AND(), new ROL(), new RLA(), new SEC(), new AND(), new NOP(), new RLA(), new NOP(), new AND(), new ROL(), new RLA(), 
	// 	new RTI(), new EOR(), new NOP(), new SRE(), new NOP(), new EOR(), new LSR(), new SRE(), new PHA(), new EOR(), new LSR(), new NOP(), new JMP(), new EOR(), new LSR(), new SRE(), 
	// 	new BVC(), new EOR(), new NOP(), new SRE(), new NOP(), new EOR(), new LSR(), new SRE(), new CLI(), new EOR(), new NOP(), new SRE(), new NOP(), new EOR(), new LSR(), new SRE(), 
	// 	new RTS(), new ADC(), new NOP(), new RRA(), new NOP(), new ADC(), new ROR(), new RRA(), new PLA(), new ADC(), new ROR(), new NOP(), new JMP(), new ADC(), new ROR(), new RRA(), 
	// 	new BVS(), new ADC(), new NOP(), new RRA(), new NOP(), new ADC(), new ROR(), new RRA(), new SEI(), new ADC(), new NOP(), new RRA(), new NOP(), new ADC(), new ROR(), new RRA(), 
	// 	new NOP(), new STA(), new NOP(), new SAX(), new STY(), new STA(), new STX(), new SAX(), new DEY(), new NOP(), new TXA(), new NOP(), new STY(), new STA(), new STX(), new SAX(), 
	// 	new BCC(), new STA(), new NOP(), new NOP(), new STY(), new STA(), new STX(), new SAX(), new TYA(), new STA(), new TXS(), new NOP(), new NOP(), new STA(), new NOP(), new NOP(), 
	// 	new LDY(), new LDA(), new LDX(), new LAX(), new LDY(), new LDA(), new LDX(), new LAX(), new TAY(), new LDA(), new TAX(), new NOP(), new LDY(), new LDA(), new LDX(), new LAX(), 
	// 	new BCS(), new LDA(), new NOP(), new LAX(), new LDY(), new LDA(), new LDX(), new LAX(), new CLV(), new LDA(), new TSX(), new LAX(), new LDY(), new LDA(), new LDX(), new LAX(), 
	// 	new CPY(), new CMP(), new NOP(), new DCP(), new CPY(), new CMP(), new DEC(), new DCP(), new INY(), new CMP(), new DEX(), new NOP(), new CPY(), new CMP(), new DEC(), new DCP(), 
	// 	new BNE(), new CMP(), new NOP(), new DCP(), new NOP(), new CMP(), new DEC(), new DCP(), new CLD(), new CMP(), new NOP(), new DCP(), new NOP(), new CMP(), new DEC(), new DCP(), 
	// 	new CPX(), new SBC(), new NOP(), new ISB(), new CPX(), new SBC(), new INC(), new ISB(), new INX(), new SBC(), new NOP(), new SBC(), new CPX(), new SBC(), new INC(), new ISB(), 
	// 	new BEQ(), new SBC(), new NOP(), new ISB(), new NOP(), new SBC(), new INC(), new ISB(), new SED(), new SBC(), new NOP(), new ISB(), new NOP(), new SBC(), new INC(), new ISB(),
	// };

	private static final int[] delays = {
		7, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4, 6, 6,  
		2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,  
		6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 4, 4, 6, 6,  
		2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,  
		6, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6,  
		2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,  
		6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 5, 4, 6, 6,  
		2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,  
		2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,  
		2, 6, 2, 6, 4, 4, 4, 4, 2, 5, 2, 5, 5, 5, 5, 5,  
		2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,  
		2, 5, 2, 5, 4, 4, 4, 4, 2, 4, 2, 4, 4, 4, 4, 4,  
		2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,  
		2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,  
		2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,  
		2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,  
	};
}