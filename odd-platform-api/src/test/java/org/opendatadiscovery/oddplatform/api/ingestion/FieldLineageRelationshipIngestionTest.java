package org.opendatadiscovery.oddplatform.api.ingestion;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opendatadiscovery.oddplatform.BaseIngestionTest;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityLineage;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityLineageNode;
import org.opendatadiscovery.oddplatform.api.contract.model.DataSource;
import org.opendatadiscovery.oddplatform.api.ingestion.utils.IngestionModelGenerator;
import org.opendatadiscovery.oddplatform.dto.DataEntityTypeDto;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.DataEntity;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.DataEntityList;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.DataEntityType;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.DataRelationship;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.DataSet;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.ERDRelationship;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.FieldLineageEdge;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.FieldLineageRelationship;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.GraphRelationship;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DataEntityPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.FieldLineageEdgePojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.RelationshipsPojo;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveDataEntityRepository;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveERDRelationshipsRepository;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveFieldLineageEdgeRepository;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveGraphRelationshipsRepository;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveRelationshipsRepository;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class FieldLineageRelationshipIngestionTest extends BaseIngestionTest {
    @Autowired
    private ReactiveDataEntityRepository dataEntityRepository;
    @Autowired
    private ReactiveRelationshipsRepository relationshipsRepository;
    @Autowired
    private ReactiveFieldLineageEdgeRepository fieldLineageEdgeRepository;
    @Autowired
    private ReactiveERDRelationshipsRepository erdRelationshipsRepository;
    @Autowired
    private ReactiveGraphRelationshipsRepository graphRelationshipsRepository;

    @Test
    @DisplayName("a FIELD_LINEAGE relationship stores its edges and links the datasets in lineage")
    void fieldLineageIsIngested() {
        final DataSource dataSource = createDataSource();
        final DataEntity products = table();
        final DataEntity orderLines = table();
        final DataEntity relationship = fieldLineage(products, orderLines, List.of(
            edge(List.of(field(products, "tenant_id"), field(products, "sku")),
                field(orderLines, "tenant_sku"), "concat(tenant_id, sku)"),
            edge(List.of(field(products, "price")), field(orderLines, "price"), null)));

        ingestAndAssert(new DataEntityList()
            .dataSourceOddrn(dataSource.getOddrn())
            .items(List.of(products, orderLines, relationship)));

        final Map<String, DataEntityPojo> entities = entitiesByOddrn(products, orderLines, relationship);
        final DataEntityPojo relationshipEntity = entities.get(relationship.getOddrn());
        assertThat(relationshipEntity.getTypeId())
            .isEqualTo(DataEntityTypeDto.FIELD_LINEAGE_RELATIONSHIP.getId());

        final RelationshipsPojo relationshipPojo = relationshipOf(relationshipEntity);
        assertThat(relationshipPojo.getRelationshipType()).isEqualTo("FIELD_LINEAGE");
        assertThat(relationshipPojo.getSourceDatasetOddrn()).isEqualTo(products.getOddrn());
        assertThat(relationshipPojo.getTargetDatasetOddrn()).isEqualTo(orderLines.getOddrn());

        assertThat(edgesOf(relationshipPojo))
            .extracting(e -> List.of(e.getInputFieldOddrns()), FieldLineageEdgePojo::getOutputFieldOddrn,
                FieldLineageEdgePojo::getExpression)
            .containsExactlyInAnyOrder(
                tuple(List.of(field(products, "tenant_id"), field(products, "sku")),
                    field(orderLines, "tenant_sku"), "concat(tenant_id, sku)"),
                tuple(List.of(field(products, "price")), field(orderLines, "price"), null));

        assertThat(downstreamIds(entities.get(products.getOddrn()).getId()))
            .contains(entities.get(orderLines.getOddrn()).getId());
    }

    @Test
    @DisplayName("re-ingesting a FIELD_LINEAGE relationship replaces its edges")
    void reIngestReplacesEdges() {
        final DataSource dataSource = createDataSource();
        final DataEntity products = table();
        final DataEntity orderLines = table();
        final DataEntity relationship = fieldLineage(products, orderLines, List.of(
            edge(List.of(field(products, "sku")), field(orderLines, "sku"), null)));
        final DataEntityList list = new DataEntityList()
            .dataSourceOddrn(dataSource.getOddrn())
            .items(List.of(products, orderLines, relationship));
        ingestAndAssert(list);

        relationship.getDataRelationship().details(new FieldLineageRelationship()
            .relationshipEntityName("FieldLineageRelationship")
            .edges(List.of(edge(List.of(field(products, "price")), field(orderLines, "price"), null))));
        ingestAndAssert(list);

        final RelationshipsPojo relationshipPojo =
            relationshipOf(entitiesByOddrn(relationship).get(relationship.getOddrn()));
        assertThat(edgesOf(relationshipPojo))
            .extracting(FieldLineageEdgePojo::getOutputFieldOddrn)
            .containsExactly(field(orderLines, "price"));
    }

    @Test
    @DisplayName("a batch mixing ERD, GRAPH and FIELD_LINEAGE relationships stores each one's own details")
    void mixedRelationshipBatch() {
        final DataSource dataSource = createDataSource();
        final DataEntity products = table();
        final DataEntity orderLines = table();
        final DataEntity erd = relationshipEntity(DataEntityType.ENTITY_RELATIONSHIP, new DataRelationship()
            .relationshipType(DataRelationship.RelationshipTypeEnum.ERD)
            .sourceDatasetOddrn(orderLines.getOddrn())
            .targetDatasetOddrn(products.getOddrn())
            .details(new ERDRelationship()
                .relationshipEntityName("ERDRelationship")
                .sourceDatasetFieldOddrnsList(List.of(field(orderLines, "sku")))
                .targetDatasetFieldOddrnsList(List.of(field(products, "sku")))));
        final DataEntity graph = relationshipEntity(DataEntityType.GRAPH_RELATIONSHIP, new DataRelationship()
            .relationshipType(DataRelationship.RelationshipTypeEnum.GRAPH)
            .sourceDatasetOddrn(products.getOddrn())
            .targetDatasetOddrn(orderLines.getOddrn())
            .details(new GraphRelationship().relationshipEntityName("GraphRelationship").isDirected(true)));
        final DataEntity lineage = fieldLineage(products, orderLines, List.of(
            edge(List.of(field(products, "sku")), field(orderLines, "sku"), null)));

        ingestAndAssert(new DataEntityList()
            .dataSourceOddrn(dataSource.getOddrn())
            .items(List.of(products, orderLines, erd, graph, lineage)));

        final Map<String, DataEntityPojo> entities = entitiesByOddrn(erd, graph, lineage);
        final RelationshipsPojo erdPojo = relationshipOf(entities.get(erd.getOddrn()));
        final RelationshipsPojo graphPojo = relationshipOf(entities.get(graph.getOddrn()));
        final RelationshipsPojo lineagePojo = relationshipOf(entities.get(lineage.getOddrn()));

        assertThat(erdRelationshipsRepository.findERDSByRelationIds(List.of(erdPojo.getId())).block()).hasSize(1);
        assertThat(graphRelationshipsRepository.findGraphsByRelationIds(List.of(graphPojo.getId())).block())
            .hasSize(1);
        assertThat(edgesOf(lineagePojo)).hasSize(1);
    }

    private DataEntity table() {
        return IngestionModelGenerator.generateSimpleDataEntity(DataEntityType.TABLE)
            .dataset(new DataSet().fieldList(IngestionModelGenerator.generateDatasetFields(2)));
    }

    private DataEntity fieldLineage(final DataEntity source, final DataEntity target,
                                    final List<FieldLineageEdge> edges) {
        return relationshipEntity(DataEntityType.FIELD_LINEAGE_RELATIONSHIP, new DataRelationship()
            .relationshipType(DataRelationship.RelationshipTypeEnum.FIELD_LINEAGE)
            .sourceDatasetOddrn(source.getOddrn())
            .targetDatasetOddrn(target.getOddrn())
            .details(new FieldLineageRelationship()
                .relationshipEntityName("FieldLineageRelationship")
                .edges(edges)));
    }

    private DataEntity relationshipEntity(final DataEntityType type, final DataRelationship relationship) {
        final String uuid = UUID.randomUUID().toString();
        return new DataEntity()
            .oddrn("//relationships/" + uuid)
            .name("rel-" + uuid)
            .type(type)
            .dataRelationship(relationship);
    }

    private FieldLineageEdge edge(final List<String> inputs, final String output, final String expression) {
        return new FieldLineageEdge().inputs(inputs).output(output).expression(expression);
    }

    private String field(final DataEntity dataset, final String name) {
        return dataset.getOddrn() + "/columns/" + name;
    }

    private Map<String, DataEntityPojo> entitiesByOddrn(final DataEntity... entities) {
        return dataEntityRepository
            .listByOddrns(Arrays.stream(entities).map(DataEntity::getOddrn).toList(), false, false)
            .collectList()
            .block()
            .stream()
            .collect(Collectors.toMap(DataEntityPojo::getOddrn, identity()));
    }

    private RelationshipsPojo relationshipOf(final DataEntityPojo relationshipEntity) {
        final List<RelationshipsPojo> relationships =
            relationshipsRepository.getRelationshipByDataEntityIds(List.of(relationshipEntity.getId())).block();
        assertThat(relationships).hasSize(1);
        return relationships.get(0);
    }

    private List<FieldLineageEdgePojo> edgesOf(final RelationshipsPojo relationship) {
        return fieldLineageEdgeRepository.findByRelationshipIds(List.of(relationship.getId())).block();
    }

    private List<Long> downstreamIds(final long dataEntityId) {
        return webTestClient.get()
            .uri("/api/dataentities/{data_entity_id}/lineage/downstream", dataEntityId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(DataEntityLineage.class)
            .returnResult()
            .getResponseBody()
            .getDownstream()
            .getNodes()
            .stream()
            .map(DataEntityLineageNode::getId)
            .toList();
    }
}
