package com.ciphertechsolutions.io.applicationLogic.options;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.ciphertechsolutions.io.logging.LogMessageType;
import com.ciphertechsolutions.io.logging.Logging;

/**
 *  Contains the configurable options for ION.
 */
public class AdvancedOptions extends Properties {

	private static final long serialVersionUID = -4595476431254975311L;

    private final static List<String> COMPRESSION_TYPES = initializeCompressionTypes();

    private final static String BASE_CONFIG_STRING = ".\\Data\\Configs\\";
    private final static String ERROR_LOADING_MSG = "Error loading config, loading default config instead.";
    private final static String ERROR_SAVING_MSG = "Error saving config, config was not saved.";
    /**
     * An AdvancedOptions containing the default configuration.
     */
    public static final AdvancedOptions DEFAULT_OPTIONS = getDefaultConfig();
    private String name;

    /**
     * Creates a new {@link AdvancedOptions} by loading the most recent config from disk.
     */
	public AdvancedOptions() {
	    this(true);
	}

    private AdvancedOptions(boolean loadMostRecent) {
        if (loadMostRecent){
            this.putAll(loadMostRecentConfig());
        }
    }
	/**
	 * Requires constructor and ConfigOptionsEnum to be in sync.
	 * @param string
	 */
	public AdvancedOptions(String name, String saveLocation, String compressionType, String description, String examiner, String caseNum, String evidence, String note) {
	    this.name = name;
		List<String> initOptions = new ArrayList<>();
		initOptions.add(compressionType);
        initOptions.add(saveLocation);
		initOptions.add(description);
		initOptions.add(examiner);
		initOptions.add(caseNum);
		initOptions.add(evidence);
		initOptions.add(note);
		int index = 0;
    	for (ConfigOptionsEnum option : ConfigOptionsEnum.values()) {
			setProperty(option.getDisplayName(), initOptions.get(index));
			index++;
		}
    }

	/**
	 * Creates a new {@link AdvancedOptions} using the defaults from {@link ConfigOptionsEnum}.
	 * @return A new AdvancedOptions.
	 */
	public static AdvancedOptions getDefaultConfig() {
		AdvancedOptions defaultConfig = new AdvancedOptions(false);
		defaultConfig.initToDefaults();
		defaultConfig.name = "Default";
		return defaultConfig;

	}

    private void initToDefaults() {
        for (ConfigOptionsEnum option : ConfigOptionsEnum.values()) {
			setProperty(option.getDisplayName(), option.getDefaultValue());
		}
    }

    /**
     * Saves these options to disk, with the given file name.
     * @param name The name to save as.
     * @return True if the save was successful, false if there was an error.
     */
	public boolean saveConfig(String name) {
	    File saveFile = Paths.get(BASE_CONFIG_STRING + name).toFile();
	    saveFile.getParentFile().mkdirs();
		try {
		    saveFile.createNewFile();
		    FileOutputStream writeTo = new FileOutputStream(saveFile);
			this.store(writeTo, "");
			writeTo.close();
			return true;
		} catch (IOException e) {
			Logging.log(ERROR_SAVING_MSG, LogMessageType.ERROR);
			Logging.log(e);
		}
		return false;

	}

	/**
	 * Loads the most recently saved configuration from disk. On error will return the default config instead.
	 * @return An AdvancedOptions containing the most recently saved configuration.
	 */
	public static AdvancedOptions loadMostRecentConfig() {
	    AdvancedOptions config = null;
	    Optional<Path> lastFilePath;
	    try {
	        Path configDir = Paths.get(BASE_CONFIG_STRING);
	        if (configDir.toFile().isDirectory()) {
	            lastFilePath = Files.list(configDir)    // get the stream with full directory listing
	                    .filter(f -> Files.isDirectory(f) == false)  // exclude subdirectories from listing
	                    .max((f1, f2) -> (int)(f1.toFile().lastModified() - f2.toFile().lastModified()));

	            if ( lastFilePath.isPresent() ) // folder may be empty
	            {
	                config = new AdvancedOptions(false);
	                config.initConfigByPath(lastFilePath.get());
	            }
	        }
	    } catch (IOException e) {
	        Logging.log(ERROR_LOADING_MSG, LogMessageType.ERROR);
	        Logging.log(e);
	    }
	    //Otherwise get default
	    if(config == null) {
	        config = AdvancedOptions.getDefaultConfig();
	    }
	    return config;
	}

	/**
	 * Loads a configuration by name from disk. On error returns the default config instead.
	 * @param fileName The name of the configuration to load.
	 * @return The loaded configuration.
	 */
    public static AdvancedOptions loadConfigByName(String fileName) {
        AdvancedOptions config = null;
        try {
            Path configDir = Paths.get(BASE_CONFIG_STRING);
            if (configDir.toFile().isDirectory()) {
                Optional<Path> toLoad = Files.list(configDir).filter(path -> path.getFileName().toString().equals(fileName)).findAny();
                if ( toLoad.isPresent() ) // folder may be empty
                {
                    config = new AdvancedOptions(false);
                    config.initConfigByPath(toLoad.get());
                }
            }
        } catch (IOException e) {
            Logging.log(ERROR_LOADING_MSG, LogMessageType.ERROR);
            Logging.log(e);
        }
        //Otherwise get default
        if(config == null) {
            config = AdvancedOptions.getDefaultConfig();
        }
        return config;
    }

