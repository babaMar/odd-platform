import React from 'react';
import type { Mock } from 'vitest';
import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { DataEntityTypeNameEnum, type DataEntityDetails } from 'generated-sources';
import { render } from 'lib/tests/testHelpers';
import { useGetFieldLineageRelationshipById } from 'lib/hooks/api/dataModelling/relatioships';
import OverviewRelationshipStats from '../OverviewRelationshipStats';

vi.mock('lib/hooks/api/dataModelling/relatioships', () => ({
  useGetEDRRelationshipById: vi.fn(() => ({})),
  useGetGraphRelationshipById: vi.fn(() => ({})),
  useGetFieldLineageRelationshipById: vi.fn(),
}));

describe('OverviewRelationshipStats', () => {
  it('shows a field lineage relationship with its edges', () => {
    (useGetFieldLineageRelationshipById as Mock).mockImplementation(() => ({
      data: {
        id: 7,
        name: 'products_order_lines',
        sourceDataEntity: { id: 1, oddrn: '//ch/products', externalName: 'products' },
        targetDataEntity: {
          id: 2,
          oddrn: '//ch/order_lines',
          externalName: 'order_lines',
        },
        fieldLineageRelationship: {
          edges: [
            {
              inputs: [
                {
                  oddrn: '//ch/products/columns/sku',
                  name: 'sku',
                  dataEntityName: 'products',
                },
              ],
              output: {
                oddrn: '//ch/order_lines/columns/sku',
                name: 'sku',
                dataEntityName: 'order_lines',
              },
            },
          ],
        },
      },
    }));

    render(
      <OverviewRelationshipStats
        dataEntityDetails={
          {
            id: 7,
            type: { id: 28, name: DataEntityTypeNameEnum.FIELD_LINEAGE_RELATIONSHIP },
          } as DataEntityDetails
        }
      />
    );

    expect(useGetFieldLineageRelationshipById).toHaveBeenCalledWith(7);
    expect(screen.getByText('Source:')).toBeInTheDocument();
    expect(screen.getByText('order_lines.sku')).toBeInTheDocument();
    expect(screen.getByText('products.sku')).toBeInTheDocument();
  });
});
