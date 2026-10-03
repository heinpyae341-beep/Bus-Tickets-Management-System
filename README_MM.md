# မြန်မာ ဘတ်စ်ကားလက်မှတ် Web Application — Java Version

ဤ module သည် **Java Spring Boot + Thymeleaf + Relational MySQL + Chrome** ဖြင့်ရေးထားသော မြန်မာဘတ်စ်ကားလက်မှတ် Web Application ဖြစ်သည်။ XAMPP ကို မလိုအပ်ပါ။ MySQL Server ကို သီးခြား install လုပ်ထားပြီး MySQL Workbench ဖြင့် database စီမံနိုင်သည်။

## အသုံးပြုမည့် Connection Setting

သင့်စက်တွင် MySQL Server က port `3306` တွင် run နေသောကြောင့် `application.properties` တွင် အောက်ပါ setting သုံးထားသည်။

```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/bus_tickets_db?useSSL=false&serverTimezone=Asia/Yangon&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=
```

Root password သတ်မှတ်ထားပါက `spring.datasource.password=` နောက်တွင် ထည့်ပါ။

## MySQL Workbench မှ Database တည်ဆောက်ခြင်း

MySQL Server ကို Windows Services ထဲတွင် Running ဖြစ်အောင်လုပ်ပြီး MySQL Workbench ကိုဖွင့်ပါ။ Local connection ကိုဖွင့်ကာ SQL Editor ထဲသို့ `src/main/resources/schema.sql` ဖိုင်၏အကြောင်းအရာကို copy/paste လုပ်ပါ။ Lightning icon ကိုနှိပ်ပြီး Run လုပ်ပါ။ `bus_tickets_db` schema နှင့် `admin_users`, `bus`, `trip`, `route`, `bus_trip`, `ticket` tables များပေါ်လာရမည်။

## VS Code မှ ဖွင့်ခြင်း

VS Code ကိုဖွင့်ပြီး `java-web` folder ကို `File → Open Folder` ဖြင့်ဖွင့်ပါ။ `Extension Pack for Java` နှင့် `Spring Boot Extension Pack` ရှိလျှင် install လုပ်ပါ။ JDK 21 အသုံးပြုပါ။ VS Code က Maven dependencies များ download လုပ်ပြီး Java project ကို load လုပ်ပါမည်။

## Java Application Run လုပ်ခြင်း

`src/main/java/com/myanmar/bus/BusTicketApplication.java` ကိုဖွင့်ပါ။ အပေါ်မှ `Run` ကိုနှိပ်ပါ၊ သို့မဟုတ် `Run and Debug` မှ `Run Bus Ticket Spring Boot` ကိုရွေးပါ။ Terminal တွင် `Started BusTicketApplication` နှင့် port `8080` စတင်ကြောင်း ပေါ်လာသည်အထိစောင့်ပါ။

ထို့နောက် Chrome ကိုဖွင့်ပြီး—

```text
http://localhost:8080
```

သို့သွားပါ။ VS Code Terminal/Run panel မှာ application ကို Stop မလုပ်ရပါ။ Stop လုပ်လျှင် Chrome web application ရပ်သွားမည်။

## Default Login

Global Admin သည်—

```text
Username: globaladmin
Password: global123
```

Local Admin သည်—

```text
Username: localadmin
Password: local123
```

ဖြင့် login ဝင်နိုင်သည်။

## Global Admin Dashboard

Global Admin login ဝင်လျှင် သီးခြား Global Admin Dashboard သို့ ရောက်မည်။ Global Admin သည် Local Admin account များကို ဖန်တီးခြင်း၊ activate/deactivate လုပ်ခြင်းနှင့် စီမံခြင်းလုပ်နိုင်သည်။ Bus, Trip, Route နှင့် Ticket များကို **ကြည့်ရှုရုံသာ** ဖြစ်သည်။ Bus ထည့်ခြင်း၊ Trip ပြင်ခြင်း၊ Route ပယ်ဖျက်ခြင်း၊ BUS_TRIP ချိတ်ခြင်းနှင့် Ticket status ပြောင်းခြင်းများ မလုပ်နိုင်ပါ။

## Local Admin Dashboard

Local Admin login ဝင်လျှင် သီးခြား Local Admin Operations Dashboard သို့ ရောက်မည်။ Local Admin သည် Bus, Trip, Route, BUS_TRIP assignment နှင့် Ticket များကို စီမံနိုင်သည်။ Passenger အမည်၊ မြန်မာဖုန်းနံပါတ်၊ route နှင့် seat number ဖြည့်ပြီး ticket ထုတ်နိုင်သည်။

