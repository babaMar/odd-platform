import type { DataSetField } from 'generated-sources';

// the field in the URL (/structure/overview/field/:fieldId) wins; otherwise the first column
// (0 selects nothing)
export const initialSelectedFieldId = (
  datasetStructureRoot: DataSetField[] | undefined,
  urlFieldId: number
): number =>
  Number.isNaN(urlFieldId) ? (datasetStructureRoot?.[0]?.id ?? 0) : urlFieldId;
