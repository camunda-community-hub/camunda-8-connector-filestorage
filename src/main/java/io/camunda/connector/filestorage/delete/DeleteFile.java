/* ******************************************************************** */
/*                                                                      */
/*  PurgeFile                                                           */
/*                                                                      */
/* Purge a file in the storage.                                         */
/* ******************************************************************** */
package io.camunda.connector.filestorage.delete;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import io.camunda.connector.filestorage.FileStorageError;
import io.camunda.connector.filestorage.FileStorageInput;
import io.camunda.connector.filestorage.FileStorageOutput;
import io.camunda.connector.filestorage.toolbox.FileStorageSubFunction;
import io.camunda.connector.filestorage.toolbox.FileStorageToolbox;
import io.camunda.filestorage.FileRepoFactory;
import io.camunda.filestorage.FileVariableReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DeleteFile implements FileStorageSubFunction {
    private final Logger logger = LoggerFactory.getLogger(DeleteFile.class.getName());

    public DeleteFile() {
    }

    public String getSubFunctionName() {
        return "DeleteFile";
    }

    @Override
    public String getSubFunctionDescription() {
        return "According the storage definition, file may need to be purged";
    }

    public String getSubFunctionType() {
        return "delete";
    }

    @Override
    public FileStorageOutput executeSubFunction(FileStorageInput input,
                                                OutboundConnectorContext outboundConnectorContext) {
        StringBuilder traceExecution = new StringBuilder();
        traceExecution.append("---DeleteFile:");

        FileVariableReference fileVariableReference;
        try {
            fileVariableReference = FileVariableReference.fromObject(input.getSourceFile());
        } catch (Exception e) {
            throw new ConnectorException(FileStorageError.BPMNERROR_ACCESS_FILEVARIABLE,
                    "Worker [" + getSubFunctionName() + "] error during access fileVariableReference[" + input.getSourceFile()
                            + "] :" + e);
        }
        FileStorageOutput output = new FileStorageOutput();
        try {
            FileRepoFactory fileRepoFactory = FileRepoFactory.getInstance();
            FileStorageToolbox.traceValue(traceExecution, "StorageDefinition", fileVariableReference.getStorageDefinition());


            boolean filePurged = fileRepoFactory.purgeFileVariable(fileVariableReference, outboundConnectorContext);
            FileStorageToolbox.traceValue(traceExecution, "Purged[", String.valueOf(filePurged));


            output.fileIsPurged = filePurged;
            output.nbFilesProcessed = 1;

        } catch (Exception e) {
            logger.error("Can't purge file {}", e);
            output.fileIsPurged = false;
            throw new ConnectorException(FileStorageError.BPMNERROR_INCORRECT_FILESTORAGE,
                    "Worker [" + getSubFunctionName() + "] FileReference[" + fileVariableReference.content + "] can't purge");
        }
        logger.info(traceExecution.toString());

        return output;
    }

    public List<RunnerParameter> getInputsParameter() {
        return List.of(RunnerParameter.getInstance(FileStorageInput.INPUT_SOURCE_FILE, "Source file", String.class,
                RunnerParameter.Level.REQUIRED, "FileVariable used to delete"));
    }

    public List<RunnerParameter> getOutputsParameter() {
        return Arrays.asList(RunnerParameter.getInstance(FileStorageOutput.OUTPUT_FILE_IS_PURGED, //
                        "File purged", //
                        Boolean.class, //
                        RunnerParameter.Level.REQUIRED, //
                        "True if the file is correctly purge, or didn't exist"),
                RunnerParameter.getInstance(FileStorageOutput.OUTPUT_NB_FILES_PROCESSED, //
                        "Nb files processed", //
                        Integer.class, //
                        RunnerParameter.Level.REQUIRED, //
                        "Number of files processed. May be 1 or 0 (no file found)"));

    }

    public Map<String, String> getBpmnErrors() {
        return Map.of(FileStorageError.BPMNERROR_ACCESS_FILEVARIABLE, FileStorageError.BPMNERROR_ACCESS_FILEVARIABLE_EXPL,

                FileStorageError.BPMNERROR_INCORRECT_FILESTORAGE, FileStorageError.BPMNERROR_INCORRECT_FILESTORAGE_EXPL);

    }

}
