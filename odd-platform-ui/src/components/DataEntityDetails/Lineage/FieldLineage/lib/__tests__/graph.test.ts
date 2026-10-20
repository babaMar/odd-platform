import { describe, expect, it } from 'vitest';
import {
  buildElkGraph,
  connectedEdgeIds,
  edgeId,
  HEADER_HEIGHT,
  NODE_WIDTH,
  ROW_HEIGHT,
} from '../graph';
import { graphFixture } from './fixtures';

describe('buildElkGraph', () => {
  it('sizes each dataset node by its fields and gives every field an in and an out port', () => {
    const graph = buildElkGraph(graphFixture);

    const orderLines = graph.children!.find(n => n.id === '//ch/order_lines')!;
    expect(orderLines.width).toBe(NODE_WIDTH);
    expect(orderLines.height).toBe(HEADER_HEIGHT + 2 * ROW_HEIGHT);
    expect(orderLines.ports).toEqual([
      {
        id: '//ch/order_lines/columns/tenant_sku#in',
        x: 0,
        y: HEADER_HEIGHT + ROW_HEIGHT / 2,
      },
      {
        id: '//ch/order_lines/columns/tenant_sku#out',
        x: NODE_WIDTH,
        y: HEADER_HEIGHT + ROW_HEIGHT / 2,
      },
      {
        id: '//ch/order_lines/columns/price#in',
        x: 0,
        y: HEADER_HEIGHT + ROW_HEIGHT * 1.5,
      },
      {
        id: '//ch/order_lines/columns/price#out',
        x: NODE_WIDTH,
        y: HEADER_HEIGHT + ROW_HEIGHT * 1.5,
      },
    ]);
  });

  it('connects a source field out port to a target field in port', () => {
    const graph = buildElkGraph(graphFixture);

    expect(graph.edges).toContainEqual({
      id: edgeId(graphFixture.edges[3]),
      sources: ['//ch/order_lines/columns/tenant_sku#out'],
      targets: ['//ch/order_facts/columns/tenant_sku#in'],
    });
  });
});

describe('connectedEdgeIds', () => {
  it('follows a field upstream and downstream, not into sibling branches', () => {
    const ids = connectedEdgeIds(
      graphFixture.edges,
      '//ch/order_lines/columns/tenant_sku'
    );

    expect(ids).toEqual(
      new Set([
        edgeId(graphFixture.edges[0]),
        edgeId(graphFixture.edges[1]),
        edgeId(graphFixture.edges[3]),
      ])
    );
  });

  it('from a source field reaches the whole downstream chain only', () => {
    const ids = connectedEdgeIds(graphFixture.edges, '//ch/products/columns/sku');

    expect(ids).toEqual(
      new Set([edgeId(graphFixture.edges[1]), edgeId(graphFixture.edges[3])])
    );
  });
});
