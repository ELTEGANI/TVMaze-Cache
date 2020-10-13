package com.android.sample.tvmaze

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.android.sample.tvmaze.database.ShowDao
import com.android.sample.tvmaze.domain.Show
import com.android.sample.tvmaze.network.TVMazeService
import com.android.sample.tvmaze.repository.ShowRepository
import com.android.sample.tvmaze.util.Resource
import com.android.sample.tvmaze.util.TestCoroutineRule
import com.android.sample.tvmaze.util.contextProvider.TestContextProvider
import com.android.sample.tvmaze.util.isNetworkAvailable
import com.android.sample.tvmaze.viewmodel.MainViewModel
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import retrofit2.Response
import retrofit2.mock.Calls

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    @Mock
    private lateinit var api: TVMazeService

    @Mock
    private lateinit var dao: ShowDao

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var resource: Observer<Resource<List<Show>>>

    @Captor
    private lateinit var captor: ArgumentCaptor<Resource<List<Show>>>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun givenServerResponse200_whenFetch_shouldReturnSuccess() {
        mockkStatic("com.android.sample.tvmaze.util.ContextExtKt")
        every {
            context.isNetworkAvailable()
        } returns true
        `when`(api.fetchShowList()).thenReturn(Calls.response(Response.success(emptyList())))
        `when`(dao.getShows()).thenReturn(flowOf(emptyList()))
        val repository = ShowRepository(dao, api, context, TestContextProvider())
        val viewModel = MainViewModel(repository).apply {
            shows.observeForever(resource)
        }
        try {
            verify(resource, times(2)).onChanged(captor.capture())
            verify(resource).onChanged(Resource.loading())
            verify(resource).onChanged(Resource.success(emptyList()))
        } finally {
            viewModel.shows.removeObserver(resource)
        }
    }

    @Test
    fun givenNetworkUnAvailable_whenFetch_shouldReturnError() {
        val errorMsg = "error message"
        `when`(context.getString(anyInt())).thenReturn(errorMsg)
        mockkStatic("com.android.sample.tvmaze.util.ContextExtKt")
        every {
            context.isNetworkAvailable()
        } returns false
        `when`(dao.getShows()).thenReturn(flowOf(emptyList()))
        val repository = ShowRepository(dao, api, context, TestContextProvider())
        val viewModel = MainViewModel(repository).apply {
            shows.observeForever(resource)
        }
        try {
            verify(resource).onChanged(Resource.loading())
            verify(resource).onChanged(Resource.error(errorMsg))
        } finally {
            viewModel.shows.removeObserver(resource)
        }
    }

    @Test
    fun givenServerResponseError_whenFetch_shouldReturnError() {
        val errorMsg = "error message"
        `when`(context.getString(anyInt())).thenReturn(errorMsg)
        mockkStatic("com.android.sample.tvmaze.util.ContextExtKt")
        every {
            context.isNetworkAvailable()
        } returns true
        `when`(api.fetchShowList()).thenThrow(RuntimeException(""))
        `when`(dao.getShows()).thenReturn(flowOf(emptyList()))
        val repository = ShowRepository(dao, api, context, TestContextProvider())
        val viewModel = MainViewModel(repository).apply {
            shows.observeForever(resource)
        }
        try {
            verify(resource).onChanged(Resource.loading())
            verify(resource).onChanged(Resource.error(errorMsg))
        } finally {
            viewModel.shows.removeObserver(resource)
        }
    }
}