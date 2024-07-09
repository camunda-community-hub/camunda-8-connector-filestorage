package io.camunda.connector.filestorage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.camunda.connector.cherrytemplate.CherryInput;
import io.camunda.connector.filestorage.toolbox.FileStorageSubFunction;
import io.camunda.connector.filestorage.toolbox.ParameterToolbox;

import io.camunda.filestorage.StorageDefinition;
import io.camunda.filestorage.cmis.CmisParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * the JsonIgnoreProperties is mandatory: the template may contain additional widget to help the designer, especially on the OPTIONAL parameters
 * This avoids the MAPPING Exception
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileStorageInput implements CherryInput {
  /**
   * Attention, each Input here must be added in the PdfFunction, list of InputVariables
   */
  public static final String INPUT_FILESTORAGEFUNCTION = "fileStorageFunction";
  public String fileStorageFunction;

  /**
   * Input need for Upload
   */

  /**
   * Upload
   */
  public static final String INPUT_FOLDER = "folder";
  public static final String INPUT_FILE_NAME = "fileName";
  public static final String INPUT_FILTER_FILE = "filterFile";
  public static final String INPUT_POLICY = "policy";
  public static final String POLICY_V_DELETE = "DELETE";
  public static final String POLICY_V_ARCHIVE = "ARCHIVE";
  public static final String POLICY_V_UNCHANGE = "UNCHANGE";
  public static final String INPUT_STORAGEDEFINITION = "storageDefinition";
  public static final String INPUT_STORAGEDEFINITION_FOLDER_COMPLEMENT = "storageDefinitionComplement";
  public static final String INPUT_STORAGEDEFINITION_CMIS_COMPLEMENT = "storageDefinitionCmisComplement";

  public static final String INPUT_ARCHIVE_FOLDER = "archiveFolder";

  public static final String INPUT_SOURCE_FILE = "sourceFile";
  public String sourceFile;

  public String getSourceFile() {
    return sourceFile;
  }

  public static final String INPUT_FOLDER_TO_SAVE = "folder";
  public String folderToSave;

  public String getFolderToSave() {
    return folderToSave;
  }

  public String getFileStorageFunction() {
    return fileStorageFunction;
  }

  public String inputFolder;

  public String getInputFolder() {
    return inputFolder;
  }

  public String fileName;

  public String getFileName() {
    return fileName;
  }

  public String filterFile;

  public String getFilterFile() {
    return filterFile;
  }

  public String inputPolicy;

  public String getInputPolicy() {
    return inputPolicy;
  }

  public String storageDefinition;

  public String getStorageDefinition() {
    return storageDefinition;
  }

  public String storageDefinitionFolderCompletement;

  public String getStorageDefinitionFolderCompletement() {
    return storageDefinitionFolderCompletement;
  }

  public String getInputStoragedefinitionCmisComplement;

  public String getGetInputStoragedefinitionCmisComplement() {
    return getInputStoragedefinitionCmisComplement;
  }

  public String archiveFolder;

  public String getArchiveFolder() {
    return archiveFolder;
  }

  @Override
  public List<Map<String, Object>> getInputParameters() {
    return ParameterToolbox.getInputParameters();
  }
}
