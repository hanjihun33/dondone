package com.dondone.mobile.feature.recipient.presentation

import com.dondone.mobile.core.ui.toMaskedPhoneNumber

private data class RecipientDirectorySeed(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val walletAddress: String
)

private val DemoRecipientDirectorySeed = listOf(
    RecipientDirectorySeed(
        id = "contact-minh",
        name = "Minh Nguyen",
        phoneNumber = "01028411183",
        walletAddress = "0x7F4F0b8E8fA0d3B6bA91F5bEEfa2276c9168a20D"
    ),
    RecipientDirectorySeed(
        id = "contact-anh",
        name = "Anh Tran",
        phoneNumber = "01066139214",
        walletAddress = "0x50e8E7E74143F6A4F25e8f6b72A8092d18284D4c"
    ),
    RecipientDirectorySeed(
        id = "contact-lina",
        name = "Lina Park",
        phoneNumber = "01041250871",
        walletAddress = "0xF2D0C4b8A7E9E14A4A27055A933fA4DCC5cA8eE1"
    ),
    RecipientDirectorySeed(
        id = "contact-jose",
        name = "Jose Rivera",
        phoneNumber = "01090317724",
        walletAddress = "0x6b33B4F2ADeDd1F4e84A4c1c041cF236503A17f8"
    )
)

fun buildDemoRecipientDirectory(
    registeredAddresses: List<String>
): List<RecipientDirectoryContactUiModel> {
    val registered = registeredAddresses.toSet()
    return DemoRecipientDirectorySeed
        .filterNot { it.walletAddress in registered }
        .map { contact ->
            RecipientDirectoryContactUiModel(
                id = contact.id,
                name = contact.name,
                maskedPhoneNumber = contact.phoneNumber.toMaskedPhoneNumber(),
                searchablePhoneNumber = contact.phoneNumber,
                walletAddress = contact.walletAddress,
                walletAddressLabel = contact.walletAddress.toShortRecipientWalletAddress(),
                alreadyRegistered = false
            )
        }
}

private fun String.toShortRecipientWalletAddress(): String {
    return if (length <= 14) {
        this
    } else {
        "${take(8)}...${takeLast(6)}"
    }
}
