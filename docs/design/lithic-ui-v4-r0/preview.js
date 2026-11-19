'use strict';

const symbols = {
  back: 'M19 12H5m7-7-7 7 7 7', next: 'm9 6 6 6-6 6', plus: 'M12 5v14M5 12h14',
  info: 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20Zm0-11v6m0-10v.1',
  more: 'M5 12h.1m6.9 0h.1m6.9 0h.1', project: 'M4 7.5 12 3l8 4.5v9L12 21l-8-4.5ZM4 7.5l8 4.5 8-4.5M12 12v9',
  subscription: 'M4 6h16v12H4Zm0 4h16m-12 4h3', date: 'M4 6h16v14H4Zm0 4h16M8 4v4m8-4v4',
  backup: 'M4 8h16v12H4Zm2-4h12l2 4H4Zm4 9h4',
  home: 'M2.5 10.5 12 2.8l9.5 7.7M5 8.5V21h5v-6h4v6h5V8.5',
  review: 'M4.5 20v-6m5 6v-9m5 9V7.5m5 12.5V3.5',
  settings: 'M9.76 4.68 10.21 2.77h3.58l.45 1.91 1.35.57 1.67-1.04 2.53 2.53-1.04 1.67.57 1.35 1.91.45v3.58l-1.91.45-.57 1.35 1.04 1.67-2.53 2.53-1.67-1.04-1.35.57-.45 1.91h-3.58l-.45-1.91-1.35-.57-1.67 1.04-2.53-2.53 1.04-1.67-.57-1.35-1.91-.45v-3.58l1.91-.45.57-1.35-1.04-1.67 2.53-2.53 1.67 1.04ZM15.35 12a3.35 3.35 0 1 0-6.7 0 3.35 3.35 0 0 0 6.7 0Z',
  system: 'M2 11V8m3 3V6m3 5V4m3 7V2m7 3q6-5 12 0m-10 2q4-3 8 0m-6 2q2-1.5 4 0m-2 2h.1M36 3h15v8H36zm17 2v4m-14-2h8',
};
const icon = (name) => `<svg class="icon" viewBox="0 0 ${name === 'system' ? '56 14' : '24 24'}" aria-hidden="true"><path d="${symbols[name]}"/></svg>`;
const camera = '../lithic-ui-v3-03-details/boards/assets/synthetic-camera.png';
const reader = '../lithic-ui-v3-02/boards/assets/reader.jpg';
const themes = [['white','纯白'], ['stone','岩白雾蓝'], ['charcoal','深色黑灰']];
const money = (value, large = false, daily = false) => `<p class="amount${large ? ' large' : ''}">${value}${daily ? '<span class="unit">/天</span>' : ''}</p>`;
const title = (name, back = true, action = 'info') => `<header class="top">${back ? `<span class="iconbox">${icon('back')}</span>` : ''}${name ? `<h2>${name}</h2>` : ''}${action ? `<span class="iconbox">${icon(action)}</span>` : ''}</header>`;
const textAction = (label) => `<span class="text-action">${label}${icon('next')}</span>`;
const makeRow = (name, sub = '', value = '', glyph = '', arrow = true, className = '') => `<div class="row ${className}">${glyph ? icon(glyph) : ''}<div class="lead"><strong>${name}</strong>${sub ? `<p class="muted small">${sub}</p>` : ''}</div>${value ? `<span class="value muted small">${value}</span>` : ''}${arrow ? icon('next') : ''}</div>`;
const group = (rows) => `<div class="panel group">${rows.join('')}</div>`;
const section = (name, contents, action = '') => `<section class="section"><div class="section-head"><h3>${name}</h3>${action}</div>${contents}</section>`;
const metric = (label, value, large = false, daily = false, panel = false) => `<div${panel ? ' class="panel"' : ''}><p class="label">${label}</p>${money(value, large, daily)}</div>`;
const foot = (label, glyph = '') => `<footer class="footer"><div class="action">${glyph ? icon(glyph) : ''}${label}</div></footer>`;
const selector = (labels, selected) => `<div class="selector">${labels.map((label,i) => `<span${i === selected ? ' class="on"' : ''}>${label}</span>`).join('')}</div>`;
const dock = (selected) => `<div class="dock"><div class="capsule" aria-label="底部导航">${[['home','首页'],['review','回看'],['settings','设置']].map(([glyph,label]) => `<span aria-label="${label}"${glyph === selected ? ' aria-current="page"' : ''}>${icon(glyph)}</span>`).join('')}</div></div>`;

