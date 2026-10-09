package org.spongepowered.asm.lib.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.spongepowered.asm.lib.AnnotationVisitor;
import org.spongepowered.asm.lib.Attribute;
import org.spongepowered.asm.lib.ClassVisitor;
import org.spongepowered.asm.lib.Handle;
import org.spongepowered.asm.lib.Label;
import org.spongepowered.asm.lib.MethodVisitor;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.TypePath;

public class MethodNode extends MethodVisitor {

    public int access;

    public String name;

    public String desc;

    public String signature;

    public List<String> exceptions;

    public List<ParameterNode> parameters;

    public List<AnnotationNode> visibleAnnotations;

    public List<AnnotationNode> invisibleAnnotations;

    public List<TypeAnnotationNode> visibleTypeAnnotations;

    public List<TypeAnnotationNode> invisibleTypeAnnotations;

    public List<Attribute> attrs;

    public Object annotationDefault;

    public List<AnnotationNode>[] visibleParameterAnnotations;

    public List<AnnotationNode>[] invisibleParameterAnnotations;

    public InsnList instructions;

    public List<TryCatchBlockNode> tryCatchBlocks;

    public int maxStack;

    public int maxLocals;

    public List<LocalVariableNode> localVariables;

    public List<LocalVariableAnnotationNode> visibleLocalVariableAnnotations;

    public List<LocalVariableAnnotationNode> invisibleLocalVariableAnnotations;

    private boolean visited;

    public MethodNode() {
        this(327680);
        if (this.getClass() != MethodNode.class) {
            throw new IllegalStateException();
        }
    }

    public MethodNode(int api) {
        super(api);
        this.instructions = new InsnList();
    }

    public MethodNode(int access, String name, String desc, String signature, String[] exceptions) {
        this(327680, access, name, desc, signature, exceptions);
        if (this.getClass() != MethodNode.class) {
            throw new IllegalStateException();
        }
    }

    public MethodNode(int api, int access, String name, String desc, String signature, String[] exceptions) {
        super(api);
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.signature = signature;
        this.exceptions = new ArrayList<>(exceptions == null ? 0 : exceptions.length);
        boolean isAbstract = (access & 1024) != 0;
        if (!isAbstract) {
            this.localVariables = new ArrayList<>(5);
        }
        this.tryCatchBlocks = new ArrayList<>();
        if (exceptions != null) {
            this.exceptions.addAll(Arrays.asList(exceptions));
        }
        this.instructions = new InsnList();
    }

    @Override
    public void visitParameter(String name, int access) {
        if (this.parameters == null) {
            this.parameters = new ArrayList<>(5);
        }
        this.parameters.add(new ParameterNode(name, access));
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return new AnnotationNode(new ArrayList<Object>(0) {

            @Override
            public boolean add(Object o) {
                MethodNode.this.annotationDefault = o;
                return super.add(o);
            }
        });
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationNode an = new AnnotationNode(desc);
        if (visible) {
            if (this.visibleAnnotations == null) {
                this.visibleAnnotations = new ArrayList<>(1);
            }
            this.visibleAnnotations.add(an);
        } else {
            if (this.invisibleAnnotations == null) {
                this.invisibleAnnotations = new ArrayList<>(1);
            }
            this.invisibleAnnotations.add(an);
        }
        return an;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        TypeAnnotationNode an = new TypeAnnotationNode(typeRef, typePath, desc);
        if (visible) {
            if (this.visibleTypeAnnotations == null) {
                this.visibleTypeAnnotations = new ArrayList<>(1);
            }
            this.visibleTypeAnnotations.add(an);
        } else {
            if (this.invisibleTypeAnnotations == null) {
                this.invisibleTypeAnnotations = new ArrayList<>(1);
            }
            this.invisibleTypeAnnotations.add(an);
        }
        return an;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        AnnotationNode an = new AnnotationNode(desc);
        if (visible) {
            if (this.visibleParameterAnnotations == null) {
                int params = Type.getArgumentTypes(this.desc).length;
                this.visibleParameterAnnotations = new List[params];
            }
            if (this.visibleParameterAnnotations[parameter] == null) {
                this.visibleParameterAnnotations[parameter] = new ArrayList<>(1);
            }
            this.visibleParameterAnnotations[parameter].add(an);
        } else {
            if (this.invisibleParameterAnnotations == null) {
                int params = Type.getArgumentTypes(this.desc).length;
                this.invisibleParameterAnnotations = new List[params];
            }
            if (this.invisibleParameterAnnotations[parameter] == null) {
                this.invisibleParameterAnnotations[parameter] = new ArrayList<>(1);
            }
            this.invisibleParameterAnnotations[parameter].add(an);
        }
        return an;
    }

