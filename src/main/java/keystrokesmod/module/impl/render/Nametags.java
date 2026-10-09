package keystrokesmod.module.impl.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.font.FontManager;
import keystrokesmod.utility.font.RavenFontRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraftforge.client.event.RenderLivingEvent.Specials.Pre;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.opengl.GL11;

public class Nametags extends Module {

    private static final float AUTO_SCALE_THRESHOLD = 5.0F;

    private static final Comparator<Nametags.NametagRenderState> FAR_TO_NEAR = (a, b) -> Double.compare(b.distanceSq, a.distanceSq);

    private static final int ITEM_SPACING = 14;

    private static final int ENCHANT_LINE_HEIGHT = 8;

    private static final int ENCHANT_Y_OFFSET = 24;

    private SliderSetting scale;

    private SliderSetting font;

    private ButtonSetting autoScale;

    private ButtonSetting onlyRenderName;

    private SliderSetting background;

    private ButtonSetting bgBorder;

    private ButtonSetting showHealth;

    private ButtonSetting textShadow;

    private ButtonSetting showDistance;

    private ButtonSetting showInvis;

    private ButtonSetting showArmor;

    private ButtonSetting showEnchants;

    private ButtonSetting showStackSize;

    private ButtonSetting showDurability;

    private ButtonSetting renderSelf;

    private ButtonSetting hideVanilla;

    private ColorSetting friendColor;

    private ColorSetting enemyColor;

    private final List<Nametags.NametagRenderState> renderStates = new ArrayList<>();

    private int renderStateCount = 0;

    private static final int[] ARMOR_ENCHANT_IDS = new int[] { 0, 7, 34 };

    private static final String[] ARMOR_ENCHANT_ABBR = new String[] { "P", "T", "U" };

    private static final int[] SWORD_ENCHANT_IDS = new int[] { 16, 20, 19 };

    private static final String[] SWORD_ENCHANT_ABBR = new String[] { "S", "F", "K" };

    private static final int[] BOW_ENCHANT_IDS = new int[] { 48, 49, 50 };

    private static final String[] BOW_ENCHANT_ABBR = new String[] { "Pw", "Pu", "Fl" };

    private static final int[] TOOL_ENCHANT_IDS = new int[] { 32, 35, 34 };

    private static final String[] TOOL_ENCHANT_ABBR = new String[] { "E", "Fo", "U" };

    private static final int[] MISC_ENCHANT_IDS = new int[] { 19 };

    private static final String[] MISC_ENCHANT_ABBR = new String[] { "K" };

