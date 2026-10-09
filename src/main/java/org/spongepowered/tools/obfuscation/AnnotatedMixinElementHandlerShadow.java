package org.spongepowered.tools.obfuscation;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;
import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationDataProvider;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

class AnnotatedMixinElementHandlerShadow extends AnnotatedMixinElementHandler {

    AnnotatedMixinElementHandlerShadow(IMixinAnnotationProcessor ap, AnnotatedMixin mixin) {
        super(ap, mixin);
    }

    public void registerShadow(AnnotatedMixinElementHandlerShadow.AnnotatedElementShadow<?, ?> elem) {
        this.validateTarget(elem.getElement(), elem.getAnnotation(), elem.getName(), "@Shadow");
        if (elem.shouldRemap()) {
            for (TypeHandle target : this.mixin.getTargets()) {
                this.registerShadowForTarget(elem, target);
            }
        }
    }

    private void registerShadowForTarget(AnnotatedMixinElementHandlerShadow.AnnotatedElementShadow<?, ?> elem, TypeHandle target) {
        ObfuscationData<? extends IMapping<?>> obfData = (ObfuscationData<? extends IMapping<?>>) elem.getObfuscationData(this.obf.getDataProvider(), target);
        if (obfData.isEmpty()) {
            String info = this.mixin.isMultiTarget() ? " in target " + target : "";
            if (target.isSimulated()) {
                elem.printMessage(this.ap, Kind.WARNING, "Unable to locate obfuscation mapping" + info + " for @Shadow " + elem);
            } else {
                elem.printMessage(this.ap, Kind.WARNING, "Unable to locate obfuscation mapping" + info + " for @Shadow " + elem);
            }
        } else {
            for (ObfuscationType type : obfData) {
                try {
                    elem.addMapping(type, (IMapping<?>) obfData.get(type));
                } catch (Mappings.MappingConflictException exception) {
                    elem.printMessage(this.ap, Kind.ERROR, "Mapping conflict for @Shadow " + elem + ": " + exception.getNew().getSimpleName() + " for target " + target + " conflicts with existing mapping " + exception.getOld().getSimpleName());
                }
            }
        }
    }

    abstract static class AnnotatedElementShadow<E extends Element, M extends IMapping<M>> extends AnnotatedMixinElementHandler.AnnotatedElement<E> {

        private final boolean shouldRemap;

        private final AnnotatedMixinElementHandler.ShadowElementName name;

        private final IMapping.Type type;

        protected AnnotatedElementShadow(E element, AnnotationHandle annotation, boolean shouldRemap, IMapping.Type type) {
            super(element, annotation);
            this.shouldRemap = shouldRemap;
            this.name = new AnnotatedMixinElementHandler.ShadowElementName(element, annotation);
            this.type = type;
        }

        public boolean shouldRemap() {
            return this.shouldRemap;
        }

        public AnnotatedMixinElementHandler.ShadowElementName getName() {
            return this.name;
        }

        public IMapping.Type getElementType() {
            return this.type;
        }

        @Override
        public String toString() {
            return this.getElementType().name().toLowerCase();
        }

        public AnnotatedMixinElementHandler.ShadowElementName setObfuscatedName(IMapping<?> name) {
            return this.setObfuscatedName(name.getSimpleName());
        }

        public AnnotatedMixinElementHandler.ShadowElementName setObfuscatedName(String name) {
            return this.getName().setObfuscatedName(name);
        }

        public ObfuscationData<M> getObfuscationData(IObfuscationDataProvider provider, TypeHandle owner) {
            return provider.getObfEntry(this.getMapping(owner, this.getName().toString(), this.getDesc()));
        }

        public abstract M getMapping(TypeHandle typeHandle, String text, String text2);

        public abstract void addMapping(ObfuscationType obfuscationType, IMapping<?> iMapping);
    }

    class AnnotatedElementShadowField extends AnnotatedMixinElementHandlerShadow.AnnotatedElementShadow<VariableElement, MappingField> {

        public AnnotatedElementShadowField(VariableElement element, AnnotationHandle annotation, boolean shouldRemap) {
            super(element, annotation, shouldRemap, IMapping.Type.FIELD);
        }

        public MappingField getMapping(TypeHandle owner, String name, String desc) {
            return new MappingField(owner.getName(), name, desc);
        }

        @Override
        public void addMapping(ObfuscationType type, IMapping<?> remapped) {
            AnnotatedMixinElementHandlerShadow.this.addFieldMapping(type, this.setObfuscatedName(remapped), this.getDesc(), remapped.getDesc());
        }
    }

    class AnnotatedElementShadowMethod extends AnnotatedMixinElementHandlerShadow.AnnotatedElementShadow<ExecutableElement, MappingMethod> {

        public AnnotatedElementShadowMethod(ExecutableElement element, AnnotationHandle annotation, boolean shouldRemap) {
            super(element, annotation, shouldRemap, IMapping.Type.METHOD);
        }

        public MappingMethod getMapping(TypeHandle owner, String name, String desc) {
            return owner.getMappingMethod(name, desc);
        }

        @Override
        public void addMapping(ObfuscationType type, IMapping<?> remapped) {
            AnnotatedMixinElementHandlerShadow.this.addMethodMapping(type, this.setObfuscatedName(remapped), this.getDesc(), remapped.getDesc());
        }
    }
}
