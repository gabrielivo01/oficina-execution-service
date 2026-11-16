CREATE TABLE peca (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    descricao TEXT,
    preco NUMERIC(10, 2) NOT NULL,
    quantidade_estoque INTEGER NOT NULL,
    estoque_minimo INTEGER NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP(6) NOT NULL,
    atualizado_em TIMESTAMP(6) NOT NULL,
    CONSTRAINT peca_preco_positivo CHECK (preco > 0),
    CONSTRAINT peca_estoque_nao_negativo CHECK (quantidade_estoque >= 0),
    CONSTRAINT peca_estoque_minimo_nao_negativo CHECK (estoque_minimo >= 0)
);
INSERT INTO peca (id, nome, descricao, preco, quantidade_estoque, estoque_minimo, versao, criado_em, atualizado_em)
VALUES
    ('c0a8012e-0003-4000-8000-000000000003', 'Filtro de Óleo', 'Filtro de óleo para motores', 25.50, 50, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0a8012e-0003-4000-8000-000000000004', 'Pastilha de Freio', 'Pastilha de freio dianteira', 120.00, 20, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0a8012e-0003-4000-8000-000000000005', 'Bateria 60Ah', 'Bateria automotiva 60Ah', 350.00, 15, 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0a8012e-0003-4000-8000-000000000006', 'Óleo 5W30', 'Óleo lubrificante sintético', 45.00, 30, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0a8012e-0003-4000-8000-000000000007', 'Velas de Ignição', 'Jogo com 4 velas', 85.00, 25, 6, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

CREATE TABLE reserva_pecas (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    os_id VARCHAR(36) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP(6) NOT NULL,
    atualizado_em TIMESTAMP(6) NOT NULL,
    CONSTRAINT reserva_pecas_status_check CHECK (status IN ('RESERVADA', 'CONSUMIDA', 'LIBERADA'))
);

CREATE TABLE item_reserva_pecas (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    reserva_id VARCHAR(36) NOT NULL,
    peca_id VARCHAR(36) NOT NULL,
    nome_peca VARCHAR(150) NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(10, 2) NOT NULL,
    CONSTRAINT item_reserva_quantidade_check CHECK (quantidade > 0),
    CONSTRAINT item_reserva_reserva_fk FOREIGN KEY (reserva_id) REFERENCES reserva_pecas (id)
);
CREATE INDEX item_reserva_reserva_idx ON item_reserva_pecas (reserva_id);

CREATE TABLE tarefa_execucao (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    os_id VARCHAR(36) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    placa VARCHAR(10),
    observacao VARCHAR(1000),
    prioridade INTEGER NOT NULL DEFAULT 0,
    progresso VARCHAR(2000),
    criado_em TIMESTAMP(6) NOT NULL,
    iniciado_em TIMESTAMP(6),
    concluido_em TIMESTAMP(6),
    CONSTRAINT tarefa_tipo_check CHECK (tipo IN ('DIAGNOSTICO', 'REPARO')),
    CONSTRAINT tarefa_status_check CHECK (status IN ('NA_FILA', 'EM_ANDAMENTO', 'CONCLUIDA', 'RECUSADA')),
    CONSTRAINT tarefa_os_tipo_uq UNIQUE (os_id, tipo)
);
CREATE INDEX tarefa_fila_idx ON tarefa_execucao (status, prioridade DESC, criado_em);

CREATE TABLE item_diagnostico (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    tarefa_id VARCHAR(36) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    descricao VARCHAR(150) NOT NULL,
    valor_unitario NUMERIC(10, 2) NOT NULL,
    peca_id VARCHAR(36),
    quantidade INTEGER NOT NULL,
    CONSTRAINT item_diagnostico_tipo_check CHECK (tipo IN ('SERVICO', 'PECA')),
    CONSTRAINT item_diagnostico_tarefa_fk FOREIGN KEY (tarefa_id) REFERENCES tarefa_execucao (id),
    CONSTRAINT item_diagnostico_quantidade_check CHECK (quantidade > 0)
);
CREATE INDEX item_diagnostico_tarefa_idx ON item_diagnostico (tarefa_id);

CREATE TABLE outbox (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    tipo VARCHAR(100) NOT NULL,
    exchange VARCHAR(150) NOT NULL,
    routing_key VARCHAR(150) NOT NULL,
    payload TEXT NOT NULL,
    criado_em TIMESTAMP(6) NOT NULL,
    enviado_em TIMESTAMP(6),
    tentativas INTEGER NOT NULL DEFAULT 0,
    trace_parent VARCHAR(100)
);
-- Índice dos pendentes do outbox: específico por banco em db/vendor/{vendor}/V3 (o H2 não tem índice parcial).

CREATE TABLE inbox (
    message_id VARCHAR(36) NOT NULL PRIMARY KEY,
    processado_em TIMESTAMP(6) NOT NULL
);
