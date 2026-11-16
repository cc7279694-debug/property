'use strict';

const themes = [
    { id: 'white', label: '纯白' },
    { id: 'mist', label: '岩白雾蓝' },
    { id: 'dark', label: '深色黑灰' },
];
const camera = '../lithic-ui-v3-03-details/boards/assets/synthetic-camera.png';
const reader = '../lithic-ui-v3-02/boards/assets/reader.jpg';
const mark = '../../../app/src/main/res/drawable-nodpi/ic_launcher_scene_p3.png';
const currentDate = '2026-11-13';
const tabs = ['现在', '回看', '设置'];
const icon = name => `<svg aria-hidden="true"><use href="#${name}"></use></svg>`;
const iconButton = (name, title, action, filled = false) =>
    `<button type="button" class="icon-btn${filled ? ' square-visual' : ''}" aria-label="${title}" data-action="${action}">${filled ? `<span>${icon(name)}</span>` : icon(name)}</button>`;

const projects = [
    { name: '合成横向相机', type: '摄影', state: '持有中', days: '121 天', price: '9.92', media: camera, action: 'project' },
    { name: '合成阅读器', type: '数码', state: '持有中', days: '60 天', price: '3.32', media: reader, action: 'reader' },
];
const subscriptions = [
    { name: '合成阅读服务', type: '内容', state: '订阅中', days: '', price: '2.48', action: 'subscription' },
];
const moneyEvents = [
    { date: '07-16', title: '初始投入', amount: '¥900.00', detail: '2026-07-16 · 初始投入 · ¥900.00' },
    { date: '07-21', title: '补充投入', amount: '¥100.00', detail: '2026-07-21 · 补充投入 · ¥100.00' },
    { date: '08-01', title: '配件投入', amount: '¥400.00', detail: '2026-08-01 · 配件投入 · ¥400.00' },
    { date: '10-26', title: '收回', amount: '¥200.00', detail: '2026-10-26 · 收回 · ¥200.00' },
];
const payments = [
    { date: '11-10', title: '订阅付款', status: '尚未生效', amount: '¥89.40', detail: '2026-11-10 实际付款 · 2026-11-26 生效 · ¥89.40' },
    { date: '10-26', title: '订阅付款', amount: '¥59.40', detail: '2026-10-26 实际付款并生效 · ¥59.40' },
    { date: '09-26', title: '订阅付款', amount: '¥74.40', detail: '2026-09-26 实际付款并生效 · ¥74.40' },
    { date: '08-26', title: '订阅付款', amount: '¥74.40', detail: '2026-08-26 实际付款并生效 · ¥74.40' },
];

function statusBar() {
    return `<div class="status" aria-hidden="true"><span>09:41</span>${icon('system')}</div>`;
}

function nav(selected = 0) {
    return `<div class="nav-dock"><nav class="pill-nav" role="tablist" aria-label="主导航">
        ${tabs.map((tab, index) => `<button type="button" role="tab" aria-selected="${index === selected}" tabindex="${index === selected ? 0 : -1}">${tab}</button>`).join('')}
    </nav></div>`;
}

function actionButton(label, action) {
    return `<button type="button" class="primary" data-action="${action}">${icon('add')}${label}</button>`;
}

function link(label, action, hideArrow = false) {
    return `<button type="button" class="link" data-action="${action}">${label}${hideArrow ? '' : icon('right')}</button>`;
}

function card(record) {
    const metric = record.days ? '项目日均成本' : '历史平均日成本';
    const cover = record.media ? `<img src="${record.media}" alt="">`
        : `<span class="record-art" aria-hidden="true">${icon('subscription')}</span>`;
    return `<button type="button" class="surface record" data-action="${record.action}">${cover}
        <div class="record-info">
            <h3>${record.name}</h3><p class="meta">${record.type} · ${record.state}</p>
            <div class="record-bottom">${record.days ? `<span>${record.days}</span>` : ''}
                <b aria-label="${metric} ${record.price} 元每天">¥${record.price}<span class="unit" aria-hidden="true">/天</span></b>
            </div>
        </div>
    </button>`;
}

