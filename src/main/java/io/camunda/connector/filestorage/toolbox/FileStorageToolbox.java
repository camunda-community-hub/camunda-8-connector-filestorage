package io.camunda.connector.filestorage.toolbox;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.filestorage.FileStorageError;
import io.camunda.filestorage.FileVariableReference;

import java.io.File;

public class FileStorageToolbox {



  public static File getFolderFileFromName(String folderName) {
    if (folderName == null)
      return null;
    String folderSimpleName = folderName.replace("\\\"", "").trim();
    File folder = new File(folderSimpleName);
    if (!(folder.exists() && folder.isDirectory())) {
      return null;
    }
    return folder;
  }


}
