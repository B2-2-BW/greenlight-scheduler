package com.winten.greenlight.scheduler.domain.alert;

import java.util.List;

public interface AlertStateStore {
    AlertState get(String fingerprint);

    void put(String fingerprint, AlertState state);

    void delete(String fingerprint);

    List<AlertState> findAll();
}
