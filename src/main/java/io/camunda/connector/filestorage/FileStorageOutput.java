package io.camunda.connector.filestorage;

import io.camunda.connector.cherrytemplate.CherryOutput;
import io.camunda.connector.filestorage.toolbox.ParameterToolbox;

import java.util.List;
import java.util.Map;

public class FileStorageOutput implements CherryOutput {

  public static final String OUTPUT_FILE_LOADED = "fileLoaded";
  public static final String OUTPUT_FILE_NAME = "fileName";
  public static final String OUTPUT_FILE_MIMETYPE_LOADED = "fileMimeTypeLoaded";
  public static final String OUTPUT_FILE_IS_DOWNLOADED = "fileIsDownloaded";
  public static final String OUTPUT_FILE_IS_PURGED = "fileIsPurged";
  public static final String OUTPUT_NB_FILES_PROCESSED = "nbFilesProcessed";
  public String fileLoaded;
  public String fileName;
  public String fileMimeTypeLoaded;
  public boolean fileIsDownloaded;
  public boolean fileIsPurged;
  public int nbFilesProcessed = 0;

  @Override
  public List<Map<String, Object>> getOutputParameters() {
    return ParameterToolbox.getOutputParameters();
  }

}
