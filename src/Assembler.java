import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Token {
    String value;
    int offset;
    final static Token EOF = new Token("", 0);

    final static HashSet<String> opcodes = Stream.of(
            "ADC",
            "AND",
            "ASL",
            "BBR0",
            "BBR1",
            "BBR2",
            "BBR3",
            "BBR4",
            "BBR5",
            "BBR6",
            "BBR7",
            "BBS0",
            "BBS1",
            "BBS2",
            "BBS3",
            "BBS4",
            "BBS5",
            "BBS6",
            "BBS7",
            "BCC",
            "BCS",
            "BEQ",
            "BIT",
            "BMI",
            "BNE",
            "BPL",
            "BRA",
            "BRK",
            "BVC",
            "BVS",
            "CLC",
            "CLD",
            "CLI",
            "CLV",
            "CMP",
            "CPX",
            "CPY",
            "DEC",
            "DEX",
            "DEY",
            "EOR",
            "INC",
            "INX",
            "INY",
            "JMP",
            "JSR",
            "LDA",
            "LDX",
            "LDY",
            "LSR",
            "NOP",
            "ORA",
            "PHA",
            "PHP",
            "PHX",
            "PHY",
            "PLA",
            "PLP",
            "PLX",
            "PLY",
            "RMB0",
            "RMB1",
            "RMB2",
            "RMB3",
            "RMB4",
            "RMB5",
            "RMB6",
            "RMB7",
            "ROL",
            "ROR",
            "RTI",
            "RTS",
            "SBC",
            "SEC",
            "SED",
            "SEI",
            "SMB0",
            "SMB1",
            "SMB2",
            "SMB3",
            "SMB4",
            "SMB5",
            "SMB6",
            "SMB7",
            "STA",
            "STP",
            "STX",
            "STY",
            "STZ",
            "TAX",
            "TAY",
            "TRB",
            "TSB",
            "TSX",
            "TXA",
            "TXS",
            "TYA",
            "WAI"
    ).collect(Collectors.toCollection(HashSet::new));

    final static HashSet<String> directives = Stream.of(
            ".ORG", ".TEXT", ".STRING", ".WORD", ".BYTE", ".DATA"
    ).collect(Collectors.toCollection(HashSet::new));

    public Token (String value, int offset) {
        this.value = value;
        this.offset = offset;
    }

    public static List<Token> tokenise(String s) throws ParseException {
        List<Token> tokens = new ArrayList<>();
//        String completeRegex =
//                "\\s*(;\\*(?:.|\n)*?\\*;|;.*|([\"'])(?:\\\\\"|\\\\'|.)+?\\2|,[xXyY]\\b|%[01_]+\\b|[()+\\-/*|&^#,=<>%]|"
//                        + "\\.?[\\w.]+:|\\.[\\w.]+|\\$[0-9a-fA-F]{1,4}\\b|\\b\\d+\\b|\\b[\\w.]+)";
        String completeRegex =
                "\\s*(;\\*(?:.|\n)*?\\*;|;.*|([\"'])(?:\\\\\"|\\\\'|.)+?\\2|%[01_]+\\b|[()+\\-/*|&^#,=<>%]|"
                        + "\\.?[\\w.]+:|\\.[\\w.]+|\\$[0-9a-fA-F]{1,4}\\b|\\b\\d+\\b|\\b[\\w.]+)";
        Pattern tokenPattern = Pattern.compile(completeRegex);
        Matcher tokeniser = tokenPattern.matcher(s);
        int anchor = 0;
        while (tokeniser.find()) {
            if (tokeniser.start() > anchor) {
                System.out.println(tokeniser.group());
                throw new ParseException("Garbage found", anchor);
            }
            anchor = tokeniser.end();
            String token = tokeniser.group(1);
            int offset = tokeniser.start(1);
//            System.out.printf("%s\n", token);
            Token t = new Token(token, offset);
//            System.out.println(t);
            if (!t.isComment())
                tokens.add(t);
        }
        tokens.add(EOF);
        return tokens;
    }

    @Override
    public String toString() {
        return value;
    }

    public boolean isComment() {
        return !isEOF() && value.charAt(0) == ';';
    }

    public boolean isStringLiteral() {
        return !isEOF() && (value.charAt(0) == '"' || value.charAt(0) == '\'');
    }

    public String getStringLiteral() {
        String s = value.substring(1, value.length() - 1);
//        String s_backup = s;
        String completeRegex = "\\\\(0[0-7]{0,3}|0x[\\dA-Fa-f]{1,2}|\\d{1,3}|.)";
        Pattern escPattern = Pattern.compile(completeRegex);
        Matcher replacer = escPattern.matcher(s);
        while (replacer.find()) {
            String escape = replacer.group();

//            System.out.println("Found escape sequence at " + replacer.start());
//            System.out.println(s_backup);
//            for (int i = 0; i < replacer.start(); i++) {
//                System.out.print(" ");
//            }
//            System.out.println("^");

            String replacement;
            switch (escape) {
                case "\\n":
                    replacement = "\n";
                    break;
                case "\\r":
                    replacement = "\r";
                    break;
                case "\\t":
                    replacement = "\t";
                    break;
                case "\\e":
                    replacement = "\033";
                    break;
                case "\\'":
                    replacement = "'";
                    break;
                case "\\\"":
                    replacement = "\"";
                    break;
                case "\\\\":
                    replacement = "\\";
                    break;
                default:
                    try {
                        int num = Integer.decode(replacer.group(1));
                        if (num > 255) throw new NumberFormatException();
//                        System.out.println("Num is " + num);
                        replacement = String.valueOf((char) num);
                    }
                    catch (NumberFormatException e) {
                        replacement = "?";
                    }
                    break;
            }
            s = s.replace(escape, replacement);
        }
//        System.out.println("Result string:");
//        System.out.println(s);
        return s;
    }

    public boolean isNumber() {
        if (isEOF()) return false;
        if (value.charAt(0) == '%') {
            for (int i = 1; i < value.length(); i++) {
                if (value.charAt(i) != '_') return true;
            }
            return false;
        }
        return Character.isDigit(value.charAt(0)) || value.charAt(0) == '$';
    }

    public int getNumber() {
        if (value.charAt(0) == '%') {
            int num = 0;
            for (int i = 1; i < value.length(); i++) {
                if (value.charAt(i) == '1') {
                    num = (num << 1) + 1;
                }
                else if (value.charAt(i) == '0') {
                    num = num << 1;
                }
            }
            return num;
        }
        else if (value.charAt(0) == '$') {
            return Integer.parseInt(value.substring(1), 16);
        }
        else {
            return Integer.parseInt(value);
        }
    }

    public int getNumberSize() {
        if (value.charAt(0) == '%') {
            int digits = 0;
            for (int i = 1; i < value.length(); i++) {
                if (value.charAt(i) != '_') {
                    digits++;
                }
                if (digits > 8) return 2;
            }
            return 1;
        }
        else if (value.charAt(0) == '$') {
            return (value.length() > 3)? 2 : 1;
        }
        else {
            return (Integer.parseInt(value) > 255)? 2 : 1;
        }
    }

    public boolean isLabel() {
        return !isEOF() && value.charAt(value.length() - 1) == ':';
    }

    public boolean isLocal() {
        return value.charAt(0) == '.';
    }

    public String getLabel() {
        return value.substring(0, value.length() - 1);
    }

    public boolean isOpcode() {
        return opcodes.contains(value.toUpperCase());
    }

    public boolean isDirective() {
        return directives.contains(value.toUpperCase());
    }

    public boolean isIdentifier() {
        return Pattern.matches("[\\w.]+", value);
    }

    public boolean isValue() {
        return isIdentifier() || isNumber() || isStringLiteral();
    }

    public boolean isOp() {
        return Pattern.matches("[()+\\-/*|&^<>%]", value);
    }

    public boolean isRegA() {
        return is("A");
    }

    public boolean isRegX() {
        return is("X");
    }

    public boolean isRegY() {
        return is("Y");
    }

    public boolean is(String s) {
        return value.equalsIgnoreCase(s);
    }

    public boolean is(char c) {
        return !isEOF() && value.charAt(0) == c;
    }

    public boolean isIn(String s) {
        if (isEOF()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == value.charAt(0)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEOF() {
        return this == EOF;
    }

    public boolean isEndOfLine() {
        return isOpcode() || isLabel() || isDirective() || isEOF();
    }

    public static String printLineError(String s, ParseException e) {
        StringBuilder err = new StringBuilder();
        int offset = e.getErrorOffset();

        String completeRegex =
                "\\s*(;\\*(?:.|\n)*?\\*;|;.*|([\"'])(?:\\\\\"|\\\\'|.)+?\\2|%[01_]+\\b|[()+\\-/*|&^#,=<>%]|"
                        + "\\.?[\\w.]+:|\\.[\\w.]+|\\$[0-9a-fA-F]{1,4}\\b|\\b\\d+\\b|\\b[\\w.]+)";
        Pattern tokenPattern = Pattern.compile(completeRegex);
        Matcher tokeniser = tokenPattern.matcher(s);
        if (!tokeniser.find(offset) || offset >= s.length()) {
            return "Not found!";
        }

        String t = tokeniser.group(1);
        int length = t.length();

        int start = tokeniser.start(1);
        if (start > offset) {
            t = s.substring(offset, start);
            length = start - offset;
        }

        String[] lines = s.split("\n", -1);
        int line = 0;
        while (offset >= 0) {
            offset -= lines[line].length() + 1;
            line++;
        }
        int column = lines[line - 1].length() + offset + 1;

        err.append(String.format("%s\n", e));
        err.append(String.format("At line %d, column %d:\n", line, column));

        String lineStr = lines[line - 1];
        try {
            String before = lineStr.substring(0, column).replace('\t', ' ');
            String after = lineStr.substring(column + length).replace('\t', ' ');
            err.append(String.format("%s%s%s\n", before, t, after));
            for (int i = 0; i < column; i++) {
                err.append(' ');
            }
            for (int i = 0; i < length; i++) {
                err.append('^');
            }
            err.append("\n");
        }
        catch (Exception ex) {
            err.append(lineStr);
        }
        return err.toString();
    }
}

class ASTroot {
    List<ASTblock> scopes;
    Map<String, ASTnode> labels;
    Map<String, ASTsymbol> symbols;

    public ASTroot() {
        scopes  = new ArrayList<>();
        labels = new HashMap<>();
        symbols = new HashMap<>();
    }
    public void append(ASTblock n) {
        scopes.add(n);
    }
    public void declare(Token symbol, String name, ASTexp e, ASTblock scope) {
        ASTsymbol s = new ASTsymbol(symbol, e, scope);
        symbols.put(name, s);
        scope.append(s);
    }
    public void declare(String label, ASTnode n) {
        labels.put(label, n);
    }
    public boolean defined(String name) {
        return labels.containsKey(name) || symbols.containsKey(name);
    }
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Root {\n");
        for (ASTblock scope : scopes) {
            s.append(scope).append("\n");
        }
        s.append("}");
        return s.toString();
    }
}

abstract class ASTnode {
}

class ASTlabel extends ASTnode {
    String name;
    int address;
    public ASTlabel(String name) {
        this.address = -1;
        this.name = name;
    }
    @Override
    public String toString() {
        return String.format("> label %s", name);
    }
}

class ASTblock extends ASTlabel {
//    String name;
    List<ASTnode> lines;

    public ASTblock(String name) {
        super(name);
//        this.name = name;
        lines = new ArrayList<>();
//        address = -1;
    }
    public void append(ASTnode n) {
        lines.add(n);
    }
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(String.format("Block \"%s\" {\n", name));
        for (ASTnode line : lines) {
            s.append(line).append("\n");
        }
        s.append("}");
        return s.toString();
    }
}

class ASTdirec extends ASTnode {
    Token directive;
    List<ASTexp> args;

    public ASTdirec(Token directive, List<ASTexp> args) {
        this.directive = directive;
        this.args = args;
    }
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(String.format("> directive \"%s\": ", directive));
        for (ASTexp arg : args) {
            s.append(arg).append("; ");
        }
        return s.toString();
    }
}

