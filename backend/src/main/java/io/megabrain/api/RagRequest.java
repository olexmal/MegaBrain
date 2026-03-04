/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for RAG question (US-03-04, US-04-03).
 */
public class RagRequest {

    @NotBlank(message = "question is required")
    private String question;

    public RagRequest() {
    }

    public RagRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
