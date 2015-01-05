package com.society.test;

import com.society.leagues.Main;
import com.society.leagues.client.ApiFactory;
import com.society.leagues.client.api.admin.LeagueAdminApi;
import com.society.leagues.client.api.Role;
import com.society.leagues.client.api.domain.league.League;
import com.society.leagues.client.api.domain.league.LeagueType;
import com.society.leagues.client.exception.Unauthorized;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {Main.class, TestConfig.class, AdminTestConfig.class})
@IntegrationTest(value = {"server.port:0","daemon:true","debug:true"})
public class LeagueAdminTest extends TestBase {
    LeagueAdminApi api;

    @Before
    public void setup() throws Exception {
        super.setup();
        api = ApiFactory.createApi(LeagueAdminApi.class, authenticate(Role.ADMIN), baseURL);
    }

    @Test
    public void testCreate() {
        League league = new League(LeagueType.INDIVIDUAL);
        league.setId(3000);
        Mockito.when(mockLeagueDao.create(Mockito.any(League.class))).thenReturn(league);
        League returned = api.create(league);
        assertNotNull(returned);
        assertEquals(league.getType(),returned.getType());
        assertEquals(league.getId(),returned.getId());

        Mockito.reset(mockLeagueDao);

        league.setType(null);
        Mockito.when(mockLeagueDao.create(league)).thenReturn(league);
        assertNull(api.create(league));
    }

    @Test
    public void testDelete() {
        League league = new League(LeagueType.MIXED);
        league.setId(3001);
        Mockito.when(mockLeagueDao.delete(Mockito.any(League.class))).thenReturn(Boolean.TRUE);
        assertTrue(api.delete(league));

        Mockito.reset(mockLeagueDao);
        Mockito.when(mockLeagueDao.delete(Mockito.any(League.class))).thenReturn(Boolean.FALSE);
        assertFalse(api.delete(league));
    }

    @Test
    public void testModify() {
        League league = new League(LeagueType.TEAM);
        league.setId(3002);
        Mockito.when(mockLeagueDao.modify(Mockito.any(League.class))).thenReturn(league);
        assertNotNull(api.modify(league));

        league.setId(null);
        Mockito.reset(mockLeagueDao);
        Mockito.when(mockLeagueDao.modify(league)).thenReturn(league);
        assertNull(api.modify(league));
    }

    @Test
    public void testNoAccess() {
        api = ApiFactory.createApi(LeagueAdminApi.class,authenticate(Role.Player),baseURL);
        try {
            api.create(new League());
        } catch (Throwable t){
            assertTrue(t.getCause() instanceof Unauthorized);
        }
    }
}