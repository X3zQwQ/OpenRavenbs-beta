package org.spongepowered.asm.lib.tree;

import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.lib.AnnotationVisitor;
import org.spongepowered.asm.lib.Attribute;
import org.spongepowered.asm.lib.ClassVisitor;
import org.spongepowered.asm.lib.FieldVisitor;
import org.spongepowered.asm.lib.TypePath;

public class FieldNode extends FieldVisitor {

    public int access;

    public String name;

    public String desc;

    public String signature;

    public Object value;

    public List<AnnotationNode> visibleAnnotations;

    public List<AnnotationNode> invisibleAnnotations;

    public List<TypeAnnotationNode> visibleTypeAnnotations;

    public List<TypeAnnotationNode> invisibleTypeAnnotations;

    public List<Attribute> attrs;

    public FieldNode(int access, String name, String desc, String signature, Object value) {
        this(327680, access, name, desc, signature, value);
        if (this.getClass() != FieldNode.class) {
            throw new IllegalStateException();
        }
    }

    public FieldNode(int api, int access, String name, String desc, String signature, Object value) {
        super(api);
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.signature = signature;
        this.value = value;
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
    public void visitAttribute(Attribute attr) {
        if (this.attrs == null) {
            this.attrs = new ArrayList<>(1);
        }
        this.attrs.add(attr);
    }

    @Override
    public void visitEnd() {
    }

    public void check(int api) {
        if (api == 262144) {
            if (this.visibleTypeAnnotations != null && this.visibleTypeAnnotations.size() > 0) {
                throw new RuntimeException();
            }
            if (this.invisibleTypeAnnotations != null && this.invisibleTypeAnnotations.size() > 0) {
                throw new RuntimeException();
            }
        }
    }

    public void accept(ClassVisitor cv) {
        FieldVisitor fv = cv.visitField(this.access, this.name, this.desc, this.signature, this.value);
        if (fv != null) {
            int n = this.visibleAnnotations == null ? 0 : this.visibleAnnotations.size();
            for (int i = 0; i < n; i++) {
                AnnotationNode an = this.visibleAnnotations.get(i);
                an.accept(fv.visitAnnotation(an.desc, true));
            }
            n = this.invisibleAnnotations == null ? 0 : this.invisibleAnnotations.size();
            for (int intValue = 0; intValue < n; intValue++) {
                AnnotationNode an = this.invisibleAnnotations.get(intValue);
                an.accept(fv.visitAnnotation(an.desc, false));
            }
            n = this.visibleTypeAnnotations == null ? 0 : this.visibleTypeAnnotations.size();
            for (int intValue2 = 0; intValue2 < n; intValue2++) {
                TypeAnnotationNode an = this.visibleTypeAnnotations.get(intValue2);
                an.accept(fv.visitTypeAnnotation(an.typeRef, an.typePath, an.desc, true));
            }
            n = this.invisibleTypeAnnotations == null ? 0 : this.invisibleTypeAnnotations.size();
            for (int intValue3 = 0; intValue3 < n; intValue3++) {
                TypeAnnotationNode an = this.invisibleTypeAnnotations.get(intValue3);
                an.accept(fv.visitTypeAnnotation(an.typeRef, an.typePath, an.desc, false));
            }
            n = this.attrs == null ? 0 : this.attrs.size();
            for (int intValue4 = 0; intValue4 < n; intValue4++) {
                fv.visitAttribute(this.attrs.get(intValue4));
            }
            fv.visitEnd();
        }
    }
}
