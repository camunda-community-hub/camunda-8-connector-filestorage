package io.camunda.connector.filestorage;

public class FileStorageError {

  public static final String BPMNERROR_ACCESS_FILEVARIABLE = "ACCESS_FILEVARIABLE";
  public static final String BPMNERROR_ACCESS_FILEVARIABLE_EXPL = "Given file variable is not a Gson format";

  public static final String BPMNERROR_INCORRECT_FILESTORAGE = "INCORRECT_STORAGEDEFINITION";
  public static final String BPMNERROR_INCORRECT_FILESTORAGE_EXPL = "Incorrect storage definition";

  public static final String BPMNERROR_LOAD_FILE_ERROR = "LOAD_FILE_ERROR";
  public static final String BPMNERROR_LOAD_FILE_ERROR_EXPL = "Error during the load";

  public static final String BPMNERROR_WRITE_FILE_ERROR = "WRITE_FILE_ERROR";
  public static final String BPMNERROR_WRITE_FILE_ERROR_EXPL = "Error during the write";

  public static final String BPMNERROR_FOLDER_NOT_EXIST = "FOLDER_NOT_EXIST";
  public static final String BPMNERROR_FOLDER_NOT_EXIST_EXPL = "Folder does not exist, or not visible from the server";

  public static final String BPMNERROR_MOVE_FILE_ERROR = "MOVE_FILE_ERROR";
  public static final String BPMNERROR_MOVE_FILE_ERROR_EXPL = "Error when the file is moved to the archive directory";

  public static final String BPMNERROR_BAD_CMIS_PARAMETERS = "BAD_CMIS_PARAMETER";
  public static final String BPMNERROR_BAD_CMIS_PARAMETERS_EXPL = "GSON expected to get information to connect the repository";

  public static final String BPMNERROR_SAVE_FILEVARIABLE = "SAVE_FILEVARIABLE";

  public static final String ERROR_INCORRECT_STORAGEDEFINITION = "INCORRECT_STORAGEDEFINITION";
  public static final String ERROR_INCORRECT_STORAGEDEFINITION_EXPL = "Storage definition is incorrect";
}
