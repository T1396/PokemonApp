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
import com.example.pokinfo.data.models.Profile
import com.example.pokinfo.data.models.PublicProfile
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.ui.loginRegister.ContextProvider
import com.example.pokinfo.ui.teams.TeamType
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch

private const val TAG = "FirebaseViewModel"

class FirebaseViewModel(
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

    private var _allProfiles = MutableLiveData<List<PublicProfile>>()
    val allProfiles: LiveData<List<PublicProfile>> get() = _allProfiles

    private val _selectedTab = MutableLiveData(TeamType.MY_TEAMS)
    val selectedTab: LiveData<TeamType> get() = _selectedTab


    private val _selectedUserIds = MutableLiveData<List<String>>()
    val selectedUserIds: LiveData<List<String>> get() = _selectedUserIds

    fun updateSelectedUserIds(userIds: List<String>) {
        _selectedUserIds.value = userIds
    }

    fun setTeamDisplayMode(type: TeamType) {
        _selectedTab.value = type
    }

    init {
        _selectedUserIds.value = emptyList()
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
            listenForPokemonTeams()
            listenForPublicTeams()
        }
    }

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
                val result = credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )
                handleSignIn(result)
            } catch (e: NoCredentialException) {
                sharedViewModel.postMessage(R.string.google_error)
                Log.d(TAG, "Get Credential Error", e)
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "Credential Login/Sign in cancelled by user")
            } catch (e: Exception) {
                // error
                Log.e(TAG, "Get Credential Error", e)
                sharedViewModel.postMessage(R.string.credential_error)
            }
        }


    }

    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential
        try {
            // Use googleIdTokenCredential and extract id to validate and
            // authenticate on your server
            val googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(credential.data)
            val googleIdToken = googleIdTokenCredential.idToken
            signInGoogle(googleIdToken)
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Received an invalid google id token response", e)
        }
    }


//region firebase authentication and image update


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
                Log.e(TAG, it.exception.toString())
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
                createProfileDocument(userName, email)
            } else {
                showErrorMessageForUser(it)
            }
        }
    }

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
        val userData = Profile(
            username = userName ?: "Anonymous",
            emailAddress = email ?: "email error",
            registrationDate = Timestamp.now(),
            profilePicture = currentUser.photoUrl?.toString() ?: ""
        )
        profileRef.set(userData)

        val publicProfileData = mapOf(
            "userId" to currentUserId,
            "username" to userData.username,
            "profilePicture" to userData.profilePicture
        )

        val publicProfilesRef = fireStore.collection("publicUserProfiles").document(currentUser.uid)

        publicProfilesRef.set(publicProfileData)
            .addOnSuccessListener {
                Log.d(TAG, "Public profile updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating public profile", e)
                sharedViewModel.postMessage(R.string.error_user_not_found)
            }
    }

    fun uploadProfilePicture(uri: Uri, onComplete: (Uri?) -> Unit) {
        val imageRef = storage.reference.child("images/${auth.currentUser?.uid}/profilePicture")
        imageRef.putFile(uri).addOnCompleteListener { it ->
            if (it.isSuccessful) {
                imageRef.downloadUrl.addOnCompleteListener { finalImageUrl ->
                    profileRef.update("profilePicture", finalImageUrl.result.toString())
                    onComplete(finalImageUrl.result)
                    val currentUserId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val publicUserCollection =
                        fireStore.collection("publicUserProfiles").document(currentUserId)
                    publicUserCollection.update("profilePicture", finalImageUrl.result.toString())
                        .addOnSuccessListener {
                            Log.d(TAG, "Updated public user data in firestore")
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to update public user data in firestore", exception)
                        }
                }


            } else {
                Log.d(
                    TAG,
                    "Error while upload new profile picture into storage",
                    it.exception
                )
            }
        }
    }

    fun getProfilePicture(onComplete: (Uri?) -> Unit) {
        val imageRef = storage.reference.child("images/${auth.currentUser?.uid}/profilePicture")
        imageRef.downloadUrl.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(task.result)
            } else {
                Log.d("error", "image could not be loaded", task.exception)
                onComplete(null)
            }
        }
    }

//endregion


