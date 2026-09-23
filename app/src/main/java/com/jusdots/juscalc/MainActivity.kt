package com.jusdots.juscalc

import android.os.Bundle
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.core.content.edit
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.core.view.WindowCompat
import com.jusdots.juscalc.ui.theme.Geist
import com.jusdots.juscalc.ui.theme.JusCalcTheme
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.UnevenRoundedRectangle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Figma source of truth: black 1E1E1E, output card 45, small cards 20,
// bottom outer corners 45, tight few-px gaps, Geist.
private val Ink = Color(0xFF1E1E1E)
private val Frame = Color(0xFF1E1E1E)

// Figma geometry via Kyant0/Shapes: display and key panels are G2 rounded
// rectangles; the two bottom slide pills are capsules.
private val DisplayShape = RoundedRectangle(45.dp)
private val PanelShape = RoundedRectangle(20.dp)
private val BottomLeftPanelShape = UnevenRoundedRectangle(20.dp, 20.dp, 20.dp, 45.dp)
private val BottomRightPanelShape = UnevenRoundedRectangle(20.dp, 20.dp, 45.dp, 20.dp)
private val PillShape = Capsule()

// One physical language everywhere: critically-damped springs (no bounce),
// finger-following drags, short fades. Nothing bouncy, glowing or particle-y.
private fun <T> settleSpec() = spring<T>(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)
private fun <T> snapSpec() = spring<T>(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy)

private data class CalcTheme(val name: String, val main: Color)
private val Themes = listOf(
    CalcTheme("Olive", Color(0xFF748067)),
    CalcTheme("Cherry", Color(0xFFE85D75)),
    CalcTheme("Vanilla", Color(0xFFFDF4D2)),
    CalcTheme("Violet", Color(0xFFA290B7)),
    CalcTheme("Turquoise", Color(0xFF2DEEE1)),
    CalcTheme("Lavender", Color(0xFFEA9EFF)),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Fullscreen black frame: keep system bars legible on the dark background.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent { JusCalcTheme { JusCalcApp() } }
    }
}

