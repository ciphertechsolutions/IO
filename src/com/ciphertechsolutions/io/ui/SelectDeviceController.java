package com.ciphertechsolutions.io.ui;

import com.ciphertechsolutions.io.device.Device;
import com.ciphertechsolutions.io.device.Disk;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;

/**
 * The controller for the device selection screen.
 */
public class SelectDeviceController extends BaseController {

    @FXML
    private Button selectButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label devicesLoadingLabel;
    @FXML
    private ListView<Disk> diskListView;

    /**
     * Get the location of this controller's corresponding FXML file.
     * @return the FXML file locatiion.
     */
    public static String getFXMLLocation() {
        return "fxml/IONDevice.fxml";
    }

    @Override
    protected void performSetup() {
        setTitle();
        // TODO: Better loading.
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                devicesLoadingLabel.setVisible(true);
                diskListView.setCellFactory(lv -> {
                    TextFieldListCell<Disk> cell = new TextFieldListCell<>();
                    cell.setConverter(workflowController.getStringConverterForDisks());
                    return cell;
                });
                ObservableList<Disk> disks = FXCollections.observableArrayList();
                disks.addAll(workflowController.getAvailableDisks());
                devicesLoadingLabel.setVisible(false);
                diskListView.setItems(disks);
            }});
    }

    @FXML
    void onSelectDevice(ActionEvent event) {
        Device device = diskListView.getSelectionModel().getSelectedItem();
        if (device != null) {
            workflowController.selectDevice(device);
            tryBeginImaging();
        }
        else {
            displayInformationPopup("No device selected.");
        }
    }

    @FXML
    void onCancel(ActionEvent event) {
        changeScene(loadFXML(MainScreenController.class, MainScreenController.getFXMLLocation()).getScene());
    }

    @Override
    protected void setTitle() {
        setTitle("IO - Select Device");

    }

}
