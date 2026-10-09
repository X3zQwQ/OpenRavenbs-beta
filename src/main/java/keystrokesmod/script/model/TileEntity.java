package keystrokesmod.script.model;

import net.minecraft.tileentity.TileEntitySkull;

public class TileEntity {

    private net.minecraft.tileentity.TileEntity tileEntity;

    private Vec3 position;

    public String type;

    public String name;

    public TileEntity(net.minecraft.tileentity.TileEntity tileEntity) {
        this.tileEntity = tileEntity;
        this.position = new Vec3(tileEntity.func_174877_v().func_177958_n(), tileEntity.func_174877_v().func_177956_o(), tileEntity.func_174877_v().func_177952_p());
        this.type = tileEntity.func_145838_q().getClass().getSimpleName();
        this.name = tileEntity.func_145838_q().getRegistryName().replace("minecraft:", "");
    }

    public Vec3 getPosition() {
        return this.position;
    }

    public Object[] getSkullData() {
        if (this.tileEntity instanceof TileEntitySkull) {
            TileEntitySkull skull = (TileEntitySkull) this.tileEntity;
            Object[] skullData = new Object[] { skull.func_145904_a(), skull.func_145906_b(), null, null, null };
            if (skull.func_152108_a() == null) {
                skullData[2] = skullData[3] = null;
            } else {
                skullData[2] = skull.func_152108_a().getName();
                skullData[3] = skull.func_152108_a().getId();
            }
            return skullData;
        } else {
            return null;
        }
    }
}