class ASTsymbol extends ASTnode {
    int size;
    Token declaration;
    ASTexp e;
    ASTblock scope;
    int address;
    public ASTsymbol(Token declaration, ASTexp e, ASTblock scope) {
        this.declaration = declaration;
        this.scope = scope;
        this.e = e;
        this.size = 0;
        this.address = -1;
    }
    public boolean sizeCalculable(ASTroot root) throws ParseException {
        if (size > 0) return false;
        size = e.calcSize(scope, root);
        return true;
    }
    @Override
    public String toString() {
        return declaration.toString();
    }
}

class ASTinstr extends ASTnode {
    enum AddrMode {
        IMM, IMP, ABS, ABX, ABY, IND, INX, INY, ZPG, ZPX, ZPY, ZPR, REL, IZP, IZX, IZY
    }
    Token opcode;
    ASTexp param;
    AddrMode mode;
    int size;

    public ASTinstr(Token opcode, ASTexp param, AddrMode mode) {
        this.opcode = opcode;
        this.param = param;
        this.mode = mode;
        size = 0;
    }

    @Override
    public String toString() {
        return String.format("> %s: %s (%s)", opcode, param, mode);
    }
}

class ASTbranch extends ASTnode {
    final static Map<String, String> opposite = initMap();

    private static Map<String, String> initMap() {
        Map<String, String> ret = new HashMap<>();
        ret.put("BPL", "BMI");
        ret.put("BVC", "BVS");
        ret.put("BCC", "BCS");
        ret.put("BEQ", "BNE");
        ret.put("BMI", "BPL");
        ret.put("BVS", "BVC");
        ret.put("BCS", "BCC");
        ret.put("BNE", "BEQ");
        ret.put("BBR0", "BBS0");
        ret.put("BBR1", "BBS1");
        ret.put("BBR2", "BBS2");
        ret.put("BBR3", "BBS3");
        ret.put("BBR4", "BBS4");
        ret.put("BBR5", "BBS5");
        ret.put("BBR6", "BBS6");
        ret.put("BBR7", "BBS7");
        ret.put("BBS0", "BBR0");
        ret.put("BBS1", "BBR1");
        ret.put("BBS2", "BBR2");
        ret.put("BBS3", "BBR3");
        ret.put("BBS4", "BBR4");
        ret.put("BBS5", "BBR5");
        ret.put("BBS6", "BBR6");
        ret.put("BBS7", "BBR7");
        return ret;
    }
    ASTexp param;
    Token replaced;
    ASTinstr.AddrMode mode;
    public ASTbranch(ASTexp param, Token replaced, ASTinstr.AddrMode mode) {
        this.param = param;
        this.replaced = replaced;
        this.mode = mode;
    }
    public String getOpposite(String opcode) {
        return opposite.get(opcode.toUpperCase());
    }
    public int getSize() {
        int size = 0;
        if (mode == ASTinstr.AddrMode.REL) {
            if (!replaced.is("BRA")) {
                size += 2;
            }
            size += 3;
        }
        else {
            size += 6;
        }
        return size;
    }
    public void addBytes(CodeGenerator gen, ASTblock scope, ASTroot root) {
        int jmp = CodeGenerator.getInstCode("JMP", ASTinstr.AddrMode.ABS);
        ExpVal v = param.calcValue(gen.pc, scope, root);
        int num = v.getInt();
        if (mode == ASTinstr.AddrMode.REL) {
            if (!replaced.is("BRA")) {
                String opp = getOpposite(replaced.toString());
                int code = CodeGenerator.getInstCode(opp, ASTinstr.AddrMode.REL);
                gen.add(code, 3);
            }
            gen.add(jmp, num & 0xFF, num >>> 8);
        }
        else {
            String opp = getOpposite(replaced.toString());
            int code = CodeGenerator.getInstCode(opp, ASTinstr.AddrMode.ZPR);
            int l = (v.getString().charAt(2) << 8) | v.getString().charAt(1);
            gen.add(code, num & 0xFF, 3);
            gen.add(jmp, l & 0xFF, l >>> 8);
        }
    }
}

abstract class ASTexp {
    public static ASTexp EMPTY = new ASTexp() {
        @Override
        public String toString() {
            return "{EMPTY}";
        }

        @Override
        public int calcSize(ASTblock scope, ASTroot root) {
            return 0;
        }

        @Override
        public ExpVal calcValue(int location, ASTblock scope, ASTroot root) {
            return new ExpVal(0, 1);
        }

        @Override
        public boolean isConst(ASTblock scope, ASTroot root) {
            return true;
        }

        @Override
        public Token getToken() {
            return null;
        }
    };

    abstract public String toString();
    abstract public int calcSize(ASTblock scope, ASTroot root) throws ParseException;
    abstract public ExpVal calcValue(int location, ASTblock scope, ASTroot root);
    abstract public boolean isConst(ASTblock scope, ASTroot root);
    abstract public Token getToken();
}

class ASTop0 extends ASTexp {
    Token num;
    public ASTop0(Token num) {
        this.num = num;
    }

