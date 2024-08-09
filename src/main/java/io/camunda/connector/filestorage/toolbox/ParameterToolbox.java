package io.camunda.connector.filestorage.toolbox;

import io.camunda.connector.cherrytemplate.RunnerParameter;
import io.camunda.connector.filestorage.FileStorageFunction;
import io.camunda.connector.filestorage.FileStorageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParameterToolbox {
  private static final Logger logger = LoggerFactory.getLogger(ParameterToolbox.class.getName());

  /**
   * This is a toolbox, only static method
   */
  private ParameterToolbox() {
  }

  public static List<Map<String, Object>> getInputParameters() {
    return getParameters(true);
  }

  public static List<Map<String, Object>> getOutputParameters() {
    return getParameters(false);
  }

  /**
   * Return the list of parameters
   *
   * @param inputParameters type of parameters (INPUT, OUTPUT)
   * @return list of parameters on a list of MAP
   */
  private static List<Map<String, Object>> getParameters(boolean inputParameters) {

    List<FileRunnerParameter> pdfParametersCollectList = new ArrayList<>();
    logger.info("getParameters input? {}", inputParameters);

    // add the "choose the function" parameters
    FileRunnerParameter chooseFunction = new FileRunnerParameter(FileStorageInput.INPUT_FILESTORAGEFUNCTION,
        "FileStorage Function", String.class, RunnerParameter.Level.REQUIRED, "Choose the function to execute", 0);

    // add the input only at the INPUT parameters
    if (inputParameters) {
      pdfParametersCollectList.add(chooseFunction);
    }

    // We keep a list of parameters per type. Then, we will add a condition according the type
    Map<String, List<String>> parameterPerFunction = new HashMap<>();

    //  now, we collect all functions, and for each function, we collect parameters
    for (Class<?> classFunction : FileStorageFunction.allFunctions) {
      try {
        Constructor<?> constructor = classFunction.getConstructor();
        FileStorageSubFunction inputSubFunction = (FileStorageSubFunction) constructor.newInstance();

        List<FileRunnerParameter> subFunctionsParametersList = inputParameters ?
            inputSubFunction.getInputsParameter() :
            inputSubFunction.getOutputsParameter();

        chooseFunction.addChoice(inputSubFunction.getSubFunctionType(), inputSubFunction.getSubFunctionName());
        logger.info("FileStorage SubFunctionName[{}] TypeChoice [{}] parameterList.size={}",
            inputSubFunction.getSubFunctionName(), inputSubFunction.getSubFunctionType(),
            subFunctionsParametersList.size());

        for (FileRunnerParameter parameter : subFunctionsParametersList) {

          // one parameter may be used by multiple functions, and we want to create only one, but play on condition to show it
          Optional<FileRunnerParameter> parameterInList = pdfParametersCollectList.stream()
              .filter(t -> t.getName().equals(parameter.getName()))
              .findFirst();
          if (parameterInList.isEmpty()) {
            parameter.addRegisteredType(inputSubFunction.getSubFunctionType());
            // We search where to add this parameter. It is at the end of the group with the same priority
            int positionToAdd = 0;
            for (FileRunnerParameter indexParameter : pdfParametersCollectList) {
              if (indexParameter.getPriority() <= parameter.getPriority())
                positionToAdd++;
            }
            pdfParametersCollectList.add(positionToAdd, parameter);
            logger.info("  check parameter[{}.{}] : New Add at [{}] newSize[{}] - registered in[{}]",
                inputSubFunction.getSubFunctionName(), parameter.getName(), positionToAdd,
                pdfParametersCollectList.size(), parameter.getListRegisteredType());
            // Already exist
          } else {
            // Register this function in that parameter
            parameterInList.get().addRegisteredType(inputSubFunction.getSubFunctionType());
            logger.info("  check parameter[{}.{}] : Already exist - registered in[{}]",
                inputSubFunction.getSubFunctionName(), parameter.getName(),
                parameterInList.get().getListRegisteredType());
          }
        }

      } catch (Exception e) {
        logger.error("Exception during the getInputParameters functions {}", e.toString());
      }
    }

    // Now, build the list from all parameters collected
    // We add a condition for each parameter
    for (FileRunnerParameter parameter : pdfParametersCollectList) {

      // There is an explicit condition: do not override it
      if (parameter.getCondition() != null)
        continue;

      List<String> listFunctionForThisParameter = parameter.getListRegisteredType();
      if (listFunctionForThisParameter == null || listFunctionForThisParameter.isEmpty()
          || listFunctionForThisParameter.size() == FileStorageFunction.allFunctions.size()) {
        logger.info("parameter [{}] Register in NO or ALL functions", parameter.getName());
      } else {
        logger.info("parameter [{}] Register in some functions [{}]", parameter.getName(),
            listFunctionForThisParameter);
        parameter.addCondition(chooseFunction.getName(), listFunctionForThisParameter);
      }
    }
    // first, the function selection
    logger.info(" FileStorageParameter => Map Size={}", pdfParametersCollectList.size());

    return pdfParametersCollectList.stream().map(t -> t.toMap(FileStorageInput.INPUT_FILESTORAGEFUNCTION)).toList();

  }

}
