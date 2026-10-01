package com.Tir.demo;

public class Shooter {

    private int id;
    private String firstName;
    private String lastName;
    private int clubId;

    public Shooter(int id, String firstName, String lastName, int clubId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.clubId = clubId;
    }

    public int getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public int getClubId() { return clubId; }
}
