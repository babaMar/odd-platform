import { describe, expect, it } from 'vitest';
import { layoutFieldLineage } from '../layoutFieldLineage';
import { graphFixture } from './fixtures';

describe('layoutFieldLineage', () => {
  it('places datasets left to right along the data flow and routes every edge', async () => {
    const layout = await layoutFieldLineage(graphFixture);

    const x = (oddrn: string) => layout.nodes.find(n => n.id === oddrn)!.x!;
    expect(x('//ch/products')).toBeLessThan(x('//ch/order_lines'));
    expect(x('//ch/order_lines')).toBeLessThan(x('//ch/order_facts'));

    expect(layout.edges).toHaveLength(4);
    layout.edges.forEach(edge => expect(edge.sections?.length).toBeGreaterThan(0));
    expect(layout.width).toBeGreaterThan(0);
    expect(layout.height).toBeGreaterThan(0);
  });
});
