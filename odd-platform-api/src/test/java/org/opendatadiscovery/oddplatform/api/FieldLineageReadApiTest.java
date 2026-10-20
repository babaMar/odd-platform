package org.opendatadiscovery.oddplatform.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opendatadiscovery.oddplatform.BaseIngestionTest;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityDetails;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityRelationshipDetails;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityRelationshipDetailsList;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityRelationshipList;
import org.opendatadiscovery.oddplatform.api.contract.model.DataEntityRelationshipType;
import org.opendatadiscovery.oddplatform.api.contract.model.DataSetStructure;
import org.opendatadiscovery.oddplatform.api.contract.model.DataSource;
import org.opendatadiscovery.oddplatform.api.contract.model.DatasetFieldLineage;
import org.opendatadiscovery.oddplatform.api.contract.model.DatasetFieldLineageLink;
import org.opendatadiscovery.oddplatform.api.contract.model.FieldLineageFieldRef;
import org.opendatadiscovery.oddplatform.api.contract.model.FieldLineageGraph;
import org.opendatadiscovery.oddplatform.api.contract.model.FieldLineageGraphEdge;
import org.opendatadiscovery.oddplatform.api.contract.model.FieldLineageGraphNode;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.DataEntity;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.DataEntityList;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.DataEntityType;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.DataRelationship;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.DataSet;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.DataSetField;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.DataSetFieldType;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.FieldLineageEdge;
import org.opendatadiscovery.oddplatform.ingestion.contract.model.FieldLineageRelationship;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DataEntityPojo;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveDataEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Read API of field-level lineage over a chain products -> order_lines -> order_facts:
 * products.(tenant_id, sku) -> order_lines.tenant_sku, products.price -> order_lines.price,
 * order_lines.tenant_sku -> order_facts.tenant_sku.
 */
@DisplayName("Field lineage read API")
class FieldLineageReadApiTest extends BaseIngestionTest {
    @Autowired
    private ReactiveDataEntityRepository dataEntityRepository;

    private DataEntity products;
    private DataEntity orderLines;
    private DataEntity orderFacts;
    private DataEntity productsToOrderLines;
    private DataEntity orderLinesToOrderFacts;
    private Map<String, Long> ids;
    private String dataSourceOddrn;

    @BeforeEach
    void ingestChain() {
        final DataSource dataSource = createDataSource();
        dataSourceOddrn = dataSource.getOddrn();
        final String prefix = "//fl-read/" + UUID.randomUUID();
        products = table(prefix + "/products", "tenant_id", "sku", "price");
        orderLines = table(prefix + "/order_lines", "tenant_sku", "price");
        orderFacts = table(prefix + "/order_facts", "tenant_sku");
        productsToOrderLines = fieldLineage(prefix + "/rel/products_order_lines", products, orderLines, List.of(
            new FieldLineageEdge()
                .inputs(List.of(field(products, "tenant_id"), field(products, "sku")))
                .output(field(orderLines, "tenant_sku"))
                .expression("concat(tenant_id, sku)"),
            new FieldLineageEdge().inputs(List.of(field(products, "price"))).output(field(orderLines, "price"))));
        orderLinesToOrderFacts = fieldLineage(prefix + "/rel/order_lines_order_facts", orderLines, orderFacts,
            List.of(new FieldLineageEdge()
                .inputs(List.of(field(orderLines, "tenant_sku")))
                .output(field(orderFacts, "tenant_sku"))));

        ingestAndAssert(new DataEntityList()
            .dataSourceOddrn(dataSource.getOddrn())
            .items(List.of(products, orderLines, orderFacts, productsToOrderLines, orderLinesToOrderFacts)));

        ids = dataEntityRepository.listByOddrns(
                Arrays.asList(products.getOddrn(), orderLines.getOddrn(), orderFacts.getOddrn(),
                    productsToOrderLines.getOddrn(), orderLinesToOrderFacts.getOddrn()), false, false)
            .collectList().block().stream()
            .collect(Collectors.toMap(DataEntityPojo::getOddrn, DataEntityPojo::getId));
    }

