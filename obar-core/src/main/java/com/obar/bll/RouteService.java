package com.obar.bll;

import com.obar.dal.RouteRepository;
import com.obar.model.Route;

import java.util.List;
import java.util.Optional;

public class RouteService {

    private final RouteRepository routeRepository = new RouteRepository();

    public Route save(Route route) {
        return routeRepository.save(route);
    }

    public Optional<Route> findById(Integer id) {
        return routeRepository.findById(id);
    }

    public List<Route> findAll() {
        return routeRepository.findAll();
    }
}