-- Índice parcial: só as mensagens ainda não enviadas, que é o que o publicador do outbox consulta.
CREATE INDEX outbox_pendente_idx ON outbox (criado_em) WHERE enviado_em IS NULL;
