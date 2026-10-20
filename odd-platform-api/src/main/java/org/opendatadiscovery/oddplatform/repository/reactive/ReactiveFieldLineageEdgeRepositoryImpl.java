package org.opendatadiscovery.oddplatform.repository.reactive;

import java.util.Collection;
import java.util.List;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.opendatadiscovery.oddplatform.dto.DataEntityStatusDto;
import org.opendatadiscovery.oddplatform.dto.FieldLineageEdgeDto;
import org.opendatadiscovery.oddplatform.dto.RelationshipTypeDto;
import org.opendatadiscovery.oddplatform.model.tables.pojos.FieldLineageEdgePojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.RelationshipsPojo;
import org.opendatadiscovery.oddplatform.model.tables.records.FieldLineageEdgeRecord;
import org.opendatadiscovery.oddplatform.repository.util.JooqQueryHelper;
import org.opendatadiscovery.oddplatform.repository.util.JooqReactiveOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.opendatadiscovery.oddplatform.model.Tables.DATA_ENTITY;
import static org.opendatadiscovery.oddplatform.model.Tables.FIELD_LINEAGE_EDGE;
import static org.opendatadiscovery.oddplatform.model.Tables.RELATIONSHIPS;

@Repository
public class ReactiveFieldLineageEdgeRepositoryImpl
    extends ReactiveAbstractCRUDRepository<FieldLineageEdgeRecord, FieldLineageEdgePojo>
    implements ReactiveFieldLineageEdgeRepository {

    public ReactiveFieldLineageEdgeRepositoryImpl(final JooqReactiveOperations jooqReactiveOperations,
                                                  final JooqQueryHelper jooqQueryHelper) {
        super(jooqReactiveOperations, jooqQueryHelper, FIELD_LINEAGE_EDGE, FieldLineageEdgePojo.class);
    }

    @Override
    public Mono<List<FieldLineageEdgePojo>> findByRelationshipIds(final List<Long> relationshipIds) {
        if (relationshipIds.isEmpty()) {
            return Mono.just(List.of());
        }
        return jooqReactiveOperations.flux(DSL.selectFrom(FIELD_LINEAGE_EDGE)
                .where(FIELD_LINEAGE_EDGE.RELATIONSHIP_ID.in(relationshipIds))
                .orderBy(FIELD_LINEAGE_EDGE.ID))
            .map(r -> r.into(FieldLineageEdgePojo.class))
            .collectList();
    }

    @Override
    public Mono<Void> deleteByRelationshipIds(final List<Long> relationshipIds) {
        if (relationshipIds.isEmpty()) {
            return Mono.empty();
        }
        return jooqReactiveOperations.mono(DSL.deleteFrom(FIELD_LINEAGE_EDGE)
                .where(FIELD_LINEAGE_EDGE.RELATIONSHIP_ID.in(relationshipIds)))
            .then();
    }

    @Override
    public Flux<FieldLineageEdgeDto> findByFieldOddrn(final String fieldOddrn) {
        return findLive(FIELD_LINEAGE_EDGE.OUTPUT_FIELD_ODDRN.eq(fieldOddrn)
            .or(DSL.val(fieldOddrn).eq(DSL.any(FIELD_LINEAGE_EDGE.INPUT_FIELD_ODDRNS))));
    }

    @Override
    public Flux<FieldLineageEdgeDto> findByDatasetOddrns(final Collection<String> datasetOddrns,
                                                        final boolean asTarget) {
        if (datasetOddrns.isEmpty()) {
            return Flux.empty();
        }
        return findLive(asTarget
            ? RELATIONSHIPS.TARGET_DATASET_ODDRN.in(datasetOddrns)
            : RELATIONSHIPS.SOURCE_DATASET_ODDRN.in(datasetOddrns));
    }

    private Flux<FieldLineageEdgeDto> findLive(final Condition condition) {
        return jooqReactiveOperations.flux(DSL.select(FIELD_LINEAGE_EDGE.fields())
                .select(RELATIONSHIPS.fields())
                .from(FIELD_LINEAGE_EDGE)
                .join(RELATIONSHIPS).on(RELATIONSHIPS.ID.eq(FIELD_LINEAGE_EDGE.RELATIONSHIP_ID))
                .join(DATA_ENTITY).on(DATA_ENTITY.ID.eq(RELATIONSHIPS.DATA_ENTITY_ID))
                .where(condition)
                .and(RELATIONSHIPS.RELATIONSHIP_TYPE.eq(RelationshipTypeDto.FIELD_LINEAGE.name()))
                .and(DATA_ENTITY.STATUS.ne(DataEntityStatusDto.DELETED.getId()))
                .orderBy(FIELD_LINEAGE_EDGE.ID))
            .map(r -> new FieldLineageEdgeDto(
                r.into(FIELD_LINEAGE_EDGE).into(FieldLineageEdgePojo.class),
                r.into(RELATIONSHIPS).into(RelationshipsPojo.class)));
    }
}
