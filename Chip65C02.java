class Chip65C02 {

	// Nested classes
	interface AddressingMode {
		public void calcAddress(Chip65C02 thisChip);
		public int readValue(Chip65C02 thisChip);
		public void writeValue(Chip65C02 thisChip, int value);
	}

	// IMPLIED
	static class Impl implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			if (c.debug)
				System.out.println("Implied addressing mode.");
		}
		@Override
		public int readValue(Chip65C02 c) {
			return 0;
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			// Nothing
		}
	}

	// ACCUMULATOR
	static class Accu implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			if (c.debug)
				System.out.println("Accu addressing mode.");
		}
		@Override
		public int readValue(Chip65C02 c) {
			return to8b(c.a);
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			c.a = to8b(value);
		}
	}

	// IMMEDIATE
	static class Imme implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			c.address = c.pc;
			c.incPC();
		}
		@Override
		public int readValue(Chip65C02 c) {
			return to8b(c.mapper.read(c.address, false));
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			c.mapper.write(c.address, to8b(value));
		}
	}

	// ZERO PAGE
	static class Zrpg implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			c.address = to8b(c.mapper.read(c.pc, false));
			c.incPC();
		}
		@Override
		public int readValue(Chip65C02 c) {
			return to8b(c.mapper.read(c.address, false));
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			c.mapper.write(c.address, to8b(value));
		}
	}

	// ZERO PAGE, X
	static class ZrpX implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			c.address = to8b(to8b(c.mapper.read(c.pc, false)) + c.x);
			c.incPC();
		}
		@Override
		public int readValue(Chip65C02 c) {
			return to8b(c.mapper.read(c.address, false));
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			c.mapper.write(c.address, to8b(value));
		}
	}

	// ZERO PAGE, Y
	static class ZrpY implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			c.address = to8b(to8b(c.mapper.read(c.pc, false)) + c.y);
			c.incPC();
		}
		@Override
		public int readValue(Chip65C02 c) {
			return to8b(c.mapper.read(c.address, false));
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			c.mapper.write(c.address, to8b(value));
		}
	}

	// RELATIVE BRANCH
	static class Rltv implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			// Signed Rltv address
			c.address = toS16b(to8b(c.mapper.read(c.pc, false)));
			c.incPC();
		}
		@Override
		public int readValue(Chip65C02 c) {
			return c.address;
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			// Nothing
		}
	}

	// ZERO PAGE AND RELATIVE BRANCH
	static class ZRel implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			c.address = to8b(c.mapper.read(c.pc, false));
			c.incPC();
			c.address |= toHI(c.mapper.read(c.pc, false));
			c.incPC();
		}
		@Override
		public int readValue(Chip65C02 c) {
			return c.address;
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			// Nothing
		}
	}

	// ABSOLUTE
	static class Abso implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			c.address = to8b(c.mapper.read(c.pc, false));
			c.incPC();
			c.address |= toHI(c.mapper.read(c.pc, false));
			c.incPC();
		}
		@Override
		public int readValue(Chip65C02 c) {
			return to8b(c.mapper.read(c.address, false));
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			c.mapper.write(c.address, to8b(value));
		}
	}

	// ABSOLUTE, X
	static class AbsX implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			c.address = to8b(c.mapper.read(c.pc, false));
			c.incPC();
			c.address |= toHI(c.mapper.read(c.pc, false));
			c.incPC();
			
			int firstpage = getHI(c.address);
			c.address = toU16b(c.address + c.x);
			int lastpage = getHI(c.address);

			if (firstpage != lastpage) c.penaltyADDR = true;
		}
		@Override
		public int readValue(Chip65C02 c) {
			return to8b(c.mapper.read(c.address, false));
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			c.mapper.write(c.address, to8b(value));
		}
	}

	// ABSOLUTE, Y
	static class AbsY implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			c.address = to8b(c.mapper.read(c.pc, false));
			c.incPC();
			c.address |= toHI(c.mapper.read(c.pc, false));
			c.incPC();
			
			int firstpage = getHI(c.address);
			c.address = toU16b(c.address + c.y);
			int lastpage = getHI(c.address);
			
			if (firstpage != lastpage) c.penaltyADDR = true;
		}
		@Override
		public int readValue(Chip65C02 c) {
			return to8b(c.mapper.read(c.address, false));
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			c.mapper.write(c.address, to8b(value));
		}
	}

	// INDIRECT
	static class Indr implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			int a1 = to8b(c.mapper.read(c.pc, false));
			c.incPC();
			a1 |= toHI(c.mapper.read(c.pc, false));
			c.incPC();
			// Page wrap bug
			// int a2 = to8b(a1 + 1) | toHI(a1);
			int a2 = toU16b(a1 + 1);

			c.address = to8b(c.mapper.read(a1, false)) | toHI(c.mapper.read(a2, false));
		}
		@Override
		public int readValue(Chip65C02 c) {
			return to8b(c.mapper.read(c.address, false));
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			c.mapper.write(c.address, to8b(value));
		}
	}

	// ZERO PAGE INDIRECT
	static class ZpIn implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			int a1 = to8b(c.mapper.read(c.pc, false));
			c.incPC();
			// a1 |= toHI(c.mapper.read(c.pc, false));
			// c.incPC();
			// Page wrap bug
			// int a2 = to8b(a1 + 1) | toHI(a1);
			int a2 = toU16b(a1 + 1);

			c.address = to8b(c.mapper.read(a1, false)) | toHI(c.mapper.read(a2, false));
		}
		@Override
		public int readValue(Chip65C02 c) {
			return to8b(c.mapper.read(c.address, false));
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			c.mapper.write(c.address, to8b(value));
		}
	}

	// ZERO PAGE INDEXED INDIRECT
	static class IndX implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			int a1 = to8b(c.mapper.read(c.pc, false) + c.x);
			c.incPC();
			
			c.address =
				to8b(c.mapper.read(to8b(a1), false)) |
				toHI(c.mapper.read(to8b(a1 + 1), false));
		}
		@Override
		public int readValue(Chip65C02 c) {
			return to8b(c.mapper.read(c.address, false));
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			c.mapper.write(c.address, to8b(value));
		}
	}

	// ZERO PAGE INDIRECT INDEXED
	static class IndY implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			int a1 = to8b(c.mapper.read(c.pc, false));
			c.incPC();
			int a2 = to8b(a1 + 1);
			
			c.address = to8b(c.mapper.read(to8b(a1), false)) | toHI(c.mapper.read(a2, false));
			int firstpage = getHI(c.address);
			c.address = toU16b(c.address + c.y);
			int lastpage = getHI(c.address);

			if (firstpage != lastpage) c.penaltyADDR = true;
		}
		@Override
		public int readValue(Chip65C02 c) {
			return to8b(c.mapper.read(c.address, false));
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			c.mapper.write(c.address, to8b(value));
		}
	}

	// ABSOLUTE INDEXED INDIRECT
	static class AbIX implements AddressingMode {
		@Override
		public void calcAddress(Chip65C02 c) {
			// int a1 = to8b(c.mapper.read(c.pc, false)) + c.x;
			// c.incPC();

			int a1 = to8b(c.mapper.read(c.pc, false));
			c.incPC();
			a1 |= toHI(c.mapper.read(c.pc, false));
			c.incPC();
			
			a1 += c.x;

			c.address =
				to8b(c.mapper.read(toU16b(a1), false)) |
				toHI(c.mapper.read(toU16b(a1 + 1), false));
		}
		@Override
		public int readValue(Chip65C02 c) {
			return to8b(c.mapper.read(c.address, false));
		}
		@Override
		public void writeValue(Chip65C02 c, int value) {
			c.mapper.write(c.address, to8b(value));
		}
	}

	interface OPcode {
		public void execute(Chip65C02 thisChip, AddressingMode mode);
	}

	static class ADC implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.penaltyOP = true;

			int value = m.readValue(c);
			int result = c.a + value;
			
			if (c.getCarry()) result++;

			c.updateOverflow(result, c.a, value);
			c.updateCarry(result);

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

			c.updateZero(result);
			c.updateSign(result);
			c.setSign(false);
			c.a = to8b(result);
		}
	}

	static class AND implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.penaltyOP = true;

			int value = m.readValue(c);
			int result = c.a & value;

			c.updateZero(result);
			c.updateSign(result);

			c.a = to8b(result);
		}
	}

	static class ASL implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			// c.penaltyOP = true;

			int value = m.readValue(c);
			int result = value << 1;

			c.updateCarry(result);
			c.updateZero(result);
			c.updateSign(result);

			m.writeValue(c, result);
			// c.a = to8b(result);
		}
	}

	static class BBR0 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0000_0001) == 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBR1 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0000_0010) == 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBR2 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0000_0100) == 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBR3 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0000_1000) == 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBR4 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0001_0000) == 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBR5 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0010_0000) == 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBR6 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0100_0000) == 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBR7 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b1000_0000) == 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBS0 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0000_0001) != 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBS1 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0000_0010) != 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBS2 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0000_0100) != 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBS3 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0000_1000) != 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBS4 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0001_0000) != 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBS5 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0010_0000) != 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBS6 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b0100_0000) != 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BBS7 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int values = m.readValue(c);
			int zpAddr = to8b(values);
			int rel = getHItoLO(values);
			int zpValue = c.mapper.read(zpAddr, false);
			if ((zpValue & 0b1000_0000) != 0) {
				int oldpc = c.pc;
				c.pc += rel;
				// if (getHI(oldpc) != getHI(c.pc))
				// 	c.clockCycles++;
				// c.clockCycles++;
			}
		}
	}

	static class BCC implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			if (!c.getCarry()) {
				int oldpc = c.pc;
				c.pc += m.readValue(c);
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BCS implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			if (c.getCarry()) {
				int oldpc = c.pc;
				c.pc += m.readValue(c);
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BEQ implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			if (c.getZero()) {
				int oldpc = c.pc;
				c.pc += m.readValue(c);
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BIT implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			int result = c.a & value;

			c.updateZero(result);
			c.setOverflow((value & FLAG_V) != 0);
			c.setSign((value & FLAG_S) != 0);
		}
	}

	static class BMI implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			if (c.getSign()) {
				int oldpc = c.pc;
				c.pc += m.readValue(c);
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BNE implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			if (!c.getZero()) {
				int oldpc = c.pc;
				c.pc += m.readValue(c);
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BPL implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			if (!c.getSign()) {
				int oldpc = c.pc;
				c.pc += m.readValue(c);
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BRA implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int oldpc = c.pc;
			c.pc += m.readValue(c);
			if (getHI(oldpc) != getHI(c.pc))
				c.clockCycles++;
			c.clockCycles++;
		}
	}

	static class BRK implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			// c.incPC();
			if (!c.getInterrupt()) {
				if (c.debug)
					System.out.printf("Starting breakInterrupt sequence!\n");
				c.push(getHItoLO(c.pc));
				c.push(to8b(c.pc));
				c.status &= ~FLAG_D;
				c.push(c.status | FLAG_B);
				c.setInterrupt(true);
				c.mapper.onVectorPull(IRQ_VECTOR);
				int vector = to8b(c.mapper.read(IRQ_VECTOR, false));
				vector |= toHI(c.mapper.read(IRQ_VECTOR + 1, false));
				c.pc = vector;
			}
		}
	}

	static class BVC implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			if (!c.getOverflow()) {
				int oldpc = c.pc;
				c.pc += m.readValue(c);
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class BVS implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			if (c.getOverflow()) {
				int oldpc = c.pc;
				c.pc += m.readValue(c);
				if (getHI(oldpc) != getHI(c.pc))
					c.clockCycles++;
				c.clockCycles++;
			}
		}
	}

	static class CLC implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.setCarry(false);
		}
	}

	static class CLD implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.setDecimal(false);
		}
	}

	static class CLI implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.setInterrupt(false);
		}
	}

	static class CLV implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.setOverflow(false);
		}
	}

	static class CMP implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.penaltyOP = true;
			int value = m.readValue(c);
			int result = c.a - value;

			c.setCarry(c.a >= value);
			c.updateZero(result);
			c.updateSign(result);
		}
	}

	static class CPX implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			// c.penaltyOP = true;
			int value = m.readValue(c);
			int result = c.x - value;

			c.setCarry(c.x >= value);
			c.updateZero(result);
			c.updateSign(result);
		}
	}

	static class CPY implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			// c.penaltyOP = true;
			int value = m.readValue(c);
			int result = c.y - value;

			c.setCarry(c.y >= value);
			c.updateZero(result);
			c.updateSign(result);
		}
	}

	static class DEC implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			int result = value - 1;

			c.updateZero(result);
			c.updateSign(result);

			m.writeValue(c, result);
		}
	}

	static class DEX implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int result = to8b(c.x - 1);

			c.updateZero(result);
			c.updateSign(result);

			c.x = result;
		}
	}

	static class DEY implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int result = to8b(c.y - 1);

			c.updateZero(result);
			c.updateSign(result);

			c.y = result;
		}
	}

	static class EOR implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.penaltyOP = true;

			int value = m.readValue(c);
			int result = c.a ^ value;

			c.updateZero(result);
			c.updateSign(result);

			c.a = to8b(result);
		}
	}

	static class INC implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			int result = value + 1;

			c.updateZero(result);
			c.updateSign(result);

			m.writeValue(c, result);
		}
	}

	static class INX implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int result = to8b(c.x + 1);

			c.updateZero(result);
			c.updateSign(result);

			c.x = result;
		}
	}

	static class INY implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int result = to8b(c.y + 1);

			c.updateZero(result);
			c.updateSign(result);
		
			c.y = result;
		}
	}

	static class JMP implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.pc = c.address;
		}
	}

	static class JSR implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = toU16b(c.pc - 1);
			c.push(getHItoLO(value));
			c.push(to8b(value));
			c.pc = c.address;
		}
	}

	static class LDA implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.penaltyOP = true;
			int value = m.readValue(c);

			c.updateSign(value);
			c.updateZero(value);

			c.a = value;
		}
	}

	static class LDX implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.penaltyOP = true;
			int value = m.readValue(c);

			c.updateSign(value);
			c.updateZero(value);

			c.x = value;
		}
	}

	static class LDY implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.penaltyOP = true;
			int value = m.readValue(c);

			c.updateSign(value);
			c.updateZero(value);

			c.y = value;
		}
	}

	static class LSR implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			// c.penaltyOP = true;

			int value = m.readValue(c);
			int result = value >> 1;

			c.setCarry((value & 1) != 0);
			c.updateZero(result);
			c.updateSign(result);

			m.writeValue(c, result);
			// c.a = to8b(result);
		}
	}

	static class NOP implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.penaltyOP = true;
			if (
				(c.opcode & 0x07) == 3
			) c.pc = toU16b(c.pc + 0);

			if (
				(c.opcode & 0x1F) == 2 ||
				c.opcode == 0x44 ||
				c.opcode == 0x54 ||
				c.opcode == 0xD4 ||
				c.opcode == 0xF4
			) c.pc = toU16b(c.pc + 1);

			if (
				c.opcode == 0x5C ||
				c.opcode == 0xDC ||
				c.opcode == 0xFC 
			) c.pc = toU16b(c.pc + 2);

			else c.penaltyOP = false;
		}
	}

	static class ORA implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.penaltyOP = true;

			int value = m.readValue(c);
			int result = c.a | value;

			c.updateZero(result);
			c.updateSign(result);

			c.a = to8b(result);
		}
	}

	static class PHA implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.push(c.a);
		}
	}

	static class PHX implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.push(c.x);
		}
	}

	static class PHY implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.push(c.y);
		}
	}

	static class PHP implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.push(c.status | FLAG_B | FLAG_U);
		}
	}

	static class PLA implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.a = c.pull();

			c.updateZero(c.a);
			c.updateSign(c.a);
		}
	}

	static class PLX implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.x = c.pull();

			c.updateZero(c.x);
			c.updateSign(c.x);
		}
	}

	static class PLY implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.y = c.pull();

			c.updateZero(c.y);
			c.updateSign(c.y);
		}
	}

	static class PLP implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.status = c.pull() | FLAG_U;
		}
	}

	static class RMB0 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value & 0b1111_1110;
			m.writeValue(c, value);
		}
	}

	static class RMB1 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value & 0b1111_1101;
			m.writeValue(c, value);
		}
	}

	static class RMB2 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value & 0b1111_1011;
			m.writeValue(c, value);
		}
	}

	static class RMB3 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value & 0b1111_0111;
			m.writeValue(c, value);
		}
	}

	static class RMB4 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value & 0b1110_1111;
			m.writeValue(c, value);
		}
	}

	static class RMB5 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value & 0b1101_1111;
			m.writeValue(c, value);
		}
	}

	static class RMB6 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value & 0b1011_1111;
			m.writeValue(c, value);
		}
	}

	static class RMB7 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value & 0b0111_1111;
			m.writeValue(c, value);
		}
	}

	static class ROL implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			// c.penaltyOP = true;

			int value = m.readValue(c);
			int result = value << 1;
			if (c.getCarry()) result |= 1;

			c.updateCarry(result);
			c.updateZero(result);
			c.updateSign(result);

			m.writeValue(c, result);
			// c.a = to8b(result);
		}
	}

	static class ROR implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			// c.penaltyOP = true;

			int value = m.readValue(c);
			int result = value >> 1;
			if (c.getCarry()) result |= 0b1000_0000;

			c.setCarry((value & 1) != 0);
			c.updateZero(result);
			c.updateSign(result);

			m.writeValue(c, result);
			// c.a = to8b(result);
		}
	}

	static class RTI implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.status = c.pull();
			c.pc = c.pull() | toHI(c.pull());
			c.setInterrupt(false);
			// c.pc = value;
		}
	}

	static class RTS implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.pc = c.pull() | toHI(c.pull());
			c.incPC();
		}
	}

	static class SBC implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.penaltyOP = true;

			int value = m.readValue(c);
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
		public void execute(Chip65C02 c, AddressingMode m) {
			c.setCarry(true);
		}
	}

	static class SED implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.setDecimal(true);
		}
	}

	static class SEI implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.setInterrupt(true);
		}
	}

	static class SMB0 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value | 0b0000_0001;
			m.writeValue(c, value);
		}
	}

	static class SMB1 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value | 0b0000_0010;
			m.writeValue(c, value);
		}
	}

	static class SMB2 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value | 0b0000_0100;
			m.writeValue(c, value);
		}
	}

	static class SMB3 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value | 0b0000_1000;
			m.writeValue(c, value);
		}
	}

	static class SMB4 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value | 0b0001_0000;
			m.writeValue(c, value);
		}
	}

	static class SMB5 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value | 0b0010_0000;
			m.writeValue(c, value);
		}
	}

	static class SMB6 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value | 0b0100_0000;
			m.writeValue(c, value);
		}
	}

	static class SMB7 implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			value = value | 0b1000_0000;
			m.writeValue(c, value);
		}
	}

	static class STA implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			m.writeValue(c, c.a);
		}
	}

	static class STP implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.setReady(false);
		}
	}

	static class STX implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			m.writeValue(c, c.x);
		}
	}

	static class STY implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			m.writeValue(c, c.y);
		}
	}

	static class STZ implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			m.writeValue(c, 0);
		}
	}

	static class TAX implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.x = c.a;

			c.updateZero(c.x);
			c.updateSign(c.x);
		}
	}

	static class TAY implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.y = c.a;

			c.updateZero(c.y);
			c.updateSign(c.y);
		}
	}

	static class TRB implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			int result = c.a & value;
			int result2 = ~c.a & value;

			c.updateZero(result);
			m.writeValue(c, result2);
		}
	}

	static class TSB implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			int value = m.readValue(c);
			int result = c.a & value;
			int result2 = c.a | value;

			c.updateZero(result);
			m.writeValue(c, result2);
		}
	}

	static class TSX implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.x = c.sp;

			c.updateZero(c.x);
			c.updateSign(c.x);
		}
	}

	static class TXA implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.a = c.x;

			c.updateZero(c.a);
			c.updateSign(c.a);
		}
	}

	static class TXS implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.sp = c.x;
		}
	}

	static class TYA implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			c.a = c.y;

			c.updateZero(c.a);
			c.updateSign(c.a);
		}
	}

	static class WAI implements OPcode {
		@Override
		public void execute(Chip65C02 c, AddressingMode m) {
			// c.setReady(false);
			c.pc = toU16b(c.pc - 1);
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
	private boolean ready;

	// Other variables
	private int opcode, clockCycles;
	private int address;
	private boolean penaltyOP, penaltyADDR;
	private boolean debug;

	// Handle IO
	private Mapper mapper;

	// Constructors
	Chip65C02(Mapper mapper) {
		this.mapper = mapper;
		ready = false;
		reset = true;
		interruptRequest = false;
		nonMaskableInterrupt = false;
		a = 0;
		x = 0;
		y = 0;
		status = 0b00110000;
		sp = 0xFF;
		pc = 0x0400;
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
		return to8b(mapper.read(STACK_HI | sp, false));
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

	public void setDebug(boolean debug) {
		this.debug = debug;
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
			if (debug)
				System.out.printf("Not ready!\n");
			return;
		}
		if (clockCycles > 1) {
			clockCycles--;
			if (debug)
				System.out.printf("Clock cycles left = %d.\n", clockCycles);
			return;
		}

		if (debug) {
			System.out.printf("\n\n--== NEW INSTRUCTION FETCH ==--\n");
			System.out.printf("	PC = $%04X / PS = $%02X / SP = $%02X\n", pc, status, sp);
			System.out.printf("	ACC = $%02X / X = $%02X / Y = $%02X\n", a, x, y);
			System.out.printf("	NV-B DIZC (PROCESSOR STATUS FLAGS)\n");
			System.out.printf("	%s\n\n", asBinary(status, 8));
		}

		if (reset) {
			if (debug)
				System.out.printf("Starting reset sequence!\n");
			setReset(false);
			setReady(true);
			clockCycles = 7;
			status = status & 0b1100_0011 | 0b00110000;
			mapper.onVectorPull(RES_VECTOR);
			int vector = to8b(mapper.read(RES_VECTOR, false));
			vector |= toHI(mapper.read(RES_VECTOR + 1, false));
			pc = vector;
			return;
		}
		if (interruptRequest) {
			if (debug)
				System.out.printf("Starting interruptRequest sequence!\n");
			setInterruptRequest(false);
			if (!getInterrupt()) {
				// incPC();
				push(getHItoLO(pc));
				push(to8b(pc));
				push(status & ~FLAG_B);
				setInterrupt(true);
				mapper.onVectorPull(IRQ_VECTOR);
				int vector = to8b(mapper.read(IRQ_VECTOR, false));
				vector |= toHI(mapper.read(IRQ_VECTOR + 1, false));
				pc = vector;
				return;
			}
		}
		else if (nonMaskableInterrupt) {
			if (debug)
				System.out.printf("Starting nonMaskableInterrupt sequence!\n");
			setNonMaskableInterrupt(false);
			incPC();
			push(getHItoLO(pc));
			push(to8b(pc));
			push(status & ~FLAG_B);
			mapper.onVectorPull(NMI_VECTOR);
			int vector = to8b(mapper.read(IRQ_VECTOR, false));
			vector |= toHI(mapper.read(IRQ_VECTOR + 1, false));
			pc = vector;
			return;
		}

		if (debug)
			System.out.printf("Fetching new instruction!\n");
		penaltyOP = false;
		penaltyADDR = false;

		opcode = to8b(mapper.read(pc, true));
		incPC();
		if (debug)
			System.out.printf("OPcode = 0x%02X\n", opcode);

		// modes[opcode].getAddress(this);
		AddressingMode a = modes[opcode];
		a.calcAddress(this);
		codes[opcode].execute(this, modes[opcode]);

		clockCycles = delays[opcode];

		if (penaltyADDR && penaltyOP) clockCycles++;

		if (debug)
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

	// Print as binary
	private static String asBinary(int value, int bits) {
		String result = "";
		int mask = 1 << bits;
		for (int i = 0; i < bits; i++) {
			value <<= 1;
			result += ((value & mask) != 0)? "1": "0";
			if ((i & 3) == 3) result += " ";
		}
		return result;
	}

	// OP codes and addressing modes
	private static final AddressingMode[] modes = {
		new Impl(), new IndX(), new Impl(), new Impl(), new Zrpg(), new Zrpg(), new Zrpg(), new Zrpg(), new Impl(), new Imme(), new Accu(), new Impl(), new Abso(), new Abso(), new Abso(), new ZRel(),
		new Rltv(), new IndY(), new ZpIn(), new Impl(), new Zrpg(), new ZrpX(), new ZrpX(), new Zrpg(), new Impl(), new AbsY(), new Accu(), new Impl(), new Abso(), new AbsX(), new AbsX(), new ZRel(),
		new Abso(), new IndX(), new Impl(), new Impl(), new Zrpg(), new Zrpg(), new Zrpg(), new Zrpg(), new Impl(), new Imme(), new Accu(), new Impl(), new Abso(), new Abso(), new Abso(), new ZRel(),
		new Rltv(), new IndY(), new ZpIn(), new Impl(), new ZrpX(), new ZrpX(), new ZrpX(), new Zrpg(), new Impl(), new AbsY(), new Accu(), new Impl(), new AbsX(), new AbsX(), new AbsX(), new ZRel(),
		new Impl(), new IndX(), new Impl(), new Impl(), new Impl(), new Zrpg(), new Zrpg(), new Zrpg(), new Impl(), new Imme(), new Accu(), new Impl(), new Abso(), new Abso(), new Abso(), new ZRel(),
		new Rltv(), new IndY(), new ZpIn(), new Impl(), new Impl(), new ZrpX(), new ZrpX(), new Zrpg(), new Impl(), new AbsY(), new Impl(), new Impl(), new Impl(), new AbsX(), new AbsX(), new ZRel(),
		new Impl(), new IndX(), new Impl(), new Impl(), new Zrpg(), new Zrpg(), new Zrpg(), new Zrpg(), new Impl(), new Imme(), new Accu(), new Impl(), new Indr(), new Abso(), new Abso(), new ZRel(),
		new Rltv(), new IndY(), new ZpIn(), new Impl(), new ZrpX(), new ZrpX(), new ZrpX(), new Zrpg(), new Impl(), new AbsY(), new Impl(), new Impl(), new AbIX(), new AbsX(), new AbsX(), new ZRel(),
		new Rltv(), new IndX(), new Impl(), new Impl(), new Zrpg(), new Zrpg(), new Zrpg(), new Zrpg(), new Impl(), new Imme(), new Impl(), new Impl(), new Abso(), new Abso(), new Abso(), new ZRel(),
		new Rltv(), new IndY(), new ZpIn(), new Impl(), new ZrpX(), new ZrpX(), new ZrpY(), new Zrpg(), new Impl(), new AbsY(), new Impl(), new Impl(), new Abso(), new AbsX(), new AbsX(), new ZRel(),
		new Imme(), new IndX(), new Imme(), new Impl(), new Zrpg(), new Zrpg(), new Zrpg(), new Zrpg(), new Impl(), new Imme(), new Impl(), new Impl(), new Abso(), new Abso(), new Abso(), new ZRel(),
		new Rltv(), new IndY(), new ZpIn(), new Impl(), new ZrpX(), new ZrpX(), new ZrpY(), new Zrpg(), new Impl(), new AbsY(), new Impl(), new Impl(), new AbsX(), new AbsX(), new AbsY(), new ZRel(),
		new Imme(), new IndX(), new Impl(), new Impl(), new Zrpg(), new Zrpg(), new Zrpg(), new Zrpg(), new Impl(), new Imme(), new Impl(), new Impl(), new Abso(), new Abso(), new Abso(), new ZRel(),
		new Rltv(), new IndY(), new ZpIn(), new Impl(), new Impl(), new ZrpX(), new ZrpX(), new Zrpg(), new Impl(), new AbsY(), new Impl(), new Impl(), new Impl(), new AbsX(), new AbsX(), new ZRel(),
		new Imme(), new IndX(), new Impl(), new Impl(), new Zrpg(), new Zrpg(), new Zrpg(), new Zrpg(), new Impl(), new Imme(), new Impl(), new Impl(), new Abso(), new Abso(), new Abso(), new ZRel(),
		new Rltv(), new IndY(), new ZpIn(), new Impl(), new Impl(), new ZrpX(), new ZrpX(), new Zrpg(), new Impl(), new AbsY(), new Impl(), new Impl(), new Impl(), new AbsX(), new AbsX(), new ZRel(),
	};

	private static final OPcode[] codes = {
		new BRK(),  new ORA(),  new NOP(),  new NOP(),  new TSB(),  new ORA(),  new ASL(),  new RMB0(),  new PHP(),  new ORA(),  new ASL(),  new NOP(),  new TSB(),  new ORA(),  new ASL(),  new BBR0(),
		new BPL(),  new ORA(),  new ORA(),  new NOP(),  new TRB(),  new ORA(),  new ASL(),  new RMB1(),  new CLC(),  new ORA(),  new INC(),  new NOP(),  new TRB(),  new ORA(),  new ASL(),  new BBR1(),
		new JSR(),  new AND(),  new NOP(),  new NOP(),  new BIT(),  new AND(),  new ROL(),  new RMB2(),  new PLP(),  new AND(),  new ROL(),  new NOP(),  new BIT(),  new AND(),  new ROL(),  new BBR2(),
		new BMI(),  new AND(),  new AND(),  new NOP(),  new BIT(),  new AND(),  new ROL(),  new RMB3(),  new SEC(),  new AND(),  new DEC(),  new NOP(),  new BIT(),  new AND(),  new ROL(),  new BBR3(),
		new RTI(),  new EOR(),  new NOP(),  new NOP(),  new NOP(),  new EOR(),  new LSR(),  new RMB4(),  new PHA(),  new EOR(),  new LSR(),  new NOP(),  new JMP(),  new EOR(),  new LSR(),  new BBR4(),
		new BVC(),  new EOR(),  new EOR(),  new NOP(),  new NOP(),  new EOR(),  new LSR(),  new RMB5(),  new CLI(),  new EOR(),  new PHY(),  new NOP(),  new NOP(),  new EOR(),  new LSR(),  new BBR5(),
		new RTS(),  new ADC(),  new NOP(),  new NOP(),  new STZ(),  new ADC(),  new ROR(),  new RMB6(),  new PLA(),  new ADC(),  new ROR(),  new NOP(),  new JMP(),  new ADC(),  new ROR(),  new BBR6(),
		new BVS(),  new ADC(),  new ADC(),  new NOP(),  new STZ(),  new ADC(),  new ROR(),  new RMB7(),  new SEI(),  new ADC(),  new PLY(),  new NOP(),  new JMP(),  new ADC(),  new ROR(),  new BBR7(),
		new BRA(),  new STA(),  new NOP(),  new NOP(),  new STY(),  new STA(),  new STX(),  new SMB0(),  new DEY(),  new BIT(),  new TXA(),  new NOP(),  new STY(),  new STA(),  new STX(),  new BBS0(),
		new BCC(),  new STA(),  new STA(),  new NOP(),  new STY(),  new STA(),  new STX(),  new SMB1(),  new TYA(),  new STA(),  new TXS(),  new NOP(),  new STZ(),  new STA(),  new STZ(),  new BBS1(),
		new LDY(),  new LDA(),  new LDX(),  new NOP(),  new LDY(),  new LDA(),  new LDX(),  new SMB2(),  new TAY(),  new LDA(),  new TAX(),  new NOP(),  new LDY(),  new LDA(),  new LDX(),  new BBS2(),
		new BCS(),  new LDA(),  new LDA(),  new NOP(),  new LDY(),  new LDA(),  new LDX(),  new SMB3(),  new CLV(),  new LDA(),  new TSX(),  new NOP(),  new LDY(),  new LDA(),  new LDX(),  new BBS3(),
		new CPY(),  new CMP(),  new NOP(),  new NOP(),  new CPY(),  new CMP(),  new DEC(),  new SMB4(),  new INY(),  new CMP(),  new DEX(),  new WAI(),  new CPY(),  new CMP(),  new DEC(),  new BBS4(),
		new BNE(),  new CMP(),  new CMP(),  new NOP(),  new NOP(),  new CMP(),  new DEC(),  new SMB5(),  new CLD(),  new CMP(),  new PHX(),  new STP(),  new NOP(),  new CMP(),  new DEC(),  new BBS5(),
		new CPX(),  new SBC(),  new NOP(),  new NOP(),  new CPX(),  new SBC(),  new INC(),  new SMB6(),  new INX(),  new SBC(),  new NOP(),  new NOP(),  new CPX(),  new SBC(),  new INC(),  new BBS6(),
		new BEQ(),  new SBC(),  new SBC(),  new NOP(),  new NOP(),  new SBC(),  new INC(),  new SMB7(),  new SED(),  new SBC(),  new PLX(),  new NOP(),  new NOP(),  new SBC(),  new INC(),  new BBS7(),
	};

	/*

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
	*/


	private static final int[] delays = {
		7, 6, 2, 1, 5, 3, 5, 5, 3, 2, 2, 1, 6, 4, 6, 5, 
		2, 5, 5, 1, 5, 4, 6, 5, 2, 4, 2, 1, 6, 4, 6, 5, 
		6, 6, 2, 1, 3, 3, 5, 5, 4, 2, 2, 1, 4, 4, 6, 5, 
		2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 2, 1, 4, 4, 6, 5, 
		6, 6, 2, 1, 3, 3, 5, 5, 3, 2, 2, 1, 3, 4, 6, 5, 
		2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 3, 1, 8, 4, 6, 5, 
		6, 6, 2, 1, 3, 3, 5, 5, 4, 2, 2, 1, 6, 4, 6, 5, 
		2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 4, 1, 6, 4, 6, 5, 
		3, 6, 2, 1, 3, 3, 3, 5, 2, 2, 2, 1, 4, 4, 4, 5, 
		2, 6, 5, 1, 4, 4, 4, 5, 2, 5, 2, 1, 4, 5, 5, 5, 
		2, 6, 2, 1, 3, 3, 3, 5, 2, 2, 2, 1, 4, 4, 4, 5, 
		2, 5, 5, 1, 4, 4, 4, 5, 2, 4, 2, 1, 4, 4, 4, 5, 
		2, 6, 2, 1, 3, 3, 5, 5, 2, 2, 2, 3, 4, 4, 6, 5, 
		2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 3, 3, 4, 4, 7, 5, 
		2, 6, 2, 1, 3, 3, 5, 5, 2, 2, 2, 1, 4, 4, 6, 5, 
		2, 5, 5, 1, 4, 4, 6, 5, 2, 4, 4, 1, 4, 4, 7, 5, 
	};
}

/*

		OLD

		// 7, 6, 2, 1, 5, 3, 5, 5, 3, 2, 2, 1, 6, 4, 6, 5,  
		// 2, 5, 5, 1, 4, 4, 6, 6, 2, 4, 2, 1, 4, 4, 7, 7,  
		// 6, 6, 2, 1, 3, 3, 5, 5, 4, 2, 2, 1, 4, 4, 6, 6,  
		// 2, 5, 5, 1, 4, 4, 6, 6, 2, 4, 2, 1, 4, 4, 7, 7,  
		// 6, 6, 2, 1, 3, 3, 5, 5, 3, 2, 2, 1, 3, 4, 6, 6,  
		// 2, 5, 5, 1, 4, 4, 6, 6, 2, 4, 2, 1, 8, 4, 7, 7,  
		// 6, 6, 2, 1, 3, 3, 5, 5, 4, 2, 2, 1, 5, 4, 6, 6,  
		// 2, 5, 5, 1, 4, 4, 6, 6, 2, 4, 2, 1, 4, 4, 7, 7,  
		// 2, 6, 2, 1, 3, 3, 3, 3, 2, 2, 2, 1, 4, 4, 4, 4,  
		// 2, 6, 5, 1, 4, 4, 4, 4, 2, 5, 2, 1, 5, 5, 5, 5,  
		// 2, 6, 2, 1, 3, 3, 3, 3, 2, 2, 2, 1, 4, 4, 4, 4,  
		// 2, 5, 5, 1, 4, 4, 4, 4, 2, 4, 2, 1, 4, 4, 4, 4,  
		// 2, 6, 2, 1, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,  
		// 2, 5, 5, 1, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,  
		// 2, 6, 2, 1, 3, 3, 5, 5, 2, 2, 2, 1, 4, 4, 6, 6,  
		// 2, 5, 5, 1, 4, 4, 6, 6, 2, 4, 2, 1, 4, 4, 7, 7,
		7, 6, 2, 1, 5, 3, 5, 5, 3, 2, 2, 1, 6, 4, 6, 5, 

		{
			var a = document.getElementsByClassName("heyhey");
			var output = "";
			for (var i = 1; i < 16; i++) {
				for (var j = 1; j < 16; j++) {
					var char =  (
			    		a[0].children[i].children[j]
						.children[0].children[0]
						.innerText.slice(-1)
					);
					if (char === "*") {
			                char =  (
			                a[0].children[i].children[j]
			                .children[0].children[0]
			                .innerText.slice(-2, -1)
			            );
					}
					output += char + ", ";
			    }
			}
			console.log(output);
		}

		
		BRK,  ORA,  NOP,  NOP,  TSB,  ORA,  ASL,  RMB0,  PHP,  ORA,  ASL,  NOP,  TSB,  ORA,  ASL,  BBR0,
		    , (dX),     , (dX),   d ,   d ,   d ,   d ,     ,   # ,   A ,   # ,   a ,   a ,   a , ZRel,
		
		BPL,  ORA,  ORA,  NOP,  TRB,  ORA,  ASL,  RMB1,  CLC,  ORA,  INC,  NOP,  TRB,  ORA,  ASL,  BBR1,
		  r , (d)Y,  (d), (d)Y,   d ,  d,X,  d,X,   d ,     ,  a,Y,   A ,  a,Y,   a ,  a,X,  a,X, ZRel,
		
		JSR,  AND,  NOP,  NOP,  BIT,  AND,  ROL,  RMB2,  PLP,  AND,  ROL,  NOP,  BIT,  AND,  ROL,  BBR2,
		  a , (dX),     , (dX),   d ,   d ,   d ,   d ,     ,   # ,   A ,   # ,   a ,   a ,   a , ZRel,
		
		BMI,  AND,  AND,  NOP,  BIT,  AND,  ROL,  RMB3,  SEC,  AND,  DEC,  NOP,  BIT,  AND,  ROL,  BBR3,
		  r , (d)Y,  (d), (d)Y,  d,X,  d,X,  d,X,   d ,     ,  a,Y,   A ,  a,Y,  a,X,  a,X,  a,X, ZRel,
		
		RTI,  EOR,  NOP,  NOP,  NOP,  EOR,  LSR,  RMB4,  PHA,  EOR,  LSR,  NOP,  JMP,  EOR,  LSR,  BBR4,
		    , (dX),     , (dX),  (d),   d ,   d ,   d ,     ,   # ,   A ,   # ,   a ,   a ,   a , ZRel,
		
		BVC,  EOR,  EOR,  NOP,  NOP,  EOR,  LSR,  RMB5,  CLI,  EOR,  PHY,  NOP,  NOP,  EOR,  LSR,  BBR5,
		  r , (d)Y,  (d), (d)Y,  d,X,  d,X,  d,X,   d ,     ,  a,Y,     ,  a,Y,  a,X,  a,X,  a,X, ZRel,
		
		RTS,  ADC,  NOP,  NOP,  STZ,  ADC,  ROR,  RMB6,  PLA,  ADC,  ROR,  NOP,  JMP,  ADC,  ROR,  BBR6,
		    , (dX),     , (dX),   d ,   d ,   d ,   d ,     ,   # ,   A ,   # ,  (a),   a ,   a , ZRel,
		
		BVS,  ADC,  ADC,  NOP,  STZ,  ADC,  ROR,  RMB7,  SEI,  ADC,  PLY,  NOP,  JMP,  ADC,  ROR,  BBR7,
		  r , (d)Y,  (d), (d)Y,  d,X,  d,X,  d,X,   d ,     ,  a,Y,     ,  a,Y,  a,X,  a,X,  a,X, ZRel,
		
		BRA,  STA,  NOP,  NOP,  STY,  STA,  STX,  SMB0,  DEY,  BIT,  TXA,  NOP,  STY,  STA,  STX,  BBS0,
		  r , (dX),   # , (dX),   d ,   d ,   d ,   d ,     ,   # ,     ,   # ,   a ,   a ,   a , ZRel,
		
		BCC,  STA,  STA,  NOP,  STY,  STA,  STX,  SMB1,  TYA,  STA,  TXS,  NOP,  STZ,  STA,  STZ,  BBS1,
		  r , (d)Y,  (d), (d)Y,  d,X,  d,X,  d,Y,   d ,     ,  a,Y,     ,  a,Y,   a ,  a,X,  a,X, ZRel,
		
		LDY,  LDA,  LDX,  NOP,  LDY,  LDA,  LDX,  SMB2,  TAY,  LDA,  TAX,  NOP,  LDY,  LDA,  LDX,  BBS2,
		  # , (dX),   # , (dX),   d ,   d ,   d ,   d ,     ,   # ,     ,   # ,   a ,   a ,   a , ZRel,
		
		BCS,  LDA,  LDA,  NOP,  LDY,  LDA,  LDX,  SMB3,  CLV,  LDA,  TSX,  NOP,  LDY,  LDA,  LDX,  BBS3,
		  r , (d)Y,  (d), (d)Y,  d,X,  d,X,  d,Y,   d ,     ,  a,Y,     ,  a,Y,  a,X,  a,X,  a,Y, ZRel,
		
		CPY,  CMP,  NOP,  NOP,  CPY,  CMP,  DEC,  SMB4,  INY,  CMP,  DEX,  WAI,  CPY,  CMP,  DEC,  BBS4,
		  # , (dX),   # , (dX),   d ,   d ,   d ,   d ,     ,   # ,     ,     ,   a ,   a ,   a , ZRel,
		
		BNE,  CMP,  CMP,  NOP,  NOP,  CMP,  DEC,  SMB5,  CLD,  CMP,  PHX,  STP,  NOP,  CMP,  DEC,  BBS5,
		  r , (d)Y,  (d), (d)Y,  d,X,  d,X,  d,X,   d ,     ,  a,Y,     ,     ,  a,X,  a,X,  a,X, ZRel,
		
		CPX,  SBC,  NOP,  NOP,  CPX,  SBC,  INC,  SMB6,  INX,  SBC,  NOP,  SBC,  CPX,  SBC,  INC,  BBS6,
		  # , (dX),   # , (dX),   d ,   d ,   d ,   d ,     ,   # ,     ,   # ,   a ,   a ,   a , ZRel,
		
		BEQ,  SBC,  SBC,  NOP,  NOP,  SBC,  INC,  SMB7,  SED,  SBC,  PLX,  NOP,  NOP,  SBC,  INC,  BBS7,
		  r , (d)Y,  (d), (d)Y,  d,X,  d,X,  d,X,   d ,     ,  a,Y,     ,  a,Y,  a,X,  a,X,  a,X, ZRel,



BRK b	ORA (d,X)	cop b	ora d,S		Tsb d	ORA d	ASL d	ora [d]		PHP	ORA #	ASL A	phd		Tsb a		ORA a	ASL a	ora al
BPL r	ORA (d),Y	Ora (d)	ora (d,S),Y	Trb d	ORA d,X	ASL d,X	ora [d],Y	CLC	ORA a,Y	Inc A	tcs		Trb a		ORA a,X	ASL a,X	ora al,X
JSR a	AND (d,X)	jsl al	and d,S		BIT d	AND d	ROL d	and [d]		PLP	AND #	ROL A	pld		BIT a		AND a	ROL a	and al
BMI r	AND (d),Y	And (d)	and (d,S),Y	Bit d,X	AND d,X	ROL d,X	and [d],Y	SEC	AND a,Y	Dec A	tsc		Bit a,X		AND a,X	ROL a,X	and al,X
RTI		EOR (d,X)	wdm		eor d,S		mvp s,d	EOR d	LSR d	eor [d]		PHA	EOR #	LSR A	phk		JMP a		EOR a	LSR a	eor al
BVC r	EOR (d),Y	Eor (d)	eor (d,S),Y	mvn s,d	EOR d,X	LSR d,X	eor [d],Y	CLI	EOR a,Y	Phy		tcd		jmp al		EOR a,X	LSR a,X	eor al,X
RTS		ADC (d,X)	per rl	adc d,S		Stz d	ADC d	ROR d	adc [d]		PLA	ADC #	ROR A	rtl		JMP (a)		ADC a	ROR a	adc al
BVS r	ADC (d),Y	Adc (d)	adc (d,S),Y	Stz d,X	ADC d,X	ROR d,X	adc [d],Y	SEI	ADC a,Y	Ply		tdc		Jmp (a,X)	ADC a,X	ROR a,X	adc al,X
Bra r	STA (d,X)	brl rl	sta d,S		STY d	STA d	STX d	sta [d]		DEY	Bit #	TXA		phb		STY a		STA a	STX a	sta al
BCC r	STA (d),Y	Sta (d)	sta (d,S),Y	STY d,X	STA d,X	STX d,Y	sta [d],Y	TYA	STA a,Y	TXS		txy		Stz a		STA a,X	Stz a,X	sta al,X
LDY #	LDA (d,X)	LDX #	lda d,S		LDY d	LDA d	LDX d	lda [d]		TAY	LDA #	TAX		plb		LDY a		LDA a	LDX a	lda al
BCS r	LDA (d),Y	Lda (d)	lda (d,S),Y	LDY d,X	LDA d,X	LDX d,Y	lda [d],Y	CLV	LDA a,Y	TSX		tyx		LDY a,X		LDA a,X	LDX a,Y	lda al,X
CPY #	CMP (d,X)	rep #	cmp d,S		CPY d	CMP d	DEC d	cmp [d]		INY	CMP #	DEX		wai		CPY a		CMP a	DEC a	cmp al
BNE r	CMP (d),Y	Cmp (d)	cmp (d,S),Y	pei d	CMP d,X	DEC d,X	cmp [d],Y	CLD	CMP a,Y	Phx		stp		jml (a)		CMP a,X	DEC a,X	cmp al,X
CPX #	SBC (d,X)	sep #	sbc d,S		CPX d	SBC d	INC d	sbc [d]		INX	SBC #	NOP		xba		CPX a		SBC a	INC a	sbc al
BEQ r	SBC (d),Y	Sbc (d)	sbc (d,S),Y	pea a	SBC d,X	INC d,X	sbc [d],Y	SED	SBC a,Y	Plx		xce		jsr (a,X)	SBC a,X	INC a,X	sbc al,X






		"Impl", "IndX", "Impl", "IndX", "Zrpg", "Zrpg", "Zrpg", "Zrpg", "Impl", "Imme", "Accu", "Imme", "Abso", "Abso", "Abso", "ZRel",
		"Rltv", "IndY", "ZpIn", "IndY", "Zrpg", "ZrpX", "ZrpX", "Zrpg", "Impl", "AbsY", "Accu", "AbsY", "Abso", "AbsX", "AbsX", "ZRel",
		"Abso", "IndX", "Impl", "IndX", "Zrpg", "Zrpg", "Zrpg", "Zrpg", "Impl", "Imme", "Accu", "Imme", "Abso", "Abso", "Abso", "ZRel",
		"Rltv", "IndY", "ZpIn", "IndY", "ZrpX", "ZrpX", "ZrpX", "Zrpg", "Impl", "AbsY", "Accu", "AbsY", "AbsX", "AbsX", "AbsX", "ZRel",
		"Impl", "IndX", "Impl", "IndX", "Indr", "Zrpg", "Zrpg", "Zrpg", "Impl", "Imme", "Accu", "Imme", "Abso", "Abso", "Abso", "ZRel",
		"Rltv", "IndY", "ZpIn", "IndY", "ZrpX", "ZrpX", "ZrpX", "Zrpg", "Impl", "AbsY", "Impl", "AbsY", "AbsX", "AbsX", "AbsX", "ZRel",
		"Impl", "IndX", "Impl", "IndX", "Zrpg", "Zrpg", "Zrpg", "Zrpg", "Impl", "Imme", "Accu", "Imme", "Indr", "Abso", "Abso", "ZRel",
		"Rltv", "IndY", "ZpIn", "IndY", "ZrpX", "ZrpX", "ZrpX", "Zrpg", "Impl", "AbsY", "Impl", "AbsY", "AbIX", "AbsX", "AbsX", "ZRel",
		"Rltv", "IndX", "Imme", "IndX", "Zrpg", "Zrpg", "Zrpg", "Zrpg", "Impl", "Imme", "Impl", "Imme", "Abso", "Abso", "Abso", "ZRel",
		"Rltv", "IndY", "ZpIn", "IndY", "ZrpX", "ZrpX", "ZrpY", "Zrpg", "Impl", "AbsY", "Impl", "AbsY", "Abso", "AbsX", "AbsX", "ZRel",
		"Imme", "IndX", "Imme", "IndX", "Zrpg", "Zrpg", "Zrpg", "Zrpg", "Impl", "Imme", "Impl", "Imme", "Abso", "Abso", "Abso", "ZRel",
		"Rltv", "IndY", "ZpIn", "IndY", "ZrpX", "ZrpX", "ZrpY", "Zrpg", "Impl", "AbsY", "Impl", "AbsY", "AbsX", "AbsX", "AbsY", "ZRel",
		"Imme", "IndX", "Imme", "IndX", "Zrpg", "Zrpg", "Zrpg", "Zrpg", "Impl", "Imme", "Impl", "Impl", "Abso", "Abso", "Abso", "ZRel",
		"Rltv", "IndY", "ZpIn", "IndY", "ZrpX", "ZrpX", "ZrpX", "Zrpg", "Impl", "AbsY", "Impl", "Impl", "AbsX", "AbsX", "AbsX", "ZRel",
		"Imme", "IndX", "Imme", "IndX", "Zrpg", "Zrpg", "Zrpg", "Zrpg", "Impl", "Imme", "Impl", "Imme", "Abso", "Abso", "Abso", "ZRel",
		"Rltv", "IndY", "ZpIn", "IndY", "ZrpX", "ZrpX", "ZrpX", "Zrpg", "Impl", "AbsY", "Impl", "AbsY", "AbsX", "AbsX", "AbsX", "ZRel",


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





*/