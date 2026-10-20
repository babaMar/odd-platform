CREATE TABLE IF NOT EXISTS field_lineage_edge
(
    id                  bigserial PRIMARY KEY,
    relationship_id     bigint        NOT NULL,
    output_field_oddrn  varchar(512)  NOT NULL,
    input_field_oddrns  varchar(512)[] NOT NULL,
    expression          text,

    CONSTRAINT field_lineage_edge_fk_relationship FOREIGN KEY (relationship_id)
        REFERENCES relationships (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS field_lineage_edge_relationship_id_idx ON field_lineage_edge (relationship_id);
CREATE INDEX IF NOT EXISTS field_lineage_edge_output_field_oddrn_idx ON field_lineage_edge (output_field_oddrn);
CREATE INDEX IF NOT EXISTS field_lineage_edge_input_field_oddrns_idx ON field_lineage_edge USING gin (input_field_oddrns);
