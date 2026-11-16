'use strict';

const themes = {
    white: '纯白',
    'stone-mist': '岩白雾蓝',
    charcoal: '深色黑灰',
};
const boardNames = {
    'home-white': '纯白首页',
    'home-stone-mist': '岩白雾蓝首页',
    'home-charcoal': '深色黑灰首页',
    'home-record-cards': '记录卡片',
    'home-states': '首页关键状态',
    'home-responsive': '320 / 360 / 400dp',
    'home-font-scale-200': '200% 字体',
    lab: '交互检查',
};

const media = {
    camera: '../lithic-ui-v3-03-details/boards/assets/synthetic-camera.png',
    reader: '../lithic-ui-v3-02/boards/assets/reader.jpg',
    logo: '../../../app/src/main/res/drawable-nodpi/ic_launcher_scene_p3.png',
};
const date = '2026-11-13';
const projects = [
    { name: '合成横向相机', category: '摄影', status: '持有中', duration: '121 天', amount: '¥9.92', kind: 'project', cover: media.camera },
    { name: '合成阅读器', category: '数码', status: '持有中', duration: '60 天', amount: '¥3.32', kind: 'project', cover: media.reader },
];
const subscriptions = [
    { name: '合成阅读服务', category: '内容', status: '订阅中', amount: '¥2.48', kind: 'subscription' },
];
const stateNames = {
    normal: '正常有记录',
    empty: '空账本',
    'no-current': '无当前有效记录',
    'ledger-error': '读取失败 · 无快照',
    retained: '保留上次读取结果',
    'project-error': '项目预览读取失败',
    'subscription-error': '订阅预览读取失败',
    'project-retained': '保留上次项目预览',
    'subscription-retained': '保留上次订阅预览',
    'preview-loading': '预览首次读取',
    'preview-refreshing': '预览切换观察日',
    'cover-unavailable': '封面暂不可用',
    unconfirmed: '当前记录状态尚未确认',
    'retained-empty': '上次结果为空 · 当前未知',
};

const params = new URLSearchParams(location.search);
const board = document.getElementById('board');
const sheet = document.getElementById('demo-sheet');
const title = document.getElementById('demo-title');
const sheetContent = document.getElementById('demo-content');
const byId = new Map();
const icon = name => `<svg aria-hidden="true" focusable="false"><use href="#${name}"></use></svg>`;
let count = 0;

function heading(label, caption) {
    return `<header class="board-heading"><h1 id="board-title">${label}</h1><p class="board-caption">${caption}</p></header>`;
}

function iconButton(name, label, action, filled = false) {
    return `<button type="button" class="icon-button" aria-label="${label}" data-action="${action}">${filled ? `<span class="icon-surface">${icon(name)}</span>` : icon(name)}</button>`;
}

function action(label, name, arrow = false, extra = '') {
    return `<button type="button" class="text-action" data-action="${name}" ${extra}>${label}${arrow ? icon('right-icon') : ''}</button>`;
}

function money(value, description, id = '') {
    return `<span class="money" data-money aria-hidden="true">${value}</span><span class="a11y-only"${id ? ` id="${id}"` : ''}>${description}</span>`;
}

function moneyDescription(label, value, daily = false) {
    return value === '—' ? `${label}未取得` : `${label}，${value.slice(1)}元${daily ? '每天' : ''}`;
}

function newScreen(defaults = {}) {
    const state = defaults.state || 'normal';
    const forceSubscription = state === 'subscription-error' || state === 'subscription-retained';
    const model = {
        id: 'home-' + (++count),
        width: defaults.width || 360,
        height: defaults.height || 800,
        font: defaults.font || 1,
        theme: defaults.theme || 'white',
        state,
        tab: defaults.tab || (forceSubscription ? 'subscriptions' : 'projects'),
        longName: defaults.longName || false,
        scrollToRecords: defaults.scrollToRecords || false,
    };
    byId.set(model.id, model);
    return model;
}

function snapshot(state) {
    if (state === 'empty' || state === 'retained-empty') {
        return { gross: '—', daily: '—', projectCount: 0, subscriptionCount: 0, date };
    }
    if (state === 'ledger-error' || state === 'unconfirmed') {
        return { gross: '—', daily: '—', projectCount: '—', subscriptionCount: '—', date: null };
    }
    return {
        gross: '¥1,599.20', daily: state === 'no-current' ? '¥0.00' : '¥15.22',
        projectCount: 2, subscriptionCount: 1, date,
    };
}

function statusBar() {
    return `<div class="screen-status" aria-hidden="true"><span>09:41</span>${icon('system-icon')}</div>`;
}

