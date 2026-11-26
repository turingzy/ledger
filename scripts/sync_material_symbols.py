"""Sync official Material Symbols Rounded vectors into the shared Kotlin icon set."""
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
import json
import tempfile
import urllib.request
import xml.etree.ElementTree as ET

symbols = {
'ArrowBack':'arrow_back', 'Add':'add', 'AddCard':'add_card', 'AddPhotoAlternate':'add_photo_alternate', 'ArrowUpward':'arrow_upward',
'RadioButtonChecked':'radio_button_checked', 'RadioButtonUnchecked':'radio_button_unchecked',
'Backspace':'backspace', 'Check':'check', 'ChevronLeft':'chevron_left', 'ChevronRight':'chevron_right',
'CheckBox':'check_box', 'CheckBoxOutlineBlank':'check_box_outline_blank',
'Close':'close', 'Code':'code', 'CloudUpload':'cloud_upload', 'DashboardCustomize':'dashboard_customize',
'Delete':'delete', 'DirectionsBus':'directions_bus', 'EditSquare':'edit_square', 'FileCopy':'file_copy',
'ArrowDropDown':'arrow_drop_down', 'ArrowDropUp':'arrow_drop_up', 'ExpandMore':'expand_more', 'Home':'home', 'ImportExport':'swap_vert', 'Info':'info',
'LocalHospital':'local_hospital', 'MoreHoriz':'more_horiz', 'NorthEast':'north_east', 'Palette':'palette', 'Payments':'payments',
'Schedule':'schedule', 'Restaurant':'restaurant', 'Search':'search', 'ShoppingBag':'shopping_bag', 'SportsEsports':'sports_esports',
'SwapHoriz':'swap_horiz', 'AccountBalance':'account_balance',
'Undo':'undo',
'Memory':'memory', 'Contactless':'contactless', 'ShieldPerson':'shield_person',
'Visibility':'visibility', 'VisibilityOff':'visibility_off',
'TrendingUp':'trending_up', 'TrendingDown':'trending_down',
'Leaderboard':'leaderboard', 'LeaderboardFilled':'leaderboard', 'HomeFilled':'home', 'CreditCard':'credit_card', 'CreditCardFilled':'credit_card', 'DateRange':'date_range', 'CalendarMonth':'calendar_month', 'Tune':'tune',
'AccountBalanceWallet':'account_balance_wallet', 'Settings':'settings',
'SettingsFilled':'settings',
# Built-in expense and income category icons.
'Grocery':'grocery', 'LocalCafe':'local_cafe', 'ShoppingCart':'shopping_cart', 'Key':'key',
'Bolt':'bolt', 'Wifi':'wifi', 'School':'school', 'FitnessCenter':'fitness_center', 'Pets':'pets',
'FlightTakeoff':'flight_takeoff', 'Build':'build', 'LocalGasStation':'local_gas_station',
'Checkroom':'checkroom', 'Spa':'spa', 'VerifiedUser':'verified_user', 'Redeem':'redeem',
'MilitaryTech':'military_tech', 'Celebration':'celebration', 'MoreTime':'more_time', 'Work':'work',
'Storefront':'storefront', 'VolunteerActivism':'volunteer_activism', 'ReceiptLong':'receipt_long',
'AssignmentReturn':'assignment_return', 'Percent':'percent', 'Mail':'mail', 'Apartment':'apartment',
'Savings':'savings', 'PieChart':'pie_chart', 'Sell':'sell', 'EditNote':'edit_note', 'Elderly':'elderly',
'AddCircle':'add_circle',
}
ROOT = Path(__file__).resolve().parents[1]
cache = Path(tempfile.gettempdir()) / 'ledger-material-symbols'
cache.mkdir(exist_ok=True)
def fetch(item):
    name, slug=item
    filename=slug+('_fill1' if name.endswith('Filled') else '')+'_24px.svg'
    url=f'https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/{slug}/materialsymbolsrounded/{filename}'
    target=cache/(name+'.svg')
    if target.exists():
        data = target.read_bytes()
    else:
        with urllib.request.urlopen(url, timeout=40) as response:
            data=response.read()
    root=ET.fromstring(data)
    assert root.attrib['viewBox']=='0 -960 960 960', (name,root.attrib)
    paths=[node.attrib['d'] for node in root if node.tag.endswith('path')]
    assert paths, name
    target.write_bytes(data)
    return name, {'symbol':slug, 'source':url, 'paths':paths, 'mirrored':name in {'ArrowBack','ArrowForward','ChevronLeft','ChevronRight','Backspace','Undo'}}
results={};errors=[]
with ThreadPoolExecutor(max_workers=6) as executor:
    tasks={executor.submit(fetch,item):item[0] for item in symbols.items()}
    for task in as_completed(tasks):
        try:
            name,data=task.result();results[name]=data;print('Fetched',name,flush=True)
        except Exception as error:
            errors.append((tasks[task],str(error)));print('FAILED',tasks[task],str(error),flush=True)
if errors: raise SystemExit(str(errors))
(cache/'manifest.json').write_text(json.dumps(results,indent=2))
print('READY',len(results),flush=True)

header = """package cn.turingzy.ledger.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// Generated from Google Material Symbols Rounded by scripts/sync_material_symbols.py.
// Weight 400, grade 0, optical size 24. Apache-2.0; see assets/licenses/material-symbols.txt.
/**
 * 共享的 Material Symbols Rounded 矢量图标集合。
 *
 * 集中管理应用内统一使用的 Google 官方 Material Symbols 图标资源（规格为 Weight 400、Grade 0、Optical Size 24dp）。
 * 所有图标均通过 [lazy] 委托按需延迟加载解析为 [ImageVector]。
 */
internal object MaterialSymbols {
    /**
     * 根据 SVG 路径字符串构造 [ImageVector] 矢量图。
     *
     * @param name 图标名称，用于无障碍或调试标识。
     * @param path SVG 路径字符串数据。
     * @param autoMirror 在从右到左 (RTL) 布局环境中是否自动镜像翻转。
     * @return 构造完成的 [ImageVector] 实例。
     */
    private fun symbol(name: String, path: String, autoMirror: Boolean = false): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
            autoMirror = autoMirror,
        ).addGroup(translationY = 960f)
            .addPath(pathData = PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
            .clearGroup().build()
"""
parts = [header]
for name in sorted(results):
    item = results[name]
    paths = " ".join(item['paths'])
    mirror = ", autoMirror = true" if item['mirrored'] else ""
    parts.append(f'    val {name}: ImageVector by lazy {{\n        symbol("{name}", "{paths}"{mirror})\n    }}\n')
parts.append('}\n')
output = ROOT / 'app/src/main/kotlin/cn/turingzy/ledger/ui/component/MaterialSymbols.kt'
output.write_text('\n'.join(parts))
print('Generated',output,flush=True)
