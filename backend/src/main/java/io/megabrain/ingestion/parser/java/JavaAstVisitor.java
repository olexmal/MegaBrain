/*
 * Copyright (c) 2025 MegaBrain Contributors
 * Licensed under the MIT License - see LICENSE file for details.
 */

package io.megabrain.ingestion.parser.java;

import com.github.javaparser.Range;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import io.megabrain.ingestion.parser.TextChunk;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

final class JavaAstVisitor extends VoidVisitorAdapter<JavaAstVisitor.Context> {

    private static final Logger LOG = Logger.getLogger(JavaAstVisitor.class);
    private static final String ATTR_MODIFIERS = "modifiers";
    private static final String ENTITY_CLASS = "class";

    private final String language;
    private final String packageName;
    private final Path sourcePath;
    private final String source;
    private final SourceCoordinates coordinates;
    private final int sourceByteLength;
    private final List<TextChunk> chunks = new ArrayList<>();

    JavaAstVisitor(String language,
                   String packageName,
                   Path sourcePath,
                   String source,
                   SourceCoordinates coordinates,
                   int sourceByteLength) {
        this.language = language;
        this.packageName = packageName;
        this.sourcePath = sourcePath;
        this.source = source;
        this.coordinates = coordinates;
        this.sourceByteLength = sourceByteLength;
    }

    List<TextChunk> getChunks() {
        return chunks;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Context ctx) {
        ctx.pushType(n.getNameAsString());
        try {
            String kind = n.isInterface() ? "interface" : ENTITY_CLASS;
            addTypeChunk(n.getRange(), ctx, kind, modifiers(n));
            super.visit(n, ctx);
        } finally {
            ctx.popType();
        }
    }

    @Override
    public void visit(EnumDeclaration n, Context ctx) {
        ctx.pushType(n.getNameAsString());
        try {
            addTypeChunk(n.getRange(), ctx, "enum", modifiers(n));
            super.visit(n, ctx);
        } finally {
            ctx.popType();
        }
    }

    @Override
    public void visit(RecordDeclaration n, Context ctx) {
        ctx.pushType(n.getNameAsString());
        try {
            addTypeChunk(n.getRange(), ctx, "record", modifiers(n));
            super.visit(n, ctx);
        } finally {
            ctx.popType();
        }
    }

    @Override
    public void visit(ObjectCreationExpr n, Context ctx) {
        if (n.getAnonymousClassBody().isPresent()) {
            String anonName = "AnonymousClass" + ctx.nextAnonymousIndex();
            ctx.pushType(anonName);
            try {
                addTypeChunk(n.getRange(), ctx, "anonymous_class", "");
                super.visit(n, ctx);
            } finally {
                ctx.popType();
            }
            return;
        }
        super.visit(n, ctx);
    }

    @Override
    public void visit(MethodDeclaration n, Context ctx) {
        if (ctx.typeStack.isEmpty()) {
            super.visit(n, ctx);
            return;
        }
        String signature = n.getSignature().asString();
        String qualifiedParent = ctx.currentTypeFqn(packageName);
        String entityName = qualifiedParent + "#" + signature;

        Map<String, String> attributes = attributesForMember(ctx);
        attributes.put(ATTR_MODIFIERS, modifiers(n));
        attributes.put("returnType", n.getType().asString());
        attributes.put("parameters", n.getParameters().stream()
                .map(p -> p.getType().asString() + " " + p.getNameAsString())
                .collect(Collectors.joining(", ")));
        attributes.put("signature", signature);
        addChunk("method", entityName, n.getRange(), attributes);

        super.visit(n, ctx);
    }

    @Override
    public void visit(ConstructorDeclaration n, Context ctx) {
        if (ctx.typeStack.isEmpty()) {
            super.visit(n, ctx);
            return;
        }
        String signature = n.getSignature().asString();
        String qualifiedParent = ctx.currentTypeFqn(packageName);
        String entityName = qualifiedParent + "#" + signature;

        Map<String, String> attributes = attributesForMember(ctx);
        attributes.put(ATTR_MODIFIERS, modifiers(n));
        attributes.put("returnType", n.getNameAsString());
        attributes.put("parameters", n.getParameters().stream()
                .map(p -> p.getType().asString() + " " + p.getNameAsString())
                .collect(Collectors.joining(", ")));
        attributes.put("signature", signature);
        addChunk("constructor", entityName, n.getRange(), attributes);

        super.visit(n, ctx);
    }

