import React from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAppSelector } from 'redux/lib/hooks';
import {
  getDataEntityDetailsFetchingStatuses,
  getIsDataEntityBelongsToClass,
} from 'redux/selectors';
import { useDataEntityRouteParams } from 'routes';
import DEGLineageAtomProvider from './DEGLineage/lib/DEGLineageAtomProvider';
import DEGLineage from './DEGLineage/DEGLineage';
import HierarchyLineage from './HierarchyLineage/HierarchyLineage';
import FieldLineage from './FieldLineage/FieldLineage';
import LineageViewToggle, { type LineageView } from './LineageViewToggle';

const Lineage: React.FC = () => {
  const { dataEntityId } = useDataEntityRouteParams();
  const { isDEG } = useAppSelector(getIsDataEntityBelongsToClass(dataEntityId));
  const { isLoaded } = useAppSelector(getDataEntityDetailsFetchingStatuses);
  const [searchParams, setSearchParams] = useSearchParams();
  const view: LineageView = searchParams.get('view') === 'fields' ? 'fields' : 'datasets';

  if (!isLoaded) return null;

  if (isDEG) {
    return (
      <DEGLineageAtomProvider>
        <DEGLineage />
      </DEGLineageAtomProvider>
    );
  }

  // the field view keeps only its own param, so the dataset view's zoom/expansion params don't leak
  const changeView = (next: LineageView) =>
    setSearchParams(next === 'fields' ? { view: 'fields' } : {});

  return (
    <>
      <LineageViewToggle view={view} onChange={changeView} />
      {view === 'fields' ? (
        <FieldLineage dataEntityId={dataEntityId} />
      ) : (
        <HierarchyLineage />
      )}
    </>
  );
};

export default Lineage;