// The plotted points derive from the same synthetic ledger used in the lists and annual design.
// July 20 camera outlay 4500, October 12 recovery 300, September 18 reader outlay 600.
// The subscription has continuous coverage here, with an actual 30 yuan payment for each 30 days.
const rates = Array.from({length: 120}, (_, day) => {
  const cameraNet = day < 84 ? 4500 : 4200;
  const readerRate = day < 60 ? 0 : 600 / (day - 59);
  return cameraNet / (day + 1) + readerRate + 1;
});
function spark(height) {
  const width = 294;
  const max = 4501;
  const x = (i) => (37 + (width - 44) * i / (rates.length - 1)).toFixed(2);
  const y = (value) => (9 + (height - 17) * (1 - value / max)).toFixed(2);
  let path = `M${x(0)},${y(rates[0])}`;
  rates.slice(1).forEach((value, i) => { path += `L${x(i + 1)},${y(value)}`; });
  return `<figure class="plot" data-chart="multi" aria-label="7月20日至11月16日已录入成本趋势，当前日均46元">
    <svg viewBox="0 0 ${width} ${height}" aria-hidden="true"><path class="chart-grid" d="M36 9H294M36 ${height/2}H294M36 ${height-8}H294"/>
    <text class="chart-axis-label" x="0" y="10">4,501</text><text class="chart-axis-label" x="22" y="${height-7}">0</text>
    <path class="chart-line" d="${path}"/><circle class="chart-point" cx="${x(119)}" cy="${y(rates.at(-1))}" r="4.5"/></svg>
    <div class="axis"><span>07-20</span><span>09-18</span><span>11-16</span></div></figure>`;
}

function trendMetric(variant, detail = false) {
  if (variant === 'single') {
    return `<section class="panel compact-chart" data-single="true"><p class="label">2026-11-16 · 唯一已取得日期</p>
      <p class="label">总日均成本</p>${money('¥30.00', true, true)}<div class="legend small muted"><span>项目 <strong>¥30.00</strong></span><span>订阅 <strong>¥0.00</strong></span></div></section>`;
  }
  return `<section class="panel compact-chart"><div class="chart-head"><div><p class="label">总日均成本</p>${money('¥46.00', true, true)}</div>
    <span class="small muted">2026-11-16</span></div>${spark(detail ? 146 : 85)}
    ${detail ? '<div class="legend small muted"><span>项目 <strong>¥45.00</strong></span><span>订阅 <strong>¥1.00</strong></span></div>' : ''}</section>`;
}

function review(variant) {
  if (variant === 'empty') return title('回看', false) + `<div class="empty-state"><h3>暂无记录</h3><p class="muted">记下一件物品后，在这里看见成本变化。</p><div class="action">去首页记录${icon('next')}</div></div>`;
  return title('回看', false) + `<p class="overview-meta small">${variant === 'single' ? '1 条长期项目 · 0 条订阅服务' : '2 条长期项目 · 1 条订阅服务'}<span>含历史</span></p>` + trendMetric(variant)
    + `<div class="flat">${makeRow('全部历史与事件','日期与记录来源','','review')}</div>`
    + section('分析',group([
      makeRow('类别分析','项目与订阅分列','','project'), makeRow('年度回顾','按资金实际发生年份','','date'),
      makeRow('长期复盘','持有天数与折算付费时长','','review'),
    ]))
    + section('历史档案',group([
      makeRow('已结束长期项目','含后续结算','','project'), makeRow('已停止订阅','','','subscription'),
    ]));
}

function trend() {
  return title('成本趋势') + trendMetric('', true)
    + `<div class="flat tools small"><span class="text-action">前一天</span><span class="text-action" aria-disabled="true">后一天</span>${textAction('更多工具')}</div>`
    + section('所选日期事件', group([makeRow('2026-11-16','当日无已录入事件','','date',false)]))
    + section('日期与事件',group([
      makeRow('真实付款','2026-10-18 · 合成阅读服务','¥30.00'), makeRow('项目收回','2026-10-12 · 合成相机','¥300.00'),
      makeRow('开始持有','2026-09-18 · 合成阅读器','¥600.00'),
    ])) + `<div class="tools small">${textAction('查看显示点文字清单')}</div>`;
}

