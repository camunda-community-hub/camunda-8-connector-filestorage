package io.camunda.connector.filestorage.toolbox;

import io.camunda.connector.cherrytemplate.RunnerParameter;

import java.util.ArrayList;
import java.util.List;

public class FileRunnerParameter extends RunnerParameter {

  private final List<String> listRegisteredType = new ArrayList<>();
  private int priority;

  public FileRunnerParameter(String name,
                             String label,
                             Class<?> classParameter,
                             Level level,
                             String explanation,
                             int priority) {

    super(name, label, classParameter, level, explanation);
    this.priority = priority;
  }

  public FileRunnerParameter(String name, String label, Class<?> classParameter, Level level, String explanation) {
    super(name, label, classParameter, level, explanation);
  }

  public int getPriority() {
    return priority;
  }

  public List<String> getListRegisteredType() {
    return listRegisteredType;
  }

  public void addRegisteredType(String type) {
    listRegisteredType.add(type);
  }

}
