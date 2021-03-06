package mydebts.android.app.feature.event

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import mydebts.android.app.data.EventsSource
import mydebts.android.app.data.ParticipantsSource
import mydebts.android.app.data.PersonsSource
import mydebts.android.app.data.model.Event
import mydebts.android.app.data.model.Participant
import mydebts.android.app.feature.participant.ParticipantUi
import mydebts.android.app.extention.toEventDateString
import mydebts.android.app.feature.date.Date
import mydebts.android.app.rx.RxUtil
import mydebts.android.app.ui.ListEvent
import java.util.Calendar
import javax.inject.Inject

class EventViewModel constructor(
        private var event: Event?,
        private val eventsSource: EventsSource,
        private val personsSource: PersonsSource,
        private val participantsSource: ParticipantsSource,
        private val rxUtil: RxUtil,
        @ParticipantUi private val participantUiObservable: Observable<Participant>,
        private val dateObservable: Observable<Triple<Int, Int, Int>>): ViewModel()
{
    private val calendar = Calendar.getInstance()
    private val disposables = CompositeDisposable()
    private var participantDisposable: Disposable? = null
    private var dateDisposable: Disposable? = null

    private val _title = MutableLiveData<CharSequence>()
    val title: LiveData<CharSequence>
        get() {
            event?.date?.let { calendar.time = it }
            _title.value = calendar.time.toEventDateString()
            return _title
        }

    private val _dateNavigation = MutableLiveData<DateNavigation>()
    internal val dateNavigation: LiveData<DateNavigation>
        get() = _dateNavigation

    private val _deleteMenuItemVisible = MutableLiveData<Boolean>()
    val deleteMenuItemVisible: LiveData<Boolean>
        get() {
            _deleteMenuItemVisible.value = event != null
            return _deleteMenuItemVisible
        }

    private val participantsList = ArrayList<Participant>()
    private val _participants = MutableLiveData<Triple<List<Participant>?, ListEvent, Int?>>()
    val participants: LiveData<Triple<List<Participant>?, ListEvent, Int?>>
        get() {
            _participants.value = Triple<MutableList<Participant>?, ListEvent, Int?>(participantsList, ListEvent.LIST_CHANGED, null)
            loadParticipants()
            return _participants
        }

    private val _participantNavigation = MutableLiveData<ParticipantNavigation>()
    internal val participantNavigation: LiveData<ParticipantNavigation>
        get() = _participantNavigation

    private val _backNavigation = MutableLiveData<BackNavigation>()
    internal val backNavigation: LiveData<BackNavigation>
        get() = _backNavigation

    override fun onCleared() {
        disposables.clear()
        participantDisposable?.dispose()
        dateDisposable?.dispose()
    }

    internal fun onParticipantClick(position: Int) {
        if (participantDisposable == null) {
            participantDisposable = participantUiObservable.subscribe { addParticipant(it) }
        }

        _participantNavigation.value = ParticipantNavigation(participantsList[position])
        _participantNavigation.value = null
    }

    internal fun onAddNewParticipantClick() {
        if (participantDisposable == null) {
            participantDisposable = participantUiObservable.subscribe { addParticipant(it) }
        }

        _participantNavigation.value = ParticipantNavigation(null)
        _participantNavigation.value = null
    }

    internal fun onSetDateClick() {
        if (dateDisposable == null) {
            dateDisposable = dateObservable.subscribe {
                calendar.set(it.first, it.second, it.third)
                _title.value = calendar.time.toEventDateString()
            }
        }

        _dateNavigation.value = DateNavigation(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
        _dateNavigation.value = null
    }

    internal fun onSaveEventClick() {
        disposables.add(Observable.combineLatest(eventObservable(), participantObservable(),
                BiFunction { event: Event, (id, _, person, debt): Participant ->
                    Participant(id, event, person, debt) })
                .flatMap {
                    val personSingle = it.person?.let {
                        it.takeIf { it.id != null }?.let { Single.just(it) }
                                ?: personsSource.insert(it)
                    } ?: Single.just(it.person)

                    val participant = it
                    personSingle.flatMap {
                        participant.person = it
                        participant.takeIf { it.id != null }?.let { participantsSource.update(it) }
                                ?: participantsSource.insert(participant)
                    }.toObservable()
                }
                .compose(rxUtil.observableSchedulersTransformer())
                .subscribe { _ -> _backNavigation.value = BackNavigation() })
    }

    internal fun onDeleteEventClick() {
        event?.let {
            disposables.add(eventsSource.delete(it)
                    .compose(rxUtil.singleSchedulersTransformer())
                    .subscribe { _ -> _backNavigation.value = BackNavigation() })
        }
    }

    private fun loadParticipants() {
        event?.id?.let {
            disposables.add(participantsSource.getByEventId(it)
                    .compose(rxUtil.singleSchedulersTransformer())
                    .doOnSuccess { participantsList.clear(); participantsList.addAll(it) }
                    .map { Triple<MutableList<Participant>?, ListEvent, Int?>(participantsList, ListEvent.LIST_CHANGED, null) }
                    .subscribe { triple -> _participants.value = triple })
        }
    }

    private fun addParticipant(participant: Participant) {
        participantsList.indexOfFirst { it.person == participant.person }
                .let {
                    if (it > -1) {
                        participantsList[it].debt = participant.debt
                        _participants.value = Triple(null, ListEvent.ITEM_CHANGED, it)
                    } else {
                        participantsList.add(participant)
                        _participants.value = Triple(null, ListEvent.ITEM_INSERTED, participantsList.size - 1)
                    }
                }
    }

    private fun eventObservable(): Observable<Event> {
        val date = calendar.time

        val eventSingle = event?.let { it.name = date.toEventDateString(); it.date = date; it }
                ?.let { eventsSource.update(it) }
                ?: eventsSource.insert(Event( name = date.toString(), date = date))

        return eventSingle.toObservable()
    }

    private fun participantObservable(): Observable<Participant> =
            Observable.fromIterable(participantsList)

    class Factory @Inject constructor(
            private var event: Event?,
            private val eventsSource: EventsSource,
            private val personsSource: PersonsSource,
            private val participantsSource: ParticipantsSource,
            private val rxUtil: RxUtil,
            @ParticipantUi private val participantUiObservable: Observable<Participant>,
            @Date private val dateObservable: Observable<Triple<Int, Int, Int>>) : ViewModelProvider.Factory
    {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                EventViewModel(event, eventsSource, personsSource, participantsSource,
                    rxUtil, participantUiObservable, dateObservable) as T
    }
}