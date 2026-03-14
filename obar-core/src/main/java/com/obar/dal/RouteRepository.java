package com.obar.dal;

import com.obar.model.Route;

public class RouteRepository extends BaseRepository<Route, Integer> {

    public RouteRepository() {
        super(Route.class);
    }
}