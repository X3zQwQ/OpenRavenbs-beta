package org.spongepowered.tools.obfuscation.mirror;

import com.google.common.collect.ImmutableList;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public final class AnnotationHandle {

    public static final AnnotationHandle MISSING = new AnnotationHandle(null);

    private final AnnotationMirror annotation;

    private AnnotationHandle(AnnotationMirror annotation) {
        this.annotation = annotation;
    }

    public AnnotationMirror asMirror() {
        return this.annotation;
    }

    public boolean exists() {
        return this.annotation != null;
    }

    @Override
    public String toString() {
        return this.annotation == null ? "@{UnknownAnnotation}" : "@" + this.annotation.getAnnotationType().asElement().getSimpleName();
    }

    public <T> T getValue(String key, T defaultValue) {
        if (this.annotation == null) {
            return defaultValue;
        } else {
            AnnotationValue value = this.getAnnotationValue(key);
            if (defaultValue instanceof Enum && value != null) {
                VariableElement varValue = (VariableElement) value.getValue();
                return varValue == null ? defaultValue : (T) Enum.valueOf((Class) defaultValue.getClass(), varValue.getSimpleName().toString());
            } else {
                return (T) (value != null ? value.getValue() : defaultValue);
            }
        }
    }

    public <T> T getValue() {
        return this.getValue("value", null);
    }

    public <T> T getValue(String key) {
        return this.getValue(key, null);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return this.getValue(key, defaultValue);
    }

    public AnnotationHandle getAnnotation(String key) {
        Object value = this.getValue(key);
        if (value instanceof AnnotationMirror) {
            return of((AnnotationMirror) value);
        } else {
            if (value instanceof AnnotationValue) {
                Object mirror = ((AnnotationValue) value).getValue();
                if (mirror instanceof AnnotationMirror) {
                    return of((AnnotationMirror) mirror);
                }
            }
            return null;
        }
    }

    public <T> List<T> getList() {
        return this.getList("value");
    }

    public <T> List<T> getList(String key) {
        List<AnnotationValue> list = this.getValue(key, Collections.emptyList());
        return unwrapAnnotationValueList(list);
    }

    public List<AnnotationHandle> getAnnotationList(String key) {
        Object val = this.getValue(key, null);
        if (val == null) {
            return Collections.emptyList();
        } else if (val instanceof AnnotationMirror) {
            return ImmutableList.of(of((AnnotationMirror) val));
        } else {
            List<AnnotationValue> list = (List<AnnotationValue>) val;
            List<AnnotationHandle> annotations = new ArrayList<>(list.size());
            for (AnnotationValue value : list) {
                annotations.add(new AnnotationHandle((AnnotationMirror) value.getValue()));
            }
            return Collections.unmodifiableList(annotations);
        }
    }

    protected AnnotationValue getAnnotationValue(String key) {
        for (ExecutableElement elem : this.annotation.getElementValues().keySet()) {
            if (elem.getSimpleName().contentEquals(key)) {
                return this.annotation.getElementValues().get(elem);
            }
        }
        return null;
    }

    protected static <T> List<T> unwrapAnnotationValueList(List<AnnotationValue> list) {
        if (list == null) {
            return Collections.emptyList();
        } else {
            List<T> unfolded = new ArrayList<>(list.size());
            for (AnnotationValue value : list) {
                unfolded.add((T) value.getValue());
            }
            return unfolded;
        }
    }

    protected static AnnotationMirror getAnnotation(Element elem, Class<? extends Annotation> annotationClass) {
        if (elem == null) {
            return null;
        } else {
            List<? extends AnnotationMirror> annotations = elem.getAnnotationMirrors();
            if (annotations == null) {
                return null;
            } else {
                for (AnnotationMirror annotation : annotations) {
                    Element element = annotation.getAnnotationType().asElement();
                    if (element instanceof TypeElement) {
                        TypeElement annotationElement = (TypeElement) element;
                        if (annotationElement.getQualifiedName().contentEquals(annotationClass.getName())) {
                            return annotation;
                        }
                    }
                }
                return null;
            }
        }
    }

    public static AnnotationHandle of(AnnotationMirror annotation) {
        return new AnnotationHandle(annotation);
    }

    public static AnnotationHandle of(Element elem, Class<? extends Annotation> annotationClass) {
        return new AnnotationHandle(getAnnotation(elem, annotationClass));
    }
}
