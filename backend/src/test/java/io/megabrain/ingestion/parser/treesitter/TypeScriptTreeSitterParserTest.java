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
class TypeScriptTreeSitterParserTest {

    @Test
    void supportsTypeScriptExtensions() {
        TestTypeScriptTreeSitterParser parser = new TestTypeScriptTreeSitterParser(() -> null, () -> {
        });

        assertThat(parser.supports(Path.of("app.ts"))).isTrue();
        assertThat(parser.supports(Path.of("component.tsx"))).isTrue();
        assertThat(parser.supports(Path.of("file.js"))).isFalse();
    }

    @Test
    void extractsTypesInterfacesFunctionsAndTypeAliases() {
        String source = """
                class Foo<T> extends Base {
                  qux(a: string): Promise<void> { return; }
                }

                interface Bar { value: number }
                type Baz = { flag: boolean };
                async function zap(v: number): Promise<number> { return v; }
                """;

        TreeSitterSource tsSource = new TreeSitterSource(source, Path.of("Foo.ts"), StandardCharsets.UTF_8);
        Tree tree = mock(Tree.class);

        Node className = node("identifier", source, source.indexOf("Foo"), source.indexOf("Foo") + 3, 0, 0);
        Node typeParams = node("type_parameters", source, source.indexOf("<T>"), source.indexOf("<T>") + 3, 0, 0);
        Node heritage = node("heritage_clause", source, source.indexOf("extends"), source.indexOf("{", source.indexOf("extends")), 0, 0);
        Node classMethodName = node("property_identifier", source, source.indexOf("qux"), source.indexOf("qux") + 3, 1, 1);
        Node classMethodParams = node("formal_parameters", source, source.indexOf("(a: string)"), source.indexOf("(a: string)") + "(a: string)".length(), 1, 1);
        Node classMethodReturn = node("type", source, source.indexOf("Promise<void>"), source.indexOf("Promise<void>") + "Promise<void>".length(), 1, 1);
        Node classMethod = nodeWithFields("method_signature", source,
                source.indexOf("qux"), source.indexOf("qux(a: string)") + "qux(a: string): Promise<void>".length(), 1, 1,
                List.of(), List.of(classMethodName, classMethodParams, classMethodReturn),
                List.of(
                        field("name", classMethodName),
                        field("parameters", classMethodParams),
                        field("type", classMethodReturn)
                ));

        Node classNode = nodeWithFields("class_declaration", source,
                source.indexOf("class"), source.indexOf("}"), 0, 2,
                List.of(classMethod), List.of(className, typeParams, heritage, classMethod),
                List.of(
                        field("name", className),
                        field("type_parameters", typeParams),
                        field("heritage_clause", heritage)
                ));

        Node interfaceName = node("type_identifier", source, source.indexOf("Bar"), source.indexOf("Bar") + 3, 4, 4);
        Node interfaceNode = nodeWithFields("interface_declaration", source,
                source.indexOf("interface"), source.indexOf("interface") + "interface Bar { value: number }".length(), 4, 4,
                List.of(), List.of(interfaceName),
                List.of(field("name", interfaceName)));

        Node typeName = node("type_identifier", source, source.indexOf("Baz"), source.indexOf("Baz") + 3, 5, 5);
        Node typeValue = node("object_type", source, source.indexOf("{ flag"), source.indexOf("};") + 1, 5, 5);
        int typeAliasStart = source.indexOf("type Baz");
        int typeAliasEnd = source.indexOf(";", typeAliasStart) + 1;
        Node typeAliasNode = nodeWithFields("type_alias_declaration", source,
                typeAliasStart, typeAliasEnd, 5, 5,
                List.of(), List.of(typeName, typeValue),
                List.of(field("name", typeName), field("value", typeValue)));

        Node fnName = node("identifier", source, source.indexOf("zap"), source.indexOf("zap") + 3, 6, 6);
        Node fnParams = node("formal_parameters", source, source.indexOf("(v: number)"), source.indexOf("(v: number)") + "(v: number)".length(), 6, 6);
        Node fnReturn = node("type", source, source.indexOf("Promise<number>"), source.indexOf("Promise<number>") + "Promise<number>".length(), 6, 6);
        Node fnNode = nodeWithFields("function_declaration", source,
                source.indexOf("async function"), source.length(), 6, 6,
                List.of(), List.of(fnName, fnParams, fnReturn),
                List.of(field("name", fnName), field("parameters", fnParams), field("type", fnReturn)));

        Node root = nodeWithNamedChildren("program", source, 0, source.length(), 0, 7, List.of(classNode, interfaceNode, typeAliasNode, fnNode));

        TestTypeScriptTreeSitterParser parser = new TestTypeScriptTreeSitterParser(() -> null, () -> {
        });

        List<TextChunk> chunks = parser.extractChunks(root, tree, tsSource);

        assertThat(chunks)
                .hasSize(5)
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("class");
                    assertThat(chunk.entityName()).isEqualTo("Foo");
                    assertThat(chunk.language()).isEqualTo("typescript");
                    assertThat(chunk.attributes().get("type_parameters")).contains("<T>");
                    assertThat(chunk.attributes().get("heritage")).contains("extends Base");
                })
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("method");
                    assertThat(chunk.entityName()).isEqualTo("Foo.qux");
                    assertThat(chunk.language()).isEqualTo("typescript");
                    assertThat(chunk.attributes().get("parameters")).contains("(a: string)");
                    assertThat(chunk.attributes().get("return_type")).contains("Promise<void>");
                })
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("interface");
                    assertThat(chunk.entityName()).isEqualTo("Bar");
                    assertThat(chunk.language()).isEqualTo("typescript");
                })
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("type_alias");
                    assertThat(chunk.entityName()).isEqualTo("Baz");
                    assertThat(chunk.language()).isEqualTo("typescript");
                    assertThat(chunk.attributes().get("value")).contains("flag: boolean");
                })
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("function");
                    assertThat(chunk.entityName()).isEqualTo("zap");
                    assertThat(chunk.language()).isEqualTo("typescript");
                    assertThat(chunk.attributes().get("parameters")).contains("(v: number)");
                    assertThat(chunk.attributes().get("return_type")).contains("Promise<number>");
                    assertThat(chunk.attributes().get("async")).isEqualTo("true");
                });
    }

    @Test
    void parseReturnsEmptyWhenLanguageFailsToLoad(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("sample.ts");
        Files.writeString(file, "function foo(x: number) { return x; }");

        TestTypeScriptTreeSitterParser parser = new TestTypeScriptTreeSitterParser(() -> null, () -> {
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

    private static final class TestTypeScriptTreeSitterParser extends io.megabrain.ingestion.parser.treesitter.TypeScriptTreeSitterParser {
        TestTypeScriptTreeSitterParser(Supplier<Language> languageSupplier, Runnable loader) {
            super(languageSupplier, loader);
        }
    }
}

