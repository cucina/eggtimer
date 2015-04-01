package org.cucina.eggtimer;

import java.util.Date;

import org.springframework.test.util.ReflectionTestUtils;

import org.cucina.eggtimer.service.TemporalRepository;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import static org.mockito.Mockito.*;

import org.mockito.MockitoAnnotations;

/**
 * 
 *
 * @author vlevine
  */
public class CheckDateTest {
    private CheckDate checkDate;
    @Mock
    private TemporalRepository temporalRepository;

    /**
     *
     *
     * @throws Exception .
     */
    @Before
    public void setUp()
        throws Exception {
        MockitoAnnotations.initMocks(this);
        checkDate = new CheckDate();
        ReflectionTestUtils.setField(checkDate, "temporalRepository", temporalRepository);
    }

    /**
     *
     */
    @Test
    public void testCheckDate() {
        Date date = new Date();

        when(temporalRepository.beforeCurrentDate(date)).thenReturn(false).thenReturn(true);
        assertFalse(checkDate.checkDate(date));
        assertTrue(checkDate.checkDate(date));
    }
}
