package keystrokesmod.utility.shader;

import org.lwjgl.opengl.GL20;

public class GlowShader extends OutlineESPShader {

    private static final String FRAG = "#version 120\nuniform sampler2D tex;\nuniform vec4 tint;\nvoid main() {\n  float a = texture2D(tex, gl_TexCoord[0].xy).a;\n  gl_FragColor = vec4(tint.rgb, a > 0.0 ? tint.a : 0.0);\n}";

    public GlowShader() {
        super("#version 120\nuniform sampler2D tex;\nuniform vec4 tint;\nvoid main() {\n  float a = texture2D(tex, gl_TexCoord[0].xy).a;\n  gl_FragColor = vec4(tint.rgb, a > 0.0 ? tint.a : 0.0);\n}");
    }

    @Override
    public void onLink() {
        this.cacheUniform("tex");
        this.cacheUniform("tint");
    }

    @Override
    public void onUse() {
        GL20.glUseProgram(this.programId);
        int u = this.uniform("tex");
        if (u >= 0) {
            GL20.glUniform1i(u, 0);
        }
    }

    public void setColor(int r, int g, int b, int a) {
        int u = this.uniform("tint");
        if (u >= 0) {
            GL20.glUniform4f(u, r / 255.0F, g / 255.0F, b / 255.0F, a / 255.0F);
        }
    }

    public void setColorFromARGB(int argb) {
        this.setColor(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >> 24 & 0xFF);
    }
}
