package com.ciphertechsolutions.io.ui;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;

/**
 * The controller for ION's main screen.
 */
public class MainScreenController extends BaseController {

    @FXML
    private Button advancedOptionsButton;
    @FXML
    private Button infoButton;
    @FXML
    private Button manuallySelectDeviceButton;
    @FXML
    private TextArea outputTextArea;
    @FXML
    private Label writeblockStatus;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label readyLabel;
    private ChangeListener<Boolean> writeBlockListener;

    /**
     * Get the location of this controller's corresponding FXML file.
     * @return the FXML file location.
     */
    public static String getFXMLLocation() {
        return "fxml/IONMain.fxml";
    }

    @FXML
    void onManuallySelectDeviceClick(ActionEvent event) {
        changeScene(loadFXML(SelectDeviceController.class, SelectDeviceController.getFXMLLocation()).getScene());
    }

    @FXML
    void onAdvancedOptionsClick(ActionEvent event) {
        changeScene(loadFXML(AdvancedOptionsController.class, AdvancedOptionsController.getFXMLLocation()).getScene());
    }

    @FXML
    void onInfoClick(ActionEvent event) {
        changeScene(loadFXML(InfoScreenController.class, InfoScreenController.getFXMLLocation()).getScene());
    }

    private ChangeListener<Boolean> getWriteblockListener() {
        writeBlockListener = (observable, oldValue, newValue) -> Platform.runLater(() -> {
            if (newValue) {
                writeblockStatus.setText("ENABLED");
                writeblockStatus.setTextFill(Color.LIME);
                readyLabel.setVisible(true);
                writeblockStatus.autosize();
            }
            else {
                writeblockStatus.setText("DISABLED");
                writeblockStatus.setTextFill(Color.RED);
                readyLabel.setVisible(false);
                writeblockStatus.autosize();
            }
        });
        return new WeakChangeListener<>(writeBlockListener);
    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        setTitle();
    }

    @Override
    protected void setTitle() {
        setTitle("IO");
    }

    @Override
    protected void performSetup() {
        writeblockStatus.setText("ENABLED");
        writeblockStatus.setTextFill(Color.LIME);
        writeblockStatus.autosize();
        workflowController.registerWriteBlockListener(getWriteblockListener());
        if (!workflowController.checkValidSaveDirectory()) {
            displayErrorPopup("Your currently selected save location is invalid, please select a new save location.");
            Platform.runLater(() -> changeScene(loadFXML(AdvancedOptionsController.class, AdvancedOptionsController.getFXMLLocation()).getScene()));
        }
    }

}