    @Override
    public void visitAttribute(Attribute attr) {
        if (this.attrs == null) {
            this.attrs = new ArrayList<>(1);
        }
        this.attrs.add(attr);
    }

    @Override
    public void visitCode() {
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        this.instructions.add(new FrameNode(type, nLocal, local == null ? null : this.getLabelNodes(local), nStack, stack == null ? null : this.getLabelNodes(stack)));
    }

    @Override
    public void visitInsn(int opcode) {
        this.instructions.add(new InsnNode(opcode));
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        this.instructions.add(new IntInsnNode(opcode, operand));
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        this.instructions.add(new VarInsnNode(opcode, var));
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        this.instructions.add(new TypeInsnNode(opcode, type));
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        this.instructions.add(new FieldInsnNode(opcode, owner, name, desc));
    }

    @Deprecated
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if (this.api >= 327680) {
            super.visitMethodInsn(opcode, owner, name, desc);
        } else {
            this.instructions.add(new MethodInsnNode(opcode, owner, name, desc));
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (this.api < 327680) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        } else {
            this.instructions.add(new MethodInsnNode(opcode, owner, name, desc, itf));
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        this.instructions.add(new InvokeDynamicInsnNode(name, desc, bsm, bsmArgs));
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        this.instructions.add(new JumpInsnNode(opcode, this.getLabelNode(label)));
    }

    @Override
    public void visitLabel(Label label) {
        this.instructions.add(this.getLabelNode(label));
    }

    @Override
    public void visitLdcInsn(Object cst) {
        this.instructions.add(new LdcInsnNode(cst));
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        this.instructions.add(new IincInsnNode(var, increment));
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        this.instructions.add(new TableSwitchInsnNode(min, max, this.getLabelNode(dflt), this.getLabelNodes(labels)));
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        this.instructions.add(new LookupSwitchInsnNode(this.getLabelNode(dflt), keys, this.getLabelNodes(labels)));
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        this.instructions.add(new MultiANewArrayInsnNode(desc, dims));
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        AbstractInsnNode insn = this.instructions.getLast();
        while (insn.getOpcode() == -1) {
            insn = insn.getPrevious();
        }
        TypeAnnotationNode an = new TypeAnnotationNode(typeRef, typePath, desc);
        if (visible) {
            if (insn.visibleTypeAnnotations == null) {
                insn.visibleTypeAnnotations = new ArrayList<>(1);
            }
            insn.visibleTypeAnnotations.add(an);
        } else {
            if (insn.invisibleTypeAnnotations == null) {
                insn.invisibleTypeAnnotations = new ArrayList<>(1);
            }
            insn.invisibleTypeAnnotations.add(an);
        }
        return an;
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        this.tryCatchBlocks.add(new TryCatchBlockNode(this.getLabelNode(start), this.getLabelNode(end), this.getLabelNode(handler), type));
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        TryCatchBlockNode tcb = this.tryCatchBlocks.get((typeRef & 16776960) >> 8);
        TypeAnnotationNode an = new TypeAnnotationNode(typeRef, typePath, desc);
        if (visible) {
            if (tcb.visibleTypeAnnotations == null) {
                tcb.visibleTypeAnnotations = new ArrayList<>(1);
            }
            tcb.visibleTypeAnnotations.add(an);
        } else {
            if (tcb.invisibleTypeAnnotations == null) {
                tcb.invisibleTypeAnnotations = new ArrayList<>(1);
            }
            tcb.invisibleTypeAnnotations.add(an);
        }
        return an;
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        this.localVariables.add(new LocalVariableNode(name, desc, signature, this.getLabelNode(start), this.getLabelNode(end), index));
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
        LocalVariableAnnotationNode an = new LocalVariableAnnotationNode(typeRef, typePath, this.getLabelNodes(start), this.getLabelNodes(end), index, desc);
        if (visible) {
            if (this.visibleLocalVariableAnnotations == null) {
                this.visibleLocalVariableAnnotations = new ArrayList<>(1);
            }
            this.visibleLocalVariableAnnotations.add(an);
        } else {
            if (this.invisibleLocalVariableAnnotations == null) {
                this.invisibleLocalVariableAnnotations = new ArrayList<>(1);
            }
            this.invisibleLocalVariableAnnotations.add(an);
        }
        return an;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.instructions.add(new LineNumberNode(line, this.getLabelNode(start)));
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        this.maxStack = maxStack;
        this.maxLocals = maxLocals;
    }