    @Test
    void relationshipDetailsCarryResolvedEdges() {
        final DataEntityRelationshipDetails details = webTestClient.get()
            .uri("/api/relationships/field-lineage/{id}", id(productsToOrderLines))
            .exchange()
            .expectStatus().isOk()
            .expectBody(DataEntityRelationshipDetails.class)
            .returnResult().getResponseBody();

        assertThat(details.getType()).isEqualTo(DataEntityRelationshipType.FIELD_LINEAGE_RELATIONSHIP);
        assertThat(details.getFieldLineageRelationship().getEdges())
            .extracting(e -> e.getOutput().getName(),
                e -> e.getInputs().stream().map(FieldLineageFieldRef::getName).toList(),
                e -> e.getExpression())
            .containsExactlyInAnyOrder(
                tuple("tenant_sku", List.of("tenant_id", "sku"), "concat(tenant_id, sku)"),
                tuple("price", List.of("price"), null));

        final FieldLineageFieldRef tenantId = details.getFieldLineageRelationship().getEdges().stream()
            .filter(e -> e.getOutput().getName().equals("tenant_sku"))
            .findFirst().orElseThrow()
            .getInputs().get(0);
        assertThat(tenantId.getFieldId()).isEqualTo(fieldId(products, "tenant_id"));
        assertThat(tenantId.getDataEntityId()).isEqualTo(id(products));
        assertThat(tenantId.getDataEntityName()).isEqualTo(products.getName());
    }

    @Test
    void fieldRefsPointAtTheLatestFieldVersion() {
        final long firstVersionId = fieldId(orderLines, "tenant_sku");
        orderLines.getDataset().getFieldList().stream()
            .filter(f -> f.getName().equals("tenant_sku"))
            .forEach(f -> f.getType().type(DataSetFieldType.TypeEnum.INTEGER).logicalType("UInt64"));
        ingestAndAssert(new DataEntityList()
            .dataSourceOddrn(dataSourceOddrn)
            .items(List.of(orderLines)));

        final long latestVersionId = fieldId(orderLines, "tenant_sku");
        assertThat(latestVersionId).isNotEqualTo(firstVersionId);

        final DataEntityRelationshipDetails details = webTestClient.get()
            .uri("/api/relationships/field-lineage/{id}", id(productsToOrderLines))
            .exchange()
            .expectStatus().isOk()
            .expectBody(DataEntityRelationshipDetails.class)
            .returnResult().getResponseBody();
        assertThat(details.getFieldLineageRelationship().getEdges())
            .filteredOn(e -> e.getOutput().getName().equals("tenant_sku"))
            .singleElement()
            .satisfies(e -> assertThat(e.getOutput().getFieldId()).isEqualTo(latestVersionId));
    }

    @Test
    void datasetRelationshipsListIncludesFieldLineage() {
        final DataEntityRelationshipDetailsList list = webTestClient.get()
            .uri("/api/datasets/{id}/relationships?type=FIELD_LINEAGE", id(orderLines))
            .exchange()
            .expectStatus().isOk()
            .expectBody(DataEntityRelationshipDetailsList.class)
            .returnResult().getResponseBody();

        assertThat(list.getItems())
            .extracting(DataEntityRelationshipDetails::getName, r -> r.getFieldLineageRelationship().getEdges().size())
            .containsExactlyInAnyOrder(
                tuple(productsToOrderLines.getName(), 2),
                tuple(orderLinesToOrderFacts.getName(), 1));

        final DataEntityRelationshipList all = webTestClient.get()
            .uri("/api/relationships?page=1&size=50&type=FIELD_LINEAGE")
            .exchange()
            .expectStatus().isOk()
            .expectBody(DataEntityRelationshipList.class)
            .returnResult().getResponseBody();
        assertThat(all.getItems())
            .filteredOn(r -> r.getId().equals(id(productsToOrderLines)))
            .singleElement()
            .satisfies(r -> assertThat(r.getType())
                .isEqualTo(DataEntityRelationshipType.FIELD_LINEAGE_RELATIONSHIP));
    }

    @Test
    void relationshipEntityDetailsAreReadable() {
        webTestClient.get()
            .uri("/api/dataentities/{id}", id(productsToOrderLines))
            .exchange()
            .expectStatus().isOk()
            .expectBody(DataEntityDetails.class)
            .value(d -> assertThat(d.getType().getName().getValue()).isEqualTo("FIELD_LINEAGE_RELATIONSHIP"));
    }

    @Test
    void datasetFieldLineageListsUpstreamAndDownstreamFields() {
        final DatasetFieldLineage lineage = webTestClient.get()
            .uri("/api/datasetfields/{id}/lineage", fieldId(orderLines, "tenant_sku"))
            .exchange()
            .expectStatus().isOk()
            .expectBody(DatasetFieldLineage.class)
            .returnResult().getResponseBody();

        assertThat(lineage.getUpstream())
            .extracting(l -> l.getField().getDataEntityName(), l -> l.getField().getName(),
                DatasetFieldLineageLink::getRelationshipId, DatasetFieldLineageLink::getExpression)
            .containsExactlyInAnyOrder(
                tuple(products.getName(), "tenant_id", id(productsToOrderLines), "concat(tenant_id, sku)"),
                tuple(products.getName(), "sku", id(productsToOrderLines), "concat(tenant_id, sku)"));
        assertThat(lineage.getDownstream())
            .extracting(l -> l.getField().getDataEntityName(), l -> l.getField().getName(),
                l -> l.getField().getFieldId())
            .containsExactly(tuple(orderFacts.getName(), "tenant_sku", fieldId(orderFacts, "tenant_sku")));
    }

