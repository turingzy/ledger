package cn.turingzy.ledger.ui.component.card.bank

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * 钛金属材质预设枚举。
 *
 * @property dark 卡面是否为暗色调（影响文字前景与边框的反差反色）。
 * @property base 材质的基础底色。
 */
internal enum class TitaniumPreset(val dark: Boolean, val base: Color) {
    /** 浅色彩虹钛：明亮钛金底色，伴随柔和的彩虹色泽衍射。 */
    IridescentLight(false, Color(0xFFF2EDE5)),

    /** 深色彩虹钛：深灰黑钛金底色，伴随多色暗调光斑。 */
    IridescentDark(true, Color(0xFF1A1B1E)),

    /** 浅色珍珠钛：柔和珍珠米白钛质感，带有微细拉丝光泽。 */
    PearlLight(false, Color(0xFFD1CEC6)),

    /** 深色珍珠钛：深海蓝黑珍珠钛质感，带有同心各向异性径向拉丝光芒。 */
    PearlDark(true, Color(0xFF0C1417)),
}

/**
 * 材质内部彩色高光斑点描述。
 *
 * @property color 光斑颜色。
 * @property x 归一化横向中心坐标。
 * @property y 归一化纵向中心坐标。
 * @property radius 相对卡片宽度的归一化半径（保持纵横比下为正圆）。
 */
private data class Glow(val color: Color, val x: Float, val y: Float, val radius: Float)

// Radius is expressed in card widths so the highlights stay round at the card aspect ratio.
private val titaniumGlows = mapOf(
    TitaniumPreset.IridescentLight to listOf(
        Glow(Color(0xFFF7F7E9), .02f, .05f, .40f),
        Glow(Color(0xFFE7BC6C), .47f, .28f, .34f),
        Glow(Color(0xFFC2A6DB), .75f, .05f, .29f),
        Glow(Color(0xFFB5DBBF), 1.02f, .14f, .29f),
        Glow(Color(0xFFAD86E4), .25f, .81f, .35f),
        Glow(Color(0xFFF4AE79), .88f, .76f, .41f),
        Glow(Color(0xFFA7D9B9), .65f, 1.02f, .29f),
    ),
    TitaniumPreset.IridescentDark to listOf(
        Glow(Color(0xFF373943), .33f, .07f, .42f),
        Glow(Color(0xFF534147), .31f, .36f, .24f),
        Glow(Color(0xFF574B33), .84f, .25f, .34f),
        Glow(Color(0xFF644629), .64f, .55f, .30f),
        Glow(Color(0xFF172B47), .25f, .83f, .33f),
        Glow(Color(0xFF171A2A), .70f, .93f, .29f),
    ),
    TitaniumPreset.PearlLight to listOf(
        Glow(Color(0xFFEBE9DE), .83f, .11f, .42f),
        Glow(Color(0xFFDDCDBA), .55f, .48f, .43f),
        Glow(Color(0xFFF0EEE3), .20f, .83f, .42f),
        Glow(Color(0xFFC5C0B6), .83f, 1.02f, .33f),
    ),
    TitaniumPreset.PearlDark to listOf(
        Glow(Color(0xFF193540), .28f, .51f, .34f),
        Glow(Color(0xFF45616B), .63f, .36f, .36f),
        Glow(Color(0xFF2D4945), .78f, .49f, .28f),
    ),
)

/**
 * 钛金属材质绘制层。
 *
 * 在 Android 13 (API 33)+ 且非预览模式下，使用 AGSL [RuntimeShader] 绘制各向异性金属噪点、光泽与动态光斑；
 * 在低版本或预览模式下退化为纯 Compose Canvas 径向渐变模拟。
 *
 * @param preset 钛金属预设类型。
 * @param motion 包含物理倾斜角度与拖拽强度的动效状态。
 * @param tuning 钛金属物理参数。
 * @param modifier 修饰符。
 */
@Composable
internal fun TitaniumMaterial(
    preset: TitaniumPreset,
    motion: State<TitaniumMotion>,
    tuning: TitaniumCardTuning,
    modifier: Modifier,
) {
    if (Build.VERSION.SDK_INT >= 33 && !LocalInspectionMode.current) {
        TitaniumShaderSurface(preset, motion, tuning, modifier)
    } else {
        Canvas(modifier.drawWithCache {
            val glows = titaniumGlows.getValue(preset).map { glow ->
                Brush.radialGradient(
                    0f to glow.color, .5f to glow.color.copy(alpha = .45f),
                    1f to glow.color.copy(alpha = 0f),
                    center = Offset(glow.x * size.width, glow.y * size.height),
                    radius = glow.radius * size.width,
                )
            }
            onDrawBehind {
                drawRect(preset.base)
                val (angle, dragStrength) = motion.value
                scale(1.06f) {
                    translate(
                        angle.x * size.width * (.023f + .095f * dragStrength),
                        angle.y * size.height * (.023f + .080f * dragStrength),
                    ) {
                        glows.forEach {
                            drawRect(it, topLeft = Offset(-size.width, -size.height), size = size * 3f)
                        }
                    }
                }
            }
        }) {}
    }
}

