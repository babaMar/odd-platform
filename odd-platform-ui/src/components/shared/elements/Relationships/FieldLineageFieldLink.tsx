import React from 'react';
import { Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import type { FieldLineageFieldRef } from 'generated-sources';
import { datasetStructureFieldPath } from 'routes/dataEntitiesRoutes';
import AppTooltip from '../AppTooltip/AppTooltip';

interface FieldLineageFieldLinkProps {
  field: FieldLineageFieldRef;
}

export const fieldLineageFieldLabel = ({ dataEntityName, name }: FieldLineageFieldRef) =>
  dataEntityName ? `${dataEntityName}.${name}` : name;

// a field the platform doesn't know (yet) has no ids: show its name, with the oddrn on hover
export const FieldLineageFieldLink = ({ field }: FieldLineageFieldLinkProps) => {
  const label = fieldLineageFieldLabel(field);

  if (!field.dataEntityId || !field.fieldId) {
    return (
      <AppTooltip title={field.oddrn} checkForOverflow={false}>
        <Typography variant='body1'>{label}</Typography>
      </AppTooltip>
    );
  }

  return (
    <Link to={datasetStructureFieldPath(field.dataEntityId, field.fieldId)}>
      <Typography variant='body1' color='button.link.normal.color' fontWeight={500}>
        {label}
      </Typography>
    </Link>
  );
};
