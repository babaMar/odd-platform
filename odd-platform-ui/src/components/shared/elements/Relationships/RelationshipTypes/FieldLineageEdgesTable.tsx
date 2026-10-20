import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { FieldLineageEdge } from 'generated-sources';
import { FieldLineageFieldLink } from '../FieldLineageFieldLink';

interface FieldLineageEdgesTableProps {
  edges: FieldLineageEdge[];
}

export const FieldLineageEdgesTable: React.FC<FieldLineageEdgesTableProps> = ({
  edges,
}) => {
  const { t } = useTranslation();

  return (
    <Table size='small'>
      <TableHead>
        <TableRow>
          <TableCell>{t('Output field')}</TableCell>
          <TableCell>{t('Derived from')}</TableCell>
          <TableCell>{t('Expression')}</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {edges.map(edge => (
          <TableRow key={edge.output.oddrn}>
            <TableCell>
              <FieldLineageFieldLink field={edge.output} />
            </TableCell>
            <TableCell>
              {edge.inputs.map(input => (
                <FieldLineageFieldLink key={input.oddrn} field={input} />
              ))}
            </TableCell>
            <TableCell>
              <Typography variant='body1' fontFamily='monospace'>
                {edge.expression}
              </Typography>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
};
