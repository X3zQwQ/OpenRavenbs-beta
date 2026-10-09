package org.spongepowered.asm.mixin.injection;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableList;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.modify.AfterStoreLocal;
import org.spongepowered.asm.mixin.injection.modify.BeforeLoadLocal;
import org.spongepowered.asm.mixin.injection.points.AfterInvoke;
import org.spongepowered.asm.mixin.injection.points.BeforeConstant;
import org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess;
import org.spongepowered.asm.mixin.injection.points.BeforeFinalReturn;
import org.spongepowered.asm.mixin.injection.points.BeforeInvoke;
import org.spongepowered.asm.mixin.injection.points.BeforeNew;
import org.spongepowered.asm.mixin.injection.points.BeforeReturn;
import org.spongepowered.asm.mixin.injection.points.BeforeStringInvoke;
import org.spongepowered.asm.mixin.injection.points.JumpInsnPoint;
import org.spongepowered.asm.mixin.injection.points.MethodHead;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;

public abstract class InjectionPoint {

    public static final int DEFAULT_ALLOWED_SHIFT_BY = 0;

    public static final int MAX_ALLOWED_SHIFT_BY = 5;

    private static Map<String, Class<? extends InjectionPoint>> types = new HashMap<>();

    private final String slice;

    private final InjectionPoint.Selector selector;

    private final String id;

    protected InjectionPoint() {
        this("", InjectionPoint.Selector.DEFAULT, null);
    }

    protected InjectionPoint(InjectionPointData data) {
        this(data.getSlice(), data.getSelector(), data.getId());
    }

    public InjectionPoint(String slice, InjectionPoint.Selector selector, String id) {
        this.slice = slice;
        this.selector = selector;
        this.id = id;
    }

    public String getSlice() {
        return this.slice;
    }

    public InjectionPoint.Selector getSelector() {
        return this.selector;
    }

    public String getId() {
        return this.id;
    }

    public boolean checkPriority(int targetPriority, int mixinPriority) {
        return targetPriority < mixinPriority;
    }

    public abstract boolean find(String text, InsnList insnList, Collection<AbstractInsnNode> collection);

    @Override
    public String toString() {
        return String.format("@At(\"%s\")", this.getAtCode());
    }

    protected static AbstractInsnNode nextNode(InsnList insns, AbstractInsnNode insn) {
        int index = insns.indexOf(insn) + 1;
        return index > 0 && index < insns.size() ? insns.get(index) : insn;
    }

    public static InjectionPoint and(InjectionPoint... operands) {
        return new InjectionPoint.Intersection(operands);
    }

    public static InjectionPoint or(InjectionPoint... operands) {
        return new InjectionPoint.Union(operands);
    }

    public static InjectionPoint after(InjectionPoint point) {
        return new InjectionPoint.Shift(point, 1);
    }

    public static InjectionPoint before(InjectionPoint point) {
        return new InjectionPoint.Shift(point, -1);
    }

    public static InjectionPoint shift(InjectionPoint point, int count) {
        return new InjectionPoint.Shift(point, count);
    }

    public static List<InjectionPoint> parse(IInjectionPointContext owner, List<AnnotationNode> ats) {
        return parse(owner.getContext(), owner.getMethod(), owner.getAnnotation(), ats);
    }

    public static List<InjectionPoint> parse(IMixinContext context, MethodNode method, AnnotationNode parent, List<AnnotationNode> ats) {
        Builder<InjectionPoint> injectionPoints = ImmutableList.builder();
        for (AnnotationNode at : ats) {
            InjectionPoint injectionPoint = parse(context, method, parent, at);
            if (injectionPoint != null) {
                injectionPoints.add(injectionPoint);
            }
        }
        return injectionPoints.build();
    }

    public static InjectionPoint parse(IInjectionPointContext owner, At at) {
        return parse(owner.getContext(), owner.getMethod(), owner.getAnnotation(), at.value(), at.shift(), at.by(), Arrays.asList(at.args()), at.target(), at.slice(), at.ordinal(), at.opcode(), at.id());
    }

    public static InjectionPoint parse(IMixinContext context, MethodNode method, AnnotationNode parent, At at) {
        return parse(context, method, parent, at.value(), at.shift(), at.by(), Arrays.asList(at.args()), at.target(), at.slice(), at.ordinal(), at.opcode(), at.id());
    }

    public static InjectionPoint parse(IInjectionPointContext owner, AnnotationNode node) {
        return parse(owner.getContext(), owner.getMethod(), owner.getAnnotation(), node);
    }

