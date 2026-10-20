package org.opendatadiscovery.oddplatform.mapper;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityRelationship;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityRelationshipDetails;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityRelationshipDetailsList;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityRelationshipList;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityRelationshipType;
import org.opendatadiscovery.oddplatform.api.contract.model.PageInfo;
import org.opendatadiscovery.oddplatform.dto.DataSourceDto;
import org.opendatadiscovery.oddplatform.dto.RelationshipDetailsDto;
import org.opendatadiscovery.oddplatform.dto.RelationshipDto;
import org.opendatadiscovery.oddplatform.dto.RelationshipTypeDto;
import org.opendatadiscovery.oddplatform.model.tables.pojos.RelationshipsPojo;
import org.opendatadiscovery.oddplatform.utils.Page;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class RelationshipMapper {
    private final DataSourceSafeMapper dataSourceSafeMapper;
    private final DataEntityMapper dataEntityMapper;
    private final ErdRelationshipMapper erdRelationshipMapper;
    private final GraphRelationshipMapper graphRelationshipMapper;

    public DataEntityRelationshipDetailsList mapListToRelationshipList(final List<RelationshipDetailsDto> dtos) {
        return new DataEntityRelationshipDetailsList()
            .items(mapToRelationshipDetailsList(dtos))
            .pageInfo(new PageInfo().total((long) dtos.size()).hasNext(false));
    }

    private List<DataEntityRelationshipDetails> mapToRelationshipDetailsList(final List<RelationshipDetailsDto> dtos) {
        return dtos.stream()
            .map(this::mapToDatasetRelationshipDetails)
            .collect(Collectors.toList());
    }

    private List<DataEntityRelationship> mapToRelationshipList(final List<RelationshipDto> relationshipDtos) {
        return relationshipDtos.stream()
            .map(this::mapToDatasetRelationship)
            .collect(Collectors.toList());
    }

    public DataEntityRelationshipList mapListToRelationshipPage(final Page<RelationshipDto> relationshipDtoPage) {
        return new DataEntityRelationshipList()
            .items(mapToRelationshipList(relationshipDtoPage.getData()))
            .pageInfo(new PageInfo().total((relationshipDtoPage.getTotal())).hasNext(relationshipDtoPage.isHasNext()));
    }

    public DataEntityRelationship mapToDatasetRelationship(final RelationshipDto item) {
        return new DataEntityRelationship()
            .id(item.dataEntityRelationship().getId())
            .name(item.dataEntityRelationship().getExternalName())
            .oddrn(item.dataEntityRelationship().getOddrn())
            .sourceDataEntity(dataEntityMapper.mapRef(item.sourceDataEntity()))
            .targetDataEntity(dataEntityMapper.mapRef(item.targetDataEntity()))
            .dataSource(dataSourceSafeMapper.mapDto(new DataSourceDto(item.dataSourcePojo(),
                item.dataSourceNamespacePojo(), null)))
            .type(mapType(item.relationshipPojo()));
    }

    public DataEntityRelationshipDetails mapToDatasetRelationshipDetails(final RelationshipDetailsDto item) {
        return new DataEntityRelationshipDetails()
            .id(item.dataEntityRelationship().getId())
            .name(item.dataEntityRelationship().getExternalName())
            .oddrn(item.dataEntityRelationship().getOddrn())
            .sourceDataEntity(dataEntityMapper.mapRef(item.sourceDataEntity()))
            .targetDataEntity(dataEntityMapper.mapRef(item.targetDataEntity()))
            .dataSource(dataSourceSafeMapper.mapDto(new DataSourceDto(item.dataSourcePojo(),
                item.dataSourceNamespacePojo(), null)))
            .type(mapType(item.relationshipPojo()))
            .erdRelationship(
                erdRelationshipMapper.mapPojoToDetails(item.erdRelationshipDetailsDto()))
            .graphRelationship(
                graphRelationshipMapper.mapPojoToDetails(item.graphRelationshipPojo()));
    }

    private DataEntityRelationshipType mapType(final RelationshipsPojo relationship) {
        return switch (RelationshipTypeDto.valueOf(relationship.getRelationshipType())) {
            case ERD -> DataEntityRelationshipType.ENTITY_RELATIONSHIP;
            case GRAPH -> DataEntityRelationshipType.GRAPH_RELATIONSHIP;
            case FIELD_LINEAGE -> DataEntityRelationshipType.FIELD_LINEAGE_RELATIONSHIP;
        };
    }
}
