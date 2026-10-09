package org.spongepowered.tools.obfuscation.mirror;

import java.lang.annotation.Annotation;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.SignaturePrinter;

public class TypeHandleSimulated extends TypeHandle {

    private final TypeElement simulatedType;

    public TypeHandleSimulated(String name, TypeMirror type) {
        this(TypeUtils.getPackage(type), name, type);
    }

    public TypeHandleSimulated(PackageElement pkg, String name, TypeMirror type) {
        super(pkg, name);
        this.simulatedType = (TypeElement) ((DeclaredType) type).asElement();
    }

    @Override
    protected TypeElement getTargetElement() {
        return this.simulatedType;
    }

    @Override
    public boolean isPublic() {
        return true;
    }

    @Override
    public boolean isImaginary() {
        return false;
    }

    @Override
    public boolean isSimulated() {
        return true;
    }

    @Override
    public AnnotationHandle getAnnotation(Class<? extends Annotation> annotationClass) {
        return null;
    }

    @Override
    public TypeHandle getSuperclass() {
        return null;
    }

    @Override
    public String findDescriptor(MemberInfo memberInfo) {
        return memberInfo != null ? memberInfo.desc : null;
    }

    @Override
    public FieldHandle findField(String name, String type, boolean caseSensitive) {
        return new FieldHandle(null, name, type);
    }

    @Override
    public MethodHandle findMethod(String name, String desc, boolean caseSensitive) {
        return new MethodHandle(null, name, desc);
    }

    @Override
    public MappingMethod getMappingMethod(String name, String desc) {
        String signature = new SignaturePrinter(name, desc).setFullyQualified(true).toDescriptor();
        String rawSignature = TypeUtils.stripGenerics(signature);
        MethodHandle method = findMethodRecursive(this, name, signature, rawSignature, true);
        return method != null ? method.asMapping(true) : super.getMappingMethod(name, desc);
    }

    private static MethodHandle findMethodRecursive(TypeHandle target, String name, String signature, String rawSignature, boolean matchCase) {
        TypeElement elem = target.getTargetElement();
        if (elem == null) {
            return null;
        } else {
            MethodHandle method = TypeHandle.findMethod(target, name, signature, rawSignature, matchCase);
            if (method != null) {
                return method;
            } else {
                for (TypeMirror iface : elem.getInterfaces()) {
                    method = findMethodRecursive(iface, name, signature, rawSignature, matchCase);
                    if (method != null) {
                        return method;
                    }
                }
                TypeMirror superClass = elem.getSuperclass();
                return superClass != null && superClass.getKind() != TypeKind.NONE ? findMethodRecursive(superClass, name, signature, rawSignature, matchCase) : null;
            }
        }
    }

    private static MethodHandle findMethodRecursive(TypeMirror target, String name, String signature, String rawSignature, boolean matchCase) {
        if (!(target instanceof DeclaredType)) {
            return null;
        } else {
            TypeElement element = (TypeElement) ((DeclaredType) target).asElement();
            return findMethodRecursive(new TypeHandle(element), name, signature, rawSignature, matchCase);
        }
    }
}
