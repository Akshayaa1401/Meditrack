# Meditrack — Smart Biomedical Waste Management and Collection System

A full-stack JSP/Servlet/MongoDB application for hospitals to report
biomedical waste and for collectors to receive priority-ranked,
route-optimized pickups — with **automatic mid-route re-optimization**
when a new emergency request comes in.

---

## 1. Tech stack

| Layer        | Technology                                            |
|--------------|--------------------------------------------------------|
| Views        | JSP 2.3 + JSTL                                          |
| Controllers  | Java Servlets (Servlet 4.0, annotation-based routing)   |
| Server       | Apache Tomcat 9.x                                       |
| Database     | MongoDB 6.x/7.x (mongodb-driver-sync 4.11.1)            |
| Frontend     | HTML5, CSS3 (no framework), vanilla JavaScript          |
| Maps         | Leaflet.js + OpenStreetMap tiles (no API key required)  |
| Charts       | Chart.js (CDN)                                          |
| Build        | Maven (packaged as a `.war`)                            |

---

## 2. Project structure

```
meditrack/
├── pom.xml
└── src/main/
    ├── java/com/meditrack/
    │   ├── model/        # POJOs <-> MongoDB Document mappers
    │   │   ├── User.java, Hospital.java, WasteRecord.java,
    │   │   ├── Collector.java, CollectionAssignment.java,
    │   │   └── CollectionHistory.java, RouteUpdate.java
    │   ├── util/
    │   │   ├── MongoDBConnection.java   # singleton Mongo client
    │   │   ├── PasswordUtil.java        # salted SHA-256 hashing
    │   │   ├── PriorityCalculator.java  # risk+quantity+aging scoring
    │   │   └── RouteOptimizer.java      # nearest-neighbor + cheapest insertion
    │   ├── filter/
    │   │   └── AuthFilter.java          # session-based RBAC
    │   └── servlet/
    │       ├── RegisterServlet, LoginServlet, LogoutServlet
    │       ├── HospitalRegisterServlet, WasteSubmitServlet, HospitalDashboardServlet
    │       ├── RouteOptimizeServlet, EmergencyReoptimizeServlet
    │       ├── CollectorDashboardServlet, CollectorStatusUpdateServlet
    │       ├── AdminDashboardServlet, AdminHospitalsServlet, AdminCollectorsServlet
    │       ├── AnalyticsServlet
    │       └── MapDataServlet, RouteUpdateApiServlet   # JSON APIs for the map/JS
    └── webapp/
        ├── WEB-INF/
        │   ├── web.xml
        │   ├── jspf/ (taglibs.jspf, navbar.jspf — shared fragments)
        │   └── views/ (hospital/, collector/, admin/, error pages)
        ├── css/style.css
        ├── js/ (map.js, dashboard.js, validation.js)
        ├── index.jsp, login.jsp, register.jsp
```

---

## 3. MongoDB schema (collections)

MongoDB is schemaless, but here is the shape each collection holds:

**`users`**
```json
{ "_id": ObjectId, "fullName": "", "email": "", "passwordHash": "salt:hash",
  "role": "ADMIN|HOSPITAL_STAFF|COLLECTOR", "hospitalId": "", "collectorId": "",
  "phone": "", "createdAt": "ISO-8601" }
```

**`hospitals`**
```json
{ "_id": ObjectId, "name": "", "address": "", "latitude": 0.0, "longitude": 0.0,
  "contactPerson": "", "contactPhone": "", "registeredBy": "userId", "createdAt": "" }
```

**`wasteRecords`**
```json
{ "_id": ObjectId, "hospitalId": "", "hospitalName": "", "wasteType": "",
  "quantityKg": 0.0, "riskLevel": "LOW|MEDIUM|HIGH", "description": "",
  "reportedAt": "ISO-8601", "status": "PENDING|ASSIGNED|COLLECTED|DISPOSED",
  "priorityScore": 0.0, "emergency": false, "assignedCollectorId": "",
  "collectedAt": "", "disposedAt": "" }
```

**`collectors`**
```json
{ "_id": ObjectId, "userId": "", "fullName": "", "vehicleNumber": "",
  "currentLat": 0.0, "currentLng": 0.0, "status": "AVAILABLE|ON_ROUTE|OFF_DUTY",
  "totalCollections": 0, "createdAt": "" }
```

**`collectionAssignments`**
```json
{ "_id": ObjectId, "collectorId": "", "status": "ACTIVE|SUPERSEDED|COMPLETED",
  "route": [ { "wasteId":"", "hospitalId":"", "hospitalName":"", "latitude":0.0,
               "longitude":0.0, "priorityScore":0.0, "emergency": false } ],
  "totalDistanceKm": 0.0, "createdAt": "", "updatedAt": "" }
```

**`collectionHistory`**
```json
{ "_id": ObjectId, "wasteId": "", "hospitalId": "", "hospitalName": "",
  "collectorId": "", "wasteType": "", "quantityKg": 0.0, "riskLevel": "",
  "collectedAt": "", "disposedAt": "", "priorityScoreAtCollection": 0.0 }
```

**`routeUpdates`** (the dynamic re-optimization audit log)
```json
{ "_id": ObjectId, "collectorId": "", "triggeringWasteId": "", "reason": "",
  "oldRoute": [ ...stops ], "newRoute": [ ...stops ],
  "oldDistanceKm": 0.0, "newDistanceKm": 0.0, "createdAt": "" }
```

