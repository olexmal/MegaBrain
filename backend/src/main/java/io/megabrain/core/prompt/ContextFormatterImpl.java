/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core.prompt;

import io.megabrain.api.SearchResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of ContextFormatter.
 * Formats chunks consistently across languages, including markdown code blocks
 * if a language is specified.
 */
@ApplicationScoped
public class ContextFormatterImpl implements ContextFormatter {

    @Override
    public String format(SearchResult result) {
        if (result == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[Source: ").append(result.getSourceFile());

        if (result.getEntityName() != null && !result.getEntityName().isEmpty()) {
            sb.append(" - ").append(result.getEntityName());
            if ("method".equalsIgnoreCase(result.getEntityType()) || "function".equalsIgnoreCase(result.getEntityType())) {
                if (!result.getEntityName().endsWith(")") && !result.getEntityName().contains("(")) {
                    sb.append("()");
                }
            }
        }

        if (result.getLineRange() != null) {
            sb.append(" (lines ")
              .append(result.getLineRange().getStartLine())
              .append("-")
              .append(result.getLineRange().getEndLine())
              .append(")");
        }

        sb.append("]\n");

        String lang = result.getLanguage() != null ? result.getLanguage().toLowerCase() : "";
        sb.append("```").append(lang).append("\n");
        
        if (result.getContent() != null && !result.getContent().isEmpty()) {
            sb.append(result.getContent());
            if (!result.getContent().endsWith("\n")) {
                sb.append("\n");
            }
        }
        
        sb.append("```");

        return sb.toString();
    }

    @Override
    public String formatAll(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        return results.stream()
                .map(this::format)
                .collect(Collectors.joining("\n\n"));
    }
}
