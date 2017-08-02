package com.ciphertechsolutions.io.ui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Stack;

import com.ciphertechsolutions.io.applicationLogic.IProcessController;
import com.ciphertechsolutions.io.usb.UsbWriteBlock;

import javafx.application.HostServices;
//import logic.ProcessController;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * The base class for ION controllers.
 */
public abstract class BaseController implements Initializable {

    protected Parent fxmlRoot;
    protected Scene currentScene;
    protected static Stage primaryStage;
    protected static HostServices hostServices;
    private static final Stack<BaseController> previous = new Stack<>();
    protected IProcessController workflowController;

    /**
     * <b>DO NOT CALL THIS METHOD DIRECTLY.</b> Use {@link #loadFXML(String, Class)} instead.
     */
    public BaseController() {
        previous.push(this);
    }

    /**
     * Loads the given FXML and instantiates the controller associated with it.
     * Typically you should use {@link BaseController#loadFXML(Class, String)} instead.
     * @param fxmlLocation The location of the FXML file to load.
     * @param clazz The class of the controller to instantiate.
     * @return The controller for the given FXML file.
     */
    public static <T extends BaseController> T loadFXML(String fxmlLocation, Class<T> clazz) {
        FXMLLoader fxmlLoader = new FXMLLoader(clazz.getResource(fxmlLocation));
        try {
            Parent root = (Parent) fxmlLoader.load();
            T controller = fxmlLoader.getController();
            controller.fxmlRoot = root;
            controller.currentScene = new Scene(root);
            controller.currentScene.getStylesheets().add(GUILauncher.class.getResource("css/style.css").toString());
            return controller;
        }
        catch (Exception exception) {
            displayErrorPopup(exception, "Fatal Error Encountered: ");
            throw new RuntimeException(exception);
        }
    }

    /**
     * Loads the given FXML and instantiates the controller associated with it. The {@link #workflowController} will
     * be set and {@link #performSetup()} will be called.
     * @param clazz The class of the controller to instantiate.
     * @param fxmlLocation The location of the FXML file to load.
     * @return The controller.
     */
    public <T extends BaseController> T loadFXML(Class<T> clazz, String fxmlLocation) {
        T controller = loadFXML(fxmlLocation, clazz);
        if (workflowController != null) {
            controller.setWorkflowController(workflowController);
        }
        controller.performSetup();
        return controller;
    }

    /**
     * Perform any necessary setup here that requires the {@link #workflowController} to be set.
     */
    protected void performSetup() {
        // empty for overriding
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // empty for overriding

    }

    protected void setWorkflowController(IProcessController processController) {
        workflowController = processController;
    }

    /**
     * Get the currently active scene.
     * @return The scene.
     */
    public Scene getScene() {
        if (currentScene == null) {
            throw new RuntimeException("Not initialized!");
        }
        return currentScene;
    }

    /**
     * Gets the currently active controller. This method probably shouldn't exist.
     * @return The controller.
     */
    public static BaseController getCurrentController() {
        if (previous == null) {
            throw new RuntimeException("Not initialized!");
        }
        return previous.peek();
    }

    /**
     * Sets the HostServices for the controllers to use.
     * @param services The HostServices to make available.
     */
    public static void setHostServices(HostServices services) {
        hostServices = services;
    }

    /**
     * Sets the given stage as the primary stage for ION.
     * @param stage The stage to use.
     */
    public static void setStage(Stage stage) {
        primaryStage = stage;
        primaryStage.setMinWidth(560);
        primaryStage.setMinHeight(400);
    }

    /**
     * Changes the scene to the given scene. Will do its best to make the given scene resizable.
     * @param scene The scene to display.
     */
    public static void changeScene(Scene scene) {
        primaryStage.setScene(scene);
        for (Node child : scene.getRoot().getChildrenUnmodifiable()) {
            makeScale(child, scene);
        }
        Platform.runLater(() -> primaryStage.sizeToScene());
    }

    private static void makeScale(Node node, Scene scene) {
        makeLocationScale(scene, node);
        if (node instanceof Region) {
            makeScalable(scene, (Region) node);
        }
        if (node instanceof Group) {
            scaleAll(scene, node);
        }
        if (node instanceof Shape && !(node instanceof Text)) {
            scaleShape(scene, (Shape) node);
        }
    }

    protected static void goBack() {
        if (previous.isEmpty() || previous.size() == 1) {
            return;
        }
        previous.pop();
        // workflowController.undo();
        changeScene(previous.peek().getScene());
        previous.peek().setTitle();
    }

    private static Optional<ButtonType> displayPopup(AlertType type, String message) {
        Alert popup = new Alert(type, message);
        popup.setHeaderText(null);
        return popup.showAndWait();
    }

    protected static void displayErrorPopup(String messageToDisplay) {
        displayPopup(AlertType.ERROR, "Error: " + messageToDisplay);
    }

