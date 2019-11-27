package sample;

import java.io.*;

public class Assembler {
    public static void main(String[] args) {
        StringBuilder s = new StringBuilder();
        try {
            FileReader f = new FileReader("/home/alunocoltec/IdeaProjects/Test3/src/sample/asmtest.txt");
            while (true) {
                int c = f.read();
                if (c == -1) break;
                s.append((char)c);
            }
            System.out.printf("File contents: {\n%s\n}", s);
        } catch (FileNotFoundException e) {
            System.out.println("oh shit bro, wrong file name");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("oh shit bro, couldn't read");
            e.printStackTrace();
        }
        String token = "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

        }
    }
}