    @Override
    public int calcSize(ASTblock scope, ASTroot root) throws ParseException {
        if (num.is('*')) {
            return 2;
        }
        else if (num.isNumber()) {
            return num.getNumberSize();
        }
        else if (num.isStringLiteral()) {
            return num.getStringLiteral().length();
        }
        else if (CodeGenerator.instConstantExists(num.value)) {
            return 1;
        }
        else if (num.isLocal() && root.labels.containsKey(scope.name + num.value)) {
            return 2;
        }
        else if (num.isLocal() && root.symbols.containsKey(scope.name + num.value)) {
            return root.symbols.get(scope.name + num.value).size;
        }
        else if (root.labels.containsKey(num.value)) {
            return 2;
        }
        else if (root.symbols.containsKey(num.value)) {
            return root.symbols.get(num.value).size;
        }
        else {
//            return 0;
            throw new ParseException("Undefined symbol " + num, num.offset);
        }
    }

    @Override
    public ExpVal calcValue(int location, ASTblock scope, ASTroot root) {
//        System.out.println(scope.name + " -> " + num.value);
        try {
            if (num.is('*')) {
//                System.out.println("Location: " + location);
                return new ExpVal(location, calcSize(scope, root));
            }
            else if (num.isNumber()) {
                return new ExpVal(num.getNumber(), calcSize(scope, root));
            }
            else if (num.isStringLiteral()) {
                return new ExpVal(num.getStringLiteral());
            }
            else if (CodeGenerator.instConstantExists(num.value)) {
                return new ExpVal(CodeGenerator.getInstConstantCode(num.value), 1);
            }
            else if (num.isLocal() && root.labels.containsKey(scope.name + num.value)) {
                return new ExpVal(((ASTlabel) root.labels.get(scope.name + num.value)).address,
                        calcSize(scope, root));
            }
            else if (num.isLocal() && root.symbols.containsKey(scope.name + num.value)) {
                ASTsymbol s = root.symbols.get(scope.name + num.value);
                return s.e.calcValue(s.address, s.scope, root);
            }
            else if (root.labels.containsKey(num.value)) {
                ASTnode l = root.labels.get(num.value);
                if (l instanceof  ASTblock)
                    return new ExpVal(((ASTblock) l).address, calcSize(scope, root));
                else if (l instanceof ASTlabel)
                    return new ExpVal(((ASTlabel) l).address, calcSize(scope, root));
            }
            else if (root.symbols.containsKey(num.value)) {
                ASTsymbol s = root.symbols.get(num.value);
                return s.e.calcValue(s.address, s.scope, root);
            }
        }
        catch (ParseException ignored) {

        }
//        System.out.println("Failed");
        return new ExpVal(-1, 1);
    }

    @Override
    public boolean isConst(ASTblock scope, ASTroot root) {
        if (num.isNumber()) {
            return true;
        }
        else if (num.is('*')) {
            return true;
        }
        else if (num.isStringLiteral()) {
            return true;
        }
        else if (CodeGenerator.instConstantExists(num.value)) {
            return true;
        }
        else if (num.isLocal() && root.labels.containsKey(scope.name + num.value)) {
            return ((ASTlabel) root.labels.get(scope.name + num.value)).address != -1;
        }
        else if (num.isLocal() && root.symbols.containsKey(scope.name + num.value)) {
            return root.symbols.get(scope.name + num.value).e.isConst(scope, root);
        }
        else if (root.labels.containsKey(num.value)) {
            return ((ASTblock) root.labels.get(num.value)).address != -1;
        }
        else if (root.symbols.containsKey(num.value)) {
            return root.symbols.get(num.value).e.isConst(scope, root);
        }
        else {
            return true;
        }
    }

    @Override
    public String toString() {
        if (num.isNumber())
            return String.format("{#%d}", num.getNumber());
        else if (num.isStringLiteral())
            return String.format("{\"%s\"}", num.getStringLiteral());
        else
            return String.format("{$%s}", num);
    }

    @Override
    public Token getToken() {
        return num;
    }
}

class ASTop1 extends ASTexp {
    enum ExpOp {
        PAR, NEG, LO, HI, LEN
    }
    ExpOp operation;
    ASTexp num;
    public ASTop1(ASTexp num, ExpOp operation) {
        this.num = num;
        this.operation = operation;
    }

    @Override
    public int calcSize(ASTblock scope, ASTroot root) throws ParseException {
        int opsize = num.calcSize(scope, root);
        switch (operation) {
            case PAR:
                return opsize;
            case NEG:
                if (opsize > 2)
                    throw new ParseException("Invalid operand for operation " + operation, getToken().offset);
                else return opsize;
            case LO:
            case HI:
                if (opsize > 2)
                    throw new ParseException("Invalid operand for operation " + operation, getToken().offset);
                else if (opsize == 0) return 0;
                else return 1;
            case LEN:
                return 2;
            default:
                return 0;
        }
    }

    @Override
    public ExpVal calcValue(int location, ASTblock scope, ASTroot root) {
        ExpVal value = num.calcValue(location, scope, root);
//        System.out.println("Result for "
//                + operation
//                + " = (bytes) "
//                + value
//                + "; (value) "
//                + value.value
//                + "; (size) "
//                + value.size);
        try {
            switch (operation) {
                case PAR:
                    return value;
                case NEG:
                    return new ExpVal(-(value.getInt()), calcSize(scope, root));
                case LO:
                    return new ExpVal(value.getInt() & 0xFF, calcSize(scope, root));
                case HI:
                    return new ExpVal(value.getInt() >>> 8, calcSize(scope, root));
                case LEN:
                    return new ExpVal(value.size, calcSize(scope, root));
            }
        }
        catch (ParseException ignored) {

        }
//        System.out.println("Failed");
        return new ExpVal(-1, 1);
    }

    @Override
    public boolean isConst(ASTblock scope, ASTroot root) {
        return num.isConst(scope, root);
    }

    @Override
    public String toString() {
        switch (operation) {
            case PAR:
                return String.format("{(%s)}", num);
            case NEG:
                return String.format("{-%s}", num);
            case LO:
                return String.format("{<%s}", num);
            case HI:
                return String.format("{>%s}", num);
            case LEN:
                return String.format("{#%s}", num);
            default:
                return num.toString();
        }
    }

    @Override
    public Token getToken() {
        return num.getToken();
    }
}

class ASTop2 extends ASTexp {
    enum ExpOp {
        ADD, SUB, MUL, DIV, AND, OR, XOR, MOD, TWO
    }
    ExpOp operation;
    ASTexp num1;
    ASTexp num2;
    public ASTop2(ASTexp num1, ExpOp operation, ASTexp num2) {
        this.num1 = num1;
        this.operation = operation;
        this.num2 = num2;
    }

    @Override
    public int calcSize(ASTblock scope, ASTroot root) throws ParseException {
        int num1size = num1.calcSize(scope, root);
        int num2size = num2.calcSize(scope, root);
        if (operation == ExpOp.TWO) {
            if (num1size > 1)
                throw new ParseException("Invalid zero page operand for Zero Page, Relative ", getToken().offset);
            if (num2size > 2)
                throw new ParseException("Invalid relative operand for Zero Page, Relative ", getToken().offset);
            return 2;
        }
        int totalsize = Math.max(num1size, num2size);
        if (totalsize > 2 && operation != ExpOp.ADD)
            throw new ParseException("Invalid operand for operation " + operation, getToken().offset);
        else return totalsize;
    }

    @Override
    public ExpVal calcValue(int location, ASTblock scope, ASTroot root) {
        ExpVal value1 = num1.calcValue(location, scope, root);
        ExpVal value2 = num2.calcValue(location, scope, root);
//        System.out.println("Results = " + value1 + ", " + value2);
        try {
            switch (operation) {
                case ADD:
                    if (calcSize(scope, root) > 2)
                        return new ExpVal(value1.getString() + value2.getString());
                    return new ExpVal(value1.getInt() + value2.getInt(), calcSize(scope, root));
                case DIV:
                    return new ExpVal(value1.getInt() / value2.getInt(), calcSize(scope, root));
                case OR:
                    return new ExpVal(value1.getInt() | value2.getInt(), calcSize(scope, root));
                case AND:
                    return new ExpVal(value1.getInt() & value2.getInt(), calcSize(scope, root));
                case XOR:
                    return new ExpVal(value1.getInt() ^ value2.getInt(), calcSize(scope, root));
                case MOD:
                    return new ExpVal(value1.getInt() % value2.getInt(), calcSize(scope, root));
                case MUL:
                    return new ExpVal(value1.getInt() * value2.getInt(), calcSize(scope, root));
                case SUB:
                    return new ExpVal(value1.getInt() - value2.getInt(), calcSize(scope, root));
                case TWO:
                    return new ExpVal(value1.getString() + value2.getString());
            }
        }
        catch (ParseException ignored) {

        }
//        System.out.println("Failed");
        return new ExpVal(-1, 1);
    }

