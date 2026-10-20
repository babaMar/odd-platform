import React, { type FC } from 'react';
import { Grid, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { DatasetFieldLineageLink } from 'generated-sources';
import { FieldLineageFieldLink } from 'components/shared/elements';
import { useDatasetFieldLineage } from 'lib/hooks/api';
import * as S from '../DatasetFieldOverview.styles';

interface DatasetFieldLineageProps {
  datasetFieldId: number;
}

const DatasetFieldLineage: FC<DatasetFieldLineageProps> = ({ datasetFieldId }) => {
  const { t } = useTranslation();
  const { data } = useDatasetFieldLineage({ datasetFieldId });

  if (!data) return null;

  const links = (title: string, items: DatasetFieldLineageLink[]) =>
    items.length > 0 && (
      <Grid container flexDirection='column' alignItems='flex-start' mt={1}>
        <Typography variant='subtitle1'>{title}</Typography>
        {items.map(({ field, relationshipId }) => (
          <FieldLineageFieldLink key={`${relationshipId}-${field.oddrn}`} field={field} />
        ))}
      </Grid>
    );

  return (
    <S.SectionContainer container>
      <Typography variant='h5' color='texts.hint'>
        {t('FIELD LINEAGE')}
      </Typography>
      {data.upstream.length === 0 && data.downstream.length === 0 ? (
        <Typography mt={1} variant='subtitle1'>
          {t('No field lineage')}
        </Typography>
      ) : (
        <>
          {links(t('Upstream fields'), data.upstream)}
          {links(t('Downstream fields'), data.downstream)}
        </>
      )}
    </S.SectionContainer>
  );
};

export default DatasetFieldLineage;
