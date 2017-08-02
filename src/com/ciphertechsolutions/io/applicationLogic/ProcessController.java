package com.ciphertechsolutions.io.applicationLogic;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import com.ciphertechsolutions.io.applicationLogic.options.AdvancedOptions;
import com.ciphertechsolutions.io.device.Device;
import com.ciphertechsolutions.io.device.DeviceManager;
import com.ciphertechsolutions.io.device.Disk;
import com.ciphertechsolutions.io.logging.LogMessageType;
import com.ciphertechsolutions.io.logging.Logging;
import com.ciphertechsolutions.io.logging.ObservableOutputStream;
import com.ciphertechsolutions.io.processing.DriveReader;
import com.ciphertechsolutions.io.processing.ProcessorManager;
import com.ciphertechsolutions.io.ui.BaseController;
import com.ciphertechsolutions.io.ui.ImagingController;
import com.ciphertechsolutions.io.ui.MainScreenController;
import com.ciphertechsolutions.io.usb.USBPoller;
import com.ciphertechsolutions.io.usb.UsbWriteBlock;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.util.StringConverter;

/**
 * The overall application logic controller for ION. This provides ION's default implementation of
 * {@link IProcessController}.
 */
public class ProcessController implements IProcessController {

    private final ApplicationState state;
    private final DeviceManager deviceManager;
    private Thread processingThread;
    private final Collection<IStoppable> toTerminate;
    private ProcessorManager activeImaging;
    private final ObservableOutputStream outputStream;
    private final USBPoller poller;
    private boolean aborted = false;

    /**
     * Sole constructor. Calling this will create an instance of {@link USBPoller} and call
     * {@link USBPoller#startPolling()} on it.
     * {@link UsbWriteBlock#enable()} will also be triggered.
     */
    public ProcessController() {
        outputStream = new ObservableOutputStream();
        state = new ApplicationState();
        deviceManager = new DeviceManager();
        toTerminate = new ArrayList<>();
        poller = new USBPoller(this);
        toTerminate.add(poller);
        UsbWriteBlock.enable();
        poller.startPolling();
        Logging.addOutput(new PrintStream(outputStream, true), LogMessageType.ERROR, LogMessageType.WARNING, LogMessageType.USER);
    }

    @Override
    public boolean checkValidSaveDirectory() {
        File baseDir = new File(getBaseDirectory());
        return (baseDir.exists() && baseDir.isDirectory() && baseDir.canWrite()) || baseDir.mkdirs();
    }

    @Override
    public Set<Disk> getAvailableDisks() {
        return deviceManager.getCurrentDisks();
    }

    @Override
    public Disk findNewDisk() {
        Disk disk = deviceManager.scanForInsertedDisk();
        if (state.getSelectedDevice() != null) {
            if (deviceManager.getDeviceByName(state.getSelectedDevice().getName()) == null) {
                // TODO: Not this way. This code is terrible.
                ImagingController imagingController = (ImagingController) BaseController.getCurrentController();
                stopImaging(() -> imagingController.abortImaging());
            }
        }
        return disk;
    }

    @Override
    public void selectDevice(Device device) {
        state.setSelectedDevice(device);
    }

    @Override
    public boolean hasDevice() {
        return state.getSelectedDevice() != null;
    }

