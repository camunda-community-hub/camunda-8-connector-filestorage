/* ******************************************************************** */
/*                                                                      */
/*  SaveFileFromDiskWorker                                              */
/*                                                                      */
/* Save a file from the process to the disk                             */
/* C8 does not manage a file type, so there is different implementation */
/* @see FileVariableFactory                                             */
/* ******************************************************************** */
package io.camunda.connector.filestorage.download;

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
import io.camunda.filestorage.FileVariable;
import io.camunda.filestorage.FileVariableReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DownloadFile implements FileStorageSubFunction {

  private final Logger logger = LoggerFactory.getLogger(UploadFile.class.getName());


  public String getSubFunctionName() {
    return "DownloadFile";
  }

  @Override
  public String getSubFunctionDescription() {
    return "Get a file from the storage, and download it on a local folder";
  }

  public String getSubFunctionType() {
    return "download";
  }

  @Override
  public FileStorageOutput executeSubFunction(FileStorageInput input,
                                              OutboundConnectorContext outboundConnectorContext) {

    FileVariable fileVariable;
    try {
      FileVariableReference fileVariableReference = FileVariableReference.fromJson(input.getSourceFile());
      FileRepoFactory fileRepoFactory = FileRepoFactory.getInstance();
      fileVariable = fileRepoFactory.loadFileVariable(fileVariableReference);

    } catch (Exception e) {
      throw new ConnectorException(FileStorageError.BPMNERROR_ACCESS_FILEVARIABLE,
          "Worker [" + getSubFunctionName() + "] error during access fileVariableReference[" + input.getSourceFile()
              + "] :" + e);
    }

    String folderToSave = input.getFolderToSave();
    String fileName = input.getFileName();
    if (fileName == null || fileName.isEmpty()) {
      fileName = fileVariable.getName();
    }
    if (fileVariable == null) {
      logger.error("Input file variable does not exist ");
      throw new ConnectorException(FileStorageError.BPMNERROR_LOAD_FILE_ERROR, " file Input does not exist");

    }
    File folder = new File(folderToSave);
    if (!(folder.exists() && folder.isDirectory())) {
      logger.error("Folder[" + folder.getAbsolutePath() + "] does not exist ");
      throw new ConnectorException(FileStorageError.BPMNERROR_FOLDER_NOT_EXIST,
          " folder[" + folder.getAbsolutePath() + "] does not exist");
    }
    FileStorageOutput output = new FileStorageOutput();
    try {
      Path file = Paths.get(folder.getAbsolutePath() + FileSystems.getDefault().getSeparator() + fileName);
      Files.write(file, fileVariable.getValue());
      logger.info("Write file[" + file + "]");
    } catch (Exception e) {
      logger.error("Cannot save to folder[" + folderToSave + "] : " + e);
      throw new ConnectorException(FileStorageError.BPMNERROR_WRITE_FILE_ERROR,
          " Cannot save to folder[" + folderToSave + "] :" + e);
    }
    return output;
  }

  public List<FileRunnerParameter> getInputsParameter() {
    return Arrays.asList((FileRunnerParameter) new FileRunnerParameter(FileStorageInput.INPUT_SOURCE_FILE,// name
            "Source file", // label
            String.class, // type
            RunnerParameter.Level.REQUIRED, // level
            "FileVariable used to save locally", 1),

        (FileRunnerParameter) new FileRunnerParameter(FileStorageInput.INPUT_FOLDER_TO_SAVE,// name
            "Folder to save the file", // label
            String.class, // type
            RunnerParameter.Level.REQUIRED,// level
            "Folder to save the file", 1),

        (FileRunnerParameter) new FileRunnerParameter(FileStorageInput.INPUT_FILE_NAME, // name
            "File name of the new file",// label
            String.class,// type
            RunnerParameter.Level.REQUIRED, // level
            "Folder to save the file", 1));

  }

  public List<FileRunnerParameter> getOutputsParameter() {
    return Collections.emptyList();
  }

  public Map<String, String> getBpmnErrors() {
    return Map.of(FileStorageError.BPMNERROR_ACCESS_FILEVARIABLE, FileStorageError.BPMNERROR_ACCESS_FILEVARIABLE_EXPL,

        FileStorageError.BPMNERROR_LOAD_FILE_ERROR, FileStorageError.BPMNERROR_LOAD_FILE_ERROR_EXPL,

    FileStorageError.BPMNERROR_FOLDER_NOT_EXIST, FileStorageError.BPMNERROR_FOLDER_NOT_EXIST_EXPL,

        FileStorageError.BPMNERROR_WRITE_FILE_ERROR, FileStorageError.BPMNERROR_WRITE_FILE_ERROR_EXPL);

  }
}