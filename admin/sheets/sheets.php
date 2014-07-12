<?php
session_start();

include '../../include/mysql.php';

ini_set('display_errors', 'On');
error_reporting(E_ALL);

// Add classes for pdf generating
require('../../lib/fpdf/fpdf.php');
require('../../lib/fpdf/fpdi.php');

$result = mysql_query("
	SELECT * FROM match_schedule
	JOIN division ON division.division_id=match_schedule.division_id
	JOIN league ON league.league_id=division.league_id
	WHERE match_schedule.division_id='{$_GET['division_id']}' AND match_schedule.match_number='{$_GET['week']}'"
	);

$row = mysql_fetch_assoc($result);

$league_id = $row['league_id'];

// Set temlate and positions
switch ($row['league_id']) {
	// Scramble Leagues
	case '4':
		$league_type = 'Scramble';
		$handicap_DB = 'handicap_mixed_9';
		$template = 'templates/Scramble_Scoresheet.pdf';
		$team_home_x = 14;
		$team_home_y = 56;
		$team_away_x = 110;
		$team_away_y = 56;
		$team_home_x2 = 14;
		$team_home_y2 = 50;
		$team_away_x2 = 110;
		$team_away_y2 = 50;
		$players_home_x = 14;
		$players_home_y = 71;
		$players_away_x = 110;
		$players_away_y = 71;
		$game_date_x = 14;
		$game_date_y = 37;
		break;
 	// 8 Ball League
	case '2':
		$league_type = '8-Ball';
		$handicap_DB = 'handicap_eight';
		$template = 'templates/8Ball_Scoresheet.pdf';
		$team_home_x = 14;
		$team_home_y = 56.5;
		$team_away_x = 110;
		$team_away_y = 56.5;
		$team_home_x2 = 14;
		$team_home_y2 = 52;
		$team_away_x2 = 110;
		$team_away_y2 = 52;
		$players_home_x = 14;
		$players_home_y = 68;
		$players_away_x = 110;
		$players_away_y = 68;
		$game_date_x = 14;
		$game_date_y = 37;
		break;
	//9 Ball League
	case '1':
		$league_type = '9-Ball';
		$handicap_DB = 'handicap';
		$template = 'templates/9Ball_Scoresheet.pdf';
		$team_home_x = 30;
		$team_home_y = 55;
		$team_away_x = 159;
		$team_away_y = 55;
		$players_home_x = 30;
		$players_home_y = 69;
		$players_away_x = 159;
		$players_away_y = 69;
		$game_date_x = 30;
		$game_date_y = 37;
		break;
	//set default error out
	default:
		die ('This league does not require scoresheet');
		break;
}

$result = mysql_query("
	SELECT *, DATE_FORMAT(match_schedule.match_start_date, '%W - %M %D') week, ht.name home_team, vt.name visit_team, ht.team_id home_id, vt.team_id visit_id, league.league_type
	FROM match_schedule
	LEFT JOIN team ht ON ht.team_id=match_schedule.home_team_id
	LEFT JOIN team vt ON vt.team_id=match_schedule.visit_team_id
	JOIN division ON division.division_id=match_schedule.division_id
	JOIN league ON league.league_id=division.league_id 
	WHERE match_schedule.division_id='{$_GET['division_id']}' AND match_schedule.match_number='{$_GET['week']}'"
	);

// initiate FPDI
$pdf = new FPDI();

