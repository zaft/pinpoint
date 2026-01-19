/*
 * Copyright 2016 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.profiler.instrument;

import com.navercorp.pinpoint.bootstrap.interceptor.ReturnAroundInterceptor;
import com.navercorp.pinpoint.profiler.instrument.interceptor.InterceptorDefinition;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;


import java.util.List;
import java.util.Objects;

/**
 * @author jaehong.kim
 */
public class ASMMethodNodeAdapter {

    private final String declaringClassInternalName;
    private final MethodNode methodNode;
    private final ASMMethodVariables methodVariables;

    public ASMMethodNodeAdapter(final String declaringClassInternalName, final MethodNode methodNode) {
        if (declaringClassInternalName == null || methodNode == null) {
            throw new IllegalArgumentException("declaring class internal name and method annotation must not be null. class=" + declaringClassInternalName + ", methodNode=" + methodNode);
        }

        if (methodNode.instructions == null || methodNode.desc == null) {
            throw new IllegalArgumentException("method annotation's instructions or desc must not be null. class=" + declaringClassInternalName + ", method=" + methodNode.name + methodNode.desc);
        }

        this.declaringClassInternalName = declaringClassInternalName;
        this.methodNode = methodNode;
        this.methodVariables = new ASMMethodVariables(declaringClassInternalName, methodNode);
    }

    public MethodNode getMethodNode() {
        return this.methodNode;
    }

    public String getDeclaringClassInternalName() {
        return this.declaringClassInternalName;
    }

    // find interceptor local variable.
    public boolean hasInterceptor() {
        return this.methodVariables.hasInterceptor();
    }

    public String getName() {
        if (isConstructor()) {
            // simple class name.
            int index = this.declaringClassInternalName.lastIndexOf('/');
            if (index < 0) {
                return this.declaringClassInternalName;
            } else {
                return this.declaringClassInternalName.substring(index + 1);
            }
        }

        return this.methodNode.name;
    }

    public void setName(final String name) {
        if (isConstructor()) {
            // skip.
            return;
        }
        this.methodNode.name = name;
    }

    public String[] getParameterTypes() {
        return this.methodVariables.getParameterTypes();
    }

    public String[] getParameterNames() {
        return this.methodVariables.getParameterNames();
    }

    public String getReturnType() {
        return this.methodVariables.getReturnType();
    }

    public int getAccess() {
        return this.methodNode.access;
    }

    public void setAccess(final int access) {
        this.methodNode.access = access;
    }

    public boolean isConstructor() {
        return this.methodNode.name.equals("<init>");
    }

    public String getDesc() {
        return this.methodNode.desc;
    }

    public int getLineNumber() {
        AbstractInsnNode node = this.methodNode.instructions.getFirst();
        while (node != null) {
            if (node.getType() == AbstractInsnNode.LINE) {
                return ((LineNumberNode) node).line;
            }
            node = node.getNext();
        }

        return 0;
    }

    public List<String> getExceptions() {
        return this.methodNode.exceptions;
    }

    public String getSignature() {
        return this.methodNode.signature;
    }

    public String getLongName() {
        return this.declaringClassInternalName + "/" + getName() + getDesc();
    }

