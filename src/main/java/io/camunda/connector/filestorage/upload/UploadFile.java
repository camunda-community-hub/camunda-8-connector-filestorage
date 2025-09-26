/* ******************************************************************** */
/*                                                                      */
/*  LoadFileFromDiskWorker                                              */
/*                                                                      */
/* Load a file from disk to the process.                                */
/* C8 does not manage a file type, so there is different implementation */
/* @see FileVariableFactory                                             */
/* ******************************************************************** */
package io.camunda.connector.filestorage.upload;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import io.camunda.connector.filestorage.FileStorageError;
import io.camunda.connector.filestorage.FileStorageInput;
import io.camunda.connector.filestorage.FileStorageOutput;
import io.camunda.connector.filestorage.toolbox.FileStorageSubFunction;
import io.camunda.connector.filestorage.toolbox.FileStorageToolbox;
import io.camunda.filestorage.FileRepoFactory;
import io.camunda.filestorage.FileVariable;
import io.camunda.filestorage.FileVariableReference;
import io.camunda.filestorage.cmis.CmisParameters;
import io.camunda.filestorage.storage.StorageDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UploadFile implements FileStorageSubFunction {

    public static final String GROUP_SOURCE = "Source";
    public static final String GROUP_PROCESS_FILE = "Process file";
    public static final String GROUP_STORAGE_DEFINITION = "Storage definition";
    private final Logger logger = LoggerFactory.getLogger(UploadFile.class.getName());

    public UploadFile() {
    }

    public String getSubFunctionName() {
        return "UploadFile";
    }

    @Override
    public String getSubFunctionDescription() {
        return "Upload a file from the disk, and save it in a storage definition";
    }

    public String getSubFunctionType() {
        return "upload";
    }

    @Override
    public FileStorageOutput executeSubFunction(FileStorageInput input,
                                                OutboundConnectorContext outboundConnectorContext) {

        StringBuilder traceExecution = new StringBuilder();
        traceExecution.append("---UploadFile:");

        //--- get the list of files
        List<File> listFilesFiltered = getListOfFiles(input, traceExecution, outboundConnectorContext);

        FileStorageOutput fileStorageOutput = new FileStorageOutput();
        if (listFilesFiltered.isEmpty()) {
            fileStorageOutput.nbFilesProcessed = 0;
            logger.info(traceExecution.toString());
            return fileStorageOutput;
        }

        //------ Storage Definition
        // Copy to the FileStorage
        StorageDefinition storageDefinition = input.getStorageDefinitionObject();
        // Move to the file storage
        FileStorageToolbox.traceValue(traceExecution, "Storage Definition", storageDefinition.getInformation());

        //------ Policy after operation Definition
        // Apply the policy
        File archiveFolder = FileStorageToolbox.getFolderFileFromName(input.getArchiveFolder());
        String policy = input.getPolicy();
        FileStorageToolbox.traceValue(traceExecution, "PolicyArchive", policy);


        fileStorageOutput.fileLoaded = null;
        fileStorageOutput.fileNameLoaded = null;
        fileStorageOutput.fileMimeTypeLoaded = null;
        fileStorageOutput.nbFilesProcessed = 0;

        for (File fileToProcess : listFilesFiltered) {
            if (fileStorageOutput.nbFilesProcessed >= input.getMaximumFilesToProcess()) {
                FileStorageToolbox.traceValue(traceExecution, "Maximum number is reach", input.getMaximumFilesToProcess());
                break;
            }
            // load all files
            FileLoadedRecord fileLoaded = loadFile(fileToProcess, storageDefinition, traceExecution, outboundConnectorContext);

            // The storage will contain only the last file
            fileStorageOutput.fileLoaded = fileLoaded.fileVariableReference;
            fileStorageOutput.nbFilesProcessed++;
            fileStorageOutput.fileNameLoaded = fileLoaded.fileVariable.getName();
            fileStorageOutput.fileMimeTypeLoaded = fileLoaded.fileVariable.getMimeType();
            // according to the policy, move the file
            applyPolicy(fileToProcess, policy, archiveFolder, traceExecution);
        }
        logger.info(traceExecution.toString());
        return fileStorageOutput;
    }

    public List<RunnerParameter> getInputsParameter() {
        return Arrays.asList(

                RunnerParameter.getInstance(FileStorageInput.INPUT_FOLDER_TO_READ, // name
                                "Folder", // label
                                String.class, // class
                                RunnerParameter.Level.REQUIRED, // level
                                "Specify the folder where the file will be loaded. Must be visible from the server.") //
                        .setGroup(GROUP_SOURCE),


                RunnerParameter.getInstance(FileStorageInput.INPUT_FILE_NAME, // name
                                "File name", // label
                                String.class, // class
                                RunnerParameter.Level.OPTIONAL, // level
                                "Specify a file name, else the first file in the folder will be loaded") //
                        .setGroup(GROUP_SOURCE),

                RunnerParameter.getInstance(FileStorageInput.INPUT_FILTER_FILE, "Filter file", String.class,
                                // class
                                RunnerParameter.Level.OPTIONAL, // level
                                "If you didn't specify a fileName, a filter to select only part of files present in the folder") //
                        .setDefaultValue("*.*") //
                        .setGroup(GROUP_SOURCE),

                RunnerParameter.getInstance(FileStorageInput.INPUT_ZEEBE_DOCUMENT, // name
                                "Zeebe Document", // label
                                String.class, // class
                                RunnerParameter.Level.OPTIONAL, // level
                                "Give a Zeebe Document to be the source.") //
                        .setGroup(GROUP_SOURCE),

                RunnerParameter.getInstance(FileStorageInput.INPUT_MAXIMUM_FILES_TO_PROCESS,
                                "Maximum file to process",
                                Integer.class,
                                // class
                                RunnerParameter.Level.REQUIRED, // level
                                "Maximum file to process, if the source contains more than this number, they will be ignored") //
                        .setDefaultValue(1) //
                        .setGroup(GROUP_SOURCE),

                RunnerParameter.getInstance(FileStorageInput.INPUT_POLICY, "Policy", String.class,
                                RunnerParameter.Level.OPTIONAL,
                                // level
                                "Policy to manipulate the file after loading. With " + FileStorageInput.POLICY_V_ARCHIVE
                                        + ", the folder archive must be specify") //
                        .addChoice(FileStorageInput.POLICY_V_DELETE, "Delete")
                        .addChoice(FileStorageInput.POLICY_V_ARCHIVE, "Archive")
                        .addChoice(FileStorageInput.POLICY_V_UNCHANGE, "Unchange")
                        .setVisibleInTemplate()
                        .setDefaultValue(FileStorageInput.POLICY_V_UNCHANGE)
                        .setGroup(GROUP_PROCESS_FILE),

                RunnerParameter.getInstance(FileStorageInput.INPUT_ARCHIVE_FOLDER,
                                "Archive folder",
                                String.class,
                                RunnerParameter.Level.REQUIRED, // level
                                "With the policy " + FileStorageInput.POLICY_V_ARCHIVE + ". File is moved in this folder.") //
                        .addCondition(FileStorageInput.INPUT_POLICY, Collections.singletonList(FileStorageInput.POLICY_V_ARCHIVE))
                        .setGroup(GROUP_PROCESS_FILE)
                        .addCondition(FileStorageInput.INPUT_POLICY, Collections.singletonList(FileStorageInput.POLICY_V_ARCHIVE)),


                RunnerParameter.getInstance(FileStorageInput.INPUT_STORAGEDEFINITION,
                                "Storage definition",
                                String.class,
                                RunnerParameter.Level.OPTIONAL,
                                // level
                                "How to saved the FileVariable. " + StorageDefinition.StorageDefinitionType.JSON
                                        + " to save in the engine (size is linited), " + StorageDefinition.StorageDefinitionType.TEMPFOLDER
                                        + " to use the temporary folder of THIS machine" + StorageDefinition.StorageDefinitionType.FOLDER
                                        + " to specify a folder to save it (to be accessible by multiple machine if you ruin it in a cluster"
                                        + StorageDefinition.StorageDefinitionType.CMIS + " to specify a CMIS connection") //
                        .addChoice("JSON", StorageDefinition.StorageDefinitionType.JSON.toString())
                        .addChoice(StorageDefinition.StorageDefinitionType.TEMPFOLDER.toString(),
                                StorageDefinition.StorageDefinitionType.TEMPFOLDER.toString())
                        .addChoice(StorageDefinition.StorageDefinitionType.FOLDER.toString(),
                                StorageDefinition.StorageDefinitionType.FOLDER.toString())
                        .addChoice(StorageDefinition.StorageDefinitionType.CMIS.toString(),
                                StorageDefinition.StorageDefinitionType.CMIS.toString())
                        .addChoice(StorageDefinition.StorageDefinitionType.CAMUNDA.toString(),
                                StorageDefinition.StorageDefinitionType.CAMUNDA.toString())
                        .setVisibleInTemplate()
                        .setDefaultValue(StorageDefinition.StorageDefinitionType.JSON.toString())
                        .setGroup(GROUP_STORAGE_DEFINITION),

                RunnerParameter.getInstance(FileStorageInput.INPUT_STORAGEDEFINITION_FOLDER_COMPLEMENT,
                                "FOLDER Storage definition Complement", String.class, // class
                                RunnerParameter.Level.REQUIRED, // level
                                "Provide the FOLDER path on the server")// explanation
                        .addCondition(FileStorageInput.INPUT_STORAGEDEFINITION,
                                Collections.singletonList(StorageDefinition.StorageDefinitionType.FOLDER.toString()))
                        .setGroup(GROUP_STORAGE_DEFINITION),

                RunnerParameter.getInstance(FileStorageInput.INPUT_STORAGEDEFINITION_CMIS_COMPLEMENT, // name
                                "CMIS Storage definition Complement", // label
                                Object.class, // type
                                RunnerParameter.Level.REQUIRED, // level
                                "Complement to the Storage definition, if needed. " + StorageDefinition.StorageDefinitionType.FOLDER
                                        + ": please provide the folder to save the file") // parameter
                        .setGsonTemplate(CmisParameters.getGsonTemplate()) // add Gson Template
                        .addCondition(FileStorageInput.INPUT_STORAGEDEFINITION,
                                Collections.singletonList(StorageDefinition.StorageDefinitionType.CMIS.toString()))
                        .setGroup(GROUP_STORAGE_DEFINITION),

                RunnerParameter.getInstance(FileStorageInput.INPUT_JSONSTORAGEDEFINITION, "Json Storage definition", String.class,
                                RunnerParameter.Level.OPTIONAL,
                                // level
                                "Give the Storage definition as JSON") //
                        .setVisibleInTemplate()
                        .setGroup(GROUP_STORAGE_DEFINITION));
    }

    public List<RunnerParameter> getOutputsParameter() {
        return Arrays.asList(RunnerParameter.getInstance(FileStorageOutput.OUTPUT_FILE_LOADED, //
                        "File loaded", //
                        Object.class, //
                        RunnerParameter.Level.REQUIRED, //
                        "Name of the variable to save the file loaded. Content is a JSON which depend of the storage definition. In case of a list, this value contains the LAST in the list"),

                RunnerParameter.getInstance(FileStorageOutput.OUTPUT_FILE_NAME_LOADED, //
                        "File name", //
                        String.class, //
                        RunnerParameter.Level.OPTIONAL,  //
                        "Name of the file. In case of a list, this value contains the LAST in the list"),

                RunnerParameter.getInstance(FileStorageOutput.OUTPUT_FILE_MIMETYPE_LOADED, //
                        "File Mime type", //
                        String.class, //
                        RunnerParameter.Level.OPTIONAL, //
                        "MimeType of the loaded file. In case of a list, this value contains the LAST in the list"),

                RunnerParameter.getInstance(FileStorageOutput.OUTPUT_NB_FILES_PROCESSED, //
                        "Nb files processed", //
                        String.class, //
                        RunnerParameter.Level.OPTIONAL, //
                        "Number of files processed. May be 1 or 0 (no file found)"),

                RunnerParameter.getInstance(FileStorageOutput.OUTPUT_LIST_FILE_LOADED, //
                        "List Files loaded", //
                        List.class, //
                        RunnerParameter.Level.OPTIONAL, //
                        "Name of the variable to save the list of file loaded. Content is a JSON which depend of the storage definition"));
    }

    public Map<String, String> getBpmnErrors() {
        return Map.of(FileStorageError.BPMNERROR_FOLDER_NOT_EXIST, FileStorageError.BPMNERROR_FOLDER_NOT_EXIST_EXPL, // error
                FileStorageError.BPMNERROR_LOAD_FILE_ERROR, FileStorageError.BPMNERROR_LOAD_FILE_ERROR_EXPL, //
                FileStorageError.BPMNERROR_MOVE_FILE_ERROR, FileStorageError.BPMNERROR_MOVE_FILE_ERROR_EXPL, //
                FileStorageError.ERROR_INCORRECT_STORAGEDEFINITION, FileStorageError.ERROR_INCORRECT_STORAGEDEFINITION_EXPL, //
                FileStorageError.BPMNERROR_BAD_CMIS_PARAMETERS, FileStorageError.BPMNERROR_BAD_CMIS_PARAMETERS_EXPL); //

    }


    /**
     * Return the list of files
     *
     * @param input file storage
     * @param traceExecution trace to log
     * @param outboundConnectorContext context
     * @return the list of file
     * @throws ConnectorException
     */
    private List<File> getListOfFiles(FileStorageInput input,
                                      StringBuilder traceExecution,
                                      OutboundConnectorContext outboundConnectorContext) throws ConnectorException {
        // ------------ source folder
        File folder = FileStorageToolbox.getFolderFileFromName(input.getFolderToRead());
        if (folder == null) {
            String folderName = input.getFolderToRead();
            String currentPath = null;
            try {
                currentPath = new File(".").getCanonicalPath();
            } catch (IOException e) {
                // This sould never arrive
            }
            logger.error("Folder[{}] does not exist (current local folder is [{}] {}", folderName, currentPath,
                    traceExecution);
            throw new ConnectorException(FileStorageError.BPMNERROR_FOLDER_NOT_EXIST,
                    "Worker [" + getSubFunctionName() + "] folder[" + folderName + "] does not exist");
        }
        traceExecution.append("Folder:[");
        traceExecution.append(folder.getAbsolutePath());
        traceExecution.append("], ");

        // list of files to process
        String filterFile = input.getFilterFile();
        String fileName = input.getFileName();
        if (folder.listFiles() == null) {
            return Collections.emptyList();
        } else {
            List<File> listFilesFiltered = Arrays.stream(folder.listFiles()).filter(t -> {
                if (fileName != null)
                    return t.getName().equals(fileName);
                if ("*.*".equals(filterFile))
                    return true;
                if (filterFile == null || filterFile.isEmpty())
                    return true;
                return t.getName().matches(filterFile);
            }).toList();

            FileStorageToolbox.traceValue(traceExecution, "Found files", String.valueOf(folder.listFiles().length));
            FileStorageToolbox.traceValue(traceExecution, "Filterby", fileName);
            FileStorageToolbox.traceValue(traceExecution, "listFitererd", String.valueOf(listFilesFiltered.size()));

            if (input.getMaximumFilesToProcess() > 0 && listFilesFiltered.size() > input.getMaximumFilesToProcess())
                listFilesFiltered = listFilesFiltered.subList(0, input.getMaximumFilesToProcess());
            FileStorageToolbox.traceValue(traceExecution, "Max", String.valueOf(input.getMaximumFilesToProcess()));

            return listFilesFiltered;
        }
    }

    /**
     * Load file into the storage
     *
     * @param fileToProcess            File to load
     * @param storageDefinition        storage to save the file
     * @param traceExecution           trace the current execution
     * @param outboundConnectorContext context needed to save in the Camunda Storage
     * @return a records containing different information
     * @throws ConnectorException in case of error
     */
    private FileLoadedRecord loadFile(File fileToProcess, StorageDefinition storageDefinition, StringBuilder traceExecution, OutboundConnectorContext outboundConnectorContext) throws ConnectorException {
        FileVariable fileVariable = new FileVariable();

        try {
            // not possible to put the fileInputStream in the try() : we want the inputstream accessible AFTER this method
            InputStream fileInputStream = new FileInputStream(fileToProcess);
            fileVariable.setName(fileToProcess.getName());
            fileVariable.setMimeType(FileVariable.getMimeTypeFromName(fileVariable.getName()));
            fileVariable.setValueStream(fileInputStream);
        } catch (Exception e) {
            logger.error("Cannot read file[{}] {} : {}", fileToProcess.getAbsolutePath(), traceExecution, e);
            throw new ConnectorException(FileStorageError.BPMNERROR_LOAD_FILE_ERROR,
                    "Worker [" + getSubFunctionName() + "]  cannot read file[" + fileToProcess.getAbsolutePath() + "] : " + e);
        }
        FileStorageToolbox.traceValue(traceExecution, "Read FileName", fileToProcess.getName());
        FileStorageToolbox.traceValue(traceExecution, "size", String.valueOf(fileToProcess.length()));

        try {
            fileVariable.setStorageDefinition(storageDefinition);
            FileRepoFactory fileRepoFactory = FileRepoFactory.getInstance();

            long beginOperation = System.currentTimeMillis();
            FileVariableReference fileVariableReference = fileRepoFactory.saveFileVariable(fileVariable, outboundConnectorContext);
            FileStorageToolbox.traceValue(traceExecution, "Loaded in (ms)", String.valueOf(System.currentTimeMillis() - beginOperation));
            return new FileLoadedRecord(fileVariable, fileVariableReference, fileVariableReference.toJson());

        } catch (Exception e) {
            logger.error("Error during setFileVariableReference: {} : {} ", traceExecution, e);
            throw new ConnectorException(FileStorageError.BPMNERROR_SAVE_FILEVARIABLE,
                    "Worker [" + getSubFunctionName() + "] error during access storageDefinition[" + storageDefinition + "] :"
                            + e);
        }
    }

    /**
     * What do we do with the original file?
     *
     * @param fileToProcess  the file to manage
     * @param policy         policy to apply
     * @param archiveFolder  Archive folder
     * @param traceExecution trace the execution
     */
    private void applyPolicy(File fileToProcess, String policy, File archiveFolder, StringBuilder traceExecution) {
        if (FileStorageInput.POLICY_V_UNCHANGE.equals(policy)) {
            // Nothing to do here
        } else if (FileStorageInput.POLICY_V_DELETE.equals(policy)) {
            fileToProcess.delete();
        } else if (FileStorageInput.POLICY_V_ARCHIVE.equals(policy)) {
            FileStorageToolbox.traceValue(traceExecution, "ArchiveFolder", archiveFolder.getAbsolutePath());

            if (archiveFolder == null) {
                // Can't archive the file, archive folder does not exist
                String currentPath = null;
                try {
                    currentPath = new File(".").getCanonicalPath();
                } catch (IOException e) {
                }
                logger.error("Folder[{}] does not exist (current local folder is [{}] {}", archiveFolder, currentPath,
                        traceExecution);
                throw new ConnectorException(FileStorageError.BPMNERROR_FOLDER_NOT_EXIST,
                        "Worker [" + getSubFunctionName() + "] archiveFolder[" + archiveFolder.getAbsolutePath() + "] does not exist");

            }
            Path source = Paths.get(fileToProcess.getAbsolutePath());
            Path target = Paths.get(archiveFolder + "/" + fileToProcess.getName());

            try {
                // rename or move a file to other path
                // if target exists, throws FileAlreadyExistsException
                Files.move(source, target);
            } catch (Exception e) {
                logger.error("Cannot apply policy [{}] from source[{}] to [{}] {} : {}", policy, source, target,
                        traceExecution, e);
                throw new ConnectorException(FileStorageError.BPMNERROR_MOVE_FILE_ERROR,
                        "Worker [" + getSubFunctionName() + "] cannot apply the policy[" + policy + "] from source[" + source
                                + "] to [" + target + "] : " + e);
            }
        } else {
            logger.error("Unknown Policy [{}] {}", policy, traceExecution);
        }
    }

    /**
     * Result of the function. Json is here because the generation can throw an exception
     *
     * @param fileVariable
     * @param fileVariableReference
     * @param fileVariableReferenceJson
     */
    public record FileLoadedRecord(FileVariable fileVariable, FileVariableReference fileVariableReference,
                                   String fileVariableReferenceJson) {
    }


}
