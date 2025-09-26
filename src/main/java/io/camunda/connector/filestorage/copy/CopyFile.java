
    /* ******************************************************************** */
    /*                                                                      */
    /*  LoadFileFromDiskWorker                                              */
    /*                                                                      */
    /* Load a file from disk to the process.                                */
    /* C8 does not manage a file type, so there is different implementation */
    /* @see FileVariableFactory                                             */
    /* ******************************************************************** */
    package io.camunda.connector.filestorage.copy;

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

    import java.util.Arrays;
    import java.util.Collections;
    import java.util.List;
    import java.util.Map;

    public class CopyFile implements FileStorageSubFunction {

        public static final String GROUP_STORAGE_DEFINITION = "Storage definition";
        private final Logger logger = LoggerFactory.getLogger(CopyFile.class.getName());

        public CopyFile() {
        }

        public String getSubFunctionName() {
            return "CopyFile";
        }

        @Override
        public String getSubFunctionDescription() {
            return "Copy a file storage from a FileStorage to an another file storage";
        }

        public String getSubFunctionType() {
            return "copy";
        }

        @Override
        public FileStorageOutput executeSubFunction(FileStorageInput input,
                                                    OutboundConnectorContext outboundConnectorContext) {

            StringBuilder traceExecution = new StringBuilder();
            traceExecution.append("---CopyFile:");

            FileVariable sourceFileVariable = null;

            // ------------ source File
            FileVariableReference sourceFileVariableReference = null;
            try {
                sourceFileVariableReference = FileVariableReference.fromObject(input.getSourceFile());
                FileStorageToolbox.traceValue(traceExecution, "load fileReference ", sourceFileVariableReference.toJson());
            } catch (Exception e) {
                throw new ConnectorException(FileStorageError.BPMNERROR_ACCESS_FILEVARIABLE,
                        "Worker [" + getSubFunctionName() + "] error during access fileVariableReference[" + input.getSourceFile()
                                + "] :" + e);
            }
            try {
                long beginOperation = System.currentTimeMillis();
                FileRepoFactory fileRepoFactory = FileRepoFactory.getInstance();
                sourceFileVariable = fileRepoFactory.loadFileVariable(sourceFileVariableReference, outboundConnectorContext);
                FileStorageToolbox.traceValue(traceExecution, "load File ", sourceFileVariable.getName());
                FileStorageToolbox.traceValue(traceExecution, " in (ms)", String.valueOf(System.currentTimeMillis() - beginOperation));
            } catch (Exception e) {
                logger.error("Can't read file[{}] {} : {}", sourceFileVariableReference, traceExecution, e);
                throw new ConnectorException(FileStorageError.BPMNERROR_INCORRECT_FILESTORAGE,
                        "Worker [" + getSubFunctionName() + "] FileReference[" + sourceFileVariableReference.content + "] can't access");
            }
            if (sourceFileVariable == null) {
                logger.error("Input file variable does not exist {}", traceExecution);
                throw new ConnectorException(FileStorageError.BPMNERROR_LOAD_FILE_ERROR, " file Input does not exist");
            }

            // ------------ destination File
            FileStorageOutput fileStorageOutput = new FileStorageOutput();

            FileVariable destinationFileVariable = new FileVariable();

            destinationFileVariable.setValueStream(sourceFileVariable.getValueStream());
            destinationFileVariable.setName(sourceFileVariable.getName());
            destinationFileVariable.setMimeType(sourceFileVariable.getMimeType());


            //------ Storage Definition
            // Move to the FileStorage
            StorageDefinition destinationStorageDefinition = input.getStorageDefinitionObject();
            // Move to the file storage
            FileStorageToolbox.traceValue(traceExecution, "Move to FileStorage", destinationStorageDefinition.getInformation());


            try {
                destinationFileVariable.setStorageDefinition(destinationStorageDefinition);
                FileRepoFactory fileRepoFactory = FileRepoFactory.getInstance();

                long beginOperation = System.currentTimeMillis();
                FileVariableReference fileVariableReference = fileRepoFactory.saveFileVariable(destinationFileVariable, outboundConnectorContext);
                FileStorageToolbox.traceValue(traceExecution, "Loaded in (ms)", String.valueOf(System.currentTimeMillis() - beginOperation));

                fileStorageOutput.fileLoaded = fileVariableReference;
                fileStorageOutput.nbFilesProcessed++;

            } catch (Exception e) {
                logger.error("Error during setFileVariableReference: {} : {} ", traceExecution, e);
                throw new ConnectorException(FileStorageError.BPMNERROR_SAVE_FILEVARIABLE,
                        "Worker [" + getSubFunctionName() + "] error during access storageDefinition[" + destinationStorageDefinition + "] :"
                                + e);
            }
            fileStorageOutput.fileNameLoaded = destinationFileVariable.getName();
            fileStorageOutput.fileMimeTypeLoaded = destinationFileVariable.getMimeType();


            logger.info(traceExecution.toString());
            return fileStorageOutput;
        }

        public List<RunnerParameter> getInputsParameter() {
            return Arrays.asList(
                    RunnerParameter.getInstance(FileStorageInput.INPUT_SOURCE_FILE,// name
                            "Source file", // label
                            String.class, // type
                            RunnerParameter.Level.REQUIRED, // level
                            "FileVariable used to save locally"),

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

                    RunnerParameter.getInstance(FileStorageInput.INPUT_JSONSTORAGEDEFINITION, // name
                                    "Storage definition in JSON", // label
                                    Object.class, // type
                                    RunnerParameter.Level.OPTIONAL, // level
                                    "Give a JSON information to access the storage definition") // parameter
                            .setGroup(GROUP_STORAGE_DEFINITION));

        }

        public List<RunnerParameter> getOutputsParameter() {
            return Arrays.asList(RunnerParameter.getInstance(FileStorageOutput.OUTPUT_FILE_LOADED, //
                            "File loaded", //
                            Object.class, //
                            RunnerParameter.Level.REQUIRED, //
                            "Name of the variable to save the file loaded.Content depend of the storage definition"),
                    RunnerParameter.getInstance(FileStorageOutput.OUTPUT_NB_FILES_PROCESSED, //
                            "Nb files processed", //
                            Integer.class, //
                            RunnerParameter.Level.REQUIRED, //
                            "Number of files processed. May be 1 or 0 (no file found)"));
        }

        public Map<String, String> getBpmnErrors() {
            return Map.of(
                    FileStorageError.BPMNERROR_LOAD_FILE_ERROR, FileStorageError.BPMNERROR_LOAD_FILE_ERROR_EXPL, //
                    FileStorageError.BPMNERROR_COPY_FILE_ERROR, FileStorageError.BPMNERROR_COPY_FILE_ERROR_EXPL, //
                    FileStorageError.ERROR_INCORRECT_STORAGEDEFINITION, FileStorageError.ERROR_INCORRECT_STORAGEDEFINITION_EXPL, //
                    FileStorageError.BPMNERROR_BAD_CMIS_PARAMETERS, FileStorageError.BPMNERROR_BAD_CMIS_PARAMETERS_EXPL); //

        }


    }


