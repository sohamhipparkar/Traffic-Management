package org.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TrafficUI {

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // ROOT UI ROUTE
        server.createContext("/", exchange -> {

            String html = """
<!DOCTYPE html>
<html>
<head>
    <title>Smart Traffic System</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Syne:wght@700;800&display=swap" rel="stylesheet">
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }

        :root {
            --amber: #EF9F27;
            --amber-light: #FAEEDA;
            --amber-dark: #633806;
            --red: #E24B4A;
            --red-light: #FCEBEB;
            --green: #639922;
            --green-light: #EAF3DE;
            --teal: #1D9E75;
            --teal-light: #E1F5EE;
            --gray-bg: #F1EFE8;
            --gray-mid: #888780;
            --gray-dark: #2C2C2A;
            --card-bg: #ffffff;
            --border: rgba(0,0,0,0.1);
            --border-light: rgba(0,0,0,0.07);
            --input-bg: #F1EFE8;
            --text-primary: #2C2C2A;
            --text-secondary: #888780;
            --page-bg: #F5F4EF;
        }

        body {
            font-family: 'DM Mono', monospace;
            background: var(--page-bg);
            min-height: 100vh;
            padding: 2rem 1rem 3rem;
        }

        .shell {
            max-width: 520px;
            margin: 0 auto;
        }

        /* ── HEADER ── */
        .header { margin-bottom: 2rem; }

        .header-tag {
            font-size: 11px;
            font-weight: 500;
            letter-spacing: 0.12em;
            color: var(--amber);
            text-transform: uppercase;
            margin-bottom: 0.4rem;
        }

        .header h1 {
            font-family: 'Syne', sans-serif;
            font-size: 30px;
            font-weight: 800;
            color: var(--text-primary);
            line-height: 1.1;
            letter-spacing: -0.02em;
        }

        .header h1 span { color: var(--amber); }

        .signal-row {
            display: flex;
            gap: 6px;
            margin-top: 10px;
            align-items: center;
        }

        .sig {
            width: 10px; height: 10px;
            border-radius: 50%;
            background: var(--gray-mid);
            opacity: 0.25;
            transition: opacity 0.4s, background 0.4s;
        }
        .sig.red   { background: var(--red);   opacity: 1; }
        .sig.amber { background: var(--amber); opacity: 1; }
        .sig.green { background: var(--green); opacity: 1; }

        /* ── CARD ── */
        .card {
            background: var(--card-bg);
            border-radius: 16px;
            border: 0.5px solid var(--border);
            padding: 1.5rem;
            margin-bottom: 1rem;
        }

        .section-label {
            font-size: 10px;
            font-weight: 500;
            letter-spacing: 0.1em;
            text-transform: uppercase;
            color: var(--gray-mid);
            margin-bottom: 1rem;
            padding-bottom: 0.5rem;
            border-bottom: 0.5px solid var(--border-light);
        }

        /* ── FIELDS ── */
        .field-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 12px;
            margin-bottom: 12px;
        }

        .field { display: flex; flex-direction: column; gap: 6px; }
        .field.full { grid-column: 1 / -1; }

        .field label {
            font-size: 11px;
            font-weight: 500;
            letter-spacing: 0.06em;
            color: var(--text-secondary);
            text-transform: uppercase;
        }

        .field input[type="text"],
        .field input[type="number"],
        .field select {
            font-family: 'DM Mono', monospace;
            font-size: 14px;
            padding: 10px 12px;
            border: 0.5px solid rgba(0,0,0,0.15);
            border-radius: 8px;
            background: var(--input-bg);
            color: var(--text-primary);
            outline: none;
            transition: border-color 0.2s, background 0.2s;
            width: 100%;
            appearance: none;
            -webkit-appearance: none;
        }

        .field input:focus,
        .field select:focus {
            border-color: var(--amber);
            background: var(--card-bg);
        }

        .speed-wrap { position: relative; }
        .speed-wrap input { padding-right: 52px; }
        .speed-unit {
            position: absolute;
            right: 12px;
            top: 50%;
            transform: translateY(-50%);
            font-size: 11px;
            color: var(--gray-mid);
            pointer-events: none;
            font-weight: 500;
        }

        /* ── EMERGENCY TOGGLE ── */
        .toggle-row {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 12px;
            background: var(--input-bg);
            border-radius: 8px;
            border: 0.5px solid var(--border-light);
            cursor: pointer;
            transition: background 0.2s, border-color 0.2s;
            user-select: none;
        }

        .toggle-row:hover { background: var(--amber-light); border-color: var(--amber); }

        .toggle-switch {
            width: 36px; height: 20px;
            background: rgba(0,0,0,0.15);
            border-radius: 10px;
            position: relative;
            transition: background 0.3s;
            flex-shrink: 0;
        }

        .toggle-switch::after {
            content: '';
            position: absolute;
            top: 3px; left: 3px;
            width: 14px; height: 14px;
            border-radius: 50%;
            background: #fff;
            transition: transform 0.3s;
            box-shadow: 0 1px 3px rgba(0,0,0,0.2);
        }

        .toggle-switch.on { background: var(--red); }
        .toggle-switch.on::after { transform: translateX(16px); }

        .toggle-label { font-size: 13px; color: var(--text-primary); flex: 1; }

        .toggle-badge {
            font-size: 10px;
            font-weight: 500;
            padding: 2px 8px;
            border-radius: 4px;
            background: var(--red-light);
            color: var(--red);
            opacity: 0;
            transition: opacity 0.3s;
            letter-spacing: 0.06em;
        }
        .toggle-badge.visible { opacity: 1; }

        /* ── PROCESS BUTTON ── */
        .process-btn {
            width: 100%;
            padding: 14px;
            background: var(--gray-dark);
            color: #fff;
            border: none;
            border-radius: 10px;
            font-family: 'DM Mono', monospace;
            font-size: 14px;
            font-weight: 500;
            letter-spacing: 0.04em;
            cursor: pointer;
            transition: background 0.2s, transform 0.1s;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
        }

        .process-btn:hover { background: #444441; }
        .process-btn:active { transform: scale(0.99); }
        .process-btn.loading { background: var(--gray-mid); cursor: not-allowed; pointer-events: none; }

        .btn-arrow { transition: transform 0.2s; }
        .process-btn:hover .btn-arrow { transform: translateX(3px); }

        /* ── RESULT ── */
        .result-card {
            border-radius: 12px;
            padding: 1.25rem;
            display: none;
            margin: 1rem 0;
            animation: slideIn 0.3s ease;
        }

        @keyframes slideIn {
            from { opacity: 0; transform: translateY(6px); }
            to   { opacity: 1; transform: translateY(0); }
        }

        .result-card.show { display: block; }

        .result-card.ok        { background: var(--teal-light);  border: 0.5px solid #5DCAA5; }
        .result-card.violation { background: var(--red-light);   border: 0.5px solid #F09595; }
        .result-card.error     { background: var(--amber-light); border: 0.5px solid #FAC775; }

        .result-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }

        .result-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
        .ok .result-dot        { background: var(--teal); }
        .violation .result-dot { background: var(--red); }
        .error .result-dot     { background: var(--amber); }

        .result-title {
            font-size: 11px;
            font-weight: 500;
            letter-spacing: 0.08em;
            text-transform: uppercase;
        }
        .ok .result-title        { color: #0F6E56; }
        .violation .result-title { color: #A32D2D; }
        .error .result-title     { color: var(--amber-dark); }

        .result-body { font-size: 13px; line-height: 1.6; }
        .ok .result-body        { color: #085041; }
        .violation .result-body { color: #791F1F; }
        .error .result-body     { color: var(--amber-dark); }

        /* ── STATS ── */
        .stats-row {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 8px;
            margin-bottom: 1rem;
        }

        .stat {
            background: var(--card-bg);
            border-radius: 10px;
            padding: 12px;
            border: 0.5px solid var(--border);
        }

        .stat-num {
            font-family: 'Syne', sans-serif;
            font-size: 24px;
            font-weight: 700;
            color: var(--text-primary);
            line-height: 1;
        }

        .stat-lbl {
            font-size: 10px;
            text-transform: uppercase;
            letter-spacing: 0.08em;
            color: var(--gray-mid);
            margin-top: 4px;
        }

        /* ── EVENT LOG ── */
        .log-card {
            background: var(--card-bg);
            border-radius: 16px;
            border: 0.5px solid var(--border);
            overflow: hidden;
        }

        .log-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 1rem 1.5rem;
            border-bottom: 0.5px solid var(--border-light);
        }

        .log-title {
            font-size: 10px;
            font-weight: 500;
            letter-spacing: 0.1em;
            text-transform: uppercase;
            color: var(--gray-mid);
        }

        .clear-btn {
            font-family: 'DM Mono', monospace;
            font-size: 11px;
            color: var(--gray-mid);
            background: none;
            border: 0.5px solid rgba(0,0,0,0.12);
            border-radius: 4px;
            padding: 3px 8px;
            cursor: pointer;
            transition: color 0.2s, border-color 0.2s;
        }
        .clear-btn:hover { color: var(--red); border-color: var(--red); }

        .log-body {
            padding: 0.75rem 1.5rem;
            max-height: 200px;
            overflow-y: auto;
        }

        .log-empty { font-size: 12px; color: var(--gray-mid); padding: 0.5rem 0; font-style: italic; }

        .log-item {
            display: flex;
            align-items: flex-start;
            gap: 10px;
            padding: 7px 0;
            border-bottom: 0.5px solid var(--border-light);
            font-size: 12px;
            line-height: 1.4;
        }
        .log-item:last-child { border-bottom: none; }

        .log-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; margin-top: 4px; }
        .log-dot.ok   { background: var(--teal); }
        .log-dot.viol { background: var(--red); }
        .log-dot.err  { background: var(--amber); }

        .log-text { color: var(--text-secondary); flex: 1; }
        .log-time { color: var(--gray-mid); font-size: 10px; flex-shrink: 0; }
    </style>
</head>
<body>
    <div class="shell">

        <!-- Header -->
        <div class="header">
            <div class="header-tag">Traffic Control v2</div>
            <h1>Smart <span>Traffic</span><br>System</h1>
            <div class="signal-row">
                <div class="sig" id="sig-r"></div>
                <div class="sig" id="sig-a"></div>
                <div class="sig" id="sig-g"></div>
            </div>
        </div>

        <!-- Stats -->
        <div class="stats-row">
            <div class="stat">
                <div class="stat-num" id="s-total">0</div>
                <div class="stat-lbl">Processed</div>
            </div>
            <div class="stat">
                <div class="stat-num" id="s-viol">0</div>
                <div class="stat-lbl">Violations</div>
            </div>
            <div class="stat">
                <div class="stat-num" id="s-emg">0</div>
                <div class="stat-lbl">Emergency</div>
            </div>
        </div>

        <!-- Input Card -->
        <div class="card">
            <div class="section-label">Vehicle event</div>

            <div class="field-grid">
                <div class="field">
                    <label>Vehicle ID</label>
                    <input type="text" id="vid" placeholder="e.g. MH12AB1234">
                </div>
                <div class="field">
                    <label>Zone</label>
                    <select id="zone">
                        <option value="">Select zone</option>
                        <option value="school">School zone</option>
                        <option value="highway">Highway</option>
                        <option value="residential">Residential</option>
                        <option value="commercial">Commercial</option>
                        <option value="hospital">Hospital zone</option>
                    </select>
                </div>
                <div class="field full">
                    <label>Speed</label>
                    <div class="speed-wrap">
                        <input type="number" id="speed" placeholder="0" min="0" max="300">
                        <span class="speed-unit">km/h</span>
                    </div>
                </div>
            </div>

            <div class="toggle-row" id="emergency-row" onclick="toggleEmergency()">
                <div class="toggle-switch" id="toggle"></div>
                <span class="toggle-label">Emergency vehicle</span>
                <span class="toggle-badge" id="emg-badge">ACTIVE</span>
            </div>
        </div>

        <!-- Submit -->
        <button class="process-btn" id="submit-btn" onclick="send()">
            Process event
            <span class="btn-arrow">&#8594;</span>
        </button>

        <!-- Result -->
        <div id="result" class="result-card">
            <div class="result-header">
                <div class="result-dot"></div>
                <div class="result-title" id="result-title"></div>
            </div>
            <div class="result-body" id="result-body"></div>
        </div>

        <br />
        <!-- Event Log -->
        <div class="log-card">
            <div class="log-header">
                <span class="log-title">Event log</span>
                <button class="clear-btn" onclick="clearLog()">Clear</button>
            </div>
            <div class="log-body" id="log">
                <div class="log-empty" id="log-empty">No events recorded yet</div>
            </div>
        </div>

    </div>

    <script>
        let emergency = false;
        let stats = { total: 0, viol: 0, emg: 0 };
        let logs = [];

        function toggleEmergency() {
            emergency = !emergency;
            document.getElementById('toggle').classList.toggle('on', emergency);
            document.getElementById('emg-badge').classList.toggle('visible', emergency);
            setSignal(emergency ? 'amber' : 'idle');
        }

        function setSignal(state) {
            ['sig-r','sig-a','sig-g'].forEach(id => {
                document.getElementById(id).className = 'sig';
            });
            if (state === 'red')   document.getElementById('sig-r').className = 'sig red';
            if (state === 'amber') document.getElementById('sig-a').className = 'sig amber';
            if (state === 'green') document.getElementById('sig-g').className = 'sig green';
        }

        function updateStats() {
            document.getElementById('s-total').textContent = stats.total;
            document.getElementById('s-viol').textContent  = stats.viol;
            document.getElementById('s-emg').textContent   = stats.emg;
        }

        function addLog(type, message) {
            const time = new Date().toLocaleTimeString('en-IN', {
                hour: '2-digit', minute: '2-digit', second: '2-digit'
            });
            logs.unshift({ type, message, time });
            renderLog();
        }

        function renderLog() {
            const el    = document.getElementById('log');
            const empty = document.getElementById('log-empty');
            el.querySelectorAll('.log-item').forEach(i => i.remove());
            if (logs.length === 0) { empty.style.display = 'block'; return; }
            empty.style.display = 'none';
            logs.slice(0, 20).forEach(l => {
                const div = document.createElement('div');
                div.className = 'log-item';
                div.innerHTML =
                    '<div class="log-dot ' + l.type + '"></div>' +
                    '<span class="log-text">' + escapeHtml(l.message) + '</span>' +
                    '<span class="log-time">' + l.time + '</span>';
                el.appendChild(div);
            });
        }

        function clearLog() { logs = []; renderLog(); }

        function escapeHtml(str) {
            return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
        }

        function showResult(type, title, body) {
            const el = document.getElementById('result');
            el.className = 'result-card show ' + type;
            document.getElementById('result-title').textContent = title;
            document.getElementById('result-body').textContent  = body;
        }

        async function send() {
            const btn   = document.getElementById('submit-btn');
            const id    = document.getElementById('vid').value.trim();
            const speed = document.getElementById('speed').value.trim();
            const zone  = document.getElementById('zone').value;

            if (!id || !speed || !zone) {
                showResult('error', 'Incomplete input', 'Please fill in all fields before processing.');
                setSignal('red');
                return;
            }

            btn.classList.add('loading');
            btn.innerHTML = 'Processing... <span class="btn-arrow">&#8594;</span>';
            setSignal('amber');

            const url = '/process?id=' + encodeURIComponent(id)
                      + '&speed='     + encodeURIComponent(speed)
                      + '&zone='      + encodeURIComponent(zone)
                      + '&emergency=' + emergency;

            try {
                const res  = await fetch(url);
                const text = await res.text();

                stats.total++;
                if (emergency) stats.emg++;

                if (text.includes('Violation')) {
                    stats.viol++;
                    showResult('violation', 'Violation detected', text);
                    addLog('viol', '[' + id + '] ' + text);
                    setSignal('red');
                } else if (text.includes('Invalid')) {
                    showResult('error', 'Invalid input', text);
                    addLog('err', '[' + id + '] ' + text);
                    setSignal('red');
                } else {
                    showResult('ok', 'All clear', text);
                    addLog('ok', '[' + id + '] ' + text);
                    setSignal('green');
                }

                updateStats();

            } catch (e) {
                showResult('error', 'Connection error',
                    'Could not reach the traffic server. Check that the Java backend is running.');
                addLog('err', 'Server connection failed');
                setSignal('red');
            }

            btn.classList.remove('loading');
            btn.innerHTML = 'Process event <span class="btn-arrow">&#8594;</span>';
        }
    </script>
</body>
</html>
""";

            byte[] responseBytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });

        // PROCESS API (UNCHANGED LOGIC)
        server.createContext("/process", exchange -> {

            if ("GET".equals(exchange.getRequestMethod())) {

                Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());

                try {
                    String id       = params.get("id");
                    double speed    = Double.parseDouble(params.get("speed"));
                    String zone     = params.get("zone");
                    boolean isEmerg = Boolean.parseBoolean(params.get("emergency"));

                    SmartTrafficSystem.VehicleEvent event =
                            new SmartTrafficSystem.VehicleEvent(
                                    id, speed, zone, isEmerg,
                                    System.currentTimeMillis()
                            );

                    List<SmartTrafficSystem.VehicleEvent> events = Arrays.asList(event);

                    List<SmartTrafficSystem.ViolationRecord> violations =
                            events.stream()
                                    .filter(SmartTrafficSystem.TrafficRules.violationFilter)
                                    .map(ev -> new SmartTrafficSystem.ViolationRecord(
                                            ev.vehicleId,
                                            ev.speed,
                                            ev.zone,
                                            SmartTrafficSystem.fineCalculator.apply(ev.speed)
                                    ))
                                    .toList();

                    String response;
                    if (violations.isEmpty()) {
                        response = "No violation detected";
                    } else {
                        SmartTrafficSystem.ViolationRecord v = violations.get(0);
                        SmartTrafficSystem.saveViolation(v);
                        response = "Violation: " + v.toString();
                    }

                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(responseBytes);
                    os.close();

                } catch (Exception ex) {
                    String response = "Invalid input";
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(400, responseBytes.length);
                    exchange.getResponseBody().write(responseBytes);
                    exchange.close();
                }
            }
        });

        server.start();
        System.out.println("Server running on port " + port);
    }

    private static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) result.put(pair[0], pair[1]);
        }
        return result;
    }
}