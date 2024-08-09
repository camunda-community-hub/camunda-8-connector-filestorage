package io.camunda.connector.filestorage;

import io.camunda.connector.cherrytemplate.CherryOutput;
import io.camunda.connector.filestorage.toolbox.ParameterToolbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileStorageOutput implements CherryOutput {

  public static final String OUTPUT_FILE_LOADED = "fileLoaded";
  public static final String OUTPUT_FILE_NAME = "fileNameLoaded";
  public static final String OUTPUT_FILE_MIMETYPE = "fileMimeType";
  public static final String OUTPUT_FILE_IS_DONWLOADED = "filePurged";
  public static final String OUTPUT_FILE_IS_PURGED = "filePurged";
  public static final String OUTPUT_NB_FILES_PROCESSED = "nbFilesProcessed";
  public String fileLoaded;
  public String fileName;
  public String fileMimeType;
  public String destinationFile;
  public List<String> listDestinationsFile = new ArrayList<>();
  public boolean fileIsDownloaded;
  public boolean fileIsPurged;
  public int nbFilesProcessed = 0;

  public void addDestinationFileInList(String destinationFile) {
    listDestinationsFile.add(destinationFile);
  }

  @Override
  public List<Map<String, Object>> getOutputParameters() {
    return ParameterToolbox.getOutputParameters();
  }

}
