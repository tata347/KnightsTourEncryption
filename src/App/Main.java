package App;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        showPasswordDialog();
    }

    public static void showPasswordDialog() throws Exception {
        File fxmlFile = new File("src/Gui/PasswordDialog.fxml");
        Parent root = FXMLLoader.load(fxmlFile.toURI().toURL());

        Scene scene = new Scene(root, 350, 200);
        primaryStage.setTitle("Unlock Vault");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void showVault() throws Exception {
        File fxmlFile = new File("src/Gui/VaultDialog.fxml");
        Parent root = FXMLLoader.load(fxmlFile.toURI().toURL());

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setTitle("Encryption Vault");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}