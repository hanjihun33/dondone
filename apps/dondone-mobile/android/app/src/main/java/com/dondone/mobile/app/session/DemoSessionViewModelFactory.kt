package com.dondone.mobile.app.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dondone.mobile.data.advance.BackendAdvanceRepository
import com.dondone.mobile.data.auth.AuthSessionStore
import com.dondone.mobile.data.auth.BackendAuthRepository
import com.dondone.mobile.data.documents.BackendWorkproofDocumentRepository
import com.dondone.mobile.data.workproof.BackendWorkproofRepository
import okhttp3.OkHttpClient

class DemoSessionViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DemoSessionViewModel::class.java)) {
            val client = OkHttpClient()
            @Suppress("UNCHECKED_CAST")
            return DemoSessionViewModel(
                authRepository = BackendAuthRepository(
                    sessionStore = AuthSessionStore(appContext),
                    client = client
                ),
                advanceRepository = BackendAdvanceRepository(client = client),
                workproofRepository = BackendWorkproofRepository(client = client),
                workproofDocumentRepository = BackendWorkproofDocumentRepository(client = client)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
