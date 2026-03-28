package com.dondone.mobile.feature.recipient.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dondone.mobile.core.designsystem.DawnPrimary
import com.dondone.mobile.core.designsystem.DawnText
import com.dondone.mobile.core.designsystem.DawnTextSubtle
import com.dondone.mobile.core.i18n.LocalAppLanguage
import com.dondone.mobile.core.i18n.text

data class RecipientDirectoryContactUiModel(
    val id: String,
    val name: String,
    val maskedPhoneNumber: String,
    val searchablePhoneNumber: String,
    val walletAddress: String?,
    val walletAddressLabel: String,
    val candidateUserId: Long? = null,
    val alreadyRegistered: Boolean = false
)

private enum class AddWalletMode {
    PHONE,
    ADDRESS
}

private val RecipientAddSheetBackground = Color(0xFFF8FAFD)
private val RecipientAddInputBorder = Color(0xFFD9E1EA)
private val RecipientAddAccentSurface = Color(0xFFE9F2FF)
private val RecipientAddSummaryLabel = Color(0xFF9098A4)
private val RecipientAddError = Color(0xFFC93C37)
private val RecipientAddSuccessSurface = Color(0xFFF5F8FC)
private val RecipientRelationOptions = listOf("FAMILY", "SPOUSE", "PARENT", "CHILD", "SIBLING", "FRIEND", "OTHER")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientWalletAddBottomSheet(
    phoneDirectory: List<RecipientDirectoryContactUiModel>,
    supportsRemotePhoneSearch: Boolean,
    phoneSearchResults: List<RecipientDirectoryContactUiModel>,
    isPhoneSearchLoading: Boolean,
    phoneSearchErrorMessage: String?,
    isSubmitting: Boolean,
    submitErrorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Long?) -> Unit,
    onSearchByPhone: (String) -> Unit,
    onClearPhoneSearch: () -> Unit
) {
    val language = LocalAppLanguage.current
    var mode by remember { mutableStateOf(AddWalletMode.PHONE) }
    var phoneQuery by remember { mutableStateOf("") }
    var selectedPhoneContactId by remember { mutableStateOf<String?>(null) }
    var manualAlias by remember { mutableStateOf("") }
    var manualWalletAddress by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("FAMILY") }
    var hasRequestedSearch by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val normalizedQuery = remember(phoneQuery) { phoneQuery.toPhoneDigits() }
    val visiblePhoneContacts = remember(
        supportsRemotePhoneSearch,
        phoneDirectory,
        normalizedQuery,
        phoneSearchResults
    ) {
        if (supportsRemotePhoneSearch) {
            phoneSearchResults
        } else if (normalizedQuery.isBlank()) {
            phoneDirectory
        } else {
            phoneDirectory.filter { contact ->
                contact.searchablePhoneNumber.contains(normalizedQuery)
            }
        }
    }
    val selectedPhoneContact = remember(selectedPhoneContactId, visiblePhoneContacts) {
        visiblePhoneContacts.firstOrNull { it.id == selectedPhoneContactId }
    }
    val manualAddressValid = remember(manualWalletAddress) {
        manualWalletAddress.trim().isLikelyWalletAddress()
    }
    val canSearchByPhone = normalizedQuery.length in 10..11 && !isPhoneSearchLoading && !isSubmitting
    val canSubmit = when (mode) {
        AddWalletMode.PHONE -> !isSubmitting && selectedPhoneContact != null && !selectedPhoneContact.alreadyRegistered
        AddWalletMode.ADDRESS -> !isSubmitting && manualAlias.trim().isNotBlank() && manualAddressValid
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.32f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = language.text("recipient_add_wallet_title"),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = DawnText
            )
            Text(
                text = language.text("recipient_add_wallet_description"),
                style = MaterialTheme.typography.bodyMedium,
                color = DawnTextSubtle
            )

            AddWalletModeTabs(
                selectedMode = mode,
                onSelectMode = { nextMode ->
                    mode = nextMode
                    if (nextMode == AddWalletMode.ADDRESS) {
                        selectedPhoneContactId = null
                        hasRequestedSearch = false
                        onClearPhoneSearch()
                    } else if (supportsRemotePhoneSearch) {
                        selectedPhoneContactId = null
                    }
                }
            )

            if (mode == AddWalletMode.PHONE) {
                PhoneSearchForm(
                    isRemoteSearchEnabled = supportsRemotePhoneSearch,
                    phoneQuery = phoneQuery,
                    onPhoneQueryChange = {
                        phoneQuery = it
                        selectedPhoneContactId = null
                        if (supportsRemotePhoneSearch) {
                            hasRequestedSearch = false
                            onClearPhoneSearch()
                        }
                    },
                    contacts = visiblePhoneContacts,
                    selectedContactId = selectedPhoneContactId,
                    onSelectContact = { selectedPhoneContactId = it },
                    onSwitchToManual = {
                        mode = AddWalletMode.ADDRESS
                        hasRequestedSearch = false
                        onClearPhoneSearch()
                    },
                    isLoading = isPhoneSearchLoading,
                    errorMessage = phoneSearchErrorMessage,
                    canSearch = canSearchByPhone,
                    showEmptyState = if (supportsRemotePhoneSearch) {
                        hasRequestedSearch &&
                            !isPhoneSearchLoading &&
                            phoneSearchErrorMessage == null &&
                            visiblePhoneContacts.isEmpty()
                    } else {
                        visiblePhoneContacts.isEmpty()
                    },
                    onSearch = {
                        hasRequestedSearch = true
                        selectedPhoneContactId = null
                        onSearchByPhone(normalizedQuery)
                    }
                )
            } else {
                ManualAddressForm(
                    alias = manualAlias,
                    onAliasChange = { manualAlias = it },
                    walletAddress = manualWalletAddress,
                    onWalletAddressChange = { manualWalletAddress = it }
                )
            }

            RelationSelector(
                selectedRelation = relation,
                onSelectRelation = { relation = it }
            )

            if (mode == AddWalletMode.PHONE && selectedPhoneContact != null) {
                SelectedPhoneContactSummary(contact = selectedPhoneContact)
            }

            if (submitErrorMessage != null) {
                Text(
                    text = submitErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = RecipientAddError
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (canSubmit) DawnPrimary else RecipientAddInputBorder,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable(enabled = canSubmit) {
                        val alias = if (mode == AddWalletMode.PHONE) {
                            selectedPhoneContact?.name.orEmpty()
                        } else {
                            manualAlias.trim()
                        }
                        val walletAddress = if (mode == AddWalletMode.PHONE) {
                            selectedPhoneContact?.walletAddress.orEmpty()
                        } else {
                            manualWalletAddress.trim()
                        }
                        val targetUserId = if (mode == AddWalletMode.PHONE) {
                            selectedPhoneContact?.candidateUserId
                        } else {
                            null
                        }
                        onSubmit(alias, relation, walletAddress, targetUserId)
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = language.text("recipient_register_wallet"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = if (canSubmit) Color.White else RecipientAddSummaryLabel
                    )
                }
            }

            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

@Composable
private fun AddWalletModeTabs(
    selectedMode: AddWalletMode,
    onSelectMode: (AddWalletMode) -> Unit
) {
    val language = LocalAppLanguage.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RecipientAddSheetBackground, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AddWalletMode.entries.forEach { mode ->
            val selected = mode == selectedMode
            val label = if (mode == AddWalletMode.PHONE) {
                language.text("recipient_find_by_phone")
            } else {
                language.text("recipient_enter_wallet_directly")
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) Color.White else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) RecipientAddInputBorder else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectMode(mode) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = if (selected) DawnText else DawnTextSubtle
                )
            }
        }
    }
}

