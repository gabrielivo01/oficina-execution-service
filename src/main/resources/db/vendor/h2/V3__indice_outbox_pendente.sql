-- Usado só nos testes (@DataJpaTest/@SpringBootTest com H2): o H2 não suporta índice parcial.
CREATE INDEX outbox_pendente_idx ON outbox (criado_em);
