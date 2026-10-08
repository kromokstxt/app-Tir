package com.Tir.demo;

public class Shooter implements Identifiable {

    private int id;
    private String firstName;
    private String lastName;
    private int clubId;
    private String username;
    private String password;
    private boolean admin;

    public Shooter(int id, String firstName, String lastName, int clubId,
                   String username, String password, boolean admin) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.clubId = clubId;
        this.username = username;
        this.password = password;
        this.admin = admin;
    }

    public int getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public int getClubId() { return clubId; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean isAdmin() { return admin; }
}
