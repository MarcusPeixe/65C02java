package sample;

public class Ram implements Mapper {
    int[] ram;

    Ram() {
        ram = new int[0x10000];
        for (int i = 0; i < 0x10000; i++) {
            ram[i] = 0;
        }
    }

    @Override
    public int read(int addr, boolean sync) {
        return ram[addr] & 0xFF;
    }

    @Override
    public void write(int addr, int value) {
        ram[addr] = (byte)(value & 0xFF);
    }

    @Override
    public void onVectorPull(int addr) {

    }
}
