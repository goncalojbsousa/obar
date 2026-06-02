package com.obar.web.driver;

import java.util.List;

public record DriverMapSnapshotResponse(
        List<DriverMapPointResponse> drivers,
        List<DriverMapPointResponse> clients) {
}
