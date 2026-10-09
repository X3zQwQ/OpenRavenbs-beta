package keystrokesmod.script.model;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import keystrokesmod.script.ScriptDefaults;
import keystrokesmod.utility.NetworkUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class Image {

    private static HashMap<String, BufferedImage> imageCache = new HashMap<>();

    public String url;

    public BufferedImage bufferedImage;

    public int height;

    public int width;

    public int textureId = -1;

    public boolean cached;

    public Image(String url, boolean cached) {
        this.url = url;
        this.cached = cached;
        BufferedImage cachedImage = cached ? imageCache.get(url) : null;
        if (cachedImage == null) {
            ScriptDefaults.client.async(() -> {
                BufferedImage newImage = NetworkUtils.getImageFromURL(url);
                if (newImage != null) {
                    this.bufferedImage = newImage;
                    this.height = newImage.getHeight();
                    this.width = newImage.getWidth();
                    if (cached) {
                        imageCache.put(url, newImage);
                    }
                }
            });
        } else {
            this.bufferedImage = cachedImage;
            this.height = cachedImage.getHeight();
            this.width = cachedImage.getWidth();
        }
    }

    public float[] getDimensions() {
        int scaleFactor = new ScaledResolution(Minecraft.func_71410_x()).func_78325_e();
        return new float[] { this.width / scaleFactor, this.height / scaleFactor };
    }

    public boolean isLoaded() {
        return this.bufferedImage != null;
    }

    public static void clearCache() {
        imageCache.clear();
    }

    @Override
    public String toString() {
        return "Image(" + this.height + "," + this.width + "," + this.url + ")";
    }
}
