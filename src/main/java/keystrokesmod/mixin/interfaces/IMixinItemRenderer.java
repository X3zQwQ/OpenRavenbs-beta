package keystrokesmod.mixin.interfaces;

public interface IMixinItemRenderer {

    void setCancelUpdate(boolean cancelUpdate);

    void setCancelReset(boolean cancelReset);

    boolean isRenderItemInUse();

    void setRenderItemInUse(boolean renderItemInUse);
}