/**
 * Android 13+ 高精度 AGSL 运行时着色器材质渲染表面。
 */
@RequiresApi(33)
@Composable
private fun TitaniumShaderSurface(
    preset: TitaniumPreset,
    motion: State<TitaniumMotion>,
    tuning: TitaniumCardTuning,
    modifier: Modifier,
) {
    val shader = remember(preset, tuning.shaderDragBandStrength, tuning.shaderDragCoreStrength) {
        val glows = titaniumGlows.getValue(preset)
        val source = buildString {
            appendLine(lightingUniforms)
            glows.indices.forEach { i ->
                appendLine("layout(color) uniform half4 glowColor$i;")
                appendLine("uniform float3 glowPosition$i;")
            }
            append(materialShader.replace("APPLY_GLOWS", glows.indices.joinToString("\n") { i ->
                "color = mix(color, float3(glowColor$i.rgb), glow(uv, glowPosition$i));"
            }))
        }
        titaniumLightingShader(source, tuning).apply {
            setColorUniform("baseColor", preset.base.toAndroidColor())
            glows.forEachIndexed { i, glow ->
                setColorUniform("glowColor$i", glow.color.toAndroidColor())
                setFloatUniform("glowPosition$i", glow.x, glow.y, glow.radius)
            }
            setFloatUniform("finish", preset.ordinal.toFloat())
        }
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    Canvas(modifier) {
        shader.updateLighting(size.width, size.height, motion.value)
        drawRect(brush)
    }
}

private fun Color.toAndroidColor() = android.graphics.Color.valueOf(red, green, blue)

/**
 * 为自定义背景图片添加物理光照反射 [RenderEffect] 的修饰符。
 *
 * @param motion 倾斜与拖拽动效状态。
 * @param tuning 物理调节参数。
 */
@RequiresApi(33)
@Composable
internal fun Modifier.titaniumImageReflection(
    motion: State<TitaniumMotion>,
    tuning: TitaniumCardTuning,
): Modifier {
    val shader = remember(tuning.shaderDragBandStrength, tuning.shaderDragCoreStrength) {
        titaniumLightingShader("$lightingUniforms\n$imageReflectionShader", tuning)
    }
    return graphicsLayer {
        if (size.width > 0f && size.height > 0f) {
            shader.updateLighting(size.width, size.height, motion.value)
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "image").asComposeRenderEffect()
        }
    }
}

/**
 * 适用于 API < 33 或 Compose 预览环境的降级光带反射层。
 *
 * 使用线性渐变矩形跟随倾斜角度平移，模拟金属光泽滑过的视觉反馈。
 *
 * @param motion 倾斜动效状态。
 * @param tuning 调节参数。
 * @param modifier 修饰符。
 */
@Composable
internal fun TitaniumFallbackReflection(
    motion: State<TitaniumMotion>,
    tuning: TitaniumCardTuning,
    modifier: Modifier,
) {
    Canvas(modifier.drawWithCache {
        val band = Brush.linearGradient(
            0f to Color.Transparent,
            .18f to Color.White.copy(alpha = .12f),
            .46f to Color.White.copy(alpha = .40f),
            .68f to Color.White.copy(alpha = .16f),
            .80f to Color.Transparent,
            .90f to Color.Black.copy(alpha = .13f),
            1f to Color.Transparent,
            start = Offset(size.width * .31f, size.height * .34f),
            end = Offset(size.width * .76f, size.height * .65f),
        )
        onDrawBehind {
            val (angle, dragStrength) = motion.value
            translate(angle.x * size.width * .55f, angle.y * size.height * .46f) {
                drawRect(
                    band,
                    topLeft = Offset(-size.width, -size.height),
                    size = size * 3f,
                    alpha = (.1875f + tuning.fallbackDragHighlightBoost * dragStrength).coerceIn(0f, 1f),
                )
            }
        }
    }) {}
}

