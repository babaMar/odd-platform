import React from 'react';
import { Grid } from '@mui/material';
import { DataEntityClassNameEnum, type DataEntityDetails } from 'generated-sources';
import {
  EntityClassItem,
  FieldLineageEdgesTable,
  FieldLineageRelationship,
} from 'components/shared/elements';
import { useGetFieldLineageRelationshipById } from 'lib/hooks/api/dataModelling/relatioships';

interface OverviewFieldLineageRelationshipProps {
  dataEntityDetails: DataEntityDetails;
}

const OverviewFieldLineageRelationship: React.FC<
  OverviewFieldLineageRelationshipProps
> = ({ dataEntityDetails }) => {
  const { data: relationshipDetails } = useGetFieldLineageRelationshipById(
    dataEntityDetails.id
  );
  const fieldLineage = relationshipDetails?.fieldLineageRelationship;

  return (
    <Grid container>
      <Grid item xs={12} sx={{ mb: 1.25 }}>
        <EntityClassItem
          entityClassName={DataEntityClassNameEnum.RELATIONSHIP}
          fullName
        />
      </Grid>
      {relationshipDetails && fieldLineage && (
        <>
          <FieldLineageRelationship
            sourceDataEntity={relationshipDetails.sourceDataEntity}
            targetDataEntity={relationshipDetails.targetDataEntity}
            fieldLineageRelationship={fieldLineage}
          />
          <Grid item xs={12} sx={{ mt: 1.25 }}>
            <FieldLineageEdgesTable edges={fieldLineage.edges} />
          </Grid>
        </>
      )}
    </Grid>
  );
};

export default OverviewFieldLineageRelationship;
