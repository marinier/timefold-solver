package org.optaplanner.core.impl.domain.common.accessor.gizmo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Member;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.domain.entity.PlanningPin;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.impl.domain.common.accessor.MemberAccessor;
import org.optaplanner.core.impl.testdata.domain.TestdataEntity;
import org.optaplanner.core.impl.testdata.domain.TestdataValue;
import org.optaplanner.core.impl.testdata.domain.gizmo.GizmoTestdataEntity;

class GizmoMemberAccessorImplementorTest {

    @Test
    void testGeneratedMemberAccessorForMethod() throws NoSuchMethodException {
        Member member = TestdataEntity.class.getMethod("getValue");
        MemberAccessor memberAccessor = GizmoMemberAccessorImplementor.createAccessorFor(member, PlanningVariable.class);
        assertThat(memberAccessor.getName()).isEqualTo("value");
        assertThat(memberAccessor.getType()).isEqualTo(TestdataValue.class);
        assertThat(memberAccessor.getSpeedNote()).isEqualTo("Fast access with generated bytecode");
        assertThat(memberAccessor.getDeclaringClass()).isEqualTo(TestdataEntity.class);
        assertThat(memberAccessor.supportSetter()).isTrue();
        assertThat(memberAccessor.getAnnotation(PlanningVariable.class)).isNotNull();

        TestdataEntity testdataEntity = new TestdataEntity();
        TestdataValue testdataValue1 = new TestdataValue("A");
        testdataEntity.setValue(testdataValue1);
        assertThat(memberAccessor.executeGetter(testdataEntity)).isEqualTo(testdataValue1);

        TestdataValue testdataValue2 = new TestdataValue("B");
        memberAccessor.executeSetter(testdataEntity, testdataValue2);
        assertThat(memberAccessor.executeGetter(testdataEntity)).isEqualTo(testdataValue2);
    }

    @Test
    void testGeneratedMemberAccessorForMethodWithoutSetter() throws NoSuchMethodException {
        Member member = GizmoTestdataEntity.class.getMethod("getId");
        MemberAccessor memberAccessor = GizmoMemberAccessorImplementor.createAccessorFor(member, PlanningId.class);
        assertThat(memberAccessor.getName()).isEqualTo("id");
        assertThat(memberAccessor.getType()).isEqualTo(String.class);
        assertThat(memberAccessor.getSpeedNote()).isEqualTo("Fast access with generated bytecode");
        assertThat(memberAccessor.getDeclaringClass()).isEqualTo(GizmoTestdataEntity.class);
        assertThat(memberAccessor.supportSetter()).isFalse();
        assertThat(memberAccessor.getAnnotation(PlanningId.class)).isNotNull();

        GizmoTestdataEntity testdataEntity = new GizmoTestdataEntity("A", null, false);
        assertThat(memberAccessor.executeGetter(testdataEntity)).isEqualTo("A");
    }

    @Test
    void testGeneratedMemberAccessorForField() throws NoSuchFieldException {
        Member member = GizmoTestdataEntity.class.getField("value");
        MemberAccessor memberAccessor = GizmoMemberAccessorImplementor.createAccessorFor(member, PlanningVariable.class);
        assertThat(memberAccessor.getName()).isEqualTo("value");
        assertThat(memberAccessor.getType()).isEqualTo(TestdataValue.class);
        assertThat(memberAccessor.getSpeedNote()).isEqualTo("Fast access with generated bytecode");
        assertThat(memberAccessor.getDeclaringClass()).isEqualTo(GizmoTestdataEntity.class);
        assertThat(memberAccessor.supportSetter()).isTrue();
        assertThat(memberAccessor.getAnnotation(PlanningVariable.class)).isNotNull();

        GizmoTestdataEntity testdataEntity = new GizmoTestdataEntity("A", null, false);
        TestdataValue testdataValue1 = new TestdataValue("A");
        testdataEntity.setValue(testdataValue1);
        assertThat(memberAccessor.executeGetter(testdataEntity)).isEqualTo(testdataValue1);

        TestdataValue testdataValue2 = new TestdataValue("B");
        memberAccessor.executeSetter(testdataEntity, testdataValue2);
        assertThat(memberAccessor.executeGetter(testdataEntity)).isEqualTo(testdataValue2);
    }

