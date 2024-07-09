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
import io.camunda.connector.filestorage.toolbox.FileRunnerParameter;
import io.camunda.connector.filestorage.toolbox.FileStorageSubFunction;
import io.camunda.connector.filestorage.upload.UploadFile;
import io.camunda.filestorage.FileRepoFactory;
import io.camunda.filestorage.FileVariableReference;
import io.camunda.filestorage.StorageDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
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
    return "upload";
  }


  @Override
  public FileStorageOutput executeSubFunction(FileStorageInput input,
                                              OutboundConnectorContext outboundConnectorContext) {

    FileVariableReference fileVariable;
    try {
      fileVariable = FileVariableReference.fromJson(input.getSourceFile());
    } catch(Exception e) {
      throw new ConnectorException(FileStorageError.BPMNERROR_ACCESS_FILEVARIABLE,
          "Worker [" + getSubFunctionName() + "] error during access fileVariableReference[" + input.getSourceFile()
              + "] :" + e);
    }
    FileStorageOutput output = new FileStorageOutput();
    try {
      FileRepoFactory fileRepoFactory = FileRepoFactory.getInstance();

      boolean filePurged = fileRepoFactory.purgeFileVariable(fileVariable);

      output.fileIsPurged= filePurged;
    } catch (Exception e) {
      logger.error("Can't purge file " + e);
      output.fileIsPurged= false;
      throw new ConnectorException(FileStorageError.BPMNERROR_INCORRECT_FILESTORAGE,
          "Worker [" + getSubFunctionName() + "] File[" + fileVariable.content + "] can't purge");
    }
return output;
  }

  public List<FileRunnerParameter> getInputsParameter() {
    return Arrays.asList(
    (FileRunnerParameter) new FileRunnerParameter(FileStorageInput.INPUT_SOURCE_FILE,
        "Source file",
        String.class,
        RunnerParameter.Level.REQUIRED,
        "FileVariable used to delete", 1));
  }

  public List<FileRunnerParameter> getOutputsParameter(){
    return Arrays.asList(new FileRunnerParameter(FileStorageOutput.OUTPUT_FILE_IS_PURGED, "File purged",
        Object.class,
        RunnerParameter.Level.REQUIRED,
        "True if the file is correctly purge, or didn't exist"));

  }

  public Map<String, String> getBpmnErrors() {
    return Map.of(FileStorageError.BPMNERROR_ACCESS_FILEVARIABLE,FileStorageError.BPMNERROR_ACCESS_FILEVARIABLE_EXPL,

        FileStorageError.BPMNERROR_INCORRECT_FILESTORAGE, FileStorageError.BPMNERROR_INCORRECT_FILESTORAGE_EXPL);

  }

}
