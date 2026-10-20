import React, { type FC } from 'react';
import { ToggleButton, ToggleButtonGroup } from '@mui/material';
import { useTranslation } from 'react-i18next';

export type LineageView = 'datasets' | 'fields';

interface LineageViewToggleProps {
  view: LineageView;
  onChange: (view: LineageView) => void;
}

const LineageViewToggle: FC<LineageViewToggleProps> = ({ view, onChange }) => {
  const { t } = useTranslation();

  return (
    <ToggleButtonGroup
      size='small'
      exclusive
      value={view}
      onChange={(_, value: LineageView | null) => value && onChange(value)}
      sx={{ mb: 1 }}
    >
      <ToggleButton value='datasets'>{t('Datasets')}</ToggleButton>
      <ToggleButton value='fields'>{t('Fields')}</ToggleButton>
    </ToggleButtonGroup>
  );
};

export default LineageViewToggle;