while ($row = mysql_fetch_assoc($result)) {
	// get sql of home team players
	$hp_result = mysql_query("
		SELECT *, CONCAT('(',handicap_display.hcd_name,') ', player.first_name,' ',player.last_name) player_name 
		FROM team_player
		JOIN player ON player.player_id=team_player.tp_player
		JOIN team ON team.team_id=team_player.tp_team
		LEFT JOIN handicap_display ON (hcd_league={$league_id} AND hcd_handicap={$handicap_DB})
		WHERE team_player.tp_team='{$row['home_id']}' AND 
		team_player.tp_division='{$_GET['division_id']}' AND 
		NOT player.first_name LIKE '%FORFEIT%' AND
		NOT player.last_name LIKE '%FORFEIT%' AND
		NOT player.first_name LIKE '%HANDICAP%' AND
		NOT player.last_name LIKE '%HANDICAP%'"
		); 
		  
	// get sql of visiting team players
	$vp_result = mysql_query("
		SELECT *, CONCAT('(',handicap_display.hcd_name,') ', player.first_name,' ',player.last_name) player_name 
		FROM team_player 
		JOIN player ON player.player_id=team_player.tp_player
		JOIN team ON team.team_id=team_player.tp_team
		LEFT JOIN handicap_display ON (hcd_league={$league_id} AND hcd_handicap={$handicap_DB})
		WHERE team_player.tp_team='{$row['visit_id']}' AND 
		team_player.tp_division='{$_GET['division_id']}' AND 
		NOT player.first_name LIKE '%FORFEIT%' AND
		NOT player.last_name LIKE '%FORFEIT%' AND
		NOT player.first_name LIKE '%HANDICAP%' AND
		NOT player.last_name LIKE '%HANDICAP%'"
		); 

	// set the source file
	$pages= $pdf->setSourceFile($template);

	// Scramble League Scoresheet
	if ($league_type == "Scramble") {
	
		// add a page
		$pdf->AddPage();

		// import page 1
		$tplIdx = $pdf->importPage(1);

		// use the imported page
		$pdf->useTemplate($tplIdx, 0, 0, 216);

		// now write some text above the imported page
		$pdf->SetFont('Helvetica', 'B', 10);
		$pdf->SetTextColor(0, 0, 0);

		// write game date
		$pdf->SetFont('Helvetica', 'B', 10);
		$pdf->SetXY($game_date_x, $game_date_y);
		$pdf->Write(0,  $row['week']);

		// write home team
		$pdf->SetFont('Helvetica', 'B', 12);
		$pdf->SetXY($team_home_x, $team_home_y);
		$pdf->Write(0, $row['home_team']);

		// write 8 or 9 ball
		$pdf->SetFont('Helvetica', 'B', 23);
		$pdf->SetXY(178, 9.5);
		$pdf->Cell(50, 30, 'BALL');
		
		// write home team list
		$pl_home_y = $players_home_y;
		while ($hp_row = mysql_fetch_assoc($hp_result)) {
			$pdf->SetFont('Helvetica', '', 10);
			$pdf->SetXY($players_home_x, $pl_home_y);
			$pdf->Write(0, $hp_row['player_name']);
			$pl_home_y = $pl_home_y + 5; 
		}

		// write away team
		$pdf->SetFont('Helvetica', 'B', 12);
		$pdf->SetXY($team_away_x, $team_away_y);
		$pdf->Write(0, $row['visit_team']);

		// write away team list
		$vl_home_y = $players_away_y;
		while ($vp_row = mysql_fetch_assoc($vp_result)) {
			$pdf->SetFont('Helvetica', '', 10);
			$pdf->SetXY($players_away_x, $vl_home_y);
			$pdf->Write(0, $vp_row['player_name']);
			$vl_home_y = $vl_home_y + 5; 
		}

		// add a page
		$pdf->AddPage();

		// import page 2
		$tplIdx2 = $pdf->importPage(2);
		$pdf->useTemplate($tplIdx2, 0, 0, 216);

		// write 8 or 9 ball
		$pdf->SetFont('Helvetica', 'B', 23);
		$pdf->SetXY(178, 9.5);
		$pdf->Cell(50, 30, 'BALL');

		// write home team
		$pdf->SetFont('Helvetica', 'B', 12);
		$pdf->SetXY($team_home_x2, $team_home_y2);
		$pdf->Write(0, $row['home_team']);

		// write away team
		$pdf->SetFont('Helvetica', 'B', 12);
		$pdf->SetXY($team_away_x2, $team_away_y2);
		$pdf->Write(0, $row['visit_team']);
	}

	// 8-Ball Scoresheet
	if ($league_type == "8-Ball") {

		// add a page
		$pdf->AddPage();

		// import page 1
		$tplIdx = $pdf->importPage(1);

		// use the imported page
		$pdf->useTemplate($tplIdx, 0, 0, 216);

		// write game date
		$pdf->SetFont('Helvetica', 'B', 10);
		$pdf->SetXY($game_date_x, $game_date_y);
		$pdf->Write(0,  $row['week']);

		// write home team
		$pdf->SetFont('Helvetica', 'B', 12);
		$pdf->SetXY($team_home_x, $team_home_y);
		$pdf->Write(0, $row['home_team']);
	
		// write away team
		$pdf->SetFont('Helvetica', 'B', 12);
		$pdf->SetXY($team_away_x, $team_away_y);
		$pdf->Write(0, $row['visit_team']);
	
		// add a page
		$pdf->AddPage();

		// import page 2
		$tplIdx2 = $pdf->importPage(2);
		$pdf->useTemplate($tplIdx2, 0, 0, 216);

		// write home team
		$pdf->SetFont('Helvetica', 'B', 12);
		$pdf->SetXY($team_home_x2, $team_home_y2);
		$pdf->Write(0, $row['home_team']);
	
		// write away team
		$pdf->SetFont('Helvetica', 'B', 12);
		$pdf->SetXY($team_away_x2, $team_away_y2);
		$pdf->Write(0, $row['visit_team']);

		// write home team list
		$pl_home_y = $players_home_y;
		while ($hp_row = mysql_fetch_assoc($hp_result)) {
			$pdf->SetFont('Helvetica', '', 10);
			$pdf->SetXY($players_home_x, $pl_home_y);
			$pdf->Write(0, $hp_row['player_name']);
			$pl_home_y = $pl_home_y + 5; 
		}

		// write away team list
		$vl_home_y = $players_away_y;
		while ($vp_row = mysql_fetch_assoc($vp_result)) {
			$pdf->SetFont('Helvetica', '', 10);
			$pdf->SetXY($players_away_x, $vl_home_y);
			$pdf->Write(0, $vp_row['player_name']);
			$vl_home_y = $vl_home_y + 5; 
		}
	}

	// Scramble League Scoresheet
	if ($league_type == "9-Ball") {

		// import page 1
		$tplIdx = $pdf->importPage(1);
	
		// add a page
		$pdf->AddPage('L');
		
		// use the imported page 
		$pdf->useTemplate($tplIdx);
	
		// write game date
		$pdf->SetFont('Helvetica', 'B', 10);
		$pdf->SetXY($game_date_x, $game_date_y);
		$pdf->Write(0,  $row['week']);
	
		// write home team
		$pdf->SetFont('Helvetica', 'B', 12);
		$pdf->SetXY($team_home_x, $team_home_y);
		$pdf->Write(0, $row['home_team']);
	
		// write home team list
		$pl_home_y = $players_home_y;
		while ($hp_row = mysql_fetch_assoc($hp_result)) {
				$pdf->SetFont('Helvetica', '', 10);
				$pdf->SetXY($players_home_x, $pl_home_y);
				$pdf->Write(0, $hp_row['player_name']);
				$pl_home_y = $pl_home_y + 6.6; 
		}
	
		// write away team
		$pdf->SetFont('Helvetica', 'B', 12);
		$pdf->SetXY($team_away_x, $team_away_y);
		$pdf->Write(0, $row['visit_team']);
	
		// write away team list
		$vl_home_y = $players_away_y;
		while ($vp_row = mysql_fetch_assoc($vp_result)) {
				$pdf->SetFont('Helvetica', '', 10);
				$pdf->SetXY($players_away_x, $vl_home_y);
				$pdf->Write(0, $vp_row['player_name']);
				$vl_home_y = $vl_home_y + 6.7; 
		}
		// import page 2
		$tplIdx = $pdf->importPage(2);
	
		// add a page
		$pdf->AddPage('L');
	
		// use the imported page
		$pdf->useTemplate($tplIdx);
	}
}


//ob_end_clean();
$pdf->Output();

?>