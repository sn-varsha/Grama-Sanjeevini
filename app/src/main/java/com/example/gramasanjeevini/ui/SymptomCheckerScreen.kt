package com.example.gramasanjeevini.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gramasanjeevini.BuildConfig
import com.example.gramasanjeevini.utils.GroqChatService
import kotlinx.coroutines.launch

// ── Data model ────────────────────────────────────────────────────────────────
data class Message(val role: String, val content: String)

// Common symptom quick-chips to help low-literacy users
private val QUICK_SYMPTOMS = listOf(
    "I have fever",
    "Stomach pain",
    "Cold and cough",
    "Headache",
    "Vomiting",
    "Diarrhea",
    "Chest pain",
    "Skin rash",
    "Eye irritation",
    "Back pain"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomCheckerScreen(navController: NavController) {
    val scope             = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState         = rememberLazyListState()

    var input    by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Conversation: we keep full history for context-aware follow-ups
    var messages by remember {
        mutableStateOf(
            listOf(
                Message(
                    role    = "assistant",
                    content = "Namaste 🙏 I am Grama Sanjeevini AI.\n\nDescribe your symptoms and I will guide you. For emergencies like chest pain, difficulty breathing, or unconsciousness — please call **108** immediately."
                )
            )
        )
    }

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // Send message to Groq
    fun sendMessage(text: String) {
        if (text.isBlank() || isTyping) return
        errorMsg = null

        // Add user message immediately
        val userMsg = Message("user", text.trim())
        messages = messages + userMsg
        input    = ""
        isTyping = true
        keyboardController?.hide()

        // Build history for the API (all prior messages)
        val history = messages.map { Pair(it.role, it.content) }

        scope.launch {
            val result = GroqChatService.chat(
                conversationHistory = history,
                apiKey              = BuildConfig.GROQ_API_KEY
            )
            isTyping = false
            result.fold(
                onSuccess = { reply ->
                    messages = messages + Message("assistant", reply)
                },
                onFailure = { e ->
                    errorMsg = when {
                        e.message?.contains("API key", ignoreCase = true) == true ->
                            "⚙️ API key not configured. See README.md → How to Run for setup instructions."
                        e.message?.contains("rate limit", ignoreCase = true) == true ->
                            "⏳ Rate limit reached. Please wait a moment and try again."
                        else ->
                            "❌ Could not reach AI: ${e.message?.take(120)}"
                    }
                    // Still add a fallback assistant message so chat doesn't feel broken
                    messages = messages + Message(
                        "assistant",
                        "I'm having trouble connecting right now. For urgent health issues please visit your nearest PHC or call 108."
                    )
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape  = CircleShape,
                            color  = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "AI Health Assistant",
                                color      = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp
                            )
                            Text(
                                if (isTyping) "Typing…" else "Powered by Groq LLaMA 3",
                                color    = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D9488))
            )
        },
        containerColor = Color(0xFFF0FDFA)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // ── Disclaimer banner ─────────────────────────────────────────
            Surface(
                color    = Color(0xFFFFFBEB),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint     = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "General guidance only — not a medical diagnosis. Call 108 for emergencies.",
                        color = Color(0xFF92400E),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 16.sp
                    )
                }
            }

            // ── Error banner ──────────────────────────────────────────────
            if (errorMsg != null) {
                Surface(
                    color    = Color(0xFFFEE2E2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        errorMsg!!,
                        color    = Color(0xFFDC2626),
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        lineHeight = 16.sp
                    )
                }
            }

            // ── Chat messages ─────────────────────────────────────────────
            LazyColumn(
                state             = listState,
                modifier          = Modifier.weight(1f).fillMaxWidth(),
                contentPadding    = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }

                // Typing indicator
                if (isTyping) {
                    item { TypingIndicator() }
                }
            }

            // ── Quick symptom chips ───────────────────────────────────────
            if (messages.size == 1) {
                // Show only on first message (empty conversation)
                LazyRow(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(QUICK_SYMPTOMS) { symptom ->
                        AssistChip(
                            onClick = { sendMessage(symptom) },
                            label   = { Text(symptom, fontSize = 13.sp) },
                            colors  = AssistChipDefaults.assistChipColors(
                                containerColor = Color.White,
                                labelColor     = Color(0xFF0D9488)
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                borderColor = Color(0xFF0D9488),
                                enabled = true
                            )
                        )
                    }
                }
            }

            // ── Input bar ─────────────────────────────────────────────────
            Surface(
                color       = Color.White,
                shadowElevation = 8.dp,
                modifier    = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value           = input,
                        onValueChange   = { input = it },
                        placeholder     = { Text("Describe your symptoms…", color = Color.Gray) },
                        modifier        = Modifier.weight(1f),
                        shape           = RoundedCornerShape(24.dp),
                        maxLines        = 4,
                        enabled         = !isTyping,
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color(0xFF0D9488),
                            unfocusedBorderColor = Color(0xFFD1D5DB)
                        )
                    )

                    // Send button
                    FloatingActionButton(
                        onClick           = { sendMessage(input) },
                        containerColor    = if (input.isNotBlank() && !isTyping) Color(0xFF0D9488) else Color(0xFF9CA3AF),
                        contentColor      = Color.White,
                        modifier          = Modifier.size(50.dp),
                        elevation         = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Chat bubble ───────────────────────────────────────────────────────────────
@Composable
private fun ChatBubble(message: Message) {
    val isUser = message.role == "user"

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment     = Alignment.Bottom
    ) {
        // AI avatar
        if (!isUser) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D9488))
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier          = Modifier.widthIn(max = 290.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart    = 18.dp,
                    topEnd      = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd   = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) Color(0xFF0D9488) else Color.White,
                shadowElevation = if (isUser) 0.dp else 2.dp
            ) {
                Text(
                    text     = message.content,
                    color    = if (isUser) Color.White else Color(0xFF111827),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // User "U" avatar
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE6F4F1))
            ) {
                Text(
                    "U",
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF0D9488),
                    fontSize   = 14.sp
                )
            }
        }
    }
}

// ── Animated typing indicator ─────────────────────────────────────────────────
@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier          = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF0D9488))
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint     = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue  = 1f,
                        animationSpec = infiniteRepeatable(
                            animation  = tween(500, delayMillis = index * 150),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0D9488).copy(alpha = alpha))
                    )
                }
            }
        }
    }
}
