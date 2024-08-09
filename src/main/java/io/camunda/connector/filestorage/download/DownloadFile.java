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

    StringBuilder traceExecution = new StringBuilder();
    traceExecution.append("---DownloadFile:");

    //----- Source file
    FileVariable fileVariable;
    FileVariableReference fileVariableReference = null;
    try {
      fileVariableReference = FileVariableReference.fromJson(input.getSourceFile());
      traceExecution.append("load fileReference [");
      traceExecution.append(fileVariableReference);
      traceExecution.append("]");
    } catch (Exception e) {
      throw new ConnectorException(FileStorageError.BPMNERROR_ACCESS_FILEVARIABLE,
          "Worker [" + getSubFunctionName() + "] error during access fileVariableReference[" + input.getSourceFile()
              + "] :" + e);
    }
    try {
      FileRepoFactory fileRepoFactory = FileRepoFactory.getInstance();
      fileVariable = fileRepoFactory.loadFileVariable(fileVariableReference);
      traceExecution.append("load file [");
      traceExecution.append(fileVariable.getName());
      traceExecution.append("], ");

    } catch (Exception e) {
      logger.error("Can't read file[{}] {} : {}", fileVariableReference, traceExecution, e);
      throw new ConnectorException(FileStorageError.BPMNERROR_INCORRECT_FILESTORAGE,
          "Worker [" + getSubFunctionName() + "] FileReference[" + fileVariableReference.content + "] can't access");
    }
    if (fileVariable == null) {
      logger.error("Input file variable does not exist {}", traceExecution);
      throw new ConnectorException(FileStorageError.BPMNERROR_LOAD_FILE_ERROR, " file Input does not exist");
    }

    // ----- folder to save
    String folderToSave = input.getFolderToSave();
    String fileName = input.getFileNameToWrite();
    if (fileName == null || fileName.isEmpty()) {
      fileName = fileVariable.getOriginalName();
      if (fileName == null || fileName.isEmpty())
        fileName = fileVariable.getName();
    }

    traceExecution.append("Folder to download[");
    traceExecution.append(folderToSave);
    traceExecution.append("] fileName");
    traceExecution.append(fileName);
    traceExecution.append("], ");

    File folder = new File(folderToSave);
    if (!(folder.exists() && folder.isDirectory())) {
      logger.error("Folder[{}] does not exist {}", folder.getAbsolutePath(), traceExecution);
      throw new ConnectorException(FileStorageError.BPMNERROR_FOLDER_NOT_EXIST,
          " folder[" + folder.getAbsolutePath() + "] does not exist");
    }
    FileStorageOutput output = new FileStorageOutput();
    try {
      Path file = Paths.get(folder.getAbsolutePath() + FileSystems.getDefault().getSeparator() + fileName);
      Files.write(file, fileVariable.getValue());
      logger.info("Write file[" + file + "]");
      output.fileIsDownloaded = true;
      output.fileName = fileName;
      output.nbFilesProcessed = 1;
    } catch (Exception e) {
      logger.error("Cannot save to folder[{} {} : {} ", folderToSave, traceExecution, e);
      throw new ConnectorException(FileStorageError.BPMNERROR_WRITE_FILE_ERROR,
          "Cannot save to folder[" + folderToSave + "] :" + e);
    }
    logger.info(traceExecution.toString());
    return output;
  }

  public List<FileRunnerParameter> getInputsParameter() {
    return Arrays.asList(new FileRunnerParameter(FileStorageInput.INPUT_SOURCE_FILE,// name
            "Source file", // label
            String.class, // type
            RunnerParameter.Level.REQUIRED, // level
            "FileVariable used to save locally", 1),

        new FileRunnerParameter(FileStorageInput.INPUT_FOLDER_TO_SAVE,// name
            "Folder to save the file", // label
            String.class, // type
            RunnerParameter.Level.REQUIRED,// level
            "Folder to save the file", 1),

        new FileRunnerParameter(FileStorageInput.INPUT_FILE_NAME_TOWRITE, // name
            "File name of the new file",// label
            String.class,// type
            RunnerParameter.Level.OPTIONAL, // level
            "Name of the file to write. If no value is given, the name of the file in the store is used", 1));

  }

  public List<FileRunnerParameter> getOutputsParameter() {
    return Arrays.asList(
        new FileRunnerParameter(FileStorageOutput.OUTPUT_FILE_IS_DOWNLOADED, "File downloaded", Boolean.class,
            RunnerParameter.Level.REQUIRED, "True if the file is correctly downloaded"),
        new FileRunnerParameter(FileStorageOutput.OUTPUT_FILE_NAME, "File name downloaded", String.class,
            RunnerParameter.Level.OPTIONAL, "File name of the file downloaded"),
        new FileRunnerParameter(FileStorageOutput.OUTPUT_NB_FILES_PROCESSED, "Nv files processed", Integer.class,
            RunnerParameter.Level.REQUIRED, "Number of file processed. May be 1 or 0 (no file found)"));

  }

  public Map<String, String> getBpmnErrors() {
    return Map.of(FileStorageError.BPMNERROR_ACCESS_FILEVARIABLE, FileStorageError.BPMNERROR_ACCESS_FILEVARIABLE_EXPL,

        FileStorageError.BPMNERROR_INCORRECT_FILESTORAGE, FileStorageError.BPMNERROR_INCORRECT_FILESTORAGE_EXPL,

        FileStorageError.BPMNERROR_LOAD_FILE_ERROR, FileStorageError.BPMNERROR_LOAD_FILE_ERROR_EXPL,

        FileStorageError.BPMNERROR_FOLDER_NOT_EXIST, FileStorageError.BPMNERROR_FOLDER_NOT_EXIST_EXPL,

        FileStorageError.BPMNERROR_WRITE_FILE_ERROR, FileStorageError.BPMNERROR_WRITE_FILE_ERROR_EXPL);

  }
}