package org.opendatadiscovery.oddplatform.dto;

import org.opendatadiscovery.oddplatform.model.tables.pojos.FieldLineageEdgePojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.RelationshipsPojo;

public record FieldLineageEdgeDto(FieldLineageEdgePojo edge, RelationshipsPojo relationship) {
}
