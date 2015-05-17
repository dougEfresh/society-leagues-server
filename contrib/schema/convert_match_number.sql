replace into leagues.player_result(player_result_id,team_match_id,player_home_id,player_away_id,home_racks,away_racks,match_number) select r.player_result_id,r.team_match_id,r.player_home_id,r.player_away_id,r.home_racks,r.away_racks,i.match_number  from leagues.player_result r join  leagues_old.result_ind i on r.player_result_id=i.result_id  ;
