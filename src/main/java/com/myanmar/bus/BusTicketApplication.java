package com.myanmar.bus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
public class BusTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusTicketApplication.class, args);
    }
}


// =========================================================
// CONTROLLER
// =========================================================

@Controller
class BusController {

    private final JdbcTemplate db;

    BusController(JdbcTemplate db) {
        this.db = db;
    }


    // =====================================================
    // STARTUP - CLEAN DUPLICATE TRIPS
    // =====================================================

    @PostConstruct
    @Transactional
    void cleanupDuplicateTrips() {

        try {

            var groups = db.queryForList(
                "SELECT " +
                "LOWER(TRIM(departure_location)) AS dep, " +
                "LOWER(TRIM(destination_location)) AS dest, " +
                "MIN(trip_id) AS keep_id " +
                "FROM trip " +
                "GROUP BY " +
                "LOWER(TRIM(departure_location)), " +
                "LOWER(TRIM(destination_location)) " +
                "HAVING COUNT(*) > 1"
            );


            for (Map<String, Object> group : groups) {

                int keepId =
                    ((Number) group.get("keep_id")).intValue();

                String dep =
                    String.valueOf(group.get("dep"));

                String dest =
                    String.valueOf(group.get("dest"));


                var duplicateIds = db.queryForList(
                    "SELECT trip_id " +
                    "FROM trip " +
                    "WHERE LOWER(TRIM(departure_location)) = ? " +
                    "AND LOWER(TRIM(destination_location)) = ? " +
                    "AND trip_id <> ?",
                    dep,
                    dest,
                    keepId
                );


                for (Map<String, Object> row : duplicateIds) {

                    int duplicateId =
                        ((Number) row.get("trip_id")).intValue();


                    // Route တွေကို မပျောက်အောင်
                    // duplicate Trip ID ကနေ
                    // original Trip ID ဆီ ပြန်ချိတ်မယ်

                    db.update(
                        "UPDATE route SET trip_id=? WHERE trip_id=?",
                        keepId,
                        duplicateId
                    );


                    // duplicate Trip ဖျက်မယ်

                    db.update(
                        "DELETE FROM trip WHERE trip_id=?",
                        duplicateId
                    );
                }
            }

        } catch (Exception e) {

            /*
             * Database table မရှိသေးတဲ့ startup timing
             * ပြဿနာမျိုးရှိရင် application မပိတ်အောင်
             * ဒီ cleanup ကို skip လုပ်ထားမယ်။
             */
        }
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @GetMapping("/")
    String loginPage() {
        return "login";
    }


    @PostMapping("/login")
    String login(
        @RequestParam String username,
        @RequestParam String password,
        HttpSession s,
        Model m
    ) {

        var rows = db.queryForList(
            "SELECT * FROM admin_users " +
            "WHERE username=? AND password=? AND active=TRUE",
            username,
            password
        );


        if (rows.isEmpty()) {

            m.addAttribute(
                "error",
                "Username သို့မဟုတ် Password မှားနေပါတယ်"
            );

            return "login";
        }


        Map<String, Object> admin = rows.get(0);

        s.setAttribute("admin", admin);


        String role =
            String.valueOf(admin.get("admin_role"));


        if ("GLOBAL_ADMIN".equals(role)) {
            return "redirect:/global";
        }


        return "redirect:/dashboard";
    }


    // =====================================================
    // LOGOUT
    // =====================================================

    @GetMapping("/logout")
    String logout(HttpSession s) {

        s.invalidate();

        return "redirect:/";
    }


    // =====================================================
    // GLOBAL ADMIN
    // =====================================================

    @GetMapping("/global")
    String global(
        HttpSession s,
        Model m
    ) {

        if (!global(s)) {
            return deny(s);
        }


        m.addAttribute(
            "admins",
            db.queryForList(
                "SELECT * FROM admin_users ORDER BY id DESC"
            )
        );


        m.addAttribute(
            "buses",
            db.queryForList(
                "SELECT * FROM bus ORDER BY bus_id DESC"
            )
        );


        m.addAttribute(
            "trips",
            db.queryForList(
                "SELECT * FROM trip ORDER BY trip_id DESC"
            )
        );


        m.addAttribute(
            "routes",
            db.queryForList(
                "SELECT r.*, " +
                "t.departure_location, " +
                "t.destination_location, " +
                "t.distance_km " +
                "FROM route r " +
                "JOIN trip t ON t.trip_id=r.trip_id " +
                "ORDER BY r.route_id DESC"
            )
        );


        m.addAttribute(
            "tickets",
            db.queryForList(
                "SELECT k.*, " +
                "t.departure_location, " +
                "t.destination_location " +
                "FROM ticket k " +
                "JOIN route r ON r.route_id=k.route_id " +
                "JOIN trip t ON t.trip_id=r.trip_id " +
                "ORDER BY k.ticket_id DESC"
            )
        );


        return "global";
    }


    // =====================================================
    // GLOBAL ADMIN - CREATE LOCAL ADMIN
    // =====================================================

    @PostMapping("/admin")
    String admin(
        @RequestParam String name,
        @RequestParam String email,
        @RequestParam String username,
        @RequestParam String password,
        HttpSession s
    ) {

        if (!global(s)) {
            return deny(s);
        }


        db.update(
            "INSERT INTO admin_users " +
            "(name,email,username,password,admin_role) " +
            "VALUES (?,?,?,?, 'LOCAL_ADMIN')",
            name,
            email,
            username,
            password
        );


        return "redirect:/global";
    }


    // =====================================================
    // LOCAL ADMIN DASHBOARD
    // =====================================================

    @GetMapping("/dashboard")
    String dashboard(
        HttpSession s,
        Model m
    ) {

        if (!local(s)) {
            return deny(s);
        }


        return home(s, m);
    }


    // =====================================================
    // HOME DATA
    // =====================================================

    String home(
        HttpSession s,
        Model m
    ) {

        var admin =
            (Map<String, Object>) s.getAttribute("admin");


        m.addAttribute(
            "admin",
            admin
        );


        // -------------------------------------------------
        // BUSES
        // -------------------------------------------------

        m.addAttribute(
            "buses",
            db.queryForList(
                "SELECT * FROM bus ORDER BY bus_id DESC"
            )
        );


        // -------------------------------------------------
        // TRIPS
        // -------------------------------------------------

        m.addAttribute(
            "trips",
            db.queryForList(
                "SELECT * FROM trip ORDER BY trip_id DESC"
            )
        );


        // -------------------------------------------------
        // ROUTES
        // -------------------------------------------------

        m.addAttribute(
            "routes",
            db.queryForList(
                "SELECT " +
                "r.*, " +
                "t.departure_location, " +
                "t.destination_location, " +
                "t.distance_km " +
                "FROM route r " +
                "JOIN trip t ON t.trip_id=r.trip_id " +
                "ORDER BY r.route_id DESC"
            )
        );


        // -------------------------------------------------
        // TICKETS
        // -------------------------------------------------

        m.addAttribute(
            "tickets",
            db.queryForList(
                "SELECT " +
                "k.*, " +
                "t.departure_location, " +
                "t.destination_location " +
                "FROM ticket k " +
                "JOIN route r ON r.route_id=k.route_id " +
                "JOIN trip t ON t.trip_id=r.trip_id " +
                "ORDER BY k.ticket_id DESC"
            )
        );


        // -------------------------------------------------
        // STATISTICS
        // -------------------------------------------------

        Integer busCount =
            db.queryForObject(
                "SELECT COUNT(*) FROM bus WHERE active=TRUE",
                Integer.class
            );


        Integer routeCount =
            db.queryForObject(
                "SELECT COUNT(*) FROM route " +
                "WHERE status='SCHEDULED'",
                Integer.class
            );


        Integer ticketCount =
            db.queryForObject(
                "SELECT COUNT(*) FROM ticket " +
                "WHERE ticket_status='BOOKED'",
                Integer.class
            );


        BigDecimal revenue =
            db.queryForObject(
                "SELECT COALESCE(SUM(total_amount_kyat),0) " +
                "FROM ticket " +
                "WHERE ticket_status <> 'CANCELLED'",
                BigDecimal.class
            );


        m.addAttribute(
            "busCount",
            busCount == null ? 0 : busCount
        );


        m.addAttribute(
            "routeCount",
            routeCount == null ? 0 : routeCount
        );


        m.addAttribute(
            "ticketCount",
            ticketCount == null ? 0 : ticketCount
        );


        m.addAttribute(
            "revenue",
            revenue == null
                ? BigDecimal.ZERO
                : revenue
        );


        return "dashboard";
    }


    // =====================================================
    // CREATE BUS
    // =====================================================

    @PostMapping("/bus")
    String bus(
        @RequestParam String number,
        @RequestParam String type,
        @RequestParam int seats,
        HttpSession s,
        Model m
    ) {

        if (!local(s)) {
            return deny(s);
        }


        String busNumber =
            number == null ? "" : number.trim();


        String busType =
            type == null ? "" : type.trim();


        if (busNumber.isEmpty()) {

            m.addAttribute(
                "error",
                "Bus Number ဖြည့်ပေးပါ"
            );

            return home(s, m);
        }


        if (busType.isEmpty()) {

            m.addAttribute(
                "error",
                "Bus Type ဖြည့်ပေးပါ"
            );

            return home(s, m);
        }


        if (seats <= 0) {

            m.addAttribute(
                "error",
                "ထိုင်ခုံအရေအတွက်သည် 0 ထက်ကြီးရမည်"
            );

            return home(s, m);
        }


        Integer exists =
            db.queryForObject(
                "SELECT COUNT(*) FROM bus WHERE bus_number=?",
                Integer.class,
                busNumber
            );


        if (exists != null && exists > 0) {

            m.addAttribute(
                "error",
                "ဒီ Bus Number ရှိပြီးသားဖြစ်ပါတယ်"
            );

            return home(s, m);
        }


        db.update(
            "INSERT INTO bus " +
            "(bus_number,seat_capacity,bus_type) " +
            "VALUES (?,?,?)",
            busNumber,
            seats,
            busType
        );


        m.addAttribute(
            "success",
            "Bus အသစ် ထည့်ပြီးပါပြီ"
        );


        return home(s, m);
    }


    // =====================================================
    // OLD BUS METHOD
    // =====================================================

    String bus(
        String number,
        String type,
        int seats,
        HttpSession s
    ) {

        return bus(
            number,
            type,
            seats,
            s,
            new org.springframework.ui.ExtendedModelMap()
        );
    }


    // =====================================================
    // TOGGLE BUS
    // =====================================================

    @PostMapping("/bus/{id}/toggle")
    String toggleBus(
        @PathVariable int id,
        HttpSession s
    ) {

        if (!local(s)) {
            return deny(s);
        }


        db.update(
            "UPDATE bus " +
            "SET active = NOT active " +
            "WHERE bus_id=?",
            id
        );


        return "redirect:/dashboard#buses";
    }


    // =====================================================
    // CREATE TRIP
    // =====================================================

    @PostMapping("/trip")
    String trip(
        @RequestParam String from,
        @RequestParam String to,
        @RequestParam BigDecimal distance,
        HttpSession s,
        Model m
    ) {

        if (!local(s)) {
            return deny(s);
        }


        // -------------------------------------------------
        // Clean input
        // -------------------------------------------------

        String departure =
            from == null
                ? ""
                : from.trim();


        String destination =
            to == null
                ? ""
                : to.trim();


        // -------------------------------------------------
        // Validate departure
        // -------------------------------------------------

        if (departure.isEmpty()) {

            m.addAttribute(
                "error",
                "ထွက်ခွာမည့်နေရာ ဖြည့်ပေးပါ"
            );

            return home(s, m);
        }


        // -------------------------------------------------
        // Validate destination
        // -------------------------------------------------

        if (destination.isEmpty()) {

            m.addAttribute(
                "error",
                "ရောက်မည့်နေရာ ဖြည့်ပေးပါ"
            );

            return home(s, m);
        }


        // -------------------------------------------------
        // Validate distance
        // -------------------------------------------------

        if (
            distance == null ||
            distance.compareTo(BigDecimal.ZERO) <= 0
        ) {

            m.addAttribute(
                "error",
                "အကွာအဝေးသည် 0 ထက်ကြီးရမည်"
            );

            return home(s, m);
        }


        // -------------------------------------------------
        // Same location မဖြစ်ရ
        //
        // ဥပမာ:
        // ရန်ကုန် → ရန်ကုန်
        // -------------------------------------------------

        if (
            departure.equalsIgnoreCase(destination)
        ) {

            m.addAttribute(
                "error",
                "ထွက်ခွာရာနှင့် ရောက်ရာနေရာ တူလို့မရပါ"
            );

            return home(s, m);
        }


        // -------------------------------------------------
        // DUPLICATE TRIP CHECK
        //
        // ဥပမာ:
        //
        // ရန်ကုန် → နေပြည်တော်
        // ရန်ကုန် → နေပြည်တော်
        //
        // ဒုတိယတစ်ခု ထပ်ထည့်လို့မရ
        // -------------------------------------------------

        Integer duplicate =
            db.queryForObject(
                "SELECT COUNT(*) " +
                "FROM trip " +
                "WHERE LOWER(TRIM(departure_location)) " +
                "= LOWER(TRIM(?)) " +
                "AND LOWER(TRIM(destination_location)) " +
                "= LOWER(TRIM(?))",
                Integer.class,
                departure,
                destination
            );


        if (
            duplicate != null &&
            duplicate > 0
        ) {

            m.addAttribute(
                "error",
                "ဒီခရီးစဉ် (" +
                departure +
                " → " +
                destination +
                ") ရှိပြီးသားဖြစ်ပါတယ်"
            );

            return home(s, m);
        }


        // -------------------------------------------------
        // INSERT NEW TRIP
        // -------------------------------------------------

        db.update(
            "INSERT INTO trip " +
            "(departure_location," +
            "destination_location," +
            "distance_km) " +
            "VALUES (?,?,?)",
            departure,
            destination,
            distance
        );


        m.addAttribute(
            "success",
            "Trip အသစ် ထည့်ပြီးပါပြီ"
        );


        return home(s, m);
    }


    // =====================================================
    // TOGGLE TRIP
    // =====================================================

    @PostMapping("/trip/{id}/toggle")
    String toggleTrip(
        @PathVariable int id,
        HttpSession s
    ) {

        if (!local(s)) {
            return deny(s);
        }


        db.update(
            "UPDATE trip " +
            "SET active = NOT active " +
            "WHERE trip_id=?",
            id
        );


        return "redirect:/dashboard#trips";
    }


    // =====================================================
    // CREATE ROUTE
    // =====================================================

    @PostMapping("/route")
    String route(
        @RequestParam int tripId,
        @RequestParam String date,
        @RequestParam String depart,
        @RequestParam String arrive,
        @RequestParam int seats,
        @RequestParam BigDecimal price,
        HttpSession s,
        Model m
    ) {

        if (!local(s)) {
            return deny(s);
        }


        if (seats <= 0) {

            m.addAttribute(
                "error",
                "လွတ်ထိုင်ခုံအရေအတွက်သည် 0 ထက်ကြီးရမည်"
            );

            return home(s, m);
        }


        if (
            price == null ||
            price.compareTo(BigDecimal.ZERO) < 0
        ) {

            m.addAttribute(
                "error",
                "Ticket Price မှန်ကန်စွာ ဖြည့်ပေးပါ"
            );

            return home(s, m);
        }


        Integer tripExists =
            db.queryForObject(
                "SELECT COUNT(*) FROM trip " +
                "WHERE trip_id=? AND active=TRUE",
                Integer.class,
                tripId
            );


        if (
            tripExists == null ||
            tripExists == 0
        ) {

            m.addAttribute(
                "error",
                "ရွေးထားသော Trip မရှိတော့ပါ"
            );

            return home(s, m);
        }


        LocalDate routeDate;


        try {

            routeDate =
                LocalDate.parse(date);

        } catch (Exception e) {

            m.addAttribute(
                "error",
                "Route Date မှားနေပါတယ်"
            );

            return home(s, m);
        }


        db.update(
            "INSERT INTO route " +
            "(trip_id,route_date,departure_time," +
            "arrival_time,available_seats," +
            "ticket_price_kyat,status) " +
            "VALUES (?,?,?,?,?,?, 'SCHEDULED')",
            tripId,
            routeDate,
            depart,
            arrive,
            seats,
            price
        );


        m.addAttribute(
            "success",
            "Route အသစ် ထည့်ပြီးပါပြီ"
        );


        return home(s, m);
    }


    // =====================================================
    // ASSIGN BUS TO ROUTE
    // =====================================================

    @PostMapping("/assign")
    String assign(
        @RequestParam int busId,
        @RequestParam int routeId,
        HttpSession s,
        Model m
    ) {

        if (!local(s)) {
            return deny(s);
        }


        // -------------------------------------------------
        // Check bus
        // -------------------------------------------------

        var busRows =
            db.queryForList(
                "SELECT active,seat_capacity " +
                "FROM bus WHERE bus_id=?",
                busId
            );


        if (busRows.isEmpty()) {

            m.addAttribute(
                "error",
                "Bus မတွေ့ပါ"
            );

            return home(s, m);
        }


        Object activeValue =
            busRows.get(0).get("active");


        boolean busActive;


        if (activeValue instanceof Boolean) {

            busActive =
                (Boolean) activeValue;

        } else if (activeValue instanceof Number) {

            busActive =
                ((Number) activeValue).intValue() != 0;

        } else {

            busActive =
                "true".equalsIgnoreCase(
                    String.valueOf(activeValue)
                );
        }


        if (!busActive) {

            m.addAttribute(
                "error",
                "ဒီ Bus ကို ပိတ်ထားပါတယ်"
            );

            return home(s, m);
        }


        int busCapacity =
            ((Number)
                busRows.get(0).get("seat_capacity")
            ).intValue();


        // -------------------------------------------------
        // Check route
        // -------------------------------------------------

        var routeRows =
            db.queryForList(
                "SELECT available_seats,status " +
                "FROM route WHERE route_id=?",
                routeId
            );


        if (routeRows.isEmpty()) {

            m.addAttribute(
                "error",
                "Route မတွေ့ပါ"
            );

            return home(s, m);
        }


        String status =
            String.valueOf(
                routeRows.get(0).get("status")
            );


        if ("CANCELLED".equals(status)) {

            m.addAttribute(
                "error",
                "Cancelled Route ကို Bus ချိတ်လို့မရပါ"
            );

            return home(s, m);
        }


        int availableSeats =
            ((Number)
                routeRows.get(0).get("available_seats")
            ).intValue();


        if (availableSeats > busCapacity) {

            m.addAttribute(
                "error",
                "Route ထိုင်ခုံအရေအတွက်သည် Bus Capacity ထက်များနေပါတယ်"
            );

            return home(s, m);
        }


        // -------------------------------------------------
        // Check duplicate assignment
        // -------------------------------------------------

        Integer assigned =
            db.queryForObject(
                "SELECT COUNT(*) " +
                "FROM bus_trip " +
                "WHERE bus_id=? AND route_id=?",
                Integer.class,
                busId,
                routeId
            );


        if (
            assigned != null &&
            assigned > 0
        ) {

            m.addAttribute(
                "error",
                "ဒီ Bus နဲ့ Route ကို ချိတ်ပြီးသားဖြစ်ပါတယ်"
            );

            return home(s, m);
        }


        db.update(
            "INSERT INTO bus_trip " +
            "(bus_id,route_id) " +
            "VALUES (?,?)",
            busId,
            routeId
        );


        m.addAttribute(
            "success",
            "Bus နှင့် Route ချိတ်ပြီးပါပြီ"
        );


        return home(s, m);
    }


    // =====================================================
    // OLD ASSIGN METHOD
    // =====================================================

    String assign(
        int busId,
        int routeId,
        HttpSession s
    ) {

        return assign(
            busId,
            routeId,
            s,
            new org.springframework.ui.ExtendedModelMap()
        );
    }


    // =====================================================
    // CREATE TICKET
    // =====================================================

    @PostMapping("/ticket")
    String ticket(
        @RequestParam int routeId,
        @RequestParam String passenger,
        @RequestParam String phone,
        @RequestParam int seat,
        HttpSession s,
        Model m
    ) {

        if (!local(s)) {
            return deny(s);
        }


        String passengerName =
            passenger == null
                ? ""
                : passenger.trim();


        String passengerPhone =
            phone == null
                ? ""
                : phone.trim();


        // -------------------------------------------------
        // Passenger validation
        // -------------------------------------------------

        if (passengerName.isEmpty()) {

            m.addAttribute(
                "error",
                "ခရီးသည်အမည် ဖြည့်ပေးပါ"
            );

            return home(s, m);
        }


        if (passengerPhone.isEmpty()) {

            m.addAttribute(
                "error",
                "ဖုန်းနံပါတ် ဖြည့်ပေးပါ"
            );

            return home(s, m);
        }


        if (seat <= 0) {

            m.addAttribute(
                "error",
                "ထိုင်ခုံနံပါတ် မှန်ကန်စွာ ဖြည့်ပေးပါ"
            );

            return home(s, m);
        }


        // -------------------------------------------------
        // Phone validation
        // -------------------------------------------------

        if (
            !passengerPhone.matches(
                "(09|\\+959)[0-9]{7,12}"
            )
        ) {

            m.addAttribute(
                "error",
                "ဖုန်းနံပါတ်ပုံစံ မှားနေပါတယ်"
            );

            return home(s, m);
        }


        // -------------------------------------------------
        // Route data
        // -------------------------------------------------

        var routeRows =
            db.queryForList(
                "SELECT " +
                "r.available_seats, " +
                "r.ticket_price_kyat, " +
                "r.status, " +
                "t.departure_location, " +
                "t.destination_location " +
                "FROM route r " +
                "JOIN trip t ON t.trip_id=r.trip_id " +
                "WHERE r.route_id=?",
                routeId
            );


        if (routeRows.isEmpty()) {

            m.addAttribute(
                "error",
                "Route မတွေ့ပါ"
            );

            return home(s, m);
        }


        Map<String, Object> route =
            routeRows.get(0);


        String status =
            String.valueOf(
                route.get("status")
            );


        if (!"SCHEDULED".equals(status)) {

            m.addAttribute(
                "error",
                "ဒီ Route မှာ Ticket ထုတ်လို့မရပါ"
            );

            return home(s, m);
        }


        int availableSeats =
            ((Number)
                route.get("available_seats")
            ).intValue();


        if (availableSeats <= 0) {

            m.addAttribute(
                "error",
                "ထိုင်ခုံမလွတ်တော့ပါ"
            );

            return home(s, m);
        }


        // -------------------------------------------------
        // Bus capacity
        // -------------------------------------------------

        var busRows =
            db.queryForList(
                "SELECT b.seat_capacity " +
                "FROM bus_trip bt " +
                "JOIN bus b ON b.bus_id=bt.bus_id " +
                "WHERE bt.route_id=? " +
                "AND b.active=TRUE " +
                "LIMIT 1",
                routeId
            );


        if (busRows.isEmpty()) {

            m.addAttribute(
                "error",
                "ဒီ Route အတွက် Active Bus မချိတ်ထားသေးပါ"
            );

            return home(s, m);
        }


        int busCapacity =
            ((Number)
                busRows.get(0).get("seat_capacity")
            ).intValue();


        if (seat > busCapacity) {

            m.addAttribute(
                "error",
                "ရွေးထားသော Seat Number သည် Bus Capacity ထက်ကျော်နေပါတယ်"
            );

            return home(s, m);
        }


        // -------------------------------------------------
        // Check duplicate booked seat
        // -------------------------------------------------

        Integer booked =
            db.queryForObject(
                "SELECT COUNT(*) " +
                "FROM ticket " +
                "WHERE route_id=? " +
                "AND seat_number=? " +
                "AND ticket_status='BOOKED'",
                Integer.class,
                routeId,
                seat
            );


        if (
            booked != null &&
            booked > 0
        ) {

            m.addAttribute(
                "error",
                "ဒီထိုင်ခုံကို ကြိုတင်ရောင်းပြီးသားဖြစ်ပါတယ်"
            );

            return home(s, m);
        }


        // -------------------------------------------------
        // Ticket price
        // -------------------------------------------------

        Object priceValue =
            route.get("ticket_price_kyat");


        BigDecimal price;


        if (priceValue instanceof BigDecimal) {

            price =
                (BigDecimal) priceValue;

        } else if (priceValue instanceof Number) {

            price =
                BigDecimal.valueOf(
                    ((Number) priceValue).doubleValue()
                );

        } else {

            price =
                new BigDecimal(
                    String.valueOf(priceValue)
                );
        }


        // -------------------------------------------------
        // Insert Ticket
        // -------------------------------------------------

        db.update(
            "INSERT INTO ticket " +
            "(route_id,passenger_name," +
            "passenger_phone,ticket_date," +
            "seat_number,number_of_seats," +
            "total_amount_kyat,ticket_status) " +
            "VALUES (?,?,?,?,?,?,?,'BOOKED')",

            routeId,
            passengerName,
            passengerPhone,
            LocalDate.now(),
            seat,
            1,
            price
        );


        // -------------------------------------------------
        // Reduce available seats
        // -------------------------------------------------

        db.update(
            "UPDATE route " +
            "SET available_seats=available_seats-1 " +
            "WHERE route_id=? " +
            "AND available_seats>0",
            routeId
        );


        m.addAttribute(
            "success",
            "လက်မှတ်ထုတ်ပြီးပါပြီ"
        );


        return home(s, m);
    }


    // =====================================================
    // CANCEL ROUTE
    // =====================================================

    @PostMapping("/route/{id}/cancel")
    String cancelRoute(
        @PathVariable int id,
        HttpSession s
    ) {

        if (!local(s)) {
            return deny(s);
        }


        db.update(
            "UPDATE route " +
            "SET status='CANCELLED' " +
            "WHERE route_id=?",
            id
        );


        return "redirect:/dashboard#routes";
    }


    // =====================================================
    // MARK TICKET USED
    // =====================================================

    @PostMapping("/ticket/{id}/used")
    String used(
        @PathVariable int id,
        HttpSession s
    ) {

        if (!local(s)) {
            return deny(s);
        }


        db.update(
            "UPDATE ticket " +
            "SET ticket_status='USED' " +
            "WHERE ticket_id=? " +
            "AND ticket_status='BOOKED'",
            id
        );


        return "redirect:/dashboard#tickets";
    }


    // =====================================================
    // SECURITY HELPERS
    // =====================================================

    boolean local(HttpSession s) {

        Object admin =
            s.getAttribute("admin");


        if (!(admin instanceof Map)) {
            return false;
        }


        Map<?, ?> a =
            (Map<?, ?>) admin;


        return "LOCAL_ADMIN".equals(
            String.valueOf(
                a.get("admin_role")
            )
        );
    }


    boolean global(HttpSession s) {

        Object admin =
            s.getAttribute("admin");


        if (!(admin instanceof Map)) {
            return false;
        }


        Map<?, ?> a =
            (Map<?, ?>) admin;


        return "GLOBAL_ADMIN".equals(
            String.valueOf(
                a.get("admin_role")
            )
        );
    }


    String deny(HttpSession s) {

        return "redirect:/";
    }
}