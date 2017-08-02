package com.ciphertechsolutions.io.processing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.ciphertechsolutions.io.applicationLogic.IStoppable;
import com.ciphertechsolutions.io.applicationLogic.options.AdvancedOptions;
import com.ciphertechsolutions.io.device.Device;
import com.ciphertechsolutions.io.logging.LogMessageType;
import com.ciphertechsolutions.io.logging.Logging;
import com.ciphertechsolutions.io.processing.digests.Md5Digest;
import com.ciphertechsolutions.io.processing.digests.SHA1Digest;
import com.ciphertechsolutions.io.processing.triage.MagicCarver;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ChangeListener;

/**
 * This class manages ION's imaging process.
 */
public class ProcessorManager implements IStoppable {

    private final IMediaReader toProcess;
    private final String baseFileName;
    private final List<IProcessor> processors;
    private final BlockingQueue<byte[]> queue;
    private boolean isRunning = true;
    private Device device;
    /**
     * How much of {@link #toProcess} has been read.
     */
    private final LongProperty status;
    private final AdvancedOptions options;

    /**
     * Creates a new ProcessorManager to process the given device using the given IMediaReader.
     * @param toProcess The IMediaReader to read from.
     * @param device The device that is being read.
     * @param options The application options - may or may not be used depending on the processors added.
     * @param baseFileName A unique file name base to use - may or may not be used depending on the processors added.
     */
    public ProcessorManager(IMediaReader toProcess, Device device, AdvancedOptions options, String baseFileName) {
        this(toProcess, options, baseFileName);
        this.device = device;
    }

    private ProcessorManager(IMediaReader toProcess, AdvancedOptions options, String baseFileName, IProcessor... processors) {
        this.toProcess = toProcess;
        this.processors = new ArrayList<>(); // TODO: Thread safe list here instead?
        this.processors.addAll(Arrays.asList(processors));
        this.queue = new LinkedBlockingQueue<>();
        this.status = new SimpleLongProperty(0l);
        this.options = options;
        this.baseFileName = baseFileName;
    }

    /**
     * Adds ION's default processing suite: {@link ChunkedCompressor compression}, {@link Md5Digest MD5 digest},
     * {@link SHA1Digest SHA1 digest}, {@link MagicCarver magic carving}, and {@link EWFOutput outputting to Encase6}.
     */
    public void addDefaultProcessors() {
        ChunkedCompressor chunker = new ChunkedCompressor(options.getCompressionLevel());
        addProcessor(chunker);
        Md5Digest md5Digest = new Md5Digest();
        addProcessor(md5Digest);
        SHA1Digest shaDigest = new SHA1Digest();
        addProcessor(shaDigest);
        try {
            addProcessor(new MagicCarver(toProcess.getReadSize()));
            addProcessor(new EWFOutput(device, chunker.getOutputQueue(),
                    new File(baseFileName + ".E01"), md5Digest.getDigestResult(), shaDigest.getDigestResult(), options));
        }
        catch (IOException e) {
            Logging.log(e);
        }
    }

    /**
     * Adds the given {@link IProcessor processor} so that it will be run during {@link #process()}.
     * To have any impact this method must be called before {@link #process()} is called.
     * @param toAdd The {@link IProcessor processor} to add.
     */
    public void addProcessor(IProcessor toAdd) {
        processors.add(toAdd);
    }

    /**
     * Removes the given {@link IProcessor processor} so that it will not be run during {@link #process()}.
     * If kill is true, it will attempt to end the process.
     * @param toRemove The processor to remove.
     * @param kill If true, {@link IProcessor#cancel()} will be called on the given processor if it was associated with this ProcessManager.
     * @return true if the processor was present, false otherwise.
     */
    public boolean removeProcessor(IProcessor toRemove, boolean kill) {
        int index = processors.indexOf(toRemove);
        if (kill && index != -1) {
            processors.get(index).cancel();
            processors.remove(index);
            return true;
        }
        return index != -1;
    }

