package org.opendatadiscovery.oddplatform.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opendatadiscovery.oddplatform.dto.ingestion.DataEntityIngestionDto.DataRelationshipDetailsDto;
import org.opendatadiscovery.oddplatform.model.tables.pojos.FieldLineageEdgePojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.RelationshipsPojo;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveFieldLineageEdgeRepository;
import org.opendatadiscovery.oddplatform.utils.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FieldLineageIngestionServiceImpl implements FieldLineageIngestionService {
    private final ReactiveFieldLineageEdgeRepository fieldLineageEdgeRepository;

    @Override
    public Mono<Void> replaceEdges(
        final List<Pair<RelationshipsPojo, DataRelationshipDetailsDto>> relationshipPojoWithDetails) {
        final List<Long> relationshipIds = relationshipPojoWithDetails.stream()
            .map(item -> item.getLeft().getId())
            .toList();

        final List<FieldLineageEdgePojo> toCreate = relationshipPojoWithDetails.stream()
            .flatMap(item -> item.getRight().fieldLineageEdges().stream()
                .map(edge -> new FieldLineageEdgePojo(edge).setRelationshipId(item.getLeft().getId())))
            .toList();

        return fieldLineageEdgeRepository.deleteByRelationshipIds(relationshipIds)
            .then(fieldLineageEdgeRepository.bulkCreate(toCreate).then());
    }
}
