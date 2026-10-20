import React from 'react';
import { describe, expect, it } from 'vitest';
import { screen, within } from '@testing-library/react';
import type { DataEntityRef, FieldLineageRelationshipDetails } from 'generated-sources';
import { render } from 'lib/tests/testHelpers';
import { datasetStructureFieldPath } from 'routes/dataEntitiesRoutes';
import { FieldLineageRelationship } from '../FieldLineageRelationship';
import { FieldLineageEdgesTable } from '../FieldLineageEdgesTable';

const products = {
  id: 1,
  oddrn: '//ch/products',
  externalName: 'products',
} as DataEntityRef;
const orderLines = {
  id: 2,
  oddrn: '//ch/order_lines',
  externalName: 'order_lines',
} as DataEntityRef;

const details: FieldLineageRelationshipDetails = {
  edges: [
    {
      inputs: [
        {
          oddrn: '//ch/products/columns/tenant_id',
          name: 'tenant_id',
          fieldId: 11,
          dataEntityId: 1,
          dataEntityName: 'products',
        },
        {
          oddrn: '//ch/products/columns/sku',
          name: 'sku',
          fieldId: 12,
          dataEntityId: 1,
          dataEntityName: 'products',
        },
      ],
      output: {
        oddrn: '//ch/order_lines/columns/tenant_sku',
        name: 'tenant_sku',
        fieldId: 21,
        dataEntityId: 2,
        dataEntityName: 'order_lines',
      },
      expression: 'concat(tenant_id, sku)',
    },
    {
      inputs: [{ oddrn: '//ch/unknown/columns/price', name: 'price' }],
      output: {
        oddrn: '//ch/order_lines/columns/price',
        name: 'price',
        fieldId: 22,
        dataEntityId: 2,
        dataEntityName: 'order_lines',
      },
    },
  ],
};

describe('FieldLineageRelationship', () => {
  it('shows source and target datasets and the number of field edges', () => {
    render(
      <FieldLineageRelationship
        sourceDataEntity={products}
        targetDataEntity={orderLines}
        fieldLineageRelationship={details}
      />
    );

    expect(screen.getByText('Source:')).toBeInTheDocument();
    expect(screen.getByText('products')).toBeInTheDocument();
    expect(screen.getByText('Target:')).toBeInTheDocument();
    expect(screen.getByText('order_lines')).toBeInTheDocument();
    expect(screen.getByText('Field edges:')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });
});

describe('FieldLineageEdgesTable', () => {
  it('lists each output field with the fields it is derived from, linking known fields', () => {
    render(<FieldLineageEdgesTable edges={details.edges} />);

    const rows = screen.getAllByRole('row').slice(1);
    expect(rows).toHaveLength(2);

    const first = within(rows[0]);
    expect(first.getByRole('link', { name: 'order_lines.tenant_sku' })).toHaveAttribute(
      'href',
      datasetStructureFieldPath(2, 21)
    );
    expect(first.getByRole('link', { name: 'products.tenant_id' })).toHaveAttribute(
      'href',
      datasetStructureFieldPath(1, 11)
    );
    expect(first.getByRole('link', { name: 'products.sku' })).toBeInTheDocument();
    expect(first.getByText('concat(tenant_id, sku)')).toBeInTheDocument();

    const second = within(rows[1]);
    expect(second.queryByRole('link', { name: 'price' })).toBeNull();
    expect(second.getByText('price')).toBeInTheDocument();
  });
});
