package com.example.pokinfo.viewModels.teams

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.pokinfo.R
import com.example.pokinfo.data.enums.PokemonSortOption
import com.example.pokinfo.data.models.firebase.PokemonTeam
import com.example.pokinfo.data.models.firebase.PublicProfile
import com.example.pokinfo.data.util.Event
import com.example.pokinfo.ui.teams.TeamSortFilter
import com.example.pokinfo.ui.teams.TeamType
import com.example.pokinfo.viewModels.SharedViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private const val TAG = "TeamsViewModel"


class TeamsViewModel(
    application: Application,
    private val sharedViewModel: SharedViewModel
) : AndroidViewModel(application) {
    private var auth = Firebase.auth
    private val fireStore = Firebase.firestore

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _selectedTab = MutableLiveData<Event<TeamType>>()
    val selectedTab: LiveData<Event<TeamType>> = _selectedTab

    private val _selectedUserIds = MutableLiveData<List<String>>()
    val selectedUserIds: LiveData<List<String>> get() = _selectedUserIds

    private var _allProfiles = MutableLiveData<List<PublicProfile>>()
    val allProfiles: LiveData<List<PublicProfile>> get() = _allProfiles


    private val _ownPokemonTeams = MutableLiveData<List<PokemonTeam>>()
    val ownPokemonTeams: LiveData<List<PokemonTeam>>
        get() = _ownPokemonTeams

    private val _sharedPokemonTeams = MutableLiveData<List<PokemonTeam>>()
    val sharedPokemonTeams: LiveData<List<PokemonTeam>> get() = _sharedPokemonTeams

    private val _publicPokemonTeams = MutableLiveData<List<PokemonTeam>>()

    val publicPokemonTeams: LiveData<List<PokemonTeam>> get() = _publicPokemonTeams


    private val _likedTeams = MutableLiveData<List<String>>()
    val likedTeams: LiveData<List<String>> get() = _likedTeams

    private var listenerLikedTeams: ListenerRegistration? = null

    init {
        fetchSharedTeams()
        fetchPublicPokemonTeams { }
    }

    fun updateSelectedUserIds(userIds: List<String>) {
        _selectedUserIds.value = userIds
    }

    fun setTeamDisplayMode(type: TeamType) {
        _selectedTab.value = Event(type)
    }

    private val _filterStateLiveData =
        MutableLiveData<Pair<TeamSortFilter, PokemonSortOption>>()

    fun selectFilterAndState(
        sortFilter: TeamSortFilter,
        filterState: PokemonSortOption,
    ) {
        _filterStateLiveData.value = Pair(sortFilter, filterState)
        sortAndFilterPokemon()
    }


    /**
     * Sorts the list of Pokemon in homeDetail Fragment and filters
     *
     */
    private fun sortAndFilterPokemon() {

        val initialList = _publicPokemonTeams.value.orEmpty() // every pokemon

        val (sortFilter, filterState) = _filterStateLiveData.value ?: Pair(
            TeamSortFilter.DATE,
            PokemonSortOption.DESCENDING
        )

        val sortedFilteredList = sortList(initialList, sortFilter, filterState)
        _publicPokemonTeams.value = sortedFilteredList
    }

    private fun sortList(
        list: List<PokemonTeam>,
        sortFilter: TeamSortFilter,
        filterState: PokemonSortOption
    ): List<PokemonTeam> {
        val comparator = when (sortFilter) {
            TeamSortFilter.DATE -> compareBy<PokemonTeam> { it.timestamp.seconds }
            TeamSortFilter.LIKES -> compareBy { it.likeCount }
            TeamSortFilter.ALPHABETICAL -> compareBy { it.name }
        }

        return when (filterState) {
            PokemonSortOption.ASCENDING -> list.sortedWith(comparator)
            PokemonSortOption.DESCENDING -> list.sortedWith(comparator.reversed())
            else -> list
        }
    }

    fun fetchOwnTeams() {
        fireStore.collection("pokemonTeams")
            .whereEqualTo("ownerId", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                val ownTeams = documents.mapNotNull { document ->
                    PokemonTeam.fromMap(document.data)
                }
                _ownPokemonTeams.postValue(ownTeams.sortedByDescending { it.timestamp })
                Log.d(TAG, "Fetched user pokemon teams successfully")
            }
            .addOnFailureListener {
                _ownPokemonTeams.postValue(emptyList())
                Log.e(TAG, "Failed to fetch user pokemon teams userId: $currentUserId")
            }
    }


    fun fetchSharedTeams() {
        val userId = currentUserId
        if (userId == null) {
            sharedViewModel.postMessage("User not logged in")
            return
        }
        fireStore.collection("pokemonTeams")
            .whereArrayContains("sharedWith", userId)
            .get()
            .addOnSuccessListener { documents ->
                val sharedTeams = documents.mapNotNull { document ->
                    PokemonTeam.fromMap(document.data)
                }
                _sharedPokemonTeams.postValue(sharedTeams.sortedByDescending { it.timestamp })
                Log.d(TAG, "Successfully fetched shared teams for userId $userId")
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to fetch shared teams for userId $userId", it)
                _sharedPokemonTeams.postValue(emptyList())
            }
    }

    /** Fetches all public teams from firestore */
    fun fetchPublicPokemonTeams(callback: (Boolean) -> Unit) {
        fireStore.collection("pokemonTeams")
            .whereEqualTo("isPublic", true)
            .whereNotEqualTo("ownerId", currentUserId) // exclude users own teams
            .orderBy("likeCount", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val publicTeams = snapshot.documents.mapNotNull { doc ->
                        PokemonTeam.fromMap(doc.data)
                    }
                    _publicPokemonTeams.postValue(publicTeams)
                    callback(true)
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener { e ->
                callback(false)
                Log.w("FetchError", "Fetch public teams failed.", e)
            }
    }

    /** Gets all teamsIds a user liked to sync the actual likes with the UI (display hearts in recyclerView e.g.) */
    fun listenForLikedTeams() {
        val currentUserId = auth.currentUser?.uid ?: return
        val likedTeamsRef = fireStore.collection("publicUserProfiles").document(currentUserId)
            .collection("likedTeams")
        listenerLikedTeams = likedTeamsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Listen for liked teams failed", error)
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val likedTeamsIds = snapshot.documents.mapNotNull { it.id }
                _likedTeams.value = likedTeamsIds
            } else {
                _likedTeams.value = emptyList()
            }
        }
    }

    fun stopListeningForLikedTeams() {
        listenerLikedTeams?.remove()
    }

    /** Inserts a copy of a pokemon team into firestore (whether shared or a public team)
     *  as well as updating the relevant values (creator, id, ownerId etc)
     *  as well as increasing the user teamsCount
     * */
    fun insertTeamCopyToFireStore(pokemonTeam: PokemonTeam, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            sharedViewModel.postMessage("Error, you are not signed in")
            callback(false)
            return
        }
        fireStore.runTransaction { transaction ->
            val teamsRef = fireStore.collection("pokemonTeams").document()
            pokemonTeam.creator = auth.currentUser?.displayName ?: "Anonymous"
            pokemonTeam.id = teamsRef.id
            pokemonTeam.ownerId = userId
            pokemonTeam.sharedWith = emptyList()
            pokemonTeam.likeCount = 0
            val teamForFireStore = pokemonTeam.toHashMap()

            transaction.set(teamsRef, teamForFireStore) // save team
            // increment team count on user profile
            val userProfileRef = fireStore.collection("publicUserProfiles").document(userId)
            transaction.update(userProfileRef, "teamsCount", FieldValue.increment(1))

            null
        }.addOnSuccessListener {
            Log.d(TAG, "Insertion of team copy successfully")
            callback(true)
            sharedViewModel.postMessage(R.string.successfully_saved_team)
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Insertion of team copy failed", exception)
            callback(false)
            sharedViewModel.postMessage(R.string.failed_to_insert_team)
        }
    }

    fun updateTeamNameAndPublicity(newName: String, isPublic: Boolean, teamId: String) {
        val teamRef = fireStore.collection("pokemonTeams").document(teamId)
        teamRef.update(
            mapOf(
                "Name" to newName,
                "isPublic" to isPublic
            )
        ).addOnSuccessListener {
            Log.d(TAG, "Updated Team name successfully")
        }.addOnFailureListener {
            Log.e(TAG, "Failed to update team name / publicity for team: $teamId")
        }
    }

    /** Deletes a Pokemon Team and deletes it from every user like documents
     *  as well as increasing the teamsCount in the public user document
     * */
    fun deletePokemonTeam(pokemonTeam: PokemonTeam, onFinish: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val teamId = pokemonTeam.id
        val teamRef = fireStore.collection("pokemonTeams").document(teamId)
        val usersRef = fireStore.collection("publicUserProfiles")

        usersRef.get().addOnSuccessListener { snapshot ->
            val userIds = snapshot.documents.map { it.id }
            val userDocument =
                snapshot.documents.find { it.id == userId } ?: return@addOnSuccessListener
            fireStore.runTransaction { transaction ->
                // loop through every user to delete the liked teams out of their liked teams collection
                val likedTeamsToDelete = mutableListOf<DocumentReference>()
                userIds.forEach { userId ->
                    val likedTeamRef = usersRef.document(userId)
                        .collection("likedTeams").document(teamId)
                    val docSnapshot = transaction.get(likedTeamRef)
                    if (docSnapshot.exists()) {
                        likedTeamsToDelete.add(docSnapshot.reference)
                    }
                }
                // delete team, decrease teamsCount and delete like documents from other users
                transaction.delete(teamRef)
                transaction.update(userDocument.reference, "teamsCount", FieldValue.increment(-1))
                likedTeamsToDelete.forEach { transaction.delete(it) }
            }.addOnSuccessListener {
                Log.d(TAG, "Team successfully deleted")
                onFinish(true)
                sharedViewModel.postMessage(R.string.team_deleted)
            }.addOnFailureListener { exception ->
                Log.w(TAG, "Error deleting team", exception)
                onFinish(false)
                sharedViewModel.postMessage(R.string.team_deleted_error)
            }
        }.addOnFailureListener { exception ->
            onFinish(false)
            Log.w(TAG, "Failed to retrieve profiles for team deletion", exception)
            sharedViewModel.postMessage(R.string.team_deleted_error)
        }
    }

    /** Gets every public user profile  from firestore converts them to data class and updated the live-data to show them */
    fun getUsersToShareTeamsWith() {
        val allProfiles = mutableListOf<PublicProfile>()
        val userId = auth.currentUser?.uid
        // fetch every public user profile and add it to the list
        fireStore.collection("publicUserProfiles")
            .whereNotEqualTo("userId", userId).get().addOnSuccessListener { snapShot ->
                if (!snapShot.isEmpty) {
                    snapShot.documents.forEach { document ->
                        val profile = PublicProfile.fromMap(document.data)
                        if (profile != null && profile.username != userId) {
                            allProfiles.add(profile)
                        }
                    }
                    _allProfiles.postValue(allProfiles)
                }
            }
    }

    /**
     * Grants access for a list of users to the pokemon team of the actual user
     * */
    fun grantAccessToOtherUser(pokemonTeam: PokemonTeam) {
        val userIds = _selectedUserIds.value ?: emptyList()
        val docRef = fireStore.collection("pokemonTeams").document(pokemonTeam.id)
        pokemonTeam.sharedWith = userIds
        docRef.update("sharedWith", FieldValue.arrayUnion(*userIds.toTypedArray()))
            .addOnSuccessListener {
                sharedViewModel.postMessage(R.string.share_success)
                Log.d(TAG, "User ID successfully added to sharedWith list.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error while adding userIds to sharedWith List for teamId: ${pokemonTeam.id}", e)
                sharedViewModel.postMessage(R.string.unexpected_error)
            }
    }

    /** Function to let the user remove shared teams for himself from a pokemonTeams sharedList */
    fun removeAccessToPokemonTeam(pokemonTeam: PokemonTeam) {
        val userId = auth.currentUser?.uid ?: return
        val docRef = fireStore.collection("pokemonTeams").document(pokemonTeam.id)

        docRef.update("sharedWith", FieldValue.arrayRemove(userId))
            .addOnSuccessListener {
                Log.d("DELETED ACCESS", "Successfully deleted access to other team")
                sharedViewModel.postMessage(R.string.success_remove_access)
            }
            .addOnFailureListener {
                Log.d("Deletion of Access", "Failed to delete access to other team", it)
                sharedViewModel.postMessage(R.string.error_remove_access)
            }
    }

    /** Get public user data to display a profile or w/e */
    fun getPublicUserData(creatorId: String, callback: (PublicProfile) -> Unit) {
        fireStore.collection("publicUserProfiles").document(creatorId).get()
            .addOnSuccessListener { document ->
                val profile = PublicProfile.fromMap(document.data?.toMap())
                profile?.let(callback)
            }
            .addOnFailureListener {
                Log.d(TAG, "Failed to get user data for id: $creatorId", it)
            }
    }

    /** Gets all teams of a public user profile to display them in a recycler view or w/e */
    fun getTeamsByUser(userId: String, onComplete: (List<PokemonTeam>) -> Unit) {
        fireStore.collection("pokemonTeams")
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("isPublic", true)
            .get()
            .addOnSuccessListener { documents ->
                val teams = mutableListOf<PokemonTeam>()
                documents.forEach { document ->
                    PokemonTeam.fromMap(document.data)?.let { teams.add(it) }
                }
                onComplete(teams)
            }
            .addOnFailureListener {
                Log.e(TAG, "Error getting documents for user: $userId", it)
                onComplete(emptyList())
            }
    }

    private fun toggleUserTeamLike(teamId: String, ownerId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val likedTeamsRef = fireStore.collection("publicUserProfiles").document(currentUserId)
            .collection("likedTeams")
        // document Reference, if it does not exist means the user has not liked the team by now
        val teamDocRef = likedTeamsRef.document(teamId)

        // get document
        teamDocRef.get().addOnSuccessListener { document ->
            // remove the like document
            if (document.exists()) {
                teamDocRef.delete().addOnSuccessListener {
                    Log.d(TAG, "Successfully removed like for user teamLikes list user: $currentUserId team: $teamId")
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to remove like for user teamLiked list user: $currentUserId team: $teamId", exception)
                }
            } else {
                // fill document
                teamDocRef.set(mapOf("ownerId" to ownerId))
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully added like for user teamLikes list user: $currentUserId team: $teamId")
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to add like for user teamLiked list user: $currentUserId team: $teamId", exception)
                    }
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Error accessing firestore: $exception")
        }
    }

    /** Increments the like count of a pokemon team when like button was clicked,
     *  as well as updating the total like count of the user which team was liked
     * */
    fun incrementLikeCount(ownerId: String, teamId: String) {
        val userRef = fireStore.collection("publicUserProfiles").document(ownerId)
        val teamRef = fireStore.collection("pokemonTeams").document(teamId)

        fireStore.runTransaction { transaction ->
            val teamSnapshot = transaction.get(teamRef)
            val userSnapshot = transaction.get(userRef)

            // likes for the single team
            val newTeamLikeCount = (teamSnapshot.getLong("likeCount") ?: 0) + 1
            // whole user liked
            val newUserLikeCount = (userSnapshot.getLong("likeCount") ?: 0) + 1
            transaction.update(teamRef, "likeCount", newTeamLikeCount)
            transaction.update(userRef, "likeCount", newUserLikeCount)
            null

        }.addOnSuccessListener {
                Log.d(TAG, "Like count successfully incremented on teamId: $teamId \n and on user $ownerId ")
                toggleUserTeamLike(teamId, ownerId)
        }.addOnFailureListener {
                Log.e(TAG, "Failed to increment like count for teamId: $teamId and on user $ownerId", it)
        }

    }

    /** Decrements the like count of a pokemon team when like button was clicked,
     *  as well as updating the total like count of the user which team was unliked
     * */
    fun decrementLikeCount(ownerId: String, teamId: String) {
        val userRef = fireStore.collection("publicUserProfiles").document(ownerId)
        val teamRef = fireStore.collection("pokemonTeams").document(teamId)

        fireStore.runTransaction { transaction ->
            val team = transaction.get(teamRef)
            val user = transaction.get(userRef)

            val oldLikeCountTeam = team.getLong("likeCount") ?: 0
            val oldLikeCountUser = user.getLong("likeCount") ?: 0

            if (oldLikeCountTeam > 0L) {
                val newLikeCount = oldLikeCountTeam - 1
                transaction.update(teamRef, "likeCount", newLikeCount)
            }
            if (oldLikeCountUser > 0L) {
                val newLikeCountUser = oldLikeCountUser - 1
                transaction.update(userRef, "likeCount", newLikeCountUser)
            }
        }.addOnSuccessListener {
                Log.d(TAG, "Like count successfully decremented on teamId: $teamId")
                toggleUserTeamLike(teamId, ownerId)
        }
        .addOnFailureListener {
                Log.e(TAG, "Failed to decrement like count for teamId: $teamId", it)
        }
    }
}