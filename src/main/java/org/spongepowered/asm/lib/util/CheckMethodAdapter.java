package org.spongepowered.asm.lib.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.spongepowered.asm.lib.AnnotationVisitor;
import org.spongepowered.asm.lib.Attribute;
import org.spongepowered.asm.lib.Handle;
import org.spongepowered.asm.lib.Label;
import org.spongepowered.asm.lib.MethodVisitor;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.TypePath;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.analysis.Analyzer;
import org.spongepowered.asm.lib.tree.analysis.BasicValue;
import org.spongepowered.asm.lib.tree.analysis.BasicVerifier;

public class CheckMethodAdapter extends MethodVisitor {

    public int version;

    private int access;

    private boolean startCode;

    private boolean endCode;

    private boolean endMethod;

    private int insnCount;

    private final Map<Label, Integer> labels;

    private Set<Label> usedLabels;

    private int expandedFrames;

    private int compressedFrames;

    private int lastFrame = -1;

    private List<Label> handlers;

    private static final int[] TYPE;

    private static Field labelStatusField;

    public CheckMethodAdapter(MethodVisitor mv) {
        this(mv, new HashMap<>());
    }

    public CheckMethodAdapter(MethodVisitor mv, Map<Label, Integer> labels) {
        this(327680, mv, labels);
        if (this.getClass() != CheckMethodAdapter.class) {
            throw new IllegalStateException();
        }
    }

    protected CheckMethodAdapter(int api, MethodVisitor mv, Map<Label, Integer> labels) {
        super(api, mv);
        this.labels = labels;
        this.usedLabels = new HashSet<>();
        this.handlers = new ArrayList<>();
    }

