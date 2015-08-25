package com.society.leagues.resource.client;

import com.society.leagues.adapters.UserAdapter;
import com.society.leagues.client.api.domain.*;
import com.society.leagues.dao.*;
import com.society.leagues.email.EmailSender;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api")
@SuppressWarnings("unused")
public class UserResource  {
    @Autowired UserDao dao;
    @Autowired PlayerDao playerDao;
    @Autowired PlayerResultDao playerResultDao;
    @Autowired ChallengeDao challengeDao;
    @Autowired TeamResultDao teamResultDao;
    @Autowired ChallengeResource challengeResource;
    @Autowired EmailSender emailSender;
    static Map<String,LocalDateTime> resetTokens = new HashMap<>();
    @Value("${service-url:http://leaguesdev.societybilliards.com}") String serviceUrl;

    private static Logger logger = LoggerFactory.getLogger(UserResource.class);

    @RequestMapping(value = "/user", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserAdapter get(Principal principal) {
	if (principal == null) {
	    return UserAdapter.DEFAULT_USER;
	}
        return  get(principal.getName());
    }

    @RequestMapping(value = "/user/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserAdapter get(@PathVariable Integer id) {
        return get().stream().filter(u->u.getUserId().equals(id)).findFirst().orElse(null);
    }

    @RequestMapping(value = "/reset/request", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.ALL_VALUE)
    public Map<String,String> reset(@RequestBody User user) {
        String token = UUID.randomUUID().toString().replaceAll("-","");
        logger.info("Reset Token Request: " + token  +" from " + user.getLogin());
        resetTokens.put(token,LocalDateTime.now());
        Map<String,String> map = new HashMap<>();
        map.put("token","a" + token);
        User u = dao.get(user.getLogin());
        if (u == null) {
            return map;
        }

        emailSender.email(u.getEmail(),"Password Reset Request",
                String.format("Hello %s,\n     Please click: %s%s=%s \n to reset your password.",
                        u.getFirstName(),
                        serviceUrl,
                        "/#/reset?token",
                        token)
        );
        return map;
    }

    @RequestMapping(value = "/reset/register", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.ALL_VALUE)
    public Map<String,String> register(@RequestBody User user) {
        String token = UUID.randomUUID().toString().replaceAll("-","");
        logger.info("Reset Token Request: " + token  +" from " + user.getLogin());
        resetTokens.put(token,LocalDateTime.now());
        Map<String,String> map = new HashMap<>();
        map.put("token","a" + token);
        User u = dao.get(user.getLogin());
        if (u == null) {
            return map;
        }

        emailSender.email(u.getEmail(),"Register Request - Society Billiards",
                String.format("Hello %s,\n     Please click: %s%s=%s \n to register your new password.",
                        u.getFirstName(),
                        serviceUrl,
                        "/#/register?token",
                        token)
        );
        return map;
    }

    @RequestMapping(value = "/reset/request/{login:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.ALL_VALUE)
    public Map<String,String> reset(@PathVariable String login) {
        User u = dao.get(login);
        if (u == null) {
            return null;
        }
        return reset(u);
    }

    @RequestMapping(value = "/reset/register/challenge/{token}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserAdapter registerChallenge(@PathVariable String token, @RequestBody User user) {
        logger.info("Got reset password request for " + token + " " + user.getLogin());
        User resetUser = dao.get(user.getLogin());
        if (!resetTokens.containsKey(token)) {
            return null;
        }
        if (user.getPassword() != null && !(user.getPassword().isEmpty())) {
            resetUser.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
            resetTokens.remove(token);
            if (dao.modifyPassword(resetUser)) {
                return challengeResource.signup(resetUser.getId());
            }
        }
        return null;
    }

    @RequestMapping(value = "/reset/password/{token}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserAdapter reset(@PathVariable String token, @RequestBody User user) {
        logger.info("Got reset password request for " + token + " " + user.getLogin());
        User resetUser = dao.get(user.getLogin());
        if (!resetTokens.containsKey(token)) {
            return null;
        }
        resetUser.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        resetTokens.remove(token);
        if (dao.modifyPassword(resetUser)) {
            return get(user.getLogin());
        }
        return null;
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserAdapter> get() {
        List<User> users = dao.get().stream().sorted(new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                return o1.getName().compareTo(o2.getName());
            }
        }).filter(u -> !(
                        u.getName().contains("FORFEIT") || u.getName().contains("BYE") || u.getName().contains("HANDICAP")
                )
        ).collect(Collectors.toList());
        List<UserAdapter> userAdapters = users.stream().map(user -> new UserAdapter(user, playerDao.getByUser(user), challengeResource.getChallenges(user.getId()))).collect(Collectors.toList());
        LocalDateTime elevenWeeks = LocalDateTime.now().minusWeeks(11);
        List<PlayerResult> results = playerResultDao.get().stream().filter(r -> r.getTeamMatch().getDivision().isChallenge()).collect(Collectors.toList());
        for (UserAdapter userAdapter : userAdapters) {
            userAdapter.setChallengeResults(
                    results.stream().filter(
                            r -> r.getPlayerAway().getUserId().equals(userAdapter.getUserId()) ||
                                    r.getPlayerHome().getUserId().equals(userAdapter.getUserId())).
                            collect(Collectors.toList()));
        }
        return userAdapters;
    }

    public UserAdapter get(String login) {
        User u = dao.get(login);
        return  new UserAdapter(u,playerDao.getByUser(u),challengeResource.getChallenges(u.getId()));
    }

    @RequestMapping(value = "/period/{period}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Integer setPeriod(@PathVariable Integer period) {
        UserAdapter.period = period;
        return period;
    }

    @Scheduled(fixedRate = 1000*60*10)
    public synchronized void clearTokens() {

    }


}
