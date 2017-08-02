package com.ciphertechsolutions.io.processing;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.bind.DatatypeConverter;

import com.ciphertechsolutions.io.applicationLogic.Utils;
import com.ciphertechsolutions.io.applicationLogic.options.AdvancedOptions;
import com.ciphertechsolutions.io.device.Device;
import com.ciphertechsolutions.io.ewf.DataChunk;
import com.ciphertechsolutions.io.ewf.DigestSection;
import com.ciphertechsolutions.io.ewf.DoneSection;
import com.ciphertechsolutions.io.ewf.Error2Section;
import com.ciphertechsolutions.io.ewf.HashSection;
import com.ciphertechsolutions.io.ewf.Header2Section;
import com.ciphertechsolutions.io.ewf.HeaderSection;
import com.ciphertechsolutions.io.ewf.NextSection;
import com.ciphertechsolutions.io.ewf.SectorsSection;
import com.ciphertechsolutions.io.ewf.Table2Section;
import com.ciphertechsolutions.io.ewf.TableSection;
import com.ciphertechsolutions.io.ewf.VolumeSectionManager;
import com.ciphertechsolutions.io.logging.LogMessageType;
import com.ciphertechsolutions.io.logging.Logging;
import com.ciphertechsolutions.io.processing.triage.ByteUtils;

/**
 * A class for managing writing an image in the Encase6 file format.
 */
public class EWFOutput extends ProcessorBase {
    private static final long MAX_SEGMENT_SIZE = 1610612736; // TODO: Make this set in the constructor.
    private static final byte[] EWF_MAGIC = { 0x45, 0x56, 0x46, 0x09, 0x0d, 0x0a, (byte) 0xff, 0x00 };
    private final BlockingQueue<Future<DataChunk>> chunkQueue;
    private RandomAccessFile currentOutputFile;
    private final File outputFile;
    private int fileNumber;
    private TableSection currentTable;
    private SectorsSection currentSectorsSection;
    private final String serialNumber;
    private final VolumeSectionManager volumeManager;
    private int outputs;
    private long sectors;
    private final long expectedSize;
    private final int sectorSize;
    private final LocalDateTime startTime;
    private final FutureTask<byte[]> md5HashSource;
    private final FutureTask<byte[]> sha1HashSource;
    private final AdvancedOptions options;
    // TODO: Determine which EWF settings this needs to know about, and how to share those settings.
    // Perhaps repurpose the processing.options package?

    /**
     * Equivalent to {@link #EWFOutput(Device, BlockingQueue, File, FutureTask, FutureTask, AdvancedOptions)
     * EWFOutput(Device, BlockingQueue, File, null, null, AdvancedOptions)}
     * @param toImage
     * @param chunkQueue
     * @param outputFile
     * @param options
     * @throws IOException
     */
    public EWFOutput(Device toImage, BlockingQueue<Future<DataChunk>> chunkQueue, File outputFile, AdvancedOptions options) throws IOException {
        this(toImage, chunkQueue, outputFile, null, null, options);
    }

    /**
     * Equivalent to {@link #EWFOutput(Device, BlockingQueue, File, FutureTask, FutureTask, AdvancedOptions)
     * EWFOutput(Device, BlockingQueue, File, md5Hash, null, AdvancedOptions)}
     * @param toImage
     * @param chunkQueue
     * @param outputFile
     * @param options
     * @throws IOException
     */
    public EWFOutput(Device toImage, BlockingQueue<Future<DataChunk>> chunkQueue, File outputFile, FutureTask<byte[]> md5Hash, AdvancedOptions options)
            throws IOException {
        this(toImage, chunkQueue, outputFile, md5Hash, null, options);
    }