function brandBar() {
    return `<header class="brandbar">
        <div class="brand"><img src="${media.logo}" alt=""><span class="brand-name"><strong>砺石</strong><small>LITHIC</small></span></div>
        <div class="brand-actions">${iconButton('search-icon', '搜索', 'search')}${iconButton('add-icon', '新增', 'add', true)}</div>
    </header>`;
}

function ledgerNotice(state) {
    const messages = {
        'no-current': ['暂无当前有效记录', false, false],
        'ledger-error': ['暂时无法取得成本数据', true, true],
        retained: ['上次取得的结果', true, true],
        unconfirmed: ['当前记录状态尚未确认', true, false],
        'retained-empty': ['上次取得的结果', true, true],
    };
    const entry = messages[state];
    if (!entry) return '';
    const [label, error, retry] = entry;
    return `<div class="notice-box" data-ledger-status="${state}">
        <p class="notice" ${error ? 'data-error' : ''}>${label}</p>
        ${state === 'retained-empty' ? '<p class="notice">当前记录状态尚未确认</p>' : ''}
        ${retry ? action('重试读取', 'retry-ledger') : ''}
    </div>`;
}

function countLabel(value, name, retained) {
    return `<span aria-hidden="true"><b>${value}</b> ${name}</span><span class="a11y-only">${value === '—'
        ? `${name}数量未取得`
        : `${retained ? '上次取得的' : ''}${name}数量 ${value}，包含历史记录`}</span>`;
}

function overview(model) {
    const values = snapshot(model.state);
    const retained = model.state === 'retained' || model.state === 'retained-empty';
    const dateContext = values.date ? `<span class="a11y-only">${retained ? '上次结果日期' : '统计日期'} ${values.date}</span>` : '';
    return `<section class="overview" aria-label="账本总览">
        <div class="metric-grid">
            <div class="metric"><h2 class="metric-heading">总投入</h2>
                ${money(values.gross, moneyDescription('总投入', values.gross))}
            </div>
            <div class="metric daily"><h2 class="metric-heading">日均成本${iconButton('info-icon', '查看数量与成本口径', 'explanation')}</h2>
                ${money(values.daily, moneyDescription('当前日均成本', values.daily, true))}
                ${dateContext}
            </div>
        </div>
        ${ledgerNotice(model.state)}
        <div class="overview-support">
            <p class="counts">
                ${countLabel(values.projectCount, '项目', retained)}
                <span aria-hidden="true"> · </span>
                ${countLabel(values.subscriptionCount, '订阅', retained)}
            </p>
            ${model.state === 'empty' ? '' : action('成本构成', 'composition', true)}
        </div>
    </section>`;
}

function recordCard(record, id, index) {
    const prefix = id + '-record-' + index;
    const isProject = record.kind === 'project';
    const kindLabel = isProject ? '日均' : '历史日均';
    const measure = isProject ? '项目日均成本' : '订阅历史平均日成本';
    const metadata = [record.category, record.status, record.duration].filter(Boolean);
    const photo = record.cover
        ? `<img src="${record.cover}" alt="">`
        : icon(isProject ? 'project-icon' : 'subscription-icon');
    const cover = record.unavailable
        ? `<div class="card-media cover-unavailable">${icon('unavailable-icon')}<span>封面暂不可用</span></div>`
        : `<div class="card-media" aria-hidden="true">${photo}</div>`;
    return `<button class="record-card" type="button" data-action="record" data-record-index="${index}"
        aria-labelledby="${prefix}-name" aria-describedby="${prefix}-meta ${prefix}-cost">
        ${cover}
        <div class="record-body">
            <h3 id="${prefix}-name">${record.name}</h3>
            <p class="record-meta" id="${prefix}-meta">${metadata.map(value => `<span>${value}</span>`).join(' ')}</p>
            <div class="record-cost">
                <p class="rate-label" aria-hidden="true">${kindLabel}</p>
                ${money(record.amount, moneyDescription(measure, record.amount, true), prefix + '-cost')}
            </div>
        </div>
    </button>`;
}

