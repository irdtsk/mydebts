package mydebts.android.app.feature.event

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker

import javax.inject.Inject

import mydebts.android.app.R
import mydebts.android.app.data.model.Event
import mydebts.android.app.data.model.Participant
import mydebts.android.app.di.SubcomponentBuilderResolver
import mydebts.android.app.feature.main.MainRouter
import mydebts.android.app.feature.participant.ParticipantActivity

class EventFragment : Fragment(), EventScreen, DatePickerDialog.OnDateSetListener {

    @Inject lateinit var presenter: EventPresenter

    private lateinit var emptyView: View
    private lateinit var participantsRecyclerView: RecyclerView
    private lateinit var adapter: ParticipantsAdapter

    private var deleteMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)

        (SubcomponentBuilderResolver.resolve(this) as EventSubcomponent.Builder)
                .event(arguments?.getParcelable(ARG_EVENT))
                .fragment(this)
                .build()
                .inject(this)

        adapter = ParticipantsAdapter(presenter.getParticipants())
        adapter.setOnParticipantClickListener { presenter.onParticipantClick(it) }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater?.let {
                val rootView = it.inflate(R.layout.activity_event, container, false)

                emptyView = rootView.findViewById(R.id.text_no_participants)

                participantsRecyclerView = rootView.findViewById(R.id.list_participants) as RecyclerView
                participantsRecyclerView.layoutManager = LinearLayoutManager(participantsRecyclerView.context)
                participantsRecyclerView.adapter = adapter

                rootView.findViewById(R.id.button_add_participant)
                        .setOnClickListener { presenter.onAddNewParticipantClick() }

                rootView
            }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        presenter.onViewCreated()
    }

    override fun onDestroyView() {
        presenter.onDestroyView()
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_event, menu)
        deleteMenuItem = menu?.findItem(R.id.action_delete)

        presenter.onCreateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
        item?.let {
            return when (it.itemId) {
                R.id.action_set_date -> {
                    presenter.onSetDateClick()
                    true
                }
                R.id.action_save -> {
                    presenter.onActionSaveClick()
                    true
                }
                R.id.action_delete -> {
                    presenter.onActionDeleteClick()
                    true
                }
                else -> {
                    false
                }
            }
        } ?: false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_PARTICIPANT && resultCode == Activity.RESULT_OK && data != null) {
            presenter.addParticipant(data.getParcelableExtra(ParticipantActivity.EXTRA_PARTICIPANT))
        }
    }

    override fun showTitle(title: CharSequence) {
        activity.title = title
    }

    override fun setDeleteMenuItemVisible(visible: Boolean) {
        deleteMenuItem?.isVisible = visible
    }

    override fun showDatePicker(year: Int, month: Int, day: Int) {
        val datePickerDialog = DatePickerDialog(activity, 0, this, year, month, day)
        datePickerDialog.show()
    }

    override fun setEmptyViewVisibility(visibility: Int) {
        emptyView.visibility = visibility
    }

    override fun setParticipantsViewVisibility(visibility: Int) {
        participantsRecyclerView.visibility = visibility
    }

    override fun notifyParticipantsChanged() {
        adapter.notifyDataSetChanged()
    }

    override fun notifyParticipantInserted(position: Int) {
        adapter.notifyItemInserted(position)
    }

    override fun showParticipant(participant: Participant) {
        startActivityForResult(ParticipantActivity.newIntent(activity, participant), REQUEST_CODE_PARTICIPANT)
    }

    override fun showNewParticipant() {
        startActivityForResult(ParticipantActivity.newIntent(activity), REQUEST_CODE_PARTICIPANT)
    }

    override fun navigateBack() {
        (activity as MainRouter).navigateBack()
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        presenter.setDate(year, month, dayOfMonth)
    }

    companion object {
        private val ARG_EVENT = "ARG_EVENT"

        private val REQUEST_CODE_PARTICIPANT = 0

        fun newInstance(event: Event): EventFragment {
            val fragment = newInstance()
            fragment.arguments = Bundle()
            fragment.arguments.putParcelable(ARG_EVENT, event)
            return fragment
        }

        fun newInstance(): EventFragment = EventFragment()
    }
}
