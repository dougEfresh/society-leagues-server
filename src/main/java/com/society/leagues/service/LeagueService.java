package com.society.leagues.service;

import com.society.leagues.exception.InvalidLeagueObject;
import com.society.leagues.cache.CachedCollection;
import com.society.leagues.cache.CacheUtil;
import com.society.leagues.client.api.domain.*;
import com.society.leagues.listener.DaoListener;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.*;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

@Component
public class LeagueService {

    final static Logger logger = Logger.getLogger(LeagueService.class);
    @Autowired(required = false) List<DaoListener> daoListeners = new ArrayList<>();
    @Autowired CacheUtil cacheUtil;
    Validator validator;
    @Autowired List<MongoRepository> mongoRepositories;

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        if (daoListeners == null)
            daoListeners = new ArrayList<>();

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        cacheUtil.initialize(mongoRepositories);
        List<User> fakes = findAll(User.class).stream().filter(u->!u.isReal()).collect(Collectors.toList());
        final MongoRepository repo = cacheUtil.getCache(TeamMembers.class).getRepo();
        findAll(Team.class).stream().filter(t->t.getSeason() != null).filter(t->t.getSeason().isActive()).forEach(
                t->fakes.stream()
                        .forEach(u->{t.getMembers().removeMember(u);
                            repo.save(t.getMembers());})
        );
        List<TeamMatch> teamMatches = findAll(TeamMatch.class).stream().filter(t->
                t.getMatchDate() == null || t.getHome() == null || t.getAway() == null
        ).collect(Collectors.toList());
        for (TeamMatch teamMatch : teamMatches) {
            logger.info("Removing tm with no date: " + teamMatch.getId());
            //purge(teamMatch);
        }

        Map<String,List<TeamMatch>> matches = teamMatches.stream().filter(s->s.getSeason().isActive()).filter(s->s.getSeason().isScramble())
                .sorted(TeamMatch.sortAcc())
                .collect(Collectors.groupingBy(tm->tm.getMatchDate().toLocalDate().toString()));
        Map<String,List<TeamMatch>> sorted = new TreeMap<>(matches);
        Division division = Division.MIXED_NINE;
        for (String s : sorted.keySet()) {
            division = division == Division.MIXED_NINE ? Division.MIXED_EIGHT : Division.MIXED_NINE;
            logger.info("Setting division " + division);
            for (TeamMatch match : sorted.get(s)) {
                match.setDivision(division);
            }
            save(sorted.get(s));
        }

        purge(new Season("-1"));
        purge(new Team("-1"));
        purge(new User("-1"));
        purge(new TeamMatch("-1"));
        purge(new PlayerResult("-1"));
    }

    @SuppressWarnings("unchecked")
    public <T extends LeagueObject> T save(final T entity) {
        if ("-1".equals(entity.getId())) {
            throw new InvalidLeagueObject(entity.getId() + " " + entity.getClass().getCanonicalName());
        }
        MongoRepository repo = cacheUtil.getCache(entity).getRepo();
        Set<ConstraintViolation<T>> constraintViolations = validator.validate(entity);
        if (!constraintViolations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<T> constraintViolation : constraintViolations) {
                sb.append(constraintViolation.toString());
            }
            throw new InvalidLeagueObject("Could not validate " + entity + "\n" + sb.toString());
        }
        repo.save(entity);
        T newEntity = (T) repo.findOne(entity.getId());
        CachedCollection c = cacheUtil.getCache(entity);
        if (c != null) {
            c.add(newEntity);
        }
        for (DaoListener daoListener : daoListeners) {
            daoListener.onChange(newEntity);
        }
        return newEntity;
    }

    public <T extends LeagueObject> void save(final List<T> entities) {
        for (T entity : entities) {
            MongoRepository repo = cacheUtil.getCache(entity).getRepo();
            Set<ConstraintViolation<T>> constraintViolations = validator.validate(entity);
            if (!constraintViolations.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ConstraintViolation<T> constraintViolation : constraintViolations) {
                    sb.append(constraintViolation.toString());
                }
                throw new RuntimeException("Could not validate " + entity + "\n" + sb.toString());
            }
            repo.save(entity);
            T newEntity = (T) repo.findOne(entity.getId());
            CachedCollection c = cacheUtil.getCache(entity);
            if (c != null) {
                c.add(newEntity);
            }
        }
        if (!entities.isEmpty()) {
            for (DaoListener daoListener : daoListeners) {
                daoListener.onChange(entities.get(0));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends LeagueObject> Boolean delete(T entity) {
        cacheUtil.getCache(entity).remove(entity);
        for (DaoListener daoListener : daoListeners) {
            daoListener.onChange(entity);
        }
        return Boolean.TRUE;
    }

    @SuppressWarnings("unchecked")
    public <T extends LeagueObject> T findOne(T entity) {
        if (entity == null)
            return null;
        CachedCollection<List<LeagueObject>> repo = cacheUtil.getCache(entity);
        if (repo == null) {
            return null;
        }
        return (T) repo.get().parallelStream().filter(e -> e.getId().equals(entity.getId())).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    public User findByLogin(String login) {
        if (login == null) {
            return null;
        }
        return (User) cacheUtil.getCache(new User()).get().parallelStream().filter(u -> login.equals(((User) u).getLogin())).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T extends LeagueObject> List<T> findAll(Class<T> clz) {
        try {
            CachedCollection<List<LeagueObject>> cache = cacheUtil.getCache(clz.newInstance());
            if (cache == null) {
                return null;
            }
            return (List<T>) cache.get();
        } catch (InstantiationException e) {
            logger.error(e.getMessage(),e);
            return Collections.emptyList();
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(),e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends LeagueObject> List<T> findCurrent(Class<T> clz) {
        return findAll(clz);
    }

    @SuppressWarnings("unchecked")
    public  <T extends LeagueObject> void deleteAll(Class<T> clz){
        try {
            CachedCollection<List<LeagueObject>> cache = cacheUtil.getCache(clz.newInstance());
            if (cache == null) {
                return ;
            }
            //TODO cascade delete;
            cache.set(new ArrayList<>());
            cache.getRepo().deleteAll();
            //cacheUtil.refreshAllCache();
        } catch (InstantiationException | IllegalAccessException ignore) {

        }
    }


    public <T extends LeagueObject> T purge(T entity) {
        if (entity == null) {
            return null;
        }
        cacheUtil.getCache(entity).remove(entity);
        for (DaoListener daoListener : daoListeners) {
            daoListener.onDelete(entity);
        }
        entity.setDeleted(true);
        return entity;
    }

    public void addListener(DaoListener listener) {
        daoListeners.add(listener);
    }
}
