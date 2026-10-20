import React from 'react';
import { describe, expect, it, vi } from 'vitest';
import { fireEvent, screen } from '@testing-library/react';
import { render } from 'lib/tests/testHelpers';
import LineageViewToggle from '../LineageViewToggle';

describe('LineageViewToggle', () => {
  it('switches between dataset and field lineage', () => {
    const onChange = vi.fn();
    render(<LineageViewToggle view='datasets' onChange={onChange} />);

    expect(screen.getByRole('button', { name: 'Datasets' })).toHaveAttribute(
      'aria-pressed',
      'true'
    );
    fireEvent.click(screen.getByRole('button', { name: 'Fields' }));

    expect(onChange).toHaveBeenCalledWith('fields');
  });
});
