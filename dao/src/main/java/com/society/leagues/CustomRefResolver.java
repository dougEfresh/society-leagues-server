package com.society.leagues;

import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.society.leagues.client.api.domain.LeagueObject;
import org.apache.log4j.Logger;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.DbRefProxyHandler;
import org.springframework.data.mongodb.core.convert.DbRefResolverCallback;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;

import java.util.List;

public class CustomRefResolver extends DefaultDbRefResolver {
    private static Logger logger = Logger.getLogger(CustomRefResolver.class);
    final List<CachedCollection> cachedCollections;
    static long lookups = 1;
    static long cacheHits = 1;

    public CustomRefResolver(MongoDbFactory mongoDbFactory, List<CachedCollection> cachedCollections) {
        super(mongoDbFactory);
        this.cachedCollections = cachedCollections;
    }

    @Override
    public Object resolveDbRef(MongoPersistentProperty property, DBRef dbref, DbRefResolverCallback callback, DbRefProxyHandler handler) {

        if (dbref == null || dbref.getCollectionName() == null) {
            return super.resolveDbRef(property, dbref, callback, handler);
        }
        CachedCollection<List<LeagueObject>> cachedCollection =  getCache(dbref.getCollectionName());
        if (cachedCollection != null && cachedCollection.get() != null) {
            Object obj = cachedCollection.get().parallelStream().
                    filter(c-> c != null && c.getId() != null ).
                    filter(c -> c.getId().equals(dbref.getId().toString())).
                    findFirst().orElse(null);
            if (obj == null) {
                obj = super.resolveDbRef(property, dbref, callback, handler);
                cachedCollection.get().add((LeagueObject) obj);
                return obj;
            } else {
                cacheHits++;
                return obj;
            }
        }
        logger.info("No Cache for " + dbref.getCollectionName());
        return super.resolveDbRef(property, dbref, callback, handler);
    }

    @Override
    public DBObject fetch(DBRef dbRef) {
        if (dbRef == null) return null;
        if (dbRef.getCollectionName() == null) return super.fetch(dbRef);

        lookups++;
        logger.info(String.format("Cache Stats %s %s : Lookups: %d  CacheHits: %d Ratios: %f ",
                dbRef.getCollectionName(),dbRef,lookups,cacheHits,
                 ((double)lookups/ (double) cacheHits)));
        return super.fetch(dbRef);
    }

    @SuppressWarnings("unchecked")
    public CachedCollection<List<LeagueObject>> getCache(String collection) {
        /*
        for (CachedCollection cachedCollection : cachedCollections) {
            if (cachedCollection.getType().equals(collection)) {
                return cachedCollection;
            }
        }
        */
        return null;
    }
}
