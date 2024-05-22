package com.example.pokinfo.viewModels.teambuilder

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokinfo.data.models.database.pokemon.StatValues
import com.example.pokinfo.data.models.firebase.EvIvData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class PokemonStats(
    var ev: Int = 0,
    var iv: Int = 31,
    val baseStat: Int = 0
)

enum class StatsEnum(val index: Int) {
    HP(1),
    ATK(2),
    DEF(3),
    SPATK(4),
    SPDEF(5),
    INIT(6);
}

class StatManagerViewModel : ViewModel() {
    private val maxEvs = 508
    private val maxEvsPerStat = 252
    private val maxIvsPerStat = 31
    private val minLevel = 1
    private val maxLevel = 100

    private var baseValues: Map<StatsEnum, Int> = emptyMap()

    fun reset() {
        baseValues = emptyMap()
        _evsMap.value = StatsEnum.entries.associateWith { PokemonStats().ev }
        _ivsMap.value = StatsEnum.entries.associateWith { PokemonStats().iv }
        _remainingEvs.value = maxEvs
        _level.value = maxLevel
        updateAllStats()
    }

    fun setPokemonBaseValues(stats: List<StatValues>) {
        baseValues = StatsEnum.entries.associateWith { statEnum ->
            val stat = stats.find { it.statId == statEnum.index }
            stat?.statValue ?: 0
        }
        updateAllStats()
    }

    private val _evsMap = MutableLiveData(
        StatsEnum.entries.associateWith { PokemonStats().ev }
    )
    val evsMap: LiveData<Map<StatsEnum, Int>> get() = _evsMap

    fun insertEvs(evs: List<EvIvData>) {
        val evMap = _evsMap.value?.toMutableMap() ?: mutableMapOf()
        evs.forEach { evData ->
            val mapKey = evMap.keys.find { it.index == evData.statId } ?: return@forEach
            evMap[mapKey] = evData.value
        }
        _evsMap.value = (evMap)
    }

    fun insertIvs(ivs: List<EvIvData>) {
        val ivMap = _ivsMap.value?.toMutableMap() ?: mutableMapOf()
        ivs.forEach { ivData ->
            val mapKey = ivMap.keys.find { it.index == ivData.statId } ?: return@forEach
            ivMap[mapKey] = ivData.value
        }
        _ivsMap.value = (ivMap)
    }


    private val _ivsMap = MutableLiveData(
        StatsEnum.entries.associateWith { PokemonStats().iv }
    )
    val ivsMap: LiveData<Map<StatsEnum, Int>> get() = _ivsMap

    val ivsList: List<EvIvData>
        get() {
            val ivs = _ivsMap.value ?: return emptyList()
            return ivs.map { (stat, value) ->
                EvIvData(
                    statId = stat.index,
                    value = value
                )
            }
        }

    val evsList: List<EvIvData>
        get() {
            val evs = _evsMap.value ?: return emptyList()
            return evs.map { (stat, value) ->
                EvIvData(
                    statId = stat.index,
                    value = value
                )
            }
        }

    private val _remainingEvs = MutableLiveData(maxEvs)
    val remainingEvs: LiveData<Int> get() = _remainingEvs

    private val _level = MutableLiveData(maxLevel)
    val level: LiveData<Int> get() = _level

    fun updateLevel(level: Int) {
        val newLevel = level.coerceIn(minLevel, maxLevel)
        val oldLevel = _level.value ?: 100
        if (newLevel != oldLevel) {
            _level.value = newLevel
            updateAllStats()
        }
    }

    fun updateIvStat(stat: StatsEnum, value: Int) {
        val currentIvs = _ivsMap.value?.toMutableMap() ?: return
        val newIvValue = value.coerceIn(0, maxIvsPerStat)
        val oldIvValue = currentIvs[stat] ?: 31
        if (newIvValue != oldIvValue) {
            currentIvs[stat] = newIvValue
            _ivsMap.value = currentIvs
            updateSingleCalculatedValue(stat)
        }
    }

    fun updateEvStat(stat: StatsEnum, value: Int) {

        val currentEvs = _evsMap.value?.toMutableMap() ?: return
        var newStatValue = value.coerceIn(0, maxEvsPerStat)
        val oldValue = currentEvs[stat] ?: 0

        if (newStatValue == oldValue) return

        val totalAssigned = currentEvs.values.sum() - oldValue
        var totalAfterUpdate = totalAssigned + newStatValue

        if (totalAfterUpdate > maxEvs) {
            totalAfterUpdate -= newStatValue
            newStatValue = maxEvs - totalAssigned
            totalAfterUpdate += newStatValue
        }
        currentEvs[stat] = newStatValue
        _remainingEvs.postValue(maxEvs - totalAfterUpdate)
        _evsMap.value = currentEvs
        updateSingleCalculatedValue(stat)
    }

    private val _calculatedStats = MutableLiveData<Map<StatsEnum, Int>?>(null)
    val calculatedStats: LiveData<Map<StatsEnum, Int>?> get() = _calculatedStats


    private fun updateSingleCalculatedValue(stat: StatsEnum) {
        val calculatedStats = _calculatedStats.value?.toMutableMap() ?: return
        calculatedStats[stat] = calculateStat(stat)
        _calculatedStats.postValue(calculatedStats)
    }

    private fun updateAllStats() {
        val calculatedStats = StatsEnum.entries.associateWith { calculateStat(it) }
        _calculatedStats.postValue(calculatedStats)
    }


    /** Calculates the resulting value depending on the base/iv/ev Values for that stat and
     *  the Pokemon Level (e.g. Attack or HP or Init)
     * @return resulting Stat
     */
    private fun calculateStat(
        stat: StatsEnum
    ): Int {
        // hp is calculated different
        val baseValue = baseValues[stat] ?: return 0
        if (baseValue == 0) return 0
        val level = _level.value ?: maxLevel
        val ivValue = _ivsMap.value?.get(stat) ?: maxIvsPerStat
        val evValue = _evsMap.value?.get(stat) ?: maxEvsPerStat
        return when (stat) {
            StatsEnum.HP -> (((2 * baseValue + ivValue + (evValue / 4)) * level) / 100) + level + 10
            else -> (((2 * baseValue) + ivValue + (evValue / 4)) * level) / 100 + 5
        }
    }
}