function home(theme) {
    const id = 'home-' + theme.id;
    return `<figure class="frame">
        <figcaption>${theme.label}</figcaption>
        <article class="screen" data-theme="${theme.id}" id="${id}" aria-label="${theme.label}首页">
            ${statusBar()}
            <header class="screen-header">
                <div class="brand"><img src="${mark}" alt=""><div class="brand-text"><span class="brand-name">砺石</span><span class="brand-latin">LITHIC</span></div></div>
                <div class="header-actions">${iconButton('search', '搜索', 'search')}${iconButton('add', '添加记录', 'add', true)}</div>
            </header>
            <div class="screen-scroll home-content">
                <section class="surface overview" aria-label="成本总览">
                    <div class="overview-grid">
                        <div class="metric"><p class="metric-name">总投入</p><p class="number">¥1,599.20</p></div>
                        <div class="metric home-rate">
                            <p class="metric-name">日均成本${iconButton('info', '成本计算说明', 'overview-info')}</p>
                            <p class="number" aria-label="当前日均成本 15.22 元每天">¥15.22<span class="unit" aria-hidden="true">/天</span></p>
                            <time class="metric-date" datetime="${currentDate}">${currentDate}</time>
                        </div>
                    </div>
                    <div class="overview-summary">
                        <span aria-label="已录入 2 个项目及 1 项订阅，包含历史"><b>2</b> 项目　<b>1</b> 订阅</span>${link('成本构成', 'composition')}
                    </div>
                </section>
                <section class="collection" aria-label="记录预览">
                    <div class="collection-header">
                        <div class="segment" role="tablist" aria-label="记录类型">
                            <button type="button" role="tab" id="${id}-project-tab" aria-selected="true" aria-controls="${id}-project-panel">项目</button>
                            <button type="button" role="tab" id="${id}-subscription-tab" aria-selected="false" aria-controls="${id}-subscription-panel" tabindex="-1">订阅</button>
                        </div>
                        <button type="button" class="link all-records" data-action="all-projects" aria-label="查看全部长期项目">全部${icon('right')}</button>
                    </div>
                    <div class="records" id="${id}-project-panel" role="tabpanel" aria-labelledby="${id}-project-tab">${projects.map(card).join('')}</div>
                    <div class="records" id="${id}-subscription-panel" role="tabpanel" aria-labelledby="${id}-subscription-tab" hidden>${subscriptions.map(card).join('')}</div>
                </section>
            </div>
            <footer class="screen-footer">${actionButton('添加', 'add')}</footer>
            ${nav()}<div class="gesture" aria-hidden="true"></div>
        </article>
    </figure>`;
}

function history(events, subscription = false) {
    const id = subscription ? 'payment-history' : 'money-history';
    const hiddenIndex = subscription ? events.length - 1 : 0;
    return `<section class="history" aria-labelledby="${id}-title">
        <header class="section-title">
            <span><h3 id="${id}-title">${subscription ? '付款历史' : '金额记录'}</h3><span class="meta">${events.length} 笔</span></span>
            <button type="button" class="link history-toggle" aria-expanded="false" aria-controls="${id}">全部</button>
        </header>
        <div class="surface history-list" id="${id}">
            ${events.map((item, index) => `<button type="button" class="event${index === hiddenIndex ? ' extra-event' : ''}" data-event="${item.detail}" ${index === hiddenIndex ? 'hidden' : ''}>
                <span class="meta">${item.date}</span><span>${item.title}${item.status ? `<small>${item.status}</small>` : ''}</span><b>${item.amount}</b>
            </button>`).join('')}
        </div>
    </section>`;
}

function metric(label, value, daily = false, action = '') {
    return `<div class="cost-tile">
        <p class="metric-name">${label}${action ? iconButton('info', label + '说明', action) : ''}</p>
        <p class="number" aria-label="${label} ${value} 元${daily ? '每天' : ''}">¥${value}</p>
    </div>`;
}

function detailHead(kind) {
    return `<header class="screen-header detail-head">${iconButton('back', '返回', 'back')}${iconButton('more', '更多' + kind + '操作', kind === '项目' ? 'project-more' : 'subscription-more')}</header>`;
}

