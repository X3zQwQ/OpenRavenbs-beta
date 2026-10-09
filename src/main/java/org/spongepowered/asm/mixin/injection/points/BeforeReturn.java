package org.spongepowered.asm.mixin.injection.points;

import java.util.Collection;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

@InjectionPoint.AtCode("RETURN")
public class BeforeReturn extends InjectionPoint {

    private final int ordinal;

    public BeforeReturn(InjectionPointData data) {
        super(data);
        this.ordinal = data.getOrdinal();
    }

    @Override
    public boolean checkPriority(int targetPriority, int ownerPriority) {
        return true;
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        boolean found = false;
        int returnOpcode = Type.getReturnType(desc).getOpcode(172);
        int ordinal = 0;
        for (java.util.Iterator<AbstractInsnNode> insnIterator = insns.iterator(); insnIterator.hasNext(); ) {
            AbstractInsnNode insn = insnIterator.next();
            if (insn instanceof InsnNode && insn.getOpcode() == returnOpcode) {
                if (this.ordinal == -1 || this.ordinal == ordinal) {
                    nodes.add(insn);
                    found = true;
                }
                ordinal++;
            }
        }
        return found;
    }
}
