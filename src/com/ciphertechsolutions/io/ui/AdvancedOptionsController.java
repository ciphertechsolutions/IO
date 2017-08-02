package com.ciphertechsolutions.io.ui;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import com.ciphertechsolutions.io.applicationLogic.options.AdvancedOptions;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

/**
 * The options screen controller.
 */
public class AdvancedOptionsController extends BaseController {

    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button deleteButton;
    @FXML
    private ChoiceBox<String> selectConfigDDL;
    @FXML
    private TextField saveLocationField;
    @FXML
    private ChoiceBox<String> compressionTypeDDL;
    @FXML
    private TextField descriptionField;
    @FXML
    private TextField examinerNameField;
    @FXML
    private TextField caseNumberField;
    @FXML
    private TextField evidenceNumberField;
    @FXML
    private TextArea notesField;
    @FXML
    private TextField configNameField;

    private boolean wasSaveSet = false;

    private AdvancedOptions selectedConfig;

    /**
     * Sole constructor. Initializes with most recently saved configuration.
     */
    public AdvancedOptionsController() {
        super();
        getSelectedConfig();
    }

    private AdvancedOptions getSelectedConfig() {
        if (selectedConfig == null) {
            selectedConfig = AdvancedOptions.loadMostRecentConfig();
        }
        return selectedConfig;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setTitle();
        populateDDL(compressionTypeDDL, AdvancedOptions.getCompressionTypes());
        populateDDL(selectConfigDDL, AdvancedOptions.getConfigNames());
        setDisplayToSelectedConfig();
        selectConfigDDL.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    if (newValue != null && !newValue.isEmpty()) {
                        selectedConfig = AdvancedOptions.loadConfigByName(newValue);
                        workflowController.setOptions(selectedConfig);
                        setDisplayToSelectedConfig();
                    }
                });
    }

    private void setDisplayToSelectedConfig() {
        compressionTypeDDL.getSelectionModel().select(selectedConfig.getCompressionLevelName());
        selectConfigDDL.getSelectionModel().select(selectedConfig.getName());
        saveLocationField.setText(selectedConfig.getOutputFolder());
        descriptionField.setText(selectedConfig.getCaseDescription());
        examinerNameField.setText(selectedConfig.getExaminerName());
        notesField.setText(selectedConfig.getCaseNotes());
        evidenceNumberField.setText(selectedConfig.getEvidenceNumber());
        caseNumberField.setText(selectedConfig.getCaseNumber());
        configNameField.setText(selectedConfig.getName());
    }

    private static void populateDDL(ChoiceBox<String> dropDownList, Collection<String> items) {
        List<String> list = new ArrayList<>(items);
        ObservableList<String> dropDownItems = FXCollections.observableList(list);
        dropDownList.setItems(dropDownItems);
    }

    @FXML
    void handleOnDeleteAction(ActionEvent event) {
        String toDelete = selectedConfig.getName();
        AdvancedOptions.deleteConfigByName(toDelete);
        selectConfigDDL.getItems().remove(toDelete);
    }

    @FXML
    void handleOnSaveAction(ActionEvent event) {
        String compression = compressionTypeDDL.getSelectionModel().getSelectedItem();
        if (compression == null || configNameField.getText().equals("")) {
            displayErrorPopup("Unable to save. Compression, and name must be filled out.");

        }
        else {
            String saveLocation = saveLocationField.getText().endsWith("\\") ? saveLocationField.getText() : saveLocationField.getText() + "\\";
            AdvancedOptions options = new AdvancedOptions(configNameField.getText(), saveLocation, compression,
                    descriptionField.getText(), examinerNameField.getText(), caseNumberField.getText(),
                    evidenceNumberField.getText(), notesField.getText());
            workflowController.saveOptions(options);
            selectedConfig = options;
            changeScene(loadFXML(MainScreenController.class, MainScreenController.getFXMLLocation()).getScene());
        }
    }

    @FXML
    void openDirectoryChooser(Event event) {
        getDirectoryFromChooser();
    }

    protected void getDirectoryFromChooser() {
        wasSaveSet = true;
        DirectoryChooser dirChooser = new DirectoryChooser();
        File saveDirectory = dirChooser.showDialog(primaryStage);
        if (saveDirectory != null) {
            saveLocationField.setText(saveDirectory.getAbsolutePath());
        }
    }

    @FXML
    void handleOnCancelAction(ActionEvent event) {
        changeScene(loadFXML(MainScreenController.class, MainScreenController.getFXMLLocation()).getScene());
    }

    /**
     * Get the location of this controller's corresponding FXML file.
     * @return the FXML file locatiion.
     */
    public static String getFXMLLocation() {
        return "fxml/IONOptions.fxml";
    }

    @Override
    protected void performSetup() {
        saveLocationField.focusedProperty().addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
            if (newValue) {
                if (!wasSaveSet) {
                    getDirectoryFromChooser();
                }
                else {
                    wasSaveSet = false;
                }
            }
        });
    }

    @Override
    protected void setTitle() {
        setTitle("IO - Advanced Options");
    }

}
