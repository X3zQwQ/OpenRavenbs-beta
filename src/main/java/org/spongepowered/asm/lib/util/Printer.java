package org.spongepowered.asm.lib.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.lib.Attribute;
import org.spongepowered.asm.lib.Handle;
import org.spongepowered.asm.lib.Label;
import org.spongepowered.asm.lib.TypePath;

public abstract class Printer {

    public static final String[] OPCODES = new String[200];

    public static final String[] TYPES;

    public static final String[] HANDLE_TAG;

    protected final int api;

    protected final StringBuffer buf;

    public final List<Object> text;

    protected Printer(int api) {
        this.api = api;
        this.buf = new StringBuffer();
        this.text = new ArrayList<>();
    }

    public abstract void visit(int intValue, int intValue2, String text2, String text3, String text4, String[] stringArray);

    public abstract void visitSource(String text5, String text6);

    public abstract void visitOuterClass(String text7, String text8, String text9);

    public abstract Printer visitClassAnnotation(String text10, boolean enabled);

    public Printer visitClassTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        throw new RuntimeException("Must be overriden");
    }

    public abstract void visitClassAttribute(Attribute attribute);

    public abstract void visitInnerClass(String text11, String text12, String text13, int intValue3);

    public abstract Printer visitField(int intValue4, String text14, String text15, String text16, Object object);

    public abstract Printer visitMethod(int intValue5, String text17, String text18, String text19, String[] stringArray2);

    public abstract void visitClassEnd();

    public abstract void visit(String text20, Object object2);

    public abstract void visitEnum(String text21, String text22, String text23);

    public abstract Printer visitAnnotation(String text24, String text25);

    public abstract Printer visitArray(String text26);

    public abstract void visitAnnotationEnd();

    public abstract Printer visitFieldAnnotation(String text27, boolean enabled2);

    public Printer visitFieldTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        throw new RuntimeException("Must be overriden");
    }

    public abstract void visitFieldAttribute(Attribute attribute2);

    public abstract void visitFieldEnd();

    public void visitParameter(String name, int access) {
        throw new RuntimeException("Must be overriden");
    }

    public abstract Printer visitAnnotationDefault();

    public abstract Printer visitMethodAnnotation(String text28, boolean enabled3);

    public Printer visitMethodTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        throw new RuntimeException("Must be overriden");
    }

    public abstract Printer visitParameterAnnotation(int intValue6, String text29, boolean enabled4);

    public abstract void visitMethodAttribute(Attribute attribute3);

    public abstract void visitCode();

    public abstract void visitFrame(int intValue7, int intValue8, Object[] objectArray, int intValue9, Object[] objectArray2);

    public abstract void visitInsn(int intValue10);

    public abstract void visitIntInsn(int intValue11, int intValue12);

    public abstract void visitVarInsn(int intValue13, int intValue14);

    public abstract void visitTypeInsn(int intValue15, String text30);

    public abstract void visitFieldInsn(int intValue16, String text31, String text32, String text33);

    @Deprecated
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if (this.api >= 327680) {
            boolean itf = opcode == 185;
            this.visitMethodInsn(opcode, owner, name, desc, itf);
        } else {
            throw new RuntimeException("Must be overriden");
        }
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (this.api < 327680) {
            if (itf != (opcode == 185)) {
                throw new IllegalArgumentException("INVOKESPECIAL/STATIC on interfaces require ASM 5");
            } else {
                this.visitMethodInsn(opcode, owner, name, desc);
            }
        } else {
            throw new RuntimeException("Must be overriden");
        }
    }

    public abstract void visitInvokeDynamicInsn(String text34, String text35, Handle handle, Object... object3);

    public abstract void visitJumpInsn(int intValue17, Label label);

    public abstract void visitLabel(Label label2);

    public abstract void visitLdcInsn(Object object4);

    public abstract void visitIincInsn(int intValue18, int intValue19);

    public abstract void visitTableSwitchInsn(int intValue20, int intValue21, Label label3, Label... label4);

    public abstract void visitLookupSwitchInsn(Label label5, int[] intArray, Label[] labelArray);

    public abstract void visitMultiANewArrayInsn(String text36, int intValue22);

    public Printer visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        throw new RuntimeException("Must be overriden");
    }

    public abstract void visitTryCatchBlock(Label label6, Label label7, Label label8, String text37);

    public Printer visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        throw new RuntimeException("Must be overriden");
    }

    public abstract void visitLocalVariable(String text38, String text39, String text40, Label label9, Label label10, int intValue23);

    public Printer visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
        throw new RuntimeException("Must be overriden");
    }

    public abstract void visitLineNumber(int intValue24, Label label11);

    public abstract void visitMaxs(int intValue25, int intValue26);

    public abstract void visitMethodEnd();

    public List<Object> getText() {
        return this.text;
    }

    public void print(PrintWriter pw) {
        printList(pw, this.text);
    }

    public static void appendString(StringBuffer buf, String s) {
        buf.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                buf.append("\\n");
            } else if (c == '\r') {
                buf.append("\\r");
            } else if (c == '\\') {
                buf.append("\\\\");
            } else if (c == '"') {
                buf.append("\\\"");
            } else if (c >= ' ' && c <= 127) {
                buf.append(c);
            } else {
                buf.append("\\u");
                if (c < 16) {
                    buf.append("000");
                } else if (c < 256) {
                    buf.append("00");
                } else if (c < 4096) {
                    buf.append('0');
                }
                buf.append(Integer.toString(c, 16));
            }
        }
        buf.append('"');
    }

    static void printList(PrintWriter pw, List<?> l) {
        for (int i = 0; i < l.size(); i++) {
            Object o = l.get(i);
            if (o instanceof List) {
                printList(pw, (List<?>) o);
            } else {
                pw.print(o.toString());
            }
        }
    }

    static {
        String s = "NOP,ACONST_NULL,ICONST_M1,ICONST_0,ICONST_1,ICONST_2,ICONST_3,ICONST_4,ICONST_5,LCONST_0,LCONST_1,FCONST_0,FCONST_1,FCONST_2,DCONST_0,DCONST_1,BIPUSH,SIPUSH,LDC,,,ILOAD,LLOAD,FLOAD,DLOAD,ALOAD,,,,,,,,,,,,,,,,,,,,,IALOAD,LALOAD,FALOAD,DALOAD,AALOAD,BALOAD,CALOAD,SALOAD,ISTORE,LSTORE,FSTORE,DSTORE,ASTORE,,,,,,,,,,,,,,,,,,,,,IASTORE,LASTORE,FASTORE,DASTORE,AASTORE,BASTORE,CASTORE,SASTORE,POP,POP2,DUP,DUP_X1,DUP_X2,DUP2,DUP2_X1,DUP2_X2,SWAP,IADD,LADD,FADD,DADD,ISUB,LSUB,FSUB,DSUB,IMUL,LMUL,FMUL,DMUL,IDIV,LDIV,FDIV,DDIV,IREM,LREM,FREM,DREM,INEG,LNEG,FNEG,DNEG,ISHL,LSHL,ISHR,LSHR,IUSHR,LUSHR,IAND,LAND,IOR,LOR,IXOR,LXOR,IINC,I2L,I2F,I2D,L2I,L2F,L2D,F2I,F2L,F2D,D2I,D2L,D2F,I2B,I2C,I2S,LCMP,FCMPL,FCMPG,DCMPL,DCMPG,IFEQ,IFNE,IFLT,IFGE,IFGT,IFLE,IF_ICMPEQ,IF_ICMPNE,IF_ICMPLT,IF_ICMPGE,IF_ICMPGT,IF_ICMPLE,IF_ACMPEQ,IF_ACMPNE,GOTO,JSR,RET,TABLESWITCH,LOOKUPSWITCH,IRETURN,LRETURN,FRETURN,DRETURN,ARETURN,RETURN,GETSTATIC,PUTSTATIC,GETFIELD,PUTFIELD,INVOKEVIRTUAL,INVOKESPECIAL,INVOKESTATIC,INVOKEINTERFACE,INVOKEDYNAMIC,NEW,NEWARRAY,ANEWARRAY,ARRAYLENGTH,ATHROW,CHECKCAST,INSTANCEOF,MONITORENTER,MONITOREXIT,,MULTIANEWARRAY,IFNULL,IFNONNULL,";
        int i = 0;
        int j = 0;
        int l;
        while ((l = s.indexOf(44, j)) > 0) {
            OPCODES[i++] = j + 1 == l ? null : s.substring(j, l);
            j = l + 1;
        }
        s = "T_BOOLEAN,T_CHAR,T_FLOAT,T_DOUBLE,T_BYTE,T_SHORT,T_INT,T_LONG,";
        TYPES = new String[12];
        j = 0;
        i = 4;
        while ((l = s.indexOf(44, j)) > 0) {
            TYPES[i++] = s.substring(j, l);
            j = l + 1;
        }
        s = "H_GETFIELD,H_GETSTATIC,H_PUTFIELD,H_PUTSTATIC,H_INVOKEVIRTUAL,H_INVOKESTATIC,H_INVOKESPECIAL,H_NEWINVOKESPECIAL,H_INVOKEINTERFACE,";
        HANDLE_TAG = new String[10];
        j = 0;
        i = 1;
        while ((l = s.indexOf(44, j)) > 0) {
            HANDLE_TAG[i++] = s.substring(j, l);
            j = l + 1;
        }
    }
}
