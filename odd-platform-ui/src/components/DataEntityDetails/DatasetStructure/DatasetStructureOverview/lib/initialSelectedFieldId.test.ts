import { describe, expect, it } from 'vitest';
import type { DataSetField } from 'generated-sources';
import { initialSelectedFieldId } from './initialSelectedFieldId';

const root = [{ id: 1 }, { id: 2 }] as DataSetField[];

describe('initialSelectedFieldId', () => {
  it('selects the field from the URL', () => {
    expect(initialSelectedFieldId(root, 2)).toBe(2);
  });

  it('selects the first field when the URL has none', () => {
    expect(initialSelectedFieldId(root, NaN)).toBe(1);
  });

  it('selects the URL field even when it is nested (not in the root list)', () => {
    expect(initialSelectedFieldId(root, 99)).toBe(99);
  });

  it('selects nothing for an empty structure without a URL field', () => {
    expect(initialSelectedFieldId([], NaN)).toBe(0);
  });
});
