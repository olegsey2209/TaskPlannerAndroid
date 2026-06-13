package com.taskplanner.android.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.taskplanner.android.core.util.PasswordValidator
import com.taskplanner.android.ui.LocalAppGraph
import com.taskplanner.android.ui.theme.AccentGradient
import com.taskplanner.android.ui.theme.AppColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun AuthScreen() {
    var showRegister by remember { mutableStateOf(false) }

    if (showRegister) {
        RegisterScreen(onBackToLogin = { showRegister = false })
    } else {
        LoginScreen(onGoToRegister = { showRegister = true })
    }
}

@Composable
private fun LoginScreen(onGoToRegister: () -> Unit) {
    val graph = LocalAppGraph.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            GradientLogo()
            Spacer(modifier = Modifier.height(48.dp))

            ModernTextField(
                value = email,
                onValueChange = { email = it; errorMessage = "" },
                label = "Email",
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = AppColors.GrayText) },
                keyboardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            ModernTextField(
                value = password,
                onValueChange = { password = it; errorMessage = "" },
                label = "Пароль",
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = AppColors.GrayText) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = AppColors.GrayText
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = AppColors.Red,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GradientButton(
                text = "Войти",
                isLoading = isLoading,
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
                onClick = {
                    val emailTrim = email.trim()
                    if (emailTrim.isEmpty() || password.isEmpty()) {
                        errorMessage = "Заполните все поля"
                        return@GradientButton
                    }
                    isLoading = true
                    errorMessage = ""
                    scope.launch {
                        val auth = FirebaseAuth.getInstance()
                        
                        
                        runCatching {
                            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
                        }
                        try {
                            android.util.Log.d("TaskPlanner", "Login: signing in $emailTrim")
                            val result = withTimeoutOrNull(45_000L) {
                                auth.signInWithEmailAndPassword(emailTrim, password).await()
                            }
                            if (result == null) {
                                android.util.Log.w("TaskPlanner", "Login: signIn timed out (45s)")
                                errorMessage = "Сервер не отвечает. Перезапустите Wi-Fi и попробуйте ещё раз."
                                isLoading = false
                                return@launch
                            }
                            val user = result.user
                            if (user == null) {
                                android.util.Log.w("TaskPlanner", "Login: result.user is null")
                                errorMessage = "Не удалось войти"
                                isLoading = false
                                return@launch
                            }
                            val uid = user.uid
                            android.util.Log.d("TaskPlanner", "Login: got uid=$uid, persisting locally")
                            try {
                                graph.userRepository.getOrCreateUser(uid = uid, email = user.email ?: emailTrim)
                            } catch (t: Throwable) {
                                android.util.Log.e("TaskPlanner", "Login: getOrCreateUser не удалось", t)
                            }
                            try {
                                graph.categoryRepository.seedDefaultCategoriesIfEmpty(uid)
                            } catch (t: Throwable) {
                                android.util.Log.e("TaskPlanner", "Login: seed categories не удалось", t)
                            }
                            graph.setCurrentUserIdForTrigger(uid)
                            graph.userPrefs.setCurrentUserId(uid)
                            android.util.Log.d("TaskPlanner", "Login: currentUserId saved, navigating")
                            isLoading = false
                            try { graph.syncEngine.forceSync(uid) } catch (t: Throwable) {
                                android.util.Log.e("TaskPlanner", "Login: forceSync не удалось", t)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TaskPlanner", "Login: exception", e)
                            errorMessage = mapAuthError(e.localizedMessage ?: e.toString())
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onGoToRegister) {
                Text(
                    text = "Создать аккаунт",
                    color = AppColors.Blue,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RegisterScreen(onBackToLogin: () -> Unit) {
    val graph = LocalAppGraph.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var pwVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val passwordCheck = PasswordValidator.isValidPassword(password)
    val passwordsMatch = PasswordValidator.passwordsMatch(password, confirm)
    val canSubmit = username.trim().isNotEmpty() &&
        email.trim().isNotEmpty() &&
        passwordCheck.isValid &&
        passwordsMatch &&
        !isLoading

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToLogin) {
                    Icon(Icons.Filled.Close, contentDescription = "Назад", tint = AppColors.GrayText)
                }
                Spacer(modifier = Modifier.fillMaxWidth(0.5f))
            }

            Spacer(modifier = Modifier.height(8.dp))
            GradientLogo()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Создать аккаунт",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            ModernTextField(
                value = username,
                onValueChange = { username = it; errorMessage = "" },
                label = "Имя пользователя",
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = AppColors.GrayText) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ModernTextField(
                value = email,
                onValueChange = { email = it; errorMessage = "" },
                label = "Email",
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = AppColors.GrayText) },
                keyboardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ModernTextField(
                value = password,
                onValueChange = { password = it; errorMessage = "" },
                label = "Пароль",
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = AppColors.GrayText) },
                trailingIcon = {
                    IconButton(onClick = { pwVisible = !pwVisible }) {
                        Icon(
                            if (pwVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = AppColors.GrayText
                        )
                    }
                },
                visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ModernTextField(
                value = confirm,
                onValueChange = { confirm = it; errorMessage = "" },
                label = "Подтвердите пароль",
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = AppColors.GrayText) },
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            if (confirmVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = AppColors.GrayText
                        )
                    }
                },
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                modifier = Modifier.fillMaxWidth()
            )

            if (confirm.isNotEmpty() && !passwordsMatch) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Пароли не совпадают",
                    color = AppColors.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        RequirementRow("Минимум 8 символов", PasswordValidator.hasMinLength(password))
                        RequirementRow("Цифра (0-9)", PasswordValidator.hasDigit(password))
                        RequirementRow("Строчная буква (a-z)", PasswordValidator.hasLowercase(password))
                        RequirementRow("Заглавная буква (A-Z)", PasswordValidator.hasUppercase(password))
                        RequirementRow("Спецсимвол (!@#\$%^&*…)", PasswordValidator.hasSpecial(password))
                        RequirementRow("Без пробелов", PasswordValidator.hasNoSpaces(password))
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = AppColors.Red,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            GradientButton(
                text = "Создать аккаунт",
                isLoading = isLoading,
                enabled = canSubmit,
                onClick = {
                    val emailTrim = email.trim()
                    val usernameTrim = username.trim()
                    if (!passwordCheck.isValid) {
                        errorMessage = passwordCheck.message
                        return@GradientButton
                    }
                    if (!passwordsMatch) {
                        errorMessage = "Пароли не совпадают"
                        return@GradientButton
                    }
                    isLoading = true
                    errorMessage = ""
                    scope.launch {
                        val auth = FirebaseAuth.getInstance()
                        runCatching {
                            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
                        }
                        try {
                            android.util.Log.d("TaskPlanner", "Register: creating $emailTrim")
                            val result = withTimeoutOrNull(45_000L) {
                                auth.createUserWithEmailAndPassword(emailTrim, password).await()
                            }
                            if (result == null) {
                                android.util.Log.w("TaskPlanner", "Register: createUser timed out (45s)")
                                errorMessage = "Сервер не отвечает. Перезапустите Wi-Fi и попробуйте ещё раз."
                                isLoading = false
                                return@launch
                            }
                            val user = result.user
                            if (user == null) {
                                android.util.Log.w("TaskPlanner", "Register: result.user is null")
                                errorMessage = "Не удалось создать аккаунт"
                                isLoading = false
                                return@launch
                            }
                            val uid = user.uid
                            android.util.Log.d("TaskPlanner", "Register: uid=$uid, persisting locally")
                            try {
                                graph.userRepository.getOrCreateUser(
                                    uid = uid,
                                    email = user.email ?: emailTrim,
                                    username = usernameTrim
                                )
                            } catch (t: Throwable) {
                                android.util.Log.e("TaskPlanner", "Register: getOrCreateUser не удалось", t)
                            }
                            try {
                                graph.categoryRepository.seedDefaultCategoriesIfEmpty(uid)
                            } catch (t: Throwable) {
                                android.util.Log.e("TaskPlanner", "Register: seed categories не удалось", t)
                            }
                            graph.setCurrentUserIdForTrigger(uid)
                            graph.userPrefs.setCurrentUserId(uid)
                            android.util.Log.d("TaskPlanner", "Register: currentUserId saved, navigating")
                            isLoading = false
                            try { graph.syncEngine.forceSync(uid) } catch (t: Throwable) {
                                android.util.Log.e("TaskPlanner", "Register: forceSync не удалось", t)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TaskPlanner", "Register: exception", e)
                            errorMessage = mapAuthError(e.localizedMessage ?: e.toString())
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onBackToLogin) {
                Text(
                    text = "Уже есть аккаунт? Войти",
                    color = AppColors.Blue,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GradientLogo() {
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color(0xFF5B6BF8), Color(0xFF9B59B6))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color(0xFF5B6BF8), Color(0xFF9B59B6))
                        )
                    )
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(28.dp)) {
                    val stroke = 2.5.dp.toPx()
                    drawCircle(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color(0xFF5B6BF8.toInt()),
                                androidx.compose.ui.graphics.Color(0xFF9B59B6.toInt())
                            )
                        ),
                        radius = (size.minDimension - stroke) / 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color(0xFF5B6BF8), Color(0xFF9B59B6))
                        )
                    )
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "TaskPlanner", fontSize = 30.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Blue,
            unfocusedBorderColor = Color(0xFFE5E5EA),
            focusedLabelColor = AppColors.Blue
        ),
        modifier = modifier
    )
}

@Composable
private fun GradientButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (enabled) {
        Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    } else Modifier
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .shadow(elevation = if (enabled) 6.dp else 0.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) AccentGradient else SolidColor(Color(0xFFC7C7CC)))
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Text(text = text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        }
    }
}

@Composable
private fun RequirementRow(label: String, satisfied: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Icon(
            imageVector = if (satisfied) Icons.Filled.Check else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (satisfied) AppColors.Green else AppColors.GrayText,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            color = if (satisfied) AppColors.Green else AppColors.GrayText,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun mapAuthError(raw: String): String {
    val lower = raw.lowercase()
    return when {
        "password is invalid" in lower || "wrong-password" in lower || "invalid-credential" in lower ||
            "invalid login credentials" in lower || "invalid password" in lower ->
            "Неверный email или пароль"
        "no user record" in lower || "user-not-found" in lower ->
            "Пользователь не найден"
        "email address is already" in lower || "email-already-in-use" in lower ->
            "Этот email уже используется"
        "badly formatted" in lower || "invalid-email" in lower ->
            "Некорректный email"
        "network" in lower ->
            "Проблема с сетью. Проверьте подключение"
        "weak password" in lower || "weak-password" in lower ->
            "Слишком слабый пароль"
        else -> raw
    }
}
