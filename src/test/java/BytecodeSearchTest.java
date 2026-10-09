import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.*;
import org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess;
import org.spongepowered.asm.mixin.injection.points.BeforeNew;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class BytecodeSearchTest {

    @Test
    public void arraySearchStartsAfterSelectedField() {
        InsnList instructions = new InsnList();
        instructions.add(new InsnNode(Opcodes.IALOAD));
        FieldInsnNode field = new FieldInsnNode(Opcodes.GETFIELD, "Example", "array", "[I");
        instructions.add(field);
        InsnNode expected = new InsnNode(Opcodes.IALOAD);
        instructions.add(expected);
        assertSame(expected, BeforeFieldAccess.findArrayNode(instructions, field, Opcodes.IALOAD, 8));
    }

    @Test
    public void arraySearchRejectsEarlierAccess() {
        InsnList instructions = new InsnList();
        instructions.add(new InsnNode(Opcodes.IALOAD));
        FieldInsnNode field = new FieldInsnNode(Opcodes.GETFIELD, "Example", "array", "[I");
        instructions.add(field);
        instructions.add(new InsnNode(Opcodes.RETURN));
        assertNull(BeforeFieldAccess.findArrayNode(instructions, field, Opcodes.IALOAD, 8));
    }

    @Test
    public void constructorLookupSkipsEarlierAllocation() throws Exception {
        Target target = (Target) allocate(Target.class);
        InsnList instructions = new InsnList();
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "Example", "<init>", "()V", false));
        TypeInsnNode allocation = new TypeInsnNode(Opcodes.NEW, "Example");
        instructions.add(allocation);
        MethodInsnNode expected = new MethodInsnNode(Opcodes.INVOKESPECIAL, "Example", "<init>", "(I)V", false);
        instructions.add(expected);
        setField(Target.class, target, "insns", instructions);
        assertSame(expected, target.findInitNodeFor(allocation));
    }

    @Test
    public void newSearchDoesNotBorrowEarlierConstructor() throws Exception {
        BeforeNew search = (BeforeNew) allocate(BeforeNew.class);
        setField(BeforeNew.class, search, "desc", "()V");
        InsnList instructions = new InsnList();
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "Example", "<init>", "()V", false));
        TypeInsnNode allocation = new TypeInsnNode(Opcodes.NEW, "Example");
        instructions.add(allocation);
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "Example", "<init>", "(I)V", false));
        Method find = BeforeNew.class.getDeclaredMethod("findCtor", InsnList.class, TypeInsnNode.class);
        find.setAccessible(true);
        assertEquals(false, find.invoke(search, instructions, allocation));
    }

    @Test
    public void indexedSearchesKeepOriginalIteratorOverload() throws Exception {
        String[][] searches = {
            {"org/spongepowered/asm/mixin/injection/points/BeforeFieldAccess", "findArrayNode"},
            {"org/spongepowered/asm/mixin/injection/points/BeforeNew", "findCtor"},
            {"org/spongepowered/asm/mixin/injection/struct/Target", "findInitNodeFor"},
            {"org/spongepowered/asm/mixin/transformer/MixinTargetContext", "processImaginarySuper"},
            {"org/spongepowered/asm/mixin/transformer/MixinApplicatorStandard", "getInitialiser"}
        };
        for (String[] search : searches) {
            ClassNode node = new ClassNode();
            try (InputStream input = getClass().getResourceAsStream("/" + search[0] + ".class")) {
                new ClassReader(input).accept(node, 0);
            }
            int indexed = 0;
            for (MethodNode method : node.methods) {
                if (!search[1].equals(method.name)) {
                    continue;
                }
                for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                    if (instruction instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode) instruction;
                        if ("org/spongepowered/asm/lib/tree/InsnList".equals(call.owner) && "iterator".equals(call.name)) {
                            assertEquals(search[1], "(I)Ljava/util/ListIterator;", call.desc);
                            indexed++;
                        }
                    }
                }
            }
            assertEquals(search[1], 1, indexed);
        }
    }

    private Object allocate(Class<?> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return unsafeClass.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
    }

    private void setField(Class<?> type, Object target, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
