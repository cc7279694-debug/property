const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const { pathToFileURL } = require('node:url');

const boards = [
    'home-white',
    'home-stone-mist',
    'home-charcoal',
    'home-record-cards',
    'home-states',
    'home-responsive',
    'home-font-scale-200',
];
const themes = ['white', 'stone-mist', 'charcoal'];
const tabs = ['projects', 'subscriptions'];
const widths = [320, 360, 400];
const fonts = [1, 2];
const lab = { board: 'lab', width: 360, font: 1, theme: 'white', state: 'normal', tab: 'projects' };

async function load(page, parameters) {
    const url = pathToFileURL(path.join(__dirname, 'prototype.html'));
    url.search = new URLSearchParams(parameters).toString();
    await page.goto(url.href);
    await page.locator('#board').waitFor({ state: 'visible' });
    await page.evaluate(() => document.fonts.ready);
    await page.locator('#board img').evaluateAll(images =>
        Promise.all(images.map(async image => {
            await image.decode();
            if (!image.naturalWidth) throw new Error('Empty image: ' + image.currentSrc);
        }))
    );
}

async function measure(screen) {
    return screen.evaluate(element => {
        const issues = [];
        const rect = element.getBoundingClientRect();
        const visible = node => {
            const box = node.getBoundingClientRect();
            const style = getComputedStyle(node);
            return box.width > 1 && box.height > 1 && style.visibility !== 'hidden' && style.display !== 'none';
        };
        const readRect = selector => {
            const node = element.querySelector(selector);
            if (!node || !visible(node)) {
                issues.push('Missing visible region ' + selector);
                return null;
            }
            return node.getBoundingClientRect();
        };
        const viewport = readRect('[data-scroll]');
        const add = readRect('[data-add-region]');
        const nav = readRect('[data-nav-capsule]');
        const inset = readRect('[data-system-inset]');
        if (viewport && add && viewport.bottom > add.top + 1) issues.push('Scroll viewport overlaps Add');
        if (add && nav && add.bottom > nav.top + 1) issues.push('Add overlaps navigation');
        if (nav && inset && nav.bottom > inset.top + 1) issues.push('Navigation overlaps system inset');
        if (nav && Math.abs((nav.left + nav.right) / 2 - (rect.left + rect.right) / 2) > 1) {
            issues.push('Navigation is off center');
        }
        if (nav && (Math.abs(nav.width - 210) > 1 || Math.abs(nav.height - 58) > 1)) {
            issues.push('Frozen navigation is not 210×58px');
        }
        const navTabs = [...element.querySelectorAll('[data-nav-capsule] button')];
        const navNames = navTabs.map(button => button.getAttribute('aria-label'));
        if (navNames.join('/') !== '首页/回看/设置' || navTabs.some(button => button.getAttribute('role') !== 'tab')) {
            issues.push('Frozen navigation needs three accessible, ordered tabs');
        }
        if (navTabs.some(button => button.textContent.trim()) ||
            navTabs.filter(button => button.getAttribute('aria-selected') === 'true').length !== 1 ||
            navTabs[0]?.getAttribute('aria-selected') !== 'true') {
            issues.push('Home navigation should be icon-only with Home selected');
        }
        for (const button of navTabs) {
            const icon = button.querySelector('svg')?.getBoundingClientRect();
            if (!icon || Math.abs(icon.width - 24) > 1 || Math.abs(icon.height - 24) > 1) {
                issues.push('Navigation glyph is not 24×24px');
            }
            const holder = button.querySelector('span');
            const circle = holder?.getBoundingClientRect();
            if (!circle || Math.abs(circle.width - 40) > 1 || Math.abs(circle.height - 40) > 1 ||
                getComputedStyle(holder).borderRadius !== '50%') {
                issues.push('Navigation selection area is not a 40px circle');
            }
        }
        const scroller = element.querySelector('[data-scroll]');
        if (scroller && scroller.scrollWidth > scroller.clientWidth + 1) issues.push('Horizontal scrolling');
        const inspectText = node => {
            const box = node.getBoundingClientRect();
            const range = document.createRange();
            range.selectNodeContents(node);
            if (getComputedStyle(node).display !== 'inline' &&
                (node.scrollWidth > node.clientWidth + 1 || node.scrollHeight > node.clientHeight + 1)) {
                issues.push('Text is clipped by its box: ' + node.textContent.trim());
            }
            for (const line of range.getClientRects()) {
                if (!line.width) continue;
                if (line.left < rect.left - 1 || line.right > rect.right + 1) {
                    issues.push('Text extends beyond the screen: ' + node.textContent.trim());
                }
                if (getComputedStyle(node).display !== 'inline' &&
                    (line.left < box.left - 1 || line.right > box.right + 1) &&
                    getComputedStyle(node).overflowX !== 'visible') {
                    issues.push('Text extends beyond a clipping boundary: ' + node.textContent.trim());
                }
            }
        };
        const money = [...element.querySelectorAll('[data-money]')].filter(visible);
        const cards = [...element.querySelectorAll('.record-card')].filter(visible);
        for (const node of new Set([
            ...money,
            ...element.querySelectorAll('h1,h2,h3,.record-card p,.browse-all,[role=tab]'),
        ])) {
            if (visible(node) && node.textContent.trim()) inspectText(node);
        }
        if (!money.length) issues.push('No visible amounts to verify');
        for (const photo of element.querySelectorAll('.card-media')) {
            if (!visible(photo)) continue;
            const box = photo.getBoundingClientRect();
            if (Math.abs(box.width / box.height - 4 / 3) > 0.04) issues.push('Cover is not 4:3');
        }
        for (const button of element.querySelectorAll('button')) {
            if (!visible(button)) continue;
            const box = button.getBoundingClientRect();
            if (box.width < 47.5 || box.height < 47.5) {
                issues.push('Touch target below 48px: ' + (button.getAttribute('aria-label') || button.textContent.trim()));
            }
        }
        const positions = cards.map(card => card.getBoundingClientRect());
        if (positions.length > 1 && Math.abs(positions[0].top - positions[1].top) <= 1 &&
            positions[0].right > positions[1].left + 1) issues.push('Adjacent record cards overlap');
        let scrollBranch = 'fits';
        if (scroller && positions.length && scroller.scrollHeight > scroller.clientHeight + 1) {
            const initial = scroller.scrollTop;
            scroller.scrollTop = scroller.scrollHeight;
            scrollBranch = 'scroll verified';
            const last = cards.at(-1).getBoundingClientRect();
            const end = scroller.getBoundingClientRect();
            if (scroller.scrollTop <= 0 || last.bottom > end.bottom + 1 || last.bottom <= end.top) {
                issues.push('Last card cannot be reached by scrolling');
            }
            if (add && last.bottom > add.top + 1) issues.push('Last record overlaps Add when scrolled to end');
            scroller.scrollTop = initial;
        }
        return {
            width: rect.width,
            background: getComputedStyle(element).backgroundColor,
            issues: [...new Set(issues)],
            money: money.map(node => node.textContent.trim()),
            moneySizes: money.map(node => parseFloat(getComputedStyle(node).fontSize)),
            covers: [...element.querySelectorAll('.card-media img')].filter(visible).map(image => image.currentSrc),
            cards: cards.length,
            grid: positions.length > 1 ? (Math.abs(positions[0].top - positions[1].top) <= 1 ? 'two columns' : 'stacked') : 'single record',
            scrollBranch,
        };
    });
}

