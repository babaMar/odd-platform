import type { ElkExtendedEdge, ElkNode } from 'elkjs/lib/elk.bundled';
import type { FieldLineageGraph, FieldLineageGraphEdge } from 'generated-sources';

export const NODE_WIDTH = 280;
export const HEADER_HEIGHT = 40;
export const ROW_HEIGHT = 28;

export const layoutOptions: Record<string, string> = {
  'elk.algorithm': 'layered',
  'elk.direction': 'RIGHT',
  'elk.layered.spacing.nodeNodeBetweenLayers': '120',
  'elk.spacing.nodeNode': '40',
  'elk.layered.nodePlacement.bk.fixedAlignment': 'BALANCED',
};

export const edgeId = ({ sourceFieldOddrn, targetFieldOddrn }: FieldLineageGraphEdge) =>
  `${sourceFieldOddrn}->${targetFieldOddrn}`;

const inPort = (fieldOddrn: string) => `${fieldOddrn}#in`;
const outPort = (fieldOddrn: string) => `${fieldOddrn}#out`;

export const fieldRowY = (index: number) =>
  HEADER_HEIGHT + index * ROW_HEIGHT + ROW_HEIGHT / 2;

// one node per dataset; each field row gets a fixed in-port (left) and out-port (right), so a field
// that is both derived and derived-from (a middle dataset) is drawn once with edges on both sides
export function buildElkGraph({ nodes, edges }: FieldLineageGraph): ElkNode {
  return {
    id: 'root',
    layoutOptions,
    children: nodes.map(node => ({
      id: node.datasetOddrn,
      width: NODE_WIDTH,
      height: HEADER_HEIGHT + node.fields.length * ROW_HEIGHT,
      layoutOptions: { 'elk.portConstraints': 'FIXED_POS' },
      ports: node.fields.flatMap((field, i) => [
        { id: inPort(field.oddrn), x: 0, y: fieldRowY(i) },
        { id: outPort(field.oddrn), x: NODE_WIDTH, y: fieldRowY(i) },
      ]),
    })),
    edges: edges.map<ElkExtendedEdge>(edge => ({
      id: edgeId(edge),
      sources: [outPort(edge.sourceFieldOddrn)],
      targets: [inPort(edge.targetFieldOddrn)],
    })),
  };
}

// edges on any path through the field: everything it derives from and everything derived from it
export function connectedEdgeIds(
  edges: FieldLineageGraphEdge[],
  fieldOddrn: string
): Set<string> {
  const result = new Set<string>();
  const walk = (start: string, from: 'sourceFieldOddrn' | 'targetFieldOddrn') => {
    const to = from === 'sourceFieldOddrn' ? 'targetFieldOddrn' : 'sourceFieldOddrn';
    const queue = [start];
    const seen = new Set(queue);
    while (queue.length) {
      const current = queue.shift()!;
      edges
        .filter(edge => edge[from] === current)
        .forEach(edge => {
          result.add(edgeId(edge));
          if (!seen.has(edge[to])) {
            seen.add(edge[to]);
            queue.push(edge[to]);
          }
        });
    }
  };
  walk(fieldOddrn, 'sourceFieldOddrn');
  walk(fieldOddrn, 'targetFieldOddrn');
  return result;
}
