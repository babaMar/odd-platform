package org.opendatadiscovery.oddplatform.service;

import java.util.List;
import java.util.Map;
import org.opendatadiscovery.oddplatform.api.contract.model.DatasetFieldLineage;
import org.opendatadiscovery.oddplatform.api.contract.model.FieldLineageGraph;
import org.opendatadiscovery.oddplatform.api.contract.model.FieldLineageRelationshipDetails;
import org.opendatadiscovery.oddplatform.dto.RelationshipDetailsDto;
import reactor.core.publisher.Mono;

public interface FieldLineageService {
    /**
     * Field lineage details of the FIELD_LINEAGE relationships among the given ones, keyed by the
     * relationship's data entity id.
     */
    Mono<Map<Long, FieldLineageRelationshipDetails>> getRelationshipDetails(
        final List<RelationshipDetailsDto> relationships);

    Mono<DatasetFieldLineage> getDatasetFieldLineage(final long datasetFieldId);

    Mono<FieldLineageGraph> getFieldLineageGraph(final long dataEntityId, final int depth);
}
