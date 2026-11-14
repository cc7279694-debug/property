const glyphs = {
  back: 'M19 12H5m7-7-7 7 7 7',
  more: 'M12 5v.1m0 6.9v.1m0 6.9v.1',
  info: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0ZM12 11v6m0-10h.01',
  battery: 'M3 7h16v10H3zm18 3v4m-15-4h9v4H6z',
  wifi: 'M2 8a16 16 0 0 1 20 0M5 12a11 11 0 0 1 14 0m-11 4a6 6 0 0 1 8 0m-4 3h.01',
  project: 'M3 7l9-4 9 4v10l-9 4-9-4zm0 0 9 5 9-5m-9 5v9',
  service: 'M3 5h18v15H3zm0 5h18M7 3v4m10-4v4m-10 7h6m-6 3h9',
  loading: 'M20 12a8 8 0 1 1-8-8m8 0v5h-5',
  broken: 'M14 20H3V4h18v6M3 15l5-5 4 4m6-3 5 9H13zm0 4v2m0 2h.01',
  add: 'M12 5v14M5 12h14',
  edit: 'M13.5 5.5l5 5M4 20l4.5-1 11-11a2 2 0 0 0-5-5l-11 11L4 20Z',
};
const icon = name => '<svg aria-hidden="true" viewBox="0 0 24 24"><path d="' + glyphs[name] + '"/></svg>';
const iconButton = (name, label) => '<button class="icon-button" aria-label="' + label + '">' + icon(name) + '</button>';
const date = value => '<time>' + value + '</time>';
function paymentDetailsButton(paidOn, content, stale) {
  const label = stale ? '付款及覆盖日期' : '付款详情与操作';
  const actions = stale ? '' : '<div class="preview-actions"><span>修正这笔付款</span><span class="danger">删除错误付款</span></div>';
  return '<button class="icon-button row-control" data-payment-details aria-label="' + label + ' · ' +
    paidOn + '">' + icon(stale ? 'info' : 'more') + '</button><template>' + content + actions + '</template>';
}
const cash = (value, unit = '', extra = '') => '<span class="money ' + extra + '"><span class="value">' +
  (value.startsWith('−') ? '−¥' + value.slice(1) : value === '—' ? value : '¥' + value).replaceAll(',', ',<wbr>') +
  '</span>' + (unit ? ' <span class="unit">' + unit + '</span>' : '') + '</span>';
