package org.spongepowered.asm.lib.commons;

import java.util.Stack;
import org.spongepowered.asm.lib.signature.SignatureVisitor;

public class SignatureRemapper extends SignatureVisitor {

    private final SignatureVisitor v;

    private final Remapper remapper;

    private Stack<String> classNames = new Stack<>();

    public SignatureRemapper(SignatureVisitor v, Remapper remapper) {
        this(327680, v, remapper);
    }

    protected SignatureRemapper(int api, SignatureVisitor v, Remapper remapper) {
        super(api);
        this.v = v;
        this.remapper = remapper;
    }

    @Override
    public void visitClassType(String name) {
        this.classNames.push(name);
        this.v.visitClassType(this.remapper.mapType(name));
    }

    @Override
    public void visitInnerClassType(String name) {
        String outerClassName = this.classNames.pop();
        String className = outerClassName + '$' + name;
        this.classNames.push(className);
        String remappedOuter = this.remapper.mapType(outerClassName) + '$';
        String remappedName = this.remapper.mapType(className);
        int index = remappedName.startsWith(remappedOuter) ? remappedOuter.length() : remappedName.lastIndexOf(36) + 1;
        this.v.visitInnerClassType(remappedName.substring(index));
    }

    @Override
    public void visitFormalTypeParameter(String name) {
        this.v.visitFormalTypeParameter(name);
    }

    @Override
    public void visitTypeVariable(String name) {
        this.v.visitTypeVariable(name);
    }

    @Override
    public SignatureVisitor visitArrayType() {
        this.v.visitArrayType();
        return this;
    }

    @Override
    public void visitBaseType(char descriptor) {
        this.v.visitBaseType(descriptor);
    }

    @Override
    public SignatureVisitor visitClassBound() {
        this.v.visitClassBound();
        return this;
    }

    @Override
    public SignatureVisitor visitExceptionType() {
        this.v.visitExceptionType();
        return this;
    }

    @Override
    public SignatureVisitor visitInterface() {
        this.v.visitInterface();
        return this;
    }

    @Override
    public SignatureVisitor visitInterfaceBound() {
        this.v.visitInterfaceBound();
        return this;
    }

    @Override
    public SignatureVisitor visitParameterType() {
        this.v.visitParameterType();
        return this;
    }

    @Override
    public SignatureVisitor visitReturnType() {
        this.v.visitReturnType();
        return this;
    }

    @Override
    public SignatureVisitor visitSuperclass() {
        this.v.visitSuperclass();
        return this;
    }

    @Override
    public void visitTypeArgument() {
        this.v.visitTypeArgument();
    }

    @Override
    public SignatureVisitor visitTypeArgument(char wildcard) {
        this.v.visitTypeArgument(wildcard);
        return this;
    }

    @Override
    public void visitEnd() {
        this.v.visitEnd();
        this.classNames.pop();
    }
}
