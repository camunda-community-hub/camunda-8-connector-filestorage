package io.camunda.connector.filestorage.toolbox;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import io.camunda.connector.filestorage.FileStorageInput;
import io.camunda.connector.filestorage.FileStorageOutput;

import java.util.List;
import java.util.Map;

public interface FileStorageSubFunction {
    FileStorageOutput executeSubFunction(FileStorageInput pdfInput, OutboundConnectorContext context)
            throws ConnectorException;

    List<RunnerParameter> getInputsParameter();

    List<RunnerParameter> getOutputsParameter();

    Map<String, String> getBpmnErrors();

    String getSubFunctionName();

    String getSubFunctionDescription();

    String getSubFunctionType();

}