function stateMessage(model) {
    const isProject = model.tab === 'projects';
    const label = isProject ? '项目' : '订阅';
    const key = isProject ? 'project' : 'subscription';
    if (model.state === 'no-current') {
        return { title: isProject ? '暂无当前项目' : '暂无当前订阅', style: 'empty', showRecords: false };
    }
    if (model.state === 'ledger-error' || model.state === 'unconfirmed' || model.state === 'retained-empty') {
        return { title: null, style: 'none', showRecords: false };
    }
    if (model.state === 'preview-loading' || model.state === 'preview-refreshing') {
        return {
            title: model.state === 'preview-loading' ? `正在读取${label}预览` : `正在刷新${label}预览`,
            style: 'loading', showRecords: false,
        };
    }
    if (model.state === key + '-error' || model.state === key + '-retained') {
        const showingRetained = model.state.endsWith('-retained');
        return { title: `${label}预览暂时无法读取`, style: 'error', retry: `重试读取${label}预览`, showRecords: showingRetained };
    }
    return { title: null, style: 'ready', showRecords: true };
}

function previewRecords(model) {
    const current = model.tab === 'projects' ? projects : subscriptions;
    if (model.longName) {
        const name = model.tab === 'projects'
            ? '合成旅行摄影设备与备用镜头套装'
            : '合成跨设备有声书与电子阅读服务';
        return [{ ...current[0], name }, ...current.slice(1)];
    }
    if (model.state === 'cover-unavailable' && model.tab === 'projects') {
        return [{ ...current[0], unavailable: true }, ...current.slice(1)];
    }
    return current;
}

function browse(model) {
    if (model.state === 'empty') return '<p class="browse notice" data-preview-status="empty-ledger">暂无记录</p>';
    const isProject = model.tab === 'projects';
    const allLabel = isProject ? '查看全部长期项目' : '查看全部订阅服务';
    const choice = (label, tab) => {
        const active = tab === model.tab;
        return `<button type="button" role="tab" aria-selected="${active}" tabindex="${active ? 0 : -1}"
            aria-controls="${model.id}-records" id="${model.id}-${tab}-tab" data-preview-tab="${tab}">${label}</button>`;
    };
    const info = stateMessage(model);
    const statusBlock = info.title
        ? `<div class="preview-message" data-preview-status="${info.style}">
            <p class="notice" ${info.style === 'error' ? 'data-error' : ''}>${info.title}</p>
            ${info.retry && info.showRecords ? '<p class="notice">保留上次结果</p>' : ''}
            ${info.retry ? action(info.retry, isProject ? 'retry-project' : 'retry-subscription') : ''}
        </div>`
        : '';
    const records = info.showRecords ? previewRecords(model).slice(0, 2) : [];
    return `<section class="browse" aria-label="我的拥有">
        <header class="browse-heading"><h2>我的拥有</h2>
            <button type="button" class="text-action browse-all" data-action="all" aria-label="${allLabel}">全部${icon('right-icon')}</button>
        </header>
        <div class="record-tabs" role="tablist" aria-label="记录类型">
            ${choice('项目', 'projects')}${choice('订阅', 'subscriptions')}
        </div>
        <div id="${model.id}-records" role="tabpanel" aria-labelledby="${model.id}-${model.tab}-tab">
            ${statusBlock}
            ${records.length ? `<div class="records">${records.map((record, index) => recordCard(record, model.id, index)).join('')}</div>` : ''}
        </div>
    </section>`;
}

function navbar() {
    const tabs = [['首页', 'nav-home', true], ['回看', 'nav-review', false], ['设置', 'nav-settings', false]];
    return `<div class="nav-dock">
        <nav class="nav-capsule" data-nav-capsule role="tablist" aria-label="主导航">
            ${tabs.map(([label, name, selected]) => `<button type="button" role="tab" aria-label="${label}" aria-selected="${selected}"
                data-action="${name}"><span>${icon(name)}</span></button>`).join('')}
        </nav>
    </div>`;
}

function screen(configuration, caption = '') {
    const model = newScreen(configuration);
    return `<figure class="frame">
        ${caption ? `<figcaption>${caption}</figcaption>` : ''}
        <article class="screen" data-screen data-theme="${model.theme}" data-width="${model.width}" data-font="${model.font}"
            data-state="${model.state}" id="${model.id}" aria-label="${themes[model.theme]}首页"
            style="--screen-w: ${model.width}px; --screen-h: ${model.height}px">
            ${statusBar()}
            <div class="home-scroll" data-scroll>
                ${brandBar()}${overview(model)}<div data-browse>${browse(model)}</div>
            </div>
            <footer class="add-region" data-add-region>
                <button type="button" class="add-button" data-action="add">${icon('add-icon')}添加</button>
            </footer>
            ${navbar()}
            <div class="system-inset" data-system-inset aria-hidden="true"></div>
        </article>
    </figure>`;
}

function frames(items, columns) {
    return `<div class="frames" style="--columns: ${columns}">${items.join('')}</div>`;
}

