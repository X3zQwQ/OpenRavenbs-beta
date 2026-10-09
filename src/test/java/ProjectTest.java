import static org.junit.Assert.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import keystrokesmod.module.setting.impl.BlockListSetting;
import org.junit.Test;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.ClassVisitor;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class ProjectTest {

    @Test
    public void blockSettingsLoadProfiles() {
        BlockListSetting first = new BlockListSetting("Blocks");
        BlockListSetting second = new BlockListSetting(null, "Blocks");
        assertTrue(first.getBlocks().isEmpty());
        assertEquals("Blocks", second.getProfileKey());
        first.addBlock("minecraft:stone:*");
        assertTrue(first.contains("minecraft:stone:0"));
        JsonObject data = new JsonObject();
        JsonArray values = new JsonArray();
        values.add(new JsonPrimitive("minecraft:dirt:0"));
        data.add("Blocks", values);
        first.loadProfile(data);
        assertFalse(first.contains("minecraft:stone:0"));
        assertTrue(first.contains("minecraft:dirt:0"));
    }

    @Test
    public void callbackBoxesPrimitiveValues() {
        CallbackInfoReturnable<Integer> callback = new CallbackInfoReturnable<>("test", true, 7);
        assertEquals(7, callback.getReturnValueI());
    }

    @Test
    public void archiveContainsReadableClasses() throws Exception {
        int parsed = 0;
        try (JarFile jar = new JarFile(Paths.get(System.getProperty("project.jar")).toFile())) {
            java.util.Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    ClassReader reader = new ClassReader(input);
                    reader.accept(new ClassVisitor(Opcodes.ASM5) {
                    }, 0);
                    parsed++;
                }
            }
        }
        assertTrue(parsed > 1300);
        System.out.println("ARCHIVE_CLASSES=" + parsed);
    }
}