    @Test
    void testGeneratedMemberAccessorForPrimitiveField() throws NoSuchFieldException {
        Member member = GizmoTestdataEntity.class.getField("isPinned");
        MemberAccessor memberAccessor = GizmoMemberAccessorImplementor.createAccessorFor(member, PlanningPin.class);
        assertThat(memberAccessor.getName()).isEqualTo("isPinned");
        assertThat(memberAccessor.getType()).isEqualTo(boolean.class);
        assertThat(memberAccessor.getSpeedNote()).isEqualTo("Fast access with generated bytecode");
        assertThat(memberAccessor.getDeclaringClass()).isEqualTo(GizmoTestdataEntity.class);
        assertThat(memberAccessor.supportSetter()).isTrue();

        GizmoTestdataEntity testdataEntity = new GizmoTestdataEntity("A", null, false);
        assertThat(memberAccessor.executeGetter(testdataEntity)).isEqualTo(false);

        memberAccessor.executeSetter(testdataEntity, true);
        assertThat(memberAccessor.executeGetter(testdataEntity)).isEqualTo(true);
    }

    @Test
    void testGeneratedMemberAccessorSameClass() throws NoSuchMethodException {
        Member member = TestdataEntity.class.getMethod("getValue");
        MemberAccessor memberAccessor1 = GizmoMemberAccessorImplementor.createAccessorFor(member, PlanningVariable.class);
        MemberAccessor memberAccessor2 = GizmoMemberAccessorImplementor.createAccessorFor(member, PlanningVariable.class);

        assertThat(memberAccessor1.getClass()).isEqualTo(memberAccessor2.getClass());
    }

    @Test
    void testThrowsWhenGetterMethodHasParameters() throws NoSuchMethodException {
        Member member = GizmoTestdataEntity.class.getMethod("methodWithParameters", String.class);
        assertThatCode(() -> {
            GizmoMemberAccessorImplementor.createAccessorFor(member, PlanningVariable.class);
        }).hasMessage("The getterMethod (methodWithParameters) with a PlanningVariable annotation " +
                "must not have any parameters, but has parameters ([Ljava/lang/String;]).");
    }

    @Test
    void testThrowsWhenGetterMethodReturnVoid() throws NoSuchMethodException {
        Member member = GizmoTestdataEntity.class.getMethod("getVoid");
        assertThatCode(() -> {
            GizmoMemberAccessorImplementor.createAccessorFor(member, PlanningVariable.class);
        }).hasMessage("The getterMethod (getVoid) with a PlanningVariable annotation must have a non-void return type.");
    }

    @Test
    void testThrowsWhenReadMethodReturnVoid() throws NoSuchMethodException {
        Member member = GizmoTestdataEntity.class.getMethod("voidMethod");
        assertThatCode(() -> {
            GizmoMemberAccessorImplementor.createAccessorFor(member, PlanningVariable.class);
        }).hasMessage("The readMethod (voidMethod) with a PlanningVariable annotation must have a non-void return type.");
    }

    @Test
    void testGeneratedMemberAccessorForBooleanMethod() throws NoSuchMethodException {
        Member member = GizmoTestdataEntity.class.getMethod("isPinned");
        MemberAccessor memberAccessor = GizmoMemberAccessorImplementor.createAccessorFor(member, PlanningPin.class);
        assertThat(memberAccessor.getName()).isEqualTo("pinned");
        assertThat(memberAccessor.getType()).isEqualTo(boolean.class);
        assertThat(memberAccessor.getSpeedNote()).isEqualTo("Fast access with generated bytecode");
        assertThat(memberAccessor.getDeclaringClass()).isEqualTo(GizmoTestdataEntity.class);
        assertThat(memberAccessor.supportSetter()).isTrue();
        assertThat(memberAccessor.getAnnotation(PlanningPin.class)).isNotNull();

        GizmoTestdataEntity testdataEntity = new GizmoTestdataEntity("A", null, false);
        assertThat(memberAccessor.executeGetter(testdataEntity)).isEqualTo(false);

        memberAccessor.executeSetter(testdataEntity, true);
        assertThat(memberAccessor.executeGetter(testdataEntity)).isEqualTo(true);
    }

    @Test
    void testThrowsWhenGetBooleanReturnsNonBoolean() throws NoSuchMethodException {
        Member member = GizmoTestdataEntity.class.getMethod("isAMethodThatHasABadName");
        assertThatCode(() -> {
            GizmoMemberAccessorImplementor.createAccessorFor(member, PlanningVariable.class);
        }).hasMessage("The getterMethod (isAMethodThatHasABadName) with a PlanningVariable annotation must have a " +
                "primitive boolean return type but returns (L" +
                String.class.getName().replace('.', '/') + ";). Maybe rename the method (" +
                "getAMethodThatHasABadName)?");
    }
}
