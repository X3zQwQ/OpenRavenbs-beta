package keystrokesmod.utility.font;

import java.awt.Desktop;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public final class FontManager {

    private static final String MINECRAFT = "Minecraft";

    private static final String RESOURCE_ROOT = "/assets/keystrokesmod/fonts/";

    private static final long MAX_CUSTOM_FONT_BYTES = 33554432L;

    private static final int MAX_CACHED_RENDERERS = 512;

    private static final float DEFAULT_HUD_FONT_SIZE = 10.0F;

    private static final float DEFAULT_CLICK_GUI_HEADER_HEIGHT = 9.0F;

    private static final float DEFAULT_CLICK_GUI_SETTING_HEIGHT = 9.0F;

    private static final float DEFAULT_NAMETAG_FONT_SIZE = 9.0F;

    private static final FontManager.FontDefinition[] BUNDLED_FONTS = new FontManager.FontDefinition[] { FontManager.FontDefinition.bundled("Sf-Bold", "Sf-Bold.ttf"), FontManager.FontDefinition.bundled("Sf-Regular", "Sf-Regular.ttf"), FontManager.FontDefinition.bundled("Sf-Ui", "Sf-Ui.ttf") };

    private static final File FONT_DIRECTORY = new File(new File(Minecraft.func_71410_x().field_71412_D, "keystrokes"), "fonts");

    private static final List<SliderSetting> FONT_SETTINGS = Collections.synchronizedList(new ArrayList<>());

    private static final Map<String, Font> BASE_FONT_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, RavenFontRenderer> FONT_CACHE = new LinkedHashMap<>(16, 0.75F, true);

    private static volatile Map<String, FontManager.FontDefinition> fontDefinitions = Collections.emptyMap();

    private static volatile String[] hudFontOptions = new String[] { "Minecraft" };

    private FontManager() {
    }

    public static String getDefaultFontName() {
        return "Minecraft";
    }

    public static File getFontDirectory() {
        ensureFontDirectory();
        return FONT_DIRECTORY;
    }

    public static boolean openFontDirectory() {
        ensureFontDirectory();
        try {
            if (!Desktop.isDesktopSupported()) {
                return false;
            } else {
                Desktop.getDesktop().open(FONT_DIRECTORY);
                return true;
            }
        } catch (Throwable exception) {
            return false;
        }
    }

    public static SliderSetting createFontSetting(String settingName) {
        SliderSetting setting = new SliderSetting(settingName, 0, getHudFontOptions());
        FONT_SETTINGS.add(setting);
        return setting;
    }

    public static String[] getHudFontOptions() {
        return (String[]) hudFontOptions.clone();
    }

    public static synchronized FontManager.ReloadResult reloadCustomFonts() {
        ensureFontDirectory();
        LinkedHashMap<String, FontManager.FontDefinition> rebuiltDefinitions = new LinkedHashMap<>();
        LinkedHashMap<String, Font> validatedCustomFonts = new LinkedHashMap<>();
        for (FontManager.FontDefinition bundledFont : BUNDLED_FONTS) {
            rebuiltDefinitions.put(bundledFont.displayName, bundledFont);
        }
        int loaded = 0;
        int failed = 0;
        File[] files = FONT_DIRECTORY.listFiles(file -> file.isFile() && isSupportedFontFile(file.getName()));
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File file : files) {
                if (file.length() > 0L && file.length() <= 33554432L) {
                    Font baseFont = loadCustomFont(file);
                    if (baseFont == null) {
                        failed++;
                    } else {
                        String displayName = makeCustomDisplayName(file, rebuiltDefinitions);
                        FontManager.FontDefinition definition = FontManager.FontDefinition.custom(displayName, file);
                        rebuiltDefinitions.put(displayName, definition);
                        validatedCustomFonts.put(definition.cacheKey, baseFont);
                        loaded++;
                    }
                } else {
                    failed++;
                }
            }
        }
        clearRendererCache();
        BASE_FONT_CACHE.clear();
        BASE_FONT_CACHE.putAll(validatedCustomFonts);
        fontDefinitions = Collections.unmodifiableMap(rebuiltDefinitions);
        String[] options = new String[rebuiltDefinitions.size() + 1];
        options[0] = "Minecraft";
        int intValue = 1;
        for (String displayName : rebuiltDefinitions.keySet()) {
            options[intValue++] = displayName;
        }
        hudFontOptions = options;
        synchronized (FONT_SETTINGS) {
            for (SliderSetting setting : FONT_SETTINGS) {
                setting.setOptions(options);
            }
        }
        return new FontManager.ReloadResult(loaded, failed);
    }

    public static RavenFontRenderer getHudRenderer(String family, float scale) {
        float safeScale = Math.max(0.5F, Math.min(2.0F, scale));
        return getRenderer(family, 10.0F * safeScale);
    }

    public static RavenFontRenderer getClickGuiHeaderRenderer(String family) {
        return getRendererForPixelHeight(family, 9.0F);
    }

    public static RavenFontRenderer getClickGuiSettingRenderer(String family) {
        return getRendererForPixelHeight(family, 9.0F);
    }

    public static RavenFontRenderer getNametagRenderer(String family) {
        return getRenderer(family, 9.0F);
    }

    private static RavenFontRenderer getRenderer(String family, float fontSize) {
        final float safeFontSize = Math.max(1.0F, fontSize);
        if (family != null && !isMinecraftFont(family)) {
            final FontManager.FontDefinition fontDefinition = fontDefinitions.get(family);
            if (fontDefinition == null) {
                return getMinecraftRenderer(safeFontSize);
            } else {
                String key = family + "#" + quantizeForCacheKey(safeFontSize) + "#" + getUiScale();
                return getCachedRenderer(key, new Supplier<RavenFontRenderer>() {

                    public RavenFontRenderer get() {
                        Font baseFont = FontManager.getBaseFont(fontDefinition);
                        return (RavenFontRenderer) (baseFont == null ? FontManager.getMinecraftRenderer(safeFontSize) : new GlyphFontRenderer(baseFont.deriveFont(safeFontSize), true));
                    }
                });
            }
        } else {
            return getMinecraftRenderer(safeFontSize);
        }
    }

    private static RavenFontRenderer getRendererForPixelHeight(String family, float targetHeight) {
        final float safeTargetHeight = Math.max(1.0F, targetHeight);
        if (family != null && !isMinecraftFont(family)) {
            final FontManager.FontDefinition fontDefinition = fontDefinitions.get(family);
            if (fontDefinition == null) {
                return getMinecraftRenderer(safeTargetHeight);
            } else {
                String key = family + "#height#" + quantizeForCacheKey(safeTargetHeight) + "#" + getUiScale();
                return getCachedRenderer(key, new Supplier<RavenFontRenderer>() {

                    public RavenFontRenderer get() {
                        Font baseFont = FontManager.getBaseFont(fontDefinition);
                        return baseFont == null ? FontManager.getMinecraftRenderer(safeTargetHeight) : FontManager.createHeightMatchedRenderer(baseFont, safeTargetHeight);
                    }
                });
            }
        } else {
            return getMinecraftRenderer(safeTargetHeight);
        }
    }

    private static RavenFontRenderer createHeightMatchedRenderer(Font baseFont, float targetHeight) {
        float derivedSize = targetHeight;
        GlyphFontRenderer renderer = new GlyphFontRenderer(baseFont.deriveFont(targetHeight), true);
        for (int i = 0; i < 2; i++) {
            float measuredHeight = Math.max(1.0F, (float) renderer.getFontHeight());
            float difference = Math.abs(measuredHeight - targetHeight);
            if (difference <= 0.5F) {
                break;
            }
            GlyphFontRenderer previousRenderer = renderer;
            derivedSize = Math.max(1.0F, derivedSize * (targetHeight / measuredHeight));
            renderer = new GlyphFontRenderer(baseFont.deriveFont(derivedSize), true);
            previousRenderer.destroy();
        }
        return renderer;
    }

    private static RavenFontRenderer getMinecraftRenderer(float fontSize) {
        float vanillaHeight = Math.max(1.0F, (float) Minecraft.func_71410_x().field_71466_p.field_78288_b);
        final float scale = Math.max(0.5F, Math.min(2.0F, fontSize / vanillaHeight));
        String key = "Minecraft#" + quantizeForCacheKey(scale);
        return getCachedRenderer(key, new Supplier<RavenFontRenderer>() {

            public RavenFontRenderer get() {
                return new MinecraftFontAdapter(Minecraft.func_71410_x().field_71466_p, scale);
            }
        });
    }

    public static boolean isMinecraftFont(String family) {
        return family == null || "Minecraft".equalsIgnoreCase(family);
    }

    private static Font getBaseFont(FontManager.FontDefinition definition) {
        Font cached = BASE_FONT_CACHE.get(definition.cacheKey);
        if (cached != null) {
            return cached;
        } else {
            Font loaded = definition.customFile == null ? loadBundledFont(definition.bundledFileName) : loadCustomFont(definition.customFile);
            if (loaded != null) {
                Font existing = BASE_FONT_CACHE.putIfAbsent(definition.cacheKey, loaded);
                return existing == null ? loaded : existing;
            } else {
                return null;
            }
        }
    }

    private static Font loadBundledFont(String fileName) {
        try (InputStream inputStream = FontManager.class.getResourceAsStream("/assets/keystrokesmod/fonts/" + fileName)) {
            return createFont(readFontData(inputStream));
        } catch (IOException exception2) {
            return null;
        }
    }

    private static Font loadCustomFont(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return createFont(readFontData(inputStream));
        } catch (IOException exception3) {
            return null;
        }
    }

    private static Font createFont(byte[] fontData) {
        if (fontData == null) {
            return null;
        } else {
            try {
                return Font.createFont(0, new ByteArrayInputStream(fontData));
            } catch (Exception exception4) {
                try {
                    return Font.createFont(1, new ByteArrayInputStream(fontData));
                } catch (Exception exception5) {
                    return null;
                }
            }
        }
    }

    private static byte[] readFontData(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        } else {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    private static String makeCustomDisplayName(File file, Map<String, FontManager.FontDefinition> definitions) {
        String fileName = file.getName();
        int intValue2 = fileName.lastIndexOf(46);
        String baseName = intValue2 > 0 ? fileName.substring(0, intValue2) : fileName;
        if (!definitions.containsKey(baseName)) {
            return baseName;
        } else {
            String displayName = fileName;
            int suffix = 2;
            while (definitions.containsKey(displayName)) {
                displayName = fileName + " (" + suffix++ + ")";
            }
            return displayName;
        }
    }

    private static boolean isSupportedFontFile(String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        return lowerName.endsWith(".ttf") || lowerName.endsWith(".otf");
    }

    private static void ensureFontDirectory() {
        if (!FONT_DIRECTORY.exists()) {
            FONT_DIRECTORY.mkdirs();
        }
    }

    private static int getUiScale() {
        try {
            return Math.max(1, new ScaledResolution(Minecraft.func_71410_x()).func_78325_e());
        } catch (Exception exception6) {
            return 1;
        }
    }

    private static float quantizeForCacheKey(float value) {
        return Math.round(value * 100.0F) / 100.0F;
    }

    private static synchronized RavenFontRenderer getCachedRenderer(String key, Supplier<RavenFontRenderer> rendererSupplier) {
        RavenFontRenderer renderer = FONT_CACHE.get(key);
        if (renderer != null) {
            return renderer;
        } else {
            renderer = rendererSupplier.get();
            FONT_CACHE.put(key, renderer);
            trimFontCache();
            return renderer;
        }
    }

    private static synchronized void clearRendererCache() {
        for (RavenFontRenderer renderer : FONT_CACHE.values()) {
            if (renderer != null) {
                renderer.destroy();
            }
        }
        FONT_CACHE.clear();
    }

    private static void trimFontCache() {
        while (FONT_CACHE.size() > 512) {
            Iterator<Entry<String, RavenFontRenderer>> iterator = FONT_CACHE.entrySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            Entry<String, RavenFontRenderer> eldestEntry = iterator.next();
            iterator.remove();
            if (eldestEntry.getValue() != null) {
                eldestEntry.getValue().destroy();
            }
        }
    }

    static {
        reloadCustomFonts();
    }

    private static final class FontDefinition {

        private final String displayName;

        private final String bundledFileName;

        private final File customFile;

        private final String cacheKey;

        private FontDefinition(String displayName, String bundledFileName, File customFile, String cacheKey) {
            this.displayName = displayName;
            this.bundledFileName = bundledFileName;
            this.customFile = customFile;
            this.cacheKey = cacheKey;
        }

        private static FontManager.FontDefinition bundled(String displayName, String fileName) {
            return new FontManager.FontDefinition(displayName, fileName, null, "bundled:" + fileName);
        }

        private static FontManager.FontDefinition custom(String displayName, File file) {
            return new FontManager.FontDefinition(displayName, null, file, "custom:" + file.getAbsolutePath());
        }
    }

    public static final class ReloadResult {

        private final int loaded;

        private final int failed;

        private ReloadResult(int loaded, int failed) {
            this.loaded = loaded;
            this.failed = failed;
        }

        public int getLoaded() {
            return this.loaded;
        }

        public int getFailed() {
            return this.failed;
        }
    }
}
