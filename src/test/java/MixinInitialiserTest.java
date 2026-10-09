import static org.junit.Assert.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Deque;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.Test;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.MethodNode;

public class MixinInitialiserTest {

    @Test
    public void minecraftConstructorHasNoFieldInitialiser() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/keystrokesmod/mixin/impl/client/MixinMinecraft.class")) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            for (MethodNode method : node.methods) {
                if ("<init>".equals(method.name)) {
                    assertTrue(extract(method).isEmpty());
                }
            }
        }
    }

    @Test
    public void projectMixinConstructorsParse() throws Exception {
        int parsed = 0;
        try (JarFile jar = new JarFile(System.getProperty("project.jar"))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().startsWith("keystrokesmod/mixin/impl/") || !entry.getName().endsWith(".class")) {
                    continue;
                }
                ClassNode node = new ClassNode();
                try (InputStream input = jar.getInputStream(entry)) {
                    new ClassReader(input).accept(node, 0);
                }
                for (MethodNode method : node.methods) {
                    if ("<init>".equals(method.name)) {
                        Deque<AbstractInsnNode> initialiser = extract(method);
                        assertTrue(entry.getName(), initialiser == null || initialiser.isEmpty() || initialiser.peekLast().getOpcode() == 181);
                        parsed++;
                    }
                }
            }
        }
        assertTrue(parsed > 0);
        System.out.println("MIXIN_CONSTRUCTORS=" + parsed);
    }

    @SuppressWarnings("unchecked")
    private Deque<AbstractInsnNode> extract(MethodNode constructor) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        Method allocate = unsafeClass.getMethod("allocateInstance", Class.class);
        Class<?> applicatorClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinApplicatorStandard");
        Object applicator = allocate.invoke(unsafe, applicatorClass);
        Class<?> contextClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTargetContext");
        Method extract = applicatorClass.getDeclaredMethod("getInitialiser", contextClass, MethodNode.class);
        extract.setAccessible(true);
        return (Deque<AbstractInsnNode>) extract.invoke(applicator, null, constructor);
    }
}
