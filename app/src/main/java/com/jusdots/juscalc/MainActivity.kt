package com.jusdots.juscalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    CalcTheme("Vanilla Haze", Color(0xFFFDF4D2)),
    CalcTheme("Violet Dust", Color(0xFFA290B7)),
    CalcTheme("Turquoise", Color(0xFF2DEEE1)),
    CalcTheme("Neon Lavender", Color(0xFFEA9EFF)),
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
    val theme = Themes[themeIdx % Themes.size]

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

    Column(
        Modifier.fillMaxSize().background(Frame)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Display(
            expr = expr, resultPlain = resultPlain, isErr = resultErr, afterEquals = afterEquals,
            panel = theme.main, themeName = theme.name,
            onCycleTheme = { themeIdx = (themeIdx + 1) % Themes.size },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NumberDialBar(listOf("5","6","7","8","9"), 0, theme.main, { append(it) }, Modifier.weight(1f).fillMaxHeight(), "digits 5 to 9")
            ActionCell(theme.main, Modifier.size(92.dp)) {
                ParenPad(theme.main, onParen = ::appendParen, onSmart = ::smartParen)
            }
        }
        Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NumberDialBar(listOf("0","1","2","3","4"), 2, theme.main, { append(it) }, Modifier.weight(1f).fillMaxHeight(), "digits 0 to 4")
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
                            is CalcEngine.Eval.Ok -> { resultPlain = CalcEngine.formatResult(r.value); resultErr = false; afterEquals = true }
                            is CalcEngine.Eval.Err -> { resultPlain = null; resultErr = true; afterEquals = true }
                        }
                    }, leftDesc = "delete", rightDesc = "equals")
            }
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
private fun Display(expr: String, resultPlain: String?, isErr: Boolean, afterEquals: Boolean, panel: Color, themeName: String, onCycleTheme: () -> Unit, modifier: Modifier) {
    val scroll = rememberScrollState()
    val resultKey = if (afterEquals) resultPlain ?: "ERR" else ""
    Box(modifier.clip(DisplayShape).background(panel)
        .combinedClickable(onClick = {}, onLongClick = onCycleTheme, onDoubleClick = onCycleTheme)
        .semantics { contentDescription = "result display, $themeName. long press for next color" }
        .padding(20.dp), contentAlignment = Alignment.BottomEnd) {
        Column(Modifier.fillMaxWidth().verticalScroll(scroll).animateContentSize(settleSpec()),
            horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (expr.isEmpty() && resultKey.isEmpty()) {
                Text("ZERO", color = Ink.copy(alpha = 0.45f), fontSize = 40.sp, fontFamily = Geist, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
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
                            Text(if (k == "ERR") "ERROR" else CalcEngine.numberToWords(k),
                                color = Ink, fontSize = 46.sp, fontFamily = Geist, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
        Text("HOLD FOR COLOR", color = Ink.copy(alpha = 0.4f), fontSize = 10.sp, fontFamily = Geist, fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.TopStart))
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
                out += CalcEngine.numberToWords(expr.substring(i, j)) to false
                i = j
            }
            c in prettyOp -> { out += (prettyOp[c]!! to true); i++ }
            c == '(' || c == ')' -> { out += (c.toString() to true); i++ }
            else -> i++
        }
    }
    return out
}

// ---- rotary dial: selection chases the finger (drag right = higher digit),
// with a slight lean toward the drag and detent ticks; springs home on release ----
@Composable
private fun NumberDialBar(digits: List<String>, startCenter: Int, panel: Color, onPick: (String) -> Unit, modifier: Modifier, desc: String) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var center by remember { mutableIntStateOf(startCenter) }
    val offset = remember { Animatable(0f) }
    var lastShown by remember { mutableIntStateOf(startCenter) }
    var settling by remember { mutableStateOf(false) }
    val stepPx = with(density) { 64.dp.toPx() }.coerceAtLeast(1f)
    BoxWithConstraints(modifier.clip(PanelShape).background(panel)
        .semantics { contentDescription = "number dial $desc, drag to choose" }
        .pointerInput(center, stepPx) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    val picked = (center + (offset.value / stepPx).roundToInt()).coerceIn(digits.indices)
                    onPick(digits[picked]); center = picked; lastShown = picked
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    settling = true
                    scope.launch { offset.animateTo(0f, settleSpec()); settling = false }
                },
                onDragCancel = {
                    settling = true
                    scope.launch { offset.animateTo(0f, settleSpec()); settling = false }
                }
            ) { _, d -> scope.launch { offset.snapTo(offset.value + d) } }
        }) {
        val shown = (center + (offset.value / stepPx).roundToInt()).coerceIn(digits.indices)
        if (shown != lastShown) {
            lastShown = shown
            if (!settling) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        Row(Modifier.fillMaxSize().graphicsLayer { translationX = (offset.value * 0.15f).coerceIn(-48f, 48f) },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            DialDigit(digits.getOrNull(shown - 1), false) { center = (shown - 1).coerceIn(digits.indices); lastShown = center; onPick(digits[center]) }
            DialDigit(digits[shown], true) { onPick(digits[shown]) }
            DialDigit(digits.getOrNull(shown + 1), false) { center = (shown + 1).coerceIn(digits.indices); lastShown = center; onPick(digits[center]) }
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

// ---- dual slide pill: content leans toward the finger, the dragged-toward
// label brightens with progress, and the pill springs home on release ----
@Composable
private fun DualPill(left: String, right: String, accent: Color, onLeft: () -> Unit, onRight: () -> Unit, leftDesc: String, rightDesc: String) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val threshold = 90f
    val leftHeat = (-offset.value / threshold).coerceIn(0f, 1f)
    val rightHeat = (offset.value / threshold).coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize().clip(PillShape).background(Ink)
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    val v = offset.value
                    if (v > threshold) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onRight() }
                    else if (v < -threshold) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLeft() }
                    scope.launch { offset.animateTo(0f, snapSpec()) }
                },
                onDragCancel = { scope.launch { offset.animateTo(0f, snapSpec()) } }
            ) { _, d -> scope.launch { offset.snapTo((offset.value + d).coerceIn(-160f, 160f)) } }
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
private fun PadScale(pressed: Boolean): Float {
    val s by animateFloatAsState(
        if (pressed) 0.92f else 1f, settleSpec(), label = "padPress")
    return s
}

