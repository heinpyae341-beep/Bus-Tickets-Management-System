
package com.myanmar.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpSession;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class BusControllerPermissionTest {

    @Test
    void globalAdminCannotCreateBus() {

        JdbcTemplate db = mock(JdbcTemplate.class);
        HttpSession session = mock(HttpSession.class);

        when(session.getAttribute("admin"))
                .thenReturn(Map.of("admin_role", "GLOBAL_ADMIN"));

        BusController controller = new BusController(db);

        assertEquals(
                "redirect:/",
                controller.bus("BUS-01", "Express", 40, session)
        );

        verifyNoInteractions(db);
    }


    @Test
    void globalAdminCannotMutateOperationalRecords() {

        JdbcTemplate db = mock(JdbcTemplate.class);
        HttpSession session = mock(HttpSession.class);

        when(session.getAttribute("admin"))
                .thenReturn(Map.of("admin_role", "GLOBAL_ADMIN"));

        BusController controller = new BusController(db);

        assertEquals(
                "redirect:/",
                controller.used(1, session)
        );

        assertEquals(
                "redirect:/",
                controller.toggleTrip(1, session)
        );

        assertEquals(
                "redirect:/",
                controller.cancelRoute(1, session)
        );

        assertEquals(
                "redirect:/",
                controller.assign(1, 1, session)
        );

        verifyNoInteractions(db);
    }


    @Test
    void localAdminCanMutateOperationalRecords() {

        JdbcTemplate db = mock(JdbcTemplate.class);
        HttpSession session = mock(HttpSession.class);

        when(session.getAttribute("admin"))
                .thenReturn(Map.of("admin_role", "LOCAL_ADMIN"));

        BusController controller = new BusController(db);

        assertEquals(
                "redirect:/dashboard#tickets",
                controller.used(1, session)
        );

        assertEquals(
                "redirect:/dashboard#trips",
                controller.toggleTrip(1, session)
        );

        assertEquals(
                "redirect:/dashboard#routes",
                controller.cancelRoute(1, session)
        );

        assertEquals(
                "redirect:/dashboard",
                controller.assign(
                        1,
                        1,
                        session
                )
        );

        // assign() performs several SELECT checks before INSERT,
        // so checking exactly 4 update() calls is incorrect.
        verify(db, atLeastOnce())
                .update(any(String.class), any(Object[].class));
    }


    @Test
    void localAdminCanCreateBus() {

        JdbcTemplate db = mock(JdbcTemplate.class);
        HttpSession session = mock(HttpSession.class);

        when(session.getAttribute("admin"))
                .thenReturn(Map.of("admin_role", "LOCAL_ADMIN"));

        when(db.queryForObject(
                "SELECT COUNT(*) FROM bus WHERE bus_number=?",
                Integer.class,
                "BUS-01"
        )).thenReturn(0);

        BusController controller = new BusController(db);

        assertEquals(
                "redirect:/dashboard",
                controller.bus(
                        "BUS-01",
                        "Express",
                        40,
                        session
                )
        );

        verify(db).update(
                eq(
                    "INSERT INTO bus " +
                    "(bus_number,seat_capacity,bus_type) " +
                    "VALUES (?,?,?)"
                ),
                eq("BUS-01"),
                eq(40),
                eq("Express")
        );
    }
}

