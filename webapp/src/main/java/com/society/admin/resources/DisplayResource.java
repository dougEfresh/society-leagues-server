package com.society.admin.resources;

import com.society.admin.model.ScrambleStatModel;
import com.society.leagues.client.api.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DisplayResource extends BaseController {

    @RequestMapping(value = {"/display/{seasonId}"}, method = RequestMethod.GET)
    public String displaySeasonStandings(@PathVariable String seasonId, Model model, HttpServletResponse response) throws IOException {
        return processDisplay(seasonId, model, null,null);
    }

    @RequestMapping(value = {"/display/{seasonId}/{teamId}"}, method = RequestMethod.GET)
    public String displaySeasonTeamStandings(@PathVariable String seasonId, @PathVariable String teamId, Model model, HttpServletResponse response) throws IOException {
        return processDisplay(seasonId, model, teamId, null);
    }

    @RequestMapping(value = {"/display/{seasonId}/{teamId}/{userId}"}, method = RequestMethod.GET)
    public String displaySeasonTeamStandings(@PathVariable String seasonId, @PathVariable String teamId, @PathVariable String userId, Model model, HttpServletResponse response) throws IOException {
        return processDisplay(seasonId, model, teamId, userId);
    }

    @RequestMapping(value = {"/display/{seasonId}/topgun/{userId}"}, method = RequestMethod.GET)
    public String displayTopGunStandings(@PathVariable String seasonId, @PathVariable String userId, Model model, HttpServletResponse response) throws IOException {
        return processDisplay(seasonId, model, null, userId);
    }

    @ModelAttribute
    public void setDisplay(Model model) {
        model.addAttribute("display",true);
    }

    public String processDisplay(@NotNull  String seasonId, @NotNull Model model, String teamId, String userId) {
        Season s = seasonApi.get(seasonId);

        List<Team> teams = statApi.getSeasonStats(s.getId());
        model.addAttribute("season",s);
        model.addAttribute("displayTeams", teams);

        if (teamId != null) {
            List<Stat> stats = statApi.getTeamMemberStats(teamId).stream().filter(st->st.getUser().isReal()).collect(Collectors.toList());
            if (s.isScramble()) {
                TeamMembers members  = teamApi.members(teamId);
                List<ScrambleStatModel> scrambleStatModelList = new ArrayList<>();
                for (User u : members.getMembers()) {
                    if (!u.isReal())
                        continue;
                    List<Stat> scrambleStats = statApi.getUserStatsActive(u.getId())
                            .stream().filter(st->st.getSeason() != null).filter(st->st.getSeason().isActive()).collect(Collectors.toList()
                            );
                    Stat newStat = new Stat();
                    newStat.setUser(User.defaultUser());
                    scrambleStatModelList.add(new ScrambleStatModel(
                            scrambleStats.stream().filter(st->st.getType() == StatType.MIXED_EIGHT).findFirst().orElse(newStat),
                            scrambleStats.stream().filter(st->st.getType() == StatType.MIXED_NINE).findFirst().orElse(newStat),
                            scrambleStats.stream().filter(st->st.getType() == StatType.USER_SEASON).findFirst().orElse(newStat)
                    ));
                }
                model.addAttribute("displayMemberStats", scrambleStatModelList);
            } else {
                model.addAttribute("displayMemberStats", stats);
            }
            model.addAttribute("team", teamApi.get(teamId));
        }

        if (s.isChallenge()) {
            List<Stat> stats = statApi.getUsersSeasonStats(s.getId());
            stats.sort(new Comparator<Stat>() {
                           @Override
                           public int compare(Stat o1, Stat o2) {
                               if (o2.getPoints().equals(o1.getPoints())) {
                                   return o2.getRackPct().compareTo(o1.getRackPct());
                               }
                               return o2.getPoints().compareTo(o1.getPoints());
                           }
                       });
            int rank = 0;
            for (Stat stat : stats) {
                stat.setRank(++rank);
            }
            model.addAttribute("displayTeams",stats);
        }

        if (userId != null) {
            User u = userApi.get(userId);
            List<PlayerResult> results = playerResultApi.getResults(userId, seasonId);
            results.forEach(r->r.setReferenceUser(u));
            model.addAttribute("results", results);
            model.addAttribute("resultUser", userApi.get(userId));
            model.addAttribute("stats", statApi.getUserStats(userId).stream()
                    .filter(st -> s.equals(st.getSeason()))
                    .filter(st -> st.getType() == StatType.USER_SEASON)
                    .findFirst().orElse(new Stat()));
        }

        return "display/display";
    }
}
