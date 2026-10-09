package keystrokesmod.script;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import javax.tools.StandardJavaFileManager;
import keystrokesmod.Raven;
import keystrokesmod.utility.Utils;
import net.minecraft.launchwrapper.Launch;

public class Script {

    public String name;

    public Class clazz;

    public Object instance;

    public String scriptName;

    public String codeStr;

    public boolean error = false;

    public boolean usesLoadString = false;

    public int STARTING_LINE;

    public ScriptEvents event;

    public File file;

    public Script(String name) {
        this.name = name;
        this.scriptName = "sc_" + name.replace(" ", "").replace(")", "_").replace("(", "_") + "_" + Utils.generateRandomString(5);
    }

    public boolean run() {
        try {
            if (this.scriptName != null && this.codeStr != null) {
                File file = new File(Raven.scriptManager.COMPILED_DIR);
                if (!file.exists() || !file.isDirectory()) {
                    file.mkdir();
                }
                if (Raven.scriptManager.compiler == null) {
                    return false;
                } else {
                    ScriptDiagnosticListener bp = new ScriptDiagnosticListener();
                    StandardJavaFileManager fileManager = Raven.scriptManager.compiler.getStandardFileManager(bp, null, null);
                    ArrayList<String> compilationOptions = new ArrayList<>();
                    compilationOptions.add("-d");
                    compilationOptions.add(Raven.scriptManager.COMPILED_DIR);
                    compilationOptions.add("-XDuseUnsharedTable");
                    if (!(Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
                        compilationOptions.add("-classpath");
                        String s = Raven.scriptManager.jarPath;
                        try {
                            s = URLDecoder.decode(s, "UTF-8");
                        } catch (UnsupportedOperationException exception) {
                        }
                        compilationOptions.add(s);
                    }
                    boolean success = Raven.scriptManager.compiler.getTask(null, fileManager, bp, compilationOptions, null, Arrays.asList(new JavaSourceFromString(this.scriptName, this.codeStr, this.STARTING_LINE))).call();
                    if (!success) {
                        this.error = true;
                        return false;
                    } else {
                        try (SecureClassLoader secureClassLoader = new SecureClassLoader(new URL[] { file.toURI().toURL() }, Launch.classLoader)) {
                            this.clazz = secureClassLoader.loadClass(this.scriptName);
                            this.loadGeneratedNestedClasses(secureClassLoader, file);
                            this.instance = this.clazz.newInstance();
                            fileManager.close();
                        } catch (Throwable exception2) {
                            exception2.printStackTrace();
                            Utils.sendMessage("&7Script &b" + Utils.extractFileName(this.name) + " &7blocked, &cunsafe code&7 detected!");
                            this.error = true;
                            return false;
                        }
                        return true;
                    }
                }
            } else {
                return false;
            }
        } catch (Exception exception3) {
            this.error = true;
            return !this.error;
        }
    }

    public int getBoolean(String s, Object... array) {
        if (this.clazz != null && this.instance != null) {
            Method method = null;
            for (Method method2 : this.clazz.getDeclaredMethods()) {
                if (method2.getName().equalsIgnoreCase(s) && method2.getParameterCount() == array.length && method2.getReturnType().equals(boolean.class)) {
                    method = method2;
                    break;
                }
            }
            if (method != null) {
                try {
                    method.setAccessible(true);
                    Object invoke = method.invoke(this.instance, array);
                    if (invoke instanceof Boolean) {
                        return (Boolean) invoke ? 1 : 0;
                    }
                } catch (InvocationTargetException | IllegalAccessException exception4) {
                    Utils.sendMessage("&7Runtime error during script &b" + this.name);
                    if (exception4.getCause() == null) {
                        Utils.sendMessage(" &7err: &cThrowable");
                    } else {
                        Utils.sendMessage(" &7err: &c" + exception4.getCause().getClass().getSimpleName());
                        StackTraceElement[] stArr = exception4.getCause().getStackTrace();
                        if (stArr.length > 0) {
                            StackTraceElement st = stArr[0];
                            for (StackTraceElement element : exception4.getCause().getStackTrace()) {
                                if (element.getClassName().equalsIgnoreCase(this.scriptName)) {
                                    st = element;
                                    break;
                                }
                            }
                            Utils.sendMessage(" &7line: &c" + (st.getLineNumber() - this.STARTING_LINE));
                            Utils.sendMessage(" &7src: &c" + st.getMethodName());
                        }
                    }
                }
            }
            return -1;
        } else {
            return -1;
        }
    }

    public String getString(String s, Object... array) {
        if (this.clazz != null && this.instance != null) {
            Method method = null;
            for (Method method2 : this.clazz.getDeclaredMethods()) {
                if (method2.getName().equalsIgnoreCase(s) && method2.getParameterCount() == array.length && method2.getReturnType().equals(String.class)) {
                    method = method2;
                    break;
                }
            }
            if (method != null) {
                try {
                    method.setAccessible(true);
                    Object invoke = method.invoke(this.instance, array);
                    if (invoke instanceof String) {
                        return (String) invoke;
                    }
                } catch (InvocationTargetException | IllegalAccessException exception5) {
                    Utils.sendMessage("&7Runtime error during script &b" + this.name);
                    if (exception5.getCause() == null) {
                        Utils.sendMessage(" &7err: &cThrowable");
                    } else {
                        Utils.sendMessage(" &7err: &c" + exception5.getCause().getClass().getSimpleName());
                        StackTraceElement[] stArr = exception5.getCause().getStackTrace();
                        if (stArr.length > 0) {
                            StackTraceElement st = stArr[0];
                            for (StackTraceElement element : exception5.getCause().getStackTrace()) {
                                if (element.getClassName().equalsIgnoreCase(this.scriptName)) {
                                    st = element;
                                    break;
                                }
                            }
                            Utils.sendMessage(" &7line: &c" + (st.getLineNumber() - this.STARTING_LINE));
                            Utils.sendMessage(" &7src: &c" + st.getMethodName());
                        }
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    public Float[] getFloatArray(String methodName, Object... args) {
        if (this.clazz != null && this.instance != null) {
            Method method = null;
            for (Method _method : this.clazz.getDeclaredMethods()) {
                if (_method.getName().equals(methodName) && _method.getReturnType().equals(Float[].class) && _method.getParameterCount() == args.length) {
                    method = _method;
                    break;
                }
            }
            if (method != null) {
                try {
                    method.setAccessible(true);
                    Object result = method.invoke(this.instance, args);
                    if (result instanceof Float[]) {
                        return (Float[]) result;
                    }
                } catch (InvocationTargetException | IllegalAccessException exception6) {
                    Utils.sendMessage("&7Runtime error during script &b" + this.name);
                    if (exception6.getCause() == null) {
                        Utils.sendMessage(" &7err: &cThrowable");
                    } else {
                        Utils.sendMessage(" &7err: &c" + exception6.getCause().getClass().getSimpleName());
                        StackTraceElement[] stArr = exception6.getCause().getStackTrace();
                        if (stArr.length > 0) {
                            StackTraceElement st = stArr[0];
                            for (StackTraceElement element : exception6.getCause().getStackTrace()) {
                                if (element.getClassName().equalsIgnoreCase(this.scriptName)) {
                                    st = element;
                                    break;
                                }
                            }
                            Utils.sendMessage(" &7line: &c" + (st.getLineNumber() - this.STARTING_LINE));
                            Utils.sendMessage(" &7src: &c" + st.getMethodName());
                        }
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    public void delete() {
        this.clazz = null;
        this.instance = null;
        File file = new File(Raven.scriptManager.COMPILED_DIR + File.separator + this.scriptName + ".class");
        if (file.exists()) {
            file.delete();
        }
    }

    public void setCode(String code) {
        this.STARTING_LINE = 0;
        StringBuilder fileCodeContents = new StringBuilder();
        Iterator<String> iterator = Raven.scriptManager.imports.iterator();
        while (iterator.hasNext()) {
            this.STARTING_LINE++;
            fileCodeContents.append("import ").append(iterator.next()).append(";\n");
        }
        fileCodeContents.append("import keystrokesmod.script.model.*;\n");
        fileCodeContents.append("import keystrokesmod.script.packet.clientbound.*;\n");
        fileCodeContents.append("import keystrokesmod.script.packet.serverbound.*;\n");
        String name = Utils.extractFileName(this.name);
        String transformedCode = ScriptSourceTransformer.supportNestedTypes(code);
        this.codeStr = fileCodeContents + "@SuppressWarnings(\"unchecked\")public class " + this.scriptName + " extends " + ScriptDefaults.class.getName() + " {public static final " + ScriptDefaults.modules.class.getName().replace("$", ".") + " modules = new " + ScriptDefaults.modules.class.getName().replace("$", ".") + "(\"" + name + "\");public static final String scriptName = \"" + name + "\";\n" + transformedCode + "\n}";
        this.STARTING_LINE += 4;
    }

    private void loadGeneratedNestedClasses(SecureClassLoader classLoader, File outputDirectory) throws ClassNotFoundException {
        File[] nestedClassFiles = outputDirectory.listFiles((directory, value) -> value.startsWith(this.scriptName + "$") && value.endsWith(".class"));
        if (nestedClassFiles != null && nestedClassFiles.length != 0) {
            Arrays.sort(nestedClassFiles, Comparator.<File>comparingInt(classFile -> this.nestedClassDepth(classFile.getName())).thenComparing(File::getName));
            for (File nestedClassFile : nestedClassFiles) {
                String fileName = nestedClassFile.getName();
                classLoader.loadClass(fileName.substring(0, fileName.length() - ".class".length()));
            }
        }
    }

    private int nestedClassDepth(String fileName) {
        int depth = 0;
        for (int i = 0; i < fileName.length(); i++) {
            if (fileName.charAt(i) == '$') {
                depth++;
            }
        }
        return depth;
    }

    public boolean invoke(String s, Object... array) {
        if (this.clazz != null && this.instance != null) {
            Method method = null;
            for (Method method2 : this.clazz.getDeclaredMethods()) {
                if (method2.getName().equalsIgnoreCase(s) && method2.getParameterCount() == array.length && method2.getReturnType().equals(void.class)) {
                    method = method2;
                    break;
                }
            }
            if (method != null) {
                try {
                    method.setAccessible(true);
                    method.invoke(this.instance, array);
                    return true;
                } catch (InvocationTargetException | IllegalAccessException exception7) {
                    Utils.sendMessage("&7Runtime error during script &b" + this.name);
                    if (exception7.getCause() == null) {
                        Utils.sendMessage(" &7err: &cThrowable");
                    } else {
                        Utils.sendMessage(" &7err: &c" + exception7.getCause().getClass().getSimpleName());
                        StackTraceElement[] stArr = exception7.getCause().getStackTrace();
                        if (stArr.length > 0) {
                            StackTraceElement st = stArr[0];
                            for (StackTraceElement element : exception7.getCause().getStackTrace()) {
                                if (element.getClassName().equalsIgnoreCase(this.scriptName)) {
                                    st = element;
                                    break;
                                }
                            }
                            Utils.sendMessage(" &7line: &c" + (st.getLineNumber() - this.STARTING_LINE));
                            Utils.sendMessage(" &7src: &c" + st.getMethodName());
                        }
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }
}