function projectDetail() {
    return `<figure class="frame"><figcaption>项目详情 · 纯白</figcaption>
        <article class="screen" id="project-detail" data-theme="white" aria-label="项目详情">
            ${statusBar()}${detailHead('项目')}
            <div class="screen-scroll detail-content">
                <button class="photo-stage" type="button" data-action="cover" aria-label="查看完整相机封面"><img src="${camera}" alt=""></button>
                <div class="detail-identity">
                    <h3 class="detail-name">合成横向相机</h3><p class="meta">摄影 · 持有中 · 121 天</p>
                    <div class="photo-actions">${link('更换照片', 'replace-photo', true)}${link('取消封面显示', 'remove-photo', true)}</div>
                </div>
                <section class="cost-pair" aria-label="项目成本">
                    ${metric('日均成本', '9.92', true)}${metric('累计投入', '1,400.00')}
                </section>
                <details class="surface disclosure">
                    <summary>财务明细${icon('down')}</summary>
                    <div class="disclosure-body">
                        <p class="disclosure-line">累计投入<b>¥1,400.00</b></p>
                        <p class="disclosure-line">累计收回<b>¥200.00</b></p>
                        <p class="disclosure-line">净投入<b>¥1,200.00</b></p>
                    </div>
                </details>
                ${history(moneyEvents)}
                <details class="surface disclosure">
                    <summary>更多资料${icon('down')}</summary>
                    <div class="disclosure-body">${link('成本口径', 'project-info', true)}${link('成本历史', 'cost-history', true)}</div>
                </details>
            </div>
            <footer class="screen-footer">${actionButton('记一笔', 'record')}</footer>
            <div class="gesture" aria-hidden="true"></div>
        </article></figure>`;
}

function plan(title, subtitle, symbol, action) {
    return `<button type="button" class="surface plan" data-action="${action}">${icon(symbol)}
        <div><strong>${title}</strong><p class="meta">${subtitle}</p></div>${icon('right')}</button>`;
}

function subscriptionDetail() {
    return `<figure class="frame"><figcaption>订阅详情 · 纯白</figcaption>
        <article class="screen" id="subscription-detail" data-theme="white" aria-label="订阅详情">
            ${statusBar()}${detailHead('订阅')}
            <div class="screen-scroll detail-content subscription">
                <div class="service-identity"><span class="service-icon" aria-hidden="true">${icon('subscription')}</span>
                    <div><h3 class="detail-name">合成阅读服务</h3><p class="meta">内容</p></div><span class="state">订阅中</span>
                </div>
                ${metric('历史平均日成本', '2.48', true, 'subscription-info')}
                ${plan('当前套餐', '月付 · ¥59.40', 'subscription', 'current-plan')}
                ${plan('下一期', '已付款 · 11月26日生效', 'calendar', 'next-plan')}
                ${history(payments, true)}
                <details class="surface disclosure">
                    <summary>更多资料${icon('down')}</summary>
                    <div class="disclosure-body">
                        <p class="disclosure-line">累计实际付款<b>¥297.60</b></p>
                        <p class="disclosure-line">折算付费时长<b>120 天</b></p>
                        <p class="disclosure-line">已过去的付费有效天数<b>80 天</b></p>
                    </div>
                </details>
            </div>
            <footer class="screen-footer">${actionButton('记录续费', 'renew')}</footer>
            <div class="gesture" aria-hidden="true"></div>
        </article></figure>`;
}

document.querySelector('#home-screens').innerHTML = themes.map(home).join('');
document.querySelector('#detail-screens').innerHTML = projectDetail() + subscriptionDetail();
document.querySelector('#nav-specs').innerHTML = themes.map((theme, index) =>
    `<article class="nav-sample" data-theme="${theme.id}"><h3>${theme.label}</h3>${nav(index)}<div class="gesture" aria-hidden="true"></div></article>`).join('');

function selectTab(button) {
    const group = button.parentElement;
    for (const tab of group.querySelectorAll('[role=tab]')) {
        const selected = tab === button;
        tab.setAttribute('aria-selected', selected);
        tab.tabIndex = selected ? 0 : -1;
        const panel = document.getElementById(tab.getAttribute('aria-controls'));
        if (panel) panel.hidden = !selected;
    }
    if (group.classList.contains('segment')) {
        const all = group.parentElement.querySelector('.all-records');
        const subscriptionsSelected = button.textContent === '订阅';
        all.dataset.action = subscriptionsSelected ? 'all-subscriptions' : 'all-projects';
        all.setAttribute('aria-label', subscriptionsSelected ? '查看全部订阅服务' : '查看全部长期项目');
    }
}

const dialog = document.getElementById('sample-dialog');
function openDialog(title, lines = [], actions = []) {
    dialog.querySelector('h2').textContent = title;
    const body = dialog.querySelector('div');
    body.replaceChildren();
    for (const line of lines) {
        const p = document.createElement('p');
        p.textContent = line;
        body.append(p);
    }
    for (const [label, action] of actions) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'dialog-action';
        button.dataset.action = action;
        button.textContent = label;
        body.append(button);
    }
    if (!dialog.open) dialog.showModal();
}

