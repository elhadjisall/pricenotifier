package com.example.demo.controller;

import com.example.demo.model.TrackedItem;
import com.example.demo.repository.TrackedItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class TrackedItemController {

    @Autowired
    private TrackedItemRepository repository;

    @PostMapping
    public TrackedItem createItem(@RequestBody TrackedItem item) {
        return repository.save(item);
    }

    @GetMapping
    public List<TrackedItem> getAllItems() {
        return repository.findAll();
    }
}
