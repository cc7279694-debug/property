(() => {
  const reader = "电子书阅读器 Paper";
  const reading = "阅读会员";
  const today = "2026-11-12";

  const money = (amount, unit = "") =>
    `<span class="money record-number">¥${amount}${unit ? `<span class="record-unit">${unit}</span>` : ""}</span>`;
  const icon = (name) => `<span class="record-icon icon-${name}" aria-hidden="true"></span>`;
  const heading = (title) =>
    `<header class="topbar record-top"><button type="button" class="back record-back" aria-label="返回">←</button><h2>${title}</h2></header>`;
  const body = (title, content, more = "") =>
    `<div class="record-content ${more}">${heading(title)}${content}</div>`;
  const action = (label, style = "") =>
    `<button type="button" class="button record-action ${style}">${label}</button>`;
  const footer = (label, style = "") => `<div class="record-footer">${action(label, style)}</div>`;
  const buttonText = (label) => `<button type="button" class="record-text-action">${label}</button>`;
  const context = (label, value) =>
    `<div class="record-context"><span>${label}</span><strong>${value}</strong></div>`;
  const title = (label) => `<h3 class="section-title record-group-title">${label}</h3>`;
  const note = (text) => `<p class="note record-note">${text}</p>`;
  const callout = (text, tone = "") => `<p class="record-callout ${tone}">${text}</p>`;
  const choice = (name, options, selected) =>
    `<div class="record-options" role="group" aria-label="${name}">${options.map((option) =>
      `<button type="button" aria-pressed="${option === selected}">${option}</button>`
    ).join("")}</div>`;
  const field = (label, value, type = "", optional = false) =>
    `<div class="record-field ${type === "amount" ? "record-amount-field" : ""}">
      <span class="record-field-label">${label}${optional ? ` <span class="record-optional">（可选）</span>` : ""}</span>
      <div class="record-field-value${value ? "" : " is-placeholder"}">
        ${type === "amount" ? `<span class="record-currency">¥</span>` : ""}
        <span>${value || "添加备注"}</span>${type === "date" ? icon("calendar") : ""}
        ${type === "select" ? `<span class="record-chevron" aria-hidden="true">⌄</span>` : ""}
      </div>
    </div>`;
  const dates = (paid, starts, expires, due) =>
    `<div class="record-date-grid">
      ${field("真实付款日期", paid, "date")}
      ${field("实际生效日期", starts, "date")}
      ${field("付费到期边界", expires, "date")}
      ${field("提醒到期日", due, "date")}
    </div>`;
  const cover = (type) => type === "reader"
    ? `<img class="thumb record-thumb" src="assets/reader.jpg" alt="">`
    : `<span class="thumb no-cover record-thumb">${icon(type === "music" ? "music" : type === "reading" ? "book" : "archive")}</span>`;
  const item = (type, name, meta, duration, amount, label, latestPayment = "") =>
    `<article class="record-item">
      ${cover(type)}
      <div class="record-item-copy"><strong>${name}</strong><span>${meta}</span><span>${duration}</span>${latestPayment ? `<span>最近实付 ${latestPayment}</span>` : ""}</div>
      <div class="record-item-amount" aria-label="${label}，${amount} 元每天">${money(amount, "/天")}</div>
    </article>`;
  const status = (label, value) => `<div class="record-fact"><span>${label}</span><strong>${value}</strong></div>`;
  const modal = (headingText, description, actions) =>
    `<div class="record-shade">
      <div class="dialog record-dialog" role="dialog" aria-label="${headingText}">
        <h3>${headingText}</h3>
        ${description}
        <div class="record-dialog-actions">${actions}</div>
      </div>
    </div>`;

  const projectList = {
    title: "长期项目列表",
    body: body("长期项目", `
      <div class="record-heading"><span>2 项</span><span class="record-column-label">当前日均成本</span></div>
      <div class="record-list">
        ${item("reader", reader, "数码 · 持有中", "持有 120 天", "9.17", "当前日均成本")}
        ${item("archive", "帆布通勤包", "日用 · 持有中", "持有 90 天", "4.00", "当前日均成本")}
      </div>
      ${note("已结束项目可在「回看 → 历史档案」查看。")}
    `, "record-list-page"),
    opts: { footer: footer("＋ 新增长期项目", "record-outline") }
  };

  const projectCreate = {
    title: "新增长期项目",
    body: body("新增长期项目", `
      ${field("项目名称", reader)}
      <div class="record-date-grid">
        ${field("开始拥有日期", "2026-07-16", "date")}
        ${field("初始实际付款日期", "2026-07-16", "date")}
      </div>
      ${field("初始实际投入（元）", "1200.00", "amount")}
      ${note("0 元也可保存。空白不代表 0。")}
      ${field("项目类别", "数码", "select")}
      ${field("备注", "通勤路上阅读。", "", true)}
    `),
    opts: { footer: footer("保存项目") }
  };

  const projectEdit = {
    title: "编辑项目资料",
    body: body("编辑项目资料", `
      ${field("项目名称", reader)}
      ${field("开始拥有日期", "2026-07-16", "date")}
      ${field("项目类别", "数码", "select")}
      ${field("备注", "通勤路上阅读。", "", true)}
      <div class="record-original">${status("初始实际投入", money("1,200.00"))}${note("更正初始金额，请从详情打开原金额记录。")}</div>
    `),
    opts: { footer: footer("保存项目") }
  };

  const moneyEntry = (isOutlay) => ({
    title: isOutlay ? "记录实际投入" : "记录实际收回",
    body: body("记录投入或收回", `
      ${context("记录对象", reader)}
      ${title("金额方向")}
      ${choice("金额方向", ["投入", "收回"], isOutlay ? "投入" : "收回")}
      ${field("实际金额（元）", isOutlay ? "49.00" : "100.00", "amount")}
      ${field("真实发生日期", isOutlay ? today : "2026-10-20", "date")}
      ${note(isOutlay ? "按实际付款日填写。" : "按实际到账日填写。")}
      ${field("说明", isOutlay ? "更换保护套" : "收到部分退款", "", true)}
    `),
    opts: { footer: footer("保存金额记录") }
  });

  const end = {
    title: "结束项目",
    body: body("结束项目", `
      ${context("项目", reader)}
      ${callout("从结束日期起退出当前项目列表，记录仍可在历史档案中查看。")}
      ${field("结束日期", today, "date")}
      ${field("结束原因", "转赠")}
      <label class="record-check"><input type="checkbox"><span>同时记录实际收回</span></label>
      ${note("仅在收到款项后记录，按真实到账日填写。")}
    `),
    opts: { footer: footer("保存结束记录") }
  };

  const projectDetailsBehind = `
    <div class="record-background-detail">
      ${cover("reader")}
      <h3>${reader}</h3><p>持有中 · 120 天</p>
      <div class="record-divider"></div>
      ${status("项目日均成本", money("9.17", "/天"))}
      ${status("实际投入", money("1,200.00"))}
      ${status("实际收回", money("100.00"))}
    </div>`;

  const remove = {
    title: "永久删除项目确认",
    body: body("项目详情", projectDetailsBehind + modal(
      "永久删除项目？",
      `<p class="record-dialog-subject">${reader}</p><p>项目及其金额记录将被删除，历史统计会重新计算。此操作无法在 App 内撤回。</p>`,
      buttonText("取消") + action("确认永久删除", "record-danger")
    ), "record-with-modal")
  };

  const subscriptionList = {
    title: "订阅服务列表",
    body: body("订阅服务", `
      <div class="record-heading"><span>2 项</span><span class="record-column-label">历史平均日成本</span></div>
      <div class="record-list">
        ${item("reading", reading, "阅读 · 订阅中", "折算付费 30 天", "1.00", "历史平均日成本", "2026-11-01")}
        ${item("music", "音乐会员", "影音 · 订阅中", "折算付费 30 天", "0.50", "历史平均日成本", "2026-11-01")}
      </div>
      ${note("暂停或到期服务仍在这里管理。")}
    `, "record-list-page"),
    opts: { footer: footer("＋ 新增订阅", "record-outline") }
  };

  const subscriptionPayment = (mode) => {
    const create = mode === "create";
    const correct = mode === "correct";
    const screenTitle = create ? "新增订阅服务" : correct ? "修正付款" : "记录真实续费";
    const amount = "30.00";
    const paid = create ? "2026-11-01" : correct ? "2026-10-31" : today;
    const starts = create || correct ? "2026-11-01" : "2026-12-01";
    const expires = create || correct ? "2026-12-01" : "2027-01-01";
    const due = expires;
    return {
      title: screenTitle,
      body: body(screenTitle, `
        ${create ? field("服务名称", reading) : context("订阅服务", reading)}
        ${correct ? callout("原付款日 2026-11-01。修正会重算历史，保留原付款身份。") : ""}
        ${field(create ? "首次实际付款金额（元）" : "本次实际付款金额（元）", amount, "amount")}
        ${title("固定付费周期")}
        ${choice("固定付费周期", ["月付", "季付", "年付"], "月付")}
        ${title("付款与覆盖")}
        ${dates(paid, starts, expires, due)}
        ${note(correct ? "此操作不会新增付款或退款。到期边界当天不包含在前一期。"
          : create ? "仅记录实际付款。到期边界当天不包含在前一期。"
          : "12 月 1 日前仍显示当前套餐。到期边界当天不包含在前一期。")}
        ${create ? `<div class="record-date-grid">${field("订阅类别", "阅读", "select")}${field("备注", "", "", true)}</div>` : ""}
        ${buttonText("付款与日期说明")}
      `, "record-payment"),
      opts: { footer: footer(create ? "确认已付款并创建订阅" : correct ? "确认修正付款" : "确认已付款并保存续费") }
    };
  };

  const stateForm = (mode) => {
    const stopping = mode === "stop";
    const restoring = mode === "restore";
    const screenTitle = stopping ? "停止记录" : restoring ? "恢复订阅" : "暂停订阅";
    const current = restoring ? "已暂停" : "订阅中";
    const changed = restoring ? "恢复为启用状态，保留原付款及有效覆盖。"
      : "从生效日起暂停记录，真实付款历史保留。";
    return {
      title: screenTitle,
      body: body(screenTitle, `
        ${context("订阅服务", reading)}
        <div class="record-original">${status("当前状态", current)}${status("已付覆盖边界", "2026-12-01")}</div>
        ${stopping ? `${title("停止方式")}
          ${choice("停止方式", ["到期后停止", "提前停止"], "到期后停止")}
          <div class="record-original">${status("计划停止日期", "2026-12-01")}</div>
          ${note("日期确认后固定，后续付款不会自动修改。")}`
          : `${field("状态生效日期", today, "date")}${note(changed)}`}
        ${callout("仅改变砺石中的记录和统计。请在服务商处管理自动续费或扣费。")}
      `),
      opts: { footer: footer(stopping ? "确认停止记录" : restoring ? "确认恢复" : "确认暂停") }
    };
  };

  const discard = {
    title: "离开有未保存修改的表单",
    body: body("编辑项目资料", `
      ${field("项目名称", reader)}
      ${field("开始拥有日期", "2026-07-16", "date")}
      ${field("项目类别", "数码", "select")}
      ${field("备注", "出门随身带。", "", true)}
      ${modal("放弃未保存修改？", "<p>已保存的项目不会改变。关闭此提示或继续编辑会保留当前输入。</p>",
        buttonText("继续编辑") + action("放弃修改", "record-danger"))}
    `, "record-with-modal")
  };

  const pending = {
    title: "待核对写入",
    body: body("记录真实续费", `
      ${context("订阅服务", reading)}
      <section class="record-state-panel">
        <h3>保存结果尚未确认</h3>
        <p>原输入和付款身份已保留。请核对同一笔付款，暂时不能离开或另录一笔。</p>
      </section>
      <div class="record-original">
        ${status("原提交金额", money("30.00"))}
        ${status("真实付款日期", today)}
        ${status("实际生效日期", "2026-12-01")}
        ${status("付费到期边界", "2027-01-01")}
        ${status("提醒到期日", "2027-01-01")}
      </div>
    `),
    opts: { footer: footer("核对并重试同一付款") }
  };

  const committed = {
    title: "已保存，仅重新读取",
    body: body("记录真实续费", `
      ${context("订阅服务", reading)}
      <section class="record-state-panel record-state-plain">
        <h3>付款已确认保存</h3>
        <p>暂时未取得订阅详情。下方只重新读取保存结果，不会再次记录付款。</p>
      </section>
      <div class="record-original">
        ${status("已确认付款", money("30.00"))}
        ${status("真实付款日期", today)}
        ${status("实际生效日期", "2026-12-01")}
      </div>
    `),
    opts: { footer: footer("重新读取已保存订阅") }
  };

  window.recordBoards = [
    {
      id: "project-archive",
      title: "项目档案与资料",
      note: "同一记录沿用封面、名称与状态，创建和编辑使用同一表单节奏。",
      screens: [projectList, projectCreate, projectEdit]
    },
    {
      id: "project-actions",
      title: "投入、收回、结束与删除",
      note: "金额方向由操作表达。结束保留历史，永久删除需要单独确认。",
      screens: [moneyEntry(true), moneyEntry(false), end, remove]
    },
    {
      id: "subscription-payments",
      title: "订阅档案与付款",
      note: "列表显示历史平均日成本。确认付款时分别核对真实付款、实际生效、到期边界及提醒日期。",
      screens: [subscriptionList, subscriptionPayment("create"), subscriptionPayment("renew"), subscriptionPayment("correct")]
    },
    {
      id: "subscription-lifecycle",
      title: "暂停、恢复与停止",
      note: "状态调整保留原付款事实。恢复画面对应已有有效覆盖的场景。",
      screens: [stateForm("pause"), stateForm("restore"), stateForm("stop")]
    },
    {
      id: "write-guards",
      title: "草稿与写入结果保护",
      note: "放弃草稿、核对同一笔付款与重新读取已确认结果，各有自己的动作。",
      screens: [discard, pending, committed]
    }
  ];
})();
