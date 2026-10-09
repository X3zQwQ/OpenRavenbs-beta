package org.spongepowered.asm.lib.tree.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.IincInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.JumpInsnNode;
import org.spongepowered.asm.lib.tree.LabelNode;
import org.spongepowered.asm.lib.tree.LookupSwitchInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.TableSwitchInsnNode;
import org.spongepowered.asm.lib.tree.TryCatchBlockNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;

public class Analyzer<V extends Value> implements Opcodes {

    private final Interpreter<V> interpreter;

    private int n;

    private InsnList insns;

    private List<TryCatchBlockNode>[] handlers;

    private Frame<V>[] frames;

    private Subroutine[] subroutines;

    private boolean[] queued;

    private int[] queue;

    private int top;

    public Analyzer(Interpreter<V> interpreter) {
        this.interpreter = interpreter;
    }

    public Frame<V>[] analyze(String owner, MethodNode m) throws AnalyzerException {
        if ((m.access & 1280) != 0) {
            this.frames = new Frame[0];
            return this.frames;
        } else {
            this.n = m.instructions.size();
            this.insns = m.instructions;
            this.handlers = new List[this.n];
            this.frames = new Frame[this.n];
            this.subroutines = new Subroutine[this.n];
            this.queued = new boolean[this.n];
            this.queue = new int[this.n];
            this.top = 0;
            for (int i = 0; i < m.tryCatchBlocks.size(); i++) {
                TryCatchBlockNode tcb = m.tryCatchBlocks.get(i);
                int begin = this.insns.indexOf(tcb.start);
                int end = this.insns.indexOf(tcb.end);
                for (int j = begin; j < end; j++) {
                    List<TryCatchBlockNode> insnHandlers = this.handlers[j];
                    if (insnHandlers == null) {
                        insnHandlers = new ArrayList<>();
                        this.handlers[j] = insnHandlers;
                    }
                    insnHandlers.add(tcb);
                }
            }
            Subroutine main = new Subroutine(null, m.maxLocals, null);
            List<AbstractInsnNode> subroutineCalls = new ArrayList<>();
            Map<LabelNode, Subroutine> subroutineHeads = new HashMap<>();
            this.findSubroutine(0, main, subroutineCalls);
            while (!subroutineCalls.isEmpty()) {
                JumpInsnNode jsr = (JumpInsnNode) subroutineCalls.remove(0);
                Subroutine sub = subroutineHeads.get(jsr.label);
                if (sub == null) {
                    sub = new Subroutine(jsr.label, m.maxLocals, jsr);
                    subroutineHeads.put(jsr.label, sub);
                    this.findSubroutine(this.insns.indexOf(jsr.label), sub, subroutineCalls);
                } else {
                    sub.callers.add(jsr);
                }
            }
            for (int i = 0; i < this.n; i++) {
                if (this.subroutines[i] != null && this.subroutines[i].start == null) {
                    this.subroutines[i] = null;
                }
            }
            Frame<V> current = this.newFrame(m.maxLocals, m.maxStack);
            Frame<V> handler = this.newFrame(m.maxLocals, m.maxStack);
            current.setReturn(this.interpreter.newValue(Type.getReturnType(m.desc)));
            Type[] args = Type.getArgumentTypes(m.desc);
            int local = 0;
            if ((m.access & 8) == 0) {
                Type ctype = Type.getObjectType(owner);
                current.setLocal(local++, this.interpreter.newValue(ctype));
            }
            for (int ix = 0; ix < args.length; ix++) {
                current.setLocal(local++, this.interpreter.newValue(args[ix]));
                if (args[ix].getSize() == 2) {
                    current.setLocal(local++, this.interpreter.newValue(null));
                }
            }
            while (local < m.maxLocals) {
                current.setLocal(local++, this.interpreter.newValue(null));
            }
            this.merge(0, current, null);
            this.init(owner, m);
            while (this.top > 0) {
                int insn = this.queue[--this.top];
                Frame<V> f = this.frames[insn];
                Subroutine subroutine = this.subroutines[insn];
                this.queued[insn] = false;
                AbstractInsnNode insnNode = null;
                try {
                    insnNode = m.instructions.get(insn);
                    int insnOpcode = insnNode.getOpcode();
                    int insnType = insnNode.getType();
                    if (insnType != 8 && insnType != 15 && insnType != 14) {
                        current.init(f).execute(insnNode, this.interpreter);
                        subroutine = subroutine == null ? null : subroutine.copy();
                        if (insnNode instanceof JumpInsnNode) {
                            JumpInsnNode j = (JumpInsnNode) insnNode;
                            if (insnOpcode != 167 && insnOpcode != 168) {
                                this.merge(insn + 1, current, subroutine);
                                this.newControlFlowEdge(insn, insn + 1);
                            }
                            int jump = this.insns.indexOf(j.label);
                            if (insnOpcode == 168) {
                                this.merge(jump, current, new Subroutine(j.label, m.maxLocals, j));
                            } else {
                                this.merge(jump, current, subroutine);
                            }
                            this.newControlFlowEdge(insn, jump);
                        } else if (insnNode instanceof LookupSwitchInsnNode) {
                            LookupSwitchInsnNode lsi = (LookupSwitchInsnNode) insnNode;
                            int jump = this.insns.indexOf(lsi.dflt);
                            this.merge(jump, current, subroutine);
                            this.newControlFlowEdge(insn, jump);
                            for (int jx = 0; jx < lsi.labels.size(); jx++) {
                                LabelNode label = lsi.labels.get(jx);
                                jump = this.insns.indexOf(label);
                                this.merge(jump, current, subroutine);
                                this.newControlFlowEdge(insn, jump);
                            }
                        } else if (insnNode instanceof TableSwitchInsnNode) {
                            TableSwitchInsnNode tsi = (TableSwitchInsnNode) insnNode;
                            int jump = this.insns.indexOf(tsi.dflt);
                            this.merge(jump, current, subroutine);
                            this.newControlFlowEdge(insn, jump);
                            for (int jx = 0; jx < tsi.labels.size(); jx++) {
                                LabelNode label = tsi.labels.get(jx);
                                jump = this.insns.indexOf(label);
                                this.merge(jump, current, subroutine);
                                this.newControlFlowEdge(insn, jump);
                            }
                        } else if (insnOpcode == 169) {
                            if (subroutine == null) {
                                throw new AnalyzerException(insnNode, "RET instruction outside of a sub routine");
                            }
                            for (int intValue = 0; intValue < subroutine.callers.size(); intValue++) {
                                JumpInsnNode caller = subroutine.callers.get(intValue);
                                int call = this.insns.indexOf(caller);
                                if (this.frames[call] != null) {
                                    this.merge(call + 1, this.frames[call], current, this.subroutines[call], subroutine.access);
                                    this.newControlFlowEdge(insn, call + 1);
                                }
                            }
                        } else if (insnOpcode != 191 && (insnOpcode < 172 || insnOpcode > 177)) {
                            if (subroutine != null) {
                                if (insnNode instanceof VarInsnNode) {
                                    int var = ((VarInsnNode) insnNode).var;
                                    subroutine.access[var] = true;
                                    if (insnOpcode == 22 || insnOpcode == 24 || insnOpcode == 55 || insnOpcode == 57) {
                                        subroutine.access[var + 1] = true;
                                    }
                                } else if (insnNode instanceof IincInsnNode) {
                                    int var = ((IincInsnNode) insnNode).var;
                                    subroutine.access[var] = true;
                                }
                            }
                            this.merge(insn + 1, current, subroutine);
                            this.newControlFlowEdge(insn, insn + 1);
                        }
                    } else {
                        this.merge(insn + 1, f, subroutine);
                        this.newControlFlowEdge(insn, insn + 1);
                    }
                    List<TryCatchBlockNode> insnHandlers = this.handlers[insn];
                    if (insnHandlers != null) {
                        for (int intValue2 = 0; intValue2 < insnHandlers.size(); intValue2++) {
                            TryCatchBlockNode tcb = insnHandlers.get(intValue2);
                            Type type;
                            if (tcb.type == null) {
                                type = Type.getObjectType("java/lang/Throwable");
                            } else {
                                type = Type.getObjectType(tcb.type);
                            }
                            int jump = this.insns.indexOf(tcb.handler);
                            if (this.newControlFlowExceptionEdge(insn, tcb)) {
                                handler.init(f);
                                handler.clearStack();
                                handler.push(this.interpreter.newValue(type));
                                this.merge(jump, handler, subroutine);
                            }
                        }
                    }
                } catch (AnalyzerException exception) {
                    throw new AnalyzerException(exception.node, "Error at instruction " + insn + ": " + exception.getMessage(), exception);
                } catch (Exception exception2) {
                    throw new AnalyzerException(insnNode, "Error at instruction " + insn + ": " + exception2.getMessage(), exception2);
                }
            }
            return this.frames;
        }
    }

