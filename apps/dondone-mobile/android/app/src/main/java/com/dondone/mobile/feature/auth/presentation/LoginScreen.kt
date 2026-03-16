package com.dondone.mobile.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dondone.mobile.app.session.AuthUiState
import com.dondone.mobile.core.designsystem.DawnBorder
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnPrimaryDeep
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DonDoneWordmark
import com.dondone.mobile.core.designsystem.rememberDonDoneGrayRipple

private val LoginBackground = Color(0xFFF8F6FF)
private val LoginMuted = Color(0xFF7A7390)
private val LoginWarning = Color(0xFFB26E2E)
private val LoginWarningSurface = Color(0xFFFFF5E8)
private val LoginAccentText = DawnPrimaryDeep
private val LoginAccent = DawnPrimary

private enum class AuthEntryScreen {
    WELCOME,
    LOGIN,
    SIGNUP
}

@Composable
fun LoginLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = LoginAccent)
            Text(
                text = "세션을 확인하고 있어요",
                style = MaterialTheme.typography.bodyLarge,
                color = DawnText
            )
        }
    }
}

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onSignup: (String, String, String) -> Unit,
    onFieldEdited: () -> Unit
) {
    var screen by rememberSaveable { mutableStateOf(AuthEntryScreen.WELCOME) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBackground)
    ) {
        when (screen) {
            AuthEntryScreen.WELCOME -> {
                WelcomeEntryScreen(
                    onStart = {
                        onFieldEdited()
                        screen = AuthEntryScreen.SIGNUP
                    },
                    onOpenLogin = {
                        onFieldEdited()
                        screen = AuthEntryScreen.LOGIN
                    }
                )
            }

            AuthEntryScreen.LOGIN -> {
                AuthFormScreen(
                    title = "로그인",
                    subtitle = "",
                    primaryActionText = if (uiState.isSubmitting) "로그인 중..." else "로그인",
                    showNameField = false,
                    name = name,
                    email = email,
                    password = password,
                    errorMessage = uiState.errorMessage,
                    isSubmitting = uiState.isSubmitting,
                    onBack = {
                        onFieldEdited()
                        screen = AuthEntryScreen.WELCOME
                    },
                    onNameChange = {
                        name = it
                        onFieldEdited()
                    },
                    onEmailChange = {
                        email = it
                        onFieldEdited()
                    },
                    onPasswordChange = {
                        password = it
                        onFieldEdited()
                    },
                    onPrimaryAction = {
                        focusManager.clearFocus(force = true)
                        onLogin(email, password)
                    },
                    onSecondaryAction = {
                        onFieldEdited()
                        screen = AuthEntryScreen.SIGNUP
                    },
                    secondaryActionText = "계정 만들기"
                )
            }

            AuthEntryScreen.SIGNUP -> {
                AuthFormScreen(
                    title = "회원가입",
                    subtitle = "",
                    primaryActionText = if (uiState.isSubmitting) "가입 중..." else "시작하기",
                    showNameField = true,
                    name = name,
                    email = email,
                    password = password,
                    errorMessage = uiState.errorMessage,
                    isSubmitting = uiState.isSubmitting,
                    onBack = {
                        onFieldEdited()
                        screen = AuthEntryScreen.WELCOME
                    },
                    onNameChange = {
                        name = it
                        onFieldEdited()
                    },
                    onEmailChange = {
                        email = it
                        onFieldEdited()
                    },
                    onPasswordChange = {
                        password = it
                        onFieldEdited()
                    },
                    onPrimaryAction = {
                        focusManager.clearFocus(force = true)
                        onSignup(name, email, password)
                    },
                    onSecondaryAction = {
                        onFieldEdited()
                        screen = AuthEntryScreen.LOGIN
                    },
                    secondaryActionText = "이미 계정이 있어요"
                )
            }
        }
    }
}

@Composable
private fun WelcomeEntryScreen(
    onStart: () -> Unit,
    onOpenLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            DonDoneWordmark()

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "간편하게 시작하고\n바로 확인해요",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = DawnText
                )
                Text(
                    text = "로그인하고 바로 시작해요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LoginMuted
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StartPrimaryButton(
                text = "시작하기",
                onClick = onStart,
                enabled = true
            )
            AuthTextButton(
                text = "이미 계정이 있어요",
                onClick = onOpenLogin
            )
        }
    }
}

@Composable
private fun AuthFormScreen(
    title: String,
    subtitle: String,
    primaryActionText: String,
    showNameField: Boolean,
    name: String,
    email: String,
    password: String,
    errorMessage: String?,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    secondaryActionText: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
            AuthTextButton(
                text = "이전",
                alignStart = true,
                onClick = onBack
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = DawnText
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LoginMuted
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                if (showNameField) {
                    TossLikeField(
                        value = name,
                        label = "이름",
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        onValueChange = onNameChange
                    )
                }
                TossLikeField(
                    value = email,
                    label = "이메일",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    onValueChange = onEmailChange
                )
                TossLikeField(
                    value = password,
                    label = "비밀번호",
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = onPasswordChange
                )
            }

            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = LoginWarningSurface
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LoginWarning
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StartPrimaryButton(
                text = primaryActionText,
                onClick = onPrimaryAction,
                enabled = !isSubmitting
            )
            AuthTextButton(
                text = secondaryActionText,
                onClick = onSecondaryAction
            )
        }
    }
}

@Composable
private fun TossLikeField(
    value: String,
    label: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = LoginMuted
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = DawnText,
                fontWeight = FontWeight.SemiBold
            ),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            cursorBrush = SolidColor(LoginAccent),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    if (value.isBlank()) {
                        Text(
                            text = "",
                            style = MaterialTheme.typography.titleMedium,
                            color = LoginMuted
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFocused) 2.dp else 1.dp)
                .background(if (isFocused) LoginAccent else DawnBorder.copy(alpha = 0.9f))
        )
    }
}

@Composable
private fun StartPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DawnPrimary),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
        )
    }
}

@Composable
private fun AuthTextButton(
    text: String,
    onClick: () -> Unit,
    alignStart: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (alignStart) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = text,
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberDonDoneGrayRipple(bounded = false),
                    onClick = onClick
                )
                .padding(vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = LoginAccentText,
            textAlign = if (alignStart) TextAlign.Start else TextAlign.Center
        )
    }
}
