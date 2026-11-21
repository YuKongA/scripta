package top.yukonga.scripta.sandbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.scripta.editor.CodeEditor
import top.yukonga.scripta.editor.EditorColors
import top.yukonga.scripta.editor.EditorLanguage
import top.yukonga.scripta.editor.LineNumberMode
import top.yukonga.scripta.editor.highlight.SyntaxColors
import top.yukonga.scripta.editor.highlight.TokenStyle
import top.yukonga.scripta.editor.rememberSaveableCodeEditorController

/** 演示可选的三套编辑器配色：两套内置预设 + 一套全槽位自定义。 */
private enum class DemoTheme { Dark, Light, Custom }

/**
 * 自定义主题演示：EditorColors 的每个槽位（含 [SyntaxColors] 语法高亮逐类型颜色）全部显式指定，
 * 展示编辑器颜色接口的完整可配性——暖棕底、橙光标/手柄、绿字符串、粉数字。
 */
private val CustomEditorColors = EditorColors(
    background = Color(0xFF282828),
    foreground = Color(0xFFEBDBB2),
    gutterBackground = Color(0xFF32302F),
    gutterForeground = Color(0xFF928374),
    cursor = Color(0xFFFE8019),
    selection = Color(0x66458588),
    handle = Color(0xFFFE8019),
    symbolBarBackground = Color(0xFF3C3836),
    symbolBarForeground = Color(0xFFEBDBB2),
    symbolBarPressed = Color(0x33FFFFFF),
    findMatch = Color(0x4DFABD2F),
    findMatchActive = Color(0x99FE8019),
    currentLine = Color(0xFF3C3836),
    syntax = SyntaxColors(
        comment = TokenStyle(Color(0xFF928374), fontStyle = FontStyle.Italic), // 斜体注释：展示样式槽位不止颜色
        key = TokenStyle(Color(0xFF83A598)),
        string = TokenStyle(Color(0xFFB8BB26)),
        number = TokenStyle(Color(0xFFD3869B)),
        boolean = TokenStyle(Color(0xFFFB4934)),
        nullLike = TokenStyle(Color(0xFFFB4934)),
        keyword = TokenStyle(Color(0xFFFE8019)),
        punctuation = TokenStyle(Color(0xFFA89984)),
        anchor = TokenStyle(Color(0xFF8EC07C)),
        tag = TokenStyle(Color(0xFF8EC07C)),
        directive = TokenStyle(Color(0xFFFE8019)),
    ),
)

