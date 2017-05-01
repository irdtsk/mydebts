package mydebts.android.app.feature.events;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.List;

import javax.inject.Inject;

import mydebts.android.app.R;
import mydebts.android.app.db.Event;
import mydebts.android.app.di.SubcomponentBuilderResolver;
import mydebts.android.app.feature.event.EventActivity;
import mydebts.android.app.feature.main.MainRouter;

public class EventsFragment extends Fragment {

    @Inject EventsViewModel viewModel;
    @Inject RecyclerView eventsRecyclerView;
    @Inject View emptyView;
    @Inject EventsAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_events, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ((EventsSubcomponent.Builder) SubcomponentBuilderResolver.resolve(getActivity()))
                .module(new EventsUiModule(this, view))
                .build().inject(this);

        viewModel.fetchEvents()
                .subscribe(this::setEvents, this::setError);
    }

    void onAddEventClick() {
        ((MainRouter)getActivity()).navigateToNewEvent();
        //startActivity(new Intent(getActivity(), EventActivity.class));
    }

    void onEventClick(Event event) {
        Toast.makeText(getActivity(), event.getDate().toString(), Toast.LENGTH_SHORT).show();
    }

    private void setEvents(List<Event> events) {
        boolean isEmpty = events == null || events.size() == 0;

        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        eventsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        adapter.setEvents(events);
    }

    private void setError(Throwable throwable) {

    }
}