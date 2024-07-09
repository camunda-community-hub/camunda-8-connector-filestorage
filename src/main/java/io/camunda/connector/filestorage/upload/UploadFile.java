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
import io.camunda.connector.filestorage.toolbox.FileRunnerParameter;
import io.camunda.connector.filestorage.toolbox.FileStorageSubFunction;
import io.camunda.connector.filestorage.toolbox.FileStorageToolbox;
import io.camunda.filestorage.FileRepoFactory;
import io.camunda.filestorage.FileVariable;
import io.camunda.filestorage.FileVariableReference;
import io.camunda.filestorage.StorageDefinition;
import io.camunda.filestorage.cmis.CmisParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UploadFile implements FileStorageSubFunction {

  private final Logger logger = LoggerFactory.getLogger(UploadFile.class.getName());


  public static final String GROUP_SOURCE = "Source";
  public static final String GROUP_PROCESS_FILE = "Process file";
  public static final String GROUP_STORAGE_DEFINITION = "Storage definition";


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

    File folder = FileStorageToolbox.getFolderFileFromName(input.getInputFolder());
    String fileName = input.getFileName();
    String filterFile = input.getFilterFile();
    String policy = input.getInputPolicy();
    String storageDefinitionSt = input.getStorageDefinition();
    StorageDefinition storageDefinition = null;

    try {
      // with a template, the storage definition is just the droptdown value, so add the complement if present
      storageDefinition = StorageDefinition.getFromString(storageDefinitionSt);
      String storageDefinitionFolderComplement = input.getStorageDefinitionFolderCompletement();
      if (storageDefinitionFolderComplement != null && !storageDefinitionFolderComplement.trim().isEmpty())
        storageDefinition.complement = storageDefinitionFolderComplement;

      storageDefinition.complementInObject = input.getGetInputStoragedefinitionCmisComplement();
    } catch (Exception e) {
      String cmisComplementSt = input.getGetInputStoragedefinitionCmisComplement();

      logger.error("Can't get the CMIS information- bad Gson value :" + cmisComplementSt);
      throw new ConnectorException(FileStorageError.BPMNERROR_INCORRECT_CMIS_PARAMETERS,
          "Worker [" + getSubFunctionName() + "] Cmis information" + cmisComplementSt);

    }

    File archiveFolder = FileStorageToolbox.getFolderFileFromName(input.getArchiveFolder());

    FileVariable fileVariable = null;

    if (folder == null) {
      String folderName = input.getInputFolder();
      String currentPath = null;
      try {
        currentPath = new File(".").getCanonicalPath();
      } catch (IOException e) {
        // This sould never arrive
      }
      logger.error(
          getSubFunctionName() + ": folder[" + folderName + "] does not exist (current local folder is [" + currentPath
              + "])");
      throw new ConnectorException(FileStorageError.BPMNERROR_FOLDER_NOT_EXIST,
          "Worker [" + getSubFunctionName() + "] folder[" + folderName + "] does not exist");
    }
    List<File> listFilesFiltered;
    File fileToProcess = null;
    if (folder.listFiles() == null) {
      listFilesFiltered = Collections.emptyList();
    } else {
      listFilesFiltered = Arrays.stream(folder.listFiles()).filter(t -> {
        if (fileName != null)
          return t.getName().equals(fileName);
        if ("*.*".equals(filterFile))
          return true;
        if (filterFile == null || filterFile.isEmpty())
          return true;
        return t.getName().matches(filterFile);
      }).toList();
    }
    if (listFilesFiltered.isEmpty()) {
      logger.info(
          getSubFunctionName() + ": folder [" + folder.getAbsolutePath() + "] does not have any matching file " + (
              fileName != null ?
                  "fileName[" + fileName + "]" :
                  "FilterFile[" + filterFile + "]"));
    } else {
      // load the first file only
      fileToProcess = listFilesFiltered.get(0);
      fileVariable = new FileVariable();
      byte[] content = new byte[(int) fileToProcess.length()];
      fileVariable.setValue(new byte[(int) fileToProcess.length()]);
      fileVariable.setName(fileToProcess.getName());
      fileVariable.setMimeType(FileVariable.getMimeTypeFromName(fileVariable.getName()));

      try (FileInputStream fis = new FileInputStream(fileToProcess)) {
        fis.read(content);
        fileVariable.setValue(content);
      } catch (Exception e) {
        logger.error(getSubFunctionName() + ": cannot read file[" + fileToProcess.getAbsolutePath() + "] : " + e);
        throw new ConnectorException(FileStorageError.BPMNERROR_LOAD_FILE_ERROR,
            "Worker [" + getSubFunctionName() + "]  cannot read file[" + fileToProcess.getAbsolutePath() + "] : " + e);
      }
    }

    // output
    FileStorageOutput fileStorageOutput = new FileStorageOutput();
    if (fileVariable != null) {
      try {
        fileVariable.setStorageDefinition(storageDefinition);
        FileRepoFactory fileRepoFactory = FileRepoFactory.getInstance();

        FileVariableReference fileVariableReference = fileRepoFactory.saveFileVariable(fileVariable);

        fileStorageOutput.fileLoaded = fileVariableReference.toJson();
      } catch (Exception e) {
        logger.error("Error during setFileVariableReference: " + e);
        throw new ConnectorException(FileStorageError.BPMNERROR_SAVE_FILEVARIABLE,
            "Worker [" + getSubFunctionName() + "] error during access storageDefinition[" + storageDefinition + "] :"
                + e);
      }
      fileStorageOutput.fileName = fileVariable.getName();
      fileStorageOutput.fileMimeType = fileVariable.getMimeType();
    } else {
      fileStorageOutput.fileLoaded = null;
      fileStorageOutput.fileName = null;
      fileStorageOutput.fileMimeType = null;
    }

    if (fileToProcess != null) {
      // according to the policy, move the file
      if (FileStorageInput.POLICY_V_UNCHANGE.equals(policy)) {
        // Nothing to do here
      } else if (FileStorageInput.POLICY_V_DELETE.equals(policy)) {
        fileToProcess.delete();
      } else if (FileStorageInput.POLICY_V_ARCHIVE.equals(policy)) {
        if (archiveFolder == null) {
          // Can't archive the file, archive folder does not exist
          String archiveFolderName = input.getArchiveFolder();
          String currentPath = null;
          try {
            currentPath = new File(".").getCanonicalPath();
          } catch (IOException e) {
          }
          logger.error(
              getSubFunctionName() + ": folder[" + archiveFolderName + "] does not exist (current local folder is ["
                  + currentPath + "])");
          throw new ConnectorException(FileStorageError.BPMNERROR_FOLDER_NOT_EXIST,
              "Worker [" + getSubFunctionName() + "] folder[" + folder.getAbsolutePath() + "] does not exist");

        }
        Path source = Paths.get(fileToProcess.getAbsolutePath());
        Path target = Paths.get(archiveFolder + "/" + fileToProcess.getName());

        try {
          // rename or move a file to other path
          // if target exists, throws FileAlreadyExistsException
          Files.move(source, target);
        } catch (Exception e) {
          logger.error(
              getSubFunctionName() + ": cannot apply the policy[" + policy + "] from source[" + source + "] to ["
                  + target + "] : " + e);
          throw new ConnectorException(FileStorageError.BPMNERROR_MOVE_FILE_ERROR,
              "Worker [" + getSubFunctionName() + "] cannot apply the policy[" + policy + "] from source[" + source
                  + "] to [" + target + "] : " + e);
        }
      }
    }
    return fileStorageOutput;
  }

  public List<FileRunnerParameter> getInputsParameter() {
    return Arrays.asList(

        (FileRunnerParameter) new FileRunnerParameter(FileStorageInput.INPUT_FOLDER, // name
            "Folder", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Specify the folder where the file will be loaded. Must be visible from the server.", 1).setGroup(
            GROUP_SOURCE),

        (FileRunnerParameter) new FileRunnerParameter(FileStorageInput.INPUT_FILE_NAME, // name
            "File name", // label
            String.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "Specify a file name, else the first file in the folder will be loaded", 1).setGroup(GROUP_SOURCE),

        (FileRunnerParameter) new FileRunnerParameter(FileStorageInput.INPUT_FILTER_FILE, "Filter file", String.class,
            // class
            RunnerParameter.Level.OPTIONAL, // level
            "If you didn't specify a fileName, a filter to select only part of files present in the folder",
            1).setDefaultValue("*.*").setGroup(GROUP_SOURCE),

        (FileRunnerParameter) new FileRunnerParameter(FileStorageInput.INPUT_POLICY, "Policy", String.class,
            RunnerParameter.Level.OPTIONAL,
            // level
            "Policy to manipulate the file after loading. With " + FileStorageInput.POLICY_V_ARCHIVE
                + ", the folder archive must be specify", 1).addChoice(FileStorageInput.POLICY_V_DELETE, "Delete")
            .addChoice(FileStorageInput.POLICY_V_ARCHIVE, "Archive")
            .addChoice(FileStorageInput.POLICY_V_UNCHANGE, "Unchange")
            .setVisibleInTemplate()
            .setDefaultValue(FileStorageInput.POLICY_V_UNCHANGE)
            .setGroup(GROUP_PROCESS_FILE),

        (FileRunnerParameter) new FileRunnerParameter(FileStorageInput.INPUT_ARCHIVE_FOLDER, "Archive folder",
            String.class, RunnerParameter.Level.OPTIONAL, // level
            "With the policy " + FileStorageInput.POLICY_V_ARCHIVE + ". File is moved in this folder.", 1).addCondition(
                FileStorageInput.INPUT_POLICY, Collections.singletonList(FileStorageInput.POLICY_V_ARCHIVE))
            .setGroup(GROUP_PROCESS_FILE),

        (FileRunnerParameter) new FileRunnerParameter(FileStorageInput.INPUT_STORAGEDEFINITION, "Storage definition",
            String.class, RunnerParameter.Level.OPTIONAL,
            // level
            "How to saved the FileVariable. " + StorageDefinition.StorageDefinitionType.JSON
                + " to save in the engine (size is linited), " + StorageDefinition.StorageDefinitionType.TEMPFOLDER
                + " to use the temporary folder of THIS machine" + StorageDefinition.StorageDefinitionType.FOLDER
                + " to specify a folder to save it (to be accessible by multiple machine if you ruin it in a cluster"
                + StorageDefinition.StorageDefinitionType.CMIS + " to specify a CMIS connection", 1).addChoice("JSON",
                StorageDefinition.StorageDefinitionType.JSON.toString())
            .addChoice("TEMPFOLDER", StorageDefinition.StorageDefinitionType.TEMPFOLDER.toString())
            .addChoice("FOLDER", StorageDefinition.StorageDefinitionType.FOLDER.toString())
            .addChoice("CMIS", StorageDefinition.StorageDefinitionType.CMIS.toString())
            .setVisibleInTemplate()
            .setDefaultValue(StorageDefinition.StorageDefinitionType.JSON.toString())
            .setGroup(GROUP_STORAGE_DEFINITION),

        (FileRunnerParameter) new FileRunnerParameter(FileStorageInput.INPUT_STORAGEDEFINITION_FOLDER_COMPLEMENT,
            "Folder Storage definition Complement", String.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "Complement to the Storage definition, if needed. " + StorageDefinition.StorageDefinitionType.FOLDER
                + ": please provide the folder to save the file", 1).addCondition(
                FileStorageInput.INPUT_STORAGEDEFINITION,
                Collections.singletonList(StorageDefinition.StorageDefinitionType.FOLDER.toString()))
            .setGroup(GROUP_STORAGE_DEFINITION),

        (FileRunnerParameter) new FileRunnerParameter(FileStorageInput.INPUT_STORAGEDEFINITION_CMIS_COMPLEMENT, // name
            "CMIS Storage definition Complement", // label
            Object.class, // type
            RunnerParameter.Level.OPTIONAL, // level
            "Complement to the Storage definition, if needed. " + StorageDefinition.StorageDefinitionType.FOLDER
                + ": please provide the folder to save the file") // parameter
            .setGsonTemplate(CmisParameters.getGsonTemplate()) // add Gson Template
            .addCondition(FileStorageInput.INPUT_STORAGEDEFINITION,
                Collections.singletonList(StorageDefinition.StorageDefinitionType.CMIS.toString()))
            .setGroup(GROUP_STORAGE_DEFINITION));
  }

  public List<FileRunnerParameter> getOutputsParameter() {
    return Arrays.asList(new FileRunnerParameter(FileStorageOutput.OUTPUT_FILE_LOADED, "File loaded", Object.class,
            RunnerParameter.Level.REQUIRED,
            "Name of the variable to save the file loaded.Content depend of the storage definition"),

        new FileRunnerParameter(FileStorageOutput.OUTPUT_FILE_NAME, "File name", String.class,
            RunnerParameter.Level.OPTIONAL, "Name of the file"),

        new FileRunnerParameter(FileStorageOutput.OUTPUT_FILE_MIMETYPE, "File Mime type", String.class,
            RunnerParameter.Level.OPTIONAL, "MimeType of the loaded file"));
  }

  public Map<String, String> getBpmnErrors() {
    return Map.of(FileStorageError.BPMNERROR_FOLDER_NOT_EXIST,FileStorageError.BPMNERROR_FOLDER_NOT_EXIST_EXPL, // error

        FileStorageError.BPMNERROR_LOAD_FILE_ERROR, FileStorageError.BPMNERROR_LOAD_FILE_ERROR_EXPL,

        FileStorageError.BPMNERROR_MOVE_FILE_ERROR, FileStorageError.BPMNERROR_MOVE_FILE_ERROR_EXPL,

        FileStorageError.ERROR_INCORRECT_STORAGEDEFINITION, FileStorageError.ERROR_INCORRECT_STORAGEDEFINITION_EXPL,

        FileStorageError.BPMNERROR_INCORRECT_CMIS_PARAMETERS, FileStorageError.BPMNERROR_INCORRECT_CMIS_PARAMETERS_EXPL);

  }

}
