package com.society.leagues.cache;

import com.society.leagues.client.api.domain.*;
import org.apache.log4j.Logger;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;


public class CachedCollection<T extends List<LeagueObject>> implements Comparable<CachedCollection> {
    final AtomicReference<T> entity = new AtomicReference(new CopyOnWriteArrayList<>());
    private static Logger logger = Logger.getLogger(CachedCollection.class);
    final MongoRepository repo;
    final String collectionName;
    Integer order = Integer.MAX_VALUE;

    public CachedCollection(MongoRepository repo) {
        this.repo = repo;
        String name = repo.getClass().getInterfaces()[0].getSimpleName().replace("Repository","");
        this.collectionName = name.substring(0,1).toLowerCase() + name.substring(1,name.length());
        setOrder();
    }

    private void setOrder() {
        int i = 0;
        if (collectionName.equals("season")) {
            order = i+0;
            return;
        }

        if (collectionName.equals("user")) {
            order = i+1;
            return;
        }

        if (collectionName.equals("teamMembers")) {
            order = i+2;
            return;
        }

        if (collectionName.equals("team")) {
            order = i+3;
            return;
        }

        if (collectionName.equals("teamMatch")) {
            order = i+4;
            return;
        }

        if (collectionName.equals("playerResult")) {
            order = i+5;
            return;
        }
        order = 6;
    }

    public T get() {
        return entity.get();
    }

    public synchronized void add(LeagueObject obj) {
        LeagueObject cached =  this.get().stream().filter(u -> ((LeagueObject) u).getId().equals(obj.getId())).findFirst().orElse(null);
        if (cached == null) {
            entity.get().add(obj);
        } else {
            cached.merge(obj);
        }
    }

    public void set(T collection) {
        this.entity.lazySet(collection);
    }

    public synchronized void remove(LeagueObject obj) {
        this.entity.get().remove(obj);
        getRepo().delete(obj);
    }

    public MongoRepository getRepo() {
        return repo;
    }

    public String getCollection() {
        return collectionName;
    }

    @SuppressWarnings("unchecked")
    public void refresh() {
        logger.info("Refreshing " + collectionName);
        this.set((T) this.repo.findAll());
        logger.info("Done " + collectionName);
    }

    @Override
    public int compareTo(CachedCollection o) {
        return this.order.compareTo(o.order);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CachedCollection<?> that = (CachedCollection<?>) o;

        return collectionName.equals(that.collectionName);

    }

    @Override
    public int hashCode() {
        return collectionName.hashCode();
    }


}