@Composable
fun JusCalcApp() {
    var expr by remember { mutableStateOf("") }
    var resultPlain by remember { mutableStateOf<String?>(null) }
    var resultErr by remember { mutableStateOf(false) }
    var afterEquals by remember { mutableStateOf(false) }
    var themeIdx by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var dotsCount by remember { mutableIntStateOf(0) }
    var showHistory by remember { mutableStateOf(false) }
    // History survives restarts: one tab-separated "expr\tresult" line each.
    // Neither side ever contains a tab or newline, so no escaping needed.
    val appContext = LocalContext.current
    val prefs = remember { appContext.getSharedPreferences("juscalc", Context.MODE_PRIVATE) }
    val history = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            prefs.getString("history", null)?.lines()?.forEach { line ->
                val tab = line.indexOf('\t')
                if (tab > 0 && size < 30) add(line.substring(0, tab) to line.substring(tab + 1))
            }
        }
    }
    fun saveHistory() {
        prefs.edit {
            putString("history",
                history.take(30).joinToString("\n") { "${it.first}\t${it.second}" })
        }
    }
    BackHandler(enabled = showSettings || showHistory) {
        if (showSettings) showSettings = false else showHistory = false
    }
    var dotsMode by remember { mutableStateOf(false) }
    val theme = Themes[themeIdx % Themes.size]
    val haptics = LocalHapticFeedback.current

    fun append(s: String) {
        if (afterEquals) { expr = s; resultPlain = null; resultErr = false; afterEquals = false }
        else if (expr.length < 28) expr += s
    }
    fun appendDot() {
        if (afterEquals) { expr = "0."; resultPlain = null; resultErr = false; afterEquals = false; return }
        val seg = expr.takeLastWhile { it.isDigit() || it == '.' }
        if ('.' in seg) return
        expr += if (seg.isEmpty()) "0." else "."
    }
    fun appendOp(c: Char) {
        val r = resultPlain
        if (afterEquals && r != null && !resultErr) { expr = r + c; resultPlain = null; afterEquals = false; return }
        afterEquals = false; resultPlain = null
        if (expr.isEmpty()) { if (c == '-') expr = "-" ; return }
        val last = expr.last()
        expr = when {
            last in "+-*/×÷xX" -> expr.dropLast(1) + c
            last == '(' -> if (c == '-') expr + c else expr
            last == '.' -> expr + "0" + c
            else -> expr + c
        }
    }
    fun appendParen(p: Char) {
        if (afterEquals) { if (p == '(') { expr = "("; resultPlain = null; afterEquals = false } ; return }
        if (p == '(' && (expr.lastOrNull()?.isDigit() == true || expr.lastOrNull() == ')')) expr += "×("
        else expr += p
    }
    fun smartParen() {
        val open = expr.count { it == '(' }; val close = expr.count { it == ')' }
        val last = expr.lastOrNull()
        appendParen(if (last?.isDigit() == true || last == ')' && open > close) ')' else '(')
    }

    Box(Modifier.fillMaxSize().background(Frame)) {
    Column(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Display(
            expr = expr, resultPlain = resultPlain, isErr = resultErr, afterEquals = afterEquals,
            panel = theme.main, themeName = theme.name,
            onOpenSettings = { showSettings = true },
            onPullDown = { showHistory = true },
            onPullUp = { showHistory = false },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        AnimatedVisibility(showHistory,
            enter = fadeIn(tween(180)) + expandVertically(snapSpec(), expandFrom = Alignment.Top),
            exit = fadeOut(tween(150)) + shrinkVertically(snapSpec(), shrinkTowards = Alignment.Top)) {
            HistoryPanel(history, onPick = { r ->
                expr = r; resultPlain = null; resultErr = false; afterEquals = false
                showHistory = false
            }, onClear = { history.clear(); saveHistory() }, panel = theme.main)
        }
        Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (dotsMode) DotsBar(0, dotsCount,
                onCount = { c -> if (c != dotsCount) { dotsCount = c; haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } },
                onCommit = { d -> append(d.toString()); dotsCount = 0 },
                theme.main, Modifier.weight(1f).fillMaxHeight(), "top row, dots 1 to 5")
            else NumberDialBar(listOf("5","6","7","8","9"), 1, theme.main, { append(it) }, Modifier.weight(1f).fillMaxHeight(), "digits 5 to 9")
            ActionCell(theme.main, Modifier.size(92.dp)) {
                ParenPad(theme.main, onParen = ::appendParen, onSmart = ::smartParen)
            }
        }
        Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (dotsMode) DotsBar(5, dotsCount,
                onCount = { c -> if (c != dotsCount) { dotsCount = c; haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) } },
                onCommit = { d -> append(d.toString()); dotsCount = 0 },
                theme.main, Modifier.weight(1f).fillMaxHeight(), "bottom row, dots 6 to 10")
            else NumberDialBar(listOf("0","1","2","3","4"), 2, theme.main, { append(it) }, Modifier.weight(1f).fillMaxHeight(), "digits 0 to 4")
            ActionCell(theme.main, Modifier.size(92.dp)) {
                OpPad(theme.main, onOp = ::appendOp)
            }
        }
        Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().clip(BottomLeftPanelShape).background(theme.main).padding(6.dp)) {
                DualPill("AC", "•", theme.main, onLeft = { expr = ""; resultPlain = null; resultErr = false; afterEquals = false }, onRight = ::appendDot, leftDesc = "clear all", rightDesc = "decimal point")
            }
            Box(Modifier.weight(1f).fillMaxHeight().clip(BottomRightPanelShape).background(theme.main).padding(6.dp)) {
                DualPill("Del", "=", theme.main,
                    onLeft = {
                        if (afterEquals && resultPlain != null && !resultErr) { expr = resultPlain!!; resultPlain = null; afterEquals = false }
                        else if (expr.isNotEmpty()) expr = expr.dropLast(1)
                    },
                    onRight = {
                        if (expr.isBlank()) return@DualPill
                        when (val r = CalcEngine.evaluate(expr)) {
                            is CalcEngine.Eval.Ok -> {
                        val f = CalcEngine.formatResult(r.value)
                        resultPlain = f; resultErr = false; afterEquals = true
                        history.add(0, expr to f)
                        if (history.size > 30) history.removeAt(history.size - 1)
                        saveHistory()
                    }
                            is CalcEngine.Eval.Err -> { resultPlain = null; resultErr = true; afterEquals = true }
                        }
                    }, leftDesc = "delete", rightDesc = "equals")
            }
        }
    }
    if (showSettings) {
        SettingsSheet(selected = themeIdx % Themes.size,
            onSelect = { themeIdx = it % Themes.size },
            dotsMode = dotsMode,
            onToggleDots = { dotsMode = !dotsMode },
            onDismiss = { showSettings = false })
    }
    }
}

