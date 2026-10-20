package org.opendatadiscovery.oddplatform.repository.reactive;

import java.util.List;
import org.opendatadiscovery.oddplatform.model.tables.pojos.FieldLineageEdgePojo;
import reactor.core.publisher.Mono;

public interface ReactiveFieldLineageEdgeRepository extends ReactiveCRUDRepository<FieldLineageEdgePojo> {
    Mono<List<FieldLineageEdgePojo>> findByRelationshipIds(final List<Long> relationshipIds);

    Mono<Void> deleteByRelationshipIds(final List<Long> relationshipIds);
}
