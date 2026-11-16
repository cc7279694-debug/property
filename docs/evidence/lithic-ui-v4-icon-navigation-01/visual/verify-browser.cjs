const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const { pathToFileURL } = require('node:url');

const out = __dirname;
const [modulePath, browserPath] = process.argv.slice(2);
assert(modulePath && browserPath, 'Provide playwright-core module path and Chromium executable');
const { chromium } = require(path.resolve(modulePath));

async function run() {
    const browser = await chromium.launch({ executablePath: browserPath, headless: true });
    try {
        const page = await browser.newPage({
            viewport: { width: 1330, height: 1050 },
            deviceScaleFactor: 2,
            reducedMotion: 'reduce',
        });
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        await page.goto(pathToFileURL(path.join(out, 'browser.html')).href);
        await page.evaluate(() => document.fonts.ready);
        for (const key of ['home', 'review', 'settings']) {
            const xml = await fs.readFile(path.resolve(out, '../../../../app/src/main/res/drawable/ic_primary_nav_' + key + '.xml'), 'utf8');
            const androidData = xml.match(/android:pathData="([^"]+)"/)?.[1];
            assert(androidData, 'Missing VectorDrawable path for ' + key);
            assert.match(xml, /android:strokeWidth="1\.65"/);
            assert.match(xml, /android:viewportWidth="24"/);
            assert.match(xml, /android:viewportHeight="24"/);
            const paths = await page.locator('svg[data-glyph="' + key + '"] path').evaluateAll(elements => elements.map(element => element.getAttribute('d')));
            assert(paths.length > 0 && paths.every(value => value === androidData), 'Glyph differs from ' + key + ' VectorDrawable');
        }
        const comparison = page.locator('#comparison');
        assert.equal(await comparison.locator('.bar.old').count(), 3);
        assert.equal(await comparison.locator('.bar.new').count(), 3);
        const tabs = comparison.locator('.bar.new').first();
        assert.deepEqual(
            await tabs.getByRole('tab').evaluateAll(items => items.map(item => item.getAttribute('aria-label'))),
            ['首页', '回看', '设置']
        );
        assert.equal(await tabs.getByRole('tab', { name: '首页', selected: true }).count(), 1);
        await tabs.getByRole('tab', { name: '首页' }).press('ArrowRight');
        assert.equal(await tabs.getByRole('tab', { name: '回看', selected: true }).count(), 1);
        await tabs.getByRole('tab', { name: '设置' }).click();
        assert.equal(await tabs.getByRole('tab', { name: '设置', selected: true }).count(), 1);
        await tabs.getByRole('tab', { name: '首页' }).click();
        assert.equal(await tabs.getByRole('tab', { name: '首页', selected: true }).count(), 1);
        await page.evaluate(() => document.activeElement.blur());

        await page.evaluate(() => window.createNavigationMatrix());
        const results = await page.locator('#matrix-test .screen').evaluateAll(screens => {
            const rect = element => element.getBoundingClientRect();
            const close = (value, expected) => Math.abs(value - expected) <= 0.05;
            const luminance = css => {
                const [r, g, b] = css.match(/\d+/g).slice(0, 3).map(value => {
                    const n = Number(value) / 255;
                    return n <= 0.04045 ? n / 12.92 : Math.pow((n + 0.055) / 1.055, 2.4);
                });
                return 0.2126 * r + 0.7152 * g + 0.0722 * b;
            };
            const contrast = (first, second) => {
                const a = luminance(first);
                const b = luminance(second);
                return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
            };
            return screens.map(screen => {
                const { theme, width, scale, mode } = screen.dataset;
                const windowBox = rect(screen);
                const addBox = rect(screen.querySelector('.add'));
                const anchorBox = rect(screen.querySelector('.anchor'));
                const safeBox = rect(screen.querySelector('.safe'));
                const bar = screen.querySelector('.bar.new');
                const barBox = rect(bar);
                const tabNodes = [...bar.querySelectorAll('.tab')];
                const tabBoxes = tabNodes.map(rect);
                const selected = bar.querySelector('[aria-selected=true]');
                const selection = selected.querySelector('.inset-circle');
                const selectedBox = rect(selection);
                const inactive = bar.querySelector('[aria-selected=false]');
                const checks = {
                    logicalWidth: close(windowBox.width, +width),
                    capsuleSize: close(barBox.width, 210) && close(barBox.height, 58),
                    centered: close(barBox.left + barBox.width / 2, windowBox.left + windowBox.width / 2),
                    spacing: close(barBox.top - anchorBox.top, 4) && close(safeBox.top - barBox.bottom, 14),
                    safeArea: close(safeBox.height, mode === 'gesture' ? 24 : 48),
                    addSeparate: addBox.bottom < barBox.top && addBox.height >= 48,
                    targets: tabBoxes.length === 3 && tabBoxes.every(box => box.width >= 48 && box.height >= 48 && close(box.width, tabBoxes[0].width)),
                    selection: close(selectedBox.width, 40) && close(selectedBox.height, 40) &&
                        getComputedStyle(selection).backgroundColor !== 'rgba(0, 0, 0, 0)' &&
                        getComputedStyle(inactive.querySelector('.inset-circle')).backgroundColor === 'rgba(0, 0, 0, 0)',
                    iconOnly: tabNodes.every(tab => {
                        const bounds = rect(tab.querySelector('svg'));
                        return close(bounds.width, 24) && close(bounds.height, 24) && tab.textContent.trim() === '';
                    }),
                    reducedMotion: getComputedStyle(selection).transitionDuration === '0s',
                    fontSimulation: close(parseFloat(getComputedStyle(screen.querySelector('.add')).fontSize), 15 * +scale),
                };
                return {
                    theme,
                    width: +width,
                    fontScale: +scale,
                    mode,
                    checks,
                    selectedContrast: contrast(getComputedStyle(selected).color, getComputedStyle(selection).backgroundColor),
                    inactiveContrast: contrast(getComputedStyle(inactive).color, getComputedStyle(bar).backgroundColor),
                };
            });
        });
        assert.equal(results.length, 36);
        for (const sample of results) {
            const failures = Object.keys(sample.checks).filter(key => !sample.checks[key]);
            assert.deepEqual(failures, [], JSON.stringify(sample));
            assert(sample.selectedContrast >= 4.5);
            assert(sample.inactiveContrast >= 4.5);
        }
        assert.deepEqual(errors, []);
        await page.evaluate(() => document.querySelector('#matrix-test').classList.remove('enabled'));
        for (const [selector, name] of [['#comparison', 'new-old-three-themes.png'], ['#responsive', 'width-font-insets.png']]) {
            await page.locator(selector).screenshot({ path: path.join(out, name), animations: 'disabled' });
        }

        const report = {
            status: 'BROWSER_PASS',
            androidRuntime: 'NOT_RUN_BY_THIS_SCRIPT',
            chrome: browser.version(),
            samples: results.length,
            scale: 'CSS sample; native font scaling not exercised',
            insets: 'illustrative 24px and 48px system navigation areas; device insets not exercised',
            glyphs: 'VectorDrawable path data and viewport compared against repository XML',
            semantics: 'browser role, accessible names, selected attribute, keyboard and click; TalkBack not exercised',
            selectedContrastMin: Number(Math.min(...results.map(item => item.selectedContrast)).toFixed(2)),
            inactiveContrastMin: Number(Math.min(...results.map(item => item.inactiveContrast)).toFixed(2)),
            perTheme: Object.fromEntries(['white', 'mist', 'dark'].map(theme => {
                const item = results.find(result => result.theme === theme);
                return [theme, { selected: +item.selectedContrast.toFixed(2), inactive: +item.inactiveContrast.toFixed(2) }];
            })),
            browserErrors: errors,
        };
        await fs.writeFile(path.join(out, 'browser-results.json'), JSON.stringify(report, null, 2) + '\n', 'utf8');
        console.log(JSON.stringify(report, null, 2));
    } finally {
        await browser.close();
    }
}
run().catch(error => { console.error(error); process.exitCode = 1; });