    public static InjectionPoint parse(IMixinContext context, MethodNode method, AnnotationNode parent, AnnotationNode node) {
        String at = Annotations.getValue(node, "value");
        List<String> args = Annotations.getValue(node, "args");
        String target = Annotations.getValue(node, "target", "");
        String slice = Annotations.getValue(node, "slice", "");
        At.Shift shift = Annotations.getValue(node, "shift", At.Shift.class, At.Shift.NONE);
        int by = Annotations.getValue(node, "by", 0);
        int ordinal = Annotations.getValue(node, "ordinal", -1);
        int opcode = Annotations.getValue(node, "opcode", 0);
        String id = Annotations.getValue(node, "id");
        if (args == null) {
            args = ImmutableList.of();
        }
        return parse(context, method, parent, at, shift, by, args, target, slice, ordinal, opcode, id);
    }

    public static InjectionPoint parse(IMixinContext context, MethodNode method, AnnotationNode parent, String at, At.Shift shift, int by, List<String> args, String target, String slice, int ordinal, int opcode, String id) {
        InjectionPointData data = new InjectionPointData(context, method, parent, at, args, target, slice, ordinal, opcode, id);
        Class<? extends InjectionPoint> ipClass = findClass(context, data);
        InjectionPoint point = create(context, data, ipClass);
        return shift(context, method, parent, point, shift, by);
    }

    private static Class<? extends InjectionPoint> findClass(IMixinContext context, InjectionPointData data) {
        String type = data.getType();
        Class<? extends InjectionPoint> ipClass = types.get(type);
        if (ipClass != null) {
            return ipClass;
        } else if (!type.matches("^([A-Za-z_][A-Za-z0-9_]*\\.)+[A-Za-z_][A-Za-z0-9_]*$")) {
            throw new InvalidInjectionException(context, data + " is not a valid injection point specifier");
        } else {
            try {
                ipClass = (Class<? extends InjectionPoint>) Class.forName(type);
                types.put(type, ipClass);
                return ipClass;
            } catch (Exception exception) {
                throw new InvalidInjectionException(context, data + " could not be loaded or is not a valid InjectionPoint", exception);
            }
        }
    }

    private static InjectionPoint create(IMixinContext context, InjectionPointData data, Class<? extends InjectionPoint> ipClass) {
        Constructor<? extends InjectionPoint> ipCtor = null;
        try {
            ipCtor = ipClass.getDeclaredConstructor(InjectionPointData.class);
            ipCtor.setAccessible(true);
        } catch (NoSuchMethodException exception2) {
            throw new InvalidInjectionException(context, ipClass.getName() + " must contain a constructor which accepts an InjectionPointData", exception2);
        }
        InjectionPoint point = null;
        try {
            return ipCtor.newInstance(data);
        } catch (Exception exception3) {
            throw new InvalidInjectionException(context, "Error whilst instancing injection point " + ipClass.getName() + " for " + data.getAt(), exception3);
        }
    }

    private static InjectionPoint shift(IMixinContext context, MethodNode method, AnnotationNode parent, InjectionPoint point, At.Shift shift, int by) {
        if (point != null) {
            if (shift == At.Shift.BEFORE) {
                return before(point);
            }
            if (shift == At.Shift.AFTER) {
                return after(point);
            }
            if (shift == At.Shift.BY) {
                validateByValue(context, method, parent, point, by);
                return shift(point, by);
            }
        }
        return point;
    }

    private static void validateByValue(IMixinContext context, MethodNode method, AnnotationNode parent, InjectionPoint point, int by) {
        MixinEnvironment env = context.getMixin().getConfig().getEnvironment();
        InjectionPoint.ShiftByViolationBehaviour err = env.getOption(MixinEnvironment.Option.SHIFT_BY_VIOLATION_BEHAVIOUR, InjectionPoint.ShiftByViolationBehaviour.WARN);
        if (err != InjectionPoint.ShiftByViolationBehaviour.IGNORE) {
            String limitBreached = "the maximum allowed value: ";
            String advice = "Increase the value of maxShiftBy to suppress this warning.";
            int allowed = 0;
            if (context instanceof MixinTargetContext) {
                allowed = ((MixinTargetContext) context).getMaxShiftByValue();
            }
            if (by > allowed) {
                if (by > 5) {
                    limitBreached = "MAX_ALLOWED_SHIFT_BY=";
                    advice = "You must use an alternate query or a custom injection point.";
                    allowed = 5;
                }
                String message = String.format("@%s(%s) Shift.BY=%d on %s::%s exceeds %s%d. %s", Bytecode.getSimpleName(parent), point, by, context, method.name, limitBreached, allowed, advice);
                if (err == InjectionPoint.ShiftByViolationBehaviour.WARN && allowed < 5) {
                    LogManager.getLogger("mixin").warn(message);
                } else {
                    throw new InvalidInjectionException(context, message);
                }
            }
        }
    }