    @Override
    public boolean isConst(ASTblock scope, ASTroot root) {
        return num1.isConst(scope, root) && num2.isConst(scope, root);
    }

    @Override
    public String toString() {
        switch (operation) {
            case ADD:
                return String.format("{%s + %s}", num1, num2);
            case SUB:
                return String.format("{%s - %s}", num1, num2);
            case MUL:
                return String.format("{%s * %s}", num1, num2);
            case DIV:
                return String.format("{%s / %s}", num1, num2);
            case AND:
                return String.format("{%s & %s}", num1, num2);
            case OR:
                return String.format("{%s | %s}", num1, num2);
            case XOR:
                return String.format("{%s ^ %s}", num1, num2);
            case MOD:
                return String.format("{%s %% %s}", num1, num2);
            case TWO:
                return String.format("{%s, %s}", num1, num2);
            default:
                return String.format("{%s ? %s}", num1, num2);
        }
    }

    @Override
    public Token getToken() {
        return num1.getToken();
    }
}

class ExpVal {
    Object value;
    int size;
    public ExpVal(int value, int size) {
        this.value = value;
        this.size = size;
    }
    public ExpVal(String value) {
        this.value = value;
        this.size = value.length();
    }

    public String getString() {
        if (value instanceof Integer) {
            int num = (int) value;
            StringBuilder ret = new StringBuilder();
            for (int i = 0; i < size; i++) {
                ret.append((char) (num & 0xFF));
                num >>>= 8;
            }
            return ret.toString();
        }
        else return (String) value;
    }

    public int getInt() {
        if (value instanceof String) {
            String s = (String) value;
            int ret = 0;
            for (int i = s.length() - 1; i >= 0; i--) {
                char c = s.charAt(i);
                ret = ret << 8 | c;
            }
            return ret & 0xFFFF;
        }
        else return (int) value;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        String chars = getString();
        for (int i = 0; i < chars.length(); i++) {
            int c = chars.charAt(i);
            res.append(String.format("%02X ", c));
        }
        return res.toString();
    }
}

class Parser {
    List<Token> tokens;
    int ptr;
    Stack<Integer> context;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.ptr = 0;
        this.context = new Stack<>();
    }

    public void consume() {
        ptr++;
    }

    public void consume(int num) {
        ptr += num;
    }

    public Token peek() {
        try {
            return tokens.get(ptr);
        }
        catch (IndexOutOfBoundsException e) {
            return Token.EOF;
        }
    }

    public Token peek(int num) {
        try {
            return tokens.get(ptr + num);
        }
        catch (IndexOutOfBoundsException e) {
            return Token.EOF;
        }
    }

    public void save() {
        context.push(ptr);
    }

    public void restore() {
        ptr = context.pop();
    }

    public ASTroot parse() throws ParseException {
        ASTroot root = new ASTroot();
        ASTblock currentLabel = new ASTblock("_root");
        while (!peek().isEOF()) {
            Token t = peek();
            if (t.isLabel()) {
//                System.out.printf("label '%s':\n", t.getLabel());
                if (t.isLocal()) {
//                    System.out.printf(" -> scope '%s':\n", currentLabel.name);
                    String fullname = currentLabel.name + t.getLabel();
                    ASTlabel l = new ASTlabel(fullname);
                    currentLabel.append(l);
                    if (root.defined(fullname)) {
                        throw new ParseException("Name already defined somewhere else", t.offset);
                    }
                    root.declare(fullname, l);
                }
                else {
                    root.append(currentLabel);
//                    System.out.println(currentLabel);
                    currentLabel = new ASTblock(t.getLabel());
                    if (root.defined(t.getLabel())) {
                        throw new ParseException("Name already defined somewhere else", t.offset);
                    }
                    root.declare(t.getLabel(), currentLabel);
                }
                consume();
            }
            else if (t.isDirective()) {
                currentLabel.append(parseDirective());
            }
            else if (t.isOpcode()) {
//                System.out.println(parseLine());
                currentLabel.append(parseLine());
            }
            else if (t.isIdentifier()) {
                consume();
                if (!peek().is('=')) {
                    throw new ParseException("Expected = after identifier", peek().offset);
                }
                consume();
                ASTexp e = parseExp();
                String name;
                if (t.isLocal()) {
                    name = currentLabel.name + t;
//                    System.out.printf("Local symbol '%s' defined as: %s\n", name, e);
                }
                else {
                    name = t.toString();
//                    System.out.printf("Symbol '%s' defined as: %s\n", name, e);
                }
                if (root.defined(name)) {
                    throw new ParseException("Name already defined somewhere else", t.offset);
                }
                root.declare(t, name, e, currentLabel);
            }
            else {
                throw new ParseException("Unexpected token", t.offset);
            }
        }
        root.append(currentLabel);
//        System.out.println("== LABELS ==");
//        for (String label: root.labels.keySet()) {
//            System.out.println(label);
//        }
//        System.out.println("== SYMBOLS ==");
//        for (String symbol: root.symbols.keySet()) {
//            System.out.printf("%s = %s;\n", symbol, root.symbols.get(symbol).e);
//        }

        return root;
    }

    public ASTdirec parseDirective() throws ParseException {
        Token t = peek();
        consume();
        List<ASTexp> argList = parseArgs();
        return new ASTdirec(t, argList);
    }

    public ASTinstr parseLine() throws ParseException {
        Token t = peek();
        consume();
        peek();
        ASTexp e;
        ASTinstr.AddrMode m;
        if (peek().is('#')) {
            consume();
            e = parseExp();
//            System.out.printf("%s: imm %s\n", t, e);
            m = ASTinstr.AddrMode.IMM;
        }
        else if (peek().isEndOfLine() || peek(1).is('=')) {
//            System.out.printf("%s: imp\n", t);
            e = ASTexp.EMPTY;
            m = ASTinstr.AddrMode.IMP;
        }
        else if (peek().isRegA()) {
            consume();
            e = ASTexp.EMPTY;
//            System.out.printf("%s: imm %s\n", t, e);
            m = ASTinstr.AddrMode.IMP;
        }
        else if (peek().is('(')) {
            save();
            try {
                e = parseExp();
            }
            catch (ParseException ex) {
//                System.out.println("Failed prediction, restoring old context...");
                restore();
                consume();
                e = parseExp();
                if (peek().is(',') && peek(1).isRegX()) {
                    consume();
                    consume();
                    if (peek().is(")")) {
                        consume();
//                        System.out.printf("%s: indX %s\n", t, e);
                        m = ASTinstr.AddrMode.INX;
                    }
                    else throw new ParseException("Expected )", peek().offset);
                }
                else if (peek().is(',') && peek(1).isRegY()) {
                    throw new ParseException("Invalid indexed mode", peek(1).offset);
                }
                else {
                    throw new ParseException("Unexpected token", peek().offset);
                }
                return new ASTinstr(t, e, m);
            }
            if (peek().is(',') && peek(1).isRegY()) {
                consume();
                consume();
//                    System.out.printf("%s: indY %s\n", t, e);
                m = ASTinstr.AddrMode.INY;
            }
            else if (peek().is(',') && peek(1).isRegX()) {
                throw new ParseException("Invalid indexed mode", peek(1).offset);
            }
            else if (peek(-1).is(")")) {
//                    System.out.printf("%s: ind %s\n", t, e);
                m = ASTinstr.AddrMode.IND;
            }
            else {
//                    System.out.printf("%s: abs %s\n", t, e);
                m = ASTinstr.AddrMode.ABS;
            }
        }
        else {
            e = parseExp();
            if (peek().is(',') && peek(1).isRegX()) {
//                System.out.printf("%s: absX %s\n", t, e);
                m = ASTinstr.AddrMode.ABX;
                consume();
                consume();
            }
            else if (peek().is(',') && peek(1).isRegY()) {
//                System.out.printf("%s: absY %s\n", t, e);
                m = ASTinstr.AddrMode.ABY;
                consume();
                consume();
            }
            else if (peek().is(',')) {
                consume();
                ASTexp e2 = parseExp();
                m = ASTinstr.AddrMode.ZPR;
                e = new ASTop2(e, ASTop2.ExpOp.TWO, e2);
            }
            else {
//                System.out.printf("%s: abs %s\n", t, e);
                m = ASTinstr.AddrMode.ABS;
            }
        }
        return new ASTinstr(t, e, m);
    }

    public List<ASTexp> parseArgs() throws ParseException {
        List<ASTexp> argv = new ArrayList<>();
        save();
        try {
            argv.add(parseExp());
        }
        catch (ParseException ignored) {
//            System.out.println("No args found, restoring...");
            restore();
            return argv;
        }
        while (peek().is(',')) {
            consume();
            argv.add(parseExp());
        }
        return argv;
    }

    public ASTexp parseExp() throws ParseException { // OR
        ASTexp e = parseExp2();
        while (peek().is('|')) {
            consume();
            ASTexp e2 = parseExp2();
            e = new ASTop2(e, ASTop2.ExpOp.OR, e2);
        }
        return e;
    }

    public ASTexp parseExp2() throws ParseException { // AND
        ASTexp e = parseExp3();
        while (peek().is('&')) {
            consume();
            ASTexp e2 = parseExp3();
            e = new ASTop2(e, ASTop2.ExpOp.AND, e2);
        }
        return e;
    }

    public ASTexp parseExp3() throws ParseException { // XOR
        ASTexp e = parseExp4();
        while (peek().is('^')) {
            consume();
            ASTexp e2 = parseExp4();
            e = new ASTop2(e, ASTop2.ExpOp.XOR, e2);
        }
        return e;
    }

    public ASTexp parseExp4() throws ParseException { // ADD SUB
        ASTexp e = parseExp5();
        while (peek().isIn("+-")) {
            Token op = peek();
            consume();
            ASTexp e2 = parseExp5();
            if (op.is('+'))
                e = new ASTop2(e, ASTop2.ExpOp.ADD, e2);
            else if (op.is('-'))
                e = new ASTop2(e, ASTop2.ExpOp.SUB, e2);
        }
        return e;
    }

    public ASTexp parseExp5() throws ParseException { // MUL DIV MOD
        ASTexp e = parseExp6();
        while (peek().isIn("/*%")) {
            Token op = peek();
            consume();
            ASTexp e2 = parseExp6();
            if (op.is('/'))
                e = new ASTop2(e, ASTop2.ExpOp.DIV, e2);
            else if (op.is('*'))
                e = new ASTop2(e, ASTop2.ExpOp.MUL, e2);
            else if (op.is('%'))
                e = new ASTop2(e, ASTop2.ExpOp.MOD, e2);
        }
        return e;
    }

    public ASTexp parseExp6() throws ParseException { // NEG LO HI & NUM
        if (peek().isIn("-<>#")) {
            Token op = peek();
            consume();
            ASTexp e = parseExp6();
            if (op.is('-'))
                return new ASTop1(e, ASTop1.ExpOp.NEG);
            else if (op.is('<'))
                return new ASTop1(e, ASTop1.ExpOp.LO);
            else if (op.is('>'))
                return new ASTop1(e, ASTop1.ExpOp.HI);
            else if (op.is('#'))
                return new ASTop1(e, ASTop1.ExpOp.LEN);
        }
        else {
            return parseExp7();
        }
        return null;
    }

    public ASTexp parseExp7() throws ParseException { // PARENTHESES
        if (peek().is('(')) {
            consume();
            ASTexp e = parseExp();
            if (!peek().is(')')) {
                throw new ParseException("Expected operator or )", peek().offset);
            }
            consume();
            return new ASTop1(e, ASTop1.ExpOp.PAR);
        }
        else if ((peek().isValue() && !peek(1).is('=') && !peek().isEndOfLine()) || peek().is('*')) {
            Token num = peek();
            consume();
            return new ASTop0(num);
        }
        else {
            throw new ParseException("Expected number, identifier or (", peek().offset);
        }
    }

