create or replace view team_home_result_vw
as
 select m.home_team_id as team_id ,m.season_id,
     case when home_racks > away_racks then 1 else 0 end as win, 
     case when home_racks < away_racks then 1 else 0 end as lost, 
     home_racks as racks_for,
     away_racks as racks_against 
      from team_result r join team_match m on r.team_match_id=m.team_match_id
      join division d on d.division_id=m.division_id where division_type not like '%CHALLENGE%' 

;

create or replace view team_away_result_vw
as
  select m.away_team_id as team_id,m.season_id,
     case when home_racks > away_racks then 0 else 1 end as win, 
     case when home_racks < away_racks then 0 else 1 end as lost, 
     home_racks as racks_for,
     away_racks as racks_against 
      from team_result r join team_match m on r.team_match_id=m.team_match_id
      join division d on d.division_id=m.division_id where division_type not like '%CHALLENGE%' 
;

create or replace view team_home_stats_vw
AS
select team_id,season_id,sum(win) as wins ,sum(lost) as loses, sum(racks_for) as racks_for ,sum(racks_against) as racks_against  from team_home_result_vw
 group by team_id,season_id;


create or replace view team_away_stats_vw
AS
select team_id,season_id,sum(win) as wins ,sum(lost) as loses, sum(racks_for) as racks_for ,sum(racks_against) as racks_against  from team_away_result_vw
 group by team_id,season_id;



create or replace view team_set_home_vw as 
select team_id,season_id, sum(win) as setWins, sum(lost) as setLoses  
from player_home_result_vw a 
join player p on a.player_home_id=p.player_id 
group by team_id,season_id;

create or replace view team_set_away_vw as 
select team_id,season_id, sum(win) as setWins, sum(lost) as setLoses  
from player_away_result_vw a 
join player p on a.player_away_id=p.player_id 
group by team_id,season_id;

create or replace view team_set_vw as 
select home.team_id,home.season_id,
cast(home.setWins + away.setWins as unsigned integer)  as setWins,
cast(home.setLoses + away.setLoses as unsigned integer) as  setLoses
from team_set_home_vw  home join 
team_set_away_vw away on home.team_id=away.team_id and home.season_id=away.season_id
;

create or replace view team_stats_vw
AS
select 
home.team_id,home.season_id,
cast(sum(home.wins) + sum(away.wins) as unsigned integer)  as wins,
cast(sum(home.loses) + sum(away.loses)  as unsigned integer ) as loses,
cast(sum(home.racks_for) + sum(away.racks_for)  as unsigned integer) as racks_for,
cast(sum(home.racks_against) + sum(away.racks_against)  as unsigned integer ) as racks_against
 from 
team_home_stats_vw home 
join team_away_stats_vw away 
on home.team_id=away.team_id and home.season_id=away.season_id 
group by home.team_id,home.season_id,away.team_id,away.season_id
;

