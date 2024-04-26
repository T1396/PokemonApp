package com.example.pokinfo.viewModels

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.pokinfo.R
import com.example.pokinfo.data.models.Profile
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage


class FirebaseViewModel(private val application: Application) : AndroidViewModel(application) {


    private var auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    private val _user: MutableLiveData<FirebaseUser?> = MutableLiveData()
    val user: LiveData<FirebaseUser?>
        get() = _user

    private val _errorMessage: MutableLiveData<String> = MutableLiveData()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private lateinit var profileRef: DocumentReference

    private val _signInResult: MutableLiveData<Intent?> = MutableLiveData()
    val signInResult: LiveData<Intent?>
        get() = _signInResult

    fun isUserLoggedIn(): Boolean {
        val user = auth.currentUser
        return user != null
    }

    private val _messageSender: MutableLiveData<Int> = MutableLiveData()
    val messageSender: LiveData<Int>
        get() = _messageSender


    init {
        setupUserEnv()
    }

    private fun setupUserEnv() {

        _user.value = auth.currentUser
        auth.currentUser?.let { firebaseUser ->
            profileRef = firestore.collection("userData").document(firebaseUser.uid)
        }
    }

    fun startGoogleSignIn(string: String) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(string)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(application, gso)
        googleSignInClient.signOut()
        val signInIntent = googleSignInClient.signInIntent
        _signInResult.value = signInIntent
    }

    fun handleGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken)
        } catch (e: ApiException) {
            _errorMessage.value = "Google sign in failed: ${e.statusCode}"
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        if (idToken == null) {
            _errorMessage.value = "Google ID Token is missing"
            return
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    setupUserEnv()
                    updateProfile()
                } else {
                    _errorMessage.value = "Authentication Failed."
                }
            }
    }


    //region firebase authentication and image update


    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                setupUserEnv()
            } else {
                _errorMessage.value = it.exception?.message
            }
        }
    }

    fun logout() {
        auth.signOut()
        setupUserEnv()
        val googleSignInClient = GoogleSignIn.getClient(
            application,
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
        googleSignInClient.signOut()
        _signInResult.value = null
    }

    fun register(userName: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                //User wurde erstellt
                setupUserEnv()
                val newProfile = Profile(userName, email, Timestamp.now())
                profileRef.set(newProfile)
            } else {
                _errorMessage.value = it.exception?.message
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
                Log.d("FirebaseViewModel", "Error while upload new profile picture into storage", it.exception)
            }
        }
    }

    fun getProfilePicture(onComplete: (Uri?) -> Unit) {
        val imageRef = storage.reference.child("images/${auth.currentUser?.uid}/profilePicture")
        imageRef.downloadUrl.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(task.result)
            } else {
                Log.d("error", "Bild konnte nicht geladen werden")
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

    fun listenForTeamsInFirestore(onPostValue: () -> Unit) {
        val allTeamsList = mutableListOf<PokemonTeam>()
        val teamsRef = profileRef.collection("createdTeams")

        teamsListenerRegistration = teamsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("FirebaseViewModel", "Listen for pokemon Teams failed", error)
                _errorMessage.postValue("An error occured while fetching pokemon teams")
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


            Log.d("snapshotListener", "converted Teams: $allTeamsList")
        }
    }


    fun insertTeamToFireStore(pokemonTeam: PokemonTeam, callback: (Boolean) -> Unit) {
        val documentRef = profileRef.collection("createdTeams").document()
        pokemonTeam.id = documentRef.id
        val teamForFireStore = pokemonTeam.toHashMap()
        documentRef.set(teamForFireStore)
            .addOnSuccessListener {
                Log.d("MainViewModel", "Successfully saved team into firestore")
                callback(true)
                _messageSender.value = R.string.successfully_saved_team
            }
            .addOnFailureListener {
                Log.d("MainViewModel", "Failed to save team into firestore", it)
                callback(false)
                _messageSender.value = R.string.failed_to_insert_team
            }
    }

    fun updateTeam(pokemonTeam: PokemonTeam) {
        try {
            val teamDocumentReference = profileRef.collection("createdTeams").document(pokemonTeam.id)
            teamDocumentReference.update(pokemonTeam.toHashMap())
            _messageSender.value = R.string.success_update
        } catch (e: Exception) {
            Log.d("FirebaseViewModel", "Failed to update team in firestore", e)
        }

    }

    fun deletePokemonTeam(pokemonTeam: PokemonTeam) {
        val teamId = pokemonTeam.id
        val teamRef = profileRef.collection("createdTeams").document(teamId)
        teamRef.delete().addOnSuccessListener {
            Log.d("FirebaseViewModel", "Team successfully deleted")
            _messageSender.postValue(R.string.team_deleted)
        }.addOnFailureListener {
            Log.w("FirebaseViewModel", "Error deleting team", it)
            _messageSender.postValue(R.string.team_deleted_error)
        }
    }

    //endregion

}