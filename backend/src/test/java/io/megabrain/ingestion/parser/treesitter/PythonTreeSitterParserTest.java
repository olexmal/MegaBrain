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
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PythonTreeSitterParserTest {

    @Test
    void supportsPythonExtensions() {
        TestPythonTreeSitterParser parser = new TestPythonTreeSitterParser(() -> null, () -> {
        });

        assertThat(parser.supports(Path.of("sample.py"))).isTrue();
        assertThat(parser.supports(Path.of("sample.pyw"))).isTrue();
        assertThat(parser.supports(Path.of("sample.pyi"))).isTrue();
        assertThat(parser.supports(Path.of("sample.txt"))).isFalse();
    }

    @Test
    void extractsFunctionsClassesAndMethods() {
        String source = """
                @dec
                async def top_fn(x: int) -> str:
                    \"\"\"doc\"\"\"
                    return x

                class Foo:
                    def method(self, y):
                        return y
                """;

        TreeSitterSource tsSource = new TreeSitterSource(source, Path.of("sample.py"), StandardCharsets.UTF_8);
        Tree tree = mock(Tree.class);

        Node decorator = node("decorator", source, 0, 5, 0, 5);

        Node fnName = node("identifier", source, source.indexOf("top_fn"), source.indexOf("top_fn") + "top_fn".length(), 1, 1);
        Node fnParams = node("parameters", source, source.indexOf("(x: int)"), source.indexOf("(x: int)") + "(x: int)".length(), 1, 1);
        Node fnReturn = node("type", source, source.indexOf("str"), source.indexOf("str") + 3, 1, 1);
        Node fnDocString = node("string", source, source.indexOf("\"\"\"doc\"\"\""), source.indexOf("\"\"\"doc\"\"\"") + "\"\"\"doc\"\"\"".length(), 2, 2);
        Node fnExprStmt = nodeWithChildren("expression_statement", source, fnDocString.getStartByte(), fnDocString.getEndByte(), 2, 2, List.of(fnDocString));
        Node fnBody = nodeWithNamedChildren("block", source, source.indexOf("    \"\"\"doc\"\"\""), source.indexOf("return x") + "return x".length(), 2, 3, List.of(fnExprStmt));

        Node topFunction = nodeWithFields("function_definition", source,
                source.indexOf("async def"), source.indexOf("return x") + "return x".length(), 1, 3,
                List.of(), List.of(fnName, fnParams, fnReturn, fnBody),
                List.of(field("name", fnName), field("parameters", fnParams), field("return_type", fnReturn), field("body", fnBody)));

        Node decoratedDef = nodeWithNamedChildren("decorated_definition", source, 0, topFunction.getEndByte(), 0, 3, List.of(decorator, topFunction));
        when(decorator.getChildren()).thenReturn(List.of());
        when(decorator.getNamedChildren()).thenReturn(List.of());

        Node methodName = node("identifier", source, source.indexOf("method"), source.indexOf("method") + "method".length(), 6, 6);
        Node methodParams = node("parameters", source, source.indexOf("(self, y)"), source.indexOf("(self, y)") + "(self, y)".length(), 6, 6);
        Node methodBody = nodeWithNamedChildren("block", source,
                source.indexOf("        return y"), source.indexOf("return y") + "return y".length(), 7, 7, List.of());
        Node methodFunction = nodeWithFields("function_definition", source,
                source.indexOf("def method"), source.indexOf("return y") + "return y".length(), 6, 7,
                List.of(), List.of(methodName, methodParams, methodBody),
                List.of(field("name", methodName), field("parameters", methodParams), field("body", methodBody)));

        Node className = node("identifier", source, source.indexOf("Foo"), source.indexOf("Foo") + 3, 5, 5);
        Node classBody = nodeWithNamedChildren("block", source,
                methodFunction.getStartByte(), methodFunction.getEndByte(), 6, 7, List.of(methodFunction));

        Node classDef = nodeWithFields("class_definition", source,
                source.indexOf("class Foo"), methodFunction.getEndByte(), 5, 7,
                List.of(methodFunction), List.of(className, classBody),
                List.of(field("name", className), field("body", classBody)));
        when(methodFunction.getParent()).thenReturn(Optional.of(classDef));

        Node root = nodeWithNamedChildren("module", source, 0, source.length(), 0, 7, List.of(decoratedDef, classDef));

        TestPythonTreeSitterParser parser = new TestPythonTreeSitterParser(() -> null, () -> {
        });

        List<TextChunk> chunks = parser.extractChunks(root, tree, tsSource);

        assertThat(chunks)
                .hasSize(3)
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("function");
                    assertThat(chunk.entityName()).isEqualTo("top_fn");
                    assertThat(chunk.language()).isEqualTo("python");
                    assertThat(chunk.attributes().get("decorators")).contains("@dec");
                    assertThat(chunk.attributes()).containsEntry("async", "true");
                    assertThat(chunk.attributes().get("parameters")).contains("(x: int)");
                    assertThat(chunk.attributes().get("return_type")).contains("str");
                    assertThat(chunk.attributes().get("docstring")).contains("doc");
                })
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("class");
                    assertThat(chunk.entityName()).isEqualTo("Foo");
                    assertThat(chunk.language()).isEqualTo("python");
                })
                .anySatisfy(chunk -> {
                    assertThat(chunk.entityType()).isEqualTo("method");
                    assertThat(chunk.entityName()).isEqualTo("Foo.method");
                    assertThat(chunk.language()).isEqualTo("python");
                    assertThat(chunk.attributes().get("parameters")).contains("(self, y)");
                });
    }

    @Test
    void parseReturnsEmptyWhenLanguageFailsToLoad(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("sample.py");
        Files.writeString(file, "def foo():\n    return 1\n");

        TestPythonTreeSitterParser parser = new TestPythonTreeSitterParser(() -> null, () -> {
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

    private static final class TestPythonTreeSitterParser extends PythonTreeSitterParser {
        TestPythonTreeSitterParser(Supplier<Language> languageSupplier, Runnable loader) {
            super(languageSupplier, loader);
        }
    }
}