//    public void printLineError(String s, ParseException e) {
//        Token t = tokens.get(e.getErrorOffset());
//        System.out.printf("\033[31m%s\033[0m\n", e);
//        System.out.printf("\033[31mToken \"\033[0m%s\033[31m\"\033[0m\n", t);
//        int offset = t.offset;
//        int length = t.value.length();
//        if (offset >= s.length()) {
//            System.out.println("Not found!");
//            return;
//        }
//        String[] lines = s.split("[\n\r]", -1);
//        int line = 0;
//        while (offset >= 0) {
//            offset -= lines[line].length() + 1;
//            line++;
//        }
//        int column = lines[line - 1].length() + offset + 1;
//        System.out.printf("\033[31mAt line \033[0m%d\033[31m, column \033[0m%d\033[31m:\033[0m\n", line, column);
//        String lineStr = lines[line - 1];
//        String before = lineStr.substring(0, column);
//        String after = lineStr.substring(column + length);
//        System.out.printf("%s\033[31m%s\033[0m%s\n", before, t, after);
//        for (int i = 0; i < column; i++) {
//            System.out.print(" ");
//        }
//        for (int i = 0; i < length; i++) {
//            System.out.print("^");
//        }
//        System.out.println();
//    }
}

class CodeGenerator {

    final static Map<String, Integer> opcodes = initMap();

    int[] ram;
    int pc;
    ASTroot root;

    public CodeGenerator(ASTroot root) {
        ram = new int[0x10000]; // 65K
//        clearMemory();
        this.root = root;
    }