function annual() {
  const row = (label, value) => makeRow(label,'',value,'',false);
  const series = [['项目投入',5100,'¥5,100.00'],['项目收回',300,'¥300.00'],['项目净投入',4800,'¥4,800.00'],['订阅实际付款',120,'¥120.00']];
  const bars = series.map(([name, value, label]) => `<div><p>${name} · ${label}</p><div class="bar"><span style="width:${value/5100*100}%"></span></div></div>`).join('');
  return title('年度回顾') + `<div class="annual-year"><div class="yearnav"><span class="iconbox" aria-label="上一年度">${icon('back')}</span><strong>2026 年</strong><span class="iconbox" aria-label="下一年度" aria-disabled="true">${icon('next')}</span></div>${textAction('查看年度')}</div>`
    + `<div class="metric-stack flat">${metric('项目净投入','¥4,800.00',true)}${metric('订阅实际付款','¥120.00',true)}</div>`
    + `<p class="small muted" style="padding-top:8px">结果截至 2026-11-16</p>`
    + section('项目实际金额',group([row('投入','¥5,100.00'),row('收回','¥300.00')]))
    + section('年度类别构成',group([
      '<div class="summary"><strong>摄影</strong><p class="small muted">投入 ¥4,500.00 · 收回 ¥300.00</p><p class="small">净投入 ¥4,200.00</p></div>',
      '<div class="summary"><strong>数码</strong><p class="small muted">投入 ¥600.00 · 收回 ¥0.00</p><p class="small">净投入 ¥600.00</p></div>',
      '<div class="summary"><strong>内容</strong><p class="small">订阅实际付款 ¥120.00</p></div>',
    ])) + section('年度真实金额趋势',group([`<div class="summary"><strong>2026 年</strong><div class="year-bars small">${bars}</div>${textAction('查看该年明细')}</div>`]))
    + section('该年度相关记录',group([makeRow('合成阅读服务','2026-10-18 · 实际付款','¥30.00'),makeRow('合成相机','2026-10-12 · 记录收回','¥300.00')]));
}

function longTerm() {
  return title('长期复盘') + selector(['长期项目','订阅服务'],0)
    + `<div class="sorting flat"><strong>陪伴时间最长</strong><span class="text-action">日均成本最低</span></div>`
    + `<p class="small muted" style="margin:12px 0 8px">共 2 条 · 2 条可排名 · 观察日 2026-11-16</p>`
    + group([
      makeRow('1. 合成相机','摄影 · 持有中 · 日均 ¥35.00','120 天'),
      makeRow('2. 合成阅读器','数码 · 持有中 · 日均 ¥10.00','60 天'),
    ]);
}

function listRecord(name, meta, rate, image) {
  return `<article class="row panel record"><img src="${image}" alt=""><div class="lead"><p class="name">${name}</p><p class="small muted">${meta}</p></div>
  <div class="value"><p class="small muted">当前日均</p>${money(rate)}</div>${icon('next')}</article>`;
}

function projectList() {
  return title('长期项目') + `<p class="overview-meta small">2 条长期项目</p>`
    + listRecord('合成阅读器','数码 · 持有中 · 60 天','¥10.00',reader)
    + listRecord('合成相机','摄影 · 持有中 · 120 天','¥35.00',camera)
    + `<p class="small muted" style="margin-top:12px">已显示 2 条，已到列表末尾</p>`;
}

function project() {
  return title('项目详情',true,'more') + `<img class="cover" src="${camera}" alt=""><div class="identity"><h2>合成相机</h2><p class="muted small">摄影 · 持有中 · 持有 120 天</p></div>`
    + `<section class="metric-stack cost-pair">${metric('日均成本','¥35.00',true,false,true)}${metric('累计投入','¥4,500.00',true,false,true)}</section>`
    + `<div class="disclosure">${group([makeRow('财务明细')])}</div>`
    + section('金额记录 <span class="small muted">2 笔</span>',group([
      makeRow('收回','2026-10-12','¥300.00','',true,'event'),makeRow('投入','2026-07-20','¥4,500.00','',true,'event'),
    ])) + `<div class="disclosure">${group([makeRow('更多资料')])}</div>`;
}

function editor() {
  return title('',true,'') + `<div class="form"><h2 class="form-title">新增长期项目</h2>
    <div class="field">名称<span class="input focus">合成通勤耳机</span></div>
    <div class="field">初始实际投入（元）<span class="input">320.00</span><p class="muted small">0 元也可保存，空白不代表 0。</p></div>
    ${group([makeRow('开始拥有日期','','2026-11-16','date'),makeRow('类别','','数码','project'),
      makeRow('初始实际付款日期','与开始日期相同','2026-11-16','date')])}
    <div>${textAction('金额与日期说明')}<div class="flat">${makeRow('备注（可选）')}</div></div></div>`;
}

