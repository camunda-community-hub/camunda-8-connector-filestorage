package io.camunda.connector.filestorage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.cherrytemplate.CherryInput;
import io.camunda.connector.filestorage.toolbox.ParameterToolbox;
import io.camunda.filestorage.storage.StorageDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * the JsonIgnoreProperties is mandatory: the template may contain additional widget to help the designer, especially on the OPTIONAL parameters
 * This avoids the MAPPING Exception
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileStorageInput implements CherryInput {

    public static final String INPUT_FILESTORAGEFUNCTION = "fileStorageFunction";
    public static final String INPUT_SOURCE_FILE = "sourceFile";
    public static final String INPUT_FOLDER_TO_SAVE = "folderToSave";
    public static final String INPUT_FOLDER_TO_READ = "folderToRead";
    public static final String INPUT_FILE_NAME = "fileName";
    public static final String INPUT_FILE_NAME_TOWRITE = "fileNameToWrite";
    public static final String INPUT_FILTER_FILE = "filterFile";
    public static final String INPUT_MAXIMUM_FILES_TO_PROCESS = "maximumFilesToProcess";
    public static final String INPUT_POLICY = "policy";
    public static final String POLICY_V_DELETE = "DELETE";
    public static final String POLICY_V_ARCHIVE = "ARCHIVE";
    public static final String POLICY_V_UNCHANGE = "UNCHANGE";

    public static final String INPUT_JSONSTORAGEDEFINITION = "jsonStorageDefinition";
    public static final String INPUT_STORAGEDEFINITION = "storageDefinition";
    public static final String INPUT_STORAGEDEFINITION_FOLDER_COMPLEMENT = "storageDefinitionFolderComplement";
    public static final String INPUT_STORAGEDEFINITION_CMIS_COMPLEMENT = "storageDefinitionCmisComplement";


    public static final String INPUT_ARCHIVE_FOLDER = "archiveFolder";
    public static final String INPUT_ZEEBE_DOCUMENT = "zeebeDocument";
    private final Logger logger = LoggerFactory.getLogger(FileStorageInput.class.getName());
    public String fileStorageFunction;
    public Object sourceFile;
    public String folderToSave;
    public String folderToRead;
    public String fileName;
    public String fileNameToWrite;
    public String filterFile;
    public String policy;
    public Integer maximumFilesToProcess;
    public String jsonStorageDefinition;
    public String storageDefinition;
    public String storageDefinitionFolderCompletement;
    public String storageDefinitionCmisComplement;
    public String archiveFolder;
    public io.camunda.document.Document zeebeDocument;

    public io.camunda.document.Document getZeebeDocument() {
        return zeebeDocument;
    }


    public String getFileStorageFunction() {
        return fileStorageFunction;
    }

    public Object getSourceFile() {
        return sourceFile;
    }

    public String getFolderToSave() {
        return folderToSave;
    }

    public String getFolderToRead() {
        return folderToRead;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileNameToWrite() {
        return fileNameToWrite;
    }

    public String getFilterFile() {
        return filterFile;
    }

    public String getPolicy() {
        return policy;
    }

    public String getStorageDefinition() {
        return storageDefinition;
    }

    public String getStorageDefinitionFolderCompletement() {
        return storageDefinitionFolderCompletement;
    }

    public String getStorageDefinitionCmisComplement() {
        return storageDefinitionCmisComplement;
    }

    public Integer getMaximumFilesToProcess() {
        return maximumFilesToProcess;
    }

    //* if jsonStorageDefinition is provided, return it, else compose it from the storage + Folder qnd CMIS complement
    public String getCompleteStorageDefinition() {
        if (jsonStorageDefinition == null || jsonStorageDefinition.isEmpty()) {
            return storageDefinition;
        }
        return jsonStorageDefinition;
    }

    public String getDestCompleteStorageDefinition() {
        return "";
    }

    public String getArchiveFolder() {
        return archiveFolder;
    }

    public String getJsonStorageDefinition() {
        return jsonStorageDefinition;
    }

    public boolean isJsonStorageDefinition() {
        return jsonStorageDefinition != null && !jsonStorageDefinition.trim().isEmpty();
    }


    /**
     * Return a Storage definition
     *
     * @return the storage definition
     * @throws ConnectorException if the connection
     */
    public StorageDefinition getStorageDefinitionObject() throws ConnectorException {
        try {
            StorageDefinition storageDefinition = StorageDefinition.getFromString(isJsonStorageDefinition() ?
                    getJsonStorageDefinition() : getStorageDefinition());

            storageDefinition.complement = getStorageDefinitionFolderCompletement();
            if (storageDefinition.complement != null && storageDefinition.complement.isEmpty())
                storageDefinition.complement = null;

            storageDefinition.complementInObject = getStorageDefinitionCmisComplement();
            return storageDefinition;
        } catch (Exception e) {
            logger.error("Can't get the FileStorage - bad Gson value :" + getStorageDefinition());
            throw new ConnectorException(FileStorageError.BPMNERROR_INCORRECT_FILESTORAGE,
                    "FileStorage information" + getStorageDefinition());
        }
    }

    @Override
    public List<Map<String, Object>> getInputParameters() {
        return ParameterToolbox.getInputParameters();
    }
}
