/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.connections.jpa.updater.liquibase.lock;

import liquibase.database.core.DerbyDatabase;
import liquibase.exception.DatabaseException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.lockservice.StandardLockService;
import liquibase.statement.core.CreateDatabaseChangeLogLockTableStatement;
import liquibase.statement.core.DropTableStatement;
import liquibase.statement.core.InitializeDatabaseChangeLogLockTableStatement;
import liquibase.statement.core.LockDatabaseChangeLogStatement;
import liquibase.statement.core.RawSqlStatement;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.reflections.Reflections;

import java.lang.reflect.Field;

/**
 * Liquibase lock service, which has some bugfixes and assumes timeouts to be configured in milliseconds
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CustomLockService extends StandardLockService {

    private static final Logger log = Logger.getLogger(CustomLockService.class);

    @Override
    public void init() throws DatabaseException {
        boolean createdTable = false;
        Executor executor = ExecutorService.getInstance().getExecutor(database);

        if (!hasDatabaseChangeLogLockTable()) {

            try {
                if (log.isTraceEnabled()) {
                    log.trace("Create Database Lock Table");
                }
                executor.execute(new CreateDatabaseChangeLogLockTableStatement());
                database.commit();
            } catch (DatabaseException de) {
                log.warn("Failed to create lock table. Maybe other transaction created in the meantime. Retrying...");
                if (log.isTraceEnabled()) {
                    log.trace(de.getMessage(), de); //Log details at trace level
                }
                database.rollback();
                throw new LockRetryException(de);
            }

            log.debugf("Created database lock table with name: %s", database.escapeTableName(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), database.getDatabaseChangeLogLockTableName()));

            try {
                Field field = Reflections.findDeclaredField(StandardLockService.class, "hasDatabaseChangeLogLockTable");
                Reflections.setAccessible(field);
                field.set(CustomLockService.this, true);
            } catch (IllegalAccessException iae) {
                throw new RuntimeException(iae);
            }

            createdTable = true;
        }


        if (!isDatabaseChangeLogLockTableInitialized(createdTable)) {
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Initialize Database Lock Table");
                }
                executor.execute(new InitializeDatabaseChangeLogLockTableStatement());
                database.commit();

            } catch (DatabaseException de) {
                log.warn("Failed to insert first record to the lock table. Maybe other transaction inserted in the meantime. Retrying...");
                if (log.isTraceEnabled()) {
                    log.trace(de.getMessage(), de); // Log details at trace level
                }
                database.rollback();
                throw new LockRetryException(de);
            }

            log.debug("Initialized record in the database lock table");
        }


        // Keycloak doesn't support Derby, but keep it for sure...
        if (executor.updatesDatabase() && database instanceof DerbyDatabase && ((DerbyDatabase) database).supportsBooleanDataType()) { //check if the changelog table is of an old smallint vs. boolean format
            String lockTable = database.escapeTableName(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), database.getDatabaseChangeLogLockTableName());
            Object obj = executor.queryForObject(new RawSqlStatement("select min(locked) as test from " + lockTable + " fetch first row only"), Object.class);
            if (!(obj instanceof Boolean)) { //wrong type, need to recreate table
                executor.execute(new DropTableStatement(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName(), database.getDatabaseChangeLogLockTableName(), false));
                executor.execute(new CreateDatabaseChangeLogLockTableStatement());
                executor.execute(new InitializeDatabaseChangeLogLockTableStatement());
            }
        }

    }

    @Override
    public void waitForLock() {
        boolean locked = false;
        long startTime = Time.toMillis(Time.currentTime());
        long timeToGiveUp = startTime + (getChangeLogLockWaitTime());
        boolean nextAttempt = true;

        while (nextAttempt) {
            locked = acquireLock();
            if (!locked) {
                int remainingTime = ((int)(timeToGiveUp / 1000)) - Time.currentTime();
                if (remainingTime > 0) {
                    log.debugf("Will try to acquire log another time. Remaining time: %d seconds", remainingTime);
                } else {
                    nextAttempt = false;
                }
            } else {
                nextAttempt = false;
            }
        }

        if (!locked) {
            int timeout = ((int)(getChangeLogLockWaitTime() / 1000));
            throw new IllegalStateException("Could not acquire change log lock within specified timeout " + timeout + " seconds.  Currently locked by other transaction");
        }
    }

    @Override
    public boolean acquireLock() {
        if (hasChangeLogLock) {
            // We already have a lock
            return true;
        }

        Executor executor = ExecutorService.getInstance().getExecutor(database);

        try {
            database.rollback();

            // Ensure table created and lock record inserted
            this.init();
        } catch (DatabaseException de) {
            throw new IllegalStateException("Failed to retrieve lock", de);
        }

        try {
            log.debug("Trying to lock database");
            executor.execute(new LockDatabaseChangeLogStatement());
            log.debug("Successfully acquired database lock");

            hasChangeLogLock = true;
            database.setCanCacheLiquibaseTableInfo(true);
            return true;

        } catch (DatabaseException de) {
            log.warn("Lock didn't yet acquired. Will possibly retry to acquire lock. Details: " + de.getMessage());
            if (log.isTraceEnabled()) {
                log.debug(de.getMessage(), de);
            }
            return false;
        }
    }


    @Override
    public void releaseLock() {
        try {
            if (hasChangeLogLock) {
                log.debug("Going to release database lock");
                database.commit();
            } else {
                log.warn("Attempt to release lock, which is not owned by current transaction");
            }
        } catch (Exception e) {
            log.error("Database error during release lock", e);
        } finally {
            try {
                hasChangeLogLock = false;
                database.setCanCacheLiquibaseTableInfo(false);
                database.rollback();
            } catch (DatabaseException e) {
                ;
            }
        }
    }


}
