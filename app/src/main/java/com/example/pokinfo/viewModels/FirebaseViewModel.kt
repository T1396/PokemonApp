package com.example.pokinfo.viewModels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pokinfo.BuildConfig
import com.example.pokinfo.R
import com.example.pokinfo.data.models.Profile
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.ui.loginRegister.ContextProvider
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch


class FirebaseViewModel(
    private val application: Application,
    private val sharedViewModel: SharedViewModel
) : AndroidViewModel(application) {

    private var auth = Firebase.auth
    private val fireStore = Firebase.firestore
    private val storage = Firebase.storage

    private val _user: MutableLiveData<FirebaseUser?> = MutableLiveData()
    val user: LiveData<FirebaseUser?>
        get() = _user

    private lateinit var profileRef: DocumentReference
    private val webClientId = BuildConfig.webClientId


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
        }
    }

    private fun signInGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser == true
                    setupUserEnv()
                    if (isNewUser) updateProfile()
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
                sharedViewModel.postMessage(R.string.no_google_account)
                Log.d("FirebaseViewModel", "Get Credential Error", e)
            } catch (e: Exception) {
                // error
                Log.e("FirebaseViewModel", "Get Credential Error", e)
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
        Log.e("FirebaseViewModel", "Received an invalid google id token response", e)
    }
}


//region firebase authentication and image update


fun login(email: String, password: String) {
    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
        if (it.isSuccessful) {
            setupUserEnv()
        } else {
            sharedViewModel.postMessage(it.exception?.message ?: "Error with Firebase Login")
        }
    }
}

fun logout() {
    auth.signOut()
    setupUserEnv()
}

fun register(email: String, password: String) {
    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
        if (it.isSuccessful) {
            // user created
            setupUserEnv()
            updateProfile()
        } else {
            sharedViewModel.postMessage(it.exception?.message ?: "Error registering to firebase")
        }
    }
}

private fun updateProfile() {
    val currentUser = auth.currentUser
    if (currentUser != null) {
        profileRef.set(
            Profile(
                currentUser.displayName ?: "",
                currentUser.email ?: "",
                Timestamp.now(),
            )
        )
    }
}

fun uploadProfilePicture(uri: Uri, onComplete: (Uri?) -> Unit) {
    val imageRef = storage.reference.child("images/${auth.currentUser?.uid}/profilePicture")
    imageRef.putFile(uri).addOnCompleteListener {
        if (it.isSuccessful) {
            imageRef.downloadUrl.addOnCompleteListener { finalImageUrl ->
                profileRef.update("profilePicture", finalImageUrl.result.toString())
                onComplete(finalImageUrl.result)
            }
        } else {
            Log.d(
                "FirebaseViewModel",
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

private val _pokemonTeams = MutableLiveData<List<PokemonTeam>>()
val pokemonTeams: LiveData<List<PokemonTeam>>
    get() = _pokemonTeams

private var teamsListenerRegistration: ListenerRegistration? = null

fun stopListeningForTeams() {
    teamsListenerRegistration?.remove()
}

fun listenForTeamsInFireStore(onPostValue: () -> Unit) {
    val allTeamsList = mutableListOf<PokemonTeam>()
    val teamsRef = profileRef.collection("createdTeams")

    teamsListenerRegistration = teamsRef.addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.w("FirebaseViewModel", "Listen for pokemon Teams failed", error)
            sharedViewModel.postMessage("An error occurred while fetching pokemon teams")
            return@addSnapshotListener
        }

        allTeamsList.clear()
        snapshot?.forEach { document ->
            val teamData = document.data
            val teamMapped = PokemonTeam.fromMap(teamData, document.id)
            if (teamMapped?.pokemons?.any { it != null } == true) allTeamsList.add(teamMapped)
        }

        _pokemonTeams.value = allTeamsList
        onPostValue()
    }
}


fun insertTeamToFireStore(pokemonTeam: PokemonTeam, callback: (Boolean) -> Unit) {
    val documentRef = profileRef.collection("createdTeams").document()
    pokemonTeam.id = documentRef.id
    val teamForFireStore = pokemonTeam.toHashMap()
    documentRef.set(teamForFireStore)
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
            profileRef.collection("createdTeams").document(pokemonTeam.id)
        teamDocumentReference.update(pokemonTeam.toHashMap())
        sharedViewModel.postMessage(R.string.success_update)
        true
    } catch (e: Exception) {
        Log.d("FirebaseViewModel", "Failed to update team in fireStore", e)
        sharedViewModel.postMessage(R.string.failed_to_update_team)
        false
    }
}

fun deletePokemonTeam(pokemonTeam: PokemonTeam) {
    val teamId = pokemonTeam.id
    val teamRef = profileRef.collection("createdTeams").document(teamId)
    teamRef.delete().addOnSuccessListener {
        Log.d("FirebaseViewModel", "Team successfully deleted")
        sharedViewModel.postMessage(R.string.team_deleted)
    }.addOnFailureListener {
        Log.w("FirebaseViewModel", "Error deleting team", it)
        sharedViewModel.postMessage(R.string.team_deleted_error)
    }
}

//endregion

}