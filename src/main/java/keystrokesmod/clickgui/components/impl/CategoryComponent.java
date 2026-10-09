package keystrokesmod.clickgui.components.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import keystrokesmod.Raven;
import keystrokesmod.clickgui.animation.ScrollOffsetAnimation;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.font.RavenFontRenderer;
import keystrokesmod.utility.profile.Manager;
import keystrokesmod.utility.profile.Profile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class CategoryComponent {

    private static long interactionSequence;

    private static final Map<Module.category, CategoryComponent.CategoryIconStacks> CATEGORY_ICON_STACKS = buildCategoryIconStacks();

    private static final Comparator<Module> MODULE_NAME_COMPARATOR = Comparator.comparingInt(CategoryComponent::getModuleNameGroup).thenComparing(Module::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(Module::getName);

    public List<ModuleComponent> modules = new CopyOnWriteArrayList<>();

    public Module.category category;

    public boolean opened;

    public float width;

    public float y;

    public float x;

    public float titleHeight;

    public boolean dragging;

    public float xx;

    public float yy;

    public boolean hovering = false;

    public boolean hoveringOverCategory = false;

    public Timer smoothTimer;

    private Timer textTimer;

    public float big;

    private static final int TRANSLUCENT_BACKGROUND = new Color(0, 0, 0, 110).getRGB();

    private static final int REGULAR_OUTLINE = new Color(81, 99, 149).getRGB();

    private static final int REGULAR_OUTLINE2 = new Color(97, 67, 133).getRGB();

    private static final int CATEGORY_NAME_COLOR = new Color(220, 220, 220).getRGB();

    private float lastHeight;

    private float lastNamePos;

    private float animationStartNamePos;

    public float moduleY;

    private float screenHeight;

    private float screenWidth;

    private float animationStartHeight;

    private final ScrollOffsetAnimation scrollAnim = new ScrollOffsetAnimation(200L);

    public long lastInteractedTime = 0L;

    public CategoryComponent(Module.category category) {
        this.category = category;
        this.width = 92.0F;
        this.x = 5.0F;
        this.moduleY = this.y = 5.0F;
        this.titleHeight = 13.0F;
        float moduleRenderY = this.titleHeight + 3.0F;
        this.scrollAnim.reset(this.moduleY);
        this.lastHeight = this.y + this.titleHeight + 4.0F;
        this.animationStartHeight = this.lastHeight;
        for (Module mod : sortModules(Raven.getModuleManager().inCategory(this.category))) {
            if (Gui.shouldShowModule(mod)) {
                ModuleComponent b = new ModuleComponent(mod, this, moduleRenderY);
                this.modules.add(b);
                moduleRenderY += 16.0F;
            }
        }
    }

    public List<ModuleComponent> getModules() {
        return this.modules;
    }

    public void reloadModules() {
        Map<String, Boolean> openStates = this.captureModuleOpenStates();
        this.modules.clear();
        this.titleHeight = 13.0F;
        float moduleRenderY = this.titleHeight + 3.0F;
        for (Module mod : sortModules(Raven.getModuleManager().inCategory(this.category))) {
            if (Gui.shouldShowModule(mod)) {
                ModuleComponent component = new ModuleComponent(mod, this, moduleRenderY);
                component.restoreOpenState(Boolean.TRUE.equals(openStates.get(mod.getName())));
                this.modules.add(component);
                moduleRenderY += 16.0F;
            }
        }
        this.syncAfterModuleReload();
    }

    public void reloadModules(boolean isProfile) {
        Map<String, Boolean> openStates = this.captureModuleOpenStates();
        this.modules.clear();
        this.titleHeight = 13.0F;
        float moduleRenderY = this.titleHeight + 3.0F;
        if (this.category == Module.category.profiles && isProfile || this.category == Module.category.scripts && !isProfile) {
            ModuleComponent manager = new ModuleComponent((Module) (isProfile ? new Manager() : new keystrokesmod.script.Manager()), this, moduleRenderY);
            manager.restoreOpenState(Boolean.TRUE.equals(openStates.get(manager.mod.getName())));
            this.modules.add(manager);
            if (Raven.profileManager == null && isProfile || Raven.scriptManager == null && !isProfile) {
                return;
            }
            if (isProfile) {
                List<Module> profileModules = new ArrayList<>();
                for (Profile profile : Raven.profileManager.profiles) {
                    profileModules.add(profile.getModule());
                }
                for (Module module : sortModules(profileModules)) {
                    moduleRenderY += 16.0F;
                    ModuleComponent b = new ModuleComponent(module, this, moduleRenderY);
                    b.restoreOpenState(Boolean.TRUE.equals(openStates.get(module.getName())));
                    this.modules.add(b);
                }
            } else {
                Collection<Module> modulesCollection = Raven.scriptManager.scripts.values();
                for (Module module : sortModules(modulesCollection)) {
                    moduleRenderY += 16.0F;
                    ModuleComponent b = new ModuleComponent(module, this, moduleRenderY);
                    b.restoreOpenState(Boolean.TRUE.equals(openStates.get(module.getName())));
                    this.modules.add(b);
                }
            }
        }
        this.syncAfterModuleReload();
    }

    private static List<Module> sortModules(Collection<Module> modules) {
        List<Module> sortedModules = new ArrayList<>(modules);
        sortedModules.sort(MODULE_NAME_COMPARATOR);
        return sortedModules;
    }

    private static int getModuleNameGroup(Module module) {
        String name = module.getName();
        if (name != null && !name.isEmpty()) {
            char firstCharacter = name.charAt(0);
            if (Character.isLetter(firstCharacter)) {
                return 0;
            } else {
                return Character.isDigit(firstCharacter) ? 1 : 2;
            }
        } else {
            return 2;
        }
    }

    private Map<String, Boolean> captureModuleOpenStates() {
        Map<String, Boolean> openStates = new HashMap<>();
        for (ModuleComponent moduleComponent : this.modules) {
            if (moduleComponent.mod != null) {
                openStates.put(moduleComponent.mod.getName(), moduleComponent.isOpened);
            }
        }
        return openStates;
    }

    private void syncAfterModuleReload() {
        CategoryComponent.CategoryLayoutMetrics layoutMetrics = this.computeLayoutMetrics(this.opened || this.smoothTimer != null);
        float minScrollY = layoutMetrics.minScrollY;
        float maxScrollY = this.y;
        float clampedScroll = Math.max(minScrollY, Math.min(maxScrollY, this.scrollAnim.getTarget()));
        this.moduleY = clampedScroll;
        this.scrollAnim.reset(clampedScroll);
        if (this.opened && !this.modules.isEmpty()) {
            this.big = layoutMetrics.visibleHeight;
            this.lastHeight = layoutMetrics.contentBottom;
        } else {
            if (!this.opened && this.smoothTimer == null) {
                this.big = 0.0F;
            }
            this.lastHeight = this.y + this.titleHeight + 4.0F;
        }
    }

    public void setX(float newX, boolean limit) {
        if (limit) {
            newX = Math.max(newX, 2.0F);
            newX = Math.min(newX, this.screenWidth - this.width - 4.0F);
        }
        this.x = newX;
    }

    public void setY(float y, boolean limit) {
        if (limit) {
            y = Math.max(y, 1.0F);
            float maxY = this.screenHeight - this.titleHeight - 5.0F;
            y = Math.min(y, maxY);
        }
        float scrollOffset = this.scrollAnim.getTarget() - this.y;
        this.y = y;
        float newTarget = y + scrollOffset;
        this.moduleY = newTarget;
        this.scrollAnim.reset(newTarget);
    }

    public void overTitle(boolean d) {
        this.dragging = d;
    }

    public boolean isOpened() {
        return this.opened;
    }

    public void markInteracted() {
        this.lastInteractedTime = ++interactionSequence;
    }

    public void mouseClicked(boolean on) {
        this.animationStartHeight = this.getCurrentAnimatedCategoryHeight();
        this.animationStartNamePos = this.getCurrentAnimatedNamePos();
        float animationDuration = 250.0F;
        this.opened = on;
        (this.smoothTimer = new Timer(animationDuration)).start();
        (this.textTimer = new Timer(animationDuration)).start();
    }

    public void onScroll(int mouseScrollInput) {
        this.onScroll(mouseScrollInput, Float.NaN, Float.NaN);
    }

    public void onScroll(int mouseScrollInput, float mouseX, float mouseY) {
        for (ModuleComponent mod : this.modules) {
            mod.onScroll(mouseScrollInput);
        }
        if (this.hoveringOverCategory && this.opened) {
            if (!Float.isNaN(mouseX) && !Float.isNaN(mouseY)) {
                for (ModuleComponent mod : this.modules) {
                    for (Component comp : mod.settings) {
                        if (mod.isOpened && mod.isVisible(comp)) {
                            if (comp instanceof AbstractSearchListComponent) {
                                AbstractSearchListComponent searchListComponent = (AbstractSearchListComponent) comp;
                                if (searchListComponent.capturesCategoryScroll(mouseX, mouseY)) {
                                    return;
                                }
                            } else if (comp instanceof PlayerListComponent) {
                                PlayerListComponent plc = (PlayerListComponent) comp;
                                if (plc.capturesCategoryScroll(mouseX, mouseY)) {
                                    return;
                                }
                            } else if (comp instanceof StringListComponent) {
                                StringListComponent slc = (StringListComponent) comp;
                                if (slc.capturesCategoryScroll(mouseX, mouseY)) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            this.markInteracted();
            float scrollSpeed = (float) Gui.scrollSpeed.getInput();
            float minScrollY = this.computeMinScrollY();
            float maxScrollY = this.y;
            float delta = scrollSpeed * (mouseScrollInput / 120.0F);
            if (delta != 0.0F) {
                this.scrollAnim.extend(delta);
            }
            this.scrollAnim.clampTarget(minScrollY, maxScrollY);
        }
    }

    private float getTotalScrollExtentHeightF() {
        float total = 0.0F;
        for (ModuleComponent c : this.modules) {
            total += c.getScrollExtentHeightF();
        }
        return total;
    }

    private float computeMinScrollY() {
        return this.computeLayoutMetrics(false).minScrollY;
    }

    public void render(FontRenderer renderer) {
        this.width = 92.0F;
        RavenFontRenderer titleRenderer = Gui.getClickGuiHeaderFontRenderer();
        if (this.smoothTimer != null && System.currentTimeMillis() - this.smoothTimer.last >= 280L) {
            this.smoothTimer = null;
        }
        if (this.textTimer != null && System.currentTimeMillis() - this.textTimer.last >= 280L) {
            this.textTimer = null;
        }
        for (ModuleComponent c : this.modules) {
            c.updateAnimationState();
        }
        CategoryComponent.CategoryLayoutMetrics layoutMetrics = this.computeLayoutMetrics(this.opened || this.smoothTimer != null);
        this.big = !this.opened && this.smoothTimer == null ? 0.0F : layoutMetrics.visibleHeight;
        float maxScrollY = this.y;
        float minScrollY = layoutMetrics.minScrollY;
        this.scrollAnim.clampTarget(minScrollY, maxScrollY);
        this.moduleY = this.scrollAnim.getValue();
        this.moduleY = Math.max(minScrollY, Math.min(maxScrollY, this.moduleY));
        float middlePos = this.x + this.width / 2.0F - titleRenderer.getStringWidth(this.category.name()) / 2.0F;
        float contentBottom = layoutMetrics.contentBottom;
        float extra;
        if (this.smoothTimer != null) {
            float targetHeight = this.opened ? contentBottom : this.y + this.titleHeight + 4.0F;
            extra = this.smoothTimer.getValueFloat(this.animationStartHeight, targetHeight, 1);
            if (this.opened && extra > targetHeight || !this.opened && extra < targetHeight) {
                extra = targetHeight;
            }
        } else {
            extra = contentBottom;
        }
        float targetNamePos = this.opened ? middlePos : this.x + 12.0F;
        float namePos;
        if (this.textTimer == null) {
            namePos = targetNamePos;
        } else {
            namePos = this.textTimer.getValueFloat(this.animationStartNamePos, targetNamePos, 1);
        }
        this.lastNamePos = namePos;
        this.lastHeight = extra;
        GL11.glPushMatrix();
        RenderUtils.drawRoundedGradientOutlinedRectangle(this.x - 2.0F, this.y, this.x + this.width + 2.0F, extra, 10.0F, TRANSLUCENT_BACKGROUND, (this.opened || this.hovering) && Gui.rainBowOutlines.isToggled() ? RenderUtils.setAlpha(Utils.getChroma(2L, 0L), 0.5) : REGULAR_OUTLINE, (this.opened || this.hovering) && Gui.rainBowOutlines.isToggled() ? RenderUtils.setAlpha(Utils.getChroma(2L, 700L), 0.5) : REGULAR_OUTLINE2);
        this.renderItemForCategory(this.category, (int) (this.x + 1.0F), (int) (this.y + 4.0F), this.opened || this.hovering);
        titleRenderer.drawString(this.category.name(), namePos, this.y + 4.0F, CATEGORY_NAME_COLOR, false);
        float moduleAreaTop = this.y + this.titleHeight + 3.0F;
        float scissorBottom = extra - 1.0F;
        float moduleAreaHeight = Math.max(0.0F, scissorBottom - moduleAreaTop);
        if (this.opened || this.smoothTimer != null) {
            GL11.glEnable(3089);
            RenderUtils.scissor(0.0, moduleAreaTop, this.x + this.width + 4.0F, moduleAreaHeight);
            float scrollOffset = this.moduleY - this.y;
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, scrollOffset, 0.0F);
            for (Component c2 : this.modules) {
                c2.render();
            }
            GL11.glPopMatrix();
            GL11.glDisable(3089);
        }
        GL11.glPopMatrix();
    }

    public void updateHeight() {
        float y = this.titleHeight + 3.0F;
        for (Component component : this.modules) {
            component.updateHeight(y);
            y += component.getHeightF();
        }
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getModuleY() {
        return this.moduleY;
    }

    public float getWidth() {
        return this.width;
    }

    public void mousePosition(int mouseX, int mouseY, boolean isTopmostUnderCursor) {
        if (this.dragging) {
            float newX = mouseX - this.xx;
            float newY = mouseY - this.yy;
            newX = Math.max(newX, 2.0F);
            newX = Math.min(newX, this.screenWidth - this.width - 4.0F);
            newY = Math.max(newY, 1.0F);
            int maxY = (int) (this.screenHeight - this.titleHeight - 5.0F);
            newY = Math.min(newY, (float) maxY);
            this.setX(newX, false);
            this.setY(newY, false);
        }
        this.hoveringOverCategory = isTopmostUnderCursor && this.overCategory(mouseX, mouseY);
        this.hovering = isTopmostUnderCursor && this.overTitle(mouseX, mouseY);
    }

    public boolean overTitle(int x, int y) {
        return x >= this.x && x <= this.x + this.width && y >= this.y + 2.0F && y <= this.y + this.titleHeight + 1.0F;
    }

    public boolean overCategory(int x, int y) {
        return x >= this.x - 2.0F && x <= this.x + this.width + 2.0F && y >= this.y + 2.0F && y <= this.y + this.titleHeight + this.big + 1.0F;
    }

    public boolean draggable(int x, int y) {
        return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.titleHeight;
    }

    public boolean overRect(int x, int y) {
        return x >= this.x - 2.0F && x <= this.x + this.width + 2.0F && y >= this.y && y <= this.lastHeight;
    }

    private void renderItemForCategory(Module.category category, int x, int y, boolean enchant) {
        RenderItem renderItem = Minecraft.func_71410_x().func_175599_af();
        double scale = 0.55;
        GlStateManager.func_179094_E();
        GlStateManager.func_179139_a(scale, scale, scale);
        CategoryComponent.CategoryIconStacks iconStacks = CATEGORY_ICON_STACKS.get(category);
        ItemStack itemStack = iconStacks == null ? null : (enchant ? iconStacks.activeStack : iconStacks.normalStack);
        if (itemStack != null) {
            RenderHelper.func_74520_c();
            GlStateManager.func_179084_k();
            GlStateManager.func_179109_b((float) (x / scale), (float) (y / scale), 0.0F);
            renderItem.func_180450_b(itemStack, 0, 0);
            GlStateManager.func_179147_l();
            RenderHelper.func_74518_a();
        }
        GlStateManager.func_179152_a(1.0F, 1.0F, 1.0F);
        GlStateManager.func_179121_F();
    }

    private float getCurrentCategoryBottomFromContent() {
        if (this.modules.isEmpty() || !this.opened && this.smoothTimer == null) {
            return this.y + this.titleHeight + 4.0F;
        } else {
            float maxBottom = this.y + this.screenHeight * 0.9F;
            return Math.min(this.y + this.titleHeight + this.big + 4.0F, maxBottom);
        }
    }

    private float getCurrentAnimatedNamePos() {
        if (this.textTimer != null) {
            return this.lastNamePos;
        } else {
            float middlePos = this.x + this.width / 2.0F - Gui.getClickGuiHeaderFontRenderer().getStringWidth(this.category.name()) / 2.0F;
            return this.opened ? middlePos : this.x + 12.0F;
        }
    }

    private float getCurrentAnimatedCategoryHeight() {
        if (this.lastHeight > 0.0F) {
            return this.lastHeight;
        } else if (!this.modules.isEmpty() && (this.opened || this.smoothTimer != null)) {
            float modulesHeight = 0.0F;
            for (ModuleComponent c : this.modules) {
                modulesHeight += c.getHeightF();
            }
            return this.y + this.titleHeight + modulesHeight + 4.0F;
        } else {
            return this.y + this.titleHeight + 4.0F;
        }
    }

    public void setScreenSize(float screenWidth, float screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void limitPositions() {
        this.setX(this.x, true);
        this.setY(this.y, true);
    }

    public void applySavedState(float x, float y, boolean opened, boolean clampToScreen) {
        if (clampToScreen) {
            this.setX(x, true);
            this.setY(y, true);
        } else {
            float scrollOffset = this.scrollAnim.getTarget() - this.y;
            this.x = x;
            this.y = y;
            float newTarget = y + scrollOffset;
            this.moduleY = newTarget;
            this.scrollAnim.reset(newTarget);
        }
        this.opened = opened;
        this.smoothTimer = null;
        this.textTimer = null;
        if (opened && !this.modules.isEmpty()) {
            CategoryComponent.CategoryLayoutMetrics layoutMetrics = this.computeLayoutMetrics(true);
            this.big = layoutMetrics.visibleHeight;
            this.lastHeight = layoutMetrics.contentBottom;
        } else {
            this.big = 0.0F;
            this.lastHeight = this.y + this.titleHeight + 4.0F;
        }
        this.moduleY = this.y;
        this.scrollAnim.reset(this.y);
    }

    public void onGuiClosed() {
        if (this.smoothTimer != null || this.textTimer != null) {
            float finalHeight = this.y + this.titleHeight;
            if (this.opened && !this.modules.isEmpty()) {
                float modulesHeight = 0.0F;
                for (ModuleComponent c : this.modules) {
                    modulesHeight += c.getHeightF();
                }
                finalHeight += modulesHeight + 4.0F;
            } else {
                finalHeight += 4.0F;
            }
            this.lastHeight = finalHeight;
        }
        this.smoothTimer = null;
        this.textTimer = null;
        this.moduleY = this.scrollAnim.getTarget();
        this.scrollAnim.reset(this.moduleY);
    }

    private CategoryComponent.CategoryLayoutMetrics computeLayoutMetrics(boolean updateModuleOffsets) {
        if (!this.modules.isEmpty() && (this.opened || this.smoothTimer != null)) {
            float maxModulesHeight = this.screenHeight * 0.9F - this.titleHeight - 4.0F;
            float visibleHeight = 0.0F;
            float totalScrollExtent = 0.0F;
            float moduleOffset = this.titleHeight + 3.0F;
            for (ModuleComponent component : this.modules) {
                if (updateModuleOffsets) {
                    component.updateHeight(moduleOffset);
                }
                float componentHeight = component.getHeightF();
                moduleOffset += componentHeight;
                totalScrollExtent += component.getScrollExtentHeightF();
                if (visibleHeight < maxModulesHeight) {
                    visibleHeight += Math.min(componentHeight, maxModulesHeight - visibleHeight);
                }
            }
            float viewport = Math.min(maxModulesHeight, totalScrollExtent);
            float overflow = Math.max(0.0F, totalScrollExtent - viewport);
            float minScrollY = overflow > 0.0F ? this.y - overflow : this.y;
            float maxBottom = this.y + this.screenHeight * 0.9F;
            float contentBottom = Math.min(this.y + this.titleHeight + visibleHeight + 4.0F, maxBottom);
            return new CategoryComponent.CategoryLayoutMetrics(Math.max(0.0F, visibleHeight), minScrollY, contentBottom);
        } else {
            return new CategoryComponent.CategoryLayoutMetrics(0.0F, this.y, this.y + this.titleHeight + 4.0F);
        }
    }

    private static Map<Module.category, CategoryComponent.CategoryIconStacks> buildCategoryIconStacks() {
        EnumMap<Module.category, CategoryComponent.CategoryIconStacks> iconStacks = new EnumMap<>(Module.category.class);
        for (Module.category category : Module.category.values()) {
            ItemStack normalStack = createCategoryIconStack(category, false);
            ItemStack activeStack = createCategoryIconStack(category, true);
            if (normalStack != null && activeStack != null) {
                iconStacks.put(category, new CategoryComponent.CategoryIconStacks(normalStack, activeStack));
            }
        }
        return iconStacks;
    }

    private static ItemStack createCategoryIconStack(Module.category category, boolean active) {
        ItemStack itemStack;
        switch(category) {
            case combat:
                itemStack = new ItemStack(Items.field_151048_u);
                break;
            case movement:
                itemStack = new ItemStack(Items.field_151175_af);
                break;
            case player:
                itemStack = new ItemStack(Items.field_151153_ao);
                break;
            case world:
                itemStack = new ItemStack(Items.field_151098_aY);
                break;
            case render:
                itemStack = new ItemStack(Items.field_151061_bv);
                break;
            case minigames:
                itemStack = new ItemStack(Items.field_151043_k);
                break;
            case network:
                itemStack = new ItemStack(Items.field_151132_bS);
                break;
            case other:
                itemStack = new ItemStack(Items.field_151113_aN);
                break;
            case client:
                itemStack = new ItemStack(Items.field_151111_aL);
                break;
            case profiles:
                itemStack = new ItemStack(Items.field_151122_aG);
                break;
            case scripts:
                itemStack = new ItemStack(Items.field_151137_ax);
                break;
            default:
                return null;
        }
        if (!active) {
            return itemStack;
        } else {
            if (category != Module.category.player) {
                itemStack.func_77966_a(Enchantment.field_77347_r, 2);
            } else {
                itemStack.func_77964_b(1);
            }
            return itemStack;
        }
    }

    private static final class CategoryIconStacks {

        private final ItemStack normalStack;

        private final ItemStack activeStack;

        private CategoryIconStacks(ItemStack normalStack, ItemStack activeStack) {
            this.normalStack = normalStack;
            this.activeStack = activeStack;
        }
    }

    private static final class CategoryLayoutMetrics {

        private final float visibleHeight;

        private final float minScrollY;

        private final float contentBottom;

        private CategoryLayoutMetrics(float visibleHeight, float minScrollY, float contentBottom) {
            this.visibleHeight = visibleHeight;
            this.minScrollY = minScrollY;
            this.contentBottom = contentBottom;
        }
    }
}