    public boolean isStatic() {
        return (this.methodNode.access & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isAbstract() {
        return (this.methodNode.access & Opcodes.ACC_ABSTRACT) != 0;
    }

    public boolean isPrivate() {
        return (this.methodNode.access & Opcodes.ACC_PRIVATE) != 0;
    }

    public boolean isNative() {
        return (this.methodNode.access & Opcodes.ACC_NATIVE) != 0;
    }

    public boolean hasAnnotation(final Class<?> annotationClass) {
        if (annotationClass == null) {
            return false;
        }

        final String desc = Type.getDescriptor(annotationClass);
        return hasAnnotation(desc, this.methodNode.invisibleAnnotations) || hasAnnotation(desc, this.methodNode.visibleAnnotations);
    }

    private boolean hasAnnotation(final String annotationClassDesc, final List<AnnotationNode> annotationNodes) {
        if (annotationClassDesc == null || annotationNodes == null) {
            return false;
        }

        for (AnnotationNode annotation : annotationNodes) {
            if (annotation.desc != null && annotation.desc.equals(annotationClassDesc)) {
                return true;
            }
        }

        return false;
    }

    public void addDelegator(final String superClassInternalName) {
        Objects.requireNonNull(superClassInternalName, "superClassInternalName");

        final InsnList instructions = this.methodNode.instructions;
        if (isStatic()) {
            this.methodVariables.initLocalVariables(instructions);
            // load parameters
            this.methodVariables.loadArgs(instructions);
            // invoke static
            instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, superClassInternalName, this.methodNode.name, this.methodNode.desc, false));
        } else {
            this.methodVariables.initLocalVariables(instructions);
            // load this
            this.methodVariables.loadVar(instructions, 0);
            // load parameters
            this.methodVariables.loadArgs(instructions);
            // invoke special
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, superClassInternalName, this.methodNode.name, this.methodNode.desc, false));
        }
        // return
        this.methodVariables.returnValue(instructions);
    }

    public void rename(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("methodName");
        }

        final ASMMethodInsnNodeRemapper.Builder remapBuilder = new ASMMethodInsnNodeRemapper.Builder();
        remapBuilder.addFilter(this.declaringClassInternalName, this.methodNode.name, this.methodNode.desc);
        remapBuilder.setName(name);
        // change recursive call.
        ASMMethodInsnNodeRemapper remapper = remapBuilder.build();
        remapMethodInsnNode(remapper);

        // change name.
        this.methodNode.name = name;
    }

    public void remapMethodInsnNode(final ASMMethodInsnNodeRemapper remapper) {
        AbstractInsnNode insnNode = this.methodNode.instructions.getFirst();
        while (insnNode != null) {
            if (insnNode instanceof MethodInsnNode) {
                final MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                remapper.mapping(methodInsnNode);
            }
            insnNode = insnNode.getNext();
        }
    }

    public void remapLocalVariables(final String name, final String desc) {
        if (methodNode.localVariables == null) {
            return;
        }

        final List<LocalVariableNode> localVariableNodes = methodNode.localVariables;
        for (LocalVariableNode node : localVariableNodes) {
            if (node.name.equals(name)) {
                node.desc = desc;
            }
        }
    }

    private void initInterceptorLocalVariables(final int interceptorId, final InterceptorDefinition interceptorDefinition, final int apiId) {
        final InsnList instructions = new InsnList();
        if (this.methodVariables.initInterceptorLocalVariables(instructions, interceptorId, interceptorDefinition, apiId)) {
            // if first time.
            this.methodNode.instructions.insertBefore(this.methodVariables.getEnterInsnNode(), instructions);
        }
    }

    public void addBeforeInterceptor(final int interceptorId, final InterceptorDefinition interceptorDefinition, final int apiId) {
        initInterceptorLocalVariables(interceptorId, interceptorDefinition, apiId);

        final InsnList instructions = new InsnList();
        this.methodVariables.loadInterceptorLocalVariables(instructions, interceptorDefinition, false);

        final String description = Type.getMethodDescriptor(interceptorDefinition.getBeforeMethod());
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(interceptorDefinition.getInterceptorBaseClass()), "before", description, true));
        if(ReturnAroundInterceptor.class.isAssignableFrom(interceptorDefinition.getInterceptorClass()) ) {
            test(instructions, interceptorDefinition);
        }
        this.methodNode.instructions.insertBefore(this.methodVariables.getEnterInsnNode(), instructions);
    }

    public void test(InsnList instructions, InterceptorDefinition interceptorDefinition) {
        LabelNode continueOriginal = new LabelNode();  // 继续执行原有逻辑
        LabelNode returnValue = new LabelNode();       // 返回值不为空，直接返回
        // 4. 复制返回值，一份用于判断，一份用于返回
        instructions.add(new InsnNode(Opcodes.DUP));
        // 5. 判断是否为空
        instructions.add(new JumpInsnNode(Opcodes.IFNULL, continueOriginal));
        // 6. 如果不为空，跳转到返回逻辑
        instructions.add(new JumpInsnNode(Opcodes.GOTO, returnValue));
        // 7. 添加返回标签位置
        instructions.add(returnValue);
        Type returnType = Type.getReturnType(interceptorDefinition.getBeforeMethod());
        instructions.add(new InsnNode(getReturnOpcode(returnType)));

        // 9. 添加继续执行原有逻辑的标签
        instructions.add(continueOriginal);
        // 10. 弹出栈顶的null值（IFNULL判断后，栈顶是检查方法的返回值，已经知道是null）
        instructions.add(new InsnNode(Opcodes.POP));
    }

    /**
     * 获取返回操作码
     */
    private static int getReturnOpcode(Type type) {
        switch (type.getSort()) {
            case Type.VOID:
                return Opcodes.RETURN;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return Opcodes.IRETURN;
            case Type.LONG:
                return Opcodes.LRETURN;
            case Type.FLOAT:
                return Opcodes.FRETURN;
            case Type.DOUBLE:
                return Opcodes.DRETURN;
            case Type.ARRAY:
            case Type.OBJECT:
                return Opcodes.ARETURN;
            default:
                return Opcodes.ARETURN;
        }
    }



    public void addAfterInterceptor(final int interceptorId, final InterceptorDefinition interceptorDefinition, final int apiId) {
        initInterceptorLocalVariables(interceptorId, interceptorDefinition, apiId);

        // add try catch block.
        final ASMTryCatch tryCatch = new ASMTryCatch(this.methodNode);
        this.methodNode.instructions.insertBefore(this.methodVariables.getEnterInsnNode(), tryCatch.getStartLabelNode());
        this.methodNode.instructions.insert(this.methodVariables.getExitInsnNode(), tryCatch.getEndLabelNode());

        // find return.
        AbstractInsnNode insnNode = this.methodNode.instructions.getFirst();
        while (insnNode != null) {
            final int opcode = insnNode.getOpcode();
            if (this.methodVariables.isReturnCode(opcode)) {
                final InsnList instructions = new InsnList();
                this.methodVariables.storeResultVar(instructions, opcode);
                invokeAfterInterceptor(instructions, interceptorDefinition, false);
                this.methodNode.instructions.insertBefore(insnNode, instructions);
            }
            insnNode = insnNode.getNext();
        }

        // try catch handler.
        InsnList instructions = new InsnList();
        this.methodVariables.storeThrowableVar(instructions);
        invokeAfterInterceptor(instructions, interceptorDefinition, true);
        // throw exception.
        this.methodVariables.loadInterceptorThrowVar(instructions);
        this.methodNode.instructions.insert(tryCatch.getEndLabelNode(), instructions);
        tryCatch.sort();
    }

    private void invokeAfterInterceptor(final InsnList instructions, final InterceptorDefinition interceptorDefinition, final boolean throwable) {
        this.methodVariables.loadInterceptorLocalVariables(instructions, interceptorDefinition, true);
        final String description = Type.getMethodDescriptor(interceptorDefinition.getAfterMethod());
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(interceptorDefinition.getInterceptorBaseClass()), "after", description, true));
    }
}