    @Override
    public void visitEnd() {
    }

    protected LabelNode getLabelNode(Label l) {
        if (!(l.info instanceof LabelNode)) {
            l.info = new LabelNode();
        }
        return (LabelNode) l.info;
    }

    private LabelNode[] getLabelNodes(Label[] l) {
        LabelNode[] nodes = new LabelNode[l.length];
        for (int i = 0; i < l.length; i++) {
            nodes[i] = this.getLabelNode(l[i]);
        }
        return nodes;
    }

    private Object[] getLabelNodes(Object[] objs) {
        Object[] nodes = new Object[objs.length];
        for (int i = 0; i < objs.length; i++) {
            Object o = objs[i];
            if (o instanceof Label) {
                o = this.getLabelNode((Label) o);
            }
            nodes[i] = o;
        }
        return nodes;
    }

    public void check(int api) {
        if (api == 262144) {
            if (this.visibleTypeAnnotations != null && this.visibleTypeAnnotations.size() > 0) {
                throw new RuntimeException();
            }
            if (this.invisibleTypeAnnotations != null && this.invisibleTypeAnnotations.size() > 0) {
                throw new RuntimeException();
            }
            int n = this.tryCatchBlocks == null ? 0 : this.tryCatchBlocks.size();
            for (int i = 0; i < n; i++) {
                TryCatchBlockNode tcb = this.tryCatchBlocks.get(i);
                if (tcb.visibleTypeAnnotations != null && tcb.visibleTypeAnnotations.size() > 0) {
                    throw new RuntimeException();
                }
                if (tcb.invisibleTypeAnnotations != null && tcb.invisibleTypeAnnotations.size() > 0) {
                    throw new RuntimeException();
                }
            }
            for (int i = 0; i < this.instructions.size(); i++) {
                AbstractInsnNode insn = this.instructions.get(i);
                if (insn.visibleTypeAnnotations != null && insn.visibleTypeAnnotations.size() > 0) {
                    throw new RuntimeException();
                }
                if (insn.invisibleTypeAnnotations != null && insn.invisibleTypeAnnotations.size() > 0) {
                    throw new RuntimeException();
                }
                if (insn instanceof MethodInsnNode) {
                    boolean itf = ((MethodInsnNode) insn).itf;
                    if (itf != (insn.opcode == 185)) {
                        throw new RuntimeException();
                    }
                }
            }
            if (this.visibleLocalVariableAnnotations != null && this.visibleLocalVariableAnnotations.size() > 0) {
                throw new RuntimeException();
            }
            if (this.invisibleLocalVariableAnnotations != null && this.invisibleLocalVariableAnnotations.size() > 0) {
                throw new RuntimeException();
            }
        }
    }

    public void accept(ClassVisitor cv) {
        String[] exceptions = new String[this.exceptions.size()];
        this.exceptions.toArray(exceptions);
        MethodVisitor mv = cv.visitMethod(this.access, this.name, this.desc, this.signature, exceptions);
        if (mv != null) {
            this.accept(mv);
        }
    }

