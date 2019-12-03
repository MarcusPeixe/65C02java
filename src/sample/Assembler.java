package sample;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            System.out.printf("File contents: {\n%s\n}\n", s);
        } catch (FileNotFoundException e) {
            System.out.println("oh shit bro, wrong file name.");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("oh shit bro, couldn't read from file.");
            e.printStackTrace();
        }
        Pattern tokenPattern = Pattern.compile("(?:\\s*)(;.*|[^\\s]+)");
//        Pattern directive = Pattern.compile("\\.\\w+");
//        Pattern number = Pattern.compile("\\$\\w+|\\d+");
//        Pattern symbol = Pattern.compile("[#,()]");
//        Pattern label = Pattern.compile("\\b\\w+:\\b");
//        Pattern comment = Pattern.compile(";.*");
        Matcher tokeniser = tokenPattern.matcher(s);
        while (tokeniser.find()) {
            String token = tokeniser.group(1);
//            System.out.printf("Match \"%s\" at %d. ", token, tokeniser.start());
            if (Pattern.matches(";.*", token)) {
//                System.out.print("(comment)\n");
                System.out.printf("\033[0;90m%s", tokeniser.group());
            }
            else if (Pattern.matches("\\w+:", token)) {
//                System.out.print("(label)\n");
                System.out.printf("\033[1;36m%s", tokeniser.group());
            }
            else if (Pattern.matches("\\.\\w+", token)) {
//                System.out.print("(assembler directive)\n");
                System.out.printf("\033[1;35m%s", tokeniser.group());
            }
            else if (Pattern.matches("[,()]", token)) {
//                System.out.print("(punctuation)\n");
                System.out.printf("\033[0;37m%s", tokeniser.group());
            }
            else if (Pattern.matches("\\w+(,X|,Y|,x|,y)", token)) {
//                System.out.print("(name + addressing mode)\n");
                System.out.printf("\033[0;33m%s", tokeniser.group());
            }
            else if (Pattern.matches("#?(\\$\\w+|\\d+)", token)) {
//                System.out.print("(literal value)\n");
                System.out.printf("\033[0;33m%s", tokeniser.group());
            }
            else if (Pattern.matches("\\w+", token)) {
//                System.out.print("(name)\n");
                System.out.printf("\033[0;31m%s", tokeniser.group());
            }
            else {
//                System.out.print("(unknown)\n");
                System.out.printf("\033[1;41m%s", tokeniser.group());
            }
        }
    }
}