async function verifyLayouts(page) {
    const cases = [];
    const reference = new Map();
    const backgrounds = new Map();
    for (const width of widths) {
        for (const theme of themes) {
            for (const tab of tabs) {
                let originalFontSizes;
                for (const font of fonts) {
                    await load(page, { ...lab, width, theme, tab, font });
                    const screen = page.locator('#board [data-screen]');
                    assert.equal(await screen.count(), 1, 'Lab renders exactly one screen');
                    const result = await measure(screen);
                    const context = `${width}px / ${theme} / ${tab} / ${font * 100}%`;
                    assert(Math.abs(result.width - width) <= 1, context + ' wrong width');
                    assert.deepEqual(result.issues, [], context);
                    assert.equal(result.cards, tab === 'projects' ? 2 : 1, context + ' wrong record count');
                    if (tab === 'projects') assert.equal(result.grid, 'two columns', context);
                    assert(result.money.some(amount => amount.includes('1,599.20')), context + ' missing exact total investment');
                    assert(result.money.some(amount => amount.includes('15.22')), context + ' missing exact daily cost');
                    if (font === 1) {
                        originalFontSizes = result.moneySizes;
                        if (tab === 'projects') {
                            assert.equal(result.covers.length, 2, context + ' missing authorized fixture cover');
                            backgrounds.set(theme, result.background);
                        }
                        const comparisonKey = `${width}/${tab}`;
                        const sameLedger = [result.money, result.covers];
                        if (reference.has(comparisonKey)) {
                            assert.deepEqual(sameLedger, reference.get(comparisonKey), context + ' changed theme fixture');
                        } else {
                            reference.set(comparisonKey, sameLedger);
                        }
                    } else {
                        assert.equal(result.moneySizes.length, originalFontSizes.length, context + ' dropped amount');
                        for (const [index, size] of result.moneySizes.entries()) {
                            assert(size >= originalFontSizes[index] * 1.95, context + ' amount text was not doubled');
                        }
                        const name = tab === 'projects' ? '合成旅行摄影设备与备用镜头套装' : '合成跨设备有声书与电子阅读服务';
                        const expectedTitle = screen.getByText(name, { exact: true });
                        assert(await expectedTitle.isVisible(), context + ' incomplete long title');
                        assert(await expectedTitle.evaluate(title => title.clientHeight > parseFloat(getComputedStyle(title).lineHeight) + 1),
                            context + ' long title did not wrap');
                    }
                    cases.push({ width, theme, tab, font, cards: result.cards, grid: result.grid, scroll: result.scrollBranch });
                }
            }
        }
    }
    assert.equal(new Set(backgrounds.values()).size, themes.length, 'Three separately styled theme surfaces must render');
    return cases;
}