@Composable
fun SampleScreen(modifier: Modifier = Modifier) {
    // saveable 版：旋转/进程死亡后文本、选区、脏标记自动恢复（超大文档自动跳过 instance state）。
    val controller = rememberSaveableCodeEditorController(SAMPLE_YAML)
    // 文档经 saveable controller 恢复，语言/文件名这两个文档元数据必须一起持久化——否则进程死亡
    // 恢复后 .kt 内容按 YAML 高亮、标题丢文件名（不能靠 openedName 反推语言：示例路径 name=null 但语言=Yaml）。
    var language by rememberSaveable(
        stateSaver = Saver({ it.name }, { EditorLanguage.valueOf(it) }),
    ) { mutableStateOf(EditorLanguage.Yaml) }
    var wrap by remember { mutableStateOf(false) }
    var readOnly by remember { mutableStateOf(false) }
    var lineNumberMode by remember { mutableStateOf(LineNumberMode.PinnedToScreen) }
    var openedName by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val systemDark = isSystemInDarkTheme()
    var themeOverride by remember { mutableStateOf<DemoTheme?>(null) }
    val theme = themeOverride ?: if (systemDark) DemoTheme.Dark else DemoTheme.Light
    val dark = theme != DemoTheme.Light // 自定义主题为暖深色底，顶栏按深色系取色
    val editorColors = when (theme) {
        DemoTheme.Dark -> EditorColors.Default
        DemoTheme.Light -> EditorColors.Light
        DemoTheme.Custom -> CustomEditorColors
    }

    val cWindow = if (dark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val cBar = if (dark) Color(0xFF2D2D30) else Color(0xFFECECEC)
    val cOpen = if (dark) Color(0xFF56A3F5) else Color(0xFF1A73E8)
    val cSample = if (dark) Color(0xFFE0E0E0) else Color(0xFF3C3C3C)
    val cWrap = if (dark) Color(0xFF6FCF97) else Color(0xFF2E9E5B)
    val cReadOnly = if (dark) Color(0xFFE0A458) else Color(0xFFC77A17)
    val cLineNo = if (dark) Color(0xFFB39DDB) else Color(0xFF7E57C2)
    val cTheme = if (dark) Color(0xFFF48FB1) else Color(0xFFC2185B)
    val cName = if (dark) Color(0xFF858585) else Color(0xFF6B6B6B)
    val cHistory = if (dark) Color(0xFF80CBC4) else Color(0xFF00796B)
    val cHistoryOff = cHistory.copy(alpha = 0.35f)

    val opener = rememberDocumentOpener(
        onOpened = { name, content ->
            errorMessage = null
            openedName = name
            language = languageForName(name)
            controller.setDocument(content)
        },
        onError = { errorMessage = it },
    )
    SyncSystemBarsAppearance(dark)

    Column(
        modifier
            .fillMaxSize()
            .background(cWindow)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(cBar)
                .windowInsetsPadding(
                    WindowInsets.statusBars
                        .union(WindowInsets.captionBar)
                        .only(WindowInsetsSides.Top)
                )
                .padding(8.dp)
        ) {
            // 第一行：文档 / 视图开关（FlexBox 兜底折行，防超窄屏溢出）。
            FlexBox(
                modifier = Modifier.fillMaxWidth(),
                config = { wrap(FlexWrap.Wrap) },
            ) {
                BasicText(
                    text = "  打开  ",
                    style = TextStyle(color = cOpen, fontSize = 13.sp),
                    modifier = Modifier.clickable { opener.open() },
                )
                BasicText(
                    text = "  示例  ",
                    style = TextStyle(color = cSample, fontSize = 13.sp),
                    modifier = Modifier.clickable {
                        controller.setDocument(SAMPLE_YAML)
                        language = EditorLanguage.Yaml
                        openedName = null
                        errorMessage = null
                    },
                )
                BasicText(
                    text = if (wrap) "  换行: 开  " else "  换行: 关  ",
                    style = TextStyle(color = cWrap, fontSize = 13.sp),
                    modifier = Modifier.clickable { wrap = !wrap },
                )
                BasicText(
                    text = if (readOnly) "  只读: 开  " else "  只读: 关  ",
                    style = TextStyle(color = cReadOnly, fontSize = 13.sp),
                    modifier = Modifier.clickable { readOnly = !readOnly },
                )
                BasicText(
                    text = if (lineNumberMode == LineNumberMode.PinnedToScreen) "  行号: 固定  " else "  行号: 跟随  ",
                    style = TextStyle(color = cLineNo, fontSize = 13.sp),
                    modifier = Modifier.clickable {
                        lineNumberMode = if (lineNumberMode == LineNumberMode.PinnedToScreen) {
                            LineNumberMode.PinnedToLine
                        } else {
                            LineNumberMode.PinnedToScreen
                        }
                    },
                )
                BasicText(
                    text = when (theme) {
                        DemoTheme.Dark -> "  主题: 深  "
                        DemoTheme.Light -> "  主题: 浅  "
                        DemoTheme.Custom -> "  主题: 自定义  "
                    },
                    style = TextStyle(color = cTheme, fontSize = 13.sp),
                    modifier = Modifier.clickable {
                        themeOverride = when (theme) {
                            DemoTheme.Dark -> DemoTheme.Light
                            DemoTheme.Light -> DemoTheme.Custom
                            DemoTheme.Custom -> DemoTheme.Dark
                        }
                    },
                )
            }
            // 第二行：编辑动作（撤销 / 重做 / 查找 / 替换 / 跳转）单独一行，行尾右对齐显示打开的文件名或
            // 错误消息——首行 FlowRow 没有「剩余空间」语义，文件名放那儿放不下就被折成独立一行、夹在两行
            // 按钮之间；本行按钮定宽靠左，右侧天然留白，文件名 weight 吸收余宽、超长单行省略，永不折行。
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = "  撤销  ",
                    style = TextStyle(color = if (controller.canUndo) cHistory else cHistoryOff, fontSize = 13.sp),
                    modifier = Modifier.clickable { controller.undo() },
                )
                BasicText(
                    text = "  重做  ",
                    style = TextStyle(color = if (controller.canRedo) cHistory else cHistoryOff, fontSize = 13.sp),
                    modifier = Modifier.clickable { controller.redo() },
                )
                BasicText(
                    text = "  查找  ",
                    style = TextStyle(color = cOpen, fontSize = 13.sp),
                    modifier = Modifier.clickable {
                        if (controller.isFindVisible && !controller.isReplaceVisible) {
                            controller.closeFind()
                        } else {
                            controller.openFind()
                        }
                    },
                )
                BasicText(
                    text = "  替换  ",
                    style = TextStyle(color = cOpen, fontSize = 13.sp),
                    modifier = Modifier.clickable {
                        if (controller.isFindVisible && controller.isReplaceVisible) {
                            controller.closeFind()
                        } else {
                            controller.openReplace()
                        }
                    },
                )
                BasicText(
                    text = "  跳转  ",
                    style = TextStyle(color = cOpen, fontSize = 13.sp),
                    modifier = Modifier.clickable {
                        if (controller.isGotoLineVisible) controller.closeGotoLine() else controller.openGotoLine()
                    },
                )
                (errorMessage ?: openedName)?.let {
                    BasicText(
                        text = it,
                        style = TextStyle(color = cName, fontSize = 13.sp, textAlign = TextAlign.End),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp, end = 6.dp),
                    )
                }
            }
        }
        CodeEditor(
            controller = controller,
            language = language,
            colors = editorColors,
            softWrap = wrap,
            readOnly = readOnly,
            lineNumberMode = lineNumberMode,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}
