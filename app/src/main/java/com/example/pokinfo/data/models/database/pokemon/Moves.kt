package com.example.pokinfo.data.models.database.pokemon

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Entity(
    tableName = "pokemon_moves_list",
    foreignKeys = [
        ForeignKey(
            entity = Pokemon::class,
            parentColumns = ["id"],
            childColumns = ["pokemonId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
)
data class PkMoves(
    @PrimaryKey
    val pokemonId: Int,
    val moveIds: List<Long>
)

@Entity(
    tableName = "pokemon_move_data",
    foreignKeys = [
        ForeignKey(
            entity = PokemonType::class,
            parentColumns = ["id"],
            childColumns = ["typeId"]
        )
    ],
    indices = [Index(value = ["typeId"])]

)
data class PkMove(
    @PrimaryKey val id: Long,
    val name: String,
    val typeId: Int,
    val accuracy: Int?,
    val effectText: String?,
    val moveDamageClassId: Int?,
    val type: String? = null,
    val power: Int? = null,
    val ap: Int? = null
)

@Entity(
    tableName = "pokemon_move_names",
    foreignKeys = [
        ForeignKey(
            entity = PkMove::class,
            parentColumns = ["id"],
            childColumns = ["moveId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["moveId"])]
)
data class PkMoveNames(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moveId: Int,
    val name: String,
    val languageId: Int
)

@Parcelize
@Entity(
    tableName = "pokemon_move_version_group_details",
    foreignKeys = [
        ForeignKey(
            entity = PkMove::class,
            parentColumns = ["id"],
            childColumns = ["moveId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Pokemon::class,
            parentColumns = ["id"],
            childColumns = ["pokemonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["moveId"]), Index(value = ["pokemonId"])]

)
data class PkMoveVersionGroupDetail(
    @PrimaryKey val id: Long = 0,
    val moveId: Long,
    val levelLearnedAt: Int?,
    val pokemonId: Int?,
    val moveLearnMethod: String?,
    val moveLearnMethodId: Int?,
    val versionGroupId: Int?
) : Parcelable

@Entity(
    tableName = "pokemon_move_machines",
    foreignKeys = [
        ForeignKey(entity = PkMove::class, parentColumns = ["id"], childColumns = ["moveId"]),
    ],
    indices = [Index(value = ["moveId"])]
)
data class PkMoveMachines(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moveId: Int,
    val machineNr: Int,
    val versionGroupId: Int,
)
