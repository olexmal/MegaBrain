/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.java;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import io.megabrain.ingestion.parser.CodeParser;
import io.megabrain.ingestion.parser.TextChunk;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Java parser implementation that converts Java source files into structured {@link TextChunk}s.
 * Uses JavaParser to extract classes, methods, fields, and nested/anonymous types.
 */
@ApplicationScoped
public class JavaParserService implements CodeParser {

    private static final Logger LOG = Logger.getLogger(JavaParserService.class);
    private static final String LANGUAGE = "java";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("java");

    private final JavaParser javaParser;

    public JavaParserService() {
        this(createDefaultParser());
    }

    JavaParserService(JavaParser javaParser) {
        this.javaParser = Objects.requireNonNull(javaParser, "javaParser");
    }

    @Override
    public boolean supports(Path filePath) {
        if (filePath == null) {
            return false;
        }
        String name = filePath.getFileName().toString().toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    @Override
    public List<TextChunk> parse(Path filePath) {
        Objects.requireNonNull(filePath, "filePath");

        if (!supports(filePath)) {
            return List.of();
        }
        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("File does not exist or is not a regular file: " + filePath);
        }

        String source;
        try {
            source = Files.readString(filePath);
        } catch (IOException e) {
            LOG.errorf(e, "Failed to read Java file: %s", filePath);
            return List.of();
        }

        try {
            ParseResult<CompilationUnit> result = javaParser.parse(source);
            result.getProblems().forEach(problem ->
                    LOG.warnf("Java parse issue in %s at %s: %s",
                            filePath,
                            problem.getLocation().map(Object::toString).orElse("unknown location"),
                            problem.getMessage())
            );

            Optional<CompilationUnit> compilationUnit = result.getResult();
            if (compilationUnit.isEmpty()) {
                LOG.warnf("No parse result produced for %s; returning empty chunk list", filePath);
                return List.of();
            }

            SourceCoordinates coordinates = new SourceCoordinates(source);
            String packageName = compilationUnit.get()
                    .getPackageDeclaration()
                    .map(pd -> pd.getName().asString())
                    .orElse("");

            int sourceByteLength = source.getBytes(StandardCharsets.UTF_8).length;
            JavaAstVisitor visitor = new JavaAstVisitor(
                    LANGUAGE,
                    packageName,
                    filePath,
                    source,
                    coordinates,
                    sourceByteLength
            );
            compilationUnit.get().accept(visitor, new JavaAstVisitor.Context());

            return List.copyOf(visitor.getChunks());
        } catch (Exception e) {
            LOG.errorf(e, "Failed to parse Java file %s", filePath);
            return List.of();
        }
    }

    @Override
    public String language() {
        return LANGUAGE;
    }

    private static JavaParser createDefaultParser() {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        configuration.setAttributeComments(false);
        configuration.setTabSize(4);
        configuration.setStoreTokens(true);
        return new JavaParser(configuration);
    }

}

