import React from 'react';
import { Grid, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { DataEntityRef, FieldLineageRelationshipDetails } from 'generated-sources';
import { RelationshipDatasetInfo } from '../RelationshipDatasetInfo';

interface FieldLineageRelationshipProps {
  sourceDataEntity: DataEntityRef;
  targetDataEntity: DataEntityRef;
  fieldLineageRelationship: FieldLineageRelationshipDetails;
}

export const FieldLineageRelationship: React.FC<FieldLineageRelationshipProps> = ({
  sourceDataEntity,
  targetDataEntity,
  fieldLineageRelationship,
}) => {
  const { t } = useTranslation();

  return (
    <Grid item container flexWrap='nowrap' columnGap={1}>
      <Grid
        item
        container
        xs={3}
        direction='column'
        alignItems='flex-start'
        alignContent='flex-start'
        sx={{ overflow: 'hidden' }}
      >
        <Typography variant='h4' sx={{ mb: 2 }}>
          {t('Source:')}
        </Typography>
        <RelationshipDatasetInfo
          dataEntityId={sourceDataEntity.id}
          name={sourceDataEntity.internalName || sourceDataEntity.externalName || ''}
          oddrn={sourceDataEntity.oddrn || ''}
        />
      </Grid>
      <Grid
        item
        container
        xs={3}
        direction='column'
        alignItems='flex-start'
        alignContent='flex-start'
        sx={{ overflow: 'hidden' }}
      >
        <Typography variant='h4' sx={{ mb: 2 }}>
          {t('Target:')}
        </Typography>
        <RelationshipDatasetInfo
          dataEntityId={targetDataEntity.id}
          name={targetDataEntity.internalName || targetDataEntity.externalName || ''}
          oddrn={targetDataEntity.oddrn || ''}
        />
      </Grid>
      <Grid
        item
        container
        xs={2}
        direction='column'
        alignItems='flex-start'
        alignContent='flex-start'
      >
        <Typography variant='h4' sx={{ mb: 2 }}>
          {t('Field edges:')}
        </Typography>
        <Typography variant='body1'>{fieldLineageRelationship.edges.length}</Typography>
      </Grid>
    </Grid>
  );
};
