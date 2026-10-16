from main_capture_helpers import *
import json
expected=json.loads((O/'final-apk.json').read_text(encoding='utf8'))['sha256']
package=shell('pm','path','com.cooper.lithic.debug').split('package:')[1].splitlines()[0]
assert shell('sha256sum',package).split()[0]==expected
for theme in ('light','dark'):
 shell('cmd','uimode','night','no' if theme=='light' else 'yes');restart()
 capture('final-now-'+theme)
 open_list('我的长期项目');capture('final-project-list-'+theme)
 text('M10 QA Recovery Project',scroll=True);capture('final-project-detail-'+theme)
 restart();open_list('我的订阅');capture('final-subscription-list-'+theme)
 text('M10 QA Subscription',scroll=True);capture('final-subscription-detail-'+theme)
 restart();text('回看');capture('final-review-'+theme)
 text('历史档案');capture('final-project-history-'+theme)
 desc('返回回看');text('已停止订阅');capture('final-subscription-history-'+theme)
 desc('返回回看');text('设置');capture('final-settings-'+theme)
 shell('input','swipe','540','1450','540','700','450');capture('final-settings-about-'+theme)
 restart();desc('新增记录');capture('final-add-menu-'+theme)
shell('cmd','uimode','night','no');restart()
