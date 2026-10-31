package com.athar.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athar.mobile.AtharUiState

@Composable
fun AtharApp(
    state: AtharUiState,
    onQueryChange: (String) -> Unit,
    onLookup: () -> Unit,
    onAnalyze: () -> Unit,
    onDemo: () -> Unit
) {
    val rtl = TextStyle(
        textDirection = TextDirection.Rtl,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 16.sp
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        when {
            state.loading -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            state.error != null -> {
                Text(
                    text = state.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    style = rtl
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "أثَر",
                        style = rtl.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "مطابقة الآيات والأحاديث على الجهاز — بلا خادم للمطابقة",
                        style = rtl.copy(fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = state.corpusLabel,
                        style = rtl.copy(fontSize = 13.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = state.asrNote,
                        style = rtl.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                            .padding(14.dp)
                    ) {
                        if (state.query.isEmpty()) {
                            Text(
                                "الصق نص محاضرة أو آية أو حديث…",
                                style = rtl.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                            )
                        }
                        BasicTextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            textStyle = rtl.copy(lineHeight = 26.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onLookup,
                            enabled = !state.busy && state.query.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) { Text("اسأل أثَر") }
                        Button(
                            onClick = onAnalyze,
                            enabled = !state.busy && state.query.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) { Text("حلّل النص") }
                    }
                    TextButton(onClick = onDemo, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("جرّب مثالًا", style = rtl)
                    }

                    if (state.busy) {
                        CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    }

                    state.lookup?.let { result ->
                        SectionTitle("نتائج البحث")
                        if (result.candidates.isEmpty()) {
                            Text(
                                "لم يُعثر عليه في المصادر المحمّلة. هذا ليس حكمًا على النص.",
                                style = rtl
                            )
                        } else {
                            result.candidates.forEach { hit ->
                                ResultBlock(
                                    title = "${hit.passage.ref} — ${hit.status.ar}",
                                    body = hit.passage.text,
                                    meta = buildString {
                                        append("دليل ${"%.1f".format(hit.evidence)}")
                                        hit.grade?.let { g ->
                                            append(" · ${g.label}")
                                            g.scholar?.let { append(" — $it") }
                                        }
                                    },
                                    rtl = rtl
                                )
                            }
                        }
                    }

                    state.analyze?.let { result ->
                        SectionTitle("السجل (${result.ledger.size}) · ${result.elapsedMs}ms")
                        if (result.ledger.isEmpty()) {
                            Text("لم تُكتشف اقتباسات في هذا النص.", style = rtl)
                        } else {
                            result.ledger.forEach { e ->
                                ResultBlock(
                                    title = "${e.passage?.ref ?: "—"} — ${e.status.ar}",
                                    body = e.spokenSpan,
                                    meta = "كلمات ${e.startWord + 1}–${e.endWord}",
                                    rtl = rtl
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "الدرجات منقولة من المصادر · النموذج لا يفتي",
                        style = rtl.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = TextStyle(
            textDirection = TextDirection.Rtl,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Right
    )
}

@Composable
private fun ResultBlock(title: String, body: String, meta: String, rtl: TextStyle) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = rtl.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary))
        Text(body, style = rtl.copy(lineHeight = 26.sp))
        Text(meta, style = rtl.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)))
    }
}
