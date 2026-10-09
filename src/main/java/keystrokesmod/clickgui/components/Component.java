package keystrokesmod.clickgui.components;

public class Component {

    public void render() {
    }

    public void drawScreen(int x, int y) {
    }

    public boolean onClick(int x, int y, int b) {
        return false;
    }

    public void mouseReleased(int x, int y, int m) {
    }

    public void keyTyped(char t, int k) {
    }

    public void updateHeight(float n) {
    }

    public int getHeight() {
        return Math.round(this.getHeightF());
    }

    public float getHeightF() {
        return 0.0F;
    }

    public void onGuiClosed() {
    }

    public void onScroll(int scroll) {
    }

    public float getOffset() {
        return 0.0F;
    }

    public boolean isBaseVisible() {
        return true;
    }
}
