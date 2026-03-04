/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.core;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts citations from LLM-generated RAG answers (US-03-05 T2).
 * Parses format [Source: path:line] or [Source: path:lineStart-lineEnd].
 * Handles multiple citations per answer and malformed citations gracefully.
 */
@ApplicationScoped
public class CitationParser {

    private static final Logger LOG = Logger.getLogger(CitationParser.class);

    /**
     * Matches [Source: ...] with any content inside brackets. Case-sensitive "Source".
     */
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[Source:\\s*([^\\]]+)\\]");

    /**
     * Extracts all citations from answer text. Malformed citations are skipped and logged.
     *
     * @param answerText the full LLM answer text
     * @return list of successfully parsed citations (file path, line numbers); never null
     */
    public List<ExtractedCitation> parse(String answerText) {
        List<ExtractedCitation> result = new ArrayList<>();
        if (answerText == null || answerText.isBlank()) {
            return result;
        }
        Matcher matcher = CITATION_PATTERN.matcher(answerText);
        while (matcher.find()) {
            String inner = matcher.group(1).trim();
            String rawSegment = matcher.group(0);
            ExtractedCitation citation = parseInner(inner, rawSegment);
            if (citation != null) {
                result.add(citation);
            }
        }
        return result;
    }

    /**
     * Parses the inner part of a citation (content between "Source:" and "]").
     * Expected: "path:line" or "path:lineStart-lineEnd". Invalid entries return null.
     */
    ExtractedCitation parseInner(String inner, String rawSegment) {
        if (inner == null || inner.isBlank()) {
            LOG.debugf("Skipping empty citation segment: %s", rawSegment);
            return null;
        }
        int lastColon = inner.lastIndexOf(':');
        if (lastColon <= 0) {
            LOG.debugf("Skipping citation with no path:line: %s", rawSegment);
            return null;
        }
        String filePath = inner.substring(0, lastColon).trim();
        String linePart = inner.substring(lastColon + 1).trim();
        if (filePath.isEmpty() || linePart.isEmpty()) {
            LOG.debugf("Skipping citation with empty path or line: %s", rawSegment);
            return null;
        }
        int lineStart;
        int lineEnd;
        if (linePart.contains("-")) {
            String[] range = linePart.split("-", 2);
            try {
                lineStart = Integer.parseInt(range[0].trim());
                lineEnd = Integer.parseInt(range[1].trim());
            } catch (NumberFormatException e) {
                LOG.debugf("Skipping citation with invalid line range: %s", rawSegment);
                return null;
            }
            if (lineStart <= 0 || lineEnd <= 0 || lineStart > lineEnd) {
                LOG.debugf("Skipping citation with invalid line range values: %s", rawSegment);
                return null;
            }
        } else {
            try {
                lineStart = Integer.parseInt(linePart);
            } catch (NumberFormatException e) {
                LOG.debugf("Skipping citation with non-numeric line: %s", rawSegment);
                return null;
            }
            if (lineStart <= 0) {
                LOG.debugf("Skipping citation with non-positive line: %s", rawSegment);
                return null;
            }
            lineEnd = lineStart;
        }
        return ExtractedCitation.of(filePath, lineStart, lineEnd, rawSegment);
    }
}
