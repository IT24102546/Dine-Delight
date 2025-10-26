package org.example.dinedelightsystems.web;

import org.example.dine_delight.model.EventLocation;
import org.example.dine_delight.model.EventSpace;
import org.example.dine_delight.repository.EventLocationRepository;
import org.example.dine_delight.repository.EventSpaceRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/places")
public class AdminPlaceController {

    private final EventLocationRepository locationRepository;
    private final EventSpaceRepository spaceRepository;

    public AdminPlaceController(EventLocationRepository locationRepository, EventSpaceRepository spaceRepository) {
        this.locationRepository = locationRepository;
        this.spaceRepository = spaceRepository;
    }

    @GetMapping
    public String manage(Model model) {
        model.addAttribute("locations", locationRepository.findAll());
        model.addAttribute("spaces", spaceRepository.findAll());
        return "admin/places";
    }

    @PostMapping("/location")
    public String addLocation(@RequestParam String name) {
        EventLocation loc = new EventLocation();
        loc.setName(name);
        locationRepository.save(loc);
        return "redirect:/admin/places?locAdded";
    }

    @PostMapping("/space")
    public String addSpace(@RequestParam String name,
                           @RequestParam int capacity,
                           @RequestParam Long locationId,
                           @RequestParam(defaultValue = "1000") int pricePerPersonPerHourLkr) {
        EventLocation loc = locationRepository.findById(locationId).orElse(null);
        if (loc == null) return "redirect:/admin/places?error";
        EventSpace space = new EventSpace();
        space.setName(name);
        space.setCapacity(capacity);
        space.setLocation(loc);
        // Convert LKR to cents
        space.setPricePerPersonPerHourCents(pricePerPersonPerHourLkr * 100);
        spaceRepository.save(space);
        return "redirect:/admin/places?spaceAdded";
    }

    @PostMapping("/space/{id}")
    public String updateSpace(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam int capacity,
                              @RequestParam Long locationId,
                              @RequestParam(defaultValue = "1000") int pricePerPersonPerHourLkr) {
        EventLocation loc = locationRepository.findById(locationId).orElse(null);
        if (loc == null) return "redirect:/admin/places?error";
        return spaceRepository.findById(id)
                .map(s -> {
                    s.setName(name);
                    s.setCapacity(capacity);
                    s.setLocation(loc);
                    s.setPricePerPersonPerHourCents(pricePerPersonPerHourLkr * 100);
                    spaceRepository.save(s);
                    return "redirect:/admin/places?spaceUpdated";
                })
                .orElse("redirect:/admin/places?error");
    }

    @PostMapping("/space/{id}/delete")
    public String deleteSpace(@PathVariable Long id) {
        if (spaceRepository.existsById(id)) {
            spaceRepository.deleteById(id);
            return "redirect:/admin/places?spaceDeleted";
        }
        return "redirect:/admin/places?error";
    }

    @PostMapping("/location/{id}/delete")
    public String deleteLocation(@PathVariable Long id) {
        if (locationRepository.existsById(id)) {
            locationRepository.deleteById(id);
            return "redirect:/admin/places?locDeleted";
        }
        return "redirect:/admin/places?error";
    }
}



