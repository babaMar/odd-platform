import React from 'react';
import type { Mock } from 'vitest';
import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from 'lib/tests/testHelpers';
import { useDatasetFieldLineage } from 'lib/hooks/api';
import { datasetStructureFieldPath } from 'routes/dataEntitiesRoutes';
import DatasetFieldLineage from '../DatasetFieldLineage';

vi.mock('lib/hooks/api', () => ({ useDatasetFieldLineage: vi.fn() }));

const ref = (dataset: string, name: string, dataEntityId?: number, fieldId?: number) => ({
  oddrn: `//ch/${dataset}/columns/${name}`,
  name,
  dataEntityId,
  fieldId,
  dataEntityName: dataset,
});

describe('DatasetFieldLineage', () => {
  it('lists upstream and downstream fields with links', () => {
    (useDatasetFieldLineage as Mock).mockImplementation(() => ({
      data: {
        upstream: [
          { field: ref('products', 'tenant_id', 1, 11), relationshipId: 5 },
          { field: ref('products', 'sku', 1, 12), relationshipId: 5 },
        ],
        downstream: [
          { field: ref('order_facts', 'tenant_sku', 3, 31), relationshipId: 6 },
        ],
      },
    }));

    render(<DatasetFieldLineage datasetFieldId={21} />);

    expect(useDatasetFieldLineage).toHaveBeenCalledWith({ datasetFieldId: 21 });
    expect(screen.getByText('FIELD LINEAGE')).toBeInTheDocument();
    expect(screen.getByText('Upstream fields')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'products.tenant_id' })).toHaveAttribute(
      'href',
      datasetStructureFieldPath(1, 11)
    );
    expect(screen.getByRole('link', { name: 'products.sku' })).toBeInTheDocument();
    expect(screen.getByText('Downstream fields')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'order_facts.tenant_sku' })).toHaveAttribute(
      'href',
      datasetStructureFieldPath(3, 31)
    );
  });

  it('says so when the field has no lineage', () => {
    (useDatasetFieldLineage as Mock).mockImplementation(() => ({
      data: { upstream: [], downstream: [] },
    }));

    render(<DatasetFieldLineage datasetFieldId={21} />);

    expect(screen.getByText('No field lineage')).toBeInTheDocument();
    expect(screen.queryByText('Upstream fields')).toBeNull();
  });
});
