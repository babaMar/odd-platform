import type { DataEntityRef, FieldLineageGraph } from 'generated-sources';

const f = (dataset: string, name: string) => ({
  oddrn: `//ch/${dataset}/columns/${name}`,
  name,
  dataEntityName: dataset,
});

// products.(tenant_id, sku) -> order_lines.tenant_sku -> order_facts.tenant_sku; products.price -> order_lines.price
export const graphFixture: FieldLineageGraph = {
  nodes: [
    {
      datasetOddrn: '//ch/products',
      dataEntity: {
        id: 1,
        oddrn: '//ch/products',
        externalName: 'products',
      } as DataEntityRef,
      fields: [f('products', 'tenant_id'), f('products', 'sku'), f('products', 'price')],
    },
    {
      datasetOddrn: '//ch/order_lines',
      dataEntity: {
        id: 2,
        oddrn: '//ch/order_lines',
        externalName: 'order_lines',
      } as DataEntityRef,
      fields: [f('order_lines', 'tenant_sku'), f('order_lines', 'price')],
    },
    {
      datasetOddrn: '//ch/order_facts',
      fields: [f('order_facts', 'tenant_sku')],
    },
  ],
  edges: [
    {
      sourceFieldOddrn: '//ch/products/columns/tenant_id',
      targetFieldOddrn: '//ch/order_lines/columns/tenant_sku',
      relationshipId: 5,
    },
    {
      sourceFieldOddrn: '//ch/products/columns/sku',
      targetFieldOddrn: '//ch/order_lines/columns/tenant_sku',
      relationshipId: 5,
    },
    {
      sourceFieldOddrn: '//ch/products/columns/price',
      targetFieldOddrn: '//ch/order_lines/columns/price',
      relationshipId: 5,
    },
    {
      sourceFieldOddrn: '//ch/order_lines/columns/tenant_sku',
      targetFieldOddrn: '//ch/order_facts/columns/tenant_sku',
      relationshipId: 6,
    },
  ],
};
