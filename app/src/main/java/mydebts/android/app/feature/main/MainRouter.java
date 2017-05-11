package mydebts.android.app.feature.main;

import android.support.annotation.NonNull;

import mydebts.android.app.data.model.Event;

public interface MainRouter {

    void navigateToEvents();

    void navigateToNewEvent();

    void navigateToEvent(@NonNull Event event);

    void navigateBack();
}
