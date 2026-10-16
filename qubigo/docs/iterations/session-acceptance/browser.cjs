// Disposable HTTPS acceptance; no production data, intercepted auth, or cookie injection.
// See ../003-session-persistence-task-5.md for prerequisites and invocation.
const {spawn, spawnSync} = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const http = require('node:http');
const https = require('node:https');
const assert = require('node:assert/strict');
const {chromium} = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const root = path.resolve(__dirname, '../../..');
const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'qubigo-browser-'));
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
const children = [];
let browser, server, backend, backendPort, vitePort;
const evidence = {client: '', pids: [], network: []};
async function until(fn, timeout = 60000) {
    const end = Date.now() + timeout;
    while (Date.now() < end) { if (await fn()) return; await delay(50); }
    throw new Error('bounded readiness/expiry timeout');
}
function child(command, args, options = {}) {
    const process = spawn(command, args, {stdio: ['pipe', 'ignore', 'ignore'], ...options});
    children.push(process);
    process.on('error', () => { process.failed = true; });
    return process;
}
async function stop(process) {
    if (!process || process.exitCode !== null) return;
    process.stdin.end();
    await until(() => process.exitCode !== null, 20000);
    assert.equal(process.exitCode, 0, 'backend graceful exit');
}
async function start(seed) {
    fs.rmSync(path.join(directory, 'ready'), {force: true});
    backend = child(path.join(process.env.JAVA_HOME, 'bin/java'), ['-cp',
        fs.readFileSync(process.env.ACCEPTANCE_CLASSPATH_FILE, 'utf8'),
        'com.qubigo.qubigobackend.users.adapters.repositories.SessionAcceptanceProcess',
        directory, seed ? 'seed' : 'resume', 'browser']);
    evidence.pids.push(backend.pid);
    await until(() => {
        assert(!backend.failed && backend.exitCode === null, 'backend exited before ready');
        return fs.existsSync(path.join(directory, 'ready'));
    });
    backendPort = Number(fs.readFileSync(path.join(directory, 'ready'), 'utf8'));
    await until(async () => {
        try { return (await fetch(`http://127.0.0.1:${backendPort}/api/games`)).status === 401; }
        catch { return false; }
    });
}
(async () => {
    // Local TLS only. Cookie flags are unchanged. Only the disposable certificate is trusted by the context.
    const cert = path.join(directory, 'cert.pem'), key = path.join(directory, 'key.pem');
    assert.equal(spawnSync('openssl', ['req', '-x509', '-newkey', 'rsa:2048', '-nodes',
        '-keyout', key, '-out', cert, '-days', '1', '-subj', '/CN=localhost',
        '-addext', 'subjectAltName=DNS:localhost,IP:127.0.0.1'], {stdio: 'ignore'}).status, 0);
    server = https.createServer({key: fs.readFileSync(key), cert: fs.readFileSync(cert)}, (req, res) => {
        const port = req.url.startsWith('/api/') ? backendPort : vitePort;
        if (!port) { res.writeHead(503).end(); return; }
        const upstream = http.request({hostname: '127.0.0.1', port, method: req.method,
            path: req.url, headers: {...req.headers, host: `127.0.0.1:${port}`,
                'x-forwarded-proto': 'https', 'x-forwarded-host': req.headers.host}}, reply => {
            res.writeHead(reply.statusCode, reply.headers); reply.pipe(res);
        });
        upstream.on('error', () => { if (!res.headersSent) res.writeHead(503); res.end(); });
        req.pipe(upstream);
    });
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    const origin = `https://localhost:${server.address().port}`;
    // Reserve an ephemeral loopback port for Vite; no change to frontend sources/env files.
    const reservation = http.createServer();
    await new Promise(resolve => reservation.listen(0, '127.0.0.1', resolve));
    vitePort = reservation.address().port;
    await new Promise(resolve => reservation.close(resolve));
    const vite = child(process.execPath, [path.join(root, 'qubigo-frontend/node_modules/vite/bin/vite.js'),
        '--host', '127.0.0.1', '--port', String(vitePort), '--strictPort'],
    {cwd: path.join(root, 'qubigo-frontend'), env: {...process.env, VITE_API_URL: origin}});
    await until(async () => {
        assert(!vite.failed && vite.exitCode === null, 'Vite failed');
        try { return (await fetch(`http://127.0.0.1:${vitePort}`)).ok; } catch { return false; }
    });
    await start(true);
    browser = await chromium.launch({headless: true});
    evidence.client = `Chromium ${browser.version()} (headless desktop)`;
    const context = await browser.newContext({ignoreHTTPSErrors: true});
    // Prevent accidental remote requests; this is network containment, not auth mocking.
    await context.route('**/*', route => {
        const url = new URL(route.request().url());
        return url.hostname === 'localhost' || url.hostname === '127.0.0.1'
            ? route.continue() : route.abort();
    });
    const page = await context.newPage();
    page.on('response', response => {
        const url = new URL(response.url());
        if (url.pathname.startsWith('/api/')) evidence.network.push({
            method: response.request().method(), path: url.pathname, status: response.status()});
    });
    await page.goto(`${origin}/#/signin`);
    await page.locator('#sign-in-username input').fill('rob@mail.org.io');
    await page.locator('#sign-in-password input').fill('disposable-test-password');
    await page.locator('#sign-in-button').click();
    await page.waitForURL('**/#/games');
    await page.getByText('No games yet', {exact: true}).waitFor();
    await until(() => evidence.network.some(r => r.path === '/api/games' && r.status === 200));
    const expiry = await page.evaluate(() => JSON.parse(atob(localStorage.getItem('authToken').split('.')[1])).exp * 1000);
    const cookie = (await context.cookies()).find(c => c.name === 'refreshToken');
    assert(cookie && cookie.secure && cookie.httpOnly && cookie.sameSite === 'None', 'real secure refresh cookie delivered');
    const originalRefresh = cookie.value; // In-memory only, never print.
    evidence.cookie = {secure: cookie.secure, httpOnly: cookie.httpOnly, sameSite: cookie.sameSite, path: cookie.path};
    evidence.accessExpiry = expiry;
    await stop(backend);
    assert(Date.now() < expiry, 'backend stopped before original access expiry');
    const stopped = Date.now();
    backendPort = undefined;
    await until(() => Date.now() >= expiry + 1000, 65000);
    await start(false);
    evidence.interruptionMs = Date.now() - stopped;
    const before = evidence.network.length;
    // Reload real My Games page; existing client automatically refreshes rejected access.
    await page.reload();
    await page.getByText('No games yet', {exact: true}).waitFor();
    await until(() => evidence.network.slice(before).some(r => r.path === '/api/games' && r.status === 200));
    const recovery = evidence.network.slice(before);
    const rejected = recovery.findIndex(r => r.path === '/api/games' && r.status === 401);
    const refreshed = recovery.findIndex((r, i) => i > rejected && r.path === '/api/refresh' && r.method === 'POST' && r.status === 200);
    const completed = recovery.findIndex((r, i) => i > refreshed && r.path === '/api/games' && r.status === 200);
    assert(rejected >= 0 && refreshed > rejected && completed > refreshed, 'expired My Games access rejected, automatically refreshed, then retried successfully');
    assert.equal((await context.cookies()).find(c => c.name === 'refreshToken').value, originalRefresh, 'refresh identity unchanged');
    assert.equal(evidence.network.filter(r => r.path === '/api/loginjwt').length, 1, 'one credential login only');
    assert.equal(await page.locator('#sign-in-button').count(), 0, 'no credential prompt');
    assert(page.url().endsWith('/#/games'), 'My Games route');
    evidence.result = 'PASS: expired access rejected; browser cookie automatically refreshed; My Games empty state rendered without credentials';
    evidence.refreshIdentityUnchanged = true;
    evidence.durationsMs = {access: 60000, refresh: 1209600000};
    await stop(backend);
    console.log(JSON.stringify(evidence, null, 2));
})().catch(() => {
    // No Playwright call logs, DOM dumps or response bodies on failure.
    console.error('Browser acceptance FAILED (diagnostics redacted); last stage network:', JSON.stringify(evidence));
    process.exitCode = 1;
}).finally(async () => {
    if (browser) await browser.close();
    for (const process of children) {
        if (process.exitCode === null && process.signalCode === null) {
            await new Promise(resolve => {
                process.once('exit', resolve);
                process.kill('SIGKILL');
            });
        }
    }
    if (server) await new Promise(resolve => server.close(resolve));
    fs.rmSync(directory, {recursive: true, force: true});
});