async function verifyInteractions(page) {
    await load(page, lab);
    const home = page.locator('#board [data-screen]');
    const dialog = page.getByRole('dialog');
    const tab = (name) => home.getByRole('tab', { name, exact: true });
    assert.equal(await tab('项目').getAttribute('aria-selected'), 'true');
    assert.equal(await home.locator('.record-card:visible').count(), 2);
    await tab('订阅').click();
    assert.equal(await tab('订阅').getAttribute('aria-selected'), 'true');
    assert.equal(await home.locator('.record-card:visible').count(), 1);
    await home.getByRole('button', { name: '查看全部订阅服务' }).click();
    await dialog.getByRole('heading', { name: '全部订阅服务' }).waitFor();
    await dismiss(dialog, page);
    await home.getByRole('button', { name: '合成阅读服务', exact: true }).click();
    await dialog.getByRole('heading', { name: '合成阅读服务' }).waitFor();
    assert(await dialog.getByText('订阅历史平均日成本，2.48元每天').isVisible());
    await dismiss(dialog, page);
    await tab('项目').click();
    assert.equal(await tab('项目').getAttribute('aria-selected'), 'true');
    await home.getByRole('button', { name: '查看全部长期项目' }).click();
    await dialog.getByRole('heading', { name: '全部长期项目' }).waitFor();
    await dismiss(dialog, page);
    await home.getByRole('button', { name: '合成横向相机', exact: true }).click();
    await dialog.getByRole('heading', { name: '合成横向相机' }).waitFor();
    assert(await dialog.getByText('项目日均成本，9.92元每天').isVisible());
    await dismiss(dialog, page);
    for (const trigger of [
        home.getByRole('button', { name: /^(?:新增|新增记录)$/ }),
        home.getByRole('button', { name: '添加', exact: true }),
    ]) {
        await trigger.click();
        await dialog.waitFor({ state: 'visible' });
        assert(await dialog.getByRole('button', { name: '新增项目' }).isVisible());
        assert(await dialog.getByRole('button', { name: '新增订阅' }).isVisible());
        await dismiss(dialog, page);
    }
    await home.getByRole('button', { name: /搜索/ }).click();
    await dialog.waitFor({ state: 'visible' });
    await dialog.getByRole('searchbox').fill('相机');
    assert(await dialog.getByText('合成横向相机', { exact: true }).isVisible());
    await dismiss(dialog, page);
    await home.getByRole('button', { name: /成本构成/ }).click();
    await dialog.waitFor({ state: 'visible' });
    assert(await dialog.getByText(/长期项目\s*¥13\.24/).isVisible());
    assert(await dialog.getByText(/订阅服务\s*¥1\.98/).isVisible());
    await dismiss(dialog, page);
    await home.getByRole('button', { name: '查看数量与成本口径' }).click();
    await dialog.getByRole('heading', { name: '数量与成本口径' }).waitFor();
    assert(await dialog.getByText(/统计日期\s*2026-11-13/).isVisible());
    await dismiss(dialog, page);
    return [
        '项目和订阅分段及对应全部入口',
        '记录卡片入口',
        '顶部新增和底部添加共享菜单',
        '搜索入口',
        '成本构成入口与口径说明',
    ];
}

