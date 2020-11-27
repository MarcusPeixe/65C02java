import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gui.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();
        primaryStage.setTitle("65C02 Emulator");
        primaryStage.setScene(new Scene(root, 1300, 800));
        primaryStage.setOnCloseRequest(handler -> controller.onClose());
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
