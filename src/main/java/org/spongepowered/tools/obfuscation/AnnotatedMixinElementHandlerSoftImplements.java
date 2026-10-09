package org.spongepowered.tools.obfuscation;

import java.util.List;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic.Kind;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.MethodHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;

public class AnnotatedMixinElementHandlerSoftImplements extends AnnotatedMixinElementHandler {

    AnnotatedMixinElementHandlerSoftImplements(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        super(ap, mixin);
    }

    public void process(AnnotationHandle implementsAnnotation) {
        if (this.mixin.remap()) {
            List<AnnotationHandle> interfaces = implementsAnnotation.getAnnotationList("value");
            if (interfaces.size() < 1) {
                this.ap.printMessage(Kind.WARNING, "Empty @Implements annotation", this.mixin.getMixin(), implementsAnnotation.asMirror());
            } else {
                for (AnnotationHandle interfaceAnnotation : interfaces) {
                    Interface.Remap remap = interfaceAnnotation.getValue("remap", Interface.Remap.ALL);
                    if (remap != Interface.Remap.NONE) {
                        try {
                            TypeHandle iface = new TypeHandle(interfaceAnnotation.<javax.lang.model.type.DeclaredType>getValue("iface"));
                            String prefix = interfaceAnnotation.getValue("prefix");
                            this.processSoftImplements(remap, iface, prefix);
                        } catch (Exception exception) {
                            this.ap.printMessage(Kind.ERROR, "Unexpected error: " + exception.getClass().getName() + ": " + exception.getMessage(), this.mixin.getMixin(), interfaceAnnotation.asMirror());
                        }
                    }
                }
            }
        }
    }

    private void processSoftImplements(Interface.Remap remap, TypeHandle iface, String prefix) {
        for (ExecutableElement method : iface.<ExecutableElement>getEnclosedElements(ElementKind.METHOD)) {
            this.processMethod(remap, iface, prefix, method);
        }
        for (TypeHandle superInterface : iface.getInterfaces()) {
            this.processSoftImplements(remap, superInterface, prefix);
        }
    }

    private void processMethod(Interface.Remap remap, TypeHandle iface, String prefix, ExecutableElement method) {
        String name = method.getSimpleName().toString();
        String sig = TypeUtils.getJavaSignature(method);
        String desc = TypeUtils.getDescriptor(method);
        if (remap != Interface.Remap.ONLY_PREFIXED) {
            MethodHandle mixinMethod = this.mixin.getHandle().findMethod(name, sig);
            if (mixinMethod != null) {
                this.addInterfaceMethodMapping(remap, iface, null, mixinMethod, name, desc);
            }
        }
        if (prefix != null) {
            MethodHandle prefixedMixinMethod = this.mixin.getHandle().findMethod(prefix + name, sig);
            if (prefixedMixinMethod != null) {
                this.addInterfaceMethodMapping(remap, iface, prefix, prefixedMixinMethod, name, desc);
            }
        }
    }

    private void addInterfaceMethodMapping(Interface.Remap remap, TypeHandle iface, String prefix, MethodHandle method, String name, String desc) {
        MappingMethod mapping = new MappingMethod(iface.getName(), name, desc);
        ObfuscationData<MappingMethod> obfData = this.obf.getDataProvider().getObfMethod(mapping);
        if (obfData.isEmpty()) {
            if (remap.forceRemap()) {
                this.ap.printMessage(Kind.ERROR, "No obfuscation mapping for soft-implementing method", method.getElement());
            }
        } else {
            this.addMethodMappings(method.getName(), desc, this.applyPrefix(obfData, prefix));
        }
    }

    private ObfuscationData<MappingMethod> applyPrefix(ObfuscationData<MappingMethod> data, String prefix) {
        if (prefix == null) {
            return data;
        } else {
            ObfuscationData<MappingMethod> prefixed = new ObfuscationData<>();
            for (ObfuscationType type : data) {
                MappingMethod mapping = data.get(type);
                prefixed.put(type, mapping.addPrefix(prefix));
            }
            return prefixed;
        }
    }
}