## အသုံးပြုရန် အစီအစဉ်

ပထမဦးစွာ Bus ထည့်ပါ။ ထို့နောက် Trip ထည့်ပါ။ Trip ID ကိုသုံးပြီး Route date, departure time, arrival time, available seats နှင့် ticket price ကိုထည့်ပါ။ ထို့နောက် Bus ID နှင့် Route ID ကို BUS_TRIP assignment ဖြင့်ချိတ်ပါ။ နောက်ဆုံး Passenger Ticket ထုတ်ပါ။

Ticket ထုတ်ရာတွင် Java က Route status, available seat နှင့် ရွေးထားသော seat ကိုစစ်သည်။ Ticket အောင်မြင်လျှင် `ticket` table ထဲသို့ record ထည့်ပြီး Route ၏ available seat ကို တစ်ခုံလျှော့သည်။ ထိုလုပ်ဆောင်ချက်များကို transaction ဖြင့်လုပ်ထားသောကြောင့် seat မလုံလောက်ပါက ticket မထုတ်ပေးပါ။

## လွယ်ကူသော အသုံးပြုနည်း (Local Admin)

လုပ်ငန်းစဉ်ကို ရှုပ်မသွားအောင် Dashboard တွင် အဆင့် ၅ ဆင့်အဖြစ် စီထားသည်။

1. **① Bus အသစ်** — Bus Number, Type, Seat Capacity ထည့်ပါ။ Bus Number တူတာကို system က မထည့်ပေးပါ။
2. **② Trip အသစ်** — ထွက်ခွာရာ၊ ရောက်ရာ၊ အကွာအဝေး ထည့်ပါ။
3. **③ Route အသစ်** — Trip ကို dropdown မှ ရွေးပြီး Date, Departure Time, Arrival Time, Seats, Price ထည့်ပါ။ Trip ID ကို မှတ်ထားပြီး ကိုယ်တိုင်ရိုက်ရန် မလိုပါ။
4. **④ Bus → Route ချိတ်ရန်** — Bus နှင့် Route ကို dropdown မှ ရွေးပါ။ Bus/Route ID ကို ကိုယ်တိုင်ရိုက်ရန် မလိုပါ။
5. **⑤ Ticket ထုတ်ရန်** — Route ကို dropdown မှ ရွေးပြီး Passenger Name, Phone, Seat Number ဖြည့်ပါ။ System က route status, available seats, duplicate booked seat နှင့် assigned bus capacity ကို စစ်ပေးပါသည်။

Global Admin Dashboard တွင် Bus → Route → Trip ဆက်စပ်မှုကို တစ်ကြောင်းတည်းနှင့် Route → Ticket ဆက်စပ်မှုကို တစ်ကြောင်းတည်းဖြင့် ကြည့်နိုင်သည်။ Global Admin သည် operational data ကို ပြင်ဆင်ခွင့်မရှိဘဲ Local Admin account များကိုသာ စီမံနိုင်သည်။

## Database Relationship

```text
BUS 1 ─── M BUS_TRIP M ─── 1 ROUTE
TRIP 1 ─── M ROUTE
ROUTE 1 ─── M TICKET
```

`bus_trip` သည် Bus နှင့် Route ကိုချိတ်ပေးသော relationship table ဖြစ်ပြီး composite primary key `(bus_id, route_id)` ပါဝင်သည်။

## Source Files

`BusTicketApplication.java` တွင် Spring Boot start, login/logout, role checking, Local Admin management, Bus/Trip/Route/BUS_TRIP/Ticket operations နှင့် ticket transaction ပါဝင်သည်။ `RolePolicy.java` တွင် Global Admin နှင့် Local Admin permission rules ပါဝင်သည်။ `login.html` သည် login page ဖြစ်ပြီး `global.html` သည် Global Admin read-only dashboard ဖြစ်သည်။ `dashboard.html` သည် Local Admin management dashboard ဖြစ်သည်။ `style.css` သည် Myanmar Unicode responsive design ဖြစ်သည်။ `schema.sql` သည် MySQL Workbench တွင် run ရမည့် relational database script ဖြစ်သည်။

## MySQL Login မဝင်ပါက

MySQL Workbench မှာအသုံးပြုသော root password ကို `application.properties` ထဲမှာတူအောင်ထည့်ပါ။ MySQL Server က port `3306` မှာ Running ဖြစ်ရမည်။ Windows `services.msc` ထဲတွင် `MySQL80` သို့မဟုတ် သင့် MySQL service ကို Start လုပ်ထားကြောင်းစစ်ပါ။
