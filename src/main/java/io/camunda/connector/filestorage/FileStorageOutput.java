package io.camunda.connector.filestorage;

import io.camunda.connector.cherrytemplate.CherryInput;
import io.camunda.connector.cherrytemplate.CherryOutput;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import io.camunda.connector.filestorage.toolbox.FileStorageSubFunction;
import io.camunda.connector.filestorage.toolbox.ParameterToolbox;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileStorageOutput implements CherryOutput {

  public static final String OUTPUT_FILE_LOADED = "fileLoaded";

  public String fileLoaded;

  public static final String OUTPUT_FILE_NAME = "fileNameLoaded";
public String fileName;


  public static final String OUTPUT_FILE_MIMETYPE = "fileMimeType";
  public String fileMimeType;

  public String destinationFile;
  public List<String> listDestinationsFile = new ArrayList<>();

  public void addDestinationFileInList(String destinationFile) {
    listDestinationsFile.add(destinationFile);
  }

  public static final String OUTPUT_FILE_IS_PURGED = "filePurged";
public boolean fileIsPurged;

  @Override
  public List<Map<String, Object>> getOutputParameters() {
    return ParameterToolbox.getOuputParameters();
  }


}
