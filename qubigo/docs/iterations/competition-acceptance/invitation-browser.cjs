/* eslint-env node */
// One-off local acceptance. Requires Java 21, generated main classpath and external Playwright.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {spawn, execFileSync} = require('node:child_process');
const {pathToFileURL} = require('node:url');
const {chromium} = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const root = path.resolve(__dirname, '../../..');
const scratch = fs.mkdtempSync(path.join(root, '.factory/competition-invitation-'));
const java = process.env.JAVA_HOME;
const classpath = fs.readFileSync(process.env.ACCEPTANCE_CLASSPATH_FILE, 'utf8').trim();
const password = 'Ab12345678'; // synthetic disposable accounts only
const base = 'http://localhost:5196';
let backend, vite, browser;

async function post(url, body, token) {
    const response = await fetch(`http://localhost:8096${url}`, {
        method: 'POST', headers: {'Content-Type': 'application/json', ...(token ? {Authorization: `Bearer ${token}`} : {})},
        body: JSON.stringify(body)
    });
    assert.ok(response.ok, `${url}: HTTP ${response.status}`);
    return response.json();
}
async function login(page) {
    await page.locator('#sign-in-username input').fill('entrant@example.test');
    await page.locator('#sign-in-password input').fill(password);
    await page.locator('#sign-in-button').click();
}
async function main() {
    execFileSync(path.join(java, 'bin/javac'), ['-cp', classpath, '-d', scratch, path.join(__dirname, 'InvitationBackend.java')]);
    const ready = path.join(scratch, 'ready');
    const email = path.join(scratch, 'email');
    const log = fs.openSync(path.join(scratch, 'backend.log'), 'w');
    const startup = new Promise((resolve, reject) => {
        const timer = setTimeout(() => { watcher.close(); reject(Error('Backend startup timed out; inspect disposable backend.log')); }, 60000);
        const watcher = fs.watch(scratch, () => {
            if (fs.existsSync(ready)) { clearTimeout(timer); watcher.close(); resolve(); }
        });
        backend = spawn(path.join(java, 'bin/java'), ['-Dspring.devtools.restart.enabled=false', '-cp', `${scratch}:${classpath}`, 'InvitationBackend',
            '--server.port=8096', `--spring.datasource.url=jdbc:sqlite:${scratch}/competition.db?date_class=TEXT`,
            '--qubigo.jwt.key=YWNjZXB0YW5jZS1vbmx5LXN5bnRoZXRpYy1rZXktMzJieXRlcw==',
            '--qubigo.remember-me.key=acceptance-only', '--qubigo.cookie.options=samesite=lax;httponly',
            '--qubigo.messaging.smtp-host=localhost', '--qubigo.messaging.smtp-port=1'], {
            cwd: root, env: {...process.env, ACCEPTANCE_READY_FILE: ready, ACCEPTANCE_EMAIL_FILE: email}, stdio: ['ignore', log, log]
        });
        backend.once('exit', code => { clearTimeout(timer); watcher.close(); reject(Error(`Backend exited ${code}; inspect disposable backend.log`)); });
    });
    await startup;
    const auth = await post('/api/loginjwt', {username: 'organizer@example.test', password});
    const draft = await post('/api/admin/competitions', {name: 'Browser evening', closesAt: '2099-11-07T15:00:00Z', gameAllowance: 4, minimumParticipants: 2}, auth.authToken);
    await post(`/api/admin/competitions/${draft.id}/publish`, {}, auth.authToken);
    process.env.VITE_API_URL = '';
    const {createServer} = await import(pathToFileURL(path.join(root, 'qubigo-frontend/node_modules/vite/dist/node/index.js')));
    vite = await createServer({root: path.join(root, 'qubigo-frontend'), server: {port: 5196, strictPort: true, proxy: {'/api': {target: 'http://localhost:8096', configure: proxy => proxy.on('proxyReq', request => request.removeHeader('origin'))}}}});
    await vite.listen();
    browser = await chromium.launch({headless: true, args: ['--no-sandbox']});
    const context = await browser.newContext();
    const page = await context.newPage();
    page.on('pageerror', error => console.error(`Browser error: ${error.message}`));
    page.on('response', response => {
        if (response.url().includes('/api/') && !response.ok()) console.error(`HTTP ${response.status()}: ${new URL(response.url()).pathname}`);
    });
    let joins = 0;
    page.on('request', request => { if (request.method() === 'POST' && request.url().endsWith('/join')) joins++; });
    const invitation = `${base}/#/competitions/${draft.id}`;
    await page.goto(invitation);
    await page.getByRole('heading', {name: 'Browser evening'}).waitFor();
    assert.equal(joins, 0);
    assert.equal(await page.getByRole('link', {name: 'Qubigo tutorial'}).getAttribute('href'), '#/demo');
    await page.getByRole('link', {name: 'register', exact: true}).click();
    await page.locator('#registration-name input').fill('Browser entrant');
    await page.locator('#registration-email input').fill('entrant@example.test');
    await page.locator('#registration-password input').fill(password);
    await page.locator('#confirm-password input').fill(password);
    await page.locator('#registration-button').click();
    await page.getByRole('heading', {name: 'Check your email'}).waitFor();
    await page.goto(`${base}/#/signin`);
    await login(page);
    await page.locator('#sign-in-verification-reminder:not([hidden])').waitFor();
    assert.equal(joins, 0);
    const mail = fs.readFileSync(email, 'utf8');
    const verification = mail.match(/http[^\s]+\/#\/confirm-registration\/[^\s]+/)[0];
    assert.ok(verification.startsWith(base), 'Captured email must return to the same local origin');
    // Separate tab models opening the actual captured verification email.
    const verificationTab = await page.context().newPage();
    await verificationTab.goto(verification);
    await verificationTab.getByRole('button', {name: 'Sign me in'}).waitFor();
    await page.reload();
    await login(page);
    await page.getByRole('heading', {name: 'Browser evening'}).waitFor();
    assert.equal(joins, 0, 'Authentication must not enroll');
    await page.reload();
    await page.getByRole('heading', {name: 'Browser evening'}).waitFor();
    assert.equal(joins, 0, 'Reload must not enroll');
    await page.getByRole('button', {name: 'Join competition', exact: true}).click();
    await page.getByRole('status').filter({hasText: 'Joined.'}).waitFor();
    assert.equal(joins, 1);
    assert.equal(await page.evaluate(() => localStorage.getItem('intended-competition')), null);
    console.log(JSON.stringify({passed: true, registration: 'real backend', verification: 'captured email in separate tab', signIn: 'retry after verification and reload', enrollment: 'one explicit join only', nativeAssociation: 'not tested'}));
}
main().catch(error => { console.error(error.message); process.exitCode = 1; }).finally(async () => {
    if (browser) await browser.close();
    if (vite) await vite.close();
    if (backend && backend.exitCode === null) {
        await new Promise(resolve => { backend.once('exit', resolve); backend.kill(); });
    }
    // Retain logs only after failure, never publish email contents or auth material.
    if (!process.exitCode) fs.rmSync(scratch, {recursive: true, force: true});
});
