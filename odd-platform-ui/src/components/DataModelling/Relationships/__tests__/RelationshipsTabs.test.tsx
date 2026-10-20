import React from 'react';
import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from 'lib/tests/testHelpers';
import RelationshipsTabs from '../RelationshipsTabs';

describe('RelationshipsTabs', () => {
  it('offers a field lineage tab', () => {
    render(<RelationshipsTabs />);

    expect(screen.getByRole('tab', { name: 'Field lineage' })).toBeInTheDocument();
  });
});
