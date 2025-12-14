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
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CTreeSitterParserTest {

    @Test
    void supportsCExtensions() {
        TestCTreeSitterParser parser = new TestCTreeSitterParser(() -> null, () -> {});

        assertThat(parser.supports(Path.of("file.c"))).isTrue();
        assertThat(parser.supports(Path.of("header.h"))).isTrue();
        assertThat(parser.supports(Path.of("lib.cpp"))).isFalse();
    }

    @Test
    void extractsStructsAndFunctions() {
        String source = """
                typedef struct Foo { int x; } Foo;
                int add(int a, int b) { return a + b; }
                """;

        TreeSitterSource tsSource = new TreeSitterSource(source, Path.of("sample.c"), StandardCharsets.UTF_8);
        Tree tree = mock(Tree.class);

        Node structName = node("type_identifier", source, source.indexOf("Foo"), source.indexOf("Foo") + 3, 0, 0);
        Node structNode = nodeWithField("struct_specifier", source,
                source.indexOf("struct"), source.indexOf("Foo;"), 0, 0, "name", structName);

        Node fnName = node("identifier", source, source.indexOf("add"), source.indexOf("add") + 3, 1, 1);
        Node fnParams = node("parameter_list", source, source.indexOf("(int a, int b)"), source.indexOf("(int a, int b)") + "(int a, int b)".length(), 1, 1);
        Node fnDeclWithParams = nodeWithFields("function_declarator", source,
                source.indexOf("add"), source.indexOf(")") + 1, 1, 1,
                List.of(), List.of(fnName, fnParams),
                List.of(field("declarator", fnName), field("parameters", fnParams)));
        Node fnNode = nodeWithField("function_definition", source,
                source.indexOf("int add"), source.indexOf("return a + b;") + "return a + b;".length(), 1, 1,
                "declarator", fnDeclWithParams);

        Node root = nodeWithChildren("translation_unit", source, 0, source.length(), 0, 2, List.of(structNode, fnNode));

        TestCTreeSitterParser parser = new TestCTreeSitterParser(() -> null, () -> {});

        List<TextChunk> chunks = parser.extractChunks(root, tree, tsSource);

        assertThat(chunks)
                .hasSize(2)
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("struct");
                    assertThat(chunk.entityName()).isEqualTo("Foo");
                    assertThat(chunk.language()).isEqualTo("c");
                })
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("function");
                    assertThat(chunk.entityName()).isEqualTo("add");
                    assertThat(chunk.language()).isEqualTo("c");
                    assertThat(chunk.attributes().get("parameters")).contains("(int a, int b)");
                });
    }

    @Test
    void parseReturnsEmptyWhenLanguageFailsToLoad(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("sample.c");
        Files.writeString(file, "int foo() { return 1; }");

        TestCTreeSitterParser parser = new TestCTreeSitterParser(() -> null, () -> {
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

    private static Node nodeWithChildren(String type, String source, int startByte, int endByte, int startRow, int endRow, List<Node> children) {
        Node node = node(type, source, startByte, endByte, startRow, endRow);
        when(node.getChildren()).thenReturn(children);
        when(node.getNamedChildren()).thenReturn(children);
        when(node.getChildCount()).thenReturn(children.size());
        for (int i = 0; i < children.size(); i++) {
            when(node.getChild(i)).thenReturn(Optional.of(children.get(i)));
        }
        return node;
    }

    private static Node nodeWithField(String type,
                                      String source,
                                      int startByte,
                                      int endByte,
                                      int startRow,
                                      int endRow,
                                      String fieldName,
                                      Node fieldNode) {
        Node node = node(type, source, startByte, endByte, startRow, endRow);
        when(node.getChildByFieldName(fieldName)).thenReturn(Optional.of(fieldNode));
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
        Node node = nodeWithChildren(type, source, startByte, endByte, startRow, endRow, children);
        when(node.getNamedChildren()).thenReturn(namedChildren);
        when(node.getChildByFieldName(anyString())).thenReturn(Optional.empty());
        for (Field field : fields) {
            when(node.getChildByFieldName(field.name())).thenReturn(Optional.of(field.node()));
        }
        return node;
    }

    private static final class TestCTreeSitterParser extends CTreeSitterParser {
        TestCTreeSitterParser(Supplier<Language> languageSupplier, Runnable loader) {
            super(languageSupplier, loader);
        }
    }
}

