"""Sync official Material Symbols Rounded vectors into the shared Kotlin icon set."""
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
import json
import tempfile
import urllib.request
import xml.etree.ElementTree as ET

symbols = {
'Add':'add', 'AddCard':'add_card', 'AddPhotoAlternate':'add_photo_alternate', 'ArrowBack':'arrow_back', 'ArrowForward':'arrow_forward',
'RadioButtonChecked':'radio_button_checked', 'RadioButtonUnchecked':'radio_button_unchecked',
'Backspace':'backspace', 'Check':'check', 'ChevronLeft':'chevron_left', 'ChevronRight':'chevron_right',
'CheckBox':'check_box', 'CheckBoxOutlineBlank':'check_box_outline_blank', 'CloudDownload':'cloud_download',
'Close':'close', 'Code':'code', 'CloudUpload':'cloud_upload', 'DashboardCustomize':'dashboard_customize',
'Delete':'delete', 'DirectionsBus':'directions_bus', 'Edit':'edit', 'EditSquare':'edit_square',
'ArrowDropDown':'arrow_drop_down', 'ExpandMore':'expand_more', 'Home':'home', 'Image':'image', 'ImportExport':'swap_vert', 'Info':'info',
'LocalHospital':'local_hospital', 'MoreHoriz':'more_horiz', 'NorthEast':'north_east', 'Palette':'palette', 'Payments':'payments',
'Schedule':'schedule', 'Restaurant':'restaurant', 'Search':'search', 'ShoppingBag':'shopping_bag', 'SportsEsports':'sports_esports',
'SwapHoriz':'swap_horiz', 'AccountBalance':'account_balance', 'Upload':'upload',
'Undo':'undo', 'Menu':'menu', 'DragIndicator':'drag_indicator', 'ViewCarousel':'view_carousel', 'ViewAgenda':'view_agenda',
'Memory':'memory', 'Contactless':'contactless', 'ShieldPerson':'shield_person',
'Book':'book', 'Paid':'paid', 'Receipt':'receipt', 'BarChart':'bar_chart', 'Wallet':'wallet', 'AccountBalanceWallet':'account_balance_wallet', 'Settings':'settings',
'BookFilled':'book', 'BarChartFilled':'bar_chart', 'AccountBalanceWalletFilled':'account_balance_wallet', 'SettingsFilled':'settings',
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
internal object MaterialSymbols {
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