    /**
     * Creates a new EWFOutput with the given parameters.
     * @param toImage The device this image is of.
     * @param chunkQueue The source of data chunks to build the {@link SectorsSection Sectors} segments with.
     * @param outputFile The file to write the output to. Should end in .E01.
     * @param md5Hash The source of the MD5 digest hash.
     * @param sha1Hash The source of the SHA1 digest hash.
     * @param options The options used for this image.
     * @throws IOException
     */
    public EWFOutput(Device toImage, BlockingQueue<Future<DataChunk>> chunkQueue, File outputFile,
            FutureTask<byte[]> md5Hash, FutureTask<byte[]> sha1Hash, AdvancedOptions options) throws IOException {
        super("EWFWriter");
        this.fileNumber = 1;
        this.outputFile = outputFile;
        this.expectedSize = toImage.getSize();
        this.serialNumber = toImage.getSerialNumber();
        this.volumeManager = new VolumeSectionManager(toImage);
        if (!outputFile.exists()) {
            if (this.outputFile.getParentFile() != null) {
                this.outputFile.getParentFile().mkdirs();
            }
            this.outputFile.createNewFile();
        }
        this.md5HashSource = md5Hash;
        this.sha1HashSource = sha1Hash;
        currentOutputFile = new RandomAccessFile(this.outputFile.getAbsolutePath(), "rw");
        currentOutputFile.setLength(0);
        this.chunkQueue = chunkQueue;
        this.sectorSize = 512; // TODO: remove hardcoded
        this.options = options;
        this.startTime = LocalDateTime.now();
    }

    @Override
    public void process(byte[] toProcess) {
        // Do nothing.
    }

    @Override
    public void initialize() {
        try {
            initializeEWFFile();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        startThreads();
    }

    private void initializeEWFFile() throws IOException {
        writeFileHeader();
        writeHeaderSection();
        writeHeaderSection(); // TODO: Check that an exact duplicate is OK and necessary
        writeDiskSection(); // TODO: Write volume section instead if not a physical disk?
        beginSectorsSection();
    }

    private void beginSectorsSection() throws IOException {
        currentSectorsSection = new SectorsSection(currentOutputFile.getFilePointer());
        currentOutputFile.write(currentSectorsSection.getInitialHeader());
        currentTable = new TableSection(0);
    }

    private void writeDiskSection() throws IOException {
        volumeManager.writePreliminaryDiskSection(currentOutputFile);
    }

    private void writeFileHeader() throws IOException {
        currentOutputFile.write(EWF_MAGIC);
        currentOutputFile.writeByte(1);
        byte[] segmentNumber = ByteUtils.intToBytes(fileNumber);
        currentOutputFile.writeByte(segmentNumber[0]);
        currentOutputFile.writeByte(segmentNumber[1]);
        currentOutputFile.writeByte(0);
        currentOutputFile.writeByte(0);
    }

    private void writeHeaderSection() throws IOException {
        HeaderSection header = new HeaderSection(currentOutputFile.getFilePointer());
        header.setHeaderSection(options.getCaseDescription(), options.getCaseNumber(), options.getEvidenceNumber(),
                options.getExaminerName(), options.getCaseNotes(), serialNumber);
        byte[] headerString = header.getHeaderAsBytes(); // Need to do this first to get size information.
        currentOutputFile.write(header.getFullHeader());
        currentOutputFile.write(headerString);
    }

    private void writeHeader2Section() throws IOException {
        Header2Section header = new Header2Section(currentOutputFile.getFilePointer());
        header.setHeaderSection(options.getCaseDescription(), options.getCaseNumber(), options.getEvidenceNumber(),
                options.getExaminerName(), options.getCaseNotes(), serialNumber);
        byte[] headerString = header.getHeaderAsBytes(); // Need to do this first to get size information.
        currentOutputFile.write(header.getFullHeader());
        currentOutputFile.write(headerString);
    }

    private void writeError2Section() throws IOException {
        Error2Section error2Section = new Error2Section(currentOutputFile.getFilePointer());
        ArrayList<Long> badSectors = DriveReader.getBadSectorsList();
        int numBad = 0;
        long badSectorStart = -1;
        for (long badSector : badSectors) {
            if (badSector - numBad == badSectorStart) {
                numBad++;
            }
            else if (numBad != 0 && badSectorStart >= 0) {
                error2Section.addEntry((int) badSectorStart, numBad); // Error2SectionEntry reqs int for sector #, information loss
                numBad = 0;
            }
            else {
                badSectorStart = badSector;
                numBad = 1;
            }
        }

        currentOutputFile.write(error2Section.getFullHeader());
        currentOutputFile.write(error2Section.getFullBytes());

    }

    @Override
    public void finish() {
        // This class processes the compressed chunks rather than the raw stream, so do nothing here.
    }

    @Override
    protected int getThreadCount() {
        return 1;
    }

    @Override
    protected void internalProcess() {
        try {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                Future<DataChunk> toRead = chunkQueue.poll(5, TimeUnit.SECONDS);
                if (toRead != null) {
                    DataChunk chunk = toRead.get();
                    if (chunk.originalSize == 0) {
                        Logging.log("Final output count: " + sectors + " sectors", LogMessageType.DEBUG);
                        finalizeFile();
                        volumeManager.setVolumeSize(outputs, sectors);
                        volumeManager.writeProperVolumeSections(true);
                        writeReport();
                        return;
                    }
                    if (isSegmentFull()) {
                        finalizeSegment();
                        createNewSegment();
                        initializeSegment();
                    }
                    addChunkToSegment(chunk);
                    outputs++;
                    sectors += (chunk.originalSize / sectorSize);
                    if (outputs % 10000 == 0) {
                        Logging.log("Output: " + sectors + " sectors", LogMessageType.DEBUG);
                    }
                }
            }
        }
        catch (InterruptedException | ExecutionException | IOException e) {
            Logging.log(e);
        }
    }

