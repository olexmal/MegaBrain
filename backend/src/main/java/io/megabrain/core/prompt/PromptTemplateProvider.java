/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core.prompt;

import dev.langchain4j.model.input.PromptTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides access to configured prompt templates.
 */
@ApplicationScoped
public class PromptTemplateProvider {

    private final PromptTemplateConfiguration config;
    private PromptTemplate defaultTemplate;
    private PromptTemplate systemTemplate;
    private final Map<String, PromptTemplate> modelTemplatesCache = new ConcurrentHashMap<>();

    @Inject
    public PromptTemplateProvider(PromptTemplateConfiguration config) {
        this.config = config;
    }

    /**
     * Initializes the templates by loading them from configured paths.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        String path = config.templatePath();
        try {
            String content = loadContent(path);
            validateTemplateContent(content, path, true);
            this.defaultTemplate = PromptTemplate.from(content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt template from " + path, e);
        }

        if (config.systemTemplatePath().isPresent()) {
            String sysPath = config.systemTemplatePath().get();
            try {
                String sysContent = loadContent(sysPath);
                // System template usually doesn't need question, but might need context
                this.systemTemplate = PromptTemplate.from(sysContent);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load system prompt template from " + sysPath, e);
            }
        }

        Map<String, String> modelTemplates = config.modelTemplates();
        if (modelTemplates != null) {
            for (Map.Entry<String, String> entry : modelTemplates.entrySet()) {
                try {
                    String content = loadContent(entry.getValue());
                    validateTemplateContent(content, entry.getValue(), true);
                    modelTemplatesCache.put(entry.getKey(), PromptTemplate.from(content));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load model prompt template for '" + entry.getKey() + "' from " + entry.getValue(), e);
                }
            }
        }
    }

    private void validateTemplateContent(String content, String sourcePath, boolean isUserTemplate) {
        if (isUserTemplate) {
            if (!content.contains("{{question}}")) {
                throw new IllegalArgumentException("Template from " + sourcePath + " is missing required variable: {{question}}");
            }
            if (!content.contains("{{context}}")) {
                throw new IllegalArgumentException("Template from " + sourcePath + " is missing required variable: {{context}}");
            }
        }
    }

    /**
     * Gets the default RAG prompt template.
     * 
     * @return the prompt template
     */
    public PromptTemplate getDefaultTemplate() {
        if (defaultTemplate == null) {
            init();
        }
        return defaultTemplate;
    }

    /**
     * Gets the prompt template for a specific model, falling back to default if not configured.
     *
     * @param modelName the name of the model
     * @return the prompt template
     */
    public PromptTemplate getTemplateForModel(String modelName) {
        if (modelName == null) {
            return getDefaultTemplate();
        }
        if (defaultTemplate == null) {
            init();
        }
        return modelTemplatesCache.getOrDefault(modelName, defaultTemplate);
    }

    /**
     * Gets the optional system prompt template.
     *
     * @return the system prompt template, or empty if not configured
     */
    public Optional<PromptTemplate> getSystemTemplate() {
        if (defaultTemplate == null && systemTemplate == null) {
            init();
        }
        return Optional.ofNullable(systemTemplate);
    }

    private String loadContent(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            String resourcePath = path.substring("classpath:".length());
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    throw new IOException("Resource not found: " + resourcePath);
                }
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } else if (path.startsWith("file:")) {
            Path filePath = Paths.get(path.substring("file:".length()));
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } else {
            // Assume file path if no prefix
            return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        }
    }
}
