package ai.timefold.solver.constraint.streams.bavet.quad;

import static ai.timefold.solver.constraint.streams.bavet.common.GroupNodeConstructor.fourKeysGroupBy;
import static ai.timefold.solver.constraint.streams.bavet.common.GroupNodeConstructor.oneKeyGroupBy;
import static ai.timefold.solver.constraint.streams.bavet.common.GroupNodeConstructor.threeKeysGroupBy;
import static ai.timefold.solver.constraint.streams.bavet.common.GroupNodeConstructor.twoKeysGroupBy;
import static ai.timefold.solver.constraint.streams.bavet.common.GroupNodeConstructor.zeroKeysGroupBy;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Function;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.bi.BavetAbstractBiConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.BavetScoringConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeBiConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeQuadConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeTriConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeUniConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeBiConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeQuadConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeTriConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;
import ai.timefold.solver.constraint.streams.bavet.tri.BavetAbstractTriConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.uni.BavetAbstractUniConstraintStream;
import ai.timefold.solver.constraint.streams.common.RetrievalSemantics;
import ai.timefold.solver.constraint.streams.common.ScoreImpactType;
import ai.timefold.solver.constraint.streams.common.penta.PentaJoinerComber;
import ai.timefold.solver.constraint.streams.common.quad.InnerQuadConstraintStream;
import ai.timefold.solver.constraint.streams.common.quad.QuadConstraintBuilderImpl;
import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintStream;
import ai.timefold.solver.core.api.score.stream.penta.PentaJoiner;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintStream;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

