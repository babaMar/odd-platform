package org.opendatadiscovery.oddplatform.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.opendatadiscovery.oddplatform.api.contract.model.DatasetFieldLineage;
import org.opendatadiscovery.oddplatform.api.contract.model.DatasetFieldLineageLink;
import org.opendatadiscovery.oddplatform.api.contract.model.FieldLineageEdge;
import org.opendatadiscovery.oddplatform.api.contract.model.FieldLineageFieldRef;
import org.opendatadiscovery.oddplatform.api.contract.model.FieldLineageGraph;
import org.opendatadiscovery.oddplatform.api.contract.model.FieldLineageGraphEdge;
import org.opendatadiscovery.oddplatform.api.contract.model.FieldLineageGraphNode;
import org.opendatadiscovery.oddplatform.api.contract.model.FieldLineageRelationshipDetails;
import org.opendatadiscovery.oddplatform.dto.FieldLineageEdgeDto;
import org.opendatadiscovery.oddplatform.dto.RelationshipDetailsDto;
import org.opendatadiscovery.oddplatform.dto.RelationshipTypeDto;
import org.opendatadiscovery.oddplatform.exception.NotFoundException;
import org.opendatadiscovery.oddplatform.mapper.DataEntityMapper;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DataEntityPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.DatasetFieldPojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.FieldLineageEdgePojo;
import org.opendatadiscovery.oddplatform.model.tables.pojos.RelationshipsPojo;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveDataEntityRepository;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveDatasetFieldRepository;
import org.opendatadiscovery.oddplatform.repository.reactive.ReactiveFieldLineageEdgeRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FieldLineageServiceImpl implements FieldLineageService {
    private final ReactiveFieldLineageEdgeRepository fieldLineageEdgeRepository;
    private final ReactiveDatasetFieldRepository datasetFieldRepository;
    private final ReactiveDataEntityRepository dataEntityRepository;
    private final DataEntityMapper dataEntityMapper;

    @Override
    public Mono<Map<Long, FieldLineageRelationshipDetails>> getRelationshipDetails(
        final List<RelationshipDetailsDto> relationships) {
        final Map<Long, RelationshipsPojo> fieldLineageRelationships = relationships.stream()
            .map(RelationshipDetailsDto::relationshipPojo)
            .filter(r -> RelationshipTypeDto.FIELD_LINEAGE.name().equals(r.getRelationshipType()))
            .collect(Collectors.toMap(RelationshipsPojo::getId, Function.identity(), (a, b) -> a));
        if (fieldLineageRelationships.isEmpty()) {
            return Mono.just(Map.of());
        }

        return fieldLineageEdgeRepository.findByRelationshipIds(List.copyOf(fieldLineageRelationships.keySet()))
            .flatMap(edges -> {
                final List<FieldLineageEdgeDto> dtos = edges.stream()
                    .map(e -> new FieldLineageEdgeDto(e, fieldLineageRelationships.get(e.getRelationshipId())))
                    .toList();
                return resolve(dtos).map(refs -> {
                    final Map<Long, FieldLineageRelationshipDetails> result = new LinkedHashMap<>();
                    fieldLineageRelationships.values().forEach(r -> result.put(r.getDataEntityId(),
                        new FieldLineageRelationshipDetails().edges(new ArrayList<>())));
                    dtos.forEach(dto -> result.get(dto.relationship().getDataEntityId()).getEdges().add(
                        new FieldLineageEdge()
                            .inputs(inputs(dto.edge()).stream()
                                .map(oddrn -> refs.ref(oddrn, dto.relationship().getSourceDatasetOddrn()))
                                .toList())
                            .output(refs.ref(dto.edge().getOutputFieldOddrn(),
                                dto.relationship().getTargetDatasetOddrn()))
                            .expression(dto.edge().getExpression())));
                    return result;
                });
            });
    }

    @Override
    public Mono<DatasetFieldLineage> getDatasetFieldLineage(final long datasetFieldId) {
        return datasetFieldRepository.get(datasetFieldId)
            .switchIfEmpty(Mono.error(() -> new NotFoundException("Dataset field", datasetFieldId)))
            .flatMap(field -> fieldLineageEdgeRepository.findByFieldOddrn(field.getOddrn()).collectList()
                .flatMap(dtos -> resolve(dtos).map(refs -> {
                    final List<DatasetFieldLineageLink> upstream = new ArrayList<>();
                    final List<DatasetFieldLineageLink> downstream = new ArrayList<>();
                    dtos.forEach(dto -> {
                        final RelationshipsPojo relationship = dto.relationship();
                        if (field.getOddrn().equals(dto.edge().getOutputFieldOddrn())) {
                            inputs(dto.edge()).forEach(input -> upstream.add(link(
                                refs.ref(input, relationship.getSourceDatasetOddrn()), dto)));
                        }
                        if (inputs(dto.edge()).contains(field.getOddrn())) {
                            downstream.add(link(
                                refs.ref(dto.edge().getOutputFieldOddrn(), relationship.getTargetDatasetOddrn()), dto));
                        }
                    });
                    return new DatasetFieldLineage().upstream(upstream).downstream(downstream);
                })));
    }

    @Override
    public Mono<FieldLineageGraph> getFieldLineageGraph(final long dataEntityId, final int depth) {
        return dataEntityRepository.get(dataEntityId)
            .switchIfEmpty(Mono.error(() -> new NotFoundException("Data entity", dataEntityId)))
            .flatMap(root -> Mono.zip(
                    walk(Set.of(root.getOddrn()), depth, true),
                    walk(Set.of(root.getOddrn()), depth, false))
                .flatMap(walks -> {
                    final Map<Long, FieldLineageEdgeDto> edges = new LinkedHashMap<>();
                    Stream.concat(walks.getT1().stream(), walks.getT2().stream())
                        .forEach(dto -> edges.putIfAbsent(dto.edge().getId(), dto));
                    return resolve(edges.values()).map(refs -> graph(root.getOddrn(), edges.values(), refs));
                }));
    }

    /**
     * Collects edges `depth` datasets away from the start, upstream (towards sources) or downstream.
     */
    private Mono<List<FieldLineageEdgeDto>> walk(final Set<String> start, final int depth, final boolean upstream) {
        return walk(start, new HashSet<>(start), depth, upstream, new ArrayList<>());
    }

    private Mono<List<FieldLineageEdgeDto>> walk(final Set<String> frontier, final Set<String> visited,
                                                 final int depthLeft, final boolean upstream,
                                                 final List<FieldLineageEdgeDto> acc) {
        if (depthLeft <= 0 || frontier.isEmpty()) {
            return Mono.just(acc);
        }
        return fieldLineageEdgeRepository.findByDatasetOddrns(frontier, upstream).collectList()
            .flatMap(found -> {
                acc.addAll(found);
                final Set<String> next = found.stream()
                    .map(dto -> upstream
                        ? dto.relationship().getSourceDatasetOddrn()
                        : dto.relationship().getTargetDatasetOddrn())
                    .filter(visited::add)
                    .collect(Collectors.toSet());
                return walk(next, visited, depthLeft - 1, upstream, acc);
            });
    }

    private FieldLineageGraph graph(final String rootOddrn,
                                    final Collection<FieldLineageEdgeDto> edges,
                                    final ResolvedRefs refs) {
        final Map<String, Set<String>> fieldsByDataset = new LinkedHashMap<>();
        fieldsByDataset.put(rootOddrn, new LinkedHashSet<>());
        final List<FieldLineageGraphEdge> graphEdges = new ArrayList<>();

        edges.forEach(dto -> {
            final String source = dto.relationship().getSourceDatasetOddrn();
            final String target = dto.relationship().getTargetDatasetOddrn();
            final String output = dto.edge().getOutputFieldOddrn();
            fieldsByDataset.computeIfAbsent(target, k -> new LinkedHashSet<>()).add(output);
            inputs(dto.edge()).forEach(input -> {
                fieldsByDataset.computeIfAbsent(source, k -> new LinkedHashSet<>()).add(input);
                graphEdges.add(new FieldLineageGraphEdge()
                    .sourceFieldOddrn(input)
                    .targetFieldOddrn(output)
                    .relationshipId(dto.relationship().getDataEntityId())
                    .expression(dto.edge().getExpression()));
            });
        });

        final List<FieldLineageGraphNode> nodes = fieldsByDataset.entrySet().stream()
            .map(e -> {
                final DataEntityPojo dataset = refs.datasets().get(e.getKey());
                return new FieldLineageGraphNode()
                    .datasetOddrn(e.getKey())
                    .dataEntity(dataset != null ? dataEntityMapper.mapRef(dataset) : null)
                    .fields(e.getValue().stream().map(oddrn -> refs.ref(oddrn, e.getKey())).toList());
            })
            .toList();

        return new FieldLineageGraph().nodes(nodes).edges(graphEdges);
    }

    private Mono<ResolvedRefs> resolve(final Collection<FieldLineageEdgeDto> dtos) {
        final Set<String> fieldOddrns = new HashSet<>();
        final Set<String> datasetOddrns = new HashSet<>();
        dtos.forEach(dto -> {
            fieldOddrns.add(dto.edge().getOutputFieldOddrn());
            fieldOddrns.addAll(inputs(dto.edge()));
            datasetOddrns.add(dto.relationship().getSourceDatasetOddrn());
            datasetOddrns.add(dto.relationship().getTargetDatasetOddrn());
        });
        if (dtos.isEmpty()) {
            return Mono.just(new ResolvedRefs(Map.of(), Map.of()));
        }

        return Mono.zip(
            datasetFieldRepository.getLastVersionDatasetFieldsByOddrns(List.copyOf(fieldOddrns))
                .collectMap(DatasetFieldPojo::getOddrn, Function.identity()),
            dataEntityRepository.listByOddrns(datasetOddrns, true, false)
                .collectMap(DataEntityPojo::getOddrn, Function.identity())
        ).map(t -> new ResolvedRefs(t.getT1(), t.getT2()));
    }

    private static List<String> inputs(final FieldLineageEdgePojo edge) {
        return Arrays.asList(edge.getInputFieldOddrns());
    }

    private static DatasetFieldLineageLink link(final FieldLineageFieldRef field, final FieldLineageEdgeDto dto) {
        return new DatasetFieldLineageLink()
            .field(field)
            .relationshipId(dto.relationship().getDataEntityId())
            .expression(dto.edge().getExpression());
    }

    private record ResolvedRefs(Map<String, DatasetFieldPojo> fields, Map<String, DataEntityPojo> datasets) {
        FieldLineageFieldRef ref(final String fieldOddrn, final String datasetOddrn) {
            final DatasetFieldPojo field = fields.get(fieldOddrn);
            final DataEntityPojo dataset = datasets.get(datasetOddrn);
            return new FieldLineageFieldRef()
                .oddrn(fieldOddrn)
                .name(field != null ? field.getName() : fieldOddrn.substring(fieldOddrn.lastIndexOf('/') + 1))
                .fieldId(field != null ? field.getId() : null)
                .dataEntityId(dataset != null && !Boolean.TRUE.equals(dataset.getHollow()) ? dataset.getId() : null)
                .dataEntityName(dataset != null ? datasetName(dataset) : null);
        }

        private static String datasetName(final DataEntityPojo dataset) {
            return dataset.getInternalName() != null ? dataset.getInternalName() : dataset.getExternalName();
        }
    }
}
