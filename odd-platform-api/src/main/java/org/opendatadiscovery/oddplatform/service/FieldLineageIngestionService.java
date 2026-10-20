package org.opendatadiscovery.oddplatform.service;

import java.util.List;
import org.opendatadiscovery.oddplatform.dto.ingestion.DataEntityIngestionDto.DataRelationshipDetailsDto;
import org.opendatadiscovery.oddplatform.model.tables.pojos.RelationshipsPojo;
import org.opendatadiscovery.oddplatform.utils.Pair;
import reactor.core.publisher.Mono;

public interface FieldLineageIngestionService {
    /**
     * Replaces the edges of each relationship with the ingested ones.
     */
    Mono<Void> replaceEdges(
        final List<Pair<RelationshipsPojo, DataRelationshipDetailsDto>> relationshipPojoWithDetails);
}