async function dismiss(dialog, page) {
    const close = dialog.getByRole('button', { name: '关闭', exact: true });
    if (await close.isVisible()) await close.click();
    else await page.keyboard.press('Escape');
    await dialog.waitFor({ state: 'hidden' });
}

async function verifyStates(page) {
    const checks = [];
    for (const [state, tab, message, retry] of [
        ['normal', 'projects', null, null],
        ['empty', 'projects', '暂无记录', null],
        ['no-current', 'projects', /暂无当前有效记录|当前无生效项目或订阅|暂无当前项目/, null],
        ['ledger-error', 'projects', '暂时无法取得成本数据', '重试读取'],
        ['retained', 'projects', '上次取得的结果', '重试读取'],
        ['project-error', 'projects', null, '重试读取项目预览'],
        ['subscription-error', 'subscriptions', null, '重试读取订阅预览'],
    ]) {
        await load(page, { ...lab, state, tab });
        const home = page.locator('#board [data-screen]');
        const geometry = await measure(home);
        assert.deepEqual(geometry.issues, [], state + ' layout');
        if (message) assert(await home.getByText(message, { exact: typeof message === 'string' }).first().isVisible(), state);
        if (retry) assert(await home.getByRole('button', { name: retry, exact: true }).isVisible(), state);
        const wrongRetry = tab === 'projects' ? '重试读取订阅预览' : '重试读取项目预览';
        assert.equal(await home.getByRole('button', { name: wrongRetry, exact: true }).count(), 0, state);
        const counts = (await home.locator('.counts b').allTextContents()).map(value => value.trim());
        if (state === 'empty') {
            assert.equal(await home.getByRole('tab', { name: /^(项目|订阅)$/ }).count(), 0, 'Empty ledger has no record tabs');
            assert.equal(await home.getByRole('button', { name: /全部|成本构成/ }).count(), 0, 'Empty ledger has no list or composition entry');
            assert.deepEqual(counts, ['0', '0']);
            assert.deepEqual(geometry.money, ['—', '—'], 'Empty ledger keeps both amounts out of the totals');
        }
        if (state === 'no-current') {
            assert.deepEqual(counts, ['2', '1'], 'No active records retains historical counts');
            assert.deepEqual(geometry.money, ['¥1,599.20', '¥0.00'], 'Kept investment and confirmed zero daily rate');
        }
        if (state === 'ledger-error') {
            assert.deepEqual(counts, ['—', '—']);
            assert.deepEqual(geometry.money, ['—', '—']);
            assert.equal(await home.getByText(/统计日期|上次结果日期/).count(), 0, 'Unknown ledger has no observation date');
        }
        if (state === 'retained') {
            assert.deepEqual(geometry.money.slice(0, 2), ['¥1,599.20', '¥15.22']);
            assert.deepEqual(counts, ['2', '1']);
            assert.equal(await home.getByText('上次结果日期 2026-11-13', { exact: true }).count(), 1);
            await home.getByRole('button', { name: '查看数量与成本口径' }).click();
            const dialog = page.getByRole('dialog');
            assert(await dialog.getByText(/上次结果日期\s*2026-11-13/).isVisible());
            await dismiss(dialog, page);
        }
        if (state === 'project-error' || state === 'subscription-error') {
            assert.deepEqual(geometry.money, ['¥1,599.20', '¥15.22'], 'Preview failure preserves overview');
            assert.equal(geometry.cards, 0);
            assert.equal(await home.getByRole('button', { name: '重试读取', exact: true }).count(), 0);
            await home.getByRole('tab', { name: tab === 'projects' ? '订阅' : '项目', exact: true }).click();
            assert.equal(await home.getByRole('button', { name: retry, exact: true }).count(), 0);
            assert.equal(await home.locator('.record-card:visible').count(), tab === 'projects' ? 1 : 2);
        }
        checks.push({ state, tab, cards: geometry.cards, amounts: geometry.money, counts });
    }
    return checks;
}

