package com.ciphertechsolutions.io.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * The controller for ION's info screen.
 */
public class InfoScreenController extends BaseController {

    @FXML
    private Button closeButton;
    @FXML
    private Label versionLabel;

    private static final String ION_VERSION = "20170906.0"; // TODO: Make this update via build script?

    @Override
    protected void setTitle() {
        setTitle("IO - About");
    }

    @Override
    protected void performSetup() {
        versionLabel.setText("Version: " + ION_VERSION);
    }

    @FXML
    void onCloseClick(ActionEvent event) {
        changeScene(loadFXML(MainScreenController.class, MainScreenController.getFXMLLocation()).getScene());
    }

    @FXML
    void onMailToTeam(ActionEvent event) {
        hostServices.showDocument("mailto:io@ciphertechsolutions.com");
    }

    @FXML
    void onGetWebsite(ActionEvent event) {
        hostServices.showDocument("https://www.ciphertechsolutions.com/open-source/");
    }

    /**
     * Get the location of this controller's corresponding FXML file.
     * 
     * @return the FXML file location.
     */
    public static String getFXMLLocation() {
        return "fxml/IONInfo.fxml";
    }

}
