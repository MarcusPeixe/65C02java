package sample;

public interface Mapper {

    public int read(int addr, boolean sync);
    public void write(int addr, int value);

    public void onVectorPull(int addr);

}