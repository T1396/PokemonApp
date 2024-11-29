package com.example.pokinfo.viewModels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pokinfo.BuildConfig
import com.example.pokinfo.R
import com.example.pokinfo.data.models.firebase.Profile
import com.example.pokinfo.data.models.firebase.PublicProfile
import com.example.pokinfo.ui.loginRegister.ContextProvider
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch

private const val TAG = "AuthenticationViewModel"

class AuthenticationViewModel(
    application: Application,
    private val sharedViewModel: SharedViewModel,
) : AndroidViewModel(application) {

    private var auth = Firebase.auth
    private val fireStore = Firebase.firestore
    private val storage = Firebase.storage

    private val _user: MutableLiveData<FirebaseUser?> = MutableLiveData()
    val user: LiveData<FirebaseUser?>
        get() = _user

    private lateinit var profileRef: DocumentReference
    private val webClientId = BuildConfig.webClientId

    private val _userImage: MutableLiveData<String?> = MutableLiveData()
    val userImage: LiveData<String?> get() = _userImage


    init {
        setupUserEnv()
    }

    fun isUserLoggedIn(): Boolean {
        val user = auth.currentUser
        return user != null
    }

    private fun setupUserEnv() {
        _user.value = auth.currentUser
        auth.currentUser?.let { firebaseUser ->
            profileRef = fireStore.collection("userData").document(firebaseUser.uid)
            getProfilePicUrl() // once the profile exists load image Url
        }
    }



    //region google sign
    private fun signInGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser == true
                    setupUserEnv()
                    val user = auth.currentUser ?: return@addOnCompleteListener
                    if (isNewUser) {
                        createProfileDocument(user.displayName, user.email)
                    }
                } else {
                    sharedViewModel.postMessage(task.exception?.message ?: "Google sign in error")
                    Log.d("FirebaseAuthGoogle", task.exception?.message.toString())
                }
            }
    }


    fun setUpGoogleSignIn(contextProvider: ContextProvider, filter: Boolean) {
        val activityContext = contextProvider.getActivityContext()
        val credentialManager = CredentialManager.create(activityContext)
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filter)
            .setServerClientId(webClientId)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(activityContext, request)
                handleSignIn(result)
            } catch (e: NoCredentialException) {
                sharedViewModel.postMessage(R.string.google_error)
                Log.d(TAG, "Get Credential Error", e)
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "Credential Login/Sign in cancelled by user or some other unused behaviour", e)
            } catch (e: Exception) {
                Log.e(TAG, "Get Credential Error", e)
                sharedViewModel.postMessage(R.string.credential_error)
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential
        try {
            // Use googleIdTokenCredential and extract id to validate and auth on server
            val googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(credential.data)
            val googleIdToken = googleIdTokenCredential.idToken
            signInGoogle(googleIdToken)
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Received an invalid google id token response", e)
        }
    }
    //endregion

    //region login - register
    fun login(email: String, password: String) {
        if (!isValidEmail(email)) {
            sharedViewModel.postMessage(R.string.error_invalid_email)
            return
        }
        if (!isValidPassword(password)) {
            sharedViewModel.postMessage(R.string.error_invalid_password_length)
            return
        }

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                setupUserEnv()
            } else {
                Log.e(TAG, it.exception?.localizedMessage.toString())
                showErrorMessageForUser(it)
            }
        }
    }


    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }


    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }


    fun logout() {
        auth.signOut()
        setupUserEnv()
    }

    fun register(userName: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                // user created
                setupUserEnv()
                val user = auth.currentUser ?: return@addOnCompleteListener
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(userName)
                    .build()

                user.updateProfile(profileUpdates).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        Log.d(TAG, "User profile updated")
                        createProfileDocument(userName, email)
                    } else {
                        Log.e(TAG, "Error updating user profile", updateTask.exception)
                    }
                }
            } else {
                showErrorMessageForUser(it)
            }
        }
    }
    //endregion

    //region create profile document update image
    private fun showErrorMessageForUser(it: Task<AuthResult>) {
        val errorCode = (it.exception as? FirebaseAuthException)?.errorCode
        val errorMessage = getErrorMessage(errorCode)
        sharedViewModel.postMessage(errorMessage)
    }

    private fun getErrorMessage(errorCode: String?): Int {
        val errorMessage = when (errorCode) {
            "ERROR_INVALID_EMAIL" -> R.string.error_invalid_email
            "ERROR_WRONG_PASSWORD" -> R.string.error_wrong_password
            "ERROR_USER_NOT_FOUND" -> R.string.error_user_not_found
            "ERROR_USER_DISABLED" -> R.string.error_user_disabled
            "ERROR_TOO_MANY_REQUESTS" -> R.string.error_too_many_requests
            "ERROR_NETWORK_REQUEST_FAILED" -> R.string.error_network_request_failed
            "ERROR_INVALID_CREDENTIAL" -> R.string.wrong_login_details
            else -> R.string.error_auth_failed
        }
        return errorMessage
    }

    private fun createProfileDocument(userName: String?, email: String?) {
        val currentUser = auth.currentUser
        val currentUserId = currentUser?.uid ?: return
        val publicData = PublicProfile(
            userId = currentUserId,
            username = userName ?: "Anonymous",
            profilePicture = currentUser.photoUrl?.toString() ?: "",
            registrationDate = Timestamp.now()
        )

        val userData = Profile(
            userId = currentUserId,
            emailAddress = email ?: "email error",
        )
        profileRef.set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "Private profile created successfully")

                fireStore.collection("publicUserProfiles").document(currentUserId)
                    .set(publicData.toHashMap())
                    .addOnSuccessListener {
                        Log.d(TAG, "Public profile data successfully created")
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "Error creating public profile", it)
                    }
            }
            .addOnFailureListener {
                Log.e(TAG, "Error creating private profile")
            }
    }

    fun uploadProfilePicture(uri: Uri, onComplete: (String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        val imageRef = storage.reference.child("images/${currentUserId}/profilePicture")
        imageRef.putFile(uri).addOnCompleteListener { it ->
            if (it.isSuccessful) {
                imageRef.downloadUrl.addOnCompleteListener { finalImageUrl ->
                    fireStore.collection("publicUserProfiles").document(currentUserId)
                        .update("profilePicture", finalImageUrl.result.toString())
                        .addOnSuccessListener {
                            Log.d(TAG, "Updated public user data in firestore")
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to update public user data in firestore", exception)
                        }
                    onComplete(finalImageUrl.result.toString())
                }
            } else {
                Log.d(TAG, "URI: $uri")
                Log.d(TAG, "Error while upload new profile picture into storage", it.exception)
            }
        }
    }

    private fun getProfilePicUrl() {
        val currentUserId = auth.currentUser?.uid ?: return
        fireStore.collection("publicUserProfiles").document(currentUserId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.e(TAG, "Snapshotlistener failed for user: $currentUserId", error)
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val url = document.getString("profilePicture")
                    _userImage.postValue(url)
                }
            }
    }
    //endregion



}