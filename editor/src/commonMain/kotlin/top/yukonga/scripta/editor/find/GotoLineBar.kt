package top.yukonga.scripta.editor.find

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.scripta.editor.EditorColors
import top.yukonga.scripta.editor.GotoLineSession

private object DigitsOnlyInputTransformation : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        for (i in length - 1 downTo 0) {
            if (!charAt(i).isDigit()) delete(i, i + 1)
        }
    }
}

/**
 * 停靠式跳转行号条：与查找条同族——嵌在编辑器根 Column 顶部、占布局行，不用 focusable Popup
 * （Android 上会吞浮层外全部触摸，见 [FindReplaceBar]）。输入框只收数字；Enter / 确认跳转并
 * 回焦编辑器，Esc / ✕ 关闭回焦。
 */
@Composable
internal fun GotoLineBar(
    session: GotoLineSession,
    lineCount: Int,
    colors: EditorColors,
    onRequestEditorFocus: () -> Unit,
) {
    if (!session.visible) return
    val fieldFocus = remember { FocusRequester() }

    fun closeAndRefocus() {
        session.close()
        onRequestEditorFocus()
    }

    fun jumpAndRefocus() {
        if (session.jump()) onRequestEditorFocus()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.symbolBarBackground)
            .padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FindField(
            state = session.inputState,
            placeholder = "跳转到行",
            colors = colors,
            modifier = Modifier
                .weight(1f)
                .focusRequester(fieldFocus)
                .onPreviewKeyEvent { ev ->
                    if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (ev.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            jumpAndRefocus(); true
                        }

                        Key.Escape -> {
                            closeAndRefocus(); true
                        }

                        else -> false
                    }
                },
            keyboardType = KeyboardType.Number,
            inputTransformation = DigitsOnlyInputTransformation,
            onImeSearch = { jumpAndRefocus() },
        )
        val counterStyle = remember(colors.symbolBarForeground) {
            TextStyle(color = colors.symbolBarForeground.copy(alpha = 0.75f), fontSize = 12.sp)
        }
        BasicText(
            text = "共 $lineCount 行",
            style = counterStyle,
            maxLines = 1,
        )
        ActionChip("跳转", colors) { jumpAndRefocus() }
        ActionChip("✕", colors) { closeAndRefocus() }
    }
    // 挂载后聚焦输入框（初值为当前行号、挂载态全选：直接键入即覆盖）。
    LaunchedEffect(Unit) { fieldFocus.requestFocus() }
}
