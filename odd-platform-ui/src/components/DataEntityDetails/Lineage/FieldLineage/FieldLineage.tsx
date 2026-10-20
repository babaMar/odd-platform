import React, { type FC, useEffect, useMemo, useRef, useState } from 'react';
import { MenuItem, Select, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { line, curveBasis } from 'd3-shape';
import { select } from 'd3-selection';
import { zoom as d3Zoom } from 'd3-zoom';
import type { ElkExtendedEdge } from 'elkjs/lib/elk.bundled';
import type { FieldLineageGraphNode } from 'generated-sources';
import { AppCircularProgress } from 'components/shared/elements';
import { useFieldLineageGraph } from 'lib/hooks/api';
import { dataEntityDetailsPath } from 'routes';
import { connectedEdgeIds, edgeId } from './lib/graph';
import { type FieldLineageLayout, layoutFieldLineage } from './lib/layoutFieldLineage';
import * as S from './FieldLineage.styles';

const DEPTHS = [1, 2, 3, 4, 5];

const nodeName = (node: FieldLineageGraphNode) =>
  node.dataEntity?.internalName ||
  node.dataEntity?.externalName ||
  node.datasetOddrn.substring(node.datasetOddrn.lastIndexOf('/') + 1);

const edgePath = (edge: ElkExtendedEdge) => {
  const section = edge.sections?.[0];
  if (!section) return null;
  const points = [section.startPoint, ...(section.bendPoints ?? []), section.endPoint];
  return line<{ x: number; y: number }>()
    .x(p => p.x)
    .y(p => p.y)
    .curve(curveBasis)(points);
};

interface FieldLineageProps {
  dataEntityId: number;
}

const FieldLineage: FC<FieldLineageProps> = ({ dataEntityId }) => {
  const { t } = useTranslation();
  const [lineageDepth, setLineageDepth] = useState(1);
  const { data, isLoading } = useFieldLineageGraph({ dataEntityId, lineageDepth });
  const [layout, setLayout] = useState<FieldLineageLayout>();
  const [hoveredField, setHoveredField] = useState<string>();
  const viewportRef = useRef<HTMLDivElement>(null);
  const [transform, setTransform] = useState('translate(16px, 16px)');

  useEffect(() => {
    let cancelled = false;
    if (data && data.edges.length > 0) {
      layoutFieldLineage(data).then(result => {
        if (!cancelled) setLayout(result);
      });
    }
    return () => {
      cancelled = true;
    };
  }, [data]);

  useEffect(() => {
    if (!viewportRef.current) return;
    const zoom = d3Zoom<HTMLDivElement, unknown>()
      .scaleExtent([0.1, 2])
      .on('zoom', ({ transform: { x, y, k } }) =>
        setTransform(`translate(${x}px, ${y}px) scale(${k})`)
      );
    select(viewportRef.current).call(zoom);
  }, [layout]);

  const nodesByOddrn = useMemo(
    () => new Map(data?.nodes.map(node => [node.datasetOddrn, node])),
    [data]
  );
  const highlighted = useMemo(
    () => (hoveredField && data ? connectedEdgeIds(data.edges, hoveredField) : new Set()),
    [hoveredField, data]
  );
  const highlightedFields = useMemo(() => {
    const fields = new Set<string>();
    data?.edges
      .filter(edge => highlighted.has(edgeId(edge)))
      .forEach(edge => {
        fields.add(edge.sourceFieldOddrn);
        fields.add(edge.targetFieldOddrn);
      });
    return fields;
  }, [highlighted, data]);

  const depthSelect = (
    <Select
      size='small'
      value={lineageDepth}
      onChange={e => setLineageDepth(Number(e.target.value))}
      sx={{ width: 'fit-content' }}
      renderValue={value => `${t('Depth')}: ${value}`}
    >
      {DEPTHS.map(depth => (
        <MenuItem key={depth} value={depth}>
          {depth}
        </MenuItem>
      ))}
    </Select>
  );

  if (isLoading) return <AppCircularProgress size={16} text={t('Loading lineage')} />;

  if (data && data.edges.length === 0) {
    return <Typography variant='subtitle1'>{t('No field lineage')}</Typography>;
  }

  return (
    <S.Container>
      {depthSelect}
      <S.Viewport ref={viewportRef}>
        {layout && (
          <S.Canvas style={{ transform, width: layout.width, height: layout.height }}>
            <svg
              width={layout.width}
              height={layout.height}
              style={{ position: 'absolute', overflow: 'visible', pointerEvents: 'none' }}
            >
              {layout.edges.map(edge => (
                <path
                  key={edge.id}
                  data-testid='field-lineage-edge'
                  d={edgePath(edge) ?? undefined}
                  fill='none'
                  stroke={highlighted.has(edge.id) ? '#0080FF' : '#A8B0BD'}
                  strokeWidth={highlighted.has(edge.id) ? 2 : 1.5}
                />
              ))}
            </svg>
            {layout.nodes.map(elkNode => {
              const node = nodesByOddrn.get(elkNode.id);
              if (!node) return null;
              return (
                <S.Node
                  key={elkNode.id}
                  style={{ left: elkNode.x, top: elkNode.y, height: elkNode.height }}
                >
                  <S.NodeHeader>
                    {node.dataEntity ? (
                      <Link to={dataEntityDetailsPath(node.dataEntity.id)}>
                        <Typography variant='h4' color='button.link.normal.color'>
                          {nodeName(node)}
                        </Typography>
                      </Link>
                    ) : (
                      <Typography variant='h4'>{nodeName(node)}</Typography>
                    )}
                  </S.NodeHeader>
                  {node.fields.map(field => (
                    <S.FieldRow
                      key={field.oddrn}
                      $highlighted={highlightedFields.has(field.oddrn)}
                      onMouseEnter={() => setHoveredField(field.oddrn)}
                      onMouseLeave={() => setHoveredField(undefined)}
                      title={field.oddrn}
                    >
                      <Typography variant='body1'>{field.name}</Typography>
                    </S.FieldRow>
                  ))}
                </S.Node>
              );
            })}
          </S.Canvas>
        )}
      </S.Viewport>
    </S.Container>
  );
};

export default FieldLineage;
