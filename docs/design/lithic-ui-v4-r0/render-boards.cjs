const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const { pathToFileURL } = require('node:url');

const boards = ['review-themes', 'cost-analysis', 'long-term-states', 'project-themes', 'list-editor', 'settings-adaptive'];
const widths = [320, 360, 400];
const scales = [1, 2];

async function checkFrames(page) {
  await page.evaluate(() => Promise.all([...document.images].map(image => image.decode())));
  return page.locator('.phone').evaluateAll(phones => phones.map(phone => {
    const scroller = phone.querySelector('.scroll');
    const footer = phone.querySelector('.dock,.footer');
    const rect = scroller.getBoundingClientRect();
    const outOfBounds = scroller.scrollWidth > scroller.clientWidth + 1;
    const overlapsFooter = footer && rect.bottom > footer.getBoundingClientRect().top + 1;
    scroller.scrollTop = scroller.scrollHeight;
    const reachesEnd = scroller.scrollTop + scroller.clientHeight >= scroller.scrollHeight - 2;
    scroller.scrollTop = 0;
    const clipped = [...scroller.querySelectorAll('.row,.panel,.field,.amount,.top')].some(node => {
      const bounds = node.getBoundingClientRect();
      return bounds.left < rect.left - 1 || bounds.right > rect.right + 1;
    });
    return { screen: phone.dataset.screen, theme: phone.dataset.theme, outOfBounds, overlapsFooter: !!overlapsFooter, reachesEnd, clipped };
  }));
}

async function main() {
  const [modulePath, chromePath] = process.argv.slice(2);
  assert(modulePath && chromePath, 'Pass the local Playwright module and Chrome executable paths.');
  const { chromium } = require(path.resolve(modulePath));
  const browser = await chromium.launch({ executablePath: chromePath, headless: true });
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1050 }, deviceScaleFactor: 2 });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    const preview = pathToFileURL(path.join(__dirname, 'preview.html'));
    async function open(board, width, font) {
      preview.search = new URLSearchParams({ board, ...(width ? {width, font} : {}) }).toString();
      await page.goto(preview.href);
      await page.evaluate(() => document.fonts.ready);
    }
    let framesChecked = 0;
    const boardReceipts = [];
    for (const board of boards) {
      for (const width of widths) for (const font of scales) {
        await open(board, width, font);
        const frames = await checkFrames(page);
        assert(frames.length > 0, board);
        for (const frame of frames) {
          assert(!frame.outOfBounds && !frame.clipped && !frame.overlapsFooter && frame.reachesEnd,
            `${board}, ${width}px, ${font}×: ${JSON.stringify(frame)}`);
        }
        assert.equal(await page.locator('[data-single] [data-chart]').count(), 0, 'A singleton has no trend canvas');
        assert.equal(await page.locator('[data-variant="empty"] [data-chart]').count(), 0, 'An empty ledger has no trend canvas');
        framesChecked += frames.length;
      }
      await open(board);
      await checkFrames(page);
      const output = `${board}.png`;
      const bytes = await page.locator('#board').screenshot({path:path.join(__dirname,output), animations:'disabled'});
      boardReceipts.push({ name: output, pixels: [bytes.readUInt32BE(16), bytes.readUInt32BE(20)], sha256: createHash('sha256').update(bytes).digest('hex') });
    }
    assert.deepEqual(errors, []);
    const receipt = { scope: 'BROWSER_DESIGN_ONLY', browser: browser.version(), android: 'NOT_RUN', widths, scales, framesChecked, browserErrors: errors.length, boards: boardReceipts };
    await fs.writeFile(path.join(__dirname, 'render-verification.json'), `${JSON.stringify(receipt,null,2)}\n`);
    console.log(JSON.stringify(receipt,null,2));
  } finally {
    await browser.close();
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