Recommended indexes (run once in `mongosh`):
```js
use meditrack
db.users.createIndex({ email: 1 }, { unique: true })
db.wasteRecords.createIndex({ status: 1 })
db.wasteRecords.createIndex({ hospitalId: 1 })
db.collectionAssignments.createIndex({ collectorId: 1, status: 1 })
db.collectionHistory.createIndex({ disposedAt: 1 })
```

---

## 4. How the core logic works

- **Priority score** (`PriorityCalculator`): `0.40×risk + 0.30×quantity + 0.30×aging`,
  each sub-score normalized to 0–100. Anything HIGH risk, or scoring ≥ 75, is
  flagged `emergency: true`.
- **Route building** (`RouteOptimizer.buildRoute`): priority-weighted nearest
  neighbor — at each step picks the pending stop maximizing
  `(priority+1)/(distance+0.1)`, with a 3× boost for emergencies.
- **Dynamic re-optimization** (the unique feature): when `WasteSubmitServlet`
  detects a new emergency, it calls
  `EmergencyReoptimizeServlet.reoptimizeForEmergency(...)`, which finds the
  nearest collector with an `ACTIVE` route and uses **cheapest insertion** to
  splice the emergency stop in at the position adding the least extra travel
  distance. The before/after route and distances are saved to `routeUpdates`
  so the collector dashboard can render an "old vs. updated route" comparison.

---

## 5. Prerequisites

- JDK 11+ (`java -version`)
- Apache Maven 3.6+
- Apache Tomcat 9.x (Servlet 4.0 / JSP 2.3)
- MongoDB 6.x or 7.x running locally or reachable via URI

---

## 6. Setup instructions

### 6.1 Install & start MongoDB
```bash
# Ubuntu/Debian example — adjust for your OS / use MongoDB Atlas instead
sudo systemctl start mongod
mongosh --eval "db.runCommand({ ping: 1 })"   # sanity check
```

### 6.2 Configure the database connection (optional)
By default the app connects to `mongodb://localhost:27017` and uses
database `meditrack`. To override, set environment variables **before**
starting Tomcat:
```bash
export MONGO_URI="mongodb://localhost:27017"
export MONGO_DB="meditrack"
```
(or edit the `<context-param>` defaults in `WEB-INF/web.xml` / the
constants in `MongoDBConnection.java`.)

### 6.3 Build the WAR
```bash
cd meditrack
mvn clean package
# produces target/meditrack.war
```

### 6.4 Deploy to Tomcat
```bash
# Option A: copy the WAR into Tomcat's webapps folder
cp target/meditrack.war $CATALINA_HOME/webapps/

# Option B: use the Tomcat Manager app to upload meditrack.war

# Start Tomcat (if not already running)
$CATALINA_HOME/bin/startup.sh      # Linux/Mac
$CATALINA_HOME/bin/startup.bat     # Windows
```
Tomcat auto-explodes the WAR and deploys it at context path `/meditrack`.

### 6.5 Open the app
```
http://localhost:8080/meditrack/
```
You'll land on the login page. Click **Register here** to create your
first account — pick **Administrator** for a full system overview, or
**Hospital Staff** / **Waste Collector** to try those flows.

---

## 7. Suggested first walkthrough

1. Register an **Admin** account, an **Hospital Staff** account, and a
   **Waste Collector** account (3 separate browser sessions/incognito tabs,
   or log out/in between).
2. As Hospital Staff: go to **Hospital Profile** → click a point on the map
   → **Save hospital profile**.
3. As Hospital Staff: **Report Waste** → submit a LOW or MEDIUM risk record.
4. As Collector: click **Re-generate Route** → see the pending stop appear
   on the route map.
5. Back as Hospital Staff: submit a **HIGH** risk waste record from a
   different (or the same) hospital. This is auto-flagged as an emergency.
6. As Collector: refresh the dashboard — the emergency banner appears, and
   the **"Show old vs. new route"** panel on the dashboard shows the
   before/after comparison map.
7. As Admin: check **Dashboard** for the active-emergency list and the
   recent route re-optimization log, and **Analytics** for charts.

---

## 8. Running locally without a separate Tomcat install (dev convenience)

If you have the Tomcat Maven plugin available in your environment, you can
add it to `pom.xml` and run `mvn tomcat7:run` or `mvn cargo:run` instead of
manually copying the WAR. This project ships without that plugin pre-wired
so it stays portable to any Tomcat 9 install, but it's a one-block addition
if you want hot-reload during development.

---

## 9. Security notes for production hardening

- Passwords are hashed with salted SHA-256 here to minimize dependencies.
  For production, swap in bcrypt or Argon2 (e.g. the `jBCrypt` library).
- Add HTTPS (terminate TLS at a reverse proxy like Nginx, or configure a
  Tomcat connector with a keystore).
- Set `MONGO_URI` to a credentialed connection string
  (`mongodb://user:pass@host:27017/meditrack?authSource=admin`) and enable
  MongoDB auth — local dev defaults assume an open local instance.
- Consider rate-limiting `/login` and `/register` to deter brute force.

---

## 10. Troubleshooting

| Symptom | Likely cause |
|---|---|
| Blank page / 500 on every page | MongoDB not running, or wrong `MONGO_URI` |
| 403 right after login | Session role doesn't match the URL prefix (`/admin`, `/hospital`, `/collector`) — log out and log back in with the right role |
| Map doesn't render | Browser blocked the Leaflet/OSM CDN — check console, ensure internet access from the browser (not the server) |
| JSTL tags print literally (`<c:forEach ...>` shown as text) | JSTL jar missing from `WEB-INF/lib` — re-run `mvn clean package` and confirm the dependency resolved |
