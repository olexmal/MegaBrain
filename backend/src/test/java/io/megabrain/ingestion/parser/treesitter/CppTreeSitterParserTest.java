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
class CppTreeSitterParserTest {

    @Test
    void supportsCppExtensions() {
        TestCppTreeSitterParser parser = new TestCppTreeSitterParser(() -> null, () -> {});

        assertThat(parser.supports(Path.of("file.cpp"))).isTrue();
        assertThat(parser.supports(Path.of("file.hh"))).isTrue();
        assertThat(parser.supports(Path.of("file.c"))).isFalse();
    }

    @Test
    void extractsNamespacesClassesMethodsAndFunctions() {
        String source = """
                namespace ns {
                  template<typename T>
                  class Bar {
                    T baz(T v) { return v; }
                  };
                }
                int top(int x) { return x; }
                """;

        TreeSitterSource tsSource = new TreeSitterSource(source, Path.of("sample.cpp"), StandardCharsets.UTF_8);
        Tree tree = mock(Tree.class);

        Node nsName = node("identifier", source, source.indexOf("ns"), source.indexOf("ns") + 2, 0, 0);
        Node nsNode = nodeWithFieldAndChildren("namespace_definition", source,
                source.indexOf("namespace"), source.indexOf("}\nint"), 0, 5,
                "name", nsName, List.of());

        Node methodName = node("identifier", source, source.indexOf("baz"), source.indexOf("baz") + 3, 3, 3);
        Node methodParams = node("parameter_list", source,
                source.indexOf("(T v)"), source.indexOf("(T v)") + "(T v)".length(), 3, 3);
        Node methodType = node("type", source, source.indexOf("T baz"), source.indexOf("T baz") + 1, 3, 3);
        Node methodDecl = nodeWithFields("function_declarator", source,
                methodName.getStartByte(), source.indexOf("{ return v; }"), 3, 3,
                List.of(), List.of(methodName, methodParams),
                List.of(field("declarator", methodName), field("parameters", methodParams)));
        Node methodDef = nodeWithFields("function_definition", source,
                source.indexOf("T baz"), source.indexOf("return v;") + "return v;".length(), 3, 4,
                List.of(), List.of(methodType, methodDecl),
                List.of(field("type", methodType), field("declarator", methodDecl)));

        Node className = node("type_identifier", source, source.indexOf("Bar"), source.indexOf("Bar") + 3, 2, 2);
        Node classNode = nodeWithFields("class_specifier", source,
                source.indexOf("class"), source.indexOf("};") + 2, 2, 5,
                List.of(methodDef), List.of(className, methodDef),
                List.of(field("name", className), field("body", node("field_declaration_list", source, source.indexOf("{"), source.indexOf("};") + 1, 2, 5))));

        Node tplParams = node("template_parameter_list", source,
                source.indexOf("<typename T>"), source.indexOf("<typename T>") + "<typename T>".length(), 1, 1);
        Node tplNode = nodeWithFieldAndChildren("template_declaration", source,
                source.indexOf("template"), source.indexOf("class") - 1, 1, 1,
                "parameters", tplParams, List.of(classNode));

        Node topName = node("identifier", source, source.indexOf("top"), source.indexOf("top") + 3, 6, 6);
        Node topParams = node("parameter_list", source,
                source.indexOf("(int x)"), source.indexOf("(int x)") + "(int x)".length(), 6, 6);
        Node topDecl = nodeWithFields("function_declarator", source,
                source.indexOf("top"), source.indexOf("(int x)") + "(int x)".length(), 6, 6,
                List.of(), List.of(topName, topParams),
                List.of(field("declarator", topName), field("parameters", topParams)));
        Node topType = node("type", source, source.indexOf("int top"), source.indexOf("int top") + 3, 6, 6);
        Node topFunction = nodeWithFields("function_definition", source,
                source.indexOf("int top"), source.indexOf("return x;") + "return x;".length(), 6, 6,
                List.of(), List.of(topType, topDecl),
                List.of(field("type", topType), field("declarator", topDecl)));

        when(nsNode.getNamedChildren()).thenReturn(List.of(tplNode));
        when(tplNode.getNamedChildren()).thenReturn(List.of(classNode));
        when(classNode.getNamedChildren()).thenReturn(List.of(methodDef));
        when(methodDef.getNamedChildren()).thenReturn(List.of(methodType, methodDecl));

        Node root = nodeWithChildren("translation_unit", source, 0, source.length(), 0, 7, List.of(nsNode, topFunction));

        TestCppTreeSitterParser parser = new TestCppTreeSitterParser(() -> null, () -> {});

        List<TextChunk> chunks = parser.extractChunks(root, tree, tsSource);

        assertThat(chunks)
                .hasSize(3)
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("class");
                    assertThat(chunk.entityName()).isEqualTo("ns.Bar");
                    assertThat(chunk.language()).isEqualTo("cpp");
                })
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("method");
                    assertThat(chunk.entityName()).isEqualTo("ns.Bar.baz");
                    assertThat(chunk.language()).isEqualTo("cpp");
                    assertThat(chunk.attributes().get("parameters")).contains("(T v)");
                })
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("function");
                    assertThat(chunk.entityName()).isEqualTo("top");
                    assertThat(chunk.language()).isEqualTo("cpp");
                });
    }

    @Test
    void parseReturnsEmptyWhenLanguageFailsToLoad(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("sample.cpp");
        Files.writeString(file, "int foo() { return 1; }");

        TestCppTreeSitterParser parser = new TestCppTreeSitterParser(() -> null, () -> {
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

    private static Node nodeWithFieldAndChildren(String type,
                                                 String source,
                                                 int startByte,
                                                 int endByte,
                                                 int startRow,
                                                 int endRow,
                                                 String fieldName,
                                                 Node fieldNode,
                                                 List<Node> children) {
        Node node = nodeWithChildren(type, source, startByte, endByte, startRow, endRow, children);
        when(node.getChildByFieldName(fieldName)).thenReturn(Optional.of(fieldNode));
        return node;
    }

    private static final class TestCppTreeSitterParser extends io.megabrain.ingestion.parser.treesitter.CppTreeSitterParser {
        TestCppTreeSitterParser(Supplier<Language> languageSupplier, Runnable loader) {
            super(languageSupplier, loader);
        }
    }
}

