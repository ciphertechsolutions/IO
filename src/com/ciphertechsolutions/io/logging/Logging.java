package com.ciphertechsolutions.io.logging;

import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class for performing some basic logging functions.
 */
public class Logging {

    private static final List<LoggingStream> LOGGING_PRINT_STREAMS = Collections.synchronizedList(new ArrayList<>());

    static {
        LOGGING_PRINT_STREAMS.add(new LoggingStream(System.out, true));
    }

    /**
     * Adds the given PrintStream and sets it to accept all levels of logging output.
     * @param outputStream The stream to print the logging output to.
     */
    public static void addOutput(PrintStream outputStream) {
        LOGGING_PRINT_STREAMS.add(new LoggingStream(outputStream, true));
    }

    /**
     * Adds the given PrintStream and sets it to accept the given {@link LogMessageType message types}
     * @param outputStream The stream to print the logging output to.
     * @param types The {@link LogMessageType message types} to print.
     */
    public static void addOutput(PrintStream outputStream, LogMessageType... types) {
        LOGGING_PRINT_STREAMS.add(new LoggingStream(outputStream, types));
    }

    /**
     * Stops outputting logging to the given PrintStream.
     * @param outputStream The PrintStream to cease logging to.
     */
    public static void removeOutput(PrintStream outputStream) {
        LOGGING_PRINT_STREAMS.remove(outputStream);
    }

    /**
     * Logs the given object by calling toString() on it. {@code null} will not cause a crash,
     * but will also not be logged. Log message will include a timestamp.
     * @param message
     */
    public static void log(Object message) {
        if (message != null) {
            log(message.toString());
        }
    }

    /**
     * Logs the given object by calling toString() on it. {@code null} will not cause a crash,
     * but will also not be logged. Log message will include a timestamp. The message will be of type messageType.
     * @param message
     * @param messageType
     */
    public static void log(Object message, LogMessageType messageType) {
        if (message != null) {
            log(message.toString(), messageType);
        }
    }

    /**
     * Logs an exception with a timestamp and relevant details.
     * @param exception The exception to log.
     */
    public static void log(Exception exception) {
        for (LoggingStream loggingStream : LOGGING_PRINT_STREAMS) {
            if (loggingStream.shouldPrint(LogMessageType.ERROR)) {
                @SuppressWarnings("resource")
                PrintStream printStream = loggingStream.getStream();
                printStream.println(new Timestamp(new Date().getTime()).toString());
                printStream.println(exception);
                exception.printStackTrace(printStream);
            }
        }
    }

    /**
     * Logs an exception with a timestamp and relevant details.
     * @param exception The exception to log.
     */
    public static void log(Throwable throwable) {
        for (LoggingStream loggingStream : LOGGING_PRINT_STREAMS) {
            if (loggingStream.shouldPrint(LogMessageType.ERROR)) {
                @SuppressWarnings("resource")
                PrintStream printStream = loggingStream.getStream();
                printStream.println(new Timestamp(new Date().getTime()).toString());
                printStream.println(throwable);
                throwable.printStackTrace(printStream);
            }
        }
    }

    /**
     * Logs the given message, with a timestamp.
     * @param message
     */
    public static void log(String message) {
        for (LoggingStream loggingStream : LOGGING_PRINT_STREAMS) {
            if (loggingStream.shouldPrint(LogMessageType.INFO)) {
                loggingStream.getStream().println("[" + new Date() + "]\t" + message);
            }
        }
    }

    /**
     * Logs the given message. The message will be of type messageType.
     * @param message
     * @param messageType the type of the message.
     */
    public static void logSimple(String message, LogMessageType messageType) {
        for (LoggingStream loggingStream : LOGGING_PRINT_STREAMS) {
            if (loggingStream.shouldPrint(messageType)) {
                loggingStream.getStream().println(message);
            }
        }
    }

    /**
     * Logs the given message, with a timestamp. The message will be of type messageType.
     * @param message
     * @param messageType the type of the message.
     */
    public static void log(String message, LogMessageType messageType) {
        for (LoggingStream loggingStream : LOGGING_PRINT_STREAMS) {
            if (loggingStream.shouldPrint(messageType)) {
                loggingStream.getStream().println("[" + new Date() + "]\t" + message);
            }
        }
    }

    /**
     * Logs the given message, with a timestamp. The message will be of type messageType.
     * @param message
     * @param messageType the type of the message.
     */
    public static void log(String message, LogMessageType... messageType) {
        for (LoggingStream loggingStream : LOGGING_PRINT_STREAMS) {
            if (loggingStream.shouldPrint(messageType)) {
                loggingStream.getStream().println("[" + new Date() + "]\t" + message);
            }
        }
    }
}

class LoggingStream {
    private final PrintStream stream;
    private final Set<LogMessageType> types;
    private final static Set<LogMessageType> allTypesSet = new HashSet<>(Arrays.asList(LogMessageType.values()));

    LoggingStream(PrintStream stream, boolean allTypes) {
        this.stream = stream;
        if (allTypes) {
            this.types = allTypesSet;
        }
        else {
            this.types = Collections.emptySet();
        }
    }

    LoggingStream(PrintStream stream, LogMessageType... types) {
        this.stream = stream;
        this.types = new HashSet<>(Arrays.asList(types));
    }

    boolean shouldPrint(LogMessageType type) {
        return this.types.contains(type);
    }

    boolean shouldPrint(LogMessageType[] types) {
        for (LogMessageType type : types) {
            if (this.types.contains(type)) {
                return true;
            }
        }
        return false;
    }

    PrintStream getStream() {
        return this.stream;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LoggingStream) {
            return this.stream.equals(((LoggingStream) other).stream);
        }
        if (other instanceof PrintStream) {
            return this.stream.equals(other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.stream.hashCode();
    }
}