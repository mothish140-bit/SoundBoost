package com.mothish.soundboost

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2

private val Bg = Color(0xFF0B0F1E)
private val Card = Color(0xFF151B33)
private val Cyan = Color(0xFF22D3EE)
private val Violet = Color(0xFFA78BFA)
private val Red = Color(0xFFFB7185)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Bg, surface = Card)) {
                var splash by remember { mutableStateOf(true) }
                Crossfade(splash, animationSpec = tween(500), label = "root") {
                    if (it) Splash { splash = false } else Home()
                }
            }
        }
    }
}

@Composable
fun Splash(onDone: () -> Unit) {
    val scale = remember { Animatable(0.3f) }
    val sweep = remember { Animatable(0f) }
    val text = remember { Animatable(0f) }
    val pulse = rememberInfiniteTransition(label = "p").animateFloat(
        0.9f, 1.15f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pp")
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, spring(0.45f, Spring.StiffnessLow)) }
        launch { sweep.animateTo(360f, tween(1200, easing = FastOutSlowInEasing)) }
        delay(800); text.animateTo(1f, tween(600)); delay(700); onDone()
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Bg, Color(0xFF1A1040)))), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(220.dp), Alignment.Center) {
                Canvas(Modifier.fillMaxSize().scale(pulse.value)) {
                    drawCircle(Brush.radialGradient(listOf(Cyan.copy(.35f), Color.Transparent)), size.minDimension / 2)
                    drawArc(Brush.sweepGradient(listOf(Cyan, Violet, Cyan)), -90f, sweep.value, false,
                        style = Stroke(8.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(20.dp.toPx(), 20.dp.toPx()), size = Size(size.width - 40.dp.toPx(), size.height - 40.dp.toPx()))
                }
                Image(painterResource(R.drawable.ic_logo), null, Modifier.size(170.dp).scale(scale.value))
            }
            Spacer(Modifier.height(16.dp))
            Text("SoundBoost", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.alpha(text.value))
            Text("LOUDER · CLEARER · YOURS", fontSize = 12.sp, color = Cyan, letterSpacing = 3.sp, modifier = Modifier.alpha(text.value))
        }
    }
}

@Composable
fun Home() {
    val ctx = LocalContext.current
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun toggle(on: Boolean) {
        AudioFx.enabled = on
        val i = Intent(ctx, BoosterService::class.java)
        if (on) { ContextCompat.startForegroundService(ctx, i); AudioFx.apply() }
        else { ctx.stopService(i); AudioFx.release() }
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val accent = when { AudioFx.boost > 300 -> Red; AudioFx.boost > 200 -> Violet; else -> Cyan }

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()
        .verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_logo), null, Modifier.size(44.dp))
            Text("SoundBoost", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 6.dp))
            Switch(AudioFx.enabled, { toggle(it) })
        }
        Spacer(Modifier.height(20.dp))
        Dial(accent)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(100, 200, 300, 400).forEach { v ->
                FilterChip(AudioFx.boost == v, { AudioFx.boost = v; AudioFx.apply() }, { Text("$v%") })
            }
        }
        if (AudioFx.boost > 250) Text("⚠ High boost can distort sound and may damage speakers or hearing. Use carefully.",
            color = Red, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(20.dp))

        Section("Presets") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AudioFx.presets.forEach { p -> FilterChip(AudioFx.preset == p, { AudioFx.applyPreset(p) }, { Text(p) }) }
            }
        }
        Section("Bass Boost") { Slide("Bass", AudioFx.bass / 10f, 0f..100f) { AudioFx.bass = (it * 10).toInt(); AudioFx.apply() } }
        Section("Surround (Virtualizer)") { Slide("Width", AudioFx.surround / 10f, 0f..100f) { AudioFx.surround = (it * 10).toInt(); AudioFx.apply() } }
        Section("Equalizer") {
            if (AudioFx.freqs.isEmpty()) Text("Turn the power switch on to load equalizer bands.", color = Color.Gray, fontSize = 13.sp)
            AudioFx.freqs.forEachIndexed { i, f ->
                Slide(if (f >= 1000) "${f / 1000}kHz" else "${f}Hz", AudioFx.bands.getOrElse(i) { 0 }.toFloat(),
                    AudioFx.minLvl.toFloat()..AudioFx.maxLvl.toFloat()) {
                    AudioFx.bands[i] = it.toInt(); AudioFx.preset = "Custom"; AudioFx.apply()
                }
            }
        }
        Text("App by C G Mothish", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun Dial(accent: Color) {
    val frac by animateFloatAsState((AudioFx.boost - 100) / 300f, tween(150), label = "d")
    fun set(o: Offset, c: Offset) {
        var a = Math.toDegrees(atan2((o.y - c.y).toDouble(), (o.x - c.x).toDouble())).toFloat() - 135f
        a = (a + 360f) % 360f
        if (a > 270f) a = if (a > 315f) 0f else 270f
        AudioFx.boost = (100 + a / 270f * 300).toInt().coerceIn(100, 400); AudioFx.apply()
    }
    Box(Modifier.size(260.dp), Alignment.Center) {
        Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures { ch, _ -> set(ch.position, Offset(size.width / 2f, size.height / 2f)) }
        }.pointerInput(Unit) { detectTapGestures { set(it, Offset(size.width / 2f, size.height / 2f)) } }) {
            val s = Stroke(22.dp.toPx(), cap = StrokeCap.Round); val pad = 20.dp.toPx()
            val tl = Offset(pad, pad); val sz = Size(size.width - pad * 2, size.height - pad * 2)
            drawArc(Color(0xFF232B4D), 135f, 270f, false, tl, sz, style = s)
            drawArc(Brush.sweepGradient(listOf(Cyan, Violet, Red, Cyan), center), 135f, 270f * frac, false, tl, sz, style = s)
            drawCircle(Brush.radialGradient(listOf(accent.copy(.18f), Color.Transparent)), size.minDimension * .38f)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${AudioFx.boost}%", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            Text(if (AudioFx.enabled) "ACTIVE" else "OFF", fontSize = 12.sp, letterSpacing = 3.sp, color = Color.Gray)
        }
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).background(Card, RoundedCornerShape(20.dp)).padding(16.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
        content()
    }
}

@Composable
fun Slide(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(60.dp), fontSize = 12.sp, color = Color.LightGray)
        Slider(value.coerceIn(range), onChange, valueRange = range, modifier = Modifier.weight(1f))
    }
}