    private void writeReport() {
        try {
            Logging.log("Output " + sectors + " of an expected " + (expectedSize / sectorSize) + " sectors.", LogMessageType.REPORT, LogMessageType.USER);
            Logging.log("Completed in " + Utils.getPrettyTime(Duration.between(startTime, LocalDateTime.now())), LogMessageType.REPORT, LogMessageType.USER);
            if (md5HashSource != null) {
                Logging.log("MD5 Hash: " + DatatypeConverter.printHexBinary(md5HashSource.get(1, TimeUnit.SECONDS)), LogMessageType.REPORT,
                        LogMessageType.USER);
            }
            if (sha1HashSource != null) {
                Logging.log("SHA1 Hash: " + DatatypeConverter.printHexBinary(sha1HashSource.get(1, TimeUnit.SECONDS)), LogMessageType.REPORT,
                        LogMessageType.USER);
            }
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            Logging.log(e);
        }

    }

    private void initializeSegment() throws IOException {
        writeFileHeader();
        writeDataSection();
        beginSectorsSection();
    }

    private void writeDataSection() throws IOException {
        volumeManager.writePreliminaryDataSection(currentOutputFile);
    }

    private void addChunkToSegment(DataChunk chunk) throws IOException {
        if (currentTable.isFull()) {
            finalizeSectorsSection();
            writeTableSections();
            beginSectorsSection();
        }
        currentTable.add(currentOutputFile.getFilePointer(), chunk.compressed);
        writeChunk(chunk);
    }

    private void finalizeSectorsSection() throws IOException {
        long formerOffset = currentOutputFile.getFilePointer();
        currentOutputFile.seek(currentSectorsSection.getCurrentOffset() + 16); // TODO: Fix the magic
        writeLong(currentSectorsSection.getNextOffset());
        writeLong(currentSectorsSection.getSectionSize());
        currentOutputFile.seek(currentOutputFile.getFilePointer() + 40); // TODO: Fix this magic, this is the padding.
        writeInt(currentSectorsSection.getAdler32());
        currentOutputFile.seek(formerOffset);
    }

    private void writeChunk(DataChunk chunk) throws IOException {
        currentSectorsSection.add(chunk.size);
        currentOutputFile.write(chunk.data);
    }

    private void createNewSegment() {
        fileNumber++;
        File newOutputFile = new File(getNewFileName());
        try {
            newOutputFile.createNewFile();
            currentOutputFile = new RandomAccessFile(newOutputFile, "rw");
            currentOutputFile.setLength(0);
        }
        catch (IOException e) {
            Logging.log(e);
            // TODO: Stop processing?
        }

    }

    private String getNewFileName() {
        String fileString = outputFile.toString();
        String suffix = "E" + (fileNumber > 99 ? convertFileNumber() : (fileNumber >= 10 ? fileNumber : ("0" + fileNumber)));
        return fileString.substring(0, fileString.lastIndexOf(".")) + "." + suffix;
    }

    private String convertFileNumber() {
        int modifiedNumber = fileNumber - 100;
        char firstLetter = (char) (65 + modifiedNumber / 26);
        char secondLetter = (char) (65 + modifiedNumber % 26);
        return Character.toString(firstLetter) + Character.toString(secondLetter);
    }

