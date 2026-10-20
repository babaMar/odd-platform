import styled, { type CSSObject } from 'styled-components';
import { HEADER_HEIGHT, NODE_WIDTH, ROW_HEIGHT } from './lib/graph';

export const Container = styled('div')(({ theme }) => ({
  display: 'flex',
  flexDirection: 'column',
  gap: theme.spacing(1),
  height: 'calc(100vh - 220px)',
  minHeight: '400px',
}));

export const Viewport = styled('div')(
  ({ theme }) =>
    ({
      flex: 1,
      position: 'relative',
      overflow: 'hidden',
      cursor: 'grab',
      border: `1px solid ${theme.palette.border.primary}`,
      borderRadius: '8px',
    }) as CSSObject
);

export const Canvas = styled('div')({
  position: 'absolute',
  top: 0,
  left: 0,
  transformOrigin: '0 0',
});

export const Node = styled('div')(({ theme }) => ({
  position: 'absolute',
  width: `${NODE_WIDTH}px`,
  boxSizing: 'border-box',
  backgroundColor: theme.palette.background.default,
  border: `1px solid ${theme.palette.border.primary}`,
  borderRadius: '8px',
  overflow: 'hidden',
}));

export const NodeHeader = styled('div')(({ theme }) => ({
  height: `${HEADER_HEIGHT}px`,
  display: 'flex',
  alignItems: 'center',
  padding: theme.spacing(0, 1.5),
  backgroundColor: theme.palette.backgrounds.primary,
  boxSizing: 'border-box',
  overflow: 'hidden',
  whiteSpace: 'nowrap',
  textOverflow: 'ellipsis',
}));

export const FieldRow = styled('div')<{ $highlighted: boolean }>(
  ({ theme, $highlighted }) => ({
    height: `${ROW_HEIGHT}px`,
    display: 'flex',
    alignItems: 'center',
    padding: theme.spacing(0, 1.5),
    boxSizing: 'border-box',
    cursor: 'pointer',
    backgroundColor: $highlighted ? theme.palette.backgrounds.secondary : 'transparent',
  })
);
