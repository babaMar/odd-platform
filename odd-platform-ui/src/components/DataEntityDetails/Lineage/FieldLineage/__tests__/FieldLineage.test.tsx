import React from 'react';
import type { Mock } from 'vitest';
import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from 'lib/tests/testHelpers';
import { useFieldLineageGraph } from 'lib/hooks/api';
import FieldLineage from '../FieldLineage';
import { graphFixture } from '../lib/__tests__/fixtures';

vi.mock('lib/hooks/api', () => ({ useFieldLineageGraph: vi.fn() }));

describe('FieldLineage', () => {
  it('draws each dataset with its lineage fields', async () => {
    (useFieldLineageGraph as Mock).mockImplementation(() => ({
      data: graphFixture,
      isSuccess: true,
    }));

    render(<FieldLineage dataEntityId={2} />);

    expect(await screen.findByText('products')).toBeInTheDocument();
    expect(screen.getByText('order_lines')).toBeInTheDocument();
    expect(screen.getByText('order_facts')).toBeInTheDocument();
    expect(screen.getAllByText('tenant_sku')).toHaveLength(2);
    expect(screen.getAllByTestId('field-lineage-edge')).toHaveLength(4);
  });

  it('says so when there is no field lineage', () => {
    (useFieldLineageGraph as Mock).mockImplementation(() => ({
      data: { nodes: [{ datasetOddrn: '//ch/x', fields: [] }], edges: [] },
      isSuccess: true,
    }));

    render(<FieldLineage dataEntityId={2} />);

    expect(screen.getByText('No field lineage')).toBeInTheDocument();
  });
});