@Composable
private fun ActionCell(panel: Color, modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.clip(PanelShape).background(panel), contentAlignment = Alignment.Center) { content() }
}

// ---- display: each token its own right-aligned line; new lines fade/slide in,
// result crossfades instead of popping ----
@Composable
private fun Display(expr: String, resultPlain: String?, isErr: Boolean, afterEquals: Boolean, panel: Color, themeName: String, onOpenSettings: () -> Unit, onPullDown: () -> Unit, onPullUp: () -> Unit, modifier: Modifier) {
    val scroll = rememberScrollState()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val resultKey = if (afterEquals) resultPlain ?: "ERR" else ""
    // No animateContentSize here: it animates the frame while scrolled content
    // overflows unclipped, painting the result over the expression. The scroll
    // snaps to the bottom instead, so the fresh result is always visible.
    // One frame delay: maxValue is only valid after the new content is laid out.
    LaunchedEffect(expr, resultKey) {
        withFrameNanos { }
        scroll.scrollTo(scroll.maxValue)
    }
    Box(modifier.clip(DisplayShape).background(panel)
        .pointerInput(Unit) {
            // Pull down to reveal history, push up to hide it. The token list
            // sits snapped at the bottom, so downward drags always reach here.
            var total = 0f
            detectVerticalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = {
                    if (total > 48.dp.toPx()) onPullDown()
                    else if (total < -48.dp.toPx()) onPullUp()
                    total = 0f
                },
                onDragCancel = { total = 0f }
            ) { _, d -> total += d }
        }
        .combinedClickable(
            onClick = {
                // Tap copies what's showing: the result digits after equals,
                // otherwise the expression being typed.
                val text = if (afterEquals && resultPlain != null && !isErr) resultPlain
                    else if (expr.isNotEmpty()) expr else return@combinedClickable
                context.getSystemService(ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("JusCalc", text))
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
            },
            onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onOpenSettings() })
        .semantics { contentDescription = "result display, $themeName. tap to copy, hold to open settings" }
        .padding(20.dp), contentAlignment = Alignment.BottomEnd) {
        Column(Modifier.fillMaxWidth().verticalScroll(scroll),
            horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (expr.isEmpty() && resultKey.isEmpty()) {
                Text("0", color = Ink.copy(alpha = 0.45f), fontSize = 40.sp, fontFamily = Geist, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
            } else {
                displayTokens(expr).forEachIndexed { i, (text, isOp) ->
                    key(i, text) {
                        AnimatedVisibility(visible = true,
                            enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { it / 3 },
                            label = "tokenIn") {
                            Text(text, color = Ink, fontSize = if (isOp) 24.sp else 38.sp,
                                fontFamily = Geist, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, lineHeight = if (isOp) 28.sp else 42.sp)
                        }
                    }
                }
                AnimatedContent(targetState = resultKey, label = "result",
                    transitionSpec = {
                        (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 3 }) togetherWith fadeOut(tween(150))
                    }) { k ->
                    if (k.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("=", color = Ink, fontSize = 24.sp, fontFamily = Geist, fontWeight = FontWeight.Medium)
                            Text(if (k == "ERR") "ERROR" else CalcEngine.numberToCompact(k),
                                color = Ink, fontSize = 46.sp, fontFamily = Geist, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, lineHeight = 52.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---- history: pulled down from under the display. Tapping an entry reloads
// its result; results persist in SharedPreferences across restarts. ----
@Composable
private fun HistoryPanel(entries: List<Pair<String, String>>, onPick: (String) -> Unit, onClear: () -> Unit, panel: Color) {
    Column(Modifier.fillMaxWidth().clip(PanelShape).background(panel).padding(14.dp)
        .heightIn(max = 264.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("History", color = Ink, fontSize = 18.sp, fontFamily = Geist,
                fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            if (entries.isNotEmpty()) {
                Text("Clear", color = Ink.copy(alpha = 0.6f), fontSize = 14.sp,
                    fontFamily = Geist, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onClear() }.padding(6.dp))
            }
        }
        if (entries.isEmpty()) {
            Text("Nothing yet — results land here", color = Ink.copy(alpha = 0.45f),
                fontSize = 14.sp, fontFamily = Geist, fontWeight = FontWeight.Medium)
        } else {
            entries.forEach { (e, r) ->
                Box(Modifier.fillMaxWidth().clip(PanelShape)
                    .background(Ink.copy(alpha = 0.08f))
                    .clickable { onPick(r) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("$e = ${CalcEngine.numberToCompact(r)}", color = Ink, fontSize = 17.sp,
                        fontFamily = Geist, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ---- settings: custom bottom sheet, no scrim. The panel floats over the
// calculator and throws a soft shadow upward onto it (the mock's little
// shadow). Slides up on open, drag down or tap outside to dismiss. ----
@Composable
private fun SettingsSheet(selected: Int, onSelect: (Int) -> Unit, dotsMode: Boolean, onToggleDots: () -> Unit, onDismiss: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(2400f) }
    var sheetH by remember { mutableFloatStateOf(1f) }
    val shadow = with(density) { 24.dp.toPx() }
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    fun close() {
        scope.launch {
            offsetY.animateTo(maxOf(sheetH, offsetY.value), snapSpec())
            onDismiss()
        }
    }
    LaunchedEffect(sheetH) { offsetY.animateTo(0f, settleSpec()) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = ::close))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .onSizeChanged { sheetH = it.height.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                translationY = offsetY.value
                shadowElevation = shadow
                shape = sheetShape
                clip = false
            }
            .pointerInput(sheetH) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY.value > sheetH * 0.35f) close()
                        else scope.launch { offsetY.animateTo(0f, settleSpec()) }
                    },
                    onDragCancel = { scope.launch { offsetY.animateTo(0f, settleSpec()) } }
                ) { _, d -> scope.launch { offsetY.snapTo((offsetY.value + d).coerceAtLeast(0f)) } }
            }
            .clip(sheetShape).background(Frame)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(12.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp, bottom = 4.dp)
                    .size(width = 48.dp, height = 5.dp)
                    .clip(CircleShape).background(Color.White))
                Themes.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { t ->
                            val i = Themes.indexOf(t)
                            val sel = i == selected
                            Box(Modifier.weight(1f).aspectRatio(1f)
                                .graphicsLayer { alpha = if (sel) 1f else 0.45f }
                                .clip(PanelShape)
                                .border(2.dp, if (sel) Color.White else Color.Transparent, PanelShape)
                                .background(t.main)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(i)
                                }
                                .padding(12.dp),
                                contentAlignment = Alignment.BottomStart) {
                                Text(t.name, color = Ink, fontSize = 22.sp,
                                    fontFamily = Geist, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth()
                    .clip(PanelShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onToggleDots() }
                    .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Number dots", color = Color.White, fontSize = 18.sp,
                            fontFamily = Geist, fontWeight = FontWeight.Medium)
                        Text("Slide across dots to enter digits", color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp, fontFamily = Geist, fontWeight = FontWeight.Medium)
                    }
                    Switch(checked = dotsMode, onCheckedChange = { onToggleDots() })
                }
                Box(Modifier.fillMaxWidth()
                    .clip(PanelShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        uriHandler.openUri("https://github.com/shubh72010/JusCalc")
                    }
                    .padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("GitHub", color = Color.White, fontSize = 18.sp,
                                fontFamily = Geist, fontWeight = FontWeight.Medium)
                            Text("shubh72010/JusCalc", color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp, fontFamily = Geist, fontWeight = FontWeight.Medium)
                        }
                        Text("›", color = Color.White.copy(alpha = 0.6f), fontSize = 24.sp,
                            fontFamily = Geist, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

private fun displayTokens(expr: String): List<Pair<String, Boolean>> {
    if (expr.isEmpty()) return emptyList()
    val out = mutableListOf<Pair<String, Boolean>>()
    var i = 0
    val prettyOp = mapOf('+' to "+", '-' to "−", '*' to "×", 'x' to "×", 'X' to "×", '×' to "×", '/' to "÷", '÷' to "÷")
    while (i < expr.length) {
        val c = expr[i]
        when {
            c.isDigit() || c == '.' || (c == '-' && (i == 0 || expr[i-1] in "+-*/×÷xX(") && i+1 < expr.length && (expr[i+1].isDigit() || expr[i+1]=='.')) -> {
                var j = i + 1
                while (j < expr.length && (expr[j].isDigit() || expr[j]=='.')) j++
                out += CalcEngine.numberToCompact(expr.substring(i, j)) to false
                i = j
            }
            c in prettyOp -> { out += (prettyOp[c]!! to true); i++ }
            c == '(' || c == ')' -> { out += (c.toString() to true); i++ }
            else -> i++
        }
    }
    return out
}

// ---- rotary dial: selection chases the finger (drag right = lower digit,
// the left neighbour slides into the middle), with a slight lean toward the
// drag and detent ticks; springs home on release. Every pick (tap or drag)
// jumps the dial back to its default digit. ----
@Composable
private fun NumberDialBar(digits: List<String>, defaultCenter: Int, panel: Color, onPick: (String) -> Unit, modifier: Modifier, desc: String) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var center by remember { mutableIntStateOf(defaultCenter) }
    val offset = remember { Animatable(0f) }
    var lastShown by remember { mutableIntStateOf(defaultCenter) }
    var settling by remember { mutableStateOf(false) }
    val stepPx = with(density) { 64.dp.toPx() }.coerceAtLeast(1f)
    val tapSlopPx = with(density) { 16.dp.toPx() }
    var barW by remember { mutableFloatStateOf(0f) }
    Box(modifier.clip(PanelShape).background(panel)
        .semantics { contentDescription = "number dial $desc, drag to choose" }
        .onSizeChanged { barW = it.width.toFloat() }
        .pointerInput(center, stepPx, barW) {
            var total = 0f
            var upX = -1f
            // A tap with finger jitter crosses touch slop, which cancels the
            // digit's click and lands here as a tiny drag. Resolving it by
            // release position instead of center stops the "mistypes".
            fun tapPick(x: Float) {
                if (barW <= 0f) return
                val startShown = (center - (offset.value / stepPx).roundToInt()).coerceIn(digits.indices)
                val p = (startShown + ((x / barW) * 3).toInt().coerceIn(0, 2) - 1).coerceIn(digits.indices)
                if (digits.getOrNull(p) == null) return
                onPick(digits[p]); center = defaultCenter; lastShown = defaultCenter
            }
            detectHorizontalDragGestures(
                onDragStart = { total = 0f; upX = -1f },
                onDragEnd = {
                    if (kotlin.math.abs(total) < tapSlopPx && upX >= 0f) tapPick(upX)
                    else {
                        val picked = (center - (offset.value / stepPx).roundToInt()).coerceIn(digits.indices)
                        onPick(digits[picked])
                        center = defaultCenter; lastShown = defaultCenter
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    settling = true
                    scope.launch { offset.animateTo(0f, settleSpec()); settling = false }
                },
                onDragCancel = {
                    settling = true
                    scope.launch { offset.animateTo(0f, settleSpec()); settling = false }
                }
            ) { change, d -> total += d; upX = change.position.x; scope.launch { offset.snapTo(offset.value + d) } }
        }) {
        val shown = (center - (offset.value / stepPx).roundToInt()).coerceIn(digits.indices)
        if (shown != lastShown) {
            lastShown = shown
            if (!settling) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        Row(Modifier.fillMaxSize().graphicsLayer { translationX = (offset.value * 0.15f).coerceIn(-48f, 48f) },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            DialDigit(digits.getOrNull(shown - 1), false) {
                val p = (shown - 1).coerceIn(digits.indices)
                onPick(digits[p]); center = defaultCenter; lastShown = defaultCenter
            }
            DialDigit(digits[shown], true) { onPick(digits[shown]) }
            DialDigit(digits.getOrNull(shown + 1), false) {
                val p = (shown + 1).coerceIn(digits.indices)
                onPick(digits[p]); center = defaultCenter; lastShown = defaultCenter
            }
        }
    }
}

@Composable
private fun DialDigit(text: String?, centered: Boolean, onTap: () -> Unit) {
    // No ripple: the dial's drag detector owns the press, a bounded indication
    // would freeze mid-swipe. Detent haptics + the result are the feedback.
    Box(Modifier.widthIn(min = 72.dp)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
            enabled = text != null, onClick = onTap).padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center) {
        if (text != null) Text(text, color = if (centered) Ink else Ink.copy(alpha = 0.55f),
            fontSize = if (centered) 46.sp else 22.sp, fontWeight = if (centered) FontWeight.SemiBold else FontWeight.Medium)
    }
}

// ---- dots bar: both bars are one 1-10 counter (top row 1-5, bottom row
// 6-10). Drag from dot 1 through to bottom-row dot 2 and 7 dots light up,
// so 7 commits; all 10 lit commits 0. Sliding past the bar's far edge
// continues the count in the other row. Clean taps hit the dot directly. ----
@Composable
private fun DotsBar(base: Int, count: Int, onCount: (Int) -> Unit, onCommit: (Int) -> Unit, panel: Color, modifier: Modifier, desc: String) {
    val haptics = LocalHapticFeedback.current
    Box(modifier.clip(PanelShape).background(panel)
        .semantics { contentDescription = "number dots $desc, slide to choose" }
        .pointerInput(base) {
            val padPx = 24.dp.toPx()
            val edgePx = 2.dp.toPx()
            // Unified 1-10 position: this bar's dots, or the other row's when
            // the finger slides past the far edge (same x mapping both rows).
            fun countAt(p: Offset): Int {
                val usable = (size.width - padPx * 2).coerceAtLeast(1f)
                val idx = (((p.x - padPx) / usable) * 5).toInt().coerceIn(0, 4)
                val bottomRow = if (base == 0) p.y > size.height + edgePx else p.y >= -edgePx
                return ((if (bottomRow) 5 else 0) + idx + 1).coerceIn(1, 10)
            }
            var last = 0
            detectDragGestures(
                onDragStart = { p -> last = countAt(p); onCount(last) },
                onDragEnd = {
                    if (last in 1..10) {
                        onCommit(last % 10)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                onDragCancel = { }
            ) { change, _ ->
                val c = countAt(change.position)
                if (c != last) {
                    last = c
                    onCount(c)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }) {
        Row(Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly) {
            repeat(5) { i ->
                val pos = base + i + 1
                val on = pos <= count
                val s by animateDpAsState(if (on) 20.dp else 13.dp, snapSpec(), label = "dotPop")
                Box(Modifier.size(s).clip(CircleShape)
                    .background(Ink.copy(alpha = if (on) 1f else 0.4f))
                    .clickable(interactionSource = remember { MutableInteractionSource() },
                        indication = null, onClick = { onCommit(pos % 10) })
                    .semantics { contentDescription = "$pos of 10" })
            }
        }
    }
}

// ---- dual slide pill: content follows the finger, so a right-drag slides the
// left label into the middle (and the right label out of view); the label in
// the middle brightens, and the pill springs home on release ----
@Composable
private fun DualPill(left: String, right: String, accent: Color, onLeft: () -> Unit, onRight: () -> Unit, leftDesc: String, rightDesc: String) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val threshold = 90f
    val tapSlopPx = with(density) { 16.dp.toPx() }
    var barW by remember { mutableFloatStateOf(0f) }
    val leftHeat = (offset.value / threshold).coerceIn(0f, 1f)
    val rightHeat = (-offset.value / threshold).coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize().clip(PillShape).background(Ink)
        .onSizeChanged { barW = it.width.toFloat() }
        .pointerInput(barW) {
            var total = 0f
            var upX = -1f
            detectHorizontalDragGestures(
                onDragStart = { total = 0f; upX = -1f },
                onDragEnd = {
                    val v = offset.value
                    // Same jitter story as the dials: a wobbly tap arrives as a
                    // tiny drag, so resolve it by release half, not by distance.
                    if (kotlin.math.abs(total) < tapSlopPx && upX >= 0f && barW > 0f) {
                        if (upX < barW / 2f) onLeft() else onRight()
                    }
                    else if (v > threshold) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLeft() }
                    else if (v < -threshold) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onRight() }
                    scope.launch { offset.animateTo(0f, snapSpec()) }
                },
                onDragCancel = { scope.launch { offset.animateTo(0f, snapSpec()) } }
            ) { change, d -> total += d; upX = change.position.x; scope.launch { offset.snapTo((offset.value + d).coerceIn(-160f, 160f)) } }
        }) {
        Row(Modifier.fillMaxSize().graphicsLayer { translationX = offset.value * 0.25f },
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).fillMaxHeight()
                .semantics { contentDescription = leftDesc }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onLeft), contentAlignment = Alignment.Center) {
                Text(left, color = accent.copy(alpha = 0.7f + 0.3f * leftHeat),
                    fontSize = if (left.length > 2) 20.sp else 24.sp, fontFamily = Geist, fontWeight = FontWeight.Medium)
            }
            Box(Modifier.weight(1f).fillMaxHeight()
                .semantics { contentDescription = rightDesc }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRight), contentAlignment = Alignment.Center) {
                Text(right, color = accent.copy(alpha = 0.7f + 0.3f * rightHeat),
                    fontSize = 24.sp, fontFamily = Geist, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ---- round pads: tactile press scale + crossfading drag-direction preview ----
@Composable
private fun padScale(pressed: Boolean): Float {
    val s by animateFloatAsState(
        if (pressed) 0.92f else 1f, settleSpec(), label = "padPress")
    return s
}

@Composable
private fun PadOption(text: String, active: Boolean, accent: Color) {
    // The stack floats over olive cards and the black frame alike, so each
    // circle carries its own contrast: dark disc + accent ring at rest (reads
    // on olive), accent fill + dark ring when active (reads on olive, pops on
    // black). A bare tint vanishes on the card it matches.
    Box(Modifier.size(64.dp).clip(CircleShape)
        .background(if (active) accent else Ink)
        .border(if (active) 2.dp else 1.5.dp, if (active) Ink else accent, CircleShape),
        contentAlignment = Alignment.Center) {
        Text(text, color = if (active) Ink else accent,
            fontSize = 24.sp, fontFamily = Geist, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ParenPad(accent: Color, onParen: (Char) -> Unit, onSmart: () -> Unit) {
    var preview by remember { mutableStateOf<String?>(null) }
    var dragging by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    // Popup pitch: 64dp circles + 4dp gaps. Row sits left of the pad with ")"
    // nearest the finger (68dp out) and "(" one pitch further (136dp out).
    val pitchPx = with(density) { 68.dp.toPx() }
    val entryPx = with(density) { 34.dp.toPx() }
    val tapSlopPx = with(density) { 16.dp.toPx() }
    val interaction = remember { MutableInteractionSource() }
    val tapped by interaction.collectIsPressedAsState()
    val scale = padScale(tapped || dragging)
    Box(Modifier.size(76.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(CircleShape).background(Ink)
        .semantics { contentDescription = "parentheses, drag left into the options to choose, tap for smart" }
        .clickable(interactionSource = interaction, indication = null, onClick = onSmart)
        .pointerInput(Unit) {
            var total = 0f
            // Highlight follows where the finger is in the popup row, not just
            // which way it moved: each 68dp of left travel covers one option.
            fun previewFor(t: Float): String? {
                if (t > 20f) return ")"
                val left = -t
                if (left < entryPx) return null
                return if (((left - entryPx) / pitchPx).toInt() == 0) ")" else "("
            }
            detectHorizontalDragGestures(
                onDragStart = { dragging = true },
                onDragEnd = {
                    // Jitter under tap slop counts as the tap it was, not a drag.
                    if (kotlin.math.abs(total) < tapSlopPx) onSmart()
                    else when (previewFor(total)) {
                        "(" -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onParen('(') }
                        ")" -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onParen(')') }
                    }
                    total = 0f; preview = null; dragging = false
                },
                onDragCancel = { total = 0f; preview = null; dragging = false }
            ) { _, d -> total += d
                val p = previewFor(total)
                if (p != preview) {
                    if (p != null) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    preview = p
                }
            }
        }, contentAlignment = Alignment.Center) {
        Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(targetState = preview, label = "parenPreview",
                transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) }) {
                Text(it ?: "( )", color = accent, fontSize = 24.sp, fontFamily = Geist, fontWeight = FontWeight.Medium)
            }
        }
        if (dragging) {
            Popup(alignment = Alignment.CenterStart,
                offset = IntOffset(with(density) { (-130).dp.toPx().toInt() }, 0)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PadOption("(", active = preview == "(", accent)
                    PadOption(")", active = preview == ")", accent)
                }
            }
        }
    }
}

// ---- operator: drag up through the popup stack (bottom to top: ÷ × + −),
// or flick sideways (left ×, right ÷); tap=×. Highlight follows the finger. ----
@Composable
private fun OpPad(accent: Color, onOp: (Char) -> Unit) {
    var preview by remember { mutableStateOf<String?>(null) }
    var dragging by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    // Popup pitch: 64dp circles + 4dp gaps. Stack rises above the pad with "/"
    // nearest the finger (68dp up), then ×, +, − one pitch apart.
    val pitchPx = with(density) { 68.dp.toPx() }
    val entryPx = with(density) { 34.dp.toPx() }
    val tapSlopPx = with(density) { 16.dp.toPx() }
    val interaction = remember { MutableInteractionSource() }
    val tapped by interaction.collectIsPressedAsState()
    val scale = padScale(tapped || dragging)
    Box(Modifier.size(76.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(CircleShape).background(Ink)
        .semantics { contentDescription = "operators, drag up into the stack to choose, tap times" }
        .clickable(interactionSource = interaction, indication = null, onClick = { onOp('*') })
        .pointerInput(Unit) {
            var dx = 0f; var dy = 0f
            val stack = listOf("−", "+", "×", "÷")
            // Highlight follows where the finger is in the popup stack, not
            // just which way it moved: each 68dp of upward travel covers one row.
            fun previewFor(x: Float, y: Float): String? {
                if (kotlin.math.abs(x) > kotlin.math.abs(y)) {
                    if (x < -20f) return "×"
                    if (x > 20f) return "÷"
                    return null
                }
                val up = -y
                if (up < entryPx) return null
                return stack[(3 - ((up - entryPx) / pitchPx).toInt()).coerceIn(0, 3)]
            }
            detectDragGestures(
                onDragStart = { dragging = true },
                onDragEnd = {
                    // Jitter under tap slop counts as the tap it was, not a drag.
                    if (kotlin.math.abs(dx) < tapSlopPx && kotlin.math.abs(dy) < tapSlopPx) onOp('*')
                    else when (previewFor(dx, dy)) {
                        "−" -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onOp('-') }
                        "+" -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onOp('+') }
                        "×" -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onOp('*') }
                        "÷" -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onOp('/') }
                    }
                    dx = 0f; dy = 0f; preview = null; dragging = false
                },
                onDragCancel = { dx = 0f; dy = 0f; preview = null; dragging = false }
            ) { _, d -> dx += d.x; dy += d.y
                val p = previewFor(dx, dy)
                if (p != preview) {
                    if (p != null) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    preview = p
                }
            }
        }, contentAlignment = Alignment.Center) {
        Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(targetState = preview, label = "opPreview",
                transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) }) {
                Text(it ?: "X", color = accent, fontSize = 26.sp, fontFamily = Geist, fontWeight = FontWeight.Medium)
            }
        }
        if (dragging) {
            Popup(alignment = Alignment.CenterEnd,
                offset = IntOffset(with(density) { (-6).dp.toPx().toInt() }, with(density) { (-170).dp.toPx().toInt() })) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    PadOption("−", active = preview == "−", accent)
                    PadOption("+", active = preview == "+", accent)
                    PadOption("×", active = preview == "×", accent)
                    PadOption("/", active = preview == "÷", accent)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JusCalcPreview() { JusCalcTheme { JusCalcApp() } }
