package org.opendatadiscovery.oddplatform.housekeeping;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opendatadiscovery.oddplatform.BaseIntegrationTest;
import org.opendatadiscovery.oddplatform.dto.DataEntityClassDto;
import org.opendatadiscovery.oddplatform.dto.DataEntityStatusDto;
import org.opendatadiscovery.oddplatform.dto.DataEntityTypeDto;
import org.opendatadiscovery.oddplatform.housekeeping.config.HousekeepingTTLProperties;
import org.opendatadiscovery.oddplatform.housekeeping.job.DataEntityHousekeepingJob;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DataEntityPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.FieldLineageEdgePojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.LineagePojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.RelationshipsPojo;
import org.opendatadiscovery.oddplatform.notification.PGConnectionFactory;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveDataEntityRepository;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveFieldLineageEdgeRepository;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveLineageRepository;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveRelationshipsRepository;
import org.opendatadiscovery.oddplatform.service.attachment.FileUploadService;
import org.opendatadiscovery.oddplatform.service.ingestion.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.opendatadiscovery.oddplatform.model.Tables.DATA_ENTITY;
import static org.opendatadiscovery.oddplatform.model.Tables.FIELD_LINEAGE_EDGE;
import static org.opendatadiscovery.oddplatform.model.Tables.LINEAGE;
import static org.opendatadiscovery.oddplatform.model.Tables.RELATIONSHIPS;

@DisplayName("DataEntityHousekeepingJob hard-deletes relationship entities with their details")
class DataEntityHousekeepingRelationshipTest extends BaseIntegrationTest {
    @Autowired
    private PGConnectionFactory pgConnectionFactory;
    @Autowired
    private FileUploadService fileUploadService;
    @Autowired
    private ReactiveDataEntityRepository dataEntityRepository;
    @Autowired
    private ReactiveRelationshipsRepository relationshipsRepository;
    @Autowired
    private ReactiveFieldLineageEdgeRepository fieldLineageEdgeRepository;
    @Autowired
    private ReactiveLineageRepository lineageRepository;

    @Test
    void deletedFieldLineageRelationshipIsRemovedWithEdgesAndDerivedLineage() throws Exception {
        final String prefix = "//hk-rel/" + UUID.randomUUID();
        final DataEntityPojo relationshipEntity = dataEntityRepository.create(new DataEntityPojo()
                .setOddrn(prefix + "/rel")
                .setExternalName("rel")
                .setEntityClassIds(new Integer[] {DataEntityClassDto.DATA_RELATIONSHIP.getId()})
                .setTypeId(DataEntityTypeDto.FIELD_LINEAGE_RELATIONSHIP.getId())
                .setHollow(false)
                .setExcludeFromSearch(false)
                .setStatus(DataEntityStatusDto.DELETED.getId()))
            .block();
        final RelationshipsPojo relationship = relationshipsRepository.create(new RelationshipsPojo()
                .setDataEntityId(relationshipEntity.getId())
                .setSourceDatasetOddrn(prefix + "/source")
                .setTargetDatasetOddrn(prefix + "/target")
                .setRelationshipType("FIELD_LINEAGE"))
            .block();
        fieldLineageEdgeRepository.create(new FieldLineageEdgePojo()
                .setRelationshipId(relationship.getId())
                .setInputFieldOddrns(new String[] {prefix + "/source/columns/a"})
                .setOutputFieldOddrn(prefix + "/target/columns/a"))
            .block();
        lineageRepository.batchInsertLineages(List.of(new LineagePojo()
                .setParentOddrn(prefix + "/source")
                .setChildOddrn(prefix + "/target")
                .setEstablisherOddrn(relationshipEntity.getOddrn())))
            .blockLast();

        try (final Connection connection = pgConnectionFactory.getConnection()) {
            DSL.using(connection).update(DATA_ENTITY)
                .set(DATA_ENTITY.STATUS_UPDATED_AT, DateTimeUtil.generateNow().minusDays(30))
                .where(DATA_ENTITY.ID.eq(relationshipEntity.getId()))
                .execute();
        }

        final HousekeepingTTLProperties ttl = new HousekeepingTTLProperties();
        ttl.setDataEntityDeleteDays(1);
        try (final Connection connection = pgConnectionFactory.getConnection()) {
            new DataEntityHousekeepingJob(ttl, fileUploadService).doHousekeeping(connection);
        }

        try (final Connection connection = pgConnectionFactory.getConnection()) {
            final var dsl = DSL.using(connection);
            assertThat(dsl.fetchCount(DATA_ENTITY, DATA_ENTITY.ID.eq(relationshipEntity.getId()))).isZero();
            assertThat(dsl.fetchCount(RELATIONSHIPS, RELATIONSHIPS.ID.eq(relationship.getId()))).isZero();
            assertThat(dsl.fetchCount(FIELD_LINEAGE_EDGE, FIELD_LINEAGE_EDGE.RELATIONSHIP_ID.eq(relationship.getId())))
                .isZero();
            assertThat(dsl.fetchCount(LINEAGE, LINEAGE.ESTABLISHER_ODDRN.eq(relationshipEntity.getOddrn()))).isZero();
        }
    }
}
