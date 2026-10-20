package org.opendatadiscovery.oddplatform.repository.reactive;

import java.util.List;
import org.jooq.impl.DSL;
import org.opendatadiscovery.oddplatform.model.tables.pojos.FieldLineageEdgePojo;
import org.opendatadiscovery.oddplatform.model.tables.records.FieldLineageEdgeRecord;
import org.opendatadiscovery.oddplatform.repository.util.JooqQueryHelper;
import org.opendatadiscovery.oddplatform.repository.util.JooqReactiveOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import static org.opendatadiscovery.oddplatform.model.Tables.FIELD_LINEAGE_EDGE;

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
}