//region PokemonTeams

    private val _ownPokemonTeams = MutableLiveData<List<PokemonTeam>>()
    val ownPokemonTeams: LiveData<List<PokemonTeam>>
        get() = _ownPokemonTeams

    private val _sharedPokemonTeams = MutableLiveData<List<PokemonTeam>>()
    val sharedPokemonTeams: LiveData<List<PokemonTeam>> get() = _sharedPokemonTeams

    private val _publicPokemonTeams = MutableLiveData<List<PokemonTeam>>()
    val publicPokemonTeams: LiveData<List<PokemonTeam>> get() = _publicPokemonTeams


    private fun listenForPublicTeams() {
        val currentUserId = auth.currentUser?.uid ?: return
        fireStore.collection("pokemonTeams")
            .whereEqualTo("isPublic", true)
            .whereNotEqualTo("ownerId", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ListenError", "Listen for public teams failed.", e)
                    _publicPokemonTeams.postValue(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val publicTeams = snapshot.documents.mapNotNull { doc ->
                        PokemonTeam.fromMap(doc.data as Map<String, Any?>, doc.id)
                    }
                    _publicPokemonTeams.postValue(publicTeams)
                } else {
                    _publicPokemonTeams.postValue(emptyList())
                }
            }
    }

    private fun listenForPokemonTeams() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            sharedViewModel.postMessage("User not logged in")
            return
        }

        // Listener für eigene Teams
        fireStore.collection("pokemonTeams")
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ListenError", "Listen failed.", e)
                    _ownPokemonTeams.postValue(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val ownTeams = snapshot.documents.mapNotNull { doc ->
                        PokemonTeam.fromMap(doc.data as Map<String, Any?>, doc.id)
                    }
                    _ownPokemonTeams.postValue(ownTeams)
                } else {
                    _ownPokemonTeams.postValue(emptyList())
                }
            }

        // Listener für geteilte Teams
        fireStore.collection("pokemonTeams")
            .whereArrayContains("sharedWith", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ListenError", "Listen failed.", e)
                    _sharedPokemonTeams.postValue(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val sharedTeams = snapshot.documents.mapNotNull { doc ->
                        PokemonTeam.fromMap(doc.data as Map<String, Any?>, doc.id)
                    }
                    _sharedPokemonTeams.postValue(sharedTeams)
                } else {
                    _sharedPokemonTeams.postValue(emptyList())
                }
            }
    }



    fun insertTeamToFireStore(pokemonTeam: PokemonTeam, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            sharedViewModel.postMessage("Error, you are not signed in")
            return
        }

        val teamsRef = fireStore.collection("pokemonTeams").document()
        pokemonTeam.id = teamsRef.id
        pokemonTeam.ownerId = userId
        pokemonTeam.sharedWith = emptyList() // if this function is used to copy a team that is shared for someone to remove the accessList of the copied team
        val teamForFireStore = pokemonTeam.toHashMap()
        teamsRef.set(teamForFireStore)
            .addOnSuccessListener {
                Log.d("MainViewModel", "Successfully saved team into fireStore")
                callback(true)
                sharedViewModel.postMessage(R.string.successfully_saved_team)
            }
            .addOnFailureListener {
                Log.d("MainViewModel", "Failed to save team into fireStore", it)
                callback(false)
                sharedViewModel.postMessage(R.string.failed_to_insert_team)
            }
    }

    fun updateTeam(pokemonTeam: PokemonTeam): Boolean {
        return try {
            val teamDocumentReference =
                fireStore.collection("pokemonTeams").document(pokemonTeam.id)
            teamDocumentReference.update(pokemonTeam.toHashMap())
            sharedViewModel.postMessage(R.string.success_update)
            true
        } catch (e: Exception) {
            Log.d(TAG, "Failed to update team in fireStore", e)
            sharedViewModel.postMessage(R.string.failed_to_update_team)
            false
        }
    }

    fun deletePokemonTeam(pokemonTeam: PokemonTeam) {
        val teamId = pokemonTeam.id

        val teamRef = fireStore.collection("pokemonTeams").document(teamId)
        teamRef.delete().addOnSuccessListener {
            Log.d(TAG, "Team successfully deleted")
            sharedViewModel.postMessage(R.string.team_deleted)
        }.addOnFailureListener {
            Log.w(TAG, "Error deleting team", it)
            sharedViewModel.postMessage(R.string.team_deleted_error)
        }
    }

    fun getUsersToShareTeamsWith() {
        //val sharedWithIds = pokemonTeam.accessList
        val allProfiles = mutableListOf<PublicProfile>()
        val currentUserName = auth.currentUser?.displayName
        fireStore.collection("publicUserProfiles")
            .get().addOnSuccessListener { snapShot ->
                if (!snapShot.isEmpty) {
                    snapShot.documents.forEach { document ->
                        val profile = PublicProfile.fromMap(document.data ?: emptyMap())
                        if (profile != null && /*!sharedWithIds.contains(document.id) &&*/ profile.username != currentUserName) {
                            allProfiles.add(profile)
                        }
                    }
                    _allProfiles.postValue(allProfiles)
                }
            }
    }

    fun grantAccessToOtherUser(pokemonTeam: PokemonTeam, userIds: List<String>) {
        val docRef = fireStore.collection("pokemonTeams").document(pokemonTeam.id)

        docRef.update("sharedWith", FieldValue.arrayUnion(*userIds.toTypedArray()))
            .addOnSuccessListener {
                Log.d(TAG, "User ID erfolgreich zur accessList hinzugefügt.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Fehler beim Hinzufügen der User ID zur accessList", e)
            }
    }

    fun removeAccessToPokemonTeam(pokemonTeam: PokemonTeam) {
        val userId = auth.currentUser?.uid ?: return
        val docRef = fireStore.collection("pokemonTeams").document(pokemonTeam.id)

        docRef.update("sharedWith", FieldValue.arrayRemove(userId))
            .addOnSuccessListener {
                Log.d("DELETED ACCESS", "Successfully deleted access to other team")
            }
            .addOnFailureListener {
                Log.d("Deletion of Access", "Failed to delete access to other team", it)
            }
    }

    fun resetUserList() {
        _allProfiles.postValue(emptyList())
    }

//endregion

}