@Composable
private fun PhoneSearchForm(
    isRemoteSearchEnabled: Boolean,
    phoneQuery: String,
    onPhoneQueryChange: (String) -> Unit,
    contacts: List<RecipientDirectoryContactUiModel>,
    selectedContactId: String?,
    onSelectContact: (String) -> Unit,
    onSwitchToManual: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    canSearch: Boolean,
    showEmptyState: Boolean,
    onSearch: () -> Unit
) {
    val language = LocalAppLanguage.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = phoneQuery,
            onValueChange = onPhoneQueryChange,
            singleLine = true,
            label = { Text(language.text("recipient_phone_number")) },
            placeholder = { Text("01012345678") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(16.dp)
        )
        Text(
            text = if (isRemoteSearchEnabled) {
                language.text("recipient_find_registered_wallet_by_phone")
            } else {
                language.text("recipient_find_in_demo_contacts_only")
            },
            style = MaterialTheme.typography.bodySmall,
            color = RecipientAddSummaryLabel
        )

        if (isRemoteSearchEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (canSearch) DawnPrimary else RecipientAddSheetBackground,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = canSearch, onClick = onSearch)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = language.text("recipient_search_by_phone_number"),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = if (canSearch) Color.White else RecipientAddSummaryLabel
                    )
                }
            }
        }

        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RecipientAddSheetBackground, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = RecipientAddError
                    )
                    Text(
                        text = language.text("recipient_can_register_direct_if_knows_wallet"),
                        style = MaterialTheme.typography.bodySmall,
                        color = DawnTextSubtle
                    )
                    Text(
                        modifier = Modifier.clickable(onClick = onSwitchToManual),
                        text = language.text("recipient_switch_to_direct_wallet_input"),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = DawnPrimary
                    )
                }
            }
        } else if (showEmptyState) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RecipientAddSheetBackground, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = language.text("recipient_no_matching_contact"),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = DawnText
                    )
                    Text(
                        text = language.text("recipient_can_register_direct_if_knows_wallet"),
                        style = MaterialTheme.typography.bodySmall,
                        color = DawnTextSubtle
                    )
                    Text(
                        modifier = Modifier.clickable(onClick = onSwitchToManual),
                        text = language.text("recipient_switch_to_direct_wallet_input"),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = DawnPrimary
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                contacts.forEach { contact ->
                    val selected = contact.id == selectedContactId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) RecipientAddAccentSurface else RecipientAddSheetBackground,
                                RoundedCornerShape(18.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) DawnPrimary else Color.Transparent,
                                RoundedCornerShape(18.dp)
                            )
                            .clickable(enabled = !contact.alreadyRegistered) { onSelectContact(contact.id) }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = DawnText
                            )
                            Text(
                                text = contact.maskedPhoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = RecipientAddSummaryLabel
                            )
                            Text(
                                text = contact.walletAddressLabel,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = DawnTextSubtle
                            )
                            if (contact.alreadyRegistered) {
                                Text(
                                    text = language.text("recipient_already_registered_wallet"),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = RecipientAddError
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualAddressForm(
    alias: String,
    onAliasChange: (String) -> Unit,
    walletAddress: String,
    onWalletAddressChange: (String) -> Unit
) {
    val language = LocalAppLanguage.current
    val addressValid = walletAddress.trim().isLikelyWalletAddress()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = alias,
            onValueChange = onAliasChange,
            singleLine = true,
            label = { Text(language.text("recipient_alias")) },
            shape = RoundedCornerShape(16.dp)
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = walletAddress,
            onValueChange = onWalletAddressChange,
            singleLine = true,
            label = { Text(language.text("recipient_wallet_address")) },
            placeholder = { Text("0x...") },
            supportingText = {
                Text(
                    text = if (walletAddress.isBlank() || addressValid) {
                        language.text("recipient_enter_testnet_evm_wallet")
                    } else {
                        language.text("recipient_enter_42_char_0x")
                    },
                    color = if (walletAddress.isBlank() || addressValid) {
                        RecipientAddSummaryLabel
                    } else {
                        RecipientAddError
                    }
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun RelationSelector(
    selectedRelation: String,
    onSelectRelation: (String) -> Unit
) {
    val language = LocalAppLanguage.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = language.text("recipient_relationship"),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        RecipientRelationOptions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { code ->
                    val selected = selectedRelation == code
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) RecipientAddAccentSurface else Color.White,
                                RoundedCornerShape(14.dp)
                            )
                            .border(
                                1.dp,
                                if (selected) DawnPrimary else RecipientAddInputBorder,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectRelation(code) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = language.text(code.lowercase()),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = if (selected) DawnPrimary else DawnTextSubtle
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SelectedPhoneContactSummary(
    contact: RecipientDirectoryContactUiModel
) {
    val language = LocalAppLanguage.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RecipientAddSuccessSurface, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = language.text("recipient_selected_contact"),
            style = MaterialTheme.typography.bodySmall,
            color = RecipientAddSummaryLabel
        )
        Text(
            text = contact.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
            color = DawnText
        )
        Text(
            text = contact.maskedPhoneNumber,
            style = MaterialTheme.typography.bodySmall,
            color = DawnTextSubtle
        )
        Text(
            text = contact.walletAddressLabel,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = DawnTextSubtle
        )
    }
}

private fun String.toPhoneDigits(): String = filter(Char::isDigit)

private fun String.isLikelyWalletAddress(): Boolean {
    return matches(Regex("^0x[a-fA-F0-9]{40}$"))
}