    /**
     * Adds the given ChangeListener to listen on {@link #status} which records the progress in processing the IMediaReader.
     * @param listener The listener to add.
     */
    public void addProgressMonitor(ChangeListener<Number> listener) {
        status.addListener(listener);
    }

    /**
     * Processes the {@link IMediaReader} associated with this ProcessorManager using the associated {@link IProcessor IProcessors}.
     * {@link IProcessor#initialize()} will be called on all IProcessors, then reading from the IMediaReader will begin. Once
     * all data has been read, {@link IProcessor#finish()} will be called on all IProcessors, then {@link IProcessor#waitForExit()}
     * will be called on all IProcessors.
     */
    public void process() {
        initializeProcessors();
        Logging.log("Starting.", LogMessageType.USER);
        logCaseInfo();
        logDeviceInfo();
        beginReading();
        long reads = 0l;
        try {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                byte[] toRead = queue.poll(5, TimeUnit.SECONDS);
                if (toRead != null) {
                    if (toRead.length > 0) {
                        reads++;
                        status.set(status.get() + toRead.length);
                        if (reads % 5000 == 0) {
                            Logging.log("Read " + status.get() + " bytes.", LogMessageType.INFO);
                        }
                        for (IProcessor processor : processors) {
                            processor.process(toRead);
                        }
                    }
                    else {
                        isRunning = false;
                    }
                }
            }
        }
        catch (InterruptedException e) {
            Logging.log(e);
        }
        for (IProcessor processor : processors) {
            processor.finish();
        }
        for (IProcessor processor : processors) {
            processor.waitForExit();
        }
        status.set(status.get() + 1);
    }

    private void logCaseInfo() {
        Logging.log("Imaging Options:", LogMessageType.REPORT, LogMessageType.USER);
        String toLog = options.getCaseNumber();
        logIfAvailable("Case Number: ", toLog);
        toLog = options.getExaminerName();
        logIfAvailable("Examiner: ", toLog);
        toLog = options.getEvidenceNumber();
        logIfAvailable("Evidence Number: ", toLog);
        toLog = options.getCaseDescription();
        logIfAvailable("Case Description: ", toLog);
        toLog = options.getCaseNotes();
        logIfAvailable("Case Notes: ", toLog);
        Logging.log("Compression algorithm: zlib DEFLATE", LogMessageType.REPORT, LogMessageType.USER);
        toLog = options.getCompressionLevelName();
        logIfAvailable("Compression Level: ", toLog);
        toLog = "" + options.getCompressionLevel();
        logIfAvailable("Compression Level (numeric): ", toLog);
    }

    private void logIfAvailable(String label, String toLog) {
        if (toLog != null && !toLog.isEmpty()) {
            Logging.log(label + toLog, LogMessageType.REPORT, LogMessageType.USER);
        }
    }

    private void logDeviceInfo() {
        Logging.log("Device Info:", LogMessageType.REPORT, LogMessageType.USER);
        Logging.log(device.details(), LogMessageType.REPORT, LogMessageType.USER);
    }

    private void beginReading() {
        Thread readingThread = new Thread((Runnable) () -> {
            int read = 0;
            while (isRunning && read >= 0) {
                try {
                    read = toProcess.read();
                    if (read > 0) {
                        byte[] bytes = toProcess.getBytes();
                        queue.add(bytes);
                    }
                }
                catch (IOException e1) {
                    read = -1;
                    Logging.log(e1);
                }
            }
            queue.add(new byte[0]);
            try {
                toProcess.close();
            }
            catch (Exception e2) {
                Logging.log(e2);
            }
        } , "DeviceReading");
        readingThread.start();
    }

    private void initializeProcessors() {
        for (IProcessor processor : processors) {
            processor.initialize();
        }
    }

    @Override
    public void stop() {
        isRunning = false;
    }
}