    /**
     * Deletes a configuration with the given name from disk.
     * @param fileName The name of the configuration to delete.
     */
    public static void deleteConfigByName(String fileName) {
        try {
            Path configDir = Paths.get(BASE_CONFIG_STRING);
            if (configDir.toFile().isDirectory()) {
                Optional<Path> toLoad = Files.list(configDir).filter(path -> path.getFileName().toString().equals(fileName)).findAny();
                if ( toLoad.isPresent() ) // folder may be empty
                {
                    toLoad.get().toFile().delete();
                }
            }
        } catch (IOException e) {
            Logging.log("Failed to delete configuration " + fileName, LogMessageType.ERROR);
            Logging.log(e);
        }
    }

    /**
     * Get a set of all configuration names present on disk.
     * @return A set containing all available configurations.
     */
	public static Set<String> getConfigNames() {
        Path configDir = Paths.get(BASE_CONFIG_STRING);
        if (configDir.toFile().isDirectory()) {
            try {
                return Files.list(configDir).filter(f -> Files.isDirectory(f) == false).
                        map(A -> A.getFileName().toString()).collect(Collectors.<String>toSet());
            }
            catch (IOException e) {
                Logging.log(e);
            }
        }
        return Collections.emptySet();
	}

	private void initConfigByPath(Path configPath) {
		try (FileReader configReader = new FileReader(configPath.toFile())){
			load(configReader);
			name = configPath.getFileName().toString();
		} catch (IOException e) {
			initToDefaults();
			Logging.log(ERROR_LOADING_MSG, LogMessageType.ERROR);
            Logging.log(e);
		}
	}

	private static List<String> initializeCompressionTypes() {
		List<String> compressionTypes = new ArrayList<>();
		for (CompressionTypesEnum compression : CompressionTypesEnum.values()) {
			compressionTypes.add(compression.getDisplayName());
		}

		return compressionTypes;
	}

	/**
	 * Gets the name of this AdvancedOptions.
	 * @return The name.
	 */
	public String getName() {
	    return this.name;
	}

	/**
	 * Get the output folder location.
	 * @return The output folder.
	 */
	public String getOutputFolder() {
	    return this.getProperty(ConfigOptionsEnum.OutputFolder.getDisplayName(), "./");
	}

	/**
	 * Get the desired compression level.
	 * @return The compression level.
	 */
	public int getCompressionLevel() {
	    return CompressionTypesEnum.getLevelByName(this.getProperty(ConfigOptionsEnum.CompressionType.getDisplayName()));
	}

	/**
	 * Get the display-friendly name of the compression level.
	 * @return The display-friendly name of compression level.
	 */
    public String getCompressionLevelName() {
        return this.getProperty(ConfigOptionsEnum.CompressionType.getDisplayName());
    }

    /**
     * Get the case number.
     * @return The case number.
     */
	public String getCaseNumber() {
	    return getProperty(ConfigOptionsEnum.CaseNumber.getDisplayName());
	}

	/**
	 * Get the case description.
	 * @return The case description.
	 */
    public String getCaseDescription() {
        return getProperty(ConfigOptionsEnum.Description.getDisplayName());
    }

    /**
     * Get the examiner name.
     * @return The examiner name.
     */
    public String getExaminerName() {
        return getProperty(ConfigOptionsEnum.ExaminerName.getDisplayName());
    }

    /**
     * Get the case notes.
     * @return The case notes.
     */
    public String getCaseNotes() {
        return getProperty(ConfigOptionsEnum.Notes.getDisplayName());
    }

    /**
     * Get the evidence number
     * @return The evidence number.
     */
    public String getEvidenceNumber() {
        return getProperty(ConfigOptionsEnum.EvidenceNumber.getDisplayName());
    }

    /**
     * Get a list containing the possible compression type settings, in display-friendly string form.
     * @return The compression types.
     */
	public static List<String> getCompressionTypes() {
		return COMPRESSION_TYPES;
	}

	private enum ConfigOptionsEnum {

	    CompressionType("compressionType", CompressionTypesEnum.getDefaultCompressionType().getDisplayName()),
	    OutputFolder("outputFolder", "./_ForensicImages/"),
	    Description("description", ""),
	    ExaminerName("examinerName", ""),
	    CaseNumber("caseNumber", ""),
	    EvidenceNumber("evidenceNumber", ""),
	    Notes("notes", "");

		String displayName;
		String defaultValue;

		ConfigOptionsEnum(String name, String value) {
			displayName = name;
			defaultValue = value;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getDefaultValue() {
			return defaultValue;
		}
	}


}
