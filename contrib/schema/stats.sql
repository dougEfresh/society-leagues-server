alter view player_home_result_vw
as
 select r.player_home_id, p.user_id,
     case when home_racks > away_racks then 1 else 0 end as win, 
     case when home_racks < away_racks then 1 else 0 end as lost, 
     home_racks as racks_for,
     away_racks as racks_against 
      from player_result r
      join player p on r.player_home_id=p.player_id 

;

alter view player_away_result_vw
as
 select player_away_id, p.user_id,
     case when away_racks > home_racks then 1 else 0 end as win, 
     case when away_racks < home_racks then 1 else 0 end as lost, 
     away_racks as racks_for,
     home_racks as racks_against 
      from player_result r
      join player p on r.player_away_id=p.player_id 
;

create  view user_stats_home_vw
AS
select user_id,player_home_id,
count(*) as matches,
sum(win) as wins,
sum(lost) as loses,
sum(racks_for) as racks_for,
sum(racks_against) as racks_against
from player_home_result_vw r 
group by player_home_id,user_id
;

create  view user_stats_away_vw
AS
select user_id,player_away_id,
count(*) as matches,
sum(win) as wins,
sum(lost) as loses,
sum(racks_for) as racks_for,
sum(racks_against) as racks_against
from player_away_result_vw r 
group by player_away_id,user_id
;


create view user_stats_vw as
select home.user_id,home.player_home_id as player_id,
       home.matches + away.matches as matches, 
       home.wins + away.wins  as wins , 
       home.loses + away.loses as loses,
       home.racks_for + away.racks_for as racks_for,
       home.racks_against + away.racks_against as racks_against
from user_stats_home_vw home 
join user_stats_away_vw away 
on home.user_id=away.user_id and home.player_home_id=away.player_away_id
;

create view user_stats_all_vw as
  select user_id,sum(matches) as matches ,sum(wins) as wins ,sum(loses) as loses , sum(racks_for) as racks_for , sum(racks_against)  as racks_agains from user_stats_vw group by user_id
 ;

create view user_stats_season_vw as
  select s.user_id,season_id, sum(matches) as matches ,sum(wins) as wins ,sum(loses) as loses , sum(racks_for) as racks_for , sum(racks_against)  as racks_agains from user_stats_vw s  join player p on p.player_id = s.player_id  group by s.user_id,season_id;

create view user_stats_division_vw as
  select s.user_id,division_id, sum(matches) as matches ,sum(wins) as wins ,sum(loses) as loses , sum(racks_for) as racks_for , sum(racks_against)  as racks_agains from user_stats_vw s  join player p on p.player_id = s.player_id  group by s.user_id,division_id;

create view user_stats_challenge_vw as
  select s.user_id,sum(matches) as matches ,sum(wins) as wins ,sum(loses) as loses , sum(racks_for) as racks_for , sum(racks_against)  as racks_agains from user_stats_vw s  join player p on p.player_id = s.player_id
    join division d on p.division_id = d.division_id where d.division_type like '%CHALLENGE%' group by s.user_id;