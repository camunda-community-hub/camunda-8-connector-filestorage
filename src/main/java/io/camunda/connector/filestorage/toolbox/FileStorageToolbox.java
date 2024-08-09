package io.camunda.connector.filestorage.toolbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FileStorageToolbox {
  private final static Logger logger = LoggerFactory.getLogger(FileStorageToolbox.class.getName());

  public static File getFolderFileFromName(String folderName) {
    if (folderName == null)
      return null;
    if (folderName.startsWith(".")) {
      try {
        String currentPath = new File(".").getCanonicalPath();
        folderName = currentPath + folderName.substring(1);
      } catch (Exception e) {
        // Can't get the local path
        logger.error("Can't get local path and folder start with [.] {} : {}", folderName, e);
      }
    }
    String folderSimpleName = folderName.replace("\\\"", "").replace("\\", "/").trim();
    File folder = new File(folderSimpleName);
    if (!(folder.exists() && folder.isDirectory())) {
      return null;
    }
    return folder;
  }

}
