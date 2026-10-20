import ELK from 'elkjs/lib/elk.bundled';
import type { ElkExtendedEdge, ElkNode } from 'elkjs/lib/elk.bundled';
import type { FieldLineageGraph } from 'generated-sources';
import { buildElkGraph } from './graph';

export interface FieldLineageLayout {
  nodes: ElkNode[];
  edges: ElkExtendedEdge[];
  width: number;
  height: number;
}

export async function layoutFieldLineage(
  graph: FieldLineageGraph
): Promise<FieldLineageLayout> {
  const layouted = await new ELK().layout(buildElkGraph(graph));
  return {
    nodes: layouted.children ?? [],
    edges: layouted.edges ?? [],
    width: layouted.width ?? 0,
    height: layouted.height ?? 0,
  };
}
