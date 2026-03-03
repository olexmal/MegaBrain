/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core.prompt;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.Optional;

/**
 * Configuration mapping for RAG prompt templates.
 */
@ConfigMapping(prefix = "megabrain.rag.prompt")
public interface PromptTemplateConfiguration {

    /**
     * Path to the default RAG prompt template.
     * Can be a classpath resource (e.g., classpath:prompt-templates/default-rag-template.txt)
     * or a file path (e.g., file:/path/to/template.txt).
     *
     * @return The template path
     */
    @WithDefault("classpath:prompt-templates/default-rag-template.txt")
    String templatePath();

    /**
     * Optional path to the system prompt template if separated from user question.
     * 
     * @return The system template path
     */
    Optional<String> systemTemplatePath();
}
