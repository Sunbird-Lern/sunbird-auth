package org.keycloak.models.sessions.infinispan.initializer;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.infinispan.util.concurrent.IsolationLevel;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;

import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Unit tests to make sure our model caching concurrency model will work.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Ignore
public class ConcurrencyVersioningTest {

    public static abstract class AbstractThread implements Runnable {
        EmbeddedCacheManager cacheManager;
        boolean success;
        CountDownLatch latch = new CountDownLatch(1);

        public AbstractThread(EmbeddedCacheManager cacheManager) {
            this.cacheManager = cacheManager;
        }

        public boolean isSuccess() {
            return success;
        }

        public CountDownLatch getLatch() {
            return latch;
        }
    }

    public static class RemoveThread extends AbstractThread {
        public RemoveThread(EmbeddedCacheManager cacheManager) {
            super(cacheManager);
        }

        public void run() {
            Cache<String, String> cache = cacheManager.getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
            try {
                startBatch(cache);
                cache.remove("key");
                //cache.getAdvancedCache().getTransactionManager().commit();
                endBatch(cache);
                success = true;
            } catch (Exception e) {
                success = false;
            }
            latch.countDown();
        }

    }


    public static class UpdateThread extends AbstractThread {
        public UpdateThread(EmbeddedCacheManager cacheManager) {
            super(cacheManager);
        }

        public void run() {
            Cache<String, String> cache = cacheManager.getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
            try {
                startBatch(cache);
                cache.putForExternalRead("key", "value2");
                //cache.getAdvancedCache().getTransactionManager().commit();
                endBatch(cache);
                success = true;
            } catch (Exception e) {
                success = false;
            }
            latch.countDown();
        }

    }

    /**
     * Tests that if remove executes before put, then put still succeeds.
     *
     * @throws Exception
     */
    @Test
    public void testGetRemovePutOnNonExisting() throws Exception {
        final DefaultCacheManager cacheManager = getVersionedCacheManager();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        RemoveThread removeThread = new RemoveThread(cacheManager);

        Cache<String, String> cache = cacheManager.getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
        cache.remove("key");
        startBatch(cache);
        cache.get("key");
        executor.execute(removeThread);
        removeThread.getLatch().await();
        cache.putForExternalRead("key", "value1");
        endBatch(cache);
        Assert.assertEquals(cache.get("key"), "value1");
        Assert.assertTrue(removeThread.isSuccess());
    }


    /**
     * Test that if a put of an existing key is removed after the put and before tx commit, it is evicted
     *
     * @throws Exception
     */
    @Test
    public void testGetRemovePutOnExisting() throws Exception {
        final DefaultCacheManager cacheManager = getVersionedCacheManager();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        RemoveThread removeThread = new RemoveThread(cacheManager);

        Cache<String, String> cache = cacheManager.getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
        cache.put("key", "value0");
        startBatch(cache);
        cache.get("key");
        executor.execute(removeThread);
        removeThread.getLatch().await();
        cache.put("key", "value1");
        try {
            endBatch(cache);
            Assert.fail("Write skew should be detected");
        } catch (Exception e) {


        }
        Assert.assertNull(cache.get("key"));
        Assert.assertTrue(removeThread.isSuccess());
    }

    /**
     * Test that if a put of an existing key is removed after the put and before tx commit, it is evicted
     *
     * @throws Exception
     */
    @Test
    public void testGetRemovePutEternalOnExisting() throws Exception {
        final DefaultCacheManager cacheManager = getVersionedCacheManager();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        RemoveThread removeThread = new RemoveThread(cacheManager);

        Cache<String, String> cache = cacheManager.getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
        cache.put("key", "value0");
        startBatch(cache);
        cache.get("key");
        executor.execute(removeThread);
        cache.putForExternalRead("key", "value1");
        removeThread.getLatch().await();
        try {
            endBatch(cache);
//            Assert.fail("Write skew should be detected");
        } catch (Exception e) {

        }
        Assert.assertNull(cache.get("key"));
        Assert.assertTrue(removeThread.isSuccess());
    }

    @Test
    public void testPutExternalRemoveOnExisting() throws Exception {
        final DefaultCacheManager cacheManager = getVersionedCacheManager();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        RemoveThread removeThread = new RemoveThread(cacheManager);

        Cache<String, String> cache = cacheManager.getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
        cache.put("key", "value0");
        startBatch(cache);
        cache.putForExternalRead("key", "value1");
        executor.execute(removeThread);
        removeThread.getLatch().await();
        try {
            endBatch(cache);
//            Assert.fail("Write skew should be detected");
        } catch (Exception e) {

        }
        Assert.assertNull(cache.get("key"));
        Assert.assertTrue(removeThread.isSuccess());
    }


    public static void startBatch(Cache<String, String> cache) {
        try {
            if (cache.getAdvancedCache().getTransactionManager().getStatus() == Status.STATUS_NO_TRANSACTION) {
                System.out.println("begin");
                cache.getAdvancedCache().getTransactionManager().begin();
            }
        } catch (NotSupportedException e) {
            throw new RuntimeException(e);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }

    }

    public static void endBatch(Cache<String, String> cache) {
        boolean commit = true;
        try {
            if (cache.getAdvancedCache().getTransactionManager().getStatus() == Status.STATUS_ACTIVE) {
                if (commit) {
                    cache.getAdvancedCache().getTransactionManager().commit();

                } else {
                    cache.getAdvancedCache().getTransactionManager().rollback();

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    protected DefaultCacheManager getVersionedCacheManager() {
        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();


        boolean clustered = false;
        boolean async = false;
        boolean allowDuplicateJMXDomains = true;

        if (clustered) {
            gcb.transport().defaultTransport();
        }
        gcb.globalJmxStatistics().allowDuplicateDomains(allowDuplicateJMXDomains);

        final DefaultCacheManager cacheManager = new DefaultCacheManager(gcb.build());
        ConfigurationBuilder invalidationConfigBuilder = new ConfigurationBuilder();
        invalidationConfigBuilder
                //.invocationBatching().enable()
                .transaction().transactionMode(TransactionMode.TRANSACTIONAL)
                .transaction().transactionManagerLookup(new DummyTransactionManagerLookup())
                .locking().isolationLevel(IsolationLevel.REPEATABLE_READ).writeSkewCheck(true).versioning().enable().scheme(VersioningScheme.SIMPLE);


        //invalidationConfigBuilder.locking().isolationLevel(IsolationLevel.REPEATABLE_READ).writeSkewCheck(true).versioning().enable().scheme(VersioningScheme.SIMPLE);

        if (clustered) {
            invalidationConfigBuilder.clustering().cacheMode(async ? CacheMode.INVALIDATION_ASYNC : CacheMode.INVALIDATION_SYNC);
        }
        Configuration invalidationCacheConfiguration = invalidationConfigBuilder.build();
        cacheManager.defineConfiguration(InfinispanConnectionProvider.REALM_CACHE_NAME, invalidationCacheConfiguration);
        return cacheManager;
    }
}
