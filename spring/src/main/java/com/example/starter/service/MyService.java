package com.example.starter.service;

import com.example.starter.entity.Item;
import com.example.starter.repository.ItemRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MyService {

    private final ItemRepo itemRepo;

    public MyService(ItemRepo itemRepo) {

        this.itemRepo = itemRepo;
    }

    public Item createNewItem(Item item) {

       return itemRepo.save(item);
    }

    public List<Item> getAllItems() {
        return itemRepo.findAll();
    }

    public String sayHello(String name) {
        return "Hello "+name +" ,This is done using IoC";
    }
}
