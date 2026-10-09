package org.spongepowered.asm.lib.tree.analysis;

import java.util.List;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;

public abstract class Interpreter<V extends Value> {

    protected final int api;

    protected Interpreter(int api) {
        this.api = api;
    }

    public abstract V newValue(Type type);

    public abstract V newOperation(AbstractInsnNode abstractInsnNode) throws AnalyzerException;

    public abstract V copyOperation(AbstractInsnNode abstractInsnNode2, V value) throws AnalyzerException;

    public abstract V unaryOperation(AbstractInsnNode abstractInsnNode3, V value2) throws AnalyzerException;

    public abstract V binaryOperation(AbstractInsnNode abstractInsnNode4, V value3, V value4) throws AnalyzerException;

    public abstract V ternaryOperation(AbstractInsnNode abstractInsnNode5, V value5, V value6, V value7) throws AnalyzerException;

    public abstract V naryOperation(AbstractInsnNode abstractInsnNode6, List<? extends V> list) throws AnalyzerException;

    public abstract void returnOperation(AbstractInsnNode abstractInsnNode7, V value8, V value9) throws AnalyzerException;

    public abstract V merge(V value10, V value11);
}