/**
 * 创建并配置光照计算所需的 [RuntimeShader]。
 */
@RequiresApi(33)
private fun titaniumLightingShader(source: String, tuning: TitaniumCardTuning): RuntimeShader =
    RuntimeShader(source).apply {
        setFloatUniform("dragHighlight", tuning.shaderDragBandStrength, tuning.shaderDragCoreStrength)
    }

/**
 * 将当前视口尺寸与动态倾斜物理数据更新至着色器 uniform 变量中。
 */
@RequiresApi(33)
private fun RuntimeShader.updateLighting(width: Float, height: Float, motion: TitaniumMotion) {
    setFloatUniform("resolution", width, height)
    setFloatUniform("tilt", motion.tilt.x, motion.tilt.y)
    setFloatUniform("dragStrength", motion.dragStrength)
}

private const val lightingUniforms = """
uniform float2 resolution;
uniform float2 tilt;
uniform float dragStrength;
uniform float2 dragHighlight;

float2 reflection(float2 uv) {
    float2 normal = normalize(float2(.85, .49 + tilt.y * (.28 + .25 * dragStrength)));
    float travel = mix(.62 * tilt.x + .34 * tilt.y, .47 * tilt.x + .29 * tilt.y, dragStrength);
    float d = dot(uv - .5, normal) - travel;
    float band = exp(-d * d * 42.);
    float ambient = .035 + .13 * min(length(tilt), 1.);
    if (dragStrength <= 0.) return float2(band * ambient, 0.);

    float core = exp(-d * d * 112.);
    float strength = mix(ambient, dragHighlight.x, dragStrength);
    float light = clamp(band * strength + core * dragHighlight.y * dragStrength, 0., 1.);
    float shoulder = d - .22;
    float shadow = exp(-shoulder * shoulder * 72.) * .13 * dragStrength;
    return float2(light, shadow);
}
"""

private const val materialShader = """
layout(color) uniform half4 baseColor;
uniform float finish;

float hash(float2 p) {
    float3 q = fract(float3(p.xyx) * .1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

float glow(float2 uv, float3 pool) {
    float2 delta = (uv - pool.xy) * float2(1., resolution.y / resolution.x);
    float d = length(delta) / pool.z;
    return exp(-d * d * 2.65);
}

half4 main(float2 position) {
    float2 face = position / resolution;
    float2 uv = face - tilt * (float2(.045, .035) + dragStrength * float2(.085, .075));
    float3 color = float3(baseColor.rgb);
    APPLY_GLOWS

    float grain = hash(floor(position)) - .5;
    float sand = grain * .036 + (hash(floor(position * .53)) - .5) * .016;
    if (finish == 2.) {
        float silk = (uv.x + uv.y * .74) - .83;
        color += float3(.045, .043, .039) * exp(-silk * silk * 18.);
        sand = grain * .026 + (hash(floor(position * float2(.24, .83))) - .5) * .017;
    } else if (finish == 3.) {
        float2 center = (uv - float2(.50, .49)) * float2(1., resolution.y / resolution.x);
        float radius = length(center);
        float2 direction = center / max(radius, .001);
        float alignment = dot(direction, normalize(float2(.64, -.77)));
        float spoke = abs(alignment);
        float halo = exp(-radius * radius * 13.);
        float radiusPx = radius * resolution.x;
        float rings = hash(float2(floor(radiusPx * .84), 7.)) - .5;
        float fineRings = sin(radiusPx * 2.2 + 2.4 * rings);
        float brush = (rings * .037 + fineRings * .009) * (.26 + spoke * .74) * halo;
        color *= .66 + pow(spoke, 3.) * .47;
        float faceLight = mix(.48, 1., smoothstep(-.15, .20, alignment));
        color += float3(.095, .132, .145) * pow(spoke, 10.) * halo * faceLight
            * smoothstep(0., 5., radiusPx);
        sand = grain * .016 + brush * smoothstep(1., 5., radiusPx);
    }
    color = clamp(color + sand, 0., 1.);
    float2 light = reflection(face);
    color = mix(color * (1. - light.y), float3(1., .98, .95), light.x);
    return half4(half3(color), 1.);
}
"""

private const val imageReflectionShader = """
uniform shader image;

half4 main(float2 position) {
    half4 sampleColor = image.eval(position);
    half2 light = half2(reflection(position / resolution));
    half3 reflected = half3(1., .98, .95) * sampleColor.a;
    return half4(mix(sampleColor.rgb * (1. - light.y), reflected, light.x), sampleColor.a);
}
"""