const dialogs = {
    add: ['添加', [], [['新增项目', 'new-project'], ['新增订阅', 'new-subscription']]],
    'new-project': ['新增项目', ['使用正式应用既有的项目表单。']],
    'new-subscription': ['新增订阅', ['使用正式应用既有的订阅表单。']],
    search: ['搜索', ['搜索项目或订阅。']],
    'overview-info': ['成本计算说明', ['总投入包括截至观察日的长期项目投入，含折抵，未扣收回。', '日均成本按当前有效记录计算，不是今日扣款。已录入数量包含历史。']],
    composition: ['成本构成', ['长期项目 ¥13.24 /天', '当前订阅 ¥1.98 /天']],
    'all-projects': ['全部长期项目', ['合成横向相机', '合成阅读器']],
    'all-subscriptions': ['全部订阅服务', ['合成阅读服务']],
    reader: ['合成阅读器', ['数码 · 持有中 · 60 天', '项目日均成本 ¥3.32 /天']],
    'project-info': ['成本口径', ['截至 2026-11-13', '净投入 ¥1,200.00 ÷ 121 天']],
    'cost-history': ['成本历史', ['沿用正式应用原入口。']],
    'subscription-info': ['历史平均日成本', ['截至 2026-11-13', '累计实际付款 ¥297.60 ÷ 折算付费天数 120 天']],
    'current-plan': ['当前套餐', ['月付 · ¥59.40', '当前标准日均价 ¥1.98', '2026-10-26 生效 · 2026-11-26 到期（当日不含）']],
    'next-plan': ['下一期', ['已付款，尚未生效', '月付 · ¥89.40', '2026-11-10 付款 · 2026-11-26 生效']],
    'project-more': ['更多项目操作', ['按现有操作资格显示编辑、结束、成本历史及删除。']],
    'subscription-more': ['更多订阅操作', ['按现有操作资格显示订阅管理和付款纠错。']],
    'replace-photo': ['更换照片', ['正式应用通过受保护的照片选择流程打开。']],
    'remove-photo': ['取消封面显示', ['确认后取消当前封面显示。历史照片不会立即删除。']],
    record: ['记一笔', [], [['记录投入', 'outlay'], ['记录收回', 'recovery']]],
    outlay: ['记录投入', ['沿用正式应用投入表单。']],
    recovery: ['记录收回', ['沿用正式应用收回表单。']],
    renew: ['记录续费', ['沿用正式应用真实续费表单。']],
};
document.addEventListener('click', event => {
    const button = event.target.closest('button');
    if (!button) return;
    if (button.matches('[role=tab]')) {
        selectTab(button);
        return;
    }
    if (button.classList.contains('history-toggle')) {
        const expanded = button.getAttribute('aria-expanded') !== 'true';
        button.setAttribute('aria-expanded', expanded);
        button.textContent = expanded ? '收起' : '全部';
        for (const row of document.getElementById(button.getAttribute('aria-controls')).querySelectorAll('.extra-event')) row.hidden = !expanded;
        return;
    }
    if (button.dataset.event) {
        openDialog('记录详情', [button.dataset.event, '正式应用沿用原纠错入口。']);
        return;
    }
    const action = button.dataset.action;
    if (action === 'project' || action === 'subscription') {
        document.getElementById(action + '-detail').scrollIntoView({ block: 'center' });
    } else if (action === 'back') {
        document.getElementById('home-themes').scrollIntoView();
    } else if (action === 'cover') {
        openDialog('项目封面');
        const preview = document.createElement('img');
        preview.src = camera;
        preview.alt = '合成横向相机完整封面';
        dialog.querySelector('div').append(preview);
    } else if (dialogs[action]) {
        openDialog(...dialogs[action]);
    }
});
document.addEventListener('keydown', event => {
    const tab = event.target.closest('[role=tab]');
    if (!tab || !['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    const group = Array.from(tab.parentElement.querySelectorAll('[role=tab]'));
    const current = group.indexOf(tab);
    const next = { ArrowLeft: (current - 1 + group.length) % group.length, ArrowRight: (current + 1) % group.length, Home: 0, End: group.length - 1 }[event.key];
    event.preventDefault();
    selectTab(group[next]);
    group[next].focus();
});
