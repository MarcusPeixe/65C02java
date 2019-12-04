package sample;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Token {
    enum Type {
        DIRECTIVE, VALUE, COMMENT, LABEL, INSTRUCTION, ACC, IMM, REL, ABS, ABSX, ABSY, IND, INDX, INDY
    }
    
    String value;
    Type type;
    
    public Token() {}
    public Token (Type type, String value) {
        this.type = type;
        this.value = value;
    }
}

class ValueToken extends Token {
    public ValueToken (Type type, String value) {
        this.type = type;
        this.value = null;
        Matcher matcher;
        switch (type) {
            case IMM:
                matcher = Pattern.compile("").matcher(value);
                if (!matcher.find()) break;
                this.value = matcher.group();
                break;
            case REL:
                matcher = Pattern.compile("\\*[+-](\\w+)").matcher(value);
                if (!matcher.find()) break;
                this.value = matcher.group(1);
                break;
            case ABS:
                matcher = Pattern.compile("").matcher(value);
                if (!matcher.find()) break;
                this.value = matcher.group();
                break;
            case ABSX:
                matcher = Pattern.compile("").matcher(value);
                if (!matcher.find()) break;
                this.value = matcher.group();
                break;
            case ABSY:
                matcher = Pattern.compile("").matcher(value);
                if (!matcher.find()) break;
                this.value = matcher.group();
                break;
            case IND:
                matcher = Pattern.compile("\\((\\w+)\\)").matcher(value);
                if (!matcher.find()) break;
                this.value = matcher.group(1);
                break;
            case INDX:
                matcher = Pattern.compile("").matcher(value);
                if (!matcher.find()) break;
                this.value = matcher.group();
                break;
            case INDY:
                matcher = Pattern.compile("").matcher(value);
                if (!matcher.find()) break;
                this.value = matcher.group();
                break;
            default: break;
        }
//        this.value = value;
    }
}

public class Assembler {
    public static void main(String[] args) {
        StringBuilder s = new StringBuilder();
        try {
            FileReader f = new FileReader("/home/alunocoltec/IdeaProjects/Test3/src/sample/test0Asm.txt");
            while (true) {
                int c = f.read();
                if (c == -1) break;
                s.append((char) c);
            }
            System.out.printf("File contents: {\n%s\n}\n", s);
        } catch (FileNotFoundException e) {
            System.out.println("oh shit bro, wrong file name.");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("oh shit bro, couldn't read from file.");
            e.printStackTrace();
        }
        List<Token> tokens = new ArrayList<>();
        Pattern tokenPattern = Pattern.compile("\\s*(;.*|[^\\s]+?)(?=\\s)");
        Matcher tokeniser = tokenPattern.matcher(s);
        while (tokeniser.find()) {
            String token = tokeniser.group(1);
//            System.out.printf("Match \"%s\" at %d. ", token, tokeniser.start());
            if (Pattern.matches(";.*", token)) {
//                System.out.print("(comment)\n");
                tokens.add(new Token(Token.Type.COMMENT, token));
                System.out.printf("\033[0;90m%s", tokeniser.group());
            }
            else if (Pattern.matches("[a-zA-Z_]\\w+:", token)) {
//                System.out.print("(label)\n");
                tokens.add(new Token(Token.Type.LABEL, token));
                System.out.printf("\033[0;1;96m%s", tokeniser.group());
            }
            else if (Pattern.matches("\\.\\w+", token)) {
//                System.out.print("(assembler directive)\n");
                tokens.add(new Token(Token.Type.DIRECTIVE, token));
                System.out.printf("\033[0;1;31m%s", tokeniser.group());
            }
            else if (Pattern.matches("A", token)) {
//                System.out.print("(accumulator)\n");
                tokens.add(new Token(Token.Type.ACC, token));
                System.out.printf("\033[0;34m%s", tokeniser.group());
            }
            else if (Pattern.matches("\\*[+-]\\w+", token)) {
//                System.out.print("(relative)\n");
                tokens.add(new Token(Token.Type.REL, token));
                System.out.printf("\033[0;34m%s", tokeniser.group());
            }
            else if (Pattern.matches("\\(\\w+\\)", token)) {
//                System.out.print("(indirect)\n");
                tokens.add(new Token(Token.Type.IND, token));
                System.out.printf("\033[0;34m%s", tokeniser.group());
            }
            else if (Pattern.matches("\\(\\w+,[xX]\\)", token)) {
//                System.out.print("(indirect X)\n");
                tokens.add(new Token(Token.Type.INDX, token));
                System.out.printf("\033[0;34m%s", tokeniser.group());
            }
            else if (Pattern.matches("\\(\\w+,[yY]\\)", token)) {
//                System.out.print("(indirect Y)\n");
                tokens.add(new Token(Token.Type.INDY, token));
                System.out.printf("\033[0;34m%s", tokeniser.group());
            }
            else if (Pattern.matches("\\(?\\w+(,X|,Y|,x|,y)\\)?", token)) {
//                System.out.print("(name + addressing mode)\n");
                tokens.add(new Token(Token.Type.VALUE, token));
                System.out.printf("\033[0;34m%s", tokeniser.group());
            }
            else if (Pattern.matches("#?(\\$\\w+|\\d+|b[01]+)", token)) {
//                System.out.print("(literal value)\n");
                System.out.printf("\033[0;35m%s", tokeniser.group());
            }
            else if (Pattern.matches("[a-zA-Z]{3}\\d?", token)) {
//                System.out.print("(instruction)\n");
                tokens.add(new Token(Token.Type.INSTRUCTION, token));
                System.out.printf("\033[0;31m%s", tokeniser.group());
            }
            else if (Pattern.matches("[a-zA-Z_]\\w+", token)) {
//                System.out.print("(name)\n");
                tokens.add(new Token(Token.Type.VALUE, token));
                System.out.printf("\033[0;1;92m%s", tokeniser.group());
            }
            else {
//                System.out.print("(unknown)\n");
                System.out.printf("\033[0;97;41m%s", tokeniser.group());
            }
        }
    }
}
