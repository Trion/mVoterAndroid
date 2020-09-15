package com.popstack.mvoter2015.feature.votingguide

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.popstack.mvoter2015.databinding.ItemElectionCountdownBinding
import com.popstack.mvoter2015.databinding.ItemHowToVoteHeaderBinding
import com.popstack.mvoter2015.databinding.ItemHowToVoteSectionTitleBinding
import com.popstack.mvoter2015.databinding.ItemHowToVoteStepBinding
import com.popstack.mvoter2015.feature.votingguide.viewholders.*
import com.popstack.mvoter2015.helper.extensions.inflater

class VotingGuideRecyclerViewAdapter(private val viewItems: List<VotingGuideViewItem>) :
  RecyclerView.Adapter<VotingGuideViewHolder>() {

  companion object {
    const val VIEW_TYPE_HEADER = 1
    const val VIEW_TYPE_SECTION_TITLE = 2
    const val VIEW_TYPE_STEP = 3
    const val VIEW_TYPE_COUNT_DOWN = 4
  }

  override fun getItemViewType(position: Int): Int {
    return when (viewItems[position]) {
      is Header -> VIEW_TYPE_HEADER
      is SectionTitle -> VIEW_TYPE_SECTION_TITLE
      is Step -> VIEW_TYPE_STEP
      is CountDown -> VIEW_TYPE_COUNT_DOWN
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VotingGuideViewHolder {
    return when (viewType) {
      VIEW_TYPE_HEADER -> HeaderViewHolder(
        ItemHowToVoteHeaderBinding.inflate(
          parent.inflater(),
          parent,
          false
        )
      )
      VIEW_TYPE_SECTION_TITLE -> SectionTitleViewHolder(
        ItemHowToVoteSectionTitleBinding.inflate(
          parent.inflater(),
          parent,
          false
        )
      )
      VIEW_TYPE_COUNT_DOWN -> CountDownViewHolder(
        ItemElectionCountdownBinding.inflate(
          parent.inflater(),
          parent,
          false)
      )
      else -> StepViewHolder(ItemHowToVoteStepBinding.inflate(parent.inflater(), parent, false))
    }
  }

  override fun onBindViewHolder(holder: VotingGuideViewHolder, position: Int) {
    holder.bind(viewItems[position])
  }

  override fun onViewRecycled(holder: VotingGuideViewHolder) {
    super.onViewRecycled(holder)
    (holder as? CountDownViewHolder)?.stopCountDown()
  }

  override fun getItemCount(): Int = viewItems.size

}