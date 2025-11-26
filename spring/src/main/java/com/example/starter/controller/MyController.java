package com.example.starter.controller;

import com.example.starter.entity.Item;
import com.example.starter.service.MyService;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MyController {

    private final MyService myService;

    public MyController(MyService myService) {
        this.myService = myService;
    }

    @PostMapping("/item")
    public Item addItem(@RequestBody Item newitem) {

        Item createdItem = myService.createNewItem(newitem);
        return createdItem;
    }

    @GetMapping("/items")
    public List<Item> getItems() {
        List<Item> items = myService.getAllItems();
        return items;
    }

    @GetMapping("/hello/{name}")
    public String index(@PathVariable String name) {
        return myService.sayHello(name);
    }
}
