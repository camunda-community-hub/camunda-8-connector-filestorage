package io.camunda.connector.filestorage.toolbox;

import io.camunda.connector.cherrytemplate.CherryInput;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileStorageParameter {
  private static final Logger logger = LoggerFactory.getLogger(FileStorageParameter.class.getName());

  private final String name;

  private final String label;
  private final Class<?> classParameter;
  private final  RunnerParameter.Level level;
  private final String explanation;
  private final int priority;
  private String groupId;
  private String defaultValue=null;
  private Boolean visibleInTemplate= Boolean.FALSE;
  public String condition;
  public String conditionEquals;

  public List<String> conditionOneOf;
  public List<String> choiceList;


  // we want to keep the order
  private final List<Map<String, String>> listOfChoices = new ArrayList<>();
  private final List<String> listRegisteredType = new ArrayList<>();

  /**
   * @param name name
   * @param label label
   * @param classParameter class
   * @param level level:  CherryInput.PARAMETER_MAP_LEVEL_REQUIRED or  CherryInput.PARAMETER_MAP_LEVEL_OPTIONAL
   * @param explanation explanation
   * @param priority       to order the parameters BETWEEN all functions, we use the priority field. Then, all priority
   */
  public FileStorageParameter(String name, String label, Class<?> classParameter, RunnerParameter.Level level,
                              String explanation,
                              int priority) {
    this.name = name;
    this.label = label;
    this.classParameter = classParameter;
    this.level = level;
    this.explanation = explanation;
    this.priority = priority;
  }

  public String getName() {
    return name;
  }

  public String getLabel() {
    return label;
  }

  public Class<?> getClassParameter() {
    return classParameter;
  }

  public  RunnerParameter.Level getLevel() {
    return level;
  }

  public String getExplanation() {
    return explanation;
  }

  public List<String> getListRegisteredType() {
    return listRegisteredType;
  }

  public void addRegisteredType(String type) {
    listRegisteredType.add(type);
  }

  public FileStorageParameter addChoice(String code, String displayName) {
    Map<String, String> oneChoice = new HashMap<>();
    oneChoice.put(CherryInput.PARAMETER_MAP_CHOICE_LIST_CODE, code);
    oneChoice.put(CherryInput.PARAMETER_MAP_CHOICE_LIST_DISPLAY_NAME, displayName);

    listOfChoices.add(oneChoice);
    return this;
  }

  public int getPriority() {
    return priority;
  }


  public FileStorageParameter setGroup(String groupId) {
    this.groupId = label.toLowerCase().replace(" ", "_");
    return this;
  }
  public FileStorageParameter setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  public FileStorageParameter setVisibleInTemplate() {
    this.visibleInTemplate = Boolean.TRUE;
    return this;
  }
  public FileStorageParameter addCondition(String property, List<String> oneOf) {
    this.condition = property;
    this.conditionOneOf = oneOf;
    return this;
  }




  public Map<String, Object> getMap(String parameterNameForCondition) {
    Map<String, Object> oneParameter = new HashMap<>();
    oneParameter.put(CherryInput.PARAMETER_MAP_NAME, name);
    oneParameter.put(CherryInput.PARAMETER_MAP_LABEL, label);
    oneParameter.put(CherryInput.PARAMETER_MAP_CLASS, classParameter);
    oneParameter.put(CherryInput.PARAMETER_MAP_LEVEL, level.toString());
    oneParameter.put(CherryInput.PARAMETER_MAP_EXPLANATION, explanation);

    oneParameter.put(CherryInput.PARAMETER_MAP_CONDITION, listOfChoices);
    oneParameter.put(CherryInput.PARAMETER_MAP_CHOICE_LIST, listOfChoices);

    if (!listRegisteredType.isEmpty()) {
      oneParameter.put(CherryInput.PARAMETER_MAP_CONDITION, parameterNameForCondition);
      oneParameter.put(CherryInput.PARAMETER_MAP_CONDITION_ONE_OF, listRegisteredType);
    }
    oneParameter.put(CherryInput.PARAMETER_MAP_GROUP, groupId);
    if (defaultValue!=null)
      oneParameter.put(CherryInput.PARAMETER_MAP_DEFAULT_VALUE, defaultValue);
    if (visibleInTemplate!=null)
      oneParameter.put(CherryInput.PARAMETER_MAP_VISIBLE_IN_TEMPLATE, visibleInTemplate);

    logger.info("PdfParameters getMap:{}", oneParameter);

    return oneParameter;
  }
}
