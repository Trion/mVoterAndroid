package com.popstack.mvoter2015.data.android.news

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.popstack.mvoter2015.data.android.paging.RemoteKeyDbProvider
import com.popstack.mvoter2015.data.cache.entity.NewsSearchRemoteKeyTable
import com.popstack.mvoter2015.data.common.news.NewsCacheSource
import com.popstack.mvoter2015.data.common.news.NewsNetworkSource
import com.popstack.mvoter2015.data.network.exception.NetworkException
import com.popstack.mvoter2015.domain.news.model.News

@OptIn(ExperimentalPagingApi::class)
class NewsRemoteMediator constructor(
  context: Context,
  private val newsCacheSource: NewsCacheSource,
  private val newsNetworkSource: NewsNetworkSource
) : RemoteMediator<Int, News>() {

  companion object {
    private const val START_PAGE_KEY = 1
  }

  private val remoteKeyDb = RemoteKeyDbProvider.INSTANCE(context)

  @OptIn(ExperimentalPagingApi::class)
  override suspend fun load(
    loadType: LoadType,
    state: PagingState<Int, News>
  ): RemoteMediator.MediatorResult {
    val page: Int = when (loadType) {
      LoadType.REFRESH -> {
        val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
        remoteKeys?.nextKey?.minus(1)
          ?.toInt() ?: START_PAGE_KEY
      }
      LoadType.PREPEND -> {
        val remoteKeys = getRemoteKeyForFirstItem(state)
          ?: return RemoteMediator.MediatorResult.Success(
            endOfPaginationReached = true
          )
        remoteKeys.previousKey?.toInt() ?: return RemoteMediator.MediatorResult.Success(
          endOfPaginationReached = true
        )
      }
      LoadType.APPEND -> {
        val remoteKeys = getRemoteKeyForLastItem(state)
          ?: return RemoteMediator.MediatorResult.Success(
            endOfPaginationReached = true
          )
        remoteKeys.nextKey?.toInt()
          ?: return RemoteMediator.MediatorResult.Success(endOfPaginationReached = true)
      }
    }
    try {
      val newsListFromNetwork = newsNetworkSource.getNewsList(page, state.config.pageSize)

      val endOfPaginationReached = newsListFromNetwork.isEmpty()

      val prevKey = if (page == START_PAGE_KEY) null else page - 1
      val nextKey = if (endOfPaginationReached) null else page + 1

      remoteKeyDb.transaction {
        if (loadType == LoadType.REFRESH) {
          remoteKeyDb.newsSearchRemoteKeyTableQueries.deleteAll()
          //TODO: Should delete everything from news table as well?
          //newsCacheSource.wipe()
        }

        newsListFromNetwork.forEach { news ->
          remoteKeyDb.newsSearchRemoteKeyTableQueries.insertOrReplace(
            newsId = news.id,
            previousKey = prevKey?.toLong(),
            nextKey = nextKey?.toLong()
          )
        }
        newsCacheSource.putNews(newsListFromNetwork)
      }

      return RemoteMediator.MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
    } catch (exception: NetworkException) {
      return RemoteMediator.MediatorResult.Error(exception)
    }
  }

  private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, News>): NewsSearchRemoteKeyTable? {
    return state.pages.lastOrNull() { it.data.isNotEmpty() }?.data?.lastOrNull()
      ?.let { news ->
        // Get the remote keys of the last item retrieved
        remoteKeyDb.newsSearchRemoteKeyTableQueries.selectById(news.id)
          .executeAsOne()
      }
  }

  private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, News>): NewsSearchRemoteKeyTable? {
    // Get the first page that was retrieved, that contained items.
    // From that first page, get the first item
    return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
      ?.let { news ->
        // Get the remote keys of the first items retrieved
        remoteKeyDb.newsSearchRemoteKeyTableQueries.selectById(news.id)
          .executeAsOne()
      }
  }

  private suspend fun getRemoteKeyClosestToCurrentPosition(
    state: PagingState<Int, News>
  ): NewsSearchRemoteKeyTable? {
    // The paging library is trying to load data after the anchor position
    // Get the item closest to the anchor position
    return state.anchorPosition?.let { position ->
      state.closestItemToPosition(position)
        ?.let { news ->
          remoteKeyDb.newsSearchRemoteKeyTableQueries.selectById(news.id)
            .executeAsOne()
        }
    }
  }

}