    private void finalizeSegment() throws IOException {
        finalizeSectorsSection();
        writeTableSections();
        writeNextSection();
        // currentOutputFile.close();
    }

    private void writeTableSections() throws IOException {
        currentTable.setCurrentOffset(currentSectorsSection.getNextOffset());
        writeTableSection();
        writeTable2Section();
    }

    private void writeTableSection() throws IOException {
        currentOutputFile.write(currentTable.getFullHeader());
        writeInt(currentTable.getArray().size());
        writeInt(0);
        writeLong(currentTable.getBaseOffset());
        writeInt(0);
        writeInt(currentTable.getSecondaryAdler32());
        for (Integer offset : currentTable.getArray()) {
            writeInt(offset);
        }
        writeInt(currentTable.getTableAdler32());
    }

    private void writeTable2Section() throws IOException {
        currentTable = new Table2Section(currentOutputFile.getFilePointer(), currentTable);
        writeTableSection();
    }

    private void writeNextSection() throws IOException {
        currentOutputFile.write(new NextSection(currentOutputFile.getFilePointer()).getFullHeader());
    }

    private boolean isSegmentFull() throws IOException {
        return currentOutputFile.getFilePointer() >= MAX_SEGMENT_SIZE;
    }

    private void finalizeFile() throws IOException {
        finalizeSectorsSection();
        writeTableSections();
        if (fileNumber == 1) {
            writeDataSection();
        }
        writeHashSections();
        writeDoneSection();
        // currentOutputFile.close();
    }

    private void writeHashSections() throws IOException {
        try {
            if (sha1HashSource != null || md5HashSource != null) {
                writeDigestSection();
            }
            if (md5HashSource != null) {
                writeHashSection();
            }
        }
        catch (InterruptedException | ExecutionException e) {
            // What to do?
            Logging.log(e);
        }
    }

    private void writeDigestSection() throws IOException, InterruptedException, ExecutionException {
        byte[] md5Hash = new byte[16];
        if (md5HashSource != null) {
            md5HashSource.run(); // TODO: Better way of doing this?
            md5Hash = md5HashSource.get();
        }
        byte[] sha1Hash = new byte[20];
        if (sha1HashSource != null) {
            sha1HashSource.run(); // TODO: Better way of doing this?
            sha1Hash = sha1HashSource.get();
        }
        DigestSection digestSection = new DigestSection(currentOutputFile.getFilePointer(), md5Hash, sha1Hash);
        currentOutputFile.write(digestSection.getFullHeader());
        currentOutputFile.write(digestSection.getFullBytes());
    }

    private void writeHashSection() throws IOException, InterruptedException, ExecutionException {
        md5HashSource.run(); // TODO: Better way of doing this?
        HashSection hashSection = new HashSection(currentOutputFile.getFilePointer(), md5HashSource.get());
        currentOutputFile.write(hashSection.getFullHeader());
        currentOutputFile.write(hashSection.getFullBytes());
    }

    private void writeDoneSection() throws IOException {
        currentOutputFile.write(new DoneSection(currentOutputFile.getFilePointer()).getFullHeader());
    }

    private void writeInt(int toWrite) throws IOException {
        currentOutputFile.write(toWrite & 0xff);
        currentOutputFile.write((toWrite >> 8) & 0xff);
        currentOutputFile.write((toWrite >> 16) & 0xff);
        currentOutputFile.write((toWrite >> 24) & 0xff);
    }

    private void writeLong(long toWrite) throws IOException {
        currentOutputFile.write((int) (toWrite & 0xff));
        currentOutputFile.write((int) ((toWrite >> 8) & 0xff));
        currentOutputFile.write((int) ((toWrite >> 16) & 0xff));
        currentOutputFile.write((int) ((toWrite >> 24) & 0xff));
        currentOutputFile.write((int) ((toWrite >> 32) & 0xff));
        currentOutputFile.write((int) ((toWrite >> 40) & 0xff));
        currentOutputFile.write((int) ((toWrite >> 48) & 0xff));
        currentOutputFile.write((int) ((toWrite >> 56) & 0xff));
    }
}
