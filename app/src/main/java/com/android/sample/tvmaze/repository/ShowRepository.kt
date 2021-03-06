package com.android.sample.tvmaze.repository

import android.content.Context
import com.android.sample.tvmaze.R
import com.android.sample.tvmaze.database.ShowDao
import com.android.sample.tvmaze.database.asDomainModel
import com.android.sample.tvmaze.domain.Show
import com.android.sample.tvmaze.domain.asDatabaseModel
import com.android.sample.tvmaze.network.TVMazeService
import com.android.sample.tvmaze.util.Resource
import com.android.sample.tvmaze.util.contextProvider.CoroutineContextProvider
import com.android.sample.tvmaze.util.isNetworkAvailable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class ShowRepository(
    private val dao: ShowDao,
    private val api: TVMazeService,
    private val context: Context,
    private val contextProvider: CoroutineContextProvider
) {

    private val _shows = MutableStateFlow<Resource<List<Show>>>(Resource.idle())
    val shows: StateFlow<Resource<List<Show>>>
        get() = _shows

    @FlowPreview
    suspend fun fetchShows() {
        _shows.value = Resource.loading()
        dao.getShows().flatMapConcat { showsFromDb ->
            if (showsFromDb.isEmpty()) {
                if (context.isNetworkAvailable()) {
                    val apiShows = api.fetchShowList()
                    dao.insertAll(*apiShows.asDatabaseModel())
                    flow {
                        emit(Resource.success(apiShows))
                    }
                } else {
                    flow {
                        emit(Resource.error(context.getString(R.string.failed_network_msg)))
                    }
                }
            } else {
                flow {
                    emit(Resource.success(showsFromDb.asDomainModel()))
                }
            }
        }.flowOn(contextProvider.io)
            .catch {
                _shows.value = Resource.error(context.getString(R.string.failed_loading_msg))
            }.collect {
                _shows.value = it
            }
    }
}