    public Nametags() {
        super("Nametags", Module.category.render, 0);
        this.liteModule = true;
        this.registerSetting(this.background = new SliderSetting("Background", true, 0.5, 0.0, 1.0, 0.05));
        this.registerSetting(this.scale = new SliderSetting("Scale", 1.0, 0.1, 2.0, 0.1));
        this.registerSetting(this.font = FontManager.createFontSetting("Font"));
        this.registerSetting(this.autoScale = new ButtonSetting("Auto-scale", false));
        this.registerSetting(this.onlyRenderName = new ButtonSetting("Only render name", false));
        this.registerSetting(this.renderSelf = new ButtonSetting("Render self", false));
        this.registerSetting(this.bgBorder = new ButtonSetting("Background border", false));
        this.registerSetting(this.showHealth = new ButtonSetting("Show health", false));
        this.registerSetting(this.textShadow = new ButtonSetting("Text shadow", false));
        this.registerSetting(this.hideVanilla = new ButtonSetting("Hide vanilla", true));
        this.registerSetting(this.showDistance = new ButtonSetting("Show distance", false));
        this.registerSetting(this.showInvis = new ButtonSetting("Show invis", true));
        this.registerSetting(this.friendColor = new ColorSetting("Friend color", 85, 255, 255));
        this.registerSetting(this.enemyColor = new ColorSetting("Enemy color", 255, 85, 85));
        this.registerSetting(new DescriptionSetting("Armor settings"));
        this.registerSetting(this.showArmor = new ButtonSetting("Show armor", false));
        this.registerSetting(this.showEnchants = new ButtonSetting("Show enchants", false));
        this.registerSetting(this.showDurability = new ButtonSetting("Show durability", false));
        this.registerSetting(this.showStackSize = new ButtonSetting("Show stack size", false));
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            if (Utils.nullCheck() && mc.field_71441_e != null) {
                this.updateRenderStates();
            } else {
                this.renderStateCount = 0;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (Utils.nullCheck()) {
            this.renderNametags(event.partialTicks);
        }
    }

    @SubscribeEvent
    public void onRenderLiving(Pre event) {
        if (this.hideVanilla.isToggled()) {
            if (event.entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) event.entity;
                if (this.shouldRenderNametag(player)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    private void updateRenderStates() {
        RavenFontRenderer fontRenderer = this.getNametagFontRenderer();
        Entity viewer = mc.func_175606_aa();
        if (viewer == null) {
            this.renderStateCount = 0;
        } else {
            boolean renderDistance = this.showDistance.isToggled();
            boolean renderArmor = this.showArmor.isToggled();
            float baseScale = this.computeBaseScaleValue();
            this.renderStateCount = 0;
            for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                if (this.shouldRenderNametag(player)) {
                    double dx = player.field_70165_t - viewer.field_70165_t;
                    double dy = player.field_70163_u - viewer.field_70163_u;
                    double dz = player.field_70161_v - viewer.field_70161_v;
                    double distanceSq = dx * dx + dy * dy + dz * dz;
                    float distance = (float) Math.sqrt(distanceSq);
                    String displayName = this.buildDisplayName(player, renderDistance, distance);
                    int stringHalfWidth = fontRenderer.getStringWidth(displayName) / 2;
                    int relationshipColor = this.resolveRelationshipColor(player);
                    int[] playerNameRange = this.findVisiblePlayerNameRange(displayName, player.func_70005_c_());
                    ItemStack heldItem = null;
                    ItemStack boots = null;
                    ItemStack leggings = null;
                    ItemStack chestplate = null;
                    ItemStack helmet = null;
                    int totalItems = 0;
                    if (renderArmor) {
                        heldItem = player.func_71124_b(0);
                        if (heldItem != null) {
                            totalItems++;
                        }
                        boots = player.func_71124_b(1);
                        if (boots != null) {
                            totalItems++;
                        }
                        leggings = player.func_71124_b(2);
                        if (leggings != null) {
                            totalItems++;
                        }
                        chestplate = player.func_71124_b(3);
                        if (chestplate != null) {
                            totalItems++;
                        }
                        helmet = player.func_71124_b(4);
                        if (helmet != null) {
                            totalItems++;
                        }
                    }
                    if (this.renderStateCount >= this.renderStates.size()) {
                        this.renderStates.add(new Nametags.NametagRenderState());
                    }
                    this.renderStates.get(this.renderStateCount++).set(player, displayName, stringHalfWidth, Utils.getColorFromEntity(player), relationshipColor, distanceSq, baseScale, (player.func_70093_af() ? player.field_70131_O - 0.3F : player.field_70131_O) + 0.3F, heldItem, boots, leggings, chestplate, helmet, totalItems);
                }
            }
            if (this.renderStateCount > 1) {
                this.renderStates.subList(0, this.renderStateCount).sort(FAR_TO_NEAR);
            }
        }
    }

    private void renderNametags(float partialTicks) {
        RenderManager renderManager = mc.func_175598_ae();
        FontRenderer itemFontRenderer = mc.field_71466_p;
        RavenFontRenderer textRenderer = this.getNametagFontRenderer();
        if (renderManager != null && itemFontRenderer != null && this.renderStateCount != 0) {
            ((IAccessorEntityRenderer) mc.field_71460_t).callSetupCameraTransform(partialTicks, 0);
            for (int i = 0; i < this.renderStateCount; i++) {
                Nametags.NametagRenderState renderState = this.renderStates.get(i);
                if (renderState.player != null && RenderUtils.isInViewFrustum(renderState.player)) {
                    this.renderCustomName(renderState, partialTicks, renderManager, textRenderer, itemFontRenderer);
                }
            }
            GlStateManager.func_179126_j();
            GlStateManager.func_179132_a(true);
            GlStateManager.func_179140_f();
            GlStateManager.func_179101_C();
            GlStateManager.func_179084_k();
            GlStateManager.func_179141_d();
            GlStateManager.func_179098_w();
            GlStateManager.func_179120_a(770, 771, 1, 0);
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private boolean shouldRenderNametag(EntityPlayer player) {
        if (player == null) {
            return false;
        } else if (player != mc.field_71439_g) {
            if (player.field_70128_L || player.field_70725_aQ > 0) {
                return false;
            } else {
                return !this.showInvis.isToggled() && player.func_82150_aj() ? false : !AntiBot.isBot(player);
            }
        } else {
            return this.renderSelf.isToggled() && mc.field_71474_y.field_74320_O != 0;
        }
    }

    private String buildDisplayName(EntityPlayer entity, boolean showDist, float distance) {
        String name;
        if (this.onlyRenderName.isToggled()) {
            String formatted = Utils.getFirstColorCode(entity.func_145748_c_().func_150254_d());
            String color = formatted.length() >= 2 && formatted.charAt(0) == 167 ? formatted : "";
            name = color + entity.func_70005_c_();
        } else {
            name = entity.func_145748_c_().func_150254_d();
        }
        if (this.showHealth.isToggled()) {
            name = this.appendHealth(name, entity);
        }
        if (showDist) {
            int dist = (int) distance;
            String distColor = dist <= 8 ? "§c" : (dist <= 15 ? "§6" : (dist <= 25 ? "§e" : "§7"));
            name = distColor + dist + "m§r " + name;
        }
        return name;
    }

    private int resolveRelationshipColor(EntityPlayer entity) {
        if (Utils.isFriended(entity)) {
            return this.friendColor.getColor();
        } else {
            return Utils.isEnemy(entity) ? this.enemyColor.getColor() : -1;
        }
    }

    private float computeBaseScaleValue() {
        return (float) this.scale.getInput() * 0.02F;
    }

    private float computeScaleValue(float distance, boolean scaleByDistance) {
        float scaleValue = this.computeBaseScaleValue();
        if (!scaleByDistance) {
            return scaleValue;
        } else {
            float effectiveDistance = Math.max(1.0F, distance);
            float scaledValue = scaleValue * (effectiveDistance / 5.0F);
            return Math.max(scaleValue, scaledValue);
        }
    }

    private void renderCustomName(Nametags.NametagRenderState state, float partialTicks, RenderManager renderManager, RavenFontRenderer textRenderer, FontRenderer itemFontRenderer) {
        EntityPlayer entity = state.player;
        if (entity != null && !entity.field_70128_L && entity.field_70725_aQ <= 0) {
            double x = entity.field_70142_S + (entity.field_70165_t - entity.field_70142_S) * partialTicks - renderManager.field_78730_l;
            double y = entity.field_70137_T + (entity.field_70163_u - entity.field_70137_T) * partialTicks - renderManager.field_78731_m;
            double z = entity.field_70136_U + (entity.field_70161_v - entity.field_70136_U) * partialTicks - renderManager.field_78728_n;
            float renderScale = state.baseScale;
            if (this.autoScale.isToggled()) {
                renderScale = this.computeScaleValue((float) Math.sqrt(x * x + y * y + z * z), true);
            }
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b((float) x, (float) y + state.yOffset, (float) z);
            GlStateManager.func_179114_b(-renderManager.field_78735_i, 0.0F, 1.0F, 0.0F);
            GlStateManager.func_179114_b(renderManager.field_78732_j, 1.0F, 0.0F, 0.0F);
            GlStateManager.func_179152_a(-renderScale, -renderScale, renderScale);
            GlStateManager.func_179140_f();
            GlStateManager.func_179132_a(false);
            GlStateManager.func_179097_i();
            GlStateManager.func_179147_l();
            GlStateManager.func_179120_a(770, 771, 1, 0);
            GlStateManager.func_179109_b(0.0F, -10.0F, 0.0F);
            if (this.background.getInput() > 0.01 || this.bgBorder.isToggled() || state.relationshipColor != -1) {
                this.renderBackground(state.stringHalfWidth, 0.0F, state.teamColor, state.relationshipColor, textRenderer);
                this.applyNametagTextState();
            }
            this.drawDisplayName(state, textRenderer);
            this.applyNametagTextState();
            if (state.totalItems > 0) {
                int iconX = -(state.totalItems * 14) / 2;
                int iconY = -20;
                if (state.heldItem != null) {
                    this.renderItemStack(state.heldItem, iconX, iconY, itemFontRenderer);
                    iconX += 14;
                }
                if (state.helmet != null) {
                    this.renderItemStack(state.helmet, iconX, iconY, itemFontRenderer);
                    iconX += 14;
                }
                if (state.chestplate != null) {
                    this.renderItemStack(state.chestplate, iconX, iconY, itemFontRenderer);
                    iconX += 14;
                }
                if (state.leggings != null) {
                    this.renderItemStack(state.leggings, iconX, iconY, itemFontRenderer);
                    iconX += 14;
                }
                if (state.boots != null) {
                    this.renderItemStack(state.boots, iconX, iconY, itemFontRenderer);
                }
            }
            GlStateManager.func_179126_j();
            GlStateManager.func_179132_a(true);
            GlStateManager.func_179145_e();
            GlStateManager.func_179084_k();
            GlStateManager.func_179120_a(770, 771, 1, 0);
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.func_179121_F();
        }
    }

    private void applyNametagTextState() {
        GlStateManager.func_179140_f();
        GlStateManager.func_179097_i();
        GlStateManager.func_179132_a(false);
        GlStateManager.func_179098_w();
        GlStateManager.func_179141_d();
        GlStateManager.func_179092_a(516, 0.1F);
        GlStateManager.func_179147_l();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GL11.glTexEnvi(8960, 8704, 8448);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawDisplayName(Nametags.NametagRenderState state, RavenFontRenderer textRenderer) {
        textRenderer.drawString(state.displayName, -state.stringHalfWidth, 0.0F, -1, this.textShadow.isToggled());
    }

    private void renderBackground(int stringWidth, float textY, int teamColor, int relationshipColor, RavenFontRenderer fontRenderer) {
        GlStateManager.func_179090_x();
        Tessellator tessellator = Tessellator.func_178181_a();
        WorldRenderer worldRenderer = tessellator.func_178180_c();
        float alpha = (float) this.background.getInput();
        float innerLeft = -stringWidth - 3.0F;
        float innerRight = stringWidth + 3.0F;
        float innerTop = textY + fontRenderer.getTextTopOffset() - 3.0F;
        float innerBottom = textY + fontRenderer.getTextBottomOffset() + 2.0F;
        boolean renderBaseFill = alpha > 0.01F;
        if (renderBaseFill) {
            worldRenderer.func_181668_a(7, DefaultVertexFormats.field_181706_f);
            worldRenderer.func_181662_b(innerLeft, innerTop, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, alpha).func_181675_d();
            worldRenderer.func_181662_b(innerLeft, innerBottom, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, alpha).func_181675_d();
            worldRenderer.func_181662_b(innerRight, innerBottom, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, alpha).func_181675_d();
            worldRenderer.func_181662_b(innerRight, innerTop, 0.0).func_181666_a(0.0F, 0.0F, 0.0F, alpha).func_181675_d();
            tessellator.func_78381_a();
        }
        int borderColor = relationshipColor != -1 ? relationshipColor : teamColor;
        if (this.bgBorder.isToggled() || relationshipColor != -1) {
            float red;
            float green;
            float blue;
            if (borderColor != -1) {
                red = (borderColor >> 16 & 0xFF) / 255.0F;
                green = (borderColor >> 8 & 0xFF) / 255.0F;
                blue = (borderColor & 0xFF) / 255.0F;
            } else {
                red = 0.6F;
                green = 0.6F;
                blue = 0.6F;
            }
            float borderThickness = 1.0F;
            float borderAlpha = relationshipColor != -1 ? alpha : 1.0F;
            float left = innerLeft - borderThickness;
            float right = innerRight + borderThickness;
            float top = innerTop - borderThickness;
            float bottom = innerBottom + borderThickness;
            float borderZ = -0.001F;
            worldRenderer.func_181668_a(7, DefaultVertexFormats.field_181706_f);
            worldRenderer.func_181662_b(left, top, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(left, innerTop, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(right, innerTop, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(right, top, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(left, innerBottom, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(left, bottom, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(right, bottom, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(right, innerBottom, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(left, innerTop, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(left, innerBottom, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(innerLeft, innerBottom, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(innerLeft, innerTop, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(innerRight, innerTop, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(innerRight, innerBottom, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(right, innerBottom, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            worldRenderer.func_181662_b(right, innerTop, borderZ).func_181666_a(red, green, blue, borderAlpha).func_181675_d();
            tessellator.func_78381_a();
        }
        GlStateManager.func_179098_w();
    }

    private int[] findVisiblePlayerNameRange(String formattedText, String playerName) {
        String strippedText = this.stripFormattingCodes(formattedText);
        int nameStart = strippedText.indexOf(playerName);
        return nameStart < 0 ? new int[] { -1, -1 } : new int[] { nameStart, nameStart + playerName.length() };
    }

    private String stripFormattingCodes(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == 167 && i + 1 < text.length()) {
                i++;
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private String getSelectedFontName() {
        if (this.font == null) {
            return FontManager.getDefaultFontName();
        } else {
            int index = (int) Math.max(0.0, Math.min((double) (this.font.getOptions().length - 1), this.font.getInput()));
            return this.font.getOptions()[index];
        }
    }

    private RavenFontRenderer getNametagFontRenderer() {
        return FontManager.getNametagRenderer(this.getSelectedFontName());
    }

    private String appendHealth(String name, EntityPlayer entity) {
        float health = Math.max(0.0F, entity.func_110143_aJ());
        float maxHealth = entity.func_110138_aP();
        if (maxHealth <= 0.0F) {
            maxHealth = 20.0F;
        }
        boolean heartsMode = Settings.showHealthAsHearts.isToggled();
        double ratio = health / maxHealth;
        String color = ratio < 0.3 ? "§c" : (ratio < 0.5 ? "§6" : (ratio < 0.7 ? "§e" : "§a"));
        float displayValue = heartsMode ? health / 2.0F : health;
        String valueStr = this.fastOneDecimal(displayValue);
        String text2 = Settings.showHeartSymbol.isToggled() ? " ❤" : "";
        name = name + " " + color + valueStr + text2;
        float absorption = entity.func_110139_bj();
        if (absorption > 0.0F) {
            float absDisplay = heartsMode ? absorption / 2.0F : absorption;
            String absStr = this.fastOneDecimal(absDisplay);
            String text3 = Settings.showHeartSymbol.isToggled() ? " ❤" : "";
            name = name + " §6+" + absStr + text3;
        }
        return name + "§r";
    }

    private String fastOneDecimal(float value) {
        int whole = (int) value;
        if (value == whole) {
            return String.valueOf(whole);
        } else {
            int tenths = Math.round(value * 10.0F);
            int intPart = tenths / 10;
            int fracPart = Math.abs(tenths % 10);
            return intPart + "." + fracPart;
        }
    }

    private void renderItemStack(ItemStack stack, int xPos, int yPos, FontRenderer fontRenderer) {
        if (stack != null) {
            RenderUtils.renderItemAndEffectIntoGui3D(stack, xPos, yPos);
            if (this.showEnchants.isToggled()) {
                GlStateManager.func_179094_E();
                GlStateManager.func_179139_a(0.5, 0.5, 0.5);
                GlStateManager.func_179109_b(0.0F, -10.0F, 0.0F);
                this.renderEnchantText(stack, xPos, yPos, fontRenderer);
                GlStateManager.func_179121_F();
            }
            GlStateManager.func_179097_i();
            if (stack.field_77994_a > 1) {
                String countStr = String.valueOf(stack.field_77994_a);
                fontRenderer.func_175063_a(countStr, xPos + 17 - fontRenderer.func_78256_a(countStr), yPos + 9, 16777215);
            }
            if (this.showDurability.isToggled() && stack.func_77984_f() && stack.func_77952_i() > 0) {
                int maxDamage = stack.func_77958_k();
                int currentDamage = stack.func_77952_i();
                float durabilityRatio = 1.0F - (float) currentDamage / maxDamage;
                RenderUtils.drawDurabilityBar(xPos, yPos, durabilityRatio);
            }
            GlStateManager.func_179126_j();
        }
    }

    private void renderEnchantText(ItemStack stack, int xPos, int yPos, FontRenderer fontRenderer) {
        Item item = stack.func_77973_b();
        int[] ids;
        String[] abbreviations;
        if (item instanceof ItemArmor) {
            ids = ARMOR_ENCHANT_IDS;
            abbreviations = ARMOR_ENCHANT_ABBR;
        } else if (item instanceof ItemSword) {
            ids = SWORD_ENCHANT_IDS;
            abbreviations = SWORD_ENCHANT_ABBR;
        } else if (item instanceof ItemBow) {
            ids = BOW_ENCHANT_IDS;
            abbreviations = BOW_ENCHANT_ABBR;
        } else if (item instanceof ItemTool) {
            ids = TOOL_ENCHANT_IDS;
            abbreviations = TOOL_ENCHANT_ABBR;
        } else {
            ids = MISC_ENCHANT_IDS;
            abbreviations = MISC_ENCHANT_ABBR;
        }
        int drawX = xPos * 2;
        int drawY = yPos - 24;
        for (int i = 0; i < ids.length; i++) {
            int level = EnchantmentHelper.func_77506_a(ids[i], stack);
            if (level > 0) {
                this.drawEnchantLine(fontRenderer, abbreviations[i], level, drawX, drawY);
                drawY += 8;
            }
        }
    }

    private void drawEnchantLine(FontRenderer fontRenderer, String abbreviation, int level, int x, int y) {
        fontRenderer.func_175063_a(abbreviation, x, y, 16777215);
        int advance = fontRenderer.func_78256_a(abbreviation);
        fontRenderer.func_175063_a(String.valueOf(level), x + advance, y, this.colorForEnchantLevel(level));
    }

    private int colorForEnchantLevel(int level) {
        if (level <= 5) {
            if (level == 1) {
                return 16777215;
            }
            if (level == 2) {
                return 5636095;
            }
            if (level == 3) {
                return 43690;
            }
            if (level == 4) {
                return 11141290;
            }
            if (level == 5) {
                return 16755200;
            }
        }
        return 16733695;
    }

    private static class NametagRenderState {

        private EntityPlayer player;

        private String displayName;

        private int stringHalfWidth;

        private int teamColor;

        private int relationshipColor;

        private double distanceSq;

        private float baseScale;

        private float yOffset;

        private ItemStack heldItem;

        private ItemStack boots;

        private ItemStack leggings;

        private ItemStack chestplate;

        private ItemStack helmet;

        private int totalItems;

        private NametagRenderState() {
        }

        private void set(EntityPlayer player, String displayName, int stringHalfWidth, int teamColor, int relationshipColor, double distanceSq, float baseScale, float yOffset, ItemStack heldItem, ItemStack boots, ItemStack leggings, ItemStack chestplate, ItemStack helmet, int totalItems) {
            this.player = player;
            this.displayName = displayName;
            this.stringHalfWidth = stringHalfWidth;
            this.teamColor = teamColor;
            this.relationshipColor = relationshipColor;
            this.distanceSq = distanceSq;
            this.baseScale = baseScale;
            this.yOffset = yOffset;
            this.heldItem = heldItem;
            this.boots = boots;
            this.leggings = leggings;
            this.chestplate = chestplate;
            this.helmet = helmet;
            this.totalItems = totalItems;
        }
    }
}
