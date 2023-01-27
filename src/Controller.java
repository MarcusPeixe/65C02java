import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.*;
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
    int irqCounter = 0;
    boolean irqsEnabled = false;

    int monitorOffset = 0;
    boolean monitorChanged;
    boolean monitorMoved;
    boolean screenChanged;
    File lastDir;

    private GraphicsContext gc;

    @Override
    public int read(int addr, boolean sync) {
        return ram[addr] & 0xFF;
    }

    @Override
    public void write(int addr, int value) {
        ram[addr] = value & 0xFF;
        if (addr >= 0x0400 && addr <= 0x07FF) {
//            drawPixel(addr, value);
            screenChanged = true;
        }
        if (showDisassembly.isSelected() || (addr >= monitorOffset && addr < monitorOffset + 0x100)) {
            monitorChanged = true;
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
        gc.fillRect(0, 0, 512, 512);
        gc.setFont(new Font("monospace", 14));

        loop = new AnimationTimer()
        {
            public void handle(long currentNanoTime)
            {
                if (irqCounter <= 0 && irqsEnabled) {
                    chip.setInterruptRequest(true);
                    irqCounter = (int) periodic.getValue();
                }
                monitorChanged = true;
                monitorMoved = true;
                if ((int) freq.getValue() == 1000) {
                    executeInstruction(0x10000);
                }
                else {
                    executeInstruction((int) freq.getValue());
                }
                irqCounter--;
            }
        };

        freq.valueProperty().addListener((observableValue, number, t1) -> {
            if (t1.intValue() == 1000)
                freqCounter.setText("Speed: Maxed out!");
            else
                freqCounter.setText(String.format("Speed: %d instruction(s) per tick (60fps)", t1.intValue()));
        });

        periodic.valueProperty().addListener((observableValue, number, t1) -> {
            if (t1.intValue() == 0) {
                periodicCounter.setText("Periodic IRQs disabled.");
                irqsEnabled = false;
            }
            else {
                periodicCounter.setText(String.format("Periodic IRQs: %d tick delay.", t1.intValue()));
                irqsEnabled = true;
            }
        });

        monitorSlider.valueProperty().addListener((observableValue, number, t1) -> {
            if (showDisassembly.isSelected()) {
                if (monitorSlider.getValue() < 1) {
                    monitorSlider.setValue(1);
                }
            }
            else {
                if (monitorSlider.getValue() < 256) {
                    monitorSlider.setValue(256);
                }
            }
            monitorOffset = 0x10000 - (int) monitorSlider.getValue();
            monitorChanged = true;
            updateMonitor();
        });

        showDisassembly.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                monitorSlider.setBlockIncrement(1);
                monitorSlider.setSnapToTicks(true);
                if (monitorSlider.getValue() < 256) {
                    monitorSlider.setValue(256);
                }
            }
            else {
                monitorSlider.setBlockIncrement(256);
                monitorSlider.setSnapToTicks(true);
            }
            monitorOffset = 0x10000 - (int) monitorSlider.getValue();
//            monitorChanged = true;
            monitorMoved = true;
            updateMonitor();
        });

        screenChanged = false;
        monitorChanged = true;
        monitorMoved = true;
        updateMonitor();
        lastDir = null;

        try {
            FileInputStream properties = new FileInputStream("./res/state.properties");
            Properties p = new Properties();
            p.load(properties);
            String file = p.getProperty("currentfile");
            String caret = p.getProperty("caretposition");
            String scroll = p.getProperty("scroll");
            if (file != null) {
                updateLastDir(new File(file));
            }
            else {
                console.appendText("Error loading property 'currentfile'.\n");
            }

            File autosave = new File("./res/tempcode.s");
            loadFileToCode(autosave);

            if (caret != null) {
                codeEditor.positionCaret(Integer.parseInt(caret));
            }
            else {
                console.appendText("Error loading property 'caretposition'.\n");
            }
            if (scroll != null) {
                codeEditor.setScrollTop(Double.parseDouble(scroll));
            }
            else {
                console.appendText("Error loading property 'scroll'.\n");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            console.appendText("Error loading 'state.properties'");
        }
    }

    void onClose() {
        try {
            FileOutputStream properties = new FileOutputStream("./res/state.properties");
            Properties p = new Properties();
            if (lastDir != null)
                p.setProperty("currentfile", lastDir.getAbsolutePath());
            p.setProperty("caretposition", String.valueOf(codeEditor.getCaretPosition()));
            p.setProperty("scroll", String.valueOf(codeEditor.getScrollTop()));
            p.store(properties, "saved");

            File autosave = new File("./res/tempcode.s");
            saveFileFromCode(autosave);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    void drawPixel(int index, int value) {
        int offset = index - 0x0400;
        int x = offset % 32, y = offset / 32;
        value &= 0xFF;

        // int val;

        // val = ((value >> 0) & 0b11) * 0x55;
        // gc.setFill(Color.rgb(val, val, val));
        // gc.fillRect(x * 16 + 0, y * 16 + 0, 8, 8);

        // val = ((value >> 2) & 0b11) * 0x55;
        // gc.setFill(Color.rgb(val, val, val));
        // gc.fillRect(x * 16 + 8, y * 16 + 0, 8, 8);

        // val = ((value >> 4) & 0b11) * 0x55;
        // gc.setFill(Color.rgb(val, val, val));
        // gc.fillRect(x * 16 + 0, y * 16 + 8, 8, 8);
        
        // val = ((value >> 6) & 0b11) * 0x55;
        // gc.setFill(Color.rgb(val, val, val));
        // gc.fillRect(x * 16 + 8, y * 16 + 8, 8, 8);

        if (value < 16) {
            int r = 0, g = 0, b = 0;
            
            if (value < 8) {
                r = ((value & 1) != 0)? 0xAA: 0x00;
                g = ((value & 2) != 0)? 0xAA: 0x00;
                b = ((value & 4) != 0)? 0xAA: 0x00;
            }
            else if (value < 16) {
                r = ((value & 1) != 0)? 0xFF: 0x55;
                g = ((value & 2) != 0)? 0xFF: 0x55;
                b = ((value & 4) != 0)? 0xFF: 0x55;
            }

            Paint colour = Color.rgb(r, g, b);
            gc.setFill(colour);
            gc.fillRect(x * 16, y * 16, 16, 16);
        }
        else if (value < 32) {
            gc.setFill(Color.BLACK);
            gc.fillRect(x * 16, y * 16, 16, 16);
            
            gc.setFill(Color.WHITE);
            if ((value & 1) != 0)
                gc.fillRect(x * 16 + 0, y * 16 + 0, 8, 8);
            if ((value & 2) != 0)
                gc.fillRect(x * 16 + 8, y * 16 + 0, 8, 8);
            if ((value & 4) != 0)
                gc.fillRect(x * 16 + 0, y * 16 + 8, 8, 8);
            if ((value & 8) != 0)
                gc.fillRect(x * 16 + 8, y * 16 + 8, 8, 8);
        }
        else if (value < 127) {
            char c = (char) value;

            gc.setFill(Color.WHITE);
            gc.fillText(c + "", x * 16 + 1, y * 16 + 12, 14);
        }
        else {
            int r = 0, g = 0, b = 0;
            
            if (value < 192) {
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

            Paint colour = Color.rgb(r, g, b);
            gc.setFill(colour);
            gc.fillRect(x * 16, y * 16, 16, 16);
        }
    }

    private void loadFileToCode(File file) {
        FileInputStream input;
        try {
            input = new FileInputStream(file);
            StringBuilder text = new StringBuilder();
            while (true) {
                int c = input.read();
                if (c == -1) break;
                text.append((char) c);
            }

            console.setText(String.format("Text loaded from file \"%s\".\n", file));
            codeEditor.setText(text.toString());
            input.close();
        }
        catch (FileNotFoundException e) {
            console.setText(String.format("Error! File \"%s\" not found!\n", file));
            e.printStackTrace();
        }
        catch (IOException e) {
            console.setText(String.format("Error reading \"%s\".\n", file));
            e.printStackTrace();
        }
    }

    private void saveFileFromCode(File file) {
        String code = codeEditor.getText();
        FileOutputStream output;
        try {
            output = new FileOutputStream(file);
            output.write(code.getBytes());

            output.close();
            console.setText(String.format("Text saved to file \"%s\".\n", file));
        }
        catch (IOException e) {
            console.setText(String.format("Error! File \"%s\" not found!", file));
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
        s += String.format(
                "Ready? %s\n", chip.getReady()? "yes": "no"
        );
        s += String.format(
                "Stopped? %s\n", chip.getStopped()? "yes": "no"
        );
        console.setText(s);
    }

    public void display() {
//        String out = "+----------------------------------------------------------------+\n";
        if (!screenChanged) return;
        screenChanged = false;
        for (int i = 0x0400; i < 0x0800; i++) {
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
    private Label codelabel;
    
    @FXML
    private TextArea codeEditor;

    @FXML
    private Canvas screen;

    @FXML
    private Button binaryB;

    @FXML
    private Button loadB;

    @FXML
    private Button saveB;

    @FXML
    private Button saveAsB;

    @FXML
    private CheckBox loadAutomatically;

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
    private Slider freq;

    @FXML
    private Label freqCounter;

    @FXML
    private Slider periodic;

    @FXML
    private Label periodicCounter;

    @FXML
    private TextArea monitor;

    @FXML
    private Slider monitorSlider;

    @FXML
    private CheckBox keyIRQ;

    @FXML
    private CheckBox showDisassembly;

    @FXML
    void nmiTrigger() {
        chip.setNonMaskableInterrupt(true);
        console.appendText("NMI triggered!\n");
    }

    @FXML
    void irqTrigger() {
        chip.setInterruptRequest(true);
        console.appendText("IRQ triggered!\n");
    }

    @FXML
    void resTrigger() {
        chip.setReset(true);
//        System.out.printf("RESET SET! ready = %s\n", chip.getReady());
        console.appendText("RES triggered!\n");
    }

    @FXML
    void loadImageToMemory(ActionEvent event) {
        console.setText("");
        if (loadAutomatically.isSelected() && lastDir != null) {
            loadFileToCode(lastDir);
        }

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, 320, 320);

        String code = codeEditor.getText();
        try {
            ram = Assembler.assemble(code);
            console.appendText("Assembly successful!\n");
        }
        catch (ParseException e) {
            console.appendText(Token.printLineError(code, e));
        }
        stopCode();
        resTrigger();
        screenChanged = true;
        monitorChanged = true;
        if (!showDisassembly.isSelected()) {
            Disassembler.disassemble(chip, ram);
        }
        updateMonitor();
        display();
        event.consume();
    }

    @FXML
    void runCode() {
        chip.setReady(true);
        loop.start();
        irqCounter = (int) periodic.getValue();
    }

    @FXML
    void stepCode() {
        chip.setReady(true);
        monitorChanged = true;
        monitorMoved = true;
        executeInstruction(1);
        state();
    }

    private void updateMonitor() {
        if (!monitorChanged && !monitorMoved) return;
        monitor.clear();
        if (showDisassembly.isSelected()) {
//            monitor.appendText("Work in progress.\n");
            if (monitorChanged) {
                Disassembler.disassemble(chip, ram);
                monitor.appendText(Disassembler.scroll(chip, ram, monitorOffset));
            }
        }
        else {
            for (int i = 0; i < 0x10; i++) {
                monitor.appendText(String.format("$%04X: ", (i << 4) + (monitorOffset & 0xFFF0)));
                for (int j = 0; j < 0x10; j++) {
                    monitor.appendText(String.format("%02X", ram[(i << 4 | j) + (monitorOffset & 0xFFF0)]));
                    if (j < 0xf) {
                        monitor.appendText(" ");
                    }
                }
                if (i < 0xf) {
                    monitor.appendText("\n");
                }
            }
        }
        monitorChanged = false;
        monitorMoved = false;
    }

    private void executeInstruction(int amount) {
        for (int i = 0; i < amount; i++) {
            if (!chip.tickClock()) break;
        }
        display();
        updateMonitor();
        state();
    }

    @FXML
    void stopCode() {
        chip.setReady(false);
        loop.stop();
//        imageNotLoaded = true;
        console.appendText("Execution stopped!\n");
//        System.out.print("\033[H\033[2J");
//        System.out.flush();
//        memDump(0, 0x06FF, 16);
    }

    @FXML
    void monitorArrowKey(KeyEvent event) {
        switch (event.getCode()) {
            case UP:
            case PAGE_UP:
                if (showDisassembly.isSelected()) {
                    monitorSlider.setValue(monitorSlider.getValue() + 64);
                }
                else {
                    monitorSlider.setValue(monitorSlider.getValue() + monitorSlider.getBlockIncrement());
                }
                break;
            case DOWN:
            case PAGE_DOWN:
                if (showDisassembly.isSelected()) {
                    monitorSlider.setValue(monitorSlider.getValue() - 64);
                }
                else {
                    monitorSlider.setValue(monitorSlider.getValue() - monitorSlider.getBlockIncrement());
                }
                break;
            case LEFT:
                if (showDisassembly.isSelected()) {
                    monitorSlider.setValue(monitorSlider.getValue() + 1);
                }
                else {
                    monitorSlider.setValue(monitorSlider.getValue() + 16);
                }
                break;
            case RIGHT:
                if (showDisassembly.isSelected()) {
                    monitorSlider.setValue(monitorSlider.getValue() - 1);
                }
                else {
                    monitorSlider.setValue(monitorSlider.getValue() - 16);
                }
                break;
            default:
        }
    }

    @FXML
    void monitorScroll(ScrollEvent event) {
        double blocks = event.getDeltaY() * monitorSlider.getBlockIncrement() / event.getMultiplierY();
        if (showDisassembly.isSelected()) {
            monitorSlider.setValue(monitorSlider.getValue() + blocks * 4);
        }
        else {
            monitorSlider.setValue(monitorSlider.getValue() + blocks);
        }
    }

    @FXML
    void scrollBarScrolled(ScrollEvent event) {
        double blocks = event.getDeltaY() * monitorSlider.getBlockIncrement() / event.getMultiplierY();
        if (showDisassembly.isSelected()) {
            monitorSlider.setValue(monitorSlider.getValue() + blocks * 128);
        }
        else {
            monitorSlider.setValue(monitorSlider.getValue() + blocks * 4);
        }
    }

    @FXML
    void highlight(DragEvent event) {
//        codeEditor.setStyle("-fx-font-family: monospace; -fx-border-color: blue; -fx-border-width: 3; -fx-border-radius: 10; -fx-control-inner-background: black; -fx-text-fill: white; -fx-font-size: 16; -fx-prompt-text-fill: gray; -fx-highlight-fill: orange;");
        codeEditor.setStyle("-fx-font-family: monospace; -fx-border-color: lime; -fx-border-width: 10; -fx-border-radius: 10; -fx-control-inner-background: black; -fx-text-fill: white; -fx-font-size: 16; -fx-prompt-text-fill: gray; -fx-highlight-fill: orange;");
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.ANY);
        }
        event.consume();
    }

    @FXML
    void unHighlight(DragEvent event) {
        codeEditor.setStyle("-fx-font-family: monospace; -fx-border-color: blue; -fx-border-width: 3; -fx-border-radius: 10; -fx-control-inner-background: black; -fx-text-fill: white; -fx-font-size: 16; -fx-prompt-text-fill: gray; -fx-highlight-fill: orange;");
//        codeEditor.setStyle("-fx-font-family: monospace; -fx-border-color: lime; -fx-border-width: 10; -fx-border-radius: 10; -fx-control-inner-background: black; -fx-text-fill: white; -fx-font-size: 16; -fx-prompt-text-fill: gray; -fx-highlight-fill: orange;");
        event.consume();
    }

    @FXML
    void loadDroppedFile(DragEvent event) {
//        unHighlight(null);
//        java.util.List<java.io.File> files = event.getDragboard().getFiles();
        File file = event.getDragboard().getFiles().get(0);
        updateLastDir(file);
        loadFileToCode(file);
        event.consume();
    }

    @FXML
    void keyPressed(KeyEvent event) {
//        System.out.printf("KeyEvent: %s\n", event.toString());
        int c = event.getCharacter().charAt(0);
        if (c == '\r') c = '\n';
        ram[0xFF] = c;
        console.appendText(String.format(
                "Key code: %d\n", c
        ));
        if (keyIRQ.isSelected()) {
            irqTrigger();
        }
        monitorChanged = true;
        updateMonitor();
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
        updateLastDir(file);
        loadFileToCode(file);
        event.consume();
    }

    @FXML
    void saveFile(ActionEvent event) {
        if (lastDir != null) {
            saveFileFromCode(lastDir);
        }
        else {
            saveAsFile(event);
            return;
        }
        event.consume();
    }

    @FXML
    void saveAsFile(ActionEvent event) {
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
        updateLastDir(file);
        saveFileFromCode(file);
        event.consume();
    }

    void updateLastDir(File file) {
        lastDir = file;
        codelabel.setText("Code editor - " + file.getName());
    }

//    @FXML
    void editorHandle(KeyEvent event) {
        KeyCode k = event.getCode();
        if (k == KeyCode.ENTER) {
            int caret = codeEditor.getCaretPosition();
            double offset = codeEditor.getScrollTop();
            String before = codeEditor.getText(0, caret);
            String after = codeEditor.getText(caret, codeEditor.getLength());

            String[] lines = before.split("\n", -1);
            String lastLine = lines[lines.length - 2];
            StringBuilder spaces = new StringBuilder();
            for (int i = 0; i < lastLine.length(); i++) {
                char c = lastLine.charAt(i);
                if (Character.isWhitespace(c))
                    spaces.append(c);
                else break;
            }
            if (lastLine.length() > 1 && lastLine.charAt(lastLine.length() - 1) == ':') {
                spaces.append("  ");
            }
            codeEditor.setText(
                    before + spaces + after
            );
            codeEditor.positionCaret(caret + spaces.length());
            codeEditor.setScrollTop(offset);
        }
        event.consume();
    }
}
