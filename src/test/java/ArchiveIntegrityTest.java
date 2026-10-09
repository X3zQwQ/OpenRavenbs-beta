import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

public class ArchiveIntegrityTest {

    @Test
    public void everyMethodPassesBasicDataflowVerification() throws Exception {
        int verified = 0;
        try (JarFile jar = new JarFile(System.getProperty("project.jar"))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                ClassNode node = readClass(jar, entry);
                assertTrue(entry.getName(), node.version <= Opcodes.V1_8);
                for (Object value : node.methods) {
                    MethodNode method = (MethodNode) value;
                    if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                        new Analyzer(new BasicVerifier()).analyze(node.name, method);
                        verified++;
                    }
                }
            }
        }
        assertTrue(verified > 11000);
        System.out.println("VERIFIED_METHODS=" + verified);
    }

    @Test
    public void mixinConfigurationReferencesBundledClasses() throws Exception {
        try (JarFile jar = new JarFile(System.getProperty("project.jar"))) {
            JsonObject config;
            try (InputStream input = jar.getInputStream(jar.getJarEntry("mixins.raven.json"))) {
                config = new JsonParser().parse(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            assertEquals("0.7.11", config.get("minVersion").getAsString());
            assertEquals("JAVA_8", config.get("compatibilityLevel").getAsString());
            int count = 0;
            for (com.google.gson.JsonElement mixin : config.getAsJsonArray("mixins")) {
                String name = (config.get("package").getAsString() + "." + mixin.getAsString()).replace('.', '/') + ".class";
                assertNotNull(name, jar.getJarEntry(name));
                count++;
            }
            assertTrue(count >= 50);
            try (InputStream input = jar.getInputStream(jar.getJarEntry(config.get("refmap").getAsString()))) {
                assertTrue(new JsonParser().parse(new InputStreamReader(input, StandardCharsets.UTF_8)).isJsonObject());
            }
            assertEquals("org.spongepowered.asm.launch.MixinTweaker", jar.getManifest().getMainAttributes().getValue("TweakClass"));
            assertEquals("mixins.raven.json", jar.getManifest().getMainAttributes().getValue("MixinConfigs"));
            System.out.println("CONFIGURED_MIXINS=" + count);
        }
    }

    @Test
    public void rotationQuantisationKeepsDoubleArithmetic() throws Exception {
        try (JarFile jar = new JarFile(System.getProperty("project.jar"))) {
            ClassNode node = readClass(jar, jar.getJarEntry("keystrokesmod/module/impl/player/Scaffold.class"));
            int remainders = 0;
            for (Object value : node.methods) {
                MethodNode method = (MethodNode) value;
                if (!method.name.equals("quantizeRotation")) {
                    continue;
                }
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    assertNotEquals(Opcodes.FREM, insn.getOpcode());
                    if (insn.getOpcode() == Opcodes.DREM) {
                        remainders++;
                    }
                }
            }
            assertEquals(1, remainders);
        }
    }

    @Test
    public void refinementLoopKeepsOriginalStep() throws Exception {
        try (JarFile jar = new JarFile(System.getProperty("project.jar"))) {
            ClassNode node = readClass(jar, jar.getJarEntry("keystrokesmod/module/impl/combat/Displace.class"));
            int stepAdditions = 0;
            for (Object value : node.methods) {
                MethodNode method = (MethodNode) value;
                if (!method.name.equals("findBestVoidYaw")) {
                    continue;
                }
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof LdcInsnNode && Double.valueOf(1.5).equals(((LdcInsnNode) insn).cst)) {
                        AbstractInsnNode next = insn.getNext();
                        while (next != null && next.getOpcode() < 0) {
                            next = next.getNext();
                        }
                        if (next != null && next.getOpcode() == Opcodes.DADD) {
                            stepAdditions++;
                        }
                    }
                }
            }
            assertEquals(2, stepAdditions);
        }
    }

    private ClassNode readClass(JarFile jar, JarEntry entry) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = jar.getInputStream(entry)) {
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }
}
