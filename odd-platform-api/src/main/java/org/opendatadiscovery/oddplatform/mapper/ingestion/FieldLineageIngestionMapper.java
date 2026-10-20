package org.opendatadiscovery.oddplatform.mapper.ingestion;

import java.util.List;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.FieldLineageRelationship;
import org.opendatadiscovery.oddplatform.model.tables.pojos.FieldLineageEdgePojo;

public interface FieldLineageIngestionMapper {
    List<FieldLineageEdgePojo> mapEdges(final FieldLineageRelationship relationship);
}
