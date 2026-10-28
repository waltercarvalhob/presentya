-- Cria o banco BDPresentya no PostgreSQL local.
-- Execute no pgAdmin (Query Tool conectado ao banco "postgres") ou pelo terminal:
--   "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -h localhost -f database\criar-banco.sql
--
-- As tabelas são criadas automaticamente pela API (Spring Boot/JPA) na primeira execução,
-- junto com o usuário administrador, as categorias e os produtos iniciais.

CREATE DATABASE "BDPresentya"
    WITH ENCODING 'UTF8'
    TEMPLATE template0;