    protected static void displayErrorPopup(Throwable errorToDisplay) {
        displayErrorPopup(errorToDisplay, "Error: ");
    }

    protected static void displayErrorPopup(Throwable errorToDisplay, String initialMessage) {
        Alert alert = new Alert(AlertType.ERROR, initialMessage + errorToDisplay.getMessage());

        GridPane expContent = createExceptionTextArea(errorToDisplay);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }

    protected static GridPane createExceptionTextArea(Throwable errorToDisplay) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        errorToDisplay.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);
        return expContent;
    }

    protected static boolean displayYesNoPopup(String message) {
        Alert popup = new Alert(AlertType.CONFIRMATION, message);
        popup.setHeaderText(null);
        popup.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        return popup.showAndWait().get() == ButtonType.YES;
    }

    protected static void displayInformationPopup(String messageToDisplay) {
        displayPopup(AlertType.INFORMATION, messageToDisplay);
    }

    protected static boolean displayConfirmationPopup(String messageToDisplay) {
        return displayPopup(AlertType.CONFIRMATION, messageToDisplay).get() == ButtonType.OK;
    }

    protected void setTitle(String string) {
        primaryStage.setTitle(string);
    }

    protected void tryBeginImaging() {
        if (!UsbWriteBlock.getWriteBlockStatus()) {
            displayInformationPopup("Write block has been disabled, please enable it prior to imaging.");
            return;
        }
        if (!workflowController.setupImaging()) {
            Platform.runLater(() -> displayInformationPopup("No valid drive selected."));
        }
        else {
            changeScene(loadFXML(ImagingController.class, ImagingController.getFXMLLocation()).getScene());
            workflowController.beginImaging();
        }
    }

    private static void makeScalable(Scene scene, Region region) {
        double regionWidth = region.getPrefWidth();
        double sceneWidth = scene.getWidth();
        double regionHeight = region.getPrefHeight();
        double sceneHeight = scene.getHeight();
        if (regionWidth != -1.0) {
            ChangeListener<Number> widthResizer = (arg0, oldValue, newValue) -> region.setPrefWidth(regionWidth / sceneWidth * newValue.doubleValue());
            scene.widthProperty().addListener(widthResizer);
        }
        if (regionHeight != -1.0) {
            ChangeListener<Number> heightResizer = (arg0, oldValue, newValue) -> region.setPrefHeight(regionHeight / sceneHeight * newValue.doubleValue());
            scene.heightProperty().addListener(heightResizer);
        }
    }

    private static void makeLocationScale(Scene scene, Node node) {
        double nodeX = node.getLayoutX();
        double sceneWidth = scene.getWidth();
        double nodeY = node.getLayoutY();
        double sceneHeight = scene.getHeight();
        if (nodeX != 0.0) {
            ChangeListener<Number> xResizer = (arg0, oldValue, newValue) -> node.relocate(nodeX / sceneWidth * newValue.doubleValue(), node.getLayoutY());
            scene.widthProperty().addListener(xResizer);
        }
        if (nodeY != 0.0) {
            ChangeListener<Number> yResizer = (arg0, oldValue, newValue) -> node.relocate(node.getLayoutX(), nodeY / sceneHeight * newValue.doubleValue());
            scene.heightProperty().addListener(yResizer);
        }
    }

    private static void scaleShape(Scene scene, Shape shape) {
        double sceneWidth = scene.getWidth();
        double sceneHeight = scene.getHeight();
        ChangeListener<Number> widthResizer = (arg0, oldValue, newValue) -> shape.setScaleX(newValue.doubleValue() / sceneWidth);
        scene.widthProperty().addListener(widthResizer);
        ChangeListener<Number> heightResizer = (arg0, oldValue, newValue) -> shape.setScaleY(newValue.doubleValue() / sceneHeight);
        scene.heightProperty().addListener(heightResizer);

    }

    private static void scaleAll(Scene scene, Node node) {
        double translateX = node.getTranslateX();
        double translateY = node.getTranslateY();
        double baseX = node.getBoundsInParent().getMinX();
        double baseY = node.getBoundsInParent().getMinY();
        double sceneWidth = scene.getWidth();
        double sceneHeight = scene.getHeight();
        ChangeListener<Number> widthResizer = (arg0, oldValue, newValue) -> {
            node.setScaleX(newValue.doubleValue() / sceneWidth);
            node.setTranslateX(translateX + (baseX * newValue.doubleValue() / sceneWidth) - baseX);
        };

        scene.widthProperty().addListener(widthResizer);
        ChangeListener<Number> heightResizer = (arg0, oldValue, newValue) -> {
            node.setScaleY(newValue.doubleValue() / sceneHeight);
            node.setTranslateY(translateY + (baseY * newValue.doubleValue() / sceneHeight) - baseY);
        };
        scene.heightProperty().addListener(heightResizer);

    }

    protected abstract void setTitle();

}
