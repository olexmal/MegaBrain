/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.treesitter;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Node;
import io.github.treesitter.jtreesitter.Point;
import io.github.treesitter.jtreesitter.Tree;
import io.megabrain.ingestion.parser.TextChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JavaScriptTreeSitterParserTest {

    @Test
    void supportsJavaScriptExtensions() {
        TestJavaScriptTreeSitterParser parser = new TestJavaScriptTreeSitterParser(() -> null, () -> {
        });

        assertThat(parser.supports(Path.of("app.js"))).isTrue();
        assertThat(parser.supports(Path.of("component.jsx"))).isTrue();
        assertThat(parser.supports(Path.of("lib.mjs"))).isTrue();
        assertThat(parser.supports(Path.of("legacy.cjs"))).isTrue();
        assertThat(parser.supports(Path.of("file.ts"))).isFalse();
    }

    @Test
    void extractsClassesMethodsAndFunctions() {
        String source = """
                class Foo {
                  bar(x) { return x; }
                }

                async function baz(y) { return y; }
                """;

        TreeSitterSource tsSource = new TreeSitterSource(source, Path.of("Foo.js"), StandardCharsets.UTF_8);
        Tree tree = mock(Tree.class);

        Node className = node("identifier", source, source.indexOf("Foo"), source.indexOf("Foo") + 3, 0, 0);
        Node methodName = node("property_identifier", source, source.indexOf("bar"), source.indexOf("bar") + 3, 1, 1);
        Node methodParams = node("formal_parameters", source, source.indexOf("(x)"), source.indexOf("(x)") + 3, 1, 1);
        Node methodNode = nodeWithFields("method_definition", source,
                source.indexOf("bar"), source.indexOf("bar(x)") + "bar(x) { return x; }".length(), 1, 1,
                List.of(), List.of(methodName, methodParams),
                List.of(field("name", methodName), field("parameters", methodParams)));

        Node classNode = nodeWithFields("class_declaration", source,
                source.indexOf("class"), source.indexOf("}"), 0, 2,
                List.of(methodNode), List.of(className, methodNode),
                List.of(field("name", className)));

        Node fnName = node("identifier", source, source.indexOf("baz"), source.indexOf("baz") + 3, 4, 4);
        Node fnParams = node("formal_parameters", source, source.indexOf("(y)"), source.indexOf("(y)") + 3, 4, 4);
        Node fnNode = nodeWithFields("function_declaration", source,
                source.indexOf("async function"), source.length(), 4, 4,
                List.of(), List.of(fnName, fnParams),
                List.of(field("name", fnName), field("parameters", fnParams)));

        Node root = nodeWithNamedChildren("program", source, 0, source.length(), 0, 5, List.of(classNode, fnNode));

        TestJavaScriptTreeSitterParser parser = new TestJavaScriptTreeSitterParser(() -> null, () -> {
        });

        List<TextChunk> chunks = parser.extractChunks(root, tree, tsSource);

        assertThat(chunks)
                .hasSize(3)
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("class");
                    assertThat(chunk.entityName()).isEqualTo("Foo");
                    assertThat(chunk.language()).isEqualTo("javascript");
                })
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("method");
                    assertThat(chunk.entityName()).isEqualTo("Foo.bar");
                    assertThat(chunk.language()).isEqualTo("javascript");
                    assertThat(chunk.attributes().get("parameters")).contains("(x)");
                    assertThat(chunk.attributes()).containsEntry("async", "false");
                })
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("function");
                    assertThat(chunk.entityName()).isEqualTo("baz");
                    assertThat(chunk.language()).isEqualTo("javascript");
                    assertThat(chunk.attributes()).containsEntry("async", "true");
                    assertThat(chunk.attributes().get("parameters")).contains("(y)");
                });
    }

    @Test
    void parseReturnsEmptyWhenLanguageFailsToLoad(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("sample.js");
        Files.writeString(file, "function foo() { return 1; }");

        TestJavaScriptTreeSitterParser parser = new TestJavaScriptTreeSitterParser(() -> null, () -> {
        });

        assertThat(parser.parse(file)).isEmpty();
    }

    private static Node node(String type, String source, int startByte, int endByte, int startRow, int endRow) {
        Node node = mock(Node.class);
        when(node.getType()).thenReturn(type);
        when(node.getStartByte()).thenReturn(startByte);
        when(node.getEndByte()).thenReturn(endByte);
        when(node.getStartPoint()).thenReturn(new Point(startRow, 0));
        when(node.getEndPoint()).thenReturn(new Point(endRow, 0));
        when(node.getChildren()).thenReturn(List.of());
        when(node.getNamedChildren()).thenReturn(List.of());
        when(node.getChildCount()).thenReturn(0);
        when(node.getChild(0)).thenReturn(Optional.empty());
        when(node.getNamedChild(0)).thenReturn(Optional.empty());
        when(node.getParent()).thenReturn(Optional.empty());
        when(node.getText()).thenReturn(source.substring(startByte, endByte));
        return node;
    }

    private static Node nodeWithNamedChildren(String type, String source, int startByte, int endByte, int startRow, int endRow, List<Node> namedChildren) {
        Node node = node(type, source, startByte, endByte, startRow, endRow);
        when(node.getNamedChildren()).thenReturn(namedChildren);
        when(node.getChildren()).thenReturn(namedChildren);
        when(node.getChildCount()).thenReturn(namedChildren.size());
        for (int i = 0; i < namedChildren.size(); i++) {
            when(node.getNamedChild(i)).thenReturn(Optional.of(namedChildren.get(i)));
            when(node.getChild(i)).thenReturn(Optional.of(namedChildren.get(i)));
        }
        return node;
    }

    private record Field(String name, Node node) {
    }

    private static Field field(String name, Node node) {
        return new Field(name, node);
    }

    private static Node nodeWithFields(String type,
                                       String source,
                                       int startByte,
                                       int endByte,
                                       int startRow,
                                       int endRow,
                                       List<Node> children,
                                       List<Node> namedChildren,
                                       List<Field> fields) {
        Node node = nodeWithNamedChildren(type, source, startByte, endByte, startRow, endRow, namedChildren);
        when(node.getChildByFieldName(anyString())).thenReturn(Optional.empty());
        for (Field field : fields) {
            when(node.getChildByFieldName(field.name())).thenReturn(Optional.of(field.node()));
        }
        return node;
    }

    private static final class TestJavaScriptTreeSitterParser extends JavaScriptTreeSitterParser {
        TestJavaScriptTreeSitterParser(Supplier<Language> languageSupplier, Runnable loader) {
            super(languageSupplier, loader);
        }
    }
}