    private void findSubroutine(int insn, Subroutine sub, List<AbstractInsnNode> calls) throws AnalyzerException {
        while (insn >= 0 && insn < this.n) {
            if (this.subroutines[insn] != null) {
                return;
            }
            this.subroutines[insn] = sub.copy();
            AbstractInsnNode node = this.insns.get(insn);
            if (node instanceof JumpInsnNode) {
                if (node.getOpcode() == 168) {
                    calls.add(node);
                } else {
                    JumpInsnNode jnode = (JumpInsnNode) node;
                    this.findSubroutine(this.insns.indexOf(jnode.label), sub, calls);
                }
            } else if (node instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode tsnode = (TableSwitchInsnNode) node;
                this.findSubroutine(this.insns.indexOf(tsnode.dflt), sub, calls);
                for (int i = tsnode.labels.size() - 1; i >= 0; i--) {
                    LabelNode l = tsnode.labels.get(i);
                    this.findSubroutine(this.insns.indexOf(l), sub, calls);
                }
            } else if (node instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode lsnode = (LookupSwitchInsnNode) node;
                this.findSubroutine(this.insns.indexOf(lsnode.dflt), sub, calls);
                for (int i = lsnode.labels.size() - 1; i >= 0; i--) {
                    LabelNode l = lsnode.labels.get(i);
                    this.findSubroutine(this.insns.indexOf(l), sub, calls);
                }
            }
            List<TryCatchBlockNode> insnHandlers = this.handlers[insn];
            if (insnHandlers != null) {
                for (int i = 0; i < insnHandlers.size(); i++) {
                    TryCatchBlockNode tcb = insnHandlers.get(i);
                    this.findSubroutine(this.insns.indexOf(tcb.handler), sub, calls);
                }
            }
            switch(node.getOpcode()) {
                case 167:
                case 169:
                case 170:
                case 171:
                case 172:
                case 173:
                case 174:
                case 175:
                case 176:
                case 177:
                case 191:
                    return;
                case 168:
                case 178:
                case 179:
                case 180:
                case 181:
                case 182:
                case 183:
                case 184:
                case 185:
                case 186:
                case 187:
                case 188:
                case 189:
                case 190:
                default:
                    insn++;
            }
        }
        throw new AnalyzerException(null, "Execution can fall off end of the code");
    }