    private static Map<String, Integer> initMap() {
        final List<String> opcodeList = Stream.of(
                "_BRK_IMP",
                "_ORA_IZX",
                "_NOP_IMM",
                "_NOP_IMP",
                "_TSB_ZPG",
                "_ORA_ZPG",
                "_ASL_ZPG",
                "_RMB0_ZPG",
                "_PHP_IMP",
                "_ORA_IMM",
                "_ASL_IMP",
                "_NOP_IMP",
                "_TSB_ABS",
                "_ORA_ABS",
                "_ASL_ABS",
                "_BBR0_ZPR",
                "_BPL_REL",
                "_ORA_IZY",
                "_ORA_IZP",
                "_NOP_IMP",
                "_TRB_ZPG",
                "_ORA_ZPX",
                "_ASL_ZPX",
                "_RMB1_ZPG",
                "_CLC_IMP",
                "_ORA_ABY",
                "_INC_IMP",
                "_NOP_IMP",
                "_TRB_ABS",
                "_ORA_ABX",
                "_ASL_ABX",
                "_BBR1_ZPR",
                "_JSR_ABS",
                "_AND_IZX",
                "_NOP_IMM",
                "_NOP_IMP",
                "_BIT_ZPG",
                "_AND_ZPG",
                "_ROL_ZPG",
                "_RMB2_ZPG",
                "_PLP_IMP",
                "_AND_IMM",
                "_ROL_IMP",
                "_NOP_IMP",
                "_BIT_ABS",
                "_AND_ABS",
                "_ROL_ABS",
                "_BBR2_ZPR",
                "_BMI_REL",
                "_AND_IZY",
                "_AND_IZP",
                "_NOP_IMP",
                "_BIT_ZPX",
                "_AND_ZPX",
                "_ROL_ZPX",
                "_RMB3_ZPG",
                "_SEC_IMP",
                "_AND_ABY",
                "_DEC_IMP",
                "_NOP_IMP",
                "_BIT_ABX",
                "_AND_ABX",
                "_ROL_ABX",
                "_BBR3_ZPR",
                "_RTI_IMP",
                "_EOR_IZX",
                "_NOP_IMM",
                "_NOP_IMP",
                "_NOP_ZPG",
                "_EOR_ZPG",
                "_LSR_ZPG",
                "_RMB4_ZPG",
                "_PHA_IMP",
                "_EOR_IMM",
                "_LSR_IMP",
                "_NOP_IMP",
                "_JMP_ABS",
                "_EOR_ABS",
                "_LSR_ABS",
                "_BBR4_ZPR",
                "_BVC_REL",
                "_EOR_IZY",
                "_EOR_IZP",
                "_NOP_IMP",
                "_NOP_ZPX",
                "_EOR_ZPX",
                "_LSR_ZPX",
                "_RMB5_ZPG",
                "_CLI_IMP",
                "_EOR_ABY",
                "_PHY_IMP",
                "_NOP_IMP",
                "_NOP_ABS",
                "_EOR_ABX",
                "_LSR_ABX",
                "_BBR5_ZPR",
                "_RTS_IMP",
                "_ADC_IZX",
                "_NOP_IMM",
                "_NOP_IMP",
                "_STZ_ZPG",
                "_ADC_ZPG",
                "_ROR_ZPG",
                "_RMB6_ZPG",
                "_PLA_IMP",
                "_ADC_IMM",
                "_ROR_IMP",
                "_NOP_IMP",
                "_JMP_IND",
                "_ADC_ABS",
                "_ROR_ABS",
                "_BBR6_ZPR",
                "_BVS_REL",
                "_ADC_IZY",
                "_ADC_IZP",
                "_NOP_IMP",
                "_STZ_ZPX",
                "_ADC_ZPX",
                "_ROR_ZPX",
                "_RMB7_ZPG",
                "_SEI_IMP",
                "_ADC_ABY",
                "_PLY_IMP",
                "_NOP_IMP",
                "_JMP_INX",
                "_ADC_ABX",
                "_ROR_ABX",
                "_BBR7_ZPR",
                "_BRA_REL",
                "_STA_IZX",
                "_NOP_IMM",
                "_NOP_IMP",
                "_STY_ZPG",
                "_STA_ZPG",
                "_STX_ZPG",
                "_SMB0_ZPG",
                "_DEY_IMP",
                "_BIT_IMM",
                "_TXA_IMP",
                "_NOP_IMP",
                "_STY_ABS",
                "_STA_ABS",
                "_STX_ABS",
                "_BBS0_ZPR",
                "_BCC_REL",
                "_STA_IZY",
                "_STA_IZP",
                "_NOP_IMP",
                "_STY_ZPX",
                "_STA_ZPX",
                "_STX_ZPY",
                "_SMB1_ZPG",
                "_TYA_IMP",
                "_STA_ABY",
                "_TXS_IMP",
                "_NOP_IMP",
                "_STZ_ABS",
                "_STA_ABX",
                "_STZ_ABX",
                "_BBS1_ZPR",
                "_LDY_IMM",
                "_LDA_IZX",
                "_LDX_IMM",
                "_NOP_IMP",
                "_LDY_ZPG",
                "_LDA_ZPG",
                "_LDX_ZPG",
                "_SMB2_ZPG",
                "_TAY_IMP",
                "_LDA_IMM",
                "_TAX_IMP",
                "_NOP_IMP",
                "_LDY_ABS",
                "_LDA_ABS",
                "_LDX_ABS",
                "_BBS2_ZPR",
                "_BCS_REL",
                "_LDA_IZY",
                "_LDA_IZP",
                "_NOP_IMP",
                "_LDY_ZPX",
                "_LDA_ZPX",
                "_LDX_ZPY",
                "_SMB3_ZPG",
                "_CLV_IMP",
                "_LDA_ABY",
                "_TSX_IMP",
                "_NOP_IMP",
                "_LDY_ABX",
                "_LDA_ABX",
                "_LDX_ABY",
                "_BBS3_ZPR",
                "_CPY_IMM",
                "_CMP_IZX",
                "_NOP_IMM",
                "_NOP_IMP",
                "_CPY_ZPG",
                "_CMP_ZPG",
                "_DEC_ZPG",
                "_SMB4_ZPG",
                "_INY_IMP",
                "_CMP_IMM",
                "_DEX_IMP",
                "_WAI_IMP",
                "_CPY_ABS",
                "_CMP_ABS",
                "_DEC_ABS",
                "_BBS4_ZPR",
                "_BNE_REL",
                "_CMP_IZY",
                "_CMP_IZP",
                "_NOP_IMP",
                "_NOP_ZPX",
                "_CMP_ZPX",
                "_DEC_ZPX",
                "_SMB5_ZPG",
                "_CLD_IMP",
                "_CMP_ABY",
                "_PHX_IMP",
                "_STP_IMP",
                "_NOP_ABS",
                "_CMP_ABX",
                "_DEC_ABX",
                "_BBS5_ZPR",
                "_CPX_IMM",
                "_SBC_IZX",
                "_NOP_IMM",
                "_NOP_IMP",
                "_CPX_ZPG",
                "_SBC_ZPG",
                "_INC_ZPG",
                "_SMB6_ZPG",
                "_INX_IMP",
                "_SBC_IMM",
                "_NOP_IMP",
                "_NOP_IMP",
                "_CPX_ABS",
                "_SBC_ABS",
                "_INC_ABS",
                "_BBS6_ZPR",
                "_BEQ_REL",
                "_SBC_IZY",
                "_SBC_IZP",
                "_NOP_IMP",
                "_NOP_ZPX",
                "_SBC_ZPX",
                "_INC_ZPX",
                "_SMB7_ZPG",
                "_SED_IMP",
                "_SBC_ABY",
                "_PLX_IMP",
                "_NOP_IMP",
                "_NOP_ABS",
                "_SBC_ABX",
                "_INC_ABX",
                "_BBS7_ZPR"
        ).collect(Collectors.toCollection(ArrayList::new));

        Map<String, Integer> map = new HashMap<>();
        int opnum = 0;
        for (String s : opcodeList) {
            map.put(s, opnum);
            opnum++;
        }
        return map;
    }

