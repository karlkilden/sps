CREATE TABLE item
(
    item_id bigint,
    data    json
);

ALTER TABLE item
    owner TO postgres;

INSERT INTO item (item_id, data)
VALUES (100, '{
  "producer": "Karl",
  "genre": "drama"
}'),
       (101, '{
         "rating": "pg13",
         "genre": "action"
       }'),
       (102, '{
         "rating": "pg9",
         "genre": "action"
       }');