const btn = (label, attr = '') => '<button class="detail-action" ' + attr + '>' + label + '</button>';
const sec = (title, content) => '<section class="section"><h3>' + title + '</h3>' + content + '</section>';
const line = (label, value) => '<div class="line"><span class="muted">' + label + '</span><span>' + value + '</span></div>';
const state = label => '<span class="state">' + label + '</span>';
const camera = '<img src="../../lithic-ui-v3-03-details/boards/assets/synthetic-camera.png" alt="合成相机完整封面">';
const registered = '<img src="../../lithic-ui-v3-02/boards/assets/reader.jpg" alt="已关联的合成阅读服务封面示例">';
const photoCopy = {
  empty: ['project', ''],
  loading: ['loading', '封面读取中'],
  broken: ['broken', '封面暂不可用'],
};
function cover(status) {
  if (status === 'normal') return '<div class="cover">' + camera + '</div>';
  const [shape, label] = photoCopy[status];
  return '<div class="cover' + (status === 'empty' ? ' compact' : '') + '"><span class="placeholder">' +
    icon(shape) + (label ? '<span class="small">' + label + '</span>' : '') + '</span></div>';
}
function notice(label, text, retry = false) {
  return '<aside class="notice"><p class="error"><b>' + label + '</b></p>' +
    (text ? '<p>' + text + '</p>' : '') + (retry ? btn('重新读取') : '') + '</aside>';
}
function hero(title, value, suffix, options) {
  const isDate = suffix.startsWith('结果截至 ');
  return '<section class="hero"><div class="hero-main"><p class="hero-label">' + title + '</p>' +
    cash(value, value === '—' ? '' : '/天', 'hero-value' + (options.long ? ' extreme' : '')) + '</div>' +
    (suffix ? '<p class="hero-date muted">' + (isDate ? '截至 <time>' + suffix.slice(5) + '</time>' : suffix) + '</p>' : '') +
    iconButton('info', title + '说明') + (options.long ? btn('查看完整金额') : '') + '</section>';
}
function fact(label, value) {
  return '<div class="fact"><span class="muted">' + label + '</span>' + cash(value) + '</div>';
}
function event(when, label, value, detail, action) {
  return '<article class="record"><div class="record-title"><div><span class="date">' + date(when) +
    '</span><b>' + label + '</b><p class="muted small">' + detail + '</p></div>' +
    cash(value) + (action ? '<button class="icon-button row-control" aria-label="' + action + ' · ' + when + '">' +
      icon('edit') + '</button>' : '') + '</div></article>';
}
function recordsCard(title, records) {
  return '<section class="record-group"><h3>' + title + '</h3><div class="group-card">' + records + '</div></section>';
}
function project(options) {
  const {read, photo = 'normal', empty, ended, long, linked, negative, zero, mediaActions} = options;
  if (read === 'failed') return notice('暂时无法读取项目', '', true);
  const visibleDate = read === 'stale' ? '2026-11-12' : '2026-11-13';
  const days = long ? '1 天' : ended ? '110 天' : read === 'stale' ? '120 天' : '121 天';
  const name = long ? '合成横向相机与旅行备用镜头套装' : '合成横向相机';
  const shownRate = empty ? '—' : long ? '9,999,999,999.99' : ended ? '8.64' : negative ? '−3.31' : zero ? '0.00' : read === 'stale' ? '10.00' : '9.92';
  const identity = '<section class="identity">' + cover(photo) + '<h2>' + name + '</h2><p class="meta">其他 ' +
    state(ended ? '已结束' : '持有中') + '<span>累计持有 ' + days + '</span></p>' +
    (photo === 'empty' && !read ? '<div class="media-actions">' + btn('添加照片') + '</div>' :
      mediaActions ? '<div class="media-actions">' + btn('更换照片') + btn('取消封面显示') + '</div>' : '') + '</section>';
  let records = '';
  if (empty) records = '<p class="muted">没有金额记录</p>';
  else if (zero) records = event('2026-07-16', '初始投入', '0.00', '现金', '纠正这条记录');
  else if (long) records = event('2026-11-13', '初始投入', '9,999,999,999.99', '现金', '纠正这条记录');
  else records = event('2026-07-16', '初始投入', linked ? '700.00' : '1,000.00', '现金', linked ? '修改关联说明' : '纠正这条记录') +
    (linked ? event('2026-07-16', '投入', '300.00', '非现金折抵 · 以旧换新关联事实，金额和日期不能单独修改。', '修改关联说明') : '') +
    event('2026-08-01', '投入', '400.00', '现金 · 镜头维修', '纠正这条记录') +
    event('2026-10-26', '收回', negative ? '1,800.00' : '200.00', '现金 · 配件收回', '纠正这条记录') +
    (ended ? event('2026-11-10', '收回', '250.00', '现金 · 结束后实际结算', '纠正这条记录') : '');
  return (read === 'stale' ? notice('暂时无法刷新', '以下为上次读取记录。', true) : '') + identity +
    hero(ended ? '截至观察日的日均成本' : '当前日均成本', shownRate,
      empty ? '' : '结果截至 ' + visibleDate, options) +
    (ended ? '<p class="muted small">包含结束后的实际结算；持有天数不再增加。</p>' : '') +
    (empty ? '' : ('<div aria-label="累计资金" class="facts' + (linked || long ? ' stack' : '') + '">' +
      fact(linked ? '累计投入（含非现金折抵）' : '累计实际投入', long ? '9,999,999,999.99' : zero ? '0.00' : '1,400.00') +
      fact(linked ? '累计收回（含非现金折抵）' : '累计实际收回', long ? '0.00' : ended ? '450.00' : negative ? '1,800.00' : zero ? '0.00' : '200.00') +
      fact('净投入', long ? '9,999,999,999.99' : ended ? '950.00' : negative ? '−400.00' : zero ? '0.00' : '1,200.00') + '</div>')) +
    recordsCard('金额记录', records) +
    sec('资料与历史', line('开始拥有', date(long ? '2026-11-13' : '2026-07-16')) +
      (ended ? line('已记录结束日期', date('2026-11-02')) + line('结束原因', '转交闲置设备') : '') +
      (linked ? '<p class="muted">有关联换新，金额与日期不能单独修改。</p>' : '') + btn('查看成本历史'));
}
function payment(paidOn, effective, end, value, future, stale = false) {
  const dates = '<p>实际付款日 ' + date(paidOn) + '</p><p>生效 ' + date(effective) +
    '</p><p>到期边界 ' + date(end) + '（不含当日）</p><p>月付 · 折算 30 天</p>';
  return '<article class="record"><div class="record-title"><div><span class="date">付款 ' +
    date(paidOn) + '</span><b>月付</b>' + (future ? '<p class="muted small">尚未生效</p>' : '') +
    '</div>' + cash(value) +
    paymentDetailsButton(paidOn, dates, stale) + '</div>' +
    (stale ? '<p class="muted small">生效 ' + date(effective) + ' · 到期 ' + date(end) + '（不含当日）</p>' : '') + '</article>';
}
function subscription(options) {
  const {read, photo = 'empty', empty, ended, paused, long, historyStale} = options;
  if (read === 'failed') return notice('暂时无法读取订阅', '', true);
  const tile = photo === 'normal' ? registered : icon(photo === 'broken' ? 'broken' : 'service');
  const name = long ? '合成阅读服务与离线阅读资料长期订阅' : '合成阅读服务';
  const service = '<section class="identity service"><span class="tile">' + tile + '</span><div><h2>' +
    name + '</h2><p class="meta">内容 ' + state(ended ? '已停止' : paused ? '已暂停' : empty ? '已到期' : '订阅中') + '</p>' +
    (photo === 'broken' ? '<p class="muted small">封面暂不可用</p>' : '') + '</div></section>';
  const plan = ended || paused || empty ? '<p>当前无有效套餐</p>' + line('当前日均贡献', cash('0.00', '/天')) :
    '<div class="plan-title"><b>月付 ' + cash(long ? '9,999,999,999.99' : '59.40') + '</b>' + state('生效中') + '</div>' +
    '<p class="muted small">2026-10-26 至 2026-11-26（不含当日）</p>' +
    line('当前标准日均价', cash(long ? '333,333,333.33' : '1.98', '/天'));
  const next = !long && !empty ? '<section class="plan"><h3>下一期 · 已付款</h3>' +
    '<div class="plan-title"><b>月付 ' + cash('89.40') + '</b>' + state('尚未生效') + '</div>' +
    '<p class="muted small">2026-11-26 至 2026-12-26（不含当日）</p></section>' : '';
  const history = (historyStale ? notice('付款历史暂时无法读取', '以下为上次读取的付款历史。', true) : '') + (empty ? '<p class="muted">暂无付费记录</p>' : long ?
    payment('2026-10-26', '2026-10-26', '2026-11-26', '9,999,999,999.99', false) :
    payment('2026-11-10', '2026-11-26', '2026-12-26', '89.40', true, read === 'stale' || historyStale) +
    payment('2026-10-26', '2026-10-26', '2026-11-26', '59.40', false, read === 'stale' || historyStale));
  const suffix = empty ? '暂无已确认付款' : '结果截至 ' + (read === 'stale' ? '2026-11-12' : '2026-11-13');
  return (read === 'stale' ? notice('暂时无法刷新', '以下为上次读取记录。', true) : '') + service +
    hero('历史平均日成本', empty ? '—' : long ? '333,333,333.33' : '2.48', suffix, options) +
    '<section class="plan"><h3>当前套餐</h3>' + plan + '</section>' + next +
    (ended || paused ? sec('状态事实', line(ended ? '已停止' : '已暂停', date('2026-11-13')) + btn('修正这条状态事实')) : '') +
    recordsCard('付款记录', history) +
    (empty ? '' : sec('已确认付款', line('累计实际付款', cash(long ? '9,999,999,999.99' : '148.80')) +
      line('折算付费时长', long ? '30 天' : '60 天') +
      line('实际已过去的付费有效天数', ended || paused || read === 'stale' ? '18 天' : '19 天') + btn('查看价格历史')));
}
function screen(frame) {
  const {entity, width = 360, theme = 'white', scale = 1, pos = 0, read, ended} = frame;
  const isProject = entity === 'project';
  const name = isProject ? '项目详情' : '订阅详情';
  const action = isProject ? '记一笔' : '记录真实续费';
  const status = '<div class="status"><span>09:31</span><div>' + icon('wifi') + icon('battery') + '</div></div>';
  const toolbar = '<div class="toolbar">' + iconButton('back', '返回来源') + '<b class="sr-only">' + name +
    '</b>' + (read === 'failed' ? '<span></span>' : iconButton('more', isProject ? '更多项目操作' : '更多订阅操作')) + '</div>';
  const footer = read === 'failed' || (!isProject && ended) ? '<div></div>' :
    '<div class="footer"><button ' + (read === 'stale' && !isProject ? 'disabled' : '') + '>' + icon('add') + action + '</button></div>';
  return '<article class="screen ' + theme + (scale === 2 ? ' big' : '') + '" style="--width:' + width +
    'px;--scale:' + scale + '">' + status + toolbar + '<div class="scroll" data-position="' + pos +
    '"><div class="body">' + (isProject ? project(frame) : subscription(frame)) + '</div></div>' +
    footer + '<div class="system-nav"><i></i></div></article>';
}
const p = (label, props = {}) => ({label, entity: 'project', ...props});
const s = (label, props = {}) => ({label, entity: 'subscription', ...props});
const pair = (width, scale) => [p('项目 · ' + width + 'dp · ' + scale * 100 + '%', {width, scale}), s('订阅 · ' + width + 'dp · ' + scale * 100 + '%', {width, scale})];
const themes = [['纯白', 'white'], ['岩白雾蓝', 'stone'], ['深色黑灰', 'dark']];
const boards = {
  pair: ['项目与订阅 · 纯白详情', 2, pair(360, 1)],
  'project-themes': ['项目详情 · 三主题', 3, themes.map(([label, theme]) => p(label, {theme}))],
  'subscription-themes': ['订阅详情 · 三主题', 3, themes.map(([label, theme]) => s(label, {theme}))],
  'width-400': ['400dp · 常规字号', 2, pair(400, 1)],
  'font-200': ['200% 字体 · 360 与 400', 4, [...pair(360, 2), ...pair(400, 2)]],
  'history': ['记录与付款事实 · 向下滚动', 2, [p('项目 · 折抵与纠错', {linked: true, pos: 480}),
    s('订阅 · 付款与实付汇总', {pos: 430})]],
  'states': ['关键状态', 3, [p('项目 · 封面不可用', {photo: 'broken'}), p('项目 · 旧快照', {read: 'stale', photo: 'empty'}),
    s('订阅 · 已停止', {ended: true}), p('项目 · 长名称与大金额', {long: true, scale: 2, photo: 'empty', pos: 60}),
    s('订阅 · 付款历史旧页', {historyStale: true, pos: 400}), s('订阅 · 长名称与大金额', {long: true, scale: 2, pos: 20}), s('订阅 · 无快照', {read: 'failed'})]],
};
const params = new URLSearchParams(location.search);
document.body.classList.toggle('export', params.has('export'));
const selected = Object.hasOwn(boards, params.get('board')) ? params.get('board') : 'pair';
const [title, columns, frames] = boards[selected];
document.getElementById('title').textContent = title;
document.getElementById('boards').innerHTML = Object.entries(boards).map(([id, [label]]) =>
  '<a href="?board=' + id + '"' + (id === selected ? ' aria-current="page"' : '') + '>' + label + '</a>').join('');
const root = document.getElementById('frames');
root.style.setProperty('--columns', columns);
root.innerHTML = frames.map(frame => '<figure><figcaption>' + frame.label + '</figcaption>' + screen(frame) + '</figure>').join('');
window.boardReady = document.fonts.ready.then(() => {
  root.querySelectorAll('.scroll').forEach(element => { element.scrollTop = Number(element.dataset.position || 0); });
});

// Read-only date inspection for the review board. Financial actions remain visual examples.
const modal = document.createElement('dialog');
modal.className = 'preview-dialog';
root.append(modal);
root.addEventListener('click', event => {
  const trigger = event.target.closest('button[data-payment-details]');
  if (!trigger) return;
  const details = trigger.nextElementSibling;
  if (!(details instanceof HTMLTemplateElement)) return;
  modal.innerHTML = '<h2>付款详情</h2><div class="preview-data"></div><form method="dialog"><button>关闭</button></form>';
  modal.querySelector('.preview-data').append(details.content.cloneNode(true));
  modal.showModal();
});
