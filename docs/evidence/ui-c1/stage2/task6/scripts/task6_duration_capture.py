from main_capture_helpers import shell,restart,open_list,text,capture
for theme in ('light','dark'):
 shell('cmd','uimode','night','no' if theme=='light' else 'yes');restart();open_list('我的订阅');text('M10 QA Subscription',scroll=True);capture('task6-subscription-detail-refined-'+theme)
shell('cmd','uimode','night','no');restart()