function photoCredit() {
    return '<p class="board-note">图片为设计样本。阅读器摄影 Hexafluoride · <a href="https://commons.wikimedia.org/wiki/File:Kindle_4th_Generation.jpg">图片出处</a> · <a href="https://creativecommons.org/licenses/by-sa/4.0/">CC BY-SA 4.0</a> · 画板做了 4:3 裁切。</p>';
}

function buildBoard(name) {
    if (['home-white', 'home-stone-mist', 'home-charcoal'].includes(name)) {
        const theme = name.slice(5);
        return heading(`砺石 V4 · ${themes[theme]}`, '360dp · 100% 字体 · 同一组合成账本')
            + frames([screen({ theme })], 1) + photoCredit();
    }
    if (name === 'home-record-cards') {
        const variants = [
            ['项目 · 有封面', projects[0]],
            ['订阅 · 有封面', { ...subscriptions[0], cover: media.reader }],
            ['项目 · 无封面', { ...projects[0], cover: null }],
            ['订阅 · 无封面', subscriptions[0]],
        ];
        return heading('记录卡片', '4:3 封面。项目有持有天数，订阅显示历史日均。')
            + frames(variants.map(([label, record], i) => `<figure class="study" data-theme="white"><figcaption>${label}</figcaption>
                ${recordCard(record, 'study-' + i, 0)}</figure>`), 4)
            + photoCredit();
    }
    if (name === 'home-states') {
        const order = ['normal', 'empty', 'no-current', 'ledger-error', 'retained', 'project-error', 'subscription-error'];
        return heading('首页关键状态', '确认空账本、当期没有有效记录、读取失败与上次快照分别呈现。')
            + frames(order.map((state) => screen({ theme: 'white', width: 320, height: 730, state }, stateNames[state])), 4)
            + photoCredit();
    }
    if (name === 'home-responsive') {
        return heading('首页宽度对照', '100% 字体 · 双列卡片 · 添加、悬浮导航与系统区独立占位')
            + frames([320, 360, 400].map(width => screen({ width, theme: 'stone-mist' }, `${width}dp`)), 3)
            + photoCredit();
    }
    if (name === 'home-font-scale-200') {
        const examples = [
            [{ width: 320, font: 2, longName: true }, '320dp · 首屏金额'],
            [{ width: 360, font: 2, longName: true, scrollToRecords: true, theme: 'stone-mist' }, '360dp · 滚至项目'],
            [{ width: 400, font: 2, longName: true, scrollToRecords: true, theme: 'charcoal', tab: 'subscriptions' }, '400dp · 滚至订阅'],
        ];
        return heading('200% 字体', '浏览器模拟 · 金额纵排 · 卡片保留双列，长名称自然换行')
            + frames(examples.map(([model, caption]) => screen(model, caption)), 3) + photoCredit();
    }
    const width = [320, 360, 400].includes(Number(params.get('width'))) ? Number(params.get('width')) : 360;
    const font = params.get('font') === '2' ? 2 : 1;
    const theme = Object.hasOwn(themes, params.get('theme')) ? params.get('theme') : 'white';
    const state = Object.hasOwn(stateNames, params.get('state')) ? params.get('state') : 'normal';
    const tab = ['projects', 'subscriptions'].includes(params.get('tab')) ? params.get('tab') : undefined;
    return heading('砺石首页 · 交互样稿', `${themes[theme]} · ${width}dp · ${font * 100}% 字体 · ${stateNames[state]}`)
        + frames([screen({ width, font, theme, state, tab, longName: font === 2 })], 1)
        + photoCredit();
}

const requested = params.get('board') || 'lab';
const currentBoard = Object.hasOwn(boardNames, requested) ? requested : 'lab';
board.classList.toggle('single-phone-board', ['lab', 'home-white', 'home-stone-mist', 'home-charcoal'].includes(currentBoard));
document.getElementById('board-links').innerHTML = Object.entries(boardNames).map(([name, caption]) =>
    `<a href="?board=${name}"${name === currentBoard ? ' aria-current="page"' : ''}>${caption}</a>`).join('');
board.innerHTML = buildBoard(currentBoard);

function scrollStudy() {
    for (const model of byId.values()) {
        if (!model.scrollToRecords) continue;
        const phone = document.getElementById(model.id);
        const scroll = phone.querySelector('[data-scroll]');
        const browseStart = phone.querySelector('.browse-heading').getBoundingClientRect().top;
        scroll.scrollTop = browseStart - scroll.getBoundingClientRect().top + scroll.scrollTop;
    }
}

function showSheet(model, name, body) {
    sheet.dataset.theme = model?.theme || 'white';
    title.textContent = name;
    sheetContent.innerHTML = body;
    if (!sheet.open) sheet.showModal();
}

