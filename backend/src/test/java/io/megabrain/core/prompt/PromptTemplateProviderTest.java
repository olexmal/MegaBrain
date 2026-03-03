/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core.prompt;

import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromptTemplateProviderTest {

    @Test
    @DisplayName("loads default template from classpath")
    void testLoadFromClasspath() {
        // Arrange
        PromptTemplateConfiguration config = mock(PromptTemplateConfiguration.class);
        when(config.templatePath()).thenReturn("classpath:prompt-templates/default-rag-template.txt");
        when(config.systemTemplatePath()).thenReturn(Optional.empty());

        PromptTemplateProvider provider = new PromptTemplateProvider(config);

        // Act
        provider.init();
        PromptTemplate template = provider.getDefaultTemplate();

        // Assert
        assertThat(template).isNotNull();
        assertThat(provider.getSystemTemplate()).isEmpty();

        // Verify we can render it with variables
        Prompt prompt = template.apply(Map.of(
            "context", "public void test() {}",
            "question", "What does this code do?"
        ));

        String promptText = prompt.text();
        assertThat(promptText).contains("public void test() {}");
        assertThat(promptText).contains("What does this code do?");
        assertThat(promptText).contains("ROLE DEFINITION");
        assertThat(promptText).contains("CONSTRAINTS");
        assertThat(promptText).contains("INSTRUCTIONS");
        assertThat(promptText).contains("CONTEXT FORMATTING");
        assertThat(promptText).contains("CITATION REQUIREMENTS");
    }

    @Test
    @DisplayName("loads system template from classpath when configured")
    void testLoadSystemTemplate() {
        // Arrange
        PromptTemplateConfiguration config = mock(PromptTemplateConfiguration.class);
        when(config.templatePath()).thenReturn("classpath:prompt-templates/default-rag-template.txt");
        when(config.systemTemplatePath()).thenReturn(Optional.of("classpath:prompt-templates/system-rag-template.txt"));

        PromptTemplateProvider provider = new PromptTemplateProvider(config);

        // Act
        provider.init();
        Optional<PromptTemplate> systemTemplateOpt = provider.getSystemTemplate();

        // Assert
        assertThat(systemTemplateOpt).isPresent();
        
        Prompt sysPrompt = systemTemplateOpt.get().apply(Map.of(
            "context", "some context"
        ));
        assertThat(sysPrompt.text()).contains("You are an expert");
    }
}
