interface Mapper {

	public int read(int addr);
	public void write(int addr, int value);

	public void onVectorPull(int addr);

}