package com.fisiogestao.config;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.query.sqm.FetchClauseType;

public class PostgreSqlCompatibleDialect extends PostgreSQLDialect {

    @Override
    public LimitHandler getLimitHandler() {
        return LimitOffsetLimitHandler.INSTANCE;
    }

    @Override
    public boolean supportsFetchClause(FetchClauseType type) {
        return false;
    }
}