    public int[] generate() throws ParseException {
        clearMemory();

        for (int i = 0; i < root.symbols.size(); i++) {
//            System.out.println("Iteration " + i);
            boolean changed = false;
            for (ASTsymbol symbol : root.symbols.values()) {
                if (symbol.sizeCalculable(root)) {
//                    System.out.println(symbol + " changed to " + symbol.size);
                    changed = true;
                }
            }
            if (!changed) break;
        }

        StringBuilder errors = new StringBuilder();
        int lastOffset = 0;
//        System.out.println("== SYMBOLS ==");
        for (String key : root.symbols.keySet()) {
            ASTsymbol symbol = root.symbols.get(key);
            if (symbol.size == 0) {
                errors.append(key).append("; ");
                lastOffset = symbol.declaration.offset;
            }
//            System.out.printf("Symbol %s: %s (size: %d)\n", key, symbol, symbol.size);
        }
        if (errors.length() > 0)
            throw new ParseException("Could not define symbol(s): " + errors, lastOffset);

        // REPEAT THIS IF BRANCH OUT OF RANGE
        reset:
        while (true) {
            int labelPc = 0;
            for (ASTblock scope : root.scopes) {
                //            System.out.printf("%s: ($%04X)\n", scope.name, labelPc);
                scope.address = labelPc;
                for (ASTnode line : scope.lines) {
                    if (line instanceof ASTinstr) {
                        Token opcode = ((ASTinstr) line).opcode;
                        int size = ((ASTinstr) line).param.calcSize(scope, root);
                        int offset = opcode.offset;
                        if (size == 0 && ((ASTinstr) line).mode != ASTinstr.AddrMode.IMP) {
                            throw new ParseException("An error occurred while analysing the parameter", offset);
                        }

                        switch (((ASTinstr) line).mode) {
                            case ABS:
                                if (instExists(opcode.toString(), ASTinstr.AddrMode.REL))
                                    ((ASTinstr) line).mode = ASTinstr.AddrMode.REL;
                                else if (size == 1
                                        && instExists(opcode.toString(), ASTinstr.AddrMode.ZPG))
                                    ((ASTinstr) line).mode = ASTinstr.AddrMode.ZPG;
                                else if (
                                        instExists(opcode.toString(), ASTinstr.AddrMode.ZPG)
                                        && !instExists(opcode.toString(), ASTinstr.AddrMode.ABS)
                                )
                                    ((ASTinstr) line).mode = ASTinstr.AddrMode.ZPG;
//                                    throw new ParseException("Absolute addressing not available, use Zero Page instead",
//                                            offset);
                                break;
                            case ABX:
                                if (size == 1 && instExists(opcode.toString(), ASTinstr.AddrMode.ZPX))
                                    ((ASTinstr) line).mode = ASTinstr.AddrMode.ZPX;
                                else if (size == 2
                                        && !instExists(opcode.toString(), ASTinstr.AddrMode.ABX))
                                    ((ASTinstr) line).mode = ASTinstr.AddrMode.ZPX;
                                break;
                            case ABY:
                                if (size == 1 && instExists(opcode.toString(), ASTinstr.AddrMode.ZPY))
                                    ((ASTinstr) line).mode = ASTinstr.AddrMode.ZPY;
                                else if (size == 2
                                        && !instExists(opcode.toString(), ASTinstr.AddrMode.ABY))
                                    ((ASTinstr) line).mode = ASTinstr.AddrMode.ZPY;
                                break;
                            case IND:
                                if (size == 1)
                                    ((ASTinstr) line).mode = ASTinstr.AddrMode.IZP;
                                else if (size == 2
                                        && !instExists(opcode.toString(), ASTinstr.AddrMode.IND))
                                    ((ASTinstr) line).mode = ASTinstr.AddrMode.IZP;
                                break;
                            case INX:
                                if (size == 1)
                                    ((ASTinstr) line).mode = ASTinstr.AddrMode.IZX;
                                else if (size == 2
                                        && !instExists(opcode.toString(), ASTinstr.AddrMode.INX))
                                    ((ASTinstr) line).mode = ASTinstr.AddrMode.IZX;
                                break;
                            case INY:
                                if (size == 1)
                                    ((ASTinstr) line).mode = ASTinstr.AddrMode.IZY;
                                else if (size == 2
                                        && !instExists(opcode.toString(), ASTinstr.AddrMode.INY))
                                    ((ASTinstr) line).mode = ASTinstr.AddrMode.IZY;
//                                else
//                                    throw new ParseException(
//                                            "Invalid argument for indirect,y addressing mode (too large)",
//                                            offset);
                                break;
                            case ZPR:
                                // To do: support for these instructions
                                if (instExists(opcode.toString(), ASTinstr.AddrMode.ZPR)) {
                                    // BB*b $XX,label
                                    size = 2;
                                }

                        }
                        if (!instExists(opcode.toString(), ((ASTinstr) line).mode))
                            throw new ParseException(
                                    "Addressing mode does not exist for corresponding opcode: "
                                            + ((ASTinstr) line).mode,
                                    offset
                            );
//                        System.out.printf("$%04X: %02X ", labelPc, getInstCode(opcode, ((ASTinstr) line).mode));
                        ((ASTinstr) line).size = size;
                        labelPc += size + 1;
                        if (((ASTinstr) line).mode == ASTinstr.AddrMode.REL && size == 2)
                            labelPc--;
//                        if (size == 1) {
//                            System.out.print("XX ");
//                                  pc += 2;
//                        }
//                        else if (size == 2) {
//                            System.out.print("XX XX ");
//                                  pc += 3;
//                        }
//                        System.out.printf(
//                                "  %s (%s): %s (size: %d)\n",
//                                opcode,
//                                ((ASTinstr) line).mode,
//                                ((ASTinstr) line).param,
//                                size
//                        );
//                        System.out.println();
                    }
                    else if (line instanceof ASTbranch) {
                        labelPc += ((ASTbranch) line).getSize();
                    }
                    else if (line instanceof ASTlabel) {
//                        System.out.printf("%s: ($%04X)\n", ((ASTlabel) line).name, labelPc);
                        ((ASTlabel) line).address = labelPc;
                    }
                    else if (line instanceof ASTsymbol) {
                        System.out.printf("%s: ($%04X)\n", ((ASTsymbol) line).address, labelPc);
                        ((ASTsymbol) line).address = labelPc;
                    }
                    else if (line instanceof ASTdirec) {
//                        System.out.printf("%s: ($%04X)\n", ((ASTlabel) line).name, pc);
                        List<ASTexp> args = ((ASTdirec) line).args;
                        Token name = ((ASTdirec) line).directive;
                        switch (name.value.toLowerCase()) {
                            case ".org": {
                                if (args.size() > 1) {
                                    throw new ParseException(
                                            "Too many arguments to directive .org, 1 expected, found " + args.size(),
                                            name.offset
                                    );
                                }
                                if (!args.get(0).isConst(scope, root)) {
                                    throw new ParseException(
                                            "Expression must be constant in .org directive",
                                            name.offset
                                    );
                                }
                                int size = args.get(0).calcSize(scope, root);
                                if (size > 2) {
                                    throw new ParseException(
                                            "Invalid argument to directive .org (too large)",
                                            name.offset
                                    );
                                }
                                labelPc = args.get(0).calcValue(labelPc, scope, root).getInt();
//                                System.out.printf("labelPc = %04X\n", labelPc);
                            }
                            break;
                            case ".text": {
                                int totalsize = 0;
                                for (ASTexp e : args) {
                                    totalsize += e.calcSize(scope, root);
                                }
                                labelPc += totalsize;
//                                System.out.printf("pc + %d = $%04X\n", totalsize, pc);
                            }
                            break;
                            case ".string": {
                                int totalsize = 1;
                                for (ASTexp e : args) {
                                    totalsize += e.calcSize(scope, root);
                                }
                                labelPc += totalsize;
//                                System.out.printf("pc + %d = $%04X\n", totalsize, pc);
                            }
                            break;
                            case ".word": {
                                int totalsize = 0;
                                for (ASTexp e : args) {
                                    int size = e.calcSize(scope, root);
                                    if (size > 2) {
                                        throw new ParseException(
                                                "Invalid argument to directive .word (too large)",
                                                name.offset
                                        );
                                    }
                                    totalsize += 2;
                                }
                                if (totalsize == 0) totalsize = 2;
                                labelPc += totalsize;
//                                System.out.printf("pc + %d = $%04X\n", totalsize, pc);
                            }
                            break;
                            case ".byte": {
                                int totalsize = 0;
                                for (ASTexp e : args) {
                                    int size = e.calcSize(scope, root);
                                    if (size > 2) {
                                        throw new ParseException(
                                                "Invalid argument to directive .byte (too large)",
                                                name.offset
                                        );
                                    }
                                    totalsize++;
                                }
                                if (totalsize == 0) totalsize = 1;
                                labelPc += totalsize;
//                                System.out.printf("pc + %d = $%04X\n", totalsize, pc);
                            }
                            break;
                            case ".data": {
                                if (args.size() > 1) {
                                    throw new ParseException(
                                            "Too many arguments to directive .skip, 1 expected, found " + args.size(),
                                            name.offset
                                    );
                                }
                                if (!args.get(0).isConst(scope, root)) {
                                    throw new ParseException(
                                            "Expression must be constant in .skip directive",
                                            name.offset
                                    );
                                }
                                int size = args.get(0).calcSize(scope, root);
                                if (size > 2) {
                                    throw new ParseException(
                                            "Invalid argument to directive .skip (too large)",
                                            name.offset
                                    );
                                }
                                labelPc += args.get(0).calcValue(pc, scope, root).getInt();
//                                System.out.printf("pc = %s\n", pc);
                            }
                            break;
                        }
                    }
                }
            }
//            System.out.printf("%d ($%04X) bytes.\n", pc, pc);
//
//            System.out.println("== SYMBOLS ==");
//            for (ASTsymbol s : root.symbols.values()) {
//                System.out.println("scope " + s.scope.name);
//                System.out.printf("%s = %s\n", s.declaration, s.e.calcValue(s.scope, root).getInt());
//            }

            for (ASTblock scope : root.scopes) {
//                System.out.printf("%s: ($%04X)\n", scope.name, pc);
//                scope.address = pc;
                for (ASTnode line : scope.lines) {
                    if (line instanceof ASTinstr) {
                        ASTinstr instr = (ASTinstr) line;
                        Token opcode = instr.opcode;
//                        System.out.println("Calculating " + instr.param);
                        ExpVal value = instr.param.calcValue(pc, scope, root);
                        int offset = opcode.offset;

                        if (instr.mode == ASTinstr.AddrMode.ZPR) {
                            if (value.getString().length() == 2) {
                                add(getInstCode(opcode.toString(), instr.mode));
                                int num = value.getInt();
                                add(num & 0xFF, num >>> 8);
                            }
                            else {
                                add(getInstCode(opcode.toString(), instr.mode));
                                int num = value.getInt();
                                add(num & 0xFF);
                                int l = (value.getString().charAt(2) << 8) | value.getString().charAt(1);
                                l = l - pc - 2;
//                                System.out.println(l);
                                if (l > 128 || l < -127) {
//                                    throw new ParseException("Branch instruction out of range " + l, opcode.offset);
                                    scope.lines.set(
                                            scope.lines.indexOf(line),
                                            new ASTbranch(instr.param,
                                                    opcode,
                                                    ASTinstr.AddrMode.ZPR)
                                    );
                                    continue reset;
                                }
                                add(l & 0xFF);
                            }
                        }
                        else {
                            int num = value.getInt();
                            if (instr.mode == ASTinstr.AddrMode.IMM) {
                                if (num >= 256) {
                                    throw new ParseException("Invalid parameter for immediate addressing (too large)",
                                            offset);
                                }
                            }
                            else if (instr.mode == ASTinstr.AddrMode.ABS) {
                                if (num < 256 && instExists(opcode.toString(), ASTinstr.AddrMode.ZPG)) {
                                    instr.mode = ASTinstr.AddrMode.ZPG;
                                    instr.size = 1;
                                    instr.param = new ASTop1(instr.param, ASTop1.ExpOp.LO);
//                                    System.out.println("ABS to ZPG! " + instr.opcode + " at " + pc);
                                    continue reset;
                                }
//                                else {
//                                    System.out.println("Remained ABS: " + instr.opcode + " at " + pc);
//                                }
                            }
                            else if (instr.mode == ASTinstr.AddrMode.ZPG) {
                                if (num >= 256) {
                                    throw new ParseException("Invalid parameter for zero page addressing (too large)",
                                            offset);
                                }
                                else if (instr.size > 1) {
                                    instr.size = 1;
                                    instr.param = new ASTop1(instr.param, ASTop1.ExpOp.LO);
                                    continue reset;
                                }
                            }
                            else if (instr.mode == ASTinstr.AddrMode.ZPX) {
                                if (num >= 256) {
                                    throw new ParseException("Invalid parameter for zero page,x addressing (too large)",
                                            offset);
                                }
                                else if (instr.size > 1) {
                                    instr.size = 1;
                                    instr.param = new ASTop1(instr.param, ASTop1.ExpOp.LO);
                                    continue reset;
                                }
                            }
                            else if (instr.mode == ASTinstr.AddrMode.ZPY) {
                                if (num >= 256) {
                                    throw new ParseException("Invalid parameter for zero page,y addressing (too large)",
                                            offset);
                                }
                                else if (instr.size > 1) {
                                    instr.size = 1;
                                    instr.param = new ASTop1(instr.param, ASTop1.ExpOp.LO);
                                    continue reset;
                                }
                            }
                            else if (instr.mode == ASTinstr.AddrMode.IZP) {
                                if (num >= 256) {
                                    throw new ParseException(
                                            "Invalid parameter for indirect zero page addressing (too large)",
                                            offset);
                                }
                                else if (instr.size > 1) {
                                    instr.size = 1;
                                    instr.param = new ASTop1(instr.param, ASTop1.ExpOp.LO);
                                    continue reset;
                                }
                            }
                            else if (instr.mode == ASTinstr.AddrMode.IZX) {
                                if (num >= 256) {
                                    throw new ParseException(
                                            "Invalid parameter for indirect zero page,x addressing (too large)",
                                            offset);
                                }
                                else if (instr.size > 1) {
                                    instr.size = 1;
                                    instr.param = new ASTop1(instr.param, ASTop1.ExpOp.LO);
                                    continue reset;
                                }
                            }
                            else if (instr.mode == ASTinstr.AddrMode.IZY) {
                                if (num >= 256) {
                                    throw new ParseException(
                                            "Invalid parameter for indirect zero page,y addressing (too large)",
                                            offset);
                                }
                                else if (instr.size > 1) {
                                    instr.size = 1;
                                    instr.param = new ASTop1(instr.param, ASTop1.ExpOp.LO);
                                    continue reset;
                                }
                            }
                            else if (instr.mode == ASTinstr.AddrMode.REL) {
                                if (instr.size > 1) {
//                                    System.out.printf("$%04X - $%04X = %d\n", num, pc, num - pc);
                                    num = num - pc - 2;
                                    if (num > 128 || num < -127) {
                                        scope.lines.set(
                                                scope.lines.indexOf(line),
                                                new ASTbranch(instr.param,
                                                        opcode,
                                                        ASTinstr.AddrMode.REL)
                                        );
                                        continue reset;
//                                        throw new ParseException("Branch instruction out of range", opcode.offset);
                                    }
                                    instr.size = 1;
                                }
                            }
//                            System.out.printf("$%04X: ", pc);
//                            System.out.printf("$%04X: %02X ", pc, getInstCode(opcode, instr.mode));
                            add(getInstCode(opcode.toString(), instr.mode));
                            switch (instr.size) {
                                case 1:
//                                    System.out.printf("%02X ", num & 0xFF);
                                    add(num & 0xFF);
                                    break;
                                case 2:
//                                    System.out.printf("%02X %02X ", num & 0xFF, num >>> 8);
                                    add(num & 0xFF, num >>> 8);
                                    break;
                            }
                        }
//                        System.out.printf(
//                                "  %s (%s): %s (%s)\n",
//                                opcode,
//                                instr.mode,
//                                instr.param,
//                                num
//                        );
//                        System.out.println();
                    }
                    else if (line instanceof ASTbranch) {
                        ((ASTbranch) line).addBytes(this, scope, root);
                    }
//                    else if (line instanceof ASTlabel) {
//                        System.out.printf("%s: ($%04X)\n", ((ASTlabel) line).name, labelPc);
//                    }
                    else if (line instanceof ASTdirec) {
//                        System.out.printf("%s: ($%04X)\n", ((ASTlabel) line).name, pc);
                        List<ASTexp> args = ((ASTdirec) line).args;
                        Token name = ((ASTdirec) line).directive;
                        switch (name.value.toLowerCase()) {
                            case ".org": {
                                org(args.get(0).calcValue(pc, scope, root).getInt());
//                                System.out.printf("pc = $%04X\n", pc);
                            }
                            break;
                            case ".text": {
                                StringBuilder s = new StringBuilder();
                                for (ASTexp e : args) {
                                    s.append(e.calcValue(pc, scope, root).getString());
                                }
//                                System.out.printf("$%04X: ", pc);
                                for (int i = 0; i < s.length(); i++) {
                                    int c = s.charAt(i);
                                    add(c);
//                                    System.out.printf("%02X ", c);
                                }
//                                System.out.println();
//                                System.out.printf("pc = $%04X\n", pc);
                            }
                            break;
                            case ".string": {
                                StringBuilder s = new StringBuilder();
                                for (ASTexp e : args) {
                                    s.append(e.calcValue(pc, scope, root).getString());
                                }
                                s.append('\0');
//                                System.out.printf("$%04X: ", pc);
                                for (int i = 0; i < s.length(); i++) {
                                    int c = s.charAt(i);
//                                    System.out.printf("%02X ", c);
                                    add(c);
                                }
//                                System.out.println();
//                                System.out.printf("pc = $%04X\n", pc);
                            }
                            break;
                            case ".word": {
//                                System.out.printf("$%04X: ", pc);
                                for (ASTexp e : args) {
                                    ExpVal value = e.calcValue(pc, scope, root);
//                                    System.out.printf("%02X %02X ", value.getInt() & 0xFF, value.getInt() >>> 8);
                                    add(value.getInt() & 0xFF, value.getInt() >>> 8);
                                }
                                if (args.size() == 0) skip(2);
//                                System.out.println();
//                                System.out.printf("pc = $%04X\n",pc);
                            }
                            break;
                            case ".byte": {
//                                System.out.printf("$%04X: ", pc);
                                for (ASTexp e : args) {
                                    ExpVal value = e.calcValue(pc, scope, root);
                                    int v = value.getInt();
                                    if (v >= 256) {
                                        throw new ParseException(
                                                "Invalid argument to directive .byte (too large)",
                                                name.offset
                                        );
                                    }
//                                    System.out.printf("%02X ", value.getInt() & 0xFF);
                                    add(v & 0xFF);
                                }
                                if (args.size() == 0) skip(1);
//                                System.out.println();
//                                System.out.printf("pc = $%04X\n", pc);
                            }
                            break;
                            case ".data": {
                                skip(args.get(0).calcValue(pc, scope, root).getInt());
//                                System.out.printf("pc = $%04X\n", pc);
                            }
                            break;
                        }
                    }
                }
            }
            break;
        }

        return ram;
    }

