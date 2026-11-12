(() => {
const paths = {
  search: '<circle cx="10.8" cy="10.8" r="5.8"/><path d="m15.2 15.2 4.7 4.7"/>',
  back: '<path d="M20 12H4m7 7-7-7 7-7"/>',
  more: '<circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  next: '<path d="m9 5 7 7-7 7"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6m0-10v.1"/>',
  now: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7v5l3 2"/>',
  review: '<rect x="5" y="4" width="14" height="16" rx="2"/><path d="M8 2v4m8-4v4M8 10h8m-8 4h5"/>',
  settings: '<path d="M5 4v16m7-16v16m7-16v16M2 9h6m1 6h6m1-5h6"/>',
  box: '<path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Zm0 9 8-4.5M12 12 4 7.5M12 12v9"/>',
  reading: '<path d="M12 6.5C9.4 4 5.6 4 3 4.8V18c3-.9 6.6-.5 9 1.5 2.4-2 6-2.4 9-1.5V4.8c-2.6-.8-6.4-.8-9 1.7Zm0 0v13"/>',
  music: '<path d="M9 18V6l11-2v11M9 18c0 1.1-1.2 2-2.7 2S4 19.3 4 18.2s1.2-2 2.7-2 2.3.7 2.3 1.8Zm11-3c0 1.1-1.2 2-2.7 2s-2.3-.7-2.3-1.8 1.2-2 2.7-2 2.3.7 2.3 1.8Z"/>',
};
function icon(name) {
  return `<svg aria-hidden="true" class="glyph" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round">${paths[name]}</svg>`;
}
const reader = '电子书阅读器 Paper';
const bag = '帆布通勤包';
const logo = '../../../../app/src/main/res/drawable-nodpi/ic_launcher_scene_p3.png';
const iconBtn = (name,label,cls='icon-button') => `<button class="${cls}" type="button" aria-label="${label}">${icon(name)}</button>`;
const btn = (label,cls='button') => `<button class="${cls}" type="button">${label}</button>`;
const day = (value, size='') => `<span class="money ${size}" aria-label="${value} 元每天">¥${value}<span class="unit">/天</span></span>`;
const money = value => `<span class="money">¥${value}</span>`;
const moreLink = label => btn(`${label} ${icon('next')}`,'text-button');
const photo = (css='thumb') => `<img class="${css}" src="assets/reader.jpg" alt="黑色电子书阅读器正面">`;
const noCover = name => `<span class="thumb no-cover" aria-hidden="true">${icon(name)}</span>`;
const header = () => `<div class="topbar"><span class="brand"><img src="${logo}" alt="">砺石</span>${iconBtn('search','搜索项目和订阅')}</div>`;
const top = label => `<div class="topbar">${iconBtn('back','返回','back')}<span class="bar-label">${label}</span>${iconBtn('more','更多操作')}</div>`;
const section = (title, accessible, action='全部') => `<div class="split section-line"><h2 class="section-title">${title}</h2><button type="button" class="text-button" aria-label="${accessible}">${action} ${icon('next')}</button></div>`;
const detailRows = (rows) => `<div class="rows">${rows.map(([label,value])=>`<div class="row"><span class="row-copy">${label}</span><span class="trailing">${value}</span></div>`).join('')}</div>`;
const record = (name, fact, cover, amount, aria) => `<div class="row record-row">${cover}<div class="row-copy"><strong>${name}</strong><p class="note">${fact}</p></div><div class="trailing" aria-label="${aria} ${amount} 元每天">${day(amount)}</div></div>`;
const eRow = record(reader,'数码 · 持有中 · 120 天',photo(),'9.17','项目日均成本');
const bRow = record(bag,'日用 · 持有中 · 90 天',noCover('box'),'4.00','项目日均成本');
const sRows = `<div class="record-labels">历史平均日成本</div><div class="rows">${record('阅读会员','阅读 · 订阅中 · 折算付费 30 天',noCover('reading'),'1.00','订阅历史平均日成本')}${record('音乐会员','影音 · 订阅中 · 折算付费 30 天',noCover('music'),'0.50','订阅历史平均日成本')}</div>`;
const projects = `<div class="record-labels">项目日均成本</div><div class="rows">${eRow}${bRow}</div>`;
const homeLower = `<div class="home-lower"><div class="note">已录入（含历史） · 2 项目 · 2 订阅</div><div class="split">${moreLink('成本与类别构成')}</div></div>`;
const homeOptions = {nav:'now',footer:btn(icon('plus')+' 新增记录','button wide-action home-add')};
const costStrip = `<div class="archive-metric split"><div><p class="kicker">当前日均成本</p><p class="note">截至 2026-11-12 · 非今日扣款</p></div><div class="split" style="gap:2px"><span class="money-box">${day('14.67','medium-money')}</span>${iconBtn('info','计算说明')}</div></div>`;
function archiveHome(coverState='ready') {
  const feature = coverState==='ready' ? `${photo('cover-feature')}
  <div class="split feature-facts"><div class="row-copy"><strong class="feature-title">${reader}</strong><p class="note">数码 · 持有中 · 120 天</p></div><div class="trailing">${day('9.17')}<p class="note record-metric">项目日均成本</p></div></div>
  ` : `<div class="record-labels">项目日均成本</div><div class="rows">${record(reader,'数码 · 持有中 · 120 天',noCover('box'),'9.17','项目日均成本')}</div>${coverState==='unavailable' ? btn('封面暂不可用','text-button') : ''}`;
  return `${header()}<div class="archive-home">${section('长期项目','查看全部长期项目')}${feature}
  <div class="rows">${bRow}</div>${costStrip}<div class="subscription-group">${section('订阅服务','查看全部订阅服务')}${sRows}</div>${homeLower}</div>`;
}
function ledgerHome() {
  return `${header()}<div class="ledger-home"><div class="ledger-metric"><div class="split"><h1 class="kicker">当前日均成本</h1>${iconBtn('info','计算说明')}</div>
  <div class="money-box">${day('14.67','big-money')}</div><p class="note">截至 2026-11-12 · 非今日扣款</p>
  <div class="ledger-splits"><div><p class="kicker">项目当前贡献</p>${day('13.17')}</div><div><p class="kicker">订阅当前贡献</p>${day('1.50')}</div></div></div>
  <div class="split">${moreLink('成本与类别构成')}</div>${section('长期项目','查看全部长期项目')}${projects}
  ${section('订阅服务','查看全部订阅服务')}${sRows}<div class="home-lower"><p class="note">已录入（含历史） · 2 项目 · 2 订阅</p></div></div>`;
}
const projectFacts = `<div class="mini-stats"><div><p class="kicker">累计投入</p>${money('1,200.00')}</div><div><p class="kicker">累计收回</p>${money('100.00')}</div><div><p class="kicker">净投入</p>${money('1,100.00')}</div></div>`;
const projectTimeline = `<h2 class="section-title">金额记录</h2>${detailRows([
  ['收回 <span class="note">· 10月20日</span>',money('100.00')],
  ['初始投入 <span class="note">· 7月16日</span>',money('1,200.00')],
])}${moreLink('查看成本历史')}`;
const projectId = (featured) => `<div class="detail-identity"><p class="kicker">数码 · 持有中</p><h1 class="page-title">${reader}</h1><p class="note">2026-07-16 开始拥有 · 持有 120 天</p>${featured ? photo('cover-feature detail-cover') : ''}</div>`;
function projectDetail(ledger=false) {
  const metric = `<div class="${ledger?'ledger-metric':'detail-metric'}"><p class="kicker">项目日均成本 · 截至 2026-11-12</p><div class="money-box">${day('9.17',ledger?'big-money':'medium-money')}</div>${ledger?'<p class="note">净投入 ¥1,100.00 · 持有 120 天</p>':''}</div>`;
  return `${top('长期项目')}<div class="${ledger?'ledger-detail':'archive-detail'}">
    ${ledger ? metric+projectFacts+`<div class="split">${projectId(false)}${photo()}</div>` : projectId(true)+metric+projectFacts}
    ${projectTimeline}</div>`;
}
const subId = `<div class="detail-identity"><p class="kicker">阅读 · 订阅中</p><h1 class="page-title">阅读会员</h1><p class="note">付费覆盖 2026-11-01 至 2026-11-30</p></div>`;
const plan = `<div class="split detail-metric"><div><p class="kicker">当前套餐 · 月付</p><div class="money medium-money">¥30.00</div></div><div class="trailing"><p class="kicker">标准日均价</p>${day('1.00')}</div></div>`;
const avg = ledger => `<div class="${ledger?'ledger-metric':'detail-metric'}"><p class="kicker">历史平均日成本</p><div class="money-box">${day('1.00',ledger?'big-money':'medium-money')}</div><p class="note">折算付费 30 天 · 已过去的付费有效 12 天</p></div>`;
const subTimeline = `<h2 class="section-title">实际付款</h2>${detailRows([['2026-11-01 · 月付',money('30.00')]])}<p class="note">2026-11-01 生效 · 到期边界 2026-12-01</p>${moreLink('查看价格历史')}${moreLink('订阅管理')}`;
function subDetail(ledger=false) {
  return `${top('订阅服务')}<div class="${ledger?'ledger-detail':'archive-detail'}">${ledger ? avg(true)+plan+subId : subId+plan+avg(false)}${subTimeline}</div>`;
}
const frame = (title, body, opts={}) => ({title,body,opts});
const projectOpts = {footer:btn('记一笔','button wide-action')};
const subOpts = {footer:btn('记录真实续费','button wide-action')};
const themeNames = {stone:'岩白雾蓝',white:'纯白',dark:'深色黑灰'};
function variants(body, opts) {
  return Object.entries(themeNames).map(([theme,name])=>frame(name,body,{...opts,theme}));
}
function stateHome(kind) {
  const status = {
    empty: ['—','当前日均成本','已确认空账本','录入第一个长期项目或订阅。','0 项目 · 0 订阅'],
    history: ['0.00','当前日均成本','暂无当前有效记录','截至 2026-11-12 · 非今日扣款','2 项目 · 2 订阅'],
    zero: ['0.00','当前日均成本','真实零值','截至 2026-11-12 · 非今日扣款','1 项目 · 0 订阅'],
    stale: ['14.79','上次取得的日均成本','读取失败，显示上次结果','上次截至 2026-11-11 · 非今日扣款','2 项目 · 2 订阅'],
    unknown: ['—','当前日均成本','暂时无法取得成本数据','', '项目 — · 订阅 —'],
    waiting: ['—','当前日均成本','正在读取账本','','项目 — · 订阅 —'],
  }[kind];
  const [value,metric,title,note,counts]=status;
  const signed = value==='—' ? '<span class="money big-money" aria-label="金额未取得">—</span>' : day(value,'big-money');
  const retry=['stale','unknown'].includes(kind);
  return `${header()}<div class="state-body"><p class="kicker">${metric}</p><div class="money-box">${signed}</div>
  ${note ? `<p class="note">${note}</p>`:''}<hr class="rule"><div class="state-note"><h1 class="page-title">${title}</h1>
  ${retry ? btn('重试读取','secondary') : ''}</div>
  ${kind==='zero' ? `<div class="record-labels">项目日均成本</div><div class="rows">${record('赠送的帆布袋','日用 · 持有中 · 30 天',noCover('box'),'0.00','项目日均成本')}</div>`:''}
  ${kind==='history' ? `<div class="rows"><div class="row"><span class="row-copy">历史档案</span><span class="trailing">在「回看」中查阅</span></div></div>`:''}
  ${kind==='stale' ? `<h2 class="section-title">上次取得的项目</h2><div class="rows">${record(reader,'数码 · 持有中 · 119 天',photo(),'9.24','项目日均成本')}${record(bag,'日用 · 持有中 · 89 天',noCover('box'),'4.04','项目日均成本')}</div>`:''}
  <p class="note">已录入（含历史） · ${counts}</p></div>`;
}
const detailTheme = projectDetail(false);
const coreBoards = [
 {id:'compare-home',title:'首页 · 两种阅读顺序',note:'同一账本、同一主题。左侧先认出物品，右侧先读总成本和贡献。画板展示首屏，向下可继续浏览。',screens:[
  frame('A · 私人物档',archiveHome(),homeOptions),frame('B · 成本总览',ledgerHome(),homeOptions)]},
 {id:'compare-project',title:'项目详情 · 先看身份或先看成本',note:'同一件电子书阅读器。比较完整封面与持有事实的顺序，以及金额组的阅读速度。静态设计样稿。',screens:[
  frame('A · 私人物档',detailTheme,projectOpts),frame('B · 成本总览',projectDetail(true),projectOpts)]},
 {id:'compare-subscription',title:'订阅详情 · 当前覆盖与历史均值',note:'同一笔真实付款样本。A 先读服务和当前套餐，B 先读历史均值并对照当前套餐。静态设计样稿。',screens:[
  frame('A · 私人物档',subDetail(),subOpts),frame('B · 成本总览',subDetail(true),subOpts)]},
 {id:'themes-home',title:'三主题 · 现在',note:'同一版式、同一账本与照片。雾蓝用于交互，纯白靠字与线分层，黑灰靠明度区分。静态设计样稿。',screens:variants(archiveHome(),homeOptions)},
 {id:'themes-project',title:'三主题 · 项目详情',note:'同一物品、照片、金额和事件，比较三套主题的表面和文字层次。静态设计样稿。',screens:variants(detailTheme,projectOpts)},
 {id:'themes-subscription',title:'三主题 · 订阅详情',note:'同一服务、当前套餐与历史均值，覆盖与付款日期保持一致。静态设计样稿。',screens:variants(subDetail(),subOpts)},
 {id:'responsive',title:'窄屏、宽屏与大字体',note:'同一首页。左图按 2× 近似重排，中间展示窄屏正常字，右图展示宽屏正常字。Android 字号缩放与系统行为待实测。',screens:[
  frame('320 · 大字示意',archiveHome(),{...homeOptions,w:320,large:true}),frame('360 · 常规字号',archiveHome(),homeOptions),
  frame('400 · 常规字号',archiveHome(),{...homeOptions,w:400})]},
 {id:'responsive-wide',title:'宽窗口 · 内容保持可读行宽',note:'较宽窗口维持相同三目的地及内容顺序。当前画板只展示布局样稿。',screens:[
  frame('720 · 居中阅读列',archiveHome(),{...homeOptions,w:720,h:840})]},
 {id:'identity-states',title:'档案身份 · 无封面、封面不可用与大字滚动',note:'无图条目回到自然列表，避免空白图片位。已登记而无法读取时保留独立反馈。右图为 2× 字号的订阅区。',screens:[
  frame('首条项目无封面',archiveHome('none'),homeOptions),
  frame('首条项目封面不可用',archiveHome('unavailable'),homeOptions),
  frame('360 · 大字滚至订阅区',archiveHome(),{...homeOptions,large:true,startAt:'.subscription-group'})]},
 {id:'ledger-states',title:'首页状态 · 空、历史、真实零值',note:'使用不同合成账本状态。空账本显示横杠，已知零保留金额单位。每种状态的新增入口均独占布局空间。',screens:[
  frame('确认空账本',stateHome('empty'),homeOptions),frame('只有历史',stateHome('history'),homeOptions),frame('持有中且实际零投入',stateHome('zero'),homeOptions)]},
 {id:'read-states',title:'读取状态 · 进行中、旧快照、未知',note:'日期仅在有证据时呈现。无可信快照保留横杠与重试，旧快照明确标注为上次结果。',screens:[
  frame('正在读取',stateHome('waiting'),homeOptions),frame('读取失败 · 保留旧快照',stateHome('stale'),homeOptions),frame('读取失败 · 无快照',stateHome('unknown'),homeOptions)]},
];
const statusIcon = '<svg viewBox="0 0 39 12" aria-hidden="true"><path d="M0 4a8 8 0 0 1 11 0l-5.5 6.8Z" fill="currentColor"/><path d="m15 10 9-8v8z" fill="currentColor"/><rect x="28" y="2" width="8" height="8" rx="1.2" fill="none" stroke="currentColor"/><path d="M37 5v2M30 4v4h4V4z" fill="currentColor" stroke="currentColor"/></svg>';
const navigation = selected => `<div class="app-nav" aria-label="主导航">${Object.entries({now:'现在',review:'回看',settings:'设置'}).map(([id,label])=>`<button type="button"${selected===id?' aria-current="page"':''}><span class="tab-indicator">${icon(id)}</span><span>${label}</span></button>`).join('')}</div>`;
function renderFrame(item, themeOverride) {
  const {title,body,opts={}} = item;
  const theme = themeOverride || opts.theme || 'stone';
  const w = opts.w || 360;
  const h = opts.h || 800;
  return `<figure class="frame" style="--width:${w}px;--height:${h}px"><div class="device theme-${theme}${opts.large?' large-type':''}${w<=320?' narrow':''}${w>=600?' wide':''}">
  <div class="system-bar" aria-hidden="true"><span>9:41</span>${statusIcon}</div>
  <div class="app-content"${opts.startAt?` data-start-at="${opts.startAt}"`: ''}>${body}</div>${opts.footer?`<div class="app-footer">${opts.footer}</div>`:''}${opts.nav?navigation(opts.nav):''}
  <div class="system-gesture" aria-hidden="true"><i></i></div></div><figcaption>${title}</figcaption></figure>`;
}
const boards = [...coreBoards, ...(window.recordBoards || []), ...(window.analysisBoards || [])];
window.lithicBoards = boards.map(({id,title,screens})=>({id,title,frames:screens.map(({title,opts={}})=>({title,w:opts.w||360,h:opts.h||800}))}));
const params = new URLSearchParams(location.search);
const selection = boards.find(board=>board.id===params.get('board')) || boards[0];
const themeOverride = Object.keys(themeNames).includes(params.get('theme')) ? params.get('theme') : '';
document.body.classList.toggle('capture',params.has('capture'));
document.getElementById('board-title').textContent = selection.title;
document.getElementById('board-note').textContent = selection.note;
const frames = document.querySelector('.frames');
frames.style.setProperty('--columns',selection.screens.length);
frames.innerHTML = selection.screens.map(item=>renderFrame(item,themeOverride)).join('');
for (const scroller of frames.querySelectorAll('[data-start-at]')) {
  const group = scroller.querySelector(scroller.dataset.startAt);
  if (group) scroller.scrollTop = group.getBoundingClientRect().top - scroller.getBoundingClientRect().top + scroller.scrollTop;
}
document.querySelector('.review-menu').innerHTML=boards.map(({id,title})=>`<a href="?board=${id}"${id===selection.id?' aria-current="page"':''}>${title}</a>`).join('');
})();
