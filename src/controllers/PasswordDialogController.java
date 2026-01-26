package controllers;

import App.Main;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;

public class PasswordDialogController {

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button unlockButton;

    @FXML
    private void handleUnlock() {
        String password = passwordField.getText();
        if (password.equals("1234")) {
            try{
                Main.showVault();
            }
            catch (Exception e){

            }
        }
    }
}