function settings(theme) {
  return title('设置',false) + group([makeRow('外观与主题','',themes.find(([key]) => key === theme)[1],'settings')])
    + section('记录管理',group([makeRow('管理类别','','','project'),makeRow('记录时区','','Asia/Shanghai','date',false)]))
    + section('提醒','<p class="muted small">自动提醒将在 V1.1 提供。</p>')
    + section('备份与恢复',group([
      makeRow('导出完整备份','文件未加密，请妥善保管','','backup'),
      makeRow('检查备份文件','仅检查完整性，不覆盖','','info'),
      makeRow('选择完整备份','完整覆盖，不合并；先预览再确认','','review'),
    ]))
    + section('照片空间管理',group([
      makeRow('已登记照片','','2 张','project',false),
      makeRow('当前记录引用','含历史项目与订阅，照片去重','2 张','project',false),
      makeRow('历史未引用照片','','0 张','review',false),
      makeRow('历史照片登记体积','','0 字节','info',false),
      makeRow('预计登记体积','','262144 字节','info',false),
    ]) + '<p class="small muted" style="margin-top:8px">按登记长度估算。未逐张验证，未计临时副本和外部备份。</p><span class="text-action">重新读取</span>')
    + section('说明与关于',group([makeRow('成本计算说明','','','info'),makeRow('关于砺石','','','settings')]));
}

const screenByName = {
  review: {render: review, dock: 'review'}, trend: {render: trend}, annual: {render: annual}, long: {render: longTerm},
  list: {render: projectList, footer: '新增长期项目'}, project: {render: project, footer: '记一笔'},
  editor: {render: editor, footer: '保存项目'}, settings: {render: (_, theme) => settings(theme), dock: 'settings'},
};

function frame([screen, label, theme = 'stone', variant = '', width = 360, scale = 1]) {
  const scene = screenByName[screen];
  return `<figure class="frame"><figcaption>${label}</figcaption>
    <article class="phone screen" data-screen="${screen}" data-theme="${theme}" data-variant="${variant}" data-font="${scale}" style="--width:${width}px;--font:${scale}">
      <div class="statusbar" aria-hidden="true"><span>09:41</span>${icon('system')}</div>
      <div class="scroll">${scene.render(variant,theme)}</div>
      ${scene.footer ? foot(scene.footer,screen === 'list' ? 'plus' : '') : ''}${scene.dock ? dock(scene.dock) : ''}
      <div class="system" aria-hidden="true"></div>
    </article></figure>`;
}

const gallery = {
  'review-themes': ['回看首页 · 三主题', '成本趋势摘要、分析入口和历史档案。', themes.map(([theme,label])=>['review',label,theme])],
  'cost-analysis': ['成本趋势与年度回顾', '同一组合成记录。日均成本与年度实际付款分别呈现。', [['trend','成本趋势'], ['annual','年度回顾','white']]],
  'long-term-states': ['长期复盘与少数据状态', '单个日期以金额明细代替曲线。确认空账本保留现有去首页入口。', [['long','长期复盘'], ['review','只有一个日期','white','single'], ['review','确认空账本','charcoal','empty']]],
  'project-themes': ['项目详情 · 三主题', '保留双成本卡、收起的财务明细和金额记录顺序。', themes.map(([theme,label])=>['project',label,theme])],
  'list-editor': ['长期项目列表与新增项目', '同一视觉系统下，列表适合连续浏览，表单保留日期和金额口径。', [['list','长期项目列表'], ['editor','新增长期项目','white']]],
  'settings-adaptive': ['设置与大字适配', '左为设置默认态，右为宽度 320、模拟 200% 字号的表单。', [['settings','设置首页','stone'], ['editor','320dp · 模拟 200%','charcoal','',320,2]]],
};

const params = new URLSearchParams(location.search);
const key = Object.hasOwn(gallery, params.get('board')) ? params.get('board') : 'review-themes';
const [label, description, original] = gallery[key];
const width = [320,360,400].includes(Number(params.get('width'))) ? Number(params.get('width')) : 0;
const font = params.get('font') === '2' ? 2 : 0;
const frames = original.map(([screen,name,theme,variant,w,s]) => [screen,name,theme,variant,width || w,font || s]);
document.getElementById('boards').innerHTML = Object.entries(gallery).map(([id,[name]]) => `<a href="?board=${id}"${id === key ? ' aria-current="page"' : ''}>${name}</a>`).join('');
document.getElementById('board').innerHTML = `<header class="boardhead"><h1>${label}</h1><p>砺石 LITHIC · V4 R0</p></header>
  <div class="frames" style="--columns:${frames.length}">${frames.map(frame).join('')}</div><p class="caption">${description} 设计预览，金额、日期与图片均为示例素材。</p>`;