function switchSegment(phone, tab, setFocus = false) {
    const model = byId.get(phone.id);
    model.tab = tab;
    phone.querySelector('[data-browse]').innerHTML = browse(model);
    if (setFocus) phone.querySelector(`[data-preview-tab="${tab}"]`).focus({ preventScroll: true });
}

function showPreviewAction(trigger) {
    const phone = trigger.closest('[data-screen]');
    const model = phone ? byId.get(phone.id) : null;
    const press = trigger.dataset.action;
    const isProject = model?.tab !== 'subscriptions';
    if (press === 'search') {
        showSheet(model, '搜索', '<label for="demo-search">输入项目或订阅名称</label><input id="demo-search" type="search" autocomplete="off"><div data-search-result></div>');
    } else if (press === 'add') {
        showSheet(model, '添加', '<button class="sheet-row" data-demo="新增项目">新增项目</button><button class="sheet-row" data-demo="新增订阅">新增订阅</button>');
    } else if (press === 'record' && model) {
        const row = previewRecords(model)[Number(trigger.dataset.recordIndex)];
        showSheet(model, row.name, `<p>${row.category} · ${row.status}</p><p>${moneyDescription(isProject ? '项目日均成本' : '订阅历史平均日成本', row.amount, true)}</p>`);
    } else if (press === 'all') {
        const label = isProject ? '全部长期项目' : '全部订阅服务';
        showSheet(model, label, (isProject ? projects : subscriptions).map(item => `<p>${item.name}</p>`).join(''));
    } else if (press === 'composition' && model) {
        const values = snapshot(model.state);
        const projectRate = values.daily === '¥15.22' ? '¥13.24' : values.daily;
        const subscriptionRate = values.daily === '¥15.22' ? '¥1.98' : values.daily;
        showSheet(model, '成本构成', `<p>长期项目　${projectRate}</p><p>订阅服务　${subscriptionRate}</p><p>按当前日均成本归类。分项显示值四舍五入，汇总使用未舍入结果。</p>`);
    } else if (press === 'explanation' && model) {
        const values = snapshot(model.state);
        const dateLabel = ['retained', 'retained-empty'].includes(model.state) ? '上次结果日期' : '统计日期';
        showSheet(model, '数量与成本口径', `${values.date ? `<p>${dateLabel}　${values.date}</p>` : ''}
            <p>总投入累计长期项目投入，含已结束项目与非现金折抵。收回及订阅付款不计入总投入。</p>
            <p>当前日均成本覆盖有效记录，不代表今日扣款。项目日均与订阅历史平均使用各自口径。</p>
            <p>已录入数量包含历史记录。</p><button class="sheet-row" data-demo="完整计算说明">完整计算说明</button>`);
    } else if (press?.startsWith('retry-')) {
        showSheet(model, trigger.textContent.trim(), '<p>预览保留当前样本状态，正式入口由原读取回调处理。</p>');
    } else if (press === 'nav-review' || press === 'nav-settings') {
        showSheet(model, press === 'nav-review' ? '回看' : '设置', '<p>此画板只呈现首页。</p>');
    }
}

document.addEventListener('click', event => {
    const trigger = event.target.closest('button');
    if (!trigger) return;
    if (trigger.dataset.previewTab) {
        switchSegment(trigger.closest('[data-screen]'), trigger.dataset.previewTab, true);
    } else if (trigger.dataset.demo) {
        title.textContent = trigger.dataset.demo;
        sheetContent.innerHTML = '<p>此画板展示入口，使用合成内容。</p>';
    } else if (trigger.dataset.action) {
        showPreviewAction(trigger);
    }
});

document.addEventListener('keydown', event => {
    const tab = event.target.closest('[data-preview-tab]');
    if (!tab || !['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    event.preventDefault();
    const next = event.key === 'Home' ? 'projects' : event.key === 'End' ? 'subscriptions'
        : tab.dataset.previewTab === 'projects' ? 'subscriptions' : 'projects';
    switchSegment(tab.closest('[data-screen]'), next, true);
});

sheet.addEventListener('input', event => {
    if (event.target.id !== 'demo-search') return;
    const result = sheet.querySelector('[data-search-result]');
    result.replaceChildren();
    const query = event.target.value.trim();
    if (!query) return;
    for (const record of projects.concat(subscriptions).filter(item => item.name.includes(query))) {
        const row = document.createElement('p');
        row.textContent = record.name;
        result.appendChild(row);
    }
});

window.addEventListener('load', scrollStudy);
window.lithicHomePreview = { scrollStudy };