    @Override
    public boolean setupImaging() {
        aborted = false;
        Device device = state.getSelectedDevice();
        if (device == null) {
            return false;
        }
        final String baseName = setupDirectory();
        DriveReader reader;
        try {
            reader = new DriveReader(device.getPath());
            activeImaging = new ProcessorManager(reader, device, state.getOptions(), baseName);
        }
        catch (IOException e) {
            Logging.log(e);
            return false;
        }
        processingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    PrintStream reportStream = setupReportStream(baseName);
                    PrintStream csvStream = setupCSVStream(baseName);
                    toTerminate.add(activeImaging);
                    activeImaging.addDefaultProcessors();
                    activeImaging.process();
                    if (aborted) {
                        logBadSectors(DriveReader.getBadSectorsList());
                        Logging.log("Process aborted.", LogMessageType.REPORT, LogMessageType.USER);
                    }
                    else {
                        logBadSectors(DriveReader.getBadSectorsList());
                        Logging.log("Imaging Completed Successfully.", LogMessageType.REPORT, LogMessageType.USER);
                    }
                    Logging.removeOutput(csvStream);
                    csvStream.close();
                    Logging.removeOutput(reportStream);
                    reportStream.close();
                    state.setSelectedDevice(null);
                }
                catch (IOException e) {
                    Logging.log(e);
                }
            }

            private void logBadSectors(ArrayList<Long> badSectorsList) {
                StringBuilder sb = new StringBuilder("Bad sectors found at the following sectors: \n");
                for (Long sector : badSectorsList) {
                    sb.append(sector + "\n");
                }
                if (badSectorsList.size() > 0) {
                    Logging.log(sb.toString(), LogMessageType.REPORT);
                }

            }

            protected PrintStream setupReportStream(final String baseName) throws IOException {
                File reportFile = new File(baseName + "_report.txt");
                reportFile.createNewFile();
                PrintStream reportStream = new PrintStream(reportFile);
                Logging.addOutput(reportStream, LogMessageType.REPORT);
                return reportStream;
            }

            protected PrintStream setupCSVStream(final String baseName) throws IOException {
                File csvFile = new File(baseName + "_gps.csv");
                csvFile.createNewFile();
                PrintStream csvStream = new PrintStream(csvFile);
                Logging.addOutput(csvStream, LogMessageType.GPS);
                Logging.logSimple("Camera Make, Camera Model, Latitude, Longitude, Altitude, Altitude Reference, Time Stamp", LogMessageType.GPS);
                return csvStream;
            }
        }, "ProcessingMain");
        outputStream.clear();
        return true;
    }

    @Override
    public void beginImaging() {
        processingThread.start();
    }

    protected String setupDirectory() {
        String baseName = getBaseDirectory() + state.getSelectedDevice().toFileNameString() + "_" +
                new SimpleDateFormat("yyyyMMddHHmm").format(new Date()) + "/";
        new File(baseName).mkdirs();
        baseName += state.getSelectedDevice().toFileNameString();
        return baseName;
    }

    protected String getBaseDirectory() {
        String outputFolder = state.getOptions().getOutputFolder();
        if (outputFolder == null || outputFolder.equals("")) {
            outputFolder = "./";
        }
        return outputFolder;
    }

    @Override
    public void registerOutputListener(ListChangeListener<String> listener) {
        outputStream.addListener(listener);
    }

    @Override
    public void registerProgressListener(ChangeListener<Number> listener) {
        activeImaging.addProgressMonitor(listener);
    }

    @Override
    public void registerWriteBlockListener(ChangeListener<Boolean> listener) {
        poller.addWriteBlockListener(listener);
    }

    @Override
    public long getMaxOutputCount() {
        return state.getSelectedDevice().getSize() + 1;
    }

    @Override
    public BaseController getController() {
        return BaseController.loadFXML(MainScreenController.getFXMLLocation(), MainScreenController.class);
    }

    @Override
    public StringConverter<Disk> getStringConverterForDisks() {
        return new NameStringConverter(deviceManager);
    }

    private static class NameStringConverter extends StringConverter<Disk> {
        private final DeviceManager manager;

        public NameStringConverter(DeviceManager manager) {
            this.manager = manager;
        }

        @Override
        public Disk fromString(String name) {
            if (name == null || name == "" || !name.contains(":")) {
                return null;
            }
            return manager.getDeviceByName(name.split(":", 1)[0]);
        }

        @Override
        public String toString(Disk disk) {
            if (disk == null) {
                return "";
            }
            double bytesInGB = disk.getSize() / 1073741824.0;
            DecimalFormat df = new DecimalFormat("#.##"); 
            df.setRoundingMode(RoundingMode.CEILING);
            return disk.getName() + ": " + disk.model + " [" +  df.format(bytesInGB) + "GB]";

        }
    }

    @Override
    public void handleDiskInsertion(Disk insertedDisk) {
        if (state.getSelectedDevice() != null) {
            return;
        }
        else {
            Platform.runLater(() -> {
                state.setSelectedDevice(insertedDisk);
                setupImaging();
                // TODO: Not this way. This code is terrible.
                ImagingController imagingController = BaseController.getCurrentController().loadFXML(ImagingController.class,
                        ImagingController.getFXMLLocation());
                BaseController.changeScene(imagingController.getScene());
                beginImaging();
            });
        }
    }

    @Override
    public void stop() {
        aborted = true;
        for (IStoppable toStop : toTerminate) {
            toStop.stop();
        }
    }

    @Override
    public void stopImaging(Runnable callback) {
        aborted = true;
        activeImaging.stop();
        try {
            processingThread.join();
        }
        catch (InterruptedException e) {
            // Nothing to do here.
        }
        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public void saveOptions(AdvancedOptions options) {
        state.setOptions(options);
        options.saveConfig(options.getName());
        Logging.log("Save successful, config selected.", LogMessageType.INFO);
    }

    @Override
    public void setOptions(AdvancedOptions options) {
        state.setOptions(options);
    }
}