@Composable
private fun ParenPad(accent: Color, onParen: (Char) -> Unit, onSmart: () -> Unit) {
    var preview by remember { mutableStateOf<String?>(null) }
    var dragging by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val tapped by interaction.collectIsPressedAsState()
    val scale = PadScale(tapped || dragging)
    Box(Modifier.size(76.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(CircleShape).background(Ink)
        .semantics { contentDescription = "parentheses, drag left for open, right for close, tap for smart" }
        .clickable(interactionSource = interaction, indication = null, onClick = onSmart)
        .pointerInput(Unit) {
            var total = 0f
            detectHorizontalDragGestures(
                onDragStart = { dragging = true },
                onDragEnd = {
                    if (total < -60f) onParen('(') else if (total > 60f) onParen(')')
                    total = 0f; preview = null; dragging = false
                },
                onDragCancel = { total = 0f; preview = null; dragging = false }
            ) { _, d -> total += d; preview = if (total < -20f) "(" else if (total > 20f) ")" else null }
        }, contentAlignment = Alignment.Center) {
        Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(targetState = preview, label = "parenPreview",
                transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) }) {
                Text(it ?: "( )", color = accent, fontSize = 24.sp, fontFamily = Geist, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ---- operator: 4-way drag (up=+, down=−, left=×, right=÷), tap=× ----
@Composable
private fun OpPad(accent: Color, onOp: (Char) -> Unit) {
    var preview by remember { mutableStateOf<String?>(null) }
    var dragging by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val tapped by interaction.collectIsPressedAsState()
    val scale = PadScale(tapped || dragging)
    Box(Modifier.size(76.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(CircleShape).background(Ink)
        .semantics { contentDescription = "operators, drag up plus, down minus, left times, right divide, tap times" }
        .clickable(interactionSource = interaction, indication = null, onClick = { onOp('*') })
        .pointerInput(Unit) {
            var dx = 0f; var dy = 0f
            detectDragGestures(
                onDragStart = { dragging = true },
                onDragEnd = {
                    val pick = if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                        if (dx < -60f) '*' else if (dx > 60f) '/' else null
                    } else {
                        if (dy < -60f) '+' else if (dy > 60f) '-' else null
                    }
                    if (pick != null) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onOp(pick) }
                    dx = 0f; dy = 0f; preview = null; dragging = false
                },
                onDragCancel = { dx = 0f; dy = 0f; preview = null; dragging = false }
            ) { _, d -> dx += d.x; dy += d.y
                preview = if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                    if (dx < -20f) "×" else if (dx > 20f) "÷" else null
                } else { if (dy < -20f) "+" else if (dy > 20f) "−" else null }
            }
        }, contentAlignment = Alignment.Center) {
        Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(targetState = preview, label = "opPreview",
                transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) }) {
                Text(it ?: "X", color = accent, fontSize = 26.sp, fontFamily = Geist, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JusCalcPreview() { JusCalcTheme { JusCalcApp() } }
