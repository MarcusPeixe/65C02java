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
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Mapper {

    Chip65C02 chip;

    int[] ram;

    AnimationTimer loop;

    @Override
    public int read(int addr, boolean sync) {
//        if (sync) readInstruction = true;
//        System.out.println("bbb" + sync);
        return ram[addr] & 0xFF;
    }

    @Override
    public void write(int addr, int value) {
        ram[addr] = (byte)(value & 0xFF);
    }

    @Override
    public void onVectorPull(int addr) {

    }

    private GraphicsContext gc;

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
//        for (int i = 0; i < 10; i++) {
//            drawPixel(i, i, Color.BLUE);
//        }
//        screen.setDisable(true);
//        screen.setVisible(false);

//        System.out.println(gc.getFill() == Color.BLACK);
        loop = new AnimationTimer()
        {
            public void handle(long currentNanoTime)
            {
                executeInstruction(1000);
                // background image clears canvas
//                gc.drawImage( space, 0, 0 );
//                gc.drawImage( earth, x, y );
//                gc.drawImage( sun, 196, 196 );
            }
        };
    }

    void drawPixel(int x, int y, Color colour, char c) {
        gc.setFill(colour);
//        gc.setStroke(Color.TRANSPARENT);
        gc.fillRect(x * 10, y * 10, 10, 10);
        gc.setFill(Color.WHITE);
        gc.fillText(c + "", x * 10 + 2, y * 10 + 8);
    }

    private void loadFileToCode(File file) {
        FileInputStream input;
//        System.out.println("TEST");
        System.out.printf("File \"%s\"\n", file.toString());
        try {
            input = new FileInputStream(file);
            String text = "";
            int linebreak = 0;
            while (true) {
                int b = 0;
                b = input.read();
                if (b == -1) break;
                String literal = String.format("%02X ", b);
//                while (literal.length() < 2) {
//                    final String lookup = "0123456789ABCDEF";
//                    literal = lookup.charAt(b % 16) + literal;
//                    b = b / 16;
//                }
//                System.out.printf("Number: %s\n", literal);
                if (linebreak > 7) {
                    text += "\n";
                    linebreak = 0;
                }
                linebreak++;
                text += literal;
            }
            System.out.printf("Text loaded: %s\n", text);
            binaryEditor.setText(text);
            input.close();
        }
        catch (FileNotFoundException e) {
            System.out.printf("Error! File \"%s\" not found!\n", file.toString());
            e.printStackTrace();
        }
        catch (IOException e) {
            System.out.printf("Error! Reading \"%s\"\n", file.toString());
            e.printStackTrace();
        }
    }

    private void saveFileFromCode(File file) {
        String[] strings = binaryEditor.getText().split("\\s+");
        FileOutputStream output;
        try {
            output = new FileOutputStream(file);
            for (String s : strings) {
                output.write(Integer.parseInt(s.trim(), 16));
//                System.out.printf("String parsed and written: \"%s\" result: %02X;\n", s.trim(), Integer.parseInt(s.trim(), 16));
//                System.out.printf("result: \"%s\"\n", s.trim());
            }
            output.close();
        }
        catch (IOException e) {
            System.out.printf("Error! File \"%s\" not found!", file.toString());
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
        s += String.format("NV-B DIZC (PROCESSOR STATUS FLAGS)\n");
        s += String.format(
                "%s\n", Chip65C02.asBinary(chip.getStatusFlags(), 8)
        );
        s += String.format(
                "READY = %s\n", chip.getReady()? "true": "false"
        );
        s += String.format(
                "STOPPED = %s\n", chip.getStopped()? "true": "false"
        );
        console.setText(s);
    }

    public void display() {
//        String out = "+----------------------------------------------------------------+\n";
        for (int i = 0x0200; i < 0x0600; i++) {
            int value = ram[i] & 0xFF;
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
                r = ((value >> 0) & 3) * 0x55;
                g = ((value >> 2) & 3) * 0x55;
                b = ((value >> 4) & 3) * 0x55;
            }
            else {
                int greyscale = (value - 192) * 4;
                r = greyscale;
                g = greyscale;
                b = greyscale;
            }
            int offset = i - 0x0200;
            int x = offset % 32, y = offset / 32;
            drawPixel(x, y, Color.rgb(r, g, b), c);
        }
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
    }

    @FXML
    void irqTrigger(ActionEvent event) {
        chip.setInterruptRequest(true);
    }

    @FXML
    void resTrigger(ActionEvent event) {
        chip.setReset(true);
        System.out.printf("RESET SET! ready = %s\n", chip.getReady());
    }

    @FXML
    void loadImageToMemory() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, 320, 320);
        String[] nums = binaryEditor.getText().split("\\s+");
