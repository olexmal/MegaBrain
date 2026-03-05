/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for RAG question (US-03-04, US-04-03).
 * Optional: context_limit (max context chunks), model (LLM model override).
 */
public class RagRequest {

    @NotBlank(message = "question is required")
    private String question;

    @Positive(message = "context_limit must be positive when provided")
    @JsonProperty("context_limit")
    private Integer contextLimit;

    private String model;

    public RagRequest() {
    }

    public RagRequest(String question) {
        this.question = question;
    }

    public RagRequest(String question, Integer contextLimit, String model) {
        this.question = question;
        this.contextLimit = contextLimit;
        this.model = model;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getContextLimit() {
        return contextLimit;
    }

    public void setContextLimit(Integer contextLimit) {
        this.contextLimit = contextLimit;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
