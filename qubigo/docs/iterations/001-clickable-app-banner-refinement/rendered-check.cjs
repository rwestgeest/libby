// Run from repository root after starting Vite on 127.0.0.1:5173.
// Tooling is temporary, outside the frontend dependency tree.
const {chromium} = require('../../../.factory/browser-tools/node_modules/playwright');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const path = require('node:path');
const out = __dirname;
(async () => {
    const browser = await chromium.launch({args: ['--no-sandbox']});
    const results = {browser: browser.version(), environment: 'Linux headless Chromium; iOS UA/viewport emulation, not Safari or physical devices', cases: []};
    try {
        for (const [name, width, height, insets, ua] of [
            ['narrow-portrait', 320, 568, {top: 0, left: 0, right: 0, bottom: 0}, 'iPhone'],
            ['notched-portrait', 390, 844, {top: 47, left: 0, right: 0, bottom: 34}, 'iPhone'],
            ['phone-landscape', 844, 390, {top: 0, left: 47, right: 47, bottom: 21}, 'iPhone'],
            ['ipad-portrait', 768, 1024, {top: 24, left: 0, right: 0, bottom: 20}, 'Macintosh'],
            ['ipad-landscape', 1024, 768, {top: 24, left: 0, right: 0, bottom: 20}, 'Macintosh']
        ]) {
            const context = await browser.newContext({viewport: {width, height}, userAgent: `Mozilla/5.0 (${ua}) AppleWebKit/605.1.15`, hasTouch: true});
            const page = await context.newPage();
            const cdp = await context.newCDPSession(page);
            // Chromium's actual env(safe-area-inset-*) values, not CSS replacement.
            await cdp.send('Emulation.setSafeAreaInsetsOverride', {insets});
            await page.goto('http://127.0.0.1:5173/#/signin');
            const link = page.locator('app-banner a');
            const dismiss = page.locator('app-banner button');
            await link.waitFor();
            await page.evaluate(() => document.fonts.ready);
            await page.screenshot({path: path.join(out, `${name}.png`)});
            const geometry = await link.evaluate(a => {
                const b = a.parentElement.querySelector('button');
                const container = a.parentElement;
                const rect = e => {const r = e.getBoundingClientRect(); return {x:r.x,y:r.y,width:r.width,height:r.height,right:r.right,bottom:r.bottom};};
                const style = getComputedStyle(a);
                const textRange = document.createRange(); textRange.selectNodeContents(a);
                return {link: rect(a), dismiss: rect(b), banner: rect(container), text: rect({getBoundingClientRect: () => textRange.getBoundingClientRect()}), padding: {top: getComputedStyle(container).paddingTop,left:getComputedStyle(container).paddingLeft,right:getComputedStyle(container).paddingRight}, color:style.color,background:style.backgroundColor,font:style.font,textDecoration:style.textDecorationLine};
            });
            assert(geometry.link.height >= 44 && geometry.dismiss.height >= 44 && geometry.dismiss.width >= 44);
            assert(geometry.dismiss.x - geometry.link.right >= 8);
            assert(geometry.link.x >= insets.left + 4 && geometry.dismiss.right <= width - insets.right - 4);
            assert(geometry.link.y >= insets.top + 4);
            assert(geometry.text.x >= geometry.link.x && geometry.text.right <= geometry.link.right);
            assert(geometry.text.y >= geometry.link.y && geometry.text.bottom <= geometry.link.bottom);
            // Reset focus and use real Tab events through the open shadow root.
            await page.evaluate(() => {document.activeElement?.blur(); document.querySelector('app-banner').shadowRoot.activeElement?.blur();});
            await page.keyboard.press('Tab');
            const focus = await link.evaluate(a => ({active:a.getRootNode().activeElement === a, visible:a.matches(':focus-visible'),outline:getComputedStyle(a).outline,offset:getComputedStyle(a).outlineOffset}));
            assert(focus.active && focus.visible && focus.outline.includes('2px'));
            await page.screenshot({path: path.join(out, `${name}-link-focus.png`)});
            await page.keyboard.press('Tab');
            assert(await dismiss.evaluate(b => b.getRootNode().activeElement === b && b.matches(':focus-visible')));
            await page.screenshot({path: path.join(out, `${name}-dismiss-focus.png`)});
            await page.keyboard.press('Enter');
            await assert.doesNotReject(() => link.waitFor({state:'detached'}));
            assert.equal(page.url(), 'http://127.0.0.1:5173/#/signin');
            assert.equal(await page.evaluate(() => sessionStorage.getItem('app-banner-dismissed')), 'true');
            await page.reload();
            assert.equal(await link.count(), 0);
            results.cases.push({name,viewport:{width,height},simulatedSafeArea:insets,geometry,focus,dismissal:'Enter hides without navigation and persists across reload',result:'PASS'});
            await context.close();
        }
        fs.writeFileSync(path.join(out, 'rendered-results.json'), JSON.stringify(results, null, 2) + '\n');
        console.log(JSON.stringify(results, null, 2));
    } finally {await browser.close();}
})().catch(error => {console.error(error); process.exitCode = 1;});
