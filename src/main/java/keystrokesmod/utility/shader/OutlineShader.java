package keystrokesmod.utility.shader;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL20;

public class OutlineShader extends OutlineESPShader {

    private static final String FRAG = "#version 120\nuniform sampler2D tex;\nuniform vec2 texelSize;\nuniform float kernel;\nvoid main() {\n  vec4 c = texture2D(tex, gl_TexCoord[0].xy);\n  if (c.a > 0.0) { gl_FragColor = vec4(0.0); return; }\n  gl_FragColor = vec4(0.0);\n  for (float dx = -kernel; dx <= kernel; dx += 1.0)\n    for (float dy = -kernel; dy <= kernel; dy += 1.0) {\n      vec4 n = texture2D(tex, gl_TexCoord[0].xy + vec2(dx, dy) * texelSize);\n      if (n.a > 0.0) gl_FragColor = n;\n    }\n}";

    public OutlineShader() {
        super("#version 120\nuniform sampler2D tex;\nuniform vec2 texelSize;\nuniform float kernel;\nvoid main() {\n  vec4 c = texture2D(tex, gl_TexCoord[0].xy);\n  if (c.a > 0.0) { gl_FragColor = vec4(0.0); return; }\n  gl_FragColor = vec4(0.0);\n  for (float dx = -kernel; dx <= kernel; dx += 1.0)\n    for (float dy = -kernel; dy <= kernel; dy += 1.0) {\n      vec4 n = texture2D(tex, gl_TexCoord[0].xy + vec2(dx, dy) * texelSize);\n      if (n.a > 0.0) gl_FragColor = n;\n    }\n}");
    }

    @Override
    public void onLink() {
        this.cacheUniform("tex");
        this.cacheUniform("texelSize");
        this.cacheUniform("kernel");
    }

    @Override
    public void onUse() {
        GL20.glUseProgram(this.programId);
        int u = this.uniform("tex");
        if (u >= 0) {
            GL20.glUniform1i(u, 0);
        }
        u = this.uniform("texelSize");
        if (u >= 0) {
            Minecraft mc = Minecraft.func_71410_x();
            GL20.glUniform2f(u, 1.0F / mc.field_71443_c, 1.0F / mc.field_71440_d);
        }
        u = this.uniform("kernel");
        if (u >= 0) {
            GL20.glUniform1f(u, 2.0F);
        }
    }
}
