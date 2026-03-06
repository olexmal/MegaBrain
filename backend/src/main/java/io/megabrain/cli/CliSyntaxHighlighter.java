/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.cli;

import org.fusesource.jansi.AnsiConsole;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Keyword/pattern-based syntax highlighter for CLI snippets.
 * Supports Java, Python, JavaScript; other languages fall back to plain text.
 * Uses Jansi for ANSI codes; null/blank language or useColor false returns content unchanged.
 */
@ApplicationScoped
public class CliSyntaxHighlighter implements SyntaxHighlighter {

    /** Languages that have highlighting rules; others fall back to plain. */
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("java", "python", "javascript", "typescript");

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_KEYWORD = "\u001B[33m";   // yellow/bright
    private static final String ANSI_STRING = "\u001B[32m";   // green
    private static final String ANSI_NUMBER = "\u001B[35m";    // magenta
    private static final String ANSI_COMMENT = "\u001B[90m";   // bright black/gray

    // Java/JS/TS keywords (common subset)
    private static final Pattern KEYWORD_PATTERN = Pattern.compile(
        "\\b(public|private|protected|static|final|class|interface|extends|implements|void|return|if|else|for|while|do|switch|case|break|continue|try|catch|finally|throw|throws|new|this|super|import|package|def|lambda|async|await|const|let|var|function|true|false|null)\\b"
    );
    // Python keywords
    private static final Pattern PYTHON_KEYWORD_PATTERN = Pattern.compile(
        "\\b(def|class|if|elif|else|for|while|try|except|finally|with|as|import|from|return|yield|lambda|and|or|not|in|is|None|True|False|async|await)\\b"
    );
    // Number (integer or float)
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+\\.?\\d*\\b");

    static {
        AnsiConsole.systemInstall();
    }

    @Override
    public String highlight(String content, String language, boolean useColor) {
        if (content == null) {
            return "";
        }
        if (!useColor || language == null || language.isBlank()) {
            return content;
        }
        String lang = language.trim().toLowerCase();
        if (!SUPPORTED_LANGUAGES.contains(lang)) {
            return content;
        }
        try {
            return applyHighlight(content, lang);
        } catch (Exception e) {
            return content;
        }
    }

    private String applyHighlight(String content, String lang) {
        if (content.isEmpty()) {
            return content;
        }
        if ("python".equals(lang)) {
            return highlightPython(content);
        }
        return highlightJavaLike(content);
    }

    private String highlightJavaLike(String content) {
        StringBuilder sb = new StringBuilder(content.length() * 2);
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(highlightLineJavaLike(lines[i]));
        }
        return sb.toString();
    }

    private String highlightLineJavaLike(String line) {
        String rest = line;
        StringBuilder out = new StringBuilder();
        int idx = 0;
        while (idx < rest.length()) {
            int commentStart = rest.indexOf("//", idx);
            if (commentStart >= 0) {
                out.append(highlightSegment(rest.substring(idx, commentStart), true));
                out.append(ANSI_COMMENT).append(rest.substring(commentStart)).append(ANSI_RESET);
                return out.toString();
            }
            int nextDq = rest.indexOf('"', idx);
            int nextSq = rest.indexOf('\'', idx);
            int next = rest.length();
            char quote = 0;
            if (nextDq >= 0 && nextDq < next) {
                next = nextDq;
                quote = '"';
            }
            if (nextSq >= 0 && nextSq < next) {
                next = nextSq;
                quote = '\'';
            }
            String segment = rest.substring(idx, next);
            out.append(highlightSegment(segment, true));
            if (quote != 0 && next < rest.length()) {
                int end = next + 1;
                while (end < rest.length()) {
                    char c = rest.charAt(end);
                    if (c == '\\') {
                        end += 2;
                        continue;
                    }
                    if (c == quote) {
                        end++;
                        break;
                    }
                    end++;
                }
                out.append(ANSI_STRING).append(rest.substring(next, end)).append(ANSI_RESET);
                idx = end;
            } else {
                idx = next;
            }
        }
        return out.toString();
    }

    private String highlightSegment(String segment, boolean javaLike) {
        if (segment.isEmpty()) {
            return segment;
        }
        Pattern kw = javaLike ? KEYWORD_PATTERN : PYTHON_KEYWORD_PATTERN;
        String s = segment;
        java.util.regex.Matcher m = kw.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, ANSI_KEYWORD + m.group() + ANSI_RESET);
        }
        m.appendTail(sb);
        s = sb.toString();
        m = NUMBER_PATTERN.matcher(s);
        sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, ANSI_NUMBER + m.group() + ANSI_RESET);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String highlightPython(String content) {
        StringBuilder sb = new StringBuilder(content.length() * 2);
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            String line = lines[i];
            int hash = line.indexOf('#');
            if (hash >= 0) {
                sb.append(highlightSegment(line.substring(0, hash), false));
                sb.append(ANSI_COMMENT).append(line.substring(hash)).append(ANSI_RESET);
            } else {
                sb.append(highlightLinePythonStrings(line));
            }
        }
        return sb.toString();
    }

    private String highlightLinePythonStrings(String line) {
        StringBuilder out = new StringBuilder();
        int idx = 0;
        while (idx < line.length()) {
            int nextDq = line.indexOf('"', idx);
            int nextSq = line.indexOf('\'', idx);
            int next = line.length();
            char quote = 0;
            if (nextDq >= 0 && nextDq < next) {
                next = nextDq;
                quote = '"';
            }
            if (nextSq >= 0 && nextSq < next) {
                next = nextSq;
                quote = '\'';
            }
            String segment = line.substring(idx, next);
            out.append(highlightSegment(segment, false));
            if (quote != 0 && next < line.length()) {
                int end = next + 1;
                while (end < line.length()) {
                    char c = line.charAt(end);
                    if (c == '\\') {
                        end += 2;
                        continue;
                    }
                    if (c == quote) {
                        end++;
                        break;
                    }
                    end++;
                }
                out.append(ANSI_STRING).append(line.substring(next, end)).append(ANSI_RESET);
                idx = end;
            } else {
                idx = next;
            }
        }
        return out.toString();
    }
}