    public Frame<V>[] getFrames() {
        return this.frames;
    }

    public List<TryCatchBlockNode> getHandlers(int insn) {
        return this.handlers[insn];
    }

    protected void init(String owner, MethodNode m) throws AnalyzerException {
    }

    protected Frame<V> newFrame(int nLocals, int nStack) {
        return new Frame<>(nLocals, nStack);
    }

    protected Frame<V> newFrame(Frame<? extends V> src) {
        return new Frame<>(src);
    }

    protected void newControlFlowEdge(int insn, int successor) {
    }

    protected boolean newControlFlowExceptionEdge(int insn, int successor) {
        return true;
    }

    protected boolean newControlFlowExceptionEdge(int insn, TryCatchBlockNode tcb) {
        return this.newControlFlowExceptionEdge(insn, this.insns.indexOf(tcb.handler));
    }

    private void merge(int insn, Frame<V> frame, Subroutine subroutine) throws AnalyzerException {
        Frame<V> oldFrame = this.frames[insn];
        Subroutine oldSubroutine = this.subroutines[insn];
        boolean changes;
        if (oldFrame == null) {
            this.frames[insn] = this.newFrame(frame);
            changes = true;
        } else {
            changes = oldFrame.merge(frame, this.interpreter);
        }
        if (oldSubroutine == null) {
            if (subroutine != null) {
                this.subroutines[insn] = subroutine.copy();
                changes = true;
            }
        } else if (subroutine != null) {
            changes |= oldSubroutine.merge(subroutine);
        }
        if (changes && !this.queued[insn]) {
            this.queued[insn] = true;
            this.queue[this.top++] = insn;
        }
    }

    private void merge(int insn, Frame<V> beforeJSR, Frame<V> afterRET, Subroutine subroutineBeforeJSR, boolean[] access) throws AnalyzerException {
        Frame<V> oldFrame = this.frames[insn];
        Subroutine oldSubroutine = this.subroutines[insn];
        afterRET.merge(beforeJSR, access);
        boolean changes;
        if (oldFrame == null) {
            this.frames[insn] = this.newFrame(afterRET);
            changes = true;
        } else {
            changes = oldFrame.merge(afterRET, this.interpreter);
        }
        if (oldSubroutine != null && subroutineBeforeJSR != null) {
            changes |= oldSubroutine.merge(subroutineBeforeJSR);
        }
        if (changes && !this.queued[insn]) {
            this.queued[insn] = true;
            this.queue[this.top++] = insn;
        }
    }
}
