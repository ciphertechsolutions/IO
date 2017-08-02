package com.ciphertechsolutions.io.ui;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;

import com.ciphertechsolutions.io.applicationLogic.Utils;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;

/**
 * The controller for the imaging screen in ION.
 */
public class ImagingController extends BaseController {

    @FXML
    private Button returnButton;
    @FXML
    private Button abortImagingButton;
    @FXML
    private TextArea outputTextArea;
    @FXML
    private Label timeEstimateLabel;
    @FXML
    private ProgressBar progressBar;

    private ListChangeListener<String> outputListener;
    private ChangeListener<Number> progressListener;

    /**
     * Get the location of this controller's corresponding FXML file.
     *
     * @return the FXML file location.
     */
    public static String getFXMLLocation() {
        return "fxml/IONImaging.fxml";
    }

    @FXML
    void onAbortImagingClick(ActionEvent event) {
        workflowController.stopImaging(() -> abortImaging());
    }

    /**
     * Sets the screen to its aborted state.
     */
    public void abortImaging() {
        Platform.runLater(() -> {
            setReturnVisible();
            timeEstimateLabel.setText("Aborted");
        });
    }

    private void setReturnVisible() {
        abortImagingButton.setVisible(false);
        abortImagingButton.setDisable(true);
        returnButton.setVisible(true);
        returnButton.setDisable(false);
    }

    @FXML
    void onReturnClick(ActionEvent event) {
        changeScene(loadFXML(MainScreenController.class, MainScreenController.getFXMLLocation()).getScene());
    }

    private void displayImagingProgress() {
        workflowController.registerOutputListener(getOutputToScreenLogger());
        workflowController.registerProgressListener(getProgressUpdater());
    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        setTitle();
    }

    @Override
    protected void performSetup() {
        timeEstimateLabel.setVisible(false);
        displayImagingProgress();
    }

    @Override
    protected void setTitle() {
        setTitle("IO - Imaging");
    }

    private ListChangeListener<String> getOutputToScreenLogger() {
        outputListener = new ListChangeListener<String>() {
            boolean first = true;

            @Override
            public void onChanged(ListChangeListener.Change<? extends String> change) {
                change.next();
                Platform.runLater(() -> {
                    List<? extends String> changedList = change.getList();
                    for (int index = first ? 0 : change.getFrom(); index < change.getTo(); index++) {
                        String added = changedList.get(index);
                        if (added != null) {
                            outputTextArea.appendText(added);
                        }
                    }
                    first = false;
                });
            }
        };
        return new WeakListChangeListener<>(outputListener);
    }

    private ChangeListener<Number> getProgressUpdater() {
        ProgressStatus currentStatus = new ProgressStatus();
        progressListener = (arg0, oldVal, newVal) -> Platform.runLater(() -> {
            if (!progressBar.isDisabled()) {
                long newLong = newVal.longValue();
                if (newLong > 0) {
                    currentStatus.onChange(newLong);
                }
                else if (newVal.intValue() == -1) {
                    // failureLabel.setVisible(true);
                    progressBar.setDisable(true);
                    displayErrorPopup("Error: Failed to complete imaging.\n");
                }
            }
        });
        return new WeakChangeListener<>(progressListener);
    }

    private class ProgressStatus {
        private int calls;
        private double lastAverage;
        private final long max;
        private final long startTime;

        protected ProgressStatus() {
            calls = 0;
            lastAverage = 0;
            max = workflowController.getMaxOutputCount();
            startTime = System.nanoTime();
        }

        protected void onChange(long newCount) {
            double percentComplete = newCount / (double) max;
            calls++;
            if (calls % 250 == 0) {
                estimateRemainingTime(percentComplete);
            }
            if (newCount == max) {
                timeEstimateLabel.setText("Complete");
                setReturnVisible();
            }
            else if (lastAverage > 0) {
                timeEstimateLabel.setText(Math.round((1 - percentComplete) * 100) + "% remaining: " + getTime());
                timeEstimateLabel.setVisible(true);
            }
            progressBar.setProgress(percentComplete);
        }

        protected String getTime() {
            return Utils.getPrettyTime(Duration.ofSeconds(Math.max(Math.round(lastAverage - (System.nanoTime() - startTime) / 1000000000), 0)));
        }

        protected void estimateRemainingTime(double percentComplete) {
            double newEstimate = (System.nanoTime() - startTime) / percentComplete / 1000000000;
            double estimatedTimeRemaining;
            if (lastAverage > 0) {
                estimatedTimeRemaining = (newEstimate + lastAverage) / 2;
            }
            else {
                estimatedTimeRemaining = newEstimate;
            }
            lastAverage = estimatedTimeRemaining;
        }
    }

}