    protected String getAtCode() {
        InjectionPoint.AtCode code = this.getClass().getAnnotation(InjectionPoint.AtCode.class);
        return code == null ? this.getClass().getName() : code.value();
    }

    public static void register(Class<? extends InjectionPoint> type) {
        InjectionPoint.AtCode code = type.getAnnotation(InjectionPoint.AtCode.class);
        if (code == null) {
            throw new IllegalArgumentException("Injection point class " + type + " is not annotated with @AtCode");
        } else {
            Class<? extends InjectionPoint> existing = types.get(code.value());
            if (existing != null && !existing.equals(type)) {
                LogManager.getLogger("mixin").debug("Overriding InjectionPoint {} with {} (previously {})", new Object[] { code.value(), type.getName(), existing.getName() });
            }
            types.put(code.value(), type);
        }
    }

    static {
        register(BeforeFieldAccess.class);
        register(BeforeInvoke.class);
        register(BeforeNew.class);
        register(BeforeReturn.class);
        register(BeforeStringInvoke.class);
        register(JumpInsnPoint.class);
        register(MethodHead.class);
        register(AfterInvoke.class);
        register(BeforeLoadLocal.class);
        register(AfterStoreLocal.class);
        register(BeforeFinalReturn.class);
        register(BeforeConstant.class);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface AtCode {

        String value();
    }

    abstract static class CompositeInjectionPoint extends InjectionPoint {

        protected final InjectionPoint[] components;

        protected CompositeInjectionPoint(InjectionPoint... components) {
            if (components != null && components.length >= 2) {
                this.components = components;
            } else {
                throw new IllegalArgumentException("Must supply two or more component injection points for composite point!");
            }
        }

        @Override
        public String toString() {
            return "CompositeInjectionPoint(" + this.getClass().getSimpleName() + ")[" + Joiner.on(',').join(this.components) + "]";
        }
    }

    static final class Intersection extends InjectionPoint.CompositeInjectionPoint {

        public Intersection(InjectionPoint... points) {
            super(points);
        }

        @Override
        public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
            boolean found = false;
            ArrayList<AbstractInsnNode>[] allNodes = (ArrayList<AbstractInsnNode>[]) Array.newInstance(ArrayList.class, this.components.length);
            for (int i = 0; i < this.components.length; i++) {
                allNodes[i] = new ArrayList<>();
                this.components[i].find(desc, insns, allNodes[i]);
            }
            ArrayList<AbstractInsnNode> alpha = allNodes[0];
            for (int intValue = 0; intValue < alpha.size(); intValue++) {
                AbstractInsnNode node = alpha.get(intValue);
                boolean in = true;
                int b = 1;
                while (b < allNodes.length && allNodes[b].contains(node)) {
                    b++;
                }
                if (in) {
                    nodes.add(node);
                    found = true;
                }
            }
            return found;
        }
    }

    public static enum Selector {

        FIRST, LAST, ONE;

        public static final InjectionPoint.Selector DEFAULT = FIRST;
    }

    static final class Shift extends InjectionPoint {

        private final InjectionPoint input;

        private final int shift;

        public Shift(InjectionPoint input, int shift) {
            if (input == null) {
                throw new IllegalArgumentException("Must supply an input injection point for SHIFT");
            } else {
                this.input = input;
                this.shift = shift;
            }
        }

        @Override
        public String toString() {
            return "InjectionPoint(" + this.getClass().getSimpleName() + ")[" + this.input + "]";
        }

        @Override
        public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
            List<AbstractInsnNode> list = (List<AbstractInsnNode>) (nodes instanceof List ? (List) nodes : new ArrayList<>(nodes));
            this.input.find(desc, insns, nodes);
            for (int i = 0; i < list.size(); i++) {
                list.set(i, insns.get(insns.indexOf(list.get(i)) + this.shift));
            }
            if (nodes != list) {
                nodes.clear();
                nodes.addAll(list);
            }
            return nodes.size() > 0;
        }
    }

    static enum ShiftByViolationBehaviour {

        IGNORE, WARN, ERROR
    }

    static final class Union extends InjectionPoint.CompositeInjectionPoint {

        public Union(InjectionPoint... points) {
            super(points);
        }

        @Override
        public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
            LinkedHashSet<AbstractInsnNode> allNodes = new LinkedHashSet<>();
            for (int i = 0; i < this.components.length; i++) {
                this.components[i].find(desc, insns, allNodes);
            }
            nodes.addAll(allNodes);
            return allNodes.size() > 0;
        }
    }
}
