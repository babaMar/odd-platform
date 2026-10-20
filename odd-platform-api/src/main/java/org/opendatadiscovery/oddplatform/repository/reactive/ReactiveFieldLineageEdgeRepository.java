package org.opendatadiscovery.oddplatform.repository.reactive;

import java.util.Collection;
import java.util.List;
import org.opendatadiscovery.oddplatform.dto.FieldLineageEdgeDto;
import org.opendatadiscovery.oddplatform.model.tables.pojos.FieldLineageEdgePojo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveFieldLineageEdgeRepository extends ReactiveCRUDRepository<FieldLineageEdgePojo> {
    Mono<List<FieldLineageEdgePojo>> findByRelationshipIds(final List<Long> relationshipIds);

    Mono<Void> deleteByRelationshipIds(final List<Long> relationshipIds);

    /**
     * Edges of live relationships where the field is the output or one of the inputs.
     */
    Flux<FieldLineageEdgeDto> findByFieldOddrn(final String fieldOddrn);

    /**
     * Edges of live relationships whose source or target dataset is one of the given datasets.
     */
    Flux<FieldLineageEdgeDto> findByDatasetOddrns(final Collection<String> datasetOddrns, final boolean asTarget);
}
