(() => {
  const icons = {
    back: '<path d="m15 5-7 7 7 7M8 12h12"/>',
    forward: '<path d="m9 6 6 6-6 6"/>',
    down: '<path d="m6 9 6 6 6-6"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/>',
    search: '<circle cx="10.5" cy="10.5" r="5.5"/><path d="m15 15 4.5 4.5"/>',
    book: '<path d="M12 6c-3-2-6-2-9-1v13c3-1 6-1 9 1 3-2 6-2 9-1V5c-3-1-6-1-9 1Zm0 0v13"/>',
    bag: '<path d="M5 8h14l1 12H4L5 8Zm4 0V6a3 3 0 0 1 6 0v2"/>',
    check: '<path d="m5 12 4 4L19 6"/>',
    warn: '<path d="M12 3 2 20h20L12 3Zm0 6v4m0 3h.01"/>',
    archive: '<rect x="4" y="3" width="16" height="5" rx="1"/><path d="M6 8v12h12V8m-8 4h4"/>'
  };

  const icon = (name) => `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${icons[name]}</svg>`;
  const iconButton = (name, label) => `<button type="button" class="as-icon-button" aria-label="${label}">${icon(name)}</button>`;
  const top = (title, { root = false, action = "" } = {}) => `<header class="as-top${root ? " as-root" : ""}">${root ? "" : iconButton("back", "返回")}<h2>${title}</h2>${action}</header>`;
  const panel = (topbar, body, extra = "") => `<section class="as-page ${extra}">${topbar}${body}</section>`;
  const section = (title, action = "") => `<div class="as-section"><h3>${title}</h3>${action}</div>`;
  const action = (label, className = "", attrs = "") => `<button type="button" class="as-action ${className}" ${attrs}>${label}</button>`;
  const choice = (label, attrs = "") => action(`${label} ${icon("down")}`, "as-choice", attrs);
  const headingMoney = (label, amount, perDay = false) => `<div class="as-readout"><span>${label}</span><strong>${amount}${perDay ? "<small> / 天</small>" : ""}</strong></div>`;
  const row = (label, secondary = "", trailing = "", { interactive = false, iconName = "" } = {}) => `<${interactive ? 'button type="button"' : 'div'} class="as-row${interactive ? " as-row-action" : ""}">${iconName ? `<span class="as-row-icon">${icon(iconName)}</span>` : ""}<span class="as-row-label"><strong>${label}</strong>${secondary ? `<small>${secondary}</small>` : ""}</span>${trailing ? `<span class="as-row-value">${trailing}</span>` : ""}${interactive ? `<span class="as-forward">${icon("forward")}</span>` : ""}</${interactive ? "button" : "div"}>`;
  const group = (label, rows) => `${section(label)}<div class="as-rows">${rows}</div>`;
  const toggle = (labels, selected) => `<div class="as-segment" role="group" aria-label="记录类型">${labels.map((label, index) => action(label, "", `aria-pressed="${index === selected}"`)).join("")}</div>`;
  const fact = (label, val) => `<div class="as-fact"><span>${label}</span><strong>${val}</strong></div>`;

  function recordRow(name, metadata, { cover = false, fallback = "book", extra = "" } = {}) {
    return `<button type="button" class="as-record"><span class="as-cover ${cover ? "" : "as-no-cover"}">${cover ? '<img src="./assets/reader.jpg" alt="">' : icon(fallback)}</span><span class="as-record-label"><strong>${name}</strong><small>${metadata}</small></span>${extra ? `<span class="as-record-extra">${extra}</span>` : ""}<span class="as-forward">${icon("forward")}</span></button>`;
  }

  const records = {
    reader: "电子书阅读器 Paper",
    bag: "帆布通勤包",
    reading: "阅读会员",
    music: "音乐会员"
  };

  const trendPoints = [
    { day: "11 月 1 日", value: 16.15 },
    { day: "11 月 4 日", value: 15.71 },
    { day: "11 月 8 日", value: 15.17 },
    { day: "11 月 12 日", value: 14.67 }
  ];

  function trendChart({ short = false } = {}) {
    const top = 17;
    const plotHeight = 108;
    const left = 29;
    const right = 298;
    const scaleX = (offset) => left + offset * (right - left) / 11;
    const scaleY = (value) => top + (1 - value / 18) * plotHeight;
    const dots = trendPoints.map((point, index) => [scaleX([0, 3, 7, 11][index]), scaleY(point.value)]);
    const paths = dots.map(([x, y], index) => `${index === 0 ? "M" : "L"} ${x.toFixed(1)} ${y.toFixed(1)}`).join(" ");
    const [finalX, finalY] = dots.at(-1);
    const ticks = [18, 12, 6, 0].map((value) => {
      const y = scaleY(value);
      return `<text x="3" y="${y + 4}">${value}</text><path class="as-grid${value === 0 ? " as-zero" : ""}" d="M${left} ${y}H${right}"/>`;
    }).join("");
    return `<figure class="as-chart${short ? " as-chart-short" : ""}">
      <figcaption>纵轴 · 元／天</figcaption>
      <svg viewBox="0 0 310 148" role="img" aria-label="总日均成本从 11 月 1 日的 16.15 元每天降至 11 月 12 日的 14.67 元每天。11 月 4 日为 15.71，11 月 8 日为 15.17 元每天。">
        ${ticks}
        <path class="as-marker" d="M${finalX} ${finalY}V125"/>
        <path class="as-trend-path" d="${paths}"/>
        ${dots.map(([x, y]) => `<circle cx="${x}" cy="${y}" r="2.4" class="as-dot"/>`).join("")}
        <circle cx="${finalX}" cy="${finalY}" r="5.3" class="as-selected-dot"/>
        <text x="${left}" y="144">11.01</text><text x="${right}" y="144" text-anchor="end">11.12</text>
      </svg>
    </figure>`;
  }

  const search = {
    title: "搜索结果",
    body: panel(top("搜索", {action: iconButton("info", "名称搜索范围")}), `
      <div class="as-search">${icon("search")}<label><span class="as-sr">按名称搜索项目和订阅</span><input value="阅" type="search" readonly></label></div>
      <div class="as-find-meta"><strong>2 条匹配</strong><span>状态日期 2026-11-12</span></div>
      <div class="as-rows">
        ${recordRow(records.reader, "长期项目 · 数码 · 持有中", {cover:true})}
        ${recordRow(records.reading, "订阅服务 · 阅读 · 订阅中")}
      </div>
      <p class="as-note">搜索范围包括当前与历史记录。</p>`)
  };

  const review = {
    title: "回看",
    opts: {nav: "review"},
    body: panel(top("回看", {root: true, action: iconButton("info", "查看回看口径")}), `
      <div class="as-review-trend">
        ${headingMoney("总日均成本 · 11 月 12 日", "¥14.67", true)}
        ${trendChart({short:true})}
        ${row("查看完整成本趋势", "", "", {interactive:true})}
      </div>
      ${group("分析", row("类别分析", "", "", {interactive:true}) + row("年度回顾", "", "", {interactive:true}) + row("长期复盘", "", "", {interactive:true}))}
      ${group("历史档案", row("已结束项目", "", "", {interactive:true}) + row("已停止订阅", "", "", {interactive:true}))}`)
  };

  const trend = {
    title: "总成本趋势",
    body: panel(top("成本趋势", {action: iconButton("info", "趋势说明")}), `
      <div class="as-date-tools">
        ${iconButton("back", "前一天")}
        ${choice("2026-11-12", 'aria-label="指定观察日期，当前 2026 年 11 月 12 日"')}
        <button type="button" class="as-icon-button" aria-label="后一天" disabled>${icon("forward")}</button>
      </div>
      ${headingMoney("所选日 · 总日均成本", "¥14.67", true)}
      ${trendChart()}
      <div class="as-facts as-facts-two">
        ${fact("项目贡献", "¥13.17／天")}${fact("订阅贡献", "¥1.50／天")}
      </div>
      <p class="as-note">按已录入事实重建 · 非所选日扣款</p>
      <div class="as-rows">
        ${row("显示分项曲线", "", "", {interactive:true})}
        ${row("显示点文字清单", "", "4 个日期", {interactive:true})}
        ${row("相关事件", "", "", {interactive:true})}
      </div>`)
  };

  const category = {
    title: "类别分析",
    body: panel(top("类别分析", {action: iconButton("info", "类别分析口径")}), `
      ${toggle(["长期项目","订阅服务"],0)}
      <div class="as-tools">${choice("数码", 'aria-label="当前类别 数码"')}<span>按当前分类</span></div>
      ${headingMoney("数码 · 当前日均成本贡献", "¥9.17", true)}
      <p class="as-note">结果截至 2026-11-12</p>
      ${section("历史累计真实金额")}
      <div class="as-facts">
        ${fact("投入", "¥1,200.00")}${fact("收回", "¥100.00")}${fact("净投入", "¥1,100.00")}
      </div>
      ${section("各年度真实金额")}
      <div class="as-year-line"><span>2026</span><div class="as-year-bar" aria-hidden="true"><i style="--bar:73.33%"></i></div><strong>¥1,100.00</strong></div>
      <p class="as-note">条形为净投入 · ¥0 至 ¥1,500</p>
      ${section("当前与历史记录")}
      <div class="as-rows">${recordRow(records.reader, "数码 · 持有中", {cover:true})}</div>`)
  };

  const annual = {
    title: "年度回顾",
    body: panel(top("年度回顾", {action: iconButton("info", "年度回顾口径")}), `
      <div class="as-date-tools">
        ${iconButton("back", "上一年度")}
        ${choice("2026 年", 'aria-label="查看年度，当前 2026 年"')}
        <button type="button" class="as-icon-button" aria-label="下一年度" disabled>${icon("forward")}</button>
      </div>
      <p class="as-note as-year-note">按实际资金日期归年 · 截至 2026-11-12</p>
      ${section("长期项目")}
      <div class="as-facts">
        ${fact("投入", "¥1,560.00")}${fact("收回", "¥100.00")}${fact("净投入", "¥1,460.00")}
      </div>
      <div class="as-rows">
        ${row("数码", "", "净投入 ¥1,100.00")}
        ${row("日用", "", "净投入 ¥360.00")}
      </div>
      ${section("订阅服务")}
      ${headingMoney("实际付款", "¥45.00")}
      <div class="as-rows">${row("阅读", "", "¥30.00")}${row("影音", "", "¥15.00")}</div>
      <div class="as-rows">
        ${row("跨年实际金额", "", "", {interactive:true})}
        ${row("查看相关记录", "", "", {interactive:true})}
      </div>`)
  };

  const longTerm = {
    title: "长期复盘",
    body: panel(top("长期复盘", {action: iconButton("info", "长期复盘口径")}), `
      ${toggle(["长期项目","订阅服务"],0)}
      <div class="as-tools">${choice("陪伴时间最长", 'aria-label="排序：陪伴时间最长"')}</div>
      <p class="as-note">观察日 2026-11-12 · 含当前和历史</p>
      <ol class="as-ranking" aria-label="按持有天数排列">
        <li>${recordRow(records.reader, "数码 · 持有中", {cover:true, extra:"120 天"})}</li>
        <li>${recordRow(records.bag, "日用 · 持有中", {fallback:"bag", extra:"90 天"})}</li>
      </ol>
      <p class="as-note">2 条记录可参与排名</p>`)
  };

  const settings = {
    title: "设置首页",
    opts: {nav: "settings"},
    body: panel(top("设置", {root:true, action:iconButton("info", "本地保存与备份说明")}), `
      ${group("外观", row("主题", "", '<span class="as-theme-stone">岩白雾蓝</span><span class="as-theme-white">纯白</span><span class="as-theme-dark">深色黑灰</span>', {interactive:true}))}
      ${group("记录管理", row("管理类别", "", "", {interactive:true}) + row("记录时区", "", "Asia/Shanghai"))}
      ${group("备份与恢复",
        row("导出完整备份", "文件未加密，请妥善保管", "", {interactive:true})
        + row("检查备份文件", "仅检查文件完整性", "", {interactive:true})
        + row("选择完整备份", "先核对备份，再确认完整覆盖", "", {interactive:true}))}
      ${section("照片存储")}
      <details class="as-storage">
        <summary><span>已登记 1 张 · 历史未引用 0 张</span>${icon("down")}</summary>
        <div class="as-rows">
          ${row("当前有引用", "", "1 张")}
          ${row("历史未引用登记体积", "", "0 字节")}
          ${row("预计总登记体积", "", "164,352 字节")}
        </div>
        <p class="as-note">登记体积是估算。取消封面显示会保留旧照片；历史照片可能参与完整备份。目前无法逐张查看或手动清理。</p>
        ${action("重新读取", "as-choice")}
      </details>
      ${group("说明与关于", row("成本计算说明", "", "", {interactive:true}) + row("关于砺石", "", "", {interactive:true}))}
      <p class="as-note">自动提醒将在 V1.1 提供。</p>`, "as-settings")
  };

  const manage = {
    title: "类别管理",
    body: panel(top("管理类别"), `
      ${toggle(["长期项目","订阅服务"],0)}
      <div class="as-tools">${action(icon("plus") + " 新建类别", "as-choice")}</div>
      ${section("长期项目类别")}
      <div class="as-rows as-category-list">
        ${["数码","日用"].map((name) => `<div class="as-category-item">${row(name, "1 条记录 · 含历史")}<div class="as-category-actions">${action("重命名", "", `aria-label="重命名${name}"`)}${action("合并到…", "", `aria-label="将${name}合并到其他类别"`)}</div></div>`).join("")}
        <div class="as-category-item">${row("其他", "默认类别 · 0 条记录（含历史）")}</div>
      </div>
      <p class="as-note">合并前会显示受影响的记录。资金和付款事实保持原值。</p>`)
  };

  const exportScreen = {
    title: "完整备份导出",
    opts: {nav: "settings"},
    body: panel(top("设置", {root:true}), `
      ${section("备份与恢复")}
      <div class="as-inline-task">
        <div class="as-task-title"><span class="as-status-icon">${icon("archive")}</span><h3>导出完整备份</h3></div>
        <p>包含当前账本的记录、已登记图片与业务设置。</p>
        <div class="as-risk-line">${icon("warn")} <span>备份文件未加密，请妥善保管。</span></div>
        ${action("选择保存位置", "as-filled")}
        <p class="as-note">由 Android 打开文件选择器。完整写入并校验通过后显示结果。</p>
      </div>
      <div class="as-rows">
        ${row("检查备份文件", "仅检查文件完整性", "", {interactive:true})}
        ${row("选择完整备份", "先核对备份，再确认完整覆盖", "", {interactive:true})}
      </div>`)
  };

  const inspection = {
    title: "备份检查结果",
    opts: {nav:"settings"},
    body: panel(top("设置", {root:true}), `
      ${section("备份与恢复")}
      ${row("导出完整备份", "文件未加密，请妥善保管", "", {interactive:true})}
      <div class="as-inline-task">
        <h3>检查备份文件</h3>
        <p class="as-note as-file">lithic-2026-11-12.zip</p>
        <div class="as-result" role="status">
          <span class="as-status-icon">${icon("check")}</span>
          <div><strong>完整性校验通过</strong><p>当前账本未改变。</p></div>
        </div>
        <div class="as-check-facts">
          <p>格式 4 · 创建于 2026-11-12 19:30</p>
          <p>物品 2 · 订阅 2 · 照片 1</p>
          <p>照片登记长度 164,352 字节</p>
        </div>
        <p class="as-note">已检查文件结构、关联、长度、校验和及媒体容器。恢复需要单独预览并确认覆盖。</p>
        ${action("检查备份文件", "as-choice", 'aria-label="选择待检查的备份文件"')}
      </div>
      ${row("选择完整备份", "先核对备份，再确认完整覆盖", "", {interactive:true})}`)
  };

  const compareRows = [
    ["长期项目", 2, 1],
    ["项目金额记录", 3, 2],
    ["订阅", 2, 0],
    ["订阅付款", 2, 0],
    ["订阅状态记录", 0, 0],
    ["类别", 6, 6],
    ["图片", 1, 1],
    ["替换关系", 0, 0],
    ["提醒回执", 0, 0],
    ["里程碑抑制水位", 0, 0]
  ];

  const countTable = (items, label = "当前账本与备份内容数量对照") => `<table class="as-compare" aria-label="${label}">
    <thead><tr><th scope="col">内容</th><th scope="col">当前</th><th scope="col">备份</th></tr></thead>
    <tbody>${items.map(([name, current, backup]) => `<tr><th scope="row">${name}</th><td>${current}</td><td>${backup}</td></tr>`).join("")}</tbody>
  </table>`;

  const restoreBody = panel(top("设置", {root:true}), `
    ${section("核对备份")}
    <p class="as-file">lithic-2026-10-31.zip</p>
    <p class="as-note">创建于 2026-10-31 19:30 · Asia/Shanghai</p>
    ${countTable(compareRows)}
    <p class="as-note">当前数量为校验时快照。账本发生变化后需重新选择备份。</p>
      <section class="as-impact" aria-labelledby="as-restore-impact">
        <div class="as-task-title">${icon("warn")}<h3 id="as-restore-impact">完整覆盖当前账本</h3></div>
        <p>将从 lithic-2026-10-31.zip 恢复。</p>
        <p>当前记录、图片和业务设置将被备份内容替换。备份中没有的记录无法保留。</p>
        <p>如需保留当前内容，请先取消并导出当前完整备份。</p>
        <p class="as-note">恢复后按当日重新计算天数和状态。系统通知权限不随备份恢复。</p>
      </section>
      <div class="as-confirm-actions">
        ${action("取消恢复，保留当前记录", "as-choice")}
        ${action("确认覆盖恢复", "as-danger")}
      </div>
`);
  const restorePreview = {title:"恢复预览", opts:{nav:"settings"}, body:restoreBody};
  const restoreConfirm = {title:"覆盖确认", opts:{nav:"settings", startAt:".as-impact"}, body:restoreBody};
  window.analysisBoards = [
    {id:"search-and-review", title:"搜索与回看", note:"保持名称和档案身份。分析图表从日期与金额起读，细项留在曲线之后。", screens:[search,review,trend,category]},
    {id:"review-and-settings", title:"年度、复盘与设置", note:"项目与订阅保持独立金额口径。设置用分组和明确动作表达层级。", screens:[annual,longTerm,settings,manage]},
    {id:"backup-and-restore", title:"备份、检查与恢复", note:"四帧对应设置页内状态。前两帧展示独立操作，后两帧是同一次核对的两个滚动位置，只有一次明确覆盖确认。", screens:[exportScreen,inspection,restorePreview,restoreConfirm]}
  ];
})();
