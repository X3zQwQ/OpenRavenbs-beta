package keystrokesmod.module.impl.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class ItemESP extends Module {

    private final ButtonSetting disableDiamond;

    private final ButtonSetting disableEmerald;

    private final ButtonSetting disableGold;

    private final ButtonSetting disableIron;

    public ItemESP() {
        super("ItemESP", Module.category.render);
        this.liteModule = true;
        this.registerSetting(this.disableDiamond = new ButtonSetting("Disable diamond", false));
        this.registerSetting(this.disableEmerald = new ButtonSetting("Disable emerald", false));
        this.registerSetting(this.disableGold = new ButtonSetting("Disable gold", false));
        this.registerSetting(this.disableIron = new ButtonSetting("Disable iron", false));
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent e) {
        HashMap<Item, ArrayList<EntityItem>> itemsMap = new HashMap<>();
        HashMap<Double, Integer> colorMap = new HashMap<>();
        for (Entity entity : mc.field_71441_e.field_72996_f) {
            if (entity instanceof EntityItem && entity.field_70173_aa >= 3) {
                EntityItem entityItem = (EntityItem) entity;
                if (entityItem.func_92059_d().field_77994_a != 0) {
                    Item currentItem = entityItem.func_92059_d().func_77973_b();
                    if (currentItem != null) {
                        int stackSize = entityItem.func_92059_d().field_77994_a;
                        double colorDouble = this.getColorForItem(currentItem, entity.field_70165_t, entity.field_70163_u, entity.field_70161_v);
                        Integer existingStackCount = colorMap.get(colorDouble);
                        int newStackCount;
                        if (existingStackCount == null) {
                            newStackCount = stackSize;
                            ArrayList<EntityItem> itemList = itemsMap.get(currentItem);
                            if (itemList == null) {
                                itemList = new ArrayList<>();
                            }
                            itemList.add(entityItem);
                            itemsMap.put(currentItem, itemList);
                        } else {
                            newStackCount = existingStackCount + stackSize;
                        }
                        colorMap.put(colorDouble, newStackCount);
                    }
                }
            }
        }
        if (!itemsMap.isEmpty()) {
            float renderPartialTicks = ((IAccessorMinecraft) mc).getTimer().field_74281_c;
            Iterator iterator2 = itemsMap.entrySet().iterator();
            while (true) {
                Entry<Item, ArrayList<EntityItem>> entry;
                Item item;
                int boxColor;
                int textColor;
                while (true) {
                    if (!iterator2.hasNext()) {
                        return;
                    }
                    entry = (Entry<Item, ArrayList<EntityItem>>) iterator2.next();
                    item = entry.getKey();
                    if (item == Items.field_151042_j && !this.disableIron.isToggled()) {
                        boxColor = -1;
                        textColor = -1;
                        break;
                    }
                    if (item == Items.field_151043_k && !this.disableGold.isToggled()) {
                        boxColor = -331703;
                        textColor = -152;
                        break;
                    }
                    if (item == Items.field_151045_i && !this.disableDiamond.isToggled()) {
                        boxColor = -10362113;
                        textColor = -7667713;
                        break;
                    }
                    if (item == Items.field_151166_bC && !this.disableEmerald.isToggled()) {
                        boxColor = -15216030;
                        textColor = -14614644;
                        break;
                    }
                }
                for (EntityItem entityItem2 : entry.getValue()) {
                    double itemColor = this.getColorForItem(item, entityItem2.field_70165_t, entityItem2.field_70163_u, entityItem2.field_70161_v);
                    double interpolatedX = entityItem2.field_70142_S + (entityItem2.field_70165_t - entityItem2.field_70142_S) * renderPartialTicks;
                    double interpolatedY = entityItem2.field_70137_T + (entityItem2.field_70163_u - entityItem2.field_70137_T) * renderPartialTicks;
                    double interpolatedZ = entityItem2.field_70136_U + (entityItem2.field_70161_v - entityItem2.field_70136_U) * renderPartialTicks;
                    EntityPlayer self = (EntityPlayer) (Freecam.freeEntity == null ? mc.field_71439_g : Freecam.freeEntity);
                    double diffX = self.field_70142_S + (self.field_70165_t - self.field_70142_S) * renderPartialTicks - interpolatedX;
                    double diffY = self.field_70137_T + (self.field_70163_u - self.field_70137_T) * renderPartialTicks - interpolatedY;
                    double diffZ = self.field_70136_U + (self.field_70161_v - self.field_70136_U) * renderPartialTicks - interpolatedZ;
                    double dist = MathHelper.func_76133_a(diffX * diffX + diffY * diffY + diffZ * diffZ);
                    GlStateManager.func_179094_E();
                    this.drawBox(boxColor, textColor, colorMap.get(itemColor), interpolatedX, interpolatedY, interpolatedZ, dist);
                    GlStateManager.func_179121_F();
                }
            }
        }
    }

    public double getColor(double x, double y, double z) {
        if (x == 0.0) {
            x = 1.0;
        }
        if (y == 0.0) {
            y = 1.0;
        }
        if (z == 0.0) {
            z = 1.0;
        }
        return Math.round((x + 1.0) * Math.floor(y) * (z + 2.0));
    }

    private double getColorForItem(Item item, double x, double y, double z) {
        double color = this.getColor(x, y, z);
        if (item == Items.field_151042_j) {
            color += 0.155;
        } else if (item == Items.field_151043_k) {
            color += 0.255;
        } else if (item == Items.field_151045_i) {
            color += 0.355;
        } else if (item == Items.field_151166_bC) {
            color += 0.455;
        }
        return color;
    }

    public void drawBox(int boxColor, int textColor, int size, double posY, double posX, double posZ, double dist) {
        posY -= mc.func_175598_ae().field_78730_l;
        posX -= mc.func_175598_ae().field_78731_m;
        posZ -= mc.func_175598_ae().field_78728_n;
        GL11.glPushMatrix();
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glLineWidth(2.0F);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        float r = (boxColor >> 16 & 0xFF) / 255.0F;
        float g = (boxColor >> 8 & 0xFF) / 255.0F;
        float b = (boxColor & 0xFF) / 255.0F;
        float radius = Math.min(Math.max(0.2F, (float) (0.01F * dist)), 0.4F);
        RenderUtils.drawBoundingBox(new AxisAlignedBB(posY - radius, posX, posZ - radius, posY + radius, posX + radius * 2.0F, posZ + radius), r, g, b, 0.35F);
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
        GL11.glPopMatrix();
        GlStateManager.func_179094_E();
        GlStateManager.func_179137_b((float) posY, (float) posX + 0.3, (float) posZ);
        GlStateManager.func_179114_b(-mc.func_175598_ae().field_78735_i, 0.0F, 1.0F, 0.0F);
        GlStateManager.func_179114_b((mc.field_71474_y.field_74320_O == 2 ? -1 : 1) * mc.func_175598_ae().field_78732_j, 1.0F, 0.0F, 0.0F);
        float scale = Math.min(Math.max(0.02266667F, (float) (0.0015F * dist)), 0.07F);
        GlStateManager.func_179152_a(-scale, -scale, -scale);
        GlStateManager.func_179132_a(false);
        GlStateManager.func_179097_i();
        String value = String.valueOf(size);
        mc.field_71466_p.func_175065_a(value, -(mc.field_71466_p.func_78256_a(value) / 2) + scale * 3.5F, -(123.805F * scale - 2.47494F), textColor, true);
        GlStateManager.func_179126_j();
        GlStateManager.func_179132_a(true);
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.func_179121_F();
    }
}
