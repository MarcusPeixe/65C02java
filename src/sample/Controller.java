package sample;

import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.*;
import java.text.ParseException;
import java.util.Properties;

public class Controller implements Mapper {

    Chip65C02 chip;

    int[] ram;

    AnimationTimer loop;

    boolean updateScreen;
    File lastDir;

    private GraphicsContext gc;

    @Override
    public int read(int addr, boolean sync) {
//        if (sync) readInstruction = true;
//        System.out.println("bbb" + sync);
        return ram[addr] & 0xFF;
    }

    @Override
    public void write(int addr, int value) {
        ram[addr] = value & 0xFF;
        if (addr >= 0x0200 && addr <= 0x05FF) {
//            drawPixel(addr, value);
            updateScreen = true;
        }
    }

    @Override
    public void onVectorPull(int addr) {

    }

    @FXML
    void initialize() {
        ram = new int[0x10000];
        for (int i = 0; i < 0x10000; i++) {
            ram[i] = 0;
        }
        chip = new Chip65C02(this);
        chip.setDebug(false);
        chip.setReset(true);

        gc = screen.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, 320, 320);
        gc.setFont(new Font(10));

        loop = new AnimationTimer()
        {
            public void handle(long currentNanoTime)
            {
                executeInstruction(0x10000);
            }
        };

        updateScreen = false;
        lastDir = null;

