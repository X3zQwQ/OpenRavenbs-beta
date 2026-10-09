package keystrokesmod.utility.shader;

import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL20;

public abstract class OutlineESPShader {

    private static final String VERT = "#version 120\nvoid main() {\n  gl_TexCoord[0] = gl_MultiTexCoord0;\n  gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n}";

    protected int programId = -1;

    private final Map<String, Integer> uniforms = new HashMap<>();

    public OutlineESPShader(String fragSrc) {
        int v = this.compile("#version 120\nvoid main() {\n  gl_TexCoord[0] = gl_MultiTexCoord0;\n  gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n}", 35633);
        int f = this.compile(fragSrc, 35632);
        if (v >= 0 && f >= 0) {
            this.programId = GL20.glCreateProgram();
            GL20.glAttachShader(this.programId, v);
            GL20.glAttachShader(this.programId, f);
            GL20.glLinkProgram(this.programId);
            if (GL20.glGetProgrami(this.programId, 35714) == 0) {
                this.programId = -1;
            } else {
                this.onLink();
            }
        }
    }

    private int compile(String src, int type) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        return GL20.glGetShaderi(id, 35713) == 0 ? -1 : id;
    }

    protected void cacheUniform(String name) {
        if (this.programId >= 0) {
            this.uniforms.put(name, GL20.glGetUniformLocation(this.programId, name));
        }
    }

    protected int uniform(String name) {
        return this.uniforms.getOrDefault(name, -1);
    }

    public abstract void onLink();

    public abstract void onUse();

    public void use() {
        if (this.programId >= 0) {
            this.onUse();
        }
    }

    public void stop() {
        GL20.glUseProgram(0);
    }

    public boolean isValid() {
        return this.programId >= 0;
    }
}