    public static boolean instExists(String opcode, ASTinstr.AddrMode mode) {
//        System.out.printf("'%s' exists?\n", opcode + " " + mode);
        return opcodes.containsKey("_" + opcode.toUpperCase() + "_" + mode);
    }
    
    public static boolean instConstantExists(String constant) {
//        System.out.printf("'%s' exists?\n", opcode + " " + mode);
        return opcodes.containsKey(constant);
    }

    public static int getInstCode(String opcode, ASTinstr.AddrMode mode) {
        return opcodes.get("_" + opcode.toUpperCase() + "_" + mode);
    }

    public static int getInstConstantCode(String constant) {
        return opcodes.get(constant);
    }

    public void clearMemory() {
        for (int i = 0; i < 0x10000; i++) {
            ram[i] = 0x00;
        }
        pc = 0;
    }

    public void org(int newAddress) {
        pc = newAddress & 0xFFFF;
    }

    public void skip(int relAddress) {
        pc = (pc + relAddress) & 0xFFFF;
    }

    public void add(int b) {
        ram[pc] = b & 0xFF;
//        System.out.printf("%02X ", b);
        skip(1);
    }

    public void add(int ... bs) {
        for (int b : bs) {
            add(b);
        }
    }
}

public class Assembler {

    public static int[] assemble(String code) throws ParseException {

        List<Token> tokens = Token.tokenise(code);
        Parser parser = new Parser(tokens);

        ASTroot root = parser.parse();

        CodeGenerator generator = new CodeGenerator(root);

        return generator.generate();


    }
}