package com.senac.corpfinancialapi;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
class Account {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false)
    private boolean demo;

    protected Account() {
    }

    Account(String name, boolean demo) {
        this.name = name;
        this.demo = demo;
    }

    UUID id() {
        return id;
    }

    String name() {
        return name;
    }

    boolean demo() {
        return demo;
    }
}
