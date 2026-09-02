package com.hepexta.allagents.ports;

import java.time.LocalDateTime;

public interface Clock {

    LocalDateTime now();
}
