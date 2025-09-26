package io.camunda.filestorage;

import io.camunda.cherry.definition.RunnerDecorationTemplate;
import io.camunda.connector.filestorage.FileStorageFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementTemplateGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ElementTemplateGenerator.class.getName());

    public static void generate() {
        // Call the Cherry runtime
        try {
            RunnerDecorationTemplate runnerDecorationTemplate = new RunnerDecorationTemplate(new FileStorageFunction());
            runnerDecorationTemplate.generateElementTemplate("./element-templates/");
        } catch (Exception e) {
            logger.error("Error during generation", e);
        }
    }

    public static void main(String[] args) {
        generate();
    }
}
