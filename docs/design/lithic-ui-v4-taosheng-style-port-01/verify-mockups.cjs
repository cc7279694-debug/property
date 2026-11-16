const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const { pathToFileURL } = require('node:url');

async function run() {
    const [modulePath, browserPath] = process.argv.slice(2);
    assert(modulePath && browserPath, 'Provide the playwright-core module path and a Chromium executable');
    const { chromium } = require(path.resolve(modulePath));
    const browser = await chromium.launch({ executablePath: browserPath, headless: true });
    try {
        const page = await browser.newPage({ viewport: { width: 1520, height: 1100 }, deviceScaleFactor: 2 });
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        page.on('response', response => { if (response.status() >= 400) errors.push(response.url()); });
        await page.goto(pathToFileURL(path.join(__dirname, 'mockups.html')).href);
        await page.evaluate(() => document.fonts.ready);
        await page.locator('.screen img').evaluateAll(images => Promise.all(images.map(image => image.decode())));
        assert.equal(await page.locator('.screen').count(), 5);
        const contrast = await page.locator('#home-screens .screen').evaluateAll(elements => elements.map(screen => {
            const color = getComputedStyle(screen);
            const luminance = variable => {
                const hex = color.getPropertyValue(variable).trim().slice(1);
                const rgb = hex.length === 3 ? [...hex].map(char => char + char) : hex.match(/../g);
                const [r, g, b] = rgb.map(channel => {
                    const number = parseInt(channel, 16) / 255;
                    return number <= 0.04045 ? number / 12.92 : ((number + 0.055) / 1.055) ** 2.4;
                });
                return 0.2126 * r + 0.7152 * g + 0.0722 * b;
            };
            const pairs = [
                ['--text', '--bg'], ['--sub', '--bg'], ['--sub', '--surface'],
                ['--sub', '--fill'], ['--sub', '--cost'], ['--sub', '--nav-surface'],
                ['--selected-text', '--select'], ['--on-primary', '--primary'],
            ];
            const ratios = pairs.map(([foreground, background]) => {
                const values = [luminance(foreground), luminance(background)];
                return (Math.max(...values) + 0.05) / (Math.min(...values) + 0.05);
            });
            return { theme: screen.dataset.theme, minimum: Number(Math.min(...ratios).toFixed(2)), pairs: pairs.length };
        }));
        for (const item of contrast) assert(item.minimum >= 4.5, item.theme + ' color token contrast');
        const results = [];
        for (const width of [320, 360, 390, 400]) {
            for (const scale of [1, 2]) {
                await page.evaluate(({ width, scale }) => {
                    document.documentElement.style.setProperty('--screen-w', width + 'px');
                    document.documentElement.style.setProperty('--font-scale', scale);
                    document.body.classList.toggle('large-layout', scale >= 2);
                }, { width, scale });
                const screens = await page.locator('.screen').evaluateAll(elements => elements.map(screen => {
                    const inside = screen.getBoundingClientRect();
                    const scroll = screen.querySelector('.screen-scroll');
                    const footer = screen.querySelector('.screen-footer').getBoundingClientRect();
                    const nav = screen.querySelector('.nav-dock')?.getBoundingClientRect();
                    const problems = [];
                    if (scroll.scrollWidth > scroll.clientWidth + 1) problems.push('horizontal scroll');
                    if (scroll.getBoundingClientRect().bottom > footer.top + 1) problems.push('footer overlaps content');
                    if (nav && footer.bottom > nav.top + 1) problems.push('nav overlaps primary action');
                    const cover = screen.querySelector('.photo-stage')?.getBoundingClientRect();
                    if (cover) {
                        const identity = screen.querySelector('.detail-identity').getBoundingClientRect();
                        if (cover.bottom > identity.top - 1) problems.push('cover overlaps identity');
                        if (Math.abs(cover.width / cover.height - 4 / 3) > 0.02) problems.push('cover frame is not 4:3');
                        if (inside.width === 390 && !document.body.classList.contains('large-layout')) {
                            const lastBlock = scroll.lastElementChild.getBoundingClientRect();
                            if (lastBlock.bottom > scroll.getBoundingClientRect().bottom) problems.push('last disclosure is clipped in the review frame');
                        }
                    }
                    for (const button of screen.querySelectorAll('.pill-nav button, .icon-btn, .primary, .segment button, .all-records')) {
                        const box = button.getBoundingClientRect();
                        if (box.width < 48 || box.height < 48) problems.push('small touch target: ' + button.textContent);
                        if (box.right > inside.right + 1 || box.left < inside.left - 1) problems.push('button outside frame: ' + button.textContent);
                    }
                    for (const node of screen.querySelectorAll('.number, .meta, .event b, .record-info h3, .record-bottom, .detail-name, .pill-nav button')) {
                        if (!node.getClientRects().length) continue;
                        const box = node.getBoundingClientRect();
                        const range = document.createRange();
                        range.selectNodeContents(node);
                        for (const line of range.getClientRects()) {
                            if (line.width && (line.left < inside.left - 1 || line.right > inside.right + 1)) problems.push('text outside frame: ' + node.textContent);
                            if (line.width && (line.left < box.left - 1 || line.right > box.right + 1)) problems.push('text outside its element: ' + node.textContent);
                        }
                    }
                    return { id: screen.id, width: inside.width, problems };
                }));
                for (const item of screens) {
                    assert.equal(item.width, width, item.id + ' frame width');
                    assert.deepEqual(item.problems, [], item.id + ' @ ' + width + ' / ' + scale);
                }
                results.push({ width, scale, frames: screens.length });
            }
        }
        await page.evaluate(() => {
            document.documentElement.style.setProperty('--screen-w', '390px');
            document.documentElement.style.setProperty('--font-scale', 1);
            document.body.classList.remove('large-layout');
        });
        const home = page.locator('#home-white');
        const tab = home.getByRole('tab', { name: '项目', exact: true });
        await tab.focus();
        await tab.press('ArrowRight');
        assert.equal(await home.getByRole('tab', { name: '订阅', exact: true }).getAttribute('aria-selected'), 'true');
        assert(await home.getByRole('button', { name: '查看全部订阅服务' }).isVisible());
        assert(await home.getByText('合成阅读服务').isVisible());
        await home.getByRole('tab', { name: '项目', exact: true }).click();
        assert(await home.getByRole('button', { name: '查看全部长期项目' }).isVisible());
        for (const trigger of [home.getByRole('button', { name: '添加记录' }), home.getByRole('button', { name: '添加', exact: true })]) {
            await trigger.click();
            assert(await page.getByRole('dialog').getByRole('button', { name: '新增项目' }).isVisible());
            assert(await page.getByRole('dialog').getByRole('button', { name: '新增订阅' }).isVisible());
            await page.getByRole('dialog').getByRole('button', { name: '关闭' }).click();
        }
        const detail = page.locator('#project-detail');
        assert.equal(await detail.locator('details[open]').count(), 0);
        await detail.getByText('财务明细', { exact: true }).click();
        assert(await detail.locator('details[open] .disclosure-line').filter({ hasText: '净投入' }).isVisible());
        await detail.getByText('财务明细', { exact: true }).click();
        const subscription = page.locator('#subscription-detail');
        assert.equal(await subscription.locator('details[open]').count(), 0);
        await subscription.getByText('更多资料', { exact: true }).click();
        assert(await subscription.locator('details[open] .disclosure-line').filter({ hasText: '累计实际付款' }).isVisible());
        await subscription.getByText('更多资料', { exact: true }).click();
        for (const id of ['#money-history', '#payment-history']) {
            const list = page.locator(id);
            assert.equal(await list.locator('.event:visible').count(), 3);
            const toggle = page.locator('[aria-controls="' + id.slice(1) + '"]');
            await toggle.click();
            assert.equal(await list.locator('.event:visible').count(), 4);
            await toggle.click();
            assert.equal(await list.locator('.event:visible').count(), 3);
        }
        assert(await subscription.locator('#payment-history').getByText('尚未生效', { exact: true }).isVisible());
        const plans = subscription.locator('.plan');
        await plans.nth(1).click();
        assert(await page.getByRole('dialog').getByText('已付款，尚未生效').isVisible());
        await page.getByRole('dialog').getByRole('button', { name: '关闭' }).click();
        const nav = page.locator('#nav-specs .nav-sample').first();
        await nav.getByRole('tab', { name: '回看' }).click();
        assert.equal(await nav.getByRole('tab', { name: '回看' }).getAttribute('aria-selected'), 'true');
        await nav.getByRole('tab', { name: '现在' }).click();
        assert.deepEqual(errors, []);
        await page.evaluate(() => {
            document.querySelectorAll('.screen-scroll').forEach(scroller => { scroller.scrollTop = 0; });
            document.activeElement.blur();
        });
        const boards = ['home-themes', 'detail-pair', 'bottom-navigation'];
        if (process.env.LITHIC_EXPORT_BOARDS === '1') {
            for (const id of boards) {
                await page.locator('#' + id).screenshot({ path: path.join(__dirname, id + '.png'), animations: 'disabled' });
                const file = await fs.stat(path.join(__dirname, id + '.png'));
                assert(file.size > 0);
            }
        }
        console.log(JSON.stringify({ browser: browser.version(), layouts: results, contrast, browserErrors: errors.length, interactions: ['record tabs and contextual list', 'top and bottom add buttons', 'financial disclosure and subscription facts', 'two inline histories', 'paid next-plan summary', 'navigation selection'] }, null, 2));
    } finally {
        await browser.close();
    }
}

run().catch(error => { console.error(error); process.exitCode = 1; });
