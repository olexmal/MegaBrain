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
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

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
class JavaTreeSitterParserTest {

    @Test
    void supportsJavaExtensions() {
        TestJavaTreeSitterParser parser = new TestJavaTreeSitterParser(() -> null, () -> {
        });

        assertThat(parser.supports(Path.of("Sample.java"))).isTrue();
        assertThat(parser.supports(Path.of("Sample.txt"))).isFalse();
    }

    @Test
    void extractsTypesConstructorsAndMethods() {
        String source = """
                package com.example;

                import java.util.List;

                @ClassAnn
                public class Foo<T> extends Base implements Serializable {
                    @Inject
                    public Foo() {}

                    @Deprecated
                    public List<String> bar(String input) throws IOException {
                        return List.of(input);
                    }
                }
                """;

        TreeSitterSource tsSource = new TreeSitterSource(source, Path.of("Foo.java"), StandardCharsets.UTF_8);
        Tree tree = mock(Tree.class);

        Node packageName = node("identifier", source, source.indexOf("com.example"), source.indexOf("com.example") + "com.example".length(), 0, 0);
        Node packageDecl = nodeWithFields("package_declaration", source,
                source.indexOf("package"), source.indexOf(";") + 1, 0, 0,
                List.of(), List.of(packageName),
                List.of(field("name", packageName)));

        Node importDecl = node("import_declaration", source,
                source.indexOf("import"), source.indexOf("import") + "import java.util.List;".length(), 2, 2);

        Node className = node("identifier", source, source.indexOf("Foo"), source.indexOf("Foo") + "Foo".length(), 5, 5);
        Node classModifiers = nodeWithChildren("modifiers", source,
                source.indexOf("@ClassAnn"), source.indexOf("class"), 4, 5,
                List.of(node("marker_annotation", source, source.indexOf("@ClassAnn"), source.indexOf("@ClassAnn") + "@ClassAnn".length(), 4, 4)));
        Node typeParams = node("type_parameters", source,
                source.indexOf("<T>"), source.indexOf("<T>") + 3, 5, 5);
        Node superclass = node("superclass", source,
                source.indexOf("extends"), source.indexOf("implements") - 1, 5, 5);
        Node interfaces = node("interfaces", source,
                source.indexOf("implements"), source.indexOf("{"), 5, 5);

        Node ctorName = node("identifier", source, source.indexOf("Foo()"), source.indexOf("Foo()") + "Foo".length(), 7, 7);
        Node ctorModifiers = nodeWithChildren("modifiers", source,
                source.indexOf("@Inject"), source.indexOf("public Foo") + "public".length(), 6, 7,
                List.of(node("marker_annotation", source, source.indexOf("@Inject"), source.indexOf("@Inject") + "@Inject".length(), 6, 6)));
        Node ctorParams = node("formal_parameters", source,
                source.indexOf("()"), source.indexOf("()") + 2, 7, 7);
        Node ctorNode = nodeWithFields("constructor_declaration", source,
                source.indexOf("public Foo"), source.indexOf("public Foo") + "public Foo() {}".length(), 7, 7,
                List.of(), List.of(ctorName, ctorModifiers, ctorParams),
                List.of(field("name", ctorName), field("modifiers", ctorModifiers), field("parameters", ctorParams)));

        Node methodName = node("identifier", source, source.indexOf("bar"), source.indexOf("bar") + "bar".length(), 10, 10);
        Node methodModifiers = nodeWithChildren("modifiers", source,
                source.indexOf("@Deprecated"), source.indexOf("public List") + "public".length(), 9, 10,
                List.of(node("marker_annotation", source, source.indexOf("@Deprecated"), source.indexOf("@Deprecated") + "@Deprecated".length(), 9, 9)));
        Node methodParams = node("formal_parameters", source,
                source.indexOf("(String input)"), source.indexOf("(String input)") + "(String input)".length(), 10, 10);
        Node methodReturn = node("type", source,
                source.indexOf("List<String>"), source.indexOf("List<String>") + "List<String>".length(), 10, 10);
        Node methodThrows = node("throws", source,
                source.indexOf("throws"), source.indexOf("{", source.indexOf("throws")), 10, 10);
        Node methodNode = nodeWithFields("method_declaration", source,
                source.indexOf("public List"), source.indexOf("return List.of") + "return List.of(input);".length(), 10, 12,
                List.of(), List.of(methodName, methodModifiers, methodParams, methodReturn, methodThrows),
                List.of(
                        field("name", methodName),
                        field("modifiers", methodModifiers),
                        field("parameters", methodParams),
                        field("type", methodReturn),
                        field("throws", methodThrows)
                ));

        Node classNode = nodeWithFields("class_declaration", source,
                source.indexOf("public class"), source.lastIndexOf("}") + 1, 5, 13,
                List.of(ctorNode, methodNode), List.of(classModifiers, className, typeParams, superclass, interfaces, ctorNode, methodNode),
                List.of(
                        field("name", className),
                        field("modifiers", classModifiers),
                        field("type_parameters", typeParams),
                        field("superclass", superclass),
                        field("interfaces", interfaces)
                ));

        Node root = nodeWithNamedChildren("program", source, 0, source.length(), 0, 13, List.of(packageDecl, importDecl, classNode));

        TestJavaTreeSitterParser parser = new TestJavaTreeSitterParser(() -> null, () -> {
        });

        List<TextChunk> chunks = parser.extractChunks(root, tree, tsSource);

        assertThat(chunks)
                .hasSize(3)
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("class");
                    assertThat(chunk.entityName()).isEqualTo("com.example.Foo");
                    assertThat(chunk.language()).isEqualTo("java");
                    assertThat(chunk.attributes()).containsEntry("package", "com.example");
                    assertThat(chunk.attributes().get("imports")).contains("java.util.List");
                    assertThat(chunk.attributes().get("modifiers")).contains("public");
                    assertThat(chunk.attributes().get("annotations")).contains("@ClassAnn");
                    assertThat(chunk.attributes().get("type_parameters")).contains("<T>");
                    assertThat(chunk.attributes().get("superclass")).contains("extends Base");
                    assertThat(chunk.attributes().get("interfaces")).contains("implements Serializable");
                })
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("constructor");
                    assertThat(chunk.entityName()).isEqualTo("com.example.Foo.Foo");
                    assertThat(chunk.language()).isEqualTo("java");
                    assertThat(chunk.attributes()).containsEntry("enclosing_type", "Foo");
                    assertThat(chunk.attributes().get("modifiers")).contains("public");
                    assertThat(chunk.attributes().get("annotations")).contains("@Inject");
                    assertThat(chunk.attributes()).containsEntry("parameters", "()");
                    assertThat(chunk.attributes()).isEmpty();
                })
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("method");
                    assertThat(chunk.entityName()).isEqualTo("com.example.Foo.bar");
                    assertThat(chunk.language()).isEqualTo("java");
                    assertThat(chunk.attributes()).containsEntry("enclosing_type", "Foo");
                    assertThat(chunk.attributes().get("modifiers")).contains("public");
                    assertThat(chunk.attributes().get("annotations")).contains("@Deprecated");
                    assertThat(chunk.attributes().get("parameters")).contains("(String input)");
                    assertThat(chunk.attributes().get("return_type")).contains("List<String>");
                    assertThat(chunk.attributes().get("throws")).contains("IOException");
                });
    }

    @Test
    void parseReturnsEmptyWhenLanguageFailsToLoad(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, "package demo; class Foo {}");

        TestJavaTreeSitterParser parser = new TestJavaTreeSitterParser(() -> null, () -> {
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
        Node node = nodeWithChildren(type, source, startByte, endByte, startRow, endRow, children);
        when(node.getNamedChildren()).thenReturn(namedChildren);
        when(node.getChildByFieldName(anyString())).thenReturn(Optional.empty());
        for (Field field : fields) {
            when(node.getChildByFieldName(field.name())).thenReturn(Optional.of(field.node()));
        }
        return node;
    }

    private static final class TestJavaTreeSitterParser extends JavaTreeSitterParser {
        TestJavaTreeSitterParser(Supplier<Language> languageSupplier, Runnable loader) {
            super(languageSupplier, loader);
        }
    }
}