    public void accept(MethodVisitor mv) {
        int n = this.parameters == null ? 0 : this.parameters.size();
        for (int i = 0; i < n; i++) {
            ParameterNode parameter = this.parameters.get(i);
            mv.visitParameter(parameter.name, parameter.access);
        }
        if (this.annotationDefault != null) {
            AnnotationVisitor av = mv.visitAnnotationDefault();
            AnnotationNode.accept(av, null, this.annotationDefault);
            if (av != null) {
                av.visitEnd();
            }
        }
        n = this.visibleAnnotations == null ? 0 : this.visibleAnnotations.size();
        for (int intValue = 0; intValue < n; intValue++) {
            AnnotationNode an = this.visibleAnnotations.get(intValue);
            an.accept(mv.visitAnnotation(an.desc, true));
        }
        n = this.invisibleAnnotations == null ? 0 : this.invisibleAnnotations.size();
        for (int intValue2 = 0; intValue2 < n; intValue2++) {
            AnnotationNode an = this.invisibleAnnotations.get(intValue2);
            an.accept(mv.visitAnnotation(an.desc, false));
        }
        n = this.visibleTypeAnnotations == null ? 0 : this.visibleTypeAnnotations.size();
        for (int intValue3 = 0; intValue3 < n; intValue3++) {
            TypeAnnotationNode an = this.visibleTypeAnnotations.get(intValue3);
            an.accept(mv.visitTypeAnnotation(an.typeRef, an.typePath, an.desc, true));
        }
        n = this.invisibleTypeAnnotations == null ? 0 : this.invisibleTypeAnnotations.size();
        for (int intValue4 = 0; intValue4 < n; intValue4++) {
            TypeAnnotationNode an = this.invisibleTypeAnnotations.get(intValue4);
            an.accept(mv.visitTypeAnnotation(an.typeRef, an.typePath, an.desc, false));
        }
        n = this.visibleParameterAnnotations == null ? 0 : this.visibleParameterAnnotations.length;
        for (int intValue5 = 0; intValue5 < n; intValue5++) {
            List<?> l = this.visibleParameterAnnotations[intValue5];
            if (l != null) {
                for (int j = 0; j < l.size(); j++) {
                    AnnotationNode an = (AnnotationNode) l.get(j);
                    an.accept(mv.visitParameterAnnotation(intValue5, an.desc, true));
                }
            }
        }
        n = this.invisibleParameterAnnotations == null ? 0 : this.invisibleParameterAnnotations.length;
        for (int intValue6 = 0; intValue6 < n; intValue6++) {
            List<?> l = this.invisibleParameterAnnotations[intValue6];
            if (l != null) {
                for (int j = 0; j < l.size(); j++) {
                    AnnotationNode an = (AnnotationNode) l.get(j);
                    an.accept(mv.visitParameterAnnotation(intValue6, an.desc, false));
                }
            }
        }
        if (this.visited) {
            this.instructions.resetLabels();
        }
        n = this.attrs == null ? 0 : this.attrs.size();
        for (int intValue7 = 0; intValue7 < n; intValue7++) {
            mv.visitAttribute(this.attrs.get(intValue7));
        }
        if (this.instructions.size() > 0) {
            mv.visitCode();
            n = this.tryCatchBlocks == null ? 0 : this.tryCatchBlocks.size();
            for (int intValue8 = 0; intValue8 < n; intValue8++) {
                this.tryCatchBlocks.get(intValue8).updateIndex(intValue8);
                this.tryCatchBlocks.get(intValue8).accept(mv);
            }
            this.instructions.accept(mv);
            n = this.localVariables == null ? 0 : this.localVariables.size();
            for (int intValue9 = 0; intValue9 < n; intValue9++) {
                this.localVariables.get(intValue9).accept(mv);
            }
            n = this.visibleLocalVariableAnnotations == null ? 0 : this.visibleLocalVariableAnnotations.size();
            for (int intValue10 = 0; intValue10 < n; intValue10++) {
                this.visibleLocalVariableAnnotations.get(intValue10).accept(mv, true);
            }
            n = this.invisibleLocalVariableAnnotations == null ? 0 : this.invisibleLocalVariableAnnotations.size();
            for (int intValue11 = 0; intValue11 < n; intValue11++) {
                this.invisibleLocalVariableAnnotations.get(intValue11).accept(mv, false);
            }
            mv.visitMaxs(this.maxStack, this.maxLocals);
            this.visited = true;
        }
        mv.visitEnd();
    }
}