//        System.out.printf("nums = %s\n", nums);
        for (int i = 0; i < 0x10000; i++) {
            ram[i] = 0x00;
        }
        for (int i = 0; i < nums.length; i++) {
            int num = 0;
            try {
                num = Integer.parseInt(nums[i].trim(), 16) & 0xFF;
                //            System.out.printf("%02X ", ram[(i + 0x0600) & 0xFFFF]);
            }
            catch (NumberFormatException e) {
                System.out.printf("Number Format Exception: at number %d: number \"%s\"\n", i, nums[i].trim());
                System.out.println(e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
//            System.out.printf("at number %d: number \"%s\": int %02X\n", i, nums[i].trim(), num);
            ram[(i + 0x0600) & 0xFFFF] = num;
        }
//        System.out.println();
        ram[0xFFFA] = 0x00;
        ram[0xFFFB] = 0x06;
        ram[0xFFFC] = 0x00;
        ram[0xFFFD] = 0x06;
        ram[0xFFFE] = 0x1D;
        ram[0xFFFF] = 0x06;
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
            for (int j = 0; j < 10; j++) {
                if (chip.tickClock()) break;
            }
        }
        display();
        state();
    }

    @FXML
    void stopCode(ActionEvent event) {
        chip.setReady(false);
        loop.stop();
//        imageNotLoaded = true;
    }

    @FXML
    void highlight(DragEvent event) {
//        codeEditor.setStyle("-fx-font-family: monospace; -fx-border-color: orange; -fx-border-width: 3; -fx-border-radius: 10; -fx-control-inner-background: black; -fx-text-fill: white; -fx-font-size: 16; -fx-prompt-text-fill: gray; -fx-highlight-fill: orange;");
        binaryEditor.setStyle("-fx-font-family: monospace; -fx-border-color: lime; -fx-border-width: 10; -fx-border-radius: 10; -fx-control-inner-background: black; -fx-text-fill: white; -fx-font-size: 16; -fx-prompt-text-fill: gray; -fx-highlight-fill: orange;");
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.ANY);
        }
        event.consume();
    }

    @FXML
    void unHighlight(DragEvent event) {
        binaryEditor.setStyle("-fx-font-family: monospace; -fx-border-color: orange; -fx-border-width: 3; -fx-border-radius: 10; -fx-control-inner-background: black; -fx-text-fill: white; -fx-font-size: 16; -fx-prompt-text-fill: gray; -fx-highlight-fill: orange;");
//        codeEditor.setStyle("-fx-font-family: monospace; -fx-border-color: lime; -fx-border-width: 10; -fx-border-radius: 10; -fx-control-inner-background: black; -fx-text-fill: white; -fx-font-size: 16; -fx-prompt-text-fill: gray; -fx-highlight-fill: orange;");
        event.consume();
    }

    @FXML
    void loadDroppedFile(DragEvent event) {
//        unHighlight(null);
//        java.util.List<java.io.File> files = event.getDragboard().getFiles();
        File file = event.getDragboard().getFiles().get(0);
        loadFileToCode(file);
        event.consume();
    }

    @FXML
    void loadFile(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load a source code file in assembly");
//        System.out.printf("%s\n", event.getSource().toString());
        Window mainStage = ((Node)event.getSource()).getScene().getWindow();
        File file = fc.showOpenDialog(mainStage);
        loadFileToCode(file);
        event.consume();
    }

    @FXML
    void saveFile(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load a source code file in assembly");
//        System.out.printf("%s\n", event.getSource().toString());
        Window mainStage = ((Node)event.getSource()).getScene().getWindow();
        File file = fc.showSaveDialog(mainStage);
        saveFileFromCode(file);
        event.consume();
    }

}
