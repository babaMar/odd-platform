package org.opendatadiscovery.oddplatform.mapper.ingestion;

import java.util.List;
import org.apache.commons.collections4.ListUtils;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.FieldLineageRelationship;
import org.opendatadiscovery.oddplatform.model.tables.pojos.FieldLineageEdgePojo;
import org.springframework.stereotype.Component;

@Component
public class FieldLineageIngestionMapperImpl implements FieldLineageIngestionMapper {
    @Override
    public List<FieldLineageEdgePojo> mapEdges(final FieldLineageRelationship relationship) {
        return ListUtils.emptyIfNull(relationship.getEdges()).stream()
            .map(edge -> new FieldLineageEdgePojo()
                .setInputFieldOddrns(edge.getInputs().toArray(String[]::new))
                .setOutputFieldOddrn(edge.getOutput())
                .setExpression(edge.getExpression()))
            .toList();
    }
}
