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

    FileVariable fileVariable = null;

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
    FileStorageOutput fileStorageOutput = new FileStorageOutput();
    if (listFilesFiltered.isEmpty()) {
      traceExecution.append(" Folder Does not have any matching file. Filename[");
      traceExecution.append(fileName);
      traceExecution.append("] FilterFile[");
      traceExecution.append(filterFile);
      traceExecution.append("], ");
      fileStorageOutput.nbFilesProcessed = 0;
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
        logger.error("Cannot read file[{}] {} : {}", fileToProcess.getAbsolutePath(), traceExecution, e);
        throw new ConnectorException(FileStorageError.BPMNERROR_LOAD_FILE_ERROR,
            "Worker [" + getSubFunctionName() + "]  cannot read file[" + fileToProcess.getAbsolutePath() + "] : " + e);
      }
      traceExecution.append("Read FileName[");
      traceExecution.append(fileToProcess.getName());
      traceExecution.append("] size[");
      traceExecution.append(fileToProcess.length());
      traceExecution.append("], ");
    }

    //------ Storage Definition
    // Move to the FileStorage
    StorageDefinition storageDefinition = input.getStorageDefinitionObject();
    // Move to the file storage
    traceExecution.append("Move to FileStorage[");
    traceExecution.append(storageDefinition.getInformation());
    traceExecution.append("]");

    if (fileVariable != null) {
      try {
        fileVariable.setStorageDefinition(storageDefinition);
        FileRepoFactory fileRepoFactory = FileRepoFactory.getInstance();

        long beginOperation = System.currentTimeMillis();
        FileVariableReference fileVariableReference = fileRepoFactory.saveFileVariable(fileVariable);
        traceExecution.append(" in ");
        traceExecution.append(System.currentTimeMillis() - beginOperation);
        traceExecution.append(" ms, ");

        fileStorageOutput.fileLoaded = fileVariableReference.toJson();
        fileStorageOutput.nbFilesProcessed++;

      } catch (Exception e) {
        logger.error("Error during setFileVariableReference: {} : {} ", traceExecution, e);
        throw new ConnectorException(FileStorageError.BPMNERROR_SAVE_FILEVARIABLE,
            "Worker [" + getSubFunctionName() + "] error during access storageDefinition[" + storageDefinition + "] :"
                + e);
      }
      fileStorageOutput.fileName = fileVariable.getName();
      fileStorageOutput.fileMimeTypeLoaded = fileVariable.getMimeType();
    } else {
      fileStorageOutput.fileLoaded = null;
      fileStorageOutput.fileName = null;
      fileStorageOutput.fileMimeTypeLoaded = null;
    }

    //------ Policy after operation Definition
    // Apply the policy
    File archiveFolder = FileStorageToolbox.getFolderFileFromName(input.getArchiveFolder());
    String policy = input.getPolicy();
    traceExecution.append("PolicyArchive[");
    traceExecution.append(policy);
    traceExecution.append("]");

    if (fileToProcess != null) {
      // according to the policy, move the file
      if (FileStorageInput.POLICY_V_UNCHANGE.equals(policy)) {
        // Nothing to do here
      } else if (FileStorageInput.POLICY_V_DELETE.equals(policy)) {
        fileToProcess.delete();
      } else if (FileStorageInput.POLICY_V_ARCHIVE.equals(policy)) {
        traceExecution.append("ArchiveFolder[");
        traceExecution.append(archiveFolder);
        traceExecution.append("] ");
        if (archiveFolder == null) {
          // Can't archive the file, archive folder does not exist
          String archiveFolderName = input.getArchiveFolder();
          String currentPath = null;
          try {
            currentPath = new File(".").getCanonicalPath();
          } catch (IOException e) {
          }
          logger.error("Folder[{}] does not exist (current local folder is [{}] {}", archiveFolderName, currentPath,
              traceExecution);
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

        RunnerParameter.getInstance(FileStorageInput.INPUT_ARCHIVE_FOLDER, "Archive folder", String.class,
                RunnerParameter.Level.REQUIRED, // level
                "With the policy " + FileStorageInput.POLICY_V_ARCHIVE + ". File is moved in this folder.") //
            .addCondition(FileStorageInput.INPUT_POLICY, Collections.singletonList(FileStorageInput.POLICY_V_ARCHIVE))
            .setGroup(GROUP_PROCESS_FILE)
            .addCondition(FileStorageInput.INPUT_POLICY, Collections.singletonList(FileStorageInput.POLICY_V_ARCHIVE)),

        RunnerParameter.getInstance(FileStorageInput.INPUT_STORAGEDEFINITION, "Storage definition", String.class,
                RunnerParameter.Level.REQUIRED,
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
            .setGroup(GROUP_STORAGE_DEFINITION));
  }

  public List<RunnerParameter> getOutputsParameter() {
    return Arrays.asList(RunnerParameter.getInstance(FileStorageOutput.OUTPUT_FILE_LOADED, //
            "File loaded", //
            Object.class, //
            RunnerParameter.Level.REQUIRED, //
            "Name of the variable to save the file loaded.Content depend of the storage definition"),

        RunnerParameter.getInstance(FileStorageOutput.OUTPUT_FILE_NAME, //
            "File name", //
            String.class, //
            RunnerParameter.Level.OPTIONAL,  //
            "Name of the file"),

        RunnerParameter.getInstance(FileStorageOutput.OUTPUT_FILE_MIMETYPE_LOADED, //
            "File Mime type", //
            String.class, //
            RunnerParameter.Level.OPTIONAL, //
            "MimeType of the loaded file"),

        RunnerParameter.getInstance(FileStorageOutput.OUTPUT_NB_FILES_PROCESSED, //
            "Nb files processed", //
            String.class, //
            RunnerParameter.Level.OPTIONAL, //
            "Number of files processed. May be 1 or 0 (no file found)"));
  }

  public Map<String, String> getBpmnErrors() {
    return Map.of(FileStorageError.BPMNERROR_FOLDER_NOT_EXIST, FileStorageError.BPMNERROR_FOLDER_NOT_EXIST_EXPL, // error
        FileStorageError.BPMNERROR_LOAD_FILE_ERROR, FileStorageError.BPMNERROR_LOAD_FILE_ERROR_EXPL, //
        FileStorageError.BPMNERROR_MOVE_FILE_ERROR, FileStorageError.BPMNERROR_MOVE_FILE_ERROR_EXPL, //
        FileStorageError.ERROR_INCORRECT_STORAGEDEFINITION, FileStorageError.ERROR_INCORRECT_STORAGEDEFINITION_EXPL, //
        FileStorageError.BPMNERROR_BAD_CMIS_PARAMETERS, FileStorageError.BPMNERROR_BAD_CMIS_PARAMETERS_EXPL); //

  }

}
