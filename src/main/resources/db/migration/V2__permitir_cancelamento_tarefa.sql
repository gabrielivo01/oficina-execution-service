ALTER TABLE tarefa_execucao
    DROP CONSTRAINT tarefa_status_check;

ALTER TABLE tarefa_execucao
    ADD CONSTRAINT tarefa_status_check
        CHECK (status IN ('NA_FILA', 'EM_ANDAMENTO', 'CONCLUIDA', 'RECUSADA', 'CANCELADA'));
