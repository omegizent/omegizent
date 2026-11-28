/*

Copyright 2026 Jeffrey J. Weston <jjweston@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/

CREATE TABLE embedding_model
(
    id    INTEGER  PRIMARY KEY,
    name  TEXT     NOT NULL
);

CREATE UNIQUE INDEX embedding_model_name_uix
    ON embedding_model ( name );

INSERT INTO embedding_model ( id, name )
     VALUES ( 1, 'text-embedding-3-small' );

CREATE TABLE embedding_new
(
    id                  INTEGER  PRIMARY KEY,
    embedding_model_id  INTEGER  NOT NULL  REFERENCES embedding_model ( id ),
    input               TEXT     NOT NULL,
    vector              TEXT     NOT NULL
);

INSERT INTO embedding_new ( id, embedding_model_id, input, vector )
     SELECT id, 1, input, vector
       FROM embedding;

DROP TABLE embedding;

ALTER TABLE embedding_new RENAME TO embedding;

CREATE UNIQUE INDEX embedding_embedding_model_id_input_uix
    ON embedding ( embedding_model_id, input );
