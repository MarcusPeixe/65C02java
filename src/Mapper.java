public interface Mapper {

    int read(int addr, boolean sync);
    void write(int addr, int value);

    void onVectorPull(int addr);

}