public abstract class BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractConstraintStream<Solution_>
        implements InnerQuadConstraintStream<A, B, C, D> {

    protected BavetAbstractQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> parent) {
        super(constraintFactory, parent);
    }

    protected BavetAbstractQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            RetrievalSemantics retrievalSemantics) {
        super(constraintFactory, retrievalSemantics);
    }

    // ************************************************************************
    // Filter
    // ************************************************************************

    @Override
    public BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> filter(QuadPredicate<A, B, C, D> predicate) {
        return shareAndAddChild(new BavetFilterQuadConstraintStream<>(constraintFactory, this, predicate));
    }

    // ************************************************************************
    // If (not) exists
    // ************************************************************************

    @SafeVarargs
    @Override
    public final <E> QuadConstraintStream<A, B, C, D> ifExists(Class<E> otherClass, PentaJoiner<A, B, C, D, E>... joiners) {
        if (getRetrievalSemantics() == RetrievalSemantics.STANDARD) {
            return ifExists(constraintFactory.forEach(otherClass), joiners);
        } else {
            // Calls fromUnfiltered() for backward compatibility only
            return ifExists(constraintFactory.fromUnfiltered(otherClass), joiners);
        }
    }

    @SafeVarargs
    @Override
    public final <E> QuadConstraintStream<A, B, C, D> ifExistsIncludingUnassigned(Class<E> otherClass,
            PentaJoiner<A, B, C, D, E>... joiners) {
        if (getRetrievalSemantics() == RetrievalSemantics.STANDARD) {
            return ifExists(constraintFactory.forEachIncludingUnassigned(otherClass), joiners);
        } else {
            return ifExists(constraintFactory.fromUnfiltered(otherClass), joiners);
        }
    }

    @SafeVarargs
    public final <E> QuadConstraintStream<A, B, C, D> ifExists(UniConstraintStream<E> otherStream,
            PentaJoiner<A, B, C, D, E>... joiners) {
        return ifExistsOrNot(true, otherStream, joiners);
    }

    @SafeVarargs
    @Override
    public final <E> QuadConstraintStream<A, B, C, D> ifNotExists(Class<E> otherClass, PentaJoiner<A, B, C, D, E>... joiners) {
        if (getRetrievalSemantics() == RetrievalSemantics.STANDARD) {
            return ifNotExists(constraintFactory.forEach(otherClass), joiners);
        } else {
            // Calls fromUnfiltered() for backward compatibility only
            return ifNotExists(constraintFactory.fromUnfiltered(otherClass), joiners);
        }
    }

    @SafeVarargs
    @Override
    public final <E> QuadConstraintStream<A, B, C, D> ifNotExistsIncludingUnassigned(Class<E> otherClass,
            PentaJoiner<A, B, C, D, E>... joiners) {
        if (getRetrievalSemantics() == RetrievalSemantics.STANDARD) {
            return ifNotExists(constraintFactory.forEachIncludingUnassigned(otherClass), joiners);
        } else {
            return ifNotExists(constraintFactory.fromUnfiltered(otherClass), joiners);
        }
    }

    @SafeVarargs
    public final <E> QuadConstraintStream<A, B, C, D> ifNotExists(UniConstraintStream<E> otherStream,
            PentaJoiner<A, B, C, D, E>... joiners) {
        return ifExistsOrNot(false, otherStream, joiners);
    }

    private <E> QuadConstraintStream<A, B, C, D> ifExistsOrNot(boolean shouldExist,
            UniConstraintStream<E> otherStream, PentaJoiner<A, B, C, D, E>[] joiners) {
        var other = (BavetAbstractUniConstraintStream<Solution_, E>) otherStream;
        var joinerComber = PentaJoinerComber.comb(joiners);
        var parentBridgeD = other.shareAndAddChild(new BavetForeBridgeUniConstraintStream<>(constraintFactory, other));
        return constraintFactory.share(
                new BavetIfExistsQuadConstraintStream<>(constraintFactory, this, parentBridgeD,
                        shouldExist, joinerComber.getMergedJoiner(), joinerComber.getMergedFiltering()),
                childStreamList::add);
    }

    // ************************************************************************
    // Group by
    // ************************************************************************

    @Override
    public <ResultContainer_, Result_> UniConstraintStream<Result_> groupBy(
            QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<UniTuple<Result_>> nodeConstructor =
                zeroKeysGroupBy(collector, Group0Mapping1CollectorQuadNode::new);
        return buildUniGroupBy(nodeConstructor);
    }

    private <NewA> UniConstraintStream<NewA> buildUniGroupBy(GroupNodeConstructor<UniTuple<NewA>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetUniGroupQuadConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeUniConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_> BiConstraintStream<ResultA_, ResultB_> groupBy(
            QuadConstraintCollector<A, B, C, D, ResultContainerA_, ResultA_> collectorA,
            QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB) {
        GroupNodeConstructor<BiTuple<ResultA_, ResultB_>> nodeConstructor =
                zeroKeysGroupBy(collectorA, collectorB, Group0Mapping2CollectorQuadNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    private <NewA, NewB> BiConstraintStream<NewA, NewB>
            buildBiGroupBy(GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetBiGroupQuadConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<ResultA_, ResultB_, ResultC_>
            groupBy(QuadConstraintCollector<A, B, C, D, ResultContainerA_, ResultA_> collectorA,
                    QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC) {
        GroupNodeConstructor<TriTuple<ResultA_, ResultB_, ResultC_>> nodeConstructor =
                zeroKeysGroupBy(collectorA, collectorB, collectorC, Group0Mapping3CollectorQuadNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    private <NewA, NewB, NewC> TriConstraintStream<NewA, NewB, NewC>
            buildTriGroupBy(GroupNodeConstructor<TriTuple<NewA, NewB, NewC>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetTriGroupQuadConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultContainerA_, ResultA_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_>
            groupBy(QuadConstraintCollector<A, B, C, D, ResultContainerA_, ResultA_> collectorA,
                    QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC,
                    QuadConstraintCollector<A, B, C, D, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<ResultA_, ResultB_, ResultC_, ResultD_>> nodeConstructor =
                zeroKeysGroupBy(collectorA, collectorB, collectorC, collectorD, Group0Mapping4CollectorQuadNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    private <NewA, NewB, NewC, NewD> QuadConstraintStream<NewA, NewB, NewC, NewD>
            buildQuadGroupBy(GroupNodeConstructor<QuadTuple<NewA, NewB, NewC, NewD>> nodeConstructor) {
        var stream = shareAndAddChild(new BavetQuadGroupQuadConstraintStream<>(constraintFactory, this, nodeConstructor));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <GroupKey_> UniConstraintStream<GroupKey_> groupBy(QuadFunction<A, B, C, D, GroupKey_> groupKeyMapping) {
        GroupNodeConstructor<UniTuple<GroupKey_>> nodeConstructor =
                oneKeyGroupBy(groupKeyMapping, Group1Mapping0CollectorQuadNode::new);
        return buildUniGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_>
            TriConstraintStream<GroupKey_, ResultB_, ResultC_> groupBy(QuadFunction<A, B, C, D, GroupKey_> groupKeyMapping,
                    QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC) {
        GroupNodeConstructor<TriTuple<GroupKey_, ResultB_, ResultC_>> nodeConstructor =
                oneKeyGroupBy(groupKeyMapping, collectorB, collectorC, Group1Mapping2CollectorQuadNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainerB_, ResultB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKey_, ResultB_, ResultC_, ResultD_>
            groupBy(QuadFunction<A, B, C, D, GroupKey_> groupKeyMapping,
                    QuadConstraintCollector<A, B, C, D, ResultContainerB_, ResultB_> collectorB,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC,
                    QuadConstraintCollector<A, B, C, D, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKey_, ResultB_, ResultC_, ResultD_>> nodeConstructor =
                oneKeyGroupBy(groupKeyMapping, collectorB, collectorC, collectorD, Group1Mapping3CollectorQuadNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKey_, ResultContainer_, Result_> BiConstraintStream<GroupKey_, Result_> groupBy(
            QuadFunction<A, B, C, D, GroupKey_> groupKeyMapping,
            QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<BiTuple<GroupKey_, Result_>> nodeConstructor =
                oneKeyGroupBy(groupKeyMapping, collector, Group1Mapping1CollectorQuadNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_> BiConstraintStream<GroupKeyA_, GroupKeyB_> groupBy(
            QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping,
            QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping) {
        GroupNodeConstructor<BiTuple<GroupKeyA_, GroupKeyB_>> nodeConstructor =
                twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, Group2Mapping0CollectorQuadNode::new);
        return buildBiGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainer_, Result_> TriConstraintStream<GroupKeyA_, GroupKeyB_, Result_> groupBy(
            QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping,
            QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
            QuadConstraintCollector<A, B, C, D, ResultContainer_, Result_> collector) {
        GroupNodeConstructor<TriTuple<GroupKeyA_, GroupKeyB_, Result_>> nodeConstructor =
                twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, collector, Group2Mapping1CollectorQuadNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, ResultContainerC_, ResultC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_> groupBy(
                    QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping,
                    QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
                    QuadConstraintCollector<A, B, C, D, ResultContainerC_, ResultC_> collectorC,
                    QuadConstraintCollector<A, B, C, D, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, ResultC_, ResultD_>> nodeConstructor =
                twoKeysGroupBy(groupKeyAMapping, groupKeyBMapping, collectorC, collectorD,
                        Group2Mapping2CollectorQuadNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_> TriConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_> groupBy(
            QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping, QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
            QuadFunction<A, B, C, D, GroupKeyC_> groupKeyCMapping) {
        GroupNodeConstructor<TriTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_>> nodeConstructor =
                threeKeysGroupBy(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, Group3Mapping0CollectorQuadNode::new);
        return buildTriGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultContainerD_, ResultD_>
            QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_>
            groupBy(QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping,
                    QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
                    QuadFunction<A, B, C, D, GroupKeyC_> groupKeyCMapping,
                    QuadConstraintCollector<A, B, C, D, ResultContainerD_, ResultD_> collectorD) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_, ResultD_>> nodeConstructor =
                threeKeysGroupBy(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, collectorD,
                        Group3Mapping1CollectorQuadNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    @Override
    public <GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_> QuadConstraintStream<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>
            groupBy(QuadFunction<A, B, C, D, GroupKeyA_> groupKeyAMapping,
                    QuadFunction<A, B, C, D, GroupKeyB_> groupKeyBMapping,
                    QuadFunction<A, B, C, D, GroupKeyC_> groupKeyCMapping,
                    QuadFunction<A, B, C, D, GroupKeyD_> groupKeyDMapping) {
        GroupNodeConstructor<QuadTuple<GroupKeyA_, GroupKeyB_, GroupKeyC_, GroupKeyD_>> nodeConstructor =
                fourKeysGroupBy(groupKeyAMapping, groupKeyBMapping, groupKeyCMapping, groupKeyDMapping,
                        Group4Mapping0CollectorQuadNode::new);
        return buildQuadGroupBy(nodeConstructor);
    }

    // ************************************************************************
    // Operations with duplicate tuple possibility
    // ************************************************************************

    @Override
    public QuadConstraintStream<A, B, C, D> distinct() {
        if (guaranteesDistinct()) {
            return this;
        } else {
            return groupBy(ConstantLambdaUtils.quadPickFirst(),
                    ConstantLambdaUtils.quadPickSecond(),
                    ConstantLambdaUtils.quadPickThird(),
                    ConstantLambdaUtils.quadPickFourth());
        }
    }

    @Override
    public QuadConstraintStream<A, B, C, D> concat(UniConstraintStream<A> otherStream) {
        var other = (BavetAbstractUniConstraintStream<Solution_, A>) otherStream;
        var leftBridge = new BavetForeBridgeQuadConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeUniConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetConcatQuadConstraintStream<>(constraintFactory, leftBridge, rightBridge);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public QuadConstraintStream<A, B, C, D> concat(BiConstraintStream<A, B> otherStream) {
        var other = (BavetAbstractBiConstraintStream<Solution_, A, B>) otherStream;
        var leftBridge = new BavetForeBridgeQuadConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeBiConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetConcatQuadConstraintStream<>(constraintFactory, leftBridge, rightBridge);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public QuadConstraintStream<A, B, C, D> concat(TriConstraintStream<A, B, C> otherStream) {
        var other = (BavetAbstractTriConstraintStream<Solution_, A, B, C>) otherStream;
        var leftBridge = new BavetForeBridgeQuadConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeTriConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetConcatQuadConstraintStream<>(constraintFactory, leftBridge, rightBridge);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public QuadConstraintStream<A, B, C, D> concat(QuadConstraintStream<A, B, C, D> otherStream) {
        var other = (BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>) otherStream;
        var leftBridge = new BavetForeBridgeQuadConstraintStream<>(constraintFactory, this);
        var rightBridge = new BavetForeBridgeQuadConstraintStream<>(constraintFactory, other);
        var concatStream = new BavetConcatQuadConstraintStream<>(constraintFactory, leftBridge, rightBridge);
        return constraintFactory.share(concatStream, concatStream_ -> {
            // Connect the bridges upstream
            getChildStreamList().add(leftBridge);
            other.getChildStreamList().add(rightBridge);
        });
    }

    @Override
    public <ResultA_> UniConstraintStream<ResultA_> map(QuadFunction<A, B, C, D, ResultA_> mapping) {
        var stream = shareAndAddChild(new BavetUniMapQuadConstraintStream<>(constraintFactory, this, mapping));
        return constraintFactory.share(new BavetAftBridgeUniConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_> BiConstraintStream<ResultA_, ResultB_> map(QuadFunction<A, B, C, D, ResultA_> mappingA,
            QuadFunction<A, B, C, D, ResultB_> mappingB) {
        var stream = shareAndAddChild(new BavetBiMapQuadConstraintStream<>(constraintFactory, this, mappingA, mappingB));
        return constraintFactory.share(new BavetAftBridgeBiConstraintStream<>(constraintFactory, stream), stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_, ResultC_> TriConstraintStream<ResultA_, ResultB_, ResultC_> map(
            QuadFunction<A, B, C, D, ResultA_> mappingA, QuadFunction<A, B, C, D, ResultB_> mappingB,
            QuadFunction<A, B, C, D, ResultC_> mappingC) {
        var stream = shareAndAddChild(
                new BavetTriMapQuadConstraintStream<>(constraintFactory, this, mappingA, mappingB, mappingC));
        return constraintFactory.share(new BavetAftBridgeTriConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultA_, ResultB_, ResultC_, ResultD_> QuadConstraintStream<ResultA_, ResultB_, ResultC_, ResultD_> map(
            QuadFunction<A, B, C, D, ResultA_> mappingA, QuadFunction<A, B, C, D, ResultB_> mappingB,
            QuadFunction<A, B, C, D, ResultC_> mappingC, QuadFunction<A, B, C, D, ResultD_> mappingD) {
        var stream = shareAndAddChild(
                new BavetQuadMapQuadConstraintStream<>(constraintFactory, this, mappingA, mappingB, mappingC, mappingD));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    @Override
    public <ResultD_> QuadConstraintStream<A, B, C, ResultD_> flattenLast(Function<D, Iterable<ResultD_>> mapping) {
        var stream = shareAndAddChild(new BavetFlattenLastQuadConstraintStream<>(constraintFactory, this, mapping));
        return constraintFactory.share(new BavetAftBridgeQuadConstraintStream<>(constraintFactory, stream),
                stream::setAftBridge);
    }

    // ************************************************************************
    // Penalize/reward
    // ************************************************************************

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            ToIntQuadFunction<A, B, C, D> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringQuadConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, constraintWeight, scoreImpactType);
    }

    private <Score_ extends Score<Score_>> QuadConstraintBuilderImpl<A, B, C, D, Score_> newTerminator(
            BavetScoringConstraintStream<Solution_> stream, Score_ constraintWeight, ScoreImpactType impactType) {
        return new QuadConstraintBuilderImpl<>(
                (constraintPackage, constraintName, constraintWeight_, impactType_, justificationMapping,
                        indictedObjectsMapping) -> buildConstraint(constraintPackage, constraintName, constraintWeight_,
                                impactType_, justificationMapping, indictedObjectsMapping, stream),
                impactType, constraintWeight);
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            ToLongQuadFunction<A, B, C, D> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringQuadConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, constraintWeight, scoreImpactType);
    }

    @Override
    public <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher, ScoreImpactType scoreImpactType) {
        var stream = shareAndAddChild(new BavetScoringQuadConstraintStream<>(constraintFactory, this, matchWeigher));
        return newTerminator(stream, constraintWeight, scoreImpactType);
    }

    @Override
    protected final PentaFunction<A, B, C, D, Score<?>, DefaultConstraintJustification> getDefaultJustificationMapping() {
        return InnerQuadConstraintStream.createDefaultJustificationMapping();
    }

    @Override
    protected final QuadFunction<A, B, C, D, Collection<?>> getDefaultIndictedObjectsMapping() {
        return InnerQuadConstraintStream.createDefaultIndictedObjectsMapping();
    }

}
