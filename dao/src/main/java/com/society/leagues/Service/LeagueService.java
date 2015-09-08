package com.society.leagues.Service;

import com.society.leagues.CachedCollection;
import com.society.leagues.client.api.domain.*;
import com.society.leagues.mongo.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

@Component
public class LeagueService {

    final static Logger logger = Logger.getLogger(LeagueService.class);

    @Autowired ChallengeRepository challengeRepository;
    @Autowired UserRepository userRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired TeamMatchRepository teamMatchRepository;
    @Autowired TeamRepository teamRepository;
    @Autowired SeasonRepository seasonRepository;
    @Autowired PlayerResultRepository playerResultRepository;

    @Autowired @Qualifier("challengeCachedCollection") CachedCollection<List<Challenge>> challengeCachedCollection;
    @Autowired @Qualifier("userCachedCollection") CachedCollection<List<User>> userCachedCollection;
    @Autowired @Qualifier("slotCachedCollection") CachedCollection<List<Slot>> slotCachedCollection;
    @Autowired @Qualifier("teamMatchCachedCollection") CachedCollection<List<TeamMatch>> teamMatchCachedCollection;
    @Autowired @Qualifier("teamCachedCollection")  CachedCollection<List<Team>> teamCachedCollection;
    @Autowired @Qualifier("seasonCachedCollection") CachedCollection<List<Season>> seasonCachedCollection;
    @Autowired @Qualifier("playerResultCachedCollection") CachedCollection<List<PlayerResult>> playerResultCachedCollection;
    @Autowired List<CachedCollection> cachedCollections;

    ExecutorService executorService = Executors.newFixedThreadPool(10);
    Validator validator;

    @PostConstruct
    @SuppressWarnings("unused")
    public void init() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        logger.info("Refreshing all cache");
        refreshAllCache();
    }

    @PreDestroy
    public void destroy() {
        executorService.shutdownNow();
    }

    public <T extends LeagueObject> T save(T entity) {
        MongoRepository repo = getRepo(entity);
        Set<ConstraintViolation<T>> constraintViolations = validator.validate(entity);

        if (!constraintViolations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<T> constraintViolation : constraintViolations) {
                sb.append(constraintViolation.toString());
            }
            throw  new RuntimeException("Could not validate " + entity + "\n" + sb.toString());
        }
        repo.save(entity);
        CachedCollection cachedCollection = getCache(entity);
        cachedCollection.update();
        refreshAllCache();
        return (T) repo.findOne(entity.getId());
    }

    public <T extends LeagueObject> Boolean delete(T entity) {
        MongoRepository repo = getRepo(entity);
        repo.delete(entity);
        refreshAllCache();
        return Boolean.TRUE;
    }

    public <T extends LeagueObject> T findOne(T entity) {
        CachedCollection<List<T>> repo = getCache(entity);
        if (repo == null) {
            return null;
        }
        return  repo.get().stream().filter(e->e.getId().equals(entity.getId())).findFirst().orElse(null);
    }

    public User findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    public List<Team> findTeamBySeason(Season season) {
        return teamCachedCollection.get().stream().filter(t->t.getSeason().equals(season)).collect(Collectors.toList());
    }

    public List<TeamMatch> findTeamMatchBySeason(Season season) {
        return teamMatchCachedCollection.get().stream().filter(t->t.getSeason().equals(season)).collect(Collectors.toList());
    }

     public List<TeamMatch> findTeamMatchByTeam(Team team) {
         return teamMatchCachedCollection.get().stream().filter(tm->tm.hasTeam(team)).collect(Collectors.toList());
    }

    public <T extends LeagueObject> List<T> findAll(Class<T> clz) {
        try {
            CachedCollection<List<T>> cache = getCache(clz.newInstance());
            return cache.get();
        } catch (InstantiationException e) {
            logger.error(e.getMessage(),e);
            return Collections.emptyList();
        } catch (IllegalAccessException e) {
            logger.error(e.getMessage(),e);
            return Collections.emptyList();
        }
    }

    public List<PlayerResult> findPlayerResultBySeason(Season s) {
        return playerResultCachedCollection.get().stream()
                .filter(pr -> pr.getSeason().equals(s))
                .filter(pr->!pr.getLoser().isFake())
                .filter(pr->!pr.getWinner().isFake())
                .collect(Collectors.toList());
    }

    public List<PlayerResult> findPlayerResultByUser(User u) {
        return playerResultCachedCollection.get().stream().filter(pr->pr.hasUser(u)).collect(Collectors.toList());
    }

    private CachedCollection getCache(LeagueObject entity) {
        Class clz = entity.getClass();

        if (clz.getCanonicalName().endsWith("Challenge")) {
            return challengeCachedCollection;
        }
        if (clz.getCanonicalName().endsWith("User")) {
            return userCachedCollection;
        }
        if (clz.getCanonicalName().endsWith("Slot")) {
            return slotCachedCollection;
        }
        if (clz.getCanonicalName().endsWith("Team")) {
            return teamCachedCollection;
        }
        if (clz.getCanonicalName().endsWith("TeamMatch")) {
            return teamMatchCachedCollection;
        }

        if (clz.getCanonicalName().endsWith("Season")) {
            return seasonCachedCollection;
        }

        if (clz.getCanonicalName().endsWith("PlayerResult")) {
            return playerResultCachedCollection;
        }

        return null;
    }

    private MongoRepository getRepo(LeagueObject entity) {
        Class clz = entity.getClass();
        if (clz.getCanonicalName().endsWith("Challenge")) {
            return challengeRepository;
        }
        if (clz.getCanonicalName().endsWith("User")) {
            return userRepository;
        }
        if (clz.getCanonicalName().endsWith("Slot")) {
            return slotRepository;
        }
        if (clz.getCanonicalName().endsWith("Team")) {
            return teamRepository;
        }
        if (clz.getCanonicalName().endsWith("TeamMatch")) {
            return teamMatchRepository;
        }

        if (clz.getCanonicalName().endsWith("Season")) {
            return seasonRepository;
        }

        if (clz.getCanonicalName().endsWith("PlayerResult")) {
            return playerResultRepository;
        }

        return null;
    }

    public void refreshAllCache() {
        for (final CachedCollection cachedCollection : cachedCollections) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    long now = System.currentTimeMillis();
                    logger.info("Refreshing cache: " + cachedCollection.getClass().getCanonicalName() + " " + cachedCollection.getType() ) ;
                    cachedCollection.update();
                    logger.info("Done refreshing cache (" + (System.currentTimeMillis() - now) + ")" + cachedCollection.getClass().getCanonicalName() + " " + cachedCollection.getType()) ;
                    logger.info("Size " + cachedCollection.getType() + " " + cachedCollection.get().size());

                }
            });
        }
    }

}
