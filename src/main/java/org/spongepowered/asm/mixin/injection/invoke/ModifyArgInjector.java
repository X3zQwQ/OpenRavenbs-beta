package org.spongepowered.asm.mixin.injection.invoke;

import java.util.Arrays;
import java.util.List;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Bytecode;

public class ModifyArgInjector extends InvokeInjector {

    private final int index;

    private final boolean singleArgMode;

    public ModifyArgInjector(InjectionInfo info, int index) {
        super(info, "@ModifyArg");
        this.index = index;
        this.singleArgMode = this.methodArgs.length == 1;
    }

    @Override
    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        super.sanityCheck(target, injectionPoints);
        if (this.singleArgMode && !this.methodArgs[0].equals(this.returnType)) {
            throw new InvalidInjectionException(this.info, "@ModifyArg return type on " + this + " must match the parameter type. ARG=" + this.methodArgs[0] + " RETURN=" + this.returnType);
        }
    }

    @Override
    protected void checkTarget(Target target) {
        if (!this.isStatic && target.isStatic) {
            throw new InvalidInjectionException(this.info, "non-static callback method " + this + " targets a static method which is not supported");
        }
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        this.checkTargetForNode(target, node);
        super.inject(target, node);
    }

    @Override
    protected void injectAtInvoke(Target target, InjectionNodes.InjectionNode node) {
        MethodInsnNode methodNode = (MethodInsnNode) node.getCurrentTarget();
        Type[] args = Type.getArgumentTypes(methodNode.desc);
        int intValue3 = this.findArgIndex(target, args);
        InsnList insns = new InsnList();
        int extraLocals = 0;
        if (this.singleArgMode) {
            extraLocals = this.injectSingleArgHandler(target, args, intValue3, insns);
        } else {
            extraLocals = this.injectMultiArgHandler(target, args, intValue3, insns);
        }
        target.insns.insertBefore(methodNode, insns);
        target.addToLocals(extraLocals);
        target.addToStack(2 - (extraLocals - 1));
    }

    private int injectSingleArgHandler(Target target, Type[] args, int intValue, InsnList insns) {
        int[] argMap = this.storeArgs(target, args, insns, intValue);
        this.invokeHandlerWithArgs(args, insns, argMap, intValue, intValue + 1);
        this.pushArgs(args, insns, argMap, intValue + 1, args.length);
        return argMap[argMap.length - 1] - target.getMaxLocals() + args[args.length - 1].getSize();
    }

    private int injectMultiArgHandler(Target target, Type[] args, int intValue2, InsnList insns) {
        if (!Arrays.equals((Object[]) args, (Object[]) this.methodArgs)) {
            throw new InvalidInjectionException(this.info, "@ModifyArg method " + this + " targets a method with an invalid signature " + Bytecode.getDescriptor(args) + ", expected " + Bytecode.getDescriptor(this.methodArgs));
        } else {
            int[] argMap = this.storeArgs(target, args, insns, 0);
            this.pushArgs(args, insns, argMap, 0, intValue2);
            this.invokeHandlerWithArgs(args, insns, argMap, 0, args.length);
            this.pushArgs(args, insns, argMap, intValue2 + 1, args.length);
            return argMap[argMap.length - 1] - target.getMaxLocals() + args[args.length - 1].getSize();
        }
    }

    protected int findArgIndex(Target target, Type[] args) {
        if (this.index > -1) {
            if (this.index < args.length && args[this.index].equals(this.returnType)) {
                return this.index;
            } else {
                throw new InvalidInjectionException(this.info, "Specified index " + this.index + " for @ModifyArg is invalid for args " + Bytecode.getDescriptor(args) + ", expected " + this.returnType + " on " + this);
            }
        } else {
            int intValue4 = -1;
            for (int arg = 0; arg < args.length; arg++) {
                if (args[arg].equals(this.returnType)) {
                    if (intValue4 != -1) {
                        throw new InvalidInjectionException(this.info, "Found duplicate args with index [" + intValue4 + ", " + arg + "] matching type " + this.returnType + " for @ModifyArg target " + target + " in " + this + ". Please specify index of desired arg.");
                    }
                    intValue4 = arg;
                }
            }
            if (intValue4 == -1) {
                throw new InvalidInjectionException(this.info, "Could not find arg matching type " + this.returnType + " for @ModifyArg target " + target + " in " + this);
            } else {
                return intValue4;
            }
        }
    }
}