async function verifyBoards(page, exportBoards) {
    const result = [];
    for (const name of boards) {
        await load(page, { board: name });
        const board = page.locator('#board');
        const dimensions = await board.boundingBox();
        assert(dimensions && dimensions.width > 200 && dimensions.height > 200, name + ' missing content');
        const screens = await board.locator('[data-screen]').all();
        for (const [index, screen] of screens.entries()) {
            const layout = await measure(screen);
            assert.deepEqual(layout.issues, [], `${name} screen ${index + 1}`);
        }
        if (name === 'home-record-cards') {
            const variants = await board.locator('.study .card-media').evaluateAll(covers =>
                covers.map(cover => ({
                    image: Boolean(cover.querySelector('img')),
                    ratio: cover.clientWidth / cover.clientHeight,
                }))
            );
            assert.deepEqual(variants.map(cover => cover.image), [true, true, false, false]);
            assert(variants.every(cover => Math.abs(cover.ratio - 4 / 3) < 0.04), 'Card study covers must maintain 4:3');
        }
        const item = { name, width: Math.round(dimensions.width), height: Math.round(dimensions.height), screens: screens.length };
        if (exportBoards) {
            const output = path.join(__dirname, name + '.png');
            const bytes = await board.screenshot({ path: output, animations: 'disabled' });
            assert.equal(bytes.readUInt32BE(0), 0x89504e47, name + ' does not contain a PNG');
            item.pixels = [bytes.readUInt32BE(16), bytes.readUInt32BE(20)];
            item.bytes = (await fs.stat(output)).size;
            item.sha256 = createHash('sha256').update(bytes).digest('hex');
        }
        result.push(item);
    }
    return result;
}

async function run() {
    const [modulePath, browserPath] = process.argv.slice(2);
    assert(modulePath && browserPath, 'Provide the playwright-core module path and a Chromium executable');
    const moduleDirectory = path.resolve(modulePath);
    const { chromium } = require(moduleDirectory);
    const playwrightVersion = require(path.join(moduleDirectory, 'package.json')).version;
    const browser = await chromium.launch({ executablePath: browserPath, headless: true });
    try {
        const page = await browser.newPage({ viewport: { width: 1760, height: 1100 }, deviceScaleFactor: 2, reducedMotion: 'reduce' });
        page.setDefaultTimeout(8_000);
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        page.on('requestfailed', request => errors.push(request.url() + ': ' + request.failure()?.errorText));
        const layouts = await verifyLayouts(page);
        const interactions = await verifyInteractions(page);
        const states = await verifyStates(page);
        const exports = await verifyBoards(page, process.env.LITHIC_EXPORT_BOARDS === '1');
        assert.deepEqual(errors, [], 'Unexpected browser or asset errors');
        console.log(JSON.stringify({
            scope: 'BROWSER_DESIGN_VERIFICATION_ONLY',
            android: 'NOT_RUN',
            browser: browser.version(),
            playwright: playwrightVersion,
            layoutCaseCount: layouts.length,
            stateCaseCount: states.length,
            layouts,
            interactions,
            states,
            exports,
            browserErrors: errors.length,
        }, null, 2));
    } finally {
        await browser.close();
    }
}

run().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