    @Override
    public void visit(FieldDeclaration n, Context ctx) {
        if (ctx.typeStack.isEmpty()) {
            super.visit(n, ctx);
            return;
        }
        for (VariableDeclarator variable : n.getVariables()) {
            String qualifiedParent = ctx.currentTypeFqn(packageName);
            String entityName = qualifiedParent + "#" + variable.getNameAsString();
            Map<String, String> attributes = attributesForMember(ctx);
            attributes.put(ATTR_MODIFIERS, modifiers(n));
            attributes.put("fieldType", variable.getType().asString());
            addChunk("field", entityName, variable.getRange().or(n::getRange), attributes);
        }
        super.visit(n, ctx);
    }

    private void addTypeChunk(Optional<Range> range, Context ctx, String kind, String modifiers) {
        String entityName = ctx.currentTypeFqn(packageName);
        Map<String, String> attributes = attributesForType(ctx);
        attributes.put("kind", kind);
        attributes.put(ATTR_MODIFIERS, modifiers);
        addChunk(kind.equals(ENTITY_CLASS) ? ENTITY_CLASS : kind, entityName, range, attributes);
    }

    private void addChunk(String entityType,
                          String entityName,
                          Optional<Range> range,
                          Map<String, String> attributes) {
        if (range.isEmpty()) {
            LOG.debugf("Skipping entity %s due to missing range information", entityName);
            return;
        }
        Range r = range.get();
        int startByte = coordinates.toByteOffset(r.begin);
        int endByte = Math.min(coordinates.toByteOffset(r.end) + 1, sourceByteLength);
        String snippet = coordinates.slice(r, source);

        chunks.add(new TextChunk(
                snippet,
                language,
                entityType,
                entityName,
                sourcePath.toString(),
                r.begin.line,
                r.end.line,
                startByte,
                endByte,
                attributes
        ));
    }

    private Map<String, String> baseAttributes() {
        Map<String, String> attributes = new HashMap<>();
        if (!packageName.isBlank()) {
            attributes.put("package", packageName);
        }
        return attributes;
    }

    private Map<String, String> attributesForMember(Context ctx) {
        Map<String, String> attributes = baseAttributes();
        attributes.put("parent", ctx.currentTypeFqn(packageName));
        return attributes;
    }

    private Map<String, String> attributesForType(Context ctx) {
        Map<String, String> attributes = baseAttributes();
        ctx.parentFqn(packageName).ifPresent(parent -> attributes.put("parent", parent));
        return attributes;
    }

    private String modifiers(NodeWithModifiers<?> node) {
        String joined = node.getModifiers().stream()
                .map(modifier -> modifier.getKeyword().asString())
                .collect(Collectors.joining(" "));
        return joined.isBlank() ? "" : joined;
    }

    /**
     * Context holds the nesting stack and anonymous counters while traversing.
     */
    static final class Context {
        private final Deque<String> typeStack = new ArrayDeque<>();
        private final AtomicInteger anonymousCounter = new AtomicInteger();

        void pushType(String simpleName) {
            typeStack.push(simpleName);
        }

        void popType() {
            typeStack.pop();
        }

        String currentTypeFqn(String packageName) {
            List<String> types = new ArrayList<>(typeStack);
            List<String> ordered = new ArrayList<>();
            for (String type : types) {
                ordered.addFirst(type);
            }
            String typePath = String.join(".", ordered);
            if (packageName == null || packageName.isBlank()) {
                return typePath;
            }
            return typePath.isEmpty() ? packageName : packageName + "." + typePath;
        }

        Optional<String> parentFqn(String packageName) {
            if (typeStack.size() <= 1) {
                return Optional.empty();
            }
            List<String> types = new ArrayList<>(typeStack);
            types.removeFirst();
            List<String> ordered = new ArrayList<>();
            for (String type : types) {
                ordered.addFirst(type);
            }
            String typePath = String.join(".", ordered);
            if (packageName == null || packageName.isBlank()) {
                return typePath.isBlank() ? Optional.empty() : Optional.of(typePath);
            }
            return Optional.of(packageName + "." + typePath);
        }

        int nextAnonymousIndex() {
            return anonymousCounter.incrementAndGet();
        }
    }
}

