const glyphs = {
  back: 'M19 12H5m7-7-7 7 7 7',
  more: 'M5 12h.1m6.9 0h.1m6.9 0h.1',
  info: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0ZM12 11v6m0-10h.01',
  battery: 'M3 7h16v10H3zm18 3v4m-15-4h9v4H6z',
  wifi: 'M2 8a16 16 0 0 1 20 0M5 12a11 11 0 0 1 14 0m-11 4a6 6 0 0 1 8 0m-4 3h.01',
  project: 'M3 7l9-4 9 4v10l-9 4-9-4zm0 0 9 5 9-5m-9 5v9',
  service: 'M3 5h18v15H3zm0 5h18M7 3v4m10-4v4m-10 7h6m-6 3h9',
  loading: 'M20 12a8 8 0 1 1-8-8m8 0v5h-5',
  broken: 'M14 20H3V4h18v6M3 15l5-5 4 4m6-3 5 9H13zm0 4v2m0 2h.01',
  add: 'M12 5v14M5 12h14',
  chevron: 'M9 6l6 6-6 6',
};
const icon = name => '<svg aria-hidden="true" viewBox="0 0 24 24"><path d="' + glyphs[name] + '"/></svg>';
const iconButton = (name, label) => '<button class="icon-button" aria-label="' + label + '">' + icon(name) + '</button>';
const date = value => '<time>' + value + '</time>';
const cash = (value, unit = '', extra = '') => '<span class="money ' + extra + '"><span class="value">' +
  (value.startsWith('−') ? '−¥' + value.slice(1) : value === '—' ? value : '¥' + value).replaceAll(',', ',<wbr>') +
  '</span>' + (unit ? ' <span class="unit">' + unit + '</span>' : '') + '</span>';