    public CheckMethodAdapter(int access, String name, String desc, MethodVisitor cmv, Map<Label, Integer> labels) {
        this(new MethodNode(327680, access, name, desc, null, null) {

            @Override
            public void visitEnd() {
                Analyzer<BasicValue> a = new Analyzer<>(new BasicVerifier());
                try {
                    a.analyze("dummy", this);
                } catch (Exception exception) {
                    if (exception instanceof IndexOutOfBoundsException && this.maxLocals == 0 && this.maxStack == 0) {
                        throw new RuntimeException("Data flow checking option requires valid, non zero maxLocals and maxStack values.");
                    }
                    exception.printStackTrace();
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw, true);
                    CheckClassAdapter.printAnalyzerResult(this, a, pw);
                    pw.close();
                    throw new RuntimeException(exception.getMessage() + ' ' + sw.toString());
                }
                this.accept(cmv);
            }
        }, labels);
        this.access = access;
    }

    @Override
    public void visitParameter(String name, int access) {
        if (name != null) {
            checkUnqualifiedName(this.version, name, "name");
        }
        CheckClassAdapter.checkAccess(access, 36880);
        super.visitParameter(name, access);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        this.checkEndMethod();
        checkDesc(desc, false);
        return new CheckAnnotationAdapter(super.visitAnnotation(desc, visible));
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        this.checkEndMethod();
        int sort = typeRef >>> 24;
        if (sort != 1 && sort != 18 && sort != 20 && sort != 21 && sort != 22 && sort != 23) {
            throw new IllegalArgumentException("Invalid type reference sort 0x" + Integer.toHexString(sort));
        } else {
            CheckClassAdapter.checkTypeRefAndPath(typeRef, typePath);
            checkDesc(desc, false);
            return new CheckAnnotationAdapter(super.visitTypeAnnotation(typeRef, typePath, desc, visible));
        }
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        this.checkEndMethod();
        return new CheckAnnotationAdapter(super.visitAnnotationDefault(), false);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        this.checkEndMethod();
        checkDesc(desc, false);
        return new CheckAnnotationAdapter(super.visitParameterAnnotation(parameter, desc, visible));
    }

    @Override
    public void visitAttribute(Attribute attr) {
        this.checkEndMethod();
        if (attr == null) {
            throw new IllegalArgumentException("Invalid attribute (must not be null)");
        } else {
            super.visitAttribute(attr);
        }
    }

    @Override
    public void visitCode() {
        if ((this.access & 1024) != 0) {
            throw new RuntimeException("Abstract methods cannot have code");
        } else {
            this.startCode = true;
            super.visitCode();
        }
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        if (this.insnCount == this.lastFrame) {
            throw new IllegalStateException("At most one frame can be visited at a given code location.");
        } else {
            this.lastFrame = this.insnCount;
            int mLocal;
            int mStack;
            switch(type) {
                case -1:
                case 0:
                    mLocal = Integer.MAX_VALUE;
                    mStack = Integer.MAX_VALUE;
                    break;
                case 1:
                case 2:
                    mLocal = 3;
                    mStack = 0;
                    break;
                case 3:
                    mLocal = 0;
                    mStack = 0;
                    break;
                case 4:
                    mLocal = 0;
                    mStack = 1;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid frame type " + type);
            }
            if (nLocal > mLocal) {
                throw new IllegalArgumentException("Invalid nLocal=" + nLocal + " for frame type " + type);
            } else if (nStack > mStack) {
                throw new IllegalArgumentException("Invalid nStack=" + nStack + " for frame type " + type);
            } else {
                if (type != 2) {
                    if (nLocal > 0 && (local == null || local.length < nLocal)) {
                        throw new IllegalArgumentException("Array local[] is shorter than nLocal");
                    }
                    for (int i = 0; i < nLocal; i++) {
                        this.checkFrameValue(local[i]);
                    }
                }
                if (nStack <= 0 || stack != null && stack.length >= nStack) {
                    for (int i = 0; i < nStack; i++) {
                        this.checkFrameValue(stack[i]);
                    }
                    if (type == -1) {
                        this.expandedFrames++;
                    } else {
                        this.compressedFrames++;
                    }
                    if (this.expandedFrames > 0 && this.compressedFrames > 0) {
                        throw new RuntimeException("Expanded and compressed frames must not be mixed.");
                    } else {
                        super.visitFrame(type, nLocal, local, nStack, stack);
                    }
                } else {
                    throw new IllegalArgumentException("Array stack[] is shorter than nStack");
                }
            }
        }
    }

    @Override
    public void visitInsn(int opcode) {
        this.checkStartCode();
        this.checkEndCode();
        checkOpcode(opcode, 0);
        super.visitInsn(opcode);
        this.insnCount++;
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        this.checkStartCode();
        this.checkEndCode();
        checkOpcode(opcode, 1);
        switch(opcode) {
            case 16:
                checkSignedByte(operand, "Invalid operand");
                break;
            case 17:
                checkSignedShort(operand, "Invalid operand");
                break;
            default:
                if (operand < 4 || operand > 11) {
                    throw new IllegalArgumentException("Invalid operand (must be an array type code T_...): " + operand);
                }
        }
        super.visitIntInsn(opcode, operand);
        this.insnCount++;
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        this.checkStartCode();
        this.checkEndCode();
        checkOpcode(opcode, 2);
        checkUnsignedShort(var, "Invalid variable index");
        super.visitVarInsn(opcode, var);
        this.insnCount++;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        this.checkStartCode();
        this.checkEndCode();
        checkOpcode(opcode, 3);
        checkInternalName(type, "type");
        if (opcode == 187 && type.charAt(0) == '[') {
            throw new IllegalArgumentException("NEW cannot be used to create arrays: " + type);
        } else {
            super.visitTypeInsn(opcode, type);
            this.insnCount++;
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        this.checkStartCode();
        this.checkEndCode();
        checkOpcode(opcode, 4);
        checkInternalName(owner, "owner");
        checkUnqualifiedName(this.version, name, "name");
        checkDesc(desc, false);
        super.visitFieldInsn(opcode, owner, name, desc);
        this.insnCount++;
    }

    @Deprecated
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if (this.api >= 327680) {
            super.visitMethodInsn(opcode, owner, name, desc);
        } else {
            this.doVisitMethodInsn(opcode, owner, name, desc, opcode == 185);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (this.api < 327680) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        } else {
            this.doVisitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    private void doVisitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        this.checkStartCode();
        this.checkEndCode();
        checkOpcode(opcode, 5);
        if (opcode != 183 || !"<init>".equals(name)) {
            checkMethodIdentifier(this.version, name, "name");
        }
        checkInternalName(owner, "owner");
        checkMethodDesc(desc);
        if (opcode == 182 && itf) {
            throw new IllegalArgumentException("INVOKEVIRTUAL can't be used with interfaces");
        } else if (opcode == 185 && !itf) {
            throw new IllegalArgumentException("INVOKEINTERFACE can't be used with classes");
        } else if (opcode == 183 && itf && (this.version & 65535) < 52) {
            throw new IllegalArgumentException("INVOKESPECIAL can't be used with interfaces prior to Java 8");
        } else {
            if (this.mv != null) {
                this.mv.visitMethodInsn(opcode, owner, name, desc, itf);
            }
            this.insnCount++;
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        this.checkStartCode();
        this.checkEndCode();
        checkMethodIdentifier(this.version, name, "name");
        checkMethodDesc(desc);
        if (bsm.getTag() != 6 && bsm.getTag() != 8) {
            throw new IllegalArgumentException("invalid handle tag " + bsm.getTag());
        } else {
            for (int i = 0; i < bsmArgs.length; i++) {
                this.checkLDCConstant(bsmArgs[i]);
            }
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
            this.insnCount++;
        }
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        this.checkStartCode();
        this.checkEndCode();
        checkOpcode(opcode, 6);
        this.checkLabel(label, false, "label");
        checkNonDebugLabel(label);
        super.visitJumpInsn(opcode, label);
        this.usedLabels.add(label);
        this.insnCount++;
    }

    @Override
    public void visitLabel(Label label) {
        this.checkStartCode();
        this.checkEndCode();
        this.checkLabel(label, false, "label");
        if (this.labels.get(label) != null) {
            throw new IllegalArgumentException("Already visited label");
        } else {
            this.labels.put(label, this.insnCount);
            super.visitLabel(label);
        }
    }

    @Override
    public void visitLdcInsn(Object cst) {
        this.checkStartCode();
        this.checkEndCode();
        this.checkLDCConstant(cst);
        super.visitLdcInsn(cst);
        this.insnCount++;
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        this.checkStartCode();
        this.checkEndCode();
        checkUnsignedShort(var, "Invalid variable index");
        checkSignedShort(increment, "Invalid increment");
        super.visitIincInsn(var, increment);
        this.insnCount++;
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        this.checkStartCode();
        this.checkEndCode();
        if (max < min) {
            throw new IllegalArgumentException("Max = " + max + " must be greater than or equal to min = " + min);
        } else {
            this.checkLabel(dflt, false, "default label");
            checkNonDebugLabel(dflt);
            if (labels != null && labels.length == max - min + 1) {
                for (int i = 0; i < labels.length; i++) {
                    this.checkLabel(labels[i], false, "label at index " + i);
                    checkNonDebugLabel(labels[i]);
                }
                super.visitTableSwitchInsn(min, max, dflt, labels);
                for (int i = 0; i < labels.length; i++) {
                    this.usedLabels.add(labels[i]);
                }
                this.insnCount++;
            } else {
                throw new IllegalArgumentException("There must be max - min + 1 labels");
            }
        }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        this.checkEndCode();
        this.checkStartCode();
        this.checkLabel(dflt, false, "default label");
        checkNonDebugLabel(dflt);
        if (keys != null && labels != null && keys.length == labels.length) {
            for (int i = 0; i < labels.length; i++) {
                this.checkLabel(labels[i], false, "label at index " + i);
                checkNonDebugLabel(labels[i]);
            }
            super.visitLookupSwitchInsn(dflt, keys, labels);
            this.usedLabels.add(dflt);
            for (int i = 0; i < labels.length; i++) {
                this.usedLabels.add(labels[i]);
            }
            this.insnCount++;
        } else {
            throw new IllegalArgumentException("There must be the same number of keys and labels");
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        this.checkStartCode();
        this.checkEndCode();
        checkDesc(desc, false);
        if (desc.charAt(0) != '[') {
            throw new IllegalArgumentException("Invalid descriptor (must be an array type descriptor): " + desc);
        } else if (dims < 1) {
            throw new IllegalArgumentException("Invalid dimensions (must be greater than 0): " + dims);
        } else if (dims > desc.lastIndexOf(91) + 1) {
            throw new IllegalArgumentException("Invalid dimensions (must not be greater than dims(desc)): " + dims);
        } else {
            super.visitMultiANewArrayInsn(desc, dims);
            this.insnCount++;
        }
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        this.checkStartCode();
        this.checkEndCode();
        int sort = typeRef >>> 24;
        if (sort != 67 && sort != 68 && sort != 69 && sort != 70 && sort != 71 && sort != 72 && sort != 73 && sort != 74 && sort != 75) {
            throw new IllegalArgumentException("Invalid type reference sort 0x" + Integer.toHexString(sort));
        } else {
            CheckClassAdapter.checkTypeRefAndPath(typeRef, typePath);
            checkDesc(desc, false);
            return new CheckAnnotationAdapter(super.visitInsnAnnotation(typeRef, typePath, desc, visible));
        }
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        this.checkStartCode();
        this.checkEndCode();
        this.checkLabel(start, false, "start label");
        this.checkLabel(end, false, "end label");
        this.checkLabel(handler, false, "handler label");
        checkNonDebugLabel(start);
        checkNonDebugLabel(end);
        checkNonDebugLabel(handler);
        if (this.labels.get(start) == null && this.labels.get(end) == null && this.labels.get(handler) == null) {
            if (type != null) {
                checkInternalName(type, "type");
            }
            super.visitTryCatchBlock(start, end, handler, type);
            this.handlers.add(start);
            this.handlers.add(end);
        } else {
            throw new IllegalStateException("Try catch blocks must be visited before their labels");
        }
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        this.checkStartCode();
        this.checkEndCode();
        int sort = typeRef >>> 24;
        if (sort != 66) {
            throw new IllegalArgumentException("Invalid type reference sort 0x" + Integer.toHexString(sort));
        } else {
            CheckClassAdapter.checkTypeRefAndPath(typeRef, typePath);
            checkDesc(desc, false);
            return new CheckAnnotationAdapter(super.visitTryCatchAnnotation(typeRef, typePath, desc, visible));
        }
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        this.checkStartCode();
        this.checkEndCode();
        checkUnqualifiedName(this.version, name, "name");
        checkDesc(desc, false);
        this.checkLabel(start, true, "start label");
        this.checkLabel(end, true, "end label");
        checkUnsignedShort(index, "Invalid variable index");
        int s = this.labels.get(start);
        int e = this.labels.get(end);
        if (e < s) {
            throw new IllegalArgumentException("Invalid start and end labels (end must be greater than start)");
        } else {
            super.visitLocalVariable(name, desc, signature, start, end, index);
        }
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
        this.checkStartCode();
        this.checkEndCode();
        int sort = typeRef >>> 24;
        if (sort != 64 && sort != 65) {
            throw new IllegalArgumentException("Invalid type reference sort 0x" + Integer.toHexString(sort));
        } else {
            CheckClassAdapter.checkTypeRefAndPath(typeRef, typePath);
            checkDesc(desc, false);
            if (start != null && end != null && index != null && end.length == start.length && index.length == start.length) {
                for (int i = 0; i < start.length; i++) {
                    this.checkLabel(start[i], true, "start label");
                    this.checkLabel(end[i], true, "end label");
                    checkUnsignedShort(index[i], "Invalid variable index");
                    int s = this.labels.get(start[i]);
                    int e = this.labels.get(end[i]);
                    if (e < s) {
                        throw new IllegalArgumentException("Invalid start and end labels (end must be greater than start)");
                    }
                }
                return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);
            } else {
                throw new IllegalArgumentException("Invalid start, end and index arrays (must be non null and of identical length");
            }
        }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.checkStartCode();
        this.checkEndCode();
        checkUnsignedShort(line, "Invalid line number");
        this.checkLabel(start, true, "start label");
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        this.checkStartCode();
        this.checkEndCode();
        this.endCode = true;
        for (Label l : this.usedLabels) {
            if (this.labels.get(l) == null) {
                throw new IllegalStateException("Undefined label used");
            }
        }
        int i = 0;
        while (i < this.handlers.size()) {
            Integer start = this.labels.get(this.handlers.get(i++));
            Integer end = this.labels.get(this.handlers.get(i++));
            if (start == null || end == null) {
                throw new IllegalStateException("Undefined try catch block labels");
            }
            if (end <= start) {
                throw new IllegalStateException("Emty try catch block handler range");
            }
        }
        checkUnsignedShort(maxStack, "Invalid max stack");
        checkUnsignedShort(maxLocals, "Invalid max locals");
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitEnd() {
        this.checkEndMethod();
        this.endMethod = true;
        super.visitEnd();
    }

    void checkStartCode() {
        if (!this.startCode) {
            throw new IllegalStateException("Cannot visit instructions before visitCode has been called.");
        }
    }

    void checkEndCode() {
        if (this.endCode) {
            throw new IllegalStateException("Cannot visit instructions after visitMaxs has been called.");
        }
    }

    void checkEndMethod() {
        if (this.endMethod) {
            throw new IllegalStateException("Cannot visit elements after visitEnd has been called.");
        }
    }

    void checkFrameValue(Object value) {
        if (value != Opcodes.TOP && value != Opcodes.INTEGER && value != Opcodes.FLOAT && value != Opcodes.LONG && value != Opcodes.DOUBLE && value != Opcodes.NULL && value != Opcodes.UNINITIALIZED_THIS) {
            if (value instanceof String) {
                checkInternalName((String) value, "Invalid stack frame value");
            } else if (!(value instanceof Label)) {
                throw new IllegalArgumentException("Invalid stack frame value: " + value);
            } else {
                this.usedLabels.add((Label) value);
            }
        }
    }

    static void checkOpcode(int opcode, int type) {
        if (opcode < 0 || opcode > 199 || TYPE[opcode] != type) {
            throw new IllegalArgumentException("Invalid opcode: " + opcode);
        }
    }

    static void checkSignedByte(int value, String msg) {
        if (value < -128 || value > 127) {
            throw new IllegalArgumentException(msg + " (must be a signed byte): " + value);
        }
    }

    static void checkSignedShort(int value, String msg) {
        if (value < -32768 || value > 32767) {
            throw new IllegalArgumentException(msg + " (must be a signed short): " + value);
        }
    }

    static void checkUnsignedShort(int value, String msg) {
        if (value < 0 || value > 65535) {
            throw new IllegalArgumentException(msg + " (must be an unsigned short): " + value);
        }
    }

    static void checkConstant(Object cst) {
        if (!(cst instanceof Integer) && !(cst instanceof Float) && !(cst instanceof Long) && !(cst instanceof Double) && !(cst instanceof String)) {
            throw new IllegalArgumentException("Invalid constant: " + cst);
        }
    }

    void checkLDCConstant(Object cst) {
        if (cst instanceof Type) {
            int s = ((Type) cst).getSort();
            if (s != 10 && s != 9 && s != 11) {
                throw new IllegalArgumentException("Illegal LDC constant value");
            }
            if (s != 11 && (this.version & 65535) < 49) {
                throw new IllegalArgumentException("ldc of a constant class requires at least version 1.5");
            }
            if (s == 11 && (this.version & 65535) < 51) {
                throw new IllegalArgumentException("ldc of a method type requires at least version 1.7");
            }
        } else if (cst instanceof Handle) {
            if ((this.version & 65535) < 51) {
                throw new IllegalArgumentException("ldc of a handle requires at least version 1.7");
            }
            int tag = ((Handle) cst).getTag();
            if (tag < 1 || tag > 9) {
                throw new IllegalArgumentException("invalid handle tag " + tag);
            }
        } else {
            checkConstant(cst);
        }
    }

    static void checkUnqualifiedName(int version, String name, String msg) {
        if ((version & 65535) < 49) {
            checkIdentifier(name, msg);
        } else {
            for (int i = 0; i < name.length(); i++) {
                if (".;[/".indexOf(name.charAt(i)) != -1) {
                    throw new IllegalArgumentException("Invalid " + msg + " (must be a valid unqualified name): " + name);
                }
            }
        }
    }

    static void checkIdentifier(String name, String msg) {
        checkIdentifier(name, 0, -1, msg);
    }

    static void checkIdentifier(String name, int start, int end, String msg) {
        if (name != null && (end == -1 ? name.length() > start : end > start)) {
            if (!Character.isJavaIdentifierStart(name.charAt(start))) {
                throw new IllegalArgumentException("Invalid " + msg + " (must be a valid Java identifier): " + name);
            } else {
                int max = end == -1 ? name.length() : end;
                for (int i = start + 1; i < max; i++) {
                    if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                        throw new IllegalArgumentException("Invalid " + msg + " (must be a valid Java identifier): " + name);
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid " + msg + " (must not be null or empty)");
        }
    }

    static void checkMethodIdentifier(int version, String name, String msg) {
        if (name != null && name.length() != 0) {
            if ((version & 65535) >= 49) {
                for (int i = 0; i < name.length(); i++) {
                    if (".;[/<>".indexOf(name.charAt(i)) != -1) {
                        throw new IllegalArgumentException("Invalid " + msg + " (must be a valid unqualified name): " + name);
                    }
                }
            } else if (!Character.isJavaIdentifierStart(name.charAt(0))) {
                throw new IllegalArgumentException("Invalid " + msg + " (must be a '<init>', '<clinit>' or a valid Java identifier): " + name);
            } else {
                for (int ix = 1; ix < name.length(); ix++) {
                    if (!Character.isJavaIdentifierPart(name.charAt(ix))) {
                        throw new IllegalArgumentException("Invalid " + msg + " (must be '<init>' or '<clinit>' or a valid Java identifier): " + name);
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid " + msg + " (must not be null or empty)");
        }
    }

    static void checkInternalName(String name, String msg) {
        if (name != null && name.length() != 0) {
            if (name.charAt(0) == '[') {
                checkDesc(name, false);
            } else {
                checkInternalName(name, 0, -1, msg);
            }
        } else {
            throw new IllegalArgumentException("Invalid " + msg + " (must not be null or empty)");
        }
    }

    static void checkInternalName(String name, int start, int end, String msg) {
        int max = end == -1 ? name.length() : end;
        try {
            int begin = start;
            int slash;
            do {
                slash = name.indexOf(47, begin + 1);
                if (slash == -1 || slash > max) {
                    slash = max;
                }
                checkIdentifier(name, begin, slash, null);
                begin = slash + 1;
            } while (slash != max);
        } catch (IllegalArgumentException exception2) {
            throw new IllegalArgumentException("Invalid " + msg + " (must be a fully qualified class name in internal form): " + name);
        }
    }

    static void checkDesc(String desc, boolean canBeVoid) {
        int end = checkDesc(desc, 0, canBeVoid);
        if (end != desc.length()) {
            throw new IllegalArgumentException("Invalid descriptor: " + desc);
        }
    }

    static int checkDesc(String desc, int start, boolean canBeVoid) {
        if (desc != null && start < desc.length()) {
            switch(desc.charAt(start)) {
                case 'B':
                case 'C':
                case 'D':
                case 'F':
                case 'I':
                case 'J':
                case 'S':
                case 'Z':
                    return start + 1;
                case 'E':
                case 'G':
                case 'H':
                case 'K':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'T':
                case 'U':
                case 'W':
                case 'X':
                case 'Y':
                default:
                    throw new IllegalArgumentException("Invalid descriptor: " + desc);
                case 'L':
                    int index = desc.indexOf(59, start);
                    if (index != -1 && index - start >= 2) {
                        try {
                            checkInternalName(desc, start + 1, index, null);
                        } catch (IllegalArgumentException exception3) {
                            throw new IllegalArgumentException("Invalid descriptor: " + desc);
                        }
                        return index + 1;
                    }
                    throw new IllegalArgumentException("Invalid descriptor: " + desc);
                case 'V':
                    if (canBeVoid) {
                        return start + 1;
                    }
                    throw new IllegalArgumentException("Invalid descriptor: " + desc);
                case '[':
                    int indexValue = start + 1;
                    while (indexValue < desc.length() && desc.charAt(indexValue) == '[') {
                        indexValue++;
                    }
                    if (indexValue < desc.length()) {
                        return checkDesc(desc, indexValue, false);
                    } else {
                        throw new IllegalArgumentException("Invalid descriptor: " + desc);
                    }
            }
        } else {
            throw new IllegalArgumentException("Invalid type descriptor (must not be null or empty)");
        }
    }

    static void checkMethodDesc(String desc) {
        if (desc != null && desc.length() != 0) {
            if (desc.charAt(0) == '(' && desc.length() >= 3) {
                int start = 1;
                if (desc.charAt(start) != ')') {
                    do {
                        if (desc.charAt(start) == 'V') {
                            throw new IllegalArgumentException("Invalid descriptor: " + desc);
                        }
                        start = checkDesc(desc, start, false);
                    } while (start < desc.length() && desc.charAt(start) != ')');
                }
                start = checkDesc(desc, start + 1, true);
                if (start != desc.length()) {
                    throw new IllegalArgumentException("Invalid descriptor: " + desc);
                }
            } else {
                throw new IllegalArgumentException("Invalid descriptor: " + desc);
            }
        } else {
            throw new IllegalArgumentException("Invalid method descriptor (must not be null or empty)");
        }
    }

    void checkLabel(Label label, boolean checkVisited, String msg) {
        if (label == null) {
            throw new IllegalArgumentException("Invalid " + msg + " (must not be null)");
        } else if (checkVisited && this.labels.get(label) == null) {
            throw new IllegalArgumentException("Invalid " + msg + " (must be visited first)");
        }
    }

    private static void checkNonDebugLabel(Label label) {
        Field f = getLabelStatusField();
        int status = 0;
        try {
            status = f == null ? 0 : (Integer) f.get(label);
        } catch (IllegalAccessException exception4) {
            throw new Error("Internal error");
        }
        if ((status & 1) != 0) {
            throw new IllegalArgumentException("Labels used for debug info cannot be reused for control flow");
        }
    }

    private static Field getLabelStatusField() {
        if (labelStatusField == null) {
            labelStatusField = getLabelField("a");
            if (labelStatusField == null) {
                labelStatusField = getLabelField("status");
            }
        }
        return labelStatusField;
    }

    private static Field getLabelField(String name) {
        try {
            Field f = Label.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException exception5) {
            return null;
        }
    }

    static {
        String s = "BBBBBBBBBBBBBBBBCCIAADDDDDAAAAAAAAAAAAAAAAAAAABBBBBBBBDDDDDAAAAAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBJBBBBBBBBBBBBBBBBBBBBHHHHHHHHHHHHHHHHDKLBBBBBBFFFFGGGGAECEBBEEBBAMHHAA";
        TYPE = new int[s.length()];
        for (int i = 0; i < TYPE.length; i++) {
            TYPE[i] = s.charAt(i) - 'A' - 1;
        }
    }
}