        try {
            FileInputStream properties = new FileInputStream("res/state.properties");
            Properties p = new Properties();
            p.load(properties);
            String file = p.getProperty("currentfile");
            if (file != null) {
                lastDir = new File(file);
            }

            File autosave = new File("res/tempcode.s");
            loadFileToCode(autosave);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    void onClose() {
        try {
            FileOutputStream properties = new FileOutputStream("res/state.properties");
            Properties p = new Properties();
            p.setProperty("currentfile", lastDir.getAbsolutePath());
            p.store(properties, "saved");

            File autosave = new File("res/tempcode.s");
            saveFileFromCode(autosave);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    void drawPixel(int index, int value) {
        value &= 0xFF;
        int r = 0, g = 0, b = 0;
        char c = ' ';
        if (value < 8) {
            r = ((value & 1) != 0)? 0x88: 0x00;
            g = ((value & 2) != 0)? 0x88: 0x00;
            b = ((value & 4) != 0)? 0x88: 0x00;
        }
        else if (value < 16) {
            r = ((value & 1) != 0)? 0xCC: 0x00;
            g = ((value & 2) != 0)? 0xCC: 0x00;
            b = ((value & 4) != 0)? 0xCC: 0x00;
        }
        else if (value < 24) {
            r = ((value & 1) != 0)? 0xFF: 0x88;
            g = ((value & 2) != 0)? 0xFF: 0x88;
            b = ((value & 4) != 0)? 0xFF: 0x88;
        }
        else if (value < 32) {
            r = ((value & 1) != 0)? 0xFF: 0x44;
            g = ((value & 2) != 0)? 0xFF: 0x44;
            b = ((value & 4) != 0)? 0xFF: 0x44;
        }
        else if (value < 127) {
            c = (char) value;
        }
        else if (value < 192) {
            r = ((value) & 3) * 0x55;
            g = ((value >> 2) & 3) * 0x55;
            b = ((value >> 4) & 3) * 0x55;
        }
        else {
            int greyscale = (value - 192) * 4;
            r = greyscale;
            g = greyscale;
            b = greyscale;
        }
        int offset = index - 0x0200;
        int x = offset % 32, y = offset / 32;
        Paint colour = Color.rgb(r, g, b);
        gc.setFill(colour);
        gc.fillRect(x * 10, y * 10, 10, 10);
        gc.setFill(Color.WHITE);
        gc.fillText(c + "", x * 10, y * 10 + 8);
    }

    private void loadFileToCode(File file) {
        FileInputStream input;
//        System.out.println("TEST");
//        System.out.printf("File \"%s\"\n", file.toString());
        try {
            input = new FileInputStream(file);
            String text = new String(input.readAllBytes());
//            int linebreak = 0;
//            while (true) {
//                int b = 0;
//                b = input.read();
//                if (b == -1) break;
//                String literal = String.format("%02X ", b);
////                while (literal.length() < 2) {
////                    final String lookup = "0123456789ABCDEF";
////                    literal = lookup.charAt(b % 16) + literal;
////                    b = b / 16;
////                }
////                System.out.printf("Number: %s\n", literal);
//                if (linebreak > 7) {
//                    text += "\n";
//                    linebreak = 0;
//                }
//                linebreak++;
//                text += literal;
//            }
            console.setText(String.format("Text loaded from file \"%s\".\n", file.toString()));
            binaryEditor.setText(text);
            input.close();
        }
        catch (FileNotFoundException e) {
            console.setText(String.format("Error! File \"%s\" not found!\n", file.toString()));
            e.printStackTrace();
        }
        catch (IOException e) {
            console.setText(String.format("Error reading \"%s\".\n", file.toString()));
            e.printStackTrace();
        }
    }

    private void saveFileFromCode(File file) {
        String code = binaryEditor.getText();
//        System.out.printf("Text:\n%s\n", code);
        FileOutputStream output;
        try {
            output = new FileOutputStream(file);
            output.write(code.getBytes());
//            for (String s : strings) {
//                output.write(Integer.parseInt(s.trim(), 16));
////                System.out.printf("String parsed and written: \"%s\" result: %02X;\n", s.trim(), Integer.parseInt(s.trim(), 16));
////                System.out.printf("result: \"%s\"\n", s.trim());
//            }
            output.close();
        }
        catch (IOException e) {
            console.setText(String.format("Error! File \"%s\" not found!", file.toString()));
            e.printStackTrace();
        }
    }

    public void state() {
        String s =
        String.format(
                "PC = $%04X / PS = $%02X / SP = $%02X\n",
                chip.getProgramCounter(),
                chip.getStatusFlags(),
                chip.getStackPointer()
        );
        s += String.format(
                "ACC = $%02X / X = $%02X / Y = $%02X\n",
                chip.getAccumulator(),
                chip.getRegisterX(),
                chip.getRegisterY()
        );
        s += "NV-B DIZC (PROCESSOR STATUS FLAGS)\n";
        s += String.format(
                "%s\n", Chip65C02.asBinary(chip.getStatusFlags(), 8)
        );
//        s += String.format(
//                "READY = %s\n", chip.getReady()? "yes": "no"
//        );
//        s += String.format(
//                "STOPPED = %s\n", chip.getStopped()? "yes": "no"
//        );
        console.setText(s);
    }

    public void display() {
//        String out = "+----------------------------------------------------------------+\n";
        if (!updateScreen) return;
        updateScreen = false;
        for (int i = 0x0200; i < 0x0600; i++) {
            drawPixel(i, ram[i]);
        }
    }

    public void memDump(int start, int end, int columns) {
        if (columns < 1) return;
        int startCbytes = start / columns * columns;
        int endCbytes = (end + columns - 1) / columns * columns;
        for (int i = startCbytes; i < start; i++) {
            if (i % 256 == 0)
                System.out.print("\nPAGE CROSS: ");
            if (i % columns == 0)
                System.out.printf("\n$%04X = ", i);
            System.out.print("-- ");
        }
        for (int i = start; i <= end; i++) {
            if (i % 256 == 0)
                System.out.print("\nPAGE CROSS: ");
            if (i % columns == 0)
                System.out.printf("\n$%04X = ", i);
            System.out.printf("\033[%dm%02X \033[0m", 40 + (ram[i] & 0x07), ram[i]);
        }
        for (int i = end + 1; i < endCbytes; i++) {
            if (i % 256 == 0)
                System.out.print("\nPAGE CROSS: ");
            if (i % columns == 0)
                System.out.printf("\n$%04X = ", i);
            System.out.print("-- ");
        }
        System.out.println();
    }

    @FXML
    private TextArea binaryEditor;

    @FXML
    private Canvas screen;

    @FXML
    private Button binaryB;

    @FXML
    private Button loadB;

    @FXML
    private Button saveB;

    @FXML
    private Button runB;

    @FXML
    private Button stopB;

    @FXML
    private Button stepB;

    @FXML
    private Button RES;

    @FXML
    private Button IRQ;

    @FXML
    private Button NMI;

    @FXML
    private TextArea console;

    @FXML
    void nmiTrigger(ActionEvent event) {
        chip.setNonMaskableInterrupt(true);
        console.appendText("NMI trigger!\n");
    }

    @FXML
    void irqTrigger(ActionEvent event) {
        chip.setInterruptRequest(true);
        console.appendText("IRQ trigger!\n");
    }

    @FXML
    void resTrigger(ActionEvent event) {
        chip.setReset(true);
//        System.out.printf("RESET SET! ready = %s\n", chip.getReady());
        console.appendText("RES trigger!\n");
    }

    @FXML
    void loadImageToMemory() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, 320, 320);
//        Process process;
//        byte[] binary;
//        try {
//            File source = new File("out/temp/input.s");
//            saveFileFromCode(source);
//            process = new ProcessBuilder(
//                    "out/production/65C02java-master/sample/vasm6502_oldstyle.exe",
//                    "-Fbin",
//                    "-chklabels",
//                    "-wdc02",
//                    "-esc",
//                    "-opt-branch",
//                    "-dotdir",
//                    "out/temp/input.s",
//                    "-o",
//                    "out/temp/output.out"
//            ).start();
//            process.waitFor();
//            int exitStatus = process.exitValue();
//            if (exitStatus != 0) {
////                byte[] out = process.getInputStream().readAllBytes();
//                byte[] err = process.getErrorStream().readAllBytes();
//                console.setText(String.format(
//                        "Exit code: %d\nErrors:\n%s\n", exitStatus, new String(err)
//                ));
//                return;
//            }
//            binary = (new FileInputStream(new File("out/temp/output.out"))).readAllBytes();
//            console.setText(String.format(
//                    "Assembly successful! %d (0x%04X) bytes.\n", binary.length, binary.length
//            ));
//        }
//        catch (Exception e) {
//            console.setText(String.format(
//                    "Assembly failed :(\nException:\n%s\n", e.toString()
//            ));
//            return;
//        }
//        if (binary.length == 0x10000) {
//            for (int i = 0; i < 0x10000; i++) {
//                ram[i] = (int) binary[i] & 0xFF;
//            }
//        }
//        else if (binary.length + 0x0600 <= 0x10000) {
//            for (int i = 0; i < 0x10000; i++) {
//                ram[i] = 0x00;
//            }
//            ram[0xFFFA] = 0x00;
//            ram[0xFFFB] = 0x06;
//            ram[0xFFFC] = 0x00;
//            ram[0xFFFD] = 0x06;
//            ram[0xFFFE] = 0x00;
//            ram[0xFFFF] = 0x06;
//            for (int i = 0; i < binary.length; i++) {
//                ram[i + 0x0600] = (int) binary[i] & 0xFF;
//            }
//        }
//        else {
//            console.appendText(String.format(
//                    "Error while loading binary! Code too big. Expected %d (0x%04X) bytes, found %d (0x%04X).\n",
//                    0x10000, 0x10000, binary.length + 0x0600, binary.length + 0x0600
//            ));
//        }
        String code = binaryEditor.getText();
        try {
            ram = Assembler.assemble(code);
            console.setText("Assembly successful!\n");
        }
        catch (ParseException e) {
            console.setText(Token.printLineError(code, e));
        }
        stopCode(null);
        resTrigger(null);
        updateScreen = true;
        display();
    }

    @FXML
    void runCode(ActionEvent event) {
        chip.setReady(true);
        loop.start();
    }

    @FXML
    void stepCode(ActionEvent event) {
        chip.setReady(true);
        executeInstruction(1);
//        state();
    }

    private void executeInstruction(int amount) {
        for (int i = 0; i < amount; i++) {
            if (!chip.tickClock()) break;
        }
        display();
        state();
    }

    @FXML
    void stopCode(ActionEvent event) {
        chip.setReady(false);
        loop.stop();
//        imageNotLoaded = true;
        console.appendText("Execution stopped!\n");
        System.out.print("\033[H\033[2J");
        System.out.flush();
        memDump(0, 0x06FF, 16);
    }

    @FXML
    void highlight(DragEvent event) {
//        codeEditor.setStyle("-fx-font-family: monospace; -fx-border-color: blue; -fx-border-width: 3; -fx-border-radius: 10; -fx-control-inner-background: black; -fx-text-fill: white; -fx-font-size: 16; -fx-prompt-text-fill: gray; -fx-highlight-fill: orange;");
        binaryEditor.setStyle("-fx-font-family: monospace; -fx-border-color: lime; -fx-border-width: 10; -fx-border-radius: 10; -fx-control-inner-background: black; -fx-text-fill: white; -fx-font-size: 16; -fx-prompt-text-fill: gray; -fx-highlight-fill: orange;");
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.ANY);
        }
        event.consume();
    }

    @FXML
    void unHighlight(DragEvent event) {
        binaryEditor.setStyle("-fx-font-family: monospace; -fx-border-color: blue; -fx-border-width: 3; -fx-border-radius: 10; -fx-control-inner-background: black; -fx-text-fill: white; -fx-font-size: 16; -fx-prompt-text-fill: gray; -fx-highlight-fill: orange;");
//        codeEditor.setStyle("-fx-font-family: monospace; -fx-border-color: lime; -fx-border-width: 10; -fx-border-radius: 10; -fx-control-inner-background: black; -fx-text-fill: white; -fx-font-size: 16; -fx-prompt-text-fill: gray; -fx-highlight-fill: orange;");
        event.consume();
    }

    @FXML
    void loadDroppedFile(DragEvent event) {
//        unHighlight(null);
//        java.util.List<java.io.File> files = event.getDragboard().getFiles();
        File file = event.getDragboard().getFiles().get(0);
        lastDir = file;
        loadFileToCode(file);
        event.consume();
    }

    @FXML
    void keyPressed(KeyEvent event) {
//        System.out.printf("KeyEvent: %s\n", event.toString());
        int c = event.getCharacter().charAt(0);
        ram[0xFF] = c;
        console.appendText(String.format(
                "Key code: %d\n", c
        ));
    }

    @FXML
    void requestFocusConsole(MouseEvent event) {
        console.requestFocus();
    }

    @FXML
    void loadFile(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load the source code in assembly from a file");
        if (lastDir != null)
            fc.setInitialDirectory(lastDir.getParentFile());
//        System.out.printf("%s\n", event.getSource().toString());
        Window mainStage = ((Node)event.getSource()).getScene().getWindow();
        File file = fc.showOpenDialog(mainStage);
        if (file == null) {
            console.appendText("Load file cancelled.\n");
            return;
        }
        lastDir = file;
        loadFileToCode(file);
        event.consume();
    }

    @FXML
    void saveFile(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save the source code in assembly to a file");
        if (lastDir != null)
            fc.setInitialDirectory(lastDir.getParentFile());
//        System.out.printf("%s\n", event.getSource().toString());
        Window mainStage = ((Node)event.getSource()).getScene().getWindow();
        File file = fc.showSaveDialog(mainStage);
        if (file == null) {
            console.appendText("Save file cancelled.\n");
            return;
        }
        lastDir = file;
        saveFileFromCode(file);
        event.consume();
    }
}
