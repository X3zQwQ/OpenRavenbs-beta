package org.spongepowered.asm.util;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import org.spongepowered.asm.lib.signature.SignatureReader;
import org.spongepowered.asm.lib.signature.SignatureVisitor;
import org.spongepowered.asm.lib.signature.SignatureWriter;
import org.spongepowered.asm.lib.tree.ClassNode;

public class ClassSignature {

    protected static final String OBJECT = "java/lang/Object";

    private final Map<ClassSignature.TypeVar, ClassSignature.TokenHandle> types = new LinkedHashMap<>();

    private ClassSignature.Token superClass = new ClassSignature.Token("java/lang/Object");

    private final List<ClassSignature.Token> interfaces = new ArrayList<>();

    private final Deque<String> rawInterfaces = new LinkedList<>();

    ClassSignature() {
    }

    private ClassSignature read(String signature) {
        if (signature != null) {
            try {
                new SignatureReader(signature).accept(new ClassSignature.SignatureParser());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return this;
    }

    protected ClassSignature.TypeVar getTypeVar(String varName) {
        for (ClassSignature.TypeVar typeVar : this.types.keySet()) {
            if (typeVar.matches(varName)) {
                return typeVar;
            }
        }
        return null;
    }

    protected ClassSignature.TokenHandle getType(String varName) {
        for (ClassSignature.TypeVar typeVar : this.types.keySet()) {
            if (typeVar.matches(varName)) {
                return this.types.get(typeVar);
            }
        }
        ClassSignature.TokenHandle handle = new ClassSignature.TokenHandle();
        this.types.put(new ClassSignature.TypeVar(varName), handle);
        return handle;
    }

    protected String getTypeVar(ClassSignature.TokenHandle handle) {
        for (Entry<ClassSignature.TypeVar, ClassSignature.TokenHandle> type : this.types.entrySet()) {
            ClassSignature.TypeVar typeVar = type.getKey();
            ClassSignature.TokenHandle typeHandle = type.getValue();
            if (handle == typeHandle || handle.asToken() == typeHandle.asToken()) {
                return "T" + typeVar + ";";
            }
        }
        return handle.token.asType();
    }

    protected void addTypeVar(ClassSignature.TypeVar typeVar, ClassSignature.TokenHandle handle) throws IllegalArgumentException {
        if (this.types.containsKey(typeVar)) {
            throw new IllegalArgumentException("TypeVar " + typeVar + " is already present on " + this);
        } else {
            this.types.put(typeVar, handle);
        }
    }

    protected void setSuperClass(ClassSignature.Token superClass) {
        this.superClass = superClass;
    }

    public String getSuperClass() {
        return this.superClass.asType(true);
    }

    protected void addInterface(ClassSignature.Token iface) {
        if (!iface.isRaw()) {
            String raw = iface.asType(true);
            ListIterator<ClassSignature.Token> iter = this.interfaces.listIterator();
            while (iter.hasNext()) {
                ClassSignature.Token intrface = iter.next();
                if (intrface.isRaw() && intrface.asType(true).equals(raw)) {
                    iter.set(iface);
                    return;
                }
            }
        }
        this.interfaces.add(iface);
    }

    public void addInterface(String iface) {
        this.rawInterfaces.add(iface);
    }

    protected void addRawInterface(String iface) {
        ClassSignature.Token token = new ClassSignature.Token(iface);
        String raw = token.asType(true);
        for (ClassSignature.Token intrface : this.interfaces) {
            if (intrface.asType(true).equals(raw)) {
                return;
            }
        }
        this.interfaces.add(token);
    }

    public void merge(ClassSignature other) {
        try {
            Set<String> typeVars = new HashSet<>();
            for (ClassSignature.TypeVar typeVar : this.types.keySet()) {
                typeVars.add(typeVar.toString());
            }
            other.conform(typeVars);
        } catch (IllegalStateException exception2) {
            exception2.printStackTrace();
            return;
        }
        for (Entry<ClassSignature.TypeVar, ClassSignature.TokenHandle> type : other.types.entrySet()) {
            this.addTypeVar(type.getKey(), type.getValue());
        }
        for (ClassSignature.Token iface : other.interfaces) {
            this.addInterface(iface);
        }
    }

    private void conform(Set<String> typeVars) {
        for (ClassSignature.TypeVar typeVar : this.types.keySet()) {
            String name = this.findUniqueName(typeVar.getOriginalName(), typeVars);
            typeVar.rename(name);
            typeVars.add(name);
        }
    }

    private String findUniqueName(String typeVar, Set<String> typeVars) {
        if (!typeVars.contains(typeVar)) {
            return typeVar;
        } else {
            if (typeVar.length() == 1) {
                String name = this.findOffsetName(typeVar.charAt(0), typeVars);
                if (name != null) {
                    return name;
                }
            }
            String name = this.findOffsetName('T', typeVars, "", typeVar);
            if (name != null) {
                return name;
            } else {
                name = this.findOffsetName('T', typeVars, typeVar, "");
                if (name != null) {
                    return name;
                } else {
                    name = this.findOffsetName('T', typeVars, "T", typeVar);
                    if (name != null) {
                        return name;
                    } else {
                        name = this.findOffsetName('T', typeVars, "", typeVar + "Type");
                        if (name != null) {
                            return name;
                        } else {
                            throw new IllegalStateException("Failed to conform type var: " + typeVar);
                        }
                    }
                }
            }
        }
    }

    private String findOffsetName(char c, Set<String> typeVars) {
        return this.findOffsetName(c, typeVars, "", "");
    }

    private String findOffsetName(char c, Set<String> typeVars, String prefix, String suffix) {
        String name = String.format("%s%s%s", prefix, c, suffix);
        if (!typeVars.contains(name)) {
            return name;
        } else {
            if (c > '@' && c < '[') {
                for (int s = c - '@'; s + 65 != c; s = ++s % 26) {
                    name = String.format("%s%s%s", prefix, (char) (s + 65), suffix);
                    if (!typeVars.contains(name)) {
                        return name;
                    }
                }
            }
            return null;
        }
    }

    public SignatureVisitor getRemapper() {
        return new ClassSignature.SignatureRemapper();
    }

    @Override
    public String toString() {
        while (this.rawInterfaces.size() > 0) {
            this.addRawInterface(this.rawInterfaces.remove());
        }
        StringBuilder sb = new StringBuilder();
        if (this.types.size() > 0) {
            boolean valid = false;
            StringBuilder types = new StringBuilder();
            for (Entry<ClassSignature.TypeVar, ClassSignature.TokenHandle> type : this.types.entrySet()) {
                String bound = type.getValue().asBound();
                if (!bound.isEmpty()) {
                    types.append(type.getKey()).append(':').append(bound);
                    valid = true;
                }
            }
            if (valid) {
                sb.append('<').append((CharSequence) types).append('>');
            }
        }
        sb.append(this.superClass.asType());
        for (ClassSignature.Token iface : this.interfaces) {
            sb.append(iface.asType());
        }
        return sb.toString();
    }

    public ClassSignature wake() {
        return this;
    }

    public static ClassSignature of(String signature) {
        return new ClassSignature().read(signature);
    }

    public static ClassSignature of(ClassNode classNode) {
        return classNode.signature != null ? of(classNode.signature) : generate(classNode);
    }

    public static ClassSignature ofLazy(ClassNode classNode) {
        return (ClassSignature) (classNode.signature != null ? new ClassSignature.Lazy(classNode.signature) : generate(classNode));
    }

    private static ClassSignature generate(ClassNode classNode) {
        ClassSignature generated = new ClassSignature();
        generated.setSuperClass(new ClassSignature.Token(classNode.superName != null ? classNode.superName : "java/lang/Object"));
        for (String iface : classNode.interfaces) {
            generated.addInterface(new ClassSignature.Token(iface));
        }
        return generated;
    }

    interface IToken {

        String WILDCARDS = "+-";

        String asType();

        String asBound();

        ClassSignature.Token asToken();

        ClassSignature.IToken setArray(boolean array2);

        ClassSignature.IToken setWildcard(char wildcard2);
    }

    static class Lazy extends ClassSignature {

        private final String sig;

        private ClassSignature generated;

        Lazy(String sig) {
            this.sig = sig;
        }

        @Override
        public ClassSignature wake() {
            if (this.generated == null) {
                this.generated = ClassSignature.of(this.sig);
            }
            return this.generated;
        }
    }

    class SignatureParser extends SignatureVisitor {

        private ClassSignature.SignatureParser.FormalParamElement param;

        SignatureParser() {
            super(327680);
        }

        @Override
        public void visitFormalTypeParameter(String name) {
            this.param = new ClassSignature.SignatureParser.FormalParamElement(name);
        }

        @Override
        public SignatureVisitor visitClassBound() {
            return this.param.visitClassBound();
        }

        @Override
        public SignatureVisitor visitInterfaceBound() {
            return this.param.visitInterfaceBound();
        }

        @Override
        public SignatureVisitor visitSuperclass() {
            return new ClassSignature.SignatureParser.SuperClassElement();
        }

        @Override
        public SignatureVisitor visitInterface() {
            return new ClassSignature.SignatureParser.InterfaceElement();
        }

        class BoundElement extends ClassSignature.SignatureParser.TokenElement {

            private final ClassSignature.SignatureParser.TokenElement type;

            private final boolean classBound;

            BoundElement(ClassSignature.SignatureParser.TokenElement type, boolean classBound) {
                this.type = type;
                this.classBound = classBound;
            }

            @Override
            public void visitClassType(String name) {
                this.token = this.type.token.addBound(name, this.classBound);
            }

            @Override
            public void visitTypeArgument() {
                this.token.addTypeArgument('*');
            }

            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
                return SignatureParser.this.new TypeArgElement(this, wildcard);
            }
        }

        class FormalParamElement extends ClassSignature.SignatureParser.TokenElement {

            private final ClassSignature.TokenHandle handle;

            FormalParamElement(String param) {
                this.handle = ClassSignature.this.getType(param);
                this.token = this.handle.asToken();
            }
        }

        class InterfaceElement extends ClassSignature.SignatureParser.TokenElement {

            @Override
            public void visitEnd() {
                ClassSignature.this.addInterface(this.token);
            }
        }

        abstract class SignatureElement extends SignatureVisitor {

            public SignatureElement() {
                super(327680);
            }
        }

        class SuperClassElement extends ClassSignature.SignatureParser.TokenElement {

            @Override
            public void visitEnd() {
                ClassSignature.this.setSuperClass(this.token);
            }
        }

        abstract class TokenElement extends ClassSignature.SignatureParser.SignatureElement {

            protected ClassSignature.Token token;

            private boolean array;

            public ClassSignature.Token getToken() {
                if (this.token == null) {
                    this.token = new ClassSignature.Token();
                }
                return this.token;
            }

            protected void setArray() {
                this.array = true;
            }

            private boolean getArray() {
                boolean array = this.array;
                this.array = false;
                return array;
            }

            @Override
            public void visitClassType(String name) {
                this.getToken().setType(name);
            }

            @Override
            public SignatureVisitor visitClassBound() {
                this.getToken();
                return SignatureParser.this.new BoundElement(this, true);
            }

            @Override
            public SignatureVisitor visitInterfaceBound() {
                this.getToken();
                return SignatureParser.this.new BoundElement(this, false);
            }

            @Override
            public void visitInnerClassType(String name) {
                this.token.addInnerClass(name);
            }

            @Override
            public SignatureVisitor visitArrayType() {
                this.setArray();
                return this;
            }

            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
                return SignatureParser.this.new TypeArgElement(this, wildcard);
            }

            ClassSignature.Token addTypeArgument() {
                return this.token.addTypeArgument('*').asToken();
            }

            ClassSignature.IToken addTypeArgument(char symbol) {
                return this.token.addTypeArgument(symbol).setArray(this.getArray());
            }

            ClassSignature.IToken addTypeArgument(String name) {
                return this.token.addTypeArgument(name).setArray(this.getArray());
            }

            ClassSignature.IToken addTypeArgument(ClassSignature.Token token) {
                return this.token.addTypeArgument(token).setArray(this.getArray());
            }

            ClassSignature.IToken addTypeArgument(ClassSignature.TokenHandle token) {
                return this.token.addTypeArgument(token).setArray(this.getArray());
            }
        }

        class TypeArgElement extends ClassSignature.SignatureParser.TokenElement {

            private final ClassSignature.SignatureParser.TokenElement type;

            private final char wildcard;

            TypeArgElement(ClassSignature.SignatureParser.TokenElement type, char wildcard) {
                this.type = type;
                this.wildcard = wildcard;
            }

            @Override
            public SignatureVisitor visitArrayType() {
                this.type.setArray();
                return this;
            }

            @Override
            public void visitBaseType(char descriptor) {
                this.token = this.type.addTypeArgument(descriptor).asToken();
            }

            @Override
            public void visitTypeVariable(String name) {
                ClassSignature.TokenHandle token = ClassSignature.this.getType(name);
                this.token = this.type.addTypeArgument(token).setWildcard(this.wildcard).asToken();
            }

            @Override
            public void visitClassType(String name) {
                this.token = this.type.addTypeArgument(name).setWildcard(this.wildcard).asToken();
            }

            @Override
            public void visitTypeArgument() {
                this.token.addTypeArgument('*');
            }

            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
                return SignatureParser.this.new TypeArgElement(this, wildcard);
            }

            @Override
            public void visitEnd() {
            }
        }
    }

    class SignatureRemapper extends SignatureWriter {

        private final Set<String> localTypeVars = new HashSet<>();

        @Override
        public void visitFormalTypeParameter(String name) {
            this.localTypeVars.add(name);
            super.visitFormalTypeParameter(name);
        }

        @Override
        public void visitTypeVariable(String name) {
            if (!this.localTypeVars.contains(name)) {
                ClassSignature.TypeVar typeVar = ClassSignature.this.getTypeVar(name);
                if (typeVar != null) {
                    super.visitTypeVariable(typeVar.toString());
                    return;
                }
            }
            super.visitTypeVariable(name);
        }
    }

    static class Token implements ClassSignature.IToken {

        static final String SYMBOLS = "+-*";

        private final boolean inner;

        private boolean array;

        private char symbol = 0;

        private String type;

        private List<ClassSignature.Token> classBound;

        private List<ClassSignature.Token> ifaceBound;

        private List<ClassSignature.IToken> signature;

        private List<ClassSignature.IToken> suffix;

        private ClassSignature.Token tail;

        Token() {
            this(false);
        }

        Token(String type) {
            this(type, false);
        }

        Token(char symbol) {
            this();
            this.symbol = symbol;
        }

        Token(boolean inner) {
            this(null, inner);
        }

        Token(String type, boolean inner) {
            this.inner = inner;
            this.type = type;
        }

        ClassSignature.Token setSymbol(char symbol) {
            if (this.symbol == 0 && "+-*".indexOf(symbol) > -1) {
                this.symbol = symbol;
            }
            return this;
        }

        ClassSignature.Token setType(String type) {
            if (this.type == null) {
                this.type = type;
            }
            return this;
        }

        boolean hasClassBound() {
            return this.classBound != null;
        }

        boolean hasInterfaceBound() {
            return this.ifaceBound != null;
        }

        @Override
        public ClassSignature.IToken setArray(boolean array) {
            this.array |= array;
            return this;
        }

        @Override
        public ClassSignature.IToken setWildcard(char wildcard) {
            return "+-".indexOf(wildcard) == -1 ? this : this.setSymbol(wildcard);
        }

        private List<ClassSignature.Token> getClassBound() {
            if (this.classBound == null) {
                this.classBound = new ArrayList<>();
            }
            return this.classBound;
        }

        private List<ClassSignature.Token> getIfaceBound() {
            if (this.ifaceBound == null) {
                this.ifaceBound = new ArrayList<>();
            }
            return this.ifaceBound;
        }

        private List<ClassSignature.IToken> getSignature() {
            if (this.signature == null) {
                this.signature = new ArrayList<>();
            }
            return this.signature;
        }

        private List<ClassSignature.IToken> getSuffix() {
            if (this.suffix == null) {
                this.suffix = new ArrayList<>();
            }
            return this.suffix;
        }

        ClassSignature.IToken addTypeArgument(char symbol) {
            if (this.tail != null) {
                return this.tail.addTypeArgument(symbol);
            } else {
                ClassSignature.Token token = new ClassSignature.Token(symbol);
                this.getSignature().add(token);
                return token;
            }
        }

        ClassSignature.IToken addTypeArgument(String name) {
            if (this.tail != null) {
                return this.tail.addTypeArgument(name);
            } else {
                ClassSignature.Token token = new ClassSignature.Token(name);
                this.getSignature().add(token);
                return token;
            }
        }

        ClassSignature.IToken addTypeArgument(ClassSignature.Token token) {
            if (this.tail != null) {
                return this.tail.addTypeArgument(token);
            } else {
                this.getSignature().add(token);
                return token;
            }
        }

        ClassSignature.IToken addTypeArgument(ClassSignature.TokenHandle token) {
            if (this.tail != null) {
                return this.tail.addTypeArgument(token);
            } else {
                ClassSignature.TokenHandle handle = token.clone();
                this.getSignature().add(handle);
                return handle;
            }
        }

        ClassSignature.Token addBound(String bound, boolean classBound) {
            return classBound ? this.addClassBound(bound) : this.addInterfaceBound(bound);
        }

        ClassSignature.Token addClassBound(String bound) {
            ClassSignature.Token token = new ClassSignature.Token(bound);
            this.getClassBound().add(token);
            return token;
        }

        ClassSignature.Token addInterfaceBound(String bound) {
            ClassSignature.Token token = new ClassSignature.Token(bound);
            this.getIfaceBound().add(token);
            return token;
        }

        ClassSignature.Token addInnerClass(String name) {
            this.tail = new ClassSignature.Token(name, true);
            this.getSuffix().add(this.tail);
            return this.tail;
        }

        @Override
        public String toString() {
            return this.asType();
        }

        @Override
        public String asBound() {
            StringBuilder sb = new StringBuilder();
            if (this.type != null) {
                sb.append(this.type);
            }
            if (this.classBound != null) {
                for (ClassSignature.Token token : this.classBound) {
                    sb.append(token.asType());
                }
            }
            if (this.ifaceBound != null) {
                for (ClassSignature.Token token : this.ifaceBound) {
                    sb.append(':').append(token.asType());
                }
            }
            return sb.toString();
        }

        @Override
        public String asType() {
            return this.asType(false);
        }

        public String asType(boolean raw) {
            StringBuilder sb = new StringBuilder();
            if (this.array) {
                sb.append('[');
            }
            if (this.symbol != 0) {
                sb.append(this.symbol);
            }
            if (this.type == null) {
                return sb.toString();
            } else {
                if (!this.inner) {
                    sb.append('L');
                }
                sb.append(this.type);
                if (!raw) {
                    if (this.signature != null) {
                        sb.append('<');
                        for (ClassSignature.IToken token : this.signature) {
                            sb.append(token.asType());
                        }
                        sb.append('>');
                    }
                    if (this.suffix != null) {
                        for (ClassSignature.IToken token : this.suffix) {
                            sb.append('.').append(token.asType());
                        }
                    }
                }
                if (!this.inner) {
                    sb.append(';');
                }
                return sb.toString();
            }
        }

        boolean isRaw() {
            return this.signature == null;
        }

        String getClassType() {
            return this.type != null ? this.type : "java/lang/Object";
        }

        @Override
        public ClassSignature.Token asToken() {
            return this;
        }
    }

    class TokenHandle implements ClassSignature.IToken {

        final ClassSignature.Token token;

        boolean array;

        char wildcard;

        TokenHandle() {
            this(new ClassSignature.Token());
        }

        TokenHandle(ClassSignature.Token token) {
            this.token = token;
        }

        @Override
        public ClassSignature.IToken setArray(boolean array) {
            this.array |= array;
            return this;
        }

        @Override
        public ClassSignature.IToken setWildcard(char wildcard) {
            if ("+-".indexOf(wildcard) > -1) {
                this.wildcard = wildcard;
            }
            return this;
        }

        @Override
        public String asBound() {
            return this.token.asBound();
        }

        @Override
        public String asType() {
            StringBuilder sb = new StringBuilder();
            if (this.wildcard > 0) {
                sb.append(this.wildcard);
            }
            if (this.array) {
                sb.append('[');
            }
            return sb.append(ClassSignature.this.getTypeVar(this)).toString();
        }

        @Override
        public ClassSignature.Token asToken() {
            return this.token;
        }

        @Override
        public String toString() {
            return this.token.toString();
        }

        public ClassSignature.TokenHandle clone() {
            return ClassSignature.this.new TokenHandle(this.token);
        }
    }

    static class TypeVar implements Comparable<ClassSignature.TypeVar> {

        private final String originalName;

        private String currentName;

        TypeVar(String name) {
            this.currentName = this.originalName = name;
        }

        public int compareTo(ClassSignature.TypeVar other) {
            return this.currentName.compareTo(other.currentName);
        }

        @Override
        public String toString() {
            return this.currentName;
        }

        String getOriginalName() {
            return this.originalName;
        }

        void rename(String name) {
            this.currentName = name;
        }

        public boolean matches(String originalName) {
            return this.originalName.equals(originalName);
        }

        @Override
        public boolean equals(Object obj) {
            return this.currentName.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.currentName.hashCode();
        }
    }
}