const btn = (label, attr = '') => '<button class="detail-action" ' + attr + '>' + label + '</button>';
const sec = (title, content) => '<section class="section"><h3>' + title + '</h3>' + content + '</section>';
const line = (label, value) => '<div class="line"><span class="muted">' + label + '</span><span>' + value + '</span></div>';
const state = label => '<span class="state">' + label + '</span>';
const camera = '<img src="assets/synthetic-camera.png" alt="合成相机完整封面">';
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
  return '<section class="hero"><div class="hero-label"><span>' + title + '</span>' +
    iconButton('info', title + '说明') + '</div>' + cash(value, value === '—' ? '' : '/天', 'hero-value' + (options.long ? ' extreme' : '')) +
    (suffix ? '<p class="muted">' + suffix + '</p>' : '') +
    (options.long ? btn('查看完整金额') : '') + '</section>';
}
function fact(label, value) {
  return '<div class="fact"><span class="muted">' + label + '</span>' + cash(value) + '</div>';
}
function event(when, label, value, detail, action) {
  return '<article class="record"><p class="date">' + date(when) + '</p><div class="record-title"><b>' +
    label + '</b>' + cash(value) + '</div><p class="muted">' + detail + '</p>' +
    (action ? btn(action) : '') + '</article>';
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
    (empty ? '' : sec('累计资金', '<div class="facts' + (linked ? ' stack' : '') + '">' +
      fact(linked ? '累计投入（含非现金折抵）' : '累计实际投入', long ? '9,999,999,999.99' : zero ? '0.00' : '1,400.00') +
      fact(linked ? '累计收回（含非现金折抵）' : '累计实际收回', long ? '0.00' : ended ? '450.00' : negative ? '1,800.00' : zero ? '0.00' : '200.00') +
      fact('净投入', long ? '9,999,999,999.99' : ended ? '950.00' : negative ? '−400.00' : zero ? '0.00' : '1,200.00') + '</div>')) +
    sec('真实金额记录', records) +
    sec('资料与历史', line('开始拥有', date(long ? '2026-11-13' : '2026-07-16')) +
      (ended ? line('已记录结束日期', date('2026-11-02')) + line('结束原因', '转交闲置设备') : '') +
      (linked ? '<p class="muted">有关联换新，金额与日期不能单独修改。</p>' : '') + btn('查看成本历史'));
}
function payment(paidOn, effective, end, value, future, stale = false) {
  return '<article class="record"><p class="date">付款 ' + date(paidOn) + '</p><div class="record-title">' +
    '<b>月付 · 折算 30 天</b>' + cash(value) + '</div>' + (future ? state('尚未生效') : '') +
    '<p class="muted">生效 ' + date(effective) + '<br>到期 ' + date(end) + '（不含当日）</p>' +
    '<div class="row-actions">' + iconButton('info', '付款及覆盖日期') +
    btn('付款操作', stale ? 'disabled' : '') + '</div></article>';
}
function subscription(options) {
  const {read, photo = 'empty', empty, ended, paused, long, historyStale} = options;
  if (read === 'failed') return notice('暂时无法读取订阅', '', true);
  const tile = photo === 'normal' ? registered : icon(photo === 'broken' ? 'broken' : 'service');
  const name = long ? '合成阅读服务与离线阅读资料长期订阅' : '合成阅读服务';
  const service = '<section class="identity service"><span class="tile">' + tile + '</span><div><h2>' +
    name + '</h2><p class="meta">内容 ' + state(ended ? '已停止' : paused ? '已暂停' : empty ? '已到期' : '订阅中') + '</p>' +
    (photo === 'broken' ? '<p class="muted small">封面暂不可用</p>' : '') + '</div></section>';
  const plan = ended || paused ? '<p>当前无有效套餐</p>' + line('当前日均贡献', cash('0.00', '/天')) :
    line('套餐价格 · 月付', cash(long ? '9,999,999,999.99' : '59.40')) +
    line('当前标准日均价', cash(long ? '333,333,333.33' : '1.98', '/天')) +
    line('实际生效日期', date('2026-10-26')) +
    line('付费到期边界', date('2026-11-26') + '（不含当日）');
  const history = (historyStale ? notice('付款历史暂时无法读取', '以下为上次读取的付款历史。', true) : '') + (empty ? '<p class="muted">暂无付费记录</p>' : long ?
    payment('2026-10-26', '2026-10-26', '2026-11-26', '9,999,999,999.99', false) :
    payment('2026-11-10', '2026-11-26', '2026-12-26', '89.40', true, read === 'stale' || historyStale) +
    payment('2026-10-26', '2026-10-26', '2026-11-26', '59.40', false, read === 'stale' || historyStale));
  const suffix = empty ? '暂无已确认付款' : '结果截至 ' + (read === 'stale' ? '2026-11-12' : '2026-11-13');
  return (read === 'stale' ? notice('暂时无法刷新', '以下为上次读取记录。', true) : '') + service +
    hero('历史平均日成本', empty ? '—' : long ? '333,333,333.33' : '2.48', suffix, options) +
    (empty ? '' : sec('已确认付款', line('累计实际付款', cash(long ? '9,999,999,999.99' : '148.80')) +
      line('折算付费时长', long ? '30 天' : '60 天') +
      line('实际已过去的付费有效天数', ended || paused || read === 'stale' ? '18 天' : '19 天'))) +
    (empty ? '' : sec('当前套餐', plan + (long ? '' : '<p class="muted small">已付下一期尚未生效。</p>'))) +
    (ended || paused ? sec('状态事实', line(ended ? '已停止' : '已暂停', date('2026-11-13')) + btn('修正这条状态事实')) : '') +
    sec('真实付款历史', history) +
    (empty ? '' : sec('资料与历史', btn('查看价格历史')));
}
const provider = '暂停、恢复或停止只改变砺石中的记录；取消服务商自动续费需到服务商处操作。';
const dialogs = {
  end: ['结束项目', '从结束日期起退出当前项目列表，项目与金额记录仍保留。', '保存结束记录', '<div class="form-fields"><p>结束日期　2026-11-13</p><p>结束原因</p><p>□ 同时记录实际收回</p><p class="muted">只有实际收到款项时才勾选。</p></div>'],
  delete: ['永久删除项目？', '将删除「合成横向相机」及其金额记录，历史统计也会重算。无法在砺石内撤回。', '确认永久删除'],
  pause: ['暂停订阅', provider + ' 真实付款历史保留。', '确认暂停', '<div class="form-fields"><p>状态生效日期　2026-11-13</p><p>原因（可选）</p><p>备注（可选）</p></div>'],
  stop: ['停止记录', provider + ' 真实付款历史保留。', '确认停止记录', '<div class="form-fields"><p>已确认付费的最后边界　2026-12-26</p><p>确认后日期固定，后续付款不会自动移动。</p><div class="choice"><button aria-pressed="true">到期后停止</button><button aria-pressed="false">提前停止</button></div><p>状态生效日期　2026-12-26</p><p>原因（可选）</p><p>备注（可选）</p></div>'],
  dirty: ['放弃未保存修改？', '返回将丢弃当前表单的修改。', '放弃修改'],
  payment: ['付款操作', '实际付款日 2026-11-10 · ¥89.40', '修正这笔付款'],
};
function screen(frame) {
  const {entity, width = 360, theme = 'white', scale = 1, pos = 0, read, ended, dialog} = frame;
  const isProject = entity === 'project';
  const name = isProject ? '项目详情' : '订阅详情';
  const action = dialog === 'dirty' ? '保存修改' : isProject ? icon('add') + '记一笔' : '记录真实续费';
  const statusBar = '<div class="status"><span>09:31</span><div>' + icon('wifi') + icon('battery') + '</div></div>';
  const topBar = '<div class="toolbar">' + iconButton('back', '返回来源') + '<b>' + (dialog === 'dirty' ? '编辑项目资料' : name) + '</b>' +
    (read === 'failed' || dialog === 'dirty' ? '<span></span>' : iconButton('more', isProject ? '更多项目操作' : '更多订阅操作')) + '</div>';
  const footer = read === 'failed' || (!isProject && ended) ? '<div></div>' :
    '<div class="footer"><button ' + (read === 'stale' && !isProject ? 'disabled' : '') + '>' + action + '</button></div>';
  let content = isProject ? project(frame) : subscription(frame);
  if (dialog === 'end' || dialog === 'pause' || dialog === 'stop') {
    const [title, copy, primary, fields] = dialogs[dialog];
    content = '<section class="identity"><h2>' + (isProject ? '合成横向相机' : '合成阅读服务') +
      '</h2></section><section class="hero"><p>' + copy + '</p>' + fields + '</section>';
    return '<article class="screen ' + theme + '" style="--width:' + width + 'px;--scale:' + scale + '">' + statusBar +
      '<div class="toolbar">' + iconButton('back', '返回详情') + '<b>' + title + '</b><span></span></div>' +
      '<div class="scroll"><div class="body">' + content + '</div></div><div class="footer"><button>' + primary +
      '</button></div><div class="system-nav"><i></i></div></article>';
  }
  let overlay = '';
  if (dialog) {
    const [title, copy, primary] = dialogs[dialog];
    const safe = dialog === 'dirty' ? '继续编辑' : '取消';
    if (dialog === 'dirty') content = '<section class="identity"><h2>编辑项目资料</h2><p class="meta">合成横向相机</p></section>' +
      sec('备注', '<p>旅途中携带的相机</p>');
    overlay = dialog === 'payment' ? '<div class="payment-menu"><div class="modal"><div class="action-list" aria-label="' + copy + '">' + btn(primary) + '<button class="detail-action danger">删除错误付款</button>' +
      '</div></div></div>' : '<div class="modal-backdrop"><div class="modal"><h3>' + title + '</h3><p>' + copy +
      '</p><div class="dialog-actions"><button>' + safe + '</button><button class="confirm destructive">' + primary +
      '</button></div></div></div>';
  }
  return '<article class="screen ' + theme + (scale === 2 ? ' big' : '') +
    '" style="--width:' + width + 'px;--scale:' + scale + '">' + statusBar + topBar +
    '<div class="scroll" data-position="' + pos + '"><div class="body">' + content + '</div></div>' +
    footer + '<div class="system-nav"><i></i></div>' + overlay + '</article>';
}
const p = (label, props = {}) => ({label, entity: 'project', ...props});
const s = (label, props = {}) => ({label, entity: 'subscription', ...props});
const pair = (width, scale) => [p('项目 · ' + width + 'dp · ' + scale * 100 + '%', {width, scale}), s('订阅 · ' + width + 'dp · ' + scale * 100 + '%', {width, scale})];
const themes = [['纯白', 'white'], ['岩白雾蓝', 'stone'], ['深色黑灰', 'dark']];
const boards = {
  pair: ['项目与订阅 · 首屏配对', 2, [p('项目详情 · 封面与持有成本'), s('订阅详情 · 平均成本与有效套餐')]],
  'project-themes': ['项目详情 · 三主题', 3, themes.map(([label, theme]) => p(label, {theme}))],
  'subscription-themes': ['订阅详情 · 三主题', 3, themes.map(([label, theme]) => s(label, {theme}))],
  'history': ['真实金额记录与付款历史', 4, [p('项目 · 记录时间线', {pos: 430}), p('项目 · 非现金折抵与纠错', {linked: true, pos: 550}),
    s('订阅 · 已付款未生效', {pos: 450}), s('订阅 · 付款纠错入口', {dialog: 'payment', pos: 530})]],
  'font-200': ['360dp · 200% 字体重排', 4, [...pair(360, 2), p('项目 · 大字滚动至资金', {scale: 2, pos: 580}), s('订阅 · 大字滚动至套餐', {scale: 2, pos: 570})]],
  'width-400': ['400dp · 常规与大字', 4, [...pair(400, 1), ...pair(400, 2)]],
  'wide-720': ['720dp · 居中阅读列', 2, [...pair(720, 1), ...pair(720, 2)]],
  'project-states': ['项目 · 封面、历史与读取状态', 4, [p('已有封面 · API 36 合格时的操作', {mediaActions: true}), p('无封面', {photo: 'empty'}), p('封面读取中', {photo: 'loading'}), p('封面暂不可用', {photo: 'broken'}),
    p('已结束 · 持有天数已冻结', {ended: true}), p('已结束 · 滚动至实际结算', {ended: true, pos: 500}), p('空历史 · 合成防御态', {empty: true, photo: 'empty'}),
    p('读取失败 · 旧快照', {read: 'stale', photo: 'empty'}), p('读取失败 · 无快照', {read: 'failed'}), p('长名称与极端金额', {long: true, scale: 2, photo: 'empty', pos: 55}),
    p('真实零值', {zero: true, photo: 'empty'}), p('负净投入', {negative: true, photo: 'empty'})]],
  'subscription-states': ['订阅 · 封面、停止与读取状态', 4, [s('无封面'), s('已关联封面', {photo: 'normal'}), s('封面暂不可用', {photo: 'broken'}),
    s('已停止 · 无当前套餐', {ended: true}), s('空历史 · 合成防御态', {empty: true}), s('读取失败 · 旧快照', {read: 'stale'}),
    s('读取失败 · 无快照', {read: 'failed'}), s('长名称与极端金额', {long: true, scale: 2, pos: 20}), s('已暂停 · 无当前套餐', {paused: true}),
    s('付款历史失败 · 保留已读页', {historyStale: true, pos: 530})]],
  'guards': ['状态确认与未保存保护', 3, [p('项目 · 结束持有', {dialog: 'end'}), p('项目 · 永久删除', {dialog: 'delete'}),
    p('表单 · 未保存返回', {dialog: 'dirty'}), s('订阅 · 暂停', {dialog: 'pause'}), s('订阅 · 停止记录', {dialog: 'stop'})]],
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