    @Test
    void fieldLineageGraphFollowsRelationshipsUpToDepth() {
        final FieldLineageGraph depthOne = fieldLineageGraph(id(orderLines), 1);
        assertThat(depthOne.getNodes())
            .extracting(FieldLineageGraphNode::getDatasetOddrn)
            .containsExactlyInAnyOrder(products.getOddrn(), orderLines.getOddrn(), orderFacts.getOddrn());
        assertThat(depthOne.getEdges())
            .extracting(FieldLineageGraphEdge::getSourceFieldOddrn, FieldLineageGraphEdge::getTargetFieldOddrn)
            .containsExactlyInAnyOrder(
                tuple(field(products, "tenant_id"), field(orderLines, "tenant_sku")),
                tuple(field(products, "sku"), field(orderLines, "tenant_sku")),
                tuple(field(products, "price"), field(orderLines, "price")),
                tuple(field(orderLines, "tenant_sku"), field(orderFacts, "tenant_sku")));
        assertThat(depthOne.getNodes())
            .filteredOn(n -> n.getDatasetOddrn().equals(products.getOddrn()))
            .singleElement()
            .satisfies(n -> {
                assertThat(n.getDataEntity().getId()).isEqualTo(id(products));
                assertThat(n.getFields()).extracting(FieldLineageFieldRef::getName)
                    .containsExactlyInAnyOrder("tenant_id", "sku", "price");
            });

        assertThat(fieldLineageGraph(id(products), 1).getNodes())
            .extracting(FieldLineageGraphNode::getDatasetOddrn)
            .containsExactlyInAnyOrder(products.getOddrn(), orderLines.getOddrn());
        assertThat(fieldLineageGraph(id(products), 2).getNodes())
            .extracting(FieldLineageGraphNode::getDatasetOddrn)
            .containsExactlyInAnyOrder(products.getOddrn(), orderLines.getOddrn(), orderFacts.getOddrn());
    }

    private FieldLineageGraph fieldLineageGraph(final long dataEntityId, final int depth) {
        return webTestClient.get()
            .uri("/api/dataentities/{id}/lineage/fields?lineage_depth={depth}", dataEntityId, depth)
            .exchange()
            .expectStatus().isOk()
            .expectBody(FieldLineageGraph.class)
            .returnResult().getResponseBody();
    }

    private long id(final DataEntity entity) {
        return ids.get(entity.getOddrn());
    }

    private long fieldId(final DataEntity dataset, final String name) {
        final DataSetStructure structure = webTestClient.get()
            .uri("/api/datasets/{id}/structure", id(dataset))
            .exchange()
            .expectStatus().isOk()
            .expectBody(DataSetStructure.class)
            .returnResult().getResponseBody();
        final Map<String, Long> byOddrn = structure.getFieldList().stream()
            .collect(Collectors.toMap(f -> f.getOddrn(), f -> f.getId()));
        return byOddrn.get(field(dataset, name));
    }

    private static DataEntity table(final String oddrn, final String... fieldNames) {
        final List<DataSetField> fields = Arrays.stream(fieldNames)
            .map(name -> new DataSetField()
                .oddrn(oddrn + "/columns/" + name)
                .name(name)
                .type(new DataSetFieldType()
                    .type(DataSetFieldType.TypeEnum.STRING)
                    .logicalType("String")
                    .isNullable(false)))
            .toList();
        return new DataEntity()
            .oddrn(oddrn)
            .name(oddrn.substring(oddrn.lastIndexOf('/') + 1))
            .type(DataEntityType.TABLE)
            .dataset(new DataSet().fieldList(fields));
    }

    private static DataEntity fieldLineage(final String oddrn, final DataEntity source, final DataEntity target,
                                           final List<FieldLineageEdge> edges) {
        return new DataEntity()
            .oddrn(oddrn)
            .name(oddrn.substring(oddrn.lastIndexOf('/') + 1))
            .type(DataEntityType.FIELD_LINEAGE_RELATIONSHIP)
            .dataRelationship(new DataRelationship()
                .relationshipType(DataRelationship.RelationshipTypeEnum.FIELD_LINEAGE)
                .sourceDatasetOddrn(source.getOddrn())
                .targetDatasetOddrn(target.getOddrn())
                .details(new FieldLineageRelationship()
                    .relationshipEntityName("FieldLineageRelationship")
                    .edges(edges)));
    }

    private static String field(final DataEntity dataset, final String name) {
        return dataset.getOddrn() + "/columns/" + name;
    }
}
