package com.ciphertechsolutions.io.ui;

import java.awt.SplashScreen;

import com.ciphertechsolutions.io.applicationLogic.IProcessController;
import com.ciphertechsolutions.io.applicationLogic.ProcessController;
import com.ciphertechsolutions.io.usb.UsbWriteBlock;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * The primary class of the ION GUI, launches the application.
 */
public class GUILauncher extends Application {
    private IProcessController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            BaseController.setStage(primaryStage);
            BaseController.setHostServices(getHostServices());
            primaryStage.getIcons().add(new Image("/com/ciphertechsolutions/io/ui/icons/ion.png"));

            SplashScreen splash = SplashScreen.getSplashScreen();
            MainScreenController root = BaseController.loadFXML(MainScreenController.getFXMLLocation(),
                    MainScreenController.class);
            controller = new ProcessController();
            root.setWorkflowController(controller);
            root.performSetup();
            primaryStage.setScene(root.getScene());
            if (splash != null) {
                splash.close();
            }
            primaryStage.show();
            BaseController.changeScene(root.getScene());

        }
        catch (Exception e) {
            BaseController.displayErrorPopup(e, "Failed to initialize! Details: ");
            System.exit(1);
        }
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.stop();
        }
        if (UsbWriteBlock.getInitialState()) {
            if (BaseController.displayYesNoPopup("The system-wide USB write block was enabled when ION began, do you want to turn it off? "
                    + "WARNING: CLICKING YES WILL DISABLE THE SOFTWARE WRITE-BLOCK FOR ALL CURRENTLY RUNNING ION INSTANCES.")) {
                UsbWriteBlock.disable();
            }
        }
        else {
            UsbWriteBlock.disable();
        }
    }

    /**
     * Launches the application.
     * @param args No args taken.
     */
    public static void main(String[] args) {
        launch(args);
    }

}
